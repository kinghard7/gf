package com.king.luna.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// 不写死颜色，让 Material3 按 ColorScheme 自动决定（onBackground / onSurfaceVariant）
private val base = TextStyle(color = Color.Unspecified)

// 显示族：标题 / 大数字 用 Serif，呼应原型的杂志感
private val Display = FontFamily.Serif
// Meta 族：小标签 / eyebrow / 数字 metric 用等宽
private val Mono = FontFamily.Monospace

val LunaTypography = Typography(
    // 大标题：Serif + Medium，原型里"Today's Cycle / 排卵期"那种
    displayLarge = base.copy(fontFamily = Display, fontSize = 40.sp, fontWeight = FontWeight.Medium),
    headlineLarge = base.copy(fontFamily = Display, fontSize = 28.sp, fontWeight = FontWeight.Medium),
    headlineMedium = base.copy(fontFamily = Display, fontSize = 22.sp, fontWeight = FontWeight.Medium),
    titleLarge = base.copy(fontFamily = Display, fontSize = 18.sp, fontWeight = FontWeight.Medium),

    // titleMedium / body 走默认 sans，正文该好读就好读
    titleMedium = base.copy(fontSize = 16.sp, fontWeight = FontWeight.Medium),
    bodyLarge = base.copy(fontSize = 16.sp),
    bodyMedium = base.copy(fontSize = 14.sp),
    bodySmall = base.copy(fontSize = 12.sp),

    // Label：等宽 + 大写感（letterSpacing 在使用处加，这里只锁字族）
    labelLarge = base.copy(fontFamily = Mono, fontSize = 13.sp, fontWeight = FontWeight.Medium),
    labelMedium = base.copy(fontFamily = Mono, fontSize = 11.sp, fontWeight = FontWeight.Medium),
    labelSmall = base.copy(fontFamily = Mono, fontSize = 10.sp, fontWeight = FontWeight.Medium)
)
