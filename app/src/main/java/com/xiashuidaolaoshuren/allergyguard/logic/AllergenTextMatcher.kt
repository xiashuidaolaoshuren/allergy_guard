package com.xiashuidaolaoshuren.allergyguard.logic

import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

object AllergenTextMatcher {
    /**
     * Finds which allergens are present in [recognizedText].
     *
     * @param recognizedText  The (translated) OCR text to search.
     * @param allergenSynonyms  Map of allergen display name → list of synonyms/derivatives
     *   (built-in synonyms + user aliases). An empty synonym list is fine — the allergen
     *   name itself is always checked as well.
     * @return List of allergen display names that were matched.
     */
    fun findMatches(
        recognizedText: String,
        allergenSynonyms: Map<String, List<String>>
    ): List<String> {
        val startMs = OcrDebugProfiler.frameStartToken()
        if (recognizedText.isBlank() || allergenSynonyms.isEmpty()) {
            return emptyList()
        }

        val normalizedText = normalize(recognizedText)
        if (normalizedText.isBlank()) {
            return emptyList()
        }

        val tokens = normalizedText
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        val tokenSet = tokens.toHashSet()

        val matches = allergenSynonyms.keys.filter { allergenName ->
            val allTerms = buildList {
                add(allergenName)
                addAll(allergenSynonyms[allergenName] ?: emptyList())
            }
            allTerms.any { term ->
                val normalizedTerm = normalize(term)
                if (normalizedTerm.isBlank()) return@any false
                if (shouldSkipHeuristicTerm(allergenName, normalizedTerm, normalizedText)) {
                    return@any false
                }
                containsWholeTerm(normalizedText, normalizedTerm) || tokens.any { token ->
                    val allowedDistance = maxDistance(normalizedTerm.length)
                    token == normalizedTerm ||
                        (normalizedTerm !in tokenSet &&
                            abs(token.length - normalizedTerm.length) <= allowedDistance &&
                            levenshteinDistance(token, normalizedTerm) <= allowedDistance)
                }
            }
        }

        val latencyMs = OcrDebugProfiler.frameStartToken() - startMs
        OcrDebugProfiler.markMatcherLatency(latencyMs = latencyMs, tokenCount = tokens.size)
        return matches
    }

    private fun normalize(input: String): String {
        return input
            .lowercase()
            .replace(NON_WORD_REGEX, " ")
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }

    private fun containsWholeTerm(normalizedText: String, normalizedTerm: String): Boolean {
        if (containsCjk(normalizedTerm)) {
            return normalizedText.contains(normalizedTerm)
        }

        val pattern = WHOLE_TERM_PATTERN_CACHE.getOrPut(normalizedTerm) {
            Regex("(?<![\\p{L}\\p{N}])${Regex.escape(normalizedTerm)}(?![\\p{L}\\p{N}])")
        }
        return pattern.containsMatchIn(normalizedText)
    }

    private fun containsCjk(text: String): Boolean {
        return text.any { char ->
            Character.UnicodeScript.of(char.code) in CJK_SCRIPTS
        }
    }

    private fun shouldSkipHeuristicTerm(
        allergenName: String,
        normalizedTerm: String,
        normalizedText: String
    ): Boolean {
        val normalizedAllergen = normalize(allergenName)
        val heuristicTerms = HEURISTIC_TERMS_BY_ALLERGEN[normalizedAllergen] ?: return false
        if (normalizedTerm !in heuristicTerms) {
            return false
        }

        return CAKE_EXCEPTION_TERMS.any { exceptionTerm ->
            containsWholeTerm(normalizedText, exceptionTerm)
        }
    }

    private fun maxDistance(length: Int): Int {
        return when {
            length <= 4 -> 0
            length <= 8 -> 1
            else -> 2
        }
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        val previousRow = IntArray(b.length + 1) { it }
        val currentRow = IntArray(b.length + 1)

        for (i in a.indices) {
            currentRow[0] = i + 1
            for (j in b.indices) {
                val substitutionCost = if (a[i] == b[j]) 0 else 1
                currentRow[j + 1] = minOf(
                    currentRow[j] + 1,
                    previousRow[j + 1] + 1,
                    previousRow[j] + substitutionCost
                )
            }
            for (j in previousRow.indices) {
                previousRow[j] = currentRow[j]
            }
        }

        return previousRow[b.length]
    }

    private val NON_WORD_REGEX = Regex("[^\\p{L}\\p{N}\\s]")
    private val WHITESPACE_REGEX = Regex("\\s+")
    private val WHOLE_TERM_PATTERN_CACHE = ConcurrentHashMap<String, Regex>()
    private val CJK_SCRIPTS = setOf(
        Character.UnicodeScript.HAN,
        Character.UnicodeScript.HIRAGANA,
        Character.UnicodeScript.KATAKANA,
        Character.UnicodeScript.HANGUL
    )
    private val HEURISTIC_TERMS_BY_ALLERGEN = mapOf(
        "milk" to setOf("cake", "蛋糕", "ケーキ", "케이크"),
        "eggs" to setOf("cake", "蛋糕", "ケーキ", "케이크")
    )
    private val CAKE_EXCEPTION_TERMS = setOf(
        "fish cake",
        "fishcake",
        "魚餅",
        "鱼饼",
        "turnip cake",
        "蘿蔔糕",
        "萝卜糕",
        "rice cake",
        "年糕",
        "mochi"
    ).map(::normalize).toSet()
}