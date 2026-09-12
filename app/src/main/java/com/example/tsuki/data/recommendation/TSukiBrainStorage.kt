package com.example.tsuki.data.recommendation

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.Serializer
import androidx.datastore.dataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream

internal class TSukiBrainStorage(private val appContext: Context) {

    companion object {
        private const val LOG_TAG = "TSukiBrainStorage"
        private const val CURRENT_SCHEMA = 1
        private const val FILE_NAME = "tsuki_brain_v1.json"
    }

    private val decodeJson = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val encodeJson = Json {
        encodeDefaults = true
    }

    @Serializable
    data class SerializableVector(
        val topics: Map<String, Double> = emptyMap(),
        val duration: Double = 0.5,
        val pacing: Double = 0.5,
        val complexity: Double = 0.5,
        val isLive: Double = 0.0
    )

    @Serializable
    data class SerializableFeedEntry(
        val lastShown: Long = 0L,
        val showCount: Int = 0
    )

    @Serializable
    data class SerializableRejectionSignal(
        val count: Int = 0,
        val lastRejectedAt: Long = 0L
    )

    @Serializable
    data class SerializableTopicEvidence(
        val positiveSignals: Int = 0,
        val watchSignals: Int = 0,
        val positiveScore: Double = 0.0,
        val videoIds: Set<String> = emptySet(),
        val channelIds: Set<String> = emptySet(),
        val firstSeenAt: Long = 0L,
        val lastSeenAt: Long = 0L
    )

    @Serializable
    data class SerializableBrain(
        val schemaVersion: Int = CURRENT_SCHEMA,
        val timeVectors: Map<String, SerializableVector> = emptyMap(),
        val global: SerializableVector = SerializableVector(),
        val channelScores: Map<String, Double> = emptyMap(),
        val topicAffinities: Map<String, Double> = emptyMap(),
        val interactions: Int = 0,
        val consecutiveSkips: Int = 0,
        val blockedTopics: Set<String> = emptySet(),
        val blockedChannels: Set<String> = emptySet(),
        val preferredTopics: Set<String> = emptySet(),
        val hasCompletedOnboarding: Boolean = false,
        val lastPersona: String? = null,
        val personaStability: Int = 0,
        val idfWordFrequency: Map<String, Int> = emptyMap(),
        val idfTotalDocuments: Int = 0,
        val watchHistoryMap: Map<String, Float> = emptyMap(),
        val channelTopicProfiles: Map<String, Map<String, Double>> = emptyMap(),
        val suppressedVideoIds: Map<String, Long> = emptyMap(),
        val suppressedChannels: Map<String, Long> = emptyMap(),
        val rejectionPatterns: Map<String, SerializableRejectionSignal> = emptyMap(),
        val feedHistory: Map<String, SerializableFeedEntry> = emptyMap(),
        val recentQueryTokens: List<List<String>> = emptyList(),
        val topicEvidence: Map<String, SerializableTopicEvidence> = emptyMap()
    )

    private object PersistSerializer : Serializer<SerializableBrain> {
        private val readJson = Json { ignoreUnknownKeys = true; isLenient = true }
        private val writeJson = Json { encodeDefaults = true }
        override val defaultValue: SerializableBrain = SerializableBrain()

        override suspend fun readFrom(input: InputStream): SerializableBrain {
            return try {
                val raw = input.bufferedReader().readText()
                if (raw.isBlank()) defaultValue else readJson.decodeFromString(raw)
            } catch (thrown: Exception) {
                Log.e(LOG_TAG, "read brain failed", thrown)
                defaultValue
            }
        }

        override suspend fun writeTo(t: SerializableBrain, output: OutputStream) {
            val encoded = writeJson.encodeToString(t)
            output.write(encoded.toByteArray(Charsets.UTF_8))
        }
    }

    private val Context.persistedStore: DataStore<SerializableBrain> by dataStore(
        fileName = FILE_NAME,
        serializer = PersistSerializer
    )

    suspend fun load(): TSukiBrain? = withContext(Dispatchers.IO) {
        try {
            val snapshot = appContext.persistedStore.data.first()
            if (!snapshot.hasPersistedState()) null
            else {
                Log.i(LOG_TAG, "Loaded TSukiBrain v${snapshot.schemaVersion}, ${snapshot.interactions} interactions")
                snapshot.toDomain()
            }
        } catch (thrown: Exception) {
            Log.e(LOG_TAG, "Failed to load TSukiBrain", thrown)
            null
        }
    }

    suspend fun save(brain: TSukiBrain) = withContext(Dispatchers.IO) {
        try {
            appContext.persistedStore.updateData { brain.toPersisted() }
        } catch (thrown: Exception) {
            Log.e(LOG_TAG, "Failed to save TSukiBrain", thrown)
        }
    }

