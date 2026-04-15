package com.vine.ht_operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vine.connector_api.InventoryGateway
import com.vine.connector_api.MoveCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HtMoveUiState(
    val productCode: String = "",
    val fromLocationCode: String = "",
    val toLocationCode: String = "",
    val quantity: String = "",
    val note: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val completedMessage: String? = null,
)

@HiltViewModel
class HtMoveViewModel @Inject constructor(
    private val inventoryGateway: InventoryGateway,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HtMoveUiState())
    val uiState: StateFlow<HtMoveUiState> = _uiState

    fun onProductCodeChanged(value: String) {
        _uiState.update { it.copy(productCode = value, errorMessage = null) }
    }

    fun onFromLocationCodeChanged(value: String) {
        _uiState.update { it.copy(fromLocationCode = value, errorMessage = null) }
    }

    fun onToLocationCodeChanged(value: String) {
        _uiState.update { it.copy(toLocationCode = value, errorMessage = null) }
    }

    fun onQuantityChanged(value: String) {
        _uiState.update { it.copy(quantity = value, errorMessage = null) }
    }

    fun onNoteChanged(value: String) {
        _uiState.update { it.copy(note = value) }
    }

    fun submit() {
        val current = _uiState.value

        if (current.productCode.isBlank()) {
            _uiState.update { it.copy(errorMessage = "商品コードを入力してください") }
            return
        }

        if (current.fromLocationCode.isBlank()) {
            _uiState.update { it.copy(errorMessage = "移動元ロケーションを入力してください") }
            return
        }

        if (current.toLocationCode.isBlank()) {
            _uiState.update { it.copy(errorMessage = "移動先ロケーションを入力してください") }
            return
        }

        if (current.fromLocationCode == current.toLocationCode) {
            _uiState.update { it.copy(errorMessage = "移動元と移動先が同じです") }
            return
        }

        val quantityValue: Long = current.quantity.toLongOrNull()
            ?.takeIf { it > 0L }
            ?: run {
                _uiState.update { it.copy(errorMessage = "数量は1以上で入力してください") }
                return
            }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val result = inventoryGateway.registerMove(
                MoveCommand(
                    productCode = current.productCode,
                    fromWarehouseCode = DEFAULT_WAREHOUSE_CODE,
                    fromLocationCode = current.fromLocationCode,
                    toWarehouseCode = DEFAULT_WAREHOUSE_CODE,
                    toLocationCode = current.toLocationCode,
                    quantity = quantityValue,
                    operatorCode = DEFAULT_OPERATOR_CODE,
                    deviceId = DEFAULT_DEVICE_ID,
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

    companion object {
        private const val DEFAULT_WAREHOUSE_CODE = "WH-01"
        private const val DEFAULT_OPERATOR_CODE = "OP-0001"
        private const val DEFAULT_DEVICE_ID = "HT-01"
    }
}