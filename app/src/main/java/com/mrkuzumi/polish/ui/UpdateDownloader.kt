package com.mrkuzumi.polish.ui

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** 下载进度 */
data class DownloadProgress(val percent: Int, val done: Boolean)

/** APK 下载 + 安装工具 */
object UpdateDownloader {

    /**
     * 下载 APK 到缓存目录，回调进度（0..100）。
     * 返回下载成功的 File，失败返回 null。
     */
    suspend fun download(
        url: String,
        destDir: File,
        onProgress: (Int) -> Unit,
    ): File? = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000; readTimeout = 30_000
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/octet-stream")
            }
            val total = conn.contentLength.toLong()
            val dest = File(destDir, "update.apk")
            conn.inputStream.use { input ->
                dest.outputStream().use { output ->
                    val buf = ByteArray(8192)
                    var downloaded = 0L
                    var n: Int
                    while (input.read(buf).also { n = it } != -1) {
                        output.write(buf, 0, n)
                        downloaded += n
                        if (total > 0) {
                            withContext(Dispatchers.Main) { onProgress((downloaded * 100 / total).toInt()) }
                        }
                    }
                }
            }
            conn.disconnect()
            dest
        } catch (_: Exception) {
            null
        }
    }

    /** 使用 FileProvider 安装 APK */
    fun install(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        }
    }
}
