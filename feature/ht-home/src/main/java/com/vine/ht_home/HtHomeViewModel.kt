package com.vine.ht_home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vine.connector_api.HomeDashboardGateway
import com.vine.connector_api.HomeDashboardSummary
import com.vine.connector_api.InventoryGateway
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class HtHomeViewModel @Inject constructor(
    private val homeDashboardGateway: HomeDashboardGateway,
    private val inventoryGateway: InventoryGateway,
) : ViewModel() {

    private val summaryState = MutableStateFlow(HomeDashboardSummary())
    private val unsyncedCountState = MutableStateFlow(0)
    private val isLoadingState = MutableStateFlow(false)

    val uiState: StateFlow<HtHomeUiState> = combine(
        summaryState,
        unsyncedCountState,
        isLoadingState,
    ) { summary, unsyncedCount, isLoading ->
        HtHomeUiState(
            todayInbound = summary.todayInboundQuantity,
            todayOutbound = summary.todayOutboundQuantity,
            unsyncedCount = unsyncedCount,
            operatorName = "テスト担当者",
            warehouseName = "本倉庫",
            isLoading = isLoading,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HtHomeUiState(isLoading = true),
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            isLoadingState.value = true
            runCatching {
                homeDashboardGateway.getSummary()
            }.onSuccess { summary ->
                summaryState.value = summary
            }
            runCatching {
                inventoryGateway.getUnsyncedCount()
            }.onSuccess { unsyncedCount ->
                unsyncedCountState.value = unsyncedCount
            }
            isLoadingState.value = false
        }
    }
}
