package com.tiksave.downloader

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast

class FloatingService : Service() {
    private lateinit var wm: WindowManager
    private var bubble: TextView? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(1001, makeNotification())
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val view = TextView(this).apply {
            text = "⬇ DOWNLOAD"
            textSize = 13f
            setTextColor(0xFFFFFFFF.toInt())
            setPadding(24, 18, 24, 18)
            setBackgroundColor(0xFF119B83.toInt())
            elevation = 12f
            isClickable = true
            isFocusable = false
            setOnClickListener {
                val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clipText = if (clipboard.hasPrimaryClip()) {
                    clipboard.primaryClip?.getItemAt(0)?.coerceToText(this@FloatingService)?.toString()?.trim().orEmpty()
                } else ""

                val clipboardUrl = clipText.lineSequence()
                    .map { it.trim() }
                    .firstOrNull {
                        (it.startsWith("https://") || it.startsWith("http://")) &&
                            it.contains("tiktok.com", ignoreCase = true)
                    }

                val savedUrl = getSharedPreferences("tiksave", MODE_PRIVATE)
                    .getString("video_url", null)
                    ?.trim()
                    ?.takeIf {
                        (it.startsWith("https://") || it.startsWith("http://")) &&
                            it.contains("tiktok.com", ignoreCase = true)
                    }

                val url = clipboardUrl ?: savedUrl
                val detected = !url.isNullOrBlank()

                if (!detected || url.isNullOrBlank()) {
                    Toast.makeText(
                        this@FloatingService,
                        "Pautan TikTok tiada dalam clipboard. Buka TikTok, tekan Share > Copy link, kemudian tekan DOWNLOAD.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }

                Toast.makeText(
                    this@FloatingService,
                    "Mula memuat turun video...",
                    Toast.LENGTH_SHORT
                ).show()

                TikSaveDownloader.download(this@FloatingService, url) { result ->
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        val message = result.fold(
                            { "Muat turun selesai: ${it.name}" },
                            { error ->
                                val detail = generateSequence(error as Throwable?) { it.cause }
                                    .map { "${it.javaClass.name}: ${it.message ?: "tiada mesej"}" }
                                    .joinToString("\n")
                                runCatching {
                                    openFileOutput("TikSave-error.txt", MODE_PRIVATE).use {
                                        it.write(detail.toByteArray(Charsets.UTF_8))
                                    }
                                }
                                runCatching {
                        val clipboard = getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(
                            android.content.ClipData.newPlainText("TikSave error", detail)
                        )
                    }
                    "GAGAL: Ralat penuh disalin ke clipboard. Paste di Termux."
                            }
                        )
                        Toast.makeText(
                            this@FloatingService, message, Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
        bubble = view
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 18
            y = 260
        }
        wm.addView(view, params)
    }

    private fun makeNotification(): Notification {
        val channelId = "tiksave_overlay"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "TikSave floating button", NotificationManager.IMPORTANCE_LOW)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
        }
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            Notification.Builder(this, channelId) else Notification.Builder(this)
        return builder.setContentTitle("TikSave aktif")
            .setContentText("Floating button tersedia. Tekan untuk membuka TikSave.")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .build()
    }

    override fun onDestroy() {
        bubble?.let { try { wm.removeView(it) } catch (_: Exception) {} }
        bubble = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
