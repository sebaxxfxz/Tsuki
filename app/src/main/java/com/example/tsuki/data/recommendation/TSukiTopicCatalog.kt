package com.example.tsuki.data.recommendation

internal object TSukiTopicCatalog {
    val TOPIC_CATEGORIES: List<TSukiTopicCategory> = mutableListOf(
        TSukiTopicCategory("Arte & Creación", "🎨", mutableListOf("Dibujo", "Pintura", "Fotografía", "Video", "Diseño Gráfico", "Arquitectura", "Escultura", "Ilustración", "Pixel Art")),
        TSukiTopicCategory("Deportes", "⚽", mutableListOf("Fútbol", "Básquetbol", "Tenis", "Fórmula 1", "UFC", "Béisbol", "Boxeo", "Atletismo", "Skateboarding")),
        TSukiTopicCategory("Lifestyle", "✨", mutableListOf("Viajes", "Cocina", "Fitness", "Moda", "Belleza", "DIY", "Minimalismo", "Meditación", "Lectura")),
        TSukiTopicCategory("Tecnología", "💻", mutableListOf("Inteligencia Artificial", "Android", "Programación", "Gadgets", "PC Gaming", "Ciencia", "Espacio", "Ciberseguridad", "Robótica")),
        TSukiTopicCategory("Música", "🎵", mutableListOf("Pop", "Rock", "Hip Hop", "Reggaeton", "Electronic", "Classical", "Jazz", "Lo-Fi", "R&B", "K-Pop", "Metal", "Indie", "Trap", "Salsa", "Cumbia")),
        TSukiTopicCategory("Educación", "📚", mutableListOf("Historia", "Ciencias", "Idiomas", "Matemáticas", "Documentales", "Filosofía", "Psicología", "Economía")),
        TSukiTopicCategory("Entretenimiento", "🎬", mutableListOf("Comedia", "Películas", "Series", "Anime", "Animación", "Celebridades", "Vlogs", "Reacciones", "Sketches", "Stand Up")),
        TSukiTopicCategory("Gaming", "🎮", mutableListOf("Minecraft", "Fortnite", "Valorant", "League of Legends", "GTA", "Call of Duty", "Roblox", "Elden Ring", "Genshin Impact", "Among Us", "FIFA", "Speedrun"))
    )

    private val CATEGORY_ES_TO_EN = mapOf(
        "Arte & Creación" to "Art & Creation",
        "Deportes" to "Sports",
        "Lifestyle" to "Lifestyle",
        "Tecnología" to "Technology",
        "Música" to "Music",
        "Educación" to "Education",
        "Entretenimiento" to "Entertainment",
        "Gaming" to "Gaming"
    )

    private val TOPIC_ES_TO_EN = mapOf(
        "Dibujo" to "Drawing",
        "Pintura" to "Painting",
        "Fotografía" to "Photography",
        "Video" to "Video",
        "Diseño Gráfico" to "Graphic Design",
        "Arquitectura" to "Architecture",
        "Escultura" to "Sculpture",
        "Ilustración" to "Illustration",
        "Pixel Art" to "Pixel Art",
        "Fútbol" to "Soccer",
        "Básquetbol" to "Basketball",
        "Tenis" to "Tennis",
        "Fórmula 1" to "Formula 1",
        "UFC" to "UFC",
        "Béisbol" to "Baseball",
        "Boxeo" to "Boxing",
        "Atletismo" to "Athletics",
        "Skateboarding" to "Skateboarding",
        "Viajes" to "Travel",
        "Cocina" to "Cooking",
        "Fitness" to "Fitness",
        "Moda" to "Fashion",
        "Belleza" to "Beauty",
        "DIY" to "DIY",
        "Minimalismo" to "Minimalism",
        "Meditación" to "Meditation",
        "Lectura" to "Reading",
        "Inteligencia Artificial" to "Artificial Intelligence",
        "Android" to "Android",
        "Programación" to "Coding",
        "Gadgets" to "Gadgets",
        "PC Gaming" to "PC Gaming",
        "Ciencia" to "Science",
        "Espacio" to "Space",
        "Ciberseguridad" to "Cybersecurity",
        "Robótica" to "Robotics",
        "Pop" to "Pop",
        "Rock" to "Rock",
        "Hip Hop" to "Hip Hop",
        "Reggaeton" to "Reggaeton",
        "Electronic" to "Electronic",
        "Classical" to "Classical",
        "Jazz" to "Jazz",
        "Lo-Fi" to "Lo-Fi",
        "R&B" to "R&B",
        "K-Pop" to "K-Pop",
        "Metal" to "Metal",
        "Indie" to "Indie",
        "Trap" to "Trap",
        "Salsa" to "Salsa",
        "Cumbia" to "Cumbia",
        "Historia" to "History",
        "Ciencias" to "Science",
        "Idiomas" to "Languages",
        "Matemáticas" to "Mathematics",
        "Documentales" to "Documentaries",
        "Filosofía" to "Philosophy",
        "Psicología" to "Psychology",
        "Economía" to "Economics",
        "Comedia" to "Comedy",
        "Películas" to "Movies",
        "Series" to "TV Series",
        "Anime" to "Anime",
        "Animación" to "Animation",
        "Celebridades" to "Celebrities",
        "Vlogs" to "Vlogs",
        "Reacciones" to "Reactions",
        "Sketches" to "Sketches",
        "Stand Up" to "Stand Up",
        "Minecraft" to "Minecraft",
        "Fortnite" to "Fortnite",
        "Valorant" to "Valorant",
        "League of Legends" to "League of Legends",
        "GTA" to "GTA",
        "Call of Duty" to "Call of Duty",
        "Roblox" to "Roblox",
        "Elden Ring" to "Elden Ring",
        "Genshin Impact" to "Genshin Impact",
        "Among Us" to "Among Us",
        "FIFA" to "FIFA",
        "Speedrun" to "Speedrun"
    )

