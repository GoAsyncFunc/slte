package com.slte.app.kernel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.yaml.snakeyaml.Yaml

class SanitizerProxyNamesTest {

    private val domains = listOf("example.com")

    private val duplicated =
        """
        |mixed-port: 7890
        |proxies:
        |  - name: "jp-01"
        |    type: vless
        |    server: 1.1.1.1
        |    port: 443
        |  - name: jp-01
        |    type: vless
        |    server: 2.2.2.2
        |    port: 443
        |proxy-groups:
        |  - name: 节点选择
        |    type: select
        |    proxies:
        |      - jp-01
        |      - DIRECT
        |rules:
        |  - MATCH,节点选择
        """.trimMargin()

    @Suppress("UNCHECKED_CAST")
    private fun parse(text: String): Map<String, Any?> = Yaml().load(text) as Map<String, Any?>

    @Suppress("UNCHECKED_CAST")
    private fun proxyNames(text: String): List<String> {
        val proxies = parse(text)["proxies"] as List<Map<String, Any?>>
        return proxies.map { it["name"] as String }
    }

    @Suppress("UNCHECKED_CAST")
    private fun groupMembers(
        text: String,
        group: String,
    ): List<String> {
        val groups = parse(text)["proxy-groups"] as List<Map<String, Any?>>
        val target = groups.first { it["name"] == group }
        return target["proxies"] as List<String>
    }

    @Test
    fun `重名节点自动改名且分组补齐新名字`() {
        val cleaned = SubscriptionSanitizer.sanitize(duplicated, domains)

        val names = proxyNames(cleaned)
        assertEquals(2, names.size)
        assertEquals(names.size, names.distinct().size)
        assertTrue("首个节点保留原名", names.contains("jp-01"))
        assertTrue("重名节点改名后可区分", names.contains("jp-01 #2"))

        val members = groupMembers(cleaned, "节点选择")
        assertTrue("分组仍引用原名", members.contains("jp-01"))
        assertTrue("改名后的节点也进入分组", members.contains("jp-01 #2"))
        assertTrue("分组内不重复", members.size == members.distinct().size)
        assertTrue(members.contains("DIRECT"))
    }

    @Test
    fun `清洗结果不再有重名且可被内核加载`() {
        val cleaned = SubscriptionSanitizer.sanitize(duplicated, domains)

        assertTrue(SubscriptionSanitizer.isKernelLoadable(cleaned))
        assertTrue(SanitizerProxyNames.duplicateNames(cleaned).isEmpty())
        assertTrue(SanitizerProxyNames.duplicateNames(duplicated).isNotEmpty())
    }

    @Test
    fun `无重名时内容逐字节不变`() {
        val single =
            listOf(
                "proxies:",
                "  - name: \"jp-01\"",
                "    type: ss",
                "    server: 1.2.3.4",
                "    port: 8388",
            )
        val lines = single.toMutableList()

        assertEquals(0, SanitizerProxyNames.dedupe(lines))
        assertEquals(single, lines)
    }

    @Test
    fun `重复执行结果一致且幂等`() {
        val once = SubscriptionSanitizer.sanitize(duplicated, domains)
        val twice = SubscriptionSanitizer.sanitize(once, domains)

        assertEquals(once, twice)
        assertTrue(SanitizerProxyNames.duplicateNames(twice).isEmpty())
    }

    @Test
    fun `flow 风格条目也能识别重名`() {
        val flow =
            """
            |proxies:
            |  - {name: "same", type: ss, server: 1.1.1.1, port: 1}
            |  - {name: "same", type: ss, server: 2.2.2.2, port: 2}
            |rules:
            |  - MATCH,DIRECT
            """.trimMargin()

        val cleaned = SubscriptionSanitizer.sanitize(flow, domains)
        val names = proxyNames(cleaned)

        assertEquals(2, names.size)
        assertEquals(names.size, names.distinct().size)
    }

