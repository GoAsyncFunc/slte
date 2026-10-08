package com.slte.app.data.remote.config

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AllowedHostsTest {
    @Test
    fun `白名单域及其子域放行`() {
        assertTrue(AllowedHosts.isAllowedHost("example.com"))
        assertTrue(AllowedHosts.isAllowedHost("api.example.com"))
        assertTrue(AllowedHosts.isAllowedHost("a.b.example.com"))
        assertTrue(AllowedHosts.isAllowedHost("API.EXAMPLE.COM"))
    }

    @Test
    fun `非白名单域被拒绝_含后缀伪装`() {
        assertFalse(AllowedHosts.isAllowedHost("attacker.tld"))
        assertFalse(AllowedHosts.isAllowedHost("notexample.com"))
        assertFalse(AllowedHosts.isAllowedHost("example.com.attacker.tld"))
        assertFalse(AllowedHosts.isAllowedHost(""))
        assertFalse(AllowedHosts.isAllowedHost(null))
    }

    @Test
    fun `URL校验由调用方拆解为host后走isAllowedHost`() {
        // isAllowedUrl 已删除：URL 级校验（https + host 白名单）由 SubscribeSourceImpl /
        // RemoteConfigParser 在拆出 host 后各自调用 isAllowedHost，此处保留语义回归
        val urlHost = { url: String? ->
            url?.removePrefix("https://")?.removePrefix("http://")?.substringBefore('/')?.substringBefore(':')
        }
        assertTrue(AllowedHosts.isAllowedHost(urlHost("https://api.example.com/api/v1/client/subscribe?token=x")))
        assertFalse(AllowedHosts.isAllowedHost(urlHost("http://attacker.tld/subscribe")))
    }
}
