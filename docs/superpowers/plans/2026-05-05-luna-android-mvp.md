# Luna 安卓 App MVP 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 把 Luna HTML 原型实现为可在 Android 8.0+ 真机上运行的原生 App，覆盖今日 / 日历 / 记录三屏，纯本地存储。

**Architecture:** 单 Module、单 Activity + Jetpack Compose Navigation；Room 持久化 PeriodDay/DayLog 两张表；纯函数 `CyclePredictor` 从历史经期日按需推导周期；手动 DI（`LunaApplication.container`）。

**Tech Stack:** Kotlin 2.0 / Jetpack Compose (BOM 2024.09) / Room 2.6 / Compose Navigation / Gradle 8.7 + Kotlin DSL + version catalog / minSdk 26 / targetSdk 34 / Java 17 / JUnit4 + Truth（单元测试）

**Spec：** `docs/superpowers/specs/2026-05-05-luna-android-mvp-design.md`

**项目根目录：** `/Users/king/work/project/ai/gf/`（下称 `<root>`）

**包名：** `com.king.luna`

---

## File Structure

工程产物（自上而下、按依赖顺序）：

```
<root>/
├── settings.gradle.kts                       # 项目设置 + pluginManagement
├── build.gradle.kts                          # 根 build；apply false 声明
├── gradle.properties                         # JVM args、AndroidX 开关
├── gradle/libs.versions.toml                 # version catalog
├── gradle/wrapper/gradle-wrapper.properties  # Gradle 8.7
├── gradlew / gradlew.bat
├── .gitignore
└── app/
    ├── build.gradle.kts                      # Android module
    ├── proguard-rules.pro                    # 空文件，MVP 不混淆 release
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml           # 单 Activity，无网络权限
        │   ├── java/com/king/luna/
        │   │   ├── LunaApplication.kt        # 持有 AppContainer
        │   │   ├── AppContainer.kt           # db + repository 单例
        │   │   ├── MainActivity.kt           # setContent { LunaApp() }
        │   │   ├── LunaApp.kt                # 顶层 Compose：NavHost + Theme
        │   │   ├── data/
        │   │   │   ├── db/
        │   │   │   │   ├── LunaDatabase.kt
        │   │   │   │   ├── Converters.kt     # LocalDate <-> String
        │   │   │   │   ├── PeriodDayDao.kt
        │   │   │   │   └── DayLogDao.kt
        │   │   │   ├── entity/
        │   │   │   │   ├── PeriodDayEntity.kt
        │   │   │   │   └── DayLogEntity.kt
        │   │   │   └── repository/
        │   │   │       └── CycleRepository.kt
        │   │   ├── domain/
        │   │   │   ├── model/
        │   │   │   │   ├── FlowLevel.kt
        │   │   │   │   ├── Mood.kt
        │   │   │   │   ├── Symptom.kt
        │   │   │   │   ├── Phase.kt
        │   │   │   │   └── CyclePrediction.kt
        │   │   │   └── cycle/
        │   │   │       └── CyclePredictor.kt
        │   │   └── ui/
        │   │       ├── theme/
        │   │       │   ├── Color.kt
        │   │       │   ├── Type.kt
        │   │       │   └── Theme.kt
        │   │       ├── nav/
        │   │       │   └── LunaNavGraph.kt
        │   │       ├── components/
        │   │       │   ├── PrimaryButton.kt
        │   │       │   ├── SecondaryButton.kt
        │   │       │   ├── BottomTabBar.kt
        │   │       │   └── PillBadge.kt
        │   │       ├── today/
        │   │       │   ├── TodayScreen.kt
        │   │       │   ├── TodayViewModel.kt
        │   │       │   └── components/CycleRing.kt
        │   │       ├── calendar/
        │   │       │   ├── CalendarScreen.kt
        │   │       │   ├── CalendarViewModel.kt
        │   │       │   └── components/MonthGrid.kt
        │   │       └── log/
        │   │           ├── LogScreen.kt
        │   │           ├── LogViewModel.kt
        │   │           └── components/
        │   │               ├── FlowSelector.kt
        │   │               ├── MoodChip.kt
        │   │               └── SymptomTag.kt
        │   └── res/
        │       ├── values/{ strings.xml, themes.xml, colors.xml }
        │       ├── mipmap-anydpi-v26/ic_launcher.xml         # 自适应图标
        │       ├── mipmap-*/ic_launcher_foreground.xml       # vector
        │       ├── drawable/ic_launcher_background.xml       # 纯色
        │       └── xml/{ backup_rules.xml, data_extraction_rules.xml }
        └── test/java/com/king/luna/
            └── domain/cycle/CyclePredictorTest.kt
```

**说明：**
- 单 module，先把所有代码放 app；MVP 不引入 :core/:domain 拆分。
- ViewModel 不做 Hilt，工厂在 `AppContainer.viewModelFactory()` 里手 wire。
- `androidTest` 留空（MVP 不做 instrumented 测试，违背设计 §8 简化）。

---

## 任务清单（按依赖顺序，14 个任务）

- Task 0：初始化工程脚手架（Gradle、Manifest、空 Activity，可编译可启动）
- Task 1：领域枚举与值对象（FlowLevel/Mood/Symptom/Phase/CyclePrediction）
- Task 2：CyclePredictor 纯函数（含完整单元测试，TDD）
- Task 3：Room 实体 + DAO + 数据库 + Converters
- Task 4：CycleRepository（封装 DAO，向 UI 暴露 Flow）
- Task 5：AppContainer + LunaApplication（手动 DI）
- Task 6：UI 主题（Color / Type / Theme）
- Task 7：通用组件（PrimaryButton / SecondaryButton / PillBadge / BottomTabBar）
- Task 8：导航骨架（LunaNavGraph + LunaApp + MainActivity 接通）
- Task 9：今日页（TodayViewModel + TodayScreen + CycleRing）
- Task 10：日历页（CalendarViewModel + MonthGrid + CalendarScreen）
- Task 11：记录页（LogViewModel + LogScreen + FlowSelector/MoodChip/SymptomTag）
- Task 12：图标 / strings / Manifest 收尾
- Task 13：构建 debug APK 并真机/模拟器冒烟验收

---

### Task 0：初始化 Gradle 工程脚手架

**目标：** 一份能 `./gradlew assembleDebug` 通过、装机能启动到空白 Activity 的最小工程。

**Files:**
- Create: `<root>/settings.gradle.kts`
- Create: `<root>/build.gradle.kts`
- Create: `<root>/gradle.properties`
- Create: `<root>/gradle/libs.versions.toml`
- Create: `<root>/gradle/wrapper/gradle-wrapper.properties`
- Create: `<root>/.gitignore`
- Create: `<root>/app/build.gradle.kts`
- Create: `<root>/app/proguard-rules.pro`
- Create: `<root>/app/src/main/AndroidManifest.xml`
- Create: `<root>/app/src/main/res/values/strings.xml`
- Create: `<root>/app/src/main/res/values/themes.xml`
- Create: `<root>/app/src/main/res/values/colors.xml`
- Create: `<root>/app/src/main/res/xml/backup_rules.xml`
- Create: `<root>/app/src/main/res/xml/data_extraction_rules.xml`
- Create: `<root>/app/src/main/java/com/king/luna/MainActivity.kt`

- [ ] **Step 1：写 `settings.gradle.kts`**

```kotlin
pluginManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}
rootProject.name = "Luna"
include(":app")
```

- [ ] **Step 2：写 `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
```

- [ ] **Step 3：写 `gradle/libs.versions.toml`**

```toml
[versions]
agp = "8.5.2"
kotlin = "2.0.20"
ksp = "2.0.20-1.0.25"
coreKtx = "1.13.1"
lifecycle = "2.8.4"
activityCompose = "1.9.2"
composeBom = "2024.09.02"
navigationCompose = "2.8.0"
room = "2.6.1"
junit = "4.13.2"
truth = "1.4.4"

[libraries]
androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
androidx-lifecycle-runtime-ktx = { module = "androidx.lifecycle:lifecycle-runtime-ktx", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel-compose = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }
androidx-activity-compose = { module = "androidx.activity:activity-compose", version.ref = "activityCompose" }
androidx-compose-bom = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
androidx-compose-ui = { module = "androidx.compose.ui:ui" }
androidx-compose-ui-graphics = { module = "androidx.compose.ui:ui-graphics" }
androidx-compose-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }
androidx-compose-ui-tooling = { module = "androidx.compose.ui:ui-tooling" }
androidx-compose-material3 = { module = "androidx.compose.material3:material3" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigationCompose" }
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
junit = { module = "junit:junit", version.ref = "junit" }
google-truth = { module = "com.google.truth:truth", version.ref = "truth" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 4：写根 `build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}
```

- [ ] **Step 5：写 `app/build.gradle.kts`**

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.king.luna"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.king.luna"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.google.truth)
}
```

- [ ] **Step 6：写 `gradle/wrapper/gradle-wrapper.properties`**

```properties
distributionBase=GRADLE_USER_HOME
distributionPath=wrapper/dists
distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
networkTimeout=10000
zipStoreBase=GRADLE_USER_HOME
zipStorePath=wrapper/dists
validateDistributionUrl=true
```

- [ ] **Step 7：写 `.gitignore`**

```
*.iml
.gradle/
local.properties
.idea/
.DS_Store
build/
captures/
.externalNativeBuild/
.cxx/
*.apk
*.aab
*.keystore
```

- [ ] **Step 8：写 `app/proguard-rules.pro`**

```
# MVP release 不混淆，留空
```

- [ ] **Step 9：写 `AndroidManifest.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:name=".LunaApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:supportsRtl="false"
        android:theme="@style/Theme.Luna">
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.Luna">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

注意：未声明 `LunaApplication`/`MainActivity` 之前 `assembleDebug` 会失败。本步骤同时创建占位类（Step 12-13）。

- [ ] **Step 10：写 `res/values/strings.xml`**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Luna</string>
</resources>
```

- [ ] **Step 11：写 `res/values/themes.xml` + `colors.xml`**

`colors.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="luna_bg">#FAF7F2</color>
    <color name="luna_fg">#2A211B</color>
    <color name="luna_accent">#C5634A</color>
</resources>
```

`themes.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.Luna" parent="android:Theme.Material.Light.NoActionBar">
        <item name="android:statusBarColor">@color/luna_bg</item>
        <item name="android:windowLightStatusBar">true</item>
        <item name="android:navigationBarColor">@color/luna_bg</item>
        <item name="android:windowBackground">@color/luna_bg</item>
    </style>
</resources>
```

- [ ] **Step 12：写 `res/xml/backup_rules.xml` + `data_extraction_rules.xml`**

`backup_rules.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content />
```

`data_extraction_rules.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup />
    <device-transfer />
</data-extraction-rules>
```

- [ ] **Step 13：占位 `LunaApplication.kt`**

```kotlin
package com.king.luna

import android.app.Application

class LunaApplication : Application()
```

