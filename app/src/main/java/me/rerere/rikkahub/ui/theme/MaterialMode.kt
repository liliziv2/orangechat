package me.rerere.rikkahub.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.data.datastore.DisplayMaterialMode

// 全局材质边框。
//
// 这条边框被十几个设置页 / 扩展页 / 抽屉下拉共用（都走 materialModeBorderStroke()），
// 原来 alpha 0.18 让每个容器都描一圈可见的框 —— 叠加起来就是"整个 App 都像玻璃"
// 的主要来源之一。玻璃质感应该只属于消息气泡，普通页面容器不需要靠描边交代材质。
//
// 0.18 → 0.07：留一点材质暗示，但不再抢层级。气泡自己的边框（GLASS_BUBBLE_BORDER_ALPHA
// 等，在 ChatMessage.kt）不受这里影响。
private const val MATERIAL_BORDER_ALPHA = 0.07f

/**
 * 表面变体 —— 同一材质模式在不同“语义级别”的控件上的强度档位。
 *
 * 任务单要求“同一模式在侧栏、顶栏按钮、输入框、普通表面和弹出界面遵循同一套
 * 基准规则，同时允许组件通过明确的样式变体降低装饰强度”。这里就是那个
 * “明确的样式变体”：
 *
 * - CONTENT：内容表面（思维链卡片、消息气泡、侧栏容器等）—— 基准强度
 * - CONTROL：控件外壳（顶栏圆钮、助手条 chip、Thinking chip 等）—— 降档，
 *            避免每个小元件都变成抢眼的玻璃球
 *
 * 新增变体必须在这里显式列出，不允许组件自己手写 alpha。
 */
enum class MaterialVariant {
    /** 内容表面：基准强度 */
    CONTENT,

    /** 控件外壳：降低装饰强度，但不改变是否响应滑块 */
    CONTROL,
}

/**
 * 材质解析结果 —— 纯数据，不依赖 Compose，可直接单元测试。
 *
 * @param fillAlpha 填充层的基础 alpha（**不包含**任何用户透明度滑块）
 * @param borderAlpha 发丝描边 alpha；null 表示该模式不画描边
 * @param glassLayerAlpha 玻璃高光/层次层的强度系数；0f 表示不画玻璃层
 */
data class SurfaceMaterialSpec(
    val fillAlpha: Float,
    val borderAlpha: Float?,
    val glassLayerAlpha: Float,
)

/**
 * 单一、可预测的材质解析入口。
 *
 * 所有需要「按材质模式决定底色/描边/玻璃层」的组件都应该读这里，而不是各自
 * 手写 `when (mode)` —— 这正是旧实现里同一模式在不同页面表现不一致的根源。
 *
 * 数值与旧实现完全一致（CONTENT 取 ChainOfThought 的 0.78/0.56，CONTROL 取
 * MaterialActionSurface 的 0.6/0.55/0.45），本次抽取不改变任何观感。
 */
fun resolveSurfaceMaterial(
    mode: DisplayMaterialMode,
    variant: MaterialVariant = MaterialVariant.CONTENT,
): SurfaceMaterialSpec {
    val resolved = if (mode == DisplayMaterialMode.FOLLOW_THEME) {
        // 「跟随主题」只做兼容映射：渲染上等同 FLAT，不作为第四种可选模式。
        DisplayMaterialMode.FLAT
    } else {
        mode
    }
    return when (resolved) {
        // 平面：主题实色表面，不画玻璃渐变/高光/额外描边。
        // 注意：这**只**说明材质是平面的，并不代表透明度滑块失效。
        DisplayMaterialMode.FLAT -> SurfaceMaterialSpec(
            fillAlpha = if (variant == MaterialVariant.CONTROL) 0.6f else 1f,
            borderAlpha = null,
            glassLayerAlpha = 0f,
        )

        // 轻透：表面透出正确背景 + 细、低对比度描边，不叠多层高光。
        DisplayMaterialMode.TRANSLUCENT -> SurfaceMaterialSpec(
            fillAlpha = if (variant == MaterialVariant.CONTROL) 0.55f else 0.78f,
            borderAlpha = MATERIAL_BORDER_ALPHA,
            glassLayerAlpha = 0f,
        )

        // 玻璃：轻微的层次、高光和细描边，视觉重量保持克制。
        DisplayMaterialMode.GLASS -> SurfaceMaterialSpec(
            fillAlpha = if (variant == MaterialVariant.CONTROL) 0.45f else 0.56f,
            borderAlpha = MATERIAL_BORDER_ALPHA,
            glassLayerAlpha = 1f,
        )

        // 不可达（上面已归一化），为穷举性保留。
        DisplayMaterialMode.FOLLOW_THEME -> resolveSurfaceMaterial(
            DisplayMaterialMode.FLAT,
            variant,
        )
    }
}

/**
 * 把“模式决定的材质 alpha”与“用户滑块值”合成为最终有效 alpha。
 *
 * 这是整个材质/透明度体系唯一允许做乘法的地方：
 * - 模式负责“材质表现”（fillAlpha / 描边 / 玻璃层）
 * - 滑块负责“这个表面多透”（sliderPercent 0..100，100 = 不额外加透）
 * 两者不互相覆盖，也不会在多层重复相乘（调用方必须只在这里合成一次）。
 *
 * @param sliderPercent 用户设置值 0..100；越界值会被夹回范围内。
 */
fun effectiveSurfaceAlpha(
    spec: SurfaceMaterialSpec,
    sliderPercent: Float,
): Float {
    val factor = (sliderPercent / 100f).coerceIn(0f, 1f)
    return (spec.fillAlpha * factor).coerceIn(0f, 1f)
}


@Composable
fun materialModeBorderStroke(): BorderStroke? = if (LocalMaterialMode.current.hasMaterialBorder) {
    BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = MATERIAL_BORDER_ALPHA),
    )
} else {
    null
}

@Composable
fun Modifier.materialTopAppBarDivider(): Modifier {
    if (!LocalMaterialMode.current.hasMaterialBorder) return this

    // 同上：TopBar 底部那条线也是"全局玻璃感"的一部分，跟边框用同一个 alpha
    val dividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = MATERIAL_BORDER_ALPHA)
    return drawWithContent {
        drawContent()
        val strokeWidth = 1.dp.toPx()
        val y = size.height - strokeWidth / 2f
        drawLine(
            color = dividerColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth,
        )
    }
}

private val DisplayMaterialMode.hasMaterialBorder: Boolean
    get() = this == DisplayMaterialMode.TRANSLUCENT || this == DisplayMaterialMode.GLASS