package com.king.luna.ui.screen.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.king.luna.data.entity.DayLogEntity
import com.king.luna.data.repo.CycleRepository
import com.king.luna.domain.model.CyclePrediction
import com.king.luna.domain.model.Phase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth

data class CalendarUiState(
    val month: YearMonth = YearMonth.now(),
    val periodDays: Set<LocalDate> = emptySet(),
    val loggedDates: Set<LocalDate> = emptySet(),
    val dayLogs: List<DayLogEntity> = emptyList(),
    val prediction: CyclePrediction = CyclePrediction(
        cycleStart = null, cycleDay = null, avgCycleLength = 28, avgPeriodLength = 5,
        nextPeriodStart = null, ovulationDay = null, fertileWindow = null, phase = Phase.UNKNOWN
    )
)

class CalendarViewModel(repo: CycleRepository) : ViewModel() {

    private val _month = MutableStateFlow(YearMonth.now())

    val state: StateFlow<CalendarUiState> = combine(
        _month,
        repo.observePeriodDays(),
        repo.observeLoggedDates(),
        repo.observeDayLogs(),
        repo.observePrediction()
    ) { month, periods, loggedDates, dayLogs, prediction ->
        CalendarUiState(
            month = month,
            periodDays = periods.toSet(),
            loggedDates = loggedDates,
            dayLogs = dayLogs,
            prediction = prediction
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    fun prevMonth() { _month.value = _month.value.minusMonths(1) }
    fun nextMonth() { _month.value = _month.value.plusMonths(1) }

    class Factory(private val repo: CycleRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CalendarViewModel(repo) as T
    }
}
