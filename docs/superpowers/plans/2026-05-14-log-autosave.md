# LogScreen 自动保存改造 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 移除"日记录"页面的保存按钮,改为操作即生效的自动保存(chips/流量即点即存,笔记失焦写,DisposableEffect 离场兜底)。

**Architecture:** 改造仅限 `LogViewModel` 与 `LogScreen` 两个文件,以及 `LunaApp` 的调用点。`LogViewModel.save()/ackSaved()` 被替换为隐式写入语义:`setFlow/toggleMood/toggleSymptom` 内部直接 launch 协程写盘;`note` 走"显式 commit"语义,失焦时 ViewModel 对比 `lastPersistedNote` 决定是否写盘。`LogScreen` 用 `Modifier.onFocusChanged` + `DisposableEffect` 触发笔记 commit。

**Tech Stack:** Kotlin、Jetpack Compose、AndroidX ViewModel、Kotlin Coroutines (`viewModelScope`、`NonCancellable`)、JUnit4 + Truth(单元测试)。

**Spec:** `docs/superpowers/specs/2026-05-14-log-autosave-design.md`

---

## 文件清单

- 修改:`app/src/main/java/com/king/luna/ui/screen/log/LogViewModel.kt`
- 修改:`app/src/main/java/com/king/luna/ui/screen/log/LogScreen.kt`
- 修改:`app/src/main/java/com/king/luna/ui/nav/LunaApp.kt`(删除 `onSaved` 实参)
- 新建:`app/src/test/java/com/king/luna/ui/screen/log/LogViewModelAutosaveTest.kt`(只测 VM,Compose UI 不在单测覆盖)

---

## 测试策略

`LogViewModel` 是纯 Kotlin + 协程对象,可用 `runTest` 覆盖。我们针对:

1. 设置流量后 → `repo.setPeriod` 被调用一次
2. 切换心情后 → `repo.setLog` 被调用一次,参数包含新心情
3. 切换症状后 → `repo.setLog` 被调用一次
4. `setNote` 不触发任何写盘
5. `commitNoteIfChanged` 在 note 与 `lastPersistedNote` 不同时调用 `repo.setLog`
6. `commitNoteIfChanged` 在 note 与 `lastPersistedNote` 相同时 **不** 调用 `repo.setLog`(去重)
7. `pickDate` 重新加载后,`commitNoteIfChanged` 以新日期的 note 作为基线

由于项目当前测试代码没有 mock 框架(只有 JUnit + Truth),我们用**手写假 Repo**:继承 `CycleRepository` 不可行(类有 dao 依赖),改为定义最小接口/或者用一个测试专用的 fake 实现。看代码,`CycleRepository` 是 `class` 不是接口,无法直接子类化无副作用 — 但 `LogViewModel` 持有的是具体类型 `CycleRepository`,要测必须传入真对象。

**可行方案**:把 `LogViewModel` 对仓库的依赖**收窄成内部用到的两个 suspend 函数**仍然不行,VM 构造接收的是 `CycleRepository`。

**最务实的方案**:为 `LogViewModel` 引入一个内部 `LogRepoPort` 接口(只列出 VM 用到的 4 个方法),`CycleRepository` 实现它,VM 接收该接口。这样:
- 生产代码零行为变更
- 测试用 fake `LogRepoPort` 实现,完全离线
- 是个**有边界的抽象**,不是"为未来扩展"的过度设计 — 它服务于此处真实的可测试性需求

**但**:这是脱离 spec 的额外结构变更。先在 Task 1 起草前向你确认这个抽象是否可以引入。如果你拒绝,改用**纯集成的 instrumentation 测试**或**只人工验证**,跳过单元测试 task。

---

## 任务清单

### Task 0: 决策点 — 测试方案

- [ ] **Step 0:跟用户确认**

二选一:

- **A.** 引入 `LogRepoPort` 接口让 `LogViewModel` 可单测(改动 Repository 实现一行 `: LogRepoPort`,新增接口文件)
- **B.** 跳过单元测试,直接改代码,人工验证 + 现有 `CalendarLogSummaryTest` 等保护别处不被波及

