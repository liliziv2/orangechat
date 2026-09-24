package me.rerere.rikkahub.data.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

/**
 * State 层内核的回归测试。
 *
 * 覆盖三块：
 * 1. [DriveEngine] 的公式与不变量（归一、脉冲、互抑、逃逸阀、时间推进、事件折算）；
 * 2. [DriveBaseline] 的基线挑选与越线判定；
 * 3. [DialogueEventSource] 的对话事件折算。
 *
 * 断言策略沿用 [MemoryDecayEngineTest] 的做法：能手算绝对值的点用手算值钉死
 * （`sqrt`、`pow` 在整数/半数点上是精确的），其余用关系断言（单调性、方向、上下界），
 * 避免浮点运算顺序依赖。
 */
class DriveEngineTest {

    private companion object {
        const val EPS = 1e-6
        const val HOUR = 3_600_000L
        const val NOW = 1_700_000_000_000L

        /** 全基线的 9 维。 */
        fun baselineDrives(): Map<String, Double> = DriveEngine.DRIVE_BASELINES
    }

    private fun drivesWith(vararg pairs: Pair<String, Double>): Map<String, Double> {
        val out = LinkedHashMap(DriveEngine.DRIVE_BASELINES)
        pairs.forEach { (key, value) -> out[key] = value }
        return out
    }

    // ------------------------------------------------------------------
    // 归一与基础工具
    // ------------------------------------------------------------------

    @Test
    fun `normalizeDriveValues fills missing dimensions from baseline`() {
        val normalized = DriveEngine.normalizeDriveValues(mapOf("attachment" to 0.9))
        assertEquals(DriveEngine.DRIVE_KEYS.size, normalized.size)
        assertEquals(0.9, normalized.getValue("attachment"), EPS)
        // 没给到的维度回落到基线，不是 0
        assertEquals(
            DriveEngine.DRIVE_BASELINES.getValue("social"),
            normalized.getValue("social"),
            EPS,
        )
    }

    @Test
    fun `normalizeDriveValues drops unknown keys and clamps out of range`() {
        val normalized = DriveEngine.normalizeDriveValues(
            mapOf("attachment" to 1.8, "not_a_drive" to 0.5, "stress" to -0.3),
        )
        assertEquals(1.0, normalized.getValue("attachment"), EPS)
        assertEquals(0.0, normalized.getValue("stress"), EPS)
        assertFalse(normalized.containsKey("not_a_drive"))
    }

    @Test
    fun `normalizeDriveKey resolves the legacy duty alias`() {
        assertEquals("stewardship", DriveEngine.normalizeDriveKey("duty"))
        assertEquals("stewardship", DriveEngine.normalizeDriveKey("DUTY"))
        assertEquals("attachment", DriveEngine.normalizeDriveKey("Attachment"))
        assertNull(DriveEngine.normalizeDriveKey("nope"))
    }

    // ------------------------------------------------------------------
    // 每维独立疲劳
    // ------------------------------------------------------------------

    @Test
    fun `local fatigue is global fatigue scaled per dimension sensitivity`() {
        val local = DriveEngine.computeLocalFatigue(0.10)
        // attachment 敏感度 0.12 → 0.012；social 0.78 → 0.078；fatigue 自己 0.0
        assertEquals(0.012, local.getValue("attachment"), EPS)
        assertEquals(0.078, local.getValue("social"), EPS)
        assertEquals(0.0, local.getValue("fatigue"), EPS)
        assertEquals(DriveEngine.DRIVE_KEYS.size, local.size)
    }

    @Test
    fun `local fatigue is zero when global fatigue is zero`() {
        val local = DriveEngine.computeLocalFatigue(0.0)
        DriveEngine.DRIVE_KEYS.forEach { key ->
            assertEquals(0.0, local.getValue(key), EPS)
        }
    }

    // ------------------------------------------------------------------
    // 脉冲
    // ------------------------------------------------------------------

    @Test
    fun `pulse gain shrinks as the drive approaches saturation`() {
        // current = 0 → 全量
        assertEquals(0.18, DriveEngine.pulseGain(0.0, 0.18), EPS)
        // current = 0.75 → sqrt(0.25) = 0.5 倍
        assertEquals(0.18 * 0.5, DriveEngine.pulseGain(0.75, 0.18), EPS)
        // current = 1 → 打不动
        assertEquals(0.0, DriveEngine.pulseGain(1.0, 0.18), EPS)
    }

