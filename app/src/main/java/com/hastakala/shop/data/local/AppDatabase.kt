package com.hastakala.shop.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hastakala.shop.data.local.dao.ProductDao
import com.hastakala.shop.data.local.dao.SaleDao
import com.hastakala.shop.data.local.dao.VariantDao

@Database(
    entities = [Product::class, Variant::class, Sale::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): ProductDao
    abstract fun variantDao(): VariantDao
    abstract fun saleDao(): SaleDao
}
