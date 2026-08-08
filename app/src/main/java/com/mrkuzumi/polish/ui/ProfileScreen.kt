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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mrkuzumi.polish.util.Prefs
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ProfileScreen(
    showSnackbar: (String) -> Unit,
    onManualUpdateCheck: suspend () -> UpdateInfo,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 头像
    val avatarFile = remember { File(context.filesDir, "avatar.jpg") }
    var avatarVersion by rememberSaveable { mutableStateOf(0) }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let {
            context.contentResolver.openInputStream(it)?.use { input ->
                avatarFile.outputStream().use { output -> input.copyTo(output) }
            }
            avatarVersion++
        }
    }

    // 性别
    var gender by rememberSaveable { mutableStateOf(Prefs.getGender(context) ?: "male") }

    // 更新弹窗
    var dialogInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showDialog by rememberSaveable { mutableStateOf(false) }

    val cs = MaterialTheme.colorScheme

    // ===== 整体分为上 1/3 + 分隔 + 下 2/3 =====
    Column(Modifier.fillMaxSize()) {
        // ----- 上 1/3：头像 + 性别 -----
        Column(
            Modifier.fillMaxWidth().weight(1f).padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(cs.surfaceVariant)
                .clickable { imagePicker.launch("image/*") },
            contentAlignment = Alignment.Center,
        ) {
            if (avatarFile.exists()) {
                AsyncImage(
                    model = ImageRequest.Builder(context).data(avatarFile).crossfade(true).build(),
                    contentDescription = "头像",
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = "设置头像",
                    modifier = Modifier.size(56.dp),
                    tint = cs.onSurfaceVariant,
                )
            }
        }
        Text(
            "点击更换头像",
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(16.dp))

        // 性别显示 + 切换
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "性别",
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant,
            )
            Spacer(Modifier.size(12.dp))
            val isMale = gender == "male"
            Text(
                text = if (isMale) "♂ 男" else "♀ 女",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = cs.primary,
            )
            Spacer(Modifier.size(20.dp))
            Button(
                onClick = {
                    gender = if (isMale) "female" else "male"
                    Prefs.setGender(context, gender)
                    showSnackbar(if (isMale) "已切换为女性" else "已切换为男性")
                },
                shape = MaterialTheme.shapes.medium,
            ) {
                Text("切换")
            }
        }
    }

    // ===== 分隔线 =====
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp),
        color = cs.outlineVariant,
    )

    // ===== 下 2/3：功能按钮 =====
    Column(
        Modifier.fillMaxWidth().weight(2f).padding(horizontal = 32.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.Top,
    ) {
        ProfileButton("关于作者") {
            openUrl(context, "https://github.com/Mrkuzumi")
        }
        ProfileButton("查看源码") {
            openUrl(context, "https://github.com/Mrkuzumi/Polish")
        }
        ProfileButton("检查更新") {
            scope.launch {
                val info = onManualUpdateCheck()
                dialogInfo = info
                showDialog = true
            }
        }
    }

    } // 关闭外层 fillMaxSize Column

    // 更新弹窗
    if (showDialog && dialogInfo != null) {
        val info = dialogInfo!!
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    if (info.available) "发现新版本 V${info.latestVersion}" else "已是最新版本",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Column {
                    if (info.available) {
                        Text(
                            info.releaseNotes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant,
                            maxLines = 20,
                            textAlign = TextAlign.Start,
                        )
                    } else {
                        Text("当前版本 V1.1.0 已是最新。", color = cs.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                if (info.available) {
                    Button(onClick = {
                        openUrl(context, info.releaseUrl.ifEmpty { info.downloadUrl })
                        showDialog = false
                    }) { Text("前往下载") }
                } else {
                    Button(onClick = { showDialog = false }) { Text("确定") }
                }
            },
            dismissButton = {
                if (info.available) {
                    TextButton(onClick = { showDialog = false }) { Text("暂不更新") }
                }
            },
        )
    }
}

@Composable
private fun ProfileButton(label: String, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp).padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
    if (intent.resolveActivity(context.packageManager) != null) {
        context.startActivity(intent)
    }
}
