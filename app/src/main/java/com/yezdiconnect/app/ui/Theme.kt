package com.yezdiconnect.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Amber = Color(0xFFE0A030)

@Composable
fun YezdiTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) {
        darkColorScheme(primary = Amber, secondary = Amber)
    } else {
        lightColorScheme(primary = Color(0xFF8A5A00), secondary = Color(0xFF8A5A00))
    }
    MaterialTheme(colorScheme = colors, content = content)
}
