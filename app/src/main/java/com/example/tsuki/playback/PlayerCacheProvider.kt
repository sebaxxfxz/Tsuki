package com.example.tsuki.playback

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.example.tsuki.data.local.PlayerPreferences
import java.io.File

object PlayerCacheProvider {

    @Volatile
    private var cache: Cache? = null

    fun get(context: Context): Cache =
        cache ?: synchronized(this) {
            cache ?: create(context.applicationContext).also { cache = it }
        }

    private fun create(context: Context): Cache {
        val sizeMb = readSizeMb(context)

        val evictor = if (sizeMb <= 0) {
            NoOpCacheEvictor()
        } else {
            LeastRecentlyUsedCacheEvictor(sizeMb.toLong() * 1024L * 1024L)
        }
        return SimpleCache(
            File(context.cacheDir, "exoplayer"),
            evictor,
            StandaloneDatabaseProvider(context),
        )
    }

    private const val CONFIG_PREFS = "tsuki_cache_config"
    private const val KEY_CACHE_SIZE_MB = "cache_size_mb"

    private fun readSizeMb(context: Context): Int = runCatching {
        context.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_CACHE_SIZE_MB, PlayerPreferences.CACHE_SIZE_DEFAULT_MB)
    }.getOrDefault(PlayerPreferences.CACHE_SIZE_DEFAULT_MB)

    fun persistSizeMb(context: Context, sizeMb: Int) {
        runCatching {
            context.applicationContext.getSharedPreferences(CONFIG_PREFS, Context.MODE_PRIVATE)
                .edit().putInt(KEY_CACHE_SIZE_MB, sizeMb).apply()
        }
    }

    fun clear(context: Context) {
        val target = get(context)
        target.keys.toList().forEach { key ->
            runCatching { target.removeResource(key) }
        }
    }

    fun usedBytes(context: Context): Long = runCatching { get(context).cacheSpace }.getOrDefault(0L)
}
