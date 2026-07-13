package com.diprotec.inventariozebratc27.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TealPrimary,
    secondary = TealPrimaryDark,
    tertiary = TealLight,
    background = Background,
    surface = Surface,
    onPrimary = White,
    onSecondary = White,
    onTertiary = TextPrimary,
    onBackground = White,
    onSurface = White,
    error = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = TealPrimary,
    secondary = TealPrimaryDark,
    tertiary = TealLight,
    background = Background,
    surface = Surface,
    onPrimary = White,
    onSecondary = White,
    onTertiary = TextPrimary,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = ErrorRed
)

@Composable
fun InventarioTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}