    @Test
    fun `flow 风格订阅的分组里也会补上改名节点`() {
        val flow =
            """
            |proxies:
            |    - { name: '[vless]jp-01', type: vless, server: a.example.com, port: '443' }
            |    - { name: '[vless]jp-01', type: vless, server: b.example.com, port: '443' }
            |proxy-groups:
            |    - { name: 节点选择, type: select, proxies: [自动选择, DIRECT, '[vless]jp-01'] }
            |rules:
            |    - MATCH,节点选择
            """.trimMargin()

        val cleaned = SubscriptionSanitizer.sanitize(flow, domains)
        val names = proxyNames(cleaned)
        val members = groupMembers(cleaned, "节点选择")

        assertEquals(2, names.size)
        assertEquals(names.size, names.distinct().size)
        assertTrue(members.contains("[vless]jp-01"))
        assertTrue("改名后的节点应进入分组", members.contains("[vless]jp-01 #2"))
        assertTrue(members.contains("DIRECT"))
        assertEquals(members.size, members.distinct().size)
        assertTrue(SubscriptionSanitizer.isKernelLoadable(cleaned))
        assertEquals("再次清洗应保持不变", cleaned, SubscriptionSanitizer.sanitize(cleaned, domains))
    }

    @Test
    fun `与订阅里已有的后缀名字不冲突`() {
        val preset =
            """
            |proxies:
            |  - name: "jp-01"
            |    type: ss
            |  - name: "jp-01"
            |    type: ss
            |  - name: "jp-01 #2"
            |    type: ss
            |rules:
            |  - MATCH,DIRECT
            """.trimMargin()

        val cleaned = SubscriptionSanitizer.sanitize(preset, domains)
        val names = proxyNames(cleaned)

        assertEquals(3, names.size)
        assertEquals(names.size, names.distinct().size)
        assertTrue(names.contains("jp-01 #2"))
    }

    @Test
    fun `已带后缀的节点会被补进引用原名的分组`() {
        val preset =
            """
            |proxies:
            |  - name: "jp-01"
            |    type: ss
            |  - name: "jp-01 #2"
            |    type: ss
            |proxy-groups:
            |  - name: 节点选择
            |    type: select
            |    proxies:
            |      - jp-01
            |      - DIRECT
            |rules:
            |  - MATCH,节点选择
            """.trimMargin()

        val cleaned = SubscriptionSanitizer.sanitize(preset, domains)
        val members = groupMembers(cleaned, "节点选择")

        assertTrue(members.contains("jp-01"))
        assertTrue("已有的 #2 节点也应补进分组", members.contains("jp-01 #2"))
        assertEquals(members.size, members.distinct().size)
    }

    @Test
    fun `缺少节点名或节点列表时判定不可加载`() {
        val missingName =
            """
            |proxies:
            |  - type: ss
            |    server: 1.1.1.1
            |    port: 1
            |rules:
            |  - MATCH,DIRECT
            """.trimMargin()

        assertFalse(SubscriptionSanitizer.isKernelLoadable(missingName))
        assertFalse(SubscriptionSanitizer.isKernelLoadable("proxies:\nrules:\n  - MATCH,DIRECT\n"))
        assertFalse(SubscriptionSanitizer.isKernelLoadable(""))
        assertTrue(SubscriptionSanitizer.isKernelLoadable(SubscriptionSanitizer.sanitize(duplicated, domains)))
    }

    @Test
    fun `只有 provider 的订阅不会被误判为不可加载`() {
        val providerOnly =
            """
            |proxy-providers:
            |  airport:
            |    type: http
            |    url: "https://example.com/sub"
            |    interval: 3600
            |rules:
            |  - MATCH,DIRECT
            """.trimMargin()
        val emptyInlineWithProvider =
            """
            |proxies: []
            |proxy-providers:
            |  airport:
            |    type: http
            |    url: "https://example.com/sub"
            |rules:
            |  - MATCH,DIRECT
            """.trimMargin()

        assertTrue(SubscriptionSanitizer.isKernelLoadable(providerOnly))
        assertTrue(SubscriptionSanitizer.isKernelLoadable(emptyInlineWithProvider))
        assertTrue(SubscriptionSanitizer.isKernelLoadable(SubscriptionSanitizer.sanitize(providerOnly, domains)))
    }

    @Test
    fun `三条同名节点依次改名且名字都唯一`() {
        val triple =
            """
            |proxies:
            |  - name: "jp-01"
            |    type: ss
            |  - name: "jp-01"
            |    type: ss
            |  - name: "jp-01"
            |    type: ss
            |rules:
            |  - MATCH,DIRECT
            """.trimMargin()

        val names = proxyNames(SubscriptionSanitizer.sanitize(triple, domains))

        assertEquals(3, names.size)
        assertEquals(names.size, names.distinct().size)
        assertEquals(listOf("jp-01", "jp-01 #2", "jp-01 #3"), names)
    }

