package com.liv.ol1viapa

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object LeauChatVault {
    private const val PREFS = "leau_chat_vault"
    private const val KEY = "conversations"

    data class Conversation(val id: Long, val title: String, val messages: List<ChatMessage>)

    fun saveConversation(context: Context, messages: List<ChatMessage>) {
        if (messages.isEmpty()) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val all = readJson(prefs.getString(KEY, "[]"))
        val firstUser = messages.firstOrNull { !it.fromLeau }?.text?.trim().orEmpty()
        val title = firstUser.ifBlank { "Leau conversation" }.take(48)
        val item = JSONObject().apply {
            put("id", System.currentTimeMillis())
            put("title", title)
            put("messages", JSONArray().apply {
                messages.takeLast(50).forEach { message ->
                    put(JSONObject().put("text", message.text).put("fromLeau", message.fromLeau))
                }
            })
        }
        all.put(item)
        while (all.length() > 50) all.remove(0)
        prefs.edit().putString(KEY, all.toString()).apply()
    }

    fun getConversations(context: Context): List<Conversation> {
        val all = readJson(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, "[]"))
        val result = mutableListOf<Conversation>()
        for (i in all.length() - 1 downTo 0) {
            val item = all.optJSONObject(i) ?: continue
            val list = mutableListOf<ChatMessage>()
            val jsonMessages = item.optJSONArray("messages") ?: JSONArray()
            for (j in 0 until jsonMessages.length()) {
                val m = jsonMessages.optJSONObject(j) ?: continue
                list.add(ChatMessage(m.optString("text"), m.optBoolean("fromLeau")))
            }
            result.add(Conversation(item.optLong("id"), item.optString("title", "Leau conversation"), list))
        }
        return result
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY).apply()
    }

    private fun readJson(raw: String?): JSONArray = runCatching { JSONArray(raw ?: "[]") }.getOrElse { JSONArray() }
}
