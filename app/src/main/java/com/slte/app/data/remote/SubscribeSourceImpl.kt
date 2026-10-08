package com.slte.app.data.remote

import com.slte.app.data.local.SessionStore
import com.slte.app.data.remote.api.AuthApi
import com.slte.app.data.remote.config.RemoteConfig
import com.slte.app.kernel.SubscribeSource
import com.slte.app.utils.AppLog
import com.slte.app.utils.sanitizeLog
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.ResponseBody

@Singleton
class SubscribeSourceImpl
@Inject
constructor(
    private val sessionStore: SessionStore,
    private val authApi: AuthApi,
    private val remoteConfig: RemoteConfig,
) : SubscribeSource {
    override fun getEmail(): String? = sessionStore.getEmail()

    override suspend fun fetchSubscribeYaml(): ResponseBody? {
        val url =
            accountSubscribeUrl()
                ?: sessionStore.getSubscribeToken()?.let { subscribeFetchUrl(remoteConfig.data.apiBaseUrl, it) }
                ?: return null
        // host 可能含 userinfo（https://user:pass@host/...），落日志前先脱敏
        val host = sanitizeLog(url.substringAfter("://").substringBefore('/'))
        val body =
            try {
                authApi.fetchSubscribeYaml(url)
            } catch (e: Exception) {
                // 只记主机与异常类型：URL 里带订阅令牌，不能整条落日志
                AppLog.w("SLTE-Subscribe", "订阅拉取失败 host=$host: ${sanitizeLog(e.javaClass.simpleName)}")
                throw e
            }
        // contentLength() 对 chunked / 未知长度响应返回 -1，同样视为拿不到内容
        val length = body?.contentLength() ?: -1L
        if (body == null || length == 0L) {
            // chunked 响应的 contentLength 为 -1，不能据此判断响应体为空。
            AppLog.w("SLTE-Subscribe", "订阅响应为空 host=$host length=$length")
        }
        return body
    }

    private fun accountSubscribeUrl(): String? {
        val raw = sessionStore.getSubscribeUrl()?.trim().orEmpty()
        if (raw.startsWith("https://")) return raw
        if (raw.isNotEmpty()) {
            AppLog.w("SLTE-Subscribe", "订阅地址被拒（须 https）: ${sanitizeLog(raw)}")
        }
        return null
    }

    override fun saveSubscriptionUpdatedAt() = sessionStore.saveSubscriptionUpdatedAt()
}
