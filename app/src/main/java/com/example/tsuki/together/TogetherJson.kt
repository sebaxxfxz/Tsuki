package com.example.tsuki.together

import kotlinx.serialization.json.Json

object TogetherJson {
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
        classDiscriminator = "type"
        coerceInputValues = true
    }
}
