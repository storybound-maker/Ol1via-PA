package com.liv.ol1viapa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.liv.ol1viapa.ui.theme.LeauPATheme
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.random.Random

data class ChatMessage(val text: String, val fromLeau: Boolean)

class MainActivity : ComponentActivity() {
    private var recognizedText by mutableStateOf("")
    private var isListening by mutableStateOf(false)
    private var isSpeaking by mutableStateOf(false)
    private var textToSpeech: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var sendRecognizedMessage: ((String) -> Unit)? = null
    private val microphonePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) startSpeechRecognition() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale.US
                textToSpeech?.setSpeechRate(0.95f)
                textToSpeech?.setPitch(1.05f)
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { runOnUiThread { isSpeaking = true } }
                    override fun onDone(utteranceId: String?) { runOnUiThread { isSpeaking = false } }
                    override fun onError(utteranceId: String?) { runOnUiThread { isSpeaking = false } }
                })
            }
        }
        setupSpeechRecognizer()
        enableEdgeToEdge()
        setContent { LeauPATheme { PremiumLeauHomeScreen(recognizedText, isListening, isSpeaking, ::startListening, ::speak) { sender -> sendRecognizedMessage = sender } } }
    }

    private fun normalizeVoiceInput(input: String): String {
        val cleaned = input.trim().replace(Regex("\\s+"), " ")
        return when (cleaned.lowercase(Locale.US)) {
            "hey you", "hey u", "hi you", "hi u", "hello you", "hello u", "hey leo", "hi leo", "hello leo", "hey leu", "hi leu", "hello leu", "hey layou", "hi layou", "hello layou", "hey liu", "hi liu", "hello liu" -> cleaned.replaceFirst(Regex("(?i)^(hey|hi|hello)\\b.*$"), "$1 Leau")
            else -> cleaned
        }
    }

    private fun setupSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) { runOnUiThread { isListening = true } }
                override fun onBeginningOfSpeech() { runOnUiThread { isListening = true } }
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() { runOnUiThread { isListening = false } }
                override fun onPartialResults(partialResults: Bundle?) { val p = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty(); if (p.isNotBlank()) runOnUiThread { recognizedText = p } }
                override fun onResults(results: Bundle?) { val raw = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty(); val recognized = normalizeVoiceInput(raw); runOnUiThread { isListening = false; if (recognized.isNotEmpty()) { recognizedText = recognized; sendRecognizedMessage?.invoke(recognized) } } }
                override fun onError(error: Int) { runOnUiThread { isListening = false; if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) Toast.makeText(this@MainActivity, "I couldn't understand that. Try again.", Toast.LENGTH_SHORT).show() } }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
    }

    private fun startListening() { if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startSpeechRecognition() else microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }

    private fun startSpeechRecognition() {
        if (speechRecognizer == null) setupSpeechRecognizer()
        val recognizer = speechRecognizer ?: return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply { putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM); putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag()); putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true); putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1); putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to Leau") }
        recognizedText = ""
        isListening = true
        recognizer.startListening(intent)
    }

    private fun speak(text: String) {
        val spokenText = text.replace(Regex("\\bLeau\\b", RegexOption.IGNORE_CASE), "Liu")
        textToSpeech?.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, "leau_reply")
    }

    override fun onDestroy() {
        sendRecognizedMessage = null
        speechRecognizer?.cancel(); speechRecognizer?.destroy(); speechRecognizer = null
        textToSpeech?.stop(); textToSpeech?.shutdown(); textToSpeech = null
        super.onDestroy()
    }
}

@Composable
fun LeauHomeScreen(initialMessage: String, isListening: Boolean, isSpeaking: Boolean, onMicClick: () -> Unit, onLeauReply: (String) -> Unit, registerVoiceMessageSender: ((String) -> Unit) -> Unit) {
    // Kept as a compatibility wrapper for any existing callers. The premium screen is now the default.
    PremiumLeauHomeScreen(initialMessage, isListening, isSpeaking, onMicClick, onLeauReply, registerVoiceMessageSender)
}

@Composable private fun BlinkingLeauEyes() { EyeAnimation(220f, 220, "blink") }
@Composable private fun ThinkingLeauEyes() { EyeAnimation(220f, 180, "think") }
@Composable private fun ListeningLeauEyes() { EyeAnimation(220f, 220, "listen") }
@Composable private fun SpeakingLeauEyes() { EyeAnimation(220f, 220, "speak") }

@Composable
private fun EyeAnimation(height: Float, width: Int, mode: String) {
    val animatedHeight = remember { Animatable(height) }
    LaunchedEffect(Unit) { while (true) { when (mode) { "blink" -> { delay(Random.nextLong(2500L, 5000L)); animatedHeight.animateTo(8f, tween(90)); animatedHeight.animateTo(height, tween(120)) }; "think" -> { animatedHeight.animateTo(190f, tween(350)); animatedHeight.animateTo(height, tween(450)); delay(180L) }; "listen" -> { animatedHeight.animateTo(205f, tween(500)); animatedHeight.animateTo(height, tween(500)); delay(120L) }; else -> { animatedHeight.animateTo(200f, tween(180)); animatedHeight.animateTo(height, tween(180)); delay(80L) } } } }
    Box(Modifier.size(width.dp, height.dp), contentAlignment = Alignment.Center) { Image(painterResource(R.drawable.leau_eyes), "Leau", Modifier.size(width.dp, animatedHeight.value.dp), contentScale = ContentScale.FillBounds) }
}
