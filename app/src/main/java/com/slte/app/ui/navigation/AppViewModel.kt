package com.slte.app.ui.navigation

import androidx.lifecycle.ViewModel
import com.slte.app.domain.model.SessionNotice
import com.slte.app.domain.model.SessionState
import com.slte.app.domain.repository.AuthRepository
import com.slte.app.domain.repository.LocaleRepository
import com.slte.app.domain.service.SupportChat
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class AppViewModel
@Inject
constructor(
    authRepository: AuthRepository,
    val supportChat: SupportChat,
    val localeRepository: LocaleRepository,
) : ViewModel() {
    val sessionState: StateFlow<SessionState> = authRepository.sessionState

    val sessionNotices: SharedFlow<SessionNotice> = authRepository.sessionNotices

    val locale: StateFlow<Locale?> = localeRepository.locale
}
