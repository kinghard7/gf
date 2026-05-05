# Luna 安卓 App · MVP 设计文档

- 日期：2026-05-05
- 原型来源：`/Users/king/work/project/ai/open-design/.od/projects/a47c6951-5e7e-4ef3-91d9-9e9899025e82/index.html`
- 项目目录：`/Users/king/work/project/ai/gf/`
- 包名：`com.king.luna`
- 范围：MVP 三屏（今日 / 日历 / 记录），第二、三批列入路线图

---

## 1. 关键决策（已敲定，避免回头）

| 决策项 | 取值 | 理由 |
|---|---|---|
| 平台 | 原生 Android | 用户已选方案 B |
| UI 框架 | Jetpack Compose | 与原型声明式风格直译最近 |
| 最低 SDK | 26（Android 8.0） | 免 java.time desugaring；覆盖 95%+ |
| 目标 SDK | 34（Android 14） | 商店基线 |
| 语言 | Kotlin | 唯一现代选择 |
| 构建 | Gradle Kotlin DSL + version catalog | 标准做法 |
| 持久化 | Room | 类型安全、Flow 内置 |
| 账号/网络 | **无** | 纯本地、零网络权限 |
| 同步 | **无** | YAGNI；导出/导入留 v2 |
| DI | **不用** | MVP 一个 Repository，手动 wire |
| 导航 | Compose Navigation | 标准 |
| 周期算法 | 最近 6 次实际间隔的算术平均；不足 6 次用现有全部；0 次用默认 28/5 | 教科书做法 |
| 默认周期 | 28 天周期，5 天经期 | 行业默认 |
| 时区/日期 | `java.time.LocalDate`，本地时区 | minSdk 26 直接可用 |
| 应用语言 | 仅中文（zh-CN） | 与原型一致；i18n 是 YAGNI |

---

## 2. MVP 范围

### 包含

- **屏 01 今日（Today）**：周期天数大圆环、当前阶段标签、Luna 提醒文案、下次经期预测、"记录今天的状态"按钮、底部 4-Tab。
- **屏 02 日历（Calendar）**：月历，区分实际经期日（实心）、预测经期日（虚线）、排卵期高亮、今天外框；左右切换月份；点击某天可跳转到该天的记录。
- **屏 03 记录（Log）**：选定一天 → 流量（无/轻/中/重）、心情 Chip 多选、症状标签多选、备注、保存。

### 不包含（明确写下来防止 scope creep）

- 屏 04 周期洞察（柱状图统计）→ **路线图 V2**
- 屏 05 提醒与设置（本地通知）→ **路线图 V2**
- 屏 06 备孕模式（月相 hero）→ **路线图 V3**
- 账号 / 云同步 / 多设备 → **不做**
- 数据导出 / 导入 → **路线图 V2**（用户换机时再做）
- 国际化 / 多语言 → **不做**
- 暗色主题 → **路线图 V2**（warm-soft 在浅色下定义；先复刻浅色）
- 平板 / 折叠屏适配 → **不做**

### 路线图（按优先级，仅作记录，不在本次实施）

- **V2.1**：屏 04 洞察（周期长度 / 经期长度 / 平均流量柱状图）
- **V2.2**：屏 05 提醒（经期前 N 天通知；用 WorkManager + AlarmManager）
- **V2.3**：数据导出（JSON）/ 导入
- **V2.4**：暗色主题
- **V3**：屏 06 备孕模式（高生育力可视化）

---

## 3. 架构（极简分层，单 Module）

