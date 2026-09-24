package me.rerere.rikkahub.utils

import org.apache.commons.text.StringEscapeUtils
import java.net.URLDecoder
import java.net.URLEncoder
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

fun String.urlEncode(): String {
    return URLEncoder.encode(this, "UTF-8")
}

fun String.urlDecode(): String {
    return URLDecoder.decode(this, "UTF-8")
}

@OptIn(ExperimentalEncodingApi::class)
fun String.base64Encode(): String {
    return Base64.encode(this.toByteArray())
}

@OptIn(ExperimentalEncodingApi::class)
fun String.base64Decode(): String {
    return String(Base64.decode(this))
}

fun String.escapeHtml(): String {
    return StringEscapeUtils.escapeHtml4(this)
}

fun String.unescapeHtml(): String {
    return StringEscapeUtils.unescapeHtml4(this)
}

fun Number.toFixed(digits: Int = 0) = "%.${digits}f".format(this)

fun String.applyPlaceholders(
    vararg placeholders: Pair<String, String>,
): String {
    var result = this
    for ((placeholder, replacement) in placeholders) {
        result = result.replace("{$placeholder}", replacement)
    }
    return result
}

fun Long.fileSizeToString(): String {
    return when {
        this < 1024 -> "$this B"
        this < 1024 * 1024 -> "${this / 1024} KB"
        this < 1024 * 1024 * 1024 -> "${this / (1024 * 1024)} MB"
        else -> "${this / (1024 * 1024 * 1024)} GB"
    }
}

fun Int.formatNumber(): String {
    val absValue = kotlin.math.abs(this)
    val sign = if (this < 0) "-" else ""

    return when {
        absValue < 1000 -> this.toString()
        absValue < 1000000 -> {
            val value = absValue / 1000.0
            if (value == value.toInt().toDouble()) {
                "$sign${value.toInt()}K"
            } else {
                "$sign${value.toFixed(1)}K"
            }
        }

        absValue < 1000000000 -> {
            val value = absValue / 1000000.0
            if (value == value.toInt().toDouble()) {
                "$sign${value.toInt()}M"
            } else {
                "$sign${value.toFixed(1)}M"
            }
        }

        else -> {
            val value = absValue / 1000000000.0
            if (value == value.toInt().toDouble()) {
                "$sign${value.toInt()}B"
            } else {
                "$sign${value.toFixed(1)}B"
            }
        }
    }
}

fun Float.toFixed(digits: Int = 0) = "%.${digits}f".format(this)
fun Double.toFixed(digits: Int = 0) = "%.${digits}f".format(this)

/**
 * 提取字符串中所有引号内的内容
 *
 * 支持四类引号：弯双引号 “…”、弯单引号 ‘…’、直双引号 "…"、直单引号 '…'。
 * 弯引号是中文排版里的默认引号，也是模型中文回复里最常出现的那种。
 *
 * 这里刻意用 `\uXXXX` 转义而不是直接写字符：弯引号和直引号在编辑器里长得几乎一样，
 * 之前那版就是把「中文双引号」写成了直引号，于是四条 pattern 实际只有两条、
 * 弯引号永远匹配不到 —— 表现为「只朗读引号内容」静默退化成朗读整段回复。
 *
 * @return 所有引号内内容的列表
 */
fun String.extractQuotedContent(): List<String> {
    val result = mutableListOf<String>()
    // 匹配多种引号类型
    val patterns = listOf(
        "\u201C([^\u201D]*?)\u201D", // 弯双引号 “...”
        "\u2018([^\u2019]*?)\u2019", // 弯单引号 ‘...’
        "\"([^\"]*?)\"",              // 直双引号 "..."
        "'([^']*?)'",                 // 直单引号 '...'
    )
    for (pattern in patterns) {
        val regex = Regex(pattern)
        regex.findAll(this).forEach { matchResult ->
            val content = matchResult.groupValues[1]
            if (content.isNotBlank()) {
                result.add(content)
            }
        }
    }
    return result
}

/**
 * 提取字符串中所有引号内的内容并合并为一个字符串
 * @param separator 分隔符，默认为换行
 * @return 合并后的字符串，如果没有引号内容则返回 null
 */
fun String.extractQuotedContentAsText(separator: String = "\n"): String? {
    val contents = extractQuotedContent()
    return if (contents.isNotEmpty()) {
        contents.joinToString(separator)
    } else {
        null
    }
}

/**
 * 只保留可以用英文音色朗读的内容。
 *
 * 逐字符过滤：拉丁字母、数字、常见标点和空白留下，中文/日文/韩文等一律丢掉。
 * 丢字符会让 "你好world" 变成 "world"，词与词之间可能粘连，所以最后把连续空白压成一个空格。
 * 过滤完只剩标点时返回空串，调用方据此跳过这次朗读。
 */
fun String.keepEnglishOnlyForTts(): String {
    val filtered = buildString {
        for (ch in this@keepEnglishOnlyForTts) {
            when {
                ch.code < 128 -> append(ch)
                ch.isWhitespace() -> append(' ')
                // 非 ASCII 的字母/表意文字直接丢，替换成空格避免单词粘在一起
                else -> append(' ')
            }
        }
    }
    val collapsed = filtered.replace(Regex("\\s+"), " ").trim()
    // 只剩标点符号时没有朗读价值
    return if (collapsed.none { it.isLetterOrDigit() }) "" else collapsed
}

/**
 * 把助手回复转换成实际要朗读的文本。
 *
 * 顺序有讲究：先剥内部协议（silent / [JUMP] 这些不能念出去），再按"只读引用"缩小范围，
 * 然后去 Markdown，最后才做英文过滤 —— 反过来做的话 Markdown 里的星号会被当成英文留下来。
 * 剥内部协议必须在提取引号内容之前：moodlet 的 reason 属性自带引号，
 * 先提取就只剩 reason 文本，再也认不出它原本属于 silent 标签。
 */
fun String.toChatTtsText(
    ttsOnlyReadQuoted: Boolean,
    ttsEnglishOnly: Boolean,
): String {
    val sanitized = stripTtsInternalMarkup()
    val scoped = if (ttsOnlyReadQuoted) sanitized.extractQuotedContentAsText() ?: sanitized else sanitized
    val plain = scoped.stripMarkdown()
    return if (ttsEnglishOnly) plain.keepEnglishOnlyForTts() else plain.trim()
}
