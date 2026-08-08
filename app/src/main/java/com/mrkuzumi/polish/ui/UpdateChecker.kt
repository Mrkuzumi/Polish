package com.mrkuzumi.polish.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * GitHub Releases 更新信息
 */
data class UpdateInfo(
    val available: Boolean,
    val latestVersion: String,
    val releaseNotes: String,
    val downloadUrl: String,
    val releaseUrl: String,
)

/**
 * 查询 GitHub Releases 是否有新版本。
 * 在后台线程执行，建议从 [Dispatchers.IO] 协程调用。
 *
 * @param currentVersion 当前 App 版本号（纯数字，如 "1.1.0"）
 * @param repoOwner      仓库所有者
 * @param repoName       仓库名
 */
suspend fun checkForUpdate(
    currentVersion: String,
    repoOwner: String = "Mrkuzumi",
    repoName: String = "Polish",
): UpdateInfo = withContext(Dispatchers.IO) {
    try {
        val url = URL("https://api.github.com/repos/$repoOwner/$repoName/releases/latest")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        if (conn.responseCode != 200) {
            conn.disconnect()
            return@withContext UpdateInfo(false, "", "", "", "")
        }
        val body = conn.inputStream.bufferedReader().use { it.readText() }
        conn.disconnect()

        val json = JSONObject(body)
        val tag = json.optString("tag_name", "").trimStart('V', 'v')
        val notes = json.optString("body", "")
        val htmlUrl = json.optString("html_url", "")
        val assets = json.optJSONArray("assets")
        val dlUrl = if (assets != null && assets.length() > 0) {
            assets.getJSONObject(0).optString("browser_download_url", htmlUrl)
        } else {
            htmlUrl
        }

        val available = compareVersions(tag, currentVersion) > 0
        UpdateInfo(
            available = available,
            latestVersion = tag.ifEmpty { "unknown" },
            releaseNotes = notes.ifEmpty { "暂无发布说明" },
            downloadUrl = dlUrl.ifEmpty { htmlUrl },
            releaseUrl = htmlUrl.ifEmpty { "https://github.com/$repoOwner/$repoName/releases" },
        )
    } catch (_: Exception) {
        // 网络不通或 API 不可达 → 静默忽略
        UpdateInfo(false, "", "", "", "")
    }
}

/** 比较两个语义化版本字符串，返回 >0 / 0 / <0 */
private fun compareVersions(a: String, b: String): Int {
    val partsA = a.split(".").map { it.toIntOrNull() ?: 0 }
    val partsB = b.split(".").map { it.toIntOrNull() ?: 0 }
    for (i in 0 until maxOf(partsA.size, partsB.size)) {
        val va = partsA.getOrElse(i) { 0 }
        val vb = partsB.getOrElse(i) { 0 }
        if (va != vb) return va - vb
    }
    return 0
}
