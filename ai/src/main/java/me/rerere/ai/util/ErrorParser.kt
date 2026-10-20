package me.rerere.ai.util
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

class HttpException(
    message: String,
    /** 已知时携带真实 HTTP 状态码, 供 UI/日志区分错误类别; 连接层异常等无状态码场景为 null */
    val httpStatusCode: Int? = null,
) : RuntimeException(message)

fun JsonElement.parseErrorDetail(): HttpException {
    return when (this) {
        is JsonObject -> {
            // 常见错误字段, 依次找第一个「存在且携带真实信息」的
            val errorFields = listOf("error", "detail", "message", "description", "type", "code")

            // 判断字段值是否携带可读信息:
            // - 布尔 / 空白字符串: 纯标记, 无信息
            // - 字面量 "true"/"false" 字符串: 网关把布尔序列化成字符串的产物,
            //   如 {"message":"true","type":"bad_response_status_code"}, 同样无信息
            fun JsonElement.hasRealInfo(): Boolean = when (this) {
                is JsonPrimitive -> {
                    val c = content
                    !(booleanOrNull != null || c.isBlank() || c.equals("true", true) || c.equals("false", true))
                }
                else -> true
            }

            // 无信息的标记字段跳过后继续找, 全部没有才把整个对象序列化出来 (保留完整上下文供排查)
            val found = errorFields.firstOrNull { key -> this[key]?.hasRealInfo() == true }
            if (found != null) {
                this[found]!!.parseErrorDetail()
            } else {
                HttpException(Json.encodeToString(JsonElement.serializer(), this))
            }
        }
        is JsonArray -> {
            if (this.isEmpty()) {
                HttpException("Unknown error: Empty JSON array")
            } else {
                // 递归解析数组的第一个元素
                this.first().parseErrorDetail()
            }
        }
        is JsonPrimitive -> {
            val isBooleanLike = booleanOrNull != null ||
                content.equals("true", true) || content.equals("false", true)
            if (isBooleanLike) {
                // 裸布尔值(含被序列化成字符串的)不携带错误信息, 注明无详情, 避免 UI 显示 "true"
                HttpException("Unknown error: $content (no detail provided)")
            } else {
                // 对于基本类型，直接使用其内容
                HttpException(this.jsonPrimitive.content)
            }
        }
        else -> {
            // 其他情况，序列化整个元素
            HttpException(Json.encodeToString(JsonElement.serializer(), this))
        }
    }
}

/**
 * 解析 HTTP 错误响应体并在异常上保留真实状态码。
 * 响应体无法解析为 JSON 时, 异常消息带上状态码与截断后的原始体, 不丢排查线索。
 * 调用方(各 Provider onFailure / 非 2xx 分支)统一改走此入口, 避免 response.code 中途丢失。
 */
fun parseHttpErrorResponse(
    statusCode: Int,
    body: String?,
): HttpException {
    val trimmed = body?.trim().orEmpty()
    if (trimmed.isEmpty()) {
        return HttpException("HTTP $statusCode (empty response body)", statusCode)
    }
    return try {
        val parsed = Json.parseToJsonElement(trimmed).parseErrorDetail()
        HttpException("HTTP $statusCode: ${parsed.message}", statusCode)
    } catch (e: Throwable) {
        val preview = if (trimmed.length > 300) trimmed.take(300) + "...(${trimmed.length} chars)" else trimmed
        HttpException("HTTP $statusCode: $preview", statusCode)
    }
}
