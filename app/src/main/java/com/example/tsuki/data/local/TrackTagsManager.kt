package com.example.tsuki.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

data class TrackTagEntry(
    val videoId: String,
    val title: String,
    val artist: String,
    val mood: String,
    val updated: Long
)

class TrackTagsDbHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_TRACK_TAGS (
                video_id TEXT PRIMARY KEY,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                mood TEXT NOT NULL,
                updated INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_TRACK_TAGS")
        onCreate(db)
    }

    companion object {
        const val DATABASE_NAME = "tsuki_tags.db"
        const val DATABASE_VERSION = 1
        const val TABLE_TRACK_TAGS = "track_tags"
    }
}

class TrackTagsManager private constructor(context: Context) {

    private val dbHelper = TrackTagsDbHelper(context.applicationContext)
    private val _tagsVersion = MutableStateFlow(System.currentTimeMillis())
    val tagsVersion: StateFlow<Long> = _tagsVersion.asStateFlow()

    suspend fun setMood(videoId: String, title: String, artist: String, mood: String) =
        withContext(Dispatchers.IO) {
            try {
                val db = dbHelper.writableDatabase
                val values = ContentValues().apply {
                    put("video_id", videoId)
                    put("title", title)
                    put("artist", artist)
                    put("mood", mood.trim().lowercase())
                    put("updated", System.currentTimeMillis())
                }
                db.insertWithOnConflict(
                    TrackTagsDbHelper.TABLE_TRACK_TAGS,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
                _tagsVersion.value = System.currentTimeMillis()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

    suspend fun getMood(videoId: String): String? = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.readableDatabase
            db.query(
                TrackTagsDbHelper.TABLE_TRACK_TAGS,
                arrayOf("mood"),
                "video_id = ?",
                arrayOf(videoId),
                null, null, null
            ).use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteMood(videoId: String) = withContext(Dispatchers.IO) {
        try {
            val db = dbHelper.writableDatabase
            db.delete(TrackTagsDbHelper.TABLE_TRACK_TAGS, "video_id = ?", arrayOf(videoId))
            _tagsVersion.value = System.currentTimeMillis()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getAllTags(): List<TrackTagEntry> = withContext(Dispatchers.IO) {
        val list = mutableListOf<TrackTagEntry>()
        try {
            val db = dbHelper.readableDatabase
            db.query(
                TrackTagsDbHelper.TABLE_TRACK_TAGS,
                null, null, null, null, null,
                "updated DESC"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    list.add(
                        TrackTagEntry(
                            videoId = cursor.getString(cursor.getColumnIndexOrThrow("video_id")),
                            title = cursor.getString(cursor.getColumnIndexOrThrow("title")),
                            artist = cursor.getString(cursor.getColumnIndexOrThrow("artist")),
                            mood = cursor.getString(cursor.getColumnIndexOrThrow("mood")),
                            updated = cursor.getLong(cursor.getColumnIndexOrThrow("updated"))
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
        private var instance: TrackTagsManager? = null

        fun getInstance(context: Context): TrackTagsManager {
            return instance ?: synchronized(this) {
                instance ?: TrackTagsManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
