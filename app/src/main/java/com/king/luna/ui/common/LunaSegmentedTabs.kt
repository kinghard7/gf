package com.king.luna.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.king.luna.ui.theme.LunaCardShape
import com.king.luna.ui.theme.lunaCardBorder

private val SegmentShape = RoundedCornerShape(14.dp)

/**
 * 双栏分割选项：与 [ToggleChip]、卡片圆角同一套语言，避免 Material Tab 下划线与 Luna 页割裂。
 *
 * 选中项使用主题色：第 0 项 primary（经期）、第 1 项 tertiary（排卵）。
 */
@Composable
fun LunaSegmentedTwoTabs(
    left: String,
    right: String,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(LunaCardShape)
            .border(lunaCardBorder(), LunaCardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SegmentCell(
            label = left,
            selected = selectedIndex == 0,
            selectedContainer = MaterialTheme.colorScheme.primary,
            selectedContent = MaterialTheme.colorScheme.onPrimary,
            onClick = { onSelect(0) },
            modifier = Modifier.weight(1f)
        )
        SegmentCell(
            label = right,
            selected = selectedIndex == 1,
            selectedContainer = MaterialTheme.colorScheme.tertiary,
            selectedContent = MaterialTheme.colorScheme.onTertiary,
            onClick = { onSelect(1) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SegmentCell(
    label: String,
    selected: Boolean,
    selectedContainer: Color,
    selectedContent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (selected) selectedContainer else Color.Transparent
    val fg = if (selected) {
        selectedContent
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .clip(SegmentShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = fg
            )
        )
    }
}
