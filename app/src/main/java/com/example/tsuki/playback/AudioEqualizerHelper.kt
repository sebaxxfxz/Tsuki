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
    @Volatile private var isEnabled: Boolean = false
    @Volatile private var bassBoostStrength: Int = 0
    @Volatile private var virtualizerStrength: Int = 0
    private val bandLevelsMap = java.util.concurrent.ConcurrentHashMap<Int, Int>()

    @Volatile private var secondaryEqualizer: Equalizer? = null
    @Volatile private var secondaryBassBoost: BassBoost? = null
    @Volatile private var secondaryVirtualizer: Virtualizer? = null
    @Volatile private var secondaryLoudness: LoudnessEnhancer? = null
    @Volatile private var secondarySessionId: Int = 0

    private val _sessionState = kotlinx.coroutines.flow.MutableStateFlow(0)
    val sessionState: kotlinx.coroutines.flow.StateFlow<Int> = _sessionState

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
        synchronized(this) {
            if (audioSessionId == 0) return
            if (currentSessionId == audioSessionId && equalizer != null) return
            releaseInternal()
            try {
                currentSessionId = audioSessionId
                equalizer = Equalizer(0, audioSessionId)
                bassBoost = try { BassBoost(0, audioSessionId) } catch (_: Exception) { null }
                virtualizer = try { Virtualizer(0, audioSessionId) } catch (_: Exception) { null }
                loudness = try { LoudnessEnhancer(audioSessionId) } catch (_: Exception) { null }
                _sessionState.value = audioSessionId
                Log.d(TAG, "Audio effects attached to session $audioSessionId bands=${equalizer?.numberOfBands}")
            } catch (e: Exception) {
                Log.e(TAG, "initAudioEffects failed", e)
            }
        }
    }

    fun initSecondaryAudioEffects(audioSessionId: Int) {
        synchronized(this) {
            if (audioSessionId == 0 || audioSessionId == currentSessionId) return
            releaseSecondaryInternal()
            try {
                secondarySessionId = audioSessionId
                val eq = Equalizer(0, audioSessionId)
                secondaryEqualizer = eq
                val bb = try { BassBoost(0, audioSessionId) } catch (_: Exception) { null }
                secondaryBassBoost = bb
                val vz = try { Virtualizer(0, audioSessionId) } catch (_: Exception) { null }
                secondaryVirtualizer = vz
                val ld = try { LoudnessEnhancer(audioSessionId) } catch (_: Exception) { null }
                secondaryLoudness = ld

                if (isEnabled) {
                    eq.enabled = true
                    bb?.enabled = true
                    vz?.enabled = true
                    ld?.enabled = true
                }
                bandLevelsMap.forEach { (band, level) ->
                    try { eq.setBandLevel(band.toShort(), level.toShort()) } catch (_: Exception) {}
                }
                if (bassBoostStrength > 0) {
                    try { bb?.setStrength(bassBoostStrength.toShort()) } catch (_: Exception) {}
                }
                if (virtualizerStrength > 0) {
                    try { vz?.setStrength(virtualizerStrength.toShort()) } catch (_: Exception) {}
                }
                if (lastOutputGainMb > 0) {
                    try { ld?.setTargetGain(lastOutputGainMb) } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                Log.e(TAG, "initSecondaryAudioEffects failed", e)
            }
        }
    }

    fun releaseSecondaryAudioEffects() {
        synchronized(this) {
            releaseSecondaryInternal()
        }
    }

    private fun releaseSecondaryInternal() {
        try { secondaryEqualizer?.release() } catch (_: Exception) {}
        try { secondaryBassBoost?.release() } catch (_: Exception) {}
        try { secondaryVirtualizer?.release() } catch (_: Exception) {}
        try { secondaryLoudness?.release() } catch (_: Exception) {}
        secondaryEqualizer = null
        secondaryBassBoost = null
        secondaryVirtualizer = null
        secondaryLoudness = null
        secondarySessionId = 0
    }

    fun setEnabled(enabled: Boolean) {
        synchronized(this) {
            isEnabled = enabled
            try {
                equalizer?.enabled = enabled
                bassBoost?.enabled = enabled
                virtualizer?.enabled = enabled
                secondaryEqualizer?.enabled = enabled
                secondaryBassBoost?.enabled = enabled
                secondaryVirtualizer?.enabled = enabled
                val targetGain = if (enabled) lastOutputGainMb.coerceIn(0, 1500) else 0
                loudness?.let {
                    it.setTargetGain(targetGain)
                    it.enabled = enabled
                }
                secondaryLoudness?.let {
                    it.setTargetGain(targetGain)
                    it.enabled = enabled
                }
            } catch (e: Exception) {
                Log.w(TAG, "setEnabled failed: ${e.message}")
            }
        }
    }

    fun setBandLevel(band: Int, levelMb: Int) {
        synchronized(this) {
            bandLevelsMap[band] = levelMb
            try {
                equalizer?.setBandLevel(band.toShort(), levelMb.toShort())
                secondaryEqualizer?.setBandLevel(band.toShort(), levelMb.toShort())
            } catch (e: Exception) {
                Log.w(TAG, "setBandLevel failed: ${e.message}")
            }
        }
    }

    fun setBassBoostStrength(strength: Int) {
        synchronized(this) {
            val clamped = strength.coerceIn(0, 1000)
            bassBoostStrength = clamped
            try {
                bassBoost?.setStrength(clamped.toShort())
                secondaryBassBoost?.setStrength(clamped.toShort())
            } catch (e: Exception) {
                Log.w(TAG, "setBassBoost failed: ${e.message}")
            }
        }
    }

    fun setVirtualizerStrength(strength: Int) {
        synchronized(this) {
            val clamped = strength.coerceIn(0, 1000)
            virtualizerStrength = clamped
            try {
                virtualizer?.setStrength(clamped.toShort())
                secondaryVirtualizer?.setStrength(clamped.toShort())
            } catch (e: Exception) {
                Log.w(TAG, "setVirtualizer failed: ${e.message}")
            }
        }
    }

    fun setOutputGainMb(gainMb: Int) {
        synchronized(this) {
            val clamped = gainMb.coerceIn(0, 1500)
            lastOutputGainMb = clamped
            try {
                loudness?.enabled = clamped > 0
                loudness?.setTargetGain(clamped)
                secondaryLoudness?.enabled = clamped > 0
                secondaryLoudness?.setTargetGain(clamped)
            } catch (e: Exception) {
                Log.w(TAG, "setOutputGain failed: ${e.message}")
            }
        }
    }

    fun setOutputGainDb(gainDb: Float) {
        setOutputGainMb((gainDb * 100).toInt())
    }

    fun useSystemPreset(presetIndex: Int) {
        synchronized(this) {
            try {
                equalizer?.usePreset(presetIndex.toShort())
                secondaryEqualizer?.usePreset(presetIndex.toShort())
            } catch (e: Exception) {
                Log.w(TAG, "useSystemPreset failed: ${e.message}")
            }
        }
    }

    fun getCurrentPreset(): Int = synchronized(this) {
        try {
            equalizer?.currentPreset?.toInt() ?: -1
        } catch (_: Exception) { -1 }
    }

    private fun releaseInternal() {
        try { equalizer?.release() } catch (_: Exception) {}
        try { bassBoost?.release() } catch (_: Exception) {}
        try { virtualizer?.release() } catch (_: Exception) {}
        try { loudness?.release() } catch (_: Exception) {}
        equalizer = null
        bassBoost = null
        virtualizer = null
        loudness = null
        currentSessionId = 0
        _sessionState.value = 0
    }

    fun release() {
        synchronized(this) {
            releaseInternal()
            releaseSecondaryInternal()
        }
    }
}
