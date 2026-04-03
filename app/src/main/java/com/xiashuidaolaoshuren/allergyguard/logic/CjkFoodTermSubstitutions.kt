package com.xiashuidaolaoshuren.allergyguard.logic

/**
 * Normalizes known CJK menu terms that are commonly mistranslated by literal OCR translation.
 */
object CjkFoodTermSubstitutions {
    private val substitutions: Map<String, String> = linkedMapOf(
        "魚蛋" to "fish ball",
        "鱼蛋" to "fish ball",
        "魚丸" to "fish ball",
        "鱼丸" to "fish ball",
        "墨魚丸" to "squid ball",
        "墨鱼丸" to "squid ball",
        "牛丸" to "beef ball"
    )

    fun apply(text: String): String {
        if (text.isBlank()) {
            return text
        }

        var updated = text
        substitutions.forEach { (term, replacement) ->
            updated = updated.replace(term, replacement)
        }
        return updated
    }
}
