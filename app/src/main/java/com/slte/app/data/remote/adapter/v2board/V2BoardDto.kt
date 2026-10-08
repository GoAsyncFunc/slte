package com.slte.app.data.remote.adapter.v2board

import com.slte.app.data.remote.api.ApiResponse
import com.slte.app.data.remote.api.dto.CouponCheckResultDto
import com.slte.app.data.remote.api.dto.LoginResponseDto
import com.slte.app.data.remote.api.dto.OrderInfoDto
import com.slte.app.data.remote.api.dto.PaymentMethodDto
import com.slte.app.data.remote.api.dto.SubscribeInfoDto
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

/**
 * V2Board / Xboard 两个面板共用的请求、响应与数据模型。
 *
 * 下面这些类型两个面板的字段与类型完全一致，放一份就够，避免两边各存一份而悄悄分叉。
 *
 * 不带标志位的字段（0/1 与 true/false 之分）**故意不共享**：v2board 用 0/1、
 * Xboard 用 true/false，各自保留严格类型，面板下发形态变了要立刻解析报错；
 * 相关类型见两个面板包里的 UserInfoData / PlanData / InviteCodeData / NoticeData。
 */

@Serializable
data class V2BoardResponse<T>(
    override val data: T? = null,
    override val message: String? = null,
) : ApiResponse<T>

@Serializable
data class V2BoardLoginRequest(
    val email: String,
    val password: String,
)

@Serializable
data class V2BoardRegisterRequest(
    val email: String,
    val password: String,
    val email_code: String? = null,
    val invite_code: String? = null,
)

@Serializable
data class V2BoardForgotRequest(
    val email: String,
    val password: String,
    val email_code: String,
)

@Serializable
data class V2BoardSendCodeRequest(
    val email: String,
    val isforget: Int,
)

@Serializable
data class V2BoardTransferRequest(
    @SerialName("transfer_amount")
    val transferAmount: Int,
)

@Serializable
data class V2BoardWithdrawRequest(
    @SerialName("withdraw_method")
    val withdrawMethod: String,
    @SerialName("withdraw_account")
    val withdrawAccount: String,
)

@Serializable
data class V2BoardUserCommConfigData(
    @SerialName("withdraw_methods")
    val withdrawMethods: List<String>? = null,
    @SerialName("withdraw_close")
    val withdrawClose: Int? = 0,
)

@Serializable
data class V2BoardRemoveSessionRequest(
    @SerialName("session_id")
    val sessionId: String,
)

@Serializable
data class V2BoardUpdateUserRequest(
    @SerialName("remind_expire")
    val remindExpire: Int? = null,
    @SerialName("remind_traffic")
    val remindTraffic: Int? = null,
)

@Serializable
data class V2BoardChangePasswordRequest(
    @SerialName("old_password")
    val oldPassword: String,
    @SerialName("new_password")
    val newPassword: String,
)

@Serializable
data class V2BoardCreateOrderRequest(
    @SerialName("plan_id")
    val planId: Int,
    val period: String,
    @SerialName("coupon_code")
    val couponCode: String? = null,
)

@Serializable
data class V2BoardCouponCheckRequest(
    val code: String,
    @SerialName("plan_id")
    val planId: Int? = null,
)

@Serializable
data class V2BoardCheckoutRequest(
    @SerialName("trade_no")
    val tradeNo: String,
    val method: Int,
)

@Serializable
data class V2BoardCancelOrderRequest(
    @SerialName("trade_no")
    val tradeNo: String,
)

@Serializable
data class V2BoardLoginData(
    val token: String,
    val auth_data: String,
) {
    fun toDomainLoginResponse() = LoginResponseDto(
        token = token,
        authData = auth_data,
    )
}

@Serializable
data class V2BoardSiteConfig(
    val is_email_verify: Int? = 0,
    val is_invite_force: Int? = 0,
    // 邮箱后缀白名单：各面板可能是数组 / 字符串 / 0，统一交给 parseEmailWhitelistSuffixes
    val email_whitelist_suffix: JsonElement? = null,
)

/**
 * 订阅 / 订单里内嵌的套餐对象。
 *
 * 两个面板的完整套餐模型带标志位（show/renew）无法共享，但这两处只用到套餐名，
 * 用一个只取名字的小模型即可对齐。
 */
@Serializable
data class V2BoardPlanSummary(
    val name: String = "",
)

