package com.liv.ol1viapa

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { LeauPATheme(this) { LeauSettingsScreen(this) } }
    }
    fun requestRuntimePermissions() {
        val permissions = buildList { add(Manifest.permission.RECORD_AUDIO); if (Build.VERSION.SDK_INT >= 33) add(Manifest.permission.POST_NOTIFICATIONS) }
        permissionLauncher.launch(permissions.toTypedArray())
    }
}

@Composable
private fun LeauSettingsScreen(context: Context) {
    var section by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    fun redraw() { refresh++ }
    val prefs = remember { context.getSharedPreferences("leau_preferences", Context.MODE_PRIVATE) }
    val fontScale = prefs.getFloat("font_scale", 1f)

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        key(refresh, fontScale) {
            if (section == null) {
                LazyColumn(Modifier.fillMaxSize().padding(horizontal = 20.dp), contentPadding = PaddingValues(top = 10.dp, bottom = 36.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { (context as? ComponentActivity)?.finish() }, modifier = Modifier.offset(y = 5.dp)) { Icon(Icons.Outlined.ArrowBack, "Back", tint = Green) }
                            Column(Modifier.weight(1f)) { Text("Leau", color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold); Text("SETTINGS", color = Muted, style = MaterialTheme.typography.labelMedium) }
                        }
                    }
                    item { SettingsCard("Account", LeauSettings.accountEmail(context).ifBlank { "Sign in or create your account" }, Icons.Outlined.Person) { section = "account" } }
                    item { SettingsCard("Appearance", "Theme, font size and motion", Icons.Outlined.DarkMode) { section = "appearance" } }
                    item { SettingsCard("Notifications", "Alerts, sounds and vibration", Icons.Outlined.Notifications) { section = "notifications" } }
                    item { SettingsCard("Voice & interaction", "Speech, listening and haptics", Icons.Outlined.RecordVoiceOver) { section = "voice" } }
                    item { SettingsCard("Accessibility", "Readable text and comfortable controls", Icons.Outlined.Accessibility) { section = "accessibility" } }
                    item { SettingsCard("Floating pill", "Control Leau when you leave the app", Icons.Outlined.BubbleChart) { section = "pill" } }
                    item { SettingsCard("Chats & memory", "History and what Leau remembers", Icons.Outlined.Psychology) { section = "memory" } }
                    item { SettingsCard("Permissions", "Microphone, notifications and overlay", Icons.Outlined.Security) { section = "permissions" } }
                    item { SettingsCard("About Leau", "Version, privacy, terms and support", Icons.Outlined.Info) { section = "about" } }
                }
            } else SettingsSection(section!!, context, { section = null }, { redraw() })
        }
    }
}

@Composable private fun SettingsSection(title: String, context: Context, onBack: () -> Unit, redraw: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.offset(y = 5.dp)) { Icon(Icons.Outlined.ArrowBack, "Back", tint = Green) }
            Text(title.replaceFirstChar { it.uppercase() }, color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleLarge)
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

private fun openAuth(context: Context) = context.startActivity(Intent(context, LeauOnboardingActivity::class.java).putExtra(LeauOnboardingActivity.EXTRA_FORCE_AUTH, true))

@Composable private fun AccountSection(context: Context) {
    var signedOut by remember { mutableStateOf(false) }
    val email = LeauSettings.accountEmail(context)
    if (email.isBlank() || signedOut) {
        SettingsCard("Sign in with Google", "Use your Google account with Leau", Icons.Outlined.AccountCircle) { openAuth(context) }
        SettingsCard("Create account", "Create a Leau account with email and password", Icons.Outlined.PersonAdd) { openAuth(context) }
        SettingsCard("Sign in", "Use your existing Leau account", Icons.Outlined.Login) { openAuth(context) }
    } else {
        SettingsCard(LeauSettings.accountName(context).ifBlank { "Leau account" }, email, Icons.Outlined.Person) { }
        SettingsCard("Sign out", "Sign out of Firebase on this device", Icons.Outlined.Logout) { LeauAuth(context).signOut(); LeauSettings.clearAccount(context); signedOut = true }
    }
}

@Composable private fun AppearanceSection(context: Context, redraw: () -> Unit) {
    var theme by remember { mutableStateOf(LeauSettings.theme(context)) }
    var font by remember { mutableFloatStateOf(LeauSettings.fontScale(context)) }
    var motion by remember { mutableStateOf(LeauSettings.reduceMotion(context)) }
    SettingsCard("Theme", "Cycle Dark → Light → System", Icons.Outlined.Palette) { theme = when (theme) { "dark" -> "light"; "light" -> "system"; else -> "dark" }; LeauSettings.setTheme(context, theme); redraw() }
    Text("Current: ${theme.replaceFirstChar { it.uppercase() }}", color = Muted, modifier = Modifier.padding(horizontal = 12.dp))
    Spacer(Modifier.height(8.dp)); Text("App font size", color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.padding(horizontal = 12.dp))
    Slider(value = font, onValueChange = { font = it; LeauSettings.setFontScale(context, it); redraw() }, valueRange = .85f..1.35f, steps = 4)
    Text("${(font * 100).toInt()}%", color = Muted, modifier = Modifier.padding(horizontal = 12.dp))
    ToggleCard("Reduce motion", "Minimize non-essential animation", motion) { motion = it; LeauSettings.setReduceMotion(context, it); redraw() }
}

