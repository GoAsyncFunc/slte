package com.slte.app.domain.repository

interface GiftCardRepository {
    suspend fun redeem(code: String): Result<Unit>
}
