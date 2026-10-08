package com.slte.app.data.remote.adapter.xboard

import com.slte.app.data.remote.adapter.v2board.V2BoardAuthRetrofit
import com.slte.app.data.remote.adapter.v2board.V2BoardUserRetrofit
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Xboard 的 Retrofit 接口。
 *
 * 与 v2board 一致的端点全部继承自
 * [com.slte.app.data.remote.adapter.v2board] 的共享接口，这里只声明 Xboard 特有的三处：
 * 会话列表（令牌数组）、节点列表（NodeResource）、新版礼品卡兑换端点。
 */

interface XboardAuthRetrofit : V2BoardAuthRetrofit

interface XboardUserPlanRetrofit {
    @GET("user/plan/fetch")
    suspend fun fetchPlans(): XboardResponse<List<XboardPlanData>>
}

interface XboardUserRetrofit : V2BoardUserRetrofit {
    @GET("user/info")
    suspend fun fetchUserInfo(): XboardResponse<XboardUserInfoData>

    @GET("user/notice/fetch")
    suspend fun fetchNotices(
        @Query("current") current: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): XboardResponse<List<XboardNoticeData>>

    @GET("user/invite/fetch")
    suspend fun fetchInviteInfo(): XboardResponse<XboardInviteData>

    @GET("user/getActiveSession")
    suspend fun getActiveSessions(
        @Header("Authorization") authData: String?,
    ): XboardResponse<JsonElement?>

    @GET("user/server/fetch")
    suspend fun fetchServers(): XboardResponse<List<XboardServerData>>

    @POST("user/gift-card/redeem")
    suspend fun redeemGiftCard(
        @Body request: XboardGiftCardRedeemRequest,
    ): XboardResponse<JsonElement>
}