    suspend fun exportToStream(brain: TSukiBrain, output: OutputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            val payload = brain.toPersisted()
            val bytes = encodeJson.encodeToString(payload).toByteArray(Charsets.UTF_8)
            output.write(bytes)
            output.flush()
            Log.i(LOG_TAG, "TSukiBrain exported")
            true
        } catch (thrown: Exception) {
            Log.e(LOG_TAG, "Export failed", thrown)
            false
        }
    }

    suspend fun importFromStream(input: InputStream): TSukiBrain? = withContext(Dispatchers.IO) {
        try {
            val rawText = input.bufferedReader().readText()
            val decoded: SerializableBrain = decodeJson.decodeFromString(rawText)
            val domain = decoded.toDomain()
            Log.i(LOG_TAG, "TSukiBrain imported (${domain.totalInteractions} interactions)")
            domain
        } catch (thrown: Exception) {
            Log.e(LOG_TAG, "Import failed", thrown)
            null
        }
    }

    private fun TSukiContentVector.toPersisted(): SerializableVector = SerializableVector(
        topics = topics,
        duration = duration,
        pacing = pacing,
        complexity = complexity,
        isLive = isLive
    )

    private fun SerializableVector.toDomainVector(): TSukiContentVector = TSukiContentVector(
        topics = topics,
        duration = duration,
        pacing = pacing,
        complexity = complexity,
        isLive = isLive
    )

    private fun TSukiBrain.toPersisted(): SerializableBrain {
        val mappedVectors = timeVectors.mapKeys { it.key.name }.mapValues { it.value.toPersisted() }
        val mappedRejections = rejectionPatterns.mapValues { (_, sig) ->
            SerializableRejectionSignal(count = sig.count, lastRejectedAt = sig.lastRejectedAt)
        }
        val mappedFeed = feedHistory.mapValues { (_, entry) ->
            SerializableFeedEntry(lastShown = entry.lastShown, showCount = entry.showCount)
        }
        val mappedEvidence = topicEvidence.mapValues { (_, ev) ->
            SerializableTopicEvidence(
                positiveSignals = ev.positiveSignals,
                watchSignals = ev.watchSignals,
                positiveScore = ev.positiveScore,
                videoIds = ev.videoIds,
                channelIds = ev.channelIds,
                firstSeenAt = ev.firstSeenAt,
                lastSeenAt = ev.lastSeenAt
            )
        }
        return SerializableBrain(
            schemaVersion = CURRENT_SCHEMA,
            timeVectors = mappedVectors,
            global = globalVector.toPersisted(),
            channelScores = channelScores,
            topicAffinities = topicAffinities,
            interactions = totalInteractions,
            consecutiveSkips = consecutiveSkips,
            blockedTopics = blockedTopics,
            blockedChannels = blockedChannels,
            preferredTopics = preferredTopics,
            hasCompletedOnboarding = hasCompletedOnboarding,
            lastPersona = lastPersona,
            personaStability = personaStability,
            idfWordFrequency = idfWordFrequency,
            idfTotalDocuments = idfTotalDocuments,
            watchHistoryMap = watchHistoryMap,
            channelTopicProfiles = channelTopicProfiles,
            suppressedVideoIds = suppressedVideoIds,
            suppressedChannels = suppressedChannels,
            rejectionPatterns = mappedRejections,
            feedHistory = mappedFeed,
            recentQueryTokens = recentQueryTokens.map { it.toList() },
            topicEvidence = mappedEvidence
        )
    }

    private fun SerializableBrain.toDomain(): TSukiBrain {
        val restoredVectors = TSukiTimeBucket.entries.associateWith { bucket ->
            timeVectors[bucket.name]?.toDomainVector() ?: TSukiContentVector()
        }
        val restoredRejections = rejectionPatterns.mapValues { (_, persisted) ->
            TSukiRejectionSignal(persisted.count, persisted.lastRejectedAt)
        }
        val restoredFeed = feedHistory.mapValues { (_, persisted) ->
            TSukiFeedEntry(persisted.lastShown, persisted.showCount)
        }
        val restoredEvidence = topicEvidence.mapValues { (_, persisted) ->
            TSukiTopicEvidence(
                positiveSignals = persisted.positiveSignals,
                watchSignals = persisted.watchSignals,
                positiveScore = persisted.positiveScore,
                videoIds = persisted.videoIds,
                channelIds = persisted.channelIds,
                firstSeenAt = persisted.firstSeenAt,
                lastSeenAt = persisted.lastSeenAt
            )
        }
        return TSukiBrain(
            timeVectors = restoredVectors,
            globalVector = global.toDomainVector(),
            channelScores = channelScores,
            topicAffinities = topicAffinities,
            totalInteractions = interactions,
            consecutiveSkips = consecutiveSkips,
            blockedTopics = blockedTopics,
            blockedChannels = blockedChannels,
            preferredTopics = preferredTopics,
            hasCompletedOnboarding = hasCompletedOnboarding,
            lastPersona = lastPersona,
            personaStability = personaStability,
            idfWordFrequency = idfWordFrequency,
            idfTotalDocuments = idfTotalDocuments,
            watchHistoryMap = watchHistoryMap,
            channelTopicProfiles = channelTopicProfiles,
            suppressedVideoIds = suppressedVideoIds,
            suppressedChannels = suppressedChannels,
            rejectionPatterns = restoredRejections,
            feedHistory = restoredFeed,
            recentQueryTokens = recentQueryTokens.map { it.toSet() },
            topicEvidence = restoredEvidence
        )
    }

    private fun SerializableBrain.hasPersistedState(): Boolean {
        if (interactions > 0) return true
        if (hasCompletedOnboarding) return true
        if (preferredTopics.isNotEmpty()) return true
        if (blockedTopics.isNotEmpty()) return true
        if (blockedChannels.isNotEmpty()) return true
        if (channelScores.isNotEmpty()) return true
        if (topicAffinities.isNotEmpty()) return true
        if (idfWordFrequency.isNotEmpty()) return true
        if (watchHistoryMap.isNotEmpty()) return true
        if (channelTopicProfiles.isNotEmpty()) return true
        if (suppressedVideoIds.isNotEmpty()) return true
        if (feedHistory.isNotEmpty()) return true
        if (global.topics.isNotEmpty()) return true
        if (timeVectors.values.any { it.topics.isNotEmpty() }) return true
        return false
    }
}
