/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.service

import kotlinx.serialization.json.jsonObject
import me.rerere.rikkahub.utils.JsonInstant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Behavior 层内核的回归测试。
 *
 * ## 覆盖范围
 *
 * 1. [ImpulsePolicy] 的状态机边界 —— 租约过期、延后到点、可认领、重试预算、延迟收敛；
 * 2. [EmotionWakePayload] 的编解码与「中性」这个不变量；
 * 3. [EmotionWakeBridge] 与 Elektron `behavior.py` 逐条对齐的常量；
 * 4. [DriveEngine] 的行为回写因子（`satisfy` / `refuseIntent`）。
 *
 * ## 明确没覆盖的
 *
 * **Room 侧全部没测** —— [ImpulseQueueService] 的落库、租约恢复、去重插入都需要一个
 * 内存数据库，而单测环境里没有 Robolectric（`app/build.gradle.kts` 只有 `junit`）。
 * 那部分由两道别的门锁着：DDL 与实体的逐列一致性在 `audit/preflight.py` 里查，
 * 运行期由 Room 的 `validateMigration` 在打开库时抛 `IllegalStateException` 兜底。
 * 报告里不要把这份测试说成「验证了队列的持久化」。
 *
 * ## 为什么值得写
 *
 * 这些边界条件（`lease_until <= now` 还是 `< now`、延后下界是 300 还是 3600、
 * `failed` 算不算终态）是最容易写错、也最难在真机上观察的一段。它们在 CI 里
 * 是硬门槛（`Debug FX Build` 会跑 `:app:testDebugUnitTest`，挂了就挂构建）。
 */
class ImpulsePolicyTest {

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val SECOND = 1_000L
        const val MINUTE = 60_000L
        const val HOUR = 3_600_000L

        val ALL_STATUSES = listOf(
            ImpulsePolicy.STATUS_PENDING,
            ImpulsePolicy.STATUS_CLAIMED,
            ImpulsePolicy.STATUS_DEFERRED,
            ImpulsePolicy.STATUS_DONE,
            ImpulsePolicy.STATUS_FAILED,
            ImpulsePolicy.STATUS_IGNORED,
        )

