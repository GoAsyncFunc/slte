package com.slte.app.data.repository

import com.slte.app.data.remote.api.AuthApi
import com.slte.app.domain.repository.GiftCardRepository as GiftCardRepositoryContract
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GiftCardRepositoryImpl
@Inject
constructor(
    private val authApi: AuthApi,
) : GiftCardRepositoryContract {
    override suspend fun redeem(code: String): Result<Unit> = runApi {
        authApi.redeemGiftCard(code.trim())
    }
}
