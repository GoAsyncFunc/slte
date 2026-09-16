package com.slte.app.data.remote.config

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.slte.app.BuildConfig
import com.slte.app.data.local.SecurePreferences
import com.slte.app.data.remote.ApiPaths
import com.slte.app.kernel.AppRemoteConfig
import com.slte.app.utils.AppLog
import com.slte.app.utils.sanitizeLog
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
data class RemoteConfigData(

    val apiBaseUrl: String = BuildConfig.API_BASE_URL,

    val apiBaseUrls: List<String> = emptyList(),

    val directDomains: List<String> = emptyList(),
    val apiType: String = BuildConfig.API_TYPE,
    val crispWebsiteId: String = BuildConfig.CRISP_WEBSITE_ID,
    val crispEnabled: Boolean = BuildConfig.CRISP_ENABLED,

    val updateVersion: String = "",

    val updateChangelogTitle: String = "",

    val updateChangelog: String = "",

    val updateForce: Boolean = false,

    val updateApkUrl: String = "",
)

@Serializable
private data class RemoteConfigDto(
    @SerialName("api_base_url") val apiBaseUrl: String? = null,

    @SerialName("api_base_urls") val apiBaseUrls: JsonElement? = null,

    @SerialName("api") val api: JsonElement? = null,
    @SerialName("direct_domains") val directDomains: JsonElement? = null,
    @SerialName("api_type") val apiType: String? = null,
    @SerialName("crisp_website_id") val crispWebsiteId: String? = null,
    @SerialName("crisp_enabled") val crispEnabled: Boolean? = null,

    @SerialName("config_version") val configVersion: String? = null,
    @SerialName("update_version") val updateVersion: String? = null,
    @SerialName("update_changelog_title") val updateChangelogTitle: String? = null,
    @SerialName("update_changelog") val updateChangelog: String? = null,
    @SerialName("update_force") val updateForce: Boolean? = null,
    @SerialName("update_apk_url") val updateApkUrl: String? = null,
)

@Serializable
internal data class CachedConfig(
    val config: RemoteConfigData,

    val version: String = "",

    val fetchedAt: Long = 0L,

    val sourceUrl: String = "",

    val etag: String = "",
)

