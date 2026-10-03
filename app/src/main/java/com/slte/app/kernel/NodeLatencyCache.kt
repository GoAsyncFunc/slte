package com.slte.app.kernel

import com.github.kr328.clash.core.model.ProxySort
import com.github.kr328.clash.core.model.UrlTestResult
import com.github.kr328.clash.service.remote.IClashManager

/**
 * 节点延迟的读取与缓存。
 *
 * 测速流程本身在 [nodeLatencyStream]（谁先出结果先推谁）；
 * 这里只放"怎么问内核要延迟"和"缓存里存了什么"这两件事。
 */

/** 所有代理组里节点的延迟快照；[NodeDelaySnapshot.measured] 为 false 表示还没测到，delay 不可当结果。 */
internal data class NodeDelaySnapshot(
    val delay: Int,
    val measured: Boolean,
)

internal fun KernelProxy.queryAllGroupDelays(clash: IClashManager): Map<String, NodeDelaySnapshot> = clash
    .queryProxyGroupNames(excludeNotSelectable = false)
    .asSequence()
    .filter { it != "GLOBAL" }
    .flatMap { group ->
        clash.queryProxyGroup(group, ProxySort.Delay).proxies.asSequence()
    }.filter { !it.isGroup && it.name != "DIRECT" && it.name != "REJECT" }
    .fold(mutableMapOf()) { acc, proxy ->
        val delay = normalizeDelay(proxy.delay)
        val existing = acc[proxy.name]
        if (existing == null || !existing.measured) acc[proxy.name] = NodeDelaySnapshot(delay, proxy.measured)
        acc
    }

/** 上次测速的缓存结果（按节点名），用于先渲染再测速。 */
fun KernelProxy.cachedSpeedResults(): Map<String, Int>? = speedResultStore.getSpeedResults()

/** 探测确认离线的节点（内核名），用于在节点行显示"离线"。 */
fun KernelProxy.cachedOfflineNodes(): Set<String>? = speedResultStore.getOfflineNodes()

fun KernelProxy.saveOfflineNodes(names: Set<String>) = speedResultStore.saveOfflineNodes(names)

/**
 * 内核对单个节点跑一次真实测速并分类失败原因。
 * 返回 [UrlTestResult.KIND_OFFLINE] / [UrlTestResult.KIND_TIMEOUT]，null = 存活或内核不可用。
 */
suspend fun KernelProxy.urlTestFailureKind(
    name: String,
    timeoutMs: Int,
): String? = safe(null, "urlTest") {
    manager.clash()?.urlTest(name, timeoutMs)?.kind?.takeIf { it != UrlTestResult.KIND_ALIVE }
}

/** 订阅更新后延迟会变，缓存直接失效，避免界面先显示上一份订阅的旧延迟。 */
fun KernelProxy.clearSpeedResults() = speedResultStore.clearSpeedResults()
