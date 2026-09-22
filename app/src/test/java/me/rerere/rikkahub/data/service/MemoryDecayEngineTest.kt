/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [MemoryDecayEngine] 的公式回归测试。
 *
 * 用例锁的是 Elektron `decay_engine.py` 里几条容易被"顺手优化"掉的语义：
 * 钉选桶不衰减、结案加速淡化、长期靠情绪权重、时间字段缺失的兜底方向。
 *
 * `days = 0` 的用例用**手算的绝对值**断言 —— 那一点上 `exp(0) = 1`、`1^0.3 = 1`，
 * 没有超越函数误差，能把公式钉死；其余用例用关系断言。
 */
class MemoryDecayEngineTest {

    private companion object {
        const val NOW = 1_700_000_000_000L
        const val DAY = 86_400_000L
        const val EPS = 1e-6

        /**
         * days = 0 / importance = 5 / activationCount = 1 时的 combined_weight。
         *
         * 短期段（≤3 天）：`freshness×0.7 + emotion×0.3`，其中 freshness(0) = 2.0。
         */
        fun combinedAtDayZero(arousal: Double): Double {
            val emotion = MemoryDecayEngine.EMOTION_BASE + arousal * MemoryDecayEngine.AROUSAL_BOOST
            return 2.0 * MemoryDecayEngine.SHORT_TERM_TIME_SHARE +
                emotion * (1.0 - MemoryDecayEngine.SHORT_TERM_TIME_SHARE)
        }
    }

    private fun input(
        type: String = "manual",
        importance: Int = 5,
        activationCount: Int = 1,
        lastActiveAt: Long = NOW,
        arousal: Float = 0.3f,
        resolved: Boolean = false,
        digested: Boolean = false,
        pinned: Boolean = false,
        isProtected: Boolean = false,
        now: Long = NOW,
    ) = DecayInput(
        type = type,
        importance = importance,
        activationCount = activationCount,
        lastActiveAt = lastActiveAt,
        arousal = arousal,
        resolved = resolved,
        digested = digested,
        pinned = pinned,
        isProtected = isProtected,
        now = now,
    )

    // ------------------------------------------------------------------
    // 永不衰减
    // ------------------------------------------------------------------

    @Test
    fun `pinned and protected and permanent never decay`() {
        val expected = MemoryDecayEngine.NEVER_DECAY_SCORE
        assertEquals(expected, MemoryDecayEngine.score(input(pinned = true, lastActiveAt = NOW - 400 * DAY)), EPS)
        assertEquals(expected, MemoryDecayEngine.score(input(isProtected = true, lastActiveAt = NOW - 400 * DAY)), EPS)
        assertEquals(
            expected,
            MemoryDecayEngine.score(input(type = MemoryDecayEngine.TYPE_PERMANENT, lastActiveAt = NOW - 400 * DAY)),
            EPS,
        )
    }

    // ------------------------------------------------------------------
    // 手算绝对值：把公式钉死
    // ------------------------------------------------------------------

    @Test
    fun `day zero score matches the hand computed value`() {
        // combined = 2.0×0.7 + 1.24×0.3 = 1.772，urgency 不触发
        val expected = 5.0 * combinedAtDayZero(0.3)
        assertEquals(8.86, expected, EPS)
        assertEquals(expected, MemoryDecayEngine.score(input(arousal = 0.3f)), EPS)
    }

    @Test
    fun `arousal above the gate multiplies by one and a half`() {
        val base = 5.0 * combinedAtDayZero(0.8)
        assertEquals(14.19, base * MemoryDecayEngine.URGENCY_BOOST, EPS)
        assertEquals(base * MemoryDecayEngine.URGENCY_BOOST, MemoryDecayEngine.score(input(arousal = 0.8f)), EPS)
    }

    @Test
    fun `the urgency gate is strictly greater than`() {
        // 0.7 不满足 `arousal > 0.7`：相邻两个 arousal 之间不应出现 1.5 倍的跳变
        val atGate = MemoryDecayEngine.score(input(arousal = 0.7f))
        val justBelow = MemoryDecayEngine.score(input(arousal = 0.69f))
        assertTrue("0.70 must not jump over 0.69, ratio=${atGate / justBelow}", atGate / justBelow < 1.01)

        // 0.71 越过门：必须出现那一档
        val justAbove = MemoryDecayEngine.score(input(arousal = 0.71f))
        assertTrue("0.71 must jump over 0.70, ratio=${justAbove / atGate}", justAbove / atGate > 1.4)
    }

    @Test
    fun `resolved without urgency matches the hand computed value`() {
        val base = 5.0 * combinedAtDayZero(0.8)
        assertEquals(base * MemoryDecayEngine.RESOLVED_FACTOR, MemoryDecayEngine.score(input(arousal = 0.8f, resolved = true)), EPS)
        assertEquals(
            base * MemoryDecayEngine.RESOLVED_DIGESTED_FACTOR,
            MemoryDecayEngine.score(input(arousal = 0.8f, resolved = true, digested = true)),
            EPS,
        )
    }

