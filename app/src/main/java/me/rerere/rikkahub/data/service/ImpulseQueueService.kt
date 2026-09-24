package me.rerere.rikkahub.data.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.rerere.rikkahub.data.db.dao.ImpulseDAO
import me.rerere.rikkahub.data.db.entity.ImpulseEntity
import kotlin.uuid.Uuid

/**
 * 冲动队列的**策略**部分 —— 纯函数，无 IO、不碰时钟。
 *
 * 拆出来的理由跟 `DriveBaseline` 一样：状态机的边界条件（租约什么时候算过期、
 * 什么时候该退回队列、什么时候该放弃重试）是最容易写错、也最值得单测的一段，
 * 而它完全不需要 Room 就能判断。
 */
object ImpulsePolicy {

    const val KIND_EMOTION_WAKE = "emotion_wake"

    /** 唤醒载荷的 schema 版本。改结构时递增，好分辨库里那些老行。 */
    const val SCHEMA_VERSION = "impulse_v1"

    const val STATUS_PENDING = "pending"
    const val STATUS_CLAIMED = "claimed"
    const val STATUS_DEFERRED = "deferred"
    const val STATUS_DONE = "done"
    const val STATUS_FAILED = "failed"
    const val STATUS_IGNORED = "ignored"

    /** 还没处理完的状态。去重看的就是这一组。 */
    val ACTIVE_STATUSES: Set<String> = setOf(STATUS_PENDING, STATUS_CLAIMED, STATUS_DEFERRED)

    /** 不会再变的状态。 */
    val TERMINAL_STATUSES: Set<String> = setOf(STATUS_DONE, STATUS_FAILED, STATUS_IGNORED)

    /** 合法终态。`ignored` 是"Agent 自己决定不做"，不是失败。 */
    val FINISH_STATUSES: Set<String> = setOf(STATUS_DONE, STATUS_DEFERRED, STATUS_FAILED, STATUS_IGNORED)

    /**
     * 入队到到点之间的最小提前量。
     *
     * 跟 `ProactiveSchedulePlanner.MIN_LEAD_MILLIS` 同一个理由：不让内部状态
     * 在用户刚说完话的下一秒就插一条主动消息进来，观感是抢话。
     */
    const val MIN_LEAD_MILLIS = 60_000L

    /**
     * 租约时长。
     *
     * 比一次生成的正常耗时（含工具链，`MAX_TOOL_STEPS = 5`）长一截，
     * 又不能长到"进程被杀之后半天不恢复"。10 分钟是这两个约束的交点。
     */
    const val LEASE_MILLIS = 10 * 60_000L

    /** 延后的默认重试间隔。 */
    const val DEFERRED_RETRY_SECONDS = 3600L

    /** 延后的最小重试间隔（`impulse_queue.py` 里是 `max(300, retry_seconds)`）。 */
    const val DEFERRED_MIN_RETRY_SECONDS = 300L

    /** 终态行保留多久。7 天，跟 Elektron 的 `prune(keep_seconds=7*86400)` 一致。 */
    const val TERMINAL_RETENTION_MILLIS = 7L * 86_400_000L

    /** 默认重试上限。 */
    const val DEFAULT_MAX_ATTEMPTS = 3

    fun isActive(status: String): Boolean = status in ACTIVE_STATUSES

    fun isTerminal(status: String): Boolean = status in TERMINAL_STATUSES

    /** 租约过期了没有。没有租约（0）不算过期。 */
    fun leaseExpired(status: String, leaseExpiresAt: Long, now: Long): Boolean =
        status == STATUS_CLAIMED && leaseExpiresAt > 0L && leaseExpiresAt <= now

    /** 延后到点了没有。 */
    fun deferredReady(status: String, retryAt: Long, now: Long): Boolean =
        status == STATUS_DEFERRED && retryAt > 0L && retryAt <= now

    /** 到点且可以认领。 */
    fun claimable(status: String, dueAt: Long, now: Long): Boolean =
        status == STATUS_PENDING && dueAt <= now

