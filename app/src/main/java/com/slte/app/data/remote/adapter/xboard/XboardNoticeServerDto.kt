package com.slte.app.data.remote.adapter.xboard

import com.slte.app.domain.model.ServerNode
import com.slte.app.domain.model.ServerType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Xboard 的节点列表。
 *
 * 两个面板的 `user/server/fetch` 字段结构不同（Xboard 是 NodeResource：只有元信息，
 * v2board 是原始协议行），这一块不共享。
 */
@Serializable
data class XboardServerData(
    val id: Int = 0,

    val type: String = "",
    val version: String? = null,
    val name: String = "",

    val rate: JsonElement? = null,
    val tags: List<String>? = null,
    @SerialName("is_online")
    val isOnline: Int = 1,

    // 连接字段：部分 xboard 版本的 server/fetch 不下发，缺省为空；
    // 节点的真实连接信息以订阅 YAML 为准，这里只影响列表展示
    val host: String = "",
    val port: Int = 0,
) {
    private fun resolveType(): ServerType = when (type) {
        "shadowsocks" -> ServerType.SHADOWSOCKS
        "vmess" -> ServerType.VMESS
        "vless" -> ServerType.VLESS
        "trojan" -> ServerType.TROJAN
        "tuic" -> ServerType.TUIC
        "hysteria" -> ServerType.HYSTERIA
        "hysteria2" -> ServerType.HYSTERIA2
        "anytls" -> ServerType.ANYTLS
        else -> ServerType.SHADOWSOCKS
    }

    fun toServerNode() = ServerNode(
        id = id,
        name = name,
        type = resolveType(),
        host = host,
        port = port,
    )
}
