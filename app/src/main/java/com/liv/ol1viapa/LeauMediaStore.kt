package com.liv.ol1viapa

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File

object LeauMediaStore {
    private fun copyIntoMediaStore(context: Context, uri: Uri, file: File, pendingColumn: String?): Boolean {
        return runCatching {
            val output = context.contentResolver.openOutputStream(uri) ?: return@runCatching false
            output.use { stream -> file.inputStream().use { input -> input.copyTo(stream) } }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && pendingColumn != null) {
                context.contentResolver.update(uri, ContentValues().apply { put(pendingColumn, 0) }, null, null)
            }
            file.delete()
            true
        }.getOrElse {
            runCatching { context.contentResolver.delete(uri, null, null) }
            false
        }
    }

    fun saveVideo(context: Context, file: File): Uri? {
        if (!file.exists() || file.length() <= 0L) return null
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.nameWithoutExtension + ".mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/Leau")
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return if (copyIntoMediaStore(context, uri, file, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Video.Media.IS_PENDING else null)) uri else null
    }

    fun saveAudio(context: Context, file: File): Uri? {
        if (!file.exists() || file.length() <= 0L) return null
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, file.nameWithoutExtension + ".m4a")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/Leau")
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
        }
        val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: return null
        return if (copyIntoMediaStore(context, uri, file, if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.Audio.Media.IS_PENDING else null)) uri else null
    }
}
