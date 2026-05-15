plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

// Gradle 8.7 / AGP 8.x 在 JDK 22+（含 25）上常随机失败，Gradle 有时只在 “What went wrong” 里打出 java.version（如 25.0.3）。
val javaFeature = Runtime.version().feature()
require(javaFeature in 17..21) {
    """构建被拒绝：当前用于运行 ./gradlew 的 JDK 主版本为 $javaFeature（${System.getProperty("java.version")}），需要 JDK 17–21。

请任选其一：
  • macOS： export JAVA_HOME=$(/usr/libexec/java_home -v 17)
  • 通用：在 ~/.gradle/gradle.properties 中设置 org.gradle.java.home=/绝对路径/jdk17
  • Android Studio：Settings → Build Tools → Gradle → Gradle JDK → 17
""".trimIndent()
}
