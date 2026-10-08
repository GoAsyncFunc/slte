package com.slte.app.domain.repository

import com.slte.app.domain.model.CheckoutResult
import com.slte.app.domain.model.CouponCheckResult
import com.slte.app.domain.model.CreateOrderResult
import com.slte.app.domain.model.OrderInfo
import com.slte.app.domain.model.PaymentMethod
import com.slte.app.domain.model.PlanInfo

interface OrderRepository {
    suspend fun fetchPlans(): Result<List<PlanInfo>>
    suspend fun createOrder(planId: Int, period: String, couponCode: String? = null): Result<CreateOrderResult>
    suspend fun getOrderDetail(tradeNo: String): Result<OrderInfo>
    suspend fun checkCoupon(code: String, planId: Int?): Result<CouponCheckResult>
    suspend fun checkoutOrder(tradeNo: String, paymentMethod: Int): Result<CheckoutResult>
    suspend fun getPaymentMethods(): Result<List<PaymentMethod>>
    suspend fun cancelOrder(tradeNo: String): Result<Unit>
    suspend fun fetchOrders(): Result<List<OrderInfo>>
}