    @Test
    fun `resolved fades harder when a feel has also been written`() {
        val resolved = MemoryDecayEngine.score(input(resolved = true))
        val digested = MemoryDecayEngine.score(input(resolved = true, digested = true))
        val untouched = MemoryDecayEngine.score(input(resolved = false))

        assertTrue("digested must sit below plain resolved", digested < resolved)
        assertTrue("resolved must sit below untouched", resolved < untouched)
        assertEquals(untouched * MemoryDecayEngine.RESOLVED_FACTOR, resolved, EPS)
        assertEquals(untouched * MemoryDecayEngine.RESOLVED_DIGESTED_FACTOR, digested, EPS)
    }

    // ------------------------------------------------------------------
    // 短期 vs 长期
    // ------------------------------------------------------------------

    @Test
    fun `a fresh memory outranks a stale one`() {
        val fresh = MemoryDecayEngine.score(input(lastActiveAt = NOW))
        val stale = MemoryDecayEngine.score(input(lastActiveAt = NOW - 30 * DAY))
        assertTrue("fresh=$fresh should beat stale=$stale", fresh > stale)
    }

    @Test
    fun `score decreases monotonically as the memory ages`() {
        var previous = Double.MAX_VALUE
        for (days in 0..60 step 5) {
            val score = MemoryDecayEngine.score(input(lastActiveAt = NOW - days * DAY))
            assertTrue("score at day $days ($score) must sit below the previous day's ($previous)", score < previous)
            previous = score
        }
    }

    @Test
    fun `long term memories lean on emotion more than short term ones`() {
        val shortHigh = MemoryDecayEngine.score(input(arousal = 1.0f, lastActiveAt = NOW))
        val shortLow = MemoryDecayEngine.score(input(arousal = 0.0f, lastActiveAt = NOW))
        val longHigh = MemoryDecayEngine.score(input(arousal = 1.0f, lastActiveAt = NOW - 30 * DAY))
        val longLow = MemoryDecayEngine.score(input(arousal = 0.0f, lastActiveAt = NOW - 30 * DAY))

        val shortSpread = shortHigh / shortLow
        val longSpread = longHigh / longLow
        assertTrue(
            "long-term spread ($longSpread) should exceed short-term spread ($shortSpread)",
            longSpread > shortSpread,
        )
    }

    @Test
    fun `freshness weight starts at two and settles to one`() {
        assertEquals(2.0, MemoryDecayEngine.freshnessWeight(0.0), EPS)
        assertEquals(1.0, MemoryDecayEngine.freshnessWeight(1000.0), 1e-6)
    }

    // ------------------------------------------------------------------
    // 时间字段的兜底方向
    // ------------------------------------------------------------------

    @Test
    fun `missing last active falls back to thirty days not to now`() {
        assertEquals(MemoryDecayEngine.FALLBACK_DAYS, MemoryDecayEngine.daysSince(0L, NOW), EPS)
        // 兜底成"很久没动"：得分必须低于刚写入的记忆。
        // 反过来兜底成 0 的话，一批几十天前的旧记忆时间分会集体拉满，
        // 正是 PITFALLS 里「搬家之后所有旧记忆的排序全乱了」那次的根因。
        val fallback = MemoryDecayEngine.score(input(lastActiveAt = 0L))
        val brandNew = MemoryDecayEngine.score(input(lastActiveAt = NOW))
        assertTrue("fallback=$fallback must not outrank brandNew=$brandNew", fallback < brandNew)
    }

    @Test
    fun `a future timestamp clamps to zero days`() {
        assertEquals(0.0, MemoryDecayEngine.daysSince(NOW + 10 * DAY, NOW), EPS)
    }

    // ------------------------------------------------------------------
    // 其余因子
    // ------------------------------------------------------------------

    @Test
    fun `importance and activation count both raise the score`() {
        val low = MemoryDecayEngine.score(input(importance = 1, activationCount = 1))
        val important = MemoryDecayEngine.score(input(importance = 10, activationCount = 1))
        val recalled = MemoryDecayEngine.score(input(importance = 1, activationCount = 20))

        assertTrue(important > low)
        assertTrue(recalled > low)
    }

    @Test
    fun `threshold stays a reference line and is not an archive trigger`() {
        assertTrue(MemoryDecayEngine.isBelowThreshold(0.2))
        assertFalse(MemoryDecayEngine.isBelowThreshold(0.4))
        // 低于阈值的记忆仍然只是一个数字 —— 引擎不提供任何"该归档了"的出口
        val stale = MemoryDecayEngine.score(input(lastActiveAt = NOW - 3650 * DAY, importance = 1))
        assertTrue(MemoryDecayEngine.isBelowThreshold(stale))
    }
}
