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

    fun getConversations(context: Context): List<Conversation> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val messageArray = obj.optJSONArray("messages") ?: JSONArray()
                    val messages = buildList {
                        for (j in 0 until messageArray.length()) {
                            val m = messageArray.optJSONObject(j) ?: continue
                            val text = m.optString("text").trim()
                            if (text.isNotEmpty()) add(VaultMessage(text, m.optBoolean("fromLeau")))
                        }
                    }
                    val title = obj.optString("title").trim().ifEmpty { "Conversation" }
                    add(Conversation(title, messages, obj.optLong("updatedAt", 0L)))
                }
            }.sortedByDescending { it.updatedAt }
        }.getOrDefault(emptyList())
    }

    fun saveConversation(context: Context, messages: List<VaultMessage>) {
        val clean = messages.map { it.copy(text = it.text.trim()) }.filter { it.text.isNotEmpty() }
        if (clean.isEmpty()) return
        val firstUser = clean.firstOrNull { !it.fromLeau }?.text ?: clean.first().text
        val title = firstUser.take(48).let { if (firstUser.length > 48) "$it…" else it }
        val conversation = JSONObject()
            .put("title", title)
            .put("updatedAt", System.currentTimeMillis())
            .put("messages", JSONArray().apply {
                clean.forEach { put(JSONObject().put("text", it.text).put("fromLeau", it.fromLeau)) }
            })
        val existing = getConversations(context).toMutableList()
        existing.add(0, Conversation(title, clean, System.currentTimeMillis()))
        val array = JSONArray()
        existing.take(50).forEach { c ->
            array.put(JSONObject().put("title", c.title).put("updatedAt", c.updatedAt).put("messages", JSONArray().apply {
                c.messages.forEach { put(JSONObject().put("text", it.text).put("fromLeau", it.fromLeau)) }
            }))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, array.toString()).apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }
}
