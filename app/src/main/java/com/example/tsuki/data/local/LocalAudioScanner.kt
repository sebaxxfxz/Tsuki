package com.example.tsuki.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.example.tsuki.domain.model.MediaTrack
import com.example.tsuki.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LocalAudioScanner(private val context: Context) {

    private companion object {
        const val MIN_MUSIC_DURATION_MS = 30_000L
        val CHAT_FOLDER_REGEX = Regex("/(WhatsApp|Telegram|com\\.whatsapp|Media/WhatsApp[A-Za-z ]*)/", RegexOption.IGNORE_CASE)
    }

    suspend fun scanLocalTracks(excludedFolders: Set<String> = emptySet()): List<MediaTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<MediaTrack>()
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.IS_MUSIC,
            MediaStore.Audio.Media.DATA
        )

        val selection = buildString {
            append("${MediaStore.Audio.Media.IS_MUSIC} != 0")
            append(" AND ${MediaStore.Audio.Media.IS_NOTIFICATION} = 0")
            append(" AND ${MediaStore.Audio.Media.IS_ALARM} = 0")
            append(" AND ${MediaStore.Audio.Media.IS_RINGTONE} = 0")
            append(" AND ${MediaStore.Audio.Media.IS_PODCAST} = 0")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                append(" AND ${MediaStore.Audio.Media.IS_RECORDING} = 0")
            }
        }.toString()
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            val artworkUriBase = Uri.parse("content://media/external/audio/albumart")

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val title = cursor.getString(titleColumn) ?: "Desconocido"
                val artist = cursor.getString(artistColumn) ?: "Artista Desconocido"
                val album = cursor.getString(albumColumn) ?: "Álbum Desconocido"
                val albumId = cursor.getLong(albumIdColumn)
                val duration = cursor.getLong(durationColumn)

                if (duration > 0L && duration < MIN_MUSIC_DURATION_MS) continue

                val dataPath = try { cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)) ?: "" } catch (_: Exception) { "" }
                val folder = dataPath.substringBeforeLast('/')
                if (folder in excludedFolders) continue

                val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                val albumArtUri = ContentUris.withAppendedId(artworkUriBase, albumId).toString()

                tracks.add(
                    MediaTrack(
                        id = id.toString(),
                        title = title,
                        artist = artist,
                        album = album,
                        durationMs = duration,
                        streamUrl = contentUri.toString(),
                        artworkUrl = albumArtUri,
                        isLocal = true,
                        mediaType = MediaType.LOCAL_AUDIO
                    )
                )
            }
        }
        tracks
    }

    data class AudioFolder(val path: String, val trackCount: Int)

    suspend fun listAudioFolders(): List<AudioFolder> = withContext(Dispatchers.IO) {
        val counts = LinkedHashMap<String, Int>()
        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Media.DATA),
                "${MediaStore.Audio.Media.IS_MUSIC} != 0",
                null, null
            )?.use { c ->
                val dCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                while (c.moveToNext()) {
                    val dir = c.getString(dCol)?.substringBeforeLast('/') ?: continue
                    counts[dir] = (counts[dir] ?: 0) + 1
                }
            }
        } catch (_: Exception) {}
        counts.map { AudioFolder(it.key, it.value) }.sortedByDescending { it.trackCount }
    }
}
