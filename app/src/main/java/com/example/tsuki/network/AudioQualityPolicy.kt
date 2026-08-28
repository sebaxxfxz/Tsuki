package com.example.tsuki.network

import com.example.tsuki.data.local.PlayerPreferences

object AudioQualityPolicy {

    @Volatile
    var effectiveQuality: String = PlayerPreferences.AUDIO_QUALITY_HIGH

    const val MEDIUM_TARGET_BITRATE = 128_000

    fun computeEffective(userQuality: String, dataSaver: Boolean, metered: Boolean): String =
        when {
            dataSaver && metered -> PlayerPreferences.AUDIO_QUALITY_LOW
            userQuality == PlayerPreferences.AUDIO_QUALITY_AUTO ->
                if (metered) PlayerPreferences.AUDIO_QUALITY_MEDIUM else PlayerPreferences.AUDIO_QUALITY_HIGH
            else -> userQuality
        }

    fun <T> selectStream(streams: List<T>, bitrateOf: (T) -> Int): T? {
        if (streams.isEmpty()) return null
        return when (effectiveQuality) {
            PlayerPreferences.AUDIO_QUALITY_LOW,
            PlayerPreferences.AUDIO_QUALITY_MEDIUM -> {
                val target = MEDIUM_TARGET_BITRATE
                streams.filter { bitrateOf(it) <= target }
                    .minByOrNull { kotlin.math.abs(bitrateOf(it) - target) }
                    ?: streams.minByOrNull { bitrateOf(it) }
            }
            else -> streams.first()
        }
    }
}
