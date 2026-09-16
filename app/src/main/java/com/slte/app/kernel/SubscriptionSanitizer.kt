package com.slte.app.kernel

import com.slte.app.utils.AppLog
import com.slte.app.utils.sanitizeLog

object SubscriptionSanitizer {

    private const val HEALTH_CHECK_URL = "https://www.gstatic.com/generate_204"

    private const val HEALTH_CHECK_TIMEOUT_MS = 5_000

    private const val MAX_DOMAIN_LENGTH = 253

    private const val MAX_DOMAIN_LABEL_LENGTH = 63

    private const val LOG_TAG = "SLTE-Sanitizer"

    private const val BOM = '\uFEFF'

    private val HEALTH_CHECK_GROUP_TYPES = setOf("url-test", "fallback", "load-balance")

    private val ZEROED_PORT_KEYS = setOf("port", "socks-port", "mixed-port", "redir-port", "tproxy-port")

    private val NEUTRALIZED_SCALAR_KEYS =
        setOf(
            "external-controller",
            "external-controller-tls",
            "external-controller-unix",
            "external-controller-pipe",
            "external-ui",
            "external-ui-name",
            "external-ui-url",
            "secret",
        )

    private val DROPPED_TOP_LEVEL_KEYS = setOf("hosts", "script", "scripting", "web", "listeners", "<<")

    private val FORCED_OFF_TOP_LEVEL_KEYS = setOf("tun")

    private val ZEROED_SWITCH_KEYS = setOf("allow-lan", "bind-address")

    private val REWRITTEN_TOP_LEVEL_KEYS = ZEROED_PORT_KEYS + ZEROED_SWITCH_KEYS

    private val RULES_KEY = "rules"

    private val DNS_KEY = "dns"

    private val PLAIN_KEY = Regex("^[A-Za-z0-9_-]+$")

    private val SUBSCRIBE_ENTRY_KEY = Regex("^['\"]?(proxies|proxy-providers)['\"]?\\s*:\\s*(?:$|#|\\[|\\{|&|!)")

    private val TOP_LEVEL_BLOCK_HEAD = Regex("^['\"]?(?:<<|[A-Za-z0-9_-]+)['\"]?\\s*:\\s*(?:&\\S+)?\\s*$")

    private val TOP_LEVEL_FLOW_HEAD = Regex("^['\"]?(?:<<|[A-Za-z0-9_-]+)['\"]?\\s*:\\s*(?:&\\S+\\s*)?\\{")

    private val TOP_LEVEL_KEY_VALUE =
        Regex("^['\"]?(${REWRITTEN_TOP_LEVEL_KEYS.joinToString("|")})['\"]?\\s*:\\s*.*$")

    private val SUBTITLE_PATTERN_LINE = Regex("^(\\s*ui-subtitle-pattern\\s*:\\s*).*$")

    private val GROUP_ITEM_START = Regex("^\\s*-\\s*name\\s*:")

    private val PROVIDER_KEY = Regex("^[A-Za-z0-9_-]+\\s*:\\s*$")

    private val BLOCK_KEY = Regex("^(\\s*)([A-Za-z0-9_-]+)\\s*:")

    private val ENABLE_KEY = Regex("^['\"]?enable['\"]?\\s*:")

    private val FAKE_IP_FILTER_LINE = Regex("^fake-ip-filter\\s*:\\s*$")

    private val INLINE_FAKE_IP_FILTER = Regex("^fake-ip-filter\\s*:\\s*\\S")

    private val LIST_ITEM = Regex("^-\\s*")

    private val KEY_SMUGGLING = Regex("[\\\\&*]")

    fun isValidSubscribeYaml(text: String): Boolean {
        if (text.isBlank()) return false
        if (text.any { it < ' ' && it != '\n' && it != '\r' && it != '\t' }) return false
        val body = stripBom(text)
        val head = body.trimStart()
        if (head.startsWith("<") || head.startsWith("{")) return false
        return body.lineSequence().any { SUBSCRIBE_ENTRY_KEY.containsMatchIn(it.trimStart()) }
    }

