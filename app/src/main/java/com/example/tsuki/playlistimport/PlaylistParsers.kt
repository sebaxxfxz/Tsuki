package com.example.tsuki.playlistimport


data class ImportedSong(
    val title: String,
    val artists: List<String>,
    val album: String? = null,
    val durationMs: Int? = null
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
        val hasHeader = titleIdx >= 0 || artistIdx >= 0
        if (!hasHeader) {
            titleIdx = 0
            artistIdx = if (header.size > 1) 1 else -1
        }

        return lines.drop(if (hasHeader) 1 else 0)
            .mapNotNull { line ->
                val cells = splitCsvLine(line)
                val title = cells.getOrNull(titleIdx)?.trim()?.trim('"')?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val artists = artistIdx
                    .takeIf { it >= 0 }
                    ?.let { cells.getOrNull(it) }
                    ?.split(';', '|')
                    ?.map { it.trim().trim('"') }
                    ?.filter { it.isNotBlank() }
                    .orEmpty()
                ImportedSong(title = title, artists = artists)
            }
    }

    private val TITLE_HEADERS = setOf("title", "tracktitle", "songtitle", "trackname", "name")
    private val ARTIST_HEADERS = setOf("artist", "artists", "artistname", "artistnames", "trackartist")

    private fun normalizeHeaderCell(cell: String): String =
        cell.trim().trim('"').lowercase().replace(Regex("""[_\s]"""), "")

    private fun splitCsvLine(line: String): List<String> {
        val cells = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        for (c in line) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    cells.add(sb.toString()); sb.clear()
                }
                else -> sb.append(c)
            }
        }
        cells.add(sb.toString())
        return cells
    }
}

object PlaylistM3uParser {

    fun parse(content: String): List<ImportedSong> {
        if (!content.contains("#EXTM3U")) return emptyList()
        return content.lineSequence()
            .filter { it.startsWith("#EXTINF:") }
            .mapNotNull { line ->
                val info = line.removePrefix("#EXTINF:")
                val commaIdx = info.indexOf(',')
                val text = if (commaIdx >= 0) info.substring(commaIdx + 1) else info
                val dashIdx = text.indexOf(" - ")
                if (dashIdx > 0) {
                    ImportedSong(
                        title = text.substring(dashIdx + 3).trim(),
                        artists = text.substring(0, dashIdx).split(';', '|').map { it.trim() }.filter { it.isNotBlank() }
                    )
                } else {
                    ImportedSong(title = text.trim(), artists = emptyList())
                }.takeIf { it.title.isNotBlank() }
            }
            .toList()
    }
}
