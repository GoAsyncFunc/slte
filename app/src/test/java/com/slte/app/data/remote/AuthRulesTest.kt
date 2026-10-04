package com.slte.app.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRulesTest {
    @Test
    fun `认证接口路径判定`() {
        assertTrue(AuthRules.isAuthPath("/api/v1/user/info"))
        assertTrue(AuthRules.isAuthPath("/api/v1/user/subscribe"))
        assertFalse(AuthRules.isAuthPath("/api/v1/subscribe/token"))
        assertFalse(AuthRules.isAuthPath("/api/v1/orders"))
        assertFalse(AuthRules.isAuthPath(""))
    }

    @Test
    fun `响应体文本判定登录态失效`() {
        assertTrue(AuthRules.isAuthFailureBodyText("未登录"))
        assertTrue(AuthRules.isAuthFailureBodyText("登陆已过期"))
        assertTrue(AuthRules.isAuthFailureBodyText("登录已过期"))
        assertTrue(AuthRules.isAuthFailureBodyText("UNAUTHORIZED"))
        assertTrue(AuthRules.isAuthFailureBodyText("TOKEN EXPIRED"))
        assertTrue(AuthRules.isAuthFailureBodyText("{\"message\":\"invalid token\"}"))
        assertFalse(AuthRules.isAuthFailureBodyText(""))
        assertFalse(AuthRules.isAuthFailureBodyText(null))
        assertFalse(AuthRules.isAuthFailureBodyText("无套餐"))
        assertFalse(AuthRules.isAuthFailureBodyText("订阅已过期"))
    }

    @Test
    fun `403空响应体在认证接口也判为登录态失效`() {
        assertTrue(AuthRules.isAuthFailureBody(""))
        assertTrue(AuthRules.isAuthFailureBody(null))
        assertTrue(AuthRules.isAuthFailureBody("未登录"))
        assertTrue(AuthRules.isAuthFailureBody("{\"message\":\"invalid token\"}"))
        assertFalse(AuthRules.isAuthFailureBody("无套餐"))
    }

    @Test
    fun `注入token：有token处白名单且无Authorization头`() {
        assertTrue(
            AuthRules.decide("tok", isAllowedHost = true, hasAuthHeader = false, 200, false, isAuthPath = true).attachToken,
        )

        assertFalse(AuthRules.decide(null, true, false, 200, false, isAuthPath = true).attachToken)

        assertFalse(AuthRules.decide("tok", false, false, 200, false, isAuthPath = true).attachToken)

        assertFalse(AuthRules.decide("tok", true, true, 200, false, isAuthPath = true).attachToken)
    }

    @Test
    fun `认证接口401清会话`() {
        assertTrue(AuthRules.decide("tok", true, false, 401, false, isAuthPath = true).clearSession)

        assertTrue(AuthRules.decide("tok", true, false, 401, true, isAuthPath = true).clearSession)

        assertFalse(AuthRules.decide(null, true, false, 401, false, isAuthPath = true).clearSession)
    }

    @Test
    fun `非认证接口401不再踢登录`() {
        // 白名单内非 auth 路径（订阅 CDN/WAF 等）的 401 是接口级失败，不是登录态失效
        assertFalse(AuthRules.decide("tok", true, false, 401, false, isAuthPath = false).clearSession)
        assertFalse(AuthRules.decide("tok", true, false, 401, true, isAuthPath = false).clearSession)
    }

    @Test
    fun `403仅在认证接口且响应体含失效关键词时清会话`() {
        assertTrue(AuthRules.decide("tok", true, false, 403, isAuthFailureBody = true, isAuthPath = true).clearSession)

        assertFalse(AuthRules.decide("tok", true, false, 403, isAuthFailureBody = false, isAuthPath = true).clearSession)

        assertFalse(AuthRules.decide("tok", true, false, 403, isAuthFailureBody = true, isAuthPath = false).clearSession)
    }

    @Test
    fun `200及非失效状态不清会话`() {
        assertFalse(AuthRules.decide("tok", true, false, 200, false, isAuthPath = true).clearSession)
        assertFalse(AuthRules.decide("tok", true, false, 400, true, isAuthPath = true).clearSession)
        assertFalse(AuthRules.decide("tok", true, false, 500, false, isAuthPath = true).clearSession)
    }

    @Test
    fun `注入与清会话相互独立`() {
        val noToken = AuthRules.decide(null, true, false, 401, false, isAuthPath = true)
        assertFalse(noToken.attachToken)
        assertFalse(noToken.clearSession)

        val withHeader = AuthRules.decide("tok", true, true, 401, false, isAuthPath = true)
        assertFalse(withHeader.attachToken)
        assertTrue(withHeader.clearSession)

        val notAllowedHost = AuthRules.decide("tok", false, false, 401, false, isAuthPath = true)
        assertFalse(notAllowedHost.attachToken)
        assertTrue(notAllowedHost.clearSession)
    }
}
