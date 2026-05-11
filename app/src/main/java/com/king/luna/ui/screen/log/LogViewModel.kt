package com.king.luna.ui.screen.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.king.luna.data.repo.CycleRepository
import com.king.luna.data.repo.toMoodSet
import com.king.luna.data.repo.toSymptomSet
import com.king.luna.domain.model.FlowLevel
import com.king.luna.domain.model.Mood
import com.king.luna.domain.model.Symptom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class LogUiState(
    val date: LocalDate = LocalDate.now(),
    val flow: FlowLevel = FlowLevel.NONE,
    val moods: Set<Mood> = emptySet(),
    val symptoms: Set<Symptom> = emptySet(),
    val note: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false       // 一次性标志，UI 用完置回
)

class LogViewModel(private val repo: CycleRepository, initialDate: LocalDate) : ViewModel() {

    private val _state = MutableStateFlow(LogUiState())
    val state: StateFlow<LogUiState> = _state.asStateFlow()

    init { reload(initialDate) }

    fun pickDate(date: LocalDate) = reload(date)

    private fun reload(date: LocalDate) {
        viewModelScope.launch {
            val period = repo.getPeriodDay(date)
            val log = repo.getDayLog(date)
            _state.value = LogUiState(
                date = date,
                flow = period?.flow ?: FlowLevel.NONE,
                moods = log?.moods?.toMoodSet() ?: emptySet(),
                symptoms = log?.symptoms?.toSymptomSet() ?: emptySet(),
                note = log?.note ?: ""
            )
        }
    }

    fun setFlow(flow: FlowLevel) { _state.value = _state.value.copy(flow = flow) }

    fun toggleMood(mood: Mood) {
        val cur = _state.value.moods
        _state.value = _state.value.copy(
            moods = if (mood in cur) cur - mood else cur + mood
        )
    }

    fun toggleSymptom(s: Symptom) {
        val cur = _state.value.symptoms
        _state.value = _state.value.copy(
            symptoms = if (s in cur) cur - s else cur + s
        )
    }

    fun setNote(note: String) { _state.value = _state.value.copy(note = note) }

    fun save() {
        val s = _state.value
        viewModelScope.launch {
            _state.value = s.copy(saving = true)
            repo.setPeriod(s.date, s.flow)
            repo.setLog(s.date, s.moods, s.symptoms, s.note)
            _state.value = _state.value.copy(saving = false, saved = true)
        }
    }

    fun ackSaved() { _state.value = _state.value.copy(saved = false) }

    class Factory(private val repo: CycleRepository, private val initialDate: LocalDate = LocalDate.now()) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LogViewModel(repo, initialDate) as T
    }
}
