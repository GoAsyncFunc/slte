package com.slte.app.kernel

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 一批测速增量结果。
 *
 * @property fresh 本次新拿到结果的节点（key 为内核节点名）
 * @property all 到目前为止已知的全部结果
 * @property finished 是否已收尾（所有节点有结果，或到达等待上限）
 */
data class NodeLatencyUpdate(
    val fresh: Map<String, Int>,
    val all: Map<String, Int>,
    val finished: Boolean,
)

/**
 * 节点测速流：哪个节点先出结果就先推哪一批，不等所有节点测完。
 *
 * 首页与服务器页共用这一条流——服务器页把 [NodeLatencyUpdate.fresh] 立刻写进列表，
 * 首页只需要它驱动内核健康检查并把结果写进缓存；每批结果都会落盘，进程被杀也能保住。
 *
 * 整个流跑在 IO 线程（[flowOn]）：`healthCheckAll`/`queryProxyGroup` 都是阻塞的跨进程
 * binder 调用，且内核服务运行在独立进程；每 400ms 轮询一次、最长 45 秒，
 * 再加每批结果的 JSON 编码与加密偏好写盘，放在主线程会直接卡界面甚至 ANR。
 */
fun KernelProxy.nodeLatencyStream(
    pollIntervalMs: Long = LATENCY_POLL_INTERVAL_MS,
    maxWaitMs: Long = LATENCY_MAX_WAIT_MS,
): Flow<NodeLatencyUpdate> = flow {
    val clash = manager.awaitClash()
    if (clash == null) {
        emit(NodeLatencyUpdate(emptyMap(), emptyMap(), finished = true))
        return@flow
    }
    if (selectorGroup() == null) {
        config.ensureProfile()
        clash.loadActiveProfile()
        if (waitForGroups() == null) {
            emit(NodeLatencyUpdate(emptyMap(), emptyMap(), finished = true))
            return@flow
        }
    }

    clash.healthCheckAll()

    val known = mutableMapOf<String, Int>()
    // 用协程超时而不是挂钟时间：既能真正给测速兜底，也能在虚拟时间的单元测试里正常收敛
    withTimeoutOrNull(maxWaitMs) {
        while (true) {
            val snapshot = queryAllGroupDelays(clash)
            // 只把"内核已经测完"（measured，含失败）的节点当结果：死节点测完立刻推"超时"并停止转圈，
            // 还没轮到的继续等；全部测完就提前收尾，不用干等满 45 秒。
            // 其中后端已不存在的，收尾时由内核 urlTest 改判为离线
            val fresh =
                snapshot
                    .filter { it.value.measured }
                    .mapValues { it.value.delay }
                    .filter { (name, delay) -> known[name] != delay }
            if (fresh.isNotEmpty()) {
                known.putAll(fresh)
                speedResultStore.saveSpeedResults(known.toMap())
                emit(NodeLatencyUpdate(fresh = fresh, all = known.toMap(), finished = false))
            }
            if (snapshot.isNotEmpty() && snapshot.values.all { it.measured }) return@withTimeoutOrNull
            delay(pollIntervalMs)
        }
    }
    emit(NodeLatencyUpdate(emptyMap(), known.toMap(), finished = true))
}.flowOn(faultReporter.ioDispatcher)

/** 服务器页测速时的轮询间隔：够快能看出"谁先出结果"，又不至于刷爆内核 IPC。 */
const val LATENCY_POLL_INTERVAL_MS = 400L

/** 一次测速的最长等待时间；超时就用已有结果收尾，不把界面卡住。 */
const val LATENCY_MAX_WAIT_MS = 45_000L
