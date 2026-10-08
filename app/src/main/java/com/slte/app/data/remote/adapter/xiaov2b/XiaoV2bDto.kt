package com.slte.app.data.remote.adapter.xiaov2b

import com.slte.app.data.remote.adapter.v2board.V2BoardCancelOrderRequest
import com.slte.app.data.remote.adapter.v2board.V2BoardChangePasswordRequest
import com.slte.app.data.remote.adapter.v2board.V2BoardCheckoutRequest
import com.slte.app.data.remote.adapter.v2board.V2BoardCommissionRecordData
import com.slte.app.data.remote.adapter.v2board.V2BoardCouponCheckRequest
import com.slte.app.data.remote.adapter.v2board.V2BoardCouponData
import com.slte.app.data.remote.adapter.v2board.V2BoardCreateOrderRequest
import com.slte.app.data.remote.adapter.v2board.V2BoardForgotRequest
import com.slte.app.data.remote.adapter.v2board.V2BoardLoginData
import com.slte.app.data.remote.adapter.v2board.V2BoardLoginRequest
import com.slte.app.data.remote.adapter.v2board.V2BoardOrderData
import com.slte.app.data.remote.adapter.v2board.V2BoardPaymentMethodData
import com.slte.app.data.remote.adapter.v2board.V2BoardPlanSummary
import com.slte.app.data.remote.adapter.v2board.V2BoardRegisterRequest
import com.slte.app.data.remote.adapter.v2board.V2BoardRemoveSessionRequest
import com.slte.app.data.remote.adapter.v2board.V2BoardResponse
import com.slte.app.data.remote.adapter.v2board.V2BoardSendCodeRequest
import com.slte.app.data.remote.adapter.v2board.V2BoardSiteConfig
import com.slte.app.data.remote.adapter.v2board.V2BoardSubscribeData
import com.slte.app.data.remote.adapter.v2board.V2BoardTransferRequest
import com.slte.app.data.remote.adapter.v2board.V2BoardUpdateUserRequest
import com.slte.app.data.remote.adapter.v2board.V2BoardUserCommConfigData
import com.slte.app.data.remote.adapter.v2board.V2BoardWithdrawRequest
import com.slte.app.data.remote.api.dto.PlanInfoDto
import com.slte.app.data.remote.api.dto.UserInfoDto
import com.slte.app.domain.model.InviteCodeInfo
import com.slte.app.domain.model.InviteInfo
import com.slte.app.domain.model.InviteStat
import com.slte.app.domain.model.Notice
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * v2board 面板的取词表。
 *
 * 请求、响应与数据模型两个面板完全一致，实现都在
 * [com.slte.app.data.remote.adapter.v2board]；这里保留 v2board 前缀的别名，
 * 让面板代码读起来仍是"这个面板的模型"，同时不再各存一份定义。
 *
 * 下列类型**故意不共享**：同一字段 v2board 下发 0/1、Xboard 下发 true/false，
 * 两边各自保留严格类型（见 DtoBoundaryTest「拒绝布尔/数字字面量」两例）——
 * 面板下发形态真的变了要立刻报错，而不是被静默兼容掉。
 */

typealias XiaoV2bResponse<T> = V2BoardResponse<T>

typealias XiaoV2bLoginRequest = V2BoardLoginRequest

typealias XiaoV2bRegisterRequest = V2BoardRegisterRequest

typealias XiaoV2bForgotRequest = V2BoardForgotRequest

typealias XiaoV2bSendCodeRequest = V2BoardSendCodeRequest

typealias XiaoV2bLoginData = V2BoardLoginData

typealias XiaoV2bSiteConfig = V2BoardSiteConfig

typealias XiaoV2bCommissionRecordData = V2BoardCommissionRecordData

typealias XiaoV2bTransferRequest = V2BoardTransferRequest

typealias XiaoV2bWithdrawRequest = V2BoardWithdrawRequest

typealias XiaoV2bUserCommConfigData = V2BoardUserCommConfigData

typealias XiaoV2bRemoveSessionRequest = V2BoardRemoveSessionRequest

typealias XiaoV2bUpdateUserRequest = V2BoardUpdateUserRequest

typealias XiaoV2bChangePasswordRequest = V2BoardChangePasswordRequest

typealias XiaoV2bCreateOrderRequest = V2BoardCreateOrderRequest

typealias XiaoV2bCouponCheckRequest = V2BoardCouponCheckRequest

typealias XiaoV2bCheckoutRequest = V2BoardCheckoutRequest

typealias XiaoV2bCancelOrderRequest = V2BoardCancelOrderRequest

typealias XiaoV2bOrderData = V2BoardOrderData

typealias XiaoV2bCouponData = V2BoardCouponData

typealias XiaoV2bPaymentMethodData = V2BoardPaymentMethodData

typealias XiaoV2bSubscribeData = V2BoardSubscribeData

typealias XiaoV2bPlanSummary = V2BoardPlanSummary

