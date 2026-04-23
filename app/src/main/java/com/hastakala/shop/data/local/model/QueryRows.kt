package com.hastakala.shop.data.local.model

data class InventoryRow(
    val variantId: Long,
    val productId: Long,
    val productName: String,
    val category: String,
    val basePrice: Double,
    val imageUri: String?,
    val color: String,
    val stock: Int,
    val lowStockThreshold: Int
)

data class BestSellerRow(
    val productName: String,
    val color: String,
    val quantitySold: Int
)

data class RevenuePointRow(
    val day: String,
    val totalRevenue: Double
)

data class VariantDailySalesRow(
    val variantId: Long,
    val day: String,
    val quantitySold: Int
)

data class TopProductRow(
    val productName: String,
    val quantitySold: Int
)

data class SaleExportRow(
    val saleId: Long,
    val productName: String,
    val color: String,
    val quantity: Int,
    val totalPrice: Double,
    val timestamp: Long
)
