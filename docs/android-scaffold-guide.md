# Android 原生 App 脚手架复用指南

> 适用范围：基于 Kotlin + Jetpack Compose 的纯本地 Android App。
> 模板源头：Luna 项目（`/Users/king/work/project/ai/gf`）。
> 写这份文档的目的：**下次新建 Android 项目，照着抄就行，不要再踩同样的坑。**

---

## 一、原则

- **环境装一次，模板抄一次，业务码自己写。**
- **复制粘贴 > 抽 library module。** 别为了"复用"硬抽 base 模块，业务差异会让 base 越改越烂。
- **手撸 DI > Hilt。** 容器模式（`AppContainer`）足够用，少一层注解处理器开销。
- **本地优先。** Maven 走 `mavenLocal()`，仓库地址走全局 settings.xml。

---

## 二、一次性环境（装完永久用）

下面这些**只装一次，所有项目共享**，新建 app 时不要再装。

### 1. JDK 17

- 路径：`/Users/king/work/devsoft/java/jdk-17.0.17.jdk`
- AGP 8.x 强依赖 JDK 17。**不要用 JDK 21/25**，AGP 不认。
- 验证：`/Users/king/work/devsoft/java/jdk-17.0.17.jdk/Contents/Home/bin/java -version`

### 2. Android SDK / cmdline-tools

- 路径：`~/Library/Android/sdk`
- 安装方式：`brew install --cask android-commandlinetools`
- 安装完执行一次：
  ```bash
  yes | sdkmanager --licenses
  sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
  ```
- 所有项目共用这个 SDK，不要每个项目自己装。

### 3. 本地 Maven 仓库

- 仓库：`/Users/king/work/devsoft/maven/repository`
- 配置：`/Users/king/work/devsoft/maven/apache-maven-3.9.9/conf/setting.xml`
- Gradle 项目里走 `mavenLocal()` 就能直接命中。

### 4. 环境变量（`~/.zshrc`）

```bash
export JAVA_HOME=/Users/king/work/devsoft/java/jdk-17.0.17.jdk/Contents/Home
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin:$PATH
```

> **检查命令**：`java -version && sdkmanager --version`，两个都通就 OK。

---

## 三、新项目脚手架步骤（每次都要做）

### Step 0：确定基本信息

| 项 | 示例 |
|---|---|
| 项目名 | `Luna` |
| 包名（namespace） | `com.king.luna` |
| 项目目录 | `/Users/king/work/project/ai/<projectName>` |
| 中文名 | `Luna` |
| minSdk | `26`（支持 `java.time.LocalDate`） |
| targetSdk / compileSdk | `34` |

### Step 1：拷贝模板

```bash
# 从 Luna 复制（不要 cp -r 整包，挑几个目录）
cp -r /Users/king/work/project/ai/gf/gradle               <new-project>/
cp    /Users/king/work/project/ai/gf/settings.gradle.kts  <new-project>/
cp    /Users/king/work/project/ai/gf/gradle.properties    <new-project>/  # 如果有
cp    /Users/king/work/project/ai/gf/app/build.gradle.kts <new-project>/app/
```

### Step 2：替换包名 / 应用名

需要在新项目里改的地方：

1. **`settings.gradle.kts`**：改 `rootProject.name`
2. **`app/build.gradle.kts`**：改 `namespace = "com.king.<xxx>"` 和 `applicationId`
3. **`AndroidManifest.xml`**：保持包名为空（用 `namespace`）
4. **`app/src/main/res/values/strings.xml`**：改 `<string name="app_name">`
5. **包路径目录**：`app/src/main/java/com/king/<xxx>/`（重命名）

### Step 3：保留下来的"通用层"代码

下面这些文件直接拷贝到新项目对应位置，不用改逻辑（最多换主题色）：

```
ui/theme/Color.kt       # 改色板
ui/theme/Type.kt        # 字号一般不动
ui/theme/Theme.kt       # 不动
ui/common/Buttons.kt    # 不动
ui/common/PillBadge.kt  # 不动
ui/common/BottomTabBar.kt  # 不动
AppContainer.kt         # 留模板，业务依赖自己加
LunaApplication.kt      # 改类名 + 包名
data/db/Converters.kt   # 留 LocalDate 转换器
```

### Step 4：删掉的"业务层"

