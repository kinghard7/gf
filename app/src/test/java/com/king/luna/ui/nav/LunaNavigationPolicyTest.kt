package com.king.luna.ui.nav

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class LunaNavigationPolicyTest {

    @Test
    fun `显式日志日期路由不恢复旧状态`() {
        assertThat(LunaNavigationPolicy.shouldRestoreState("log/2026-05-06")).isFalse()
    }

    @Test
    fun `普通底部 tab 路由恢复旧状态`() {
        assertThat(LunaNavigationPolicy.shouldRestoreState(LunaRoutes.CALENDAR)).isTrue()
        assertThat(LunaNavigationPolicy.shouldRestoreState(LunaRoutes.INSIGHT)).isTrue()
    }

    @Test
    fun `底部顶级路由不包含记录 tab`() {
        assertThat(LunaTopLevelRoutes.keys).containsExactly(
            LunaRoutes.TODAY,
            LunaRoutes.CALENDAR,
            LunaRoutes.INSIGHT,
            LunaRoutes.NOTIFICATIONS
        ).inOrder()
    }
}