    private val TOPIC_EN_TO_ES = TOPIC_ES_TO_EN.entries.associate { (es, en) -> en.lowercase() to es }
    private val CATEGORY_EN_TO_ES = CATEGORY_ES_TO_EN.entries.associate { (es, en) -> en.lowercase() to es }

    private fun stripAccents(s: String): String =
        java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()

    fun getCategories(isEnglish: Boolean): List<TSukiTopicCategory> {
        if (!isEnglish) return TOPIC_CATEGORIES
        return TOPIC_CATEGORIES.map { cat ->
            TSukiTopicCategory(
                name = CATEGORY_ES_TO_EN[cat.name] ?: cat.name,
                icon = cat.icon,
                topics = cat.topics.map { TOPIC_ES_TO_EN[it] ?: it }.toMutableList()
            )
        }
    }

    fun getLocalizedTopic(topic: String, isEnglish: Boolean): String {
        val trimmed = topic.trim()
        val stripped = stripAccents(trimmed)
        if (isEnglish) {
            TOPIC_ES_TO_EN.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) || stripAccents(it.key) == stripped }?.let { return it.value }
            CATEGORY_ES_TO_EN.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) || stripAccents(it.key) == stripped }?.let { return it.value }
            return trimmed
        } else {
            TOPIC_EN_TO_ES.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) || stripAccents(it.key) == stripped }?.let { return it.value }
            CATEGORY_EN_TO_ES.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) || stripAccents(it.key) == stripped }?.let { return it.value }
            TOPIC_ES_TO_EN.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) || stripAccents(it.key) == stripped }?.let { return it.key }
            CATEGORY_ES_TO_EN.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) || stripAccents(it.key) == stripped }?.let { return it.key }
            return trimmed
        }
    }

    fun getLocalizedCategory(category: String, isEnglish: Boolean): String {
        val trimmed = category.trim()
        val stripped = stripAccents(trimmed)
        if (isEnglish) {
            CATEGORY_ES_TO_EN.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) || stripAccents(it.key) == stripped }?.let { return it.value }
            return trimmed
        } else {
            CATEGORY_EN_TO_ES.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) || stripAccents(it.key) == stripped }?.let { return it.value }
            CATEGORY_ES_TO_EN.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) || stripAccents(it.key) == stripped }?.let { return it.key }
            return trimmed
        }
    }

    fun getSearchQueryForTopic(topic: String, isEnglish: Boolean): String {
        val localized = getLocalizedTopic(topic, isEnglish)
        return when (stripAccents(localized)) {
            "cooking" -> "cooking recipes"
            "cocina" -> "cocina recetas"
            "travel" -> "travel vlog"
            "viajes" -> "viajes vlog"
            "drawing" -> "drawing tutorial art"
            "dibujo" -> "dibujo tutorial arte"
            "coding" -> "coding programming"
            "programacion" -> "programacion tutorial"
            "workout" -> "fitness workout"
            else -> localized
        }
    }

    fun getCanonicalTopic(topic: String): String {
        val trimmed = topic.trim()
        val stripped = stripAccents(trimmed)
        return TOPIC_EN_TO_ES[trimmed.lowercase()]
            ?: TOPIC_EN_TO_ES.entries.firstOrNull { stripAccents(it.key) == stripped }?.value
            ?: TOPIC_ES_TO_EN.entries.firstOrNull { it.key.equals(trimmed, ignoreCase = true) || stripAccents(it.key) == stripped }?.key
            ?: trimmed
    }

    val ALL_TOPICS: Set<String> by lazy {
        buildSet {
            for (cat in TOPIC_CATEGORIES) {
                for (entry in cat.topics) {
                    add(entry.lowercase())
                    TOPIC_ES_TO_EN[entry]?.let { add(it.lowercase()) }
                }
            }
        }
    }

    fun buildInitialTopicVector(selectedTopics: Set<String>): Map<String, Double> =
        selectedTopics.filter { it.isNotBlank() }.associate { raw -> getCanonicalTopic(raw).lowercase() to 0.5 }
}