```
app/
└── src/main/java/com/king/luna/
    ├── LunaApplication.kt          // 持有 AppContainer 单例（手动 DI）
    ├── MainActivity.kt             // 单 Activity；setContent { LunaApp() }
    ├── data/
    │   ├── db/
    │   │   ├── LunaDatabase.kt     // RoomDatabase
    │   │   ├── PeriodDayDao.kt     // 经期日（流量记录）
    │   │   └── DayLogDao.kt        // 单日记录（心情/症状/备注）
    │   ├── entity/
    │   │   ├── PeriodDayEntity.kt
    │   │   └── DayLogEntity.kt
    │   └── repository/
    │       └── CycleRepository.kt  // 唯一 Repository，所有读写出入口
    ├── domain/
    │   ├── model/
    │   │   ├── Cycle.kt            // 周期值对象
    │   │   ├── DayLog.kt
    │   │   ├── FlowLevel.kt        // enum: NONE, LIGHT, MEDIUM, HEAVY
    │   │   ├── Mood.kt             // enum
    │   │   └── Symptom.kt          // enum
    │   └── cycle/
    │       └── CyclePredictor.kt   // 纯函数：List<经期开始日> → CyclePrediction
    └── ui/
        ├── theme/
        │   ├── Color.kt            // warm-soft 调色板（OKLCH 转 sRGB）
        │   ├── Type.kt             // 字体（系统衬线/无衬线 fallback）
        │   └── Theme.kt            // LunaTheme()
        ├── nav/
        │   └── LunaNavGraph.kt     // 4 个 destination
        ├── today/
        │   ├── TodayScreen.kt
        │   ├── TodayViewModel.kt
        │   └── components/CycleRing.kt
        ├── calendar/
        │   ├── CalendarScreen.kt
        │   ├── CalendarViewModel.kt
        │   └── components/MonthGrid.kt
        ├── log/
        │   ├── LogScreen.kt
        │   ├── LogViewModel.kt
        │   └── components/{ MoodChip.kt, SymptomTag.kt, FlowSelector.kt }
        └── components/
            ├── PrimaryButton.kt
            ├── SecondaryButton.kt
            ├── BottomTabBar.kt
            └── PillBadge.kt
```

### 为什么这样切？

- **单 Module**：MVP 三屏，多 module 是装逼；等代码超过 5000 行再拆。
- **单 Activity + Compose Navigation**：声明式 UI 的标准形态。
- **手动 DI**：`LunaApplication.container` 持有 `Database` + `Repository` 单例。Hilt 在三屏面前是 80% 的样板换来 0% 的好处。
- **`CyclePredictor` 是纯函数**：输入 `List<LocalDate>` 经期开始日列表，输出预测结果。**纯函数无副作用**——单元测试不用 mock 任何东西。
- **ViewModel 用 `StateFlow<UiState>`**：每个屏一个 sealed UiState（Loading / Ready），不搞各种花架子。

---

## 4. 数据模型

### 4.1 设计原则

例假数据本质就两类：

1. **经期日（PeriodDay）**：哪天处于经期 + 当天流量
2. **当日记录（DayLog）**：心情、症状、备注（与经期解耦——平时也能记心情）

**不要**用一张表硬塞所有字段。"今天我没经期但心情糟"是合法状态，把流量和心情塞同一行就要处理一堆 nullable 边界。

### 4.2 表设计

```kotlin
@Entity(tableName = "period_day")
data class PeriodDayEntity(
    @PrimaryKey val date: LocalDate,   // 主键即日期；同一天只能一条
    val flow: FlowLevel                // enum 存 String
)

@Entity(tableName = "day_log")
data class DayLogEntity(
    @PrimaryKey val date: LocalDate,
    val moods: String,                 // CSV: "calm,tired"（Mood enum 名）
    val symptoms: String,              // CSV: "cramps,headache"
    val note: String                   // 默认 ""
)
```

**类型转换器**：`LocalDate ↔ String (ISO_LOCAL_DATE)`，全库一个 `@TypeConverters`。

**为什么 moods/symptoms 用 CSV 而不是关联表？**
枚举值有限（心情 6 个、症状 ~10 个），写表是过度设计。等真要做"按症状统计"时（V2.1）再正规化也不晚。一行一个 String，应用层 split，结束。

### 4.3 周期推导（不存 Cycle 表）

**关键决策：周期不持久化，每次按需从 `period_day` 推导。**

理由：
- 周期定义是"两次经期开始之间的间隔"，是 PeriodDay 的派生数据。
- 持久化派生数据 = 双写 = 不一致 = bug 温床。
- 性能：用户一年最多 12-13 个周期记录，全表扫描排序 < 1ms。

**推导规则（CyclePredictor）**：

