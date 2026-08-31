package com.l1vo.ol1via.pa.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LeauColors = darkColorScheme(
    primary = Color(0xFFB9FF63),
    onPrimary = Color(0xFF152000),
    secondary = Color(0xFF76E0B4),
    onSecondary = Color(0xFF002117),
    background = Color(0xFF0A0F10),
    onBackground = Color(0xFFF0F7F2),
    surface = Color(0xFF111819),
    onSurface = Color(0xFFF0F7F2),
    surfaceVariant = Color(0xFF172021),
    onSurfaceVariant = Color(0xFFB8C5C0),
    outline = Color(0xFF55635D)
)

@Composable
fun LeauTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LeauColors,
        typography = Typography,
        content = content
    )
}

@Composable
fun Ol1viaPATheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = LeauTheme(content)
