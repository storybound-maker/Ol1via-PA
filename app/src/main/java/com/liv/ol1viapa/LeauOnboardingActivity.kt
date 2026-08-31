package com.liv.ol1viapa

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.liv.ol1viapa.ui.theme.LeauPATheme

private val Green = Color(0xFFB8FF5A)
private val Bg = Color(0xFF07100D)
private val Muted = Color(0xFF8BA69C)

class LeauOnboardingActivity : ComponentActivity() {
    private lateinit var auth: LeauAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = LeauAuth(this)
        if (LeauSettings.onboardingComplete(this) || auth.currentUser() != null) {
            openMain()
            return
        }
        setContent { LeauPATheme { OnboardingScreen() } }
    }

    private fun openMain() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    private fun complete() {
        LeauSettings.setOnboardingComplete(this, true)
        openMain()
    }

    private fun showResult(result: Result<*>, onError: (String) -> Unit) {
        result.onSuccess {
            val user = auth.currentUser()
            LeauSettings.saveAccount(this, user?.displayName ?: "Leau user", user?.email ?: "", user?.providerData?.lastOrNull()?.providerId ?: "firebase")
            complete()
        }.onFailure { onError(it.message ?: "Authentication failed. Try again.") }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_REQUEST) {
            // The Firebase account is finalized only after the Google credential is exchanged.
            auth.finishGoogleSignIn(data) { result ->
                runOnUiThread { showResult(result) {} }
            }
        }
    }

    @Composable
    private fun OnboardingScreen() {
        var mode by remember { mutableStateOf("welcome") }
        var name by remember { mutableStateOf("") }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var error by remember { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }

        fun authenticate() {
            error = ""
            if (email.isBlank() || password.length < 6 || (mode == "create" && name.isBlank())) {
                error = if (mode == "signin") "Enter a valid email and a password of at least 6 characters." else "Add your name, email and a password of at least 6 characters."
                return
            }
            busy = true
            val callback: (Result<*>) -> Unit = { result ->
                runOnUiThread {
                    busy = false
                    showResult(result) { error = it }
                }
            }
            if (mode == "create") auth.signUp(email, password, callback)
            else auth.signIn(email, password, callback)
        }

        Box(Modifier.fillMaxSize().background(Bg).padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Leau", color = Green, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(if (mode == "welcome") "A quieter way to talk to your assistant." else if (mode == "create") "Create your Leau account" else "Welcome back", color = Muted, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(30.dp))
                when (mode) {
                    "welcome" -> {
                        AuthButton("Continue with Google", Icons.Outlined.AccountCircle) {
                            val intent = auth.googleSignInIntent(this@LeauOnboardingActivity)
                            if (intent == null) error = "Firebase is not configured yet. Add the Firebase project values and Google web client ID."
                            else startActivityForResult(intent, GOOGLE_REQUEST)
                        }
                        AuthButton("Create account", Icons.Outlined.PersonAdd) { error = ""; mode = "create" }
                        AuthButton("Sign in", Icons.Outlined.Login) { error = ""; mode = "signin" }
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { complete() }) { Text("Continue without an account", color = Muted) }
                    }
                    else -> {
                        if (mode == "create") Field("Name", name) { name = it }
                        Field("Email", email) { email = it }
                        Field("Password", password, true) { password = it }
                        if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(8.dp))
                        Button(onClick = { authenticate() }, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(18.dp)) {
                            Text(if (busy) "Please wait…" else if (mode == "create") "Create account" else "Sign in")
                        }
                        if (mode == "signin") TextButton(onClick = {
                            if (email.isBlank()) error = "Enter your email first."
                            else { busy = true; auth.sendPasswordReset(email) { result -> runOnUiThread { busy = false; result.onSuccess { error = "Password reset email sent." }.onFailure { error = it.message ?: "Could not send reset email." } } } }
                        }) { Text("Forgot password?", color = Green) }
                        TextButton(onClick = { mode = "welcome"; error = "" }) { Text("Back", color = Muted) }
                    }
                }
                if (error.isNotBlank() && mode == "welcome") Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 12.dp))
            }
        }
    }

    companion object { private const val GOOGLE_REQUEST = 7001 }
}

@Composable
private fun Field(label: String, value: String, password: Boolean = false, onChange: (String) -> Unit) {
    OutlinedTextField(value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), shape = RoundedCornerShape(18.dp), visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None)
}

@Composable
private fun AuthButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().height(52.dp).padding(vertical = 3.dp), shape = RoundedCornerShape(18.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) {
        Icon(icon, null, tint = Green); Spacer(Modifier.width(10.dp)); Text(text)
    }
}