确认后再进入 Task 1。**默认假设选 A**(下面任务按 A 写;若选 B,删除 Task 1、Task 2、Task 3)。

---

### Task 1: 引入 LogRepoPort 抽象

**Files:**
- Create: `app/src/main/java/com/king/luna/ui/screen/log/LogRepoPort.kt`
- Modify: `app/src/main/java/com/king/luna/data/repo/CycleRepository.kt`(加 `: LogRepoPort`)

- [ ] **Step 1:新建 `LogRepoPort.kt`**

```kotlin
package com.king.luna.ui.screen.log

import com.king.luna.data.entity.DayLogEntity
import com.king.luna.data.entity.PeriodDayEntity
import com.king.luna.domain.model.FlowLevel
import com.king.luna.domain.model.Mood
import com.king.luna.domain.model.Symptom
import java.time.LocalDate

// LogViewModel 仅需要的最小仓库面;便于单元测试替换
interface LogRepoPort {
    suspend fun getPeriodDay(date: LocalDate): PeriodDayEntity?
    suspend fun getDayLog(date: LocalDate): DayLogEntity?
    suspend fun setPeriod(date: LocalDate, flow: FlowLevel)
    suspend fun setLog(date: LocalDate, moods: Set<Mood>, symptoms: Set<Symptom>, note: String)
}
```

- [ ] **Step 2:让 `CycleRepository` 实现该接口**

修改 `CycleRepository` 类签名,从:
```kotlin
class CycleRepository(
    private val periodDayDao: PeriodDayDao,
    private val dayLogDao: DayLogDao,
    private val clock: () -> LocalDate = LocalDate::now
) {
```
改为:
```kotlin
class CycleRepository(
    private val periodDayDao: PeriodDayDao,
    private val dayLogDao: DayLogDao,
    private val clock: () -> LocalDate = LocalDate::now
) : com.king.luna.ui.screen.log.LogRepoPort {
```

`getPeriodDay / getDayLog / setPeriod / setLog` 既有签名已匹配接口,仅需在它们前补 `override`。

- [ ] **Step 3:构建验证**

Run:`./gradlew :app:compileDebugKotlin`
Expected:BUILD SUCCESSFUL

- [ ] **Step 4:Commit**

```bash
git add app/src/main/java/com/king/luna/ui/screen/log/LogRepoPort.kt \
        app/src/main/java/com/king/luna/data/repo/CycleRepository.kt
git commit -m "refactor: 抽出 LogRepoPort 接口,便于 LogViewModel 单测"
```

---

### Task 2: 写 LogViewModel 自动保存测试(失败先行)

**Files:**
- Create: `app/src/test/java/com/king/luna/ui/screen/log/LogViewModelAutosaveTest.kt`

- [ ] **Step 1:写 fake repo + 七项测试**

```kotlin
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
        // 此时 lastPersistedNote 应为 "原内容";note 也是 "原内容"

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
        // 切换后 lastPersistedNote=="新日笔记",当前 note=="新日笔记",应不写

        vm.commitNoteIfChanged()
        dispatcher.scheduler.advanceUntilIdle()

        assertThat(repo.setLogCalls).isEmpty()
    }
}
```

- [ ] **Step 2:验证测试运行并失败**

Run:`./gradlew :app:testDebugUnitTest --tests "com.king.luna.ui.screen.log.LogViewModelAutosaveTest"`

Expected:编译失败 — `LogViewModel` 构造接收 `CycleRepository` 而非 `LogRepoPort`,且 `commitNoteIfChanged` 不存在。这是预期的 RED。

- [ ] **Step 3:Commit**

```bash
git add app/src/test/java/com/king/luna/ui/screen/log/LogViewModelAutosaveTest.kt
git commit -m "test: 加入 LogViewModel 自动保存行为测试(失败先行)"
```

---

### Task 3: 改写 LogViewModel 实现自动保存

**Files:**
- Modify: `app/src/main/java/com/king/luna/ui/screen/log/LogViewModel.kt`(整体重写)

