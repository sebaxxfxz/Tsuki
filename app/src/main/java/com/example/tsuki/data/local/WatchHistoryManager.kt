package com.example.tsuki.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

data class WatchHistoryEntry(
    val videoId: String,
    val title: String,
    val artist: String,
    val artworkUrl: String?,
    val isVideoItem: Boolean,
    val playCount: Int,
    val lastPlayedTimestamp: Long,
    val watchDurationMs: Long
)

class WatchHistoryDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_HISTORY (
                video_id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                artwork_url TEXT,
                is_video INTEGER NOT NULL DEFAULT 0,
                play_count INTEGER NOT NULL DEFAULT 1,
                last_played INTEGER NOT NULL,
                watch_duration INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(SQL_CREATE_PLAY_EVENTS)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 1) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
            onCreate(db)
        }
        if (oldVersion < 2) {
            db.execSQL(SQL_CREATE_PLAY_EVENTS)
        }
    }

    companion object {
        const val DATABASE_NAME = "tsuki_history.db"
        const val DATABASE_VERSION = 2
        const val TABLE_HISTORY = "watch_history"
        const val TABLE_PLAY_EVENTS = "play_events"
        private val SQL_CREATE_PLAY_EVENTS = """
            CREATE TABLE IF NOT EXISTS play_events (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                video_id TEXT NOT NULL,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                artwork_url TEXT,
                timestamp INTEGER NOT NULL,
                play_time_ms INTEGER NOT NULL DEFAULT 0
            )
        """.trimIndent()
    }
}

class WatchHistoryManager private constructor(context: Context) {

    private val dbHelper = WatchHistoryDbHelper(context.applicationContext)
    private val _historyVersion = MutableStateFlow(System.currentTimeMillis())
    val historyVersion: StateFlow<Long> = _historyVersion.asStateFlow()

    fun recentTracksFlow(limit: Int = 20, audioOnly: Boolean = false): Flow<List<MediaTrack>> {
        return historyVersion.map {
            val tracks = getRecentTracks(limit)
            if (audioOnly) tracks.filter { !it.isVideoItem } else tracks
        }
    }

    suspend fun getRecentTracks(limit: Int = 20): List<MediaTrack> = withContext(Dispatchers.IO) {
        getRecentHistory(limit).map { entry ->
            MediaTrack(
                id = entry.videoId,
                title = entry.title,
                artist = entry.artist,
                artworkUrl = entry.artworkUrl,
                isLocal = false,
                mediaType = if (entry.isVideoItem) MediaType.STREAM_VIDEO else MediaType.STREAM_AUDIO,
                videoId = entry.videoId,
                isVideoItem = entry.isVideoItem
            )
        }
    }

    suspend fun recordPlayback(track: MediaTrack, watchDurationMs: Long = 0L) = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            val videoId = track.videoId ?: track.id
            val now = System.currentTimeMillis()

