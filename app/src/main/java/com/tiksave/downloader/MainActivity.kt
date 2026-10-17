package com.tiksave.downloader

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(36, 48, 36, 36)
            setBackgroundColor(0xFF11131B.toInt())
        }
        fun label(text: String, size: Float, color: Int): TextView =
            TextView(this).apply {
                this.text = text
                textSize = size
                setTextColor(color)
                gravity = Gravity.CENTER
                setPadding(0, 12, 0, 12)
            }
        root.addView(label("TikSave", 30f, 0xFFFFFFFF.toInt()))
        root.addView(label("Floating TikTok downloader", 16f, 0xFFB7C0D0.toInt()))
        status = label("Belum ada pautan diterima. Buka TikTok dan gunakan Share → TikSave.", 15f, 0xFFFFFFFF.toInt())
        root.addView(status)
        val overlay = Button(this).apply {
            text = "AKTIFKAN FLOATING BUTTON"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")))
                } else {
                    startService(Intent(this@MainActivity, FloatingService::class.java))
                    Toast.makeText(this@MainActivity, "Floating button diaktifkan", Toast.LENGTH_SHORT).show()
                }
            }
        }
        root.addView(overlay)
        val stop = Button(this).apply {
            text = "MATIKAN FLOATING BUTTON"
            setOnClickListener { stopService(Intent(this@MainActivity, FloatingService::class.java)) }
        }
        root.addView(stop)
        root.addView(label(
            "Penting: floating button sahaja tidak boleh membaca fail video dalaman TikTok. Versi ini menerima pautan melalui menu Share. Muat turun tanpa watermark hanya boleh dibuat jika sumber video tanpa watermark tersedia dan akses dibenarkan.",
            13f, 0xFFFFD28A.toInt()
        ))
        setContentView(root)
        handleIncoming(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncoming(intent)
    }

    private fun handleIncoming(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND) {
            val shared = intent.getStringExtra(Intent.EXTRA_TEXT)?.trim().orEmpty()
            if (shared.isNotBlank()) {
                status.text = "Pautan diterima:\n$shared\n\nLangkah seterusnya: sambungkan resolver video yang sah."
            }
        }
    }
}
