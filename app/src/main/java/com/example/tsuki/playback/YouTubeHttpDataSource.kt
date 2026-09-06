package com.example.tsuki.playback

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit








@UnstableApi
class YouTubeHttpDataSource private constructor(
    private val userAgent: String,
    private val defaultRequestProperties: Map<String, String>,
    private val client: OkHttpClient,
) : BaseDataSource(true), HttpDataSource {

    constructor(
        client: OkHttpClient,
        defaultHeaders: Map<String, String> = emptyMap()
    ) : this(
        userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0",
        defaultRequestProperties = defaultHeaders,
        client = client
    ) {
    }

    private var dataSource: DataSource? = null
    private var currentUri: Uri? = null
    private var isTransferStarted = false

    class Factory(private val client: OkHttpClient = sharedClient()) : HttpDataSource.Factory {
        private val requestProperties = HashMap<String, String>()
        private var userAgent =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        constructor() : this(sharedClient())
        constructor(client: OkHttpClient, defaultHeaders: Map<String, String> = emptyMap()) : this(client) {
            requestProperties.putAll(defaultHeaders)
        }

        override fun createDataSource(): HttpDataSource = YouTubeHttpDataSource(userAgent, requestProperties.toMap(), client)

        override fun setDefaultRequestProperties(defaultRequestProperties: MutableMap<String, String>): HttpDataSource.Factory {
            requestProperties.clear()
            requestProperties.putAll(defaultRequestProperties)
            return this
        }
    }

    companion object {
        private const val TAG = "YouTubeHttpDataSource"
        private val clientLock = Any()
        @Volatile private var cachedClient: OkHttpClient? = null

        private fun sharedClient(): OkHttpClient {
            cachedClient?.let { return it }
            return synchronized(clientLock) {
                cachedClient ?: OkHttpClient.Builder()
                    .connectionPool(okhttp3.ConnectionPool(32, 5, TimeUnit.MINUTES))
                    .fastFallback(true)
                    .connectTimeout(8, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .retryOnConnectionFailure(true)
                    .build().also { cachedClient = it }
            }
        }
    }

    @UnstableApi
    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        currentUri = dataSpec.uri
        val requestUserAgent = if (isYouTubeUri(dataSpec.uri)) resolveYouTubeUserAgent(dataSpec.uri) else userAgent
        val factory = OkHttpDataSource.Factory(client).setUserAgent(requestUserAgent)
        val requestHeaders = LinkedHashMap<String, String>()
        requestHeaders.putAll(defaultRequestProperties)
        if (isYouTubeUri(dataSpec.uri)) requestHeaders.putAll(youtubeHeaders())
        if (requestHeaders.isNotEmpty()) factory.setDefaultRequestProperties(requestHeaders)
        val newDataSource = factory.createDataSource()
        dataSource = newDataSource
        val bytesToRead = try {
            newDataSource.open(dataSpec)
        } catch (e: HttpDataSource.InvalidResponseCodeException) {
            if (e.responseCode == 403) logForbidden(dataSpec)
            throw e
        }
        isTransferStarted = true
        transferStarted(dataSpec)
        return bytesToRead
    }

    private fun logForbidden(dataSpec: DataSpec) {
        val uri = dataSpec.uri
        val expire = uri.getQueryParameter("expire")?.toLongOrNull()
        val nowSec = System.currentTimeMillis() / 1000
        val expiry = when {
            expire == null -> "expire=absent"
            expire < nowSec -> "expire=PASSED ${nowSec - expire}s ago"
            else -> "expire=valid ${expire - nowSec}s left"
        }
        Log.w(TAG, "HTTP 403 c=${uri.getQueryParameter("c")} itag=${uri.getQueryParameter("itag")} mime=${uri.getQueryParameter("mime")} pot=${uri.getQueryParameter("pot") != null} range=${dataSpec.position}+${dataSpec.length} $expiry")
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val bytesRead = dataSource?.read(buffer, offset, length) ?: C.RESULT_END_OF_INPUT
        if (bytesRead > 0) {
            bytesTransferred(bytesRead)
        }
        return bytesRead
    }

    override fun close() {
        dataSource?.close()
        dataSource = null
        if (isTransferStarted) {
            isTransferStarted = false
            transferEnded()
        }
    }

    override fun getUri(): Uri? = currentUri

    override fun getResponseCode(): Int = (dataSource as? HttpDataSource)?.responseCode ?: -1

    override fun getResponseHeaders(): Map<String, List<String>> = (dataSource as? HttpDataSource)?.responseHeaders ?: emptyMap()

    override fun clearAllRequestProperties() {}

    override fun clearRequestProperty(name: String) {}

    override fun setRequestProperty(name: String, value: String) {}

    private fun isYouTubeUri(uri: Uri): Boolean {
        val host = uri.host ?: return false
        return host.contains("youtube.com") || host.contains("googlevideo.com") || host.contains("ytimg.com")
    }

    private fun resolveYouTubeUserAgent(uri: Uri): String = when (uri.getQueryParameter("c")?.uppercase()) {
        "IOS" -> "com.google.ios.youtube/21.03.1 (iPhone16,2; U; CPU iOS 18_2 like Mac OS X;)"
        "ANDROID", "ANDROID_CREATOR" -> "com.google.android.youtube/21.03.38 (Linux; U; Android 14) gzip"
        "ANDROID_VR" -> "com.google.android.apps.youtube.vr.oculus/1.61.48 (Linux; U; Android 12; en_US; Quest 3; Build/SQ3A.220605.009.A1; Cronet/132.0.6808.3)"
        "VISIONOS" -> "Mozilla/5.0 (Macintosh; Intel Mac OS X 15_7_3) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/26.0 Safari/605.1.15"
        "TVHTML5", "TVHTML5_SIMPLY_EMBEDDED_PLAYER" -> "Mozilla/5.0 (PlayStation; PlayStation 4/12.02) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.4 Safari/605.1.15"
        "MWEB" -> "Mozilla/5.0 (iPad; CPU OS 16_7_10 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1,gzip(gfe)"
        "WEB", "WEB_REMIX" -> "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"
        else -> userAgent
    }

    private fun youtubeHeaders(): Map<String, String> = mapOf(
        "Origin" to "https://www.youtube.com",
        "Referer" to "https://www.youtube.com/",
        "Sec-Fetch-Dest" to "empty",
        "Sec-Fetch-Mode" to "cors",
        "Sec-Fetch-Site" to "cross-site",
        "Accept-Encoding" to "identity",
        "Accept" to "*/*"
    )
}
