package com.slte.app.data.repository

import com.slte.app.BuildConfig
import com.slte.app.data.local.SessionStore
import com.slte.app.data.remote.api.AuthApi
import com.slte.app.domain.model.ServerNode
import com.slte.app.domain.model.SpecialNodeSnapshot
import com.slte.app.domain.repository.ServerRepository as ServerRepositoryContract
import com.slte.app.utils.AppLog
import com.slte.app.utils.sanitizeLog
import javax.inject.Inject
import javax.inject.Singleton

internal object CachePolicy {
    const val SUBSCRIBE_INFO_TTL_MS = 30_000L

    const val USER_INFO_TTL_MS = 30_000L

    const val SERVER_NODES_TTL_MS = 30 * 60_000L

    /** 节点延迟缓存：只用来先把界面填上，超过这个时间就重新测。 */
    const val LATENCY_TTL_MS = 6 * 60 * 60_000L

    fun isFresh(
        cachedAtMs: Long,
        nowMs: Long,
        ttlMs: Long,
    ): Boolean = cachedAtMs > 0L && nowMs - cachedAtMs < ttlMs
}

@Singleton
class ServerRepositoryImpl
@Inject
constructor(
    private val authApi: AuthApi,
    private val sessionStore: SessionStore,
) : ServerRepositoryContract {

    override suspend fun fetchServers(force: Boolean): Result<List<ServerNode>> = runApi {
        val cached = sessionStore.getServerNodes()
        val fetchedAt = sessionStore.getServerNodesFetchedAt()
        if (!force && cached != null && CachePolicy.isFresh(fetchedAt, System.currentTimeMillis(), CachePolicy.SERVER_NODES_TTL_MS)) {
            debugLog("fetchServers: 缓存未过期，直接返回 ${cached.size} 个节点")
            return@runApi cached
        }

        debugLog("fetchServers: 开始请求 API...")
        val nodes = authApi.fetchServers()
        debugLog("fetchServers: 返回 ${nodes.size} 个节点")
        if (nodes.isNotEmpty()) {
            sessionStore.saveServerNodes(nodes)
        }
        nodes
    }.recoverCatching { e ->
        if (force) throw e
        val cached = sessionStore.getServerNodes()
        if (cached != null) {
            AppLog.w("SLTE-Repo", "fetchServers: 网络失败,使用本地缓存 ${cached.size} 个节点: ${sanitizeLog(e.message ?: "")}")
            cached
        } else {
            throw e
        }
    }

    override fun getCachedServers(): List<ServerNode>? = sessionStore.getServerNodes()

    override fun invalidateCache() {
        sessionStore.clearServerNodes()
    }

    /** 自动选择/故障转移最近一次成员快照：内核离线时垫显示，实时值到达后覆盖。 */
    override fun saveAutoNodeSnapshot(snapshot: SpecialNodeSnapshot) = sessionStore.saveAutoNodeSnapshot(snapshot)

    override fun getAutoNodeSnapshot(): SpecialNodeSnapshot? = sessionStore.getAutoNodeSnapshot()

    override fun saveFallbackNodeSnapshot(snapshot: SpecialNodeSnapshot) = sessionStore.saveFallbackNodeSnapshot(snapshot)

    override fun getFallbackNodeSnapshot(): SpecialNodeSnapshot? = sessionStore.getFallbackNodeSnapshot()

    override fun invalidateSpecialNodeSnapshots() = sessionStore.clearSpecialNodeSnapshots()

    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Repo", message)
        }
    }
}
