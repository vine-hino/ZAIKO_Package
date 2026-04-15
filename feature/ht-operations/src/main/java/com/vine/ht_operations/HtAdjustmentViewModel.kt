package com.vine.ht_operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vine.connector_api.AdjustmentCommand
import com.vine.connector_api.InventoryGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HtAdjustmentUiState(
    val productCode: String = "",
    val locationCode: String = "",
    val adjustQuantity: String = "",
    val reasonCode: String = "",
    val note: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val completedMessage: String? = null,
)

@HiltViewModel
class HtAdjustmentViewModel @Inject constructor(
    private val inventoryGateway: InventoryGateway,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HtAdjustmentUiState())
    val uiState: StateFlow<HtAdjustmentUiState> = _uiState

    fun onProductCodeChanged(value: String) {
        _uiState.update { it.copy(productCode = value, errorMessage = null) }
    }

    fun onLocationCodeChanged(value: String) {
        _uiState.update { it.copy(locationCode = value, errorMessage = null) }
    }

    fun onAdjustQuantityChanged(value: String) {
        _uiState.update { it.copy(adjustQuantity = value, errorMessage = null) }
    }

    fun onReasonCodeChanged(value: String) {
        _uiState.update { it.copy(reasonCode = value.uppercase(), errorMessage = null) }
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

        if (current.locationCode.isBlank()) {
            _uiState.update { it.copy(errorMessage = "ロケーションを入力してください") }
            return
        }

        if (current.reasonCode.isBlank()) {
            _uiState.update { it.copy(errorMessage = "理由コードを入力してください") }
            return
        }

        val adjustQuantityValue: Long = current.adjustQuantity.toLongOrNull()
            ?.takeIf { it != 0L }
            ?: run {
                _uiState.update { it.copy(errorMessage = "調整数は0以外で入力してください") }
                return
            }

        if (current.reasonCode == "OTHER" && current.note.isBlank()) {
            _uiState.update { it.copy(errorMessage = "理由がOTHERのときは備考を入力してください") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            val result = inventoryGateway.registerAdjustment(
                AdjustmentCommand(
                    productCode = current.productCode,
                    warehouseCode = DEFAULT_WAREHOUSE_CODE,
                    locationCode = current.locationCode,
                    adjustQuantity = adjustQuantityValue,
                    reasonCode = current.reasonCode,
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