/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 冲动队列的一行。
 *
 * 对应 Elektron 的 `data/dynamic/impulse-queue.json`（见 `behavior/impulse_queue.py`）。
 * 那边是一个带 `fcntl.flock` 的 JSON 文件，靠"整文件重写 + 原子替换"保证一致；
 * 这边换成 Room —— 拿到的是事务、进程重启后的持久化、以及"重启后 pending 不会丢"。
 *
 * ## 这个队列存在的理由
 *
 * 情绪只有**唤醒权**，没有**行动决策权**。所以状态变化不能直接变成动作，
 * 只能变成一条中性的唤醒记录排进队列；至于要不要真的做什么、由谁来唤醒，
 * 是队列之外的事（现有的 `ProactiveMessageTriggerService`）。
 *
 * ## 状态机
 *
 * ```
 *                 ┌──────────────────────────────────────────┐
 *                 │                                          │
 *   enqueue ──▶ pending ──claim──▶ claimed ──finish──▶ done / ignored / failed
 *                 ▲                  │                     │
 *                 │                  │ lease 到期          │
 *                 └──────────────────┘                     │
 *                 │                                        │
 *                 └── deferred ──retry_at 到期─────────────┘
 * ```
 *
 * - `pending` —— 排着，等 `due_at` 到点。
 * - `claimed` —— 被某个执行者领走了，带着 [leaseExpiresAt]。**租约到期自动退回 pending**，
 *   这就是崩溃恢复：进程被杀时 `claimed` 行不会永远卡住。
 * - `deferred` —— 被节流/条件不满足，[retryAt] 到点后由恢复流程放回 pending。
 * - `done` / `ignored` / `failed` —— 终态。[ignored] 是"Agent 自己决定不做"，
 *   这是**合法结果**而不是失败（`behavior.py` 的原则：情绪不指定行动）。
 *
 * ## 为什么没有唯一索引
 *
 * "同一个 dedupe_key 同时只能有一条活跃的"这条约束**没有**做成唯一索引：
 * 终态行也要留着（`closed_loop.py` 要按 `status == done and result_memory_id is null`
 * 捞出来回写记忆），唯一索引会让"处理完再排一条"直接插入失败。
 * 所以 [dedupeKey] 上只有普通索引，唯一性由 `ImpulseQueueService.enqueue` 在事务里查一遍保证。
 *
 * 另外 Room 的 schema 校验只认 `@Entity(indices = ...)` 里声明过的索引，
 * 迁移里多建一个没声明的索引会让运行时校验判定 schema 漂移 —— 所以这里
 * **声明的索引与迁移 DDL 必须一一对应**。
 */
@Entity(
    tableName = "impulse_queue",
    indices = [
        // 主查询：取到点的 pending
        Index(value = ["status", "due_at"], name = "index_impulse_queue_status_due_at"),
        // 去重查询：同一 kind + dedupe_key 有没有活跃行
        Index(value = ["kind", "dedupe_key"], name = "index_impulse_queue_kind_dedupe_key"),
        // 关闭循环的扫描：done 且还没回写记忆的行
        Index(value = ["status", "finished_at"], name = "index_impulse_queue_status_finished_at"),
    ],
)
data class ImpulseEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo("id")
    val id: Long = 0L,

    /** 入队时刻（毫秒）。 */
    @ColumnInfo("created_at")
    val createdAt: Long,

    /**
     * 到点时刻（毫秒）。`due_at > now` 的行不会被认领。
     *
     * 入队时不取 `now` 而是留一个提前量（见 `ImpulseQueueService.MIN_LEAD_MILLIS`）：
     * 用户刚说完话就立刻插一条主动消息，观感是抢话。
     */
    @ColumnInfo("due_at")
    val dueAt: Long,

    /** 队列种类。当前只有 `emotion_wake`。 */
    @ColumnInfo("kind")
    val kind: String,

    /** 见类注释的状态机。 */
    @ColumnInfo("status")
    val status: String,

    /** 这条冲动是谁产生的（`drive_crossing` 等），用于事后分辨来源。 */
    @ColumnInfo("source")
    val source: String,

    /**
     * 去重键。
     *
     * 同一个 key 在活跃状态下只允许一条。注意它**不含时间** ——
     * "某维越过线"这件事在它落回线下之前都算同一件事，重复入队只会得到噪音。
     */
    @ColumnInfo("dedupe_key")
    val dedupeKey: String,

    /**
     * 中性唤醒载荷（JSON）。
     *
     * 里面只有"哪个维越过了哪条线、当前值多少、参照的基线是什么"，
     * **没有任何动作映射**。谁看它、要不要行动，由醒来的 Agent 决定。
     */
    @ColumnInfo("payload_json")
    val payloadJson: String,

    /** 这条唤醒是给哪个助手的。空表示"当前助手"。 */
    @ColumnInfo("assistant_id")
    val assistantId: String? = null,

    /** 被认领的时刻。未认领为 0。 */
    @ColumnInfo("claimed_at")
    val claimedAt: Long = 0L,

    /** 认领者标识。用来分辨"这条租约是不是我自己的"。 */
    @ColumnInfo("lease_owner")
    val leaseOwner: String? = null,

    /** 租约到期时刻。到期后由恢复流程退回 pending。0 表示无租约。 */
    @ColumnInfo("lease_expires_at")
    val leaseExpiresAt: Long = 0L,

    /** 已经尝试执行过几次。超过 [maxAttempts] 直接判 failed，不再无限重试。 */
    @ColumnInfo("attempts")
    val attempts: Int = 0,

    /** 重试上限。 */
    @ColumnInfo("max_attempts")
    val maxAttempts: Int = 3,

    /** `deferred` 的下次可认领时刻。0 表示没有排期。 */
    @ColumnInfo("retry_at")
    val retryAt: Long = 0L,

    /** 最近一次失败/延后的原因，回给下次唤醒的上下文用。 */
    @ColumnInfo("last_error")
    val lastError: String? = null,

    /** 执行者留下的短说明（比如 Agent 回 [PASS] 时的理由摘要）。 */
    @ColumnInfo("note")
    val note: String? = null,

    /**
     * 这次行为回写成的记忆 id。
     *
     * `closed_loop.py` 用它做幂等：只处理 `status == done and result_memory_id is null`
     * 的行，回写完就 attach 上来，于是同一条冲动不会被回写两次。
     */
    @ColumnInfo("result_memory_id")
    val resultMemoryId: Int? = null,

    /** 进入终态的时刻（毫秒）。0 表示还没结束。 */
    @ColumnInfo("finished_at")
    val finishedAt: Long = 0L,

    /** 载荷 schema 版本，方便以后改结构时分辨老行。 */
    @ColumnInfo("schema_version")
    val schemaVersion: String,
)
