package com.example.tsuki.playback

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.util.Log

data class EqCapabilities(
    val bandCount: Int = 0,
    val minLevelMb: Int = -1500,
    val maxLevelMb: Int = 1500,
    val centerFreqMiliHz: List<Int> = emptyList(),
    val hasBassBoost: Boolean = false,
    val hasVirtualizer: Boolean = false,
    val systemPresetNames: List<String> = emptyList()
)

object AudioEqualizerHelper {

    private const val TAG = "AudioEq"

    @Volatile private var equalizer: Equalizer? = null
    @Volatile private var bassBoost: BassBoost? = null
    @Volatile private var virtualizer: Virtualizer? = null
    @Volatile private var loudness: LoudnessEnhancer? = null
    @Volatile private var currentSessionId: Int = 0
    @Volatile private var lastOutputGainMb: Int = 0

    val capabilities: EqCapabilities
        get() {
            val eq = equalizer ?: return EqCapabilities()
            return try {
                val range = eq.bandLevelRange
                EqCapabilities(
                    bandCount = eq.numberOfBands.toInt(),
                    minLevelMb = range?.get(0)?.toInt() ?: -1500,
                    maxLevelMb = range?.get(1)?.toInt() ?: 1500,
                    centerFreqMiliHz = (0 until eq.numberOfBands).map { eq.getCenterFreq(it.toShort()) },
                    hasBassBoost = bassBoost != null,
                    hasVirtualizer = virtualizer != null,
                    systemPresetNames = try {
                        (0 until eq.numberOfPresets).map { eq.getPresetName(it.toShort()) }
                    } catch (_: Exception) { emptyList() }
                )
            } catch (e: Exception) {
                Log.w(TAG, "capabilities error: ${e.message}")
                EqCapabilities()
            }
        }

    fun initAudioEffects(audioSessionId: Int) {
        if (audioSessionId == 0) return
        if (currentSessionId == audioSessionId && equalizer != null) return
        release()
        try {
            currentSessionId = audioSessionId
            equalizer = Equalizer(0, audioSessionId)
            bassBoost = try { BassBoost(0, audioSessionId) } catch (_: Exception) { null }
            virtualizer = try { Virtualizer(0, audioSessionId) } catch (_: Exception) { null }
            loudness = try { LoudnessEnhancer(audioSessionId) } catch (_: Exception) { null }
            Log.d(TAG, "Audio effects attached to session $audioSessionId bands=${equalizer?.numberOfBands}")
        } catch (e: Exception) {
            Log.e(TAG, "initAudioEffects failed", e)
        }
    }

    fun setEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
            loudness?.let {
                if (enabled) {
                    it.setTargetGain(lastOutputGainMb.coerceIn(-1500, 1500))
                    it.enabled = true
                } else {
                    it.setTargetGain(0)
                    it.enabled = false
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "setEnabled failed: ${e.message}")
        }
    }

    fun setBandLevel(band: Int, levelMb: Int) {
        try {
            equalizer?.setBandLevel(band.toShort(), levelMb.toShort())
        } catch (e: Exception) {
            Log.w(TAG, "setBandLevel failed: ${e.message}")
        }
    }

    fun setBassBoostStrength(strength: Int) {
        try {
            bassBoost?.setStrength(strength.coerceIn(0, 1000).toShort())
        } catch (e: Exception) {
            Log.w(TAG, "setBassBoost failed: ${e.message}")
        }
    }

    fun setVirtualizerStrength(strength: Int) {
        try {
            virtualizer?.setStrength(strength.coerceIn(0, 1000).toShort())
        } catch (e: Exception) {
            Log.w(TAG, "setVirtualizer failed: ${e.message}")
        }
    }

    fun setOutputGainMb(gainMb: Int) {
        try {
            loudness?.setTargetGain(gainMb.coerceIn(-1500, 1500))
        } catch (e: Exception) {
            Log.w(TAG, "setOutputGain failed: ${e.message}")
        }
    }

    fun setOutputGainDb(gainDb: Float) {
        try {
            loudness?.setTargetGain((gainDb * 100).toInt())
        } catch (e: Exception) {
            Log.w(TAG, "setOutputGain failed: ${e.message}")
        }
    }

    fun useSystemPreset(presetIndex: Int) {
        try {
            equalizer?.usePreset(presetIndex.toShort())
        } catch (e: Exception) {
            Log.w(TAG, "useSystemPreset failed: ${e.message}")
        }
    }

    fun getCurrentPreset(): Int = try {
        equalizer?.currentPreset?.toInt() ?: -1
    } catch (_: Exception) { -1 }

    fun release() {
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        try { loudness?.release() } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudness = null
        currentSessionId = 0
    }
}
