package com.slte.app.data.remote.adapter

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * 解析站点配置里的邮箱白名单后缀（`email_whitelist_suffix`）。
 *
 * 各面板给法不统一：数组、单个字符串、逗号分隔字符串都有，未启用时是 `0` / `null`。
 * 这里统一成规范化的域名列表（小写、去掉 `@` 与前缀通配），避免每个适配器各写一套判断。
 */
internal fun parseEmailWhitelistSuffixes(element: JsonElement?): List<String> {
    val raw =
        when (element) {
            is JsonArray ->
                element.mapNotNull { item ->
                    (item as? JsonPrimitive)?.takeIf { it.isString }?.content
                }
            is JsonPrimitive -> if (element.isString) listOf(element.content) else emptyList()
            else -> emptyList()
        }
    return raw
        .asSequence()
        .flatMap { it.split(',', ';', '|', ' ', '\n').asSequence() }
        .map { it.trim().lowercase().removePrefix("*.").removePrefix("@").trimStart('.') }
        .filter { it.isNotEmpty() && it.contains('.') }
        .distinct()
        .toList()
}
