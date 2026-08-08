package com.mrkuzumi.polish

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mrkuzumi.polish.ui.GenderSelectScreen
import com.mrkuzumi.polish.ui.HomeScreen
import com.mrkuzumi.polish.ui.NotificationState
import com.mrkuzumi.polish.ui.ProfileScreen
import com.mrkuzumi.polish.ui.DownloadProgress
import com.mrkuzumi.polish.ui.StatsScreen
import com.mrkuzumi.polish.ui.TopNotification
import com.mrkuzumi.polish.ui.UpdateDownloader
import com.mrkuzumi.polish.ui.UpdateInfo
import com.mrkuzumi.polish.ui.checkForUpdate
import com.mrkuzumi.polish.ui.rememberNotificationState
import com.mrkuzumi.polish.ui.theme.PolishTheme
import com.mrkuzumi.polish.util.Prefs
import kotlinx.coroutines.launch

private enum class MainTab { Home, Stats, Profile }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannel()
        setContent {
            PolishTheme {
                var gender by rememberSaveable { mutableStateOf(Prefs.getGender(this@MainActivity)) }
                androidx.compose.animation.AnimatedContent(targetState = gender, label = "gender") { current ->
                    if (current == null) {
                        GenderSelectScreen(onSelect = { selected ->
                            gender = selected
                            Prefs.setGender(this@MainActivity, selected)
                        })
                    } else {
                        MainApp()
                    }
                }
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "提醒", NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "预约日定时通知" }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val CHANNEL_ID = "polish_reminder"

        /** 预约未来某天 21:00 的磨剑提醒（兼容所有 Android 版本，无需特殊权限） */
        fun scheduleReminder(context: Context, dateStr: String) {
            try {
                val parts = dateStr.split("-")
                if (parts.size != 3) return
                val year = parts[0].toIntOrNull() ?: return
                val month = parts[1].toIntOrNull() ?: return
                val day = parts[2].toIntOrNull() ?: return

                val cal = java.util.Calendar.getInstance().apply {
                    set(year, month - 1, day, 21, 0, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                if (cal.timeInMillis <= System.currentTimeMillis()) return

                val intent = Intent(context, PolishReminderReceiver::class.java).apply {
                    putExtra("date", dateStr)
                    putExtra("id", year * 1000 + cal.get(java.util.Calendar.DAY_OF_YEAR))
                }
                val pending = PendingIntent.getBroadcast(
                    context,
                    year * 1000 + cal.get(java.util.Calendar.DAY_OF_YEAR),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarm.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pending)
                // 记录已预约日期
                Prefs.addBookedDate(context, dateStr)
            } catch (_: Exception) {
                android.widget.Toast.makeText(context, "预约失败，请检查系统权限", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun MainApp() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val notificationState = rememberNotificationState()
    val notify: (String) -> Unit = remember { { msg -> notificationState.show(msg) } }
    val scope = rememberCoroutineScope()

    var currentTab by rememberSaveable { mutableStateOf(MainTab.Home) }
    var dataVersion by rememberSaveable { mutableStateOf(0) }

    // 更新检查 + 下载
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var dlProgress by remember { mutableStateOf(DownloadProgress(0, false)) }

    val checkAndNotify = suspend {
        val info = checkForUpdate("1.2.15")
        updateInfo = info
        if (info.available) {
            showUpdateDialog = true
            dlProgress = DownloadProgress(0, false)
            notify("发现新版本 V${info.latestVersion}！")
        } else {
            notify("已是最新版本")
        }
    }

    LaunchedEffect(Unit) { checkAndNotify() }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    Crossfade(
                        targetState = currentTab,
                        animationSpec = tween(350),
                        label = "tab-crossfade",
                    ) { tab ->
                        when (tab) {
                            MainTab.Home -> HomeScreen(
                                dataVersion = dataVersion,
                                onDataChanged = { dataVersion++ },
                                showSnackbar = notify,
                                dlProgress = dlProgress,
                            )
                            MainTab.Stats -> StatsScreen(dataVersion = dataVersion)
                            MainTab.Profile -> ProfileScreen(
                                dataVersion = dataVersion,
                                showSnackbar = notify,
                                onCheckUpdate = { scope.launch { checkAndNotify() } },
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .padding(bottom = 12.dp, top = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(50))
                        .padding(3.dp)
                        .align(Alignment.CenterHorizontally),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    PillTab("首页", Icons.Filled.Home, currentTab == MainTab.Home) { currentTab = MainTab.Home }
                    PillTab("统计", Icons.Filled.BarChart, currentTab == MainTab.Stats) { currentTab = MainTab.Stats }
                    PillTab("我的", Icons.Filled.Person, currentTab == MainTab.Profile) { currentTab = MainTab.Profile }
                }
            }

            TopNotification(state = notificationState)
        }
    }

    // 更新弹窗（全局）
    if (showUpdateDialog && updateInfo != null) {
        val info = updateInfo!!
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("发现新版本 V${info.latestVersion}", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    if (dlProgress.percent > 0 && !dlProgress.done) {
                        // 下载中
                        Text("正在下载… ${dlProgress.percent}%", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.size(8.dp))
                        androidx.compose.material3.LinearProgressIndicator(
                            progress = { dlProgress.percent / 100f },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    } else if (dlProgress.done) {
                        Text("下载完成！", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    } else {
                        Text(info.releaseNotes, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 16)
                    }
                }
            },
            confirmButton = {
                if (dlProgress.done) {
                    // 安装前检查「安装未知应用」权限
                    val file = java.io.File(context.cacheDir, "update.apk")
                    Button(onClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
                            // 引导用户去设置页开启
                            val intent = Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                data = android.net.Uri.parse("package:${context.packageName}")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                            notify("请开启「安装未知应用」权限后重试")
                        } else {
                            val ok = UpdateDownloader.install(context, file)
                            if (ok) {
                                showUpdateDialog = false
                                dlProgress = DownloadProgress(0, false)
                            }
                        }
                    }) { Text("安装") }
                } else if (dlProgress.percent > 0) {
                    // 正在下载，不显示按钮
                } else {
                    Button(onClick = {
                        dlProgress = DownloadProgress(1, false) // 立即显示进度条
                        scope.launch {
                            val urls = mutableListOf<String>()
                            // 1) 标准 Release asset 直链
                            urls.add("https://github.com/Mrkuzumi/Polish/releases/download/V${info.latestVersion}/Polish_V${info.latestVersion}.apk")
                            // 2) API 返回的 browser_download_url
                            if (info.downloadUrl.isNotBlank() && info.downloadUrl !in urls) urls.add(info.downloadUrl)
                            // 3) release page (无法直接下载，但作为最后尝试)
                            if (info.releaseUrl.isNotBlank() && info.releaseUrl !in urls) urls.add(info.releaseUrl)

                            var ok = false
                            for ((i, u) in urls.withIndex()) {
                                android.util.Log.d("PolishUpdate", "try download [$i]: $u")
                                val f = UpdateDownloader.download(u, context.cacheDir) { p ->
                                    if (p >= 0) dlProgress = DownloadProgress(p.coerceIn(1, 99), false)
                                }
                                if (f != null) {
                                    dlProgress = DownloadProgress(100, true)
                                    ok = true
                                    break
                                }
                            }
                            if (!ok) {
                                dlProgress = DownloadProgress(0, false)
                                notify("下载失败，请检查网络后重试")
                            }
                        }
                    }) { Text("下载并更新") }
                }
            },
            dismissButton = {
                if (!dlProgress.done && dlProgress.percent == 0) {
                    TextButton(onClick = { showUpdateDialog = false }) { Text("暂不更新") }
                } else if (dlProgress.done) {
                    TextButton(onClick = {
                        showUpdateDialog = false
                        dlProgress = DownloadProgress(0, false)
                    }) { Text("以后再说") }
                }
            },
        )
    }
}

@Composable
private fun PillTab(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val bg = if (selected) cs.primaryContainer else cs.surface
    val fg = if (selected) cs.primary else cs.onSurfaceVariant

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp), tint = fg)
        if (selected) {
            Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
