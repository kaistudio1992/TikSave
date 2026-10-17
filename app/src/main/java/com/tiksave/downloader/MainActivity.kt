package com.tiksave.downloader

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.webkit.WebViewClient
import android.webkit.WebView
import android.webkit.WebChromeClient
import android.widget.EditText
import android.view.inputmethod.EditorInfo
import android.view.ViewGroup
import android.graphics.Color
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var status: TextView
    private var receivedUrl: String = ""
    private var internalWebView: WebView? = null


    private fun openInternalBrowser() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            setBackgroundColor(Color.rgb(17, 19, 27))
        }

        val address = EditText(this).apply {
            hint = "Masukkan URL video atau laman web"
            setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY)
            setSingleLine(true)
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_URI
            imeOptions = EditorInfo.IME_ACTION_GO
            setText(receivedUrl)
        }

        val controls = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val go = Button(this).apply { text = "GO" }
        val download = Button(this).apply { text = "DOWNLOAD" }
        val back = Button(this).apply { text = "KEMBALI" }

        val web = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            webViewClient = WebViewClient()
            webChromeClient = WebChromeClient()
        }
        internalWebView = web

        fun loadAddress() {
            var url = address.text.toString().trim()
            if (url.isBlank()) {
                Toast.makeText(this, "Masukkan URL dahulu", Toast.LENGTH_SHORT).show()
                return
            }
            if (!url.startsWith("https://") && !url.startsWith("http://")) {
                url = "https://$url"
            }
            address.setText(url)
            web.loadUrl(url)
        }

        go.setOnClickListener { loadAddress() }
        address.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                loadAddress()
                true
            } else false
        }

        download.setOnClickListener {
            val url = web.url ?: address.text.toString().trim()
            if (url.startsWith("https://") || url.startsWith("http://")) {
                receivedUrl = url
                getSharedPreferences("tiksave", MODE_PRIVATE)
                    .edit()
                    .putString("video_url", url)
                    .apply()
                Toast.makeText(this, "Memulakan muat turun...", Toast.LENGTH_SHORT).show()
                TikSaveDownloader.download(this, url) { result ->
                    runOnUiThread {
                        val message = result.fold(
                            { "Berjaya: ${it.name}" },
                            { "Gagal: ${it.message ?: it.javaClass.simpleName}" }
                        )
                        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Buka halaman dahulu", Toast.LENGTH_SHORT).show()
            }
        }

        back.setOnClickListener {
            internalWebView = null
            web.destroy()
            recreate()
        }

        controls.addView(go, LinearLayout.LayoutParams(0, -2, 1f))
        controls.addView(download, LinearLayout.LayoutParams(0, -2, 1.4f))
        controls.addView(back, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(address, LinearLayout.LayoutParams(-1, -2))
        root.addView(controls, LinearLayout.LayoutParams(-1, -2))
        root.addView(web, LinearLayout.LayoutParams(-1, 0, 1f))

        setContentView(root)
        if (receivedUrl.isNotBlank()) loadAddress()
        else web.loadUrl("https://www.google.com")
    }

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

        val browserButton = Button(this).apply {
            text = "BUKA BROWSER"
            setOnClickListener { openInternalBrowser() }
        }
        root.addView(browserButton)
        val openLink = Button(this).apply {
            text = "BUKA PAUTAN VIDEO"
            setOnClickListener {
                if (receivedUrl.isBlank()) {
                    Toast.makeText(this@MainActivity, "Kongsi pautan TikTok ke TikSave dahulu.", Toast.LENGTH_SHORT).show()
                } else {
                    val browser = Intent(Intent.ACTION_VIEW, Uri.parse(receivedUrl))
                    startActivity(browser)
                }
            }
        }
        root.addView(openLink)
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
                receivedUrl = shared
                getSharedPreferences("tiksave", MODE_PRIVATE)
                    .edit()
                    .putString("video_url", shared)
                    .apply()
                status.text = "Pautan diterima:\n$shared\n\nTekan BUKA PAUTAN untuk melihat video."
            }
        }
    }
}
