package com.slte.app.data.remote.adapter.xiaov2b

import com.slte.app.domain.model.ServerNode
import com.slte.app.domain.model.ServerType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * v2board 的节点列表。
 *
 * 两个面板的 `user/server/fetch` 字段结构不同（v2board 是原始协议行、Xboard 是 NodeResource），
 * 这一块不共享：v2board 侧字段多，还要归一化协议类型与 TLS 设置。
 */
@Serializable
data class XiaoV2bServerData(
    val id: Int = 0,
    val name: String = "",
    val type: String = "",
    val host: String = "",
    val port: Int = 0,
    @SerialName("server_port") val serverPort: Int = 0,
    val cipher: String? = null,
    val password: String? = null,
    val uuid: String? = null,
    @SerialName("alter_id") val alterId: Int = 0,
    val network: String? = null,
    @SerialName("network_settings") val networkSettings: JsonElement? = null,
    val tls: Int = 0,
    @SerialName("tls_settings") val tlsSettings: JsonElement? = null,
    val obfs: String? = null,
    @SerialName("obfs_settings") val obfsSettings: JsonElement? = null,
    val flow: String? = null,
    val method: String? = null,
    val protocol: String? = null,
    @SerialName("obfs_password") val obfsPassword: String? = null,

    @SerialName("group_id") val groupId: JsonElement? = null,
    val show: Int = 1,
    @SerialName("is_online") val isOnline: Int = 1,
    val description: String? = null,
) {
    private fun resolveType(): ServerType {
        if (type == "v2node" && protocol != null) {
            return when (protocol) {
                "hysteria2" -> ServerType.HYSTERIA2
                "hysteria" -> ServerType.HYSTERIA
                "shadowsocks" -> ServerType.SHADOWSOCKS
                "vmess" -> ServerType.VMESS
                "vless" -> ServerType.VLESS
                "trojan" -> ServerType.TROJAN
                "tuic" -> ServerType.TUIC
                "anytls" -> ServerType.ANYTLS
                else -> ServerType.SHADOWSOCKS
            }
        }
        return when (type) {
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
    }

    private fun extractSni(): String {
        val settings = tlsSettings ?: return ""
        val obj =
            when (settings) {
                is JsonObject -> settings
                is JsonPrimitive ->
                    runCatching {
                        Json.parseToJsonElement(settings.content) as? JsonObject
                    }.getOrNull()
                else -> null
            } ?: return ""
        return (obj["server_name"] as? JsonPrimitive)?.content ?: ""
    }

    fun toServerNode() = ServerNode(
        id = id,
        name = name,
        type = resolveType(),
        host = host,
        port = port,
        serverPort = serverPort,
        cipher = cipher ?: method ?: "",
        password = password.orEmpty(),
        obfsPassword = obfsPassword.orEmpty(),
        uuid = uuid ?: "",
        alterId = alterId,
        network = network ?: "tcp",
        networkSettings = networkSettings?.let { jsonElementToStr(it) },
        tls = tls == 1,
        tlsSettings = tlsSettings?.let { jsonElementToStr(it) },
        obfs = obfs ?: "",
        obfsSettings = obfsSettings?.let { jsonElementToStr(it) },
        flow = flow ?: "",
        sni = extractSni(),
        groupId =
        when (groupId) {
            is JsonArray -> (groupId.firstOrNull() as? JsonPrimitive)?.content?.toIntOrNull() ?: 0
            is JsonPrimitive -> groupId.content.toIntOrNull() ?: 0
            else -> 0
        },
    )
}

private fun jsonElementToStr(el: JsonElement): String = when (el) {
    is JsonPrimitive -> el.content
    else -> el.toString()
}
