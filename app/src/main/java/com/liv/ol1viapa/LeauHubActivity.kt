package com.liv.ol1viapa

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.liv.ol1viapa.ui.theme.LeauPATheme

class LeauHubActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LeauPATheme { LeauHubScreen(this) } }
    }
}

@Composable
private fun LeauHubScreen(context: Context) {
    var page by remember { mutableStateOf("home") }
    var conversations by remember { mutableStateOf(LeauChatVault.getConversations(context)) }
    var memories by remember { mutableStateOf(LeauMemory.getMemories(context)) }
    Surface(Modifier.fillMaxSize(), color = Color(0xFF07110F)) {
        Column(Modifier.fillMaxSize().padding(20.dp)) {
            if (page == "home") {
                Spacer(Modifier.height(24.dp))
                Text("LEAU", style = MaterialTheme.typography.displaySmall, color = Color(0xFFB9FFB0))
                Text("ASSISTANT HUB", style = MaterialTheme.typography.labelLarge, color = Color(0xFF79CFC0))
                Spacer(Modifier.height(28.dp))
                HubCard("🗂  Chat Vault", "Saved conversations and history") { conversations = LeauChatVault.getConversations(context); page = "history" }
                HubCard("🧠  Memory", "Saved things Leau remembers") { memories = LeauMemory.getMemories(context); page = "memory" }
                HubCard("🍅  Pomodoro", "Focus sessions and live activity") { }
                HubCard("⚙  Settings", "Voice, bubble, notifications and permissions") { page = "settings" }
                Spacer(Modifier.weight(1f))
                Text("LEAU • always here", color = Color(0xFF55736C), modifier = Modifier.align(Alignment.CenterHorizontally))
            } else {
                HubHeader(if (page == "history") "CHAT VAULT" else if (page == "memory") "MEMORY" else "SETTINGS") { page = "home" }
                when (page) {
                    "history" -> {
                        if (conversations.isEmpty()) Text("No saved conversations yet.", color = Color(0xFF9BB5AD))
                        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(conversations) { c -> HubCard(c.title, "${c.messages.size} messages") { } } }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = { LeauChatVault.clear(context); conversations = emptyList() }, modifier = Modifier.fillMaxWidth()) { Text("Clear chat vault") }
                    }
                    "memory" -> {
                        if (memories.isEmpty()) Text("Leau has no saved memories yet.", color = Color(0xFF9BB5AD))
                        else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) { items(memories) { m -> HubCard(m, "Saved memory") { } } }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(onClick = { LeauMemory.clearMemories(context); memories = emptyList() }, modifier = Modifier.fillMaxWidth()) { Text("Forget all memories") }
                    }
                    else -> {
                        HubCard("Voice", "Speech rate and voice behavior") { }
                        HubCard("Floating bubble", "Always-available overlay") { }
                        HubCard("Notifications", "Timers, alarms and assistant alerts") { }
                        HubCard("Permissions", "Microphone and overlay access") { }
                        HubCard("Appearance", "Dark futuristic LEAU theme") { }
                    }
                }
            }
        }
    }
}

@Composable private fun HubHeader(title: String, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("‹", color = Color(0xFFB9FFB0), style = MaterialTheme.typography.headlineMedium, modifier = Modifier.clickable { onBack() })
        Spacer(Modifier.width(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, color = Color(0xFFE4FFF8))
    }
}

@Composable private fun HubCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp).clickable { onClick() }, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF10211D))) {
        Column(Modifier.padding(18.dp)) {
            Text(title, color = Color(0xFFE4FFF8), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, color = Color(0xFF83A49B), style = MaterialTheme.typography.bodySmall)
        }
    }
}
