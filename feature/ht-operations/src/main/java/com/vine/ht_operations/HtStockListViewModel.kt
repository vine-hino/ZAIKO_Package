package com.vine.ht_operations.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vine.connector_api.InventoryGateway
import com.vine.connector_api.StockItem
import com.vine.connector_api.StockQuery
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HtStockListUiState(
    val keyword: String = "",
    val isLoading: Boolean = false,
    val items: List<StockItem> = emptyList(),
    val errorMessage: String? = null,
)

@HiltViewModel
class HtStockListViewModel @Inject constructor(
    private val inventoryGateway: InventoryGateway,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HtStockListUiState())
    val uiState: StateFlow<HtStockListUiState> = _uiState

    init {
        search()
    }

    fun onKeywordChanged(value: String) {
        _uiState.update { it.copy(keyword = value, errorMessage = null) }
    }

    fun search() {
        val keyword = _uiState.value.keyword.trim()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            runCatching {
                if (keyword.isBlank()) {
                    inventoryGateway.searchStock(
                        StockQuery(limit = 200),
                    )
                } else {
                    val byProductCode = inventoryGateway.searchStock(
                        StockQuery(
                            productCode = keyword,
                            limit = 200,
                        ),
                    )

                    byProductCode.ifEmpty {
                        inventoryGateway.searchStock(
                            StockQuery(
                                barcode = keyword,
                                limit = 200,
                            ),
                        )
                    }
                }
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
                        errorMessage = error.message ?: "在庫照会に失敗しました",
                    )
                }
            }
        }
    }
}