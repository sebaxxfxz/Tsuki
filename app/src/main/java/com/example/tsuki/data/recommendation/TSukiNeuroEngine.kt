package com.example.tsuki.data.recommendation

import android.content.Context
import android.util.Log
import com.example.tsuki.domain.model.MediaTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.ln

class TSukiNeuroEngine private constructor(private val appContext: Context) {

    companion object {
        private const val TAG = "TSukiNeuroEngine"
        private const val FEATURE_CACHE_MAX = 150
        private const val SAVE_DEBOUNCE_MS = 5_000L
        private const val VIDEO_SUPPRESSION_DAYS = 30L
        private const val MAX_SUPPRESSED_VIDEOS = 500
        private const val MAX_SUPPRESSED_CHANNELS = 100
        private const val COLD_START_THRESHOLD = 30
        private const val ONBOARDING_WARMUP_INTERACTIONS = 50
        private const val ONBOARDING_MAX_BOOST = 0.15
        private const val CHANNEL_EMA_ALPHA = 0.05
        private const val CHANNEL_EMA_DECAY = 1.0 - CHANNEL_EMA_ALPHA
        private const val SUBSCRIPTION_BOOST = 0.12
        private const val JITTER_COLD_START = 0.20
        private const val JITTER_NORMAL = 0.02
        private const val WATCHED_PENALTY_FULL = 0.02
        private const val WATCHED_PENALTY_HALF = 0.30
        private const val WATCHED_THRESHOLD_FULL = 0.85f
        private const val WATCHED_THRESHOLD_HALF = 0.50f
        private const val IMPRESSION_PENALTY_HEAVY = 0.05
        private const val IMPRESSION_PENALTY_MEDIUM = 0.30
        private const val IMPRESSION_THRESHOLD_HEAVY = 3
        private const val IMPRESSION_THRESHOLD_LIGHT = 1
        private const val MOMENTUM_WINDOW = 10
        private const val MOMENTUM_BOOST = 0.08
        private const val MOMENTUM_THRESHOLD = 3
        private const val MAX_CHANNEL_SCORES = 500
        private const val AFFINITY_INCREMENT = 0.01
        private const val AFFINITY_MAX = 1.0
        private const val AFFINITY_PRUNE_THRESHOLD = 0.05
        private const val AFFINITY_MAX_ENTRIES = 500
        private const val FEED_HISTORY_MAX = 3_000
        private const val FEED_HISTORY_EXPIRY_DAYS = 14L
        private const val RECENT_QUERIES_MAX = 20
        private const val REJECTION_PENALTY_1 = 0.50
        private const val REJECTION_PENALTY_2 = 0.20
        private const val REJECTION_PENALTY_3_PLUS = 0.05
        private const val REJECTION_EXPIRY_DAYS = 14L
        private const val PERSONA_STABILITY_THRESHOLD = 3
        private const val PERSONA_MAX_STABILITY = 10

        @Volatile private var instance: TSukiNeuroEngine? = null

        fun getInstance(context: Context): TSukiNeuroEngine =
            instance ?: synchronized(this) {
                instance ?: TSukiNeuroEngine(context.applicationContext).also { instance = it }
            }

        private fun requireInstance(): TSukiNeuroEngine =
            instance ?: error("TSukiNeuroEngine not initialized. Call getInstance(context) first.")

        suspend fun initialize(context: Context) = getInstance(context).initialize()
        suspend fun rank(candidates: List<MediaTrack>): List<MediaTrack> =
            requireInstance().rank(candidates, emptySet())
        suspend fun rank(candidates: List<MediaTrack>, userSubs: Set<String>): List<MediaTrack> =
            requireInstance().rank(candidates, userSubs)
        suspend fun onTrackInteraction(track: MediaTrack, type: TSukiInteractionType, percentWatched: Float = 0f) =
            requireInstance().onTrackInteraction(track, type, percentWatched)
        suspend fun completeOnboarding(selectedTopics: Set<String>) =
            requireInstance().completeOnboarding(selectedTopics)
        suspend fun bootstrapFromSubscriptions(context: Context, channelNames: List<String>) =
            getInstance(context).bootstrapFromSubscriptions(channelNames)
        suspend fun getBrainSnapshot(): TSukiBrain = requireInstance().getBrainSnapshot()
        fun getPersona(brain: TSukiBrain): TSukiPersona = requireInstance().getPersona(brain)
        suspend fun resetBrain() = requireInstance().resetBrain()
        val TOPIC_CATEGORIES get() = TSukiTopicCatalog.TOPIC_CATEGORIES
    }