@Serializable
data class V2BoardSubscribeData(
    @SerialName("plan_id")
    val planId: Int = 0,
    @SerialName("expired_at")
    val expiredAt: Long = 0L,
    @SerialName("transfer_enable")
    val transferEnable: Long = 0L,
    val u: Long = 0L,
    val d: Long = 0L,
    @SerialName("reset_day")
    val resetDay: Int? = null,
    @SerialName("subscribe_url")
    val subscribeUrl: String? = null,
    val plan: V2BoardPlanSummary? = null,
) {
    fun toDomainSubscribeInfo() = SubscribeInfoDto(
        planId = planId,
        planName = plan?.name ?: "",
        expiredAt = expiredAt,
        transferEnable = transferEnable,
        upload = u,
        download = d,
        resetDay = resetDay,
        subscribeUrl = subscribeUrl,
    )
}

@Serializable
data class V2BoardCommissionRecordData(
    val id: Int = 0,
    @SerialName("trade_no")
    val tradeNo: String = "",
    @SerialName("order_amount")
    val orderAmount: Int = 0,
    @SerialName("get_amount")
    val getAmount: Int = 0,
    @SerialName("created_at")
    val createdAt: Long = 0L,
) {
    fun toDomain() = com.slte.app.domain.model.CommissionRecord(
        id = id,
        tradeNo = tradeNo,
        orderAmount = orderAmount,
        getAmount = getAmount,
        createdAt = createdAt,
    )
}

@Serializable
data class V2BoardOrderData(
    val id: Int = 0,
    @SerialName("trade_no")
    val tradeNo: String = "",
    @SerialName("total_amount")
    val totalAmount: Int = 0,
    @SerialName("balance_amount")
    val balanceAmount: JsonElement? = null,
    @SerialName("discount_amount")
    val discountAmount: JsonElement? = null,
    @SerialName("surplus_amount")
    val surplusAmount: JsonElement? = null,
    // v2board 用 refund_amount、Xboard 用 surplus_credit 表示同一件事
    @SerialName("refund_amount")
    val refundAmount: JsonElement? = null,
    @SerialName("surplus_credit")
    val surplusCredit: JsonElement? = null,
    @SerialName("handling_amount")
    val handlingAmount: JsonElement? = null,
    val status: Int = 0,
    val period: String = "",
    @SerialName("created_at")
    val createdAt: Long = 0L,
    @SerialName("expired_at")
    val expiredAt: Long = 0L,
    val plan: V2BoardPlanSummary? = null,
) {
    /**
     * @param surplusCreditAsRefund Xboard 的退款金额取 surplus_credit，v2board 取 refund_amount。
     *   两个字段同时出现时按面板语义取，由各面板在实现里传自己的口径。
     */
    fun toDomainOrder(surplusCreditAsRefund: Boolean = false) = OrderInfoDto(
        id = id,
        tradeNo = tradeNo,
        planName = plan?.name ?: "",
        totalAmount = totalAmount,
        balanceAmount = balanceAmount.jsonIntOrNull() ?: 0,
        discountAmount = discountAmount.jsonIntOrNull() ?: 0,
        surplusAmount = surplusAmount.jsonIntOrNull() ?: 0,
        refundAmount =
        if (surplusCreditAsRefund) {
            surplusCredit.jsonIntOrNull() ?: 0
        } else {
            refundAmount.jsonIntOrNull() ?: 0
        },
        handlingAmount = handlingAmount.jsonIntOrNull(),
        status = status,
        period = period,
        createdAt = createdAt,
        expiredAt = expiredAt,
    )
}

@Serializable
data class V2BoardCouponData(
    val id: Int? = null,
    val code: String? = null,
    val name: String? = null,
    val type: Int? = null,
    val value: Int? = null,
    @SerialName("plan_id")
    val planId: Int? = null,
) {
    fun toDomainCouponCheck() = CouponCheckResultDto(
        name = name ?: code ?: "",
        type = type ?: 2,
        value = value ?: 0,
    )
}

@Serializable
data class V2BoardPaymentMethodData(
    val id: Int = 0,
    val name: String = "",
    val payment: String = "",
    val icon: String? = null,
) {
    fun toDomainPaymentMethod() = PaymentMethodDto(
        id = id,
        name = name,
        payment = payment,
        icon = icon,
    )
}

private val jsonNumberText = Regex("""[+-]?(\d+(\.\d*)?|\.\d+)([eE][+-]?\d+)?""")

/** 金额字段宽容解析：数字、数字字符串、小数都接受，取不到按 null（由映射层给默认值）。 */
internal fun JsonElement?.jsonIntOrNull(): Int? {
    val primitive = this as? JsonPrimitive ?: return null
    val text = primitive.content.trim()
    if (!jsonNumberText.matches(text)) return null
    val value = text.toDoubleOrNull() ?: return null
    val rounded = abs(value).roundToInt()
    return if (value < 0) -rounded else rounded
}
