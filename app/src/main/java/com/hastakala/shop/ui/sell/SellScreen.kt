package com.hastakala.shop.ui.sell

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hastakala.shop.data.repository.ProductCardModel
import com.hastakala.shop.data.repository.VariantStockModel
import com.hastakala.shop.ui.components.AppSearchField
import com.hastakala.shop.ui.components.EmptyState
import com.hastakala.shop.ui.components.ProductGridCard
import com.hastakala.shop.ui.components.SectionTitle
import com.hastakala.shop.ui.components.rememberTextSpeaker
import com.hastakala.shop.util.CurrencyUtils

@Composable
fun SellScreen(
    viewModel: SellViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val textSpeaker = rememberTextSpeaker(uiState.languageTag)
    var selectedProduct by remember { mutableStateOf<ProductCardModel?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val filteredProducts = uiState.products.filter { it.matchesSearch(searchQuery) }

    LaunchedEffect(uiState.message, uiState.error) {
        val text = uiState.message ?: uiState.error
        if (!text.isNullOrBlank()) {
            snackbarHostState.showSnackbar(text)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (uiState.products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    title = "Add your first product",
                    body = "Products with images will appear here for quick selling."
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    SectionTitle(
                        title = "Quick Sell",
                        subtitle = "Tap a product, pick a color, and save the sale."
                    )
                }
                item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                    AppSearchField(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        label = "Search products"
                    )
                }
                if (filteredProducts.isEmpty()) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        EmptyState(
                            title = "No products match",
                            body = "Try a different product, category, or color."
                        )
                    }
                } else {
                    items(filteredProducts, key = { it.id }) { product ->
                        ProductGridCard(product = product, onClick = { selectedProduct = product })
                    }
                }
            }
        }

        selectedProduct?.let { product ->
            SellProductDialog(
                product = product,
                aiConfigured = uiState.aiConfigured,
                aiBusy = uiState.aiBusy,
                suggestionPrice = uiState.aiSuggestion?.suggestedPrice,
                suggestionNote = uiState.aiSuggestion?.explanation,
                canSpeak = textSpeaker.canSpeak,
                onDismiss = {
                    selectedProduct = null
                    viewModel.clearSuggestion()
                },
                onAskAi = { color, materialCost, currentPrice ->
                    viewModel.requestPriceSuggestion(
                        productName = product.name,
                        color = color,
                        materialCost = materialCost,
                        currentPrice = currentPrice
                    )
                },
                onSpeakSuggestion = {
                    textSpeaker.speak(
                        buildPriceSpeech(
                            productName = product.name,
                            color = it.color,
                            suggestedPrice = uiState.aiSuggestion?.suggestedPrice,
                            suggestionNote = uiState.aiSuggestion?.explanation
                        )
                    )
                },
                onSave = { variant, quantity, pricePerUnit ->
                    viewModel.recordSale(
                        productId = product.id,
                        variantId = variant.id,
                        quantity = quantity,
                        totalPrice = pricePerUnit * quantity
                    )
                    selectedProduct = null
                    viewModel.clearSuggestion()
                }
            )
        }
    }
}

