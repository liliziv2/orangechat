package me.rerere.rikkahub.data.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.rerere.rikkahub.data.db.dao.DriveStateDAO
import me.rerere.rikkahub.data.db.entity.DriveEventEntity
import me.rerere.rikkahub.data.db.entity.DriveSampleEntity
import me.rerere.rikkahub.data.db.entity.DriveStateEntity
import me.rerere.rikkahub.utils.JsonInstant
import kotlin.math.max

/**
 * State 层服务 —— 状态的持久化与事件入口。
 *
 * 这是 Elektron `DesireEngine` 类（Python 侧带 SQLite IO 的那一半）的对应物：
 * 内核（[DriveEngine] / [DriveBaseline]）是纯函数，这里负责把结果落盘、把事件收进来。
 *
 * ## 一次事件的完整链路
 *
 * ```
 * event
 *   → 惰性推进（按 last_ts 到现在补算，不需要后台心跳）
 *   → drive update（planEvent → applyPlan）
 *   → fatigue update（computeLocalFatigue）
 *   → state snapshot（有效激活 / PA-NA / 主导维）
 *   → 落盘（drive_state 单行）
 *   → 账本追加（drive_event_ledger，压制的也记）
 *   → 历史采样（drive_sample，基线参照系）
 * ```
 *
 * ## 边界（这个类自己刻意不做的）
 *
 * - **不生成 `emotion_wake`**、不调 `ProactiveMessageTriggerService`、不自动执行行为 ——
 *   「越线 → 唤醒」那一步在 [EmotionWakeBridge]（Behavior 层）里，由它去排队、
 *   去闩锁、去让现有闹钟提前醒。本类只把观测结果交出去；
 * - 不碰 Chat UI、不碰 Memory domain、不做 Archive；
 * - 没有后台定时器：状态推进是**惰性**的，只在有事件进来时按真实间隔补算。
 *   所以 App 放着不动时状态不会自己动 —— 这是有意的，不新增第二套后台唤醒。
 *
 * 阈值观测（[crossings]）是**只读**的：它把"哪些维越过了线"交出来，然后就结束，
 * 自己既不写 `last_ts`、也不写账本、也不采样。
 */
