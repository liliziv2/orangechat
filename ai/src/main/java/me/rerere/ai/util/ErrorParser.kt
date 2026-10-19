package me.rerere.ai.util

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonPrimitive

class HttpException(
    message: String
) : RuntimeException(message)

fun JsonElement.parseErrorDetail(): HttpException {
    return when (this) {
        is JsonObject -> {
            // 尝试获取常见的错误字段
            val errorFields = listOf("error", "detail", "message", "description")

            // 查找第一个存在的错误字段
            // 跳过布尔型 / 空白字段值: 例如 {"error": true} 里的 "error" 只是错误标记,
            // 不含任何可读信息, 若直接取其 content 会把字面量 "true"/"false"/"" 当成错误文案展示
            val foundField = errorFields.firstOrNull { key ->
                val value = this[key]
                value != null && !(value is JsonPrimitive && (value.booleanOrNull != null || value.content.isBlank()))
            }

            if (foundField != null) {
                // 递归解析找到的字段值
                this[foundField]!!.parseErrorDetail()
            } else {
                // 如果没有找到任何错误字段，序列化整个对象
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
            if (booleanOrNull != null) {
                // 裸布尔值不携带任何错误信息, 保留原始标记但注明无详情, 避免 UI 显示 "true"
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
