package com.liv.ol1viapa

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.liv.ol1viapa.ui.theme.LeauPATheme

private val Green = Color(0xFFB8FF5A)
private val Bg = Color(0xFF07100D)
private val Card = Color(0xFF0D1B17)
private val Muted = Color(0xFF8BA69C)

class LeauOnboardingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (LeauSettings.onboardingComplete(this)) {
            openMain()
            return
        }
        setContent { LeauPATheme { OnboardingScreen(this) } }
    }

    fun finishOnboarding() {
        LeauSettings.setOnboardingComplete(this, true)
        openMain()
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
}

@Composable
private fun OnboardingScreen(activity: LeauOnboardingActivity) {
    var mode by remember { mutableStateOf("welcome") }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().background(Bg).padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text("Leau", color = Green, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
            Text("A quieter way to talk to your assistant.", color = Muted, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(30.dp))
            when (mode) {
                "welcome" -> {
                    AuthButton("Continue with Google", Icons.Outlined.AccountCircle) { error = "Google sign-in needs the app's configured authentication provider." }
                    AuthButton("Create account", Icons.Outlined.PersonAdd) { mode = "create" }
                    AuthButton("Sign in", Icons.Outlined.Login) { mode = "signin" }
                    Spacer(Modifier.height(12.dp))
                    TextButton(onClick = { activity.finishOnboarding() }) { Text("Continue without an account", color = Muted) }
                }
                "create", "signin" -> {
                    Text(if (mode == "create") "Create your Leau account" else "Welcome back", color = Color.White, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(18.dp))
                    if (mode == "create") Field("Name", name) { name = it }
                    Field("Email", email) { email = it }
                    Field("Password", password, true) { password = it }
                    if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
                    Button(onClick = {
                        if (email.contains("@") && password.length >= 6 && (mode == "signin" || name.isNotBlank())) {
                            LeauSettings.saveAccount(activity, if (name.isBlank()) "Leau user" else name, email, "email")
                            activity.finishOnboarding()
                        } else error = if (mode == "signin") "Enter a valid email and a password of at least 6 characters." else "Add your name, a valid email and a password of at least 6 characters."
                    }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(18.dp)) { Text(if (mode == "create") "Create account" else "Sign in") }
                    TextButton(onClick = { mode = "welcome" }) { Text("Back", color = Muted) }
                }
            }
        }
    }
}

@Composable private fun Field(label: String, value: String, password: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(18.dp), visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None)
}

@Composable private fun AuthButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp).padding(vertical = 3.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
        Icon(icon, null, tint = Green); Spacer(Modifier.width(10.dp)); Text(text)
    }
}
