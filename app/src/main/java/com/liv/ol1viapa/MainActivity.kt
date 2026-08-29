package com.liv.ol1viapa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
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
import androidx.compose.ui.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.liv.ol1viapa.ui.theme.Ol1viaPATheme
import com.liv.ol1viapa.R
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.random.Random

data class ChatMessage(val text: String, val fromOl1via: Boolean)

class MainActivity : ComponentActivity() {
    private var recognizedText by mutableStateOf("")
    private var isListening by mutableStateOf(false)
    private var isSpeaking by mutableStateOf(false)
    private var textToSpeech: TextToSpeech? = null
    private var sendRecognizedMessage: ((String) -> Unit)? = null

    private val speechLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isListening = false
        if (result.resultCode == RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val recognized = matches?.firstOrNull()?.trim().orEmpty()
            if (recognized.isNotEmpty()) {
                recognizedText = recognized
                sendRecognizedMessage?.invoke(recognized)
            }
        }
    }

    private val microphonePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
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
                    override fun onStart(utteranceId: String?) {
                        runOnUiThread { isSpeaking = true }
                    }

                    override fun onDone(utteranceId: String?) {
                        runOnUiThread { isSpeaking = false }
                    }

                    override fun onError(utteranceId: String?) {
                        runOnUiThread { isSpeaking = false }
                    }
                })
            }
        }

        enableEdgeToEdge()
        setContent {
            Ol1viaPATheme {
                Ol1viaHomeScreen(
                    initialMessage = recognizedText,
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    onMicClick = ::startListening,
                    onOl1viaReply = ::speak,
                    registerVoiceMessageSender = { sender ->
                        sendRecognizedMessage = sender
                    }
                )
            }
        }
    }

    private fun speak(text: String) {
        val spokenText = text.replace("Ol1via", "Olivia", ignoreCase = true)
        textToSpeech?.speak(
            spokenText,
            TextToSpeech.QUEUE_FLUSH,
            null,
            "olivia_reply"
        )
    }

    override fun onDestroy() {
        sendRecognizedMessage = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        super.onDestroy()
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startSpeechRecognition()
        } else {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startSpeechRecognition() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to Olivia")
        }
        if (intent.resolveActivity(packageManager) != null) {
            isListening = true
            speechLauncher.launch(intent)
        }
    }
}