    @Test
    fun `引号混用与不同协议的同名节点都能处理`() {
        val mixed =
            """
            |proxies:
            |  - { name: "jp-01", type: vless, server: a.example.com, port: 443 }
            |  - { name: 'jp-01', type: ss, server: b.example.com, port: 8388 }
            |  - { name: jp-01, type: trojan, server: c.example.com, port: 8443 }
            |  - { name: '[ss]jp-02', type: ss, server: d.example.com, port: 8388 }
            |rules:
            |  - MATCH,DIRECT
            """.trimMargin()

        val names = proxyNames(SubscriptionSanitizer.sanitize(mixed, domains))

        assertEquals(4, names.size)
        assertEquals(names.size, names.distinct().size)
        assertTrue(names.contains("jp-01"))
        assertTrue(names.contains("jp-01 #2"))
        assertTrue(names.contains("jp-01 #3"))
        assertTrue("协议不同但显示名相同也应各自保留", names.contains("[ss]jp-02"))
    }

    @Test
    fun `CRLF 与 BOM 的订阅同样能去重`() {
        val crlf =
            listOf(
                "proxies:",
                "  - name: \"jp-01\"",
                "    type: ss",
                "  - name: \"jp-01\"",
                "    type: ss",
                "rules:",
                "  - MATCH,DIRECT",
                "",
            ).joinToString("\r\n")
        val withBom = "\uFEFF$crlf"

        listOf(crlf, withBom).forEach { input ->
            val cleaned = SubscriptionSanitizer.sanitize(input, domains)
            val names = proxyNames(cleaned)
            assertEquals(2, names.size)
            assertEquals(names.size, names.distinct().size)
            assertTrue(SubscriptionSanitizer.isKernelLoadable(cleaned))
        }
    }

    @Test
    fun `名字含逗号或井号时分组引用仍然正确`() {
        val tricky =
            """
            |proxies:
            |    - { name: 'jp, 01', type: ss, server: a.example.com, port: 8388 }
            |    - { name: 'jp, 01', type: ss, server: b.example.com, port: 8388 }
            |    - { name: 'jp #01', type: ss, server: c.example.com, port: 8388 }
            |proxy-groups:
            |    - { name: 节点选择, type: select, proxies: [DIRECT, 'jp, 01', 'jp #01'] }
            |rules:
            |  - MATCH,节点选择
            """.trimMargin()

        val cleaned = SubscriptionSanitizer.sanitize(tricky, domains)
        val names = proxyNames(cleaned)
        val members = groupMembers(cleaned, "节点选择")

        assertEquals(3, names.size)
        assertEquals(names.size, names.distinct().size)
        assertTrue(names.contains("jp, 01 #2"))
        assertTrue(members.contains("jp, 01"))
        assertTrue(members.contains("jp, 01 #2"))
        assertTrue(members.contains("jp #01"))
        assertEquals(members.size, members.distinct().size)
    }

    @Test
    fun `多组重名时节点数量不变且结构完整`() {
        val multi =
            """
            |mixed-port: 7890
            |proxies:
            |    - { name: 'jp, 01', type: vless, server: a.example.com, port: 443 }
            |    - { name: 'sg, 01', type: vless, server: b.example.com, port: 443 }
            |    - { name: 'jp, 01', type: ss, server: c.example.com, port: 8388 }
            |    - { name: 'sg, 01', type: ss, server: d.example.com, port: 8388 }
            |    - { name: 'us-01', type: trojan, server: e.example.com, port: 8443 }
            |proxy-groups:
            |    - { name: 节点选择, type: select, proxies: [自动选择, DIRECT, 'jp, 01', 'sg, 01', 'us-01'] }
            |    - { name: 自动选择, type: url-test, proxies: ['jp, 01', 'sg, 01', 'us-01'] }
            |rules:
            |  - MATCH,节点选择
            """.trimMargin()

        val cleaned = SubscriptionSanitizer.sanitize(multi, domains)
        val names = proxyNames(cleaned)
        val selectMembers = groupMembers(cleaned, "节点选择")
        val autoMembers = groupMembers(cleaned, "自动选择")

        assertEquals("去重不得增删节点", 5, names.size)
        assertEquals(names.size, names.distinct().size)
        assertTrue(SubscriptionSanitizer.isKernelLoadable(cleaned))
        assertTrue(selectMembers.contains("jp, 01 #2"))
        assertTrue(selectMembers.contains("sg, 01 #2"))
        assertTrue(autoMembers.contains("jp, 01 #2"))
        assertTrue(autoMembers.contains("sg, 01 #2"))
        assertEquals(selectMembers.size, selectMembers.distinct().size)
        assertEquals(autoMembers.size, autoMembers.distinct().size)
    }

