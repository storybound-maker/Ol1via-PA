package com.liv.ol1viapa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
import androidx.compose.ui.draw.scale
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

// Existing activity and speech/AI implementation remain unchanged above the premium UI.

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
        setContent { LeauPATheme { LeauHomeScreen(recognizedText, isListening, isSpeaking, ::startListening, ::speak) { sender -> sendRecognizedMessage = sender } } }
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
    PremiumLeauHomeScreenImpl(initialMessage, isListening, isSpeaking, onMicClick, onLeauReply, registerVoiceMessageSender)
}

@Composable
private fun PremiumLeauHomeScreenImpl(initialMessage: String, isListening: Boolean, isSpeaking: Boolean, onMicClick: () -> Unit, onLeauReply: (String) -> Unit, registerVoiceMessageSender: ((String) -> Unit) -> Unit) {
    var message by remember { mutableStateOf(initialMessage) }
    var isThinking by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val context = LocalContext.current
    LaunchedEffect(initialMessage) { if (initialMessage.isNotEmpty()) message = initialMessage }

    fun history(): List<JSONObject> = messages.takeLast(20).map { m -> JSONObject().put("role", if (m.fromLeau) "model" else "user").put("parts", JSONArray().put(JSONObject().put("text", m.text))) }

    fun sendText(raw: String) {
        val text = raw.trim()
        if (text.isEmpty() || isThinking) return
        messages.add(ChatMessage(text, false)); message = ""
        LeauMemory.rememberFromUserMessage(context, text)
        isThinking = true
        LeauApi.sendMessage(text, history()) { result ->
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                result.onSuccess { reply ->
                    messages.add(ChatMessage(reply, true))
                    val vaultMessages = messages.map { LeauChatVault.VaultMessage(it.text, it.fromLeau) }
                    LeauChatVault.saveConversation(context, vaultMessages)
                    onLeauReply(reply)
                }.onFailure { error -> messages.add(ChatMessage("I couldn't reach my AI brain. ${error.message ?: "Unknown connection error"}", true)) }
                isThinking = false
            }
        }
    }

    registerVoiceMessageSender { recognized -> sendText(recognized) }

    val stateText = when { isListening -> "LISTENING"; isThinking -> "THINKING"; isSpeaking -> "SPEAKING"; else -> "READY" }
    val stateDescription = when { isListening -> "I'm listening"; isThinking -> "Processing your request"; isSpeaking -> "Leau is speaking"; else -> "What can I do for you?" }

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("LEAU", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                    Text("PERSONAL AI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
                TextButton(onClick = { context.startActivity(Intent(context, LeauHubActivity::class.java)) }) { Text("HUB") }
            }
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(30.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(260.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(250.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface))
                        when { isListening -> ListeningLeauEyes(); isThinking -> ThinkingLeauEyes(); isSpeaking -> SpeakingLeauEyes(); else -> BlinkingLeauEyes() }
                    }
                    Text(stateText, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text(stateDescription, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(12.dp))
            if (messages.isNotEmpty()) {
                LazyColumn(Modifier.weight(1f).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 4.dp)) {
                    items(messages) { item ->
                        Surface(shape = RoundedCornerShape(18.dp), color = if (item.fromLeau) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth()) {
                            Text(item.text, modifier = Modifier.padding(14.dp), color = if (item.fromLeau) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    if (isThinking) item { Text("LEAU is thinking…", color = MaterialTheme.colorScheme.secondary) }
                }
            } else {
                Spacer(Modifier.weight(1f))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Set a timer", "Open camera", "My memories").forEach { quick -> AssistChip(onClick = { sendText(quick) }, label = { Text(quick) }) }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = message, onValueChange = { message = it }, modifier = Modifier.weight(1f), placeholder = { Text("Ask Leau anything...") }, singleLine = true, enabled = !isThinking && !isListening && !isSpeaking, shape = RoundedCornerShape(22.dp))
                IconButton(onClick = { sendText(message) }, enabled = !isThinking && message.isNotBlank(), modifier = Modifier.size(52.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)) { Icon(Icons.Default.Send, "Send", tint = MaterialTheme.colorScheme.onPrimary) }
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = onMicClick, enabled = !isThinking && !isSpeaking, modifier = Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(22.dp)) { Icon(Icons.Default.Mic, null); Spacer(Modifier.width(8.dp)); Text(if (isListening) "LISTENING..." else "TALK TO LEAU") }
            Spacer(Modifier.height(18.dp))
        }
    }
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