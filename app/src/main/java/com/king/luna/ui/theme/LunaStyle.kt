package com.king.luna.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 统一卡片形状：18dp 圆角
val LunaCardShape = RoundedCornerShape(18.dp)

// 统一卡片描边：1px outline 色，避免重描边导致的"框感"
@Composable
@ReadOnlyComposable
fun lunaCardBorder() = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)

// 给任意容器加上"卡片"外观：圆角 + 描边
@Composable
fun Modifier.lunaCard(): Modifier = this
    .clip(LunaCardShape)
    .border(lunaCardBorder(), LunaCardShape)

// Eyebrow / Meta 小标签样式：等宽 + 大写 + tracking + muted
// 用于卡片小标题（"流量"/"心情"/"图例"）和 header 上方的副标
@Composable
@ReadOnlyComposable
fun lunaMetaStyle(): TextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = 11.sp,
    fontWeight = FontWeight.Medium,
    letterSpacing = 1.5.sp,
    color = MaterialTheme.colorScheme.onSurfaceVariant
)

// Header 主标：Serif Medium，配合 eyebrow 用
@Composable
@ReadOnlyComposable
fun lunaHeaderStyle(): TextStyle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 32.sp,
    fontWeight = FontWeight.Medium,
    color = MaterialTheme.colorScheme.onBackground
)
