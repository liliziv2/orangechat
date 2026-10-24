package me.rerere.rikkahub.ui.theme

import me.rerere.rikkahub.data.datastore.DisplayMaterialMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 材质解析纯函数测试（任务单 §三 / §八.3）。
 *
 * 这些测试锁住三件事：
 * 1. 三种材质模式各有明确、互不相同的填充 alpha（不会「本质都是玻璃」）；
 * 2. FOLLOW_THEME 只作兼容映射，不产生第四种行为；
 * 3. 「膜式 alpha」与「滑块 alpha」只在 effectiveSurfaceAlpha 里相乘一次，
 *    不在多层重复相乘。
 */
class MaterialSpecTest {

    @Test
    fun `flat has no border and no glass layer`() {
        val spec = resolveSurfaceMaterial(DisplayMaterialMode.FLAT)
        assertEquals(1f, spec.fillAlpha, 0.0001f)
        assertNull("平面模式不应画描边", spec.borderAlpha)
        assertEquals(0f, spec.glassLayerAlpha, 0.0001f)
    }

    @Test
    fun `flat control variant keeps slider meaningful`() {
        // 平面模式的控件外壳仍然是轻底色（不是全裸），但必须仍然是平面：
        // 没有描边、没有玻璃层 —— 滑块则通过 effectiveSurfaceAlpha 生效。
        val spec = resolveSurfaceMaterial(DisplayMaterialMode.FLAT, MaterialVariant.CONTROL)
        assertEquals(0.6f, spec.fillAlpha, 0.0001f)
        assertNull(spec.borderAlpha)
        assertEquals(0f, spec.glassLayerAlpha, 0.0001f)
        // 平面模式下把滑块拉到 0 必须真的变透明（旧版本这里是退化成无效的）。
        assertEquals(0f, effectiveSurfaceAlpha(spec, 0f), 0.0001f)
        assertEquals(0.6f, effectiveSurfaceAlpha(spec, 100f), 0.0001f)
    }

    @Test
    fun `translucent has hairline border but no glass`() {
        val spec = resolveSurfaceMaterial(DisplayMaterialMode.TRANSLUCENT)
        assertEquals(0.78f, spec.fillAlpha, 0.0001f)
        assertEquals(0.07f, spec.borderAlpha!!, 0.0001f)
        assertEquals("轻透不应叠加玻璃高光层", 0f, spec.glassLayerAlpha, 0.0001f)
    }

    @Test
    fun `glass is lighter than translucent and carries glass layer`() {
        val translucent = resolveSurfaceMaterial(DisplayMaterialMode.TRANSLUCENT)
        val glass = resolveSurfaceMaterial(DisplayMaterialMode.GLASS)
        assertTrue("玻璃应比轻透更透", glass.fillAlpha < translucent.fillAlpha)
        assertEquals(0.56f, glass.fillAlpha, 0.0001f)
        assertEquals(0.07f, glass.borderAlpha!!, 0.0001f)
        assertEquals(1f, glass.glassLayerAlpha, 0.0001f)
    }

    @Test
    fun `follow theme maps to flat and never yields a fourth behaviour`() {
        MaterialVariant.entries.forEach { variant ->
            val follow = resolveSurfaceMaterial(DisplayMaterialMode.FOLLOW_THEME, variant)
            val flat = resolveSurfaceMaterial(DisplayMaterialMode.FLAT, variant)
            assertEquals(
                "FOLLOW_THEME($variant) 必须与 FLAT 完全一致",
                flat,
                follow,
            )
        }
    }

    @Test
    fun `control variant never exceeds content variant`() {
        // 控件外壳的装饰强度必须 <= 内容表面，避免「一排玻璃球」。
        listOf(
            DisplayMaterialMode.FLAT,
            DisplayMaterialMode.TRANSLUCENT,
            DisplayMaterialMode.GLASS,
        ).forEach { mode ->
            val content = resolveSurfaceMaterial(mode, MaterialVariant.CONTENT)
            val control = resolveSurfaceMaterial(mode, MaterialVariant.CONTROL)
            assertTrue(
                "$mode 的 CONTROL 填充不应比 CONTENT 更浓",
                control.fillAlpha <= content.fillAlpha + 0.0001f,
            )
        }
    }

    @Test
    fun `effective alpha multiplies slider exactly once and clamps`() {
        val spec = resolveSurfaceMaterial(DisplayMaterialMode.TRANSLUCENT)
        // 默认滑块 100% == 模式基准 alpha（不额外加透）
        assertEquals(0.78f, effectiveSurfaceAlpha(spec, 100f), 0.0001f)
        // 50% == 基准 × 0.5（只乘一次）
        assertEquals(0.39f, effectiveSurfaceAlpha(spec, 50f), 0.0001f)
        // 越界与极端值必须夹在 0..1
        assertEquals(0f, effectiveSurfaceAlpha(spec, -20f), 0.0001f)
        assertEquals(0.78f, effectiveSurfaceAlpha(spec, 480f), 0.0001f)
    }

    @Test
    fun `effective alpha is monotonic in slider value`() {
        // 任务单 §三 要求「透明度随滑块连续变化，不能在某区间被 clamp 成几乎固定的值」。
        val spec = resolveSurfaceMaterial(DisplayMaterialMode.GLASS)
        var previous = -1f
        for (percent in 0..100) {
            val alpha = effectiveSurfaceAlpha(spec, percent.toFloat())
            assertTrue("滑块 $percent 处 alpha 未单调递增", alpha >= previous)
            previous = alpha
        }
        // 不能在一整段区间里几乎不动：0→100 的提升必须足够明显。
        assertTrue(
            "0..100 的 alpha 变化过小，说明被 clamp 了",
            effectiveSurfaceAlpha(spec, 100f) - effectiveSurfaceAlpha(spec, 0f) > 0.4f,
        )
    }

    @Test
    fun `three selectable modes are distinct`() {
        val specs = listOf(
            DisplayMaterialMode.FLAT,
            DisplayMaterialMode.TRANSLUCENT,
            DisplayMaterialMode.GLASS,
        ).map { resolveSurfaceMaterial(it) }
        assertEquals("三种可选模式必须产生不同材质", 3, specs.toSet().size)
    }
}
