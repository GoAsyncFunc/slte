package com.slte.app.data.remote

import com.slte.app.BuildConfig
import com.slte.app.data.remote.config.ApiFailoverInterceptor
import com.slte.app.data.remote.config.RemoteConfig
import com.slte.app.utils.ApiErrors
import com.slte.app.utils.AppLog
import com.slte.app.utils.Constants
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

/** Owns transport construction and keeps OkHttp/Retrofit policy out of panel adapters. */
@Singleton
class ApiRetrofitFactory
@Inject
constructor(
    private val authInterceptor: AuthInterceptor,
    private val dns: FallbackDns,
    private val remoteConfig: RemoteConfig,
) {
    private val httpClient: OkHttpClient by lazy(::createHttpClient)

    fun create(backend: ApiBackend): Retrofit {
        val baseUrl = "${backend.baseUrl.trimEnd('/')}${backend.apiPrefix.trimEnd('/')}/"
            .toHttpUrlOrNull()
            ?: throw ApiException("API 地址无效", ApiErrors.UNSUPPORTED_BACKEND)

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(JSON.asConverterFactory(Constants.JSON_MEDIA_TYPE))
            .build()
    }

    private fun createHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(Constants.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(Constants.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(Constants.API_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(Constants.API_CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .dns(dns)
        .addInterceptor(ApiFailoverInterceptor(remoteConfig, remoteConfig.endpointSelector))
        .addInterceptor(authInterceptor)
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor { chain ->
                    val request = chain.request()
                    val safePath = request.url.encodedPath
                    AppLog.d("SLTE-Api", "${request.method} $safePath")
                    val response = chain.proceed(request)
                    AppLog.d("SLTE-Api", "${request.method} ${response.code} $safePath")
                    response
                }
            }
        }
        .build()

    private companion object {
        val JSON = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }
    }
}