class DriveStateService(
    private val driveStateDAO: DriveStateDAO,
) {

    companion object {
        private val DRIVE_MAP_SERIALIZER = MapSerializer(String.serializer(), Double.serializer())
        private val STRING_LIST_SERIALIZER = ListSerializer(String.serializer())

        /**
         * 行为结果回写进账本时的来源标记。
         *
         * 与 `user_message` 那一路刻意区分开：账本要能回答"这一维是被什么推上去的"，
         * 也要能回答"它是被什么放下来的"。混成一个来源这两问都答不了。
         */
        const val SOURCE_AGENT_ACTION = "agent_action"

        /** 行为回写行的 reason。跟事件被压制的 reason 区分开。 */
        const val BEHAVIOR_REASON = "behavior_outcome"

        /** 证据字段的截断长度。 */
        const val EVIDENCE_MAX_CHARS = 400

        /**
         * 三个"列已开、本轮不驱动"的通道的默认值。
         *
         * 它们是 JSON 字面量而不是由代码算出来的 —— 因为这一轮没有任何代码会改它们，
         * 读出来必然等于写回去。等后续阶段真正驱动这些通道时，这里要换成从
         * `DriveEngine` 里的常量构造，别让字面量成为第二份真相。
         */
        private const val DEFAULT_POSSESSIVENESS_CHANNELS =
            """{"event_spike":0.0,"territorial_baseline":0.08,"last_event_ts":0,"last_baseline_ts":0}"""
        private const val DEFAULT_ATTACHMENT_REBOUND =
            """{"active":false,"phase":"settled","baseline":0.3,"overshoot":0.0,"started_at":0}"""
        private const val DEFAULT_LIBIDO_PENDING =
            """{"level":0.0,"armed":false,"last_cue_ts":0,"updated_at":0}"""
    }

    /**
     * 落盘的快照内容。
     *
     * 全是**派生量** —— 真相是 `drives_json` + `local_fatigue_json`，这一份是缓存，
     * 让"重启后能看到当时的状态长什么样"不必重新推一遍。
     */
    @Serializable
    data class DriveSnapshotPayload(
        /** 9 维有效激活（基线归一 + 疲劳压制后）。 */
        val activation: Map<String, Double>,
        /** 9 维有效分（raw × (1 − 局部疲劳)）。 */
        val effective: Map<String, Double>,
        /** PA 坐标（正向组均值）。 */
        val pa: Double,
        /** NA 坐标（负向组均值）。 */
        val na: Double,
        /** 当前激活最高的那一维。全为 0 时为 null。 */
        val dominant: String? = null,
        /** 快照时刻（毫秒）。 */
        val updatedAt: Long,
    )

    /** 读接口：当前状态的全貌。 */
    data class DriveStateView(
        val drives: Map<String, Double>,
        val localFatigue: Map<String, Double>,
        val snapshot: DriveSnapshotPayload?,
        val tickCount: Int,
        val escapeStreak: Int,
        val updatedAt: Long,
        val lastUserMessageAt: Long,
        val ledgerCount: Int,
        val sampleCount: Int,
    )

    /** 一次事件处理的结果。 */
    data class DriveEventOutcome(
        val ledgerId: Long,
        val primaryDrive: String?,
        val suppressed: Boolean,
        val reason: String,
        val applied: Map<String, DriveEngine.DriveDelta>,
        val drives: Map<String, Double>,
        val localFatigue: Map<String, Double>,
        val snapshot: DriveSnapshotPayload,
    )

    /** 账本里一条记录的可读形态。 */
    data class DriveEventRecord(
        val id: Long,
        val ts: Long,
        val source: String?,
        val eventLabel: String?,
        val primaryDrive: String?,
        val intensity: Double?,
        val confidence: Double?,
        val suppressed: Boolean,
        val reason: String?,
        val applied: Map<String, Double>,
        val evidence: List<String>,
    )

    // ─── 读 ──────────────────────────────────────────────────────────────────

    /** 当前状态全貌。首次安装、还没有任何事件时返回 null。 */
    suspend fun snapshot(): DriveStateView? = withContext(Dispatchers.IO) {
        val entity = driveStateDAO.getState() ?: return@withContext null
        DriveStateView(
            drives = decodeDrives(entity.drivesJson),
            localFatigue = decodeDrives(entity.localFatigueJson),
            snapshot = decodePayload(entity.snapshotJson),
            tickCount = entity.tickCount,
            escapeStreak = entity.escapeStreak,
            updatedAt = entity.lastTs,
            lastUserMessageAt = entity.lastUserMessageAt,
            ledgerCount = driveStateDAO.eventCount(),
            sampleCount = driveStateDAO.sampleCount(),
        )
    }

    /** 最近的事件，新的在前。 */
    suspend fun recentEvents(limit: Int = 12): List<DriveEventRecord> = withContext(Dispatchers.IO) {
        driveStateDAO.recentEvents(limit).map { row ->
            DriveEventRecord(
                id = row.id,
                ts = row.ts,
                source = row.source,
                eventLabel = row.eventLabel,
                primaryDrive = row.primaryDrive,
                intensity = row.intensity,
                confidence = row.confidence,
                suppressed = row.suppressed,
                reason = row.reason,
                applied = decodeApplied(row.appliedJson),
                evidence = decodeStringList(row.evidenceJson),
            )
        }
    }

    /** 当前基线（24 小时前同时段优先）。历史不够时返回 null。 */
    suspend fun baseline(now: Long = System.currentTimeMillis()): DriveBaseline.Baseline? =
        withContext(Dispatchers.IO) {
            val samples = driveStateDAO.samplesSince(now - DriveBaseline.retentionMillis())
                .map { DriveBaseline.Sample(it.ts, decodeDrives(it.valuesJson)) }
            DriveBaseline.pickBaseline(samples, now)
        }

    /**
     * 当前越过线的维度。**只读观测，不触发任何动作。**
     *
     * 这个函数是给后续阶段（Behavior 层）准备的接口；本轮没有任何调用点会拿它做决策。
     */
    suspend fun crossings(now: Long = System.currentTimeMillis()): List<DriveBaseline.Crossing> =
        withContext(Dispatchers.IO) {
            val entity = driveStateDAO.getState() ?: return@withContext emptyList()
            DriveBaseline.crossings(decodeDrives(entity.drivesJson), baseline(now))
        }

    // ─── 写 ──────────────────────────────────────────────────────────────────

    /**
     * 一条用户消息进入状态引擎。
     *
     * 事件包由 [DialogueEventSource] 从**对话的可观测结构**构造（不做语义分析），
     * 详见那个文件的类注释。
     */
    suspend fun onUserMessage(
        text: String,
        now: Long = System.currentTimeMillis(),
    ): DriveEventOutcome = applyEvent(DialogueEventSource.userMessage(text), now)

    /**
     * 应用一个事件包。
     *
     * 幂等性说明：**不幂等**，重复调用同一条消息会重复打脉冲。调用方负责去重
     * （当前的唯一调用点是 `ChatService.sendMessage`，一次用户发送只走一次）。
     */
    suspend fun applyEvent(
        event: DriveEngine.DriveEvent,
        now: Long = System.currentTimeMillis(),
    ): DriveEventOutcome = withContext(Dispatchers.IO) {
        val current = loadOrInit(now)
        val drivesBefore = decodeDrives(current.drivesJson)

        // 1) 惰性推进：把状态从 last_ts 推到 now。
        //    没有后台心跳，所以这是"过了多久"唯一的体现方式。
        val elapsed = max(0L, now - current.lastTs)
        val idleSeconds = if (current.lastUserMessageAt > 0L) {
            max(0.0, (now - current.lastUserMessageAt) / 1000.0)
        } else {
            max(0.0, elapsed / 1000.0)
        }
        val ticked = DriveEngine.tickDrives(
            drives = drivesBefore,
            escapeStreak = current.escapeStreak,
            elapsedMillis = elapsed,
            idleSeconds = idleSeconds,
        )

        // 2) 事件 → 计划 → 执行
        val plan = DriveEngine.planEvent(event, ticked.drives)
        val (drivesAfter, applied) = DriveEngine.applyPlan(ticked.drives, plan)

        // 3) 派生量（含每维独立疲劳）
        val localFatigue = DriveEngine.computeLocalFatigue(drivesAfter.getValue("fatigue"))
        val paNa = DriveEngine.paNaSnapshot(drivesAfter)
        val payload = DriveSnapshotPayload(
            activation = DriveEngine.activationSnapshot(drivesAfter, localFatigue),
            effective = DriveEngine.effectiveSnapshot(drivesAfter, localFatigue),
            pa = paNa.pa,
            na = paNa.na,
            dominant = DriveEngine.dominantDrive(drivesAfter, localFatigue),
            updatedAt = now,
        )

        // 4) 落盘
        val isUserMessage = event.source == DialogueEventSource.SOURCE_USER_MESSAGE
        driveStateDAO.upsertState(
            current.copy(
                drivesJson = encodeDrives(drivesAfter),
                tickCount = current.tickCount + 1,
                lastTs = now,
                prevDrivesJson = encodeDrives(drivesBefore),
                localFatigueJson = encodeDrives(localFatigue),
                snapshotJson = JsonInstant.encodeToString(DriveSnapshotPayload.serializer(), payload),
                escapeStreak = ticked.escapeStreak,
                lastUserMessageAt = if (isUserMessage) now else current.lastUserMessageAt,
            )
        )

        // 5) 账本：**被压制的也记**。只记成功的会让"今天状态为什么没动"变成无法回答的问题。
        val ledgerId = driveStateDAO.insertEvent(
            DriveEventEntity(
                ts = now,
                schemaVersion = DriveEngine.DRIVE_EVENT_SCHEMA,
                source = event.source,
                eventLabel = event.eventLabel,
                primaryDrive = plan.primaryDrive,
                intensity = plan.intensity,
                confidence = plan.confidence,
                agency = plan.agency,
                suppressed = plan.suppressed,
                reason = plan.reason,
                appliedJson = encodeApplied(applied),
                brainJson = encodeDrives(event.brain),
                evidenceJson = encodeStringList(event.evidence),
            )
        )

        // 6) 历史采样 + 清理超期行（基线参照系）
        driveStateDAO.insertSample(
            DriveSampleEntity(ts = now, valuesJson = encodeDrives(drivesAfter))
        )
        driveStateDAO.pruneSamples(now - DriveBaseline.retentionMillis())

        DriveEventOutcome(
            ledgerId = ledgerId,
            primaryDrive = plan.primaryDrive,
            suppressed = plan.suppressed,
            reason = plan.reason,
            applied = applied,
            drives = drivesAfter,
            localFatigue = localFatigue,
            snapshot = payload,
        )
    }

    // ─── 行为结果的回写（Closed Loop 的 State 侧）─────────────────────────────

    /** 一次行为的结果。两种都合法。 */
    enum class BehaviorOutcome {
        /** Agent 真的做了什么 → [DriveEngine.satisfy]（张力释放）。 */
        ACTED,

        /** Agent 自己决定不做 → [DriveEngine.refuseIntent]（中等回落）。 */
        DECLINED,
    }

    /** 回写结果。 */
    data class BehaviorWriteback(
        val ledgerId: Long,
        val eventLabel: String,
        val primaryDrive: String?,
        val applied: Map<String, Double>,
        val drives: Map<String, Double>,
        val snapshot: DriveSnapshotPayload,
    )

    /**
     * 把一个行为结果写回状态。
     *
     * ## 这是闭环的哪一段
     *
     * ```
     * emotion event → State → wake → Agent → 行为结果 → behavior memory → State/Memory 回写
     *                                                                     ^^^^^^^^^^^^^^^^ 这里
     * ```
     *
     * 没有这一步的话，"唤醒 → 行动"对内部状态没有任何影响：状态层只往外发信号，
     * 收不到任何反馈，闭环是断的。Elektron 那边对应 `desire_engine.satisfy()`
     * 与 `refuse_intent()`。
     *
     * ## 为什么 `DECLINED` 也要回写
     *
     * 因为"不做"是一个**决定**，不是失败。把它当成无事发生，会让同一条牵引
     * 在下一拍原样再唤醒一次 —— 那正是"长期高位重复唤醒"的另一种形态。
     * 回写一次中等回落，表示"这条牵引我看过了，这一刻不合当下"。
     *
     * ## 与 [applyEvent] 的关系
     *
     * 两条路径都是"改 drives → 重算派生量 → 落盘 → 追加账本 → 采样"，
     * 但**语义相反**：[applyEvent] 是事件往上推（加性脉冲），
     * 这里是行为往下放（乘性回落）。合成一条会得到一个参数比调用点还多的函数，
     * 所以刻意分成两个入口。
     *
     * [driveKey] 为 null（认不出是哪一维）时只记账本、不动 drives —— 记一笔
     * "这次行为发生了"比猜一个维度去打要诚实。
     */
    suspend fun resolveBehavior(
        driveKey: String?,
        outcome: BehaviorOutcome,
        eventLabel: String,
        detail: String,
        now: Long = System.currentTimeMillis(),
    ): BehaviorWriteback = withContext(Dispatchers.IO) {
        val current = loadOrInit(now)
        val drivesBefore = decodeDrives(current.drivesJson)

        // 与 applyEvent 同样的惰性推进：状态先补到 now，再叠加这次回写。
        val elapsed = max(0L, now - current.lastTs)
        val idleSeconds = if (current.lastUserMessageAt > 0L) {
            max(0.0, (now - current.lastUserMessageAt) / 1000.0)
        } else {
            max(0.0, elapsed / 1000.0)
        }
        val ticked = DriveEngine.tickDrives(
            drives = drivesBefore,
            escapeStreak = current.escapeStreak,
            elapsedMillis = elapsed,
            idleSeconds = idleSeconds,
        )

        val normalizedKey = DriveEngine.normalizeDriveKey(driveKey)
        val drivesAfter = when {
            normalizedKey == null -> ticked.drives
            outcome == BehaviorOutcome.ACTED -> DriveEngine.satisfy(ticked.drives, normalizedKey)
            else -> DriveEngine.refuseIntent(ticked.drives, normalizedKey)
        }

        val localFatigue = DriveEngine.computeLocalFatigue(drivesAfter.getValue("fatigue"))
        val paNa = DriveEngine.paNaSnapshot(drivesAfter)
        val payload = DriveSnapshotPayload(
            activation = DriveEngine.activationSnapshot(drivesAfter, localFatigue),
            effective = DriveEngine.effectiveSnapshot(drivesAfter, localFatigue),
            pa = paNa.pa,
            na = paNa.na,
            dominant = DriveEngine.dominantDrive(drivesAfter, localFatigue),
            updatedAt = now,
        )

        driveStateDAO.upsertState(
            current.copy(
                drivesJson = encodeDrives(drivesAfter),
                tickCount = current.tickCount + 1,
                lastTs = now,
                prevDrivesJson = encodeDrives(drivesBefore),
                localFatigueJson = encodeDrives(localFatigue),
                snapshotJson = JsonInstant.encodeToString(DriveSnapshotPayload.serializer(), payload),
                escapeStreak = ticked.escapeStreak,
            )
        )

        // 逐维变化明细：只有真的动了的维才进，没动的不写 0。
        val deltas = mutableMapOf<String, DriveEngine.DriveDelta>()
        drivesAfter.forEach { (key, after) ->
            val before = ticked.drives.getValue(key)
            if (after != before) {
                deltas[key] = DriveEngine.DriveDelta(
                    delta = DriveEngine.round4(after - before),
                    rawDelta = DriveEngine.round4(after - before),
                    before = DriveEngine.round4(before),
                    after = DriveEngine.round4(after),
                )
            }
        }

        val ledgerId = driveStateDAO.insertEvent(
            DriveEventEntity(
                ts = now,
                schemaVersion = DriveEngine.DRIVE_EVENT_SCHEMA,
                source = SOURCE_AGENT_ACTION,
                eventLabel = eventLabel,
                primaryDrive = normalizedKey,
                // 这不是"推断出来的强度"，是行为已经发生的事实：confidence/agency 拉满，
                // intensity 用张力实际掉了多少来表达。
                intensity = DriveEngine.round3(
                    deltas.values.sumOf { max(0.0, it.before - it.after) }
                ),
                confidence = 1.0,
                agency = 1.0,
                suppressed = false,
                reason = BEHAVIOR_REASON,
                appliedJson = encodeApplied(deltas),
                brainJson = null,
                evidenceJson = encodeStringList(listOf(detail.take(EVIDENCE_MAX_CHARS))),
            )
        )

        driveStateDAO.insertSample(
            DriveSampleEntity(ts = now, valuesJson = encodeDrives(drivesAfter))
        )
        driveStateDAO.pruneSamples(now - DriveBaseline.retentionMillis())

        BehaviorWriteback(
            ledgerId = ledgerId,
            eventLabel = eventLabel,
            primaryDrive = normalizedKey,
            applied = deltas.mapValues { it.value.delta },
            drives = drivesAfter,
            snapshot = payload,
        )
    }

    /** 账本里 `id > afterId` 的行，老的在前。关闭循环按这个游标增量推进。 */
    suspend fun ledgerAfter(afterId: Long, limit: Int = 50): List<DriveEventRecord> =
        withContext(Dispatchers.IO) {
            driveStateDAO.ledgerAfter(afterId, limit).map { row ->
                DriveEventRecord(
                    id = row.id,
                    ts = row.ts,
                    source = row.source,
                    eventLabel = row.eventLabel,
                    primaryDrive = row.primaryDrive,
                    intensity = row.intensity,
                    confidence = row.confidence,
                    suppressed = row.suppressed,
                    reason = row.reason,
                    applied = decodeApplied(row.appliedJson),
                    evidence = decodeStringList(row.evidenceJson),
                )
            }
        }

    /** 账本里最大的 id。首次运行建立检查点用它。 */
    suspend fun ledgerMaxId(): Long = withContext(Dispatchers.IO) { driveStateDAO.ledgerMaxId() }

    // ─── 内部 ────────────────────────────────────────────────────────────────

    /**
     * 读状态；没有就建一行初始的。
     *
     * 初始值取 [DriveEngine.DRIVE_BASELINES] —— 全新安装时"什么都没发生过"，
     * 所以每一维都停在静息水位上，有效激活全是 0。
     *
     * `lastUserMessageAt` 初始给 now 而不是 0：0 会让首次事件的缺席漂移把
     * 从 1970 年到现在的时间当成缺席时长，一上来就把私人生活三维推满。
     */
    private suspend fun loadOrInit(now: Long): DriveStateEntity {
        driveStateDAO.getState()?.let { return it }

        val initialDrives = DriveEngine.DRIVE_BASELINES
        val localFatigue = DriveEngine.computeLocalFatigue(
            initialDrives.getValue("fatigue")
        )
        val paNa = DriveEngine.paNaSnapshot(initialDrives)
        val payload = DriveSnapshotPayload(
            activation = DriveEngine.activationSnapshot(initialDrives, localFatigue),
            effective = DriveEngine.effectiveSnapshot(initialDrives, localFatigue),
            pa = paNa.pa,
            na = paNa.na,
            dominant = null,
            updatedAt = now,
        )

        val entity = DriveStateEntity(
            id = DriveStateEntity.SINGLETON_ID,
            drivesJson = encodeDrives(initialDrives),
            tickCount = 0,
            lastTs = now,
            prevDrivesJson = encodeDrives(initialDrives),
            localFatigueJson = encodeDrives(localFatigue),
            snapshotJson = JsonInstant.encodeToString(DriveSnapshotPayload.serializer(), payload),
            escapeStreak = 0,
            lastUserMessageAt = now,
            reunionPaBoost = 0.0,
            possessivenessChannelsJson = DEFAULT_POSSESSIVENESS_CHANNELS,
            attachmentReboundJson = DEFAULT_ATTACHMENT_REBOUND,
            libidoPendingJson = DEFAULT_LIBIDO_PENDING,
        )
        driveStateDAO.upsertState(entity)
        return entity
    }

    private fun encodeDrives(values: Map<String, Double>): String =
        JsonInstant.encodeToString(DRIVE_MAP_SERIALIZER, values)

    /** 解析 9 维 map。坏数据退回基线 —— 状态不该因为一行脏 JSON 就整个不可用。 */
    private fun decodeDrives(json: String?): Map<String, Double> {
        if (json.isNullOrBlank()) return DriveEngine.DRIVE_BASELINES
        val decoded = runCatching { JsonInstant.decodeFromString(DRIVE_MAP_SERIALIZER, json) }
            .getOrElse { return DriveEngine.DRIVE_BASELINES }
        return DriveEngine.normalizeDriveValues(decoded)
    }

    private fun encodeStringList(values: List<String>): String =
        JsonInstant.encodeToString(STRING_LIST_SERIALIZER, values)

    private fun decodeStringList(json: String?): List<String> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching { JsonInstant.decodeFromString(STRING_LIST_SERIALIZER, json) }
            .getOrElse { emptyList() }
    }

    /** 逐维变化明细。压制的行是 `{}`。 */
    private fun encodeApplied(applied: Map<String, DriveEngine.DriveDelta>): String {
        if (applied.isEmpty()) return "{}"
        val obj = buildJsonObject {
            applied.forEach { (key, delta) ->
                put(
                    key,
                    buildJsonObject {
                        put("delta", delta.delta)
                        put("raw_delta", delta.rawDelta)
                        put("before", delta.before)
                        put("after", delta.after)
                    },
                )
            }
        }
        return obj.toString()
    }

    /** 从 `applied_json` 里取每维的 `delta`（不是完整明细）。 */
    private fun decodeApplied(json: String?): Map<String, Double> {
        if (json.isNullOrBlank()) return emptyMap()
        return runCatching {
            val root = JsonInstant.parseToJsonElement(json)
            if (root !is kotlinx.serialization.json.JsonObject) return@runCatching emptyMap()
            root.mapNotNull { (key, element) ->
                val entry = element as? kotlinx.serialization.json.JsonObject ?: return@mapNotNull null
                val delta = entry["delta"] as? kotlinx.serialization.json.JsonPrimitive
                val value = delta?.content?.toDoubleOrNull() ?: return@mapNotNull null
                key to value
            }.toMap()
        }.getOrElse { emptyMap() }
    }

    private fun decodePayload(json: String?): DriveSnapshotPayload? {
        if (json.isNullOrBlank()) return null
        return runCatching {
            JsonInstant.decodeFromString(DriveSnapshotPayload.serializer(), json)
        }.getOrNull()
    }
}
