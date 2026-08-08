package com.mrkuzumi.polish.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mrkuzumi.polish.data.RecordRepository
import com.mrkuzumi.polish.util.Prefs
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ProfileScreen(
    dataVersion: Int,
    showSnackbar: (String) -> Unit,
    onManualUpdateCheck: suspend () -> UpdateInfo,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme

    // 头像
    val avatarFile = remember { File(context.filesDir, "avatar.jpg") }
    var avatarVersion by rememberSaveable { mutableStateOf(0) }
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { input ->
                avatarFile.outputStream().use { o -> input.copyTo(o) }
            }
            avatarVersion++
        }
    }

    // 性别
    var gender by rememberSaveable { mutableStateOf(Prefs.getGender(context) ?: "male") }

    // 用户名
    var username by rememberSaveable { mutableStateOf(Prefs.getUsername(context)) }
    var showNameDialog by rememberSaveable { mutableStateOf(false) }
    var nameInput by rememberSaveable { mutableStateOf(username) }

    // 终身统计（所有记录总和，不按月重置）
    val records = remember(dataVersion) { RecordRepository.loadAll(context) }
    val lifetimeTotal = records.values.sumOf { it.count }

    // GitHub 跳转确认弹窗
    var showLinkDialog by rememberSaveable { mutableStateOf(false) }
    var pendingUrl by remember { mutableStateOf("") }
    var pendingLabel by remember { mutableStateOf("") }

    // 更新弹窗
    var dialogInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showDialog by rememberSaveable { mutableStateOf(false) }

    // ===== 整体布局 =====
    Column(Modifier.fillMaxSize()) {
        // ----- 上 1/3：头像 + 用户名 + 性别 + 终身统计 -----
        Column(
            Modifier.fillMaxWidth().weight(1f).padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 头像（右下角摄像机图标）
            Box(
                Modifier.size(90.dp).clickable { imagePicker.launch("image/*") },
                contentAlignment = Alignment.BottomEnd,
            ) {
                Box(Modifier.size(90.dp).clip(CircleShape).background(cs.surfaceVariant), contentAlignment = Alignment.Center) {
                    if (avatarFile.exists()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(avatarFile).crossfade(true).build(),
                            contentDescription = "头像",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(50.dp), tint = cs.onSurfaceVariant)
                    }
                }
                // 摄像机图标
                Box(
                    Modifier.size(26.dp).clip(CircleShape).background(Color.Gray.copy(alpha = 0.75f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.CameraAlt, "更换头像", modifier = Modifier.size(14.dp), tint = Color.White)
                }
            }

            Spacer(Modifier.height(12.dp))

            // 用户名（居中 + 大字号）+ 右侧铅笔编辑
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)) {
                Text(
                    username,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.align(Alignment.Center).padding(horizontal = 36.dp).fillMaxWidth(),
                )
                IconButton(
                    onClick = { nameInput = username; showNameDialog = true },
                    modifier = Modifier.size(32.dp).align(Alignment.CenterEnd),
                ) {
                    Icon(Icons.Default.Edit, "编辑用户名", modifier = Modifier.size(18.dp), tint = cs.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(16.dp))

            // 性别 + 终身统计（按钮居中对齐）
            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("性别", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, modifier = Modifier.align(Alignment.CenterVertically))
                Spacer(Modifier.width(6.dp))
                val isMale = gender == "male"
                Text(if (isMale) "♂ 男" else "♀ 女", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = cs.primary, modifier = Modifier.align(Alignment.CenterVertically))
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = {
                    gender = if (isMale) "female" else "male"
                    Prefs.setGender(context, gender)
                    showSnackbar(if (isMale) "已切换为女性" else "已切换为男性")
                }, modifier = Modifier.height(32.dp).align(Alignment.CenterVertically), shape = RoundedCornerShape(12.dp)) {
                    Text("切换", fontSize = 12.sp)
                }
                Spacer(Modifier.width(16.dp))
                Text("累计磨剑", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text("🦌×$lifetimeTotal", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = cs.primary)
            }
        }

        // ----- 分隔线 -----
        HorizontalDivider(modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp), color = cs.outlineVariant)

        // ----- 下 2/3：功能按钮（适中大小） -----
        Column(
            Modifier.fillMaxWidth().weight(2f).padding(horizontal = 32.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Button(
                onClick = {
                    pendingUrl = "https://github.com/Mrkuzumi"
                    pendingLabel = "关于作者 (Mrkuzumi)"
                    showLinkDialog = true
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cs.primaryContainer, contentColor = cs.onPrimaryContainer),
            ) { Text("关于作者", style = MaterialTheme.typography.titleMedium) }
            Button(
                onClick = {
                    pendingUrl = "https://github.com/Mrkuzumi/Polish"
                    pendingLabel = "查看源码 (Polish)"
                    showLinkDialog = true
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cs.primaryContainer, contentColor = cs.onPrimaryContainer),
            ) { Text("查看源码", style = MaterialTheme.typography.titleMedium) }
            Button(
                onClick = {
                    scope.launch { val info = onManualUpdateCheck(); dialogInfo = info; showDialog = true }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = cs.primaryContainer, contentColor = cs.onPrimaryContainer),
            ) { Text("检查更新", style = MaterialTheme.typography.titleMedium) }
        }
    } // 关闭外层 fillMaxSize Column

    // 用户名编辑弹窗
    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("修改用户名") },
            text = {
                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入用户名") },
                )
            },
            confirmButton = {
                Button(onClick = {
                    val trimmed = nameInput.trim().ifEmpty { "磨剑用户" }
                    Prefs.setUsername(context, trimmed)
                    username = trimmed
                    showNameDialog = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showNameDialog = false }) { Text("取消") }
            },
        )
    }

    // GitHub 跳转确认弹窗
    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text("跳转外部链接") },
            text = { Text("是否跳转到「$pendingLabel」对应的GitHub页面？") },
            confirmButton = {
                Button(onClick = { openUrl(context, pendingUrl); showLinkDialog = false }) { Text("跳转") }
            },
            dismissButton = {
                TextButton(onClick = { showLinkDialog = false }) { Text("取消") }
            },
        )
    }

    // 更新弹窗
    if (showDialog && dialogInfo != null) {
        val info = dialogInfo!!
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(if (info.available) "发现新版本 V${info.latestVersion}" else "已是最新版本", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column {
                    if (info.available) Text(info.releaseNotes, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, maxLines = 20, textAlign = TextAlign.Start)
                    else Text("当前版本 V1.1.9 已是最新。", color = cs.onSurfaceVariant)
                }
            },
            confirmButton = {
                if (info.available) Button(onClick = { openUrl(context, info.releaseUrl.ifEmpty { info.downloadUrl }); showDialog = false }) { Text("前往下载") }
                else Button(onClick = { showDialog = false }) { Text("确定") }
            },
            dismissButton = { if (info.available) TextButton(onClick = { showDialog = false }) { Text("暂不更新") } },
        )
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            android.widget.Toast.makeText(context, "未找到浏览器应用", android.widget.Toast.LENGTH_SHORT).show()
        }
    } catch (_: Exception) {
        android.widget.Toast.makeText(context, "无法打开链接", android.widget.Toast.LENGTH_SHORT).show()
    }
}
