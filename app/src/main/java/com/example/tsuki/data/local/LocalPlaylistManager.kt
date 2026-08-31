package com.example.tsuki.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.tsuki.domain.model.MediaTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

data class LocalPlaylist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val trackCount: Int
)

data class LocalPlaylistWithTracks(
    val id: Long,
    val name: String,
    val tracks: List<MediaTrack>
)

class LocalPlaylistDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_PLAYLISTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_PLAYLIST_SONGS (
                playlist_id INTEGER NOT NULL,
                position INTEGER NOT NULL,
                track_json TEXT NOT NULL,
                PRIMARY KEY (playlist_id, position)
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        val migrated = try {
            db.beginTransaction()
            try {
                migrateTablePreservingData(db, TABLE_PLAYLISTS)
                migrateTablePreservingData(db, TABLE_PLAYLIST_SONGS)
                db.setTransactionSuccessful()
                true
            } finally {
                db.endTransaction()
            }
        } catch (_: Exception) {
            false
        }
        if (!migrated) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYLIST_SONGS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_PLAYLISTS")
            onCreate(db)
        }
    }

    private fun migrateTablePreservingData(db: SQLiteDatabase, table: String): Boolean {
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
        const val DATABASE_NAME = "tsuki_playlists.db"
        const val DATABASE_VERSION = 1
        const val TABLE_PLAYLISTS = "local_playlists"
        const val TABLE_PLAYLIST_SONGS = "local_playlist_songs"
    }
}

class LocalPlaylistManager private constructor(context: Context) {

    private val dbHelper = LocalPlaylistDbHelper(context.applicationContext)
    private val json = Json { ignoreUnknownKeys = true }

    private val _playlists = MutableStateFlow<List<LocalPlaylist>>(emptyList())
    val playlists: StateFlow<List<LocalPlaylist>> = _playlists.asStateFlow()

