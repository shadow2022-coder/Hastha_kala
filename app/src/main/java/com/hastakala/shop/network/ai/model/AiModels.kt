package com.hastakala.shop.network.ai.model

enum class AiProvider(val displayName: String, val defaultBaseUrl: String) {
    OPENAI("OpenAI", "https://api.openai.com/v1/"),
    GEMINI("Gemini", "https://generativelanguage.googleapis.com/v1beta/"),
    CLAUDE("Claude", "https://api.anthropic.com/v1/"),
    MISTRAL("Mistral", "https://api.mistral.ai/v1/");

    companion object {
        fun fromName(raw: String?): AiProvider =
            entries.firstOrNull { it.name == raw } ?: OPENAI
    }
}

enum class ReplyLanguage(val tag: String, val label: String) {
    ENGLISH("en", "English"),
    KANNADA("kn", "Kannada"),
    HINDI("hi", "Hindi"),
    TELUGU("te", "Telugu");

    companion object {
        fun fromTag(tag: String?): ReplyLanguage =
            entries.firstOrNull { it.tag == tag } ?: ENGLISH
    }
}

data class AiSettings(
    val provider: AiProvider = AiProvider.OPENAI,
    val baseUrl: String = AiProvider.OPENAI.defaultBaseUrl,
    val model: String = "",
    val apiKey: String = "",
    val language: ReplyLanguage = ReplyLanguage.ENGLISH
)

data class AppSettings(
    val languageTag: String = ReplyLanguage.ENGLISH.tag,
    val aiSettings: AiSettings = AiSettings()
)

data class PriceSuggestion(
    val suggestedPrice: Double,
    val estimatedProfit: Double,
    val explanation: String
)
