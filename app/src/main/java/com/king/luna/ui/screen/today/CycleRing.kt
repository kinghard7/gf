package com.king.luna.ui.screen.today

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.king.luna.ui.theme.LunaAccent

// 圆环：背景灰环 + 进度弧 + 中央文案
@Composable
fun CycleRing(
    progress: Float,         // 0..1
    centerLabel: String,
    centerSub: String,
    modifier: Modifier = Modifier,
    accent: Color = LunaAccent
) {
    Box(modifier = modifier.size(220.dp), contentAlignment = Alignment.Center) {
        // 背景环色用主题 surfaceVariant，深浅色自动切换
        val ringBg = MaterialTheme.colorScheme.surfaceVariant
        Canvas(Modifier.size(220.dp)) {
            val stroke = 18.dp.toPx()
            val pad = stroke / 2
            val arcSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(pad, pad)

            drawArc(
                color = ringBg,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = (progress.coerceIn(0f, 1f)) * 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        androidx.compose.foundation.layout.Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(centerLabel, style = MaterialTheme.typography.displayLarge)
            Text(centerSub, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
