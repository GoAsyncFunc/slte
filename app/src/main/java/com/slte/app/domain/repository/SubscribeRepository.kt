package com.slte.app.domain.repository

import com.slte.app.domain.model.Notice
import com.slte.app.domain.model.SubscribeInfo
import com.slte.app.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface SubscribeRepository {
    val subscribeInfo: StateFlow<SubscribeInfo?>

    suspend fun fetchSubscribeInfo(force: Boolean = false): Result<SubscribeInfo>
    fun getCachedSubscribeInfo(): SubscribeInfo?
    fun getSubscriptionUpdatedAt(): Long
    suspend fun fetchUserInfo(force: Boolean = false): Result<User>
    fun getCachedUserInfo(): User?
    fun updateCachedUserInfo(user: User)
    suspend fun fetchNotices(): Result<List<Notice>>
    fun invalidateCache()
}