    private val commentedNames =
        """
        |proxies:
        |  - name: jp-01 # 主力
        |    type: vless
        |    server: 1.1.1.1
        |    port: 443
        |  - name: jp-01 # 备用
        |    type: vless
        |    server: 2.2.2.2
        |    port: 443
        |proxy-groups:
        |  - name: 节点选择
        |    type: select
        |    proxies:
        |      - jp-01 # 主力
        |      - DIRECT
        |rules:
        |  - MATCH,节点选择
        """.trimMargin()

    @Test
    fun `行尾注释不参与重名判定`() {
        assertEquals(
            "同一名字带不同注释也算重名",
            listOf("jp-01"),
            SanitizerProxyNames.duplicateNames(commentedNames),
        )
        assertFalse("重名配置不能被判定为可加载", SubscriptionSanitizer.isKernelLoadable(commentedNames))
    }

    @Test
    fun `带注释的重名节点改名后注释保留且分组补齐新名字`() {
        val cleaned = SubscriptionSanitizer.sanitize(commentedNames, domains)
        val names = proxyNames(cleaned)
        val members = groupMembers(cleaned, "节点选择")

        assertEquals(2, names.size)
        assertEquals("改名后不得留下重名", names.size, names.distinct().size)
        assertTrue("注释必须留在行尾，不能被当成名字的一部分", cleaned.contains("# 备用"))
        assertTrue("注释前的空格不能被吃掉（否则 `#` 会粘进引号）", cleaned.contains("\"jp-01 #2\" # 备用"))
        assertTrue("改名后的节点要补进分组", members.contains("jp-01 #2"))
        assertTrue(members.contains("jp-01"))
        assertEquals(members.size, members.distinct().size)
        assertTrue(SubscriptionSanitizer.isKernelLoadable(cleaned))
    }

    private val quotedWithComments =
        """
        |proxies:
        |  - name: "jp-02" # a
        |    type: vless
        |    server: 1.1.1.1
        |    port: 443
        |  - name: "jp-02" # b
        |    type: vless
        |    server: 2.2.2.2
        |    port: 443
        |proxy-groups:
        |  - name: 节点选择
        |    type: select
        |    proxies:
        |      - "jp-02" # a
        |      - DIRECT
        |rules:
        |  - MATCH,节点选择
        """.trimMargin()

    @Test
    fun `引号值后面的注释不属于名字`() {
        assertEquals(
            "引号值应以配对引号收尾，注释不参与比较",
            listOf("jp-02"),
            SanitizerProxyNames.duplicateNames(quotedWithComments),
        )

        val cleaned = SubscriptionSanitizer.sanitize(quotedWithComments, domains)
        val names = proxyNames(cleaned)
        val members = groupMembers(cleaned, "节点选择")

        assertEquals(2, names.size)
        assertEquals(names.size, names.distinct().size)
        assertTrue(cleaned.contains("\"jp-02 #2\" # b"))
        assertTrue(members.contains("jp-02 #2"))
        assertTrue(SubscriptionSanitizer.isKernelLoadable(cleaned))
    }

    @Test
    fun `名字里的井号只要前面没空白就不算注释`() {
        val tight =
            """
            |proxies:
            |  - name: hk#01
            |    type: vless
            |    server: 1.1.1.1
            |    port: 443
            |proxy-groups:
            |  - name: 节点选择
            |    type: select
            |    proxies:
            |      - hk#01
            |      - DIRECT
            |rules:
            |  - MATCH,节点选择
            """.trimMargin()

        assertEquals(listOf("hk#01"), SanitizerProxyNames.names(tight))
        assertEquals("紧贴的井号属于名字", 0, SanitizerProxyNames.dedupe(tight.lines().toMutableList()))
    }
}
