package com.l1vo.ol1via.pa

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object Ol1viaMemory {
    private const val PREFS_NAME = "ol1via_memory"
    private const val MEMORY_KEY = "memories"
    private const val MAX_MEMORIES = 50

    private fun preferences(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getMemories(context: Context): List<String> {
        val stored = preferences(context).getString(MEMORY_KEY, "[]") ?: "[]"

        return try {
            val array = JSONArray(stored)
            buildList {
                for (i in 0 until array.length()) {
                    val memory = array.optString(i).trim()
                    if (memory.isNotEmpty()) add(memory)
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun saveMemory(context: Context, memory: String) {
        val cleaned = memory.trim()
        if (cleaned.isEmpty()) return

        val memories = getMemories(context).toMutableList()
        if (memories.any { it.equals(cleaned, ignoreCase = true) }) return

        memories.add(cleaned)

        val array = JSONArray()
        memories.takeLast(MAX_MEMORIES).forEach { array.put(it) }

        preferences(context).edit().putString(MEMORY_KEY, array.toString()).apply()
    }

    fun rememberFromUserMessage(context: Context, message: String) {
        val text = message.trim()
        if (text.isEmpty()) return

        val explicit = Regex(
            "(?i)^remember(?:\\s+that)?\\s+(.+?)[.!?]?$"
        ).find(text)
        if (explicit != null) {
            val value = explicit.groupValues[1].trim()
            if (value.isNotEmpty()) saveMemory(context, value)
        }

        val favorite = Regex(
            "(?i)\\bmy\\s+favorite\\s+(.+?)\\s+is\\s+(.+?)(?:[.!?]|$)"
        ).find(text)
        if (favorite != null) {
            val subject = favorite.groupValues[1].trim()
            val value = favorite.groupValues[2].trim()
            if (subject.isNotEmpty() && value.isNotEmpty()) {
                saveMemory(context, "The user's favorite $subject is $value.")
            }
        }

        val name = Regex(
            "(?i)\\bmy\\s+name\\s+is\\s+(.+?)(?:[.!?]|$)"
        ).find(text)
        if (name != null) {
            val value = name.groupValues[1].trim()
            if (value.isNotEmpty()) {
                saveMemory(context, "The user's name is $value.")
            }
        }

        val callMe = Regex(
            "(?i)\\bcall\\s+me\\s+(.+?)(?:[.!?]|$)"
        ).find(text)
        if (callMe != null) {
            val value = callMe.groupValues[1].trim()
            if (value.isNotEmpty()) {
                saveMemory(context, "The user prefers to be called $value.")
            }
        }
    }

    fun forgetMemory(context: Context, request: String): Boolean {
        var target = request.trim()
            .replaceFirst(Regex("(?i)^that\\s+"), "")
            .replaceFirst(Regex("(?i)^my\\s+"), "")
            .trim()
            .trimEnd('.', '!', '?')

        if (target.isEmpty()) return false

        val memories = getMemories(context)
        val targetNormalized = target.lowercase().replace(Regex("\\s+"), " ").trim()

        val remaining = memories.filterNot { memory ->
            val normalizedMemory = memory
                .lowercase()
                .replace(Regex("\\s+"), " ")
                .trim()

            normalizedMemory == targetNormalized ||
                normalizedMemory.contains(targetNormalized) ||
                targetNormalized.contains(normalizedMemory) ||
                matchesCommonMemoryPattern(normalizedMemory, targetNormalized)
        }

        if (remaining.size == memories.size) return false

        val array = JSONArray()
        remaining.forEach { array.put(it) }
        preferences(context).edit().putString(MEMORY_KEY, array.toString()).apply()
        return true
    }

    private fun matchesCommonMemoryPattern(memory: String, target: String): Boolean {
        val favorite = Regex("^the user's favorite (.+) is (.+)\\.?$").find(memory)
        if (favorite != null) {
            val subject = favorite.groupValues[1]
            val value = favorite.groupValues[2]
            val shortForm = "favorite $subject is $value"
            val userForm = "my favorite $subject is $value"
            return target == shortForm || target == userForm
        }

        val name = Regex("^the user's name is (.+)\\.?$").find(memory)
        if (name != null) {
            val value = name.groupValues[1]
            return target == "name is $value" || target == "my name is $value"
        }

        val callMe = Regex("^the user prefers to be called (.+)\\.?$").find(memory)
        if (callMe != null) {
            val value = callMe.groupValues[1]
            return target == "call me $value" || target == "prefers to be called $value"
        }

        return false
    }

    fun clearMemories(context: Context) {
        preferences(context).edit().remove(MEMORY_KEY).apply()
    }

    fun buildMemoryContext(context: Context): String {
        val memories = getMemories(context)
        if (memories.isEmpty()) return ""

        return buildString {
            appendLine("Important things I remember about the user:")
            memories.forEach { appendLine("- $it") }
        }
    }

    fun buildMemoryHistoryMessage(context: Context): JSONObject? {
        val memoryContext = buildMemoryContext(context)
        if (memoryContext.isBlank()) return null

        return JSONObject()
            .put("role", "user")
            .put(
                "parts",
                JSONArray().put(JSONObject().put("text", memoryContext))
            )
    }
}
