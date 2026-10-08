package com.slte.app.data.remote

object ApiPaths {

    const val PREFIX = "/api/v1"

    const val AUTH = "$PREFIX/user/"

    const val GUEST_CONFIG = "$PREFIX/guest/comm/config"
}

data class ApiBackend(
    val type: String,
    val baseUrl: String,
    val apiPrefix: String = ApiPaths.PREFIX,
)

/** Supported API panel families. Configuration strings are resolved at the boundary. */
internal enum class ApiBackendType(val configValue: String) {
    V2BOARD("xiaov2b"),
    XBOARD("xboard"), ;

    companion object {
        fun fromConfig(value: String): ApiBackendType? = entries.firstOrNull { it.configValue == value.trim().lowercase() }
    }
}