```
输入：List<LocalDate> = period_day 中所有 flow != NONE 的日期，升序
步骤：
1. 把日期分组为"经期段"：连续的、间隔 ≤ 1 天的日期归为一段，
   每段的第一天 = "经期开始日"。
2. 经期开始日列表 starts。
3. 周期长度数组：相邻 starts 的天数差，取最近 6 个（不足全取）。
4. 平均周期长度 cycleLen = 数组平均（向下取整）；空数组 → 28。
5. 经期长度数组：每段的天数；取最近 6 个；空数组 → 5。
6. 平均经期长度 periodLen = 同上 → 默认 5。
7. 当前周期开始日 = starts.last()（无记录则 null，UI 显示"开始记录第一次经期"）。
8. 当前周期第几天 = today - 当前周期开始日 + 1。
9. 下次预计经期 = 当前周期开始日 + cycleLen。
10. 排卵日 ≈ 下次预计经期 - 14 天。
11. 高生育力窗口 = 排卵日 ± 2 天。
```

**纯函数签名**：

```kotlin
data class CyclePrediction(
    val cycleStart: LocalDate?,        // 当前周期起点，null = 无历史
    val cycleDay: Int?,                // 第几天（1-based），null = 无历史
    val avgCycleLength: Int,           // 默认 28
    val avgPeriodLength: Int,          // 默认 5
    val nextPeriodStart: LocalDate?,   // null = 无历史
    val ovulationDay: LocalDate?,
    val fertileWindow: ClosedRange<LocalDate>?,
    val phase: Phase                   // PERIOD / FOLLICULAR / OVULATION / LUTEAL / UNKNOWN
)

object CyclePredictor {
    fun predict(periodDays: List<PeriodDayEntity>, today: LocalDate): CyclePrediction
}
```

---

## 5. 屏幕详细设计

### 5.1 屏 01 今日（TodayScreen）

**布局自上而下**：

1. 状态栏（系统自带，不画）
2. Header：左侧"M 月 D 日 · 周X" + "早安/午安/晚安，{昵称}"；右侧通知图标按钮（MVP 无功能，UI 占位）
3. 周期大圆环（自定义 Compose Canvas）：外圈灰底、内圈强调色弧，中央"第 N 天" + 阶段标签
4. Luna 今日提醒卡片（soft 背景）：根据 `phase` 显示文案（见下方文案表）
5. "下一个周期"卡片：预计日期 + "还有 N 天 · 周期 X 天" + 大数字
6. 主按钮"记录今天的状态" → 跳到 LogScreen(today)
7. 底部 Tab（4 项：今天/日历/洞察/我的；MVP 仅"今天""日历"可点，其余灰色禁用 + Toast"V2 即将上线"）

**ViewModel 状态**：

```kotlin
data class TodayUiState(
    val today: LocalDate,
    val prediction: CyclePrediction,
    val nickname: String = "你"          // MVP 写死，V2 从设置读
)
```

**Phase → 文案**（写在 strings.xml 或一个 const map）：

| Phase | 标签 | 提醒文案 |
|---|---|---|
| PERIOD | 经期 · 第 N 天 | 多喝温水，别强撑。 |
| FOLLICULAR | 卵泡期 · 恢复中 | 精力回升期，适合规划重要的事。 |
| OVULATION | 排卵期 · 高生育力 | 今天是本月高生育力。透明拉丝分泌物通常意味排卵临近。 |
| LUTEAL | 黄体期 · 经期前 N 天 | 容易情绪起伏，对自己温柔点。 |
| UNKNOWN | 还没有数据 | 记录一次经期，Luna 才能学会你的节奏。 |

### 5.2 屏 02 日历（CalendarScreen）

**布局**：

1. Header："YYYY 年 M 月" + 左右箭头切换月份；"今天"按钮回到当前月
2. 周次行（一 二 三 四 五 六 日）—— 中文，周一为首
3. 7×N 网格：每个 cell 是一个圆形日期
   - 普通日：透明底
   - 实际经期日（PeriodDay 中存在且 flow ≠ NONE）：强调色实心填充
   - 预测经期日（未来未记录的预测段）：强调色虚线边框
   - 排卵日：accent-soft 背景
   - 今日：黑色 outline
   - 有 DayLog 记录：底部加小圆点
   - 上/下月日：灰色
4. 图例（小字一行）：● 经期  ◌ 预测  · 排卵  · 已记录
5. 点击任意日 → LogScreen(那天)

**ViewModel 状态**：

