package com.example.tsuki.network

import android.content.Context
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.util.concurrent.TimeUnit




class NewPipeDownloader private constructor(private val context: Context) : Downloader() {

    @Volatile var sessionCookie: String? = null

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectionPool(okhttp3.ConnectionPool(32, 5, TimeUnit.MINUTES))
        .fastFallback(true)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0"

        @Volatile
        private var INSTANCE: NewPipeDownloader? = null

        fun getInstance(context: Context): NewPipeDownloader {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: NewPipeDownloader(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val builder = okhttp3.Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)

        if (url.contains("youtube.com") || url.contains("youtu.be")) {
            sessionCookie?.takeIf { it.isNotBlank() }?.let { cookie ->
                builder.header("Cookie", cookie)
            }
        }

        for ((key, list) in headers) {
            builder.removeHeader(key)
            for (value in list) builder.addHeader(key, value)
        }

        if (httpMethod == "POST") {
            val body = if (dataToSend != null) {
                okhttp3.RequestBody.create(null, dataToSend)
            } else {
                okhttp3.RequestBody.create(null, ByteArray(0))
            }
            builder.post(body)
        } else {
            builder.get()
        }

        return client.newCall(builder.build()).execute().use { response ->
            if (response.code == 429) throw ReCaptchaException("reCaptcha Challenge requested", url)
            val responseString = response.body?.string() ?: ""
            val responseHeaders = response.headers.toMultimap()
            Response(response.code, response.message, responseHeaders, responseString, url)
        }
    }
}
