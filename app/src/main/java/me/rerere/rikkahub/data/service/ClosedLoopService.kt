/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.service

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.data.datastore.getCurrentAssistant
import me.rerere.rikkahub.data.db.entity.MemoryBankEntity
import me.rerere.rikkahub.utils.JsonInstant
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max
import kotlin.math.min

/**
 * Closed Loop —— 把状态与行为的结果搬回记忆库。
 *
 * 对应 Elektron 的 `behavior/closed_loop.py`。它是闭环的**最后一段**：
 *
 * ```
 * emotion event → State → wake → Agent → 行为结果 → behavior memory → State / Memory 回写
 *                                    ↑                                        ↑
 *                              本类的 behavior 腿                     本类的 emotion 腿
 * ```
 *
 * 两条腿：
 *
 * | 腿 | 输入 | 输出 | 对应 |
 * |---|---|---|---|
 * | State → Memory | 账本里"够格"的事件行 | 一条 `feel` 记忆（情绪回声） | `create_emotion_memory` |
 * | Behavior → Memory | 队列里 `done` 且没挂记忆 id 的行 | 一条 `behavior` 记忆（行动回声） | `create_behavior_memory` |
 *
 * State 侧的"行为回写"在 [DriveStateService.resolveBehavior]，不在这里 ——
 * 那是状态层自己的事，这里只管记忆。
 *
 * ## 首次运行只建检查点
 *
 * `closed_loop.py` 的文件头第一句就是："The first run establishes checkpoints only.
 * This deliberately avoids replaying the existing memory archive or old drive ledger
 * into the live emotional state."（首次运行只建立检查点，刻意避免把已有的记忆档案
 * 或旧的 drive 账本重放进活着的情绪状态。）
 *
 * 换成这里的场景：用户升级到这一版时，`drive_event_ledger` 里可能已经躺着
 * 几十上百条历史事件。全部当成"刚发生"回写成记忆，会一次性灌进去一堆
 * 只有事实没有意义的条目，还会把召回排序搅乱。所以首跑只记下
 * `last_ledger_id = 当前最大 id`，一条都不写。
 *
 * ## 双轨是硬要求
 *
 * `MemoryBankService.writeMemory` 会 `require` 事实轨与情绪轨都非空 ——
 * 缺一轨的记忆读回来只剩半截。所以这里两条腿都必须同时给出 `factTrack` 与
 * `feelTrack`，不能图省事只写 content。事实轨是结构化的（时间、来源、驱动、强度），
 * 情绪轨是"当时是什么感觉"的描述 —— 后者不能由前者推导出来，得单独写。
 */
