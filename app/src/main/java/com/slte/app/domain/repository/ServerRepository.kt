package com.slte.app.domain.repository

import com.slte.app.domain.model.ServerNode
import com.slte.app.domain.model.SpecialNodeSnapshot

interface ServerRepository {
    suspend fun fetchServers(force: Boolean = false): Result<List<ServerNode>>
    fun getCachedServers(): List<ServerNode>?
    fun invalidateCache()
    fun saveAutoNodeSnapshot(snapshot: SpecialNodeSnapshot)
    fun getAutoNodeSnapshot(): SpecialNodeSnapshot?
    fun saveFallbackNodeSnapshot(snapshot: SpecialNodeSnapshot)
    fun getFallbackNodeSnapshot(): SpecialNodeSnapshot?
    fun invalidateSpecialNodeSnapshots()
}
