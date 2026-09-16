package com.slte.app.data.remote

import com.slte.app.data.local.SessionStore
import com.slte.app.data.remote.api.AuthApi
import com.slte.app.data.remote.config.AllowedHosts
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
            trustedSubscribeUrl()
                ?: sessionStore.getSubscribeToken()?.let { subscribeFetchUrl(remoteConfig.data.apiBaseUrl, it) }
                ?: return null
        return authApi.fetchSubscribeYaml(url)
    }

    private fun trustedSubscribeUrl(): String? {
        val raw = sessionStore.getSubscribeUrl()?.takeIf { it.isNotBlank() } ?: return null
        if (AllowedHosts.isAllowedUrl(raw)) return raw.trim()
        AppLog.w(
            "SLTE-Subscribe",
            "订阅地址被拒（须 https 且主机在编译期白名单内）: ${sanitizeLog(raw)}",
        )
        return null
    }

    override fun saveSubscriptionUpdatedAt() = sessionStore.saveSubscriptionUpdatedAt()
}
