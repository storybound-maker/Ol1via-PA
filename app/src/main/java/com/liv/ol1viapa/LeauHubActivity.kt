package com.liv.ol1viapa

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.liv.ol1viapa.ui.theme.LeauPATheme

class LeauHubActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LeauPATheme { LeauHubScreen(onBack = { finish() }) } }
    }
}

@androidx.compose.runtime.Composable
private fun LeauHubScreen(onBack: () -> Unit) {
    var minutes by remember { mutableIntStateOf(25) }
    var running by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val memories = LeauMemory.getMemories(context)
    val prefs = context.getSharedPreferences("leau_history", 0)
    val history = prefs.getStringSet("sessions", emptySet())?.toList()?.reversed() ?: emptyList()

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
            Column(Modifier.weight(1f)) { Text("LEAU", style = MaterialTheme.typography.headlineLarge); Text("Assistant Hub", style = MaterialTheme.typography.bodyMedium) }
        }
        HubCard("Pomodoro", "Focus timer that keeps running after LEAU closes.", Icons.Default.Timer) {
            Text("Focus length: $minutes min")
            Slider(value = minutes.toFloat(), onValueChange = { minutes = it.toInt().coerceIn(1, 120) }, valueRange = 1f..120f)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { LeauPomodoro.start(context, minutes); running = true }) { Text(if (running) "Restart focus" else "Start focus") }
                OutlinedButton(onClick = { LeauPomodoro.cancel(context); running = false }) { Text("Stop") }
            }
        }
        HubCard("Chat History Vault", "Your saved LEAU conversations appear here.", Icons.Default.History) {
            if (history.isEmpty()) Text("No saved conversations yet.") else history.take(12).forEach { Text("• $it", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            OutlinedButton(onClick = { prefs.edit().remove("sessions").apply() }) { Icon(Icons.Default.Delete, null); Spacer(Modifier.padding(2.dp)); Text("Clear vault") }
        }
        HubCard("Memory", "Manage what LEAU remembers about you.", Icons.Default.Memory) {
            if (memories.isEmpty()) Text("No saved memories yet.") else memories.forEach { Text("• $it") }
            OutlinedButton(onClick = { LeauMemory.clearMemories(context) }) { Text("Forget all memories") }
        }
        HubCard("Settings", "Voice, notifications, overlay permissions and assistant controls.", Icons.Default.Settings) {
            Button(onClick = { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))) }) { Text("Manage floating permission") }
            OutlinedButton(onClick = { context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)) }) { Text("Notification settings") }
        }
        Spacer(Modifier.height(20.dp))
        Text("LEAU is built around one simple idea: the eyes are the identity, and the pill is the presence.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@androidx.compose.runtime.Composable
private fun HubCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @androidx.compose.runtime.Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(icon, null); Spacer(Modifier.padding(5.dp)); Column { Text(title, style = MaterialTheme.typography.titleLarge); Text(subtitle, style = MaterialTheme.typography.bodySmall) } }
            content()
        }
    }
}
