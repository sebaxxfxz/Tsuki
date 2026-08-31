package com.example.tsuki.playlistimport


data class ImportedSong(
    val title: String,
    val artists: List<String>,
    val album: String? = null,
    val durationMs: Int? = null,
    val videoId: String? = null,
    val thumbnailUrl: String? = null
) {
    val artistsText: String get() = artists.joinToString(", ")
    val searchQuery: String get() = "$title${if (artists.isNotEmpty()) " - ${artists.joinToString(" ")}" else ""}"
}

object PlaylistCsvParser {






    fun parse(content: String): List<ImportedSong> {
        val lines = content.lineSequence()
            .map { it.trim('\uFEFF') }
            .filter { it.isNotBlank() }
            .toList()
        if (lines.isEmpty()) return emptyList()

        val header = splitCsvLine(lines.first()).map { normalizeHeaderCell(it) }
        var titleIdx = header.indexOfFirst { it in TITLE_HEADERS }
        var artistIdx = header.indexOfFirst { it in ARTIST_HEADERS }
        val albumIdx = header.indexOfFirst { it in ALBUM_HEADERS }
        val durationIdx = header.indexOfFirst { it in DURATION_HEADERS }
        val hasHeader = titleIdx >= 0 || artistIdx >= 0
        if (!hasHeader) {
            titleIdx = 0
            artistIdx = if (header.size > 1) 1 else -1
        }

        return lines.drop(if (hasHeader) 1 else 0)
            .mapNotNull { line ->
                val cells = splitCsvLine(line)
                val title = cells.getOrNull(titleIdx)?.trim()?.trim('"')?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val rawArtist = artistIdx
                    .takeIf { it >= 0 }
                    ?.let { cells.getOrNull(it) }
                    ?.trim()?.trim('"')
                val artists = if (rawArtist.isNullOrBlank()) emptyList() else {
                    if (rawArtist.contains(';') || rawArtist.contains('|')) {
                        rawArtist.split(';', '|').map { it.trim().trim('"') }.filter { it.isNotBlank() }
                    } else if (rawArtist.contains(", ")) {
                        rawArtist.split(", ").map { it.trim().trim('"') }.filter { it.isNotBlank() }
                    } else {
                        listOf(rawArtist)
                    }
                }
                val album = albumIdx.takeIf { it >= 0 }?.let { cells.getOrNull(it)?.trim()?.trim('"')?.takeIf { a -> a.isNotBlank() } }
                val durationMs = durationIdx.takeIf { it >= 0 }?.let { cells.getOrNull(it)?.trim()?.trim('"') }?.let { dStr ->
                    dStr.toIntOrNull() ?: if (dStr.contains(":")) {
                        val parts = dStr.split(":")
                        if (parts.size == 2) {
                            ((parts[0].toIntOrNull() ?: 0) * 60 + (parts[1].toIntOrNull() ?: 0)) * 1000
                        } else null
                    } else null
                }
                ImportedSong(title = title, artists = artists, album = album, durationMs = durationMs)
            }
    }

    private val TITLE_HEADERS = setOf("title", "tracktitle", "songtitle", "trackname", "name")
    private val ARTIST_HEADERS = setOf("artist", "artists", "artistname", "artistnames", "trackartist")
    private val ALBUM_HEADERS = setOf("album", "albumname", "trackalbum", "record")
    private val DURATION_HEADERS = setOf("duration", "durationms", "trackduration", "length", "time")

    private fun normalizeHeaderCell(cell: String): String =
        cell.trim().trim('"').lowercase().replace(Regex("""[_\s]"""), "")

    private fun splitCsvLine(line: String): List<String> {
        val cells = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        sb.append('"'); i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }
                c == ',' && !inQuotes -> {
                    cells.add(sb.toString()); sb.clear()
                }
                else -> sb.append(c)
            }
            i++
        }
        cells.add(sb.toString())
        return cells.map { it.trim().removeSurrounding("\"").replace("\"\"", "\"") }
    }
}

object PlaylistM3uParser {

    fun parse(content: String): List<ImportedSong> {
        val normalized = content.trimStart('\uFEFF', '\n', '\r', ' ', '\t')
        if (!normalized.contains("#EXTM3U")) {
            val fallback = normalized.lineSequence().filter { it.isNotBlank() && !it.startsWith("#") }.mapNotNull { line ->
                val clean = line.trim()
                if (clean.isBlank()) null else ImportedSong(title = clean.substringAfterLast('/').substringAfterLast('\\'), artists = emptyList())
            }.toList()
            if (fallback.isNotEmpty() && fallback.size >= 2) return fallback
            return emptyList()
        }
        return normalized.lineSequence()
            .filter { it.trimStart().startsWith("#EXTINF:") }
            .mapNotNull { line ->
                val info = line.trim().removePrefix("#EXTINF:")
                val commaIdx = info.indexOf(',')
                val text = if (commaIdx >= 0) info.substring(commaIdx + 1) else info
                val dashIdx = text.indexOf(" - ")
                if (dashIdx > 0) {
                    ImportedSong(
                        title = text.substring(dashIdx + 3).trim(),
                        artists = text.substring(0, dashIdx).split(';', '|', ',').map { it.trim() }.filter { it.isNotBlank() }
                    )
                } else {
                    ImportedSong(title = text.trim(), artists = emptyList())
                }.takeIf { it.title.isNotBlank() }
            }
            .toList()
    }
}