    /** 重试预算还有没有。 */
    fun canRetry(attempts: Int, maxAttempts: Int): Boolean = attempts < maxAttempts

    /** 延后时刻：不小于 [DEFERRED_MIN_RETRY_SECONDS]，也不小于调用方给的值。 */
    fun retryAt(now: Long, retrySeconds: Long): Long =
        now + maxOf(DEFERRED_MIN_RETRY_SECONDS, retrySeconds) * 1000L

    /** 延后时长收敛到一个上界，免得一条唤醒被推到遥遥无期。 */
    fun clampRetrySeconds(seconds: Long): Long =
        seconds.coerceIn(DEFERRED_MIN_RETRY_SECONDS, DEFERRED_RETRY_SECONDS)
}

/**
 * 冲动队列 —— 状态机 + 持久化。
 *
 * 对应 Elektron 的 `behavior/impulse_queue.py`。那边用 `fcntl.flock` + 整文件
 * 原子替换；这边用 Room 落盘、用一把 [lock] 收口状态迁移。
 *
 * ## 每个方法在做什么
 *
 * | 方法 | 作用 | 对应 Python |
 * |---|---|---|
 * | [enqueue] | 入队（带活跃去重） | `enqueue` |
 * | [recover] | 租约过期退回、延后到点放回、重试耗尽判失败 | `_recover` |
 * | [claimDue] | 认领最早到点的一条 | `claim` |
 * | [markDone] / [markIgnored] / [markFailed] / [defer] | 落终态或延后 | `finish` |
 * | [attachMemory] | 把回写成的记忆 id 挂回去 | `attach_memory` |
 * | [prune] | 清过期的终态行 | `prune` |
 *
 * ## 崩溃恢复就是 [recover]
 *
 * 它每次 [claimDue] 之前都会跑一遍。这不是"定时任务"，而是"每次要用队列时
 * 先把上次留下的烂摊子收拾掉" —— 所以进程被杀不会留下永远卡住的 `claimed` 行，
 * 也不需要新增任何后台唤醒。
 */
