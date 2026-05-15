package com.king.luna.ui.screen.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.king.luna.data.repo.toMoodSet
import com.king.luna.data.repo.toSymptomSet
import com.king.luna.domain.model.FlowLevel
import com.king.luna.domain.model.Mood
import com.king.luna.domain.model.Symptom
import kotlinx.coroutines.NonCancellable
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
    val note: String = ""
)

class LogViewModel(private val repo: LogRepoPort, initialDate: LocalDate) : ViewModel() {

    private val _state = MutableStateFlow(LogUiState())
    val state: StateFlow<LogUiState> = _state.asStateFlow()

    // 笔记用基线去重；否则失焦和离场会重复写库。
    private var lastPersistedNote: String = ""

    init {
        reload(initialDate)
    }

    fun pickDate(date: LocalDate) = reload(date)

    private fun reload(date: LocalDate) {
        viewModelScope.launch {
            val period = repo.getPeriodDay(date)
            val log = repo.getDayLog(date)
            val note = log?.note ?: ""
            _state.value = LogUiState(
                date = date,
                flow = period?.flow ?: FlowLevel.NONE,
                moods = log?.moods?.toMoodSet() ?: emptySet(),
                symptoms = log?.symptoms?.toSymptomSet() ?: emptySet(),
                note = note
            )
            lastPersistedNote = note
        }
    }

    fun setFlow(flow: FlowLevel) {
        val next = _state.value.copy(flow = flow)
        _state.value = next
        viewModelScope.launch {
            repo.setPeriod(next.date, next.flow)
        }
    }

    fun toggleMood(mood: Mood) {
        val current = _state.value.moods
        val moods = if (mood in current) current - mood else current + mood
        val next = _state.value.copy(moods = moods)
        _state.value = next
        lastPersistedNote = next.note
        viewModelScope.launch {
            repo.setLog(next.date, next.moods, next.symptoms, next.note)
        }
    }

    fun toggleSymptom(symptom: Symptom) {
        val current = _state.value.symptoms
        val symptoms = if (symptom in current) current - symptom else current + symptom
        val next = _state.value.copy(symptoms = symptoms)
        _state.value = next
        lastPersistedNote = next.note
        viewModelScope.launch {
            repo.setLog(next.date, next.moods, next.symptoms, next.note)
        }
    }

    fun setNote(note: String) {
        _state.value = _state.value.copy(note = note)
    }

    fun commitNoteIfChanged() {
        val current = _state.value
        if (current.note == lastPersistedNote) return

        val note = current.note
        lastPersistedNote = note
        viewModelScope.launch(NonCancellable) {
            repo.setLog(current.date, current.moods, current.symptoms, note)
        }
    }

    class Factory(
        private val repo: LogRepoPort,
        private val initialDate: LocalDate = LocalDate.now()
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LogViewModel(repo, initialDate) as T
    }
}
