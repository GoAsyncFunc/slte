package com.slte.app.domain.repository

import com.slte.app.domain.model.SessionNotice
import com.slte.app.domain.model.SessionState
import com.slte.app.domain.model.User
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/** Session state and lifecycle contract consumed by repositories and presentation-facing services. */
interface SessionRepository {
    val sessionState: StateFlow<SessionState>
    val logoutEvents: SharedFlow<Unit>
    val sessionNotices: SharedFlow<SessionNotice>

    fun setLoggedIn(user: User)
    fun updateUser(user: User)
    fun clearSession(expired: Boolean = false, notice: SessionNotice? = null)
}
