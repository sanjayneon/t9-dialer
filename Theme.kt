package com.t9dialer.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.t9dialer.app.data.ThemeMode

@Immutable
data class DialerColors(
    val bg: Color,          // behind the results list
    val panel: Color,       // keypad sheet, cards
    val ink: Color,
    val inkMuted: Color,
    val accent: Color,      // matched letters, primary buttons
    val onAccent: Color,
    val call: Color,
    val missed: Color,
    val avatars: List<Pair<Color, Color>>,  // background to initial
)

private val Light = DialerColors(
    bg = Color(0xFFEEF1F5),
    panel = Color(0xFFFFFFFF),
    ink = Color(0xFF17202B),
    inkMuted = Color(0xFF5D6878),
    accent = Color(0xFF2547C9),
    onAccent = Color(0xFFFFFFFF),
    call = Color(0xFF16A05D),
    missed = Color(0xFFC8392E),
    avatars = listOf(
        Color(0xFFDCE3FB) to Color(0xFF223E9E),
        Color(0xFFD6F0E3) to Color(0xFF146B43),
        Color(0xFFFBE3CF) to Color(0xFF8A4A12),
        Color(0xFFEADCF7) to Color(0xFF5B2E8C),
        Color(0xFFD5EDF2) to Color(0xFF155E6E),
        Color(0xFFF6DADF) to Color(0xFF8E2639),
    ),
)

private val Dark = DialerColors(
    bg = Color(0xFF1B212B),
    panel = Color(0xFF242C38),
    ink = Color(0xFFE7EBF1),
    inkMuted = Color(0xFF9AA5B5),
    accent = Color(0xFF9DB2FF),
    onAccent = Color(0xFF0F1E55),
    call = Color(0xFF2DB872),
    missed = Color(0xFFFF7A6E),
    avatars = listOf(
        Color(0xFF2D3A63) to Color(0xFFC7D3FF),
        Color(0xFF1F4A37) to Color(0xFFA8E6C6),
        Color(0xFF4E3620) to Color(0xFFF5C9A0),
        Color(0xFF3E2D57) to Color(0xFFDCC4F5),
        Color(0xFF1E4650) to Color(0xFFA9DCE7),
        Color(0xFF55262F) to Color(0xFFF5B9C4),
    ),
)

val LocalDialerColors = staticCompositionLocalOf { Light }

/** Settings > Appearance > Multiline mode: names may use two lines. */
val LocalMultilineNames = staticCompositionLocalOf { false }

/** Settings > Appearance > Show company and job. */
val LocalShowOrg = staticCompositionLocalOf { false }

/** Whether to draw dark, given the setting and (for System default) the phone's own mode. */
@Composable
fun isDark(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun DialerTheme(dark: Boolean, content: @Composable () -> Unit) {
    val c = if (dark) Dark else Light
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.onAccent,
            background = c.bg, onBackground = c.ink,
            surface = c.panel, onSurface = c.ink, onSurfaceVariant = c.inkMuted,
            surfaceContainerLow = c.panel,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.onAccent,
            background = c.bg, onBackground = c.ink,
            surface = c.panel, onSurface = c.ink, onSurfaceVariant = c.inkMuted,
            surfaceContainerLow = c.panel,
        )
    }
    CompositionLocalProvider(LocalDialerColors provides c) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
