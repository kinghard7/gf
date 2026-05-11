package com.king.luna

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.king.luna.ui.nav.LunaApp
import com.king.luna.ui.theme.LunaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as LunaApplication).container
        setContent {
            LunaTheme { LunaApp(container) }
        }
    }
}
