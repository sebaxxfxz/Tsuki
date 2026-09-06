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

data class RecognitionEntry(
    val title: String,
    val artist: String,
    val album: String? = null,
    val coverUrl: String? = null,
    val shazamUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

class RecognitionHistoryDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_HISTORY (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                album TEXT,
                cover_url TEXT,
                shazam_url TEXT,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        onCreate(db)
    }

    companion object {
        const val DATABASE_NAME = "tsuki_recognition.db"
        const val DATABASE_VERSION = 1
        const val TABLE_HISTORY = "recognition_history"
    }
}

class RecognitionHistoryManager private constructor(context: Context) {

    private val dbHelper = RecognitionHistoryDbHelper(context.applicationContext)

    private val _history = MutableStateFlow<List<RecognitionEntry>>(emptyList())
    val history: StateFlow<List<RecognitionEntry>> = _history.asStateFlow()

    suspend fun add(entry: RecognitionEntry) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("title", entry.title)
                put("artist", entry.artist)
                put("album", entry.album)
                put("cover_url", entry.coverUrl)
                put("shazam_url", entry.shazamUrl)
                put("created_at", entry.createdAt)
            }
            db.insert(RecognitionHistoryDbHelper.TABLE_HISTORY, null, values)
            db.execSQL(
                "DELETE FROM ${RecognitionHistoryDbHelper.TABLE_HISTORY} WHERE id NOT IN " +
                    "(SELECT id FROM ${RecognitionHistoryDbHelper.TABLE_HISTORY} ORDER BY created_at DESC LIMIT $MAX_ENTRIES)"
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refresh()
    }

    suspend fun remove(createdAt: Long, title: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(
            RecognitionHistoryDbHelper.TABLE_HISTORY,
            "created_at = ? AND title = ?",
            arrayOf(createdAt.toString(), title)
        )
        refresh()
    }

    suspend fun clear() = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(RecognitionHistoryDbHelper.TABLE_HISTORY, null, null)
        refresh()
    }

    suspend fun getAllEntries(): List<RecognitionEntry> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            RecognitionHistoryDbHelper.TABLE_HISTORY,
            arrayOf("title", "artist", "album", "cover_url", "shazam_url", "created_at"),
            null, null, null, null, "created_at DESC"
        )
        cursor.use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(
                        RecognitionEntry(
                            title = c.getString(0),
                            artist = c.getString(1),
                            album = c.getString(2),
                            coverUrl = c.getString(3),
                            shazamUrl = c.getString(4),
                            createdAt = c.getLong(5)
                        )
                    )
                }
            }
        }
    }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        _history.value = getAllEntries()
    }

    companion object {
        const val MAX_ENTRIES = 50

        @Volatile private var instance: RecognitionHistoryManager? = null

        fun getInstance(context: Context): RecognitionHistoryManager =
            instance ?: synchronized(this) {
                instance ?: RecognitionHistoryManager(context.applicationContext).also { instance = it }
            }
    }
}
