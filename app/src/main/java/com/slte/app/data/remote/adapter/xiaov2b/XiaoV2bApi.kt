package com.slte.app.data.remote.adapter.xiaov2b

import com.slte.app.data.remote.adapter.v2board.V2BoardAuthRetrofit
import com.slte.app.data.remote.adapter.v2board.V2BoardUserRetrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * v2board 的 Retrofit 接口。
 *
 * 与 Xboard 一致的端点全部继承自
 * [com.slte.app.data.remote.adapter.v2board] 的共享接口，这里只声明 v2board 特有的三处：
 * 会话列表（map）、节点列表（原始协议行）、旧版礼品卡兑换端点。
 */

interface XiaoV2bAuthRetrofit : V2BoardAuthRetrofit

interface XiaoV2bUserPlanRetrofit {
    @GET("user/plan/fetch")
    suspend fun fetchPlans(): XiaoV2bResponse<List<XiaoV2bPlanData>>
}

interface XiaoV2bUserRetrofit : V2BoardUserRetrofit {
    @GET("user/info")
    suspend fun fetchUserInfo(): XiaoV2bResponse<XiaoV2bUserInfoData>

    @GET("user/notice/fetch")
    suspend fun fetchNotices(
        @Query("current") current: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): XiaoV2bResponse<List<XiaoV2bNoticeData>>

    @GET("user/invite/fetch")
    suspend fun fetchInviteInfo(): XiaoV2bResponse<XiaoV2bInviteData>

    @GET("user/getActiveSession")
    suspend fun getActiveSessions(
        @Header("Authorization") authData: String?,
    ): XiaoV2bResponse<Map<String, XiaoV2bActiveSessionData>>

    @GET("user/server/fetch")
    suspend fun fetchServers(): XiaoV2bResponse<List<XiaoV2bServerData>>

    @POST("user/redeemgiftcard")
    suspend fun redeemGiftCard(
        @Body request: XiaoV2bGiftCardRedeemRequest,
    ): XiaoV2bResponse<Boolean>
}
