package me.rerere.ai.util

/**
 * HTTP 响应诊断工具（纯字符串逻辑，不依赖 OkHttp / Android，便于单元测试）。
 *
 * 背景：部分网关 / CDN（Cloudflare 等）在拦截请求时返回 HTML 错误页而不是 JSON。
 * 调用方此前会把这个 HTML 直接交给 kotlinx.serialization 解析，最终抛给用户的会是
 * `Unexpected JSON token at offset N ... JSON input: <!DOCTYPE html>` 这种无法定位的
 * 原始解析异常，真正的 HTTP 状态码 / Content-Type / 最终 URL 全部被丢弃。
 *
 * 这里的函数只做「识别 + 描述 + 构造可读异常」，不改变任何请求行为。
 */
object HttpDiagnostics {

    private const val BODY_PREVIEW_LIMIT = 300

    private val WHITESPACE = Regex("\\s+")

    /** 响应体是否看起来是 HTML（先看 Content-Type，再看正文开头）。 */
    fun looksLikeHtml(contentType: String?, body: String?): Boolean {
        val ct = contentType?.lowercase().orEmpty()
        if (ct.contains("text/html") || ct.contains("application/xhtml")) return true
        val head = body?.trimStart()?.take(16)?.lowercase().orEmpty()
        return head.startsWith("<!doctype") || head.startsWith("<html")
    }

    /** 单行、限长的响应体预览，避免把整页 HTML 写进日志或错误消息。 */
    fun bodyPreview(body: String?, limit: Int = BODY_PREVIEW_LIMIT): String {
        if (body == null) return "<empty>"
        val oneLine = body.replace(WHITESPACE, " ").trim()
        if (oneLine.isEmpty()) return "<empty>"
        return if (oneLine.length <= limit) oneLine else oneLine.take(limit) + "...(共 ${oneLine.length} 字符)"
    }

    /**
     * 描述一次 HTTP 响应：状态码 / 请求 URL / 最终 URL（重定向后）/ Content-Type /
     * Content-Length。
     *
     * 只读响应侧信息，不读请求头，因此不会把 Authorization / API key 写进日志。
     */
    fun describeHttpResponse(
        code: Int,
        requestUrl: String?,
        finalUrl: String?,
        contentType: String?,
        contentLength: String?,
    ): String = buildString {
        append("HTTP ").append(code)
        append(" | request URL: ").append(requestUrl ?: "<unknown>")
        if (finalUrl != null && finalUrl != requestUrl) {
            append(" | final URL (redirect): ").append(finalUrl)
        } else {
            append(" | final URL: ").append(finalUrl ?: "<unknown>")
        }
        append(" | Content-Type: ").append(contentType ?: "<none>")
        append(" | Content-Length: ").append(contentLength ?: "<none>")
    }

    /**
     * 构造「服务器返回 HTML，不是 JSON」的明确异常，保留状态码与最终 URL。
     *
     * 这样用户看到的不再是 kotlinx 的原始解析异常，而能直接判断是网关拦截
     * （缺代理 / 自定义 User-Agent）还是 endpoint 路径写错。
     */
    fun htmlResponseException(
        code: Int,
        finalUrl: String?,
        contentType: String?,
        body: String?,
    ): IllegalStateException = IllegalStateException(
        buildString {
            append("服务器返回 HTML，不是 JSON（HTTP ").append(code).append("）")
            if (!finalUrl.isNullOrBlank()) {
                append("，最终 URL: ").append(finalUrl)
            }
            append("，Content-Type: ").append(contentType ?: "<none>")
            append("。响应开头: ").append(bodyPreview(body))
            append("。常见原因：请求被网关/CDN 拦截（如 Cloudflare 挑战页）、缺少代理或自定义 User-Agent，或 endpoint 路径不正确。")
        }
    )
}

/** 拼接 baseUrl 与 path，避免出现双斜杠或漏斜杠。 */
fun joinUrl(base: String, path: String): String {
    val normalizedBase = base.trim().trimEnd('/')
    if (path.isEmpty()) return normalizedBase
    val normalizedPath = if (path.startsWith("/")) path else "/$path"
    return normalizedBase + normalizedPath
}
