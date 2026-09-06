package com.example.tsuki.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FavoritesDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_FAVORITES (
                video_id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                artwork_url TEXT,
                is_video INTEGER NOT NULL DEFAULT 0,
                added_timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        val migrated = try {
            db.beginTransaction()
            try {
                migrateTablePreservingData(db, TABLE_FAVORITES)
                db.setTransactionSuccessful()
                true
            } finally {
                db.endTransaction()
            }
        } catch (_: Exception) {
            false
        }
        if (!migrated) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_FAVORITES")
            onCreate(db)
        }
    }

    private fun tableExists(db: SQLiteDatabase, table: String): Boolean {
        db.rawQuery(
            "SELECT 1 FROM sqlite_master WHERE type = 'table' AND name = ?",
            arrayOf(table)
        ).use { cursor -> return cursor.moveToFirst() }
    }

    private fun migrateTablePreservingData(db: SQLiteDatabase, table: String): Boolean {
        if (!tableExists(db, table)) {
            onCreate(db)
            return true
        }
        val oldTable = "${table}_old"
        db.execSQL("DROP TABLE IF EXISTS $oldTable")
        db.execSQL("ALTER TABLE $table RENAME TO $oldTable")
        onCreate(db)
        val commonColumns = mutableListOf<String>()
        db.rawQuery("PRAGMA table_info($oldTable)", null).use { cursor ->
            while (cursor.moveToNext()) commonColumns.add(cursor.getString(1))
        }
        db.rawQuery("PRAGMA table_info($table)", null).use { cursor ->
            val newColumns = mutableSetOf<String>()
            while (cursor.moveToNext()) newColumns.add(cursor.getString(1))
            commonColumns.retainAll(newColumns)
        }
        if (commonColumns.isEmpty()) return false
        val columns = commonColumns.joinToString(",")
        db.execSQL("INSERT INTO $table ($columns) SELECT $columns FROM $oldTable")
        db.execSQL("DROP TABLE $oldTable")
        return true
    }

    companion object {
        const val DATABASE_NAME = "tsuki_favorites.db"
        const val DATABASE_VERSION = 1
        const val TABLE_FAVORITES = "favorites"
    }
}

class FavoritesManager private constructor(context: Context) {

    private val dbHelper = FavoritesDbHelper(context.applicationContext)
    private val toggleMutex = Mutex()
    private val _favoritesVersion = MutableStateFlow(System.currentTimeMillis())
    val favoritesVersion: StateFlow<Long> = _favoritesVersion.asStateFlow()

    suspend fun addFavorites(tracks: List<MediaTrack>, clearExisting: Boolean = false) = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                if (clearExisting) {
                    db.delete(FavoritesDbHelper.TABLE_FAVORITES, null, null)
                }
                var baseTimestamp = System.currentTimeMillis()
                for (track in tracks) {
                    val videoId = track.videoId ?: track.id
                    val values = ContentValues().apply {
                        put("video_id", videoId)
                        put("title", track.title)
                        put("artist", track.artist)
                        put("artwork_url", track.artworkUrl)
                        put("is_video", if (track.isVideoItem) 1 else 0)
                        put("added_timestamp", baseTimestamp)
                    }
                    baseTimestamp -= 10L
                    db.insertWithOnConflict(
                        FavoritesDbHelper.TABLE_FAVORITES,
                        null,
                        values,
                        SQLiteDatabase.CONFLICT_REPLACE
                    )
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            _favoritesVersion.value = System.currentTimeMillis()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun toggleFavorite(track: MediaTrack): Boolean = withContext(Dispatchers.IO) {
        toggleMutex.withLock {
            val key = track.videoId ?: track.id
            val isFav = isFavorite(key)
            if (isFav) {
                removeFavorite(key)
                false
            } else {
                addFavorite(track)
                true
            }
        }
    }

    suspend fun addFavorite(track: MediaTrack) = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            val videoId = track.videoId ?: track.id
            var existingTs: Long? = null
            db.query(
                FavoritesDbHelper.TABLE_FAVORITES,
                arrayOf("added_timestamp"),
                "video_id = ?",
                arrayOf(videoId),
                null, null, null
            ).use { c ->
                if (c.moveToFirst()) existingTs = c.getLong(0)
            }
            val values = ContentValues().apply {
                put("video_id", videoId)
                put("title", track.title)
                put("artist", track.artist)
                put("artwork_url", track.artworkUrl)
                put("is_video", if (track.isVideoItem) 1 else 0)
                put("added_timestamp", existingTs ?: System.currentTimeMillis())
            }
            db.insertWithOnConflict(
                FavoritesDbHelper.TABLE_FAVORITES,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
            _favoritesVersion.value = System.currentTimeMillis()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun removeFavorite(trackId: String) = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            db.delete(FavoritesDbHelper.TABLE_FAVORITES, "video_id = ?", arrayOf(trackId))
            _favoritesVersion.value = System.currentTimeMillis()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun isFavorite(trackId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.readableDatabase
            db.query(
                FavoritesDbHelper.TABLE_FAVORITES,
                arrayOf("video_id"),
                "video_id = ?",
                arrayOf(trackId),
                null, null, null
            ).use { cursor -> cursor.moveToFirst() }
        } catch (e: Exception) {
            false
        }
    }

    suspend fun isTrackFavorite(track: MediaTrack): Boolean = withContext(Dispatchers.IO) {
        val key = track.videoId ?: track.id
        isFavorite(key) || (track.videoId != null && isFavorite(track.id))
    }

    suspend fun getFavoriteTracks(): List<MediaTrack> = withContext(Dispatchers.IO) {
        val list = mutableListOf<MediaTrack>()
        try {
            val db = dbHelper.readableDatabase
            db.query(
                FavoritesDbHelper.TABLE_FAVORITES,
                null, null, null, null, null,
                "added_timestamp DESC"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val videoId = cursor.getString(cursor.getColumnIndexOrThrow("video_id"))
                    val isVideo = cursor.getInt(cursor.getColumnIndexOrThrow("is_video")) == 1
                    list.add(
                        MediaTrack(
                            id = videoId,
                            title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                            artist = cursor.getString(cursor.getColumnIndexOrThrow("artist")),
                            artworkUrl = cursor.getString(cursor.getColumnIndexOrThrow("artwork_url")),
                            isLocal = false,
                            mediaType = if (isVideo) MediaType.STREAM_VIDEO else MediaType.STREAM_AUDIO,
                            videoId = videoId,
                            isVideoItem = isVideo
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    companion object {
        @Volatile
        private var instance: FavoritesManager? = null

        fun getInstance(context: Context): FavoritesManager {
            return instance ?: synchronized(this) {
                instance ?: FavoritesManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