- [ ] **Step 1:整体替换 `LogViewModel.kt`**

```kotlin
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

    // 仅 VM 内部用,作为 note 失焦/离场写盘的去重基线
    private var lastPersistedNote: String = ""

    init { reload(initialDate) }

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

    // 流量切换:即点即存
    fun setFlow(flow: FlowLevel) {
        val s = _state.value.copy(flow = flow)
        _state.value = s
        viewModelScope.launch { repo.setPeriod(s.date, s.flow) }
    }

    // 心情切换:即点即存
    fun toggleMood(mood: Mood) {
        val cur = _state.value.moods
        val next = if (mood in cur) cur - mood else cur + mood
        val s = _state.value.copy(moods = next)
        _state.value = s
        viewModelScope.launch { repo.setLog(s.date, s.moods, s.symptoms, s.note) }
        // 写日志时把当前 note 一并落库,基线随之更新,避免后续失焦无谓再写
        lastPersistedNote = s.note
    }

    // 症状切换:即点即存
    fun toggleSymptom(sym: Symptom) {
        val cur = _state.value.symptoms
        val next = if (sym in cur) cur - sym else cur + sym
        val s = _state.value.copy(symptoms = next)
        _state.value = s
        viewModelScope.launch { repo.setLog(s.date, s.moods, s.symptoms, s.note) }
        lastPersistedNote = s.note
    }

    // 笔记仅更新内存,不写盘
    fun setNote(note: String) { _state.value = _state.value.copy(note = note) }

    // 失焦或页面离开调用;有变化才写;NonCancellable 防止 VM 销毁中协程被取消
    fun commitNoteIfChanged() {
        val s = _state.value
        if (s.note == lastPersistedNote) return
        val noteToWrite = s.note
        lastPersistedNote = noteToWrite
        viewModelScope.launch(NonCancellable) {
            repo.setLog(s.date, s.moods, s.symptoms, noteToWrite)
        }
    }

    class Factory(private val repo: LogRepoPort, private val initialDate: LocalDate = LocalDate.now()) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            LogViewModel(repo, initialDate) as T
    }
}
```

- [ ] **Step 2:跑测试**

Run:`./gradlew :app:testDebugUnitTest --tests "com.king.luna.ui.screen.log.LogViewModelAutosaveTest"`
Expected:7 个测试全部 PASS

- [ ] **Step 3:跑全量单测确认未波及别处**

Run:`./gradlew :app:testDebugUnitTest`
Expected:BUILD SUCCESSFUL,所有原有测试仍 PASS

- [ ] **Step 4:Commit**

```bash
git add app/src/main/java/com/king/luna/ui/screen/log/LogViewModel.kt
git commit -m "feat(log): LogViewModel 改为自动保存语义

- 流量/心情/症状操作即写盘
- 笔记 commitNoteIfChanged 失焦/离场时去重写
- 删除 save/ackSaved/saving/saved 字段"
```

---

### Task 4: 改写 LogScreen 删按钮 + 接焦点变化 + 离场兜底

**Files:**
- Modify: `app/src/main/java/com/king/luna/ui/screen/log/LogScreen.kt`(整体重写)

- [ ] **Step 1:整体替换 `LogScreen.kt`**

```kotlin
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
import androidx.compose.runtime.remember
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

    // restoreState 时 ViewModel 不重建,主动同步日期
    LaunchedEffect(initialDate) { vm.pickDate(initialDate) }

    // 离场兜底:用户没失焦就直接返回时,把笔记落库
    DisposableEffect(Unit) {
        onDispose { vm.commitNoteIfChanged() }
    }

    val dateFmt = remember { DateTimeFormatter.ofPattern("M 月 d 日", Locale.CHINA) }

    // 跟踪笔记输入框上一次的焦点状态,实现"由有焦点 → 无焦点"的边沿触发
    var noteWasFocused = remember { false }

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
                Mood.values().forEach { m ->
                    ToggleChip(
                        text = "${m.emoji} ${m.label}",
                        selected = m in ui.moods,
                        onClick = { vm.toggleMood(m) }
                    )
                }
            }
        }

        SectionCard(title = "症状") {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Symptom.values().forEach { s ->
                    ToggleChip(
                        text = s.label,
                        selected = s in ui.symptoms,
                        onClick = { vm.toggleSymptom(s) }
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
                        // 由 true → false 视为失焦,提交笔记
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
```

