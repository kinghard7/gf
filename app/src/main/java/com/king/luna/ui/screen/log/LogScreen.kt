package com.king.luna.ui.screen.log

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.king.luna.data.repo.CycleRepository
import com.king.luna.domain.model.Mood
import com.king.luna.domain.model.Symptom
import com.king.luna.ui.theme.LunaCardShape
import com.king.luna.ui.theme.LunaColors
import com.king.luna.ui.theme.lunaCard
import com.king.luna.ui.theme.lunaHeaderStyle
import com.king.luna.ui.theme.lunaMetaStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun LogScreen(repo: CycleRepository, initialDate: LocalDate = LocalDate.now()) {
    val vm: LogViewModel = viewModel(factory = LogViewModel.Factory(repo, initialDate))
    val ui by vm.state.collectAsState()

    // restoreState 会复用 ViewModel，日期必须显式同步。
    LaunchedEffect(initialDate) {
        vm.pickDate(initialDate)
    }

    // 未失焦直接离开页面时，仍然要把笔记刷盘。
    DisposableEffect(Unit) {
        onDispose { vm.commitNoteIfChanged() }
    }

    val dateFmt = remember { DateTimeFormatter.ofPattern("M 月 d 日", Locale.CHINA) }
    var noteWasFocused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("DAILY LOG", style = lunaMetaStyle())
        Text(ui.date.format(dateFmt), style = lunaHeaderStyle())

        SectionCard(title = "流量") {
            FlowSelector(selected = ui.flow, onSelect = vm::setFlow)
        }

        SectionCard(title = "心情") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Mood.values().forEach { mood ->
                    ToggleChip(
                        text = "${mood.emoji} ${mood.label}",
                        selected = mood in ui.moods,
                        onClick = { vm.toggleMood(mood) }
                    )
                }
            }
        }

        SectionCard(title = "症状") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Symptom.values().forEach { symptom ->
                    ToggleChip(
                        text = symptom.label,
                        selected = symptom in ui.symptoms,
                        onClick = { vm.toggleSymptom(symptom) }
                    )
                }
            }
        }

        SectionCard(title = "笔记") {
            OutlinedTextField(
                value = ui.note,
                onValueChange = vm::setNote,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .onFocusChanged { focusState ->
                        if (noteWasFocused && !focusState.isFocused) {
                            vm.commitNoteIfChanged()
                        }
                        noteWasFocused = focusState.isFocused
                    },
                placeholder = { Text("写点什么…") }
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .lunaCard()
            .background(LunaColors.card, LunaCardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = lunaMetaStyle())
        content()
    }
}
