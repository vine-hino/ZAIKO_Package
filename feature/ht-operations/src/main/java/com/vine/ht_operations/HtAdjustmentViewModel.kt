package com.vine.ht_operations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vine.connector_api.AdjustmentCommand
import com.vine.connector_api.InventoryGateway
import com.vine.connector_api.MasterLookupItem
import com.vine.connector_api.MasterReferenceGateway
import com.vine.connector_api.MasterType
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@HiltViewModel
class HtAdjustmentViewModel @Inject constructor(
    private val inventoryGateway: InventoryGateway,
    private val masterReferenceGateway: MasterReferenceGateway,
) : ViewModel() {

    var productLookup by mutableStateOf(LookupUiState())
        private set

    var locationLookup by mutableStateOf(LookupUiState())
        private set

    var reasonLookup by mutableStateOf(LookupUiState())
        private set

    var quantityText by mutableStateOf("")
        private set

    var noteText by mutableStateOf("")
        private set

    var isSubmitting by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var completedMessage by mutableStateOf<String?>(null)
        private set

    private var productSearchJob: Job? = null
    private var locationSearchJob: Job? = null
    private var reasonSearchJob: Job? = null

    fun onProductQueryChanged(value: String) {
        productLookup = productLookup.copy(
            query = value,
            selected = null,
            errorMessage = null,
        )
        searchProducts(value)
    }

    fun onLocationQueryChanged(value: String) {
        locationLookup = locationLookup.copy(
            query = value,
            selected = null,
            errorMessage = null,
        )
        searchLocations(value)
    }

    fun onReasonQueryChanged(value: String) {
        reasonLookup = reasonLookup.copy(
            query = value,
            selected = null,
            errorMessage = null,
        )
        searchReasons(value)
    }

    fun onQuantityChanged(value: String) {
        quantityText = value
        errorMessage = null
    }

    fun onNoteChanged(value: String) {
        noteText = value
        errorMessage = null
    }

    fun selectProduct(item: MasterLookupItem) {
        productLookup = productLookup.copy(
            query = item.code,
            selected = item,
            candidates = emptyList(),
            isLoading = false,
        )
    }

    fun selectLocation(item: MasterLookupItem) {
        locationLookup = locationLookup.copy(
            query = item.code,
            selected = item,
            candidates = emptyList(),
            isLoading = false,
        )
    }

    fun selectReason(item: MasterLookupItem) {
        reasonLookup = reasonLookup.copy(
            query = item.code,
            selected = item,
            candidates = emptyList(),
            isLoading = false,
        )
    }

    private fun searchProducts(query: String) {
        productSearchJob?.cancel()
        productSearchJob = viewModelScope.launch {
            if (query.isBlank()) {
                productLookup = productLookup.copy(
                    candidates = emptyList(),
                    isLoading = false,
                )
                return@launch
            }

            delay(250)
            productLookup = productLookup.copy(isLoading = true)

            runCatching {
                masterReferenceGateway.search(
                    type = MasterType.PRODUCT,
                    query = query,
                    limit = 20,
                    includeInactive = false,
                )
            }.onSuccess { items ->
                productLookup = productLookup.copy(
                    candidates = items,
                    isLoading = false,
                )
            }.onFailure { e ->
                productLookup = productLookup.copy(
                    candidates = emptyList(),
                    isLoading = false,
                    errorMessage = e.message ?: "商品候補の取得に失敗しました",
                )
            }
        }
    }

    private fun searchLocations(query: String) {
        locationSearchJob?.cancel()
        locationSearchJob = viewModelScope.launch {
            if (query.isBlank()) {
                locationLookup = locationLookup.copy(
                    candidates = emptyList(),
                    isLoading = false,
                )
                return@launch
            }

            delay(250)
            locationLookup = locationLookup.copy(isLoading = true)

            runCatching {
                masterReferenceGateway.search(
                    type = MasterType.LOCATION,
                    query = query,
                    limit = 20,
                    includeInactive = false,
                )
            }.onSuccess { items ->
                locationLookup = locationLookup.copy(
                    candidates = items,
                    isLoading = false,
                )
            }.onFailure { e ->
                locationLookup = locationLookup.copy(
                    candidates = emptyList(),
                    isLoading = false,
                    errorMessage = e.message ?: "ロケーション候補の取得に失敗しました",
                )
            }
        }
    }

    private fun searchReasons(query: String) {
        reasonSearchJob?.cancel()
        reasonSearchJob = viewModelScope.launch {
            if (query.isBlank()) {
                reasonLookup = reasonLookup.copy(
                    candidates = emptyList(),
                    isLoading = false,
                )
                return@launch
            }

            delay(250)
            reasonLookup = reasonLookup.copy(isLoading = true)

            runCatching {
                masterReferenceGateway.search(
                    type = MasterType.REASON,
                    query = query,
                    limit = 20,
                    includeInactive = false,
                )
            }.onSuccess { items ->
                reasonLookup = reasonLookup.copy(
                    candidates = items,
                    isLoading = false,
                )
            }.onFailure { e ->
                reasonLookup = reasonLookup.copy(
                    candidates = emptyList(),
                    isLoading = false,
                    errorMessage = e.message ?: "理由候補の取得に失敗しました",
                )
            }
        }
    }

    fun submitAdjustment() {
        val product = productLookup.selected ?: run {
            errorMessage = "商品を選択してください"
            return
        }
        val location = locationLookup.selected ?: run {
            errorMessage = "ロケーションを選択してください"
            return
        }
        val warehouseCode = location.warehouseCode ?: run {
            errorMessage = "倉庫情報が取得できません"
            return
        }
        val reason = reasonLookup.selected ?: run {
            errorMessage = "理由を選択してください"
            return
        }
        val quantity = quantityText.toIntOrNull()?.takeIf { it != 0 } ?: run {
            errorMessage = "調整数は0以外で入力してください"
            return
        }

        viewModelScope.launch {
            isSubmitting = true
            errorMessage = null

            val command = AdjustmentCommand(
                productCode = product.code,
                productName = product.name,
                warehouseCode = warehouseCode,
                locationCode = location.code,
                adjustQuantity = quantity.toLong(),
                reasonCode = reason.code,
                reasonName = reason.name,
                operatorCode = DEFAULT_OPERATOR_CODE,
                deviceId = DEFAULT_DEVICE_ID,
                note = noteText.ifBlank { null },
            )
            val result = inventoryGateway.registerAdjustment(command)

            isSubmitting = false
            if (result.accepted) {
                completedMessage = buildResultMessage(result.message, result.referenceId)
                resetForm()
            } else {
                errorMessage = result.message
            }
        }
    }

    fun consumeCompleted() {
        completedMessage = null
    }

    fun clearError() {
        errorMessage = null
    }

    private fun resetForm() {
        productLookup = LookupUiState()
        locationLookup = LookupUiState()
        reasonLookup = LookupUiState()
        quantityText = ""
        noteText = ""
    }

    private fun buildResultMessage(message: String, referenceId: String?): String {
        return buildString {
            append(message)
            referenceId?.let {
                append("\n受付番号: ")
                append(it)
            }
        }
    }

    companion object {
        private const val DEFAULT_OPERATOR_CODE = "OP001"
        private const val DEFAULT_DEVICE_ID = "HT-01"
    }
}