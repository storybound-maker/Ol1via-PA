package com.liv.ol1viapa

import android.Manifest
import android.app.Activity
import android.content.ContentUris
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.util.Locale

class LeauCallActivity : Activity() {
    companion object {
        const val EXTRA_TARGET = "call_target"
        private const val REQUEST_PERMISSIONS = 8117
    }

    private var target: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        target = intent.getStringExtra(EXTRA_TARGET).orEmpty().trim()
        if (target.isBlank()) {
            finish()
            return
        }

        val needed = mutableListOf<String>()
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            needed += Manifest.permission.CALL_PHONE
        }
        if (!looksLikePhoneNumber(target) && checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            needed += Manifest.permission.READ_CONTACTS
        }

        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), REQUEST_PERMISSIONS)
        } else {
            placeCall()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_PERMISSIONS) return
        if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            placeCall()
        } else {
            // If direct calling is not authorized, safely fall back to the dialer.
            placeCall(useDialerFallback = true)
        }
    }

    private fun placeCall(useDialerFallback: Boolean = false) {
        val number = resolveNumber(target)
        if (number.isNullOrBlank()) {
            Toast.makeText(this, "I couldn't find that contact.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val uri = Uri.parse("tel:${Uri.encode(number)}")
        val action = if (!useDialerFallback && checkSelfPermission(Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            Intent.ACTION_CALL
        } else {
            Intent.ACTION_DIAL
        }

        runCatching {
            startActivity(Intent(action, uri))
        }.onFailure {
            startActivity(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
        }
        finish()
    }

    private fun resolveNumber(value: String): String? {
        if (looksLikePhoneNumber(value)) {
            return value.replace(Regex("[^0-9+*#]"), "")
        }
        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) return null

        val requested = normalize(value)
        val resolver = contentResolver
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        var bestNumber: String? = null
        var bestScore = 0.0
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE NOCASE ASC"
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val name = if (nameIndex >= 0) cursor.getString(nameIndex).orEmpty() else ""
                val number = if (numberIndex >= 0) cursor.getString(numberIndex).orEmpty() else ""
                val normalizedName = normalize(name)
                val score = when {
                    normalizedName == requested -> 1.0
                    normalizedName.startsWith(requested) -> 0.96
                    normalizedName.contains(requested) -> 0.90
                    else -> characterSimilarity(normalizedName, requested)
                }
                if (score > bestScore) {
                    bestScore = score
                    bestNumber = number
                }
            }
        }
        return if (bestScore >= 0.65) bestNumber else null
    }

    private fun looksLikePhoneNumber(value: String): Boolean {
        val digits = value.count { it.isDigit() }
        return digits >= 7 && value.count { it.isLetter() } <= 1
    }

    private fun normalize(value: String): String = value
        .lowercase(Locale.US)
        .replace("’", "'")
        .replace(Regex("[^a-z0-9]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun characterSimilarity(a: String, b: String): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in a.indices) {
            current[0] = i + 1
            for (j in b.indices) {
                val cost = if (a[i] == b[j]) 0 else 1
                current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, previous[j] + cost)
            }
            val swap = previous
            for (j in current.indices) swap[j] = current[j]
            current = swap
        }
        return 1.0 - previous[b.length].toDouble() / maxOf(a.length, b.length)
    }
}