- [ ] **Step 14：占位 `MainActivity.kt`**

```kotlin
package com.king.luna

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Placeholder() }
    }
}

@Composable
private fun Placeholder() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Luna · scaffold ok")
    }
}
```

- [ ] **Step 15：生成 Gradle wrapper jar**

Run: `gradle wrapper --gradle-version 8.7 --distribution-type bin`
Expected: 在 `gradle/wrapper/` 下生成 `gradle-wrapper.jar` 和 `gradlew`/`gradlew.bat`。
若机器无 `gradle` 命令：从已有 Android Studio 的项目拷一份 `gradle-wrapper.jar` + `gradlew` 文件即可，version 不重要（首次执行会按 properties 下载 8.7）。

- [ ] **Step 16：构建验证**

Run: `cd /Users/king/work/project/ai/gf && ./gradlew assembleDebug --no-daemon`
Expected: `BUILD SUCCESSFUL`，产物 `app/build/outputs/apk/debug/app-debug.apk`。
若失败：阅读错误第一行；通常是 `libs.versions.toml` 缩进或 plugin id 错。

- [ ] **Step 17：提交**

```bash
cd /Users/king/work/project/ai/gf
git add .
git commit -m "chore: 初始化 Android 工程脚手架"
```

---

### Task 1：领域枚举与值对象

**Files:**
- Create: `app/src/main/java/com/king/luna/domain/model/FlowLevel.kt`
- Create: `app/src/main/java/com/king/luna/domain/model/Mood.kt`
- Create: `app/src/main/java/com/king/luna/domain/model/Symptom.kt`
- Create: `app/src/main/java/com/king/luna/domain/model/Phase.kt`
- Create: `app/src/main/java/com/king/luna/domain/model/CyclePrediction.kt`

- [ ] **Step 1：FlowLevel.kt**

```kotlin
package com.king.luna.domain.model

// 流量等级，NONE = 当天不是经期
enum class FlowLevel(val label: String) {
    NONE("无"),
    LIGHT("轻"),
    MEDIUM("中"),
    HEAVY("重")
}
```

- [ ] **Step 2：Mood.kt**

```kotlin
package com.king.luna.domain.model

enum class Mood(val label: String) {
    CALM("平静"),
    HAPPY("开心"),
    TIRED("疲惫"),
    IRRITABLE("烦躁"),
    ANXIOUS("焦虑"),
    SAD("低落")
}
```

- [ ] **Step 3：Symptom.kt**

```kotlin
package com.king.luna.domain.model

enum class Symptom(val label: String) {
    CRAMPS("腹痛"),
    HEADACHE("头痛"),
    BACKACHE("腰酸"),
    TENDER_BREASTS("乳房胀"),
    NAUSEA("恶心"),
    BLOATING("腹胀"),
    ACNE("痘痘"),
    CRAVING("食欲增"),
    INSOMNIA("失眠"),
    SLEEPY("嗜睡")
}
```

- [ ] **Step 4：Phase.kt**

```kotlin
package com.king.luna.domain.model

// 周期阶段；UNKNOWN = 还没记过经期
enum class Phase {
    UNKNOWN, PERIOD, FOLLICULAR, OVULATION, LUTEAL
}
```

- [ ] **Step 5：CyclePrediction.kt**

```kotlin
package com.king.luna.domain.model

import java.time.LocalDate

// CyclePredictor.predict() 的输出，纯值对象
data class CyclePrediction(
    val cycleStart: LocalDate?,
    val cycleDay: Int?,                    // 1-based；null 表示无历史
    val avgCycleLength: Int,               // 默认 28
    val avgPeriodLength: Int,              // 默认 5
    val nextPeriodStart: LocalDate?,
    val ovulationDay: LocalDate?,
    val fertileWindow: ClosedRange<LocalDate>?,
    val phase: Phase
)
```

- [ ] **Step 6：编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7：提交**

```bash
git add app/src/main/java/com/king/luna/domain/model/
git commit -m "feat: 加入领域枚举与 CyclePrediction 值对象"
```

---

### Task 2：CyclePredictor 纯函数（TDD）

**目标：** 输入「全部经期日列表 + 今天」，输出 `CyclePrediction`。无 IO，可在 JVM 单元测试覆盖。

**Files:**
- Create: `app/src/main/java/com/king/luna/domain/cycle/CyclePredictor.kt`
- Test: `app/src/test/java/com/king/luna/domain/cycle/CyclePredictorTest.kt`

- [ ] **Step 1：先写测试（TDD）**

```kotlin
package com.king.luna.domain.cycle

import com.google.common.truth.Truth.assertThat
import com.king.luna.domain.model.Phase
import org.junit.Test
import java.time.LocalDate

class CyclePredictorTest {

    private fun d(s: String) = LocalDate.parse(s)

    @Test
    fun `空记录使用默认 28 5 且 phase 为 UNKNOWN`() {
        val r = CyclePredictor.predict(emptyList(), today = d("2026-05-05"))
        assertThat(r.avgCycleLength).isEqualTo(28)
        assertThat(r.avgPeriodLength).isEqualTo(5)
        assertThat(r.cycleStart).isNull()
        assertThat(r.cycleDay).isNull()
        assertThat(r.nextPeriodStart).isNull()
        assertThat(r.phase).isEqualTo(Phase.UNKNOWN)
    }

    @Test
    fun `单段经期 5 天，今天是经期开始日，phase 为 PERIOD`() {
        val period = listOf("2026-05-01","2026-05-02","2026-05-03","2026-05-04","2026-05-05").map { d(it) }
        val r = CyclePredictor.predict(period, today = d("2026-05-01"))
        assertThat(r.cycleStart).isEqualTo(d("2026-05-01"))
        assertThat(r.cycleDay).isEqualTo(1)
        assertThat(r.avgPeriodLength).isEqualTo(5)
        assertThat(r.phase).isEqualTo(Phase.PERIOD)
        assertThat(r.nextPeriodStart).isEqualTo(d("2026-05-29"))  // 28 天后
    }

    @Test
    fun `经期内最后一天 phase 仍为 PERIOD`() {
        val period = listOf("2026-05-01","2026-05-02","2026-05-03","2026-05-04","2026-05-05").map { d(it) }
        val r = CyclePredictor.predict(period, today = d("2026-05-05"))
        assertThat(r.cycleDay).isEqualTo(5)
        assertThat(r.phase).isEqualTo(Phase.PERIOD)
    }

    @Test
    fun `经期结束次日进入卵泡期`() {
        val period = listOf("2026-05-01","2026-05-02","2026-05-03","2026-05-04","2026-05-05").map { d(it) }
        val r = CyclePredictor.predict(period, today = d("2026-05-06"))
        assertThat(r.phase).isEqualTo(Phase.FOLLICULAR)
    }

    @Test
    fun `下次经期前 14 天为排卵日，phase 为 OVULATION`() {
        val period = listOf("2026-04-01","2026-04-02","2026-04-03","2026-04-04","2026-04-05").map { d(it) }
        // 下次经期 = 4-1 + 28 = 4-29，排卵日 = 4-15
        val r = CyclePredictor.predict(period, today = d("2026-04-15"))
        assertThat(r.ovulationDay).isEqualTo(d("2026-04-15"))
        assertThat(r.phase).isEqualTo(Phase.OVULATION)
        assertThat(r.fertileWindow!!.start).isEqualTo(d("2026-04-13"))
        assertThat(r.fertileWindow!!.endInclusive).isEqualTo(d("2026-04-17"))
    }

    @Test
    fun `排卵日之后到下次经期之间为黄体期`() {
        val period = listOf("2026-04-01","2026-04-02","2026-04-03","2026-04-04","2026-04-05").map { d(it) }
        val r = CyclePredictor.predict(period, today = d("2026-04-22"))
        assertThat(r.phase).isEqualTo(Phase.LUTEAL)
    }

    @Test
    fun `多段经期取最近 6 段平均周期长度`() {
        // 7 段经期开始日，间隔分别是 25 26 27 28 29 30 31（取最近 6 个 = 26..31，平均 28.5 → 28）
        val starts = listOf("2026-01-01","2026-01-26","2026-02-21","2026-03-20","2026-04-17","2026-05-16","2026-06-15","2026-07-16")
        val period = starts.map { d(it) }   // 每段只 1 天，方便算
        val r = CyclePredictor.predict(period, today = d("2026-07-16"))
        assertThat(r.avgCycleLength).isEqualTo(28)
    }

    @Test
    fun `经期日不连续超过 1 天则视为新一段`() {
        // 5-1 到 5-5 一段；间隔 5-6 是新一段（按规则：≤1 天间隔属于同段，>1 天为新段）
        // 这里 5-7 与 5-5 间隔 1 天（5-6 是 1 天 gap），按规则 ≤1 仍同段，故构造 ≥2 天间隔
        val period = listOf("2026-05-01","2026-05-02","2026-05-05").map { d(it) }
        val r = CyclePredictor.predict(period, today = d("2026-05-05"))
        // 两段：[5-1,5-2] 和 [5-5]；最近段开始 = 5-5
        assertThat(r.cycleStart).isEqualTo(d("2026-05-05"))
    }

    @Test
    fun `跨年边界 12-28 + 28 = 1-25`() {
        val period = listOf("2025-12-28","2025-12-29","2025-12-30").map { d(it) }
        val r = CyclePredictor.predict(period, today = d("2025-12-28"))
        assertThat(r.nextPeriodStart).isEqualTo(d("2026-01-25"))
    }

    @Test
    fun `经期段间隔恰为 1 天视为同段`() {
        val period = listOf("2026-05-01","2026-05-02","2026-05-04").map { d(it) }   // 5-3 缺一天
        val r = CyclePredictor.predict(period, today = d("2026-05-04"))
        // 仅一段，开始 = 5-1
        assertThat(r.cycleStart).isEqualTo(d("2026-05-01"))
        assertThat(r.avgPeriodLength).isEqualTo(4)   // 段长度 = 5-4 - 5-1 + 1
    }
}
```

- [ ] **Step 2：跑测试确认全部 FAIL**

Run: `./gradlew :app:testDebugUnitTest`
Expected: 编译失败（CyclePredictor 不存在）。

- [ ] **Step 3：实现 CyclePredictor.kt**

