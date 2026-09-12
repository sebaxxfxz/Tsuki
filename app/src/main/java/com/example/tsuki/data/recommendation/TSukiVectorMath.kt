package com.example.tsuki.data.recommendation

import kotlin.math.abs

internal object TSukiVectorMath {
    const val TOPIC_SIMILARITY_WEIGHT = 0.70
    const val DURATION_SIMILARITY_WEIGHT = 0.10
    const val PACING_SIMILARITY_WEIGHT = 0.10
    const val COMPLEXITY_SIMILARITY_WEIGHT = 0.10
    const val TOPIC_PRUNE_THRESHOLD = 0.03
    const val SCALAR_ONLY_DAMP = 0.3
    const val ESTABLISHED_TOPIC_THRESHOLD = 0.30
    const val DEVELOPING_TOPIC_THRESHOLD = 0.10
    const val ESTABLISHED_DECAY_RATE = 0.998
    const val DEVELOPING_DECAY_RATE = 0.993
    const val EMERGING_DECAY_RATE = 0.97
    const val NEGATIVE_PROPORTIONAL_EXPONENT = 1.5
    const val NEGATIVE_FLOOR_FACTOR = 0.3
    const val NEGATIVE_SCALAR_PROPORTIONAL = 0.3
    const val NEGATIVE_SCALAR_FLOOR = 0.1
    const val COMPRESSION_THRESHOLD = 0.6
    const val COMPRESSION_CEILING = 0.5
    const val COMPRESSION_FACTOR = 0.7

    fun normalizeTopicVector(topics: MutableMap<String, Double>): Map<String, Double> {
        if (topics.isEmpty()) return topics
        var acc = 0.0
        for (v in topics.values) acc += v * v
        acc = Math.sqrt(acc)
        if (acc <= 0.0) return topics
        return topics.mapValues { (_, v) -> v / acc }
    }

    fun calculateTitleSimilarity(tokens1: Set<String>, tokens2: Set<String>): Double {
        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0.0
        val inter = tokens1.intersect(tokens2).size.toDouble()
        val uni = tokens1.union(tokens2).size.toDouble()
        return if (uni == 0.0) 0.0 else inter / uni
    }

    fun calculateCosineSimilarity(user: TSukiContentVector, content: TSukiContentVector): Double {
        val small: Map<String, Double>
        val large: Map<String, Double>
        if (user.topics.size <= content.topics.size) { small = user.topics; large = content.topics } else { small = content.topics; large = user.topics }
        val dSim = 1.0 - abs(user.duration - content.duration)
        val pSim = 1.0 - abs(user.pacing - content.pacing)
        val cSim = 1.0 - abs(user.complexity - content.complexity)
        val scalar = dSim * DURATION_SIMILARITY_WEIGHT + pSim * PACING_SIMILARITY_WEIGHT + cSim * COMPLEXITY_SIMILARITY_WEIGHT
        if (small.isEmpty()) return scalar * SCALAR_ONLY_DAMP
        val taggedByBase = HashMap<String, Pair<String, Double>>(large.size)
        val plain = HashMap<String, Double>(large.size)
        for ((k, v) in large) {
            if (':' in k) { val base = k.substringBefore(':'); if (!taggedByBase.containsKey(base)) taggedByBase[base] = k to v } else plain[k] = v
        }
        var dot = 0.0
        var hit = false
        for ((k, sv) in small) {
            val exact = large[k]
            if (exact != null) { dot += sv * exact; hit = true; continue }
            if (':' !in k) {
                val m = taggedByBase[k]
                if (m != null) { dot += sv * m.second * 0.3; hit = true }
            } else {
                val base = k.substringBefore(':')
                val m = plain[base]
                if (m != null) { dot += sv * m * 0.3; hit = true }
            }
        }
        if (!hit) return scalar * SCALAR_ONLY_DAMP
        var magA = 0.0; for (v in user.topics.values) magA += v * v
        var magB = 0.0; for (v in content.topics.values) magB += v * v
        val topicSim = if (magA > 0 && magB > 0) dot / (Math.sqrt(magA) * Math.sqrt(magB)) else 0.0
        return topicSim * TOPIC_SIMILARITY_WEIGHT + scalar
    }

    fun adjustVector(current: TSukiContentVector, target: TSukiContentVector, baseRate: Double): TSukiContentVector {
        val neg = baseRate < 0
        val next = current.topics.toMutableMap()
        for ((k, tv) in target.topics) {
            val cv = next[k] ?: 0.0
            val delta = if (neg) {
                val prop = cv * Math.pow(cv, NEGATIVE_PROPORTIONAL_EXPONENT) * baseRate
                val floor = baseRate * NEGATIVE_FLOOR_FACTOR
                if (prop < floor) prop else floor
            } else {
                val sat = Math.pow(1.0 - cv, 2.0)
                val cold = 0.5 + 0.5 * Math.min(cv / 0.20, 1.0)
                val eff = baseRate * sat * cold
                (tv - cv) * eff
            }
            next[k] = (cv + delta).coerceIn(0.0, 1.0)
        }
        val it = next.iterator()
        while (it.hasNext()) {
            val e = it.next()
            val isTarget = target.topics.containsKey(e.key)
            if (baseRate > 0 && !isTarget) {
                val rate = when {
                    e.value >= ESTABLISHED_TOPIC_THRESHOLD -> ESTABLISHED_DECAY_RATE
                    e.value >= DEVELOPING_TOPIC_THRESHOLD -> DEVELOPING_DECAY_RATE
                    else -> EMERGING_DECAY_RATE
                }
                e.setValue(e.value * rate)
            }
            if (!isTarget && e.value < TOPIC_PRUNE_THRESHOLD) it.remove()
        }
        if (neg && next.isNotEmpty()) {
            var sum = 0.0; var peak = 0.0
            for (v in next.values) { sum += v; if (v > peak) peak = v }
            if (sum > 0 && peak / sum > COMPRESSION_THRESHOLD) {
                val squashed = next.mapValues { (_, v) -> if (v > COMPRESSION_CEILING) COMPRESSION_CEILING + (v - COMPRESSION_CEILING) * COMPRESSION_FACTOR else v }
                next.clear(); next.putAll(squashed)
            }
        }
        fun scalar(cur: Double, tgt: Double): Double {
            val r = if (neg) {
                val prop = cur * baseRate * NEGATIVE_SCALAR_PROPORTIONAL
                val fl = baseRate * NEGATIVE_SCALAR_FLOOR
                cur + if (prop < fl) prop else fl
            } else {
                val sat = Math.pow(1.0 - cur, 2.0)
                cur + (tgt - cur) * baseRate * sat
            }
            return r.coerceIn(0.0, 1.0)
        }
        return current.copy(topics = next, duration = scalar(current.duration, target.duration), pacing = scalar(current.pacing, target.pacing), complexity = scalar(current.complexity, target.complexity), isLive = scalar(current.isLive, target.isLive))
    }
}
