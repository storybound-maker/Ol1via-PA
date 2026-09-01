package com.liv.ol1viapa.ui.theme

import android.content.Context
import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

private val LeauDarkScheme = darkColorScheme(
    primary = LeauLime, onPrimary = Color.Black, primaryContainer = LeauSurfaceVariant, onPrimaryContainer = LeauText,
    secondary = LeauMint, onSecondary = Color.Black, secondaryContainer = LeauSurfaceVariant, onSecondaryContainer = LeauText,
    tertiary = LeauGlow, onTertiary = Color.Black, background = LeauBackground, onBackground = LeauText,
    surface = LeauBackground, onSurface = LeauText, surfaceVariant = LeauSurfaceVariant, onSurfaceVariant = LeauText, outline = LeauMuted
)
private val LeauLightScheme = lightColorScheme(
    primary = Color(0xFF356B1A), onPrimary = Color.White, primaryContainer = Color(0xFFD8F8B8), onPrimaryContainer = Color(0xFF102A08),
    secondary = Color(0xFF176B55), onSecondary = Color.White, secondaryContainer = Color(0xFFBCEFE0), onSecondaryContainer = Color(0xFF002117),
    tertiary = Color(0xFF26715B), onTertiary = Color.White, tertiaryContainer = Color(0xFFD2F4E8), onTertiaryContainer = Color(0xFF08251C),
    background = Color(0xFFF5FBF7), onBackground = Color(0xFF101511), surface = Color(0xFFF5FBF7), onSurface = Color(0xFF101511),
    surfaceVariant = Color(0xFFDCE9E1), onSurfaceVariant = Color(0xFF414943), outline = Color(0xFF717A73)
)

@Composable
fun LeauPATheme(darkTheme: Boolean = true, dynamicColor: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) LeauDarkScheme else LeauLightScheme, typography = LeauTypography, content = content)
}

private fun TextStyle.scaled(factor: Float): TextStyle = copy(
    fontSize = if (fontSize.value > 0f) fontSize * factor else fontSize,
    lineHeight = if (lineHeight.value > 0f) lineHeight * factor else lineHeight
)

private fun Typography.scaled(factor: Float): Typography = Typography(
    displayLarge = displayLarge.scaled(factor), displayMedium = displayMedium.scaled(factor), displaySmall = displaySmall.scaled(factor),
    headlineLarge = headlineLarge.scaled(factor), headlineMedium = headlineMedium.scaled(factor), headlineSmall = headlineSmall.scaled(factor),
    titleLarge = titleLarge.scaled(factor), titleMedium = titleMedium.scaled(factor), titleSmall = titleSmall.scaled(factor),
    bodyLarge = bodyLarge.scaled(factor), bodyMedium = bodyMedium.scaled(factor), bodySmall = bodySmall.scaled(factor),
    labelLarge = labelLarge.scaled(factor), labelMedium = labelMedium.scaled(factor), labelSmall = labelSmall.scaled(factor)
)

@Composable
fun LeauPATheme(context: Context, content: @Composable () -> Unit) {
    val prefs = remember { context.getSharedPreferences("leau_preferences", Context.MODE_PRIVATE) }
    val mode = prefs.getString("theme", "dark") ?: "dark"
    val dark = when (mode) {
        "light" -> false
        "system" -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        else -> true
    }
    val fontScale = prefs.getFloat("font_scale", 1f).coerceIn(.85f, 1.35f)
    val baseTypography = LeauTypography
    MaterialTheme(
        colorScheme = if (dark) LeauDarkScheme else LeauLightScheme,
        typography = baseTypography.scaled(fontScale),
        content = content
    )
}
