package com.slte.app.ui.screen.main

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.slte.app.R
import com.slte.app.data.remote.FallbackDns
import com.slte.app.di.IoDispatcher
import com.slte.app.kernel.KernelConfig
import com.slte.app.kernel.KernelManager
import com.slte.app.kernel.KernelProxy
import com.slte.app.kernel.NodeNameResolver
import com.slte.app.kernel.ensureGlobalSelection
import com.slte.app.kernel.fetchPublicIp
import com.slte.app.kernel.liveSelectionFlow
import com.slte.app.kernel.refreshSelectionAndMeasure
import com.slte.app.utils.AppLog
import com.slte.app.utils.ErrorMessages
import com.slte.app.utils.sanitizeLog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class MainViewModel
@Inject
constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val kernelManager: KernelManager,
    private val kernelProxy: KernelProxy,
    private val kernelConfig: KernelConfig,
    private val fallbackDns: FallbackDns,
    private val subscriptionUpdater: SubscriptionUpdater,
    private val dataWriter: DashboardDataWriter,
) : ViewModel() {
    private val _data = MutableStateFlow(DashboardData())
    val data: StateFlow<DashboardData> = _data.asStateFlow()

    init {

        dataWriter.applyCached(_data)
        dataWriter.seedServerName(_data)
        refresh()
        dataWriter.loadServers(viewModelScope, _data)
        observeKernelState()
        observeProfileLoaded()
        viewModelScope.launch { subscriptionUpdater.maybeSilentUpdate(_data, viewModelScope) }

        viewModelScope.launch {
            repeat(10) {
                if (kernelProxy.warmUp()) return@launch
                delay(1000)
            }
        }
    }

    private fun observeProfileLoaded() {
        viewModelScope.launch {
            kernelManager.profileLoaded.collect {
                refreshKernelInfo()
            }
        }
    }

    private fun observeKernelState() {
        viewModelScope.launch {
            kernelManager.connected.collect { connected ->
                _data.update { it.copy(isConnected = connected, isConnecting = false) }
                if (connected) {
                    fallbackDns.clearCache()
                    viewModelScope.launch { startAutoSelectionAndTest() }
                    refreshKernelInfo()
                }
            }
        }
    }

    /**
     * 节点名与国旗的实时来源：直接跟随内核当前的落点节点。
     *
     * 自动选择/故障转移时内核会随健康检查结果自己换节点，这里只是把"它现在用的是哪个"
     * 实时反映到界面上——不依赖手动测速或更新订阅。
     *
     * 由界面在「已连接且首页可见」时调用，离开页面/断开即随协程取消，不在后台常驻轮询。
     */
    suspend fun watchLiveSelection() {
        kernelProxy.liveSelectionFlow().collect { live ->
            val kernelName = live.node ?: return@collect
            val display = NodeNameResolver.displayName(kernelName)
            _data.update { state ->
                if (state.hasPlan) {
                    state.copy(serverName = display)
                } else {
                    state
                }
            }
        }
    }

    /**
     * 立刻把选择方式交给内核（自动选择/故障转移即时生效），再在后台跑一次测速把延迟写进缓存。
     *
     * 测速是流式的：谁先出结果谁先落盘，不会阻塞界面，也不会等所有节点测完才切。
     */
    private suspend fun startAutoSelectionAndTest() {
        kernelProxy.refreshSelectionAndMeasure()
    }

    fun refreshKernelInfo() {
        viewModelScope.launch {
            withContext(ioDispatcher) {
                kernelProxy.ensureGlobalSelection()

                kernelProxy.ensurePersistedMode()
                kernelProxy.proxyMode()?.let { mode ->
                    _data.update { it.copy(proxyMode = mode) }
                }
                if (_data.value.hasPlan) {
                    kernelProxy.fetchPublicIp()?.let { info ->
                        _data.update {
                            it.copy(
                                currentIp = info.ip,
                                ipCountryCode = info.countryCode,
                            )
                        }
                    }
                }
            }
        }
    }

    fun refresh(force: Boolean = false) {
        viewModelScope.launch { subscriptionUpdater.refresh(_data, force = force) }
    }

    fun updateSubscription() {
        viewModelScope.launch { subscriptionUpdater.updateSubscription(_data, viewModelScope) }
    }

    fun refreshAfterPurchase(tradeNo: String? = null): Job = subscriptionUpdater.refreshAfterPurchase(_data, tradeNo, viewModelScope)

    fun finishPurchaseRefresh() {
        dataWriter.finishPurchaseRefresh(_data)
    }

    fun toggleConnection() {
        val current = _data.value
        if (current.isConnecting) return

        if (!current.hasPlan) return

        AppLog.i("SLTE-Main", "toggleConnection: connected=${current.isConnected} -> ${!current.isConnected}")
        if (current.isConnected) {
            kernelManager.stopVpn()
        } else {
            _data.update { it.copy(isConnecting = true, errorMessageRes = null) }
            viewModelScope.launch {
                try {
                    val profile = kernelConfig.ensureProfile()
                    if (profile == null) {
                        AppLog.w("SLTE-Main", "toggleConnection: ensureProfile 返回 null，内核不可用")
                        _data.update {
                            it.copy(
                                isConnecting = false,
                                errorMessageRes = R.string.error_vpn_kernel_unavailable,
                            )
                        }
                        return@launch
                    }
                    kernelManager.startVpn()
                } catch (e: Exception) {
                    AppLog.w("SLTE-Main", "toggleConnection: 启动失败 ${sanitizeLog(e.message ?: "Unknown")}")
                    _data.update {
                        it.copy(
                            isConnecting = false,
                            errorMessageRes = ErrorMessages.networkError(),
                        )
                    }
                }
            }
        }
    }

    fun vpnRequestIntent(): Intent? = kernelManager.vpnRequestIntent()

    fun setProxyMode(mode: String) {
        _data.update { it.copy(proxyMode = mode) }
        viewModelScope.launch {
            kernelProxy.setProxyMode(mode)
        }
    }

    fun clearError() {
        _data.update { it.copy(errorMessageRes = null) }
    }

    fun cancelUpdating() {
        _data.update { it.copy(isUpdating = false) }
    }

    fun onVpnPermissionDenied() {
        AppLog.w("SLTE-Main", "VPN 授权被拒绝，连接未建立")
        _data.update {
            it.copy(isConnecting = false, errorMessageRes = R.string.error_vpn_permission_denied)
        }
    }
}
