package com.king.luna.ui.screen.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.king.luna.data.repo.CycleRepository
import com.king.luna.domain.model.CyclePrediction
import com.king.luna.domain.model.Phase
import com.king.luna.ui.common.PillBadge
import com.king.luna.ui.common.PrimaryButton
import com.king.luna.ui.theme.LunaAccent
import com.king.luna.ui.theme.LunaAccentSoft
import com.king.luna.ui.theme.LunaCardShape
import com.king.luna.ui.theme.LunaColors
import com.king.luna.ui.theme.LunaOvulation
import com.king.luna.ui.theme.lunaCard
import com.king.luna.ui.theme.lunaHeaderStyle
import com.king.luna.ui.theme.lunaMetaStyle
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun TodayScreen(
    repo: CycleRepository,
    onJumpLog: () -> Unit
) {
    val vm: TodayViewModel = viewModel(factory = TodayViewModel.Factory(repo))
    val ui by vm.state.collectAsState()
    TodayContent(ui = ui, onTogglePeriod = vm::togglePeriod, onJumpLog = onJumpLog)
}

@Composable
private fun TodayContent(ui: TodayUiState, onTogglePeriod: () -> Unit, onJumpLog: () -> Unit) {
    val p = ui.prediction
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header: eyebrow + serif 主标 + 阶段徽章
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("TODAY'S CYCLE", style = lunaMetaStyle())
                Text("Luna", style = lunaHeaderStyle())
            }
            PillBadge(text = phaseLabel(p.phase), container = phaseColor(p.phase).copy(alpha = 0.2f))
        }
        Spacer(Modifier.height(20.dp))

        // 圆环
        val (centerLabel, centerSub) = ringLabels(ui.today, p)
        val progress = ringProgress(p)
        CycleRing(progress = progress, centerLabel = centerLabel, centerSub = centerSub, accent = phaseColor(p.phase))

        Spacer(Modifier.height(20.dp))

        // B1: Luna 提醒卡 — 阶段感知文案
        LunaTipCard(phase = p.phase)

        Spacer(Modifier.height(12.dp))

        // B2: 下次周期横排卡 — 左文右大数字
        NextCycleCard(today = ui.today, p = p)

        Spacer(Modifier.height(12.dp))

        // 信息卡片
        InfoCard(today = ui.today, p = p)

        Spacer(Modifier.height(24.dp))

        // 经期切换按钮：根据当前状态显示不同文案
        PrimaryButton(
            text = if (ui.todayIsPeriod) "取消经期标记" else "标记为经期",
            onClick = onTogglePeriod,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        PrimaryButton(text = "记录详情", onClick = onJumpLog, modifier = Modifier.fillMaxWidth())
    }
}

// B1: 阶段感知提示卡
@Composable
private fun LunaTipCard(phase: Phase) {
    val (tip, icon) = when (phase) {
        Phase.PERIOD -> "经期注意保暖，避免生冷食物" to "🌙"
        Phase.FOLLICULAR -> "精力充沛的好时段，适合运动" to "🌱"
        Phase.OVULATION -> "排卵期，身体状态最佳" to "✨"
        Phase.LUTEAL -> "可能感到疲惫，适当放松" to "🍂"
        Phase.UNKNOWN -> "开始记录，了解你的周期" to "📝"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .lunaCard()
            .background(LunaColors.card, LunaCardShape)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(icon, style = MaterialTheme.typography.headlineSmall)
        Text(tip, style = MaterialTheme.typography.bodyMedium)
    }
}

// B2: 下次周期 — 横排：左文案，右大数字
@Composable
private fun NextCycleCard(today: LocalDate, p: CyclePrediction) {
    val days = p.nextPeriodStart?.let {
        ChronoUnit.DAYS.between(today, it).toInt()
    }
    val label = when {
        days == null -> "下次经期"
        days > 0 -> "天后下次经期"
        days == 0 -> "今天来经期"
        else -> "经期已过"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .lunaCard()
            .background(LunaColors.card, LunaCardShape)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("NEXT PERIOD", style = lunaMetaStyle())
            Text(label, style = MaterialTheme.typography.bodyMedium)
        }
        days?.let {
            Text(
                "$it",
                style = MaterialTheme.typography.displayLarge.copy(
                    color = LunaAccent,
                    fontWeight = FontWeight.Bold
                )
            )
        } ?: Text("—", style = MaterialTheme.typography.displayLarge)
    }
}

@Composable
private fun InfoCard(today: LocalDate, p: CyclePrediction) {
    Column(
        Modifier
            .fillMaxWidth()
            .lunaCard()
            .background(LunaColors.card, LunaCardShape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        InfoRow("平均周期", "${p.avgCycleLength} 天")
        InfoRow("平均经期", "${p.avgPeriodLength} 天")
        p.ovulationDay?.let {
            InfoRow("预计排卵", it.format(DateTimeFormatter.ofPattern("M 月 d 日")))
        } ?: InfoRow("预计排卵", "—")
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = lunaMetaStyle())
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

private fun phaseLabel(phase: Phase): String = when (phase) {
    Phase.UNKNOWN -> "未记录"
    Phase.PERIOD -> "经期中"
    Phase.FOLLICULAR -> "卵泡期"
    Phase.OVULATION -> "排卵期"
    Phase.LUTEAL -> "黄体期"
}

@Composable
private fun phaseColor(phase: Phase) = when (phase) {
    Phase.PERIOD -> LunaAccent
    Phase.OVULATION -> LunaOvulation
    Phase.FOLLICULAR -> LunaColors.follicular
    Phase.LUTEAL -> LunaColors.luteal
    Phase.UNKNOWN -> LunaAccent
}

private fun ringLabels(today: LocalDate, p: CyclePrediction): Pair<String, String> {
    if (p.cycleStart == null || p.nextPeriodStart == null) return "—" to "无记录"
    return when (p.phase) {
        Phase.PERIOD -> "${p.cycleDay ?: 1}" to "经期第 N 天"
        else -> {
            val days = ChronoUnit.DAYS.between(today, p.nextPeriodStart).toInt().coerceAtLeast(0)
            "$days" to "天后下次经期"
        }
    }
}

private fun ringProgress(p: CyclePrediction): Float {
    val day = p.cycleDay ?: return 0f
    val total = p.avgCycleLength.coerceAtLeast(1)
    return (day.toFloat() / total).coerceIn(0f, 1f)
}
