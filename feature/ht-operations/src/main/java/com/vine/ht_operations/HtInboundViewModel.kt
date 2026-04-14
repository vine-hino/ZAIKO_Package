package com.vine.ht_operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vine.connector_api.InboundRequest
import com.vine.connector_api.InventoryGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HtInboundViewModel @Inject constructor(
    private val inventoryGateway: InventoryGateway,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HtInboundUiState())
    val uiState: StateFlow<HtInboundUiState> = _uiState

    fun onProductCodeChanged(value: String) {
        _uiState.update { it.copy(productCode = value, errorMessage = null) }
    }

    fun onLocationCodeChanged(value: String) {
        _uiState.update { it.copy(locationCode = value, errorMessage = null) }
    }

    fun onQuantityChanged(value: String) {
        _uiState.update { it.copy(quantity = value, errorMessage = null) }
    }

    fun onNoteChanged(value: String) {
        _uiState.update { it.copy(note = value) }
    }

    fun submit() {
        val current = _uiState.value
        val parsedQty = current.quantity.toIntOrNull()

        when {
            current.productCode.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "商品コードを入力してください") }
                return
            }

            current.locationCode.isBlank() -> {
                _uiState.update { it.copy(errorMessage = "ロケーションを入力してください") }
                return
            }

            parsedQty == null || parsedQty <= 0 -> {
                _uiState.update { it.copy(errorMessage = "数量は1以上で入力してください") }
                return
            }
        }

        val qty = parsedQty ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val result = inventoryGateway.registerInbound(
                InboundRequest(
                    productCode = current.productCode,
                    locationCode = current.locationCode,
                    quantity = qty,
                    note = current.note.ifBlank { null },
                ),
            )

            if (result.accepted) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        completedMessage = buildString {
                            append(result.message)
                            result.referenceId?.let { ref ->
                                append("\n受付番号: ")
                                append(ref)
                            }
                        },
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = result.message,
                    )
                }
            }
        }
    }

    fun consumeCompleted() {
        _uiState.update { it.copy(completedMessage = null) }
    }
}