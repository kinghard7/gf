package com.king.luna.ui.screen.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.king.luna.domain.model.CyclePrediction
import com.king.luna.ui.theme.LunaAccent
import com.king.luna.ui.theme.LunaAccentSoft
import com.king.luna.ui.theme.LunaInkMuted
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

private val Weekdays = listOf("一", "二", "三", "四", "五", "六", "日")

private val DayCellShape = RoundedCornerShape(12.dp)
private val DayCellPadding = 3.dp

@Composable
fun MonthGrid(
    month: YearMonth,
    today: LocalDate,
    periodDays: Set<LocalDate>,
    loggedDates: Set<LocalDate>,
    prediction: CyclePrediction,
    onDayClick: (LocalDate) -> Unit
) {
    val firstDay = month.atDay(1)
    val leading = (firstDay.dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
    val daysInMonth = month.lengthOfMonth()
    val prevMonth = month.minusMonths(1)
    val prevDays = prevMonth.lengthOfMonth()
    val totalCells = ((leading + daysInMonth + 6) / 7) * 7
    val nextMonth = month.plusMonths(1)

    Column(Modifier.fillMaxWidth()) {
        // 星期头：mono 小字
        Row(Modifier.fillMaxWidth()) {
            Weekdays.forEach { w ->
                Box(
                    modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(w, style = MaterialTheme.typography.labelSmall.copy(
                        color = LunaInkMuted,
                        fontWeight = FontWeight.Medium
                    ))
                }
            }
        }

        // 日期网格
        for (row in 0 until totalCells / 7) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val cell = row * 7 + col
                    val dayNum = cell - leading + 1
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            dayNum in 1..daysInMonth -> {
                                val date = month.atDay(dayNum)
                                DayCell(
                                    date = date,
                                    isToday = date == today,
                                    isPeriod = date in periodDays,
                                    isPredictedPeriod = isPredictedPeriod(date, prediction),
                                    isFertile = prediction.fertileWindow?.contains(date) == true,
                                    hasLog = date in loggedDates,
                                    isMuted = false,
                                    onClick = { onDayClick(date) }
                                )
                            }
                            // 上月灰色填充
                            cell < leading -> {
                                val d = prevMonth.atDay(prevDays - leading + cell + 1)
                                DayCell(date = d, isMuted = true, onClick = { onDayClick(d) })
                            }
                            // 下月灰色填充
                            else -> {
                                val d = nextMonth.atDay(dayNum - daysInMonth)
                                DayCell(date = d, isMuted = true, onClick = { onDayClick(d) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    isToday: Boolean = false,
    isPeriod: Boolean = false,
    isPredictedPeriod: Boolean = false,
    isFertile: Boolean = false,
    hasLog: Boolean = false,
    isMuted: Boolean = false,
    onClick: () -> Unit
) {
    // 前景色：经期=白，其余正常
    val fg = when {
        isPeriod -> Color.White
        isMuted -> LunaInkMuted.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.onBackground
    }

    val showPredictedRing = isPredictedPeriod && !isPeriod
    val todayBorderColor = if (isToday) LunaInkMuted else Color.Transparent
    val todayBorderWidth = if (isToday) 2.dp else 0.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .padding(DayCellPadding)
            .then(
                if (showPredictedRing) {
                    Modifier.drawBehind {
                        val strokePx = 2.dp.toPx()
                        val r = (size.minDimension - strokePx) / 2f
                        drawCircle(
                            color = LunaAccent.copy(alpha = 0.55f),
                            radius = r,
                            style = Stroke(
                                width = strokePx,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f), 0f)
                            )
                        )
                    }
                } else Modifier
            )
            .border(todayBorderWidth, todayBorderColor, DayCellShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 排卵窗口：心形水印（经期日用心形叠在红色底上会被盖住，通常不在窗口）
            if (isFertile && !isPeriod) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth(0.62f)
                        .aspectRatio(1f)
                        .alpha(0.38f),
                    tint = LunaAccentSoft
                )
            }

            // 经期：整块圆角矩形铺满格子，框住日期数字
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(
                        if (isPeriod) {
                            Modifier.background(LunaAccent, DayCellShape)
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = date.dayOfMonth.toString(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = fg,
                            fontWeight = when {
                                isPeriod -> FontWeight.SemiBold
                                isToday -> FontWeight.Bold
                                isMuted -> FontWeight.Normal
                                else -> FontWeight.Normal
                            }
                        )
                    )
                    // 非经期日的日志标记（经期整块已是红色底，不再用小点代表经期）
                    if (hasLog && !isPeriod) {
                        Box(
                            Modifier
                                .padding(top = 2.dp)
                                .size(4.dp)
                                .background(LunaAccent, CircleShape)
                        )
                    }
                }
            }
        }
    }
}

private fun isPredictedPeriod(date: LocalDate, p: CyclePrediction): Boolean {
    val start = p.nextPeriodStart ?: return false
    val end = start.plusDays((p.avgPeriodLength - 1).toLong().coerceAtLeast(0))
    return date in start..end
}