@Singleton
class RemoteConfig
@Inject
constructor(
    @ApplicationContext private val context: Context,
) : AppRemoteConfig,
    FailoverConfig {
    override val apiBaseUrl: String get() = data.apiBaseUrl

    override val directDomains: List<String> get() = data.directDomains

    private val selector = EndpointSelector()

    val endpointSelector: EndpointSelector get() = selector

    private val prefs: SharedPreferences =
        SecurePreferences.create(context, PREFS_NAME, KEY_ALIAS)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json =
        Json {
            ignoreUnknownKeys = true
            isLenient = true
        }

    private val etagByUrl = ConcurrentHashMap<String, String>()

    private val configClient: OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(CONFIG_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(CONFIG_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(CONFIG_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()

    private val speedClient: OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()

    private val _dataFlow = MutableStateFlow(loadCached()?.config ?: RemoteConfigData())
    val dataFlow: StateFlow<RemoteConfigData> = _dataFlow.asStateFlow()
    val data: RemoteConfigData get() = _dataFlow.value

    override fun apiCandidates(primary: String): List<String> = selector.candidateOrder(primary, dataFlow.value.apiBaseUrls)

    fun startFetch() {
        scope.launch { refresh(force = true) }
    }

    fun startProbeLoop() {
        scope.launch {
            while (true) {
                probeHalfOpen()
                delay(PROBE_LOOP_INTERVAL_MS)
            }
        }
    }

    private suspend fun probeHalfOpen() {
        val candidates = selector.halfOpenCandidates()
        if (candidates.isEmpty()) return
        candidates.forEach { url ->
            val latency = probeOne(url)
            if (latency != null) {
                selector.recordProbe(url, latency)
                AppLog.i("SLTE-Config", "RemoteConfig: 半开探测恢复 latency=$latency")
            } else {
                selector.recordFailure(url)
                AppLog.w("SLTE-Config", "RemoteConfig: 半开探测仍失败")
            }
        }
    }

    suspend fun refresh(force: Boolean = false): Boolean {
        val now = System.currentTimeMillis()
        val cached = loadCached()
        if (!force && cached != null && ConfigValidation.isCacheFresh(cached.fetchedAt, now, CONFIG_CACHE_TTL_MS)) {
            _dataFlow.value = cached.config
            return true
        }

        val urls = orderedConfigUrls()
        if (urls.isEmpty()) return false
        val result =
            try {
                withTimeout(CONFIG_FETCH_TIMEOUT_MS) {
                    ConfigRace.race(urls) { url -> fetchOne(url, cached) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                AppLog.w("SLTE-Config", "RemoteConfig: 配置竞速失败: ${sanitize(e.message)}")
                return false
            }
        val chosen = result.chosen ?: return false

        if (chosen.notModified && cached != null) {
            writeCache(cached.copy(fetchedAt = now, sourceUrl = chosen.url))
            _dataFlow.value = cached.config
            AppLog.i("SLTE-Config", "RemoteConfig: 304 命中，复用缓存")
            return true
        }

        val dto =
            try {
                json.decodeFromString<RemoteConfigDto>(chosen.raw)
            } catch (e: Exception) {
                AppLog.w("SLTE-Config", "RemoteConfig: 配置解析失败: ${sanitize(e.message)}")
                return false
            }

        val candidates = resolveApiCandidates(dto)
        val probes = probeAll(candidates)
        probes.forEach { (url, latency) -> selector.recordProbe(url, latency) }
        val primary =
            selector.pickPrimary(
                candidates = candidates,
                probes = probes,
                currentPrimary = data.apiBaseUrl.takeIf { it in candidates },
            ) ?: BuildConfig.API_BASE_URL
        selector.updatePrimary(primary)

        val compatible = candidates.filter { ConfigValidation.hasSamePath(it, primary) }
        if (compatible.size != candidates.size) {
            AppLog.w(
                "SLTE-Config",
                "RemoteConfig: ${candidates.size - compatible.size} 个候选与主地址 path 不一致，已剔除（failover 只重写主机）",
            )
        }

        val merged = buildMergedConfig(dto, primary, compatible)
        writeCache(
            CachedConfig(
                config = merged,
                version = chosen.version,
                fetchedAt = now,
                sourceUrl = chosen.url,
                etag = etagByUrl[chosen.url] ?: "",
            ),
        )
        _dataFlow.value = merged
        AppLog.i(
            "SLTE-Config",
            "RemoteConfig: 已更新 version=${chosen.version} candidates=${candidates.size} crisp=${merged.crispEnabled}",
        )
        return true
    }

    private suspend fun fetchOne(
        url: String,
        cached: CachedConfig?,
    ): FetchedConfig? {
        val start = System.currentTimeMillis()
        return try {
            val builder = Request.Builder().url(url)
            etagByUrl[url]?.takeIf { it.isNotBlank() }?.let { builder.header("If-None-Match", it) }
            configClient.newCall(builder.build()).execute().use { resp ->
                val elapsed = System.currentTimeMillis() - start
                when {
                    resp.code == 304 -> {
                        if (cached != null && cached.sourceUrl == url && cached.etag == etagByUrl[url]) {
                            FetchedConfig(url, "", cached.version, elapsed, notModified = true)
                        } else {
                            null
                        }
                    }
                    resp.isSuccessful -> {
                        val body =
                            resp.body?.byteStream()?.use { readLimited(it, MAX_CONFIG_BYTES) }
                                ?: return@use null
                        val raw = body.toString(Charsets.UTF_8)
                        val dto =
                            try {
                                json.decodeFromString<RemoteConfigDto>(raw)
                            } catch (_: Exception) {
                                null
                            } ?: return@use null
                        if (!validateDto(dto)) return@use null
                        etagByUrl[url] = resp.header("ETag") ?: ""
                        FetchedConfig(url, raw, dto.configVersion ?: "", elapsed)
                    }
                    else -> null
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLog.w("SLTE-Config", "RemoteConfig: 配置源不可用: ${sanitize(e.message)}")
            null
        }
    }

    private fun validateDto(dto: RemoteConfigDto): Boolean {
        val version = dto.configVersion ?: return true

        return version.matches(Regex("[0-9a-zA-Z.\\-]+"))
    }

    private fun orderedConfigUrls(): List<String> {
        val urls =
            BuildConfig.REMOTE_CONFIG_URLS
                .split(',')
                .map { it.trim() }
                .filter { it.startsWith("https://") }
        val last = prefs.getString(KEY_LAST_URL, null)
        if (last != null && last in urls) {
            return listOf(last) + urls.filter { it != last }
        }
        return urls
    }

    private fun resolveApiCandidates(dto: RemoteConfigDto): List<String> {
        val fromArray = dto.apiBaseUrls?.let { el -> jsonElementToList(el) } ?: emptyList()
        val fromApi = dto.api?.let { el -> jsonElementToList(el) } ?: emptyList()
        val list =
            buildList {
                dto.apiBaseUrl?.let { takeIfAllowed(it) }?.let { add(it) }
                (fromArray + fromApi).mapNotNull { takeIfAllowed(it) }.forEach { if (it !in this) add(it) }
                if (isEmpty()) add(BuildConfig.API_BASE_URL)
            }
        return list
    }

    private fun jsonElementToList(element: JsonElement): List<String> = when (element) {
        is JsonPrimitive -> listOf(element.content)
        is JsonArray -> element.mapNotNull { (it as? JsonPrimitive)?.content }
        else -> emptyList()
    }

    private fun takeIfAllowed(value: String): String? {
        val candidate = ConfigValidation.decodeApiCandidate(value)
        val accepted = ConfigValidation.isValidApiUrl(candidate, ALLOWED_HOST_SUFFIXES)
        if (!accepted) {
            AppLog.w(
                "SLTE-Config",
                "RemoteConfig: API 候选被拒（https 且主机需在编译期白名单内）: ${sanitizeLog(candidate)}",
            )
            return null
        }
        return candidate.trim()
    }

    private fun resolveDirectDomains(element: JsonElement?): List<String> = buildList {
        val values =
            when (element) {
                null -> emptyList()
                is JsonPrimitive -> listOf(element.content)
                is JsonArray -> element.mapNotNull { (it as? JsonPrimitive)?.content }
                else -> emptyList()
            }
        values.mapNotNull { takeDomain(it) }.forEach { if (it !in this) add(it) }
    }

    private fun takeDomain(value: String): String? = if (ConfigValidation.isValidDomain(value, ALLOWED_HOST_SUFFIXES)) {
        value.trim().lowercase().trimEnd('.')
    } else {
        AppLog.w(
            "SLTE-Config",
            "RemoteConfig: 直连域名被拒（需两段以上且在编译期白名单内）: ${sanitizeLog(value)}",
        )
        null
    }

    private fun buildMergedConfig(
        dto: RemoteConfigDto,
        primary: String,
        candidates: List<String>,
    ): RemoteConfigData {
        val updateApkUrl = dto.updateApkUrl?.trim()?.let { takeIfAllowed(it) } ?: ""
        return RemoteConfigData(
            apiBaseUrl = primary,
            apiBaseUrls = candidates.ifEmpty { listOf(BuildConfig.API_BASE_URL) },
            directDomains = resolveDirectDomains(dto.directDomains),
            apiType = dto.apiType?.trim()?.takeIf { it == BuildConfig.API_TYPE } ?: BuildConfig.API_TYPE,
            crispWebsiteId =
            dto.crispWebsiteId?.trim()?.takeIf { it.isNotBlank() }
                ?: BuildConfig.CRISP_WEBSITE_ID,
            crispEnabled = dto.crispEnabled ?: BuildConfig.CRISP_ENABLED,
            updateVersion = dto.updateVersion?.trim() ?: "",
            updateChangelogTitle = dto.updateChangelogTitle?.trim() ?: "",
            updateChangelog = dto.updateChangelog ?: "",
            updateForce = dto.updateForce == true && updateApkUrl.isNotBlank(),
            updateApkUrl = updateApkUrl,
        )
    }

    private fun readLimited(
        input: java.io.InputStream,
        max: Int,
    ): ByteArray? {
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(4096)
        var total = 0
        while (true) {
            val n = input.read(chunk)
            if (n < 0) break
            total += n
            if (total > max) return null
            buffer.write(chunk, 0, n)
        }
        return buffer.toByteArray()
    }

    private suspend fun probeAll(candidates: List<String>): Map<String, Long> {
        if (candidates.isEmpty()) return emptyMap()
        return coroutineScope {
            candidates
                .map { url ->
                    async(Dispatchers.IO) {
                        if (selector.isOpen(url)) return@async null
                        val latency = probeOne(url)
                        if (latency != null) url to latency else null
                    }
                }.awaitAll()
                .filterNotNull()
                .toMap()
        }
    }

    private suspend fun probeOne(url: String): Long? {
        val start = System.currentTimeMillis()
        return try {
            speedClient
                .newCall(
                    Request.Builder().url(url.trimEnd('/') + PROBE_PATH).build(),
                ).execute()
                .use { resp ->
                    if (resp.code in 200..499) {
                        System.currentTimeMillis() - start
                    } else {
                        null
                    }
                }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    private fun loadCached(): CachedConfig? {
        val raw = prefs.getString(KEY_CACHE, null) ?: return null
        return try {
            json.decodeFromString<CachedConfig>(raw)
        } catch (e: Exception) {
            AppLog.w("SLTE-Config", "RemoteConfig: 缓存解析失败，回退默认配置")
            null
        }
    }

    private fun writeCache(cached: CachedConfig) {
        prefs.edit {
            putString(KEY_CACHE, json.encodeToString(cached))
            putString(KEY_LAST_URL, cached.sourceUrl)
        }
    }

    private fun sanitize(message: String?): String = message?.let { sanitizeLog(it) } ?: "Unknown"

    private companion object {
        const val PREFS_NAME = "slte_remote_config"
        const val KEY_ALIAS = "slte_remote_config_master_key"
        const val KEY_CACHE = "cached_config"
        const val KEY_LAST_URL = "last_url"
        const val MAX_CONFIG_BYTES = 256 * 1024
        const val CONFIG_TIMEOUT_SECONDS = 3L
        const val CONFIG_FETCH_TIMEOUT_MS = 5_000L

        const val CONFIG_CACHE_TTL_MS = 5 * 60_000L

        const val PROBE_LOOP_INTERVAL_MS = 15_000L

        const val PROBE_PATH = ApiPaths.GUEST_CONFIG

        val ALLOWED_HOST_SUFFIXES: List<String> get() = AllowedHosts.SUFFIXES
    }
}
