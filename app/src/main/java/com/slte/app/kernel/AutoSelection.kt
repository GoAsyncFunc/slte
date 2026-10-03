package com.slte.app.kernel

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow

/**
 * 当前生效的选择方式与真实落点节点。
 *
 * 自动选择/故障转移时 [node] 是组内当前节点（不是"自动选择"这个组名），
 * 首页的节点名与国旗就靠它实时刷新。
 */
data class LiveSelection(
    val type: SelectionType,
    val node: String?,
)

/**
 * 立即切换选择方式，不等测速跑完。
 *
 * 自动选择/故障转移由内核按健康检查结果实时挑节点，所以我们要做的是"马上把
 * selector 指过去"，而不是等自己测完所有节点再切。
 */
suspend fun KernelProxy.applySelection(type: SelectionType): Boolean = when (type) {
    SelectionType.AUTO -> selectAuto()
    SelectionType.FALLBACK -> selectFallback()
    SelectionType.MANUAL -> false
}

/**
 * 实时监听真实落点节点：`intervalMs` 轮询一次，值没变就不推送。
 *
 * 取消上游协程即停止轮询（首页按连接状态启停，服务器页在页面存活期间收集）。
 */
fun KernelProxy.liveSelectionFlow(intervalMs: Long = LIVE_SELECTION_POLL_MS): Flow<LiveSelection> = flow {
    while (true) {
        val info = serverInfo()
        emit(LiveSelection(info?.selection ?: SelectionType.MANUAL, info?.node))
        delay(intervalMs)
    }
}.distinctUntilChanged()

/** 常态监听间隔：够快看出切换，又不至于频繁打内核 IPC。 */
const val LIVE_SELECTION_POLL_MS = 1_500L

/** 测速/更新订阅期间的监听间隔：内核正在挑节点，需要更快反映。 */
const val LIVE_SELECTION_BUSY_POLL_MS = 600L

/**
 * 让内核按当前选择方式实时挑节点，并跑一轮测速把结果写进缓存。
 *
 * 连接建立、订阅更新后都用它：**先重放持久化的选择**（重启后内核不保留组选择，
 * 见 [KernelProxy.ensurePersistedSelection]），再流式收结果——不阻塞界面，也不等所有节点测完。
 */
suspend fun KernelProxy.refreshSelectionAndMeasure() {
    ensurePersistedSelection()
    serverInfo()?.selection?.let { selection ->
        if (selection == SelectionType.AUTO || selection == SelectionType.FALLBACK) {
            applySelection(selection)
        }
    }
    nodeLatencyStream().collect { }
}