    @Test
    fun `pulse drive never exceeds one and never moves other dimensions`() {
        val before = baselineDrives()
        val after = DriveEngine.pulseDrive(before, "social", 0.5)
        assertEquals(0.25 + 0.5 * sqrt(0.75), after.getValue("social"), EPS)
        DriveEngine.DRIVE_KEYS.filter { it != "social" }.forEach { key ->
            assertEquals(before.getValue(key), after.getValue(key), EPS)
        }
    }

    @Test
    fun `attachment jumps to the upper basin when crossing the threshold`() {
        // 0.60 + 0.22*sqrt(0.4) = 0.7391… ≥ 0.68 → 跳到 0.82
        val jumped = DriveEngine.pulseAttachmentNonlinear(drivesWith("attachment" to 0.60), 0.22)
        assertEquals(DriveEngine.ATTACHMENT_BASIN_JUMP, jumped.getValue("attachment"), EPS)

        // 已经在盆地上方：不重复跳，走普通脉冲
        val already = DriveEngine.pulseAttachmentNonlinear(drivesWith("attachment" to 0.70), 0.22)
        assertEquals(0.70 + 0.22 * sqrt(0.30), already.getValue("attachment"), EPS)
    }

    // ------------------------------------------------------------------
    // 双层归一
    // ------------------------------------------------------------------

    @Test
    fun `drive activation is zero at baseline and one at saturation`() {
        assertEquals(0.0, DriveEngine.driveActivation("attachment", 0.30), EPS)
        assertEquals(1.0, DriveEngine.driveActivation("attachment", 1.0), EPS)
        // 半程余量 → sqrt(0.5)
        assertEquals(sqrt(0.5), DriveEngine.driveActivation("attachment", 0.65), EPS)
    }

    @Test
    fun `fatigue suppresses activation but not the resting baseline`() {
        // 静息值本身激活为 0，疲劳再高也还是 0（不会变成负数）
        assertEquals(0.0, DriveEngine.effectiveDriveActivation("attachment", 0.30, 1.0), EPS)
        // 半程余量 + 半程疲劳 → sqrt(0.5) × 0.5
        assertEquals(
            sqrt(0.5) * 0.5,
            DriveEngine.effectiveDriveActivation("attachment", 0.65, 0.5),
            EPS,
        )
    }

    @Test
    fun `activation snapshot covers all nine dimensions`() {
        val snapshot = DriveEngine.activationSnapshot(baselineDrives(), DriveEngine.computeLocalFatigue(0.1))
        assertEquals(DriveEngine.DRIVE_KEYS.size, snapshot.size)
        // 全基线 → 全 0
        DriveEngine.DRIVE_KEYS.forEach { key -> assertEquals(0.0, snapshot.getValue(key), EPS) }
    }

    // ------------------------------------------------------------------
    // ESM 软互抑 / 逃逸阀
    // ------------------------------------------------------------------

    @Test
    fun `esm inhibition presses only the excess above baseline`() {
        val drives = drivesWith("attachment" to 0.50, "stress" to 0.55)
        val result = DriveEngine.applyEsmInhibition(drives)

        // posExcess = 0.20/6, negExcess = 0.40/3
        // attachment: 0.30 + 0.20 × (1 − 0.3 × 0.133333) = 0.492
        assertEquals(0.492, result.getValue("attachment"), 1e-6)
        // stress: 0.15 + 0.40 × (1 − 0.3 × 0.033333) = 0.546
        assertEquals(0.546, result.getValue("stress"), 1e-6)
        // 未超出的维度一动不动
        assertEquals(
            DriveEngine.DRIVE_BASELINES.getValue("libido"),
            result.getValue("libido"),
            EPS,
        )
    }

    @Test
    fun `esm inhibition never pushes a drive below its baseline`() {
        val drives = drivesWith(
            "attachment" to 0.90,
            "stress" to 0.90,
            "fatigue" to 0.90,
            "possessiveness" to 0.90,
        )
        val result = DriveEngine.applyEsmInhibition(drives)
        DriveEngine.DRIVE_KEYS.forEach { key ->
            assertTrue(
                "维度 $key 被压到基线以下",
                result.getValue(key) >= DriveEngine.DRIVE_BASELINES.getValue(key) - EPS,
            )
        }
    }

