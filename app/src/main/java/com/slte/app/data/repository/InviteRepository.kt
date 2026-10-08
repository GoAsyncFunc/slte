package com.slte.app.data.repository

import com.slte.app.data.remote.api.AuthApi
import com.slte.app.domain.model.CommissionRecord
import com.slte.app.domain.model.InviteInfo
import com.slte.app.domain.repository.InviteRepository as InviteRepositoryContract
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InviteRepositoryImpl
@Inject
constructor(
    private val authApi: AuthApi,
) : InviteRepositoryContract {

    override suspend fun fetchInviteInfo(): Result<InviteInfo> = runApi {
        authApi.fetchInviteInfo()
    }

    override suspend fun generateInviteCode(): Result<Boolean> = runApi {
        authApi.generateInviteCode()
    }

    override suspend fun fetchCommissionRecords(
        page: Int,
        pageSize: Int,
    ): Result<List<CommissionRecord>> = runApi {
        authApi.fetchCommissionRecords(page, pageSize)
    }

    override suspend fun transferCommission(transferAmountCents: Int): Result<Boolean> = runApi {
        authApi.transferCommission(transferAmountCents)
    }

    override suspend fun withdrawCommission(
        method: String,
        account: String,
    ): Result<Boolean> = runApi {
        authApi.withdrawCommission(method, account)
    }

    override suspend fun fetchWithdrawMethods(): Result<List<String>> = runApi {
        authApi.fetchWithdrawMethods()
    }
}
