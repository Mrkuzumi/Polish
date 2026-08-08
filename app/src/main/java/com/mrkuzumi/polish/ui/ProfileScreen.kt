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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mrkuzumi.polish.R
import com.mrkuzumi.polish.data.RecordRepository
import com.mrkuzumi.polish.util.Prefs
import com.mrkuzumi.polish.util.Terminology
import java.io.File

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    dataVersion: Int,
    showSnackbar: (String) -> Unit,
    onCheckUpdate: () -> Unit,
) {
    val context = LocalContext.current
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
    var username by rememberSaveable { val s = Prefs.getUsername(context); mutableStateOf(s.ifEmpty { Terminology.defaultUsername(context) }) }
    var showNameDialog by rememberSaveable { mutableStateOf(false) }
    var nameInput by rememberSaveable { mutableStateOf(username) }

    // 终身统计（所有记录总和，不按月重置）
    val records = remember(dataVersion) { RecordRepository.loadAll(context) }
    val lifetimeTotal = records.values.sumOf { it.count }

    // 专用弹窗
    var showAuthorDialog by rememberSaveable { mutableStateOf(false) }
    var showSourceDialog by rememberSaveable { mutableStateOf(false) }
    var showThanksDialog by rememberSaveable { mutableStateOf(false) }

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
                        Image(
                            painter = painterResource(R.mipmap.ic_launcher),
                            contentDescription = "头像",
                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                            contentScale = ContentScale.Crop,
                        )
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
                Text("累计${Terminology.verb(context)}", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text(Terminology.emojiWithCount(context, lifetimeTotal), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = cs.primary)
            }
        }

        // ----- 分隔线 -----
        HorizontalDivider(modifier = Modifier.padding(horizontal = 32.dp, vertical = 4.dp), color = cs.outlineVariant)

        // ----- 下 2/3：功能按钮（2×2 方格） -----
        Column(
            Modifier.fillMaxWidth().weight(2f).padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileButton("关于作者", Modifier.weight(1f)) { showAuthorDialog = true }
                ProfileButton("查看源码", Modifier.weight(1f)) { showSourceDialog = true }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileButton("检查更新", Modifier.weight(1f)) { onCheckUpdate() }
                ProfileButton("特别鸣谢", Modifier.weight(1f)) { showThanksDialog = true }
            }
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
                    onValueChange = { if (it.length <= 20) nameInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("输入用户名（最多20字）") },
                    supportingText = { Text("${nameInput.length}/20") },
                )
            },
            confirmButton = {
                Button(onClick = {
                    val trimmed = nameInput.trim().take(20).ifEmpty { Terminology.defaultUsername(context) }
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

    // 关于作者弹窗
    if (showAuthorDialog) {
        AlertDialog(onDismissRequest = { showAuthorDialog = false }) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(R.drawable.author_avatar),
                    contentDescription = null,
                    modifier = Modifier.size(80.dp).clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Spacer(Modifier.height(12.dp))
                Text("Mrkuzumi", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("开发者", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { openUrl(context, "https://github.com/Mrkuzumi"); showAuthorDialog = false },
                    Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                ) { Text("访问 GitHub 主页") }
                Spacer(Modifier.height(8.dp))
                SelectionContainer {
                    Text("https://github.com/Mrkuzumi", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                }
            }
        }
    }

    // 查看源码弹窗
    if (showSourceDialog) {
        AlertDialog(onDismissRequest = { showSourceDialog = false }) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("📦 源码仓库", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Polish（磨剑 / 挖矿）", style = MaterialTheme.typography.bodyLarge, color = cs.primary)
                Text("浅嫩粉色 Material You 日常打卡日历", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { openUrl(context, "https://github.com/Mrkuzumi/Polish"); showSourceDialog = false },
                    Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = cs.primary),
                ) { Text("访问仓库") }
                Spacer(Modifier.height(8.dp))
                SelectionContainer {
                    Text("https://github.com/Mrkuzumi/Polish", style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                }
            }
        }
    }

    // 特别鸣谢弹窗
    if (showThanksDialog) {
        AlertDialog(onDismissRequest = { showThanksDialog = false }) {
            Column(
                Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("🎉 特别鸣谢", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("@ImHaoI🔻(｡ì ω í｡)🔻", style = MaterialTheme.typography.bodyLarge, color = cs.onSurface)
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { showThanksDialog = false }) { Text("关闭") }
            }
        }
    }

}

@Composable
private fun ProfileButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Button(
        onClick = onClick,
        modifier = modifier.height(50.dp),
        shape = RoundedCornerShape(20.dp),
        colors = ButtonDefaults.buttonColors(containerColor = cs.primaryContainer, contentColor = cs.onPrimaryContainer),
    ) { Text(label, style = MaterialTheme.typography.titleMedium) }
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
