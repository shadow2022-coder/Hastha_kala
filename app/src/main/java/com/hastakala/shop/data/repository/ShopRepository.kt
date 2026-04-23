package com.hastakala.shop.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.hastakala.shop.data.local.AppDatabase
import com.hastakala.shop.data.local.Product
import com.hastakala.shop.data.local.Sale
import com.hastakala.shop.data.local.Variant
import com.hastakala.shop.data.local.dao.ProductDao
import com.hastakala.shop.data.local.dao.SaleDao
import com.hastakala.shop.data.local.dao.VariantDao
import com.hastakala.shop.data.local.model.RevenuePointRow
import com.hastakala.shop.data.local.model.VariantDailySalesRow
import com.hastakala.shop.util.TimeUtils
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

enum class RevenueRange {
    WEEK,
    MONTH
}

@Singleton
class ShopRepository @Inject constructor(
    private val database: AppDatabase,
    private val productDao: ProductDao,
    private val variantDao: VariantDao,
    private val saleDao: SaleDao,
    private val moshi: Moshi,
    @ApplicationContext private val context: Context
) {
    private val thresholdWindowStart: Long
        get() = TimeUtils.startOfDaysAgo(30)

    fun observeProducts(): Flow<List<ProductCardModel>> =
        combine(
            productDao.observeProductsWithVariants(),
            saleDao.observeVariantDailySales(thresholdWindowStart)
        ) { products, dailyRows ->
            val thresholdMap = buildAutoThresholdMap(dailyRows)
            products.map { item ->
                ProductCardModel(
                    id = item.product.id,
                    name = item.product.name,
                    category = item.product.category,
                    basePrice = item.product.basePrice,
                    imageUri = item.product.imageUri,
                    variants = item.variants.map { variant ->
                        variant.toVariantStockModel(thresholdMap[variant.id] ?: 0)
                    }
                )
            }
        }

    fun observeInventory(): Flow<List<InventoryItem>> =
        combine(
            variantDao.observeInventoryRows(),
            saleDao.observeVariantDailySales(thresholdWindowStart)
        ) { rows, dailyRows ->
            val thresholdMap = buildAutoThresholdMap(dailyRows)
            rows.map { row ->
                val autoThreshold = thresholdMap[row.variantId] ?: 0
                val effectiveThreshold = row.lowStockThreshold.takeIf { it > 0 } ?: autoThreshold
                InventoryItem(
                    variantId = row.variantId,
                    productId = row.productId,
                    productName = row.productName,
                    category = row.category,
                    basePrice = row.basePrice,
                    imageUri = row.imageUri,
                    color = row.color,
                    stock = row.stock,
                    manualThreshold = row.lowStockThreshold,
                    autoThreshold = autoThreshold,
                    effectiveThreshold = effectiveThreshold,
                    isLowStock = row.stock <= effectiveThreshold
                )
            }
        }

    fun observeHomeSummary(): Flow<HomeSummary> =
        combine(
            saleDao.observeSales(),
            observeInventory(),
            productDao.observeProducts()
        ) { sales, inventoryItems, products ->
            val startOfToday = TimeUtils.startOfTodayMillis()
            val todaySales = sales.filter { it.timestamp >= startOfToday }

            HomeSummary(
                todayRevenue = todaySales.sumOf { it.totalPrice },
                todayUnitsSold = todaySales.sumOf { it.quantity },
                totalProducts = products.size,
                lowStockCount = inventoryItems.count { it.isLowStock },
                lowStockItems = inventoryItems.filter { it.isLowStock }.take(5)
            )
        }

    fun observeBestSellerSlices(): Flow<List<PieSlice>> =
        saleDao.observeBestSellerRows().map { rows ->
            rows.map {
                PieSlice(
                    label = "${it.productName} • ${it.color}",
                    value = it.quantitySold.toFloat()
                )
            }
        }

    fun observeRevenue(range: RevenueRange): Flow<List<RevenuePoint>> {
        val start = when (range) {
            RevenueRange.WEEK -> TimeUtils.startOfWeekMillis()
            RevenueRange.MONTH -> TimeUtils.startOfMonthMillis()
        }
        val end = TimeUtils.now()

        return saleDao.observeRevenuePoints(start, end).map { rows ->
            rows.toRevenuePoints(start, end)
        }
    }

    fun observeInsights(range: RevenueRange): Flow<InsightsSummary> =
        combine(
            saleDao.observeIncomeTotal(TimeUtils.startOfWeekMillis(), TimeUtils.now()),
            saleDao.observeIncomeTotal(TimeUtils.startOfMonthMillis(), TimeUtils.now()),
            saleDao.observeTotalUnitsSold(),
            saleDao.observeTopProduct(),
            observeBestSellerSlices()
        ) { weekRevenue, monthRevenue, totalUnits, topProduct, bestSellers ->
            InsightBaseData(
                weekRevenue = weekRevenue,
                monthRevenue = monthRevenue,
                totalUnits = totalUnits,
                topProduct = topProduct?.productName.orEmpty(),
                bestSellers = bestSellers
            )
        }.combine(observeRevenue(range)) { baseData, revenueTrend ->
            InsightsSummary(
                weekRevenue = baseData.weekRevenue,
                monthRevenue = baseData.monthRevenue,
                totalUnitsSold = baseData.totalUnits,
                topProduct = baseData.topProduct,
                bestSellers = baseData.bestSellers,
                revenueTrend = revenueTrend
            )
        }

    suspend fun saveProduct(form: ProductForm) {
        val price = form.basePrice.toDoubleOrNull() ?: 0.0
        val cleanedVariants = form.variants
            .mapNotNull { variant ->
                val color = variant.color.trim()
                if (color.isEmpty()) {
                    null
                } else {
                    variant.copy(
                        color = color,
                        stock = variant.stock.trim(),
                        manualThreshold = variant.manualThreshold.trim()
                    )
                }
            }
            .distinctBy { it.color.lowercase() }

        require(form.name.trim().isNotEmpty()) { "Product name is required." }
        require(cleanedVariants.isNotEmpty()) { "At least one variant is required." }

        database.withTransaction {
            val productId = if (form.id == 0L) {
                productDao.insert(
                    Product(
                        name = form.name.trim(),
                        category = form.category.trim(),
                        basePrice = price,
                        imageUri = form.imageUri
                    )
                )
            } else {
                productDao.update(
                    Product(
                        id = form.id,
                        name = form.name.trim(),
                        category = form.category.trim(),
                        basePrice = price,
                        imageUri = form.imageUri
                    )
                )
                form.id
            }

            val existing = variantDao.getVariantsForProduct(productId)
            val incomingIds = cleanedVariants.mapNotNull { it.id.takeIf { id -> id != 0L } }
            val toDelete = existing.map { it.id }.filterNot(incomingIds::contains)
            if (toDelete.isNotEmpty()) {
                variantDao.deleteByIds(toDelete)
            }

            cleanedVariants.forEach { variant ->
                val entity = Variant(
                    id = variant.id,
                    productId = productId,
                    color = variant.color.trim(),
                    stock = variant.stock.toIntOrNull()?.coerceAtLeast(0) ?: 0,
                    lowStockThreshold = variant.manualThreshold.toIntOrNull()?.coerceAtLeast(0) ?: 0
                )
                if (variant.id == 0L) {
                    variantDao.insert(entity)
                } else {
                    variantDao.update(entity)
                }
            }
        }
    }

    suspend fun deleteProduct(productId: Long) {
        val product = productDao.getProduct(productId) ?: return
        productDao.delete(product)
    }

    suspend fun deleteVariant(variantId: Long): Boolean = database.withTransaction {
        val variant = variantDao.getVariantById(variantId) ?: return@withTransaction false
        val siblingVariants = variantDao.getVariantsForProduct(variant.productId)

        if (siblingVariants.size <= 1) {
            productDao.getProduct(variant.productId)?.let { product ->
                productDao.delete(product)
            }
            true
        } else {
            variantDao.deleteByIds(listOf(variantId))
            false
        }
    }

    suspend fun logSale(
        productId: Long,
        variantId: Long,
        quantity: Int,
        totalPrice: Double,
        timestamp: Long = TimeUtils.now()
    ) {
        require(quantity > 0) { "Quantity must be greater than zero." }
        require(totalPrice >= 0) { "Total price cannot be negative." }

        database.withTransaction {
            val variant = variantDao.getVariantById(variantId)
                ?: error("Variant no longer exists.")

            if (variant.stock < quantity) {
                error("Only ${variant.stock} item(s) left in stock.")
            }

            saleDao.insert(
                Sale(
                    productId = productId,
                    variantId = variantId,
                    quantity = quantity,
                    totalPrice = totalPrice,
                    timestamp = timestamp
                )
            )
            variantDao.updateStock(variantId, variant.stock - quantity)
        }
    }

    suspend fun adjustStock(variantId: Long, newStock: Int) {
        variantDao.updateStock(variantId, newStock.coerceAtLeast(0))
    }

    suspend fun updateThreshold(variantId: Long, threshold: Int) {
        variantDao.updateThreshold(variantId, threshold.coerceAtLeast(0))
    }

    suspend fun getProductForm(productId: Long): ProductForm? {
        return productDao.getProductWithVariants(productId)?.let { data ->
            ProductForm(
                id = data.product.id,
                name = data.product.name,
                category = data.product.category,
                basePrice = data.product.basePrice.toString(),
                imageUri = data.product.imageUri,
                variants = data.variants.map { variant ->
                    VariantForm(
                        id = variant.id,
                        color = variant.color,
                        stock = variant.stock.toString(),
                        manualThreshold = variant.lowStockThreshold.takeIf { it > 0 }?.toString().orEmpty()
                    )
                }
            )
        }
    }

    suspend fun exportSalesCsv(): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val rows = saleDao.getSalesExportRows()
            val exportDir = ensureExportDirectory()
            val file = File(exportDir, "sales_${TimeUtils.fileTimeStamp()}.csv")
            val header = "Sale ID,Product,Color,Quantity,Total Price,Timestamp"
            val body = rows.joinToString(separator = "\n") { row ->
                listOf(
                    row.saleId,
                    row.productName.csvSafe(),
                    row.color.csvSafe(),
                    row.quantity,
                    row.totalPrice,
                    row.timestamp
                ).joinToString(",")
            }
            file.writeText(listOf(header, body).filter { it.isNotBlank() }.joinToString("\n"))
            file
        }
    }

    suspend fun backupToJson(): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val exportDir = ensureExportDirectory()
            val file = File(exportDir, "backup_${TimeUtils.fileTimeStamp()}.json")
            val jsonAdapter = moshi.adapter(BackupPayload::class.java)
            val products = productDao.getProductsSnapshot()
            val variants = products.flatMap { variantDao.getVariantsForProduct(it.id) }
            val payload = BackupPayload(
                exportedAt = TimeUtils.now(),
                products = products,
                variants = variants,
                sales = saleDao.getSalesSnapshot()
            )
            file.writeText(jsonAdapter.indent("  ").toJson(payload))
            file
        }
    }

    private fun ensureExportDirectory(): File {
        val exportRoot = context.getExternalFilesDir(null) ?: context.filesDir
        return File(exportRoot, "exports").apply { mkdirs() }
    }

    private fun buildAutoThresholdMap(rows: List<VariantDailySalesRow>): Map<Long, Int> =
        rows.groupBy { it.variantId }.mapValues { (_, entries) ->
            val averageDailySales = if (entries.isEmpty()) {
                0.0
            } else {
                entries.sumOf { it.quantitySold }.toDouble() / entries.size
            }
            ceil(averageDailySales * 3).toInt()
        }

    private fun Variant.toVariantStockModel(autoThreshold: Int): VariantStockModel {
        val effectiveThreshold = lowStockThreshold.takeIf { it > 0 } ?: autoThreshold
        return VariantStockModel(
            id = id,
            color = color,
            stock = stock,
            manualThreshold = lowStockThreshold,
            autoThreshold = autoThreshold,
            effectiveThreshold = effectiveThreshold,
            isLowStock = stock <= effectiveThreshold
        )
    }

    private fun List<RevenuePointRow>.toRevenuePoints(start: Long, end: Long): List<RevenuePoint> {
        val lookup = associateBy { LocalDate.parse(it.day) }
        return TimeUtils.datesBetween(start, end).map { date ->
            RevenuePoint(
                label = TimeUtils.formatShortDate(date),
                value = (lookup[date]?.totalRevenue ?: 0.0).toFloat()
            )
        }
    }

    private fun String.csvSafe(): String = "\"${replace("\"", "\"\"")}\""

    private data class InsightBaseData(
        val weekRevenue: Double,
        val monthRevenue: Double,
        val totalUnits: Int,
        val topProduct: String,
        val bestSellers: List<PieSlice>
    )
}
