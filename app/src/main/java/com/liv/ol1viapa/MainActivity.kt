package com.liv.ol1viapa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.random.Random
import com.liv.ol1viapa.ui.theme.LeauPATheme

data class ChatMessage(val text: String, val fromLeau: Boolean)

class MainActivity : ComponentActivity() {
    private var recognizedText by mutableStateOf("")
    private var isListening by mutableStateOf(false)
    private var isSpeaking by mutableStateOf(false)
    private var textToSpeech: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var sendRecognizedMessage: ((String) -> Unit)? = null

    private val microphonePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startSpeechRecognition()
    }

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
        setContent {
            LeauPATheme {
                LeauHomeScreen(
                    initialMessage = recognizedText,
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    onMicClick = ::startListening,
                    onLeauReply = ::speak,
                    registerVoiceMessageSender = { sender -> sendRecognizedMessage = sender }
                )
            }
        }
    }

    private fun normalizeVoiceInput(input: String): String {
        val cleaned = input.trim().replace(Regex("\\s+"), " ")
        val lower = cleaned.lowercase(Locale.US)

        return when (lower) {
            "hey you", "hey u", "hi you", "hi u", "hello you", "hello u",
            "hey leo", "hi leo", "hello leo",
            "hey leu", "hi leu", "hello leu",
            "hey layou", "hi layou", "hello layou",
            "hey liu", "hi liu", "hello liu" -> {
                cleaned.replaceFirst(Regex("(?i)^(hey|hi|hello)\\b.*$"), "$1 Leau")
            }
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
                override fun onPartialResults(partialResults: Bundle?) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    if (partial.isNotBlank()) runOnUiThread { recognizedText = partial }
                }
                override fun onResults(results: Bundle?) {
                    val rawRecognized = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
                    val recognized = normalizeVoiceInput(rawRecognized)
                    runOnUiThread {
                        isListening = false
                        if (recognized.isNotEmpty()) {
                            recognizedText = recognized
                            sendRecognizedMessage?.invoke(recognized)
                        }
                    }
                }
                override fun onError(error: Int) {
                    runOnUiThread {
                        isListening = false
                        if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            Toast.makeText(this@MainActivity, "I couldn't understand that. Try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })
        }
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) startSpeechRecognition()
        else microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    private fun startSpeechRecognition() {
        if (speechRecognizer == null) setupSpeechRecognizer()
        val recognizer = speechRecognizer ?: return
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to Leau")
        }
        recognizedText = ""
        isListening = true
        recognizer.startListening(intent)
    }

    private fun speak(text: String) {
        // Keep the visual spelling "Leau", but give Android TTS a phonetic spelling
        // that reliably sounds like "Liu" / "Lee-oo" instead of reading "Leau".
        val spokenText = text
            .replace(Regex("\\bLeau\\b", RegexOption.IGNORE_CASE), "Lee-oo")
            .replace(Regex("\\bOl1via\\b", RegexOption.IGNORE_CASE), "Olivia")
            .replace(Regex("\\bOlivia\\b", RegexOption.IGNORE_CASE), "Olivia")

        textToSpeech?.speak(
            spokenText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "leau_reply"
        )
    }

    override fun onDestroy() {
        sendRecognizedMessage = null
        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        super.onDestroy()
    }
}

