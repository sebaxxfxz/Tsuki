package com.example.tsuki.data.recommendation

import com.example.tsuki.domain.model.MediaTrack
import kotlin.math.ln
import kotlin.math.sqrt

internal class TSukiTokenizer {

    companion object {
        val WHITESPACE_REGEX = Regex(pattern = "\\s+")
        val TIMESTAMP_PATTERN = Regex(pattern = "\\d{1,2}:\\d{2}")
        private val YEAR_TAG_REGEX = Regex(pattern = "^20[2-9]\\d$")

        const val IDF_COLD_START_DOCS = 30
        const val IDF_MIN_WEIGHT = 0.15
        const val IDF_MAX_WEIGHT = 1.0

        const val CHANNEL_KEYWORD_WEIGHT = 0.6
        const val TITLE_KEYWORD_WEIGHT = 0.5
        const val BIGRAM_WEIGHT = 0.75
        const val BIGRAM_PRIORITY_WEIGHT = 1.2
        const val DESCRIPTION_WORD_WEIGHT = 0.2
        const val DESCRIPTION_MIN_LENGTH = 20
        const val DESCRIPTION_TAKE_WORDS = 15
        const val DESCRIPTION_TAKE_LINES = 5
        const val DESCRIPTION_LINE_MIN = 15

        const val COMPLEXITY_TITLE_LEN_MAX = 80.0
        const val COMPLEXITY_TITLE_LEN_WEIGHT = 0.4
        const val COMPLEXITY_WORD_LEN_DIVISOR = 8.0
        const val COMPLEXITY_WORD_LEN_WEIGHT = 0.4
        const val COMPLEXITY_CHAPTER_BONUS = 0.2

        const val CHAPTER_TIMESTAMP_MIN = 3
    }

    private val LEMMA_MAP = mapOf(
        "gaming" to "game",
        "games" to "game",
        "gamer" to "game",
        "gamers" to "game",
        "gameplay" to "game",
        "coding" to "code",
        "coder" to "code",
        "programming" to "program",
        "programmer" to "program",
        "cooking" to "cook",
        "cooked" to "cook",
        "songs" to "song",
        "singing" to "sing",
        "singer" to "sing",
        "musics" to "music",
        "musical" to "music",
        "musician" to "music",
        "technologies" to "technology",
        "technological" to "technology",
        "computers" to "computer",
        "computing" to "computer",
        "drawing" to "draw",
        "painting" to "paint",
        "animating" to "animation",
        "animated" to "animation",
        "animator" to "animation",
        "animations" to "animation",
        "workouts" to "workout",
        "exercising" to "exercise",
        "exercises" to "exercise",
        "learning" to "learn",
        "learned" to "learn",
        "teaching" to "teach",
        "teacher" to "teach",
        "studying" to "study",
        "studies" to "study",
        "tutorials" to "tutorial",
        "making" to "make",
        "maker" to "make",
        "reviewing" to "review",
        "reviewed" to "review",
        "testing" to "test",
        "tested" to "test",
        "editing" to "edit",
        "edited" to "edit",
        "traveling" to "travel",
        "travelled" to "travel",
        "vlogging" to "vlog",
        "vlogs" to "vlog",
        "vlogger" to "vlog",
        "reactions" to "reaction",
        "compilations" to "compilation",
        "experiments" to "experiment",
        "experimental" to "experiment",
        "sciences" to "science",
        "scientific" to "science",
        "engineering" to "engineer",
        "animals" to "animal",
        "recipes" to "recipe",
        "baking" to "bake",
        "gardening" to "garden",
        "photographing" to "photography",
        "explained" to "explain",
        "explains" to "explain",
        "created" to "create",
        "creates" to "create",
        "creator" to "create",
        "videos" to "video",
        "channels" to "channel",
        "movies" to "movie",
        "documentaries" to "documentary",
        "podcasts" to "podcast",
        "interviews" to "interview"
    )

    private val STOP_WORDS = hashSetOf(
        "the", "and", "for", "that", "this", "with", "you", "how",
        "what", "when", "your", "which", "can", "make", "most",
        "into", "best", "from", "just", "about", "more", "some",
        "will", "one", "all", "would", "there", "their", "out",
        "not", "but", "have", "has", "been", "being", "was", "were", "are",
        "video", "official", "channel", "review", "reaction",
        "full", "episode", "part", "new", "latest", "update",
        "hdr", "uhd", "fps", "live", "stream",
        "watch", "subscribe", "like", "comment",
        "share", "click", "link", "description", "below", "check",
        "1080p", "720p", "480p", "360p",
        "amazing", "insane", "crazy", "incredible", "unbelievable",
        "shocking", "exposed", "revealed", "secret", "honest", "truth",
        "el", "la", "los", "las", "de", "del", "en", "y", "a",
        "con", "para", "por", "un", "una", "es", "son", "al",
        "lo", "le", "les", "se", "su", "sus", "me", "te",
        "mi", "tu", "nos", "que", "si", "no", "pero", "como",
        "mas", "muy", "tan", "ser", "hay", "era", "fue", "han"
    )

