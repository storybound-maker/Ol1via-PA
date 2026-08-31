package com.liv.ol1viapa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.AlarmClock
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
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.liv.ol1viapa.ui.theme.LeauPATheme
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

private val LeauGreen = Color(0xFFB8FF5A)
private val LeauBackground = Color(0xFF07100D)
private val LeauCard = Color(0xFF0D1B17)
private val LeauCard2 = Color(0xFF12251F)
private val LeauMuted = Color(0xFF8BA69C)

data class ChatMessage(val text: String, val fromLeau: Boolean)
enum class LeauPage { HOME, CHAT }
enum class LeauMode { IDLE, LISTENING, SPEAKING }

class MainActivity : ComponentActivity() {
    private var recognizedText by mutableStateOf("")
    private var isListening by mutableStateOf(false)
    private var isSpeaking by mutableStateOf(false)
    private var tts: TextToSpeech? = null
    private var recognizer: SpeechRecognizer? = null
    private var voiceSender: ((String) -> Unit)? = null

    private val micPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (it) startSpeechRecognition()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(.95f)
                tts?.setPitch(1.05f)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(id: String?) { runOnUiThread { isSpeaking = true } }
                    override fun onDone(id: String?) { runOnUiThread { isSpeaking = false } }
                    override fun onError(id: String?) { runOnUiThread { isSpeaking = false } }
                })
            }
        }
        setupRecognizer()
        enableEdgeToEdge()
        setContent {
            LeauPATheme {
                LeauApp(
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    initialMessage = recognizedText,
                    startListening = ::startListening,
                    speak = ::speak
                ) { sender -> voiceSender = sender }
            }
        }
    }

    private fun setupRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        recognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(p: Bundle?) { runOnUiThread { isListening = true } }
                override fun onBeginningOfSpeech() { runOnUiThread { isListening = true } }
                override fun onRmsChanged(v: Float) = Unit
                override fun onBufferReceived(b: ByteArray?) = Unit
                override fun onEndOfSpeech() { runOnUiThread { isListening = false } }
                override fun onPartialResults(b: Bundle?) {
                    val value = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                    if (value.isNotBlank()) runOnUiThread { recognizedText = value }
                }
                override fun onResults(b: Bundle?) {
                    val value = b?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
                    runOnUiThread {
                        isListening = false
                        if (value.isNotBlank()) {
                            recognizedText = value
                            voiceSender?.invoke(value)
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
                override fun onEvent(type: Int, p: Bundle?) = Unit
            })
        }
    }

    private fun startListening() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startSpeechRecognition()
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startSpeechRecognition() {
        if (recognizer == null) setupRecognizer()
        val r = recognizer ?: return
        recognizedText = ""
        isListening = true
        r.startListening(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Talk to Leau")
        })
    }

    private fun speak(text: String) {
        tts?.speak(
            text.replace(Regex("\\bLeau\\b", RegexOption.IGNORE_CASE), "Liu"),
            TextToSpeech.QUEUE_FLUSH,
            null,
            "leau_reply"
        )
    }

    override fun onDestroy() {
        voiceSender = null
        recognizer?.cancel()
        recognizer?.destroy()
        recognizer = null
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}

