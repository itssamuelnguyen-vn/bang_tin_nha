package com.samuelcruz.bangtinnha.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.samuelcruz.bangtinnha.R

/** Be Vietnam Pro: an open-source typeface drawn for Vietnamese (licenses/BeVietnamPro-OFL.txt). */
val BeVietnamPro = FontFamily(
    Font(R.font.be_vietnam_pro_regular, FontWeight.Normal),
    Font(R.font.be_vietnam_pro_medium, FontWeight.Medium),
    Font(R.font.be_vietnam_pro_semibold, FontWeight.SemiBold),
    Font(R.font.be_vietnam_pro_bold, FontWeight.Bold),
    Font(R.font.be_vietnam_pro_extrabold, FontWeight.ExtraBold),
)

private val typography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.ExtraBold),
        displayMedium = displayMedium.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.ExtraBold),
        displaySmall = displaySmall.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.Bold),
        headlineLarge = headlineLarge.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.Bold),
        headlineMedium = headlineMedium.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.Bold),
        headlineSmall = headlineSmall.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.Bold),
        titleLarge = titleLarge.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.SemiBold),
        titleSmall = titleSmall.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.SemiBold),
        bodyLarge = bodyLarge.copy(fontFamily = BeVietnamPro),
        bodyMedium = bodyMedium.copy(fontFamily = BeVietnamPro),
        bodySmall = bodySmall.copy(fontFamily = BeVietnamPro),
        labelLarge = labelLarge.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.SemiBold),
        labelMedium = labelMedium.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.SemiBold),
        labelSmall = labelSmall.copy(fontFamily = BeVietnamPro, fontWeight = FontWeight.Medium),
    )
}

private val Light = lightColorScheme(
    primary = Color(0xFF6C5CE7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E5FF),
    secondary = Color(0xFF00B894),
    tertiary = Color(0xFFFF7675),
    background = Color(0xFFF5F6FC),
    surface = Color(0xFFF5F6FC),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF0F1F8),
    onSurface = Color(0xFF1E1B3A),
    onSurfaceVariant = Color(0xFF6B6A85),
    outlineVariant = Color(0xFFE4E4F0),
    error = Color(0xFFE84357),
)

private val Dark = darkColorScheme(
    primary = Color(0xFFA29BFE),
    onPrimary = Color(0xFF1B1446),
    primaryContainer = Color(0xFF2E2A5C),
    secondary = Color(0xFF55EFC4),
    tertiary = Color(0xFFFF9F9E),
    background = Color(0xFF0E0F1C),
    surface = Color(0xFF0E0F1C),
    surfaceContainer = Color(0xFF1A1B2E),
    surfaceContainerLow = Color(0xFF1A1B2E),
    surfaceContainerHigh = Color(0xFF24253B),
    onSurface = Color(0xFFF1F0FF),
    onSurfaceVariant = Color(0xFFA6A5C2),
    outlineVariant = Color(0xFF2E2F47),
    error = Color(0xFFFF6B7D),
)

/** Each module's own colours: a gradient for headers and a solid accent. */
data class ModuleColors(val start: Color, val end: Color, val accent: Color) {
    val gradient: Brush get() = Brush.linearGradient(listOf(start, end))
}

object Palette {
    val fx = ModuleColors(Color(0xFF00C9A7), Color(0xFF00B4D8), Color(0xFF00A88E))
    val fuel = ModuleColors(Color(0xFFFF9F43), Color(0xFFFF6B6B), Color(0xFFF2711C))
    val power = ModuleColors(Color(0xFF7B61FF), Color(0xFFB06AB3), Color(0xFF7B61FF))
    val settings = ModuleColors(Color(0xFF6C5CE7), Color(0xFF8E7CFF), Color(0xFF6C5CE7))
    val up = Color(0xFFE84357)
    val down = Color(0xFF00B894)
    val ok = Color(0xFF00B894)
    val warn = Color(0xFFFF9F43)

    /** Weather gradient by condition and time of day. */
    fun weather(code: Int, hour: Int): ModuleColors {
        val night = hour < 6 || hour >= 18
        return when {
            night -> ModuleColors(Color(0xFF232526), Color(0xFF414E7A), Color(0xFF414E7A))
            code >= 95 -> ModuleColors(Color(0xFF373B44), Color(0xFF4286F4), Color(0xFF4286F4))
            code >= 51 -> ModuleColors(Color(0xFF4E54C8), Color(0xFF8F94FB), Color(0xFF4E54C8))
            code >= 45 -> ModuleColors(Color(0xFF8E9EAB), Color(0xFFB8C6DB), Color(0xFF8E9EAB))
            code >= 2 -> ModuleColors(Color(0xFF56CCF2), Color(0xFF2F80ED), Color(0xFF2F80ED))
            else -> ModuleColors(Color(0xFFFF9A62), Color(0xFFFF5E8A), Color(0xFFFF5E8A))
        }
    }
}

val LocalDarkTheme = staticCompositionLocalOf { false }

@Composable
fun BangTinNhaTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    androidx.compose.runtime.CompositionLocalProvider(LocalDarkTheme provides dark) {
        MaterialTheme(colorScheme = if (dark) Dark else Light, typography = typography, content = content)
    }
}