    @Test
    fun `escape valve needs three consecutive imbalanced ticks`() {
        val negativeHeavy = drivesWith("stress" to 0.65)

        // 第一次：streak 0 → 1，不触发
        val first = DriveEngine.applyEscapeValve(negativeHeavy, 0)
        assertEquals(1, first.streak)
        assertEquals(0.65, first.drives.getValue("stress"), EPS)

        // 第二次：streak 1 → 2，仍不触发
        val second = DriveEngine.applyEscapeValve(first.drives, first.streak)
        assertEquals(2, second.streak)

        // 第三次：streak 2 → 3，触发并拉回一半，streak 清零
        val third = DriveEngine.applyEscapeValve(second.drives, second.streak)
        assertEquals(0, third.streak)
        // 0.15 + 0.50 × (1 − 0.5) = 0.40
        assertEquals(0.40, third.drives.getValue("stress"), 1e-6)
    }

    @Test
    fun `escape valve streak resets when balance is restored`() {
        val balanced = baselineDrives()
        val result = DriveEngine.applyEscapeValve(balanced, 2)
        assertEquals(0, result.streak)
    }

    // ------------------------------------------------------------------
    // 时间推进（惰性 tick）
    // ------------------------------------------------------------------

    @Test
    fun `tick damps a high drive back toward its baseline`() {
        val result = DriveEngine.tickDrives(
            drives = drivesWith("attachment" to 0.80),
            escapeStreak = 0,
            elapsedMillis = DriveEngine.DESIRE_TICK_MILLIS,
            idleSeconds = 0.0,
        )
        // rate = DAMPING × 0.80 = 0.016 → 0.80 + 0.016 × (0.30 − 0.80) = 0.792
        assertEquals(0.792, result.drives.getValue("attachment"), 1e-6)
    }

    @Test
    fun `tick lifts the private-life drives to the ambient floor`() {
        val result = DriveEngine.tickDrives(
            drives = baselineDrives(),
            escapeStreak = 0,
            elapsedMillis = DriveEngine.DESIRE_TICK_MILLIS,
            idleSeconds = 0.0,
        )
        // curiosity: floor = 0.22 + 0.14 = 0.36；lift 0.38 → 0.22 + 0.14×0.38 = 0.2732
        assertEquals(0.2732, result.drives.getValue("curiosity"), 1e-6)
        // reflection: floor = 0.18 + 0.12 = 0.30 → 0.18 + 0.12×0.38 = 0.2256
        assertEquals(0.2256, result.drives.getValue("reflection"), 1e-6)
        assertEquals(0.2256, result.drives.getValue("stewardship"), 1e-6)
        // 非私人生活维度停在基线上
        assertEquals(DriveEngine.DRIVE_BASELINES.getValue("attachment"), result.drives.getValue("attachment"), EPS)
        assertEquals(DriveEngine.DRIVE_BASELINES.getValue("social"), result.drives.getValue("social"), EPS)
    }

    @Test
    fun `tick result carries a recomputed local fatigue`() {
        val result = DriveEngine.tickDrives(
            drives = drivesWith("fatigue" to 0.50),
            escapeStreak = 0,
            elapsedMillis = DriveEngine.DESIRE_TICK_MILLIS,
            idleSeconds = 0.0,
        )
        val expectedFatigue = result.drives.getValue("fatigue")
        assertEquals(
            DriveEngine.clamp(expectedFatigue * DriveEngine.FATIGUE_SENSITIVITY.getValue("social")),
            result.localFatigue.getValue("social"),
            EPS,
        )
    }

    // ------------------------------------------------------------------
    // 事件包 → 增量
    // ------------------------------------------------------------------

    @Test
    fun `planEvent scales the primary delta by intensity confidence and source weight`() {
        val event = DriveEngine.DriveEvent(
            source = "user_message",
            eventLabel = "user_message",
            primaryDrive = "attachment",
            intensity = 0.5,
            confidence = 0.65,
            agency = 0.75,
            brain = mapOf("closeness_pull" to 0.5),
        )
        val plan = DriveEngine.planEvent(event, baselineDrives())

        assertFalse(plan.suppressed)
        assertEquals("attachment", plan.primaryDrive)
        // 0.22 × 0.5 × 0.65 × 1.0 × 1.35 × (1 + 0.35×0.5)
        val expected = 0.22 * 0.5 * 0.65 * 1.0 * 1.35 * (1.0 + 0.35 * 0.5)
        assertEquals(expected, plan.proposed.getValue("attachment"), 1e-9)
    }

