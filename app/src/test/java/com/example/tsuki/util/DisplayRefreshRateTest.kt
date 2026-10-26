package com.example.tsuki.util

import com.example.tsuki.data.local.RefreshRateMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplayRefreshRateTest {

    @Test
    fun testRefreshRateModeEnumValues() {
        val modes = RefreshRateMode.entries
        assertEquals(3, modes.size)
        assertTrue(modes.contains(RefreshRateMode.MAX))
        assertTrue(modes.contains(RefreshRateMode.SYSTEM))
        assertTrue(modes.contains(RefreshRateMode.SAVER_60))
    }

    @Test
    fun testRefreshRateModeValueOf() {
        assertEquals(RefreshRateMode.MAX, RefreshRateMode.valueOf("MAX"))
        assertEquals(RefreshRateMode.SYSTEM, RefreshRateMode.valueOf("SYSTEM"))
        assertEquals(RefreshRateMode.SAVER_60, RefreshRateMode.valueOf("SAVER_60"))
    }

    @Test
    fun testDisplayRefreshRateManagerSingleton() {
        assertNotNull(DisplayRefreshRateManager)
    }

    @Test
    fun testSelectBestModeInfoFor120HzPanel() {
        val modes = listOf(
            DisplayModeInfo(modeId = 1, width = 1080, height = 2400, refreshRate = 60.0f),
            DisplayModeInfo(modeId = 2, width = 1080, height = 2400, refreshRate = 90.0f),
            DisplayModeInfo(modeId = 3, width = 1080, height = 2400, refreshRate = 120.0f)
        )
        val current = modes[0]
        val selected = DisplayRefreshRateManager.selectBestModeInfo(modes, current, RefreshRateMode.MAX)
        assertNotNull(selected)
        assertEquals(3, selected?.modeId)
        assertEquals(120.0f, selected?.refreshRate ?: 0f, 0.01f)
        assertEquals(1080, selected?.width)
        assertEquals(2400, selected?.height)
    }

    @Test
    fun testSelectBestModeInfoFor144HzPanel() {
        val modes = listOf(
            DisplayModeInfo(modeId = 1, width = 1220, height = 2712, refreshRate = 60.0f),
            DisplayModeInfo(modeId = 2, width = 1220, height = 2712, refreshRate = 120.0f),
            DisplayModeInfo(modeId = 3, width = 1220, height = 2712, refreshRate = 144.0f)
        )
        val current = modes[0]
        val selected = DisplayRefreshRateManager.selectBestModeInfo(modes, current, RefreshRateMode.MAX)
        assertNotNull(selected)
        assertEquals(3, selected?.modeId)
        assertEquals(144.0f, selected?.refreshRate ?: 0f, 0.01f)
    }

    @Test
    fun testSelectBestModeInfoFor165HzPanel() {
        val modes = listOf(
            DisplayModeInfo(modeId = 1, width = 1080, height = 2460, refreshRate = 60.0f),
            DisplayModeInfo(modeId = 2, width = 1080, height = 2460, refreshRate = 165.0f)
        )
        val current = modes[0]
        val selected = DisplayRefreshRateManager.selectBestModeInfo(modes, current, RefreshRateMode.MAX)
        assertNotNull(selected)
        assertEquals(2, selected?.modeId)
        assertEquals(165.0f, selected?.refreshRate ?: 0f, 0.01f)
    }

    @Test
    fun testSelectBestModeInfoSamsungQhd60HzVsFhd120Hz() {
        val modes = listOf(
            DisplayModeInfo(modeId = 1, width = 1440, height = 3200, refreshRate = 60.0f),
            DisplayModeInfo(modeId = 2, width = 1080, height = 2400, refreshRate = 60.0f),
            DisplayModeInfo(modeId = 3, width = 1080, height = 2400, refreshRate = 120.0f)
        )
        val current = modes[0]
        val selected = DisplayRefreshRateManager.selectBestModeInfo(modes, current, RefreshRateMode.MAX)
        assertNotNull(selected)
        assertEquals(3, selected?.modeId)
        assertEquals(120.0f, selected?.refreshRate ?: 0f, 0.01f)
        assertEquals(1080, selected?.width)
    }

    @Test
    fun testSelectBestModeInfoAvoidsDownscalingOnEqualRefreshRate() {
        val modes = listOf(
            DisplayModeInfo(modeId = 1, width = 720, height = 1600, refreshRate = 120.0f),
            DisplayModeInfo(modeId = 2, width = 1080, height = 2400, refreshRate = 120.0f)
        )
        val selectedNoCurrent = DisplayRefreshRateManager.selectBestModeInfo(modes, null, RefreshRateMode.MAX)
        assertNotNull(selectedNoCurrent)
        assertEquals(2, selectedNoCurrent?.modeId)
        assertEquals(1080, selectedNoCurrent?.width)

        val current1080 = modes[1]
        val selectedWithCurrent = DisplayRefreshRateManager.selectBestModeInfo(modes, current1080, RefreshRateMode.MAX)
        assertNotNull(selectedWithCurrent)
        assertEquals(2, selectedWithCurrent?.modeId)
        assertEquals(1080, selectedWithCurrent?.width)
    }

    @Test
    fun testSelectBestModeInfoSaver60Mode() {
        val modes = listOf(
            DisplayModeInfo(modeId = 1, width = 1080, height = 2400, refreshRate = 120.0f),
            DisplayModeInfo(modeId = 2, width = 1080, height = 2400, refreshRate = 60.0f),
            DisplayModeInfo(modeId = 3, width = 1080, height = 2400, refreshRate = 90.0f)
        )
        val current = modes[0]
        val selected = DisplayRefreshRateManager.selectBestModeInfo(modes, current, RefreshRateMode.SAVER_60)
        assertNotNull(selected)
        assertEquals(2, selected?.modeId)
        assertEquals(60.0f, selected?.refreshRate ?: 0f, 0.01f)
    }

    @Test
    fun testSelectBestModeInfoSystemReturnsNull() {
        val modes = listOf(
            DisplayModeInfo(modeId = 1, width = 1080, height = 2400, refreshRate = 120.0f)
        )
        val selected = DisplayRefreshRateManager.selectBestModeInfo(modes, modes[0], RefreshRateMode.SYSTEM)
        assertNull(selected)
    }

    @Test
    fun testSelectBestModeInfoEmptyModesReturnsNull() {
        val selected = DisplayRefreshRateManager.selectBestModeInfo(emptyList(), null, RefreshRateMode.MAX)
        assertNull(selected)
    }
}
