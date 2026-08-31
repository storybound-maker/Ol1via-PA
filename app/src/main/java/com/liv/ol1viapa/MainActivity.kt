package com.l1vo.ol1via.pa

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.l1vo.ol1via.pa.ui.theme.LeauTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LeauTheme {
                LeauMainScreen(
                    onEnableFloating = ::openOverlaySettings,
                    onStartDemoTimer = { startTimer(60) }
                )
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations && Settings.canDrawOverlays(this)) {
            startService(Intent(this, LeauOverlayService::class.java))
        }
    }

    private fun openOverlaySettings() {
        if (!Settings.canDrawOverlays(this)) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }
    }

    private fun startTimer(seconds: Int) {
        val intent = Intent(this, LeauOverlayService::class.java).apply {
            action = LeauOverlayService.ACTION_START_TIMER
            putExtra(LeauOverlayService.EXTRA_SECONDS, seconds)
        }
        startService(intent)
    }
}

@Composable
private fun LeauMainScreen(
    onEnableFloating: () -> Unit,
    onStartDemoTimer: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "leau-eyes")
    val floatY by transition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "float"
    )
    val glow by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 42.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("LEAU", fontSize = 16.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
            Spacer(Modifier.height(34.dp))

            Box(
                modifier = Modifier
                    .size(190.dp)
                    .scale(0.98f)
                    .alpha(glow)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                LeauEyes(modifier = Modifier.size(128.dp), yOffset = floatY)
            }

            Spacer(Modifier.height(30.dp))
            Text("I'm listening.", fontSize = 25.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))
            Text(
                "Speak naturally — LEAU is ready.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp
            )
            Spacer(Modifier.height(26.dp))

            Row(horizontalArrangement = Arrangement.Center) {
                Button(
                    onClick = onStartDemoTimer,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("Try timer UI") }
                Spacer(Modifier.width(10.dp))
                OutlinedButton(onClick = onEnableFloating) { Text("Enable bubble") }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Voice is the default. The floating LEAU bubble stays available outside the app.",
                modifier = Modifier.width(300.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
        }
    }
}

@Composable
private fun LeauEyes(modifier: Modifier = Modifier, yOffset: Float = 0f) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        Eye(yOffset)
        Eye(-yOffset)
    }
}

@Composable
private fun Eye(yOffset: Float) {
    Surface(
        modifier = Modifier
            .size(width = 48.dp, height = 74.dp)
            .padding(top = (yOffset / 2).dp),
        shape = RoundedCornerShape(50),
        color = Color(0xFFB9FF63),
        shadowElevation = 10.dp
    ) {}
}
