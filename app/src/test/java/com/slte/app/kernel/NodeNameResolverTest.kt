package com.slte.app.kernel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NodeNameResolverTest {

    @Test
    fun `去除协议装饰前缀后同名`() {
        assertEquals(NodeNameResolver.of("🇸🇬新加坡丨BGPˣ²"), NodeNameResolver.of("[vless]🇸🇬新加坡丨BGPˣ²"))
        assertEquals(NodeNameResolver.of("香港01"), NodeNameResolver.of("[vless]【香港】香港01"))
        assertEquals(NodeNameResolver.of("香港01"), NodeNameResolver.of("🇭🇰[vless]香港01"))
        assertEquals(NodeNameResolver.of("香港01"), NodeNameResolver.of("［vless］香港01"))
    }

    @Test
    fun `忽略全角字符与空白差异`() {
        assertEquals(NodeNameResolver.of("hk01"), NodeNameResolver.of("ＨＫ 01"))
        assertEquals(NodeNameResolver.of("香港01"), NodeNameResolver.of("\u200B香港01\u00A0"))
    }

    @Test
    fun `精确同名优先于规范化匹配`() {
        assertEquals("香港01", NodeNameResolver.resolve(listOf("[vless]香港01", "香港01"), "香港01"))
    }

    @Test
    fun `唯一规范化候选时返回内核名字`() {
        val members = listOf("[vless]🇭🇰香港丨IEPLˣ³", "[vless]🇸🇬新加坡丨BGPˣ²")
        assertEquals("[vless]🇸🇬新加坡丨BGPˣ²", NodeNameResolver.resolve(members, "🇸🇬新加坡丨BGPˣ²"))
    }

    @Test
    fun `无候选或候选不唯一时返回空`() {
        assertNull(NodeNameResolver.resolve(listOf("[vless]香港01"), "日本01"))
        assertNull(NodeNameResolver.resolve(listOf("[vless]香港01", "[ss]香港01"), "🇭🇰香港01"))
        assertNull(NodeNameResolver.resolve(listOf("香港01"), ""))
    }

    @Test
    fun `序号类符号不参与归一化`() {
        assertNotEquals(NodeNameResolver.of("香港①"), NodeNameResolver.of("香港②"))
        assertNotEquals(NodeNameResolver.of("香港❶"), NodeNameResolver.of("香港❷"))
        assertNull(NodeNameResolver.resolve(listOf("香港①"), "香港②"))
        assertNull(NodeNameResolver.resolve(listOf("香港①", "香港❷"), "香港③"))
    }
}