    @Test
    fun `planEvent suppresses low agency`() {
        val event = DriveEngine.DriveEvent(
            source = "user_message",
            eventLabel = "x",
            primaryDrive = "attachment",
            agency = 0.20,
        )
        val plan = DriveEngine.planEvent(event, baselineDrives())
        assertTrue(plan.suppressed)
        assertEquals("low agency", plan.reason)
    }

    @Test
    fun `planEvent suppresses low confidence`() {
        val event = DriveEngine.DriveEvent(
            source = "user_message",
            eventLabel = "x",
            primaryDrive = "attachment",
            confidence = 0.10,
        )
        val plan = DriveEngine.planEvent(event, baselineDrives())
        assertTrue(plan.suppressed)
        assertEquals("low confidence", plan.reason)
    }

    @Test
    fun `planEvent suppresses events without a primary drive`() {
        val event = DriveEngine.DriveEvent(source = "user_message", eventLabel = "x")
        val plan = DriveEngine.planEvent(event, baselineDrives())
        assertTrue(plan.suppressed)
        assertEquals("no primary drive", plan.reason)
    }

    @Test
    fun `planEvent drops possessiveness below the territorial gate`() {
        val event = DriveEngine.DriveEvent(
            source = "user_message",
            eventLabel = "x",
            primaryDrive = "possessiveness",
            brain = mapOf("territorial_alarm" to 0.20),
        )
        val plan = DriveEngine.planEvent(event, baselineDrives())
        assertFalse(plan.proposed.containsKey("possessiveness"))
        assertTrue(plan.suppressed)
        assertEquals("territorial_alarm below gate", plan.reason)
    }

    @Test
    fun `planEvent keeps side effects within the primary budget`() {
        val event = DriveEngine.DriveEvent(
            source = "user_message",
            eventLabel = "x",
            primaryDrive = "attachment",
            intensity = 0.9,
            confidence = 0.9,
            brain = mapOf(
                "closeness_pull" to 0.9,
                "expression_pressure" to 0.9,
                "energy_cost" to 0.9,
                "tension_load" to 0.9,
            ),
        )
        val plan = DriveEngine.planEvent(event, baselineDrives())
        val primary = plan.proposed.getValue("attachment")
        val sideTotal = plan.proposed.filterKeys { it != "attachment" }.values.sum()
        assertTrue("侧效应 $sideTotal 超出了主维预算 ${primary * 0.65}", sideTotal <= primary * 0.65 + EPS)
    }

    @Test
    fun `applyPlan reports before and after for every changed dimension`() {
        val event = DriveEngine.DriveEvent(
            source = "user_message",
            eventLabel = "x",
            primaryDrive = "attachment",
            brain = mapOf("closeness_pull" to 0.5),
        )
        val before = baselineDrives()
        val plan = DriveEngine.planEvent(event, before)
        val (after, applied) = DriveEngine.applyPlan(before, plan)

        assertFalse(applied.isEmpty())
        val attachment = applied.getValue("attachment")
        assertEquals(DriveEngine.DRIVE_BASELINES.getValue("attachment"), attachment.before, 1e-4)
        assertEquals(after.getValue("attachment"), attachment.after, 1e-4)
        assertTrue(after.getValue("attachment") > before.getValue("attachment"))
    }

    @Test
    fun `applyPlan changes nothing when suppressed`() {
        val before = baselineDrives()
        val plan = DriveEngine.planEvent(
            DriveEngine.DriveEvent(source = "user_message", eventLabel = "x"),
            before,
        )
        val (after, applied) = DriveEngine.applyPlan(before, plan)
        assertTrue(applied.isEmpty())
        DriveEngine.DRIVE_KEYS.forEach { key ->
            assertEquals(before.getValue(key), after.getValue(key), EPS)
        }
    }

    // ------------------------------------------------------------------
    // 基线：24 小时前同时段优先
    // ------------------------------------------------------------------

    @Test
    fun `baseline prefers the same time yesterday`() {
        val samples = listOf(
            DriveBaseline.Sample(NOW - 25 * HOUR, mapOf("social" to 0.30)),
            DriveBaseline.Sample(NOW - 1 * HOUR, mapOf("social" to 0.60)),
        )
        val baseline = DriveBaseline.pickBaseline(samples, NOW)
        assertNotNull(baseline)
        assertEquals(DriveBaseline.BaselineKind.SAME_TIME_YESTERDAY, baseline!!.kind)
        assertEquals(0.30, baseline.values.getValue("social"), EPS)
        assertEquals(25.0, baseline.ageHours, 0.01)
        assertEquals(NOW - 25 * HOUR, baseline.ts)
    }

