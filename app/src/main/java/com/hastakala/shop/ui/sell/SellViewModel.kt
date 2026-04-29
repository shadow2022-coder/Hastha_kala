package com.hastakala.shop.ui.sell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hastakala.shop.data.repository.ProductCardModel
import com.hastakala.shop.data.repository.SettingsRepository
import com.hastakala.shop.data.repository.ShopRepository
import com.hastakala.shop.network.ai.AiManager
import com.hastakala.shop.network.ai.model.PriceSuggestion
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SellUiState(
    val products: List<ProductCardModel> = emptyList(),
    val aiConfigured: Boolean = false,
    val aiBusy: Boolean = false,
    val aiSuggestion: PriceSuggestion? = null,
    val languageTag: String = "en",
    val message: String? = null,
    val error: String? = null
)

@HiltViewModel
class SellViewModel @Inject constructor(
    private val shopRepository: ShopRepository,
    private val settingsRepository: SettingsRepository,
    private val aiManager: AiManager
) : ViewModel() {
    private val aiBusy = MutableStateFlow(false)
    private val aiSuggestion = MutableStateFlow<PriceSuggestion?>(null)
    private val message = MutableStateFlow<String?>(null)
    private val error = MutableStateFlow<String?>(null)

    private val coreState = combine(
        shopRepository.observeProducts(),
        settingsRepository.settingsFlow,
        aiBusy,
        aiSuggestion,
        message
    ) { products, settings, isBusy, suggestion, currentMessage ->
        SellUiState(
            products = products,
            aiConfigured = settings.aiSettings.apiKey.isNotBlank() && settings.aiSettings.model.isNotBlank(),
            aiBusy = isBusy,
            aiSuggestion = suggestion,
            languageTag = settings.languageTag,
            message = currentMessage
        )
    }

    val uiState: StateFlow<SellUiState> = combine(coreState, error) { baseState, currentError ->
        SellUiState(
            products = baseState.products,
            aiConfigured = baseState.aiConfigured,
            aiBusy = baseState.aiBusy,
            aiSuggestion = baseState.aiSuggestion,
            languageTag = baseState.languageTag,
            message = baseState.message,
            error = currentError
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SellUiState()
    )

    fun recordSale(productId: Long, variantId: Long, quantity: Int, totalPrice: Double) {
        viewModelScope.launch {
            runCatching {
                shopRepository.logSale(productId, variantId, quantity, totalPrice)
            }.onSuccess {
                message.value = "Sale saved"
                error.value = null
            }.onFailure {
                error.value = it.message ?: "Could not save sale."
            }
        }
    }

    fun requestPriceSuggestion(
        productName: String,
        color: String,
        materialCost: Double,
        currentPrice: Double
    ) {
        viewModelScope.launch {
            aiBusy.value = true
            aiSuggestion.value = null
            val settings = settingsRepository.currentSettings().aiSettings
            aiManager.optimizePrice(
                settings = settings,
                productName = productName,
                variantColor = color,
                materialCost = materialCost,
                currentPrice = currentPrice
            ).onSuccess {
                aiSuggestion.value = it
                error.value = null
            }.onFailure {
                error.value = it.message ?: "AI suggestion failed."
            }
            aiBusy.value = false
        }
    }

    fun clearMessage() {
        message.value = null
        error.value = null
    }

    fun clearSuggestion() {
        aiSuggestion.value = null
    }
}