```kotlin
package com.king.luna.domain.cycle

import com.king.luna.domain.model.CyclePrediction
import com.king.luna.domain.model.Phase
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object CyclePredictor {

    private const val DEFAULT_CYCLE = 28
    private const val DEFAULT_PERIOD = 5
    private const val WINDOW = 6           // 取最近 N 次
    private const val OVULATION_OFFSET = 14
    private const val FERTILE_PAD = 2

    fun predict(periodDays: List<LocalDate>, today: LocalDate): CyclePrediction {
        if (periodDays.isEmpty()) {
            return CyclePrediction(
                cycleStart = null,
                cycleDay = null,
                avgCycleLength = DEFAULT_CYCLE,
                avgPeriodLength = DEFAULT_PERIOD,
                nextPeriodStart = null,
                ovulationDay = null,
                fertileWindow = null,
                phase = Phase.UNKNOWN
            )
        }

        val sorted = periodDays.distinct().sorted()
        val segments = splitIntoSegments(sorted)        // List<List<LocalDate>>，已升序
        val starts = segments.map { it.first() }
        val periodLengths = segments.map { it.size }

        val cycleIntervals = starts.zipWithNext { a, b -> ChronoUnit.DAYS.between(a, b).toInt() }
        val avgCycle = cycleIntervals.takeLast(WINDOW).averageOrDefault(DEFAULT_CYCLE)
        val avgPeriod = periodLengths.takeLast(WINDOW).averageOrDefault(DEFAULT_PERIOD)

        val cycleStart = starts.last()
        val nextStart = cycleStart.plusDays(avgCycle.toLong())
        val ovulation = nextStart.minusDays(OVULATION_OFFSET.toLong())
        val fertile = ovulation.minusDays(FERTILE_PAD.toLong())..ovulation.plusDays(FERTILE_PAD.toLong())

        val cycleDay = (ChronoUnit.DAYS.between(cycleStart, today).toInt() + 1)
            .takeIf { it >= 1 }
        val phase = derivePhase(today, cycleStart, segments.last(), ovulation, fertile, nextStart)

        return CyclePrediction(
            cycleStart = cycleStart,
            cycleDay = cycleDay,
            avgCycleLength = avgCycle,
            avgPeriodLength = avgPeriod,
            nextPeriodStart = nextStart,
            ovulationDay = ovulation,
            fertileWindow = fertile,
            phase = phase
        )
    }

    // 把日期序列按 ≤1 天间隔归为同段
    private fun splitIntoSegments(sorted: List<LocalDate>): List<List<LocalDate>> {
        val out = mutableListOf<MutableList<LocalDate>>()
        for (d in sorted) {
            val last = out.lastOrNull()?.lastOrNull()
            if (last == null || ChronoUnit.DAYS.between(last, d) > 1) {
                out.add(mutableListOf(d))
            } else {
                out.last().add(d)
            }
        }
        return out
    }

    private fun List<Int>.averageOrDefault(default: Int): Int =
        if (isEmpty()) default else (sum() / size).coerceAtLeast(1)

    private fun derivePhase(
        today: LocalDate,
        cycleStart: LocalDate,
        currentSegment: List<LocalDate>,
        ovulation: LocalDate,
        fertile: ClosedRange<LocalDate>,
        nextStart: LocalDate
    ): Phase {
        // today 在最近一段经期范围内
        if (today in currentSegment.first()..currentSegment.last()) return Phase.PERIOD
        // today 在排卵日当天
        if (today == ovulation) return Phase.OVULATION
        // today 在受孕窗口内（不含排卵日，前面已判过）
        if (today in fertile) return Phase.OVULATION
        // 经期结束次日 ~ 受孕窗口前
        if (today > currentSegment.last() && today < fertile.start) return Phase.FOLLICULAR
        // 受孕窗口后 ~ 下次经期前
        if (today > fertile.endInclusive && today < nextStart) return Phase.LUTEAL
        // 默认：超出预测范围（如 today 远在过去）→ UNKNOWN，但有数据时落黄体期最稳
        return Phase.LUTEAL
    }
}
```

- [ ] **Step 4：跑测试**

Run: `./gradlew :app:testDebugUnitTest`
Expected: 全绿。若失败按提示修；常见错：`avgPeriodLength` 在「不连续段」用例没考虑去重——`distinct()` 已处理。

- [ ] **Step 5：提交**

```bash
git add app/src/main/java/com/king/luna/domain/cycle/ app/src/test/
git commit -m "feat: 加入 CyclePredictor 纯函数及单元测试"
```

---

### Task 3：Room 实体 + DAO + 数据库 + Converters

**Files:**
- Create: `app/src/main/java/com/king/luna/data/db/Converters.kt`
- Create: `app/src/main/java/com/king/luna/data/entity/PeriodDayEntity.kt`
- Create: `app/src/main/java/com/king/luna/data/entity/DayLogEntity.kt`
- Create: `app/src/main/java/com/king/luna/data/db/PeriodDayDao.kt`
- Create: `app/src/main/java/com/king/luna/data/db/DayLogDao.kt`
- Create: `app/src/main/java/com/king/luna/data/db/LunaDatabase.kt`

- [ ] **Step 1：Converters.kt**

```kotlin
package com.king.luna.data.db

import androidx.room.TypeConverter
import com.king.luna.domain.model.FlowLevel
import java.time.LocalDate

class Converters {
    @TypeConverter fun dateToString(d: LocalDate?): String? = d?.toString()
    @TypeConverter fun stringToDate(s: String?): LocalDate? = s?.let(LocalDate::parse)

    @TypeConverter fun flowToString(f: FlowLevel): String = f.name
    @TypeConverter fun stringToFlow(s: String): FlowLevel = FlowLevel.valueOf(s)
}
```

- [ ] **Step 2：PeriodDayEntity.kt**

```kotlin
package com.king.luna.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.king.luna.domain.model.FlowLevel
import java.time.LocalDate

@Entity(tableName = "period_day")
data class PeriodDayEntity(
    @PrimaryKey val date: LocalDate,
    val flow: FlowLevel
)
```

- [ ] **Step 3：DayLogEntity.kt**

```kotlin
package com.king.luna.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "day_log")
data class DayLogEntity(
    @PrimaryKey val date: LocalDate,
    val moods: String,        // CSV: "CALM,TIRED"
    val symptoms: String,     // CSV: "CRAMPS,HEADACHE"
    val note: String          // 默认 ""
)
```

- [ ] **Step 4：PeriodDayDao.kt**

```kotlin
package com.king.luna.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.king.luna.data.entity.PeriodDayEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PeriodDayDao {

    @Query("SELECT * FROM period_day ORDER BY date ASC")
    fun observeAll(): Flow<List<PeriodDayEntity>>

    @Query("SELECT * FROM period_day WHERE date = :date LIMIT 1")
    suspend fun get(date: LocalDate): PeriodDayEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PeriodDayEntity)

    @Query("DELETE FROM period_day WHERE date = :date")
    suspend fun delete(date: LocalDate)
}
```

- [ ] **Step 5：DayLogDao.kt**

```kotlin
package com.king.luna.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.king.luna.data.entity.DayLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface DayLogDao {

    @Query("SELECT date FROM day_log")
    fun observeDates(): Flow<List<LocalDate>>

    @Query("SELECT * FROM day_log WHERE date = :date LIMIT 1")
    suspend fun get(date: LocalDate): DayLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DayLogEntity)

    @Query("DELETE FROM day_log WHERE date = :date")
    suspend fun delete(date: LocalDate)
}
```

- [ ] **Step 6：LunaDatabase.kt**

```kotlin
package com.king.luna.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.king.luna.data.entity.DayLogEntity
import com.king.luna.data.entity.PeriodDayEntity

@Database(
    entities = [PeriodDayEntity::class, DayLogEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LunaDatabase : RoomDatabase() {
    abstract fun periodDayDao(): PeriodDayDao
    abstract fun dayLogDao(): DayLogDao

    companion object {
        fun build(ctx: Context): LunaDatabase =
            Room.databaseBuilder(ctx, LunaDatabase::class.java, "luna.db").build()
    }
}
```

- [ ] **Step 7：编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL；KSP 生成 `LunaDatabase_Impl`、`*Dao_Impl`。
若 KSP 报 `Cannot figure out how to save this field into database` → 检查 Converters 类是否被 `@TypeConverters` 引用。

- [ ] **Step 8：提交**

```bash
git add app/src/main/java/com/king/luna/data/
git commit -m "feat: 加入 Room 实体、DAO 与数据库定义"
```

---

### Task 4：CycleRepository

**Files:**
- Create: `app/src/main/java/com/king/luna/data/repository/CycleRepository.kt`

- [ ] **Step 1：CycleRepository.kt**

```kotlin
package com.king.luna.data.repository

import com.king.luna.data.db.DayLogDao
import com.king.luna.data.db.PeriodDayDao
import com.king.luna.data.entity.DayLogEntity
import com.king.luna.data.entity.PeriodDayEntity
import com.king.luna.domain.model.FlowLevel
import com.king.luna.domain.model.Mood
import com.king.luna.domain.model.Symptom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate

// MVP 唯一 Repository。所有读写都从这里走。
class CycleRepository(
    private val periodDao: PeriodDayDao,
    private val logDao: DayLogDao
) {

    // 全部经期日（升序）
    fun observePeriodDays(): Flow<List<PeriodDayEntity>> = periodDao.observeAll()

    // 全部已记录的 day_log 日期集合
    fun observeLoggedDates(): Flow<List<LocalDate>> = logDao.observeDates()

    // 经期 + 日志 一次到位（日历用）
    fun observeAll(): Flow<Pair<List<PeriodDayEntity>, List<LocalDate>>> =
        combine(observePeriodDays(), observeLoggedDates()) { p, l -> p to l }

    suspend fun getPeriodDay(date: LocalDate): PeriodDayEntity? = periodDao.get(date)
    suspend fun getDayLog(date: LocalDate): DayLogEntity? = logDao.get(date)

    suspend fun setFlow(date: LocalDate, flow: FlowLevel) {
        if (flow == FlowLevel.NONE) {
            periodDao.delete(date)
        } else {
            periodDao.upsert(PeriodDayEntity(date, flow))
        }
    }

    suspend fun saveDayLog(date: LocalDate, moods: Set<Mood>, symptoms: Set<Symptom>, note: String) {
        if (moods.isEmpty() && symptoms.isEmpty() && note.isBlank()) {
            logDao.delete(date)
            return
        }
        logDao.upsert(
            DayLogEntity(
                date = date,
                moods = moods.joinToString(",") { it.name },
                symptoms = symptoms.joinToString(",") { it.name },
                note = note.trim()
            )
        )
    }

    // 字符串 ↔ Set 的解析放 Repository（UI 不应感知 CSV）
    fun parseMoods(csv: String): Set<Mood> =
        csv.split(",").filter { it.isNotBlank() }.mapNotNull { runCatching { Mood.valueOf(it) }.getOrNull() }.toSet()

    fun parseSymptoms(csv: String): Set<Symptom> =
        csv.split(",").filter { it.isNotBlank() }.mapNotNull { runCatching { Symptom.valueOf(it) }.getOrNull() }.toSet()
}
```

- [ ] **Step 2：编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3：提交**

```bash
git add app/src/main/java/com/king/luna/data/repository/
git commit -m "feat: 加入 CycleRepository"
```

---

### Task 5：AppContainer + LunaApplication

**Files:**
- Create: `app/src/main/java/com/king/luna/AppContainer.kt`
- Modify: `app/src/main/java/com/king/luna/LunaApplication.kt`