    private val SPONSOR_LINE_PATTERNS = listOf(
        "use code ",
        "% off",
        "free trial",
        "link in",
        "sponsored by",
        "brought to you",
        "check out",
        "sign up",
        "discount",
        "promo code",
        "affiliate",
        "partner",
        "merch",
        "patreon",
        "ko-fi",
        "buymeacoffee",
        "subscribe",
        "follow me",
        "social media",
        "instagram",
        "twitter",
        "tiktok",
        "discord",
        "join the",
        "become a member",
        "business inquiries",
        "contact:",
        "►",
        "→",
        "⬇",
        "timestamps:",
        "chapters:"
    )

    val HIGH_PACING_WORDS = setOf(
        "compilation", "highlights", "speedrun", "trailer", "shorts",
        "montage", "moments", "memes", "fails", "rapid",
        "fast", "quick", "minute", "seconds", "top 10",
        "top 5", "ranked", "tier list", "versus"
    )

    val LOW_PACING_WORDS = setOf(
        "podcast", "essay", "ambient", "explained", "study",
        "meditation", "sleep", "asmr", "relaxing", "calm",
        "deep dive", "analysis", "lecture", "course",
        "documentary", "interview", "conversation", "discussion",
        "breakdown", "walkthrough"
    )

    val MUSIC_KEYWORDS = setOf(
        "music", "song", "lyrics", "remix", "lofi", "lo-fi",
        "playlist", "official audio", "official video", "music video",
        "feat", "ft.", "acoustic", "cover", "karaoke",
        "instrumental", "beat", "rap", "hip hop", "pop",
        "rock", "jazz", "classical", "edm", "mix",
        "canción", "musica", "letra", "banda", "álbum"
    )

    fun tokenize(text: String): List<String> {
        val segments = text.lowercase().split(WHITESPACE_REGEX)
        val filtered = mutableListOf<String>()
        for (raw in segments) {
            val trimmed = raw.trim { ch -> !ch.isLetterOrDigit() }
            if (trimmed.length <= 2) continue
            val lemma = normalizeLemma(trimmed)
            if (STOP_WORDS.contains(lemma)) continue
            if (YEAR_TAG_REGEX.matches(lemma)) continue
            filtered.add(lemma)
        }
        return filtered
    }

    fun normalizeLemma(word: String): String {
        val lowered = word.lowercase()
        val mapped = LEMMA_MAP[lowered]
        return mapped ?: lowered
    }

    fun tokenizeForSimilarity(text: String): Set<String> {
        val tokens = tokenize(text)
        val set = HashSet<String>(tokens.size)
        for (t in tokens) set.add(t)
        return set
    }

    fun tokenizeChannelName(channelName: String): List<String> {
        val generic = hashSetOf(
            "official", "channel", "music", "tv", "records",
            "entertainment", "media", "network", "productions",
            "studio", "studios", "the", "vevo"
        )
        val base = tokenize(channelName)
        val out = mutableListOf<String>()
        for (token in base) {
            if (token !in generic) out.add(token)
        }
        return out
    }

    fun calculateIdfWeight(
        word: String,
        baseWeight: Double,
        idfSnapshot: TSukiIdfSnapshot
    ): Double {
        if (idfSnapshot.totalDocs < IDF_COLD_START_DOCS) return baseWeight
        val freq = idfSnapshot.wordFrequency[word] ?: 0
        val total = idfSnapshot.totalDocs.toDouble()
        val rawIdf = ln(1.0 + total / (freq + 1.0))
        val ceilingIdf = ln(1.0 + total)
        val normalized = (rawIdf / ceilingIdf).coerceIn(IDF_MIN_WEIGHT, IDF_MAX_WEIGHT)
        return baseWeight * normalized
    }

