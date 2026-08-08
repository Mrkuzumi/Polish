package com.mrkuzumi.polish

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import com.mrkuzumi.polish.ui.ProfileScreen
import com.mrkuzumi.polish.ui.StatsScreen
import com.mrkuzumi.polish.ui.UpdateInfo
import com.mrkuzumi.polish.ui.checkForUpdate
import com.mrkuzumi.polish.ui.theme.PolishTheme
import com.mrkuzumi.polish.util.Prefs
import kotlinx.coroutines.launch

private enum class MainTab { Home, Stats, Profile }

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
}

@Composable
private fun MainApp() {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showSnackbar: (String) -> Unit = remember {
        { msg -> scope.launch { snackbarHostState.showSnackbar(msg) } }
    }

    var currentTab by rememberSaveable { mutableStateOf(MainTab.Home) }
    var dataVersion by rememberSaveable { mutableStateOf(0) }

    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val info = checkForUpdate("1.1.5")
        if (info.available) { updateInfo = info; showUpdateDialog = true }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // 页面内容（占满除底部导航外的全部空间，含丝滑切换动画）
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
                            showSnackbar = showSnackbar,
                        )
                        MainTab.Stats -> StatsScreen(dataVersion = dataVersion)
                        MainTab.Profile -> ProfileScreen(
                            dataVersion = dataVersion,
                            showSnackbar = showSnackbar,
                            onManualUpdateCheck = { checkForUpdate("1.1.5") },
                        )
                    }
                }
            }

            // 悬浮椭圆导航（在内容下方，不遮挡）
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
