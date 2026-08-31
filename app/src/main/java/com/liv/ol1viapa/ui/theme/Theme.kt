package com.liv.ol1viapa.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LeauColorScheme = darkColorScheme(
    primary = LeauLime,
    onPrimary = Color.Black,
    primaryContainer = LeauSurfaceVariant,
    onPrimaryContainer = LeauText,
    secondary = LeauMint,
    onSecondary = Color.Black,
    secondaryContainer = LeauSurfaceVariant,
    onSecondaryContainer = LeauText,
    tertiary = LeauGlow,
    onTertiary = Color.Black,
    background = LeauBackground,
    onBackground = LeauText,
    surface = LeauBackground,
    onSurface = LeauText,
    surfaceVariant = LeauSurfaceVariant,
    onSurfaceVariant = LeauText,
    outline = LeauMuted
)

@Composable
fun LeauPATheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LeauColorScheme,
        typography = LeauTypography,
        content = content
    )
}