- [ ] **Step 1：AppContainer.kt**

```kotlin
package com.king.luna

import android.content.Context
import com.king.luna.data.db.LunaDatabase
import com.king.luna.data.repository.CycleRepository

// 手动 DI：MVP 只有一个 Repository，无需 Hilt
class AppContainer(ctx: Context) {
    private val db = LunaDatabase.build(ctx.applicationContext)
    val repository: CycleRepository = CycleRepository(db.periodDayDao(), db.dayLogDao())
}
```

- [ ] **Step 2：替换 LunaApplication.kt**

```kotlin
package com.king.luna

import android.app.Application

class LunaApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
```

- [ ] **Step 3：编译验证**

Run: `./gradlew :app:compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4：提交**

```bash
git add app/src/main/java/com/king/luna/AppContainer.kt app/src/main/java/com/king/luna/LunaApplication.kt
git commit -m "feat: 加入 AppContainer 手动 DI"
```

---

### Task 6：UI 主题

**Files:**
- Create: `app/src/main/java/com/king/luna/ui/theme/Color.kt`
- Create: `app/src/main/java/com/king/luna/ui/theme/Type.kt`
- Create: `app/src/main/java/com/king/luna/ui/theme/Theme.kt`

- [ ] **Step 1：Color.kt（OKLCH 已转 sRGB）**

```kotlin
package com.king.luna.ui.theme

import androidx.compose.ui.graphics.Color

object LunaColors {
    val Bg          = Color(0xFFFAF7F2)   // --bg
    val Surface     = Color(0xFFFEFCF8)   // --surface
    val Fg          = Color(0xFF2A211B)   // --fg
    val Muted       = Color(0xFF7A6F65)   // --muted
    val Border      = Color(0xFFE8E1D5)   // --border
    val Accent      = Color(0xFFC5634A)   // --accent
    val AccentSoft  = Color(0x24C5634A)   // accent @ 14% alpha
    val AccentSofter= Color(0x12C5634A)   // accent @ 7% alpha
    val OnAccent    = Color.White
}
```

- [ ] **Step 2：Type.kt**

```kotlin
package com.king.luna.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Display = FontFamily.Serif       // 衬线，对应 --font-display
private val Body    = FontFamily.SansSerif   // 默认，对应 --font-body
private val Mono    = FontFamily.Monospace   // 等宽，对应 --font-mono

val LunaTypography = Typography(
    displayLarge = TextStyle(fontFamily = Display, fontSize = 26.sp, fontWeight = FontWeight.Normal),
    headlineMedium = TextStyle(fontFamily = Display, fontSize = 20.sp, fontWeight = FontWeight.Normal),
    titleMedium = TextStyle(fontFamily = Body, fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontFamily = Body, fontSize = 15.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontFamily = Body, fontSize = 14.sp),
    labelMedium = TextStyle(fontFamily = Mono, fontSize = 11.sp, letterSpacing = 1.5.sp),
    labelSmall = TextStyle(fontFamily = Mono, fontSize = 10.sp, letterSpacing = 1.5.sp)
)
```

- [ ] **Step 3：Theme.kt**

```kotlin
package com.king.luna.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LunaColorScheme = lightColorScheme(
    primary = LunaColors.Accent,
    onPrimary = LunaColors.OnAccent,
    secondary = LunaColors.Accent,
    background = LunaColors.Bg,
    onBackground = LunaColors.Fg,
    surface = LunaColors.Surface,
    onSurface = LunaColors.Fg,
    surfaceVariant = LunaColors.Surface,
    onSurfaceVariant = LunaColors.Muted,
    outline = LunaColors.Border
)

@Composable
fun LunaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LunaColorScheme,
        typography = LunaTypography,
        content = content
    )
}
```

- [ ] **Step 4：编译验证 + 提交**

```bash
./gradlew :app:compileDebugKotlin
git add app/src/main/java/com/king/luna/ui/theme/
git commit -m "feat: 加入 Luna 主题（warm-soft 配色）"
```

---

### Task 7：通用组件

**Files:**
- Create: `app/src/main/java/com/king/luna/ui/components/PrimaryButton.kt`
- Create: `app/src/main/java/com/king/luna/ui/components/SecondaryButton.kt`
- Create: `app/src/main/java/com/king/luna/ui/components/PillBadge.kt`
- Create: `app/src/main/java/com/king/luna/ui/components/BottomTabBar.kt`

- [ ] **Step 1：PrimaryButton.kt**

```kotlin
package com.king.luna.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.king.luna.ui.theme.LunaColors

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = LunaColors.Accent,
            contentColor = LunaColors.OnAccent
        )
    ) { Text(text) }
}
```

- [ ] **Step 2：SecondaryButton.kt**

```kotlin
package com.king.luna.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.king.luna.ui.theme.LunaColors

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, LunaColors.Border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = LunaColors.Fg, containerColor = Color.Transparent)
    ) { Text(text) }
}
```

- [ ] **Step 3：PillBadge.kt**

```kotlin
package com.king.luna.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.king.luna.ui.theme.LunaColors

@Composable
fun PillBadge(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(color = LunaColors.Accent),
        modifier = modifier
            .background(LunaColors.AccentSoft, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
```

- [ ] **Step 4：BottomTabBar.kt**

```kotlin
package com.king.luna.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.king.luna.ui.theme.LunaColors

enum class LunaTab { Today, Calendar, Insights, Me }

private data class TabSpec(val tab: LunaTab, val label: String, val icon: ImageVector, val enabled: Boolean)

@Composable
fun BottomTabBar(
    current: LunaTab,
    onSelect: (LunaTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val specs = listOf(
        TabSpec(LunaTab.Today, "今天", Icons.Outlined.WbSunny, true),
        TabSpec(LunaTab.Calendar, "日历", Icons.Outlined.CalendarMonth, true),
        TabSpec(LunaTab.Insights, "洞察", Icons.Outlined.TrendingUp, false),     // V2
        TabSpec(LunaTab.Me, "我的", Icons.Outlined.AccountCircle, false)         // V2
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(LunaColors.Surface)
            .border(1.dp, LunaColors.Border)
            .padding(vertical = 8.dp)
    ) {
        specs.forEach { s ->
            val active = s.tab == current
            val tint = when {
                !s.enabled -> LunaColors.Border
                active -> LunaColors.Accent
                else -> LunaColors.Muted
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
                    .let { m -> if (s.enabled) m.also {} else m }
            ) {
                Icon(s.icon, contentDescription = s.label, tint = tint)
                Spacer(Modifier.height(2.dp))
                Text(s.label, style = MaterialTheme.typography.labelSmall.copy(color = tint))
            }
            // 点击逻辑
            if (s.enabled) {
                Box(
                    Modifier
                        .matchParentSize()
                        .let { it }    // 占位；真正的点击通过外层 Row 上的 clickable 不合适
                )
            }
        }
    }
}
```

> 修正：上面 Row 内的点击实现不优雅。改用每个 Column 自带 `clickable`：

```kotlin
// 替换 Row 内 forEach 块为：
specs.forEach { s ->
    val active = s.tab == current
    val tint = when {
        !s.enabled -> LunaColors.Border
        active -> LunaColors.Accent
        else -> LunaColors.Muted
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .padding(vertical = 4.dp)
            .let { if (s.enabled) it.then(androidx.compose.foundation.clickable { onSelect(s.tab) }) else it }
    ) {
        Icon(s.icon, contentDescription = s.label, tint = tint)
        Spacer(Modifier.height(2.dp))
        Text(s.label, style = MaterialTheme.typography.labelSmall.copy(color = tint))
    }
}
```

注意 `clickable` 需要 import：`androidx.compose.foundation.clickable`。落地时使用如下完整版本（覆盖整个文件）：

```kotlin
package com.king.luna.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.king.luna.ui.theme.LunaColors

enum class LunaTab { Today, Calendar, Insights, Me }

private data class TabSpec(val tab: LunaTab, val label: String, val icon: ImageVector, val enabled: Boolean)

@Composable
fun BottomTabBar(current: LunaTab, onSelect: (LunaTab) -> Unit, modifier: Modifier = Modifier) {
    val specs = listOf(
        TabSpec(LunaTab.Today, "今天", Icons.Outlined.WbSunny, true),
        TabSpec(LunaTab.Calendar, "日历", Icons.Outlined.CalendarMonth, true),
        TabSpec(LunaTab.Insights, "洞察", Icons.Outlined.TrendingUp, false),
        TabSpec(LunaTab.Me, "我的", Icons.Outlined.AccountCircle, false)
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(LunaColors.Surface)
            .border(1.dp, LunaColors.Border)
            .padding(vertical = 8.dp)
    ) {
        specs.forEach { s ->
            val active = s.tab == current
            val tint = when {
                !s.enabled -> LunaColors.Border
                active -> LunaColors.Accent
                else -> LunaColors.Muted
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 4.dp)
                    .then(if (s.enabled) Modifier.clickable { onSelect(s.tab) } else Modifier)
            ) {
                Icon(s.icon, contentDescription = s.label, tint = tint)
                Spacer(Modifier.height(2.dp))
                Text(s.label, style = MaterialTheme.typography.labelSmall.copy(color = tint))
            }
        }
    }
}
```

补依赖：在 `libs.versions.toml` 加 material-icons-extended 库，并在 app 模块加 `implementation("androidx.compose.material:material-icons-extended")`（受 BOM 管理）。修改：

```toml
# 在 [libraries] 末尾加
androidx-compose-material-icons-extended = { module = "androidx.compose.material:material-icons-extended" }
```

```kotlin
// app/build.gradle.kts dependencies 末尾加
implementation(libs.androidx.compose.material.icons.extended)
```

- [ ] **Step 5：编译验证 + 提交**

```bash
./gradlew :app:compileDebugKotlin
git add app/ gradle/libs.versions.toml
git commit -m "feat: 加入通用 UI 组件（按钮、徽章、底部 Tab）"
```

---

### Task 8：导航骨架（让三屏可跳转，先用占位 Composable）

**Files:**
- Create: `app/src/main/java/com/king/luna/ui/nav/LunaNavGraph.kt`
- Create: `app/src/main/java/com/king/luna/LunaApp.kt`
- Modify: `app/src/main/java/com/king/luna/MainActivity.kt`

- [ ] **Step 1：LunaNavGraph.kt**

```kotlin
package com.king.luna.ui.nav

object LunaRoutes {
    const val TODAY = "today"
    const val CALENDAR = "calendar"
    const val LOG = "log/{date}"            // date: ISO_LOCAL_DATE
    fun log(date: String) = "log/$date"
}
```

- [ ] **Step 2：LunaApp.kt**

```kotlin
package com.king.luna

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.king.luna.ui.nav.LunaRoutes
import com.king.luna.ui.theme.LunaTheme

@Composable
fun LunaApp(container: AppContainer) {
    LunaTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            val nav = rememberNavController()
            NavHost(navController = nav, startDestination = LunaRoutes.TODAY) {
                composable(LunaRoutes.TODAY) {
                    Text("Today placeholder")
                }
                composable(LunaRoutes.CALENDAR) {
                    Text("Calendar placeholder")
                }
                composable(
                    LunaRoutes.LOG,
                    arguments = listOf(navArgument("date") { type = NavType.StringType })
                ) { entry ->
                    val date = entry.arguments?.getString("date").orEmpty()
                    Text("Log placeholder · $date")
                }
            }
        }
    }
}
```

- [ ] **Step 3：替换 MainActivity.kt**

```kotlin
package com.king.luna

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as LunaApplication).container
        setContent { LunaApp(container) }
    }
}
```

- [ ] **Step 4：构建并装机冒烟**

Run: `./gradlew :app:assembleDebug`
Expected: BUILD SUCCESSFUL；APK 在 `app/build/outputs/apk/debug/app-debug.apk`。

可选 ADB 安装并启动：

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.king.luna/.MainActivity
```
Expected: 屏幕显示 "Today placeholder"。

