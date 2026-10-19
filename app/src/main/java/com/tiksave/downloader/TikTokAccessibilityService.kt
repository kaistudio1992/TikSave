package com.tiksave.downloader

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class TikTokAccessibilityService : AccessibilityService() {

    private val urlPattern = Regex(
        """https?://(?:www\.|vm\.|vt\.)?tiktok\.com/[^\s"'<>]+""",
        RegexOption.IGNORE_CASE
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName != "com.zhiliaoapp.musically" &&
            packageName != "com.ss.android.ugc.trill") return

        val root = rootInActiveWindow ?: return
        try {
            findTikTokUrl(root)
        } finally {
            root.recycle()
        }
    }

    private fun findTikTokUrl(node: AccessibilityNodeInfo) {
        val visibleText = listOf(
            node.text?.toString().orEmpty(),
            node.contentDescription?.toString().orEmpty()
        ).joinToString(" ")

        val match = urlPattern.find(visibleText)?.value
        if (match != null) {
            val cleanUrl = match.trimEnd('.', ',', ')', ']', '!')
            getSharedPreferences("tiksave", MODE_PRIVATE)
                .edit()
                .putString("video_url", cleanUrl)
                .putBoolean("video_url_detected", true)
                .apply()

            Log.d("TikSaveAutoDetect", "TikTok URL detected")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                findTikTokUrl(child)
            } finally {
                child.recycle()
            }
        }
    }

    override fun onInterrupt() = Unit
}
