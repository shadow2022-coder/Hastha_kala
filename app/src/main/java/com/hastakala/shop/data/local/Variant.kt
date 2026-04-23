package com.hastakala.shop.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "variants",
    foreignKeys = [
        ForeignKey(
            entity = Product::class,
            parentColumns = ["id"],
            childColumns = ["productId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("productId"),
        Index(value = ["productId", "color"], unique = true)
    ]
)
data class Variant(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val color: String,
    val stock: Int,
    val lowStockThreshold: Int = 0
)
