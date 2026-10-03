package com.slte.app.kernel

import android.content.Context
import com.github.kr328.clash.core.model.Proxy
import com.github.kr328.clash.core.model.ProxyGroup
import com.github.kr328.clash.service.remote.IClashManager
import com.slte.app.data.local.InMemoryPreferences
import com.slte.app.support.MainDispatcherRule
import com.slte.app.utils.Constants
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * 测速流的行为约定：**谁先出结果就先推谁**，不等所有节点测完。
 *
 * 内核语义里 delay ≤ 0 且尚无测速历史表示"还没测到"，不能当结果；
 * 测过但失败（含没测到过历史就失败的）归一成 [Constants.DELAY_TIMEOUT]，也是结果。
 */
class NodeLatencyStreamTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val prefs = InMemoryPreferences()
    private val context = mockk<Context>(relaxed = true)
    private val manager = mockk<KernelManager>(relaxed = true)
    private val clash = mockk<IClashManager>(relaxed = true)
    private val config = mockk<KernelConfig>(relaxed = true)
    private val store = mockk<SpeedResultStore>(relaxed = true)
    private val geoIp = mockk<GeoIpResolver>(relaxed = true)
    private val reporter = KernelFaultReporter(mainRule.dispatcher)

    private val group = "节点选择"
    private var delays = mapOf(NODE_HK to 0, NODE_JP to 0)
    private var measured = mapOf(NODE_HK to false, NODE_JP to false)
    private val saved = slot<Map<String, Int>>()

    private fun proxyOf(
        name: String,
        delay: Int,
        measured: Boolean,
    ) = Proxy(
        name = name,
        title = name,
        subtitle = "vless",
        type = "Vless",
        delay = delay,
        isGroup = false,
        measured = measured,
    )

    private fun kernelProxy(): KernelProxy {
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { context.packageName } returns "com.slte.app"
        every { manager.clash() } returns clash
        every { clash.queryProxyGroupNames(any()) } returns listOf(group)
        every { clash.queryProxyGroup(group, any()) } answers {
            ProxyGroup(
                type = "Selector",
                proxies = delays.map { (name, delay) -> proxyOf(name, delay, measured[name] ?: false) },
                now = NODE_HK,
            )
        }
        every { store.saveSpeedResults(capture(saved)) } returns Unit
        return KernelProxy(reporter, manager, config, store, geoIp, context)
    }

    private fun TestScope.collectUpdates(
        kernel: KernelProxy,
        into: MutableList<NodeLatencyUpdate>,
    ) = launch {
        kernel.nodeLatencyStream(pollIntervalMs = POLL_INTERVAL_MS, maxWaitMs = MAX_WAIT_MS).toList(into)
    }

    @Test
    fun `节点先出结果就先推送`() = runTest(mainRule.dispatcher) {
        val kernel = kernelProxy()
        val updates = mutableListOf<NodeLatencyUpdate>()
        val job = collectUpdates(kernel, updates)

        // 第一轮：只有香港测完
        delays = mapOf(NODE_HK to 80, NODE_JP to 0)
        measured = mapOf(NODE_HK to true, NODE_JP to false)
        advanceTimeBy(POLL_ADVANCE_MS)
        runCurrent()

        // 第二轮：日本也测完，这一轮之后才收尾
        delays = mapOf(NODE_HK to 80, NODE_JP to 120)
        measured = mapOf(NODE_HK to true, NODE_JP to true)
        advanceTimeBy(POLL_ADVANCE_MS)
        runCurrent()

        advanceUntilIdle()
        job.join()

        val firstFresh = updates.firstOrNull { it.fresh.isNotEmpty() }
        assertEquals("第一批只应包含先测完的节点", setOf(NODE_HK), firstFresh?.fresh?.keys)
        assertTrue("后出结果的节点应在后续批次单独推送", updates.any { it.fresh.keys == setOf(NODE_JP) })
        assertEquals("收尾要带上全部结果", mapOf(NODE_HK to 80, NODE_JP to 120), updates.last().all)
        assertTrue("每批结果都要落盘", saved.captured.containsKey(NODE_HK))
    }

    @Test
    fun `死节点测完立刻推超时并提前收尾`() = runTest(mainRule.dispatcher) {
        val kernel = kernelProxy()
        val updates = mutableListOf<NodeLatencyUpdate>()
        val job = collectUpdates(kernel, updates)

        // 第一轮：日本测完但失败（0 → 超时），香港还没轮到
        measured = mapOf(NODE_HK to false, NODE_JP to true)
        advanceTimeBy(POLL_ADVANCE_MS)
        runCurrent()

        // 第二轮：香港出结果，全部测完应提前收尾，不等满 MAX_WAIT
        delays = mapOf(NODE_HK to 80, NODE_JP to 0)
        measured = mapOf(NODE_HK to true, NODE_JP to true)
        advanceTimeBy(POLL_ADVANCE_MS)
        runCurrent()

        advanceUntilIdle()
        job.join()

        val firstFresh = updates.firstOrNull { it.fresh.isNotEmpty() }
        assertEquals(
            "先测完的失败节点要立刻推超时，不留空白和转圈",
            setOf(NODE_JP),
            firstFresh?.fresh?.keys,
        )
        assertEquals(
            "超时也是结果，要进收尾缓存",
            Constants.DELAY_TIMEOUT,
            updates.last().all[NODE_JP],
        )
        assertTrue("全部测完就应提前收尾", updates.last().finished)
        assertTrue("失败节点的超时也要落盘", saved.captured.containsKey(NODE_JP))
    }

    private companion object {
        const val NODE_HK = "[vless]香港01"
        const val NODE_JP = "[ss]日本01"
        const val POLL_INTERVAL_MS = 50L
        const val POLL_ADVANCE_MS = 120L
        const val MAX_WAIT_MS = 5_000L
    }
}
