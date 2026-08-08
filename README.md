# Polish（磨剑 / 挖矿）

<p align="center">
  <img src="icon.png" width="160" alt="Polish icon">
</p>

<p align="center">
  浅嫩粉色 Material You 风格的 Android 日常习惯打卡日历
</p>

---

## 功能

- **性别选择** —— 首次启动选男/女，决定全应用术语（男 → 🦌 磨剑 / 女 → ⛏ 挖矿）
- **日历打卡** —— 点日期 +1，长按持续 -1，自动记录每次操作时间戳
- **编辑细节** —— 底部抽屉编辑「下饭菜」和「左/右手」信息
- **预约未来日** —— 点未来日期 → 弹窗确认 → 当天 21:00 推送通知提醒
- **统计页** —— 本月次数 + 平均时段 + 每日柱状图 + 24 小时时段分布
- **个人页** —— 头像上传、用户名编辑、性别切换、终身统计、GitHub 跳转、自动更新检查
- **自动更新** —— 启动检测 GitHub Release，弹窗下载进度条 + 一键安装

## 截图

| 主页 | 统计 | 我的 |
|:---:|:---:|:---:|
| 日历 + 🦌 计数 | 柱状图 + 时段 | 头像 + 设置 |

## 技术栈

| 层 | 技术 |
|---|---|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material Design 3 |
| 主题 | 浅嫩粉色 `#D6507E` 主色，卡片化大圆角 |
| 持久化 | SharedPreferences + JSON 文件 |
| 通知 | AlarmManager + BroadcastReceiver |
| 图标 | Adaptive Icon（自适应圆形遮罩） |
| 图片 | Coil（头像加载） |
| 网络 | HttpURLConnection（GitHub API） |
| 最低 SDK | Android 8.0 (API 26) |
| 目标 SDK | Android 15 (API 35) |

## 构建

### 环境要求
- JDK 17 (Temurin)
- Android SDK 35
- Gradle 8.9（工程自带 wrapper）

### 命令

```powershell
# Windows PowerShell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
.\gradlew.bat assembleDebug
```

产物：`app/build/outputs/apk/debug/Polish_V1.x.x.apk`

### 版本号规则

每次修改 + push 前升版本：
- `versionName` = `大版本.新功能.bug修复`（如 `1.2.6`）
- `versionCode` = `大版本×10000 + 新功能×100 + bug修复`（如 `10206`）
- 修改位置：`app/build.gradle.kts` → `defaultConfig`

## 项目结构

```
app/src/main/java/com/mrkuzumi/polish/
├── MainActivity.kt               # 入口 + 更新/通知/预约管理
├── PolishReminderReceiver.kt     # 闹钟广播接收器
├── data/
│   └── RecordRepository.kt       # JSON 文件持久化
├── ui/
│   ├── theme/                    # Color / Type / Theme
│   ├── GenderSelectScreen.kt     # 性别选择
│   ├── HomeScreen.kt             # 日历主页
│   ├── StatsScreen.kt            # 统计页
│   ├── ProfileScreen.kt          # 个人页
│   ├── EditRecordSheet.kt        # 编辑抽屉
│   ├── UpdateChecker.kt          # GitHub Release 检查
│   ├── UpdateDownloader.kt       # APK 下载
│   └── TopNotification.kt        # 顶部通知
└── util/
    ├── Prefs.kt                  # SharedPreferences
    └── Terminology.kt            # 性别联动术语
```

## 许可

MIT

---

<p align="center">
  <sub>🤖 由 Claude Code 辅助开发</sub>
</p>
