package com.xiashuidaolaoshuren.allergyguard.logic

import java.util.Locale

/**
 * Hardcoded synonym/derivative map for the nine built-in allergens.
 * Keys are lowercased normalized allergen names (matching AppDatabase seed names after lowercasing).
 * Values list common derivative ingredients whose presence implies the allergen.
 */
object AllergenSynonymMap {

    private val map: Map<String, List<String>> = mapOf(
        "milk" to listOf(
            "butter", "buttermilk", "cheese", "cream", "sour cream", "whipping cream",
            "half and half", "casein", "caseinate", "whey", "lactose", "lactalbumin",
            "lactoglobulin", "ghee", "yogurt", "custard", "curd", "dairy",
            "brie", "camembert", "parmesan", "mozzarella", "ricotta", "gouda",
            "cheddar", "feta", "colby", "havarti", "goat cheese", "cream cheese",
            "cottage cheese", "ice cream", "gelato", "pudding", "ganache", "nougat",
            "caramel", "milk chocolate"
        ),
        "eggs" to listOf(
            "egg", "yolk", "egg white", "albumin", "ovalbumin", "ovomucin",
            "ovomucoid", "ovotransferrin", "livetin", "lysozyme",
            "mayonnaise", "mayo", "meringue", "hollandaise", "aioli",
            "egg noodles", "egg pasta", "eggnog", "quiche", "frittata"
        ),
        "fish" to listOf(
            "salmon", "tuna", "cod", "halibut", "tilapia", "bass", "herring",
            "anchovy", "anchovies", "sardine", "mackerel", "mahi", "mahi mahi",
            "trout", "catfish", "snapper", "grouper", "flounder", "sole",
            "pike", "carp", "perch", "pollock", "haddock", "swordfish",
            "fish sauce", "worcestershire", "caesar dressing"
        ),
        "crustacean shellfish" to listOf(
            "shrimp", "crab", "lobster", "prawn", "crawfish", "crayfish",
            "langoustine", "scampi", "barnacle", "krill",
            "shrimp paste", "crab cake", "lobster bisque"
        ),
        "tree nuts" to listOf(
            "almond", "walnut", "cashew", "pistachio", "pecan", "hazelnut",
            "macadamia", "brazil nut", "pine nut", "chestnut", "coconut",
            "marzipan", "praline", "gianduja", "nougat", "nut butter",
            "almond milk", "almond flour", "hazelnut spread", "nutella"
        ),
        "peanuts" to listOf(
            "peanut", "groundnut", "arachis", "monkey nut", "beer nut",
            "peanut butter", "peanut oil", "satay", "mixed nuts"
        ),
        "wheat" to listOf(
            "flour", "bread", "gluten", "spelt", "farro", "durum", "kamut",
            "bulgur", "couscous", "semolina", "triticale", "seitan",
            "pasta", "noodles", "crackers", "croutons", "batter",
            "breadcrumbs", "roux", "wheat starch", "wheat germ"
        ),
        "soybeans" to listOf(
            "soy", "soya", "tofu", "edamame", "miso", "tempeh", "tamari",
            "natto", "soy sauce", "soy milk", "soy protein", "textured vegetable protein",
            "tvp", "kinako"
        ),
        "sesame" to listOf(
            "tahini", "sesame oil", "sesame seed", "til", "benne", "gingelly",
            "sesame paste", "sesame flour"
        )
    )

    /**
     * Returns the synonym list for the given allergen name, or an empty list if none are defined.
     * Lookup is case-insensitive.
     */
    fun getSynonyms(allergenName: String): List<String> {
        val key = allergenName.trim().lowercase(Locale.ROOT)
        return map[key] ?: emptyList()
    }
}
