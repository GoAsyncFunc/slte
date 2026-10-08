package com.slte.app.ui.screen.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slte.app.R
import com.slte.app.domain.model.EmailCodePurpose
import com.slte.app.domain.model.EmailWhitelist
import com.slte.app.domain.repository.AuthRepository
import com.slte.app.domain.usecase.CountdownUseCase
import com.slte.app.utils.EmailInput
import com.slte.app.utils.ErrorMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 白名单启用时邮箱 = 本地部分 + 选中的后缀；未启用时按用户原样输入。 */
private fun composeEmail(
    localPart: String,
    suffix: String?,
    whitelist: EmailWhitelist,
): String = if (whitelist.isEnabled && suffix != null) "$localPart@$suffix" else localPart

sealed interface RegisterUiState {
    data class Form(
        val email: String = "",
        val password: String = "",
        val verificationCode: String = "",
        val inviteCode: String = "",
        val emailVerifyEnabled: Boolean = false,
        val inviteForceEnabled: Boolean = false,
        /** 后端下发的邮箱后缀白名单；为空表示不限制。 */
        val emailWhitelist: EmailWhitelist = EmailWhitelist.None,
        /** 白名单启用时当前选中的后缀（邮箱 = 本地部分 + @ + 该后缀）。 */
        val emailSuffix: String? = null,
    ) : RegisterUiState

    data class SendingCode(
        val form: Form,
    ) : RegisterUiState

    data class Countdown(
        val form: Form,
        val seconds: Int,
    ) : RegisterUiState

    data class Registering(
        val form: Form,
    ) : RegisterUiState

    data class RegisterSuccess(
        val form: Form,
    ) : RegisterUiState

    data class Error(
        val form: Form,
        val messageRes: Int,
    ) : RegisterUiState
}

