package com.slte.app.ui.screen.plans

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slte.app.R
import com.slte.app.domain.model.CreateOrderResult
import com.slte.app.domain.model.PlanInfo
import com.slte.app.domain.usecase.purchase.CheckoutOutcome
import com.slte.app.domain.usecase.purchase.CouponCheck
import com.slte.app.domain.usecase.purchase.CouponChecker
import com.slte.app.domain.usecase.purchase.OrderCreator
import com.slte.app.domain.usecase.purchase.OrderPaymentLoad
import com.slte.app.domain.usecase.purchase.OrderPaymentLoader
import com.slte.app.domain.usecase.purchase.OrderPaymentPoller
import com.slte.app.domain.usecase.purchase.PaymentCheckout
import com.slte.app.domain.usecase.purchase.PollOutcome
import com.slte.app.domain.usecase.purchase.computeCouponDiscount
import com.slte.app.domain.usecase.purchase.decideCheckoutStep
import com.slte.app.domain.usecase.purchase.finalPriceCents
import com.slte.app.domain.usecase.purchase.pollOutcome
import com.slte.app.utils.AppLog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PurchaseViewModel
@Inject
constructor(
    private val couponChecker: CouponChecker,
    private val paymentLoader: OrderPaymentLoader,
    private val poller: OrderPaymentPoller,
    private val orderCreator: OrderCreator,
    private val paymentCheckout: PaymentCheckout,
) : ViewModel() {
    private val _step = MutableStateFlow<PurchaseStep>(PurchaseStep.Idle)
    val step: StateFlow<PurchaseStep> = _step.asStateFlow()

    private val _toastRes = MutableStateFlow<Int?>(null)
    val toastRes: StateFlow<Int?> = _toastRes.asStateFlow()

    private val _paymentCompleted = MutableSharedFlow<String>(replay = 1, extraBufferCapacity = 1)
    val paymentCompleted: SharedFlow<String> = _paymentCompleted.asSharedFlow()

    private val _createdTradeNo = MutableStateFlow<String?>(null)
    val createdTradeNo: StateFlow<String?> = _createdTradeNo.asStateFlow()

    private var creatingOrder = false

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun ackPaymentCompleted() {
        _paymentCompleted.resetReplayCache()
    }

    fun startPurchase(plan: PlanInfo) {
        _step.value = PurchaseStep.SelectPeriod(plan = plan)
    }

    fun selectPeriod(period: String) {
        val current = _step.value
        if (current is PurchaseStep.SelectPeriod) {
            _step.value = current.copy(selectedPeriod = period)
        }
    }

    fun updateCouponCode(code: String) {
        val current = _step.value
        if (current is PurchaseStep.SelectPeriod) {
            _step.value =
                current.copy(
                    couponCode = code,
                    couponVerified = false,
                    couponDiscount = 0,
                    isVerifying = false,
                )
        }
    }

    fun verifyCoupon() {
        val current = _step.value
        if (current !is PurchaseStep.SelectPeriod) return
        if (current.couponCode.isBlank()) return
        if (current.isVerifying) return

        _step.value = current.copy(isVerifying = true)
        viewModelScope.launch {
            val check = couponChecker.check(current.couponCode, current.plan.id.toInt(), current.priceCents)
            val state = _step.value as? PurchaseStep.SelectPeriod
            if (state == null || state.couponCode != current.couponCode) return@launch
            when (check) {
                is CouponCheck.Applied -> {
                    _step.value =
                        state.copy(
                            isVerifying = false,
                            couponVerified = true,
                            couponDiscount = check.discount,
                        )
                    _toastRes.value = R.string.purchase_coupon_applied
                }

                is CouponCheck.Rejected -> {
                    _step.value =
                        state.copy(
                            isVerifying = false,
                            couponVerified = false,
                            couponDiscount = 0,
                        )
                    _toastRes.value = check.messageRes
                }
            }
        }
    }

    fun showConfirmWarning() {
        val current = _step.value
        if (current !is PurchaseStep.SelectPeriod) return
        if (current.couponCode.isNotBlank() && !current.couponVerified) {
            _toastRes.value = R.string.purchase_coupon_verify_first
            return
        }
        _step.value = current.copy(showWarning = true)
    }

    fun cancelWarning() {
        val current = _step.value
        if (current is PurchaseStep.SelectPeriod) {
            _step.value = current.copy(showWarning = false)
        }
    }

    fun confirmWarning() {
        if (creatingOrder) return
        val current = _step.value
        if (current !is PurchaseStep.SelectPeriod) return

        val coupon = current.couponCode.takeIf { it.isNotBlank() && current.couponVerified }
        if (current.couponCode.isNotBlank() && !current.couponVerified) {
            _toastRes.value = R.string.purchase_coupon_verify_first
            return
        }
        creatingOrder = true
        viewModelScope.launch {
            try {
                val outcome = orderCreator.create(current.plan.id.toInt(), current.selectedPeriod, coupon)
                outcome.tradeNo?.let { _createdTradeNo.value = it }
                _step.value = outcome.nextStep(current, coupon)
            } finally {
                creatingOrder = false
            }
        }
    }

    fun clearCreatedTradeNo() {
        _createdTradeNo.value = null
    }

    fun loadPaymentForOrder(tradeNo: String) {
        loadOrderPayment(CreateOrderResult(tradeNo))
    }

    private fun loadOrderPayment(orderResult: CreateOrderResult) {
        viewModelScope.launch {
            val loaded = paymentLoader.load(orderResult.tradeNo)
            loaded.toToastRes()?.let { _toastRes.value = it }
            if (loaded is OrderPaymentLoad.Ready) {
                val step = loaded.toPaymentStep(orderResult.tradeNo)
                _step.value = step
                // 轮询跟随支付状态本身启动，不再依赖某个页面恰好可见
                if (step is PurchaseStep.OrderPayment) startPolling(step.tradeNo)
            }
        }
    }

    fun selectPaymentMethod(methodId: Int) {
        val current = _step.value
        if (current is PurchaseStep.OrderPayment) {
            _step.value = current.copy(selectedMethod = methodId)
        }
    }

    fun confirmPayment() {
        val current = _step.value
        if (current !is PurchaseStep.OrderPayment || current.isPaying) return
        val methodId = if (current.zeroPayable) 0 else current.selectedMethod ?: return

        _step.value = current.copy(isPaying = true)
        viewModelScope.launch {
            val outcome = paymentCheckout.checkout(current.tradeNo, methodId)
            outcome.toToastRes()?.let { _toastRes.value = it }
            _step.update { s -> if (s is PurchaseStep.OrderPayment) outcome.nextStep(s) else s }
            when (outcome) {
                is CheckoutOutcome.Completed -> _paymentCompleted.emit(outcome.tradeNo)
                // 跳浏览器后用户可能长时间停留在外，轮询在后台盯着订单状态，
                // 完成后走 paymentCompleted 统一收尾（套餐页发起的支付同样覆盖）
                is CheckoutOutcome.Redirect -> startPolling(current.tradeNo)
                is CheckoutOutcome.Retry -> Unit
            }
        }
    }

    fun goBack() {
        poller.stop()
        _step.value = (_step.value as? PurchaseStep.ExistingOrderError)?.toSelectPeriod() ?: PurchaseStep.Idle
    }

    fun onPaymentReturn() {
        AppLog.d(TAG, "onPaymentReturn: 从浏览器返回，停止轮询")
        poller.stop()
        _step.value = PurchaseStep.Idle
    }

    fun startOrderPolling(tradeNo: String) {
        startPolling(tradeNo)
    }

    /** 轮询支付结果；完成事件用挂起 emit 代替 tryEmit，缓冲占满时等待而不是静默丢弃。 */
    private fun startPolling(tradeNo: String) {
        poller.start(
            scope = viewModelScope,
            tradeNo = tradeNo,
            onCompleted = { completedTradeNo ->
                viewModelScope.launch { _paymentCompleted.emit(completedTradeNo) }
            },
            onTimeout = { _toastRes.value = R.string.purchase_pay_timeout },
        )
    }

    fun clearToast() {
        _toastRes.value = null
    }

    override fun onCleared() {
        poller.stop()
        super.onCleared()
    }

    private companion object {
        const val TAG = "SLTE-Purchase"
    }
}
