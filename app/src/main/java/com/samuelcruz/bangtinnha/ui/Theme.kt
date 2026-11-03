package com.samuelcruz.bangtinnha.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF1E5BB8),
    secondary = Color(0xFF00897B),
    background = Color(0xFFF7F9FC),
    surface = Color(0xFFF7F9FC),
    surfaceContainerLow = Color(0xFFFFFFFF),
)

private val Dark = darkColorScheme(primary = Color(0xFF9EC2FF), secondary = Color(0xFF80CBC4))

@Composable
fun BangTinNhaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
