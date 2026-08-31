package com.liv.ol1viapa

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.scale
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

@Composable
fun PremiumLeauHomeScreen(initialMessage: String, isListening: Boolean, isSpeaking: Boolean, onMicClick: () -> Unit, onLeauReply: (String) -> Unit, registerVoiceMessageSender: ((String) -> Unit) -> Unit) {
    var message by remember { mutableStateOf(initialMessage) }
    var isThinking by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val context = LocalContext.current
    val bg = Color(0xFF050B0D); val panel = Color(0xCC0B1718); val line = Color(0xFF20423D); val mint = Color(0xFF69F0C4); val cyan = Color(0xFF7DEBFF); val purple = Color(0xFF9B8CFF)
    LaunchedEffect(initialMessage) { if (initialMessage.isNotEmpty()) message = initialMessage }
    fun buildHistory(): List<JSONObject> { val h = mutableListOf<JSONObject>(); LeauMemory.buildMemoryHistoryMessage(context)?.let(h::add); h.addAll(messages.takeLast(20).map { m -> JSONObject().put("role", if (m.fromLeau) "model" else "user").put("parts", JSONArray().put(JSONObject().put("text", m.text))) }); return h }
    fun reply(text: String) { messages.add(ChatMessage(text, true)); onLeauReply(text) }
    fun launchApp(input: String, name: String, pkg: String, fallback: String): Boolean { return try { val launch = context.packageManager.getLaunchIntentForPackage(pkg); if (launch != null) context.startActivity(launch) else context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(fallback))); messages.add(ChatMessage(input, false)); message = ""; reply("Opening $name."); true } catch (_: Exception) { false } }
    fun openCommand(input: String): Boolean { val lower = input.lowercase(Locale.US).trim(); val match = Regex("^(open|launch|start|run|go to)\\s+(.+?)[.!?]?$", RegexOption.IGNORE_CASE).find(lower) ?: return false; val target = match.groupValues[2].trim(); return try { when { target == "settings" || target == "android settings" || target == "phone settings" -> { context.startActivity(Intent(Settings.ACTION_SETTINGS)); messages.add(ChatMessage(input,false)); message=""; reply("Opening Settings."); true }; target == "camera" || target == "my camera" || target == "the camera" -> { val i=Intent(MediaStore.ACTION_IMAGE_CAPTURE); if(i.resolveActivity(context.packageManager)==null)return false; context.startActivity(i); messages.add(ChatMessage(input,false)); message=""; reply("Opening the camera."); true }; target.contains("youtube") -> launchApp(input,"YouTube","com.google.android.youtube","https://www.youtube.com"); target.contains("chrome") -> launchApp(input,"Chrome","com.android.chrome","https://www.google.com"); target.contains("gmail") -> launchApp(input,"Gmail","com.google.android.gm","mailto:"); target.contains("maps") -> launchApp(input,"Maps","com.google.android.apps.maps","geo:0,0?q=maps"); target.contains("play store") || target.contains("google play") -> launchApp(input,"Play Store","com.android.vending","https://play.google.com/store"); else -> false } } catch (_: Exception) { false } }
    fun setTimer(input: String): Boolean { val m=Regex("^(set|start)(?:\\s+me)?\\s+(?:a\\s+)?timer(?:\\s+(?:for|of))?\\s+(\\d+)\\s*(second|seconds|minute|minutes|hour|hours)(?:\\s+from\\s+now)?[.!?]?$",RegexOption.IGNORE_CASE).find(input.trim())?:return false; val amount=m.groupValues[1].toLongOrNull()?:return false; if(amount<=0)return false; val unit=m.groupValues[2].lowercase(Locale.US); val seconds=when{unit.startsWith("second")->amount;unit.startsWith("minute")->amount*60;else->amount*3600}; if(seconds>Int.MAX_VALUE)return false; return try{context.startActivity(Intent(AlarmClock.ACTION_SET_TIMER).apply{putExtra(AlarmClock.EXTRA_LENGTH,seconds.toInt());putExtra(AlarmClock.EXTRA_MESSAGE,"Leau timer");putExtra(AlarmClock.EXTRA_SKIP_UI,false)});messages.add(ChatMessage(input,false));message="";reply("Setting a ${amount} ${unit} timer.");true}catch(_:Exception){Toast.makeText(context,"I couldn't open the timer on this phone.",Toast.LENGTH_SHORT).show();false} }
    fun setAlarm(input:String):Boolean{val m=Regex("^(set|create|make) (?:an )?alarm (?:for )?(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?[.!?]?$",RegexOption.IGNORE_CASE).find(input.trim())?:return false;var hour=m.groupValues[2].toIntOrNull()?:return false;val minute=m.groupValues[3].toIntOrNull()?:0;val ap=m.groupValues[4].lowercase(Locale.US);if(minute !in 0..59)return false;if(ap=="am"||ap=="pm"){if(hour !in 1..12)return false;if(ap=="am"&&hour==12)hour=0;if(ap=="pm"&&hour!=12)hour+=12}else if(hour !in 0..23)return false;return try{context.startActivity(Intent(AlarmClock.ACTION_SET_ALARM).apply{putExtra(AlarmClock.EXTRA_HOUR,hour);putExtra(AlarmClock.EXTRA_MINUTES,minute);putExtra(AlarmClock.EXTRA_MESSAGE,"Leau alarm");putExtra(AlarmClock.EXTRA_SKIP_UI,false)});messages.add(ChatMessage(input,false));message="";val dh=if(hour==0)12 else if(hour>12)hour-12 else hour;reply(String.format(Locale.US,"Setting your alarm for %d:%02d %s.",dh,minute,if(hour<12)"AM" else "PM"));true}catch(_:Exception){false}}
    fun sendText(value:String){val text=value.trim();if(text.isEmpty()||isThinking)return;if(openCommand(text)||setTimer(text)||setAlarm(text))return;val lower=text.lowercase(Locale.US);if(lower.matches(Regex("^what do you remember( about me)?[.!?]?$"))||lower.matches(Regex("^what do you know about me[.!?]?$"))||lower.matches(Regex("^show my memories[.!?]?$"))){messages.add(ChatMessage(text,false));message="";val memories=LeauMemory.getMemories(context);reply(if(memories.isEmpty())"I don't have any saved memories about you yet." else "Here's what I remember about you:\n"+memories.joinToString("\n"){"• $it"});return};if(lower.matches(Regex("^forget everything( you remember)?( about me)?[.!?]?$"))||lower.matches(Regex("^forget all( my)? memories[.!?]?$"))||lower.matches(Regex("^clear (all )?(my )?memories[.!?]?$"))){messages.add(ChatMessage(text,false));message="";LeauMemory.clearMemories(context);reply("Done. I've forgotten all of the memories I had saved about you.");return};Regex("(?i)^forget(?:\\s+that)?\\s+(.+?)[.!?]?$").find(text)?.let{messages.add(ChatMessage(text,false));message="";val removed=LeauMemory.forgetMemory(context,it.groupValues[1].trim());reply(if(removed)"Done. I've forgotten that memory." else "I couldn't find a saved memory matching that.");return};LeauMemory.rememberFromUserMessage(context,text);val history=buildHistory();messages.add(ChatMessage(text,false));message="";isThinking=true;LeauApi.sendMessage(text,history){result->Handler(Looper.getMainLooper()).post{result.onSuccess{r->messages.add(ChatMessage(r,true));onLeauReply(r)}.onFailure{e->messages.add(ChatMessage("I couldn't reach my AI brain. ${e.message?:"Unknown connection error"}",true))};isThinking=false}}}
    registerVoiceMessageSender{sendText(it)}
    val infinite=rememberInfiniteTransition(label="leauPulse");val pulse by infinite.animateFloat(1f,1.045f,infiniteRepeatable(tween(1700,easing=FastOutSlowInEasing),RepeatMode.Reverse),label="pulse");val floatY by infinite.animateFloat(-5f,5f,infiniteRepeatable(tween(2300,easing=FastOutSlowInEasing),RepeatMode.Reverse),label="float")
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF102E2A),bg,bg)))){Column(Modifier.fillMaxSize().padding(horizontal=18.dp).navigationBarsPadding()){Row(Modifier.fillMaxWidth().padding(top=18.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("LEAU",color=Color.White,fontSize=27.sp,fontWeight=FontWeight.SemiBold,letterSpacing=2.sp);Text("PERSONAL INTELLIGENCE",color=mint,fontSize=9.sp,letterSpacing=2.sp)};IconButton(onClick={runCatching{context.startActivity(Intent(context,Class.forName("com.liv.ol1viapa.LeauHubActivity")))}}){Icon(Icons.Default.Settings,"LEAU settings",tint=Color(0xFF9BB9B1))};IconButton(onClick={}){Icon(Icons.Default.MoreVert,"More",tint=Color(0xFF9BB9B1))}}
        if(messages.isEmpty()){Column(Modifier.fillMaxWidth().weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){Box(Modifier.size(270.dp).scale(if(isListening||isSpeaking)pulse else 1f),contentAlignment=Alignment.Center){Box(Modifier.fillMaxSize().clip(CircleShape).background(Brush.radialGradient(listOf(Color(0x554DFFF0),Color.Transparent))));Image(painterResource(R.drawable.leau_eyes),"LEAU",Modifier.size(210.dp).offset(y=floatY.dp),contentScale=ContentScale.Fit)};Spacer(Modifier.height(12.dp));Text(when{isListening->"LISTENING";isThinking->"THINKING";isSpeaking->"SPEAKING";else->"READY"},color=mint,fontSize=10.sp,letterSpacing=3.sp,fontWeight=FontWeight.Medium);Spacer(Modifier.height(10.dp));Text(if(isListening)"I'm listening" else if(isSpeaking)"I'm speaking" else "What can I do for you?",color=Color.White,fontSize=25.sp,fontWeight=FontWeight.Light);Spacer(Modifier.height(24.dp));Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)){QuickPrompt("Set a timer",mint){sendText("set a timer for 25 minutes")};QuickPrompt("Open camera",cyan){sendText("open camera")};QuickPrompt("What do you remember?",purple){sendText("what do you remember")}}}}
        else{LazyColumn(Modifier.weight(1f).fillMaxWidth().padding(top=10.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){item{Text("CONVERSATION",color=Color(0xFF65837B),fontSize=9.sp,letterSpacing=2.sp,modifier=Modifier.padding(start=4.dp,bottom=4.dp))};itemsIndexed(messages){_,m->Row(Modifier.fillMaxWidth(),horizontalArrangement=if(m.fromLeau)Arrangement.Start else Arrangement.End){Box(Modifier.clip(RoundedCornerShape(20.dp)).background(if(m.fromLeau)panel else Color(0xFF173C35)).border(1.dp,if(m.fromLeau)line else Color(0xFF2C665A),RoundedCornerShape(20.dp)).padding(horizontal=15.dp,vertical=11.dp)){Text(m.text,color=if(m.fromLeau)Color(0xFFDDF8EF)else Color.White,fontSize=15.sp)}}};if(isThinking)item{Text("LEAU is thinking…",color=mint,fontSize=12.sp,modifier=Modifier.padding(6.dp))}}}
        if(messages.isEmpty()&&!isListening&&!isSpeaking)Spacer(Modifier.height(8.dp));GlassInput(message,{message=it},{sendText(message)},onMicClick,isThinking||isListening||isSpeaking,mint);Spacer(Modifier.height(12.dp)}}}
}

