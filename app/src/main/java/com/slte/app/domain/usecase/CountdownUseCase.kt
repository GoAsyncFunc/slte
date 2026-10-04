package com.slte.app.domain.usecase

import javax.inject.Inject
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** 验证码倒计时参数：倒计时业务的领域常量，由本文件持有，不依赖 app 层配置。 */
object VerificationCodeConfig {
    const val countdownSeconds = 60
    const val countdownIntervalMs = 1000L
}

class CountdownUseCase
@Inject
constructor() {
    operator fun invoke(): Flow<Int> = flow {
        var remaining = VerificationCodeConfig.countdownSeconds
        while (remaining > 0) {
            emit(remaining)
            delay(VerificationCodeConfig.countdownIntervalMs)
            remaining--
        }
        emit(0)
    }
}
