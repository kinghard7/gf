package com.king.luna.ui.screen.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.king.luna.data.repo.CycleRepository
import com.king.luna.ui.theme.LunaAccent
import com.king.luna.ui.theme.LunaAccentSoft
import com.king.luna.ui.theme.LunaCardShape
import com.king.luna.ui.theme.LunaColors
import com.king.luna.ui.theme.LunaDivider
import com.king.luna.ui.theme.LunaInkMuted
import com.king.luna.ui.theme.lunaHeaderStyle
import com.king.luna.ui.theme.lunaMetaStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.YearMonth
import java.util.Locale

@Composable
fun CalendarScreen(
    repo: CycleRepository,
    onPickDay: (LocalDate) -> Unit
) {
    val vm: CalendarViewModel = viewModel(factory = CalendarViewModel.Factory(repo))
    val ui by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Header: 左 eyebrow+serif月份，右圆形按钮
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                val yearFmt = DateTimeFormatter.ofPattern("yyyy 年", Locale.CHINA)
                Text(ui.month.format(yearFmt), style = lunaMetaStyle())
                val monthFmt = DateTimeFormatter.ofPattern("M 月", Locale.CHINA)
                Text(ui.month.format(monthFmt), style = lunaHeaderStyle())
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // 圆形上/下月按钮
                CircleIconButton(onClick = vm::prevMonth, icon = { Icon(Icons.Default.ChevronLeft, "上一月") })
                CircleIconButton(onClick = vm::nextMonth, icon = { Icon(Icons.Default.ChevronRight, "下一月") })
            }
        }

        Spacer(Modifier.height(12.dp))

        // 月历网格
        MonthGrid(
            month = ui.month,
            today = LocalDate.now(),
            periodDays = ui.periodDays,
            loggedDates = ui.loggedDates,
            prediction = ui.prediction,
            onDayClick = onPickDay
        )

        Spacer(Modifier.height(18.dp))

        // 图例：一行横排，圆点+文字
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LegendPeriodChip()
            LegendFertileHeart()
            LegendPredictedRing()
        }

        Spacer(Modifier.height(18.dp))

        // 本月记录列表
        Text("本月记录", style = lunaMetaStyle())
        Spacer(Modifier.height(8.dp))

        val monthLogs = ui.dayLogs
            .filter { YearMonth.from(it.date) == ui.month }
            .sortedByDescending { it.date }
            .map(CalendarLogSummary::from)

        if (monthLogs.isEmpty()) {
            Text(
                "暂无记录，点击日期开始",
                style = MaterialTheme.typography.bodyMedium.copy(color = LunaInkMuted)
            )
        } else {
            monthLogs.forEach { summary ->
                LogSummaryRow(summary = summary, onClick = { onPickDay(summary.date) })
                Spacer(Modifier.height(8.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

// 圆形图标按钮
@Composable
private fun CircleIconButton(
    onClick: () -> Unit,
    icon: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .background(MaterialTheme.colorScheme.surface, CircleShape)
            .border(1.dp, LunaDivider, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

// 图例：与月历网格样式一致
@Composable
private fun LegendPeriodChip() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(width = 14.dp, height = 12.dp)
                .background(LunaAccent, RoundedCornerShape(4.dp))
        )
        Text("经期", style = lunaMetaStyle().copy(letterSpacing = 0.sp))
    }
}

@Composable
private fun LegendFertileHeart() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(
            Icons.Default.Favorite,
            contentDescription = null,
            modifier = Modifier.size(12.dp),
            tint = LunaAccentSoft.copy(alpha = 0.85f)
        )
        Text("排卵窗口", style = lunaMetaStyle().copy(letterSpacing = 0.sp))
    }
}

@Composable
private fun LegendPredictedRing() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(14.dp)
                .drawBehind {
                    val sw = 1.5.dp.toPx()
                    val r = (size.minDimension - sw) / 2f
                    drawCircle(
                        color = LunaAccent.copy(alpha = 0.55f),
                        radius = r,
                        style = Stroke(
                            width = sw,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f), 0f)
                        )
                    )
                }
        )
        Text("预测", style = lunaMetaStyle().copy(letterSpacing = 0.sp))
    }
}

// 日志摘要行：展示真正记录的心情、症状和笔记
@Composable
private fun LogSummaryRow(
    summary: CalendarLogSummary,
    onClick: () -> Unit
) {
    val fmt = DateTimeFormatter.ofPattern("M/d", Locale.CHINA)

    Row(
        Modifier
            .fillMaxWidth()
            .background(LunaColors.card, LunaCardShape)
            .border(1.dp, LunaDivider, LunaCardShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(40.dp)
                .background(LunaAccentSoft.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                summary.date.format(fmt),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = LunaAccent,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
        Column(Modifier.weight(1f)) {
            val tags = listOf(summary.moodText, summary.symptomText).filter { it.isNotBlank() }
            if (tags.isNotEmpty()) {
                Text(
                    tags.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (summary.noteText.isNotBlank()) {
                Text(
                    summary.noteText,
                    style = MaterialTheme.typography.bodySmall.copy(color = LunaInkMuted),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = "查看详情",
            tint = LunaInkMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}
