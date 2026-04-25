package com.hastakala.shop.network.ai

import com.hastakala.shop.data.repository.InventoryItem
import com.hastakala.shop.data.repository.PieSlice
import com.hastakala.shop.network.ai.model.AiSettings
import com.hastakala.shop.network.ai.model.PriceSuggestion

interface AiManager {
    suspend fun listModels(settings: AiSettings): Result<List<String>>

    suspend fun optimizePrice(
        settings: AiSettings,
        productName: String,
        variantColor: String,
        materialCost: Double,
        currentPrice: Double
    ): Result<PriceSuggestion>

    suspend fun demandInsight(
        settings: AiSettings,
        bestSellers: List<PieSlice>
    ): Result<String>

    suspend fun inventorySuggestion(
        settings: AiSettings,
        inventoryItems: List<InventoryItem>
    ): Result<String>
}