/** 邀请统计：codes 里是面板专属的邀请码模型（状态字段类型不同）。 */
@Serializable
data class XiaoV2bInviteData(
    val codes: List<XiaoV2bInviteCodeData> = emptyList(),
    val stat: List<Int> = emptyList(),
) {
    fun toDomain() = InviteInfo(
        codes = codes.map { it.toDomain() },
        stat =
        InviteStat(
            registeredUsers = stat.getOrElse(0) { 0 },
            totalCommission = stat.getOrElse(1) { 0 },
            pendingCommission = stat.getOrElse(2) { 0 },
            commissionRate = stat.getOrElse(3) { 0 },
            availableBalance = stat.getOrElse(4) { 0 },
        ),
    )
}

/** v2board 提醒开关是 0/1（Xboard 是布尔）。 */
@Serializable
data class XiaoV2bUserInfoData(
    val email: String = "",
    val balance: Int = 0,
    @SerialName("plan_id")
    val planId: Int = 0,
    @SerialName("expired_at")
    val expiredAt: Long = 0L,
    @SerialName("transfer_enable")
    val transferEnable: Long = 0L,
    @SerialName("remind_expire")
    val remindExpire: Int = 0,
    @SerialName("remind_traffic")
    val remindTraffic: Int = 0,
) {
    fun toDomainUserInfo() = UserInfoDto(
        email = email,
        balance = balance,
        planId = planId,
        expiredAt = expiredAt,
        transferEnable = transferEnable,
        remindExpire = remindExpire,
        remindTraffic = remindTraffic,
    )
}

/** v2board 的 show/renew 是 0/1（Xboard 是布尔）。 */
@Serializable
data class XiaoV2bPlanData(
    val id: Int = 0,
    val name: String = "",
    val description: String? = null,
    @SerialName("month_price")
    val monthPrice: Long? = null,
    @SerialName("quarter_price")
    val quarterPrice: Long? = null,
    @SerialName("half_year_price")
    val halfYearPrice: Long? = null,
    @SerialName("year_price")
    val yearPrice: Long? = null,
    @SerialName("two_year_price")
    val twoYearPrice: Long? = null,
    @SerialName("three_year_price")
    val threeYearPrice: Long? = null,
    @SerialName("onetime_price")
    val onetimePrice: Long? = null,
    @SerialName("reset_price")
    val resetPrice: Long? = null,
    @SerialName("speed_limit")
    val speedLimit: Int? = null,
    @SerialName("device_limit")
    val deviceLimit: Int? = null,
    val content: String? = null,
    val show: Int = 1,
    val renew: Int = 1,
    @SerialName("capacity_limit")
    val capacityLimit: Int? = null,
    @SerialName("group_id")
    val groupId: Int? = null,
    val sort: Int? = null,
    @SerialName("transfer_enable")
    val transferEnable: Int = 0,
) {
    fun toDomainPlan() = PlanInfoDto(
        id = id,
        name = name,
        monthPrice = monthPrice,
        quarterPrice = quarterPrice,
        halfYearPrice = halfYearPrice,
        yearPrice = yearPrice,
        twoYearPrice = twoYearPrice,
        threeYearPrice = threeYearPrice,
        onetimePrice = onetimePrice,
        resetPrice = resetPrice,
        speedLimit = speedLimit,
        deviceLimit = deviceLimit,
        content = content,
        transferEnable = transferEnable,
        show = show == 1,
        renew = renew == 1,
        sort = sort,
    )
}

/** v2board 的邀请码状态是状态码，原样透传（Xboard 是布尔，归一化为 0/1）。 */
@Serializable
data class XiaoV2bInviteCodeData(
    val id: Int = 0,
    @SerialName("user_id")
    val userId: Int = 0,
    val code: String = "",
    val status: Int = 0,
    val pv: Int = 0,
    @SerialName("created_at")
    val createdAt: Long = 0L,
    @SerialName("updated_at")
    val updatedAt: Long = 0L,
) {
    fun toDomain() = InviteCodeInfo(
        id = id,
        code = code,
        pv = pv,
        status = status,
        createdAt = createdAt,
    )
}

/** v2board 的公告 show 是 0/1（Xboard 是布尔）。 */
@Serializable
data class XiaoV2bNoticeData(
    val id: Int = 0,
    val title: String = "",
    val content: String = "",
    val tags: List<String>? = null,
    val show: Int = 1,
    @SerialName("img_url")
    val imgUrl: String? = null,
    @SerialName("created_at")
    val createdAt: Long = 0,
    @SerialName("updated_at")
    val updatedAt: Long = 0,
) {
    fun toDomain() = Notice(
        id = id,
        title = title,
        body = content,
        tags = tags ?: emptyList(),
        createdAt = createdAt,
    )
}

/** v2board 的会话列表是 `{会话id: {ip, login_at, ua}}`，只需要键。 */
@Serializable
data class XiaoV2bActiveSessionData(
    val ip: String? = null,
    @SerialName("login_at")
    val loginAt: Long = 0L,
    val ua: String? = null,
)

/** v2board 旧版礼品卡接口用 `giftcard` 字段（Xboard 是 `code`）。 */
@Serializable
data class XiaoV2bGiftCardRedeemRequest(
    val giftcard: String,
)
