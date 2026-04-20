package com.vine.ht_operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vine.connector_api.InboundCommand
import com.vine.connector_api.InventoryGateway
import com.vine.connector_api.MasterLookupItem
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HtInboundViewModel @Inject constructor(
    private val inventoryGateway: InventoryGateway,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HtInboundUiState())
    val uiState: StateFlow<HtInboundUiState> = _uiState.asStateFlow()

    fun onProductKeywordChanged(value: String) {
        _uiState.update {
            it.copy(
                productKeyword = value,
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun onLocationKeywordChanged(value: String) {
        _uiState.update {
            it.copy(
                locationKeyword = value,
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun onQuantityChanged(value: String) {
        val normalized = value.filter { it.isDigit() }
        _uiState.update {
            it.copy(
                quantity = normalized,
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun onNoteChanged(value: String) {
        _uiState.update {
            it.copy(
                note = value,
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun searchProducts() {
        val keyword = _uiState.value.productKeyword.trim()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSearchingProducts = true,
                    errorMessage = null,
                    successMessage = null,
                )
            }

            runCatching {
                inventoryGateway.searchMasters(
                    type = "PRODUCT",
                    keyword = keyword.ifBlank { null },
                    includeInactive = false,
                    limit = 20,
                )
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        productCandidates = result,
                        isSearchingProducts = false,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSearchingProducts = false,
                        errorMessage = error.message ?: "商品検索に失敗しました",
                    )
                }
            }
        }
    }

    fun searchLocations() {
        val keyword = _uiState.value.locationKeyword.trim()

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSearchingLocations = true,
                    errorMessage = null,
                    successMessage = null,
                )
            }

            runCatching {
                inventoryGateway.searchMasters(
                    type = "LOCATION",
                    keyword = keyword.ifBlank { null },
                    includeInactive = false,
                    limit = 20,
                )
            }.onSuccess { result ->
                _uiState.update {
                    it.copy(
                        locationCandidates = result,
                        isSearchingLocations = false,
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSearchingLocations = false,
                        errorMessage = error.message ?: "ロケーション検索に失敗しました",
                    )
                }
            }
        }
    }

    fun selectProduct(item: MasterLookupItem) {
        _uiState.update {
            it.copy(
                selectedProduct = item,
                productKeyword = "${item.code} ${item.name}",
                productCandidates = emptyList(),
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun selectLocation(item: MasterLookupItem) {
        _uiState.update {
            it.copy(
                selectedLocation = item,
                locationKeyword = "${item.code} ${item.name}",
                locationCandidates = emptyList(),
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun clearMessage() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                successMessage = null,
            )
        }
    }

    fun submit() {
        val current = _uiState.value

        val product = current.selectedProduct
        if (product == null) {
            _uiState.update { it.copy(errorMessage = "商品を選択してください") }
            return
        }

        val location = current.selectedLocation
        if (location == null) {
            _uiState.update { it.copy(errorMessage = "ロケーションを選択してください") }
            return
        }

        val warehouseCode = location.warehouseCode
        if (warehouseCode.isNullOrBlank()) {
            _uiState.update { it.copy(errorMessage = "選択したロケーションに倉庫コードがありません") }
            return
        }

        val quantityValue = current.quantity.toLongOrNull()
        if (quantityValue == null || quantityValue <= 0L) {
            _uiState.update { it.copy(errorMessage = "数量は1以上で入力してください") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSubmitting = true,
                    errorMessage = null,
                    successMessage = null,
                )
            }

            runCatching {
                inventoryGateway.registerInbound(
                    InboundCommand(
                        productCode = product.code,
                        toWarehouseCode = warehouseCode,
                        toLocationCode = location.code,
                        quantity = quantityValue,
                        operatorCode = DEFAULT_OPERATOR_CODE,
                        deviceId = DEFAULT_DEVICE_ID,
                        note = current.note.ifBlank { null },
                        externalDocNo = null,
                        inboundPlanId = null,
                    )
                )
            }.onSuccess { result ->
                if (result.accepted) {
                    _uiState.update {
                        it.copy(
                            quantity = "",
                            note = "",
                            isSubmitting = false,
                            errorMessage = null,
                            successMessage = result.message,
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isSubmitting = false,
                            errorMessage = result.message,
                        )
                    }
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        errorMessage = error.message ?: "入庫登録に失敗しました",
                    )
                }
            }
        }
    }

    companion object {
        private const val DEFAULT_OPERATOR_CODE = "OP001"
        private const val DEFAULT_DEVICE_ID = "HT-01"
    }
}