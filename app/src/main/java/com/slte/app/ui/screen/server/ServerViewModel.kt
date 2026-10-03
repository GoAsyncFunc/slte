package com.slte.app.ui.screen.server

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.kr328.clash.core.model.UrlTestResult
import com.slte.app.data.local.SpecialNodeSnapshot
import com.slte.app.data.repository.ServerRepository
import com.slte.app.data.repository.SubscribeRepository
import com.slte.app.kernel.KernelProxy
import com.slte.app.kernel.KernelServerInfo
import com.slte.app.kernel.LIVE_SELECTION_BUSY_POLL_MS
import com.slte.app.kernel.NodeNameResolver
import com.slte.app.kernel.SelectionType
import com.slte.app.kernel.cachedOfflineNodes
import com.slte.app.kernel.cachedSpeedResults
import com.slte.app.kernel.groupByTypeCurrentNode
import com.slte.app.kernel.nodeLatencyStream
import com.slte.app.kernel.nodeNames
import com.slte.app.kernel.saveOfflineNodes
import com.slte.app.kernel.selectAuto
import com.slte.app.kernel.selectFallback
import com.slte.app.kernel.selectNode
import com.slte.app.kernel.serverInfo
import com.slte.app.kernel.urlTestFailureKind
import com.slte.app.utils.Constants
import com.slte.app.utils.ErrorMessages
import com.slte.app.utils.extractCountryCode
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ServerViewModel
@Inject
constructor(
    private val serverRepository: ServerRepository,
    private val subscribeRepository: SubscribeRepository,
    private val kernelProxy: KernelProxy,
) : ViewModel() {

    private fun hasPlan(): Boolean = subscribeRepository.getCachedSubscribeInfo()?.hasPlan ?: true

    private val _data = MutableStateFlow(ServerData())
    val data: StateFlow<ServerData> = _data.asStateFlow()

    private val _errorMessageRes = MutableStateFlow<Int?>(null)
    val errorMessageRes: StateFlow<Int?> = _errorMessageRes.asStateFlow()

    private val refreshSeq = AtomicInteger()

    private val kernelTagToType =
        mapOf(
            "vless" to "vless",
            "vmess" to "vmess",
            "trojan" to "trojan",
            "ss" to "shadowsocks",
            "hy" to "hysteria",
            "hy2" to "hysteria2",
            "tuic" to "tuic",
            "anytls" to "anytls",
            "socks" to "socks",
        )

    init {

        serverRepository.getCachedServers()?.let { applyNodes(it) }
        applySpecialNodeSnapshots()
        refreshSpecialNodes()
    }

    private fun refreshSpecialNodes() {
        val seq = refreshSeq.incrementAndGet()
        viewModelScope.launch {
            val auto = kernelProxy.groupByTypeCurrentNode("URLTest")
            val fallback = kernelProxy.groupByTypeCurrentNode("Fallback")
            val info = kernelProxy.serverInfo()
            val kernelNames = kernelProxy.nodeNames()
            val cachedDelays = kernelProxy.cachedSpeedResults()
            val cachedOffline = kernelProxy.cachedOfflineNodes().orEmpty()
            if (seq != refreshSeq.get()) return@launch
            _data.update { state ->
                val index = kernelNameIndex(kernelNames)
                val nodes =
                    state.nodes.map { node ->
                        val proxyName = resolveKernelName(index, node)
                        node.copy(
                            proxyName = proxyName,
                            delay = proxyName?.let { cachedDelays?.get(it) } ?: node.delay,
                            offline = proxyName in cachedOffline,
                        )
                    }
                // 自动/故障转移当前成员的延迟：测速流每批都会写缓存，这里顺带取出来，
                // 让这两行"拿到值就出结果"，而不是等整轮测速结束。
                val autoDelay = cachedDelays?.get(auto)?.takeIf { it != Constants.DELAY_TIMEOUT }
                val fallbackDelay = cachedDelays?.get(fallback)?.takeIf { it != Constants.DELAY_TIMEOUT }
                state.copy(
                    nodes = nodes,
                    autoNode = auto?.let(NodeNameResolver::displayName),
                    fallbackNode = fallback?.let(NodeNameResolver::displayName),
                    autoNodeCountryCode = countryOf(nodes, auto),
                    fallbackNodeCountryCode = countryOf(nodes, fallback),
                    autoNodeDelay = autoDelay ?: state.autoNodeDelay,
                    kernelFallbackDelay = fallbackDelay ?: state.kernelFallbackDelay,
                    selectedNodeId = selectedNodeIdOf(nodes, info) ?: state.selectedNodeId,
                )
            }
            persistSpecialNodeSnapshots(auto, fallback)
        }
    }

    /** 实时值落盘成快照，下次冷启动内核还没上报时，两行先用上次的成员和国旗垫显示。 */
    private fun persistSpecialNodeSnapshots(auto: String?, fallback: String?) {
        val state = _data.value
        auto?.let {
            serverRepository.saveAutoNodeSnapshot(
                SpecialNodeSnapshot(
                    kernelName = it,
                    displayName = state.autoNode.orEmpty(),
                    countryCode = state.autoNodeCountryCode.orEmpty(),
                    delay = state.autoNodeDelay,
                ),
            )
        }
        fallback?.let {
            serverRepository.saveFallbackNodeSnapshot(
                SpecialNodeSnapshot(
                    kernelName = it,
                    displayName = state.fallbackNode.orEmpty(),
                    countryCode = state.fallbackNodeCountryCode.orEmpty(),
                    delay = state.kernelFallbackDelay,
                ),
            )
        }
    }

    /** 内核还没上报时，先用快照把两行填上；实时值到达后由 [refreshSpecialNodes] 覆盖。 */
    private fun applySpecialNodeSnapshots() {
        val auto = serverRepository.getAutoNodeSnapshot()
        val fallback = serverRepository.getFallbackNodeSnapshot()
        if (auto == null && fallback == null) return
        _data.update { state ->
            state.copy(
                autoNode = auto?.displayName ?: state.autoNode,
                autoNodeCountryCode = auto?.countryCode?.takeIf { it.isNotEmpty() } ?: state.autoNodeCountryCode,
                autoNodeDelay = auto?.delay ?: state.autoNodeDelay,
                fallbackNode = fallback?.displayName ?: state.fallbackNode,
                fallbackNodeCountryCode = fallback?.countryCode?.takeIf { it.isNotEmpty() } ?: state.fallbackNodeCountryCode,
                kernelFallbackDelay = fallback?.delay ?: state.kernelFallbackDelay,
            )
        }
    }

    private fun kernelNameIndex(kernelNames: List<String>): Map<String, List<String>> = kernelNames
        .groupBy { NodeNameResolver.of(it) }
        .filterKeys { it.isNotEmpty() }

    private fun resolveKernelName(
        index: Map<String, List<String>>,
        node: NodeItem,
    ): String? {
        val candidates = index[NodeNameResolver.of(node.name)] ?: return null
        candidates.singleOrNull()?.let { return it }

        val type = node.type.lowercase()
        return candidates
            .filter { candidate -> NodeNameResolver.protocolTag(candidate)?.let(kernelTagToType::get) == type }
            .singleOrNull()
    }

    private fun selectedNodeIdOf(
        nodes: List<NodeItem>,
        info: KernelServerInfo?,
    ): Int? {
        val current = info?.node
        return when (info?.selection) {
            SelectionType.AUTO -> 0
            SelectionType.FALLBACK -> -1
            SelectionType.MANUAL ->
                current?.let { name ->
                    nodes.firstOrNull { it.proxyName == name }?.id
                        ?: nodes.firstOrNull { it.name == name }?.id
                        ?: matchedNode(nodes, name)?.id
                }
            null -> null
        }
    }

    private fun matchedNode(
        nodes: List<NodeItem>,
        name: String,
    ): NodeItem? {
        val key = NodeNameResolver.of(name)
        if (key.isEmpty()) return null
        return nodes.filter { NodeNameResolver.of(it.name) == key }.singleOrNull()
    }

    private fun countryOf(
        nodes: List<NodeItem>,
        nodeName: String?,
    ): String? = nodeName?.let { name ->
        nodes
            .firstOrNull { it.proxyName == name || it.name == name }
            ?.countryCode
            ?.takeIf { it != "XX" }
    }

    suspend fun refreshNodesForPurchase() {
        serverRepository.fetchServers(force = true).fold(
            onSuccess = { applyNodes(it) },
            onFailure = { _errorMessageRes.value = ErrorMessages.forServer(it) },
        )
    }

    fun retry() {
        loadNodes()
    }

    fun dismissError() {
        _errorMessageRes.value = null
    }

    fun loadNodes(force: Boolean = false) {
        _errorMessageRes.value = null
        _data.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            serverRepository.fetchServers(force = force).fold(
                onSuccess = ::applyNodes,
                onFailure = { throwable ->
                    _data.update { it.copy(isLoading = false) }
                    _errorMessageRes.value = ErrorMessages.forServer(throwable)
                },
            )
        }
    }

    private fun applyNodes(servers: List<com.slte.app.domain.model.ServerNode>) {
        val existing = _data.value.nodes.associate { it.name to it.delay }
        val offlineByName = _data.value.nodes.associate { it.name to it.offline }
        val nodes =
            servers
                .distinctBy { it.name }
                .mapIndexed { index, server ->
                    NodeItem(
                        id = index + 1,
                        name = server.name,
                        countryCode = extractCountryCode(server.name),
                        type = server.type.name,
                        host = server.host,
                        delay = existing[server.name],
                        offline = offlineByName[server.name] ?: false,
                    )
                }
        _data.update { it.copy(nodes = nodes, isLoading = false) }
        refreshSpecialNodes()
    }

    fun selectNode(nodeId: Int) {
        when (nodeId) {
            0 -> {
                _data.update { it.copy(selectedNodeId = 0) }
                viewModelScope.launch {
                    kernelProxy.selectAuto()
                    refreshSpecialNodes()
                }
            }
            -1 -> {
                _data.update { it.copy(selectedNodeId = -1) }
                viewModelScope.launch {
                    kernelProxy.selectFallback()
                    refreshSpecialNodes()
                }
            }
            else -> {
                val node = _data.value.nodes.firstOrNull { it.id == nodeId } ?: return
                viewModelScope.launch {
                    if (kernelProxy.selectNode(node.proxyName ?: node.name)) {
                        _data.update { it.copy(selectedNodeId = nodeId) }
                    }
                    refreshSpecialNodes()
                }
            }
        }
    }

    fun startSpeedTest() {
        if (_data.value.isTesting) return
        if (!hasPlan()) return
        _data.update { it.copy(isTesting = true, testedNodes = emptySet()) }
        _errorMessageRes.value = null
        viewModelScope.launch {
            var latest: Map<String, Int> = emptyMap()
            var lastSpecialRefresh = 0L
            kernelProxy.nodeLatencyStream().collect { update ->
                latest = update.all
                if (update.fresh.isNotEmpty()) {
                    // 谁先出结果就先落到列表里
                    applyFreshDelays(update.fresh)
                    // 自动选择/故障转移两行跟着内核的实时切换走，但别每批都刷，避免刷爆 IPC
                    val now = System.currentTimeMillis()
                    if (now - lastSpecialRefresh >= LIVE_SELECTION_BUSY_POLL_MS) {
                        lastSpecialRefresh = now
                        refreshSpecialNodes()
                    }
                }
            }
            // 没出结果的节点逐个探活：只有确认后端不在了才记离线，缓存随延迟一起落盘
            val offlineNodes = probeFailedNodes(latest)
            if (offlineNodes.isNotEmpty()) kernelProxy.saveOfflineNodes(offlineNodes)
            finishSpeedTest(latest, offlineNodes)
        }
    }

    /**
     * 测速没出结果的节点逐个走内核真实测速，确认"后端不在了"（域名解析失败/连接被拒）才记离线；
     * 判定不了的（在但不回包、内核里已无此节点等）一律不标，保持超时语义。
     * 内核没跑起来（latest 为空）时不测，避免把整页节点误判成离线。
     */
    private suspend fun probeFailedNodes(latest: Map<String, Int>): Set<String> {
        if (latest.isEmpty()) return emptySet()
        val failed =
            _data.value.nodes
                .filter { it.proxyName != null }
                .filter { node ->
                    val key = node.proxyName ?: return@filter false
                    latest[key] == null || latest[key] == Constants.DELAY_TIMEOUT
                }
        if (failed.isEmpty()) return emptySet()
        return coroutineScope {
            failed
                .map { node ->
                    async {
                        node.proxyName?.takeIf {
                            kernelProxy.urlTestFailureKind(
                                name = it,
                                timeoutMs = Constants.NODE_URLTEST_TIMEOUT_MS,
                            ) == UrlTestResult.KIND_OFFLINE
                        }
                    }
                }.awaitAll().filterNotNull().toSet()
        }
    }

    /** 增量落结果：只用本次新出的延迟覆盖对应节点，不覆盖用户手动设置过的值。 */
    private fun applyFreshDelays(fresh: Map<String, Int>) {
        _data.update { state ->
            val nodes =
                state.nodes.map { node ->
                    val delay = fresh[node.proxyName ?: node.name]
                    if (delay != null && node.name !in state.testedNodes) node.copy(delay = delay) else node
                }
            val tested =
                state.nodes.mapNotNull { node ->
                    node.name.takeIf { fresh[node.proxyName ?: node.name] != null }
                }
            state.copy(nodes = nodes, testedNodes = state.testedNodes + tested)
        }
    }

    /** 收尾：没测到结果的节点回退到缓存值，测过但失败的标"超时"，并刷新自动/故障转移两行。 */
    private suspend fun finishSpeedTest(
        latest: Map<String, Int>,
        offlineNodes: Set<String>,
    ) {
        val cached = kernelProxy.cachedSpeedResults()
        _data.update { state ->
            val nodes =
                state.nodes.map { node ->
                    val key = node.proxyName ?: node.name
                    val measured = latest[key]?.takeIf { it != Constants.DELAY_TIMEOUT }
                    // 内核测过但没出延迟（且不是离线）：按超时收尾，不留空白
                    val failed =
                        latest.isNotEmpty() &&
                            node.proxyName != null &&
                            (latest[key] == null || latest[key] == Constants.DELAY_TIMEOUT)
                    node.copy(
                        delay = measured ?: cached?.get(key) ?: if (failed) Constants.DELAY_TIMEOUT else node.delay,
                        offline = key in offlineNodes,
                    )
                }
            state.copy(
                nodes = nodes,
                isTesting = false,
                testedNodes = emptySet(),
            )
        }
        refreshSpecialNodes()
    }

    fun updateSubscription() {
        if (!hasPlan()) return
        loadNodes(force = true)
    }
}