@Composable private fun QuickPrompt(text:String,accent:Color,onClick:()->Unit){Surface(onClick=onClick,color=Color(0x66132725),shape=RoundedCornerShape(18.dp),border=BorderStroke(1.dp,Color(0x553A6D63))){Text(text,color=accent,fontSize=12.sp,modifier=Modifier.padding(horizontal=14.dp,vertical=9.dp))}}
@Composable private fun GlassInput(value:String,onValueChange:(String)->Unit,onSend:()->Unit,onMic:()->Unit,disabled:Boolean,accent:Color){Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color(0xCC0B1718)).border(1.dp,Color(0xFF24473F),RoundedCornerShape(28.dp)).padding(5.dp),verticalAlignment=Alignment.CenterVertically){OutlinedTextField(value=value,onValueChange=onValueChange,modifier=Modifier.weight(1f),placeholder={Text("Ask LEAU anything…",color=Color(0xFF69857E))},singleLine=true,enabled=!disabled,shape=RoundedCornerShape(22.dp),colors=OutlinedTextFieldDefaults.colors(focusedBorderColor=Color.Transparent,unfocusedBorderColor=Color.Transparent,focusedContainerColor=Color.Transparent,unfocusedContainerColor=Color.Transparent,cursorColor=accent,focusedTextColor=Color.White,unfocusedTextColor=Color.White));IconButton(onClick=onMic,enabled=!disabled,modifier=Modifier.size(48.dp).clip(CircleShape).background(Color(0xFF153D35))){Icon(Icons.Default.Mic,"Talk to LEAU",tint=accent)};IconButton(onClick=onSend,enabled=!disabled&&value.isNotBlank(),modifier=Modifier.size(48.dp).clip(CircleShape).background(if(value.isNotBlank())Color(0xFF69F0C4)else Color(0x331F4940))){Icon(Icons.Default.ArrowUpward,"Send",tint=if(value.isNotBlank())Color(0xFF06100D)else Color(0xFF628078))}}}