class ImpulseQueueService(
    private val impulseDAO: ImpulseDAO,
) {
    private val lock = Mutex()

    /** 一条队列行的可读形态。载荷解析交给调用方（它才知道载荷该长什么样）。 */
    data class ImpulseView(
        val id: Long,
        val kind: String,
        val status: String,
        val source: String,
        val dedupeKey: String,
        val payloadJson: String,
        val createdAt: Long,
        val dueAt: Long,
        val attempts: Int,
        val maxAttempts: Int,
        val retryAt: Long,
        val leaseExpiresAt: Long,
        val lastError: String?,
        val note: String?,
        val resultMemoryId: Int?,
        val finishedAt: Long,
    )

    /** 入队结果。 */
    sealed interface EnqueueResult {
        /** 真的排进去了。 */
        data class Enqueued(val id: Long, val dueAt: Long) : EnqueueResult

        /** 同一个 dedupe_key 已经有活跃行了，不重复排。 */
        data class Duplicate(val existingId: Long, val existingStatus: String) : EnqueueResult
    }

    // ─── 写 ──────────────────────────────────────────────────────────────────

    /**
     * 入队。
     *
     * 活跃去重在 [lock] 里查一遍再做：`impulse_queue` 上没有唯一索引
     * （理由见 [ImpulseEntity] 的类注释），唯一性只能在这里保证。
     */
    suspend fun enqueue(
        kind: String,
        dedupeKey: String,
        payloadJson: String,
        dueAt: Long,
        source: String,
        assistantId: String? = null,
        maxAttempts: Int = ImpulsePolicy.DEFAULT_MAX_ATTEMPTS,
        now: Long = System.currentTimeMillis(),
    ): EnqueueResult = withContext(Dispatchers.IO) {
        lock.withLock {
            impulseDAO.findActive(kind, dedupeKey)?.let { existing ->
                return@withLock EnqueueResult.Duplicate(existing.id, existing.status)
            }
            val id = impulseDAO.insert(
                ImpulseEntity(
                    createdAt = now,
                    dueAt = dueAt,
                    kind = kind,
                    status = ImpulsePolicy.STATUS_PENDING,
                    source = source,
                    dedupeKey = dedupeKey,
                    payloadJson = payloadJson,
                    assistantId = assistantId,
                    attempts = 0,
                    maxAttempts = maxAttempts,
                    schemaVersion = ImpulsePolicy.SCHEMA_VERSION,
                )
            )
            EnqueueResult.Enqueued(id = id, dueAt = dueAt)
        }
    }

    /**
     * 恢复流程。**每次认领之前都会跑。**
     *
     * 三件事：
     * 1. 租约过期的 `claimed` → `pending`（进程被杀留下的）；
     * 2. 到点的 `deferred` → `pending`；
     * 3. 重试预算耗尽的 `pending` → `failed`（不再无限重试）。
     *
     * @return 被恢复的行数，调用方可以拿去记日志。
     */
    suspend fun recover(now: Long = System.currentTimeMillis()): Int =
        withContext(Dispatchers.IO) {
            lock.withLock { recoverLocked(now) }
        }

    private suspend fun recoverLocked(now: Long): Int {
        var touched = 0

        impulseDAO.expiredLeases(now).forEach { row ->
            impulseDAO.update(
                row.copy(
                    status = ImpulsePolicy.STATUS_PENDING,
                    claimedAt = 0L,
                    leaseOwner = null,
                    leaseExpiresAt = 0L,
                    lastError = "领取租约过期，自动退回队列",
                )
            )
            touched++
        }

        impulseDAO.dueDeferred(now).forEach { row ->
            impulseDAO.update(
                row.copy(
                    status = ImpulsePolicy.STATUS_PENDING,
                    retryAt = 0L,
                )
            )
            touched++
        }

        impulseDAO.exhaustedRetries().forEach { row ->
            impulseDAO.update(
                row.copy(
                    status = ImpulsePolicy.STATUS_FAILED,
                    finishedAt = now,
                    lastError = "重试次数已用尽（${row.attempts}/${row.maxAttempts}）",
                    claimedAt = 0L,
                    leaseOwner = null,
                    leaseExpiresAt = 0L,
                )
            )
            touched++
        }

        if (touched > 0) {
            logSafe("recover: 修正了 $touched 条队列行（租约/延后/重试耗尽）")
        }
        return touched
    }

    /**
     * 认领最早一条到点的冲动。
     *
     * 先跑 [recover]，再挑 `pending` 且 `due_at <= now` 的最早一条，置 `claimed`
     * 并挂租约。返回 null 表示现在没有可认领的东西。
     *
     * [owner] 是认领者标识；租约到期后这一行会被退回队列，不管是哪个 owner 挂的。
     */
    suspend fun claimDue(
        owner: String = Uuid.random().toString(),
        now: Long = System.currentTimeMillis(),
        leaseMillis: Long = ImpulsePolicy.LEASE_MILLIS,
    ): ImpulseView? = withContext(Dispatchers.IO) {
        lock.withLock {
            recoverLocked(now)

            val candidate = impulseDAO.duePending(now, limit = 1).firstOrNull()
                ?: return@withLock null

            if (!ImpulsePolicy.canRetry(candidate.attempts, candidate.maxAttempts)) {
                impulseDAO.update(
                    candidate.copy(
                        status = ImpulsePolicy.STATUS_FAILED,
                        finishedAt = now,
                        lastError = "重试次数已用尽（${candidate.attempts}/${candidate.maxAttempts}）",
                    )
                )
                return@withLock null
            }

            val claimed = candidate.copy(
                status = ImpulsePolicy.STATUS_CLAIMED,
                claimedAt = now,
                leaseOwner = owner,
                leaseExpiresAt = now + leaseMillis,
                attempts = candidate.attempts + 1,
                retryAt = 0L,
            )
            impulseDAO.update(claimed)
            claimed.toView()
        }
    }

    /**
     * 只读地看有没有到点的冲动。**不认领、不改状态。**
     *
     * 给"先判断要不要跑、跑之前才认领"的调用方用（见 `ProactiveMessageTriggerService`）：
     * 如果因为节流放弃了这一轮，不该把这条唤醒的尝试次数白白烧掉。
     */
    suspend fun peekDue(now: Long = System.currentTimeMillis()): ImpulseView? =
        withContext(Dispatchers.IO) {
            impulseDAO.duePending(now, limit = 1).firstOrNull()?.toView()
        }

    /** 落终态 `done`。行为真的发生了。 */
    suspend fun markDone(
        id: Long,
        note: String? = null,
        now: Long = System.currentTimeMillis(),
    ) = finish(id, ImpulsePolicy.STATUS_DONE, note, now)

    /**
     * 落终态 `ignored`。**Agent 自己决定不做。**
     *
     * 这是合法结果而不是失败 —— 情绪只有唤醒权，没有行动决策权；
     * 把它记成失败会让"拒绝行动"变成一件需要被重试的错事。
     */
    suspend fun markIgnored(
        id: Long,
        note: String? = null,
        now: Long = System.currentTimeMillis(),
    ) = finish(id, ImpulsePolicy.STATUS_IGNORED, note, now)

    /** 落终态 `failed`。执行出错且不再重试。 */
    suspend fun markFailed(
        id: Long,
        error: String? = null,
        now: Long = System.currentTimeMillis(),
    ) = finish(id, ImpulsePolicy.STATUS_FAILED, error, now)

    /** 延后到 [retryAt]。不落终态，[ImpulsePolicy.ACTIVE_STATUSES] 里仍有它，所以不会重复入队。 */
    suspend fun defer(
        id: Long,
        retrySeconds: Long = ImpulsePolicy.DEFERRED_RETRY_SECONDS,
        note: String? = null,
        now: Long = System.currentTimeMillis(),
    ) = withContext(Dispatchers.IO) {
        lock.withLock {
            val row = impulseDAO.getById(id) ?: return@withLock
            impulseDAO.update(
                row.copy(
                    status = ImpulsePolicy.STATUS_DEFERRED,
                    retryAt = ImpulsePolicy.retryAt(now, ImpulsePolicy.clampRetrySeconds(retrySeconds)),
                    note = note?.take(NOTE_MAX_CHARS) ?: row.note,
                    claimedAt = 0L,
                    leaseOwner = null,
                    leaseExpiresAt = 0L,
                )
            )
        }
    }

    private suspend fun finish(id: Long, status: String, note: String?, now: Long) {
        require(status in ImpulsePolicy.FINISH_STATUSES) { "invalid impulse status: $status" }
        withContext(Dispatchers.IO) {
            lock.withLock {
                val row = impulseDAO.getById(id) ?: return@withLock
                impulseDAO.update(
                    row.copy(
                        status = status,
                        note = note?.take(NOTE_MAX_CHARS) ?: row.note,
                        lastError = if (status == ImpulsePolicy.STATUS_FAILED) {
                            note?.take(NOTE_MAX_CHARS)
                        } else {
                            row.lastError
                        },
                        claimedAt = 0L,
                        leaseOwner = null,
                        leaseExpiresAt = 0L,
                        retryAt = 0L,
                        finishedAt = now,
                    )
                )
            }
        }
    }

    /**
     * 把回写成的记忆 id 挂回这一行。
     *
     * `closed_loop.py` 用 `result_memory_id` 做幂等：只处理 `done` 且没挂 id 的行，
     * 挂上之后同一条冲动不会被回写两次。所以这个字段不是装饰。
     */
    suspend fun attachMemory(id: Long, memoryId: Int): Boolean = withContext(Dispatchers.IO) {
        lock.withLock {
            val row = impulseDAO.getById(id) ?: return@withLock false
            impulseDAO.update(row.copy(resultMemoryId = memoryId))
            true
        }
    }

    /** 清掉过期的终态行。返回删了几行。 */
    suspend fun prune(now: Long = System.currentTimeMillis()): Int = withContext(Dispatchers.IO) {
        impulseDAO.pruneTerminal(now - ImpulsePolicy.TERMINAL_RETENTION_MILLIS)
    }

    // ─── 读 ──────────────────────────────────────────────────────────────────

    /** 某个 kind 有没有活跃行。去重与"已经 active 的不会重复创建"都靠它。 */
    suspend fun hasActive(kind: String, dedupeKey: String): Boolean = withContext(Dispatchers.IO) {
        impulseDAO.findActive(kind, dedupeKey) != null
    }

    /**
     * 某个 kind 有没有任意活跃行（不限 dedupe_key）。
     *
     * `behavior.py` 的 `has_active_kind` 就是这个语义：只要队列里还有一条
     * 没处理完的同 kind 冲动，就不再排新的。
     */
    suspend fun hasAnyActive(kind: String): Boolean = withContext(Dispatchers.IO) {
        impulseDAO.countActive(kind) > 0
    }

    /** 某个 kind 下最早的一条活跃行。去重判定要它的 id 与状态。 */
    suspend fun firstActive(kind: String): ImpulseView? = withContext(Dispatchers.IO) {
        impulseDAO.firstActive(kind)?.toView()
    }

    /** 某个 kind 最近一次已结束的结果。最小间隔判断用它。 */
    suspend fun lastOutcome(kind: String): ImpulseView? = withContext(Dispatchers.IO) {
        impulseDAO.lastOutcome(kind)?.toView()
    }

    /** 最早一条还没到点的 pending 的 `due_at`。让现有闹钟提前醒用。 */
    suspend fun nextPendingDueAt(now: Long = System.currentTimeMillis()): Long? =
        withContext(Dispatchers.IO) { impulseDAO.nextPendingDueAt(now) }

    /** 已经做完但还没回写记忆的行。 */
    suspend fun doneWithoutMemory(limit: Int = 3): List<ImpulseView> = withContext(Dispatchers.IO) {
        impulseDAO.doneWithoutMemory(limit).map { it.toView() }
    }

    /** 按状态数数。 */
    suspend fun countByStatus(status: String): Int = withContext(Dispatchers.IO) {
        impulseDAO.countByStatus(status)
    }

    /** 最近若干行，新的在前。 */
    suspend fun recent(limit: Int = 20): List<ImpulseView> = withContext(Dispatchers.IO) {
        impulseDAO.recent(limit).map { it.toView() }
    }

    private fun ImpulseEntity.toView() = ImpulseView(
        id = id,
        kind = kind,
        status = status,
        source = source,
        dedupeKey = dedupeKey,
        payloadJson = payloadJson,
        createdAt = createdAt,
        dueAt = dueAt,
        attempts = attempts,
        maxAttempts = maxAttempts,
        retryAt = retryAt,
        leaseExpiresAt = leaseExpiresAt,
        lastError = lastError,
        note = note,
        resultMemoryId = resultMemoryId,
        finishedAt = finishedAt,
    )

    private companion object {
        const val TAG = "ImpulseQueueService"

        /** `note` 落库前截断，免得一段模型输出把行撑爆。 */
        const val NOTE_MAX_CHARS = 800
    }

    /**
     * 包一层 [Log]：JVM 单测里 `android.util.Log` 没有被 mock，直接调会抛
     * `RuntimeException: Method i in android.util.Log not mocked`，
     * 把一次正常的恢复流程变成测试失败。跟 `WorkflowActionRunner.logSafe` 同一个理由。
     */
    private fun logSafe(message: String) {
        runCatching { Log.i(TAG, message) }
    }
}