```kotlin
data class CalendarUiState(
    val visibleMonth: YearMonth,
    val cells: List<DayCell>             // 包含上下月填充以补齐 6 行
)
data class DayCell(
    val date: LocalDate,
    val inMonth: Boolean,
    val flow: FlowLevel?,                // null = 非经期
    val isPredicted: Boolean,
    val isFertile: Boolean,
    val isOvulation: Boolean,
    val isToday: Boolean,
    val hasLog: Boolean
)
```

### 5.3 屏 03 记录（LogScreen）

**布局**：

1. Header：左"<"返回 + "M 月 D 日"标题
2. **流量** Section（grid-4，4 个 chip）：无 / 轻 / 中 / 重 —— 单选；默认值 = 当前 PeriodDay.flow，无记录则 NONE
3. **心情** Section（mood-row）：6 个 chip 横排（icon + 标签）—— 多选
   - 心情枚举：平静(calm) / 开心(happy) / 疲惫(tired) / 烦躁(irritable) / 焦虑(anxious) / 低落(sad)
4. **症状** Section（标签云）：多选
   - 症状枚举：腹痛(cramps) / 头痛(headache) / 腰酸(backache) / 乳房胀(tender_breasts) / 恶心(nausea) / 腹胀(bloating) / 痘痘(acne) / 食欲增(craving) / 失眠(insomnia) / 嗜睡(sleepy)
5. **备注** Section：多行 TextField（200 字限制）
6. 底部固定按钮："保存" → 写库 + 弹 Snackbar"已保存" + 返回

**ViewModel 状态**：

```kotlin
data class LogUiState(
    val date: LocalDate,
    val flow: FlowLevel,
    val moods: Set<Mood>,
    val symptoms: Set<Symptom>,
    val note: String,
    val saving: Boolean = false
)
```

**保存逻辑**：

```kotlin
suspend fun save() {
    if (flow == FlowLevel.NONE) {
        repo.deletePeriodDay(date)        // 取消当天经期
    } else {
        repo.upsertPeriodDay(date, flow)
    }
    if (moods.isEmpty() && symptoms.isEmpty() && note.isBlank()) {
        repo.deleteDayLog(date)
    } else {
        repo.upsertDayLog(date, moods, symptoms, note)
    }
}
```

**关键点**：保存空记录 = 删除，避免脏数据。

---

## 6. 视觉风格映射（HTML → Compose）

| 原型 token | Compose 实现 |
|---|---|
| `--bg`（暖米白） | `Color(0xFFFAF7F2)` |
| `--surface`（近白） | `Color(0xFFFEFCF8)` |
| `--fg`（深棕） | `Color(0xFF2A211B)` |
| `--muted`（中棕灰） | `Color(0xFF7A6F65)` |
| `--border`（暖灰） | `Color(0xFFE8E1D5)` |
| `--accent`（赤陶红） | `Color(0xFFC5634A)` |
| `--accent-soft` | `accent.copy(alpha = 0.14f)` |
| 圆角卡片 18px | `RoundedCornerShape(18.dp)` |
| 显示字体 Tiempos/Newsreader | 系统衬线（`FontFamily.Serif`） |
| 正文字体 Söhne | 系统无衬线（默认） |
| 等宽字体 | `FontFamily.Monospace` |

**OKLCH 转 sRGB**：原型用 OKLCH，但 Compose 不支持。一次性手动换算成上述 Hex 值，写死在 `Color.kt`。不引入 OKLCH 库。

**字体方案**：MVP 不打包自定义字体（避免 ~3MB APK 增量）。系统衬线在中文下渲染为"宋体"，足够温柔感。V2 再考虑打包 Newsreader 衬线英文 + 思源宋体子集。

---

## 7. 错误处理

**MVP 的错误极少**（无网络、无外部依赖），但要列出：

| 场景 | 处理 |
|---|---|
| Room 写失败 | try-catch；Snackbar"保存失败，请重试"；不崩溃 |
| Room 读失败 | UiState 显示空态，不崩溃 |
| 启动时数据库不存在 | Room 自动建库；首次进入显示"还没有数据"引导 |
| 日期序列化失败 | TypeConverter 内部处理；不可能失败（ISO 格式） |
| 用户在 LogScreen 选择了未来日期？| 允许（用户预测经期开始）；今日之后的"流量"语义上有点怪，但不阻止——尊重用户判断 |

