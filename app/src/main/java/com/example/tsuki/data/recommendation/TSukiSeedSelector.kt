package com.example.tsuki.data.recommendation

import com.example.tsuki.data.local.WatchHistoryEntry

internal class TSukiSeedSelector {

    companion object {
        private const val MAX_SEEDS = 5
        private const val RECENT_HISTORY_WINDOW = 50
        private const val MIN_WATCH_PERCENT_SEED = 0.20f
    }

    fun selectSeeds(
        history: List<WatchHistoryEntry>,
        brain: TSukiBrain,
        tokenizer: TSukiTokenizer
    ): List<TSukiSeedInput> {
        val windowed = history.take(RECENT_HISTORY_WINDOW)
            .filter { it.watchDurationMs > 0L || it.playCount > 1 }

        if (windowed.isEmpty()) return emptyList()

        val scoredEntries: List<Pair<WatchHistoryEntry, Double>> = windowed.map { item ->
            val normalizedDuration = (item.watchDurationMs.toDouble() / 1_200_000.0).coerceIn(0.0, 1.0)
            val recency = recencyScore(item.lastPlayedTimestamp)
            val channelAffinity = brain.channelScores[item.videoId] ?: 0.0
            val combined = normalizedDuration * 0.5 + recency * 0.3 + channelAffinity * 0.2
            item to combined
        }

        val grouped: Map<String, List<Pair<WatchHistoryEntry, Double>>> =
            scoredEntries.groupBy { (entry, _) ->
                val tokens = tokenizer.tokenize(entry.title)
                tokens.firstOrNull() ?: entry.videoId
            }

        val rankedGroups = grouped.entries
            .sortedByDescending { (_, members) ->
                members.maxOfOrNull { (_, score) -> score } ?: 0.0
            }
            .take(MAX_SEEDS)

        return rankedGroups.mapNotNull { (_, members) ->
            val best = members.maxByOrNull { (_, score) -> score } ?: return@mapNotNull null
            val (entry, weight) = best
            val percent = if (entry.watchDurationMs > 0L) {
                (entry.watchDurationMs.toDouble() / 1_200_000.0).coerceIn(0.0, 1.0)
            } else {
                0.0
            }
            TSukiSeedInput(
                id = entry.videoId,
                title = entry.title,
                channelId = "",
                source = TSukiSeedSource.WATCH_HISTORY,
                engagementWeight = weight,
                timestamp = entry.lastPlayedTimestamp,
                durationSec = (entry.watchDurationMs / 1000L).toInt(),
                percentWatched = percent
            )
        }
    }

    private fun recencyScore(timestamp: Long): Double {
        val elapsedMs = System.currentTimeMillis() - timestamp
        val elapsedDays = elapsedMs / 86_400_000.0
        if (elapsedDays < 1.0) return 1.0
        if (elapsedDays < 7.0) return 1.0 - ((elapsedDays - 1.0) / 6.0) * 0.3
        if (elapsedDays < 30.0) return 0.7 - ((elapsedDays - 7.0) / 23.0) * 0.4
        return 0.3
    }
}
