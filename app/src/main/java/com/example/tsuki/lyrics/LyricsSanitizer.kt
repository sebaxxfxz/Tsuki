package com.example.tsuki.lyrics

object LyricsSanitizer {
    private val NOISE_PATTERNS = listOf(
        Regex("""\s*[\(\[]\s*(?:Official\s+(?:Music\s+)?Video|Official\s+Audio|Lyric(?:s)?\s+Video|Audio\s+Oficial|Video\s+Oficial|Visualizer|Animated\s+Video|Performance\s+Video|Behind\s+The\s+Scenes|Live|(?:4K|HD|HQ)\s*(?:Video|Audio)?|feat\.?\s+[^\)\]]+|ft\.?\s+[^\)\]]+|prod\.?\s+(?:by\s+)?[^\)\]]+|MV|M/V|PV|Short\s+Ver(?:sion)?\.|Full\s+Ver(?:sion)?\.)\s*[\)\]]\s*""", RegexOption.IGNORE_CASE),
        Regex("""\s*[\(\[]\s*(?:from\s+".+?"|with\s+.+?)\s*[\)\]]\s*""", RegexOption.IGNORE_CASE),
        Regex("""\s*[\|\-–—]\s*(?:Official\s+(?:Music\s+)?Video|Official\s+Audio|Lyrics?)\s*$""", RegexOption.IGNORE_CASE),
        Regex("""\s*(?:\uD83C[\uDFA4\uDFB5\uDFB6\uDFBC]|\uD83D[\uDD25\uDCAF])\s*"""),
    )

    fun cleanTitle(title: String): String {
        var clean = title.trim()
        for (pattern in NOISE_PATTERNS) {
            clean = pattern.replace(clean, " ")
        }
        return clean.replace(Regex("\\s{2,}"), " ").trim()
    }

    fun cleanArtist(artist: String): String {
        return artist
            .replace(Regex("\\s*[,&×]\\s*|\\s+x\\s+", RegexOption.IGNORE_CASE), ", ")
            .replace(Regex("\\s+(?:feat|ft|featuring)\\.?\\s+.*", RegexOption.IGNORE_CASE), "")
            .split(",")
            .firstOrNull()?.trim() ?: artist.trim()
    }
}
