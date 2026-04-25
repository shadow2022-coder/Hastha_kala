package com.hastakala.shop.network.ai

import com.hastakala.shop.data.repository.InventoryItem
import com.hastakala.shop.data.repository.PieSlice
import com.hastakala.shop.network.ai.model.AiProvider
import com.hastakala.shop.network.ai.model.AiSettings
import com.hastakala.shop.network.ai.model.PriceSuggestion
import com.hastakala.shop.network.ai.provider.ClaudeAdapter
import com.hastakala.shop.network.ai.provider.GeminiAdapter
import com.hastakala.shop.network.ai.provider.MistralAdapter
import com.hastakala.shop.network.ai.provider.OpenAiAdapter
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiManagerImpl @Inject constructor(
    openAiAdapter: OpenAiAdapter,
    geminiAdapter: GeminiAdapter,
    claudeAdapter: ClaudeAdapter,
    mistralAdapter: MistralAdapter,
    moshi: Moshi
) : AiManager {
    private val adapters = mapOf(
        AiProvider.OPENAI to openAiAdapter,
        AiProvider.GEMINI to geminiAdapter,
        AiProvider.CLAUDE to claudeAdapter,
        AiProvider.MISTRAL to mistralAdapter
    )

    private val priceAdapter: JsonAdapter<PriceSuggestionPayload> =
        moshi.adapter(PriceSuggestionPayload::class.java)

    override suspend fun listModels(settings: AiSettings): Result<List<String>> = runCatching {
        require(settings.apiKey.isNotBlank()) { "Add an API key in Settings first." }
        adapterFor(settings).listModels(settings)
    }

    override suspend fun optimizePrice(
        settings: AiSettings,
        productName: String,
        variantColor: String,
        materialCost: Double,
        currentPrice: Double
    ): Result<PriceSuggestion> = runCatching {
        validateSettings(settings)
        val prompt = """
            You are a pricing helper for a micro-artisan shop.
            Reply in ${settings.language.label}.
            Analyze this product and suggest a safe selling price.

            Product: $productName
            Variant/Color: $variantColor
            Material cost: ₹$materialCost
            Current selling price: ₹$currentPrice

            Return ONLY JSON with keys:
            suggestedPrice
            estimatedProfit
            explanation
        """.trimIndent()

        val raw = adapterFor(settings).sendPrompt(settings, prompt)
        val payload = priceAdapter.fromJson(raw.cleanJson())
            ?: error("Could not read AI response.")
        PriceSuggestion(
            suggestedPrice = payload.suggestedPrice,
            estimatedProfit = payload.estimatedProfit,
            explanation = payload.explanation
        )
    }

    override suspend fun demandInsight(
        settings: AiSettings,
        bestSellers: List<PieSlice>
    ): Result<String> = runCatching {
        validateSettings(settings)
        val summary = if (bestSellers.isEmpty()) {
            "No sales yet."
        } else {
            bestSellers.joinToString(separator = "\n") { "${it.label}: ${it.value.toInt()} sold" }
        }
        adapterFor(settings).sendPrompt(
            settings = settings,
            prompt = """
                Reply in ${settings.language.label}.
                You are helping a small craft seller decide what to produce more of.
                Use this sales summary:
                $summary

                Give a short, clear recommendation with 3 small bullet points max.
            """.trimIndent()
        )
    }

    override suspend fun inventorySuggestion(
        settings: AiSettings,
        inventoryItems: List<InventoryItem>
    ): Result<String> = runCatching {
        validateSettings(settings)
        val summary = if (inventoryItems.isEmpty()) {
            "No inventory yet."
        } else {
            inventoryItems.joinToString(separator = "\n") {
                "${it.productName} ${it.color}: stock=${it.stock}, threshold=${it.effectiveThreshold}"
            }
        }
        adapterFor(settings).sendPrompt(
            settings = settings,
            prompt = """
                Reply in ${settings.language.label}.
                Suggest simple stock levels for this artisan inventory.
                $summary

                Keep the answer very short and practical.
            """.trimIndent()
        )
    }

    private fun validateSettings(settings: AiSettings) {
        require(settings.apiKey.isNotBlank()) { "Add an API key in Settings first." }
        require(settings.model.isNotBlank()) { "Pick or type a model name in Settings first." }
    }

    private fun adapterFor(settings: AiSettings): AiProviderAdapter =
        adapters[settings.provider] ?: error("Provider adapter missing.")

    private fun String.cleanJson(): String = trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()

    private data class PriceSuggestionPayload(
        val suggestedPrice: Double,
        val estimatedProfit: Double,
        val explanation: String
    )
}
