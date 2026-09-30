package me.rerere.rikkahub.ui.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.ui.theme.extendColors

enum class TagType {
    DEFAULT,
    SUCCESS,
    ERROR,
    WARNING,
    INFO
}

/**
 * 标签填充的透明度。
 *
 * 四个彩色系的 `extendColors.*2` 是**实色** —— 它们不在 Theme.kt 那份会被乘
 * `interfaceSurfaceOpacity` 的槽位表里，所以一直是全 App 饱和度最高的一块。
 * 模型卡上一次并排最多五颗（providerOverwrite / type / modality / ability 两颗），
 * 几颗高饱和胶囊压在模型名下面，视线先被标签抓走。
 *
 * 压到 0.62 而不是直接改色值：`extendColors` 是主题无关的常量表，改它等于五个
 * 主题一起变；叠一层 alpha 是让填充与它所在的面合成 —— 浅色主题下变淡、深色主题
 * 下变沉，两边都读作「降饱和」而不是「换了一个颜色」。文字色不动：填充变淡之后
 * `gray8 / red8 / orange8 / blue8` 的对比反而更清楚。
 */
private const val TagFillAlpha = 0.62f

@Composable
fun Tag(
    modifier: Modifier = Modifier,
    type: TagType = TagType.DEFAULT,
    onClick: (() -> Unit)? = null,
    children: @Composable RowScope.() -> Unit
) {
    val background = when (type) {
        TagType.SUCCESS -> MaterialTheme.extendColors.green2
        TagType.ERROR -> MaterialTheme.extendColors.red2
        TagType.WARNING -> MaterialTheme.extendColors.orange2
        TagType.INFO -> MaterialTheme.extendColors.blue2
        else -> MaterialTheme.colorScheme.tertiaryContainer
    }.copy(alpha = TagFillAlpha)
    val textColor = when (type) {
        TagType.SUCCESS -> MaterialTheme.extendColors.gray8
        TagType.ERROR -> MaterialTheme.extendColors.red8
        TagType.WARNING -> MaterialTheme.extendColors.orange8
        TagType.INFO -> MaterialTheme.extendColors.blue8
        else -> MaterialTheme.colorScheme.onTertiaryContainer
    }
    ProvideTextStyle(MaterialTheme.typography.labelSmall.copy(color = textColor)) {
        Row(
            modifier = modifier
                .clip(RoundedCornerShape(50))
                .background(background)
                .let {
                    if (onClick != null) {
                        it.clickable { onClick() }
                    } else {
                        it
                    }
                }
                .padding(horizontal = 6.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            children()
        }
    }
}

@Composable
@Preview(showBackground = true)
private fun TagPreview() {
    Column(
        modifier = Modifier.padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Tag(type = TagType.SUCCESS) {
            Text("测试")
        }
        Tag(type = TagType.ERROR) {
            Text("测试")
        }
        Tag(type = TagType.WARNING) {
            Text("测试")
        }
        Tag(type = TagType.INFO) {
            Text("测试")
        }
        Tag(type = TagType.DEFAULT) {
            Text("测试")
        }
    }
}
