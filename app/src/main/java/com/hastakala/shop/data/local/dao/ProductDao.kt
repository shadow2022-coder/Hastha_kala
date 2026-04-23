package com.hastakala.shop.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.hastakala.shop.data.local.Product
import com.hastakala.shop.data.local.model.ProductWithVariants
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {
    @Transaction
    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE")
    fun observeProductsWithVariants(): Flow<List<ProductWithVariants>>

    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE")
    fun observeProducts(): Flow<List<Product>>

    @Transaction
    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProductWithVariants(productId: Long): ProductWithVariants?

    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE")
    suspend fun getProductsSnapshot(): List<Product>

    @Query("SELECT * FROM products WHERE id = :productId")
    suspend fun getProduct(productId: Long): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(product: Product): Long

    @Update
    suspend fun update(product: Product)

    @Delete
    suspend fun delete(product: Product)
}