    fun extractDescriptionKeywords(
        description: String?,
        idfSnapshot: TSukiIdfSnapshot
    ): Map<String, Double> {
        if (description.isNullOrBlank()) return emptyMap()
        if (description.length < DESCRIPTION_MIN_LENGTH) return emptyMap()
        val lines = description.lines()
        val kept = mutableListOf<String>()
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.length <= DESCRIPTION_LINE_MIN) continue
            val lower = line.lowercase().trim()
            var isSponsor = false
            for (pattern in SPONSOR_LINE_PATTERNS) {
                if (lower.contains(pattern)) {
                    isSponsor = true
                    break
                }
            }
            if (isSponsor) continue
            if (lower.contains("http")) continue
            if (lower.trimStart().startsWith("#")) continue
            if (trimmed.length > 5 && trimmed == trimmed.uppercase()) continue
            kept.add(line)
            if (kept.size >= DESCRIPTION_TAKE_LINES) break
        }
        if (kept.isEmpty()) return emptyMap()
        val joined = kept.joinToString(separator = " ")
        if (joined.isBlank()) return emptyMap()
        val tokens = tokenize(joined)
        val limited = if (tokens.size > DESCRIPTION_TAKE_WORDS) tokens.subList(0, DESCRIPTION_TAKE_WORDS) else tokens
        val acc = mutableMapOf<String, Double>()
        for (token in limited) {
            val weight = calculateIdfWeight(token, DESCRIPTION_WORD_WEIGHT, idfSnapshot)
            acc[token] = (acc[token] ?: 0.0) + weight
        }
        return acc
    }

    fun extractFeatures(
        track: MediaTrack,
        idfSnapshot: TSukiIdfSnapshot,
        channelTopicProfile: Map<String, Double>?
    ): TSukiContentVector {
        val topicWeights = mutableMapOf<String, Double>()
        val titleTokens = tokenize(track.title)
        val channelLabel = if (track.artist.isNotBlank()) track.artist else ""
        val channelTokens = tokenize(channelLabel)
        for (token in channelTokens) {
            topicWeights[token] = calculateIdfWeight(token, CHANNEL_KEYWORD_WEIGHT, idfSnapshot)
        }
        val claimedIndices = mutableSetOf<Int>()
        if (titleTokens.size >= 2) {
            for (idx in 0 until titleTokens.size - 1) {
                val bigram = titleTokens[idx] + " " + titleTokens[idx + 1]
                topicWeights[bigram] = calculateIdfWeight(bigram, BIGRAM_WEIGHT, idfSnapshot)
            }
        }
        for (index in titleTokens.indices) {
            if (index in claimedIndices) continue
            val token = titleTokens[index]
            val existing = topicWeights[token] ?: 0.0
            val addition = calculateIdfWeight(token, TITLE_KEYWORD_WEIGHT, idfSnapshot)
            topicWeights[token] = existing + addition
        }
        val descWeights = extractDescriptionKeywords(track.descriptionText, idfSnapshot)
        for ((k, v) in descWeights) {
            topicWeights[k] = (topicWeights[k] ?: 0.0) + v
        }
        if (channelTopicProfile != null && channelTopicProfile.size >= 3) {
            for ((topic, chWeight) in channelTopicProfile) {
                if (!topicWeights.containsKey(topic)) {
                    topicWeights[topic] = chWeight * 0.3
                }
            }
        }
        val normalizedTopics: Map<String, Double>
        if (topicWeights.isNotEmpty()) {
            var sumSquares = 0.0
            for (value in topicWeights.values) sumSquares += value * value
            var magnitude = sqrt(sumSquares)
            val mapped = mutableMapOf<String, Double>()
            if (magnitude > 0) {
                for ((k, v) in topicWeights) mapped[k] = v / magnitude
            } else {
                mapped.putAll(topicWeights)
            }
            normalizedTopics = mapped
        } else {
            normalizedTopics = topicWeights.toMap()
        }
        val durationSeconds = when {
            track.durationMs > 0 -> track.durationMs / 1000.0
            track.isLive -> 3600.0
            else -> 300.0
        }
        val durationScore = (ln(1.0 + durationSeconds) / ln(1.0 + 7200.0)).coerceIn(0.0, 1.0)
        val lowerTitle = track.title.lowercase()
        var highHits = 0
        var lowHits = 0
        for (word in HIGH_PACING_WORDS) if (lowerTitle.contains(word)) highHits++
        for (word in LOW_PACING_WORDS) if (lowerTitle.contains(word)) lowHits++
        val pacingScore = when {
            highHits > lowHits -> (0.6 + highHits * 0.1).coerceAtMost(0.95)
            lowHits > highHits -> (0.4 - lowHits * 0.1).coerceAtLeast(0.05)
            track.isShort -> 0.85
            else -> 0.5
        }
        val rawDescription = track.descriptionText
        val chapterCount = if (rawDescription.isNullOrBlank()) 0 else TIMESTAMP_PATTERN.findAll(rawDescription).count()
        val hasChapters = chapterCount >= CHAPTER_TIMESTAMP_MIN
        val rawTitleParts = track.title.split(WHITESPACE_REGEX)
        val filteredParts = mutableListOf<String>()
        for (part in rawTitleParts) if (part.length > 1) filteredParts.add(part)
        val titleLenFactor = (track.title.length / COMPLEXITY_TITLE_LEN_MAX).coerceIn(0.0, COMPLEXITY_TITLE_LEN_WEIGHT)
        val avgLen = if (filteredParts.isNotEmpty()) {
            var totalLen = 0
            for (p in filteredParts) totalLen += p.length
            totalLen.toDouble() / filteredParts.size
        } else 4.0
        val wordLenFactor = (avgLen / COMPLEXITY_WORD_LEN_DIVISOR).coerceIn(0.0, COMPLEXITY_WORD_LEN_WEIGHT)
        val bonus = if (hasChapters) COMPLEXITY_CHAPTER_BONUS else 0.0
        val complexityScore = (titleLenFactor + wordLenFactor + bonus).coerceIn(0.0, 1.0)
        return TSukiContentVector(
            topics = normalizedTopics,
            duration = durationScore,
            pacing = pacingScore,
            complexity = complexityScore,
            isLive = if (track.isLive) 1.0 else 0.0
        )
    }
}
