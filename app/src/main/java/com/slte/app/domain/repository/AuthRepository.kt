package com.slte.app.domain.repository

import com.slte.app.domain.model.EmailCodePurpose
import com.slte.app.domain.model.PasswordChangeOutcome
import com.slte.app.domain.model.RegisterConfig
import com.slte.app.domain.model.SessionNotice
import com.slte.app.domain.model.SessionState
import com.slte.app.domain.model.User
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val sessionState: StateFlow<SessionState>
    val sessionNotices: SharedFlow<SessionNotice>

    fun saveCredentials(email: String, password: String)
    fun clearCredentials()
    fun clearSavedPassword()
    fun savedEmail(): String?
    fun savedPassword(): String?

    suspend fun login(email: String, password: String): Result<User>
    suspend fun register(
        email: String,
        password: String,
        emailCode: String? = null,
        inviteCode: String? = null,
    ): Result<User>
    fun cachedRegisterConfig(): RegisterConfig?
    suspend fun fetchRegisterConfig(): Result<RegisterConfig>
    suspend fun forgotPassword(email: String, emailCode: String, newPassword: String): Result<Unit>
    suspend fun sendEmailCode(
        email: String,
        purpose: EmailCodePurpose = EmailCodePurpose.FORGOT_PASSWORD,
    ): Result<Unit>
    suspend fun updateRemindExpire(enabled: Boolean): Result<Unit>
    suspend fun updateRemindTraffic(enabled: Boolean): Result<Unit>
    suspend fun changePassword(oldPassword: String, newPassword: String): Result<PasswordChangeOutcome>
    fun logout()
}
