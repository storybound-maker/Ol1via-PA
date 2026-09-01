package com.liv.ol1viapa

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.Text

private val Muted = Color(0xFF8E96A8)

@Composable
private fun AppearanceSection(context: Context, redraw: () -> Unit) {
    var theme by remember { mutableStateOf(LeauSettings.theme(context)) }
    var font by remember { mutableFloatStateOf(LeauSettings.fontScale(context)) }
    var motion by remember { mutableStateOf(LeauSettings.reduceMotion(context)) }

    SettingsCard(
        icon = Icons.Default.Palette,
        title = "Theme",
        subtitle = "Dark / light / system"
    ) {
        theme = when (theme) {
            "dark" -> "light"
            "light" -> "system"
            else -> "dark"
        }
        LeauSettings.setTheme(context, theme)
        redraw()
    }

    Text(
        "Current: ${theme.replaceFirstChar { it.uppercase() }}",
        color = Muted,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "App font size",
        color = Color.White,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
    Slider(
        value = font,
        onValueChange = {
            font = it
            LeauSettings.setFontScale(context, it)
            redraw()
        },
        valueRange = .85f..1.35f,
        steps = 4
    )
    ToggleCard("Reduce motion", "Limit Leau animations", motion) {
        motion = it
        LeauSettings.setReduceMotion(context, it)
        redraw()
    }
}
