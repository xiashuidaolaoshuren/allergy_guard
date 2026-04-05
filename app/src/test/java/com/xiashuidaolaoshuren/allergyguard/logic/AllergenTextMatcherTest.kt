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

    @Test
    fun matchesCheesecakeForMilkAndEggs() {
        val matches = AllergenTextMatcher.findMatches(
            recognizedText = "contains cheesecake",
            allergenSynonyms = mapOf(
                "Milk" to listOf("cheese", "cheesecake", "cake"),
                "Eggs" to listOf("egg", "cheesecake", "cake")
            )
        )

        assertTrue("Milk should be detected via 'cheesecake'", matches.contains("Milk"))
        assertTrue("Eggs should be detected via 'cheesecake'", matches.contains("Eggs"))
    }

    @Test
    fun matchesCantoneseCheesecakeForMilkAndEggs() {
        val matches = AllergenTextMatcher.findMatches(
            recognizedText = "今日甜品: 芝士蛋糕",
            allergenSynonyms = mapOf(
                "Milk" to listOf("芝士", "芝士蛋糕", "蛋糕"),
                "Eggs" to listOf("蛋", "蛋糕", "芝士蛋糕")
            )
        )

        assertTrue("Milk should be detected via '芝士蛋糕'", matches.contains("Milk"))
        assertTrue("Eggs should be detected via '芝士蛋糕'", matches.contains("Eggs"))
    }

    @Test
    fun doesNotMatchMilkOrEggsForFishCakeWhenOnlyCakeHeuristicProvided() {
        val matches = AllergenTextMatcher.findMatches(
            recognizedText = "Dim sum includes fish cake and fish balls",
            allergenSynonyms = mapOf(
                "Milk" to listOf("cake"),
                "Eggs" to listOf("cake")
            )
        )

        assertEquals(emptyList<String>(), matches)
    }

    @Test
    fun doesNotMatchMilkOrEggsForTurnipCakeWhenOnlyCakeHeuristicProvided() {
        val matches = AllergenTextMatcher.findMatches(
            recognizedText = "點心: 蘿蔔糕",
            allergenSynonyms = mapOf(
                "Milk" to listOf("蛋糕"),
                "Eggs" to listOf("蛋糕")
            )
        )

        assertEquals(emptyList<String>(), matches)
    }

    @Test
    fun stillMatchesMilkWhenFishCakeAndCheeseAppearTogether() {
        val matches = AllergenTextMatcher.findMatches(
            recognizedText = "fish cake with cheese topping",
            allergenSynonyms = mapOf(
                "Milk" to listOf("cake", "cheese"),
                "Eggs" to listOf("cake")
            )
        )

        assertTrue("Milk should still match via 'cheese'", matches.contains("Milk"))
    }

    // Helper overload to keep old-style calls working in tests
    private fun mapOf(vararg pairs: Pair<String, List<String>>): Map<String, List<String>> =
        pairs.toMap()
}