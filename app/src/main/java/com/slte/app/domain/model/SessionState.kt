package com.slte.app.domain.model

sealed interface SessionState {
    data object Loading : SessionState

    data object LoggedOut : SessionState

    data class LoggedIn(
        val user: User,
    ) : SessionState
}

enum class PasswordChangeOutcome {
    SESSION_RESTORED,
    SIGN_IN_REQUIRED,
}

enum class SessionNotice {
    EXPIRED,
    PASSWORD_CHANGED_REQUIRES_SIGN_IN,
}
