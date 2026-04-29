package com.hastakala.shop.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hastakala.shop.data.repository.InventoryItem
import com.hastakala.shop.ui.components.AppSearchField
import com.hastakala.shop.ui.components.EmptyState
import com.hastakala.shop.ui.components.SectionTitle
import com.hastakala.shop.ui.components.StockChip
import com.hastakala.shop.ui.components.SummaryCard
import com.hastakala.shop.util.CurrencyUtils

@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val summary = uiState.value.summary
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredLowStockItems = summary.lowStockItems.filter { it.matchesSearch(searchQuery) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            SectionTitle(
                title = "Hasta-Kala Shop",
                subtitle = "Fast billing and inventory for everyday selling."
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "Today income",
                    value = CurrencyUtils.format(summary.todayRevenue),
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Units sold",
                    value = summary.todayUnitsSold.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SummaryCard(
                    title = "Products",
                    value = summary.totalProducts.toString(),
                    modifier = Modifier.weight(1f)
                )
                SummaryCard(
                    title = "Low stock",
                    value = summary.lowStockCount.toString(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (summary.lowStockItems.isEmpty()) {
            item {
                EmptyState(
                    title = "Everything looks healthy",
                    body = "Low stock items will show here in red."
                )
            }
        } else {
            item {
                SectionTitle(
                    title = "Low stock alerts",
                    subtitle = "These items need attention soon."
                )
            }
            item {
                AppSearchField(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    label = "Search low stock"
                )
            }
            if (filteredLowStockItems.isEmpty()) {
                item {
                    EmptyState(
                        title = "No low stock matches",
                        body = "Try a different product or color name."
                    )
                }
            } else {
                items(filteredLowStockItems) { item ->
                    androidx.compose.material3.Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)
                    ) {
                        androidx.compose.foundation.layout.Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            androidx.compose.material3.Text(
                                text = item.productName,
                                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
                            )
                            androidx.compose.material3.Text(
                                text = "Stock left: ${item.stock}",
                                color = androidx.compose.material3.MaterialTheme.colorScheme.error
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                StockChip(
                                    label = item.color,
                                    value = "Need ${item.effectiveThreshold}",
                                    isAlert = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun InventoryItem.matchesSearch(query: String): Boolean {
    if (query.isBlank()) return true
    return productName.contains(query, ignoreCase = true) ||
        color.contains(query, ignoreCase = true)
}
