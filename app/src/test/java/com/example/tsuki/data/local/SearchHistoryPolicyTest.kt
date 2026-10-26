package com.example.tsuki.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SearchHistoryPolicyTest {

    @Test
    fun mergeIntoEmptyAddsQuery() {
        assertEquals(listOf("daft punk"), SearchHistoryPolicy.merge(emptyList(), "daft punk"))
    }

    @Test
    fun mergeTrimsAndRejectsBlank() {
        assertEquals(listOf("daft punk"), SearchHistoryPolicy.merge(emptyList(), "  daft punk  "))
        assertEquals(listOf("a"), SearchHistoryPolicy.merge(listOf("a"), "   "))
        assertEquals(listOf("a"), SearchHistoryPolicy.merge(listOf("a"), ""))
    }

    @Test
    fun mergeMovesDuplicateToFrontCaseInsensitive() {
        assertEquals(
            listOf("Daft Punk", "kavinsky", "m83"),
            SearchHistoryPolicy.merge(listOf("kavinsky", "daft punk", "m83"), "Daft Punk")
        )
    }

    @Test
    fun mergeKeepsMostRecentTen() {
        val current = (1..10).map { "q$it" }
        val result = SearchHistoryPolicy.merge(current, "new")
        assertEquals(10, result.size)
        assertEquals("new", result.first())
        assertEquals("q9", result.last())
        assertFalse(result.contains("q10"))
    }

    @Test
    fun mergeStaysAtLimitWhenDuplicateAdded() {
        val current = (1..10).map { "q$it" }
        val result = SearchHistoryPolicy.merge(current, "q5")
        assertEquals(10, result.size)
        assertEquals("q5", result.first())
    }
}