- [ ] **Step 2:Compile**

Run:`./gradlew :app:compileDebugKotlin`
Expected:可能因 `LunaApp.kt` 仍传 `onSaved` 而 FAIL — 这是预期,Task 5 修复。

- [ ] **Step 3(暂不 commit)**

继续 Task 5,与导航层调整一并 commit。

---

### Task 5: 删除 LunaApp 中的 onSaved 实参与跳转

**Files:**
- Modify: `app/src/main/java/com/king/luna/ui/nav/LunaApp.kt:105-109`

- [ ] **Step 1:精确替换调用**

把:
```kotlin
            LogScreen(
                repo = container.cycleRepository,
                initialDate = LocalDate.parse(dateStr),
                onSaved = { switchTab(LunaRoutes.CALENDAR) }
            )
```
改为:
```kotlin
            LogScreen(
                repo = container.cycleRepository,
                initialDate = LocalDate.parse(dateStr)
            )
```

- [ ] **Step 2:全工程编译 + 单测**

Run:`./gradlew :app:assembleDebug :app:testDebugUnitTest`
Expected:BUILD SUCCESSFUL,所有单测 PASS。

- [ ] **Step 3:Commit (Task 4 + Task 5 一并)**

```bash
git add app/src/main/java/com/king/luna/ui/screen/log/LogScreen.kt \
        app/src/main/java/com/king/luna/ui/nav/LunaApp.kt
git commit -m "feat(log): LogScreen 删除保存按钮,改用焦点失焦自动保存

- 笔记 OutlinedTextField onFocusChanged 由有→无焦点触发 commit
- DisposableEffect.onDispose 兜底刷盘
- 删除 PrimaryButton/onSaved 形参/键盘控制器
- LunaApp 调用点同步删除 onSaved 与跳转,改由用户自行返回"
```

---

### Task 6: 人工冒烟验证

- [ ] **Step 1:打 Debug 包安装到设备/模拟器**

Run:`./gradlew :app:installDebug`
Expected:BUILD SUCCESSFUL。

- [ ] **Step 2:走查清单**

逐项手动操作,每项必须满足"无任何按钮、无 toast、无跳转":

1. 进入"日历"→ 点击任意一天 → 进入日记录页 ✅ 看不到"保存"按钮
2. 点选/取消 流量 chip,然后返回日历 → 重新进入同一天 → 选中状态被保留
3. 点选/取消 心情 chip,返回再进 → 状态保留
4. 点选/取消 症状 chip,返回再进 → 状态保留
5. 在笔记区输入文字,**点击页面其它区域**(失焦)→ 返回再进 → 笔记保留
6. 在笔记区输入文字,**直接按返回键**(未失焦)→ 重新进入 → 笔记保留(DisposableEffect 兜底)
7. 笔记内容不变情况下重复进出 → 不应出现卡顿或异常(写盘去重)

如全部通过,进入 Step 3。

- [ ] **Step 3:无需新 commit,流程完成**

---

## 自检结论

- ✅ 覆盖 spec 全部行为契约(流量/心情/症状/笔记/离场)
- ✅ 删除 `save/ackSaved/saving/saved` 与按钮 — 与 spec 一致
- ✅ `lastPersistedNote` 去重 — 与 spec 一致
- ✅ `viewModelScope.launch(NonCancellable)` 应对离场取消 — 与 spec 风险条目一致
- ✅ 调用点同步删除 `onSaved` + 跳转(用户决策"自行返回")
- ✅ 类型一致:`LogRepoPort` 4 个方法签名与 `CycleRepository` 现有签名严格匹配
- ✅ 无占位符、无 TBD
- ⚠️ Task 0 是用户确认门槛,不是占位符;它在执行前明确分支
