package com.tiksave.downloader

import android.app.Activity
import android.content.ClipboardManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast

class ClipboardCaptureActivity : Activity() {
    private var handled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (!hasFocus || handled) return
        handled = true

        val clipboard = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val text = runCatching {
            val clip = clipboard.primaryClip
            if (clipboard.hasPrimaryClip() && clip != null) {
                (0 until clip.itemCount).mapNotNull { i ->
                    runCatching { clip.getItemAt(i)?.coerceToText(this)?.toString() }.getOrNull()
                }.firstOrNull { !it.isNullOrBlank() }.orEmpty()
            } else ""
        }.getOrDefault("")

        val url = text.lineSequence().map { it.trim() }.firstOrNull {
            (it.startsWith("https://") || it.startsWith("http://")) &&
                it.contains("tiktok.com", ignoreCase = true)
        }

        if (url.isNullOrBlank()) {
            Toast.makeText(applicationContext, "Pautan TikTok tidak dapat dibaca. Cuba Copy link semula.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        TikSaveDownloader.download(applicationContext, url) { result ->
            Handler(Looper.getMainLooper()).post {
                val message = result.fold(
                    { "Muat turun selesai: ${it.name}" },
                    { "Gagal: ${it.message ?: it.javaClass.simpleName}" }
                )
                Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
            }
        }
        finish()
    }
}
