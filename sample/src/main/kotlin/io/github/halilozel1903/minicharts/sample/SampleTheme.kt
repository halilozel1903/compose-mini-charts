package io.github.halilozel1903.minicharts.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** An indigo finance theme. The charts take all their colors from it. */
@Composable
fun SampleTheme(dark: Boolean, content: @Composable () -> Unit) {
    val colors = if (dark) {
        darkColorScheme(
            primary = Color(0xFFA9B4FF),
            onPrimary = Color(0xFF14206B),
            primaryContainer = Color(0xFF2E3A8F),
            onPrimaryContainer = Color(0xFFDDE1FF),
            secondary = Color(0xFFB9C3E8),
            secondaryContainer = Color(0xFF2C3247),
            onSecondaryContainer = Color(0xFFDDE3FF),
            tertiary = Color(0xFFFF97B5),
            error = Color(0xFFFF8A80),
            background = Color(0xFF0F1117),
            surface = Color(0xFF0F1117),
            surfaceVariant = Color(0xFF2A2E3A),
            onSurfaceVariant = Color(0xFFB4B9C9),
            surfaceContainerLow = Color(0xFF181B23),
            surfaceContainer = Color(0xFF1D202A),
            surfaceContainerHigh = Color(0xFF262A35),
            outlineVariant = Color(0xFF3A3F4D),
            inverseSurface = Color(0xFFE4E6EF),
            inverseOnSurface = Color(0xFF1A1C23),
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF4353D6),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFDDE1FF),
            onPrimaryContainer = Color(0xFF0E1A6B),
            secondary = Color(0xFF5A6380),
            secondaryContainer = Color(0xFFE2E6F7),
            onSecondaryContainer = Color(0xFF181D30),
            tertiary = Color(0xFFD9466F),
            error = Color(0xFFC62828),
            background = Color(0xFFF4F5FA),
            surface = Color(0xFFF4F5FA),
            surfaceVariant = Color(0xFFE3E5EE),
            onSurfaceVariant = Color(0xFF5B6070),
            surfaceContainerLow = Color(0xFFFFFFFF),
            surfaceContainer = Color(0xFFEDEEF5),
            surfaceContainerHigh = Color(0xFFFFFFFF),
            outlineVariant = Color(0xFFDADCE6),
            inverseSurface = Color(0xFF22252F),
            inverseOnSurface = Color(0xFFF1F2F8),
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}
