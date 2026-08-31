package com.liv.ol1viapa

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object LeauMemory {
    private const val PREFS_NAME = "ol1via_memory"
    private const val MEMORY_KEY = "memories"
    private const val MAX_MEMORIES = 50

    private fun preferences(context: Context) = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    fun getMemories(context: Context): List<String> {
        val stored = preferences(context).getString(MEMORY_KEY, "[]") ?: "[]"
        return try { val array = JSONArray(stored); buildList { for (i in 0 until array.length()) { val memory = array.optString(i).trim(); if (memory.isNotEmpty()) add(memory) } } } catch (_: Exception) { emptyList() }
    }
    fun saveMemory(context: Context, memory: String) {
        val cleaned = memory.trim(); if (cleaned.isEmpty()) return
        val memories = getMemories(context).toMutableList(); if (memories.any { it.equals(cleaned, ignoreCase = true) }) return
        memories.add(cleaned); val array = JSONArray(); memories.takeLast(MAX_MEMORIES).forEach { array.put(it) }
        preferences(context).edit().putString(MEMORY_KEY, array.toString()).apply()
    }
    fun rememberFromUserMessage(context: Context, message: String) {
        val text = message.trim(); if (text.isEmpty()) return
        Regex("(?i)^remember(?:\\s+that)?\\s+(.+?)[.!?]?$").find(text)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { saveMemory(context, it) }
        Regex("(?i)\\bmy\\s+favorite\\s+(.+?)\\s+is\\s+(.+?)(?:[.!?]|$)").find(text)?.let { m -> saveMemory(context, "The user's favorite ${m.groupValues[1].trim()} is ${m.groupValues[2].trim()}.") }
        Regex("(?i)\\bmy\\s+name\\s+is\\s+(.+?)(?:[.!?]|$)").find(text)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { saveMemory(context, "The user's name is $it.") }
        Regex("(?i)\\bcall\\s+me\\s+(.+?)(?:[.!?]|$)").find(text)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }?.let { saveMemory(context, "The user prefers to be called $it.") }
    }
    fun forgetMemory(context: Context, request: String): Boolean {
        val target = request.trim().replaceFirst(Regex("(?i)^that\\s+"), "").replaceFirst(Regex("(?i)^my\\s+"), "").trim().trimEnd('.', '!', '?')
        if (target.isEmpty()) return false
        val memories = getMemories(context); val targetNormalized = target.lowercase().replace(Regex("\\s+"), " ").trim()
        val remaining = memories.filterNot { memory ->
            val normalizedMemory = memory.lowercase().replace(Regex("\\s+"), " ").trim()
            normalizedMemory == targetNormalized || normalizedMemory.contains(targetNormalized) || targetNormalized.contains(normalizedMemory) || matchesCommonMemoryPattern(normalizedMemory, targetNormalized)
        }
        if (remaining.size == memories.size) return false
        val array = JSONArray(); remaining.forEach { array.put(it) }; preferences(context).edit().putString(MEMORY_KEY, array.toString()).apply(); return true
    }
    private fun matchesCommonMemoryPattern(memory: String, target: String): Boolean {
        Regex("^the user's favorite (.+) is (.+)\\.?$").find(memory)?.let { m -> return target == "favorite ${m.groupValues[1]} is ${m.groupValues[2]}" || target == "my favorite ${m.groupValues[1]} is ${m.groupValues[2]}" }
        Regex("^the user's name is (.+)\\.?$").find(memory)?.let { m -> return target == "name is ${m.groupValues[1]}" || target == "my name is ${m.groupValues[1]}" }
        Regex("^the user prefers to be called (.+)\\.?$").find(memory)?.let { m -> return target == "call me ${m.groupValues[1]}" || target == "prefers to be called ${m.groupValues[1]}" }
        return false
    }
    fun clearMemories(context: Context) { preferences(context).edit().remove(MEMORY_KEY).apply() }
    fun buildMemoryContext(context: Context): String { val memories = getMemories(context); if (memories.isEmpty()) return ""; return buildString { appendLine("Important things I remember about the user:"); memories.forEach { appendLine("- $it") } } }
    fun buildMemoryHistoryMessage(context: Context): JSONObject? { val memoryContext = buildMemoryContext(context); if (memoryContext.isBlank()) return null; return JSONObject().put("role", "user").put("parts", JSONArray().put(JSONObject().put("text", memoryContext))) }
}
