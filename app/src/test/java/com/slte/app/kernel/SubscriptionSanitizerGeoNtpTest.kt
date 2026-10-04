package com.slte.app.kernel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * geox-url / ntp 顶层键的清洗边界：
 * 订阅不得指定 geo 库下载源（供应链投毒/SSRF 面）与 NTP 服务器（时间源劫持），
 * 清洗链整块丢弃、内核 process.go patchGeneral 同键兜底（回退内置默认）。
 */
class SubscriptionSanitizerGeoNtpTest {

    private val base =
        """
        |mixed-port: 7890
        |proxies:
        |  - name: "node-1"
        |    type: ss
        |    server: 1.2.3.4
        |    port: 8388
        |    cipher: aes-128-gcm
        |    password: "pw"
        |rules:
        |  - MATCH,DIRECT
        """.trimMargin()

    @Test
    fun `geox-url 块被整块丢弃`() {
        val yaml =
            """
            |$base
            |geox-url:
            |  geoip: "https://evil.example/geoip.dat"
            |  mmdb: "https://evil.example/geoip.metadb"
            |  asn: "https://evil.example/asn.mmdb"
            |  geosite: "https://evil.example/geosite.dat"
            """.trimMargin()

        val out = SubscriptionSanitizer.sanitize(yaml, listOf("example.com"))

        assertTrue("清洗应成功输出", out.isNotBlank())
        assertFalse("geox-url 键应被丢弃", out.contains("geox-url"))
        assertFalse("投毒下载源不应残留", out.contains("evil.example"))
    }

    @Test
    fun `ntp 块被整块丢弃`() {
        val yaml =
            """
            |$base
            |ntp:
            |  enable: true
            |  server: "ntp.evil.example"
            |  port: 123
            |  interval: 30
            |  dialer-proxy: "node-1"
            |  write-to-system: true
            """.trimMargin()

        val out = SubscriptionSanitizer.sanitize(yaml, listOf("example.com"))

        assertTrue("清洗应成功输出", out.isNotBlank())
        assertFalse("ntp 键应被丢弃", out.contains("ntp:"))
        assertFalse("劫持的时间源不应残留", out.contains("ntp.evil.example"))
    }

    @Test
    fun `行内混淆写法同样被丢弃`() {
        // flow 风格 + 引号键，绕不过顶层键判定
        val yaml =
            """
            |$base
            |"geox-url": {geoip: "https://evil.example/a.dat"}
            |'ntp': {enable: true, server: "ntp.evil.example"}
            """.trimMargin()

        val out = SubscriptionSanitizer.sanitize(yaml, listOf("example.com"))

        assertTrue("清洗应成功输出", out.isNotBlank())
        assertFalse(out.contains("evil.example"))
    }

    @Test
    fun `正常订阅不含这些键时输出不受影响`() {
        val out = SubscriptionSanitizer.sanitize(base, listOf("example.com"))

        assertTrue("正常订阅应完整保留", out.isNotBlank())
        assertTrue(out.contains("node-1"))
        assertFalse(out.contains("geox-url"))
        assertFalse(out.contains("ntp:"))
    }
}
