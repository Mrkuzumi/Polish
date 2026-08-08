# Polish（磨剑）项目规范

## 项目概述
浅嫩粉色 Material You (MD3) 风格的 Android 日历应用。Kotlin + Jetpack Compose + Material 3。

- 首次启动：选择性别（男 / 女，两个大圆角卡片按钮），结果存 SharedPreferences（`Prefs.kt`）
- 主页：日历卡片占上 3/4，底部按钮拓展区占下 1/4（今天 / 记录 / 统计）
- 日历：周一开头，支持月切换、日期选择、前后月补位，今天高亮

## 版本号规则（每次修改 + push 前必须执行）
**每次对代码做任何修改，并且在 git push 之前，必须先提升版本号。**

- 版本格式：`V<大版本>.<新功能>.<bug修复>`
  - 第 1 位：大版本（重大重构/里程碑）
  - 第 2 位：新功能追加
  - 第 3 位：bug 修复
- 改动位置：`app/build.gradle.kts` 中 `defaultConfig`
  - `versionName = "1.0.0"` → 按语义升位
  - `versionCode = 10000`（公式：大版本×10000 + 新功能×100 + bug修复）同步更新
- 编译产物统一命名为 `Polish_V<versionName>.apk`（`applicationVariants` 已配置）
- 提交信息建议带上版本号，如 `feat: xxx (V1.1.0)`

## 构建命令（Windows）
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot"
.\gradlew.bat assembleDebug
```
产物：`app/build/outputs/apk/debug/Polish_V1.0.0.apk`

## 环境
- JDK 17（Temurin），Android SDK 位于 `%LOCALAPPDATA%\Android\Sdk`（见 `local.properties`，不入库）
- AGP 8.7.3 / Gradle 8.9 / Kotlin 2.0.21 / compileSdk 35 / minSdk 26 / targetSdk 35
- 本地工具 zip 解压在 `tools/`（已 gitignore）

## 目录结构
```
app/src/main/java/com/mrkuzumi/polish/
├── MainActivity.kt            # 入口：性别选择 → 主页 的切换
├── ui/
│   ├── theme/                 # Color / Type / Theme（浅嫩粉色 MD3）
│   ├── GenderSelectScreen.kt  # 性别选择页
│   └── MainScreen.kt          # 日历主页 + 底部按钮区
└── util/Prefs.kt              # SharedPreferences 封装
```

## 构建脚本里的坑
- `applicationVariants.all` 中 `outputs.all` 强制转换 `BaseVariantOutputImpl` 为 AGP 内部 API，升级 AGP 时需检查
- 版本号是双写（versionName 用于显示与文件名，versionCode 用于系统升级判定），两个都要改