object ArchiveTuneBackupParser {
    fun isZip(bytes: ByteArray): Boolean = bytes.size > 4 && bytes[0] == 0x50.toByte() && bytes[1] == 0x4B.toByte()
    fun extractTextFromZip(bytes: ByteArray): String? {
        return try {
            java.util.zip.ZipInputStream(bytes.inputStream()).use { zis ->
                var entry = zis.nextEntry
                var best: String? = null
                var bestSize = 0
                while (entry != null) {
                    val name = entry.name.lowercase()
                    if (!entry.isDirectory && (name.endsWith(".json") || name.endsWith(".csv") || name.endsWith(".m3u") || name.endsWith(".m3u8") || name.contains("playlist"))) {
                        val text = zis.readBytes().toString(Charsets.UTF_8)
                        if (text.isNotBlank() && text.length > bestSize) { best = text; bestSize = text.length }
                        if (name.endsWith(".json") && text.trimStart().startsWith("{")) return text
                    }
                    entry = zis.nextEntry
                }
                best
            }
        } catch (_: Exception) { null }
    }

    fun extractArchiveTuneDbPlaylists(context: android.content.Context, bytes: ByteArray): com.example.tsuki.playlistimport.SpotifyParsedResult? {
        return try {
            val tmpDir = java.io.File(context.cacheDir, "archivetune_import_${System.currentTimeMillis()}")
            tmpDir.mkdirs()
            var hasDb = false
            java.util.zip.ZipInputStream(bytes.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val outFile = java.io.File(tmpDir, entry.name.substringAfterLast('/').substringAfterLast('\\'))
                        if (entry.name.lowercase().endsWith(".db") || entry.name.lowercase().endsWith(".db-wal") || entry.name.lowercase().endsWith(".db-shm") || entry.name.lowercase().endsWith(".xml")) {
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { out -> zis.copyTo(out) }
                            if (outFile.name.endsWith(".db")) hasDb = true
                        } else {
                            val name = entry.name.lowercase()
                            if (name.endsWith(".db")) {
                                outFile.outputStream().use { out -> zis.copyTo(out) }
                                hasDb = true
                            }
                        }
                    }
                    entry = zis.nextEntry
                }
            }
            if (!hasDb) {
                tmpDir.deleteRecursively()
                return null
            }
            val dbFile = tmpDir.listFiles()?.firstOrNull { it.name.lowercase().endsWith(".db") } ?: run { tmpDir.deleteRecursively(); return null }
            val db = try {
                android.database.sqlite.SQLiteDatabase.openDatabase(dbFile.absolutePath, null, android.database.sqlite.SQLiteDatabase.OPEN_READWRITE)
            } catch (_: Exception) {
                tmpDir.deleteRecursively()
                return null
            }
            val resultSongs = mutableListOf<ImportedSong>()
            var playlistName: String? = null
            try {
                val plCursor = db.rawQuery("SELECT id, name FROM playlist", null)
                val playlists = mutableListOf<Pair<String, String>>()
                while (plCursor.moveToNext()) {
                    val id = plCursor.getString(0)
                    val name = plCursor.getString(1)
                    playlists.add(id to name)
                }
                plCursor.close()
                if (playlists.isNotEmpty()) {
                    val target = playlists.maxByOrNull { pid ->
                        val c = db.rawQuery("SELECT count(*) FROM playlist_song_map WHERE playlistId=?", arrayOf(pid.first))
                        c.moveToFirst()
                        val cnt = c.getInt(0)
                        c.close()
                        cnt
                    } ?: playlists.first()
                    val targetCount = db.rawQuery("SELECT count(*) FROM playlist_song_map WHERE playlistId=?", arrayOf(target.first)).use { 
                        it.moveToFirst(); it.getInt(0) 
                    }
                    val likedCount = db.rawQuery("SELECT count(*) FROM song WHERE liked=1", null).use { 
                        it.moveToFirst(); it.getInt(0) 
                    }
                    val artistMap = mutableMapOf<String, List<String>>()
                    val amCur = db.rawQuery("SELECT songId, artistId FROM song_artist_map", null)
                    val tmpMap = mutableMapOf<String, MutableList<String>>()
                    while (amCur.moveToNext()) {
                        val sid = amCur.getString(0)
                        val aid = amCur.getString(1)
                        tmpMap.getOrPut(sid) { mutableListOf() }.add(aid)
                    }
                    amCur.close()
                    val artistNames = mutableMapOf<String, String>()
                    val aCur = db.rawQuery("SELECT id, name FROM artist", null)
                    while (aCur.moveToNext()) artistNames[aCur.getString(0)] = aCur.getString(1)
                    aCur.close()
                    for ((sid, aids) in tmpMap) {
                        artistMap[sid] = aids.mapNotNull { artistNames[it] }
                    }

                    if (likedCount > 0) {
                        val likedCur = db.rawQuery("SELECT title, duration, albumName, id, thumbnailUrl FROM song WHERE liked=1", null)
                        while (likedCur.moveToNext()) {
                            val title = likedCur.getString(0) ?: continue
                            if (title.isBlank()) continue
                            val dur = if (!likedCur.isNull(1)) likedCur.getInt(1) * 1000 else null
                            val album = likedCur.getString(2)?.takeIf { it.isNotBlank() }
                            val sid = likedCur.getString(3)
                            val thumb = likedCur.getString(4)?.takeIf { it.isNotBlank() }
                            val artists = artistMap[sid] ?: emptyList()
                            resultSongs.add(ImportedSong(title = title.trim(), artists = artists, album = album, durationMs = dur, videoId = sid.takeIf { it.length == 11 }, thumbnailUrl = thumb))
                        }
                        likedCur.close()
                        playlistName = "Tus Me Gusta (ArchiveTune) • ${resultSongs.size}"
                    } else {
                        val allCur = db.rawQuery("SELECT title, duration, albumName, id, thumbnailUrl FROM song", null)
                        while (allCur.moveToNext()) {
                            val title = allCur.getString(0) ?: continue
                            if (title.isBlank()) continue
                            val dur = if (!allCur.isNull(1)) allCur.getInt(1) * 1000 else null
                            val album = allCur.getString(2)?.takeIf { it.isNotBlank() }
                            val sid = allCur.getString(3)
                            val thumb = allCur.getString(4)?.takeIf { it.isNotBlank() }
                            val artists = artistMap[sid] ?: emptyList()
                            resultSongs.add(ImportedSong(title = title.trim(), artists = artists, album = album, durationMs = dur, videoId = sid.takeIf { it.length == 11 }, thumbnailUrl = thumb))
                        }
                        allCur.close()
                        playlistName = "Respaldo Completo (ArchiveTune) • ${resultSongs.size}"
                    }

                } else {
                    val likedCur = db.rawQuery("SELECT title, duration, albumName, id, thumbnailUrl FROM song WHERE liked=1", null)
                    val artistMap = mutableMapOf<String, List<String>>()
                    val amCur = db.rawQuery("SELECT songId, artistId FROM song_artist_map", null)
                    val tmpMap2 = mutableMapOf<String, MutableList<String>>()
                    while (amCur.moveToNext()) {
                        val sid = amCur.getString(0)
                        val aid = amCur.getString(1)
                        tmpMap2.getOrPut(sid) { mutableListOf() }.add(aid)
                    }
                    amCur.close()
                    val artistNames2 = mutableMapOf<String, String>()
                    val aCur2 = db.rawQuery("SELECT id, name FROM artist", null)
                    while (aCur2.moveToNext()) artistNames2[aCur2.getString(0)] = aCur2.getString(1)
                    aCur2.close()
                    for ((sid, aids) in tmpMap2) artistMap[sid] = aids.mapNotNull { artistNames2[it] }
                    while (likedCur.moveToNext()) {
                        val title = likedCur.getString(0) ?: continue
                        if (title.isBlank()) continue
                        val dur = if (!likedCur.isNull(1)) likedCur.getInt(1) * 1000 else null
                        val album = likedCur.getString(2)?.takeIf { it.isNotBlank() }
                        val sid = likedCur.getString(3)
                        val thumb = likedCur.getString(4)?.takeIf { it.isNotBlank() }
                            val artists = artistMap[sid] ?: emptyList()
                        resultSongs.add(ImportedSong(title = title.trim(), artists = artists, album = album, durationMs = dur, videoId = sid.takeIf { it.length == 11 }, thumbnailUrl = thumb))
                    }
                    likedCur.close()
                    playlistName = "Tus Me Gusta (ArchiveTune) • ${resultSongs.size}"
                }
            } finally {
                try { db.close() } catch (_: Exception) {}
                try { tmpDir.deleteRecursively() } catch (_: Exception) {}
            }
            if (resultSongs.isEmpty()) null else com.example.tsuki.playlistimport.SpotifyParsedResult(playlistName ?: "ArchiveTune Import", resultSongs)
        } catch (e: Exception) {
            android.util.Log.w("ArchiveTuneParser", "extractArchiveTuneDbPlaylists failed: ${e.message}", e)
            null
        }
    }
}