@Immutable
data class ServerData(
    val nodes: List<NodeItem> = emptyList(),
    val selectedNodeId: Int = 0,
    val isLoading: Boolean = false,
    val isTesting: Boolean = false,

    val testedNodes: Set<String> = emptySet(),

    val kernelFallbackDelay: Int? = null,

    val autoNode: String? = null,

    val fallbackNode: String? = null,

    val autoNodeCountryCode: String? = null,

    val fallbackNodeCountryCode: String? = null,

    /** 自动选择组当前成员的延迟（来自测速流缓存）。 */
    val autoNodeDelay: Int? = null,
) {

    val autoDelay: Int?
        get() = autoNodeDelay ?: nodes
            .asSequence()
            .mapNotNull { it.delay }
            .filter { it != Constants.DELAY_TIMEOUT }
            .minOrNull()

    val fallbackDelay: Int?
        get() =
            kernelFallbackDelay ?: nodes
                .asSequence()
                .mapNotNull { it.delay }
                .filter { it != Constants.DELAY_TIMEOUT }
                .sorted()
                .toList()
                .getOrNull(1)
}

data class NodeItem(
    val id: Int = 0,
    val name: String,
    val countryCode: String = "XX",
    val type: String = "",
    val host: String = "",
    val delay: Int? = null,
    val proxyName: String? = null,
    val offline: Boolean = false,
)
