package com.liv.ol1viapa

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Send
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import kotlin.math.abs

private val LeauGreen = Color(0xFFB8FF5A)
private val LeauMint = Color(0xFF8CFFD3)
private val LeauBackground = Color(0xFF07100D)
private val LeauCard = Color(0xFF0D1B17)
private val LeauCard2 = Color(0xFF12251F)
private val LeauTextMuted = Color(0xFF8BA69C)

data class ChatMessage(val text: String, val fromLeau: Boolean)

enum class LeauPage { HOME, CHAT }

enum class LeauMode { IDLE, LISTENING, THINKING, SPEAKING }

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
                LeauApp(
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    initialMessage = recognizedText,
                    onStartListening = ::startListening,
                    onSpeak = ::speak,
                    registerVoiceSender = { sender -> sendRecognizedMessage = sender }
                )
            }
        }
    }

    private fun normalizeVoiceInput(input: String): String {
        val cleaned = input.trim().replace(Regex("\\s+"), " ")
        return when (cleaned.lowercase(Locale.US)) {
            "hey you", "hey u", "hi you", "hi u", "hello you", "hello u", "hey leo", "hi leo", "hello leo", "hey leu", "hi leu", "hello leu", "hey layou", "hi layou", "hello layou", "hey liu", "hi liu", "hello liu" -> "Leau"
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
                    val raw = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.trim().orEmpty()
                    val recognized = normalizeVoiceInput(raw)
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startSpeechRecognition()
        } else {
            microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
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
        val spokenText = text.replace(Regex("\\bLeau\\b", RegexOption.IGNORE_CASE), "Liu")
        textToSpeech?.speak(spokenText, TextToSpeech.QUEUE_FLUSH, null, "leau_reply")
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
private fun LeauApp(
    isListening: Boolean,
    isSpeaking: Boolean,
    initialMessage: String,
    onStartListening: () -> Unit,
    onSpeak: (String) -> Unit,
    registerVoiceSender: (((String) -> Unit)) -> Unit
) {
    var page by remember { mutableStateOf(LeauPage.HOME) }
    var isThinking by remember { mutableStateOf(false) }
    var showHub by remember { mutableStateOf(false) }
    var showConnect by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(initialMessage) {
        if (initialMessage.isNotBlank()) page = LeauPage.CHAT
    }

    fun buildHistory(): List<JSONObject> {
        val history = mutableListOf<JSONObject>()
        LeauMemory.buildMemoryHistoryMessage(context)?.let { history.add(it) }
        history.addAll(messages.takeLast(20).map { m ->
            JSONObject()
                .put("role", if (m.fromLeau) "model" else "user")
                .put("parts", JSONArray().put(JSONObject().put("text", m.text)))
        })
        return history
    }

    fun addReply(reply: String) {
        messages.add(ChatMessage(reply, true))
        onSpeak(reply)
    }

    fun sendText(raw: String) {
        val text = raw.trim()
        if (text.isEmpty() || isThinking) return
        keyboard?.hide()
        page = LeauPage.CHAT

        val lower = text.lowercase(Locale.US)
        if (lower == "open settings" || lower == "open android settings") {
            runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
            messages.add(ChatMessage(text, false))
            addReply("Opening Settings.")
            return
        }
        if (lower.startsWith("set a timer") || lower.startsWith("start a timer") || lower.startsWith("set timer")) {
            val match = Regex("(\\d+)\\s*(second|seconds|minute|minutes|hour|hours)", RegexOption.IGNORE_CASE).find(lower)
            val amount = match?.groupValues?.getOrNull(1)?.toIntOrNull()
            val unit = match?.groupValues?.getOrNull(2)?.lowercase(Locale.US)
            if (amount != null && amount > 0 && unit != null) {
                val seconds = when {
                    unit.startsWith("second") -> amount
                    unit.startsWith("minute") -> amount * 60
                    else -> amount * 3600
                }
                runCatching {
                    context.startActivity(Intent(AlarmClock.ACTION_SET_TIMER).apply {
                        putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                        putExtra(AlarmClock.EXTRA_MESSAGE, "Leau timer")
                        putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    })
                }
                messages.add(ChatMessage(text, false))
                addReply("Setting a $amount $unit timer.")
                return
            }
        }
        if (lower.matches(Regex("^what do you remember( about me)?[.!?]?$")) || lower.matches(Regex("^what do you know about me[.!?]?$"))) {
            messages.add(ChatMessage(text, false))
            val memories = LeauMemory.getMemories(context)
            addReply(if (memories.isEmpty()) "I don't have any saved memories about you yet." else "Here's what I remember about you:\n" + memories.joinToString("\n") { "• $it" })
            return
        }

        LeauMemory.rememberFromUserMessage(context, text)
        val history = buildHistory()
        messages.add(ChatMessage(text, false))
        isThinking = true
        LeauApi.sendMessage(text, history) { result ->
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                result.onSuccess { reply -> addReply(reply) }
                    .onFailure { error ->
                        messages.add(ChatMessage("I couldn't reach my AI brain. ${error.message ?: "Try again."}", true))
                        Toast.makeText(context, error.message ?: "Connection error", Toast.LENGTH_LONG).show()
                    }
                isThinking = false
            }
        }
    }

    registerVoiceSender { recognized -> sendText(recognized) }

    Box(Modifier.fillMaxSize().background(LeauBackground)) {
        AnimatedContent(
            targetState = page,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
            label = "leau_page"
        ) { current ->
            when (current) {
                LeauPage.HOME -> LeauHome(
                    isListening = isListening,
                    isSpeaking = isSpeaking,
                    onLeauTap = {
                        page = LeauPage.CHAT
                        onStartListening()
                    },
                    onConnect = { showConnect = true },
                    onChats = { page = LeauPage.CHAT },
                    onHub = { showHub = true },
                    onMessageTap = { page = LeauPage.CHAT }
                )
                LeauPage.CHAT -> LeauChat(
                    messages = messages,
                    isListening = isListening,
                    isThinking = isThinking,
                    isSpeaking = isSpeaking,
                    initialMessage = initialMessage,
                    onBack = { page = LeauPage.HOME },
                    onSend = ::sendText,
                    onVoice = onStartListening,
                    onHub = { showHub = true }
                )
            }
        }

        if (showHub) {
            HubSheet(onDismiss = { showHub = false }, onSettings = {
                showHub = false
                runCatching { context.startActivity(Intent(Settings.ACTION_SETTINGS)) }
            })
        }
        if (showConnect) {
            ConnectDialog(onDismiss = { showConnect = false })
        }
    }
}

@Composable
private fun LeauHome(
    isListening: Boolean,
    isSpeaking: Boolean,
    onLeauTap: () -> Unit,
    onConnect: () -> Unit,
    onChats: () -> Unit,
    onHub: () -> Unit,
    onMessageTap: () -> Unit
) {
    val mode = when {
        isListening -> LeauMode.LISTENING
        isSpeaking -> LeauMode.SPEAKING
        else -> LeauMode.IDLE
    }
    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.fillMaxWidth().padding(top = 18.dp), contentAlignment = Alignment.Center) {
            Text("Leau", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onHub, modifier = Modifier.align(Alignment.CenterEnd).size(42.dp)) {
                Icon(Icons.Outlined.Eco, contentDescription = "Hub", tint = LeauGreen, modifier = Modifier.size(25.dp))
            }
        }

        Spacer(Modifier.height(18.dp))
        Text(
            when (mode) {
                LeauMode.IDLE -> "Tap to speak"
                LeauMode.LISTENING -> "Listening…"
                LeauMode.SPEAKING -> "Leau is speaking…"
                else -> ""
            },
            color = if (mode == LeauMode.LISTENING) LeauGreen else LeauTextMuted,
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(18.dp))

        LeauEyes(
            mode = mode,
            modifier = Modifier.size(238.dp),
            onClick = onLeauTap
        )

        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            SmallHomeButton("Connect", Icons.Outlined.Link, onConnect)
            Spacer(Modifier.width(10.dp))
            SmallHomeButton("Chats", Icons.Outlined.ChatBubbleOutline, onChats)
        }

        Spacer(Modifier.weight(1f))
        MessagePill(onClick = onMessageTap)
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun LeauEyes(mode: LeauMode, modifier: Modifier, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "leau_breath")
    val floatY by transition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "float"
    )
    val breathe by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "breathe"
    )
    val listeningPulse by transition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "listenPulse"
    )

    Box(
        modifier
            .scale(breathe)
            .padding(vertical = 8.dp)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.pressed }) {
                            event.changes.forEach { it.consume() }
                            onClick()
                            while (event.changes.any { it.pressed }) {
                                awaitPointerEvent()
                            }
                            break
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.size(212.dp).alpha(if (mode == LeauMode.LISTENING) 0.88f + 0.12f * listeningPulse else 1f),
            shape = RoundedCornerShape(48.dp),
            color = LeauCard,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (mode == LeauMode.LISTENING) LeauGreen else Color(0xFF1C3930))
        ) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(id = com.liv.ol1viapa.R.drawable.leau_eyes),
                    contentDescription = "Leau",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(150.dp).offset(y = floatY.dp)
                )
            }
        }
    }
}

