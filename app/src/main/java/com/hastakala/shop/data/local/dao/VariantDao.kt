package com.hastakala.shop.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.hastakala.shop.data.local.Variant
import com.hastakala.shop.data.local.model.InventoryRow
import kotlinx.coroutines.flow.Flow

@Dao
interface VariantDao {
    @Query(
        """
        SELECT
            variants.id AS variantId,
            products.id AS productId,
            products.name AS productName,
            products.category AS category,
            products.basePrice AS basePrice,
            products.imageUri AS imageUri,
            variants.color AS color,
            variants.stock AS stock,
            variants.lowStockThreshold AS lowStockThreshold
        FROM variants
        INNER JOIN products ON products.id = variants.productId
        ORDER BY products.name COLLATE NOCASE, variants.color COLLATE NOCASE
        """
    )
    fun observeInventoryRows(): Flow<List<InventoryRow>>

    @Query("SELECT * FROM variants WHERE productId = :productId ORDER BY color COLLATE NOCASE")
    suspend fun getVariantsForProduct(productId: Long): List<Variant>

    @Query("SELECT * FROM variants WHERE id = :variantId")
    suspend fun getVariantById(variantId: Long): Variant?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(variant: Variant): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(variants: List<Variant>)

    @Update
    suspend fun update(variant: Variant)

    @Delete
    suspend fun delete(variant: Variant)

    @Query("DELETE FROM variants WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("UPDATE variants SET stock = :newStock WHERE id = :variantId")
    suspend fun updateStock(variantId: Long, newStock: Int)

    @Query("UPDATE variants SET lowStockThreshold = :threshold WHERE id = :variantId")
    suspend fun updateThreshold(variantId: Long, threshold: Int)
}