@Composable
fun LeauHomeScreen(
    initialMessage: String,
    isListening: Boolean,
    isSpeaking: Boolean,
    onMicClick: () -> Unit,
    onLeauReply: (String) -> Unit,
    registerVoiceMessageSender: ((String) -> Unit) -> Unit
) {
    var message by remember { mutableStateOf(initialMessage) }
    var isThinking by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val context = LocalContext.current

    LaunchedEffect(initialMessage) {
        if (initialMessage.isNotEmpty()) message = initialMessage
    }

    fun buildHistory(): List<JSONObject> {
        val history = mutableListOf<JSONObject>()
        LeauMemory.buildMemoryHistoryMessage(context)?.let { history.add(it) }
        history.addAll(messages.takeLast(20).map { chatMessage ->
            JSONObject().put("role", if (chatMessage.fromLeau) "model" else "user")
                .put("parts", JSONArray().put(JSONObject().put("text", chatMessage.text)))
        })
        return history
    }

    fun showLocalMemoryReply(reply: String) {
        messages.add(ChatMessage(reply, true))
        onLeauReply(reply)
    }

    fun sendText(textToSend: String) {
        val text = textToSend.trim()
        if (text.isEmpty() || isThinking) return
        val lower = text.lowercase(Locale.US)
        if (lower.matches(Regex("^what do you remember( about me)?[.!?]?$")) || lower.matches(Regex("^what do you know about me[.!?]?$")) || lower.matches(Regex("^show my memories[.!?]?$"))) {
            messages.add(ChatMessage(text, false)); message = ""
            val memories = LeauMemory.getMemories(context)
            val reply = if (memories.isEmpty()) "I don't have any saved memories about you yet." else "Here's what I remember about you:\n" + memories.joinToString("\n") { "• $it" }
            showLocalMemoryReply(reply); return
        }
        if (lower.matches(Regex("^forget everything( you remember)?( about me)?[.!?]?$")) || lower.matches(Regex("^forget all( my)? memories[.!?]?$")) || lower.matches(Regex("^clear (all )?(my )?memories[.!?]?$"))) {
            messages.add(ChatMessage(text, false)); message = ""
            LeauMemory.clearMemories(context)
            showLocalMemoryReply("Done. I've forgotten all of the memories I had saved about you."); return
        }
        val forgetMatch = Regex("(?i)^forget(?:\\s+that)?\\s+(.+?)[.!?]?$").find(text)
        if (forgetMatch != null) {
            messages.add(ChatMessage(text, false)); message = ""
            val target = forgetMatch.groupValues[1].trim()
            val removed = LeauMemory.forgetMemory(context, target)
            showLocalMemoryReply(if (removed) "Done. I've forgotten that memory." else "I couldn't find a saved memory matching that."); return
        }
        LeauMemory.rememberFromUserMessage(context, text)
        val history = buildHistory()
        messages.add(ChatMessage(text, false)); message = ""; isThinking = true
        LeauApi.sendMessage(text, history) { result ->
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                result.onSuccess { reply -> messages.add(ChatMessage(reply, true)); onLeauReply(reply) }
                    .onFailure { error ->
                        val detail = error.message ?: "Unknown connection error"
                        messages.add(ChatMessage("I couldn't reach my AI brain. $detail", true))
                        Toast.makeText(context, detail, Toast.LENGTH_LONG).show()
                    }
                isThinking = false
            }
        }
    }

    registerVoiceMessageSender { recognized -> sendText(recognized) }
    fun sendMessage() { sendText(message) }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.height(28.dp))
            Text("Leau", style = MaterialTheme.typography.headlineLarge)
            Text("Your personal assistant", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(20.dp))
            if (messages.isEmpty()) {
                when {
                    isListening -> { ListeningLeauEyes(); Spacer(modifier = Modifier.height(20.dp)); Text("I'm listening…", style = MaterialTheme.typography.headlineSmall) }
                    isSpeaking -> { SpeakingLeauEyes(); Spacer(modifier = Modifier.height(20.dp)); Text("Leau is speaking…", style = MaterialTheme.typography.headlineSmall) }
                    else -> { BlinkingLeauEyes(); Spacer(modifier = Modifier.height(20.dp)); Text("How can I help?", style = MaterialTheme.typography.headlineSmall) }
                }
                Spacer(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(messages) { chatMessage ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = if (chatMessage.fromLeau) Arrangement.Start else Arrangement.End) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(18.dp)).background(if (chatMessage.fromLeau) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary).padding(horizontal = 16.dp, vertical = 12.dp)) {
                                Text(chatMessage.text, color = if (chatMessage.fromLeau) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                    if (isThinking) { item { ThinkingLeauEyes() }; item { Text("Leau is thinking…", style = MaterialTheme.typography.bodyMedium) } }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = message, onValueChange = { message = it }, modifier = Modifier.weight(1f), placeholder = { Text("Ask Leau...") }, singleLine = true, enabled = !isThinking && !isListening && !isSpeaking, shape = RoundedCornerShape(20.dp))
                IconButton(onClick = ::sendMessage, enabled = !isThinking && !isListening && !isSpeaking) { Icon(Icons.Default.Send, contentDescription = "Send") }
            }
            Spacer(modifier = Modifier.height(12.dp))
            IconButton(onClick = onMicClick, enabled = !isThinking && !isListening && !isSpeaking, modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)) {
                Icon(Icons.Default.Mic, contentDescription = "Talk to Leau", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable private fun BlinkingLeauEyes() { EyeAnimation(height = 220f, width = 220, mode = "blink") }
@Composable private fun ThinkingLeauEyes() { EyeAnimation(height = 220f, width = 180, mode = "think") }
@Composable private fun ListeningLeauEyes() { EyeAnimation(height = 220f, width = 220, mode = "listen") }
@Composable private fun SpeakingLeauEyes() { EyeAnimation(height = 220f, width = 220, mode = "speak") }

@Composable
private fun EyeAnimation(height: Float, width: Int, mode: String) {
    val animatedHeight = remember { Animatable(height) }
    LaunchedEffect(mode) {
        animatedHeight.snapTo(height)
        when (mode) {
            "blink" -> {
                while (true) {
                    delay(Random.nextLong(2500L, 5000L))
                    animatedHeight.animateTo(8f, tween(90))
                    animatedHeight.animateTo(height, tween(120))
                }
            }
            "think" -> {
                while (true) {
                    animatedHeight.animateTo(190f, tween(350))
                    delay(180L)
                    animatedHeight.animateTo(height, tween(450))
                    delay(180L)
                }
            }
            "listen" -> {
                while (true) {
                    animatedHeight.animateTo(205f, tween(500))
                    animatedHeight.animateTo(height, tween(500))
                    delay(120L)
                }
            }
            "speak" -> {
                while (true) {
                    animatedHeight.animateTo(200f, tween(180))
                    animatedHeight.animateTo(height, tween(180))
                    delay(80L)
                }
            }
        }
    }
    Box(modifier = Modifier.size(width = width.dp, height = height.dp), contentAlignment = Alignment.Center) {
        Image(painter = painterResource(id = R.drawable.leau_eyes), contentDescription = "Leau", modifier = Modifier.size(width = width.dp, height = animatedHeight.value.dp), contentScale = ContentScale.FillBounds)
    }
}