    fun sanitize(
        text: String,
        domains: List<String>,
    ): String {
        if (text.isBlank()) return text
        val lines = stripBom(text).lines().toMutableList()
        dedentRootKeys(lines)
        normalizeQuotedKeys(lines)

        val portsRewritten = runStep("zeroTopLevelPorts") { zeroTopLevelPorts(lines) }
        val controlNeutralized = runStep("neutralizeControlSurface") { neutralizeControlSurface(lines) }
        runStep("clearSubtitlePattern") { clearSubtitlePattern(lines) }
        runStep("injectHealthCheckConfig") { injectHealthCheckConfig(lines) }

        var ruleInjected = true
        directDomains(domains).forEach { domain ->
            if (!runStep("injectDirectRule") { injectDirectRule(lines, domain) }) ruleInjected = false
            runStep("injectFakeIpFilter") { injectFakeIpFilter(lines, domain) }
        }

        if (!portsRewritten || !controlNeutralized || !ruleInjected) {
            AppLog.w(LOG_TAG, "清洗关键步骤失败，放弃输出")
            return ""
        }
        if (hasUnsafeResidue(lines)) {
            AppLog.w(LOG_TAG, "清洗后仍存在危险顶层键，放弃输出")
            return ""
        }
        return lines.joinToString("\n")
    }

    private fun directDomains(domains: List<String>): List<String> {
        val accepted = mutableListOf<String>()
        val seen = mutableSetOf<String>()
        domains.forEach { domain ->
            if (domain.isBlank()) return@forEach
            if (!isValidDirectDomain(domain)) {
                AppLog.w(LOG_TAG, "直连域名非法，已忽略: ${sanitizeLog(domain)}")
                return@forEach
            }
            if (!seen.add(domain.lowercase())) return@forEach
            accepted.add(domain)
        }
        return accepted
    }

    private fun isValidDirectDomain(domain: String): Boolean {
        if (domain.isEmpty() || domain != domain.trim() || domain.length > MAX_DOMAIN_LENGTH) return false
        val labels = domain.split('.')
        if (labels.any { it.isEmpty() || it.length > MAX_DOMAIN_LABEL_LENGTH }) return false
        return labels.all { label ->
            label.all(::isDomainChar) && isDomainEdgeChar(label.first()) && isDomainEdgeChar(label.last())
        }
    }

    private fun isDomainChar(char: Char): Boolean = isDomainEdgeChar(char) || char == '-' || char == '_'

    private fun isDomainEdgeChar(char: Char): Boolean = char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9'

    private fun runStep(
        step: String,
        block: () -> Unit,
    ): Boolean = try {
        block()
        true
    } catch (e: Throwable) {
        AppLog.w(LOG_TAG, "清洗步骤 $step 出错，跳过该步: ${e.javaClass.simpleName}: ${sanitizeLog(e.message ?: "Unknown")}")
        false
    }

    private fun stripBom(text: String): String {
        var start = 0
        while (start < text.length && text[start] == BOM) start++
        return if (start == 0) text else text.substring(start)
    }

    private fun dedentRootKeys(lines: MutableList<String>) {
        val rootIndent = lines.firstNotNullOfOrNull { rootIndentCandidate(it) } ?: return
        if (rootIndent <= 0) return
        for (i in lines.indices) {
            val line = lines[i]
            if (line.length < rootIndent) continue
            var shiftable = true
            for (j in 0 until rootIndent) {
                if (line[j] != ' ') {
                    shiftable = false
                    break
                }
            }
            if (shiftable) lines[i] = line.substring(rootIndent)
        }
    }

