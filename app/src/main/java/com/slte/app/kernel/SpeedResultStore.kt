package com.slte.app.kernel

interface SpeedResultStore {
    fun saveSpeedResults(results: Map<String, Int>)

    fun getSpeedResults(): Map<String, Int>?

    /** 订阅更新后调用：旧延迟不再可信，直接清掉。 */
    fun clearSpeedResults()

    /** 探测确认"后端不在了"的节点（内核节点名），与延迟缓存同生命周期。 */
    fun saveOfflineNodes(names: Set<String>)

    fun getOfflineNodes(): Set<String>?
}
