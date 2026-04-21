package com.vine.ht_operations.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vine.connector_api.InventoryGateway
import com.vine.connector_api.StockHistoryItem
import com.vine.connector_api.StockHistoryQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HtStockHistoryUiState(
    val isLoading: Boolean = false,
    val items: List<StockHistoryItem> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class HtStockHistoryViewModel @Inject constructor(
    private val inventoryGateway: InventoryGateway,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HtStockHistoryUiState())
    val uiState: StateFlow<HtStockHistoryUiState> = _uiState

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            runCatching {
                inventoryGateway.getStockHistory(
                    StockHistoryQuery(limit = 100),
                )
            }.onSuccess { items ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = items,
                        errorMessage = null,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        items = emptyList(),
                        errorMessage = error.message ?: "在庫履歴取得に失敗しました",
                    )
                }
            }
        }
    }
}