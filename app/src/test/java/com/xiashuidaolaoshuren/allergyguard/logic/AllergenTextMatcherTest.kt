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

    @Test
    fun matchesChineseAllergenName() {
        val matches = AllergenTextMatcher.findMatches(
            recognizedText = "配料：花生油、盐",
            enabledAllergenNames = listOf("花生", "小麦")
        )

        assertEquals(listOf("花生"), matches)
    }

    @Test
    fun matchesJapaneseAllergenName() {
        val matches = AllergenTextMatcher.findMatches(
            recognizedText = "原材料：落花生、食塩",
            enabledAllergenNames = listOf("落花生", "小麦")
        )

        assertEquals(listOf("落花生"), matches)
    }

    @Test
    fun matchesKoreanAllergenName() {
        val matches = AllergenTextMatcher.findMatches(
            recognizedText = "원재료명: 땅콩, 소금",
            enabledAllergenNames = listOf("땅콩", "밀")
        )

        assertEquals(listOf("땅콩"), matches)
    }
}