package com.xiashuidaolaoshuren.allergyguard.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AllergenTextMatcherTest {

    // Helper: build a synonym map with no synonyms (just the allergen name itself)
    private fun mapOf(vararg names: String): Map<String, List<String>> =
        names.associateWith { emptyList() }

    @Test
    fun findsMatchWhenTextHasMixedCaseAndWhitespace() {
        val matches = AllergenTextMatcher.findMatches(
            recognizedText = "Ingredients: PeAnu t oil and salt",
            allergenSynonyms = mapOf("Peanut", "Gluten")
        )

        assertEquals(listOf("Peanut"), matches)
    }

    @Test
    fun findsFuzzyMatchForMinorOcrTypo() {
        val matches = AllergenTextMatcher.findMatches(
            recognizedText = "Contains glutn",
            allergenSynonyms = mapOf("Gluten")
        )

        assertEquals(listOf("Gluten"), matches)
    }

    @Test
    fun returnsEmptyWhenNoEnabledAllergensAppear() {
        val matches = AllergenTextMatcher.findMatches(
            recognizedText = "Ingredients: rice flour and water",
            allergenSynonyms = mapOf("Shellfish", "Peanut")
        )

        assertEquals(emptyList<String>(), matches)
    }

    @Test
    fun matchesChineseAllergenName() {
        val matches = AllergenTextMatcher.findMatches(
            recognizedText = "配料：花生油、盐",
            allergenSynonyms = mapOf("花生", "小麦")
        )

        assertEquals(listOf("花生"), matches)
    }

    @Test
    fun matchesJapaneseAllergenName() {
        val matches = AllergenTextMatcher.findMatches(
            recognizedText = "原材料：落花生、食塩",
            allergenSynonyms = mapOf("落花生", "小麦")
        )

        assertEquals(listOf("落花生"), matches)
    }

    @Test
    fun matchesKoreanAllergenName() {
        val matches = AllergenTextMatcher.findMatches(
            recognizedText = "원재료명: 땅콩, 소금",
            allergenSynonyms = mapOf("땅콩", "밀")
        )

        assertEquals(listOf("땅콩"), matches)
    }

    // ── Synonym / derivative matching ────────────────────────────────────────

    @Test
    fun matchesMilkDerivativeCheeseViaSynonym() {
        val matches = AllergenTextMatcher.findMatches(
            recognizedText = "Served with melted cheese and bread",
            allergenSynonyms = mapOf("Milk" to listOf("cheese", "butter", "cream"))
        )
        assertTrue("Milk should be detected via 'cheese' synonym", matches.contains("Milk"))
    }

    @Test
    fun matchesMilkDerivativeButterViaSynonym() {
        val matches = AllergenTextMatcher.findMatches(
            recognizedText = "pan-fried in butter",
            allergenSynonyms = mapOf("Milk" to listOf("cheese", "butter", "cream"))
        )
        assertTrue("Milk should be detected via 'butter' synonym", matches.contains("Milk"))
    }

    @Test
    fun matchesUserCustomAlias() {
        val matches = AllergenTextMatcher.findMatches(
            recognizedText = "This dish contains edamame beans",
            allergenSynonyms = mapOf("MySoy" to listOf("edamame", "tofu"))
        )
        assertTrue("Custom allergen should be detected via user alias 'edamame'", matches.contains("MySoy"))
    }

    @Test
    fun doesNotMatchDerivativeWhenNotInSynonymList() {
        val matches = AllergenTextMatcher.findMatches(
            recognizedText = "Served with melted cheese",
            allergenSynonyms = mapOf("Milk" to emptyList()) // no synonyms provided
        )
        assertEquals("Milk should NOT be matched when only checking the name itself", emptyList<String>(), matches)
    }

    // Helper overload to keep old-style calls working in tests
    private fun mapOf(vararg pairs: Pair<String, List<String>>): Map<String, List<String>> =
        pairs.toMap()
}