@Composable
private fun SmallHomeButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.height(42.dp),
        shape = RoundedCornerShape(21.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF31584A)),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 15.dp)
    ) {
        Icon(icon, null, tint = LeauGreen, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, color = Color(0xFFD8E9E1), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun MessagePill(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(58.dp),
        shape = RoundedCornerShape(29.dp),
        color = LeauCard2,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF31584A))
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Ask Leau anything…", color = LeauTextMuted, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            Icon(Icons.Outlined.Send, contentDescription = "Type a message", tint = LeauGreen, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun LeauChat(
    messages: List<ChatMessage>,
    isListening: Boolean,
    isThinking: Boolean,
    isSpeaking: Boolean,
    initialMessage: String,
    onBack: () -> Unit,
    onSend: (String) -> Unit,
    onVoice: () -> Unit,
    onHub: () -> Unit
) {
    var input by remember { mutableStateOf(TextFieldValue(initialMessage)) }
    val listState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current
    var dragAmount by remember { mutableStateOf(0f) }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }
    LaunchedEffect(initialMessage) {
        if (initialMessage.isNotBlank()) input = TextFieldValue(initialMessage)
    }

    Column(
        Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 16.dp)
    ) {
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back", tint = Color.White) }
            Text("Leau", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            IconButton(onClick = onHub) { Icon(Icons.Outlined.Eco, "Hub", tint = LeauGreen) }
        }

        Text(
            when {
                isListening -> "Listening…"
                isThinking -> "Thinking…"
                isSpeaking -> "Speaking…"
                else -> ""
            },
            color = LeauGreen,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
        ) {
            items(messages) { message ->
                ChatBubble(message)
            }
            if (isThinking) {
                item { ChatBubble(ChatMessage("Thinking…", true)) }
            }
        }

        Row(
            Modifier.fillMaxWidth().imePadding().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.weight(1f).height(58.dp).pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { dragAmount = 0f },
                        onHorizontalDrag = { change, amount ->
                            if (amount > 0f) {
                                dragAmount += amount
                                change.consume()
                            }
                        },
                        onDragEnd = {
                            if (dragAmount > 90f && input.text.isNotBlank()) {
                                val toSend = input.text
                                input = TextFieldValue("")
                                keyboard?.hide()
                                onSend(toSend)
                            }
                            dragAmount = 0f
                        },
                        onDragCancel = { dragAmount = 0f }
                    )
                },
                shape = RoundedCornerShape(29.dp),
                color = LeauCard2,
                border = androidx.compose.foundation.BorderStroke(1.dp, if (dragAmount > 20f) LeauGreen else Color(0xFF31584A))
            ) {
                androidx.compose.foundation.layout.BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 18.dp, vertical = 18.dp),
                    decorationBox = { inner ->
                        if (input.text.isEmpty()) Text("Ask Leau anything…", color = LeauTextMuted, style = MaterialTheme.typography.bodyLarge)
                        inner()
                    }
                )
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = {
                    if (input.text.isNotBlank()) {
                        val toSend = input.text
                        input = TextFieldValue("")
                        keyboard?.hide()
                        onSend(toSend)
                    } else {
                        onVoice()
                    }
                },
                modifier = Modifier.size(52.dp).background(LeauGreen, RoundedCornerShape(26.dp))
            ) {
                Icon(Icons.Outlined.Send, "Send", tint = Color(0xFF07100D), modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatMessage) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (message.fromLeau) Arrangement.Start else Arrangement.End) {
        Surface(
            color = if (message.fromLeau) LeauCard2 else LeauGreen,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(0.82f)
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
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.42f)).padding(18.dp), contentAlignment = Alignment.TopEnd) {
        Surface(shape = RoundedCornerShape(28.dp), color = LeauCard, border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF31584A)), modifier = Modifier.width(300.dp)) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Eco, null, tint = LeauGreen, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Leau", color = Color.White, style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.height(8.dp))
                Text("Your control space", color = LeauTextMuted)
                Spacer(Modifier.height(18.dp))
                OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                    Icon(Icons.Outlined.Settings, null, tint = LeauGreen)
                    Spacer(Modifier.width(8.dp))
                    Text("Settings", color = Color.White)
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close", color = LeauGreen) }
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
        text = { Text("Connect Leau to services and devices as integrations are added. This is the home for Leau's external connections—not chat history.", color = LeauTextMuted) },
        confirmButton = { Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = LeauGreen, contentColor = Color(0xFF07100D))) { Text("Done") } }
    )
}
