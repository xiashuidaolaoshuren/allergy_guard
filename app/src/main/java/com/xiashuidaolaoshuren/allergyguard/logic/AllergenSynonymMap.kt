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
            "caramel", "milk chocolate",
            "milk", "牛奶", "牛乳", "乳製品", "奶", "ミルク", "牛乳", "우유"
        ),
        "eggs" to listOf(
            "egg", "yolk", "egg white", "albumin", "ovalbumin", "ovomucin",
            "ovomucoid", "ovotransferrin", "livetin", "lysozyme",
            "mayonnaise", "mayo", "meringue", "hollandaise", "aioli",
            "egg noodles", "egg pasta", "eggnog", "quiche", "frittata",
            "玉子", "卵", "たまご", "鸡蛋", "雞蛋", "煎蛋", "蛋黃", "蛋白", "계란", "달걀"
        ),
        "fish" to listOf(
            "salmon", "tuna", "cod", "halibut", "tilapia", "bass", "herring",
            "anchovy", "anchovies", "sardine", "mackerel", "mahi", "mahi mahi",
            "trout", "catfish", "snapper", "grouper", "flounder", "sole",
            "pike", "carp", "perch", "pollock", "haddock", "swordfish",
            "fish sauce", "worcestershire", "caesar dressing",
            "fish", "魚", "鱼", "鮭", "鲑", "吞拿魚", "tuna", "サーモン", "魚介", "생선"
        ),
        "crustacean shellfish" to listOf(
            "shrimp", "crab", "lobster", "prawn", "crawfish", "crayfish",
            "langoustine", "scampi", "barnacle", "krill",
            "shrimp paste", "crab cake", "lobster bisque",
            "蝦", "虾", "蟹", "螃蟹", "龍蝦", "龙虾", "海老", "えび", "かに", "새우", "게"
        ),
        "tree nuts" to listOf(
            "almond", "walnut", "cashew", "pistachio", "pecan", "hazelnut",
            "macadamia", "brazil nut", "pine nut", "chestnut", "coconut",
            "marzipan", "praline", "gianduja", "nougat", "nut butter",
            "almond milk", "almond flour", "hazelnut spread", "nutella",
            "杏仁", "核桃", "腰果", "開心果", "开心果", "榛子", "夏威夷果", "松子", "板栗", "栗子", "견과"
        ),
        "peanuts" to listOf(
            "peanut", "groundnut", "arachis", "monkey nut", "beer nut",
            "peanut butter", "peanut oil", "satay", "mixed nuts",
            "花生", "花生醬", "花生酱", "落花生", "ピーナッツ", "땅콩"
        ),
        "wheat" to listOf(
            "flour", "bread", "gluten", "spelt", "farro", "durum", "kamut",
            "bulgur", "couscous", "semolina", "triticale", "seitan",
            "pasta", "noodles", "crackers", "croutons", "batter",
            "breadcrumbs", "roux", "wheat starch", "wheat germ",
            "小麦", "小麥", "麵粉", "面粉", "麵", "面", "グルテン", "밀"
        ),
        "soybeans" to listOf(
            "soy", "soya", "tofu", "edamame", "miso", "tempeh", "tamari",
            "natto", "soy sauce", "soy milk", "soy protein", "textured vegetable protein",
            "tvp", "kinako",
            "大豆", "黃豆", "黄豆", "豆腐", "豆漿", "豆浆", "味噌", "醬油", "酱油", "醤油", "간장", "두유"
        ),
        "sesame" to listOf(
            "tahini", "sesame oil", "sesame seed", "til", "benne", "gingelly",
            "sesame paste", "sesame flour",
            "芝麻", "ごま", "胡麻", "참깨"
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
