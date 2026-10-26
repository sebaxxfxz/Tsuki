package com.example.tsuki.network

import com.example.tsuki.data.local.PlayerPreferences
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Test

class AudioQualityPolicyTest {

    private data class FakeStream(val bitrate: Int)

    private val streams = listOf(FakeStream(256_000), FakeStream(128_000), FakeStream(64_000), FakeStream(48_000))

    @After
    fun reset() {
        AudioQualityPolicy.effectiveQuality = PlayerPreferences.AUDIO_QUALITY_HIGH
    }

    @Test
    fun `low picks lowest available under 64k`() {
        AudioQualityPolicy.effectiveQuality = PlayerPreferences.AUDIO_QUALITY_LOW
        assertEquals(64_000, AudioQualityPolicy.selectStream(streams) { it.bitrate }?.bitrate)
    }

    @Test
    fun `low falls back to lowest stream when nothing under target`() {
        AudioQualityPolicy.effectiveQuality = PlayerPreferences.AUDIO_QUALITY_LOW
        val high = listOf(FakeStream(256_000), FakeStream(128_000))
        assertEquals(128_000, AudioQualityPolicy.selectStream(high) { it.bitrate }?.bitrate)
    }

    @Test
    fun `medium picks 128k`() {
        AudioQualityPolicy.effectiveQuality = PlayerPreferences.AUDIO_QUALITY_MEDIUM
        assertEquals(128_000, AudioQualityPolicy.selectStream(streams) { it.bitrate }?.bitrate)
    }

    @Test
    fun `high picks first stream`() {
        AudioQualityPolicy.effectiveQuality = PlayerPreferences.AUDIO_QUALITY_HIGH
        assertEquals(256_000, AudioQualityPolicy.selectStream(streams) { it.bitrate }?.bitrate)
    }

    @Test
    fun `low and medium differ on the same stream list`() {
        AudioQualityPolicy.effectiveQuality = PlayerPreferences.AUDIO_QUALITY_LOW
        val low = AudioQualityPolicy.selectStream(streams) { it.bitrate }
        AudioQualityPolicy.effectiveQuality = PlayerPreferences.AUDIO_QUALITY_MEDIUM
        val medium = AudioQualityPolicy.selectStream(streams) { it.bitrate }
        assertEquals(true, low?.bitrate != medium?.bitrate)
    }

    @Test
    fun `data saver on metered network forces low`() {
        assertEquals(
            PlayerPreferences.AUDIO_QUALITY_LOW,
            AudioQualityPolicy.computeEffective(PlayerPreferences.AUDIO_QUALITY_HIGH, dataSaver = true, metered = true)
        )
    }

    @Test
    fun `auto resolves to medium on metered and high otherwise`() {
        assertEquals(
            PlayerPreferences.AUDIO_QUALITY_MEDIUM,
            AudioQualityPolicy.computeEffective(PlayerPreferences.AUDIO_QUALITY_AUTO, dataSaver = false, metered = true)
        )
        assertEquals(
            PlayerPreferences.AUDIO_QUALITY_HIGH,
            AudioQualityPolicy.computeEffective(PlayerPreferences.AUDIO_QUALITY_AUTO, dataSaver = false, metered = false)
        )
    }

    @Test
    fun `user choice is respected when no override applies`() {
        assertEquals(
            PlayerPreferences.AUDIO_QUALITY_MEDIUM,
            AudioQualityPolicy.computeEffective(PlayerPreferences.AUDIO_QUALITY_MEDIUM, dataSaver = false, metered = true)
        )
    }

    @Test
    fun `empty stream list returns null`() {
        assertEquals(null, AudioQualityPolicy.selectStream(emptyList<FakeStream>()) { it.bitrate })
    }
}
