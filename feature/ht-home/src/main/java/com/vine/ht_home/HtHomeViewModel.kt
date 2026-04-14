package com.vine.ht_home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vine.database.dao.InboundRecordDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

@HiltViewModel
class HtHomeViewModel @Inject constructor(
    inboundRecordDao: InboundRecordDao,
) : ViewModel() {

    val uiState: StateFlow<HtHomeUiState> =
        combine(
            inboundRecordDao.observeTodayInboundQuantity(),
            inboundRecordDao.observeUnsyncedCount(),
        ) { todayInbound, unsyncedCount ->
            HtHomeUiState(
                todayInbound = todayInbound,
                todayOutbound = 0,
                unsyncedCount = unsyncedCount,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HtHomeUiState(),
        )
}