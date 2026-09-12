package com.example.tsuki

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.example.tsuki.network.NewPipeDownloader
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

class TSukiApp : Application(), SingletonImageLoader.Factory {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(com.example.tsuki.util.AppLocale.wrap(base, com.example.tsuki.util.AppLocale.readStored(base)))
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        com.example.tsuki.ui.player.canvas.CanvasDiskCache.init(this)
        val homePrefs = com.example.tsuki.data.local.HomePreferences(this)
        val initialAppLocale = com.example.tsuki.util.AppLocale.resolveTag(com.example.tsuki.util.AppLocale.readStored(this))
        val initialLang = if (initialAppLocale == com.example.tsuki.util.AppLocale.ENGLISH) "en" else "es"
        val initialCountry = if (initialLang == "en") "US" else "ES"
        applyContentLocale(initialLang, initialCountry)
        MainScope().launch(Dispatchers.IO) {
            combine(homePrefs.contentLanguageTag, homePrefs.contentCountry) { lang, country -> lang to country }
                .collect { (lang, country) -> applyContentLocale(lang, country) }
        }
        initTSukiBrain()
        MainScope().launch(Dispatchers.IO) {
            try {
                val authManager = com.example.tsuki.auth.YouTubeAuthManager(this@TSukiApp)
                authManager.cookie.collect { cookie ->
                    NewPipeDownloader.getInstance(this@TSukiApp).sessionCookie = cookie
                    Log.d("TSukiApp", "NewPipe session cookie ${if (cookie.isNullOrBlank()) "cleared" else "applied (${cookie.length} chars)"}")
                }
            } catch (e: Exception) {
                Log.e("TSukiApp", "Failed to sync NewPipe cookie", e)
            }
        }
        MainScope().launch(Dispatchers.IO) {
            try {
                val prefs = com.example.tsuki.data.local.HomePreferences(this@TSukiApp)
                val fav = prefs.favoriteChannels.first()
                val subRepo = com.example.tsuki.data.local.TSukiSubscriptionRepository.getInstance(this@TSukiApp)
                if (subRepo.getAllIds().isEmpty() && fav.isNotEmpty()) {
                    val toImport = fav.mapNotNull { entry: String ->
                        val p = entry.split("|")
                        if (p[0].isBlank()) null else com.example.tsuki.data.local.TSukiChannelSubscription(p[0], p.getOrNull(1) ?: p[0], p.getOrNull(2) ?: "")
                    }
                    toImport.forEach { subRepo.subscribe(it) }
                    try { com.example.tsuki.data.recommendation.TSukiNeuroEngine.bootstrapFromSubscriptions(this@TSukiApp, toImport.map { it.channelName }) } catch (_: Exception) {}
                }
            } catch (_: Exception) {}
        }
    }

    private fun initTSukiBrain() {
        MainScope().launch {
            withContext(Dispatchers.IO) {
                com.example.tsuki.data.recommendation.TSukiNeuroEngine.initialize(this@TSukiApp)
            }
        }
    }

    @Synchronized
    private fun applyContentLocale(languageTag: String, country: String) {
        try {
            val previous = Pair(
                com.example.tsuki.network.TSukiContentLocale.languageTag,
                com.example.tsuki.network.TSukiContentLocale.countryCode
            )
            com.example.tsuki.network.TSukiContentLocale.languageTag = languageTag
            com.example.tsuki.network.TSukiContentLocale.countryCode = country
            if (previous.first != null && previous != languageTag to country) {

                java.io.File(cacheDir, "tsuki_home_feed_cache.json").delete()
            }
            val languagePart = languageTag.substringBefore('-')
            val regionPart = languageTag.substringAfter('-', "").takeIf { it.isNotEmpty() }
            val localization = if (regionPart != null) Localization(languagePart, regionPart) else Localization(languagePart)
            NewPipe.init(NewPipeDownloader.getInstance(this), localization, ContentCountry(country))
            Log.d("TSukiApp", "Content locale applied: hl=$languageTag gl=$country")
        } catch (e: Exception) {
            Log.e("TSukiApp", "Failed to apply content locale", e)
            initNewPipe()
        }
    }

    private fun initNewPipe() {
        try {
            val locale = java.util.Locale.getDefault()
            val localization = Localization.fromLocale(locale)
            val countryCode = locale.country.takeIf { it.isNotBlank() } ?: "ES"
            val country = ContentCountry(countryCode)
            NewPipe.init(NewPipeDownloader.getInstance(this), localization, country)
            Log.d("TSukiApp", "NewPipe initialized: ${localization.localizationCode} ${country.countryCode}")
        } catch (e: Exception) {
            Log.e("TSukiApp", "Failed to init NewPipe", e)
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val playbackChannel = NotificationChannel(
            "tsuki_playback",
            getString(R.string.notification_channel_playback),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.notification_channel_playback_desc)
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(playbackChannel)
    }

    private val imageHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }

    override fun newImageLoader(context: android.content.Context): ImageLoader {
        return ImageLoader.Builder(context)
            .components {
                add(
                    OkHttpNetworkFetcherFactory(
                        callFactory = { imageHttpClient }
                    )
                )
            }
            .build()
    }
}