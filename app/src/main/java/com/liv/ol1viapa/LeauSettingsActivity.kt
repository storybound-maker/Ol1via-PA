package com.liv.ol1viapa

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.liv.ol1viapa.ui.theme.LeauPATheme

private val Green = Color(0xFFB8FF5A)
private val Bg = Color(0xFF07100D)
private val Card = Color(0xFF0D1B17)
private val Muted = Color(0xFF8BA69C)

class LeauSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LeauPATheme { LeauSettingsScreen(this) } }
    }
}

@Composable
private fun LeauSettingsScreen(context: Context) {
    var section by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    fun redraw() { refresh++ }

    Surface(Modifier.fillMaxSize(), color = Bg) {
        if (section == null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                contentPadding = PaddingValues(top = 28.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text("Leau", color = Green, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                    Text("SETTINGS HUB", color = Muted, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(18.dp))
                }
                item { SettingsCard("Account", "${LeauSettings.accountEmail(context).ifBlank { "Sign in or create your account" }}", Icons.Outlined.Person) { section = "account" } }
                item { SettingsCard("Appearance", "Theme, font and motion", Icons.Outlined.DarkMode) { section = "appearance" } }
                item { SettingsCard("Notifications", "Alerts, sounds and vibration", Icons.Outlined.Notifications) { section = "notifications" } }
                item { SettingsCard("Voice & interaction", "Speech, listening and haptics", Icons.Outlined.RecordVoiceOver) { section = "voice" } }
                item { SettingsCard("Accessibility", "Text size, contrast and motion", Icons.Outlined.Accessibility) { section = "accessibility" } }
                item { SettingsCard("Floating pill", "Control Leau when you leave the app", Icons.Outlined.BubbleChart) { section = "pill" } }
                item { SettingsCard("Chats & memory", "History and what Leau remembers", Icons.Outlined.Psychology) { section = "memory" } }
                item { SettingsCard("Permissions", "Microphone, overlay and app permissions", Icons.Outlined.Security) { section = "permissions" } }
                item { SettingsCard("About Leau", "Version, help, privacy and terms", Icons.Outlined.Info) { section = "about" } }
            }
        } else {
            SettingsSection(section!!, context, { section = null }, { redraw() })
        }
    }
}

@Composable
private fun SettingsSection(title: String, context: Context, onBack: () -> Unit, redraw: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = Green) }
            Text(title.replaceFirstChar { it.uppercase() }, color = Color.White, style = MaterialTheme.typography.titleLarge)
        }
        when (title) {
            "account" -> AccountSection(context)
            "appearance" -> AppearanceSection(context, redraw)
            "notifications" -> NotificationsSection(context, redraw)
            "voice" -> VoiceSection(context, redraw)
            "accessibility" -> AccessibilitySection(context, redraw)
            "pill" -> PillSection(context, redraw)
            "memory" -> MemorySection(context, redraw)
            "permissions" -> PermissionSection(context)
            "about" -> AboutSection(context)
        }
    }
}

