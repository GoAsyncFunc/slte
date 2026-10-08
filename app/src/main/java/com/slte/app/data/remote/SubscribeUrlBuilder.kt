package com.slte.app.data.remote

import com.slte.app.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal fun subscribeFetchUrl(
    apiBaseUrl: String,
    token: String,
): String? = runCatching {
    (apiBaseUrl.trimEnd('/') + BuildConfig.SUBSCRIBE_PATH)
        .toHttpUrlOrNull()
        ?.newBuilder()
        ?.addQueryParameter("token", token)
        ?.build()
        ?.toString()
}.getOrNull()
