package com.king.luna.domain.reminder

// 经期提醒合并为一类：提前天数 0 = 预测经期首日当天，1～7 = 提前 N 天。
data class ReminderSettings(
    val periodReminderEnabled: Boolean = true,
    val periodLeadDays: Int = 0,           // 0..7，0 = 预测下次经期首日当天提醒
    val ovulationEnabled: Boolean = false,
    val hourOfDay: Int = 9,                // 0..23
    val minuteOfHour: Int = 0,           // 0..59
    /** 空字符串表示使用 [ReminderPlanner] 内置默认文案 */
    val periodTitle: String = "",
    val periodBody: String = "",
    val ovulationTitle: String = "",
    val ovulationBody: String = ""
) {
    val anyEnabled: Boolean
        get() = periodReminderEnabled || ovulationEnabled
}