class ClosedLoopService(
    context: Context,
    private val driveStateService: DriveStateService,
    private val memoryBankService: MemoryBankService,
    private val impulseQueueService: ImpulseQueueService,
    private val settingsStore: SettingsStore,
) {

    private val store = CheckpointStore(context)

    /** 检查点：闭环推进到哪了。 */
    @Serializable
    data class Checkpoint(
        /** 有没有建立过检查点。首次运行为 false —— 只建点，不回写。 */
        val initialized: Boolean = false,
        /** 已经处理过的最大账本 id。 */
        val lastLedgerId: Long = 0L,
    )

    /** 一次运行的结果。 */
    data class Outcome(
        val initialized: Boolean,
        val emotionMemories: Int,
        val behaviorMemories: Int,
        val scannedLedgerRows: Int,
        val lastLedgerId: Long,
    )

    /**
     * 跑一轮。
     *
     * 幂等：靠 [Checkpoint.lastLedgerId]（账本腿）与
     * `ImpulseEntity.resultMemoryId`（行为腿）两个游标，同一行不会被回写两次。
     *
     * 调用点有两处：用户发消息之后的 fire-and-forget，以及现有主动消息服务
     * 每轮收尾。**没有新增任何后台任务** —— 这两个点本来就是现成的。
     */
    suspend fun run(now: Long = System.currentTimeMillis()): Outcome = withContext(Dispatchers.IO) {
        val checkpoint = store.read()

        if (!checkpoint.initialized) {
            val maxId = driveStateService.ledgerMaxId()
            store.write(Checkpoint(initialized = true, lastLedgerId = maxId))
            logSafe("initialized: lastLedgerId=$maxId（不重放历史事件）")
            return@withContext Outcome(
                initialized = true,
                emotionMemories = 0,
                behaviorMemories = 0,
                scannedLedgerRows = 0,
                lastLedgerId = maxId,
            )
        }

        val assistantId = resolveAssistantId()

        // ── 腿一：State → Memory（情绪回声）──
        val rows = driveStateService.ledgerAfter(checkpoint.lastLedgerId, limit = LEDGER_BATCH)
        var emotionMemories = 0
        var cursor = checkpoint.lastLedgerId
        rows.forEach { row ->
            if (qualifies(row)) {
                runCatching { writeEmotionMemory(row, assistantId) }
                    .onSuccess { emotionMemories++ }
                    .onFailure { Log.w(TAG, "回写情绪回声失败 ledger=#${row.id}", it) }
            }
            // 游标无论如何都往前推：失败的行重试一次还是失败，
            // 卡在它上面会让后面的行永远轮不到（`closed_loop.py` 也是这么推的）。
            cursor = max(cursor, row.id)
        }

        // ── 腿二：Behavior → Memory（行动回声）──
        val doneImpulses = impulseQueueService.doneWithoutMemory(limit = MAX_MEMORY_PER_RUN)
        var behaviorMemories = 0
        doneImpulses.forEach { impulse ->
            runCatching {
                val memory = writeBehaviorMemory(impulse, assistantId)
                // 挂上 id 才算回写完成 —— 幂等就靠这个字段。
                impulseQueueService.attachMemory(impulse.id, memory.id)
            }.onSuccess { behaviorMemories++ }
                .onFailure { Log.w(TAG, "回写行动回声失败 impulse=#${impulse.id}", it) }
        }

        store.write(checkpoint.copy(lastLedgerId = cursor))
        impulseQueueService.prune(now)

        if (emotionMemories > 0 || behaviorMemories > 0) {
            logSafe(
                "emotion->memory=$emotionMemories behavior->memory=$behaviorMemories " +
                    "(ledger rows=${rows.size}, lastLedgerId=$cursor)"
            )
        }

        Outcome(
            initialized = false,
            emotionMemories = emotionMemories,
            behaviorMemories = behaviorMemories,
            scannedLedgerRows = rows.size,
            lastLedgerId = cursor,
        )
    }

    /**
     * 一条账本行够不够格回写成记忆。
     *
     * 与 `closed_loop.py` 的 `qualifies` 逐条对齐：
     * 没被压制、强度 ≥ 0.5、置信度 ≥ 0.65、有主驱动。
     * 门槛偏高是刻意的 —— 记忆库是稀缺资源，宁可漏掉几条弱的。
     */
    private fun qualifies(row: DriveStateService.DriveEventRecord): Boolean {
        if (row.suppressed) return false
        val intensity = row.intensity ?: return false
        val confidence = row.confidence ?: return false
        val drive = row.primaryDrive?.takeIf { it.isNotBlank() } ?: return false
        return intensity >= MIN_INTENSITY && confidence >= MIN_CONFIDENCE && drive.isNotBlank()
    }

    /** State → Memory：一条情绪回声。字段照 `create_emotion_memory` 搬。 */
    private suspend fun writeEmotionMemory(
        row: DriveStateService.DriveEventRecord,
        assistantId: String,
    ): Int {
        val drive = row.primaryDrive.orEmpty()
        val label = row.eventLabel?.takeIf { it.isNotBlank() } ?: drive
        val intensity = row.intensity ?: 0.0
        val confidence = row.confidence ?: 0.0
        val scene = row.evidence.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: "（这条事件没有留下原始证据）"

        // 负向维给低 valence：Elektron 的 `0.28 if drive in {stress, possessiveness, fatigue} else 0.5`
        val valence = if (drive in NEGATIVE_DRIVES) 0.28f else 0.5f
        val arousal = min(1.0, max(0.3, intensity)).toFloat()

        return memoryBankService.writeMemory(
            MemoryBankService.MemoryWriteRequest(
                content = scene,
                factTrack = buildString {
                    append("时间 ${formatTs(row.ts)}；来源 ${row.source ?: "unknown"}；")
                    append("事件 $label；主驱动 $drive；")
                    append("强度 ${"%.2f".format(intensity)}；置信度 ${"%.2f".format(confidence)}；")
                    append("账本 #${row.id}")
                },
                feelTrack = buildString {
                    append("$drive 当时到了 ${"%.2f".format(intensity)}，")
                    append("触发源是 ${row.source ?: "unknown"}（$label）。")
                    append("这是一条当时的 feel 记录，不自动等同于已经想明白的 insight。")
                },
                type = TYPE_FEEL,
                assistantId = assistantId,
                domain = listOf("self", "emotion"),
                valence = valence,
                arousal = arousal,
                importance = IMPORTANCE_FEEL,
                sourceType = SOURCE_LEDGER,
                sourceId = "drive-${row.id}",
                sourceTs = row.ts,
            )
        ).id
    }

    /** Behavior → Memory：一条行动回声。字段照 `create_behavior_memory` 搬。 */
    private suspend fun writeBehaviorMemory(
        impulse: ImpulseQueueService.ImpulseView,
        assistantId: String,
    ): me.rerere.rikkahub.data.db.entity.MemoryBankEntity {
        val payload = EmotionWakePayload.decode(impulse.payloadJson)
        val note = impulse.note?.takeIf { it.isNotBlank() } ?: "完成"
        val desc = payload?.desc ?: EmotionWakePayload.DESC
        val signals = payload?.signals?.joinToString("、") { it.id }.orEmpty()

        return memoryBankService.writeMemory(
            MemoryBankService.MemoryWriteRequest(
                content = "一次由情绪驱动的行动已经完成：$desc\n\n结果：$note",
                factTrack = buildString {
                    append("唤醒 #${impulse.id}（${impulse.kind}）以 done 收尾；")
                    append("尝试 ${impulse.attempts}/${impulse.maxAttempts} 次；")
                    if (signals.isNotBlank()) append("触发信号 $signals；")
                    append("完成于 ${formatTs(impulse.finishedAt)}")
                },
                feelTrack = buildString {
                    append("一次由内部状态驱动的行动已经完成。")
                    append("这不是「被要求做的事」，是当时确实想做。")
                    append("结果：$note")
                },
                type = TYPE_BEHAVIOR,
                assistantId = assistantId,
                domain = listOf("self", "behavior"),
                valence = BEHAVIOR_VALENCE,
                arousal = BEHAVIOR_AROUSAL,
                importance = IMPORTANCE_BEHAVIOR,
                sourceType = SOURCE_IMPULSE,
                sourceId = "impulse-${impulse.id}",
                sourceTs = impulse.finishedAt,
            )
        )
    }

    /**
     * 记忆写到哪个 assistant 名下。
     *
     * **必须跟读取侧（`MemoryBankService.recallForPrompt`）同一个口径**，
     * 否则会出现"存进去了但下次读不到" —— 那是这个项目已经踩过一次的坑。
     */
    private suspend fun resolveAssistantId(): String {
        val settings = settingsStore.settingsFlow.first()
        val assistant = settings.getCurrentAssistant()
        return if (assistant.useGlobalMemory) {
            MemoryBankService.GLOBAL_MEMORY_ID
        } else {
            assistant.id.toString()
        }
    }

    private fun formatTs(ts: Long): String = if (ts <= 0L) {
        "unknown"
    } else {
        TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()))
    }

    private fun logSafe(message: String) {
        runCatching { Log.i(TAG, message) }
    }

    /**
     * 检查点的持久化。
     *
     * 放 SharedPreferences 而不是 Room：它只有两个字段、只在闭环自己这里读写，
     * 而且跟 `ProactiveScheduleStore` / `EmotionWakeStore` 是同一类东西 ——
     * "某套机制的推进游标"。丢了的后果是"可能重复回写一轮"，不会丢数据。
     */
    private class CheckpointStore(context: Context) {
        private val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        fun read(): Checkpoint {
            val raw = prefs.getString(KEY, null) ?: return Checkpoint()
            return runCatching { JsonInstant.decodeFromString<Checkpoint>(raw) }
                .getOrDefault(Checkpoint())
        }

        fun write(checkpoint: Checkpoint) {
            prefs.edit { putString(KEY, JsonInstant.encodeToString(checkpoint)) }
        }

        private companion object {
            const val PREFS_NAME = "closed_loop_prefs"
            const val KEY = "checkpoint"
        }
    }

    companion object {
        private const val TAG = "ClosedLoopService"

        /** 情绪回声的门槛。对齐 `closed_loop.py` 的 `qualifies`。 */
        const val MIN_INTENSITY = 0.5
        const val MIN_CONFIDENCE = 0.65

        /** 一轮最多回写几条行为记忆。对齐 `MAX_MEMORY_PER_RUN = 3`。 */
        const val MAX_MEMORY_PER_RUN = 3

        /**
         * 一轮最多扫多少条账本行。
         *
         * 这是个**批量上界**，不是丢弃阈值：没扫到的行留在 `last_ledger_id` 之后，
         * 下一轮继续。所以长期不用之后不会一次性灌满记忆库。
         */
        const val LEDGER_BATCH = 20

        const val TYPE_FEEL = "closed_loop_feel"
        const val TYPE_BEHAVIOR = "closed_loop_behavior"

        const val SOURCE_LEDGER = "drive_event_ledger"
        const val SOURCE_IMPULSE = "behavior_impulse"

        const val IMPORTANCE_FEEL = 6
        const val IMPORTANCE_BEHAVIOR = 5

        const val BEHAVIOR_VALENCE = 0.58f
        const val BEHAVIOR_AROUSAL = 0.4f

        /** 负向维：给低 valence。 */
        val NEGATIVE_DRIVES = setOf("stress", "possessiveness", "fatigue")

        private val TIMESTAMP_FORMAT: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    }
}
