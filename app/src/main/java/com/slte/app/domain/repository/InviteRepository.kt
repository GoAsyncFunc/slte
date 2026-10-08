package com.slte.app.domain.repository

import com.slte.app.domain.model.CommissionRecord
import com.slte.app.domain.model.InviteInfo

interface InviteRepository {
    suspend fun fetchInviteInfo(): Result<InviteInfo>
    suspend fun generateInviteCode(): Result<Boolean>
    suspend fun fetchCommissionRecords(page: Int = 1, pageSize: Int = 10): Result<List<CommissionRecord>>
    suspend fun transferCommission(transferAmountCents: Int): Result<Boolean>
    suspend fun withdrawCommission(method: String, account: String): Result<Boolean>
    suspend fun fetchWithdrawMethods(): Result<List<String>>
}
