package com.king.luna.ui.screen.notification

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.king.luna.data.settings.SettingsRepository
import com.king.luna.domain.reminder.ReminderPlanner
import com.king.luna.domain.reminder.ReminderSettings
import com.king.luna.notification.LunaImmediateNotification
import com.king.luna.ui.common.LunaSegmentedTwoTabs
import com.king.luna.ui.common.PrimaryButton
import com.king.luna.ui.theme.LunaAccent
import com.king.luna.ui.theme.LunaColors
import com.king.luna.ui.theme.LunaInkMuted
import com.king.luna.ui.theme.LunaCardShape
import com.king.luna.ui.theme.lunaCard
import com.king.luna.ui.theme.lunaHeaderStyle
import com.king.luna.ui.theme.lunaMetaStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun NotificationScreen(settingsRepo: SettingsRepository) {
    val settings by settingsRepo.flow.collectAsState(initial = ReminderSettings())
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val activity = context as? ComponentActivity

    var tabIndex by remember { mutableIntStateOf(0) }

    fun sendImmediateResolved() {
        when (tabIndex) {
            0 -> {
                val title = ReminderPlanner.resolvedPeriodTitle(settings)
                val body = ReminderPlanner.resolvedPeriodBody(settings)
                if (LunaImmediateNotification.show(context, title, body)) {
                    Toast.makeText(context, "已发送通知", Toast.LENGTH_SHORT).show()
                }
            }
            else -> {
                val title = ReminderPlanner.resolvedOvulationTitle(settings)
                val body = ReminderPlanner.resolvedOvulationBody(settings)
                if (LunaImmediateNotification.show(context, title, body)) {
                    Toast.makeText(context, "已发送通知", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val notifyPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) sendImmediateResolved()
        else Toast.makeText(context, "需要通知权限才能发送", Toast.LENGTH_SHORT).show()
    }

    fun triggerImmediate() {
        when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> sendImmediateResolved()
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED -> sendImmediateResolved()
            activity != null ->
                notifyPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else ->
                Toast.makeText(context, "无法请求通知权限", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("NOTIFICATIONS", style = lunaMetaStyle())
        Text("通知", style = lunaHeaderStyle())
        Spacer(Modifier.height(8.dp))
        LunaSegmentedTwoTabs(
            left = "经期",
            right = "排卵",
            selectedIndex = tabIndex,
            onSelect = { tabIndex = it }
        )

        SettingsSectionCard(title = "立即发送") {
            Text(
                "发送当前分类下已设置的标题与正文；留空则使用下方默认文案。",
                style = MaterialTheme.typography.bodySmall.copy(color = LunaInkMuted)
            )
            Spacer(Modifier.height(8.dp))
            PrimaryButton(
                text = "立即发送通知",
                onClick = { triggerImmediate() },
                modifier = Modifier.fillMaxWidth()
            )
        }

        SettingsSectionCard(title = "提醒时间") {
            Text(
                "每天在下方时刻触发已开启的提醒（本地时间）",
                style = MaterialTheme.typography.bodySmall.copy(color = LunaInkMuted)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${settings.hourOfDay.toString().padStart(2, '0')} : ${
                    settings.minuteOfHour.toString().padStart(2, '0')
                }",
                style = MaterialTheme.typography.titleMedium
            )
            Text("小时", style = lunaMetaStyle())
            Slider(
                value = settings.hourOfDay.toFloat(),
                onValueChange = { v ->
                    scope.launch {
                        settingsRepo.update { it.copy(hourOfDay = v.roundToInt().coerceIn(0, 23)) }
                    }
                },
                valueRange = 0f..23f,
                steps = 22,
                modifier = Modifier.fillMaxWidth()
            )
            Text("分钟", style = lunaMetaStyle())
            Slider(
                value = settings.minuteOfHour.toFloat(),
                onValueChange = { v ->
                    scope.launch {
                        settingsRepo.update { it.copy(minuteOfHour = v.roundToInt().coerceIn(0, 59)) }
                    }
                },
                valueRange = 0f..59f,
                steps = 58,
                modifier = Modifier.fillMaxWidth()
            )
        }

        SettingsSectionCard(title = "提醒内容") {
            when (tabIndex) {
                0 -> PeriodReminderFields(settings, settingsRepo, scope)
                else -> OvulationReminderFields(settings, settingsRepo, scope)
            }
        }

        SettingsSectionCard(title = "说明") {
            Text(
                """
                · 经期：提前天数为 0 表示在「预测的下次经期首日」当天提醒；1～7 表示再往前推相应天数。
                · 排卵：需在预测中存在排卵日时才会定时提醒。
                · Android 13 及以上请在系统中允许 Luna 发送通知。
                """.trimIndent(),
                style = MaterialTheme.typography.bodySmall.copy(color = LunaInkMuted)
            )
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun PeriodReminderFields(
    settings: ReminderSettings,
    settingsRepo: SettingsRepository,
    scope: CoroutineScope
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("经期提醒", style = MaterialTheme.typography.bodyLarge)
            Text(
                "一条规则：调整提前天数即可（0 = 预测首日）",
                style = MaterialTheme.typography.bodySmall.copy(color = LunaInkMuted)
            )
        }
        Switch(
            checked = settings.periodReminderEnabled,
            onCheckedChange = { on ->
                scope.launch { settingsRepo.update { it.copy(periodReminderEnabled = on) } }
            },
            colors = SwitchDefaults.colors(checkedThumbColor = LunaAccent)
        )
    }
    if (settings.periodReminderEnabled) {
        Spacer(Modifier.height(8.dp))
        Text(
            "提前 ${settings.periodLeadDays} 天（0 = 预测经期首日当天）",
            style = MaterialTheme.typography.bodyMedium
        )
        Slider(
            value = settings.periodLeadDays.toFloat(),
            onValueChange = { v ->
                scope.launch {
                    settingsRepo.update {
                        it.copy(periodLeadDays = v.roundToInt().coerceIn(0, 7))
                    }
                }
            },
            valueRange = 0f..7f,
            steps = 6,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = settings.periodTitle,
            onValueChange = { v -> scope.launch { settingsRepo.update { it.copy(periodTitle = v) } } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("标题（留空用默认）") },
            singleLine = true
        )
        OutlinedTextField(
            value = settings.periodBody,
            onValueChange = { v -> scope.launch { settingsRepo.update { it.copy(periodBody = v) } } },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("正文（留空用默认）") },
            minLines = 2
        )
    }
}

@Composable
private fun OvulationReminderFields(
    settings: ReminderSettings,
    settingsRepo: SettingsRepository,
    scope: CoroutineScope
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("排卵日提醒", style = MaterialTheme.typography.bodyLarge)
            Text(
                "预测排卵日当天提醒",
                style = MaterialTheme.typography.bodySmall.copy(color = LunaInkMuted)
            )
        }
        Switch(
            checked = settings.ovulationEnabled,
            onCheckedChange = { on ->
                scope.launch { settingsRepo.update { it.copy(ovulationEnabled = on) } }
            },
            colors = SwitchDefaults.colors(checkedThumbColor = LunaAccent)
        )
    }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = settings.ovulationTitle,
        onValueChange = { v -> scope.launch { settingsRepo.update { it.copy(ovulationTitle = v) } } },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("标题（留空用默认）") },
        singleLine = true
    )
    OutlinedTextField(
        value = settings.ovulationBody,
        onValueChange = { v -> scope.launch { settingsRepo.update { it.copy(ovulationBody = v) } } },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("正文（留空用默认）") },
        minLines = 2
    )
}

@Composable
private fun SettingsSectionCard(title: String, content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .lunaCard()
            .background(LunaColors.card, LunaCardShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(title, style = lunaMetaStyle())
        content()
    }
}
