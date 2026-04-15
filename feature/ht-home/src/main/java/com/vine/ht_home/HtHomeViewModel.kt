package com.vine.ht_home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vine.database.ZaikoDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HtHomeViewModel @Inject constructor(
    database: ZaikoDatabase,
) : ViewModel() {

    private val inboundDao = database.inboundDao()
    private val syncQueueDao = database.syncQueueDao()

    val uiState: StateFlow<HtHomeUiState> = combine(
        inboundDao.observeTodayInboundQuantity(),
        syncQueueDao.observeUnsyncedCount(),
    ) { todayInbound, unsyncedCount ->
        HtHomeUiState(
            todayInbound = todayInbound.toInt(),
            todayOutbound = 0,
            unsyncedCount = unsyncedCount,
            operatorName = "テスト担当者",
            warehouseName = "本倉庫",
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HtHomeUiState(),
    )
}