package com.liv.ol1viapa

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.liv.ol1viapa.ui.theme.LeauPATheme

private val AuthGreen = Color(0xFFB8FF5A)
private val AuthBackground = Color(0xFF07100D)
private val AuthCard = Color(0xFF0D1B17)

class LeauAuthActivity : ComponentActivity() {
    private lateinit var auth: LeauAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = LeauAuth(this)
        if (auth.currentUser() != null || getPreferences(MODE_PRIVATE).getBoolean("guest_mode", false)) {
            openApp()
            return
        }
        setContent { LeauPATheme { AuthScreen() } }
    }

    private fun openApp() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }

    private fun resultMessage(result: Result<*>) {
        result.onSuccess { openApp() }.onFailure { Toast.makeText(this, it.message ?: "Something went wrong.", Toast.LENGTH_LONG).show() }
    }

    @Composable
    private fun AuthScreen() {
        var mode by remember { mutableStateOf(AuthMode.WELCOME) }
        var email by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        var name by remember { mutableStateOf("") }
        var busy by remember { mutableStateOf(false) }
        var resetMode by remember { mutableStateOf(false) }

        fun submit() {
            if (email.isBlank() || (!resetMode && password.length < 6)) return
            busy = true
            if (resetMode) auth.sendPasswordReset(email) { busy = false; resultMessage(it.mapSuccess { Unit }) }
            else if (mode == AuthMode.SIGN_UP) auth.signUp(email, password) { busy = false; resultMessage(it) }
            else auth.signIn(email, password) { busy = false; resultMessage(it) }
        }

        Box(Modifier.fillMaxSize().background(AuthBackground).padding(24.dp)) {
            if (mode != AuthMode.WELCOME) {
                IconButton(onClick = { mode = AuthMode.WELCOME; resetMode = false }, modifier = Modifier.align(Alignment.TopStart)) {
                    Icon(Icons.Outlined.ArrowBack, "Back", tint = Color.White)
                }
            }
            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("Leau", style = MaterialTheme.typography.displaySmall, color = Color.White)
                Spacer(Modifier.height(8.dp))
                Text(if (mode == AuthMode.WELCOME) "A calmer way to get things done." else if (resetMode) "Reset your password" else if (mode == AuthMode.SIGN_UP) "Create your Leau account" else "Welcome back", color = Color(0xFF8BA69C))
                Spacer(Modifier.height(32.dp))

                when (mode) {
                    AuthMode.WELCOME -> WelcomeActions(onSignIn = { mode = AuthMode.SIGN_IN }, onCreate = { mode = AuthMode.SIGN_UP }, onGuest = { getPreferences(MODE_PRIVATE).edit().putBoolean("guest_mode", true).apply(); openApp() }, onGoogle = {
                        val intent = auth.googleSignInIntent(this@LeauAuthActivity)
                        if (intent == null) Toast.makeText(this@LeauAuthActivity, "Add the Firebase project configuration first.", Toast.LENGTH_LONG).show()
                        else startActivityForResult(intent, GOOGLE_REQUEST)
                    })
                    else -> AccountForm(mode, resetMode, email, password, name, busy, { email = it }, { password = it }, { name = it }, { resetMode = true }, ::submit)
                }
            }
        }
    }

    @Deprecated("Activity result API retained for Google sign-in compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GOOGLE_REQUEST) auth.finishGoogleSignIn(data, ::resultMessage)
    }

    companion object { private const val GOOGLE_REQUEST = 7001 }
}

enum class AuthMode { WELCOME, SIGN_IN, SIGN_UP }

private fun <T, R> Result<T>.mapSuccess(transform: (T) -> R): Result<R> = fold({ Result.success(transform(it)) }, { Result.failure(it) })

@Composable
private fun WelcomeActions(onSignIn: () -> Unit, onCreate: () -> Unit, onGuest: () -> Unit, onGoogle: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(onClick = onGoogle, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { Text("Continue with Google") }
        OutlinedButton(onClick = onCreate, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp)) { Text("Create an account") }
        TextButton(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) { Text("Sign in") }
        TextButton(onClick = onGuest, modifier = Modifier.fillMaxWidth()) { Text("Continue without an account", color = AuthGreen) }
    }
}

@Composable
private fun AccountForm(mode: AuthMode, reset: Boolean, email: String, password: String, name: String, busy: Boolean, setEmail: (String) -> Unit, setPassword: (String) -> Unit, setName: (String) -> Unit, resetPassword: () -> Unit, submit: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = AuthCard), shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (mode == AuthMode.SIGN_UP && !reset) OutlinedTextField(name, setName, Modifier.fillMaxWidth(), label = { Text("Name") }, leadingIcon = { Icon(Icons.Outlined.Person, null) }, singleLine = true)
            OutlinedTextField(email, setEmail, Modifier.fillMaxWidth(), label = { Text("Email") }, leadingIcon = { Icon(Icons.Outlined.Email, null) }, singleLine = true)
            if (!reset) OutlinedTextField(password, setPassword, Modifier.fillMaxWidth(), label = { Text("Password") }, leadingIcon = { Icon(Icons.Outlined.Lock, null) }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
            Button(onClick = submit, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(16.dp)) { Text(if (busy) "Please wait…" else if (reset) "Send reset email" else if (mode == AuthMode.SIGN_UP) "Create account" else "Sign in") }
            if (mode == AuthMode.SIGN_IN && !reset) TextButton(onClick = resetPassword, modifier = Modifier.fillMaxWidth()) { Text("Forgot password?") }
        }
    }
}