    private fun rootIndentCandidate(line: String): Int? {
        val trimmed = line.trimStart()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("#") || trimmed.startsWith("%")) return null
        if (trimmed == "---" || trimmed == "...") return null
        var count = 0
        while (count < line.length && line[count] == ' ') count++
        if (count < line.length && line[count] == '\t') return null
        return count
    }

    private fun leadingIndentLength(line: String): Int {
        var count = 0
        while (count < line.length && (line[count] == ' ' || line[count] == '\t')) count++
        return count
    }

    private fun leadingIndent(line: String): String = line.substring(0, leadingIndentLength(line))

    private fun tabIndentLength(line: String): Int {
        var index = 0
        while (index < line.length) {
            val char = line[index]
            if (char == ' ') return -1
            if (char != '\t') return index
            index++
        }
        return index
    }

    private fun isTopLevelLine(line: String): Boolean = tabIndentLength(line) >= 0

    private fun normalizeQuotedKeys(lines: MutableList<String>) {
        for (i in lines.indices) {
            val line = lines[i]
            val colon = unquotedColonIndex(line)
            if (colon <= 0) continue
            val rawKey = line.substring(0, colon).trim()
            val key = unwrapQuotes(rawKey)
            if (key == rawKey || !PLAIN_KEY.matches(key)) continue
            lines[i] = leadingIndent(line) + key + line.substring(colon)
        }
    }

    private fun unwrapQuotes(raw: String): String {
        if (raw.length < 2) return raw
        val quote = raw.first()
        if (quote != '"' && quote != '\'') return raw
        return if (raw.last() == quote) raw.substring(1, raw.length - 1) else raw
    }

    private fun unquotedColonIndex(line: String): Int {
        var index = 0
        while (index < line.length && (line[index] == ' ' || line[index] == '\t')) index++
        var quote: Char? = null
        while (index < line.length) {
            val char = line[index]
            if (quote != null) {
                if (char == quote) quote = null
            } else {
                when {
                    char == ':' -> return index
                    char == '"' || char == '\'' -> quote = char
                }
            }
            index++
        }
        return -1
    }

    private fun topLevelKey(line: String): String? {
        if (!isTopLevelLine(line)) return null
        val colon = unquotedColonIndex(line)
        if (colon <= 0) return null
        return normalizeKey(line.substring(0, colon)).ifEmpty { null }
    }

    private fun normalizeKey(raw: String): String = unwrapQuotes(raw.trim()).trim()

    private fun topLevelBlockIndices(
        lines: List<String>,
        key: String,
    ): MutableList<Int> {
        val indices = mutableListOf<Int>()
        for (i in lines.indices) {
            val line = lines[i]
            if (topLevelKey(line) == key && TOP_LEVEL_BLOCK_HEAD.matches(line)) indices.add(i)
        }
        return indices
    }

    private fun hasUnsafeResidue(lines: List<String>): Boolean {
        for (i in lines.indices) {
            val line = lines[i]
            if (!isTopLevelLine(line)) continue
            val trimmed = line.trim()
            if (trimmed.startsWith("#") || trimmed == "---" || trimmed == "...") continue
            if (trimmed == "?" || trimmed.startsWith("? ")) return true
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) return true
            if (KEY_SMUGGLING.containsMatchIn(line.substring(0, maxOf(unquotedColonIndex(line), 0)))) return true
            val key = topLevelKey(line) ?: continue
            val value = line.substring(unquotedColonIndex(line) + 1).trim()
            if (key in DROPPED_TOP_LEVEL_KEYS) return true
            if (key in NEUTRALIZED_SCALAR_KEYS && value.isNotEmpty() && value != "\"\"") return true
            if (key in ZEROED_PORT_KEYS && value != "0") return true
            if (key == "allow-lan" && value != "false") return true
            if (key == "bind-address" && value.isNotEmpty() && value != "\"\"") return true
            if (key == "authentication" && value.isNotEmpty() && value != "[]") return true
            if (key in FORCED_OFF_TOP_LEVEL_KEYS && !switchDisabled(lines, i)) return true
        }
        return false
    }

    private fun switchDisabled(
        lines: List<String>,
        index: Int,
    ): Boolean {
        val line = lines[index]
        if (TOP_LEVEL_FLOW_HEAD.containsMatchIn(line)) return line.substringAfter(':').contains("enable: false")
        if (!TOP_LEVEL_BLOCK_HEAD.matches(line)) return false
        val end = topLevelBlockEnd(lines, index)
        val childIndent = childKeyIndent(lines, index, end, "") ?: return false
        return lines.subList(index + 1, end).none { child ->
            leadingIndent(child) == childIndent &&
                ENABLE_KEY.containsMatchIn(child.trimStart()) &&
                child.substringAfter(':').trim() != "false"
        }
    }

    private fun injectHealthCheckConfig(lines: MutableList<String>) {
        injectGroupHealthCheck(lines)
        injectProviderHealthCheck(lines)
    }

    private fun injectGroupHealthCheck(lines: MutableList<String>) {
        val groupsIndex = topLevelBlockIndices(lines, "proxy-groups").firstOrNull() ?: return
        val itemIndent = blockItemIndent(lines, groupsIndex) ?: return
        val keyIndent = itemIndent + "  "

        val pending = mutableListOf<Pair<Int, List<String>>>()
        var i = groupsIndex + 1
        while (i < lines.size) {
            val line = lines[i]
            if (line.isBlank() || line.trimStart().startsWith("#")) {
                i++
                continue
            }
            val indent = leadingIndent(line)
            if (indent.length < itemIndent.length) break
            if (indent != itemIndent || !GROUP_ITEM_START.containsMatchIn(line)) {
                i++
                continue
            }

            if (line.contains('{') || line.contains('[')) {
                i++
                continue
            }
            val blockEnd = blockEndIndex(lines, i, itemIndent)
            if (groupType(lines, i, blockEnd, keyIndent) !in HEALTH_CHECK_GROUP_TYPES) {
                i = blockEnd
                continue
            }
            val additions = mutableListOf<String>()
            val replacements = mutableListOf<Pair<Int, String>>()
            for ((key, value) in listOf("url" to HEALTH_CHECK_URL, "timeout" to HEALTH_CHECK_TIMEOUT_MS.toString())) {
                when (blockKeyValue(lines, i, blockEnd, keyIndent, key)) {
                    KeyState.MISSING -> additions.add(keyIndent + "$key: $value")
                    KeyState.EMPTY ->
                        blockKeyLineIndex(lines, i, blockEnd, keyIndent, key)
                            ?.let { replacements.add(it to keyIndent + "$key: $value") }
                    KeyState.PRESENT -> Unit
                }
            }
            if (additions.isNotEmpty()) pending.add(i + 1 to additions)

            for ((at, text) in replacements.asReversed()) {
                lines[at] = text
            }
            i = blockEnd
        }
        for ((at, additions) in pending.asReversed()) {
            lines.addAll(at, additions)
        }
    }

    private fun injectProviderHealthCheck(lines: MutableList<String>) {
        val providersIndex = topLevelBlockIndices(lines, "proxy-providers").firstOrNull() ?: return
        val providerIndent = blockKeyIndent(lines, providersIndex) ?: return
        val pending = mutableListOf<Pair<Int, List<String>>>()
        var i = providersIndex + 1
        while (i < lines.size) {
            val line = lines[i]
            if (line.isBlank() || line.trimStart().startsWith("#")) {
                i++
                continue
            }
            val indent = leadingIndent(line)
            if (indent.length < providerIndent.length) break
            if (indent != providerIndent || !PROVIDER_KEY.matches(line.trim())) {
                i++
                continue
            }

            if (line.contains('{') || line.contains('[')) {
                i++
                continue
            }
            val blockEnd = blockEndIndex(lines, i, providerIndent)
            val childKeyIndent = childKeyIndent(lines, i, blockEnd, providerIndent)
            if (childKeyIndent == null) {
                i = blockEnd
                continue
            }
            if (hasBlockKey(lines, i, blockEnd, childKeyIndent, "health-check")) {
                i = blockEnd
                continue
            }

            if (lines.subList(i, blockEnd).any {
                    it.contains("health-check:") && it.trim() != "health-check:"
                }
            ) {
                i = blockEnd
                continue
            }
            pending.add(
                i + 1 to
                    listOf(
                        childKeyIndent + "health-check:",
                        childKeyIndent + "  enable: true",
                        childKeyIndent + "  url: $HEALTH_CHECK_URL",
                        childKeyIndent + "  interval: 300",
                        childKeyIndent + "  timeout: $HEALTH_CHECK_TIMEOUT_MS",
                        childKeyIndent + "  lazy: true",
                    ),
            )
            i = blockEnd
        }
        for ((at, additions) in pending.asReversed()) {
            lines.addAll(at, additions)
        }
    }

    private fun blockEndIndex(
        lines: List<String>,
        start: Int,
        itemIndent: String,
    ): Int {
        for (i in start + 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank() || line.trimStart().startsWith("#")) continue
            if (leadingIndent(line).length <= itemIndent.length) return i
        }
        return lines.size
    }

    private fun topLevelBlockEnd(
        lines: List<String>,
        index: Int,
    ): Int {
        for (i in index + 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank() || line.trimStart().startsWith("#")) continue
            if (leadingIndentLength(line) != 0) continue
            if (LIST_ITEM.containsMatchIn(line)) continue
            return i
        }
        return lines.size
    }

    private fun listBlockEnd(
        lines: List<String>,
        keyIndex: Int,
        itemIndent: String,
    ): Int {
        var end = keyIndex + 1
        while (end < lines.size) {
            val line = lines[end]
            if (line.isBlank() || line.trimStart().startsWith("#")) {
                end++
                continue
            }
            if (!LIST_ITEM.containsMatchIn(line.trimStart()) || leadingIndent(line) != itemIndent) break
            end++
        }
        return end
    }

    private fun hasBlockKey(
        lines: List<String>,
        start: Int,
        end: Int,
        keyIndent: String,
        key: String,
    ): Boolean = blockKeyValue(lines, start, end, keyIndent, key) != KeyState.MISSING

    private fun blockKeyValue(
        lines: List<String>,
        start: Int,
        end: Int,
        keyIndent: String,
        key: String,
    ): KeyState {
        for (i in start + 1 until end) {
            val line = lines[i]
            if (line.isBlank() || line.trimStart().startsWith("#")) continue
            val indent = leadingIndent(line)
            if (indent.length < keyIndent.length) return KeyState.MISSING
            if (indent != keyIndent) continue
            val m = BLOCK_KEY.find(line) ?: continue
            if (m.groupValues[2] != key) continue
            val value = line.substringAfter(':').trim()
            return if (value.isEmpty() || value == "\"\"" || value == "''") KeyState.EMPTY else KeyState.PRESENT
        }
        return KeyState.MISSING
    }

    private fun blockKeyLineIndex(
        lines: List<String>,
        start: Int,
        end: Int,
        keyIndent: String,
        key: String,
    ): Int? {
        for (i in start + 1 until end) {
            val line = lines[i]
            if (line.isBlank() || line.trimStart().startsWith("#")) continue
            val indent = leadingIndent(line)
            if (indent.length < keyIndent.length) return null
            if (indent != keyIndent) continue
            val m = BLOCK_KEY.find(line) ?: continue
            if (m.groupValues[2] == key) return i
        }
        return null
    }

    private fun groupType(
        lines: List<String>,
        start: Int,
        end: Int,
        keyIndent: String,
    ): String {
        for (i in start + 1 until end) {
            val line = lines[i]
            if (line.isBlank() || line.trimStart().startsWith("#")) continue
            val indent = leadingIndent(line)
            if (indent.length < keyIndent.length) return ""
            if (indent != keyIndent) continue
            val m = BLOCK_KEY.find(line) ?: continue
            if (m.groupValues[2] != "type") continue
            return line
                .substringAfter(':')
                .trim()
                .trim('\'')
                .trim('"')
                .lowercase()
        }
        return ""
    }

    private fun childKeyIndent(
        lines: List<String>,
        start: Int,
        end: Int,
        parentIndent: String,
    ): String? {
        for (i in start + 1 until end) {
            val line = lines[i]
            if (line.isBlank() || line.trimStart().startsWith("#")) continue
            val indent = leadingIndent(line)
            if (indent.length <= parentIndent.length) return null
            return indent
        }
        return null
    }

    private fun zeroTopLevelPorts(lines: MutableList<String>) {
        for (i in lines.indices) {
            val line = lines[i]
            val indent = tabIndentLength(line)
            if (indent < 0) continue
            val body = if (indent == 0) line else line.substring(indent)
            val m = TOP_LEVEL_KEY_VALUE.matchEntire(body) ?: continue
            val key = m.groupValues[1]
            lines[i] =
                when (key) {
                    "allow-lan" -> "allow-lan: false"
                    "bind-address" -> "bind-address: \"\""
                    in ZEROED_PORT_KEYS -> "$key: 0"
                    else -> line
                }
        }
    }

    private fun clearSubtitlePattern(lines: MutableList<String>) {
        for (i in lines.indices) {
            val m = SUBTITLE_PATTERN_LINE.matchEntire(lines[i]) ?: continue
            lines[i] = m.groupValues[1] + "\"\""
        }
    }

    private fun neutralizeControlSurface(lines: MutableList<String>) {
        dropTopLevelKeys(lines, DROPPED_TOP_LEVEL_KEYS)
        forceOffTopLevelSwitches(lines, FORCED_OFF_TOP_LEVEL_KEYS)
        for (i in lines.indices) {
            val key = topLevelKey(lines[i]) ?: continue
            if (key in NEUTRALIZED_SCALAR_KEYS) lines[i] = "$key: \"\""
        }
        clearAuthentication(lines)
    }

    private fun dropTopLevelKeys(
        lines: MutableList<String>,
        keys: Set<String>,
    ) {
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val key = topLevelKey(line)
            if (key == null || key !in keys) {
                i++
                continue
            }
            val end = if (TOP_LEVEL_BLOCK_HEAD.matches(line)) topLevelBlockEnd(lines, i) else i + 1
            lines.subList(i, end).clear()
        }
    }

    private fun forceOffTopLevelSwitches(
        lines: MutableList<String>,
        keys: Set<String>,
    ) {
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            val key = topLevelKey(line)
            if (key == null || key !in keys) {
                i++
                continue
            }

            if (TOP_LEVEL_FLOW_HEAD.containsMatchIn(line) || !TOP_LEVEL_BLOCK_HEAD.matches(line)) {
                lines[i] = "$key: {enable: false}"
                i++
                continue
            }
            val end = topLevelBlockEnd(lines, i)
            val childIndent = childKeyIndent(lines, i, end, "")
            if (childIndent == null) {
                lines.add(i + 1, "  enable: false")
                i++
                continue
            }
            var rewritten = false
            for (j in i + 1 until end) {
                val child = lines[j]
                if (leadingIndent(child) != childIndent) continue
                if (!ENABLE_KEY.containsMatchIn(child.trimStart())) continue
                lines[j] = "${childIndent}enable: false"
                rewritten = true
            }
            if (!rewritten) lines.add(i + 1, "${childIndent}enable: false")
            i++
        }
    }

    private fun clearAuthentication(lines: MutableList<String>) {
        var i = 0
        while (i < lines.size) {
            val line = lines[i]
            if (topLevelKey(line) != "authentication") {
                i++
                continue
            }
            val value = line.substring(unquotedColonIndex(line) + 1).trim()
            if (line.contains('[') || value.isNotEmpty()) {
                lines[i] = "authentication: []"
                i++
                continue
            }
            lines.subList(i, topLevelBlockEnd(lines, i)).clear()
        }
    }

    private fun injectDirectRule(
        lines: MutableList<String>,
        domain: String,
    ) {
        val rule = "DOMAIN-SUFFIX,$domain,DIRECT"
        topLevelBlockIndices(lines, RULES_KEY).asReversed().forEach { index ->
            val indent = blockItemIndent(lines, index) ?: return@forEach
            val end = listBlockEnd(lines, index, indent)
            if (hasListItem(lines, index + 1, end, rule)) return@forEach
            lines.add(index + 1, indent + "- '$rule'")
        }
    }

    private fun injectFakeIpFilter(
        lines: MutableList<String>,
        domain: String,
    ) {
        val entry = "+.$domain"
        val filterIndex = lines.indexOfFirst { FAKE_IP_FILTER_LINE.matches(it.trim()) }
        if (filterIndex >= 0) {
            val keyIndent = leadingIndent(lines[filterIndex])
            val indent = blockItemIndent(lines, filterIndex)
            if (indent == null) {
                lines.add(filterIndex + 1, keyIndent + "  - '$entry'")
                return
            }
            val end = listBlockEnd(lines, filterIndex, indent)
            if (hasListItem(lines, filterIndex + 1, end, entry)) return
            lines.add(filterIndex + 1, indent + "- '$entry'")
            return
        }

        if (lines.any { INLINE_FAKE_IP_FILTER.containsMatchIn(it.trim()) }) return

        val dnsIndex = topLevelBlockIndices(lines, DNS_KEY).lastOrNull() ?: return

        val keyIndent = blockKeyIndent(lines, dnsIndex) ?: return
        lines.add(dnsIndex + 1, keyIndent + "fake-ip-filter:")
        lines.add(dnsIndex + 2, keyIndent + "    - '$entry'")
    }

    private fun hasListItem(
        lines: List<String>,
        start: Int,
        end: Int,
        value: String,
    ): Boolean {
        for (i in start until minOf(end, lines.size)) {
            val line = lines[i]
            if (line.isBlank() || line.trimStart().startsWith("#")) continue
            val trimmed = line.trimStart()
            if (!LIST_ITEM.containsMatchIn(trimmed)) continue
            if (unwrapQuotes(trimmed.removePrefix("-").trim()).equals(value, ignoreCase = true)) return true
        }
        return false
    }

    private fun blockItemIndent(
        lines: List<String>,
        keyIndex: Int,
    ): String? {
        for (i in keyIndex + 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank() || line.trimStart().startsWith("#")) continue
            val trimmed = line.trimStart()
            if (trimmed.startsWith("-")) return line.substring(0, line.length - trimmed.length)
            return null
        }
        return null
    }

    private fun blockKeyIndent(
        lines: List<String>,
        keyIndex: Int,
    ): String? {
        for (i in keyIndex + 1 until lines.size) {
            val line = lines[i]
            if (line.isBlank() || line.trimStart().startsWith("#")) continue
            return leadingIndent(line)
        }
        return null
    }

    private enum class KeyState {
        MISSING,
        EMPTY,
        PRESENT,
    }
}
