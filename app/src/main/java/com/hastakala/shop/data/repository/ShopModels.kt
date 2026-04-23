package com.hastakala.shop.data.repository

import com.hastakala.shop.data.local.Product
import com.hastakala.shop.data.local.Sale
import com.hastakala.shop.data.local.Variant

data class VariantForm(
    val id: Long = 0,
    val color: String = "",
    val stock: String = "",
    val manualThreshold: String = ""
)

data class ProductForm(
    val id: Long = 0,
    val name: String = "",
    val category: String = "",
    val basePrice: String = "",
    val imageUri: String? = null,
    val variants: List<VariantForm> = listOf(VariantForm())
)

data class ProductCardModel(
    val id: Long,
    val name: String,
    val category: String,
    val basePrice: Double,
    val imageUri: String?,
    val variants: List<VariantStockModel>
)

data class VariantStockModel(
    val id: Long,
    val color: String,
    val stock: Int,
    val manualThreshold: Int,
    val autoThreshold: Int,
    val effectiveThreshold: Int,
    val isLowStock: Boolean
)

data class InventoryItem(
    val variantId: Long,
    val productId: Long,
    val productName: String,
    val category: String,
    val basePrice: Double,
    val imageUri: String?,
    val color: String,
    val stock: Int,
    val manualThreshold: Int,
    val autoThreshold: Int,
    val effectiveThreshold: Int,
    val isLowStock: Boolean
)

data class PieSlice(
    val label: String,
    val value: Float
)

data class RevenuePoint(
    val label: String,
    val value: Float
)

data class HomeSummary(
    val todayRevenue: Double = 0.0,
    val todayUnitsSold: Int = 0,
    val totalProducts: Int = 0,
    val lowStockCount: Int = 0,
    val lowStockItems: List<InventoryItem> = emptyList()
)

data class InsightsSummary(
    val weekRevenue: Double = 0.0,
    val monthRevenue: Double = 0.0,
    val totalUnitsSold: Int = 0,
    val topProduct: String = "",
    val bestSellers: List<PieSlice> = emptyList(),
    val revenueTrend: List<RevenuePoint> = emptyList()
)

data class BackupPayload(
    val exportedAt: Long,
    val products: List<Product>,
    val variants: List<Variant>,
    val sales: List<Sale>
)