        fun samplePayload() = EmotionWakePayload(
            value = 0.72,
            desc = EmotionWakePayload.DESC,
            signals = listOf(
                EmotionWakeSignal(
                    id = "absolute:curiosity",
                    drive = "curiosity",
                    trigger = DriveBaseline.TRIGGER_ABSOLUTE,
                    value = 0.72,
                    threshold = 0.50,
                ),
                EmotionWakeSignal(
                    id = "relative:social",
                    drive = "social",
                    trigger = DriveBaseline.TRIGGER_RELATIVE,
                    value = 0.61,
                    threshold = DriveBaseline.RELATIVE_RISE,
                    baseline = 0.25,
                    rise = 0.36,
                ),
            ),
            emotionSignature = mapOf("absolute:curiosity" to 0.72),
            emotionSnapshot = DriveEngine.DRIVE_BASELINES,
            dominant = "curiosity",
            baselineKind = "same_time_yesterday",
            baselineAgeHours = 24.0,
            bornAt = NOW,
        )
    }

    // ------------------------------------------------------------------
    // 状态集合
    // ------------------------------------------------------------------

    @Test
    fun `active and terminal statuses partition the six statuses`() {
        assertEquals(
            "活跃态与终态必须刚好把六个状态切完，多一个少一个都说明状态机有洞",
            ALL_STATUSES.toSet(),
            ImpulsePolicy.ACTIVE_STATUSES + ImpulsePolicy.TERMINAL_STATUSES,
        )
        assertTrue(
            "活跃态与终态不能重叠",
            (ImpulsePolicy.ACTIVE_STATUSES intersect ImpulsePolicy.TERMINAL_STATUSES).isEmpty(),
        )
    }

    @Test
    fun `deferred is active so a deferred wake still blocks a new one`() {
        // 这是"防重复触发"的关键：延后的那条还没处理完，队列里就不能再排新的。
        // 如果 deferred 被划进终态，去重闸就漏了，同一条牵引会被反复排队。
        assertTrue(ImpulsePolicy.isActive(ImpulsePolicy.STATUS_DEFERRED))
        assertFalse(ImpulsePolicy.isTerminal(ImpulsePolicy.STATUS_DEFERRED))
    }

    @Test
    fun `failed is terminal so it never blocks a new wake and never gets reclaimed`() {
        // 与 Elektron 的差异点，值得钉住：那边 `FINAL = {done, ignored}`，`failed` 不在里面，
        // 于是 `failed` 行永远不会被 prune 掉。这边把它归入终态（7 天后清理），
        // 语义上仍然是"不再被 claim、但会让桥进入失败冷却"。
        assertTrue(ImpulsePolicy.isTerminal(ImpulsePolicy.STATUS_FAILED))
        assertFalse(ImpulsePolicy.isActive(ImpulsePolicy.STATUS_FAILED))
    }

    @Test
    fun `finish statuses accept every terminal state plus deferred`() {
        ImpulsePolicy.TERMINAL_STATUSES.forEach { status ->
            assertTrue("$status 是合法终态", status in ImpulsePolicy.FINISH_STATUSES)
        }
        assertTrue(ImpulsePolicy.STATUS_DEFERRED in ImpulsePolicy.FINISH_STATUSES)
        assertFalse(
            "pending / claimed 不能被 finish —— 它们还得被认领一次",
            ImpulsePolicy.STATUS_PENDING in ImpulsePolicy.FINISH_STATUSES ||
                ImpulsePolicy.STATUS_CLAIMED in ImpulsePolicy.FINISH_STATUSES,
        )
    }

    // ------------------------------------------------------------------
    // 租约 / 延后 / 认领
    // ------------------------------------------------------------------

    @Test
    fun `lease expiry only applies to claimed rows with a live lease`() {
        // 到点即算过期（<=，不是 <）：租约是"最多持有这么久"，边界上应当归还。
        assertTrue(
            ImpulsePolicy.leaseExpired(ImpulsePolicy.STATUS_CLAIMED, NOW, NOW),
        )
        assertTrue(
            ImpulsePolicy.leaseExpired(ImpulsePolicy.STATUS_CLAIMED, NOW - 1, NOW),
        )
        assertFalse(
            "租约还没到期就不能被抢",
            ImpulsePolicy.leaseExpired(ImpulsePolicy.STATUS_CLAIMED, NOW + 1, NOW),
        )
        assertFalse(
            "没有租约（0）不算过期 —— 那是还没被认领过",
            ImpulsePolicy.leaseExpired(ImpulsePolicy.STATUS_CLAIMED, 0L, NOW),
        )
        assertFalse(
            "只有 claimed 会被租约回收，pending 本来就在队列里",
            ImpulsePolicy.leaseExpired(ImpulsePolicy.STATUS_PENDING, NOW - 1, NOW),
        )
    }

    @Test
    fun `deferred rows become ready only after retry_at`() {
        assertTrue(ImpulsePolicy.deferredReady(ImpulsePolicy.STATUS_DEFERRED, NOW, NOW))
        assertTrue(ImpulsePolicy.deferredReady(ImpulsePolicy.STATUS_DEFERRED, NOW - 1, NOW))
        assertFalse(ImpulsePolicy.deferredReady(ImpulsePolicy.STATUS_DEFERRED, NOW + 1, NOW))
        assertFalse(
            "retry_at 为 0 说明还没排过重试时刻，不能立刻放回",
            ImpulsePolicy.deferredReady(ImpulsePolicy.STATUS_DEFERRED, 0L, NOW),
        )
        assertFalse(
            "非 deferred 状态不看 retry_at",
            ImpulsePolicy.deferredReady(ImpulsePolicy.STATUS_PENDING, NOW - 1, NOW),
        )
    }

    @Test
    fun `claimable requires pending and a due timestamp in the past`() {
        assertTrue(ImpulsePolicy.claimable(ImpulsePolicy.STATUS_PENDING, NOW, NOW))
        assertFalse(
            "还没到点就不能认领 —— 这是入队到唤醒之间的最小提前量",
            ImpulsePolicy.claimable(ImpulsePolicy.STATUS_PENDING, NOW + 1, NOW),
        )
        assertFalse(
            "已认领的不能被第二个人再认领",
            ImpulsePolicy.claimable(ImpulsePolicy.STATUS_CLAIMED, NOW - 1, NOW),
        )
        assertFalse(
            "延后的要等 retry_at 把它放回 pending，不能直接认领",
            ImpulsePolicy.claimable(ImpulsePolicy.STATUS_DEFERRED, NOW - 1, NOW),
        )
    }

    @Test
    fun `retry budget is exhausted at max attempts`() {
        assertTrue(ImpulsePolicy.canRetry(attempts = 0, maxAttempts = 3))
        assertTrue(ImpulsePolicy.canRetry(attempts = 2, maxAttempts = 3))
        assertFalse(
            "用满就不再重试 —— 这是「不会无限重试」的那道闸",
            ImpulsePolicy.canRetry(attempts = 3, maxAttempts = 3),
        )
        assertFalse(ImpulsePolicy.canRetry(attempts = 4, maxAttempts = 3))
    }

    // ------------------------------------------------------------------
    // 延迟收敛
    // ------------------------------------------------------------------

    @Test
    fun `retryAt enforces the five minute floor`() {
        // 下界存在的理由：上游可能算出 0 秒或负数（比如节流剩余时间刚好归零），
        // 不兜住的话延后等于没延后，会立刻再被认领一次。
        assertEquals(NOW + 300 * SECOND, ImpulsePolicy.retryAt(NOW, 0))
        assertEquals(NOW + 300 * SECOND, ImpulsePolicy.retryAt(NOW, 299))
        assertEquals(NOW + 300 * SECOND, ImpulsePolicy.retryAt(NOW, 300))
        assertEquals(NOW + 600 * SECOND, ImpulsePolicy.retryAt(NOW, 600))
    }

    @Test
    fun `clampRetrySeconds bounds the delay to the hour default`() {
        assertEquals(300L, ImpulsePolicy.clampRetrySeconds(0))
        assertEquals(300L, ImpulsePolicy.clampRetrySeconds(-100))
        assertEquals(1_200L, ImpulsePolicy.clampRetrySeconds(1_200))
        assertEquals(3_600L, ImpulsePolicy.clampRetrySeconds(3_600))
        assertEquals(
            "上界兜住，免得一条唤醒被推到遥遥无期",
            3_600L,
            ImpulsePolicy.clampRetrySeconds(86_400),
        )
    }

    @Test
    fun `schedule constants stay where the behaviour contract put them`() {
        assertEquals(
            "入队到到点至少留一分钟，避免在用户刚说完话的下一秒插话",
            60_000L,
            ImpulsePolicy.MIN_LEAD_MILLIS,
        )
        assertEquals(
            "租约十分钟：比一次带工具链的生成长，又不会让进程被杀后半天不恢复",
            10 * 60_000L,
            ImpulsePolicy.LEASE_MILLIS,
        )
        assertEquals(7 * 86_400_000L, ImpulsePolicy.TERMINAL_RETENTION_MILLIS)
        assertEquals(3, ImpulsePolicy.DEFAULT_MAX_ATTEMPTS)
        assertEquals("impulse_v1", ImpulsePolicy.SCHEMA_VERSION)
    }

    // ------------------------------------------------------------------
    // 载荷：编解码 + 「中性」这个不变量
    // ------------------------------------------------------------------

    @Test
    fun `payload survives an encode decode round trip`() {
        val original = samplePayload()
        val restored = EmotionWakePayload.decode(EmotionWakePayload.encode(original))

        assertNotNull("自己编码的载荷必须能自己解回来", restored)
        assertEquals(original, restored)
    }

    @Test
    fun `decode rejects blank and malformed payloads`() {
        assertNull(EmotionWakePayload.decode(null))
        assertNull(EmotionWakePayload.decode(""))
        assertNull(EmotionWakePayload.decode("   "))
        assertNull(
            "坏载荷返回 null，调用方退回常规路径；不能让解析异常冒到 onStartCommand",
            EmotionWakePayload.decode("{ this is not json"),
        )
    }

    @Test
    fun `payload carries state descriptions only, never an action`() {
        val keys = JsonInstant
            .parseToJsonElement(EmotionWakePayload.encode(samplePayload()))
            .jsonObject
            .keys

        // State 只描述状态，不做动作决策（任务书第二节第 1 条）。
        // 这条断言是结构性的：载荷里不允许出现任何"该做什么"的字段名。
        listOf(
            "action", "actions", "todo", "task", "should", "must",
            "suggestion", "suggest", "command", "instruction", "plan",
        ).forEach { banned ->
            assertFalse(
                "载荷里不该有 $banned 字段 —— 情绪只有唤醒权，没有行动决策权",
                keys.any { it.equals(banned, ignoreCase = true) },
            )
        }

        assertTrue(
            "该有的状态字段一个都不能少",
            keys.containsAll(
                listOf("kind", "drive", "value", "desc", "signals", "emotionSnapshot", "bornAt")
            ),
        )
        assertEquals(
            "唤醒不属于任何单一维度，固定标成 emotion",
            EmotionWakePayload.DRIVE_EMOTION,
            samplePayload().drive,
        )
    }

    @Test
    fun `every signal carries an observable value, not a directive`() {
        samplePayload().signals.forEach { signal ->
            assertTrue("信号必须带值", signal.value > 0.0)
            assertTrue("信号必须带判据（阈值）", signal.threshold > 0.0)
            assertTrue(
                "trigger 只能是两种越线方式之一，不允许出现别的东西",
                signal.trigger == DriveBaseline.TRIGGER_ABSOLUTE ||
                    signal.trigger == DriveBaseline.TRIGGER_RELATIVE,
            )
            assertTrue("信号 id 要能当闩锁的 key", signal.id.contains(signal.drive))
        }
    }

    // ------------------------------------------------------------------
    // 桥：与 Elektron behavior.py 的逐条对齐
    // ------------------------------------------------------------------

    @Test
    fun `bridge constants match the elektron behaviour script`() {
        assertEquals("emotion_wake", ImpulsePolicy.KIND_EMOTION_WAKE)
        assertEquals("emotion_wake", EmotionWakeBridge.DEDUPE_KEY)
        assertEquals("drive_crossing", EmotionWakeBridge.SOURCE_DRIVE_CROSSING)
        assertEquals(
            "behavior.py: ABSOLUTE_MIN_GAP_H = 0.5",
            0.5,
            EmotionWakeBridge.ABSOLUTE_MIN_GAP_HOURS,
            1e-9,
        )
        assertEquals(
            "behavior.py: if status == failed and age < 1",
            1.0,
            EmotionWakeBridge.FAILED_COOLDOWN_HOURS,
            1e-9,
        )
    }

    @Test
    fun `wake dedupe key is a constant, not per dimension`() {
        // behavior.py 的去重是 has_active_kind(WAKE_KIND) —— 只要队列里还有一条
        // 没处理完的 emotion_wake 就不再排新的。唤醒不是某一维的私事，
        // 所以去重键不能带上维度名。
        assertEquals("emotion_wake", EmotionWakeBridge.DEDUPE_KEY)
        DriveEngine.DRIVE_KEYS.forEach { drive ->
            assertFalse(
                "去重键不该被拆成按维度 —— 那会让四条牵引同时排队",
                EmotionWakeBridge.DEDUPE_KEY.contains(drive),
            )
        }
    }

    // ------------------------------------------------------------------
    // 行为回写因子（Closed Loop 的 State 侧）
    // ------------------------------------------------------------------

    private fun drivesWith(vararg pairs: Pair<String, Double>): Map<String, Double> {
        val out = LinkedHashMap(DriveEngine.DRIVE_BASELINES)
        pairs.forEach { (key, value) -> out[key] = value }
        return out
    }

    @Test
    fun `satisfy releases the primary drive harder than its neighbours`() {
        val before = drivesWith("attachment" to 1.0, "libido" to 1.0)
        val after = DriveEngine.satisfy(before, "attachment")

        // Elektron desire_engine.SATISFY_DECAY["attachment"] = {attachment: .60, libido: .80}
        assertEquals(0.60, after.getValue("attachment"), 1e-9)
        assertEquals(0.80, after.getValue("libido"), 1e-9)
        assertTrue(
            "主维必须掉得比邻维多，否则「释放」没有主次",
            after.getValue("attachment") < after.getValue("libido"),
        )
    }

    @Test
    fun `satisfy covers all nine dimensions with the elektron table`() {
        // 表里少一维就会静默退化成 SATISFY_DEFAULT_FACTOR，很难在真机上发现。
        DriveEngine.DRIVE_KEYS.forEach { drive ->
            val table = DriveEngine.SATISFY_DECAY[drive]
            assertNotNull("$drive 必须在 SATISFY_DECAY 里登记", table)
            val decay = requireNotNull(table)
            assertTrue("$drive 的回落表必须包含它自己", decay.containsKey(drive))
            assertTrue("$drive 的系数必须在 (0,1) 里", decay.values.all { it > 0.0 && it < 1.0 })
        }
        assertEquals(9, DriveEngine.SATISFY_DECAY.size)
    }

    @Test
    fun `satisfy leaves unrelated dimensions alone`() {
        val before = drivesWith("curiosity" to 0.80, "stress" to 0.70, "social" to 0.60)
        val after = DriveEngine.satisfy(before, "curiosity")

        // curiosity 的回落表是 {curiosity: .65, reflection: .90}
        assertEquals(0.80 * 0.65, after.getValue("curiosity"), 1e-9)
        // reflection 没被指定，起点是它的静息基线 0.18（drivesWith 从基线铺底）
        assertEquals(
            0.18 * 0.90,
            after.getValue("reflection"),
            1e-9,
        )
        assertEquals("stress 不在 curiosity 的回落表里", 0.70, after.getValue("stress"), 1e-9)
        assertEquals("social 不在 curiosity 的回落表里", 0.60, after.getValue("social"), 1e-9)
    }

    @Test
    fun `satisfy with an unknown key changes nothing`() {
        val before = drivesWith("stress" to 0.70)
        val after = DriveEngine.satisfy(before, "not_a_drive")

        // normalizeDriveKey 认不出 → 返回 null → 整份 drives 原样返回。
        // 这就是 resolveBehavior 里 driveKey 传 null 时"只记账本、不动 drives"的底座。
        assertEquals(before, after)
    }

    @Test
    fun `refuseIntent only presses the target drive`() {
        val before = drivesWith("stress" to 0.80, "fatigue" to 0.40)
        val after = DriveEngine.refuseIntent(before, "stress")

        // Elektron refuse_intent: ×0.75，且只压目标维 —— 拒绝代表这条牵引不合当下，
        // 不该波及其他维。
        assertEquals(0.80 * 0.75, after.getValue("stress"), 1e-9)
        assertEquals(
            "拒绝不该动 fatigue —— 它不在满足表里，更不该被拒绝牵连",
            0.40,
            after.getValue("fatigue"),
            1e-9,
        )
    }

    @Test
    fun `refusing is gentler than satisfying`() {
        // 两种结局都合法，但"做了"释放得比"决定不做"更多。
        val satisfied = DriveEngine.satisfy(drivesWith("stress" to 1.0), "stress")
        val refused = DriveEngine.refuseIntent(drivesWith("stress" to 1.0), "stress")
        val satisfiedStress = satisfied.getValue("stress")
        val refusedStress = refused.getValue("stress")

        assertTrue(
            "refuse($refusedStress) 必须比 satisfy($satisfiedStress) 掉得少",
            refusedStress > satisfiedStress,
        )
        assertEquals(0.60, satisfiedStress, 1e-9)
        assertEquals(0.75, refusedStress, 1e-9)
    }

    @Test
    fun `writeback never pushes a drive out of range`() {
        // 回落是乘性的，不会溢出；但入参可能已经是脏值（>1 或 <0），
        // 出口必须仍然落在 [0,1] —— 否则下一次 tick 会拿脏值继续算。
        val dirty = mapOf("stress" to 4.0, "fatigue" to -2.0)
        DriveEngine.satisfy(dirty, "stress").forEach { (key, value) ->
            assertTrue("$key 出界了：$value", value >= 0.0 && value <= 1.0)
        }
        DriveEngine.refuseIntent(dirty, "stress").forEach { (key, value) ->
            assertTrue("$key 出界了：$value", value >= 0.0 && value <= 1.0)
        }
    }

    @Test
    fun `refuseIntent with an unknown key changes nothing`() {
        val before = drivesWith("stress" to 0.70)
        assertEquals(before, DriveEngine.refuseIntent(before, "not_a_drive"))
    }
}