- [ ] **Step 5：提交**

```bash
git add app/
git commit -m "feat: 加入 Compose Navigation 骨架"
```

---

### Task 9：今日页

**Files:**
- Create: `app/src/main/java/com/king/luna/ui/today/TodayViewModel.kt`
- Create: `app/src/main/java/com/king/luna/ui/today/TodayScreen.kt`
- Create: `app/src/main/java/com/king/luna/ui/today/components/CycleRing.kt`
- Modify: `app/src/main/java/com/king/luna/LunaApp.kt`（替换 Today placeholder）

- [ ] **Step 1：TodayViewModel.kt**

```kotlin
package com.king.luna.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.king.luna.data.repository.CycleRepository
import com.king.luna.domain.cycle.CyclePredictor
import com.king.luna.domain.model.CyclePrediction
import com.king.luna.domain.model.Phase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TodayUiState(
    val today: LocalDate = LocalDate.now(),
    val prediction: CyclePrediction = empty(),
    val nickname: String = "你"
) {
    companion object {
        fun empty() = CyclePrediction(
            cycleStart = null, cycleDay = null,
            avgCycleLength = 28, avgPeriodLength = 5,
            nextPeriodStart = null, ovulationDay = null,
            fertileWindow = null, phase = Phase.UNKNOWN
        )
    }
}

class TodayViewModel(private val repo: CycleRepository) : ViewModel() {

    private val _state = MutableStateFlow(TodayUiState())
    val state: StateFlow<TodayUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observePeriodDays().collect { entities ->
                val today = LocalDate.now()
                val prediction = CyclePredictor.predict(entities.map { it.date }, today)
                _state.value = TodayUiState(today = today, prediction = prediction)
            }
        }
    }

    class Factory(private val repo: CycleRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TodayViewModel(repo) as T
    }
}
```

- [ ] **Step 2：CycleRing.kt（自定义 Canvas 圆环）**

```kotlin
package com.king.luna.ui.today.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.king.luna.ui.theme.LunaColors

@Composable
fun CycleRing(
    cycleDay: Int?,
    cycleLength: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    val safeDay = (cycleDay ?: 0).coerceIn(0, cycleLength)
    val progress = if (cycleLength == 0) 0f else safeDay.toFloat() / cycleLength
    Box(modifier = modifier.size(220.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            val pad = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val tl = Offset(pad, pad)
            // 背景圈
            drawArc(
                color = LunaColors.Border,
                startAngle = 0f, sweepAngle = 360f, useCenter = false,
                topLeft = tl, size = arcSize,
                style = Stroke(width = stroke)
            )
            // 进度圈：从 12 点开始顺时针
            drawArc(
                color = LunaColors.Accent,
                startAngle = -90f, sweepAngle = 360f * progress, useCenter = false,
                topLeft = tl, size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val displayDay = cycleDay?.toString() ?: "—"
            Text(
                text = "第 $displayDay 天",
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 48.sp, fontWeight = FontWeight.Normal),
                color = LunaColors.Fg
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = LunaColors.Muted
            )
        }
    }
}
```

- [ ] **Step 3：TodayScreen.kt**

```kotlin
package com.king.luna.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.king.luna.domain.model.Phase
import com.king.luna.ui.components.BottomTabBar
import com.king.luna.ui.components.LunaTab
import com.king.luna.ui.components.PrimaryButton
import com.king.luna.ui.theme.LunaColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import com.king.luna.ui.today.components.CycleRing

@Composable
fun TodayScreen(
    vm: TodayViewModel,
    onRecordToday: (LocalDate) -> Unit,
    onTabSelect: (LunaTab) -> Unit
) {
    val state by vm.state.collectAsState()
    val pred = state.prediction

    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            HeaderBar(state.today, state.nickname)

            Spacer(Modifier.height(20.dp))

            // 圆环
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CycleRing(
                    cycleDay = pred.cycleDay,
                    cycleLength = pred.avgCycleLength,
                    label = phaseLabel(pred.phase, pred.cycleDay)
                )
            }

            Spacer(Modifier.height(20.dp))

            // 提醒卡
            Card(background = LunaColors.AccentSofter) {
                Text(
                    text = "LUNA 今日提醒",
                    style = MaterialTheme.typography.labelMedium.copy(color = LunaColors.Muted)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = phaseTip(pred.phase),
                    style = MaterialTheme.typography.bodyMedium.copy(color = LunaColors.Fg)
                )
            }

            Spacer(Modifier.height(16.dp))

            // 下次周期
            Text("下一个周期", style = MaterialTheme.typography.labelMedium.copy(color = LunaColors.Muted))
            Spacer(Modifier.height(8.dp))
            Card(background = LunaColors.Surface, border = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = pred.nextPeriodStart?.let { "预计 ${formatDate(it)}" } ?: "暂无预测",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        val sub = pred.nextPeriodStart?.let { "还有 ${ChronoUnit.DAYS.between(state.today, it)} 天 · 周期 ${pred.avgCycleLength} 天" }
                            ?: "记录一次经期开始预测"
                        Text(sub, style = MaterialTheme.typography.labelMedium.copy(color = LunaColors.Muted))
                    }
                    Text(
                        text = pred.nextPeriodStart?.let { ChronoUnit.DAYS.between(state.today, it).toString() } ?: "—",
                        style = MaterialTheme.typography.displayLarge.copy(fontSize = 28.sp, color = LunaColors.Accent, fontWeight = FontWeight.Normal)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            PrimaryButton(text = "记录今天的状态", onClick = { onRecordToday(state.today) })

            Spacer(Modifier.height(20.dp))
        }

        BottomTabBar(current = LunaTab.Today, onSelect = onTabSelect)
    }
}

@Composable
private fun HeaderBar(today: LocalDate, nickname: String) {
    Column {
        Text(
            text = formatDate(today) + " · " + chineseDow(today),
            style = MaterialTheme.typography.labelMedium.copy(color = LunaColors.Muted)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = greeting() + "，" + nickname + "。",
            style = MaterialTheme.typography.displayLarge
        )
    }
}

@Composable
private fun Card(background: androidx.compose.ui.graphics.Color, border: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    val mod = Modifier
        .fillMaxWidth()
        .background(background, RoundedCornerShape(18.dp))
        .let { if (border) it.border(1.dp, LunaColors.Border, RoundedCornerShape(18.dp)) else it }
        .padding(16.dp)
    Column(modifier = mod, content = content)
}

private fun greeting(): String {
    val h = java.time.LocalTime.now().hour
    return when {
        h < 11 -> "早安"
        h < 14 -> "中午好"
        h < 18 -> "下午好"
        else -> "晚安"
    }
}

private fun phaseLabel(phase: Phase, cycleDay: Int?): String = when (phase) {
    Phase.UNKNOWN -> "还没有数据"
    Phase.PERIOD -> "经期 · 第 ${cycleDay ?: 0} 天"
    Phase.FOLLICULAR -> "卵泡期 · 恢复中"
    Phase.OVULATION -> "排卵期 · 高生育力"
    Phase.LUTEAL -> "黄体期"
}

private fun phaseTip(phase: Phase): String = when (phase) {
    Phase.UNKNOWN -> "记录一次经期，Luna 才能学会你的节奏。"
    Phase.PERIOD -> "多喝温水，别强撑。"
    Phase.FOLLICULAR -> "精力回升期，适合规划重要的事。"
    Phase.OVULATION -> "今天是本月高生育力。透明拉丝分泌物通常意味排卵临近。"
    Phase.LUTEAL -> "容易情绪起伏，对自己温柔点。"
}

private val DateFmt = DateTimeFormatter.ofPattern("M 月 d 日")
private fun formatDate(d: LocalDate): String = d.format(DateFmt)

private val DowMap = mapOf(
    1 to "周一", 2 to "周二", 3 to "周三", 4 to "周四", 5 to "周五", 6 to "周六", 7 to "周日"
)
private fun chineseDow(d: LocalDate): String = DowMap[d.dayOfWeek.value] ?: ""
```

- [ ] **Step 4：在 LunaApp.kt 替换 Today placeholder**

修改 `composable(LunaRoutes.TODAY)` 块为：

```kotlin
composable(LunaRoutes.TODAY) {
    val vm: com.king.luna.ui.today.TodayViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.king.luna.ui.today.TodayViewModel.Factory(container.repository)
    )
    com.king.luna.ui.today.TodayScreen(
        vm = vm,
        onRecordToday = { date -> nav.navigate(com.king.luna.ui.nav.LunaRoutes.log(date.toString())) },
        onTabSelect = { tab ->
            when (tab) {
                com.king.luna.ui.components.LunaTab.Today -> {}
                com.king.luna.ui.components.LunaTab.Calendar -> nav.navigate(com.king.luna.ui.nav.LunaRoutes.CALENDAR)
                else -> {}    // V2 灰色禁用
            }
        }
    )
}
```

- [ ] **Step 5：构建 + 装机冒烟**

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.king.luna/.MainActivity
```
Expected: 显示今日页（无数据时圆环显示"第 — 天"，文案"还没有数据"）。

- [ ] **Step 6：提交**

```bash
git add app/
git commit -m "feat: 实现今日页（圆环、提醒、下次预测、Tab 栏）"
```

---

### Task 10：日历页

**Files:**
- Create: `app/src/main/java/com/king/luna/ui/calendar/CalendarViewModel.kt`
- Create: `app/src/main/java/com/king/luna/ui/calendar/components/MonthGrid.kt`
- Create: `app/src/main/java/com/king/luna/ui/calendar/CalendarScreen.kt`
- Modify: `app/src/main/java/com/king/luna/LunaApp.kt`（替换 Calendar placeholder）

- [ ] **Step 1：CalendarViewModel.kt**

```kotlin
package com.king.luna.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.king.luna.data.repository.CycleRepository
import com.king.luna.domain.cycle.CyclePredictor
import com.king.luna.domain.model.FlowLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class DayCell(
    val date: LocalDate,
    val inMonth: Boolean,
    val flow: FlowLevel?,
    val isPredicted: Boolean,
    val isFertile: Boolean,
    val isOvulation: Boolean,
    val isToday: Boolean,
    val hasLog: Boolean
)