@Composable private fun NotificationsSection(context: Context, redraw: () -> Unit) {
    ToggleCard("Notifications", "Allow Leau alerts", LeauSettings.notifications(context)) { LeauSettings.setNotifications(context, it); if (it && context is LeauSettingsActivity) context.requestRuntimePermissions(); redraw() }
    ToggleCard("Sounds", "Play assistant notification sounds", LeauSettings.sounds(context)) { LeauSettings.setSounds(context, it); redraw() }
    ToggleCard("Haptic feedback", "Vibrate for supported controls", LeauSettings.haptics(context)) { LeauSettings.setHaptics(context, it); redraw() }
}

@Composable private fun VoiceSection(context: Context, redraw: () -> Unit) {
    ToggleCard("Voice responses", "Let Leau speak replies", LeauSettings.voiceResponses(context)) { LeauSettings.setVoiceResponses(context, it); redraw() }
    ToggleCard("Haptic feedback", "Touch feedback for controls", LeauSettings.haptics(context)) { LeauSettings.setHaptics(context, it); redraw() }
    SettingsCard("Speech permission", "Grant microphone access for tap-to-speak", Icons.Outlined.Mic) { if (context is LeauSettingsActivity) context.requestRuntimePermissions() }
}

@Composable private fun AccessibilitySection(context: Context, redraw: () -> Unit) {
    Text("Accessibility controls use the same preferences as the main Leau interface.", color = Muted, modifier = Modifier.padding(8.dp))
    ToggleCard("Reduce motion", "Minimize animation and movement", LeauSettings.reduceMotion(context)) { LeauSettings.setReduceMotion(context, it); redraw() }
    Text("Font size is controlled in Appearance.", color = Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
}

@Composable private fun PillSection(context: Context, redraw: () -> Unit) {
    ToggleCard("Floating Leau pill", "Enable the floating assistant outside the app", LeauSettings.floatingPill(context)) { value ->
        LeauSettings.setFloatingPill(context, value)
        if (value) { if (android.provider.Settings.canDrawOverlays(context)) context.startService(Intent(context, LeauOverlayService::class.java).setAction(LeauOverlayService.ACTION_SHOW)) else LeauSettings.openOverlaySettings(context) }
        else context.startService(Intent(context, LeauOverlayService::class.java).setAction(LeauOverlayService.ACTION_HIDE))
        redraw()
    }
    ToggleCard("Start automatically", "Keep the pill available after leaving Leau", LeauSettings.autoPill(context)) { LeauSettings.setAutoPill(context, it); redraw() }
    SettingsCard("Overlay permission", "Open Android's floating-window permission", Icons.Outlined.Launch) { LeauSettings.openOverlaySettings(context) }
}

@Composable private fun MemorySection(context: Context, redraw: () -> Unit) {
    ToggleCard("Memory", "Allow Leau to save useful memories", LeauSettings.memory(context)) { LeauSettings.setMemory(context, it); redraw() }
    ToggleCard("Chat history", "Save conversations in the local chat vault", LeauSettings.chatHistory(context)) { LeauSettings.setChatHistory(context, it); redraw() }
    SettingsCard("Clear chat vault", "Delete all saved conversations", Icons.Outlined.Delete) { LeauChatVault.clear(context); redraw() }
    SettingsCard("Forget all memories", "Delete everything Leau has remembered", Icons.Outlined.DeleteForever) { LeauMemory.clearMemories(context); redraw() }
}

@Composable private fun PermissionSection(context: Context) {
    val micGranted = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    val overlayGranted = android.provider.Settings.canDrawOverlays(context)
    SettingsCard("Microphone", if (micGranted) "Granted" else "Required for voice interaction", Icons.Outlined.Mic) { if (context is LeauSettingsActivity) context.requestRuntimePermissions() }
    SettingsCard("Notifications", "Manage Android notification permission", Icons.Outlined.Notifications) { LeauSettings.openNotificationSettings(context) }
    SettingsCard("Floating overlay", if (overlayGranted) "Granted" else "Required for the floating pill", Icons.Outlined.BubbleChart) { LeauSettings.openOverlaySettings(context) }
    SettingsCard("App permissions", "Open Android's complete Leau permission page", Icons.Outlined.Security) { LeauSettings.openAppSettings(context) }
}

@Composable private fun AboutSection(context: Context) {
    SettingsCard("Leau", "Your personal AI companion", Icons.Outlined.Eco) { }
    Text("Version 1.0", color = Muted, modifier = Modifier.padding(12.dp))
    SettingsCard("Privacy", "Accounts use Firebase Authentication. Saved chat history and memories remain on this device unless a feature explicitly sends information to an online service. You can clear saved data from Settings.", Icons.Outlined.PrivacyTip) { }
    SettingsCard("Terms", "Leau is an assistant tool. Verify important information before acting on it. Leau does not replace professional advice, and online services may have their own terms.", Icons.Outlined.Description) { }
    SettingsCard("Health", "Health-related features and guidance are not enabled here.", Icons.Outlined.HealthAndSafety) { }
    SettingsCard("Help & support", "Support entry point reserved for the next project phase.", Icons.Outlined.HelpOutline) { }
}

@Composable fun ToggleCard(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 5.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium); Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall) }
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}

@Composable fun SettingsCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable { onClick() }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Green, modifier = Modifier.size(24.dp)); Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) { Text(title, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.titleMedium); Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall) }
            Icon(Icons.Outlined.ChevronRight, null, tint = Muted)
        }
    }
}
