package com.king.luna.ui.nav

// 路由表：单一来源，避免散落字符串
object LunaRoutes {
    const val TODAY = "today"
    const val CALENDAR = "calendar"
    const val LOG = "log/{date}"      // 带日期参数
    const val INSIGHT = "insight"
    const val NOTIFICATIONS = "notifications"
    const val PRIVACY_POLICY = "privacy_policy"

    // 不带日期跳转时用的路由（今天）
    fun logRoute(date: java.time.LocalDate) = "log/${date}"
    val LOG_TODAY = "log/${java.time.LocalDate.now()}"
}

object LunaNavigationPolicy {
    fun shouldRestoreState(route: String): Boolean =
        !route.startsWith("log/")
}

object LunaTopLevelRoutes {
    val keys = listOf(
        LunaRoutes.TODAY,
        LunaRoutes.CALENDAR,
        LunaRoutes.INSIGHT,
        LunaRoutes.NOTIFICATIONS
    )
}
