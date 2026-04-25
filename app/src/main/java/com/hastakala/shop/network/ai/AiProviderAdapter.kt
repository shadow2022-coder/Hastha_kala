package com.hastakala.shop.network.ai

import com.hastakala.shop.network.ai.model.AiProvider
import com.hastakala.shop.network.ai.model.AiSettings

interface AiProviderAdapter {
    val provider: AiProvider

    suspend fun listModels(settings: AiSettings): List<String>

    suspend fun sendPrompt(settings: AiSettings, prompt: String): String
}
