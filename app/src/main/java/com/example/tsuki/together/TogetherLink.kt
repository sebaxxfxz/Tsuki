package com.example.tsuki.together

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class TogetherJoinInfo(
    val host: String,
    val port: Int,
    val sessionId: String,
    val sessionKey: String
) {
    fun toWebSocketUrl(): String = "ws://$host:$port/together"
    fun toDeepLink(): String {
        val enc = StandardCharsets.UTF_8.name()
        val query = listOf(
            "host" to host,
            "port" to port.toString(),
            "sid" to sessionId,
            "key" to sessionKey
        ).joinToString("&") { (k, v) -> "${URLEncoder.encode(k, enc)}=${URLEncoder.encode(v, enc)}" }
        return "tsuki://together?$query"
    }
}

object TogetherLink {
    fun encode(joinInfo: TogetherJoinInfo): String = joinInfo.toDeepLink()
    fun decode(raw: String): TogetherJoinInfo? {
        val input = raw.trim()
        if (input.isEmpty()) return null
        val link = Regex("tsuki://\\S+").find(input)?.value
            ?.trimEnd('.', ',', ';', ':', '!', '?', ')') ?: input
        runCatching { URI(link) }.getOrNull()?.let { uri ->
            parseDeepLink(uri)?.let { return it }
            parseWs(uri)?.let { return it }
        }
        return parseCompact(link)
    }

    private fun parseDeepLink(uri: URI): TogetherJoinInfo? {
        if (!uri.scheme.equals("tsuki", true)) return null
        val hostPart = (uri.host ?: uri.authority ?: return null).lowercase()
        if (hostPart != "together") return null
        val q = queryMap(uri.rawQuery)
        val h = q["host"]?.trim().orEmpty()
        val p = q["port"]?.toIntOrNull() ?: return null
        val sid = q["sid"]?.trim().orEmpty()
        val key = q["key"]?.trim().orEmpty()
        if (h.isEmpty() || sid.isEmpty() || key.isEmpty()) return null
        if (p !in 1..65535) return null
        return TogetherJoinInfo(h, p, sid, key)
    }

    private fun parseWs(uri: URI): TogetherJoinInfo? {
        val s = uri.scheme?.lowercase() ?: return null
        if (s !in setOf("ws", "wss", "http", "https")) return null
        val h = uri.host?.trim().orEmpty()
        if (h.isEmpty()) return null
        val p = if (uri.port != -1) uri.port else if (s == "wss" || s == "https") 443 else 80
        val q = queryMap(uri.rawQuery)
        val sid = q["sid"]?.trim().orEmpty()
        val key = q["key"]?.trim().orEmpty()
        if (sid.isEmpty() || key.isEmpty()) return null
        if (p !in 1..65535) return null
        return TogetherJoinInfo(h, p, sid, key)
    }

    private fun parseCompact(raw: String): TogetherJoinInfo? {
        val parts = raw.replace("\\s+".toRegex(), "").split("|")
        if (parts.size != 4) return null
        val h = parts[0].trim()
        val p = parts[1].toIntOrNull() ?: return null
        val sid = parts[2].trim()
        val key = parts[3].trim()
        if (h.isEmpty() || sid.isEmpty() || key.isEmpty() || p !in 1..65535) return null
        return TogetherJoinInfo(h, p, sid, key)
    }

    private fun queryMap(rawQuery: String?): Map<String, String> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        val enc = StandardCharsets.UTF_8.name()
        return rawQuery.split("&").mapNotNull { entry ->
            val eq = entry.indexOf('=')
            if (eq <= 0) return@mapNotNull null
            val k = URLDecoder.decode(entry.substring(0, eq), enc)
            val v = URLDecoder.decode(entry.substring(eq + 1), enc)
            k to v
        }.toMap()
    }
}