@Composable
private fun LeauApp(
    isListening: Boolean,
    isSpeaking: Boolean,
    initialMessage: String,
    startListening: () -> Unit,
    speak: (String) -> Unit,
    registerVoice: (((String) -> Unit)) -> Unit
) {
    var page by remember { mutableStateOf(LeauPage.HOME) }
    var thinking by remember { mutableStateOf(false) }
    var hub by remember { mutableStateOf(false) }
    var connect by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current

    fun history(): List<JSONObject> {
        val result = mutableListOf<JSONObject>()
        LeauMemory.buildMemoryHistoryMessage(context)?.let { result.add(it) }
        result.addAll(
            messages.takeLast(20).map {
                JSONObject()
                    .put("role", if (it.fromLeau) "model" else "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", it.text)))
            }
        )
        return result
    }

    fun reply(text: String) {
        messages.add(ChatMessage(text, true))
        speak(text)
    }

    fun send(raw: String) {
        val text = raw.trim()
        if (text.isEmpty() || thinking) return
        keyboard?.hide()
        page = LeauPage.CHAT
        val lower = text.lowercase(Locale.US)

        if (lower == "open settings" || lower == "open android settings") {
            runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
            messages.add(ChatMessage(text, false))
            reply("Opening Settings.")
            return
        }

        val timer = Regex("(\\d+)\\s*(second|seconds|minute|minutes|hour|hours)", RegexOption.IGNORE_CASE).find(lower)
        if (lower.startsWith("set timer") || lower.startsWith("set a timer") || lower.startsWith("start a timer")) {
            val amount = timer?.groupValues?.get(1)?.toIntOrNull()
            val unit = timer?.groupValues?.get(2)?.lowercase(Locale.US)
            if (amount != null && amount > 0 && unit != null) {
                val seconds = when {
                    unit.startsWith("second") -> amount
                    unit.startsWith("minute") -> amount * 60
                    else -> amount * 3600
                }
                runCatching {
                    context.startActivity(
                        Intent(AlarmClock.ACTION_SET_TIMER).apply {
                            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                            putExtra(AlarmClock.EXTRA_MESSAGE, "Leau timer")
                            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                        }
                    )
                }
                messages.add(ChatMessage(text, false))
                reply("Setting a $amount $unit timer.")
                return
            }
        }

        if (lower.matches(Regex("^what do you remember( about me)?[.!?]?$")) ||
            lower.matches(Regex("^what do you know about me[.!?]?$"))) {
            messages.add(ChatMessage(text, false))
            val memories = LeauMemory.getMemories(context)
            reply(
                if (memories.isEmpty()) {
                    "I don't have any saved memories about you yet."
                } else {
                    "Here's what I remember about you:\n" + memories.joinToString("\n") { "• $it" }
                }
            )
            return
        }

        LeauMemory.rememberFromUserMessage(context, text)
        messages.add(ChatMessage(text, false))
        thinking = true
        LeauApi.sendMessage(text, history()) { result ->
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                result.onSuccess { reply(it) }
                    .onFailure {
                        messages.add(ChatMessage("I couldn't reach my AI brain. ${it.message ?: "Try again."}", true))
                        Toast.makeText(context, it.message ?: "Connection error", Toast.LENGTH_LONG).show()
                    }
                thinking = false
            }
        }
    }

    registerVoice { recognized -> send(recognized) }

    Box(Modifier.fillMaxSize().background(LeauBackground)) {
        AnimatedContent(targetState = page, label = "page") { current ->
            when (current) {
                LeauPage.HOME -> LeauHome(
                    listening = isListening,
                    speaking = isSpeaking,
                    onLeau = { page = LeauPage.CHAT; startListening() },
                    onConnect = { connect = true },
                    onChats = { page = LeauPage.CHAT },
                    onHub = { hub = true },
                    onMessage = { page = LeauPage.CHAT }
                )
                LeauPage.CHAT -> LeauChat(
                    messages = messages,
                    listening = isListening,
                    thinking = thinking,
                    speaking = isSpeaking,
                    initial = initialMessage,
                    onBack = { page = LeauPage.HOME },
                    onSend = ::send,
                    onVoice = startListening,
                    onHub = { hub = true }
                )
            }
        }
        if (hub) {
            HubSheet(
                onDismiss = { hub = false },
                onSettings = {
                    hub = false
                    runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
                }
            )
        }
        if (connect) ConnectDialog { connect = false }
    }
}

@Composable
private fun LeauHome(
    listening: Boolean,
    speaking: Boolean,
    onLeau: () -> Unit,
    onConnect: () -> Unit,
    onChats: () -> Unit,
    onHub: () -> Unit,
    onMessage: () -> Unit
) {
    val mode = when {
        listening -> LeauMode.LISTENING
        speaking -> LeauMode.SPEAKING
        else -> LeauMode.IDLE
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("Leau", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            IconButton(
                onClick = onHub,
                modifier = Modifier.align(Alignment.CenterEnd).size(42.dp)
            ) {
                Icon(Icons.Outlined.Eco, contentDescription = "Hub", tint = LeauGreen, modifier = Modifier.size(25.dp))
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            text = when (mode) {
                LeauMode.IDLE -> "Tap to speak"
                LeauMode.LISTENING -> "Listening…"
                LeauMode.SPEAKING -> "Leau is speaking…"
            },
            color = if (mode == LeauMode.LISTENING) LeauGreen else LeauMuted,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(18.dp))
        LeauEyes(mode, onLeau)
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.Center) {
            SmallButton(text = "Connect", icon = Icons.Outlined.Link, onClick = onConnect)
            Spacer(Modifier.width(10.dp))
            SmallButton(text = "Chats", icon = Icons.Outlined.ChatBubbleOutline, onClick = onChats)
        }
        Spacer(Modifier.weight(1f))
        MessagePill(onMessage)
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun LeauEyes(mode: LeauMode, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "eyes")
    val y by transition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            tween(2600, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "hover"
    )
    val scale by transition.animateFloat(
        initialValue = .97f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            tween(2200, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            RepeatMode.Reverse
        ),
        label = "breathe"
    )
    val pulse by transition.animateFloat(
        initialValue = .72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "listen"
    )

    Box(Modifier.size(238.dp).scale(scale), contentAlignment = Alignment.Center) {
        Surface(
            modifier = Modifier
                .size(212.dp)
                .alpha(if (mode == LeauMode.LISTENING) .86f + .14f * pulse else 1f)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(48.dp),
            color = LeauCard,
            border = BorderStroke(1.dp, if (mode == LeauMode.LISTENING) LeauGreen else Color(0xFF1C3930))
        ) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(R.drawable.leau_eyes),
                    contentDescription = "Leau",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(150.dp).offset(y = y.dp)
                )
            }
        }
    }
}

