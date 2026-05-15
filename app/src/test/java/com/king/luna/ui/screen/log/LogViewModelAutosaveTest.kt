package com.king.luna.ui.screen.log

import com.google.common.truth.Truth.assertThat
import com.king.luna.data.entity.DayLogEntity
import com.king.luna.data.entity.PeriodDayEntity
import com.king.luna.domain.model.FlowLevel
import com.king.luna.domain.model.Mood
import com.king.luna.domain.model.Symptom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class LogViewModelAutosaveTest {

    private val dispatcher = StandardTestDispatcher()
    private val today = LocalDate.parse("2026-05-14")

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private class FakeRepo : LogRepoPort {
        var periodDay: PeriodDayEntity? = null
        var dayLog: DayLogEntity? = null
        val setPeriodCalls = mutableListOf<Pair<LocalDate, FlowLevel>>()
        data class LogCall(val date: LocalDate, val moods: Set<Mood>, val symptoms: Set<Symptom>, val note: String)
        val setLogCalls = mutableListOf<LogCall>()

        override suspend fun getPeriodDay(date: LocalDate) = periodDay
        override suspend fun getDayLog(date: LocalDate) = dayLog
        override suspend fun setPeriod(date: LocalDate, flow: FlowLevel) { setPeriodCalls += date to flow }
        override suspend fun setLog(date: LocalDate, moods: Set<Mood>, symptoms: Set<Symptom>, note: String) {
            setLogCalls += LogCall(date, moods, symptoms, note)
        }
    }

    @Test fun `setFlow 自动写经期`() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = LogViewModel(repo, today)
        dispatcher.scheduler.advanceUntilIdle()

        vm.setFlow(FlowLevel.MEDIUM)
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(repo.setPeriodCalls).containsExactly(today to FlowLevel.MEDIUM)
    }

    @Test fun `toggleMood 自动写日志`() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = LogViewModel(repo, today)
        dispatcher.scheduler.advanceUntilIdle()

        vm.toggleMood(Mood.HAPPY)
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(repo.setLogCalls).hasSize(1)
        assertThat(repo.setLogCalls[0].moods).containsExactly(Mood.HAPPY)
    }

    @Test fun `toggleSymptom 自动写日志`() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = LogViewModel(repo, today)
        dispatcher.scheduler.advanceUntilIdle()

        vm.toggleSymptom(Symptom.CRAMPS)
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(repo.setLogCalls).hasSize(1)
        assertThat(repo.setLogCalls[0].symptoms).containsExactly(Symptom.CRAMPS)
    }

    @Test fun `setNote 不触发写盘`() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = LogViewModel(repo, today)
        dispatcher.scheduler.advanceUntilIdle()

        vm.setNote("打字中…")
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(repo.setLogCalls).isEmpty()
    }

    @Test fun `commitNoteIfChanged 内容变化时写盘`() = runTest(dispatcher) {
        val repo = FakeRepo()
        val vm = LogViewModel(repo, today)
        dispatcher.scheduler.advanceUntilIdle()

        vm.setNote("有变化")
        vm.commitNoteIfChanged()
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(repo.setLogCalls).hasSize(1)
        assertThat(repo.setLogCalls[0].note).isEqualTo("有变化")
    }

    @Test fun `commitNoteIfChanged 内容未变时不写盘`() = runTest(dispatcher) {
        val repo = FakeRepo().apply {
            dayLog = DayLogEntity(today, "", "", "原内容")
        }
        val vm = LogViewModel(repo, today)
        dispatcher.scheduler.advanceUntilIdle()

        vm.commitNoteIfChanged()
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(repo.setLogCalls).isEmpty()
    }

    @Test fun `pickDate 后 commit 以新日期基线`() = runTest(dispatcher) {
        val repo = FakeRepo().apply {
            dayLog = DayLogEntity(today, "", "", "旧日笔记")
        }
        val vm = LogViewModel(repo, today)
        dispatcher.scheduler.advanceUntilIdle()

        val newDay = LocalDate.parse("2026-05-15")
        repo.dayLog = DayLogEntity(newDay, "", "", "新日笔记")
        vm.pickDate(newDay)
        dispatcher.scheduler.advanceUntilIdle()

        vm.commitNoteIfChanged()
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(repo.setLogCalls).isEmpty()
    }
}
