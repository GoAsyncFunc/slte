package com.slte.app.domain.usecase.purchase

import com.slte.app.data.remote.api.dto.OrderInfoDto
import com.slte.app.data.repository.OrderRepositoryImpl
import com.slte.app.support.FakeAuthApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class OrderPaymentPollerTest {
    private val api = FakeAuthApi()
    private val poller = OrderPaymentPoller(OrderRepositoryImpl(api))

    @Test
    fun `订单开通后触发完成回调`() = runBlocking {
        api.orderDetail = order(status = 3)
        var completedTradeNo: String? = null

        poller.start(this, "TN-1", onCompleted = { completedTradeNo = it })
        delay(4_000)

        assertEquals("TN-1", completedTradeNo)
        assertEquals(1, api.orderDetailCalls)
    }

    @Test
    fun `同一订单重复启动只轮询一次`() = runBlocking {
        api.orderDetail = order(status = 3)

        poller.start(this, "TN-1", onCompleted = {})
        poller.start(this, "TN-1", onCompleted = {})
        delay(4_000)

        assertEquals("去重后只应发起一次查询", 1, api.orderDetailCalls)
    }

    @Test
    fun `停掉轮询后不再查询也不再回调`() = runBlocking {
        api.orderDetail = order(status = 3)

        poller.start(this, "TN-1", onCompleted = { fail("已停止的轮询不应回调") })
        poller.stop()
        delay(4_000)

        assertEquals(0, api.orderDetailCalls)
    }

    @Test
    fun `订单未支付时继续等待且不回调`() = runBlocking {
        api.orderDetail = order(status = 0)
        var completed = false

        poller.start(this, "TN-1", onCompleted = { completed = true })
        delay(4_000)

        assertEquals("待支付应继续轮询", 1, api.orderDetailCalls)
        assertEquals("未开通不应回调", false, completed)
    }

    @Test
    fun `超时按真实经过时间收尾：单次查询再慢也只在总时长内轮询`() = runTest {
        val slowApi = SlowQueryApi(QUERY_DELAY_MS)
        slowApi.orderDetail = order(status = 0)
        val slowPoller = OrderPaymentPoller(OrderRepositoryImpl(slowApi))
        var timedOut = false

        slowPoller.start(this, "TN-1", onCompleted = {}, onTimeout = { timedOut = true })
        advanceUntilIdle()

        assertTrue("超过总时长必须收尾", timedOut)
        assertTrue(
            "查询次数应按「总时长 / 单次实际耗时」收敛；旧实现只累加 delay，" +
                "会打满 100 次并让用户多等十几分钟（实际轮询 ${slowApi.orderDetailCalls} 次）",
            slowApi.orderDetailCalls <= 6,
        )
    }

    private fun order(status: Int): OrderInfoDto = OrderInfoDto(
        id = 1,
        tradeNo = "TN-1",
        planName = "进阶套餐",
        totalAmount = 5_000,
        status = status,
        createdAt = 1_700_000_000L,
        expiredAt = 1_800_000_000L,
    )

    /** 单次查询耗时很长的后端：模拟慢网络下经过 failover 重试链的订单查询。 */
    private class SlowQueryApi(
        private val perQueryDelayMs: Long,
    ) : FakeAuthApi() {
        override suspend fun getOrderDetail(tradeNo: String): OrderInfoDto {
            delay(perQueryDelayMs)
            return super.getOrderDetail(tradeNo)
        }
    }

    private companion object {
        const val QUERY_DELAY_MS = 60_000L
    }
}