@Composable private fun AccountSection(context: Context) {
    val email = LeauSettings.accountEmail(context)
    if (email.isBlank()) {
        SettingsCard("Sign in with Google", "Google authentication will be connected to the configured account provider", Icons.Outlined.AccountCircle) { }
        SettingsCard("Create account", "Create a Leau account with email and password", Icons.Outlined.PersonAdd) { }
        SettingsCard("Sign in", "Use your existing Leau account", Icons.Outlined.Login) { }
        Text("Account authentication is intentionally kept separate from local settings so credentials are never stored in this screen.", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
    } else {
        SettingsCard(LeauSettings.accountName(context).ifBlank { "Leau account" }, email, Icons.Outlined.Person) { }
        SettingsCard("Sign out", "Remove this account from this device", Icons.Outlined.Logout) { LeauSettings.clearAccount(context) }
    }
}

@Composable private fun AppearanceSection(context: Context, redraw: () -> Unit) {
    var theme by remember { mutableStateOf(LeauSettings.theme(context)) }
    var font by remember { mutableFloatStateOf(LeauSettings.fontScale(context)) }
    var motion by remember { mutableStateOf(LeauSettings.reduceMotion(context)) }
    SettingsCard("Theme", "Dark / light / system") { theme = if (theme == "dark") "light" else if (theme == "light") "system" else "dark"; LeauSettings.setTheme(context, theme); redraw() }
    Text("Current: ${theme.replaceFirstChar { it.uppercase() }}", color = Muted, modifier = Modifier.padding(horizontal = 12.dp))
    Spacer(Modifier.height(8.dp))
    Text("App font size", color = Color.White, modifier = Modifier.padding(horizontal = 12.dp))
    Slider(value = font, onValueChange = { font = it; LeauSettings.setFontScale(context, it); redraw() }, valueRange = .85f..1.35f, steps = 4)
    ToggleCard("Reduce motion", "Limit Leau animations", motion) { motion = it; LeauSettings.setReduceMotion(context, it); redraw() }
}

@Composable private fun NotificationsSection(context: Context, redraw: () -> Unit) {
    ToggleCard("Notifications", "Allow Leau alerts", LeauSettings.notifications(context)) { LeauSettings.setNotifications(context, it); redraw() }
    ToggleCard("Sounds", "Play notification sounds", LeauSettings.haptics(context)) { LeauSettings.setHaptics(context, it); redraw() }
}

@Composable private fun VoiceSection(context: Context, redraw: () -> Unit) {
    ToggleCard("Voice responses", "Let Leau speak replies", LeauSettings.voiceResponses(context)) { LeauSettings.setVoiceResponses(context, it); redraw() }
    ToggleCard("Haptic feedback", "Touch feedback for controls", LeauSettings.haptics(context)) { LeauSettings.setHaptics(context, it); redraw() }
}

@Composable private fun AccessibilitySection(context: Context, redraw: () -> Unit) {
    Text("Accessibility controls apply across Leau's main interface.", color = Muted, modifier = Modifier.padding(8.dp))
    Text("Font size is controlled in Appearance. Motion is controlled here and in Appearance so the setting is easy to find.", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
    ToggleCard("Reduce motion", "Minimize animation and movement", LeauSettings.reduceMotion(context)) { LeauSettings.setReduceMotion(context, it); redraw() }
    ToggleCard("Larger touch targets", "Accessibility-friendly control sizing", false) { }
    ToggleCard("High contrast", "Increase visual contrast", false) { }
}

@Composable private fun PillSection(context: Context, redraw: () -> Unit) {
    ToggleCard("Floating Leau pill", "Enable the floating assistant when you leave the app", LeauSettings.floatingPill(context)) { LeauSettings.setFloatingPill(context, it); redraw() }
    ToggleCard("Start automatically", "Show the pill automatically after leaving Leau", LeauSettings.autoPill(context)) { LeauSettings.setAutoPill(context, it); redraw() }
    SettingsCard("Android overlay permission", "Open system permission controls", Icons.Outlined.Launch) { LeauSettings.openAppSettings(context) }
}

@Composable private fun MemorySection(context: Context, redraw: () -> Unit) {
    ToggleCard("Memory", "Allow Leau to save useful memories", LeauSettings.memory(context)) { LeauSettings.setMemory(context, it); redraw() }
    ToggleCard("Chat history", "Save conversations in the local vault", LeauSettings.chatHistory(context)) { LeauSettings.setChatHistory(context, it); redraw() }
    SettingsCard("Clear chat vault", "Delete saved conversations", Icons.Outlined.Delete) { LeauChatVault.clear(context); redraw() }
    SettingsCard("Forget all memories", "Delete everything Leau has remembered", Icons.Outlined.DeleteForever) { LeauMemory.clearMemories(context); redraw() }
}

@Composable private fun PermissionSection(context: Context) {
    SettingsCard("App permissions", "Open Android's Leau permission page", Icons.Outlined.Security) { LeauSettings.openAppSettings(context) }
}

@Composable private fun AboutSection(context: Context) {
    SettingsCard("Leau", "Your personal AI companion", Icons.Outlined.Eco) { }
    Text("Version 1.0", color = Muted, modifier = Modifier.padding(12.dp))
    SettingsCard("Privacy", "How Leau handles your data", Icons.Outlined.PrivacyTip) { }
    SettingsCard("Terms", "Terms of use", Icons.Outlined.Description) { }
    SettingsCard("Help & support", "Get help with Leau", Icons.Outlined.HelpOutline) { }
}

@Composable private fun ToggleCard(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Card)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium); Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall) }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable private fun SettingsCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Card)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Green, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) { Text(title, color = Color.White, style = MaterialTheme.typography.titleMedium); Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall) }
            Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
        }
    }
}
