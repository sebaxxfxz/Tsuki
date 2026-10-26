package com.example.tsuki.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsUtilsTest {

    @Test
    fun keepsUntimedLinesAsContinuationOfPreviousTimedLine() {
        val entries = LyricsUtils.parseLyrics(
            "[00:10.00]First line\n" +
                "Second line without timestamp\n" +
                "Third line without timestamp"
        )
        assertEquals(listOf(10000L, 10000L, 10000L), entries.map { it.time })
        assertEquals(
            listOf("First line", "Second line without timestamp", "Third line without timestamp"),
            entries.map { it.text }
        )
    }

    @Test
    fun keepsTextlessWordTimedLineAsInstrumental() {
        val entries = LyricsUtils.parseLyrics(
            "[00:10.00]<00:10.00>Hold <00:10.50>me <00:11.00>close\n" +
                "[00:30.00]<00:30.00> <00:31.00>\n" +
                "[00:50.00]<00:50.00>Next <00:51.00>line"
        )
        assertEquals(listOf(10000L, 30000L, 50000L), entries.map { it.time })
        assertTrue(entries[1].isInstrumental)
        assertTrue(entries[1].text.isBlank())
    }

    @Test
    fun keepsTextlessTtmlParagraphAsInstrumental() {
        val ttml = "<tt xmlns=\"http://www.w3.org/ns/ttml\"><body><div>" +
            "<p begin=\"00:00:10.000\" end=\"00:00:14.000\">Hold me close</p>" +
            "<p begin=\"00:00:30.000\" end=\"00:00:34.000\">" +
            "<span begin=\"00:00:30.000\" end=\"00:00:32.000\"></span></p>" +
            "<p begin=\"00:00:50.000\" end=\"00:00:54.000\">Next line</p>" +
            "</div></body></tt>"
        val entries = LyricsUtils.parseLyrics(ttml)
        assertEquals(listOf(10000L, 30000L, 50000L), entries.map { it.time })
        assertTrue(entries[1].isInstrumental)
    }

    @Test
    fun plainTextOnlyDocumentDropsMetadataTags() {
        val entries = LyricsUtils.parseLyrics("Line one\nLine two\n\n[Verse 1]\nLine three")
        assertEquals(listOf("Line one", "Line two", "Line three"), entries.map { it.text })
        assertTrue(entries.all { it.time == -1L })
    }
}
