package com.example.tsuki.network

import com.example.tsuki.data.local.PlayerPreferences

object AudioQualityPolicy {

    @Volatile
    var effectiveQuality: String = PlayerPreferences.AUDIO_QUALITY_HIGH

    const val LOW_TARGET_BITRATE = 64_000
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
            PlayerPreferences.AUDIO_QUALITY_LOW -> pickClosest(streams, bitrateOf, LOW_TARGET_BITRATE)
            PlayerPreferences.AUDIO_QUALITY_MEDIUM -> pickClosest(streams, bitrateOf, MEDIUM_TARGET_BITRATE)
            else -> streams.first()
        }
    }

    private fun <T> pickClosest(streams: List<T>, bitrateOf: (T) -> Int, target: Int): T? =
        streams.filter { bitrateOf(it) <= target }
            .minByOrNull { kotlin.math.abs(bitrateOf(it) - target) }
            ?: streams.minByOrNull { bitrateOf(it) }
}
