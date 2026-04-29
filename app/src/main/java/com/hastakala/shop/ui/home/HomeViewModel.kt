package com.hastakala.shop.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hastakala.shop.data.repository.HomeSummary
import com.hastakala.shop.data.repository.ShopRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val summary: HomeSummary = HomeSummary()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: ShopRepository
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = repository.observeHomeSummary()
        .map { HomeUiState(summary = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState()
        )
}
