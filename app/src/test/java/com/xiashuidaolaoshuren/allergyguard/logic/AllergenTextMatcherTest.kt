package com.xiashuidaolaoshuren.allergyguard.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class AllergenTextMatcherTest {
    @Test
    fun findsMatchWhenTextHasMixedCaseAndWhitespace() {
        val matches = AllergenTextMatcher.findMatches(
            recognizedText = "Ingredients: PeAnu t oil and salt",
            enabledAllergenNames = listOf("Peanut", "Gluten")
        )

        assertEquals(listOf("Peanut"), matches)
    }

    @Test
    fun findsFuzzyMatchForMinorOcrTypo() {
        val matches = AllergenTextMatcher.findMatches(
            recognizedText = "Contains glutn",
            enabledAllergenNames = listOf("Gluten")
        )

        assertEquals(listOf("Gluten"), matches)
    }

    @Test
    fun returnsEmptyWhenNoEnabledAllergensAppear() {
        val matches = AllergenTextMatcher.findMatches(
            recognizedText = "Ingredients: rice flour and water",
            enabledAllergenNames = listOf("Shellfish", "Peanut")
        )

        assertEquals(emptyList<String>(), matches)
    }
}