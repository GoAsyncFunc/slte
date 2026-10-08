package com.slte.app.data.remote.adapter.xboard

import com.slte.app.BuildConfig
import com.slte.app.data.remote.ApiException
import com.slte.app.data.remote.adapter.AdapterExecute
import com.slte.app.data.remote.adapter.orEmptyLogged
import com.slte.app.data.remote.adapter.v2board.V2BoardAuthApi
import com.slte.app.data.remote.api.dto.PlanInfoDto
import com.slte.app.data.remote.api.dto.UserInfoDto
import com.slte.app.domain.model.InviteInfo
import com.slte.app.domain.model.Notice
import com.slte.app.domain.model.ServerNode
import com.slte.app.utils.ApiErrors
import com.slte.app.utils.AppLog
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull

/**
 * Xboard 面板的 [com.slte.app.data.remote.api.AuthApi] 实现。
 *
 * 与 v2board 的所有共同逻辑都在 [V2BoardAuthApi]，这里只实现三个面板差异点。
 */
class XboardAuthApi(
    authApi: XboardAuthRetrofit,
    userApi: XboardUserRetrofit,
    private val userPlanApi: XboardUserPlanRetrofit,
) : V2BoardAuthApi<XboardUserRetrofit>(authApi, userApi) {
    // Xboard 的退款金额取 surplus_credit（v2board 取 refund_amount）
    override val surplusCreditAsRefund: Boolean = true

    override suspend fun fetchUserInfo(): UserInfoDto {
        val response = AdapterExecute.typed { userApi.fetchUserInfo() }
        val data = response.data ?: throw ApiException("获取用户信息失败", ApiErrors.USER_INFO)
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "fetchUserInfo: planId=${data.planId}, expiredAt=${data.expiredAt}, transferEnable=${data.transferEnable}")
        }
        return data.toDomainUserInfo()
    }

    override suspend fun fetchPlans(): List<PlanInfoDto> {
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "fetchPlans: 请求 /user/plan/fetch")
        }

        val response = AdapterExecute.typed { userPlanApi.fetchPlans() }
        val data = response.data.orEmptyLogged("fetchPlans")
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "fetchPlans: 返回 ${data.size} 条套餐")
        }
        return data.map { it.toDomainPlan() }
    }

    override suspend fun fetchInviteInfo(): InviteInfo {
        val response = AdapterExecute.typed { userApi.fetchInviteInfo() }
        val data = response.data ?: throw ApiException("获取邀请信息失败", ApiErrors.INVITE_INFO)
        if (BuildConfig.DEBUG) {
            AppLog.d("SLTE-Api", "fetchInviteInfo: codes=${data.codes.size}, stat=${data.stat}")
        }
        return data.toDomain()
    }

    override suspend fun fetchNotices(
        page: Int,
        pageSize: Int,
    ): List<Notice> {
        val response = AdapterExecute.typed { userApi.fetchNotices(page, pageSize) }
        return response.data.orEmptyLogged("fetchNotices").map { it.toDomain() }
    }

    override suspend fun activeSessionIds(authData: String): List<String> {
        val data =
            AdapterExecute.typed { userApi.getActiveSessions(authData) }.data
                ?: return emptyList()

        // Sanctum 令牌列表是数组（元素 id 即 session_id）；部分版本返回 map，两种都认
        return when (data) {
            is JsonArray ->
                data.mapNotNull { item ->
                    (item as? JsonObject)?.get("id")?.let { v -> (v as? JsonPrimitive)?.content }
                }
            is JsonObject -> data.keys.toList()
            else -> emptyList()
        }
    }

    override suspend fun redeemGiftCardRemote(code: String) {
        val response =
            AdapterExecute.typed {
                userApi.redeemGiftCard(XboardGiftCardRedeemRequest(code))
            }
        // 与 v2board 对齐：HTTP 200 但 data 非 JSON true 仍是失败，不能静默报成功
        val redeemed = (response.data as? JsonPrimitive)?.booleanOrNull == true
        if (!redeemed) throw ApiException(response.message ?: "兑换失败", ApiErrors.GIFT_CARD)
    }

    override suspend fun fetchServerNodes(): List<ServerNode> = AdapterExecute.typed { userApi.fetchServers() }
        .data
        .orEmptyLogged("fetchServers")
        .map { it.toServerNode() }
}
