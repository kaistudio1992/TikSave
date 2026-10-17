package com.tiksave.downloader

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.ffmpeg.FFmpeg
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.Executors

object TikSaveDownloader {
    private val executor = Executors.newSingleThreadExecutor()

    fun download(
        context: Context,
        url: String,
        onResult: (Result<File>) -> Unit
    ) {
        executor.execute {
            var stage = "Memulakan downloader"
            try {
                val app = context.applicationContext
                val cleanUrl = url.trim().lineSequence()
                    .firstOrNull { it.startsWith("https://") || it.startsWith("http://") }
                    ?: throw IllegalArgumentException("Pautan video tidak sah.")

                val outputDir = File(app.getExternalFilesDir(null), "TikSave")
                if (!outputDir.exists() && !outputDir.mkdirs()) {
                    throw IllegalStateException("Tidak dapat menyediakan folder sementara.")
                }

                stage = "Memulakan YoutubeDL"
                Log.i("TikSaveInit", stage)
                YoutubeDL.getInstance().init(app)

                stage = "Memulakan FFmpeg"
                Log.i("TikSaveInit", stage)
                FFmpeg.getInstance().init(app)

                stage = "Memuat turun video"
                Log.i("TikSaveInit", stage)

                val outputTemplate = File(outputDir, "%(title).150B-%(id)s.%(ext)s").absolutePath
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
                    ?.filter { it.isFile && it.length() > 0L && !it.name.endsWith(".part") }
                    ?.maxByOrNull { it.lastModified() }
                    ?: throw IllegalStateException("Enjin selesai tetapi fail video tidak ditemui.")

                stage = "Menyimpan video ke Gallery"
                val savedFile = saveToGallery(app, resultFile)
                Log.i("TikSaveInit", "Video disimpan ke Gallery: ${savedFile.absolutePath}")
                onResult(Result.success(savedFile))
            } catch (error: Throwable) {
                Log.e("TikSaveInit", "Gagal pada $stage: ${error.javaClass.name}: ${error.message}", error)
                onResult(
                    Result.failure(
                        IllegalStateException(
                            "$stage — ${error.javaClass.simpleName}: ${error.message ?: "tiada mesej"}",
                            error
                        )
                    )
                )
            }
        }
    }

    private fun saveToGallery(context: Context, source: File): File {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, source.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/TikSave")
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }

        val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values)
            ?: throw IOException("Android tidak dapat mencipta fail dalam Gallery.")

        try {
            resolver.openOutputStream(uri, "w").use { output ->
                if (output == null) throw IOException("Tidak dapat membuka fail Gallery.")
                FileInputStream(source).use { input -> input.copyTo(output) }
            }

            val completed = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            resolver.update(uri, completed, null, null)

            return File(
                "/storage/emulated/0/Movies/TikSave",
                source.name
            )
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }
}
