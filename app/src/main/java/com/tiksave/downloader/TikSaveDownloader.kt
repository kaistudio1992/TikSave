package com.tiksave.downloader

import android.content.Context
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.ffmpeg.FFmpeg
import java.io.File
import java.util.concurrent.Executors

object TikSaveDownloader {
    private val executor = Executors.newSingleThreadExecutor()

    fun download(
        context: Context,
        url: String,
        onResult: (Result<File>) -> Unit
    ) {
        executor.execute {
            try {
                val app = context.applicationContext
                val cleanUrl = url.trim().lineSequence()
                    .firstOrNull { it.startsWith("https://") || it.startsWith("http://") }
                    ?: throw IllegalArgumentException("Pautan video tidak sah.")

                val outputDir = File(app.getExternalFilesDir(null), "TikSave")
                if (!outputDir.exists() && !outputDir.mkdirs()) {
                    throw IllegalStateException("Tidak dapat menyediakan folder muat turun.")
                }

                YoutubeDL.getInstance().init(app)
                FFmpeg.getInstance().init(app)

                val outputTemplate = File(outputDir, "%(title).150B-%(id)s.%(ext)s")
                    .absolutePath

                val request = YoutubeDLRequest(cleanUrl)
                    .addOption("--no-playlist")
                    .addOption("-f", "bestvideo*+bestaudio/best")
                    .addOption("--merge-output-format", "mp4")
                    .addOption("-o", outputTemplate)
                    .addOption("--no-warnings")
                    .addOption("--retries", "3")
                    .addOption("--socket-timeout", "30")

                YoutubeDL.getInstance().execute(request)

                val resultFile = outputDir.listFiles()
                    ?.filter { it.isFile && it.length() > 0L }
                    ?.maxByOrNull { it.lastModified() }
                    ?: throw IllegalStateException("Enjin selesai tetapi fail video tidak ditemui.")

                onResult(Result.success(resultFile))
            } catch (error: Throwable) {
                onResult(Result.failure(error))
            }
        }
    }
}