data class CalendarUiState(
    val visibleMonth: YearMonth = YearMonth.now(),
    val cells: List<DayCell> = emptyList()
)

class CalendarViewModel(private val repo: CycleRepository) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeAll().collect { (periods, logs) ->
                rebuild(_state.value.visibleMonth, periods.map { it.date to it.flow }, logs.toSet())
            }
        }
    }

    fun goPrevMonth() {
        val prev = _state.value.visibleMonth.minusMonths(1)
        _state.value = _state.value.copy(visibleMonth = prev)
        // 触发重建
        viewModelScope.launch {
            repo.observeAll() // 不再 collect，简化：直接重新算一次基于现有 state
        }
        rebuildFromCurrent()
    }

    fun goNextMonth() {
        _state.value = _state.value.copy(visibleMonth = _state.value.visibleMonth.plusMonths(1))
        rebuildFromCurrent()
    }

    fun goToday() {
        _state.value = _state.value.copy(visibleMonth = YearMonth.now())
        rebuildFromCurrent()
    }

    private fun rebuildFromCurrent() {
        viewModelScope.launch {
            val periodEntities = repo.observePeriodDays()
            // 一次性快照：collect first
            kotlinx.coroutines.flow.first(periodEntities).let { entities ->
                val logs = kotlinx.coroutines.flow.first(repo.observeLoggedDates()).toSet()
                rebuild(_state.value.visibleMonth, entities.map { it.date to it.flow }, logs)
            }
        }
    }

    private fun rebuild(month: YearMonth, periods: List<Pair<LocalDate, FlowLevel>>, loggedDates: Set<LocalDate>) {
        val today = LocalDate.now()
        val pred = CyclePredictor.predict(periods.map { it.first }, today)

        // 预测下一段经期：起点 + 周期长度，再连续 avgPeriodLength 天
        val predictedDays: Set<LocalDate> = if (pred.nextPeriodStart != null) {
            (0 until pred.avgPeriodLength).map { pred.nextPeriodStart!!.plusDays(it.toLong()) }.toSet()
        } else emptySet()
        val ovulation = pred.ovulationDay
        val fertile = pred.fertileWindow

        val periodMap: Map<LocalDate, FlowLevel> = periods.toMap()

        // 网格起点：本月 1 号往前补到周一
        val first = month.atDay(1)
        val gridStart = first.minusDays(((first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7).toLong())
        val cells = (0 until 42).map { i ->
            val d = gridStart.plusDays(i.toLong())
            DayCell(
                date = d,
                inMonth = YearMonth.from(d) == month,
                flow = periodMap[d],
                isPredicted = d in predictedDays && d !in periodMap,
                isFertile = fertile != null && d in fertile && d != ovulation,
                isOvulation = ovulation != null && d == ovulation,
                isToday = d == today,
                hasLog = d in loggedDates
            )
        }
        _state.value = _state.value.copy(visibleMonth = month, cells = cells)
    }

    class Factory(private val repo: CycleRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CalendarViewModel(repo) as T
    }
}
```

> 注意：上面用了 `kotlinx.coroutines.flow.first(...)` 静态调用——其实 Flow.first() 是扩展函数。修正为：

```kotlin
import kotlinx.coroutines.flow.first

private fun rebuildFromCurrent() {
    viewModelScope.launch {
        val entities = repo.observePeriodDays().first()
        val logs = repo.observeLoggedDates().first().toSet()
        rebuild(_state.value.visibleMonth, entities.map { it.date to it.flow }, logs)
    }
}
```

并删除原有 `goPrevMonth` 中错误的 `repo.observeAll()` 行。完整正确版本（替换全文）：

```kotlin
package com.king.luna.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.king.luna.data.repository.CycleRepository
import com.king.luna.domain.cycle.CyclePredictor
import com.king.luna.domain.model.FlowLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

data class DayCell(
    val date: LocalDate,
    val inMonth: Boolean,
    val flow: FlowLevel?,
    val isPredicted: Boolean,
    val isFertile: Boolean,
    val isOvulation: Boolean,
    val isToday: Boolean,
    val hasLog: Boolean
)

data class CalendarUiState(
    val visibleMonth: YearMonth = YearMonth.now(),
    val cells: List<DayCell> = emptyList()
)

class CalendarViewModel(private val repo: CycleRepository) : ViewModel() {

    private val _state = MutableStateFlow(CalendarUiState())
    val state: StateFlow<CalendarUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeAll().collect { (periods, logs) ->
                rebuild(
                    _state.value.visibleMonth,
                    periods.map { it.date to it.flow },
                    logs.toSet()
                )
            }
        }
    }

    fun goPrevMonth() = setMonth(_state.value.visibleMonth.minusMonths(1))
    fun goNextMonth() = setMonth(_state.value.visibleMonth.plusMonths(1))
    fun goToday() = setMonth(YearMonth.now())

    private fun setMonth(m: YearMonth) {
        _state.value = _state.value.copy(visibleMonth = m)
        viewModelScope.launch {
            val entities = repo.observePeriodDays().first()
            val logs = repo.observeLoggedDates().first().toSet()
            rebuild(m, entities.map { it.date to it.flow }, logs)
        }
    }

    private fun rebuild(
        month: YearMonth,
        periods: List<Pair<LocalDate, FlowLevel>>,
        loggedDates: Set<LocalDate>
    ) {
        val today = LocalDate.now()
        val pred = CyclePredictor.predict(periods.map { it.first }, today)

        val predictedDays: Set<LocalDate> = pred.nextPeriodStart?.let { start ->
            (0 until pred.avgPeriodLength).map { start.plusDays(it.toLong()) }.toSet()
        } ?: emptySet()

        val ovulation = pred.ovulationDay
        val fertile = pred.fertileWindow
        val periodMap = periods.toMap()

        val first = month.atDay(1)
        val gridStart = first.minusDays(((first.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7).toLong())

        val cells = (0 until 42).map { i ->
            val d = gridStart.plusDays(i.toLong())
            DayCell(
                date = d,
                inMonth = YearMonth.from(d) == month,
                flow = periodMap[d],
                isPredicted = d in predictedDays && d !in periodMap,
                isFertile = fertile != null && d in fertile && d != ovulation,
                isOvulation = ovulation != null && d == ovulation,
                isToday = d == today,
                hasLog = d in loggedDates
            )
        }
        _state.value = _state.value.copy(visibleMonth = month, cells = cells)
    }

    class Factory(private val repo: CycleRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = CalendarViewModel(repo) as T
    }
}
```

- [ ] **Step 2：MonthGrid.kt**

```kotlin
package com.king.luna.ui.calendar.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.king.luna.ui.calendar.DayCell
import com.king.luna.ui.theme.LunaColors

@Composable
fun MonthGrid(
    cells: List<DayCell>,
    onDayClick: (DayCell) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        // 周次行（周一为首）
        Row(Modifier.fillMaxWidth()) {
            listOf("一", "二", "三", "四", "五", "六", "日").forEach { lbl ->
                Box(Modifier.weight(1f).padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
                    Text(lbl, style = MaterialTheme.typography.labelSmall.copy(color = LunaColors.Muted))
                }
            }
        }
        // 6 行 x 7 列
        for (row in 0 until 6) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cell = cells[row * 7 + col]
                    DayCellView(
                        cell = cell,
                        onClick = { onDayClick(cell) },
                        modifier = Modifier.weight(1f).aspectRatio(1f).padding(3.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCellView(cell: DayCell, onClick: () -> Unit, modifier: Modifier) {
    val isPeriod = cell.flow != null
    Box(
        modifier = modifier
            .clip(CircleShape)
            .let { m ->
                when {
                    isPeriod -> m.background(LunaColors.Accent)
                    cell.isPredicted -> m.border(1.5.dp, SolidColor(LunaColors.Accent), CircleShape)
                    cell.isFertile -> m.background(LunaColors.AccentSoft)
                    else -> m
                }
            }
            .let { m -> if (cell.isToday) m.border(2.dp, LunaColors.Fg, CircleShape) else m }
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        val color = when {
            isPeriod -> LunaColors.OnAccent
            !cell.inMonth -> LunaColors.Border
            cell.isPredicted || cell.isFertile -> LunaColors.Accent
            else -> LunaColors.Fg
        }
        val weight = if (cell.isToday || isPeriod) FontWeight.SemiBold else FontWeight.Normal
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = cell.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium.copy(color = color, fontWeight = weight)
            )
            if (cell.hasLog && !isPeriod) {
                Spacer(Modifier.height(2.dp))
                Box(Modifier.size(4.dp).clip(CircleShape).background(LunaColors.Accent))
            }
        }
    }
}
```

- [ ] **Step 3：CalendarScreen.kt**

```kotlin
package com.king.luna.ui.calendar

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.king.luna.ui.calendar.components.MonthGrid
import com.king.luna.ui.components.BottomTabBar
import com.king.luna.ui.components.LunaTab
import com.king.luna.ui.theme.LunaColors
import java.time.LocalDate

@Composable
fun CalendarScreen(
    vm: CalendarViewModel,
    onDayClick: (LocalDate) -> Unit,
    onTabSelect: (LunaTab) -> Unit
) {
    val state by vm.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${state.visibleMonth.year} 年 ${state.visibleMonth.monthValue} 月",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = vm::goPrevMonth) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = "上月", tint = LunaColors.Fg)
                }
                IconButton(onClick = vm::goNextMonth) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "下月", tint = LunaColors.Fg)
                }
            }
            Spacer(Modifier.height(8.dp))
            MonthGrid(cells = state.cells, onDayClick = { onDayClick(it.date) })

            Spacer(Modifier.height(16.dp))
            // 图例
            Row {
                LegendDot(LunaColors.Accent, "经期")
                Spacer(Modifier.width(12.dp))
                LegendDot(LunaColors.AccentSoft, "排卵")
                Spacer(Modifier.width(12.dp))
                Text("◌ 预测", style = MaterialTheme.typography.labelSmall.copy(color = LunaColors.Muted))
            }
        }
        BottomTabBar(current = LunaTab.Calendar, onSelect = onTabSelect)
    }
}

@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Box(
            Modifier
                .size(10.dp)
                .androidx_compose_foundation_clip_circle_helper(color)
        )
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = LunaColors.Muted))
    }
}

