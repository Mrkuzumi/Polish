package com.mrkuzumi.polish

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.mrkuzumi.polish.ui.GenderSelectScreen
import com.mrkuzumi.polish.ui.MainScreen
import com.mrkuzumi.polish.ui.theme.PolishTheme
import com.mrkuzumi.polish.util.Prefs

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PolishTheme {
                // 首次启动先选性别，选择结果持久化；之后直接进入日历主页
                var gender by rememberSaveable {
                    mutableStateOf(Prefs.getGender(this@MainActivity))
                }
                AnimatedContent(targetState = gender, label = "gender-switch") { current ->
                    if (current == null) {
                        GenderSelectScreen(
                            onSelect = { selected ->
                                gender = selected
                                Prefs.setGender(this@MainActivity, selected)
                            }
                        )
                    } else {
                        MainScreen()
                    }
                }
            }
        }
    }
}
