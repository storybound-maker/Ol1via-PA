package com.l1vo.ol1via.pa

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException

object Ol1viaApi {
    private const val WORKER_URL = "https://ol1via-ai.storybound622.workers.dev/"
    private val client = OkHttpClient()
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    fun sendMessage(message: String, callback: (Result<String>) -> Unit) {
        Thread {
            try {
                val payload = JSONObject().put("message", message).toString()
                val request = Request.Builder()
                    .url(WORKER_URL)
                    .post(payload.toRequestBody(jsonMediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        throw IOException("AI service returned HTTP ${response.code}")
                    }

                    val reply = JSONObject(body).optString("reply")
                    if (reply.isBlank()) {
                        throw IOException("AI service returned an empty response")
                    }
                    callback(Result.success(reply))
                }
            } catch (e: Exception) {
                callback(Result.failure(e))
            }
        }.start()
    }
}
