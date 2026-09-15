package com.example.tsuki.lyrics

import com.example.tsuki.domain.model.LyricsEntry
import com.example.tsuki.domain.model.WordTimestamp

object LyricsUtils {
    val LINE_REGEX = Regex("""((\[\d{1,3}:\d{2}(?:[.:]\d{2,3})?\]\s*)+)(.*)""")
    val TIME_REGEX = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{2,3}))?\]""")
    private val ENHANCED_LRC_WORD_TIME_REGEX = Regex("""<(\d{1,3}):(\d{2})(?:[.:](\d{2,3}))?>""")
    private val WHITESPACE_REGEX = "\\s+".toRegex()
    private const val CONTINUATION_GAP_SEC = 0.08

    private val HTML_ENTITY_REGEX = Regex("&#(x?[0-9a-fA-F]+);|&(amp|quot|apos|lt|gt|nbsp|ndash|mdash|lsquo|rsquo|sbquo|ldquo|rdquo|bdquo|hellip|laquo|raquo|copy|reg|trade|times|divide|plusmn);")
    private data class EnhancedWordSlot(
        val text: String,
        val startSec: Double,
        val rawEndSec: Double?,
        val hadTrailingSpace: Boolean
    )
    private val VOICE_LABEL_REGEX = Regex("^([Vv]\\d{1,2})\\s*:")

    private val TTML_DETECT_REGEX = Regex("""<[A-Za-z0-9_-]*:?tt[\s>:]""")
    private val TTML_P_BEGIN_DETECT_REGEX = Regex("""<p\s+[^>]*begin\s*=""")
    private val TTML_PARAGRAPH_REGEX = Regex("""<p\b([^>]*)>(.*?)</p\s*>""", RegexOption.DOT_MATCHES_ALL)
    private val TTML_SPAN_REGEX = Regex("""<span\b([^>]*)>(.*?)</span\s*>""", RegexOption.DOT_MATCHES_ALL)
    private val TTML_ATTRIBUTE_REGEX = Regex("""([A-Za-z_][A-Za-z0-9_.:-]*)\s*=\s*("([^"]*)"|'([^']*)')""")
    private val TTML_NUMBER_PART_REGEX = Regex("""^\d+(?:\.\d+)?$""")
    private val XML_TAG_REGEX = Regex("""<[^>]*>""")
    private const val DEFAULT_TTML_LINE_DURATION_MS = 4000L

    fun unescapeHtml(text: String): String =
        if (!text.contains('&')) text
        else text.replace(HTML_ENTITY_REGEX) { m ->
            if (m.groupValues[1].isNotEmpty()) {
                val code = m.groupValues[1]
                val codePoint = if (code.startsWith("x") || code.startsWith("X")) {
                    code.substring(1).toIntOrNull(16)
                } else {
                    code.toIntOrNull()
                }
                codePoint?.takeIf { it > 0 }?.let { String(Character.toChars(it)) } ?: m.value
            } else {
                when (m.groupValues[2]) {
                    "amp" -> "&"
                    "quot" -> "\""
                    "apos" -> "'"
                    "lt" -> "<"
                    "gt" -> ">"
                    "nbsp" -> " "
                    "ndash" -> "–"
                    "mdash" -> "—"
                    "lsquo", "sbquo" -> "‘"
                    "rsquo" -> "’"
                    "ldquo", "bdquo" -> "“"
                    "rdquo" -> "”"
                    "hellip" -> "…"
                    "laquo" -> "«"
                    "raquo" -> "»"
                    "copy" -> "©"
                    "reg" -> "®"
                    "trade" -> "™"
                    "times" -> "×"
                    "divide" -> "÷"
                    "plusmn" -> "±"
                    else -> m.value
                }
            }
        }

    const val LYRICS_NOT_FOUND = "LYRICS_NOT_FOUND"

    fun isLineSyncedLrc(lyrics: String): Boolean =
        lyrics.lineSequence().any { LINE_REGEX.matches(it.trim()) }

    fun hasWordSyncedLyrics(lyrics: String): Boolean {
        if (isTtmlLyrics(lyrics)) return true
        return lyrics.lineSequence().any { line ->
            (LINE_REGEX.matches(line.trim()) || line.trim().startsWith("[")) && ENHANCED_LRC_WORD_TIME_REGEX.containsMatchIn(line)
        }
    }

    fun isTtmlLyrics(lyrics: String): Boolean =
        TTML_DETECT_REGEX.containsMatchIn(lyrics) ||
            TTML_P_BEGIN_DETECT_REGEX.containsMatchIn(lyrics) ||
            lyrics.contains("<tt") ||
            lyrics.contains("<p begin=")

    fun parseTtmlLyrics(lyrics: String): List<LyricsEntry> {
        val withoutComments = lyrics.replace(Regex("""<!--.*?-->"""), "")
        val paragraphs = TTML_PARAGRAPH_REGEX.findAll(withoutComments)
            .map { it to parseTtmlTimeMs(ttmlAttributeValue(it.groupValues[1], "begin")) }
            .toList()
        if (paragraphs.isEmpty()) return emptyList()

        val entries = mutableListOf<LyricsEntry>()
        for (index in paragraphs.indices) {
            val (match, beginMs) = paragraphs[index]
            val start = beginMs ?: continue
            val endRaw = parseTtmlTimeMs(ttmlAttributeValue(match.groupValues[1], "end"))
            val end = endRaw
                ?: paragraphs.drop(index + 1).firstOrNull { it.second != null }?.second
                ?: start + DEFAULT_TTML_LINE_DURATION_MS

            val inner = match.groupValues[2]
            val spanMatches = TTML_SPAN_REGEX.findAll(inner).toList()

            var words: List<WordTimestamp>? = null
            val text: String

            if (spanMatches.isEmpty()) {
                text = flattenTtmlText(inner)
                if (text.isEmpty()) continue
            } else {
                val segments = mutableListOf<WordTimestamp>()
                for (spanIndex in spanMatches.indices) {
                    val spanMatch = spanMatches[spanIndex]
                    val spanText = flattenTtmlText(spanMatch.groupValues[2])
                    if (spanText.isEmpty()) continue
                    val spanAttrs = spanMatch.groupValues[1]
                    val wordStartMs = parseTtmlTimeMs(ttmlAttributeValue(spanAttrs, "begin"))
                        ?: segments.lastOrNull()?.let { (it.endTime * 1000.0).toLong() }
                        ?: start
                    val wordEndMs = parseTtmlTimeMs(ttmlAttributeValue(spanAttrs, "end"))
                        ?: spanMatches.drop(spanIndex + 1)
                            .firstOrNull { flattenTtmlText(it.groupValues[2]).isNotEmpty() }
                            ?.let { next -> parseTtmlTimeMs(ttmlAttributeValue(next.groupValues[1], "begin")) }
                        ?: end
                    val gapAfter = if (spanIndex + 1 < spanMatches.size) {
                        inner.substring(spanMatch.range.last + 1, spanMatches[spanIndex + 1].range.first)
                    } else {
                        inner.substring(spanMatch.range.last + 1)
                    }
                    val spacedText = if (gapAfter.any { it.isWhitespace() }) "$spanText " else spanText
                    segments.add(
                        WordTimestamp(
                            text = spacedText,
                            startTime = wordStartMs / 1000.0,
                            endTime = wordEndMs / 1000.0
                        )
                    )
                }
                if (segments.isEmpty()) {
                    text = flattenTtmlText(inner)
                    if (text.isEmpty()) continue
                } else {
                    words = segments
                    text = segments.joinToString("") { it.text }.replace(WHITESPACE_REGEX, " ").trim()
                }
            }

            entries.add(
                LyricsEntry(
                    time = start,
                    text = text,
                    words = words,
                    durationMs = (end - start).coerceAtLeast(0L)
                )
            )
        }
        return if (entries.isEmpty()) emptyList() else entries.sorted()
    }

    private fun ttmlAttributeValue(attributes: String, localName: String): String? {
        for (match in TTML_ATTRIBUTE_REGEX.findAll(attributes)) {
            val name = match.groupValues[1].substringAfterLast(':')
            if (name.equals(localName, ignoreCase = true)) {
                return match.groupValues[3] + match.groupValues[4]
            }
        }
        return null
    }

    private fun parseTtmlTimeMs(raw: String?): Long? {
        val trimmed = raw?.trim() ?: return null
        if (trimmed.isEmpty() || trimmed.startsWith("-")) return null
        if (trimmed.endsWith("ms")) {
            return trimmed.removeSuffix("ms").toLongOrNull()
        }
        val cleaned = trimmed.removeSuffix("s")
        val parts = cleaned.split(':')
        if (parts.size !in 1..3) return null
        var seconds = 0.0
        for (part in parts) {
            if (!TTML_NUMBER_PART_REGEX.matches(part)) return null
            seconds = seconds * 60.0 + part.toDouble()
        }
        if (seconds < 0.0) return null
        return (seconds * 1000.0).toLong()
    }

    private fun flattenTtmlText(raw: String): String =
        VOICE_LABEL_REGEX.replaceFirst(unescapeHtml(XML_TAG_REGEX.replace(raw, "")), "").replace(WHITESPACE_REGEX, " ").trim()

    fun parseLyrics(lyrics: String): List<LyricsEntry> {
        val normalized = normalizeLyricsText(lyrics)
        if (isTtmlLyrics(normalized)) {
            return parseTtmlLyrics(normalized)
        }
        val lines = normalized.lines()
        val result = mutableListOf<LyricsEntry>()

        for (line in lines) {
            val parsed = parseLineSyncedLrcLine(line)
            if (parsed != null) {
                result.addAll(parsed)
            }
        }

        if (result.isEmpty() && normalized.isNotEmpty() && normalized != LYRICS_NOT_FOUND) {
            val metadataTagRegex = Regex("^\\[([a-zA-Z]+|\\d{2}:).*?\\]$")
            val plainLines = lines.map { it.trim() }.filter { line ->
                line.isNotEmpty() && !metadataTagRegex.matches(line)
            }
            return plainLines.map { LyricsEntry(time = -1L, text = it) }
        }

        return result.sorted()
    }

    private fun parseLineSyncedLrcLine(line: String): List<LyricsEntry>? {
        val trimmed = line.trim()
        val match = LINE_REGEX.matchEntire(trimmed) ?: return null
        val timesPart = match.groupValues[1]
        val rawTextPart = match.groupValues[3].trim()
        val voiceMatch = VOICE_LABEL_REGEX.find(rawTextPart)
        val agent = voiceMatch?.groupValues?.getOrNull(1)?.lowercase()
        val textPart = if (voiceMatch != null) {
            rawTextPart.substring(voiceMatch.range.last + 1).trim()
        } else {
            rawTextPart
        }

        val times = TIME_REGEX.findAll(timesPart).map { m ->
            val min = m.groupValues[1].toLongOrNull() ?: 0L
            val sec = m.groupValues[2].toLongOrNull() ?: 0L
            val milStr = m.groupValues[3]
            val mil = when (milStr.length) {
                1 -> milStr.toLong() * 100
                2 -> milStr.toLong() * 10
                3 -> milStr.toLong()
                else -> milStr.take(3).padEnd(3, '0').toLongOrNull() ?: 0L
            }
            min * 60000 + sec * 1000 + mil
        }.toList()

        if (times.isEmpty()) return null


        val words = parseEnhancedLrcWords(textPart, times.first())
        val cleanText = unescapeHtml(ENHANCED_LRC_WORD_TIME_REGEX.replace(textPart, "")).replace(WHITESPACE_REGEX, " ").trim()

        if (cleanText.isEmpty()) return null

        return times.map { time ->
            LyricsEntry(
                time = time,
                text = cleanText,
                words = words,
                agent = agent
            )
        }
    }

    private fun parseEnhancedLrcWords(textWithTimestamps: String, lineStartMs: Long): List<WordTimestamp>? {
        val wordMatches = ENHANCED_LRC_WORD_TIME_REGEX.findAll(textWithTimestamps).toList()
        if (wordMatches.isEmpty()) return null

        val segments = mutableListOf<WordTimestamp>()
        val rawEnds = mutableListOf<Double>()

        if (wordMatches.first().range.first > 0) {
            val prefix = textWithTimestamps.substring(0, wordMatches.first().range.first)
            val cleanPrefix = unescapeHtml(prefix.trim())
            if (cleanPrefix.isNotEmpty()) {
                val firstMatchTime = parseLrcMatchTimeSec(wordMatches.first())
                val startSec = lineStartMs / 1000.0
                val endSec = if (firstMatchTime > startSec) firstMatchTime else startSec + 1.0
                val spaced = if (prefix.last().isWhitespace()) "$cleanPrefix " else cleanPrefix
                segments.add(WordTimestamp(text = spaced, startTime = startSec, endTime = endSec))
                rawEnds.add(endSec)
            }
        }

        val items = mutableListOf<EnhancedWordSlot>()
        for (i in wordMatches.indices) {
            val wMatch = wordMatches[i]
            val textStart = wMatch.range.last + 1
            val textEnd = if (i + 1 < wordMatches.size) wordMatches[i + 1].range.first else textWithTimestamps.length
            if (textStart >= textEnd) continue

            val rawWordText = textWithTimestamps.substring(textStart, textEnd)
            val wordText = rawWordText.trim()
            if (wordText.isEmpty()) {
                if (rawWordText.any { it.isWhitespace() }) {
                    if (items.isNotEmpty()) {
                        val lastItem = items.last()
                        if (!lastItem.hadTrailingSpace) {
                            items[items.size - 1] = lastItem.copy(hadTrailingSpace = true)
                        }
                    } else if (segments.isNotEmpty()) {
                        val lastSeg = segments.last()
                        if (!lastSeg.text.endsWith(" ")) {
                            segments[segments.size - 1] = lastSeg.copy(text = lastSeg.text + " ")
                        }
                    }
                }
                continue
            }

            val hadTrailingSpace = rawWordText.isNotEmpty() && rawWordText.last().isWhitespace()
            val rawEndSec = if (i + 1 < wordMatches.size) {
                parseLrcMatchTimeSec(wordMatches[i + 1])
            } else {
                null
            }
            items.add(
                EnhancedWordSlot(
                    text = unescapeHtml(wordText),
                    startSec = parseLrcMatchTimeSec(wMatch),
                    rawEndSec = rawEndSec,
                    hadTrailingSpace = hadTrailingSpace
                )
            )
        }

        for ((index, item) in items.withIndex()) {
            val wordEndSec = items.getOrNull(index + 1)?.startSec
                ?: item.rawEndSec
                ?: (item.startSec + (item.text.length * 0.08).coerceIn(0.2, 0.45))
            val spacedText = if (item.hadTrailingSpace) "${item.text} " else item.text

            val prev = segments.lastOrNull()
            val prevRawEnd = rawEnds.lastOrNull() ?: prev?.endTime
            val isContinuation = prev != null && prevRawEnd != null &&
                !prev.text.endsWith(" ") &&
                item.startSec - prevRawEnd <= CONTINUATION_GAP_SEC

            if (isContinuation) {
                segments[segments.size - 1] = prev!!.copy(
                    text = prev.text + spacedText,
                    endTime = wordEndSec
                )
                rawEnds[rawEnds.size - 1] = item.rawEndSec ?: wordEndSec
            } else {
                segments.add(WordTimestamp(text = spacedText, startTime = item.startSec, endTime = wordEndSec))
                rawEnds.add(item.rawEndSec ?: wordEndSec)
            }
        }

        return segments.takeIf { it.isNotEmpty() }
    }

    private fun parseLrcMatchTimeSec(match: MatchResult): Double {
        val min = match.groupValues[1].toLongOrNull() ?: 0L
        val sec = match.groupValues[2].toLongOrNull() ?: 0L
        val milStr = match.groupValues[3]
        val mil = when (milStr.length) {
            1 -> milStr.toLong() * 100
            2 -> milStr.toLong() * 10
            3 -> milStr.toLong()
            else -> milStr.take(3).padEnd(3, '0').toLongOrNull() ?: 0L
        }
        return (min * 60000 + sec * 1000 + mil) / 1000.0
    }

    fun normalizeLyricsText(lyrics: String): String =
        lyrics
            .replace("\uFEFF", "")
            .replace(Regex("[\u200B\u200C\u200D\u2060\u00AD]"), "")
            .trim()

    fun hasMeaningfulLyricsContent(lyrics: String): Boolean {
        val normalized = normalizeLyricsText(lyrics)
        return normalized.isNotEmpty() && normalized != LYRICS_NOT_FOUND
    }
}
