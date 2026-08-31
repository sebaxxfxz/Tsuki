package com.example.tsuki.playback

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import com.example.tsuki.data.local.PlayerPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File

object PlayerCacheProvider {

    @Volatile
    private var cache: Cache? = null

    fun get(context: Context): Cache =
        cache ?: synchronized(this) {
            cache ?: create(context.applicationContext).also { cache = it }
        }

    private fun create(context: Context): Cache {
        val sizeMb = runCatching {
            runBlocking(Dispatchers.IO) {
                PlayerPreferences(context).cacheSizeMb.first()
            }
        }.getOrDefault(PlayerPreferences.CACHE_SIZE_DEFAULT_MB)

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

    fun clear(context: Context) {
        val target = get(context)
        target.keys.toList().forEach { key ->
            runCatching { target.removeResource(key) }
        }
    }

    fun usedBytes(context: Context): Long = runCatching { get(context).cacheSpace }.getOrDefault(0L)
}
