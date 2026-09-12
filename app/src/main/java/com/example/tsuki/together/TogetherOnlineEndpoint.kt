package com.example.tsuki.together

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

private val Context.togetherEndpointStore by preferencesDataStore(name = "together_endpoint")
private val EndpointCacheKey = stringPreferencesKey("endpoint_cache")
private val EndpointCheckedKey = longPreferencesKey("endpoint_last_checked_at")

object TogetherOnlineEndpoint {
    private val http = HttpClient(OkHttp) {
        engine { config { connectTimeout(10, TimeUnit.SECONDS); readTimeout(10, TimeUnit.SECONDS); writeTimeout(10, TimeUnit.SECONDS) } }
    }

    suspend fun baseUrlOrNull(context: Context): String? {
        val prefs = context.togetherEndpointStore.data.first()
        val cached = prefs[EndpointCacheKey]?.trim()?.takeIf { it.isNotEmpty() }
        if (!cached.isNullOrBlank()) return cached.trimEnd('/')
        return null
    }

    suspend fun saveBaseUrl(context: Context, url: String) {
        val clean = url.trim().trimEnd('/')
        if (clean.isEmpty()) return
        context.togetherEndpointStore.edit { it[EndpointCacheKey] = clean; it[EndpointCheckedKey] = System.currentTimeMillis() }
    }

    fun wsUrlFromBase(baseUrl: String): String? {
        val trimmed = baseUrl.trim().trimEnd('/')
        if (trimmed.isEmpty()) return null
        return try {
            val uri = java.net.URI(trimmed)
            val host = uri.host ?: return null
            val scheme = if (uri.scheme?.lowercase() == "https") "wss" else "ws"
            val port = if (uri.port != -1 && uri.port != 80 && uri.port != 443) ":${uri.port}" else ""
            val path = (uri.path?.trimEnd('/').orEmpty()).let { if (it.endsWith("/v1")) it else "$it/v1" }
            "$scheme://$host$port$path/together/ws"
        } catch (_: Exception) { null }
    }

    fun onlineWebSocketUrlOrNull(rawWsUrl: String, baseUrl: String): String? {
        val derived = wsUrlFromBase(baseUrl) ?: return null
        val norm = normalize(rawWsUrl, baseUrl) ?: return derived
        return try {
            val h = java.net.URI(norm).host?.lowercase() ?: return derived
            if (h == "localhost" || h == "127.0.0.1" || h == "0.0.0.0") derived else norm
        } catch (_: Exception) { derived }
    }

    private fun normalize(raw: String, baseUrl: String): String? {
        val t = raw.trim()
        if (t.isEmpty()) return null
        if (t.startsWith("ws://") || t.startsWith("wss://")) return t
        if (t.startsWith("http://")) return "ws://${t.removePrefix("http://")}"
        if (t.startsWith("https://")) return "wss://${t.removePrefix("https://")}"
        if (t.startsWith("/")) {
            val base = runCatching { java.net.URI(baseUrl.trim()) }.getOrNull() ?: return null
            val host = base.host ?: return null
            val scheme = if (base.scheme?.lowercase() == "https") "wss" else "ws"
            val port = if (base.port != -1 && base.port != 80 && base.port != 443) ":${base.port}" else ""
            val bp = base.path?.trimEnd('/').orEmpty()
            return "$scheme://$host$port$bp$t"
        }
        val bs = runCatching { java.net.URI(baseUrl.trim()).scheme?.lowercase() }.getOrNull()
        val wsScheme = if (bs == "https") "wss" else "ws"
        return "$wsScheme://$t"
    }
}
