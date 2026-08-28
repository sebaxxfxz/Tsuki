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
    val ALL_TOPICS: Set<String> by lazy {
        buildSet {
            for (cat in TOPIC_CATEGORIES) for (entry in cat.topics) add(entry.lowercase())
        }
    }
    fun buildInitialTopicVector(selectedTopics: Set<String>): Map<String, Double> =
        selectedTopics.filter { it.isNotBlank() }.associate { raw -> raw.trim().lowercase() to 0.5 }
}