**故意不处理**：

- 没有"加载状态"骨架屏（MVP 数据量小，Flow 首发 < 50ms）
- 没有重试机制（本地操作失败重试也是失败）
- 没有崩溃上报（隐私优先，不接 Crashlytics）

---

## 8. 测试策略

### 必须有

- **`CyclePredictorTest`（JVM 单元测试）**：纯函数最容易测，覆盖：
  - 0 条记录 → 默认 28/5
  - 1 段经期 → cycleStart 正确，nextPeriod = start + 28
  - 3 段经期间隔 [27,29,28] → avgCycleLength = 28
  - 7 段经期间隔（最近 6 个 [25,26,27,28,29,30] = 平均 27.5 → 27）→ 验证只取最近 6
  - 跨年边界（12 月 28 日 + 28 天 = 1 月 25 日）
  - 经期内每天 phase = PERIOD
  - 排卵日 = 下次经期 - 14
- **`CycleRepositoryTest`（instrumented，使用 Room in-memory db）**：upsert、delete、Flow 发射

### 不做（MVP）

- UI 测试（Compose UI Test）→ 收益低、维护成本高，V2 再说
- 端到端测试 → 没意义，三屏手测一遍 5 分钟

---

## 9. 构建与发布

- **Gradle 版本**：8.7+
- **AGP**：8.5+
- **Kotlin**：2.0+（K2 编译器）
- **Compose BOM**：2024.09 或更新
- **Java target**：17
- **签名**：MVP 阶段只生成 debug APK；release 签名等真要分发再做（避免 keystore 误提交风险）
- **本地仓库**：使用 `/Users/king/work/devsoft/maven/repository` 与 `/Users/king/work/devsoft/maven/apache-maven-3.9.9/conf/setting.xml`（用户全局规则）—— Gradle 读取本地 Maven 镜像
- **运行验证**：`./gradlew assembleDebug` 生成 APK 即视为 MVP 完成；ADB 安装到真机验收

---

## 10. 已明确不做的事（防止 scope creep）

- ❌ 后端 / 服务器
- ❌ 用户账号 / 登录
- ❌ 云同步 / 多设备
- ❌ 健康平台集成（Health Connect / Google Fit）
- ❌ 通知与提醒
- ❌ 数据导出
- ❌ 多语言
- ❌ 暗色主题
- ❌ 平板适配
- ❌ Wear OS
- ❌ 备孕模式 / 月相 hero
- ❌ 周期洞察图表
- ❌ Hilt / Dagger
- ❌ DataStore（无设置项需要持久化；MVP 全部状态在 Room）
- ❌ KSP 之外的代码生成
- ❌ Crashlytics / Analytics

---

## 11. 验收标准（MVP 完成定义）

1. ✅ `./gradlew assembleDebug` 成功产出 APK
2. ✅ APK 装到 Android 8.0+ 真机/模拟器可启动到今日页
3. ✅ 三屏 UI 视觉与原型偏差可接受（颜色、字号、间距、形状）
4. ✅ 在记录页录入一段 5 天经期，今日页圆环、阶段、下次预测均更新
5. ✅ 日历页正确高亮经期日 + 预测下个月经期日
6. ✅ 杀掉进程重开，所有数据保留
7. ✅ `CyclePredictorTest` 全部通过
8. ✅ 启动到今日页 < 1.5s（中端设备）
9. ✅ 无网络权限申请（`AndroidManifest.xml` 不含 INTERNET）

---

## 12. 路线图（仅记录，不在本次实施）

| 版本 | 内容 |
|---|---|
| V1.0 (本次) | 屏 01/02/03，纯本地，无通知 |
| V2.1 | 屏 04 周期洞察（柱状图、平均值统计） |
| V2.2 | 屏 05 提醒（经期前通知，本地，WorkManager） |
| V2.3 | 数据导出（JSON）/ 导入 |
| V2.4 | 暗色主题 |
| V3.0 | 屏 06 备孕模式（高生育力可视化、月相 hero） |

---

> Talk is cheap. 接下来由 writing-plans 技能将本文档展开为可执行的实施计划。
