package com.example.tsuki.data.recommendation

import java.time.DayOfWeek
import java.time.LocalDateTime

data class TSukiContentVector(val topics: Map<String, Double> = mapOf(), val duration: Double = 0.5, val pacing: Double = 0.5, val complexity: Double = 0.5, val isLive: Double = 0.0)

enum class TSukiTimeBucket {
    WEEKDAY_AFTERNOON, WEEKDAY_EVENING, WEEKDAY_MORNING, WEEKDAY_NIGHT, WEEKEND_AFTERNOON, WEEKEND_EVENING, WEEKEND_MORNING, WEEKEND_NIGHT;
    companion object {
        fun current(): TSukiTimeBucket {
            val now = LocalDateTime.now()
            val h = now.hour
            val d = now.dayOfWeek
            val weekend = d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY
            return when {
                weekend && h in 6..11 -> WEEKEND_MORNING
                weekend && h in 12..17 -> WEEKEND_AFTERNOON
                weekend && h in 18..23 -> WEEKEND_EVENING
                weekend -> WEEKEND_NIGHT
                h in 6..11 -> WEEKDAY_MORNING
                h in 12..17 -> WEEKDAY_AFTERNOON
                h in 18..23 -> WEEKDAY_EVENING
                else -> WEEKDAY_NIGHT
            }
        }
    }
}

data class TSukiBrain(val timeVectors: Map<TSukiTimeBucket, TSukiContentVector> = TSukiTimeBucket.entries.associateWith { TSukiContentVector() }, val globalVector: TSukiContentVector = TSukiContentVector(), val channelScores: Map<String, Double> = mapOf(), val topicAffinities: Map<String, Double> = mapOf(), val totalInteractions: Int = 0, val consecutiveSkips: Int = 0, val blockedTopics: Set<String> = setOf(), val blockedChannels: Set<String> = setOf(), val preferredTopics: Set<String> = setOf(), val hasCompletedOnboarding: Boolean = false, val lastPersona: String? = null, val personaStability: Int = 0, val idfWordFrequency: Map<String, Int> = mapOf(), val idfTotalDocuments: Int = 0, val watchHistoryMap: Map<String, Float> = mapOf(), val channelTopicProfiles: Map<String, Map<String, Double>> = mapOf(), val suppressedVideoIds: Map<String, Long> = mapOf(), val suppressedChannels: Map<String, Long> = mapOf(), val rejectionPatterns: Map<String, TSukiRejectionSignal> = mapOf(), val feedHistory: Map<String, TSukiFeedEntry> = mapOf(), val recentQueryTokens: List<Set<String>> = listOf(), val topicEvidence: Map<String, TSukiTopicEvidence> = mapOf(), val schemaVersion: Int = 1)

enum class TSukiInteractionType { CLICK, DISLIKED, LIKED, NOT_INTERESTED, SKIPPED, WATCHED }

enum class TSukiPersona(val label: String, val icon: String, val description: String) {
    AUDIOPHILE("Audiófilo", "🎧", "Vives para la música. El Feed prioriza calidad sonora."),
    BINGER("Bingeador", "🍿", "Maratones de contenido. El Feed rota para no repetir."),
    DEEP_DIVER("Explorador", "🤿", "Nichos profundos y canales únicos. El Feed descubre."),
    EXPLORER("Descubridor", "🧭", "Variedades amplias. El Feed mezcla lo conocido y lo nuevo."),
    INITIATE("Iniciado", "🌱", "Recién comenzando. El Feed se va calibrando contigo."),
    NIGHT_OWL("Búho Nocturno", "🦉", "Madrugador empedernido. Sesiones largas de noche."),
    SCHOLAR("Estudioso", "🎓", "Documentales, análisis y essays. El Feed es tu clase."),
    SKIMMER("Surfero", "⚡", "Muchos videos cortos. El Feed se adapta a tu ritmo."),
    SPECIALIST("Especialista", "🎯", "Un solo tema, muchos videos. El Feed profundiza.")
}

data class TSukiTopicCategory(val name: String, val icon: String, val topics: List<String>)

enum class TSukiSeedSource { LIKED, PLAYLIST, WATCH_HISTORY }

data class TSukiSeedInput(val id: String, val title: String, val channelId: String, val source: TSukiSeedSource, val engagementWeight: Double, val timestamp: Long, val durationSec: Int, val percentWatched: Double)

data class TSukiRejectionSignal(val count: Int, val lastRejectedAt: Long)

data class TSukiTopicEvidence(val positiveSignals: Int = 0, val negativeSignals: Int = 0, val watchSignals: Int = 0, val positiveScore: Double = 0.0, val videoIds: Set<String> = setOf(), val channelIds: Set<String> = setOf(), val firstSeenAt: Long = 0L, val lastSeenAt: Long = 0L)

data class TSukiFeedEntry(val lastShown: Long, val showCount: Int)

internal data class TSukiIdfSnapshot(val wordFrequency: Map<String, Int>, val totalDocs: Int)

internal data class TSukiImpressionEntry(var count: Int, var lastSeen: Long)

internal data class TSukiWatchEntry(val percentWatched: Float, val timestamp: Long)

internal data class TSukiMomentumEntry(val topic: String, val positive: Boolean)

internal data class TSukiSeedRank(val id: String, val clusterKey: String, val weight: Double)

internal data class TSukiScoringParams(val brain: TSukiBrain, val timeContextVector: TSukiContentVector, val wPersonality: Double, val wNovelty: Double, val isColdStart: Boolean, val isOnboarding: Boolean, val onboardingWarmup: Double, val lemmatizedPreferred: Set<String>, val sessionTopics: List<String>, val sessionVideoCount: Int, val impressions: Map<String, TSukiImpressionEntry>, val watchHistory: Map<String, TSukiWatchEntry>, val recentInteractions: List<TSukiMomentumEntry>, val candidatePoolSize: Int, val now: Long)