```
domain/model/*          # Phase, FlowLevel, Mood, Symptom 是 Luna 业务，删掉
domain/cycle/*          # CyclePredictor 是 Luna 业务，删掉
data/entity/*           # 删
data/db/PeriodDayDao    # 删
data/db/DayLogDao       # 删
data/db/LunaDatabase    # 留壳，删 entities
data/repo/CycleRepository  # 留壳，业务自己写
ui/screen/*             # 全删
ui/nav/*                # 留壳，路由表自己写
```

### Step 5：依赖目录（`gradle/libs.versions.toml`）

直接复制 Luna 的整份。需要新增依赖时**只改这一个文件**，不要在 `build.gradle.kts` 里写裸版本号。

### Step 6：第一次构建验证

```bash
cd <new-project>
./gradlew assembleDebug
```

只要能产出 `app/build/outputs/apk/debug/app-debug.apk`，脚手架就跑通了。

---

## 四、模板里的关键文件清单

下面是"脚手架模板包"应该包含的文件，新项目缺一不可：

```
<root>/
├── settings.gradle.kts                # mavenLocal 优先 + 模块声明
├── gradle.properties                  # AndroidX/Kotlin 配置
├── gradle/
│   ├── libs.versions.toml             # 依赖目录（核心）
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties  # gradle 版本
├── gradlew, gradlew.bat
└── app/
    ├── build.gradle.kts                # AGP/Compose/Room/KSP 配置
    └── src/main/
        ├── AndroidManifest.xml         # 单 Activity，无网络权限
        ├── res/
        │   ├── values/colors.xml
        │   ├── values/strings.xml
        │   ├── values/themes.xml       # statusBar/navBar 跟 bg 同色
        │   ├── xml/backup_rules.xml
        │   └── xml/data_extraction_rules.xml
        └── java/com/king/<xxx>/
            ├── <Xxx>Application.kt     # 持有 AppContainer
            ├── AppContainer.kt          # 手撸 DI
            ├── MainActivity.kt          # setContent { XxxTheme { ... } }
            ├── ui/theme/{Color,Type,Theme}.kt
            └── ui/common/{Buttons,PillBadge,BottomTabBar}.kt
```

---

## 五、脚手架检查清单（建项目时逐项核对）

- [ ] `settings.gradle.kts` 第一行有 `mavenLocal()`
- [ ] `libs.versions.toml` 的 AGP 版本与 Gradle Wrapper 兼容
- [ ] `app/build.gradle.kts` 的 `compileSdk = 34`、`minSdk = 26`、JVM target 17
- [ ] `AndroidManifest.xml` **没有** `<uses-permission>`（除非真的要联网）
- [ ] `Application` 类挂上 `AppContainer`，并在 Manifest 里登记 `android:name`
- [ ] `MainActivity` 用 `setContent { XxxTheme { ... } }` 包裹
- [ ] 跑一次 `./gradlew assembleDebug` 通过

---

## 六、常见坑（踩过一次别再踩）

| 坑 | 现象 | 解法 |
|---|---|---|
| JDK 版本太新 | AGP 8.5 报 "Unsupported class file major version" | 锁 JDK 17，不要用 21/25 |
| 没装 SDK 就 build | `SDK location not found` | 先 `brew install android-commandlinetools` 并跑 `sdkmanager` |
| 用了 `mavenCentral()` 在前 | 本地仓库没命中 | `mavenLocal()` 必须在 `mavenCentral()` 之前 |
| Compose 版本不对齐 | 编译报 metadata 不兼容 | 用 BOM：`androidx.compose:compose-bom:<version>` |
| Room + KSP 用错 | 编译时找不到 Dao 实现 | 必须 `ksp("androidx.room:room-compiler:...")`，不能用 kapt |
| `LocalDate` 报 NoSuchMethodError | minSdk 太低 | minSdk ≥ 26，或开 desugaring |
| `Theme.Material.Light.NoActionBar` 找不到 | 用了 Material3 主题没装 appcompat | 父主题用 `android:Theme.Material.Light.NoActionBar`（系统自带） |

---

## 七、什么时候**不**该套这个脚手架

- 需要联网 / 账号体系 → 加 Retrofit + DataStore + 登录态管理，脚手架不够用，要扩
- 多模块项目（feature module 拆分） → 这套是单 module 模板，多模块要重新设计
- 跨平台（KMP / Flutter / RN） → 这是纯原生模板，不适用

---

## 八、维护这份文档

- **每跑通一个新项目**，回来更新一次坑列表（第六节）。
- **依赖目录有大版本升级**（AGP/Kotlin/Compose），更新这里的版本表。
- **Luna 项目本身的演化**不影响这份文档，因为这是"通用层"指南。
