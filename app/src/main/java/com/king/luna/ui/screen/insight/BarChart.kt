package com.king.luna.ui.screen.insight

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.max

// 一个极简横排柱状图：N 根柱子按高度比例显示，柱顶标长度，柱下标日期/标签。
// 不引入图表库；MVP+ 不值得为这点功能拉 MPAndroidChart。
@Composable
fun BarChart(
    bars: List<BarDatum>,
    barColor: Color,
    modifier: Modifier = Modifier,
    maxValueOverride: Int? = null
) {
    if (bars.isEmpty()) {
        EmptyBars(modifier)
        return
    }
    val maxV = max(maxValueOverride ?: bars.maxOf { it.value }, 1)
    Row(
        modifier = modifier.heightIn(min = 140.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { b ->
            BarColumn(
                value = b.value,
                topLabel = b.value.toString(),
                bottomLabel = b.label,
                ratio = b.value.toFloat() / maxV,
                color = barColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun BarColumn(
    value: Int,
    topLabel: String,
    bottomLabel: String,
    ratio: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Text(
            topLabel,
            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        // 柱体本身：固定容器 100dp，按比例填充
        Box(
            Modifier
                .fillMaxWidth()
                .height(100.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                Modifier
                    .widthIn(max = 24.dp)
                    .fillMaxWidth()
                    .fillMaxHeight(ratio.coerceIn(0.05f, 1f))
                    .background(color, RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            bottomLabel,
            style = MaterialTheme.typography.labelSmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

@Composable
private fun EmptyBars(modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "数据不足，记一两次经期就能看到了",
            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}

data class BarDatum(val label: String, val value: Int)