    private val textTokenizer = TSukiTokenizer()
    private val brainStore = TSukiBrainStorage(appContext)

    private val stateMutex = Mutex()
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pendingPersist: Job? = null

    private val cachedVectors = object : LinkedHashMap<String, TSukiContentVector>(200, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, TSukiContentVector>?): Boolean =
            size > FEATURE_CACHE_MAX
    }

    private var sessStartAt = System.currentTimeMillis()
    private var sessCounter = 0
    private val sessTopicList = mutableListOf<String>()
    private val momentumLog = mutableListOf<TSukiMomentumEntry>()
    private val impressedInSession = HashSet<String>()

    private var docFreq = mutableMapOf<String, Int>()
    private var docTotal = 0

    private val impressionMap = LinkedHashMap<String, TSukiImpressionEntry>(600, 0.75f, true)
    private val watchMap = LinkedHashMap<String, TSukiWatchEntry>(2_100, 0.75f, true)

    private var activeBrain: TSukiBrain = TSukiBrain()
    private var engineReady = false

    suspend fun initialize() {
        stateMutex.withLock {
            if (engineReady) return
            val loaded = brainStore.load()
            if (loaded != null) {
                activeBrain = loaded
                Log.i(TAG, "TSukiBrain loaded — ${loaded.totalInteractions} interactions")
            } else {
                brainStore.save(activeBrain)
                Log.i(TAG, "TSukiBrain initialized (fresh)")
            }
            docFreq = activeBrain.idfWordFrequency.toMutableMap()
            docTotal = activeBrain.idfTotalDocuments
            for ((id, pct) in activeBrain.watchHistoryMap) {
                watchMap[id] = TSukiWatchEntry(pct, System.currentTimeMillis())
            }
            clearSessionState()
            engineReady = true
        }
    }

    fun shutdown() {
        pendingPersist?.cancel()
        ioScope.cancel()
    }

    suspend fun getBrainSnapshot(): TSukiBrain = stateMutex.withLock { activeBrain }

    suspend fun needsOnboarding(): Boolean = stateMutex.withLock {
        val b = activeBrain
        !b.hasCompletedOnboarding && b.totalInteractions < 5 && b.preferredTopics.isEmpty()
    }

    suspend fun getPreferredTopics(): Set<String> = stateMutex.withLock { activeBrain.preferredTopics }

    suspend fun getBlockedTopics(): Set<String> = stateMutex.withLock { activeBrain.blockedTopics }

    suspend fun getBlockedChannels(): Set<String> = stateMutex.withLock { activeBrain.blockedChannels }

    suspend fun rank(candidates: List<MediaTrack>): List<MediaTrack> = rank(candidates, emptySet())

    suspend fun rank(candidates: List<MediaTrack>, userSubs: Set<String>): List<MediaTrack> =
        withContext(Dispatchers.Default) {
            if (candidates.isEmpty()) return@withContext emptyList()
            val brainSnap: TSukiBrain
            val impSnap: Map<String, TSukiImpressionEntry>
            val watchSnap: Map<String, TSukiWatchEntry>
            val momentumSnap: List<TSukiMomentumEntry>
            val counterSnap: Int
            val topicsSnap: List<String>
            stateMutex.withLock {
                brainSnap = activeBrain
                impSnap = HashMap(impressionMap)
                watchSnap = HashMap(watchMap)
                momentumSnap = ArrayList(momentumLog)
                counterSnap = sessCounter
                topicsSnap = ArrayList(sessTopicList)
            }
            val idfSnap = TSukiIdfSnapshot(HashMap(docFreq), docTotal)
            val bucket = TSukiTimeBucket.current()
            val bucketVec = brainSnap.timeVectors[bucket] ?: TSukiContentVector()
            val nowMs = System.currentTimeMillis()
            val cold = brainSnap.totalInteractions < COLD_START_THRESHOLD
            val onboarding = brainSnap.hasCompletedOnboarding && brainSnap.totalInteractions < ONBOARDING_WARMUP_INTERACTIONS
            val warmup = if (onboarding) {
                val raw = 1.0 - brainSnap.totalInteractions.toDouble() / ONBOARDING_WARMUP_INTERACTIONS.toDouble()
                raw.coerceIn(0.0, 1.0)
            } else 0.0
            val prefLemmas = HashSet<String>()
            for (t in brainSnap.preferredTopics) prefLemmas.add(textTokenizer.normalizeLemma(t))
            val blockedIds = resolveSuppressedVideoSet(brainSnap, nowMs)
            val blockedChans = resolveSuppressedChannelSet(brainSnap, nowMs)
            val eligible = ArrayList<MediaTrack>(candidates.size)
            for (c in candidates) {
                val vid = c.videoId ?: c.id
                val ch = c.channelId ?: ""
                if (vid in brainSnap.blockedTopics) continue
                if (ch in brainSnap.blockedChannels) continue
                if (vid in blockedIds) continue
                if (ch in blockedChans) continue
                eligible.add(c)
            }
            val scored = ArrayList<Pair<MediaTrack, Double>>(eligible.size)
            for (track in eligible) {
                val s = evaluateCandidate(
                    track = track,
                    brain = brainSnap,
                    timeVector = bucketVec,
                    idfSnapshot = idfSnap,
                    isColdStart = cold,
                    isOnboarding = onboarding,
                    onboardingWarmup = warmup,
                    lemmatizedPreferred = prefLemmas,
                    sessionTopics = topicsSnap,
                    sessionVideoCount = counterSnap,
                    impressions = impSnap,
                    watchHistory = watchSnap,
                    recentInteractions = momentumSnap,
                    now = nowMs,
                    userSubs = userSubs
                )
                scored.add(track to s)
            }
            scored.sortByDescending { it.second }
            val ordered = scored.map { it.first }
            applyDiversification(ordered)
        }

    private fun evaluateCandidate(
        track: MediaTrack,
        brain: TSukiBrain,
        timeVector: TSukiContentVector,
        idfSnapshot: TSukiIdfSnapshot,
        isColdStart: Boolean,
        isOnboarding: Boolean,
        onboardingWarmup: Double,
        lemmatizedPreferred: Set<String>,
        sessionTopics: List<String>,
        sessionVideoCount: Int,
        impressions: Map<String, TSukiImpressionEntry>,
        watchHistory: Map<String, TSukiWatchEntry>,
        recentInteractions: List<TSukiMomentumEntry>,
        now: Long,
        userSubs: Set<String> = emptySet()
    ): Double {
        val vid = track.videoId ?: track.id
        val chan = track.channelId ?: ""
        val chanProfile = brain.channelTopicProfiles[chan]
        val vec = cachedVectors.getOrPut(vid) { textTokenizer.extractFeatures(track, idfSnapshot, chanProfile) }
        val gSim = TSukiVectorMath.calculateCosineSimilarity(brain.globalVector, vec)
        val tSim = TSukiVectorMath.calculateCosineSimilarity(timeVector, vec)
        var score = gSim * 0.65 + tSim * 0.35
        val jitterRange = if (isColdStart) JITTER_COLD_START else JITTER_NORMAL
        score += Math.random() * jitterRange
        if (isOnboarding && onboardingWarmup > 0 && lemmatizedPreferred.isNotEmpty()) {
            val combined = textTokenizer.tokenize(track.title) + textTokenizer.tokenize(track.artist)
            var hit = false
            for (tok in combined) {
                if (textTokenizer.normalizeLemma(tok) in lemmatizedPreferred) {
                    hit = true
                    break
                }
            }
            if (hit) score += onboardingWarmup * ONBOARDING_MAX_BOOST
        }
        val chanScore = brain.channelScores[chan] ?: 0.0
        if (chanScore > 0) score += chanScore * SUBSCRIPTION_BOOST
        if (chan in userSubs) score += 0.14
        val titleTokens = textTokenizer.tokenize(track.title).toSet()
        var affBonus = 0.0
        var affSeen = 0
        for (tok in titleTokens) {
            if (affSeen >= 3) break
            val v = brain.topicAffinities[tok]
            if (v != null) {
                affBonus += v * 0.05
                affSeen++
            }
        }
        if (affBonus > 0.15) affBonus = 0.15
        score += affBonus
        if (lemmatizedPreferred.isNotEmpty()) {
            val lemmas = HashSet<String>()
            for (tok in titleTokens) lemmas.add(textTokenizer.normalizeLemma(tok))
            var intersects = false
            for (l in lemmas) if (l in lemmatizedPreferred) { intersects = true; break }
            if (intersects) score += 0.08
        }
        val recency = computeRecencyFactor(track.publishedTimeText ?: "", track.isLive)
        score *= recency
        val wEntry = watchHistory[vid]
        if (wEntry != null) {
            val penalty = if (wEntry.percentWatched >= WATCHED_THRESHOLD_FULL) WATCHED_PENALTY_FULL
            else if (wEntry.percentWatched >= WATCHED_THRESHOLD_HALF) WATCHED_PENALTY_HALF
            else 0.70
            score *= penalty
        }
        val imp = impressions[vid]
        if (imp != null) {
            val impPenalty = if (imp.count >= IMPRESSION_THRESHOLD_HEAVY) IMPRESSION_PENALTY_HEAVY
            else if (imp.count >= IMPRESSION_THRESHOLD_LIGHT) IMPRESSION_PENALTY_MEDIUM
            else 0.85
            score *= impPenalty
        }
        if (recentInteractions.size >= MOMENTUM_THRESHOLD) {
            val window = recentInteractions.takeLast(MOMENTUM_WINDOW)
            val positives = ArrayList<String>()
            for (e in window) if (e.positive) positives.add(e.topic)
            if (positives.size >= MOMENTUM_THRESHOLD) {
                val counts = positives.groupingBy { it }.eachCount()
                val tTokens = textTokenizer.tokenize(track.title)
                var momentumHit = false
                for (tok in tTokens) {
                    if ((counts[tok] ?: 0) >= 2) { momentumHit = true; break }
                }
                if (momentumHit) score += MOMENTUM_BOOST
            }
        }
        val rej = computeRejectionFactor(track, brain, now)
        score *= rej
        val fed = brain.feedHistory[vid]
        if (fed != null) {
            val age = now - fed.lastShown
            val days = age / 86_400_000L
            if (days < FEED_HISTORY_EXPIRY_DAYS) {
                var p = 1.0 - fed.showCount * 0.15
                if (p < 0.05) p = 0.05
                score *= p
            }
        }
        if (score < 0.0) score = 0.0
        return score
    }

    private fun computeRecencyFactor(rawText: String, live: Boolean): Double {
        if (live) return 1.15
        val lower = rawText.lowercase()
        if (lower.contains("second") || lower.contains("minute") || lower.contains("hour")) return 1.15
        if (lower.contains("day")) return 1.10
        if (lower.contains("week")) return 1.05
        if (lower.contains("month")) {
            var digits = StringBuilder()
            for (ch in lower) if (ch.isDigit()) digits.append(ch)
            val m = digits.toString().toIntOrNull() ?: 1
            var v = 1.0 / (1.0 + 0.08 * m)
            if (v < 0.75) v = 0.75
            return v
        }
        if (lower.contains("year")) {
            var digits = StringBuilder()
            for (ch in lower) if (ch.isDigit()) digits.append(ch)
            val y = digits.toString().toIntOrNull() ?: 1
            return 1.0 / (1.0 + 0.35 * y)
        }
        return 1.0
    }

    private fun computeRejectionFactor(track: MediaTrack, brain: TSukiBrain, now: Long): Double {
        val expiry = REJECTION_EXPIRY_DAYS * 24 * 60 * 60 * 1000L
        val toks = textTokenizer.tokenize(track.title).take(5).toSet()
        var mult = 1.0
        for (tok in toks) {
            val sig = brain.rejectionPatterns[tok] ?: continue
            if (now - sig.lastRejectedAt > expiry) continue
            val p = if (sig.count >= 3) REJECTION_PENALTY_3_PLUS
            else if (sig.count == 2) REJECTION_PENALTY_2
            else REJECTION_PENALTY_1
            mult *= p
        }
        return mult
    }

    private fun resolveSuppressedVideoSet(brain: TSukiBrain, now: Long): Set<String> {
        val limit = VIDEO_SUPPRESSION_DAYS * 24 * 60 * 60 * 1000L
        val out = HashSet<String>()
        for ((k, ts) in brain.suppressedVideoIds) if (now - ts < limit) out.add(k)
        return out
    }

    private fun resolveSuppressedChannelSet(brain: TSukiBrain, now: Long): Set<String> {
        val limit = 14L * 24 * 60 * 60 * 1000L
        val out = HashSet<String>()
        for ((k, ts) in brain.suppressedChannels) if (now - ts < limit) out.add(k)
        return out
    }

    private fun applyDiversification(input: List<MediaTrack>): List<MediaTrack> {
        if (input.size <= 4) return input
        val out = mutableListOf<MediaTrack>()
        val remaining = input.toMutableList()
        var lastChan = ""
        var streak = 0
        val ceiling = 2
        while (remaining.isNotEmpty()) {
            var pickIdx = -1
            for (i in remaining.indices) {
                val candChan = remaining[i].channelId ?: remaining[i].artist
                if (candChan == lastChan && streak >= ceiling) continue
                pickIdx = i
                break
            }
            if (pickIdx < 0) pickIdx = 0
            val chosen = remaining.removeAt(pickIdx)
            out.add(chosen)
            val cur = chosen.channelId ?: chosen.artist
            if (cur == lastChan) streak += 1 else { lastChan = cur; streak = 1 }
        }
        return out
    }

    suspend fun onTrackInteraction(
        track: MediaTrack,
        type: TSukiInteractionType,
        percentWatched: Float = 0f
    ) {
        stateMutex.withLock {
            val vid = track.videoId ?: track.id
            val chan = track.channelId ?: ""
            val idfSnap = TSukiIdfSnapshot(HashMap(docFreq), docTotal)
            val vec = cachedVectors.getOrPut(vid) {
                textTokenizer.extractFeatures(track, idfSnap, activeBrain.channelTopicProfiles[chan])
            }
            if (type == TSukiInteractionType.WATCHED || type == TSukiInteractionType.CLICK || type == TSukiInteractionType.LIKED) {
                val lr = if (type == TSukiInteractionType.LIKED) 0.20
                else if (type == TSukiInteractionType.WATCHED) (0.05 + percentWatched * 0.10).coerceAtMost(0.15)
                else 0.05
                val bucket = TSukiTimeBucket.current()
                val timeMap = activeBrain.timeVectors.toMutableMap()
                val curVec = timeMap[bucket] ?: TSukiContentVector()
                timeMap[bucket] = TSukiVectorMath.adjustVector(curVec, vec, lr)
                val updatedGlobal = TSukiVectorMath.adjustVector(activeBrain.globalVector, vec, lr * 0.7)
                val chanScores = activeBrain.channelScores.toMutableMap()
                if (chan.isNotBlank()) {
                    val prev = chanScores[chan] ?: 0.0
                    var nxt = prev * CHANNEL_EMA_DECAY + lr * CHANNEL_EMA_ALPHA
                    if (nxt > 1.0) nxt = 1.0
                    chanScores[chan] = nxt
                    if (chanScores.size > MAX_CHANNEL_SCORES) {
                        val sorted = chanScores.entries.sortedBy { it.value }
                        val drop = chanScores.size - MAX_CHANNEL_SCORES + 50
                        for (j in 0 until drop) {
                            if (j >= sorted.size) break
                            chanScores.remove(sorted[j].key)
                        }
                    }
                }
                val affMap = activeBrain.topicAffinities.toMutableMap()
                val toks = textTokenizer.tokenize(track.title).take(5)
                for (tok in toks) {
                    val prev = affMap[tok] ?: 0.0
                    var nxt = prev + AFFINITY_INCREMENT
                    if (nxt > AFFINITY_MAX) nxt = AFFINITY_MAX
                    affMap[tok] = nxt
                }
                if (affMap.size > AFFINITY_MAX_ENTRIES) {
                    val sortedAff = affMap.entries.sortedBy { it.value }
                    val dropAff = affMap.size - AFFINITY_MAX_ENTRIES + 100
                    for (j in 0 until dropAff) {
                        if (j >= sortedAff.size) break
                        val e = sortedAff[j]
                        if (e.value < AFFINITY_PRUNE_THRESHOLD) affMap.remove(e.key)
                    }
                }
                if (percentWatched > 0f) {
                    watchMap[vid] = TSukiWatchEntry(percentWatched, System.currentTimeMillis())
                }
                refreshIdf(track)
                var idx = 0
                for ((k, _) in vec.topics) {
                    if (idx >= 3) break
                    sessTopicList.add(k)
                    momentumLog.add(TSukiMomentumEntry(k, positive = true))
                    idx++
                }
                if (sessTopicList.size > 50) {
                    val toDrop = sessTopicList.take(20).toSet()
                    sessTopicList.removeAll(toDrop)
                }
                if (momentumLog.size > MOMENTUM_WINDOW * 3) {
                    val toDrop = momentumLog.take(MOMENTUM_WINDOW).toSet()
                    momentumLog.removeAll(toDrop)
                }
                sessCounter++
                activeBrain = activeBrain.copy(
                    globalVector = updatedGlobal,
                    timeVectors = timeMap,
                    channelScores = chanScores,
                    topicAffinities = affMap,
                    totalInteractions = activeBrain.totalInteractions + 1,
                    consecutiveSkips = 0
                )
            } else if (type == TSukiInteractionType.SKIPPED) {
                val updated = TSukiVectorMath.adjustVector(activeBrain.globalVector, vec, -0.05)
                var c = 0
                for ((k, _) in vec.topics) {
                    if (c >= 3) break
                    momentumLog.add(TSukiMomentumEntry(k, positive = false))
                    c++
                }
                activeBrain = activeBrain.copy(
                    globalVector = updated,
                    totalInteractions = activeBrain.totalInteractions + 1,
                    consecutiveSkips = activeBrain.consecutiveSkips + 1
                )
            } else if (type == TSukiInteractionType.DISLIKED || type == TSukiInteractionType.NOT_INTERESTED) {
                val updatedGlobal = TSukiVectorMath.adjustVector(activeBrain.globalVector, vec, -0.25)
                val b = TSukiTimeBucket.current()
                val tMap = activeBrain.timeVectors.toMutableMap()
                val cur = tMap[b] ?: TSukiContentVector()
                tMap[b] = TSukiVectorMath.adjustVector(cur, vec, -0.15)
                val suppressed = activeBrain.suppressedVideoIds.toMutableMap()
                suppressed[vid] = System.currentTimeMillis()
                if (suppressed.size > MAX_SUPPRESSED_VIDEOS) {
                    val sorted = suppressed.entries.sortedBy { it.value }
                    val drop = suppressed.size - MAX_SUPPRESSED_VIDEOS + 50
                    for (j in 0 until drop) {
                        if (j >= sorted.size) break
                        suppressed.remove(sorted[j].key)
                    }
                }
                val toks = textTokenizer.tokenize(track.title).take(3).toSet()
                val rejMap = activeBrain.rejectionPatterns.toMutableMap()
                for (tok in toks) {
                    val prev = rejMap[tok] ?: TSukiRejectionSignal(0, 0L)
                    rejMap[tok] = TSukiRejectionSignal(prev.count + 1, System.currentTimeMillis())
                }
                activeBrain = activeBrain.copy(
                    globalVector = updatedGlobal,
                    timeVectors = tMap,
                    suppressedVideoIds = suppressed,
                    rejectionPatterns = rejMap,
                    totalInteractions = activeBrain.totalInteractions + 1,
                    consecutiveSkips = 0
                )
            }
            triggerDebouncedPersist()
        }
    }

    suspend fun recordFeedImpression(videoId: String) {
        stateMutex.withLock {
            if (videoId in impressedInSession) return
            impressedInSession.add(videoId)
            val cur = impressionMap[videoId] ?: TSukiImpressionEntry(0, 0L)
            impressionMap[videoId] = cur.copy(count = cur.count + 1, lastSeen = System.currentTimeMillis())
            val hist = activeBrain.feedHistory.toMutableMap()
            val prev = hist[videoId] ?: TSukiFeedEntry(0L, 0)
            hist[videoId] = TSukiFeedEntry(System.currentTimeMillis(), prev.showCount + 1)
            if (hist.size > FEED_HISTORY_MAX) {
                val cutoff = System.currentTimeMillis() - FEED_HISTORY_EXPIRY_DAYS * 86_400_000L
                val expired = ArrayList<Map.Entry<String, TSukiFeedEntry>>()
                for (e in hist.entries) if (e.value.lastShown < cutoff) expired.add(e)
                expired.sortBy { it.value.lastShown }
                val toRemove = hist.size - FEED_HISTORY_MAX + 200
                var removed = 0
                for (e in expired) {
                    if (removed >= toRemove) break
                    hist.remove(e.key)
                    removed++
                }
                if (hist.size > FEED_HISTORY_MAX) {
                    val sorted = hist.entries.sortedBy { it.value.lastShown }
                    for (j in 0 until hist.size - FEED_HISTORY_MAX) {
                        if (j >= sorted.size) break
                        hist.remove(sorted[j].key)
                    }
                }
            }
            activeBrain = activeBrain.copy(feedHistory = hist)
            triggerDebouncedPersist()
        }
    }

    suspend fun completeOnboarding(selectedTopics: Set<String>) {
        stateMutex.withLock {
            val seed = TSukiTopicCatalog.buildInitialTopicVector(selectedTopics)
            val merged = activeBrain.globalVector.topics.toMutableMap()
            for ((k, v) in seed) merged[k] = v
            val updatedVec = activeBrain.globalVector.copy(topics = merged)
            activeBrain = activeBrain.copy(
                preferredTopics = selectedTopics,
                globalVector = updatedVec,
                hasCompletedOnboarding = true
            )
            brainStore.save(activeBrain)
            Log.i(TAG, "TSukiBrain onboarding complete — ${selectedTopics.size} topics seeded")
        }
    }

    suspend fun bootstrapFromSubscriptions(channelNames: List<String>) {
        stateMutex.withLock {
            if (activeBrain.totalInteractions > 5 && activeBrain.preferredTopics.isNotEmpty()) return
            val allTokens = ArrayList<String>()
            for (name in channelNames) {
                val toks = textTokenizer.tokenize(name)
                for (t in toks) {
                    val lemma = textTokenizer.normalizeLemma(t)
                    if (lemma.length > 2) allTokens.add(lemma)
                }
            }
            val counts = allTokens.groupingBy { it }.eachCount()
            val sorted = counts.entries.sortedByDescending { it.value }.take(15)
            if (sorted.isEmpty()) return
            val topicMap = activeBrain.globalVector.topics.toMutableMap()
            val affMap = activeBrain.topicAffinities.toMutableMap()
            for ((tok, cnt) in sorted) {
                var w = 0.22 * (cnt / 3.0)
                if (w > 0.55) w = 0.55
                val curT = topicMap[tok] ?: 0.0
                if (w > curT) topicMap[tok] = w
                val curA = affMap[tok] ?: 0.0
                if (0.14 > curA) affMap[tok] = 0.14
            }
            val firstTen = sorted.take(10)
            for (i in firstTen.indices) {
                val a = firstTen[i]
                val rest = sorted.drop(i + 1).take(3)
                for (b in rest) {
                    val key = "${a.key}::${b.key}"
                    val cur = affMap[key] ?: 0.0
                    if (0.12 > cur) affMap[key] = 0.12
                }
            }
            val newGlobal = activeBrain.globalVector.copy(topics = topicMap)
            val newPrefs = HashSet<String>()
            newPrefs.addAll(activeBrain.preferredTopics)
            for (e in sorted.take(10)) newPrefs.add(e.key)
            val trimmedPrefs = newPrefs.take(10).toSet()
            activeBrain = activeBrain.copy(
                globalVector = newGlobal,
                topicAffinities = affMap,
                preferredTopics = trimmedPrefs,
                hasCompletedOnboarding = true
            )
            brainStore.save(activeBrain)
        }
    }

    suspend fun addPreferredTopic(topic: String) {
        val clean = topic.trim().takeIf { it.isNotBlank() } ?: return
        stateMutex.withLock {
            val lemma = textTokenizer.normalizeLemma(clean)
            val topics = activeBrain.globalVector.topics.toMutableMap()
            topics[lemma] = 0.5
            activeBrain = activeBrain.copy(
                preferredTopics = activeBrain.preferredTopics + clean,
                globalVector = activeBrain.globalVector.copy(topics = topics)
            )
            triggerDebouncedPersist()
        }
    }

    suspend fun removePreferredTopic(topic: String) {
        stateMutex.withLock {
            val t = topic.trim()
            activeBrain = activeBrain.copy(preferredTopics = activeBrain.preferredTopics - t)
            triggerDebouncedPersist()
        }
    }

    suspend fun blockChannel(channelId: String) {
        if (channelId.isBlank()) return
        stateMutex.withLock {
            val scores = activeBrain.channelScores.toMutableMap()
            scores.remove(channelId)
            activeBrain = activeBrain.copy(
                blockedChannels = activeBrain.blockedChannels + channelId,
                channelScores = scores
            )
            brainStore.save(activeBrain)
        }
    }

    suspend fun unblockChannel(channelId: String) {
        stateMutex.withLock {
            activeBrain = activeBrain.copy(blockedChannels = activeBrain.blockedChannels - channelId)
            brainStore.save(activeBrain)
        }
    }

    suspend fun resetBrain() {
        stateMutex.withLock {
            activeBrain = TSukiBrain()
            cachedVectors.clear()
            docFreq.clear()
            docTotal = 0
            impressionMap.clear()
            watchMap.clear()
            clearSessionState()
            brainStore.save(activeBrain)
            Log.i(TAG, "TSukiBrain reset")
        }
    }

    suspend fun exportBrain(output: OutputStream): Boolean {
        val snap = getBrainSnapshot()
        return brainStore.exportToStream(snap, output)
    }

    suspend fun importBrain(input: InputStream): Boolean {
        val imported = brainStore.importFromStream(input) ?: return false
        stateMutex.withLock {
            activeBrain = imported
            docFreq = imported.idfWordFrequency.toMutableMap()
            docTotal = imported.idfTotalDocuments
            cachedVectors.clear()
            watchMap.clear()
            for ((id, pct) in imported.watchHistoryMap) {
                watchMap[id] = TSukiWatchEntry(pct, System.currentTimeMillis())
            }
            brainStore.save(activeBrain)
        }
        return true
    }

    fun getPersona(brain: TSukiBrain): TSukiPersona {
        if (brain.totalInteractions < COLD_START_THRESHOLD) return TSukiPersona.INITIATE
        val g = brain.globalVector
        val pacing = g.pacing
        val complexity = g.complexity
        val topList = g.topics.entries.sortedByDescending { it.value }.take(3).map { it.key }
        var isAudiophile = false
        for (t in topList) {
            val low = t.lowercase()
            if (low.contains("music") || low.contains("song") || low.contains("lofi")) { isAudiophile = true; break }
        }
        if (isAudiophile && pacing < 0.5) return TSukiPersona.AUDIOPHILE
        if (g.isLive > 0.5) return TSukiPersona.NIGHT_OWL
        if (pacing > 0.7 && brain.totalInteractions > 100) return TSukiPersona.SKIMMER
        if (complexity > 0.6 && pacing < 0.4) return TSukiPersona.SCHOLAR
        if (topList.size == 1 && brain.totalInteractions > 80) return TSukiPersona.SPECIALIST
        if (brain.totalInteractions > 200 && complexity < 0.4) return TSukiPersona.BINGER
        if (brain.totalInteractions > 150 && complexity > 0.5) return TSukiPersona.DEEP_DIVER
        if (brain.topicAffinities.size > 15) return TSukiPersona.EXPLORER
        return TSukiPersona.INITIATE
    }

    private fun clearSessionState() {
        sessStartAt = System.currentTimeMillis()
        sessCounter = 0
        sessTopicList.clear()
        impressionMap.clear()
        momentumLog.clear()
        impressedInSession.clear()
    }

    private fun triggerDebouncedPersist() {
        pendingPersist?.cancel()
        pendingPersist = ioScope.launch {
            delay(SAVE_DEBOUNCE_MS)
            stateMutex.withLock { brainStore.save(activeBrain) }
        }
    }

    private fun refreshIdf(track: MediaTrack) {
        val combined = textTokenizer.tokenize(track.title) + textTokenizer.tokenize(track.artist)
        val uniq = combined.toSet()
        for (tok in uniq) {
            val cur = docFreq[tok] ?: 0
            docFreq[tok] = cur + 1
        }
        docTotal++
    }
}