            db.beginTransaction()
            try {
                var playCount = 1
                var existingDuration = 0L
                db.query(
                    WatchHistoryDbHelper.TABLE_HISTORY,
                    arrayOf("play_count", "watch_duration"),
                    "video_id = ?",
                    arrayOf(videoId),
                    null, null, null
                ).use { cursor ->
                    if (cursor.moveToFirst()) {
                        playCount = cursor.getInt(0) + 1
                        existingDuration = cursor.getLong(1)
                    }
                }

                val values = ContentValues().apply {
                    put("video_id", videoId)
                    put("title", track.title)
                    put("artist", track.artist)
                    put("artwork_url", track.artworkUrl)
                    put("is_video", if (track.isVideoItem) 1 else 0)
                    put("play_count", playCount)
                    put("last_played", now)
                    put("watch_duration", existingDuration + watchDurationMs)
                }

                db.insertWithOnConflict(
                    WatchHistoryDbHelper.TABLE_HISTORY,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            _historyVersion.value = now
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getRecentHistory(limit: Int = 30): List<WatchHistoryEntry> = withContext(Dispatchers.IO) {
        val list = mutableListOf<WatchHistoryEntry>()
        try {
            val db = dbHelper.readableDatabase
            db.query(
                WatchHistoryDbHelper.TABLE_HISTORY,
                null, null, null, null, null,
                "last_played DESC",
                limit.toString()
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(
                        WatchHistoryEntry(
                            videoId = cursor.getString(cursor.getColumnIndexOrThrow("video_id")),
                            title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                            artist = cursor.getString(cursor.getColumnIndexOrThrow("artist")),
                            artworkUrl = cursor.getString(cursor.getColumnIndexOrThrow("artwork_url")),
                            isVideoItem = cursor.getInt(cursor.getColumnIndexOrThrow("is_video")) == 1,
                            playCount = cursor.getInt(cursor.getColumnIndexOrThrow("play_count")),
                            lastPlayedTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow("last_played")),
                            watchDurationMs = cursor.getLong(cursor.getColumnIndexOrThrow("watch_duration"))
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    suspend fun getMostPlayedTracks(limit: Int = 15, days: Int = 14): List<MediaTrack> = withContext(Dispatchers.IO) {
        val list = mutableListOf<WatchHistoryEntry>()
        try {
            val db = dbHelper.readableDatabase
            val cutoff = System.currentTimeMillis() - days * 24L * 3600L * 1000L
            db.query(
                WatchHistoryDbHelper.TABLE_HISTORY,
                null,
                "last_played > ?",
                arrayOf(cutoff.toString()),
                null, null,
                "play_count DESC, last_played DESC",
                limit.toString()
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(
                        WatchHistoryEntry(
                            videoId = cursor.getString(cursor.getColumnIndexOrThrow("video_id")),
                            title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                            artist = cursor.getString(cursor.getColumnIndexOrThrow("artist")),
                            artworkUrl = cursor.getString(cursor.getColumnIndexOrThrow("artwork_url")),
                            isVideoItem = cursor.getInt(cursor.getColumnIndexOrThrow("is_video")) == 1,
                            playCount = cursor.getInt(cursor.getColumnIndexOrThrow("play_count")),
                            lastPlayedTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow("last_played")),
                            watchDurationMs = cursor.getLong(cursor.getColumnIndexOrThrow("watch_duration"))
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list.map { entry ->
            MediaTrack(
                id = entry.videoId,
                title = entry.title,
                artist = entry.artist,
                artworkUrl = entry.artworkUrl,
                isLocal = false,
                mediaType = if (entry.isVideoItem) MediaType.STREAM_VIDEO else MediaType.STREAM_AUDIO,
                videoId = entry.videoId,
                isVideoItem = entry.isVideoItem
            )
        }
    }

    suspend fun getForgottenFavorites(limit: Int = 20): List<MediaTrack> = withContext(Dispatchers.IO) {
        val list = mutableListOf<WatchHistoryEntry>()
        try {
            val db = dbHelper.readableDatabase
            val cutoff = System.currentTimeMillis() - 14L * 24L * 3600L * 1000L
            db.query(
                WatchHistoryDbHelper.TABLE_HISTORY,
                null,
                "last_played < ? AND play_count >= 2",
                arrayOf(cutoff.toString()),
                null, null,
                "play_count DESC",
                (limit * 3).toString()
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(
                        WatchHistoryEntry(
                            videoId = cursor.getString(cursor.getColumnIndexOrThrow("video_id")),
                            title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                            artist = cursor.getString(cursor.getColumnIndexOrThrow("artist")),
                            artworkUrl = cursor.getString(cursor.getColumnIndexOrThrow("artwork_url")),
                            isVideoItem = cursor.getInt(cursor.getColumnIndexOrThrow("is_video")) == 1,
                            playCount = cursor.getInt(cursor.getColumnIndexOrThrow("play_count")),
                            lastPlayedTimestamp = cursor.getLong(cursor.getColumnIndexOrThrow("last_played")),
                            watchDurationMs = cursor.getLong(cursor.getColumnIndexOrThrow("watch_duration"))
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list.shuffled().take(limit).map { entry ->
            MediaTrack(
                id = entry.videoId,
                title = entry.title,
                artist = entry.artist,
                artworkUrl = entry.artworkUrl,
                isLocal = false,
                mediaType = if (entry.isVideoItem) MediaType.STREAM_VIDEO else MediaType.STREAM_AUDIO,
                videoId = entry.videoId,
                isVideoItem = entry.isVideoItem
            )
        }
    }

    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                db.delete(WatchHistoryDbHelper.TABLE_HISTORY, null, null)
                db.delete(WatchHistoryDbHelper.TABLE_PLAY_EVENTS, null, null)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            _historyVersion.value = System.currentTimeMillis()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun recordPlayEvent(
        videoId: String,
        title: String,
        artist: String,
        artworkUrl: String?,
        playTimeMs: Long,
        timestamp: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.IO) {
        if (playTimeMs <= 0) return@withContext
        try {
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                put("video_id", videoId)
                put("title", title)
                put("artist", artist)
                put("artwork_url", artworkUrl)
                put("timestamp", timestamp)
                put("play_time_ms", playTimeMs)
            }
            db.insert(WatchHistoryDbHelper.TABLE_PLAY_EVENTS, null, values)
            try {
                db.delete(
                    WatchHistoryDbHelper.TABLE_PLAY_EVENTS,
                    "id NOT IN (SELECT id FROM play_events ORDER BY id DESC LIMIT 20000)",
                    null
                )
            } catch (_: Exception) {}
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    data class ListeningTotals(
        val totalPlays: Int,
        val totalTimeListenedMs: Long,
        val uniqueSongs: Int,
        val uniqueArtists: Int
    )

    suspend fun getListeningTotals(fromTimestamp: Long): ListeningTotals = withContext(Dispatchers.IO) {
        var totals = ListeningTotals(0, 0L, 0, 0)
        try {
            val db = dbHelper.readableDatabase
            db.rawQuery(
                """
                SELECT COUNT(1),
                       COALESCE(SUM(play_time_ms),0),
                       COUNT(DISTINCT video_id),
                       COUNT(DISTINCT artist)
                FROM play_events WHERE timestamp >= ?
                """.trimIndent(),
                arrayOf(fromTimestamp.toString())
            ).use { cursor ->
                if (cursor.moveToFirst()) {
                    totals = ListeningTotals(cursor.getInt(0), cursor.getLong(1), cursor.getInt(2), cursor.getInt(3))
                }
            }
        } catch (_: Exception) {}
        totals
    }

    data class TopEntry(val title: String, val subtitle: String, val artworkUrl: String?, val plays: Int, val timeListenedMs: Long)

    suspend fun getTopSongs(limit: Int, fromTimestamp: Long): List<TopEntry> = withContext(Dispatchers.IO) {
        val list = mutableListOf<TopEntry>()
        try {
            val db = dbHelper.readableDatabase
            db.rawQuery(
                """
                SELECT title, artist, artwork_url, COUNT(1), SUM(play_time_ms)
                FROM play_events WHERE timestamp >= ?
                GROUP BY video_id ORDER BY COUNT(1) DESC, SUM(play_time_ms) DESC LIMIT ?
                """.trimIndent(),
                arrayOf(fromTimestamp.toString(), limit.toString())
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(TopEntry(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getInt(3), cursor.getLong(4)))
                }
            }
        } catch (_: Exception) {}
        list
    }

    suspend fun getTopArtists(limit: Int, fromTimestamp: Long): List<TopEntry> = withContext(Dispatchers.IO) {
        val list = mutableListOf<TopEntry>()
        try {
            val db = dbHelper.readableDatabase
            db.rawQuery(
                """
                SELECT artist, MAX(COALESCE(artwork_url,'')), COUNT(DISTINCT video_id), SUM(play_time_ms)
                FROM play_events WHERE timestamp >= ?
                GROUP BY artist ORDER BY SUM(play_time_ms) DESC LIMIT ?
                """.trimIndent(),
                arrayOf(fromTimestamp.toString(), limit.toString())
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(TopEntry(cursor.getString(0), "${cursor.getInt(2)} canciones", cursor.getString(1), cursor.getInt(2), cursor.getLong(3)))
                }
            }
        } catch (_: Exception) {}
        list
    }

    suspend fun getHourDistribution(fromTimestamp: Long): List<Long> = withContext(Dispatchers.IO) {
        val buckets = MutableList(24) { 0L }
        try {
            val db = dbHelper.readableDatabase
            db.rawQuery(
                """
                SELECT CAST(strftime('%H', datetime(timestamp/1000,'unixepoch','localtime')) AS INTEGER), SUM(play_time_ms)
                FROM play_events WHERE timestamp >= ? GROUP BY 1
                """.trimIndent(),
                arrayOf(fromTimestamp.toString())
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val hour = cursor.getInt(0).coerceIn(0, 23)
                    buckets[hour] = cursor.getLong(1)
                }
            }
        } catch (_: Exception) {}
        buckets
    }

    suspend fun getWeekdayDistribution(fromTimestamp: Long): List<Long> = withContext(Dispatchers.IO) {
        val buckets = MutableList(7) { 0L }
        try {
            val db = dbHelper.readableDatabase
            db.rawQuery(
                """
                SELECT CAST(strftime('%w', datetime(timestamp/1000,'unixepoch','localtime')) AS INTEGER), SUM(play_time_ms)
                FROM play_events WHERE timestamp >= ? GROUP BY 1
                """.trimIndent(),
                arrayOf(fromTimestamp.toString())
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val day = cursor.getInt(0).coerceIn(0, 6)
                    buckets[day] = cursor.getLong(1)
                }
            }
        } catch (_: Exception) {}
        buckets
    }

    companion object {
        @Volatile
        private var instance: WatchHistoryManager? = null

        fun getInstance(context: Context): WatchHistoryManager {
            return instance ?: synchronized(this) {
                instance ?: WatchHistoryManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