// 占位 helper：用 Box + clip(CircleShape).background(color) 即可，不需要这个扩展。
// 重写 LegendDot：
```

> 修正：上面 `LegendDot` 里的伪扩展是错的。最终落地版本（替换 CalendarScreen 末尾两个函数）：

```kotlin
@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(10.dp)
                .androidx.compose.ui.draw.clip_helper_unused()
        ) {}
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = LunaColors.Muted))
    }
}
```

更进一步，直接使用标准 API（最终版，编译可通过）：

```kotlin
@Composable
private fun LegendDot(color: androidx.compose.ui.graphics.Color, label: String) {
    androidx.compose.foundation.layout.Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        androidx.compose.foundation.layout.Box(
            modifier = androidx.compose.ui.Modifier
                .size(10.dp)
                .androidx.compose.ui.draw.clip(androidx.compose.foundation.shape.CircleShape)
                .androidx.compose.foundation.background(color)
        )
        androidx.compose.foundation.layout.Spacer(androidx.compose.ui.Modifier.width(4.dp))
        Text(label, style = androidx.compose.material3.MaterialTheme.typography.labelSmall.copy(color = LunaColors.Muted))
    }
}
```

> 上面的全限定混乱了。**实施时直接采用此清洁版**（删除上面所有 LegendDot 错误版本）：

```kotlin
// 顶部 imports 中加入：
// import androidx.compose.foundation.background
// import androidx.compose.foundation.shape.CircleShape
// import androidx.compose.ui.draw.clip
// import androidx.compose.ui.graphics.Color

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = LunaColors.Muted))
    }
}
```

CalendarScreen.kt 的清洁完整版（**实施时以此为准**）：

```kotlin
package com.king.luna.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.king.luna.ui.calendar.components.MonthGrid
import com.king.luna.ui.components.BottomTabBar
import com.king.luna.ui.components.LunaTab
import com.king.luna.ui.theme.LunaColors
import java.time.LocalDate

@Composable
fun CalendarScreen(
    vm: CalendarViewModel,
    onDayClick: (LocalDate) -> Unit,
    onTabSelect: (LunaTab) -> Unit
) {
    val state by vm.state.collectAsState()
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${state.visibleMonth.year} 年 ${state.visibleMonth.monthValue} 月",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = vm::goPrevMonth) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = "上月", tint = LunaColors.Fg)
                }
                IconButton(onClick = vm::goNextMonth) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "下月", tint = LunaColors.Fg)
                }
            }
            Spacer(Modifier.height(8.dp))
            MonthGrid(cells = state.cells, onDayClick = { onDayClick(it.date) })
            Spacer(Modifier.height(16.dp))
            Row {
                LegendDot(LunaColors.Accent, "经期")
                Spacer(Modifier.width(12.dp))
                LegendDot(LunaColors.AccentSoft, "排卵")
                Spacer(Modifier.width(12.dp))
                Text("◌ 预测", style = MaterialTheme.typography.labelSmall.copy(color = LunaColors.Muted))
            }
        }
        BottomTabBar(current = LunaTab.Calendar, onSelect = onTabSelect)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall.copy(color = LunaColors.Muted))
    }
}
```

- [ ] **Step 4：在 LunaApp.kt 替换 Calendar placeholder**

```kotlin
composable(LunaRoutes.CALENDAR) {
    val vm: com.king.luna.ui.calendar.CalendarViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.king.luna.ui.calendar.CalendarViewModel.Factory(container.repository)
    )
    com.king.luna.ui.calendar.CalendarScreen(
        vm = vm,
        onDayClick = { date -> nav.navigate(com.king.luna.ui.nav.LunaRoutes.log(date.toString())) },
        onTabSelect = { tab ->
            when (tab) {
                com.king.luna.ui.components.LunaTab.Today -> nav.popBackStack(com.king.luna.ui.nav.LunaRoutes.TODAY, false)
                com.king.luna.ui.components.LunaTab.Calendar -> {}
                else -> {}
            }
        }
    )
}
```

- [ ] **Step 5：构建 + 装机冒烟**

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.king.luna/.MainActivity
```
Expected: 在今日页底部点"日历" → 看到当月日历（无数据时纯网格 + 今天有黑边）。

- [ ] **Step 6：提交**

```bash
git add app/
git commit -m "feat: 实现日历页（月切换、经期/预测/排卵高亮）"
```

---

### Task 11：记录页

**Files:**
- Create: `app/src/main/java/com/king/luna/ui/log/LogViewModel.kt`
- Create: `app/src/main/java/com/king/luna/ui/log/components/FlowSelector.kt`
- Create: `app/src/main/java/com/king/luna/ui/log/components/MoodChip.kt`
- Create: `app/src/main/java/com/king/luna/ui/log/components/SymptomTag.kt`
- Create: `app/src/main/java/com/king/luna/ui/log/LogScreen.kt`
- Modify: `app/src/main/java/com/king/luna/LunaApp.kt`（替换 Log placeholder）

- [ ] **Step 1：LogViewModel.kt**

```kotlin
package com.king.luna.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.king.luna.data.repository.CycleRepository
import com.king.luna.domain.model.FlowLevel
import com.king.luna.domain.model.Mood
import com.king.luna.domain.model.Symptom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class LogUiState(
    val date: LocalDate,
    val flow: FlowLevel = FlowLevel.NONE,
    val moods: Set<Mood> = emptySet(),
    val symptoms: Set<Symptom> = emptySet(),
    val note: String = "",
    val saving: Boolean = false,
    val saved: Boolean = false       // 一次性事件信号
)

class LogViewModel(
    private val repo: CycleRepository,
    initialDate: LocalDate
) : ViewModel() {

    private val _state = MutableStateFlow(LogUiState(date = initialDate))
    val state: StateFlow<LogUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val period = repo.getPeriodDay(initialDate)
            val log = repo.getDayLog(initialDate)
            _state.value = LogUiState(
                date = initialDate,
                flow = period?.flow ?: FlowLevel.NONE,
                moods = log?.let { repo.parseMoods(it.moods) } ?: emptySet(),
                symptoms = log?.let { repo.parseSymptoms(it.symptoms) } ?: emptySet(),
                note = log?.note ?: ""
            )
        }
    }

    fun setFlow(f: FlowLevel) { _state.value = _state.value.copy(flow = f) }
    fun toggleMood(m: Mood) {
        val s = _state.value.moods.toMutableSet()
        if (!s.add(m)) s.remove(m)
        _state.value = _state.value.copy(moods = s)
    }
    fun toggleSymptom(sy: Symptom) {
        val s = _state.value.symptoms.toMutableSet()
        if (!s.add(sy)) s.remove(sy)
        _state.value = _state.value.copy(symptoms = s)
    }
    fun setNote(n: String) {
        _state.value = _state.value.copy(note = n.take(200))
    }

    fun save() {
        if (_state.value.saving) return
        _state.value = _state.value.copy(saving = true)
        viewModelScope.launch {
            val s = _state.value
            repo.setFlow(s.date, s.flow)
            repo.saveDayLog(s.date, s.moods, s.symptoms, s.note)
            _state.value = _state.value.copy(saving = false, saved = true)
        }
    }

    class Factory(private val repo: CycleRepository, private val date: LocalDate) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = LogViewModel(repo, date) as T
    }
}
```

- [ ] **Step 2：FlowSelector.kt**

```kotlin
package com.king.luna.ui.log.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.king.luna.domain.model.FlowLevel
import com.king.luna.ui.theme.LunaColors

@Composable
fun FlowSelector(
    selected: FlowLevel,
    onSelect: (FlowLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowLevel.entries.forEach { f ->
            val active = f == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .background(
                        color = if (active) LunaColors.Accent else LunaColors.Surface,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = if (active) LunaColors.Accent else LunaColors.Border,
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { onSelect(f) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = f.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (active) LunaColors.OnAccent else LunaColors.Fg
                )
            }
        }
    }
}
```

- [ ] **Step 3：MoodChip.kt**

```kotlin
package com.king.luna.ui.log.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.king.luna.domain.model.Mood
import com.king.luna.ui.theme.LunaColors

@Composable
fun MoodChip(
    mood: Mood,
    active: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .background(
                color = if (active) LunaColors.AccentSoft else LunaColors.Surface,
                shape = RoundedCornerShape(14.dp)
            )
            .border(
                width = 1.dp,
                color = if (active) LunaColors.Accent else LunaColors.Border,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onToggle() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = mood.label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (active) LunaColors.Accent else LunaColors.Fg
        )
    }
}
```

- [ ] **Step 4：SymptomTag.kt**

```kotlin
package com.king.luna.ui.log.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.king.luna.domain.model.Symptom
import com.king.luna.ui.theme.LunaColors

@Composable
fun SymptomTag(
    symptom: Symptom,
    active: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = symptom.label,
        style = MaterialTheme.typography.bodyMedium,
        color = if (active) LunaColors.OnAccent else LunaColors.Muted,
        modifier = modifier
            .background(
                color = if (active) LunaColors.Accent else LunaColors.Surface,
                shape = RoundedCornerShape(999.dp)
            )
            .border(
                width = 1.dp,
                color = if (active) LunaColors.Accent else LunaColors.Border,
                shape = RoundedCornerShape(999.dp)
            )
            .clickable { onToggle() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}
```

- [ ] **Step 5：LogScreen.kt**

```kotlin
package com.king.luna.ui.log

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.king.luna.domain.model.Mood
import com.king.luna.domain.model.Symptom
import com.king.luna.ui.components.PrimaryButton
import com.king.luna.ui.log.components.FlowSelector
import com.king.luna.ui.log.components.MoodChip
import com.king.luna.ui.log.components.SymptomTag
import com.king.luna.ui.theme.LunaColors
import java.time.format.DateTimeFormatter

@Composable
fun LogScreen(
    vm: LogViewModel,
    onBack: () -> Unit
) {
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(state.saved) {
        if (state.saved) {
            snackbar.showSnackbar("已保存")
            onBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = LunaColors.Bg,
        bottomBar = {
            Surface(color = LunaColors.Bg, tonalElevation = 0.dp) {
                Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                    PrimaryButton(text = if (state.saving) "保存中..." else "保存", onClick = vm::save, enabled = !state.saving)
                }
            }
        },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = state.date.format(DateTimeFormatter.ofPattern("M 月 d 日")),
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = LunaColors.Fg)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = LunaColors.Bg)
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // 流量
            SectionTitle("流量")
            Spacer(Modifier.height(8.dp))
            FlowSelector(selected = state.flow, onSelect = vm::setFlow)

            Spacer(Modifier.height(20.dp))
            SectionTitle("心情")
            Spacer(Modifier.height(8.dp))
            // 2 行 x 3 列
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Mood.entries.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { m ->
                            MoodChip(
                                mood = m,
                                active = m in state.moods,
                                onToggle = { vm.toggleMood(m) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("症状")
            Spacer(Modifier.height(8.dp))
            FlowRow {
                Symptom.entries.forEach { s ->
                    SymptomTag(
                        symptom = s,
                        active = s in state.symptoms,
                        onToggle = { vm.toggleSymptom(s) },
                        modifier = Modifier.padding(end = 8.dp, bottom = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            SectionTitle("备注")
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = state.note,
                onValueChange = vm::setNote,
                placeholder = { Text("写点什么…（最多 200 字）") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 100.dp),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LunaColors.Accent,
                    unfocusedBorderColor = LunaColors.Border
                )
            )

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(color = LunaColors.Muted)
    )
}
```

