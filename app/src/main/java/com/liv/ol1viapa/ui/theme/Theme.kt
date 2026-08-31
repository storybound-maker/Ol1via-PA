package com.liv.ol1viapa.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LeauColorScheme = darkColorScheme(
    primary = LeauLime,
    onPrimary = ColorBlack,
    primaryContainer = LeauSurfaceVariant,
    onPrimaryContainer = LeauText,
    secondary = LeauMint,
    onSecondary = ColorBlack,
    secondaryContainer = LeauSurfaceVariant,
    onSecondaryContainer = LeauText,
    tertiary = LeauGlow,
    onTertiary = ColorBlack,
    background = LeauBackground,
    onBackground = LeauText,
    surface = LeauBackground,
    onSurface = LeauText,
    surfaceVariant = LeauSurfaceVariant,
    onSurfaceVariant = LeauText,
    outline = LeauMuted
)

private val ColorBlack = androidx.compose.ui.graphics.Color(0xFF07110E)

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
