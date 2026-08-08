package com.mrkuzumi.polish

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
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
import androidx.compose.ui.Modifier
import com.mrkuzumi.polish.ui.GenderSelectScreen
import com.mrkuzumi.polish.ui.HomeScreen
import com.mrkuzumi.polish.ui.ProfileScreen
import com.mrkuzumi.polish.ui.StatsScreen
import com.mrkuzumi.polish.ui.UpdateInfo
import com.mrkuzumi.polish.ui.checkForUpdate
import com.mrkuzumi.polish.ui.theme.PolishTheme
import com.mrkuzumi.polish.util.Prefs
import kotlinx.coroutines.launch

private enum class MainTab(val label: String) {
    Home("首页"),
    Stats("统计"),
    Profile("我的"),
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PolishTheme {
                var gender by rememberSaveable { mutableStateOf(Prefs.getGender(this@MainActivity)) }
                AnimatedContent(targetState = gender, label = "gender") { current ->
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

    // 自动更新检查（启动时静默执行）
    var updateInfo by remember { mutableStateOf<UpdateInfo?>(null) }
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val info = checkForUpdate("1.1.0")
        if (info.available) {
            updateInfo = info
            showUpdateDialog = true
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                NavigationBarItem(
                    selected = currentTab == MainTab.Home,
                    onClick = { currentTab = MainTab.Home },
                    icon = { Icon(Icons.Filled.Home, contentDescription = "首页") },
                    label = { Text("首页") },
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.Stats,
                    onClick = { currentTab = MainTab.Stats },
                    icon = { Icon(Icons.Filled.BarChart, contentDescription = "统计") },
                    label = { Text("统计") },
                )
                NavigationBarItem(
                    selected = currentTab == MainTab.Profile,
                    onClick = { currentTab = MainTab.Profile },
                    icon = { Icon(Icons.Filled.Person, contentDescription = "我的") },
                    label = { Text("我的") },
                )
            }
        },
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (currentTab) {
                MainTab.Home -> HomeScreen(
                    dataVersion = dataVersion,
                    onDataChanged = { dataVersion++ },
                    showSnackbar = showSnackbar,
                )
                MainTab.Stats -> StatsScreen(dataVersion = dataVersion)
                MainTab.Profile -> ProfileScreen(
                    showSnackbar = showSnackbar,
                    onManualUpdateCheck = { checkForUpdate("1.1.0") },
                )
            }
        }
    }
}
