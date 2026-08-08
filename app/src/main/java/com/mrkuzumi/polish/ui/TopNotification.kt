package com.mrkuzumi.polish.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** 顶部通知状态管理 */
class NotificationState {
    private val _message = mutableStateOf<String?>(null)
    internal val current: State<String?> = _message

    fun show(msg: String) {
        _message.value = msg
    }

    fun dismiss() {
        _message.value = null
    }
}

@Composable
fun rememberNotificationState(): NotificationState = remember { NotificationState() }

/** 从顶部滑入的白色通知卡片（替代 Snackbar） */
@Composable
fun TopNotification(
    state: NotificationState,
    modifier: Modifier = Modifier,
) {
    val msg = state.current.value

    // 自动消失
    if (msg != null) {
        LaunchedEffect(msg) {
            delay(2200)
            state.dismiss()
        }
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = msg != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        ) {
            msg?.let { text ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                    modifier = Modifier.padding(top = 8.dp, start = 16.dp, end = 16.dp),
                ) {
                    Text(
                        text = text,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 13.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