@HiltViewModel
class RegisterViewModel
@Inject
constructor(
    private val authRepository: AuthRepository,
    private val countdownUseCase: CountdownUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Form())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private var registerJob: Job? = null

    private fun currentForm() = when (val s = _uiState.value) {
        is RegisterUiState.Form -> s
        is RegisterUiState.SendingCode -> s.form
        is RegisterUiState.Countdown -> s.form
        is RegisterUiState.Registering -> s.form
        is RegisterUiState.RegisterSuccess -> s.form
        is RegisterUiState.Error -> s.form
    }

    private val isLoadingOrRegistering: Boolean
        get() =
            _uiState.value is RegisterUiState.SendingCode ||
                _uiState.value is RegisterUiState.Registering

    private val isCountingDown: Boolean
        get() = _uiState.value is RegisterUiState.Countdown

    fun initConfig(
        emailVerifyEnabled: Boolean,
        inviteForceEnabled: Boolean,
        emailWhitelist: EmailWhitelist = authRepository.cachedRegisterConfig()?.emailWhitelist ?: EmailWhitelist.None,
    ) {
        val f = currentForm()
        if (f.emailVerifyEnabled == emailVerifyEnabled &&
            f.inviteForceEnabled == inviteForceEnabled &&
            f.emailWhitelist == emailWhitelist
        ) {
            return
        }
        // 白名单启用时后缀由列表决定：没选过、或原来选的已不在列表里，就回到默认后缀
        val suffix =
            if (emailWhitelist.isEnabled) {
                f.emailSuffix?.takeIf { it in emailWhitelist.suffixes } ?: emailWhitelist.defaultSuffix
            } else {
                null
            }
        _uiState.value =
            RegisterUiState.Form(
                email = composeEmail(EmailInput.localPart(f.email), suffix, emailWhitelist),
                password = f.password,
                verificationCode = f.verificationCode,
                inviteCode = f.inviteCode,
                emailVerifyEnabled = emailVerifyEnabled,
                inviteForceEnabled = inviteForceEnabled,
                emailWhitelist = emailWhitelist,
                emailSuffix = suffix,
            )
    }

    private fun updateForm(transform: (RegisterUiState.Form) -> RegisterUiState.Form) {
        when (val s = _uiState.value) {
            is RegisterUiState.Form -> _uiState.value = transform(s)
            is RegisterUiState.SendingCode -> _uiState.value = RegisterUiState.SendingCode(transform(s.form))
            is RegisterUiState.Countdown -> _uiState.value = RegisterUiState.Countdown(transform(s.form), s.seconds)
            is RegisterUiState.Registering -> _uiState.value = RegisterUiState.Registering(transform(s.form))
            is RegisterUiState.RegisterSuccess -> _uiState.value = RegisterUiState.RegisterSuccess(transform(s.form))
            is RegisterUiState.Error -> _uiState.value = RegisterUiState.Error(transform(s.form), s.messageRes)
        }
    }

    /** 白名单启用时输入框只收 `@` 前面的部分，后缀由 [onEmailSuffixChange] 选择。 */
    fun onEmailChange(input: String) = updateForm { form ->
        if (form.emailWhitelist.isEnabled) {
            form.copy(email = composeEmail(EmailInput.localPart(input), form.emailSuffix, form.emailWhitelist))
        } else {
            form.copy(email = input)
        }
    }

    /** 切换邮箱后缀：只接受后端下发的后缀，选完直接把完整邮箱拼好。 */
    fun onEmailSuffixChange(suffix: String) = updateForm { form ->
        if (suffix in form.emailWhitelist.suffixes) {
            form.copy(
                emailSuffix = suffix,
                email = composeEmail(EmailInput.localPart(form.email), suffix, form.emailWhitelist),
            )
        } else {
            form
        }
    }

    fun onPasswordChange(password: String) = updateForm { it.copy(password = password) }

    fun onCodeChange(code: String) = updateForm { it.copy(verificationCode = code) }

    fun onInviteCodeChange(code: String) = updateForm { it.copy(inviteCode = code) }

    fun dismissError() {
        val f = currentForm()
        _uiState.value =
            RegisterUiState.Form(
                email = f.email,
                password = f.password,
                verificationCode = f.verificationCode,
                inviteCode = f.inviteCode,
                emailVerifyEnabled = f.emailVerifyEnabled,
                inviteForceEnabled = f.inviteForceEnabled,
                emailWhitelist = f.emailWhitelist,
                emailSuffix = f.emailSuffix,
            )
    }

    fun sendVerificationCode() {
        if (isLoadingOrRegistering || isCountingDown) return
        val f = currentForm()
        val emailError = EmailInput.errorRes(f.email)
        if (emailError != null) {
            _uiState.value = RegisterUiState.Error(f, emailError)
            return
        }
        _uiState.value = RegisterUiState.SendingCode(f)

        viewModelScope.launch {
            val result = authRepository.sendEmailCode(f.email, EmailCodePurpose.REGISTER)
            result.fold(
                onSuccess = { startCountdown() },
                onFailure = { e ->
                    val f2 = currentForm()
                    val resId =
                        ErrorMessages.forSendCode(e)
                    _uiState.value = RegisterUiState.Error(f2, resId)
                },
            )
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        val f = currentForm()
        _uiState.value = RegisterUiState.Countdown(f, 0)

        countdownJob =
            viewModelScope.launch {
                countdownUseCase().collect { seconds ->
                    val f2 = currentForm()
                    _uiState.value = RegisterUiState.Countdown(f2, seconds)
                }
            }
    }

    fun register() {
        if (isLoadingOrRegistering) return
        val f = currentForm()
        val emailError = EmailInput.errorRes(f.email)
        if (emailError != null) {
            _uiState.value = RegisterUiState.Error(f, emailError)
            return
        }
        if (f.password.isBlank()) {
            _uiState.value = RegisterUiState.Error(f, R.string.error_password_required)
            return
        }
        if (f.emailVerifyEnabled && f.verificationCode.isBlank()) {
            _uiState.value = RegisterUiState.Error(f, R.string.error_code_required)
            return
        }
        if (f.inviteForceEnabled && f.inviteCode.isBlank()) {
            _uiState.value = RegisterUiState.Error(f, R.string.error_invite_required)
            return
        }

        _uiState.value = RegisterUiState.Registering(f)

        registerJob?.cancel()
        registerJob =
            viewModelScope.launch {
                val f2 = currentForm()
                val result =
                    authRepository.register(
                        email = f2.email,
                        password = f2.password,
                        emailCode = if (f2.emailVerifyEnabled) f2.verificationCode else null,
                        inviteCode = if (f2.inviteForceEnabled || f2.inviteCode.isNotBlank()) f2.inviteCode else null,
                    )
                result.fold(
                    onSuccess = { _uiState.value = RegisterUiState.RegisterSuccess(f2) },
                    onFailure = { e ->
                        val f3 = currentForm()
                        val resId =
                            ErrorMessages.forRegister(e)
                        _uiState.value = RegisterUiState.Error(f3, resId)
                    },
                )
            }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        registerJob?.cancel()
    }

    fun cancelLoading() {
        registerJob?.cancel()
        countdownJob?.cancel()
        val f = currentForm()
        _uiState.value =
            RegisterUiState.Form(
                email = f.email,
                password = f.password,
                verificationCode = f.verificationCode,
                inviteCode = f.inviteCode,
                emailVerifyEnabled = f.emailVerifyEnabled,
                inviteForceEnabled = f.inviteForceEnabled,
            )
    }
}
