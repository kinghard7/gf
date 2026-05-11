package com.king.luna.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// 浅色色板（warm-soft，对齐原型）
val LunaBg = Color(0xFFFAF7F2)
val LunaSurface = Color(0xFFFFFFFF)
val LunaSurfaceMuted = Color(0xFFF1ECE3)
val LunaCard = Color(0xFFFDFBF7)          // 近白，靠描边而非米黄区分

val LunaAccent = Color(0xFFC5634A)        // 经期/今日
val LunaAccentSoft = Color(0xFFE8A48E)
val LunaOvulation = Color(0xFFB8794A)     // 排卵
val LunaFollicular = Color(0xFFD8B68A)
val LunaLuteal = Color(0xFFA98E72)

val LunaInk = Color(0xFF2E2A26)
val LunaInkMuted = Color(0xFF7A7068)
val LunaDivider = Color(0xFFE6DED1)

// 深色色板（warm-dark，保持暖调，避免冷蓝灰）
val LunaBgDark = Color(0xFF1A1614)
val LunaSurfaceDark = Color(0xFF26201D)
val LunaSurfaceMutedDark = Color(0xFF2F2825)
val LunaCardDark = Color(0xFF2A2421)      // 与 surface 拉开半档亮度

val LunaAccentDark = Color(0xFFE3826B)        // 略提亮，深底上不刺眼又不沉
val LunaAccentSoftDark = Color(0xFFC68A78)
val LunaOvulationDark = Color(0xFFD89668)
val LunaFollicularDark = Color(0xFFE0BE9A)
val LunaLutealDark = Color(0xFFC1A78D)

val LunaInkDark = Color(0xFFEDE6DC)
val LunaInkMutedDark = Color(0xFFA89B8E)
val LunaDividerDark = Color(0xFF3A322E)

// 自定义语义色：Material ColorScheme 不覆盖的、卡片/阶段相关的色，统一走 LunaPalette
data class LunaPalette(
    val card: Color,
    val follicular: Color,
    val luteal: Color
)

val LightLunaPalette = LunaPalette(
    card = LunaCard,
    follicular = LunaFollicular,
    luteal = LunaLuteal
)

val DarkLunaPalette = LunaPalette(
    card = LunaCardDark,
    follicular = LunaFollicularDark,
    luteal = LunaLutealDark
)

val LocalLunaPalette = staticCompositionLocalOf { LightLunaPalette }

// 取当前主题下的扩展色板。Composable 里直接 LunaColors.card / LunaColors.follicular
object LunaColors {
    val card: Color
        @Composable @ReadOnlyComposable
        get() = LocalLunaPalette.current.card
    val follicular: Color
        @Composable @ReadOnlyComposable
        get() = LocalLunaPalette.current.follicular
    val luteal: Color
        @Composable @ReadOnlyComposable
        get() = LocalLunaPalette.current.luteal
}
