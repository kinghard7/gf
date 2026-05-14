# LogScreen 自动保存改造设计

日期:2026-05-14
范围:`app/src/main/java/com/king/luna/ui/screen/log/`

## 目标
移除日记录页面的"保存"按钮。所有变更操作即生效:点击型控件即点即存,文本输入框失焦即存,页面离开兜底刷盘。

## 行为契约

| 控件 | 触发时机 | 写入路径 |
|---|---|---|
| 流量 (`FlowSelector`) | 点击瞬间 | `repo.setPeriod(date, flow)` |
| 心情 (`Mood` chips) | 点击瞬间 | `repo.setLog(date, moods, symptoms, note)` |
| 症状 (`Symptom` chips) | 点击瞬间 | `repo.setLog(...)` |
| 笔记 (`note` TextField) | 失焦时,且内容相对上次落库有变化 | `repo.setLog(...)` |
| 离开页面 | `DisposableEffect.onDispose` 兜底 flush | `repo.setLog(...)` |

无 toast、无 snackbar、无"已保存"状态文字。完全静默。

## 数据结构

`LogUiState` 删除 `saving` 与 `saved` 两个一次性标志。最终结构:

```kotlin
data class LogUiState(
    val date: LocalDate = LocalDate.now(),
    val flow: FlowLevel = FlowLevel.NONE,
    val moods: Set<Mood> = emptySet(),
    val symptoms: Set<Symptom> = emptySet(),
    val note: String = ""
)
```

新增 `LogViewModel` 私有字段 `lastPersistedNote: String`,只用于失焦/离场去重,不进 UI state。

## ViewModel 变化

- 删除:`save()`、`ackSaved()`
- `setFlow(flow)`:更新状态后立刻 `viewModelScope.launch { repo.setPeriod(...) }`
- `toggleMood(m)` / `toggleSymptom(s)`:更新状态后立刻 `viewModelScope.launch { repo.setLog(...) }`
- `setNote(note)`:仅更新内存,不写盘
- 新增 `commitNoteIfChanged()`:对比 `lastPersistedNote`,不同才写盘,写完更新缓存。为应对页面离开时 VM 即将销毁的取消问题,用 `viewModelScope.launch(NonCancellable)` 启动
- `reload(date)` 完成后同步 `lastPersistedNote = log?.note ?: ""`

## UI 变化 (`LogScreen.kt`)

- 删除 `PrimaryButton` (原 110-116 行)
- 删除 `LaunchedEffect(ui.saved) { ... }` 与 `onSaved` 形参
- 删除 `LocalSoftwareKeyboardController` 局部变量(已无 `keyboard.hide()` 调用方)
- 笔记 `OutlinedTextField` 加 `Modifier.onFocusChanged`:有焦点 → 无焦点时调用 `vm.commitNoteIfChanged()`
- 在 Composable 顶层加 `DisposableEffect(Unit) { onDispose { vm.commitNoteIfChanged() } }`,作为页面离开时的兜底

## 调用方影响

需要 grep 全工程对 `LogScreen(` 的调用点,删除 `onSaved` 实参。预计调用点位于 `LunaApp.kt` / `LunaRoutes.kt`。

## 不在范围

- 不改 `CycleRepository` 接口
- 不引入防抖/批量写
- 不改 chips、`FlowSelector`、`OutlinedTextField` 的视觉
- 不动其它屏幕(`Notification`、`Calendar`、`Today` 等)

## 风险与权衡

- **频繁写盘**:用户狂点 chips 会触发多次 `setLog`。Room 单机写入开销可忽略,IO 协程不阻塞 UI,可接受。
- **离场 flush 取消问题**:`onDispose` 触发时 VM 可能正在被销毁,普通 `viewModelScope.launch` 协程会被取消导致写丢失。对策:`commitNoteIfChanged` 内部用 `viewModelScope.launch(NonCancellable)`。Room 写盘极快,通常在 VM 清理前完成。
- **去掉视觉反馈的认知负担**:用户可能不确定"是否真的存了"。这是产品取舍 — "无按钮"本身就是契约,与 Apple Notes 同构。
