package com.liv.ol1viapa

import android.os.Handler
import android.os.Looper
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

object LeauApi {
    private const val WORKER_URL = "https://ol1via-ai.storybound622.workers.dev/"
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun sendMessage(message: String, history: List<JSONObject> = emptyList(), callback: (Result<String>) -> Unit) {
        Thread {
            try {
                val historyArray = JSONArray()
                history.forEach { historyArray.put(it) }
                val payload = JSONObject().put("message", message).put("history", historyArray).toString()
                val request = Request.Builder().url(WORKER_URL).post(payload.toRequestBody(jsonMediaType)).header("Accept", "application/json").build()
                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val serverError = runCatching { JSONObject(body).optString("error") }.getOrNull().orEmpty()
                        val detail = if (serverError.isNotBlank()) serverError else body.take(300)
                        throw IOException("AI service returned HTTP ${response.code}: $detail")
                    }
                    val reply = JSONObject(body).optString("reply")
                    if (reply.isBlank()) throw IOException("AI service returned no reply. Response: ${body.take(300)}")
                    postResult(callback, Result.success(reply))
                }
            } catch (e: Exception) { postResult(callback, Result.failure(e)) }
        }.start()
    }

    private fun postResult(callback: (Result<String>) -> Unit, result: Result<String>) { mainHandler.post { callback(result) } }
}
