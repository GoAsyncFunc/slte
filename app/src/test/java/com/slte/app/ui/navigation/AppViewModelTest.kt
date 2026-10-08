package com.slte.app.ui.navigation

import com.slte.app.domain.model.SessionNotice
import com.slte.app.domain.model.SessionState
import com.slte.app.domain.repository.AuthRepository
import com.slte.app.domain.repository.LocaleRepository
import com.slte.app.domain.service.SupportChat
import io.mockk.every
import io.mockk.mockk
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Test

class AppViewModelTest {
    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val supportChat = mockk<SupportChat>(relaxed = true)
    private val localeStore = mockk<LocaleRepository>(relaxed = true)

    @Test
    fun `会话状态与语言偏好直接透传给界面`() {
        every { authRepository.sessionState } returns MutableStateFlow(SessionState.LoggedOut)
        every { authRepository.sessionNotices } returns MutableSharedFlow<SessionNotice>()
        every { localeStore.locale } returns MutableStateFlow(Locale.SIMPLIFIED_CHINESE)

        val vm = AppViewModel(authRepository, supportChat, localeStore)

        assertEquals(SessionState.LoggedOut, vm.sessionState.value)
        assertEquals(Locale.SIMPLIFIED_CHINESE, vm.locale.value)
    }
}