@Composable
private fun SmallButton(text: String, icon: ImageVector, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(42.dp),
        shape = RoundedCornerShape(21.dp),
        border = BorderStroke(1.dp, Color(0xFF31584A)),
        contentPadding = PaddingValues(horizontal = 15.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = LeauGreen, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color(0xFFD8E9E1), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun MessagePill(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(29.dp),
        color = LeauCard2,
        border = BorderStroke(1.dp, Color(0xFF31584A))
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Ask Leau anything…", color = LeauMuted, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Icon(imageVector = Icons.Outlined.Send, contentDescription = "Type a message", tint = LeauGreen, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun LeauChat(
    messages: List<ChatMessage>,
    listening: Boolean,
    thinking: Boolean,
    speaking: Boolean,
    initial: String,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onVoice: () -> Unit,
    onHub: () -> Unit
) {
    var input by remember { mutableStateOf(TextFieldValue(initial)) }
    var drag by remember { mutableFloatStateOf(0f) }
    val list = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) list.animateScrollToItem(messages.lastIndex)
    }
    LaunchedEffect(initial) {
        if (initial.isNotBlank()) input = TextFieldValue(initial)
    }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Text("Leau", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = onHub) {
                Icon(Icons.Outlined.Eco, contentDescription = "Hub", tint = LeauGreen)
            }
        }

        Text(
            text = when {
                listening -> "Listening…"
                thinking -> "Thinking…"
                speaking -> "Speaking…"
                else -> ""
            },
            color = LeauGreen,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        LazyColumn(
            state = list,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(items = messages) { message -> ChatBubble(message) }
            if (thinking) item { ChatBubble(ChatMessage("Thinking…", true)) }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = input.text,
                onValueChange = { input = TextFieldValue(it) },
                modifier = Modifier
                    .weight(1f)
                    .height(58.dp)
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = { drag = 0f },
                            onHorizontalDrag = { change, amount ->
                                if (amount > 0) {
                                    drag += amount
                                    change.consume()
                                }
                            },
                            onDragEnd = {
                                if (drag > 90f && input.text.isNotBlank()) {
                                    val text = input.text
                                    input = TextFieldValue("")
                                    keyboard?.hide()
                                    onSend(text)
                                }
                                drag = 0f
                            },
                            onDragCancel = { drag = 0f }
                        )
                    },
                singleLine = true,
                placeholder = { Text("Ask Leau anything…", color = LeauMuted) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = LeauCard2,
                    unfocusedContainerColor = LeauCard2,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = LeauGreen,
                    unfocusedIndicatorColor = if (drag > 20f) LeauGreen else Color(0xFF31584A)
                ),
                shape = RoundedCornerShape(29.dp)
            )
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (input.text.isNotBlank()) {
                        val text = input.text
                        input = TextFieldValue("")
                        keyboard?.hide()
                        onSend(text)
                    } else {
                        onVoice()
                    }
                },
                modifier = Modifier.size(52.dp).background(LeauGreen, RoundedCornerShape(26.dp))
            ) {
                Icon(Icons.Outlined.Send, contentDescription = "Send", tint = Color(0xFF07100D), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.fromLeau) Arrangement.Start else Arrangement.End
    ) {
        Surface(
            color = if (message.fromLeau) LeauCard2 else LeauGreen,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(.82f)
        ) {
            Text(
                message.text,
                color = if (message.fromLeau) Color(0xFFE8F7F0) else Color(0xFF07100D),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(15.dp)
            )
        }
    }
}

@Composable
private fun HubSheet(onDismiss: () -> Unit, onSettings: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = .42f)).padding(18.dp),
        contentAlignment = Alignment.TopEnd
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = LeauCard,
            border = BorderStroke(1.dp, Color(0xFF31584A)),
            modifier = Modifier.width(300.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Eco, contentDescription = null, tint = LeauGreen, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Leau", color = Color.White, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(8.dp))
                Text("Your control space", color = LeauMuted)
                Spacer(Modifier.height(18.dp))
                OutlinedButton(
                    onClick = onSettings,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(Icons.Outlined.Settings, contentDescription = null, tint = LeauGreen)
                    Spacer(Modifier.width(8.dp))
                    Text("Settings", color = Color.White)
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close", color = LeauGreen)
                }
            }
        }
    }
}

@Composable
private fun ConnectDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LeauCard,
        title = { Text("Connect", color = Color.White) },
        text = {
            Text(
                "Connect Leau to services and devices as integrations are added. Connect is for external capabilities, not chat history.",
                color = LeauMuted
            )
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = LeauGreen,
                    contentColor = Color(0xFF07100D)
                )
            ) {
                Text("Done")
            }
        }
    )
}
