package com.vine.ht_operations

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vine.connector_api.InventoryGateway
import com.vine.connector_api.OutboundCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HtOutboundUiState(
    val productCode: String = "",
    val locationCode: String = "",
    val quantity: String = "",
    val note: String = "",
    val isSaving: Boolean = false,
    val isExporting: Boolean = false,
    val errorMessage: String? = null,
    val completedMessage: String? = null,
    val exportMessage: String? = null,
    val savedOperationUuid: String? = null,
) {
    val canSubmit: Boolean
        get() = productCode.isNotBlank() &&
                locationCode.isNotBlank() &&
                (quantity.toLongOrNull()?.let { it > 0L } == true) &&
                !isSaving &&
                !isExporting
}

@HiltViewModel
class HtOutboundViewModel @Inject constructor(
    private val inventoryGateway: InventoryGateway,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HtOutboundUiState())
    val uiState: StateFlow<HtOutboundUiState> = _uiState

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

        if (current.productCode.isBlank()) {
            _uiState.update { it.copy(errorMessage = "商品コードを入力してください") }
            return
        }

        if (current.locationCode.isBlank()) {
            _uiState.update { it.copy(errorMessage = "ロケーションを入力してください") }
            return
        }

        val quantityValue: Long = current.quantity.toLongOrNull()
            ?.takeIf { it > 0L }
            ?: run {
                _uiState.update { it.copy(errorMessage = "数量は1以上で入力してください") }
                return
            }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isSaving = true,
                    errorMessage = null,
                    exportMessage = null,
                )
            }

            val result = inventoryGateway.registerOutbound(
                OutboundCommand(
                    productCode = current.productCode,
                    fromWarehouseCode = DEFAULT_WAREHOUSE_CODE,
                    fromLocationCode = current.locationCode,
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
                        savedOperationUuid = result.operationUuid,
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

    fun exportJson() {
        val current = _uiState.value
        val operationUuid = current.savedOperationUuid
            ?: run {
                _uiState.update { it.copy(errorMessage = "先に出庫を保存してください") }
                return
            }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isExporting = true,
                    errorMessage = null,
                    exportMessage = null,
                )
            }

            val outputFile = createExportFile()

            val result = inventoryGateway.exportOutboundToJson(
                operationUuid = operationUuid,
                outputFilePath = outputFile.absolutePath,
                sourceDeviceId = DEFAULT_DEVICE_ID,
            )

            if (result.accepted) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportMessage = buildString {
                            append(result.message)
                            result.referenceId?.let { path ->
                                append("\n保存先: ")
                                append(path)
                            }
                        },
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = result.message,
                    )
                }
            }
        }
    }

    fun clearCompletedState() {
        _uiState.update {
            it.copy(
                completedMessage = null,
                exportMessage = null,
                savedOperationUuid = null,
            )
        }
    }

    private fun createExportFile(): File {
        val baseDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.filesDir
        val exportDir = File(baseDir, "exports").apply { mkdirs() }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(Date())

        return File(exportDir, "outbound_$timestamp.json")
    }

    companion object {
        private const val DEFAULT_WAREHOUSE_CODE = "WH-01"
        private const val DEFAULT_OPERATOR_CODE = "OP-0001"
        private const val DEFAULT_DEVICE_ID = "HT-01"
    }
}