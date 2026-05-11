package com.king.luna.ui.screen.insight

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.king.luna.domain.insight.InsightStats
import com.king.luna.domain.model.FlowLevel
import com.king.luna.ui.theme.LunaAccent
import com.king.luna.ui.theme.LunaAccentSoft
import com.king.luna.ui.theme.LunaCardShape
import com.king.luna.ui.theme.LunaColors
import com.king.luna.ui.theme.LunaOvulation
import com.king.luna.ui.theme.lunaCard
import com.king.luna.ui.theme.lunaHeaderStyle
import com.king.luna.ui.theme.lunaMetaStyle
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun InsightScreen(repo: CycleRepository) {
    val vm: InsightViewModel = viewModel(factory = InsightViewModel.Factory(repo))
    val s by vm.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header: eyebrow + serif
        Text("INSIGHTS", style = lunaMetaStyle())
        Text("周期洞察", style = lunaHeaderStyle())

        // B3: 平均周期主卡 — 红色填充突出
        AvgCycleHero(s)

        SummaryRow(s)
        CycleSection(s)
        PeriodSection(s)
        FlowSection(s)
        Spacer(Modifier.height(8.dp))
    }
}

// B3: 大号主卡，红底白字，一眼看到平均周期
@Composable
private fun AvgCycleHero(s: InsightStats) {
    Row(
        Modifier
            .fillMaxWidth()
            .lunaCard()
            .background(LunaAccent, LunaCardShape)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("平均周期", style = lunaMetaStyle().copy(color = LunaAccentSoft))
            Text("你的周期规律", style = MaterialTheme.typography.bodyMedium.copy(color = LunaAccentSoft))
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${s.avgCycle}",
                style = MaterialTheme.typography.displayLarge.copy(
                    color = LunaColors.card,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "天",
                style = MaterialTheme.typography.titleMedium.copy(
                    color = LunaAccentSoft,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}

// 剩余两卡片：平均经期 + 周期波动
@Composable
private fun SummaryRow(s: InsightStats) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(Modifier.weight(1f), "平均经期", "${s.avgPeriod}", "天", LunaOvulation)
        StatCard(
            Modifier.weight(1f),
            "周期波动",
            if (s.cycleStdDev < 0) "—" else "±${s.cycleStdDev}",
            if (s.cycleStdDev < 0) "样本不足" else "天",
            LunaAccentSoft
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    title: String,
    value: String,
    unit: String,
    accent: androidx.compose.ui.graphics.Color
) {
    Column(
        modifier = modifier
            .lunaCard()
            .background(LunaColors.card, LunaCardShape)
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Text(title, style = lunaMetaStyle())
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall.copy(
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(Modifier.width(4.dp))
            Text(
                unit,
                style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }
    }
}

@Composable
private fun CycleSection(s: InsightStats) {
    SectionCard(title = "周期长度", subtitle = "最近 ${s.cycleBars.size} 段") {
        BarChart(
            bars = s.cycleBars.map {
                BarDatum(label = it.start.format(MD_FMT), value = it.lengthDays)
            }.reversed(),
            barColor = LunaAccent
        )
    }
}

@Composable
private fun PeriodSection(s: InsightStats) {
    SectionCard(title = "经期长度", subtitle = "最近 ${s.periodBars.size} 次") {
        BarChart(
            bars = s.periodBars.map {
                BarDatum(label = it.start.format(MD_FMT), value = it.lengthDays)
            }.reversed(),
            barColor = LunaOvulation
        )
    }
}

@Composable
private fun FlowSection(s: InsightStats) {
    SectionCard(title = "流量分布", subtitle = "累计天数") {
        val ordered = listOf(FlowLevel.LIGHT, FlowLevel.MEDIUM, FlowLevel.HEAVY)
        BarChart(
            bars = ordered.map {
                BarDatum(label = it.zh, value = s.flowDays[it] ?: 0)
            },
            barColor = LunaAccentSoft
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        Modifier
            .fillMaxWidth()
            .lunaCard()
            .background(LunaColors.card, LunaCardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = lunaMetaStyle())
            Text(subtitle, style = lunaMetaStyle())
        }
        content()
    }
}

private val MD_FMT = DateTimeFormatter.ofPattern("M/d", Locale.CHINA)

private val FlowLevel.zh: String
    get() = when (this) {
        FlowLevel.NONE -> "无"
        FlowLevel.LIGHT -> "轻"
        FlowLevel.MEDIUM -> "中"
        FlowLevel.HEAVY -> "重"
    }
