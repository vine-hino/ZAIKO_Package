package com.vine.ht_operations.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vine.connector_api.InventoryGateway
import com.vine.connector_api.MasterLookupItem
import com.vine.connector_api.MasterReferenceGateway
import com.vine.connector_api.MasterType
import com.vine.ht_operations.ui.LookupUiState
import com.vine.connector_api.StocktakeCommand
import com.vine.connector_api.StocktakeLineCommand
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate

@HiltViewModel
class HtStocktakeViewModel @Inject constructor(
    private val inventoryGateway: InventoryGateway,
    private val masterReferenceGateway: MasterReferenceGateway,
) : ViewModel() {

    var productLookup by mutableStateOf(LookupUiState())
        private set

    var locationLookup by mutableStateOf(LookupUiState())
        private set

    var countedQuantityText by mutableStateOf("")
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

    fun onCountedQuantityChanged(value: String) {
        countedQuantityText = value
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

    fun submitStocktake() {
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
        val countedQuantity = countedQuantityText.toIntOrNull()?.takeIf { it >= 0 } ?: run {
            errorMessage = "実棚数は0以上で入力してください"
            return
        }

        viewModelScope.launch {
            isSubmitting = true
            errorMessage = null

            val line = StocktakeLineCommand(
                productCode = product.code,
                productName = product.name,
                warehouseCode = warehouseCode,
                locationCode = location.code,
                actualQuantity = countedQuantity.toLong(),
            )

            val command = StocktakeCommand(
                stocktakeDate = LocalDate.now().toString(),
                operatorCode = DEFAULT_OPERATOR_CODE,
                warehouseCode = warehouseCode,
                deviceId = DEFAULT_DEVICE_ID,
                note = noteText.ifBlank { null },
                lines = listOf(line),
            )
            val result = inventoryGateway.saveStocktake(command)

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
        countedQuantityText = ""
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