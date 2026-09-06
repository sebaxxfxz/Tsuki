package com.example.tsuki.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class LyricsDatabase private constructor(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE lyrics (
                video_id TEXT PRIMARY KEY,
                track_title TEXT NOT NULL,
                artist TEXT NOT NULL,
                lyrics_raw TEXT NOT NULL,
                source TEXT NOT NULL DEFAULT 'REMOTE',
                cached_timestamp INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS lyrics")
        onCreate(db)
    }

    data class CachedLyrics(val raw: String, val source: String)

    fun getCachedLyrics(videoId: String): CachedLyrics? {
        val db = readableDatabase
        return db.query(
            "lyrics",
            arrayOf("lyrics_raw", "source"),
            "video_id = ?",
            arrayOf(videoId),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) CachedLyrics(cursor.getString(0), cursor.getString(1)) else null
        }
    }

    fun getLyrics(videoId: String): String? = getCachedLyrics(videoId)?.raw

    fun saveLyrics(videoId: String, title: String, artist: String, raw: String, source: String) {
        if (videoId.isBlank() || raw.isBlank() || raw == "LYRICS_NOT_FOUND") return
        val db = writableDatabase
        val values = ContentValues().apply {
            put("video_id", videoId)
            put("track_title", title)
            put("artist", artist)
            put("lyrics_raw", raw)
            put("source", source)
            put("cached_timestamp", System.currentTimeMillis())
        }
        db.insertWithOnConflict("lyrics", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        runCatching {
            db.delete(
                "lyrics",
                "video_id NOT IN (SELECT video_id FROM lyrics ORDER BY cached_timestamp DESC LIMIT 1500)",
                null
            )
        }
    }

    companion object {
        private const val DATABASE_NAME = "lyrics.db"
        private const val DATABASE_VERSION = 1

        @Volatile
        private var instance: LyricsDatabase? = null

        fun getInstance(context: Context): LyricsDatabase {
            return instance ?: synchronized(this) {
                instance ?: LyricsDatabase(context.applicationContext).also { instance = it }
            }
        }
    }
}
