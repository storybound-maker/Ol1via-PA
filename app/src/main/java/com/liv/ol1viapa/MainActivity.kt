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
    var message by remember { mutableStateOf(initialMessage) }
    var isThinking by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val context = LocalContext.current
    LaunchedEffect(initialMessage) { if (initialMessage.isNotEmpty()) message = initialMessage }

    fun buildHistory(): List<JSONObject> {
        val history = mutableListOf<JSONObject>()
        LeauMemory.buildMemoryHistoryMessage(context)?.let { history.add(it) }
        history.addAll(messages.takeLast(20).map { m -> JSONObject().put("role", if (m.fromLeau) "model" else "user").put("parts", JSONArray().put(JSONObject().put("text", m.text))) })
        return history
    }

    fun showLocalReply(reply: String) { messages.add(ChatMessage(reply, true)); onLeauReply(reply) }

    fun openAppCommand(input: String): Boolean {
        val lower = input.lowercase(Locale.US).trim()
        val match = Regex("^(open|launch|start|run|go to)\\s+(.+?)[.!?]?$", RegexOption.IGNORE_CASE).find(lower) ?: return false
        val target = match.groupValues[2].trim()
        if (target == "settings" || target == "android settings" || target == "phone settings") {
            return try { context.startActivity(Intent(Settings.ACTION_SETTINGS)); messages.add(ChatMessage(input, false)); message = ""; showLocalReply("Opening Settings."); true } catch (_: Exception) { false }
        }
        val app = when {
            target.contains("youtube") -> Triple("YouTube", "com.google.android.youtube", "https://www.youtube.com")
            target.contains("chrome") -> Triple("Chrome", "com.android.chrome", "https://www.google.com")
            target.contains("gmail") -> Triple("Gmail", "com.google.android.gm", "mailto:")
            target.contains("maps") -> Triple("Maps", "com.google.android.apps.maps", "geo:0,0?q=maps")
            target.contains("play store") || target.contains("google play") -> Triple("Play Store", "com.android.vending", "https://play.google.com/store")
            else -> null
        } ?: return false
        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(app.second)
            if (launchIntent != null) context.startActivity(launchIntent) else context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(app.third)))
            messages.add(ChatMessage(input, false)); message = ""; showLocalReply("Opening ${app.first}."); true
        } catch (_: Exception) { false }
    }

    fun sendText(textToSend: String) {
        val text = textToSend.trim()
        if (text.isEmpty() || isThinking) return
        if (openAppCommand(text)) return
        val lower = text.lowercase(Locale.US)
        if (lower.matches(Regex("^what do you remember( about me)?[.!?]?$")) || lower.matches(Regex("^what do you know about me[.!?]?$")) || lower.matches(Regex("^show my memories[.!?]?$"))) {
            messages.add(ChatMessage(text, false)); message = ""
            val memories = LeauMemory.getMemories(context)
            val reply = if (memories.isEmpty()) "I don't have any saved memories about you yet." else "Here's what I remember about you:\n" + memories.joinToString("\n") { "• $it" }
            showLocalReply(reply); return
        }
        if (lower.matches(Regex("^forget everything( you remember)?( about me)?[.!?]?$")) || lower.matches(Regex("^forget all( my)? memories[.!?]?$")) || lower.matches(Regex("^clear (all )?(my )?memories[.!?]?$"))) {
            messages.add(ChatMessage(text, false)); message = ""; LeauMemory.clearMemories(context); showLocalReply("Done. I've forgotten all of the memories I had saved about you."); return
        }
        val forgetMatch = Regex("(?i)^forget(?:\\s+that)?\\s+(.+?)[.!?]?$").find(text)
        if (forgetMatch != null) {
            messages.add(ChatMessage(text, false)); message = ""
            val target = forgetMatch.groupValues[1].trim()
            val removed = LeauMemory.forgetMemory(context, target)
            showLocalReply(if (removed) "Done. I've forgotten that memory." else "I couldn't find a saved memory matching that."); return
        }
        LeauMemory.rememberFromUserMessage(context, text)
        val history = buildHistory()
        messages.add(ChatMessage(text, false)); message = ""; isThinking = true
        LeauApi.sendMessage(text, history) { result ->
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                result.onSuccess { reply -> messages.add(ChatMessage(reply, true)); onLeauReply(reply) }.onFailure { error -> val detail = error.message ?: "Unknown connection error"; messages.add(ChatMessage("I couldn't reach my AI brain. $detail", true)); Toast.makeText(context, detail, Toast.LENGTH_LONG).show() }
                isThinking = false
            }
        }
    }

    registerVoiceMessageSender { recognized -> sendText(recognized) }
    fun sendMessage() { sendText(message) }

    Scaffold(Modifier.fillMaxSize()) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(28.dp)); Text("Leau", style = MaterialTheme.typography.headlineLarge); Text("Your personal assistant", style = MaterialTheme.typography.bodyMedium); Spacer(Modifier.height(20.dp))
            if (messages.isEmpty()) {
                when {
                    isListening -> { ListeningLeauEyes(); Spacer(Modifier.height(20.dp)); Text("I'm listening…", style = MaterialTheme.typography.headlineSmall) }
                    isSpeaking -> { SpeakingLeauEyes(); Spacer(Modifier.height(20.dp)); Text("Leau is speaking…", style = MaterialTheme.typography.headlineSmall) }
                    else -> { BlinkingLeauEyes(); Spacer(Modifier.height(20.dp)); Text("How can I help?", style = MaterialTheme.typography.headlineSmall) }
                }
                Spacer(Modifier.weight(1f))
            } else {
                LazyColumn(Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(messages) { chatMessage -> Row(Modifier.fillMaxWidth(), horizontalArrangement = if (chatMessage.fromLeau) Arrangement.Start else Arrangement.End) { Box(Modifier.clip(RoundedCornerShape(18.dp)).background(if (chatMessage.fromLeau) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary).padding(horizontal = 16.dp, vertical = 12.dp)) { Text(chatMessage.text, color = if (chatMessage.fromLeau) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary) } } }
                    if (isThinking) { item { ThinkingLeauEyes() }; item { Text("Leau is thinking…", style = MaterialTheme.typography.bodyMedium) } }
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(value = message, onValueChange = { message = it }, Modifier.weight(1f), placeholder = { Text("Ask Leau...") }, singleLine = true, enabled = !isThinking && !isListening && !isSpeaking, shape = RoundedCornerShape(20.dp)); IconButton(onClick = ::sendMessage, enabled = !isThinking && !isListening && !isSpeaking) { Icon(Icons.Default.Send, "Send") } }
            Spacer(Modifier.height(12.dp))
            IconButton(onClick = onMicClick, enabled = !isThinking && !isListening && !isSpeaking, modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)) { Icon(Icons.Default.Mic, "Talk to Leau", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp)) }
            Spacer(Modifier.height(20.dp))
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
    LaunchedEffect(Unit) {
        while (true) {
            when (mode) {
                "blink" -> { delay(Random.nextLong(2500L, 5000L)); animatedHeight.animateTo(8f, tween(90)); animatedHeight.animateTo(height, tween(120)) }
                "think" -> { animatedHeight.animateTo(190f, tween(350)); animatedHeight.animateTo(height, tween(450)); delay(180L) }
                "listen" -> { animatedHeight.animateTo(205f, tween(500)); animatedHeight.animateTo(height, tween(500)); delay(120L) }
                else -> { animatedHeight.animateTo(200f, tween(180)); animatedHeight.animateTo(height, tween(180)); delay(80L) }
            }
        }
    }
    Box(Modifier.size(width.dp, height.dp), contentAlignment = Alignment.Center) { Image(painterResource(R.drawable.leau_eyes), "Leau", Modifier.size(width.dp, animatedHeight.value.dp), contentScale = ContentScale.FillBounds) }
}
