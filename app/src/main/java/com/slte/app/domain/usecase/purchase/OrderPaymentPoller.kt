package com.slte.app.domain.usecase.purchase

import com.slte.app.domain.repository.OrderRepository
import com.slte.app.utils.AppLog
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class OrderPaymentPoller
@Inject
constructor(
    private val orderRepository: OrderRepository,
) {
    private var pollJob: Job? = null
    private var pollingTradeNo: String? = null

    fun start(
        scope: CoroutineScope,
        tradeNo: String,
        onCompleted: (String) -> Unit,
        onTimeout: () -> Unit = {},
    ) {
        if (pollJob?.isActive == true && pollingTradeNo == tradeNo) return
        AppLog.d(TAG, "startOrderPolling: tradeNo=$tradeNo")
        pollJob?.cancel()
        pollingTradeNo = tradeNo
        pollJob =
            scope.launch {
                // 用协程超时而不是累加 delay：单次查询走 failover 重试链时可能耗时数十秒，
                // 只数 delay 会把"等 5 分钟"变成实际十几分钟、白打上百次请求；
                // withTimeoutOrNull 计的是真实经过时间（虚拟时间的单元测试同样能收敛）
                val completed =
                    withTimeoutOrNull(POLL_TIMEOUT_MS) {
                        var done = false
                        while (!done) {
                            delay(POLL_INTERVAL_MS)
                            val status = orderRepository.getOrderDetail(tradeNo).getOrNull()?.status
                            val outcome = pollOutcome(status)
                            if (outcome != null) {
                                AppLog.i(TAG, "poll 结束: tradeNo=$tradeNo status=$status")

                                if (outcome == PollOutcome.COMPLETED) {
                                    onCompleted(tradeNo)
                                }
                                done = true
                            }
                        }
                        true
                    }
                if (completed == null) {
                    // 超时不能静默：用户停留在支付流程里必须知道去哪里确认结果
                    AppLog.w(TAG, "poll 超时: tradeNo=$tradeNo")
                    onTimeout()
                }
            }
    }

    fun stop() {
        pollJob?.cancel()
        pollingTradeNo = null
    }

    private companion object {
        const val TAG = "SLTE-Purchase"

        const val POLL_INTERVAL_MS = 3_000L
        const val POLL_TIMEOUT_MS = 300_000L
    }
}
