package com.hastakala.shop.data.local.model

import androidx.room.Embedded
import androidx.room.Relation
import com.hastakala.shop.data.local.Product
import com.hastakala.shop.data.local.Variant

data class ProductWithVariants(
    @Embedded val product: Product,
    @Relation(
        parentColumn = "id",
        entityColumn = "productId"
    )
    val variants: List<Variant>
)