    @Test
    fun `baseline falls back to the oldest sample when nothing is near yesterday`() {
        val samples = listOf(
            DriveBaseline.Sample(NOW - 29 * HOUR, mapOf("social" to 0.20)),
            DriveBaseline.Sample(NOW - 1 * HOUR, mapOf("social" to 0.60)),
        )
        val baseline = DriveBaseline.pickBaseline(samples, NOW)
        assertNotNull(baseline)
        // 29 小时前离目标（24 小时前）差 5 小时，超出 3 小时容差 → 退回最老
        assertEquals(DriveBaseline.BaselineKind.WARMUP_OLDEST, baseline!!.kind)
        assertEquals(0.20, baseline.values.getValue("social"), EPS)
    }

    @Test
    fun `baseline is null when there is no usable history`() {
        assertNull(DriveBaseline.pickBaseline(emptyList(), NOW))
        // 只有刚存下的一条：不到 WARM_BASELINE_MIN_HOURS，不算历史
        val tooFresh = listOf(DriveBaseline.Sample(NOW - 60_000L, mapOf("social" to 0.30)))
        assertNull(DriveBaseline.pickBaseline(tooFresh, NOW))
    }

    // ------------------------------------------------------------------
    // 越线观测
    // ------------------------------------------------------------------

    @Test
    fun `absolute threshold only fires for the listed dimensions`() {
        val current = drivesWith("curiosity" to 0.55, "stress" to 0.10, "attachment" to 0.95)
        val crossings = DriveBaseline.crossings(current, null)
        assertEquals(1, crossings.size)
        assertEquals("curiosity", crossings[0].drive)
        assertEquals(DriveBaseline.TRIGGER_ABSOLUTE, crossings[0].trigger)
    }

    @Test
    fun `intimate dimensions never appear in the absolute threshold table`() {
        DriveBaseline.SIGNAL_THRESHOLDS.forEach { signal ->
            assertFalse(
                "亲密三维 ${signal.drive} 不该进阈值表",
                signal.drive in setOf("libido", "possessiveness", "attachment"),
            )
        }
    }

    @Test
    fun `intimate dimensions never produce a crossing, absolute or relative`() {
        // 用户 2026-09-22 拍板的第 3 条：「亲密三维永不生成 impulse」。
        //
        // 这条测试以前写反了 —— 它断言的是「relative rise fires for any dimension
        // including the intimate ones」，理由是 Elektron 的 `_relative_candidates`
        // 也全维遍历。那是把实现当成了规格：越线会带着维度名与数值进 Agent 的
        // 提示词，等于给亲密三维留了一条影响行为的通路，而拍板明确要求没有这条通路。
        val current = drivesWith("attachment" to 0.90, "libido" to 0.80, "possessiveness" to 0.70)
        // 基线给全 9 维：相对那一路遍历的是基线里的维度，只放三维进去的话
        // 这条测试就没证明"遍历到它们时会被跳过"。
        val baseline = DriveBaseline.Baseline(
            values = DriveEngine.DRIVE_BASELINES,
            ageHours = 24.0,
            kind = DriveBaseline.BaselineKind.SAME_TIME_YESTERDAY,
            ts = NOW - 24 * HOUR,
        )
        val crossings = DriveBaseline.crossings(current, baseline)

        val leaked = crossings.map { it.drive }.filter { it in DriveBaseline.NON_SIGNAL_DRIVES }
        assertTrue(
            "亲密三维涨得再多也不该产生候选，实际泄漏了 $leaked",
            leaked.isEmpty(),
        )
        assertTrue("这一组不该有任何越线", crossings.isEmpty())
        assertEquals(
            "三条亲密维度必须都在排除集合里",
            setOf("libido", "possessiveness", "attachment"),
            DriveBaseline.NON_SIGNAL_DRIVES,
        )
    }

