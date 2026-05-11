package com.king.luna.ui.screen.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.king.luna.domain.model.FlowLevel
import com.king.luna.ui.theme.LunaAccent
import com.king.luna.ui.theme.LunaAccentSoft
import com.king.luna.ui.theme.LunaCardShape
import com.king.luna.ui.theme.lunaMetaStyle

// 流量色条映射：无=灰，轻=淡粉，中=标准，重=深
@Composable
private fun FlowLevel.barColor() = when (this) {
    FlowLevel.NONE -> MaterialTheme.colorScheme.outline
    FlowLevel.LIGHT -> LunaAccentSoft
    FlowLevel.MEDIUM -> LunaAccent
    FlowLevel.HEAVY -> LunaAccent.copy(alpha = 0.85f)
}

@Composable
fun FlowSelector(
    selected: FlowLevel,
    onSelect: (FlowLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FlowLevel.values().forEach { lv ->
            val active = lv == selected
            Column(
                modifier = Modifier
                    .weight(1f)
                    .let { m ->
                        if (active) m.border(
                            1.5.dp,
                            LunaAccent,
                            LunaCardShape
                        ) else m
                    }
                    .background(MaterialTheme.colorScheme.surface, LunaCardShape)
                    .clickable { onSelect(lv) }
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // 顶部色条
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(lv.barColor(), RoundedCornerShape(2.dp))
                )
                Text(
                    lv.label,
                    style = lunaMetaStyle().copy(
                        color = if (active) LunaAccent
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
