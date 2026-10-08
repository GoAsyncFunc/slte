package com.slte.app.data.remote.adapter

import com.slte.app.domain.model.EmailWhitelist
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 白名单后缀来自后端配置，各面板给法不统一：数组 / 单个字符串 / 逗号分隔 / 0。
 * 这里把"怎么解析"和"空列表等于不限制"两件事钉住。
 */
class EmailWhitelistParserTest {

    @Test
    fun `数组形式直接取字符串项`() {
        val element = buildJsonArray {
            add(JsonPrimitive("gmail.com"))
            add(JsonPrimitive("qq.com"))
        }

        assertEquals(listOf("gmail.com", "qq.com"), parseEmailWhitelistSuffixes(element))
    }

    @Test
    fun `逗号分隔字符串也能解析并规范化`() {
        val element = JsonPrimitive(" Gmail.com, *.163.com ;outlook.com ")

        assertEquals(listOf("gmail.com", "163.com", "outlook.com"), parseEmailWhitelistSuffixes(element))
    }

    @Test
    fun `未启用时后端给 0 或 null`() {
        assertEquals(emptyList<String>(), parseEmailWhitelistSuffixes(JsonPrimitive(0)))
        assertEquals(emptyList<String>(), parseEmailWhitelistSuffixes(null))
    }

    @Test
    fun `空列表表示不限制`() {
        val whitelist = EmailWhitelist.None

        assertFalse(whitelist.isEnabled)
        assertEquals(null, whitelist.defaultSuffix)
    }

    @Test
    fun `启用后默认选第一个后缀`() {
        val whitelist = EmailWhitelist(listOf("gmail.com", "qq.com"))

        assertTrue(whitelist.isEnabled)
        assertEquals("gmail.com", whitelist.defaultSuffix)
    }
}
