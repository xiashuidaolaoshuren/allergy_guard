package com.xiashuidaolaoshuren.allergyguard.logic

object AllergenTextMatcher {
    fun findMatches(recognizedText: String, enabledAllergenNames: List<String>): List<String> {
        if (recognizedText.isBlank() || enabledAllergenNames.isEmpty()) {
            return emptyList()
        }

        val normalizedText = normalize(recognizedText)
        if (normalizedText.isBlank()) {
            return emptyList()
        }

        val tokens = recognizedText
            .split(Regex("\\s+"))
            .map(::normalize)
            .filter { it.isNotBlank() }

        return enabledAllergenNames.filter { allergen ->
            val normalizedAllergen = normalize(allergen)
            if (normalizedAllergen.isBlank()) {
                return@filter false
            }

            normalizedText.contains(normalizedAllergen) || tokens.any { token ->
                levenshteinDistance(token, normalizedAllergen) <= maxDistance(normalizedAllergen.length)
            }
        }
    }

    private fun normalize(input: String): String {
        return input
            .lowercase()
            .replace(Regex("[^\\p{L}\\p{N}]"), "")
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
}