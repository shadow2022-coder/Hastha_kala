package com.hastakala.shop.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.hastakala.shop.data.local.Sale
import com.hastakala.shop.data.local.model.BestSellerRow
import com.hastakala.shop.data.local.model.RevenuePointRow
import com.hastakala.shop.data.local.model.SaleExportRow
import com.hastakala.shop.data.local.model.TopProductRow
import com.hastakala.shop.data.local.model.VariantDailySalesRow
import kotlinx.coroutines.flow.Flow

@Dao
interface SaleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(sale: Sale): Long

    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    fun observeSales(): Flow<List<Sale>>

    @Query("SELECT * FROM sales ORDER BY timestamp DESC")
    suspend fun getSalesSnapshot(): List<Sale>

    @Query(
        """
        SELECT
            products.name AS productName,
            variants.color AS color,
            SUM(sales.quantity) AS quantitySold
        FROM sales
        INNER JOIN products ON sales.productId = products.id
        INNER JOIN variants ON sales.variantId = variants.id
        GROUP BY sales.variantId
        ORDER BY quantitySold DESC, productName COLLATE NOCASE
        """
    )
    fun observeBestSellerRows(): Flow<List<BestSellerRow>>

    @Query(
        """
        SELECT
            DATE(timestamp / 1000, 'unixepoch', 'localtime') AS day,
            SUM(totalPrice) AS totalRevenue
        FROM sales
        WHERE timestamp BETWEEN :start AND :end
        GROUP BY day
        ORDER BY day
        """
    )
    fun observeRevenuePoints(start: Long, end: Long): Flow<List<RevenuePointRow>>

    @Query(
        """
        SELECT
            variantId AS variantId,
            DATE(timestamp / 1000, 'unixepoch', 'localtime') AS day,
            SUM(quantity) AS quantitySold
        FROM sales
        WHERE timestamp >= :start
        GROUP BY variantId, day
        """
    )
    fun observeVariantDailySales(start: Long): Flow<List<VariantDailySalesRow>>

    @Query(
        """
        SELECT
            products.name AS productName,
            SUM(sales.quantity) AS quantitySold
        FROM sales
        INNER JOIN products ON sales.productId = products.id
        GROUP BY sales.productId
        ORDER BY quantitySold DESC, productName COLLATE NOCASE
        LIMIT 1
        """
    )
    fun observeTopProduct(): Flow<TopProductRow?>

    @Query("SELECT COALESCE(SUM(totalPrice), 0.0) FROM sales WHERE timestamp BETWEEN :start AND :end")
    fun observeIncomeTotal(start: Long, end: Long): Flow<Double>

    @Query("SELECT COALESCE(SUM(quantity), 0) FROM sales")
    fun observeTotalUnitsSold(): Flow<Int>

    @Query(
        """
        SELECT
            sales.id AS saleId,
            products.name AS productName,
            variants.color AS color,
            sales.quantity AS quantity,
            sales.totalPrice AS totalPrice,
            sales.timestamp AS timestamp
        FROM sales
        INNER JOIN products ON sales.productId = products.id
        INNER JOIN variants ON sales.variantId = variants.id
        ORDER BY sales.timestamp DESC
        """
    )
    suspend fun getSalesExportRows(): List<SaleExportRow>
}