    suspend fun createPlaylist(name: String, tracks: List<MediaTrack>): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("name", name)
                put("created_at", System.currentTimeMillis())
            }
            val id = db.insert(LocalPlaylistDbHelper.TABLE_PLAYLISTS, null, values)
            if (id > 0) insertTracks(db, id, tracks)
            db.setTransactionSuccessful()
            id
        } finally {
            db.endTransaction()
        }.also { refresh() }
    }

    suspend fun addTracks(playlistId: Long, tracks: List<MediaTrack>) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val existing = getCurrentTrackCount(db, playlistId)
        db.beginTransaction()
        try {
            insertTracks(db, playlistId, tracks, startPosition = existing)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refresh()
    }

    suspend fun addTrackIfNotExists(playlistId: Long, track: MediaTrack): Boolean = withContext(Dispatchers.IO) {
        val tracks = getPlaylistTracks(playlistId)
        val trackVideoId = track.videoId ?: track.id
        val alreadyExists = tracks.any { (it.videoId ?: it.id) == trackVideoId }
        if (alreadyExists) {
            false
        } else {
            addTracks(playlistId, listOf(track))
            true
        }
    }

    suspend fun removeTrack(playlistId: Long, position: Int) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(
                LocalPlaylistDbHelper.TABLE_PLAYLIST_SONGS,
                "playlist_id = ? AND position = ?",
                arrayOf(playlistId.toString(), position.toString())
            )

            val cursor = db.rawQuery(
                "SELECT rowid FROM ${LocalPlaylistDbHelper.TABLE_PLAYLIST_SONGS} WHERE playlist_id = ? ORDER BY position ASC",
                arrayOf(playlistId.toString())
            )
            val rowIds = mutableListOf<Long>()
            cursor.use { c ->
                while (c.moveToNext()) {
                    rowIds.add(c.getLong(0))
                }
            }
            rowIds.forEachIndexed { newIndex, rowId ->
                db.execSQL(
                    "UPDATE ${LocalPlaylistDbHelper.TABLE_PLAYLIST_SONGS} SET position = ? WHERE rowid = ?",
                    arrayOf<Any>(newIndex, rowId)
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refresh()
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply { put("name", newName) }
        db.update(LocalPlaylistDbHelper.TABLE_PLAYLISTS, values, "id = ?", arrayOf(playlistId.toString()))
        refresh()
    }

    suspend fun deletePlaylist(playlistId: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(LocalPlaylistDbHelper.TABLE_PLAYLIST_SONGS, "playlist_id = ?", arrayOf(playlistId.toString()))
            db.delete(LocalPlaylistDbHelper.TABLE_PLAYLISTS, "id = ?", arrayOf(playlistId.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refresh()
    }

    suspend fun getPlaylistTracks(playlistId: Long): List<MediaTrack> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            LocalPlaylistDbHelper.TABLE_PLAYLIST_SONGS,
            arrayOf("track_json"),
            "playlist_id = ?",
            arrayOf(playlistId.toString()),
            null, null, "position ASC"
        )
        cursor.use { c ->
            buildList {
                while (c.moveToNext()) {
                    runCatching {
                        add(json.decodeFromString(MediaTrack.serializer(), c.getString(0)))
                    }
                }
            }
        }
    }

        suspend fun getAllPlaylists(): List<LocalPlaylistWithTracks> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val playlists = mutableListOf<LocalPlaylistWithTracks>()
        val cursor = db.rawQuery(
            "SELECT id, name FROM ${LocalPlaylistDbHelper.TABLE_PLAYLISTS} ORDER BY created_at DESC",
            null
        )
        cursor.use { c ->
            while (c.moveToNext()) {
                val pid = c.getLong(0)
                val name = c.getString(1)
                val tracks = getPlaylistTracks(pid)
                playlists.add(LocalPlaylistWithTracks(pid, name, tracks))
            }
        }
        playlists
    }

    suspend fun getPlaylist(playlistId: Long): LocalPlaylistWithTracks? = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val name = db.query(
            LocalPlaylistDbHelper.TABLE_PLAYLISTS,
            arrayOf("name"),
            "id = ?",
            arrayOf(playlistId.toString()),
            null, null, null
        ).use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: return@withContext null
        LocalPlaylistWithTracks(playlistId, name, getPlaylistTracks(playlistId))
    }

    suspend fun refresh() = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            """
            SELECT p.id, p.name, p.created_at, COUNT(s.position) AS cnt
            FROM ${LocalPlaylistDbHelper.TABLE_PLAYLISTS} p
            LEFT JOIN ${LocalPlaylistDbHelper.TABLE_PLAYLIST_SONGS} s ON s.playlist_id = p.id
            GROUP BY p.id
            ORDER BY p.created_at DESC
            """.trimIndent(),
            null
        )
        _playlists.value = cursor.use { c ->
            buildList {
                while (c.moveToNext()) {
                    add(LocalPlaylist(c.getLong(0), c.getString(1), c.getLong(2), c.getInt(3)))
                }
            }
        }
    }

    private fun insertTracks(db: SQLiteDatabase, playlistId: Long, tracks: List<MediaTrack>, startPosition: Int = 0) {
        db.beginTransaction()
        try {
            tracks.forEachIndexed { offset, track ->
                val values = ContentValues().apply {
                    put("playlist_id", playlistId)
                    put("position", startPosition + offset)
                    put("track_json", json.encodeToString(MediaTrack.serializer(), track))
                }
                db.insert(LocalPlaylistDbHelper.TABLE_PLAYLIST_SONGS, null, values)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun getCurrentTrackCount(db: SQLiteDatabase, playlistId: Long): Int =
        db.rawQuery(
            "SELECT COUNT(*) FROM ${LocalPlaylistDbHelper.TABLE_PLAYLIST_SONGS} WHERE playlist_id = ?",
            arrayOf(playlistId.toString())
        ).use { c -> if (c.moveToFirst()) c.getInt(0) else 0 }

    companion object {
        @Volatile private var instance: LocalPlaylistManager? = null

        fun getInstance(context: Context): LocalPlaylistManager =
            instance ?: synchronized(this) {
                instance ?: LocalPlaylistManager(context.applicationContext).also { instance = it }
            }
    }
}