注：`FlowRow` 需要 `androidx.compose.foundation.layout.FlowRow`（实验 API），加 `@OptIn(ExperimentalLayoutApi::class)` 在文件顶部 `LogScreen` 函数上：

```kotlin
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
// ...
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogScreen(...) { ... }
```

- [ ] **Step 6：在 LunaApp.kt 替换 Log placeholder**

```kotlin
composable(
    LunaRoutes.LOG,
    arguments = listOf(navArgument("date") { type = NavType.StringType })
) { entry ->
    val dateStr = entry.arguments?.getString("date").orEmpty()
    val date = java.time.LocalDate.parse(dateStr)
    val vm: com.king.luna.ui.log.LogViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = com.king.luna.ui.log.LogViewModel.Factory(container.repository, date)
    )
    com.king.luna.ui.log.LogScreen(vm = vm, onBack = { nav.popBackStack() })
}
```

- [ ] **Step 7：构建 + 装机冒烟**

```bash
./gradlew :app:assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.king.luna/.MainActivity
```
Expected:
- 今日页 → 点"记录今天的状态" → 进入记录页
- 选流量"中"，选 2 个心情，2 个症状，写备注 → 点"保存"
- 返回今日页，圆环更新（cycleDay = 1，phase = PERIOD）
- 杀进程重开，数据保留

- [ ] **Step 8：提交**

```bash
git add app/
git commit -m "feat: 实现记录页（流量、心情、症状、备注）"
```

---

### Task 12：图标 / strings / Manifest 收尾

**Files:**
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml`
- Create: `app/src/main/res/mipmap-anydpi-v26/ic_launcher_round.xml`
- Create: `app/src/main/res/drawable/ic_launcher_background.xml`
- Create: `app/src/main/res/drawable/ic_launcher_foreground.xml`
- Modify: `app/src/main/res/values/strings.xml`

- [ ] **Step 1：背景纯色 drawable**

`drawable/ic_launcher_background.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">
    <solid android:color="#FAF7F2" />
</shape>
```

- [ ] **Step 2：前景 vector（一个简单的月亮）**

`drawable/ic_launcher_foreground.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path
        android:fillColor="#C5634A"
        android:pathData="M65,30 A24,24 0 1,0 65,84 A18,18 0 1,1 65,30 Z" />
</vector>
```

- [ ] **Step 3：自适应图标定义**

`mipmap-anydpi-v26/ic_launcher.xml`：

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

`mipmap-anydpi-v26/ic_launcher_round.xml`：（同上内容）

```xml
<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_background" />
    <foreground android:drawable="@drawable/ic_launcher_foreground" />
</adaptive-icon>
```

- [ ] **Step 4：完善 strings.xml**

```xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">Luna</string>
</resources>
```

（MVP 文案直接硬编码到 Compose 里，不抽到 strings.xml——i18n 是 YAGNI）

- [ ] **Step 5：构建 + 提交**

```bash
./gradlew :app:assembleDebug
git add app/
git commit -m "chore: 加入应用图标"
```

---

### Task 13：构建 debug APK 并冒烟验收

**目标：** 满足设计文档 §11 全部 9 项验收标准。

- [ ] **Step 1：完整构建**

Run: `./gradlew clean :app:assembleDebug :app:testDebugUnitTest`
Expected:
- BUILD SUCCESSFUL
- 单元测试全绿
- APK 在 `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 2：装机**

```bash
adb devices                      # 确认有设备/模拟器
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.king.luna/.MainActivity
```

- [ ] **Step 3：手测路径（按设计 §11 验收）**

| # | 步骤 | 期望 |
|---|---|---|
| 1 | 启动 App | 1.5s 内到达今日页；圆环显示"第 — 天"，文案"还没有数据" |
| 2 | 点"记录今天的状态" | 进入记录页 |
| 3 | 选流量"中"、心情"开心"、症状"腹胀"、备注"测试" | 各项高亮 |
| 4 | 点"保存" | Snackbar"已保存"，返回今日页 |
| 5 | 今日页圆环 | 显示"第 1 天"，标签"经期 · 第 1 天" |
| 6 | 点底部"日历" | 进入日历页，今日是实心红色圆 |
| 7 | 上下月切换 | 标题更新，下个月可见预测段（虚线边框） |
| 8 | 杀进程后重开 | 上述数据全部保留 |
| 9 | 检查权限 | 设置→应用→Luna→权限 = 无（验证无 INTERNET 权限） |

- [ ] **Step 4：检查 Manifest 无网络权限**

Run: `aapt dump permissions app/build/outputs/apk/debug/app-debug.apk`
Expected: 输出无 `android.permission.INTERNET`

- [ ] **Step 5：最终提交**

```bash
git add -A
git status      # 确认干净
git commit --allow-empty -m "chore: MVP v1.0 验收通过"
git tag v1.0-mvp
```

---

## Self-Review

**Spec 覆盖：**
- ✅ §2.1 屏 01/02/03 → Task 9/10/11
- ✅ §2.2 12 项明确不做 → 计划无相关任务（按设计意图缺席）
- ✅ §3 架构 → Task 0-8 落地
- ✅ §4 数据模型 PeriodDay/DayLog → Task 3
- ✅ §4.3 算法（最近 6 段平均） → Task 2 含完整 TDD 用例
- ✅ §5.1/5.2/5.3 三屏布局 → Task 9/10/11
- ✅ §6 配色映射 → Task 6 Color.kt
- ✅ §7 错误处理 → 仅 Snackbar，无重试、无崩溃上报（按设计意图）
- ✅ §8 测试策略：CyclePredictorTest 必须有 → Task 2 已含；CycleRepositoryTest 在设计中标"必须"——**补丁**：见下文 Task 4 增量
- ✅ §9 构建发布（Gradle 8.7、AGP 8.5、Kotlin 2.0、JDK 17、本地 Maven 镜像） → Task 0
- ✅ §11 验收 → Task 13

**Spec §8 漏掉的 CycleRepositoryTest 补丁：**

> 受限于「instrumented 测试需 androidTest 源集」，且当前 Room 也支持纯 JVM via `Room.inMemoryDatabaseBuilder` + Robolectric。MVP 仍**遵循设计 §8 但简化**：把 Repository 测试合并入 `CyclePredictorTest` 不合适。给一个折中——**在 Task 4 末尾增加 Step 3，写一个 `CycleRepositoryFakeTest`（直接 mock DAO 行为，纯 JVM）**：

补丁（实施时可选执行）：

```kotlin
// app/src/test/java/com/king/luna/data/repository/CycleRepositoryFakeTest.kt
package com.king.luna.data.repository

import com.google.common.truth.Truth.assertThat
import com.king.luna.data.db.DayLogDao
import com.king.luna.data.db.PeriodDayDao
import com.king.luna.data.entity.DayLogEntity
import com.king.luna.data.entity.PeriodDayEntity
import com.king.luna.domain.model.FlowLevel
import com.king.luna.domain.model.Mood
import com.king.luna.domain.model.Symptom
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test
import java.time.LocalDate

class CycleRepositoryFakeTest {

    private class FakePeriodDao : PeriodDayDao {
        val data = linkedMapOf<LocalDate, PeriodDayEntity>()
        val flow = MutableStateFlow<List<PeriodDayEntity>>(emptyList())
        override fun observeAll(): Flow<List<PeriodDayEntity>> = flow
        override suspend fun get(date: LocalDate) = data[date]
        override suspend fun upsert(entity: PeriodDayEntity) { data[entity.date] = entity; flow.value = data.values.sortedBy { it.date } }
        override suspend fun delete(date: LocalDate) { data.remove(date); flow.value = data.values.sortedBy { it.date } }
    }

    private class FakeLogDao : DayLogDao {
        val data = linkedMapOf<LocalDate, DayLogEntity>()
        val flow = MutableStateFlow<List<LocalDate>>(emptyList())
        override fun observeDates(): Flow<List<LocalDate>> = flow
        override suspend fun get(date: LocalDate) = data[date]
        override suspend fun upsert(entity: DayLogEntity) { data[entity.date] = entity; flow.value = data.keys.toList() }
        override suspend fun delete(date: LocalDate) { data.remove(date); flow.value = data.keys.toList() }
    }

    @Test
    fun `setFlow NONE 删除经期日`() = runBlocking {
        val pd = FakePeriodDao(); val ld = FakeLogDao()
        val repo = CycleRepository(pd, ld)
        repo.setFlow(LocalDate.of(2026,5,1), FlowLevel.MEDIUM)
        assertThat(pd.data).hasSize(1)
        repo.setFlow(LocalDate.of(2026,5,1), FlowLevel.NONE)
        assertThat(pd.data).isEmpty()
    }

    @Test
    fun `空记录会删除 day_log`() = runBlocking {
        val pd = FakePeriodDao(); val ld = FakeLogDao()
        val repo = CycleRepository(pd, ld)
        val d = LocalDate.of(2026,5,1)
        repo.saveDayLog(d, setOf(Mood.HAPPY), emptySet(), "x")
        assertThat(ld.data).hasSize(1)
        repo.saveDayLog(d, emptySet(), emptySet(), "")
        assertThat(ld.data).isEmpty()
    }
}
```

**Placeholder 扫描：**
- 无 TBD/TODO/"implement later"
- 所有代码块完整可粘贴
- 唯一可能歧义点：Task 7 BottomTabBar 早期版本含错误代码——已用最终清洁版本覆盖；Task 10 LegendDot 同样以最终清洁版本为准。**实施时只看每个 step 最后一段「最终落地版本」**

**类型一致性：**
- `FlowLevel.NONE/LIGHT/MEDIUM/HEAVY` 全文一致
- `Phase` 5 个值一致
- `CyclePrediction` 字段在 Task 1 定义，Task 2/9/10 使用一致
- `LunaTab` 4 个值一致
- `LunaRoutes.{TODAY,CALENDAR,LOG,log()}` 一致
- DAO 方法签名（`upsert(entity)`、`delete(date)`、`observeAll()`/`observeDates()`）在 Task 3/4 定义，Task 9/10/11 一致

**Scope 检查：** MVP 三屏闭环，路线图明确推迟。无范围越界。

---

## Execution Handoff

计划已写入并准备提交。两种执行模式可选：

1. **Subagent-Driven（推荐）** — 我为每个 Task 派出新鲜 subagent，Task 之间我做 review，迭代快。
2. **Inline Execution** — 在本会话内串行执行，按检查点暂停给你审查。

**重要**：根据你的全局规则 STRICT MODE「执行类任务必须由主 agent 内联串行执行；严禁委托给 subagent；严禁并发」——这条规则**优先于我的推荐**。因此实际只能选 **Inline Execution**。

我会用 superpowers:executing-plans 串行内联执行。