@Composable
private fun SellProductDialog(
    product: ProductCardModel,
    aiConfigured: Boolean,
    aiBusy: Boolean,
    suggestionPrice: Double?,
    suggestionNote: String?,
    canSpeak: Boolean,
    onDismiss: () -> Unit,
    onAskAi: (String, Double, Double) -> Unit,
    onSpeakSuggestion: (VariantStockModel) -> Unit,
    onSave: (VariantStockModel, Int, Double) -> Unit
) {
    var quantityText by rememberSaveable(product.id) { mutableStateOf("1") }
    var selectedVariantId by rememberSaveable(product.id) { mutableStateOf(product.variants.first().id) }
    var priceText by rememberSaveable(product.id) { mutableStateOf(product.basePrice.toString()) }
    var materialCostText by rememberSaveable(product.id) { mutableStateOf("") }
    var showAiDialog by rememberSaveable(product.id) { mutableStateOf(false) }

    val selectedVariant = product.variants.first { it.id == selectedVariantId }
    val quantity = parseSaleQuantity(quantityText, selectedVariant.stock)

    LaunchedEffect(selectedVariantId) {
        priceText = product.basePrice.toString()
        materialCostText = ""
        quantityText = defaultQuantityForStock(quantityText, selectedVariant.stock)
    }

    LaunchedEffect(suggestionPrice) {
        if (suggestionPrice != null) {
            priceText = suggestionPrice.toString()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(24.dp)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                SectionTitle(
                    title = product.name,
                    subtitle = "Stock ${selectedVariant.stock} • ${CurrencyUtils.format(product.basePrice)} base price"
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    product.variants.forEach { variant ->
                        FilterChip(
                            selected = selectedVariantId == variant.id,
                            onClick = { selectedVariantId = variant.id },
                            label = { Text("${variant.color} (${variant.stock})") }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            quantityText = adjustQuantity(
                                currentText = quantityText,
                                delta = -1,
                                maxStock = selectedVariant.stock
                            )
                        },
                        enabled = selectedVariant.stock > 0 && quantity > 1
                    ) {
                        Text("-")
                    }
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = {
                            quantityText = sanitizeQuantityInput(
                                rawValue = it,
                                maxStock = selectedVariant.stock
                            )
                        },
                        label = { Text("Quantity") },
                        supportingText = { Text("Type a value or use +/- (max ${selectedVariant.stock})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        enabled = selectedVariant.stock > 0
                    )
                    OutlinedButton(
                        onClick = {
                            quantityText = adjustQuantity(
                                currentText = quantityText,
                                delta = 1,
                                maxStock = selectedVariant.stock
                            )
                        },
                        enabled = selectedVariant.stock > 0 && quantity < selectedVariant.stock
                    ) {
                        Text("+")
                    }
                }

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price per item") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (aiConfigured) {
                    AssistChip(
                        onClick = { showAiDialog = true },
                        label = { Text("AI Price") }
                    )
                }

                if (suggestionPrice != null && !suggestionNote.isNullOrBlank()) {
                    Card(
                        colors = androidx.compose.material3.CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "AI price: ${CurrencyUtils.format(suggestionPrice)}",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(suggestionNote, style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(
                                onClick = { onSpeakSuggestion(selectedVariant) },
                                enabled = canSpeak
                            ) {
                                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Listen")
                            }
                        }
                    }
                }

                Text(
                    text = "Total: ${CurrencyUtils.format((priceText.toDoubleOrNull() ?: 0.0) * quantity)}",
                    style = MaterialTheme.typography.titleLarge
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val unitPrice = priceText.toDoubleOrNull() ?: 0.0
                            onSave(selectedVariant, quantity, unitPrice)
                        },
                        enabled = selectedVariant.stock > 0 && quantity > 0
                    ) {
                        Text("Save Sale")
                    }
                }
            }
        }
    }

    if (showAiDialog) {
        AlertDialog(
            onDismissRequest = { showAiDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val materialCost = materialCostText.toDoubleOrNull() ?: 0.0
                        val currentPrice = priceText.toDoubleOrNull() ?: product.basePrice
                        onAskAi(selectedVariant.color, materialCost, currentPrice)
                    }
                ) {
                    if (aiBusy) {
                        CircularProgressIndicator(modifier = Modifier.width(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Ask AI")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showAiDialog = false }) {
                    Text("Close")
                }
            },
            title = { Text("Price help") },
            text = {
                OutlinedTextField(
                    value = materialCostText,
                    onValueChange = { materialCostText = it },
                    label = { Text("Material cost") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        )
    }
}

private fun buildPriceSpeech(
    productName: String,
    color: String,
    suggestedPrice: Double?,
    suggestionNote: String?
): String = buildString {
    append("AI price help for ")
    append(productName)
    append(", ")
    append(color)
    append(". ")
    suggestedPrice?.let {
        append("Suggested price is ${CurrencyUtils.format(it)}. ")
    }
    suggestionNote?.takeIf { it.isNotBlank() }?.let {
        append(it.replace("\n", " "))
    }
}

private fun ProductCardModel.matchesSearch(query: String): Boolean {
    if (query.isBlank()) return true
    return name.contains(query, ignoreCase = true) ||
        category.contains(query, ignoreCase = true) ||
        variants.any { it.color.contains(query, ignoreCase = true) }
}

private fun defaultQuantityForStock(currentText: String, maxStock: Int): String {
    if (maxStock <= 0) return "0"
    val current = currentText.toIntOrNull()
    return current?.coerceIn(1, maxStock)?.toString() ?: "1"
}

private fun parseSaleQuantity(quantityText: String, maxStock: Int): Int {
    if (maxStock <= 0) return 0
    return quantityText.toIntOrNull()?.coerceIn(1, maxStock) ?: 0
}

private fun sanitizeQuantityInput(rawValue: String, maxStock: Int): String {
    if (maxStock <= 0) return "0"
    val digitsOnly = rawValue.filter(Char::isDigit).take(4)
    if (digitsOnly.isEmpty()) return ""
    return digitsOnly.toIntOrNull()?.coerceIn(1, maxStock)?.toString() ?: ""
}

private fun adjustQuantity(currentText: String, delta: Int, maxStock: Int): String {
    if (maxStock <= 0) return "0"
    val current = currentText.toIntOrNull()?.coerceIn(1, maxStock) ?: 1
    return (current + delta).coerceIn(1, maxStock).toString()
}
