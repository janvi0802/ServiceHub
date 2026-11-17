package com.example.servicehub

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.servicehub.data.CategoryStore
import com.example.servicehub.navigation.AppNavHost
import com.example.servicehub.ui.theme.ServiceHubTheme

/** The only Activity. Every screen is a Composable inside AppNavHost. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Keep the category list (built-in + admin-added) up to date for the whole app.
        CategoryStore.start()
        // Our app is always light, so ask for DARK status-bar icons (clock, battery)
        // even when the phone is in dark mode. Otherwise they would be white on white.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        setContent {
            ServiceHubTheme {
                AppNavHost()
            }
        }
    }
}
