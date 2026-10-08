package com.slte.app.kernel

import android.content.Context
import com.slte.app.data.local.InMemoryPreferences
import com.slte.app.support.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SelectNodeTest {
    @get:Rule
    val mainRule = MainDispatcherRule()

    private val prefs = InMemoryPreferences()
    private val context = mockk<Context>(relaxed = true)
    private val manager = mockk<KernelManager>(relaxed = true)
    private val clash = mockk<KernelClash>(relaxed = true)
    private val config = mockk<KernelConfig>(relaxed = true)
    private val store = mockk<SpeedResultStore>(relaxed = true)
    private val geoIp = mockk<GeoIpResolver>(relaxed = true)
    private val reporter = KernelFaultReporter(mainRule.dispatcher)

    private val group = "节点选择"

    private var members: List<KernelProxySnapshot> = emptyList()
    private var selected: String = ""

    private fun node(name: String) = KernelProxySnapshot(name = name, isGroup = false, delay = 100, measured = true)

    private fun kernelProxy(): KernelProxy {
        every { context.getSharedPreferences(any(), any()) } returns prefs
        every { context.packageName } returns "com.slte.app"
        coEvery { manager.awaitClash() } returns clash
        every { clash.queryTunnelMode() } returns KernelTunnelMode.RULE
        every { clash.queryProxyGroupNames(any()) } returns listOf(group)
        every { clash.queryProxyGroup(group, KernelProxySort.DEFAULT) } answers {
            KernelProxyGroupSnapshot(type = "Selector", proxies = members, now = selected)
        }
        every { clash.patchSelector(group, any()) } answers {
            selected = secondArg()
            true
        }
        return KernelProxy(reporter, manager, config, store, geoIp, context)
    }

    private fun givenNodes(vararg names: String) {
        members = names.map(::node)
        selected = members.first().name
    }

    @Test
    fun `订阅名字带协议前缀时按规范化结果切换`() = runTest(mainRule.dispatcher) {
        givenNodes("[vless]香港01", "[ss]日本01")
        val kernel = kernelProxy()

        assertTrue(kernel.selectNode("日本01"))
        verify { clash.patchSelector(group, "[ss]日本01") }
    }

    @Test
    fun `名字完全一致时直接切换`() = runTest(mainRule.dispatcher) {
        givenNodes("香港01", "日本01")
        val kernel = kernelProxy()

        assertTrue(kernel.selectNode("日本01"))
        verify { clash.patchSelector(group, "日本01") }
    }

    @Test
    fun `分组内没有该节点时不下发切换`() = runTest(mainRule.dispatcher) {
        givenNodes("[vless]香港01", "[ss]日本01")
        val kernel = kernelProxy()

        assertFalse(kernel.selectNode("韩国01"))
        verify(exactly = 0) { clash.patchSelector(any(), any()) }
    }

    @Test
    fun `内核未真正切换时判定失败`() = runTest(mainRule.dispatcher) {
        givenNodes("[vless]香港01", "[ss]日本01")
        val kernel = kernelProxy()
        every { clash.patchSelector(group, any()) } returns true

        assertFalse(kernel.selectNode("[ss]日本01"))
    }

    @Test
    fun `归一化后存在多个候选时拒绝切换`() = runTest(mainRule.dispatcher) {
        givenNodes("[vless]香港01", "[ss]香港01")
        val kernel = kernelProxy()

        assertFalse(kernel.selectNode("🇭🇰香港01"))
    }

    @Test
    fun `切换后短暂读到旧状态时按最终状态判定`() = runTest(mainRule.dispatcher) {
        givenNodes("[vless]香港01", "[ss]日本01")
        val kernel = kernelProxy()
        var patched = false
        var staleOnce = false
        every { clash.patchSelector(group, any()) } answers {
            selected = secondArg()
            patched = true
            true
        }
        every { clash.queryProxyGroup(group, KernelProxySort.DEFAULT) } answers {
            val now =
                if (patched && !staleOnce) {
                    staleOnce = true
                    members.first().name
                } else {
                    selected
                }
            KernelProxyGroupSnapshot(type = "Selector", proxies = members, now = now)
        }

        assertTrue(kernel.selectNode("[ss]日本01"))
    }

    @Test
    fun `列出内核节点名并排除直连与分组`() = runTest(mainRule.dispatcher) {
        givenNodes("[vless]香港01", "DIRECT", "REJECT", "[ss]日本01")
        val kernel = kernelProxy()

        assertEquals(
            listOf("[vless]香港01", "[ss]日本01"),
            kernel.nodeNames(),
        )
    }
}
