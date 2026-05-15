package com.king.luna.ui.nav

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.king.luna.AppContainer
import com.king.luna.ui.common.BottomTabBar
import com.king.luna.ui.common.TabItem
import com.king.luna.ui.screen.calendar.CalendarScreen
import com.king.luna.ui.screen.insight.InsightScreen
import com.king.luna.ui.screen.log.LogScreen
import com.king.luna.ui.screen.notification.NotificationScreen
import com.king.luna.ui.screen.today.TodayScreen
import java.time.LocalDate

@Composable
fun LunaApp(container: AppContainer) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val rawRoute = backStack?.destination?.route ?: LunaRoutes.TODAY
    val current = if (rawRoute.startsWith("log")) LunaRoutes.CALENDAR else rawRoute

    val tabs = listOf(
        TabItem(LunaRoutes.TODAY, "今日", Icons.Outlined.Favorite),
        TabItem(LunaRoutes.CALENDAR, "日历", Icons.Outlined.CalendarMonth),
        TabItem(LunaRoutes.INSIGHT, "洞察", Icons.Outlined.BarChart),
        TabItem(LunaRoutes.NOTIFICATIONS, "通知", Icons.Outlined.Notifications)
    )

    // 统一的"切 Tab"导航：底栏切换 + 屏幕内跳转都走它，避免 Log 被裸 navigate 叠多层
    val switchTab: (String) -> Unit = { route ->
        // "log" 从底栏点击 → 跳到今天的日志页
        val target = if (route == "log") LunaRoutes.logRoute(LocalDate.now()) else route
        nav.navigate(target) {
            popUpTo(nav.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = LunaNavigationPolicy.shouldRestoreState(target)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            BottomTabBar(
                tabs = tabs,
                current = current,
                onSelect = switchTab
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            LunaNavGraph(nav, container, switchTab)
        }
    }
}

@Composable
private fun LunaNavGraph(
    nav: NavHostController,
    container: AppContainer,
    switchTab: (String) -> Unit
) {
    NavHost(navController = nav, startDestination = LunaRoutes.TODAY) {
        composable(LunaRoutes.TODAY) {
            TodayScreen(
                repo = container.cycleRepository,
                onJumpLog = { switchTab(LunaRoutes.logRoute(LocalDate.now())) }
            )
        }
        composable(LunaRoutes.CALENDAR) {
            CalendarScreen(
                repo = container.cycleRepository,
                onPickDay = { date -> switchTab(LunaRoutes.logRoute(date)) }
            )
        }
        composable(
            route = LunaRoutes.LOG,
            arguments = listOf(navArgument("date") { type = NavType.StringType })
        ) { backStackEntry ->
            val dateStr = backStackEntry.arguments?.getString("date") ?: LocalDate.now().toString()
            LogScreen(
                repo = container.cycleRepository,
                initialDate = LocalDate.parse(dateStr)
            )
        }
        composable(LunaRoutes.INSIGHT) {
            InsightScreen(repo = container.cycleRepository)
        }
        composable(LunaRoutes.NOTIFICATIONS) {
            NotificationScreen(settingsRepo = container.settingsRepository)
        }
    }
}
