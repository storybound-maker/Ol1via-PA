package com.liv.ol1viapa

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Lightweight local conversation history for the LEAU Chat Vault. */
object LeauChatVault {
    data class VaultMessage(val text: String, val fromLeau: Boolean)
    data class Conversation(val title: String, val messages: List<VaultMessage>, val updatedAt: Long)

    private const val PREFS = "leau_chat_vault"
    private const val KEY = "conversations"
    private const val CURRENT_KEY = "current_conversation"

    fun getConversations(context: Context): List<Conversation> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    add(parseConversation(obj))
                }
            }.sortedByDescending { it.updatedAt }
        }.getOrDefault(emptyList())
    }

    fun getCurrentConversation(context: Context): Conversation? {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CURRENT_KEY, null) ?: return null
        return runCatching { parseConversation(JSONObject(raw)) }.getOrNull()
    }

    fun saveConversation(context: Context, messages: List<VaultMessage>) {
        saveCurrentConversation(context, messages)
    }

    fun saveCurrentConversation(context: Context, messages: List<VaultMessage>) {
        val clean = messages.map { it.copy(text = it.text.trim()) }.filter { it.text.isNotEmpty() }
        if (clean.isEmpty()) return
        val firstUser = clean.firstOrNull { !it.fromLeau }?.text ?: clean.first().text
        val title = firstUser.take(48).let { if (firstUser.length > 48) "$it…" else it }
        val updatedAt = System.currentTimeMillis()
        val conversation = JSONObject().put("title", title).put("updatedAt", updatedAt).put("messages", JSONArray().apply {
            clean.forEach { put(JSONObject().put("text", it.text).put("fromLeau", it.fromLeau)) }
        })
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.edit().putString(CURRENT_KEY, conversation.toString()).apply()

        val existing = getConversations(context).toMutableList()
        existing.removeAll { it.title == title }
        existing.add(0, Conversation(title, clean, updatedAt))
        val array = JSONArray()
        existing.take(50).forEach { c ->
            array.put(JSONObject().put("title", c.title).put("updatedAt", c.updatedAt).put("messages", JSONArray().apply {
                c.messages.forEach { put(JSONObject().put("text", it.text).put("fromLeau", it.fromLeau)) }
            }))
        }
        prefs.edit().putString(KEY, array.toString()).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).remove(CURRENT_KEY).apply()
    }

    private fun parseConversation(obj: JSONObject): Conversation {
        val messageArray = obj.optJSONArray("messages") ?: JSONArray()
        val messages = buildList {
            for (j in 0 until messageArray.length()) {
                val m = messageArray.optJSONObject(j) ?: continue
                val text = m.optString("text").trim()
                if (text.isNotEmpty()) add(VaultMessage(text, m.optBoolean("fromLeau")))
            }
        }
        return Conversation(
            title = obj.optString("title").trim().ifEmpty { "Conversation" },
            messages = messages,
            updatedAt = obj.optLong("updatedAt", 0L)
        )
    }
}