@Composable
fun Ol1viaHomeScreen(
    initialMessage: String,
    isListening: Boolean,
    isSpeaking: Boolean,
    onMicClick: () -> Unit,
    onOl1viaReply: (String) -> Unit,
    registerVoiceMessageSender: ((String) -> Unit) -> Unit
) {
    var message by remember { mutableStateOf(initialMessage) }
    var isThinking by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val context = LocalContext.current

    fun buildHistory(): List<JSONObject> {
        val history = mutableListOf<JSONObject>()
        Ol1viaMemory.buildMemoryHistoryMessage(context)?.let { history.add(it) }
        history.addAll(
            messages.takeLast(20).map { chatMessage ->
                JSONObject()
                    .put("role", if (chatMessage.fromOl1via) "model" else "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", chatMessage.text)))
            }
        )
        return history
    }

    fun showLocalMemoryReply(reply: String) {
        messages.add(ChatMessage(reply, true))
        onOl1viaReply(reply)
    }

    fun sendText(textToSend: String) {
        val text = textToSend.trim()
        if (text.isEmpty() || isThinking) return

        val lower = text.lowercase(Locale.US)

        if (lower.matches(Regex("^what do you remember( about me)?[.!?]?$")) ||
            lower.matches(Regex("^what do you know about me[.!?]?$")) ||
            lower.matches(Regex("^show my memories[.!?]?$"))
        ) {
            messages.add(ChatMessage(text, false))
            message = ""
            val memories = Ol1viaMemory.getMemories(context)
            val reply = if (memories.isEmpty()) {
                "I don't have any saved memories about you yet."
            } else {
                "Here's what I remember about you:\n" + memories.joinToString("\n") { "• $it" }
            }
            showLocalMemoryReply(reply)
            return
        }

        if (lower.matches(Regex("^forget everything( you remember)?( about me)?[.!?]?$")) ||
            lower.matches(Regex("^forget all( my)? memories[.!?]?$")) ||
            lower.matches(Regex("^clear (all )?(my )?memories[.!?]?$"))
        ) {
            messages.add(ChatMessage(text, false))
            message = ""
            Ol1viaMemory.clearMemories(context)
            showLocalMemoryReply("Done. I've forgotten all of the memories I had saved about you.")
            return
        }

        val forgetMatch = Regex("(?i)^forget(?:\\s+that)?\\s+(.+?)[.!?]?$").find(text)
        if (forgetMatch != null) {
            messages.add(ChatMessage(text, false))
            message = ""
            val target = forgetMatch.groupValues[1].trim()
            val removed = Ol1viaMemory.forgetMemory(context, target)
            val reply = if (removed) "Done. I've forgotten that memory." else "I couldn't find a saved memory matching that."
            showLocalMemoryReply(reply)
            return
        }

        Ol1viaMemory.rememberFromUserMessage(context, text)
        val history = buildHistory()
        messages.add(ChatMessage(text, false))
        message = ""
        isThinking = true

        Ol1viaApi.sendMessage(text, history) { result ->
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                result.onSuccess { reply ->
                    messages.add(ChatMessage(reply, true))
                    onOl1viaReply(reply)
                }.onFailure { error ->
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
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(28.dp))
            Text("Ol1via", style = MaterialTheme.typography.headlineLarge)
            Text("Your personal assistant", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(20.dp))

            if (messages.isEmpty()) {
                when {
                    isListening -> {
                        ListeningOl1viaEyes()
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("I'm listening…", style = MaterialTheme.typography.headlineSmall)
                    }
                    isSpeaking -> {
                        SpeakingOl1viaEyes()
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("Ol1via is speaking…", style = MaterialTheme.typography.headlineSmall)
                    }
                    else -> {
                        BlinkingOl1viaEyes()
                        Spacer(modifier = Modifier.height(20.dp))
                        Text("How can I help?", style = MaterialTheme.typography.headlineSmall)
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { chatMessage ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (chatMessage.fromOl1via) Arrangement.Start else Arrangement.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(if (chatMessage.fromOl1via) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.primary)
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    chatMessage.text,
                                    color = if (chatMessage.fromOl1via) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                    if (isThinking) {
                        item { ThinkingOl1viaEyes() }
                        item { Text("Ol1via is thinking…", style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Ask Ol1via...") },
                    singleLine = true,
                    enabled = !isThinking && !isListening && !isSpeaking,
                    shape = RoundedCornerShape(20.dp)
                )
                IconButton(onClick = ::sendMessage, enabled = !isThinking && !isListening && !isSpeaking) {
                    Icon(Icons.Default.Send, contentDescription = "Send")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            IconButton(
                onClick = onMicClick,
                enabled = !isThinking && !isListening && !isSpeaking,
                modifier = Modifier.size(72.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Mic, contentDescription = "Talk to Olivia", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(32.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun BlinkingOl1viaEyes() {
    val blinkHeight = remember { Animatable(220f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(Random.nextLong(2500L, 5000L))
            blinkHeight.animateTo(8f, animationSpec = tween(90))
            blinkHeight.animateTo(220f, animationSpec = tween(120))
        }
    }

    Box(
        modifier = Modifier.size(width = 220.dp, height = 220.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ol1via_eyes),
            contentDescription = "Ol1via",
            modifier = Modifier.size(width = 220.dp, height = blinkHeight.value.dp),
            contentScale = ContentScale.FillBounds
        )
    }
}

@Composable
private fun ThinkingOl1viaEyes() {
    val thinkingHeight = remember { Animatable(220f) }

    LaunchedEffect(Unit) {
        while (true) {
            thinkingHeight.animateTo(190f, animationSpec = tween(350))
            thinkingHeight.animateTo(220f, animationSpec = tween(450))
            delay(180L)
        }
    }

    Box(
        modifier = Modifier.size(width = 180.dp, height = 220.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ol1via_eyes),
            contentDescription = "Ol1via thinking",
            modifier = Modifier.size(width = 180.dp, height = thinkingHeight.value.dp),
            contentScale = ContentScale.FillBounds
        )
    }
}

@Composable
private fun ListeningOl1viaEyes() {
    val listeningHeight = remember { Animatable(220f) }

    LaunchedEffect(Unit) {
        while (true) {
            listeningHeight.animateTo(205f, animationSpec = tween(500))
            listeningHeight.animateTo(220f, animationSpec = tween(500))
            delay(120L)
        }
    }

    Box(
        modifier = Modifier.size(width = 220.dp, height = 220.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ol1via_eyes),
            contentDescription = "Ol1via listening",
            modifier = Modifier.size(width = 220.dp, height = listeningHeight.value.dp),
            contentScale = ContentScale.FillBounds
        )
    }
}

@Composable
private fun SpeakingOl1viaEyes() {
    val speakingHeight = remember { Animatable(220f) }

    LaunchedEffect(Unit) {
        while (true) {
            speakingHeight.animateTo(200f, animationSpec = tween(180))
            speakingHeight.animateTo(220f, animationSpec = tween(180))
            delay(80L)
        }
    }

    Box(
        modifier = Modifier.size(width = 220.dp, height = 220.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.ol1via_eyes),
            contentDescription = "Ol1via speaking",
            modifier = Modifier.size(width = 220.dp, height = speakingHeight.value.dp),
            contentScale = ContentScale.FillBounds
        )
    }
}
