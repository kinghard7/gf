package com.king.luna.ui.screen.insight

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.king.luna.data.repo.CycleRepository
import com.king.luna.domain.insight.InsightStats
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class InsightViewModel(repo: CycleRepository) : ViewModel() {

    val state: StateFlow<InsightStats> = repo.observeInsight()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = InsightStats.from(emptyList())
        )

    class Factory(private val repo: CycleRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            InsightViewModel(repo) as T
    }
}
