package com.example.tsuki.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.view.Display
import androidx.core.content.ContextCompat
import com.example.tsuki.data.local.RefreshRateMode
import kotlin.math.abs
import kotlin.math.roundToInt

data class DisplayModeInfo(
    val modeId: Int,
    val width: Int,
    val height: Int,
    val refreshRate: Float
)

object DisplayRefreshRateManager {

    private const val TAG = "DisplayRefreshRate"
    private const val PREFS_NAME = "refresh_rate_fix"
    private const val KEY_ORIGINAL_USER = "original_user_refresh_rate"
    private const val KEY_ORIGINAL_MIUI = "original_miui_refresh_rate"
    private const val KEY_ORIGINAL_PEAK = "original_peak_refresh_rate"
    private const val KEY_ORIGINAL_MIN = "original_min_refresh_rate"

    fun getDisplay(activity: Activity): Display? {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display
            } else {
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay
            }
        }.getOrNull() ?: runCatching {
            activity.window?.decorView?.display
        }.getOrNull() ?: runCatching {
            val dm = activity.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
            dm?.getDisplay(Display.DEFAULT_DISPLAY)
        }.getOrNull() ?: runCatching {
            @Suppress("DEPRECATION")
            activity.windowManager.defaultDisplay
        }.getOrNull()
    }

    fun getSupportedModes(activity: Activity): List<Display.Mode> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return emptyList()
        val display = getDisplay(activity) ?: return emptyList()
        return runCatching { display.supportedModes.toList() }.getOrDefault(emptyList())
    }

    fun getSupportedRefreshRates(activity: Activity): List<Int> {
        val modes = getSupportedModes(activity)
        if (modes.isEmpty()) {
            val current = getCurrentRefreshRate(activity).roundToInt()
            return listOf(if (current > 0) current else 60)
        }
        return modes.map { it.refreshRate.roundToInt() }
            .distinct()
            .sortedDescending()
    }

    fun getMaxHardwareRefreshRate(activity: Activity): Float {
        val modes = getSupportedModes(activity)
        if (modes.isEmpty()) return getCurrentRefreshRate(activity)
        val best = findBestMode(activity, RefreshRateMode.MAX)
        return best?.refreshRate ?: modes.maxOfOrNull { it.refreshRate } ?: 60f
    }

    fun getCurrentRefreshRate(activity: Activity): Float {
        val display = getDisplay(activity) ?: return 60f
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                display.mode?.refreshRate ?: display.refreshRate
            } else {
                display.refreshRate
            }
        }.getOrDefault(60f)
    }

    fun isSystemThrottling(activity: Activity): Boolean {
        val max = getMaxHardwareRefreshRate(activity)
        val current = getCurrentRefreshRate(activity)
        return max >= 89f && current < (max - 10f)
    }

    fun selectBestModeInfo(
        modes: List<DisplayModeInfo>,
        currentMode: DisplayModeInfo?,
        targetMode: RefreshRateMode
    ): DisplayModeInfo? {
        if (modes.isEmpty()) return null
        if (targetMode == RefreshRateMode.SYSTEM) return null

        val sameResolutionModes = if (currentMode != null) {
            modes.filter {
                (it.width == currentMode.width && it.height == currentMode.height) ||
                (it.width == currentMode.height && it.height == currentMode.width)
            }
        } else emptyList()

        return when (targetMode) {
            RefreshRateMode.MAX -> {
                val globalMaxRate = modes.maxOfOrNull { it.refreshRate } ?: 60f
                val sameResBest = sameResolutionModes.maxByOrNull { it.refreshRate }
                if (sameResBest != null && sameResBest.refreshRate >= (globalMaxRate - 1.0f)) {
                    sameResBest
                } else {
                    val topRateCandidates = modes.filter { it.refreshRate >= (globalMaxRate - 1.0f) }
                    topRateCandidates.maxWithOrNull(
                        compareBy<DisplayModeInfo> { it.refreshRate }
                            .thenBy { it.width.toLong() * it.height.toLong() }
                    ) ?: modes.maxWithOrNull(
                        compareBy<DisplayModeInfo> { it.refreshRate }
                            .thenBy { it.width.toLong() * it.height.toLong() }
                    )
                }
            }
            RefreshRateMode.SAVER_60 -> {
                val sameResClosest = sameResolutionModes.minByOrNull { abs(it.refreshRate - 60f) }
                if (sameResClosest != null && abs(sameResClosest.refreshRate - 60f) <= 1.0f) {
                    sameResClosest
                } else {
                    val minDiff = modes.minOfOrNull { abs(it.refreshRate - 60f) } ?: 0f
                    val closestCandidates = modes.filter { abs(it.refreshRate - 60f) <= minDiff + 0.5f }
                    closestCandidates.maxWithOrNull(
                        compareBy<DisplayModeInfo> { -abs(it.refreshRate - 60f) }
                            .thenBy { it.width.toLong() * it.height.toLong() }
                    ) ?: modes.minByOrNull { abs(it.refreshRate - 60f) }
                }
            }
            RefreshRateMode.SYSTEM -> null
        }
    }

    fun findBestMode(activity: Activity, mode: RefreshRateMode): Display.Mode? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return null
        val display = getDisplay(activity) ?: return null
        val modes = runCatching { display.supportedModes }.getOrNull() ?: return null
        if (modes.isEmpty()) return null

        val currentMode = runCatching { display.mode }.getOrNull()
        val modeInfoList = modes.map { DisplayModeInfo(it.modeId, it.physicalWidth, it.physicalHeight, it.refreshRate) }
        val currentModeInfo = currentMode?.let { DisplayModeInfo(it.modeId, it.physicalWidth, it.physicalHeight, it.refreshRate) }

        val selectedInfo = selectBestModeInfo(modeInfoList, currentModeInfo, mode) ?: return null
        return modes.firstOrNull { it.modeId == selectedInfo.modeId }
    }

    fun applyRefreshRate(activity: Activity, mode: RefreshRateMode = RefreshRateMode.MAX) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        try {
            val targetMode = findBestMode(activity, mode)
            val window = activity.window ?: return
            val attrs = window.attributes ?: return
            var attrsModified = false

            val targetRate = when (mode) {
                RefreshRateMode.MAX -> targetMode?.refreshRate ?: getMaxHardwareRefreshRate(activity)
                RefreshRateMode.SAVER_60 -> targetMode?.refreshRate ?: 60f
                RefreshRateMode.SYSTEM -> 0f
            }

            when (mode) {
                RefreshRateMode.MAX -> {
                    if (targetMode != null && attrs.preferredDisplayModeId != targetMode.modeId) {
                        attrs.preferredDisplayModeId = targetMode.modeId
                        attrsModified = true
                    }
                    if (targetRate > 0f && abs(attrs.preferredRefreshRate - targetRate) > 0.1f) {
                        attrs.preferredRefreshRate = targetRate
                        attrsModified = true
                    }
                    if (Build.VERSION.SDK_INT >= 35) {
                        runCatching {
                            attrs.setFrameRateBoostOnTouchEnabled(true)
                            attrs.setFrameRatePowerSavingsBalanced(false)
                            attrsModified = true
                            window.setFrameRateBoostOnTouchEnabled(true)
                            window.setFrameRatePowerSavingsBalanced(false)
                        }.onFailure { Log.w(TAG, "frame rate boost flags failed (MAX)", it) }
                    }
                }
                RefreshRateMode.SAVER_60 -> {
                    if (targetMode != null && attrs.preferredDisplayModeId != targetMode.modeId) {
                        attrs.preferredDisplayModeId = targetMode.modeId
                        attrsModified = true
                    }
                    if (targetRate > 0f && abs(attrs.preferredRefreshRate - targetRate) > 0.1f) {
                        attrs.preferredRefreshRate = targetRate
                        attrsModified = true
                    }
                    if (Build.VERSION.SDK_INT >= 35) {
                        runCatching {
                            attrs.setFrameRateBoostOnTouchEnabled(false)
                            attrs.setFrameRatePowerSavingsBalanced(true)
                            attrsModified = true
                            window.setFrameRateBoostOnTouchEnabled(false)
                            window.setFrameRatePowerSavingsBalanced(true)
                        }.onFailure { Log.w(TAG, "frame rate boost flags failed (SAVER_60)", it) }
                    }
                }
                RefreshRateMode.SYSTEM -> {
                    if (attrs.preferredDisplayModeId != 0) {
                        attrs.preferredDisplayModeId = 0
                        attrsModified = true
                    }
                    if (attrs.preferredRefreshRate != 0f) {
                        attrs.preferredRefreshRate = 0f
                        attrsModified = true
                    }
                    if (Build.VERSION.SDK_INT >= 35) {
                        runCatching {
                            attrs.setFrameRateBoostOnTouchEnabled(true)
                            attrs.setFrameRatePowerSavingsBalanced(true)
                            attrsModified = true
                            window.setFrameRateBoostOnTouchEnabled(true)
                            window.setFrameRatePowerSavingsBalanced(true)
                        }.onFailure { Log.w(TAG, "frame rate boost flags failed (SYSTEM)", it) }
                    }
                }
            }

            if (attrsModified) {
                window.attributes = attrs
            }

            if (Build.VERSION.SDK_INT >= 35) {
                runCatching {
                    window.decorView.setRequestedFrameRate(targetRate)
                }.onFailure { Log.w(TAG, "setRequestedFrameRate($targetRate) failed", it) }
            }

            window.decorView.post {
                if (Build.VERSION.SDK_INT >= 35) {
                    runCatching {
                        window.decorView.setRequestedFrameRate(targetRate)
                    }.onFailure { Log.w(TAG, "setRequestedFrameRate($targetRate) failed after attach", it) }
                }
            }

            tryApplySystemRateFix(activity, mode)
            tryApplyXiaomiSecureFix(activity, mode)
        } catch (t: Throwable) {
            Log.w(TAG, "applyRefreshRate(${mode.name}) failed", t)
        }
    }

    private fun tryApplySystemRateFix(activity: Activity, mode: RefreshRateMode) {
        if (!canWriteSystemSettings(activity)) return
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        runCatching {
            when (mode) {
                RefreshRateMode.MAX -> writeSystemRates(activity, prefs, getMaxHardwareRefreshRate(activity))
                RefreshRateMode.SAVER_60 -> writeSystemRates(activity, prefs, 60f)
                RefreshRateMode.SYSTEM -> restoreSystemRates(activity, prefs)
            }
        }.onFailure { Log.w(TAG, "system refresh rate fix failed (${mode.name})", it) }
    }

    fun canWriteSystemSettings(context: Context): Boolean =
        runCatching { Settings.System.canWrite(context) }.getOrDefault(false)

    private fun writeSystemRates(activity: Activity, prefs: SharedPreferences, rate: Float) {
        if (!prefs.contains(KEY_ORIGINAL_PEAK)) {
            prefs.edit()
                .putFloat(KEY_ORIGINAL_PEAK, readSystemFloat(activity, "peak_refresh_rate"))
                .putFloat(KEY_ORIGINAL_MIN, readSystemFloat(activity, "min_refresh_rate"))
                .apply()
        }
        if (abs(readSystemFloat(activity, "peak_refresh_rate") - rate) > 0.1f) {
            Settings.System.putFloat(activity.contentResolver, "peak_refresh_rate", rate)
        }
        if (abs(readSystemFloat(activity, "min_refresh_rate") - rate) > 0.1f) {
            Settings.System.putFloat(activity.contentResolver, "min_refresh_rate", rate)
        }
    }

    private fun restoreSystemRates(activity: Activity, prefs: SharedPreferences) {
        if (!prefs.contains(KEY_ORIGINAL_PEAK)) return
        val peak = prefs.getFloat(KEY_ORIGINAL_PEAK, 0f)
        val min = prefs.getFloat(KEY_ORIGINAL_MIN, 0f)
        if (peak > 0f && abs(readSystemFloat(activity, "peak_refresh_rate") - peak) > 0.1f) {
            Settings.System.putFloat(activity.contentResolver, "peak_refresh_rate", peak)
        }
        if (min > 0f && abs(readSystemFloat(activity, "min_refresh_rate") - min) > 0.1f) {
            Settings.System.putFloat(activity.contentResolver, "min_refresh_rate", min)
        }
        prefs.edit().remove(KEY_ORIGINAL_PEAK).remove(KEY_ORIGINAL_MIN).apply()
    }

    private fun readSystemFloat(activity: Activity, key: String): Float =
        runCatching { Settings.System.getFloat(activity.contentResolver, key) }.getOrDefault(0f)

    private fun tryApplyXiaomiSecureFix(activity: Activity, mode: RefreshRateMode) {
        if (!isXiaomiFamily() || !hasWriteSecureSettings(activity)) return
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        runCatching {
            when (mode) {
                RefreshRateMode.MAX -> writeMiuiRates(activity, prefs, getMaxHardwareRefreshRate(activity).roundToInt())
                RefreshRateMode.SAVER_60 -> writeMiuiRates(activity, prefs, 60)
                RefreshRateMode.SYSTEM -> restoreMiuiRates(activity, prefs)
            }
        }.onFailure { Log.w(TAG, "MIUI refresh rate fix failed (${mode.name})", it) }
    }

    fun isXiaomiFamily(): Boolean =
        listOf("xiaomi", "redmi", "poco", "blackshark").any {
            Build.MANUFACTURER.contains(it, ignoreCase = true) || Build.BRAND.contains(it, ignoreCase = true)
        }

    fun hasWriteSecureSettings(activity: Activity): Boolean =
        ContextCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    fun isSaverCapBlocked(activity: Activity): Boolean =
        isXiaomiFamily() && readSecureInt(activity, "user_refresh_rate") > 65

    private fun readSecureInt(activity: Activity, key: String): Int =
        runCatching { Settings.Secure.getInt(activity.contentResolver, key) }.getOrDefault(0)

    private fun writeMiuiRates(activity: Activity, prefs: SharedPreferences, rate: Int) {
        if (!prefs.contains(KEY_ORIGINAL_USER)) {
            prefs.edit()
                .putInt(KEY_ORIGINAL_USER, readSecureInt(activity, "user_refresh_rate"))
                .putInt(KEY_ORIGINAL_MIUI, readSecureInt(activity, "miui_refresh_rate"))
                .apply()
        }
        if (readSecureInt(activity, "user_refresh_rate") != rate) {
            Settings.Secure.putInt(activity.contentResolver, "user_refresh_rate", rate)
        }
        if (readSecureInt(activity, "miui_refresh_rate") != rate) {
            Settings.Secure.putInt(activity.contentResolver, "miui_refresh_rate", rate)
        }
    }

    private fun restoreMiuiRates(activity: Activity, prefs: SharedPreferences) {
        if (!prefs.contains(KEY_ORIGINAL_USER)) return
        val user = prefs.getInt(KEY_ORIGINAL_USER, 0)
        val miui = prefs.getInt(KEY_ORIGINAL_MIUI, 0)
        if (user > 0 && readSecureInt(activity, "user_refresh_rate") != user) {
            Settings.Secure.putInt(activity.contentResolver, "user_refresh_rate", user)
        }
        if (miui > 0 && readSecureInt(activity, "miui_refresh_rate") != miui) {
            Settings.Secure.putInt(activity.contentResolver, "miui_refresh_rate", miui)
        }
        prefs.edit().remove(KEY_ORIGINAL_USER).remove(KEY_ORIGINAL_MIUI).apply()
    }

    fun openDisplaySettings(context: Context) {
        runCatching {
            val intent = Intent(Settings.ACTION_DISPLAY_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun openWriteSettings(context: Context) {
        runCatching {
            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}