    @Test
    fun `relative rise still fires for the ordinary dimensions`() {
        // 上一条排的是亲密三维，不能顺手把正常维度也排掉。
        //
        // 用 reflection 而不是 social：social 的绝对阈值是 0.55，取个高值会同时
        // 命中绝对那一路，`crossings.size` 就不是 1 了 —— 那样测的是两条路，
        // 分不清相对那一路到底还在不在。reflection 阈值 0.60，取 0.50 只走相对。
        val current = drivesWith("reflection" to 0.50)
        val baseline = DriveBaseline.Baseline(
            values = mapOf("reflection" to 0.10),
            ageHours = 24.0,
            kind = DriveBaseline.BaselineKind.SAME_TIME_YESTERDAY,
            ts = NOW - 24 * HOUR,
        )
        val crossings = DriveBaseline.crossings(current, baseline)

        assertEquals(1, crossings.size)
        assertEquals("reflection", crossings[0].drive)
        assertEquals(DriveBaseline.TRIGGER_RELATIVE, crossings[0].trigger)
        assertEquals(0.40, crossings[0].rise!!, 1e-6)
    }

    @Test
    fun `relative rise ignores movements below the threshold`() {
        val current = drivesWith("social" to 0.30)
        val baseline = DriveBaseline.Baseline(
            values = mapOf("social" to 0.25),
            ageHours = 24.0,
            kind = DriveBaseline.BaselineKind.SAME_TIME_YESTERDAY,
            ts = NOW - 24 * HOUR,
        )
        assertTrue(DriveBaseline.crossings(current, baseline).isEmpty())
    }

    // ------------------------------------------------------------------
    // 对话事件折算
    // ------------------------------------------------------------------

    @Test
    fun `an empty user message still counts as a low-intensity approach`() {
        val event = DialogueEventSource.userMessage("   ")
        assertEquals(DialogueEventSource.SOURCE_USER_MESSAGE, event.source)
        assertEquals("attachment", event.primaryDrive)
        // 一条消息的存在本身就是一次靠近：三个特征都有非零下限
        assertEquals(0.35, event.brain.getValue("closeness_pull"), EPS)
        assertEquals(0.30, event.brain.getValue("expression_pressure"), EPS)
        assertEquals(0.20, event.brain.getValue("energy_cost"), EPS)
        assertEquals(0.35, event.intensity, EPS)
    }

    @Test
    fun `a full-length user message saturates the structural features`() {
        val event = DialogueEventSource.userMessage("a".repeat(DialogueEventSource.FULL_LENGTH_CHARS.toInt()))
        assertEquals(0.60, event.brain.getValue("closeness_pull"), EPS)
        assertEquals(0.80, event.brain.getValue("expression_pressure"), EPS)
        assertEquals(0.55, event.brain.getValue("energy_cost"), EPS)
        assertEquals(0.75, event.intensity, EPS)
    }

    @Test
    fun `structural features are monotonic in message length and bounded`() {
        val short = DialogueEventSource.userMessage("嗯")
        val medium = DialogueEventSource.userMessage("a".repeat(120))
        val huge = DialogueEventSource.userMessage("a".repeat(5000))

        assertTrue(medium.intensity > short.intensity)
        assertTrue(huge.intensity >= medium.intensity)
        // 超长消息不会突破上限
        assertEquals(0.75, huge.intensity, EPS)
        assertEquals(0.60, huge.brain.getValue("closeness_pull"), EPS)
    }

    @Test
    fun `evidence is truncated for the ledger`() {
        val long = "x".repeat(500)
        val evidence = DialogueEventSource.buildEvidence(long)
        assertEquals(DialogueEventSource.EVIDENCE_MAX_CHARS + 1, evidence.length)
        assertEquals("(empty)", DialogueEventSource.buildEvidence("   "))
    }

    @Test
    fun `a real user message moves attachment and fatigue through the full chain`() {
        // 一次完整的真实链路（不含 IO）：事件 → plan → apply → 疲劳重算 → 快照
        val before = baselineDrives()
        val event = DialogueEventSource.userMessage("a".repeat(240))
        val plan = DriveEngine.planEvent(event, before)
        assertFalse(plan.suppressed)

        val (after, applied) = DriveEngine.applyPlan(before, plan)
        assertTrue(applied.containsKey("attachment"))
        assertTrue(after.getValue("attachment") > before.getValue("attachment"))

        // 每维独立疲劳跟着 fatigue 重算（这里 fatigue 未动，疲劳保持基线水平）
        val localFatigue = DriveEngine.computeLocalFatigue(after.getValue("fatigue"))
        assertEquals(DriveEngine.DRIVE_KEYS.size, localFatigue.size)

        val snapshot = DriveEngine.activationSnapshot(after, localFatigue)
        assertTrue(snapshot.getValue("attachment") > 0.0)
        assertEquals("attachment", DriveEngine.dominantDrive(after, localFatigue))
    }
}
