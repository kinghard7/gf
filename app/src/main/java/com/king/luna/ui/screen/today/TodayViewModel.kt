package com.king.luna.ui.screen.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.king.luna.data.repo.CycleRepository
import com.king.luna.domain.model.CyclePrediction
import com.king.luna.domain.model.FlowLevel
import com.king.luna.domain.model.Phase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate

data class TodayUiState(
    val today: LocalDate = LocalDate.now(),
    val todayIsPeriod: Boolean = false,
    val prediction: CyclePrediction = CyclePrediction(
        cycleStart = null,
        cycleDay = null,
        avgCycleLength = 28,
        avgPeriodLength = 5,
        nextPeriodStart = null,
        ovulationDay = null,
        fertileWindow = null,
        phase = Phase.UNKNOWN
    )
)

class TodayViewModel(private val repo: CycleRepository) : ViewModel() {

    val state: StateFlow<TodayUiState> = repo.observePrediction()
        .let { flow ->
            kotlinx.coroutines.flow.combine(flow, kotlinx.coroutines.flow.flowOf(LocalDate.now())) { p, d ->
                TodayUiState(today = d, todayIsPeriod = p.phase == Phase.PERIOD, prediction = p)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    // 快速切换今天是否经期：有→删（设无），无→标记中等流量
    fun togglePeriod() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val existing = repo.getPeriodDay(today)
            if (existing != null) {
                repo.setPeriod(today, FlowLevel.NONE)
            } else {
                repo.setPeriod(today, FlowLevel.MEDIUM)
            }
        }
    }

    class Factory(private val repo: CycleRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TodayViewModel(repo) as T
    }
}
