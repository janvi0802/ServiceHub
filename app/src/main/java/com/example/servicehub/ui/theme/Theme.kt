package com.example.servicehub.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Only a light theme: dark mode is in "future scope".
private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE5FF),
    onPrimaryContainer = BrandBlueDark,
    secondary = BrandOrange,
    onSecondary = Color.White,
    background = BackgroundGrey,
    onBackground = TextDark,
    surface = Color.White,
    onSurface = TextDark,
)

@Composable
fun ServiceHubTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
