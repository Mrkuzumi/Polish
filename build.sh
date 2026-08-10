#!/usr/bin/env bash
# Polish - Build & Release Script (Linux / macOS)
# Usage: ./build.sh [-j /path/to/jdk17] [--skip-release]
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# ===== arg parsing =====
JAVA_HOME_ARG=""
SKIP_RELEASE=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        -j|--java-home) JAVA_HOME_ARG="$2"; shift 2 ;;
        --skip-release) SKIP_RELEASE=true; shift ;;
        -h|--help) echo "Usage: ./build.sh [-j JDK_PATH] [--skip-release]"; exit 0 ;;
        *) echo "[ERROR] Unknown arg: $1"; exit 1 ;;
    esac
done

# ===== colors =====
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
GRAY='\033[0;90m'
NC='\033[0m'

# ===== helpers =====
ask_fix() {
    local why="$1"
    local cmd="$2"
    echo -e "  ${RED}[原因] $why${NC}"
    echo -e "  ${YELLOW}[命令] $cmd${NC}"
    read -r -p "  是否执行此命令？[y/N] " ans
    if [[ "$ans" =~ ^[Yy] ]]; then
        echo -e "  ${GRAY}[执行] $cmd${NC}"
        eval "$cmd"
        return $?
    fi
    return 1
}

check_cmd() {
    local name="$1"
    local why="$2"
    local fix="$3"
    if ! command -v "$name" &>/dev/null; then
        echo -e "${YELLOW}[WARN] 未找到 $name${NC}"
        if ask_fix "$why" "$fix"; then
            command -v "$name" &>/dev/null && return 0 || return 1
        fi
        return 1
    fi
    return 0
}

# ===== banner =====
echo ""
echo -e "${CYAN}========================================"
echo -e "  Polish Build Script (Polish 磨剑/挖矿)"
echo -e "========================================${NC}"
echo ""

# ===== extract version =====
VERSION=$(grep -oP 'versionName\s*=\s*"\K[^"]+' app/build.gradle.kts | head -1)
if [ -z "$VERSION" ]; then
    echo -e "${RED}[ERROR] 无法从 build.gradle.kts 提取 versionName${NC}"
    exit 1
fi
echo -e "${GREEN}[INFO] 当前版本: V$VERSION${NC}"

# ===== 0. pre-flight: git =====
echo ""
echo -e "${YELLOW}[CHECK] 检查 Git ...${NC}"
if ! check_cmd "git" \
    "Git 未安装。" \
    "sudo apt-get install -y git   # Debian/Ubuntu
sudo dnf install -y git         # Fedora
brew install git                # macOS"; then
    echo -e "${RED}[ABORT] 需要 Git 才能继续${NC}"
    exit 1
fi
echo -e "  ${GRAY}git 已就绪${NC}"

# ===== 1. pre-flight: JDK =====
echo -e "${YELLOW}[CHECK] 检查 JDK 17 ...${NC}"

if [ -n "$JAVA_HOME_ARG" ]; then
    export JAVA_HOME="$JAVA_HOME_ARG"
elif [ -z "${JAVA_HOME:-}" ]; then
    # Auto-detect
    for candidate in \
        "/usr/lib/jvm/java-17-openjdk-amd64" \
        "/usr/lib/jvm/java-17-openjdk" \
        "/usr/lib/jvm/java-17" \
        "/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home" \
        "/Library/Java/JavaVirtualMachines/temurin-17.jdk/Contents/Home"; do
        if [ -f "$candidate/bin/java" ]; then
            export JAVA_HOME="$candidate"
            break
        fi
    done
fi

if [ -z "${JAVA_HOME:-}" ] || [ ! -f "$JAVA_HOME/bin/java" ]; then
    echo -e "${YELLOW}[WARN] 未找到 JDK 17${NC}"
    if ! ask_fix "未安装 JDK 17 或路径不正确" \
        "echo '请安装: sudo apt-get install -y openjdk-17-jdk   # Debian/Ubuntu
sudo dnf install -y java-17-openjdk-devel   # Fedora
brew install openjdk@17                     # macOS'"; then
        echo -e "${RED}[ABORT] 需要 JDK 17${NC}"
        exit 1
    fi
fi

echo -e "  ${GRAY}JAVA_HOME = $JAVA_HOME${NC}"
export PATH="$JAVA_HOME/bin:$PATH"
java -version 2>&1 | head -1 | while read -r line; do echo -e "  ${GRAY}$line${NC}"; done

# ===== 2. pre-flight: Android SDK =====
echo -e "${YELLOW}[CHECK] 检查 Android SDK ...${NC}"

if [ -f "local.properties" ]; then
    SDK_PATH=$(grep -oP 'sdk\.dir\s*=\s*\K.*' local.properties 2>/dev/null || true)
fi
if [ -z "${SDK_PATH:-}" ]; then
    if [ -n "${ANDROID_HOME:-}" ]; then
        SDK_PATH="$ANDROID_HOME"
    elif [ -n "${ANDROID_SDK_ROOT:-}" ]; then
        SDK_PATH="$ANDROID_SDK_ROOT"
    elif [ -d "$HOME/Android/Sdk" ]; then
        SDK_PATH="$HOME/Android/Sdk"
    fi
fi

if [ -n "${SDK_PATH:-}" ] && [ -d "$SDK_PATH" ]; then
    echo -e "  ${GRAY}SDK = $SDK_PATH${NC}"
    export ANDROID_HOME="$SDK_PATH"
    if [ ! -f "local.properties" ]; then
        echo "sdk.dir=$SDK_PATH" > local.properties
        echo -e "  ${GRAY}已写入 local.properties${NC}"
    fi
else
    if ! ask_fix "未找到 Android SDK。请安装 Android Studio 或 cmdline-tools。" \
        "echo '请安装 Android Studio: https://developer.android.com/studio'"; then
        echo -e "${RED}[ABORT] 未找到 Android SDK${NC}"
        exit 1
    fi
    exit 1
fi

# ===== 3. pre-flight: gradlew =====
echo -e "${YELLOW}[CHECK] 检查 Gradle wrapper ...${NC}"
if [ ! -f "./gradlew" ]; then
    if [ -f "./gradlew.bat" ]; then
        GRADLE="./gradlew.bat"
    else
        echo -e "${RED}[ERROR] 缺少 gradlew${NC}"
        exit 1
    fi
else
    GRADLE="./gradlew"
    chmod +x "$GRADLE" 2>/dev/null || true
fi
echo -e "  ${GRAY}$GRADLE 已就绪${NC}"

# ===== 4. build =====
echo ""
echo -e "${CYAN}========================================"
echo -e "  开始编译 (assembleDebug) ..."
echo -e "========================================${NC}"
echo ""

# Run gradle with live output + capture exit code
set +e
"$GRADLE" assembleDebug --console=plain --stacktrace 2>&1
BUILD_EXIT=$?
set -e

echo ""

if [ $BUILD_EXIT -ne 0 ]; then
    echo -e "${RED}========================================"
    echo -e "  构建失败 (exit $BUILD_EXIT)"
    echo -e "========================================${NC}"
    echo ""

    # Re-run to capture output for analysis
    BUILD_LOG=$("$GRADLE" assembleDebug --console=plain 2>&1) || true

    if echo "$BUILD_LOG" | grep -qE 'Could not resolve|Could not get resource|Connect to|Unknown host|timeout'; then
        echo -e "${RED}[分析] 网络/依赖下载失败${NC}"
        ask_fix "Gradle 无法下载依赖，可能是网络或代理问题。" \
            "echo '检查: 1) 网络连接  2) ~/.gradle/gradle.properties 中的 proxy 设置'"

    elif echo "$BUILD_LOG" | grep -qE 'Unsupported class file|UnsupportedClassVersionError|invalid source release|class file has wrong version'; then
        echo -e "${RED}[分析] JDK 版本不匹配${NC}"
        ask_fix "JDK 版本与项目要求不匹配（需要 JDK 17）" \
            "echo '请安装 JDK 17: sudo apt-get install openjdk-17-jdk / brew install openjdk@17'"

    elif echo "$BUILD_LOG" | grep -qE 'Android SDK|android\.jar|SDK location|compileSdk|sdkmanager'; then
        echo -e "${RED}[分析] Android SDK 配置问题${NC}"
        ask_fix "SDK 路径有误或缺少 API 35。请用 sdkmanager 安装：\"platforms;android-35\"" \
            "echo '请运行: \$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager \"platforms;android-35\"'"

    elif echo "$BUILD_LOG" | grep -qE 'Permission denied|Access is denied'; then
        echo -e "${RED}[分析] 文件权限问题${NC}"
        ask_fix "build/ 目录无写入权限或被占用。" \
            "rm -rf app/build && echo '已清理 app/build 目录'"

    elif echo "$BUILD_LOG" | grep -qE 'OutOfMemoryError|GC overhead'; then
        echo -e "${RED}[分析] Gradle 内存不足${NC}"
        ask_fix "Gradle 堆内存不足，需要增大上限。" \
            "echo 'org.gradle.jvmargs=-Xmx2048m' > gradle.properties"

    else
        echo -e "${RED}[分析] 未知编译错误，请检查上方输出${NC}"
    fi
    exit $BUILD_EXIT
fi

APK_PATH="app/build/outputs/apk/debug/Polish_V${VERSION}.apk"
if [ ! -f "$APK_PATH" ]; then
    echo -e "${RED}[ERROR] APK 未生成: $APK_PATH${NC}"
    exit 1
fi

APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
echo -e "${GREEN}========================================"
echo -e "  构建成功!"
echo -e "  APK: $APK_PATH"
echo -e "  大小: $APK_SIZE"
echo -e "========================================${NC}"

# ===== 5. git status =====
echo ""
echo -e "${YELLOW}[GIT] 状态检查 ...${NC}"
BRANCH=$(git branch --show-current 2>/dev/null || echo "unknown")
echo -e "  ${GRAY}分支: $BRANCH${NC}"

REMOTE=$(git remote get-url origin 2>/dev/null || true)
if [ -z "$REMOTE" ]; then
    if ! ask_fix "未配置 git remote origin，无法推送。" \
        "git remote add origin https://github.com/Mrkuzumi/Polish.git"; then
        echo -e "${YELLOW}[WARN] 跳过 Git 操作${NC}"
        SKIP_RELEASE=true
    fi
fi

DIRTY=$(git status --porcelain 2>/dev/null || true)
if [ -n "$DIRTY" ]; then
    echo -e "${YELLOW}[WARN] 有未提交的修改:${NC}"
    echo -e "${GRAY}$DIRTY${NC}"
    if ask_fix "有未提交的修改。是否先提交？" \
        "git add -A && git commit -m 'build: V$VERSION'"; then
        echo -e "  ${GREEN}已提交${NC}"
    fi
fi

# ===== 6. release =====
if [ "$SKIP_RELEASE" = true ]; then
    echo ""
    echo -e "${CYAN}[DONE] 跳过发布 (--skip-release)${NC}"
    echo -e "  ${CYAN}APK 路径: $APK_PATH${NC}"
    exit 0
fi

echo ""
read -r -p "是否发布 GitHub Release V$VERSION？[y/N] " ANSWER
if [[ ! "$ANSWER" =~ ^[Yy] ]]; then
    echo -e "${CYAN}[DONE] 跳过发布。APK: $APK_PATH${NC}"
    exit 0
fi

# ===== 6a. check gh =====
echo ""
echo -e "${YELLOW}[CHECK] 检查 gh CLI ...${NC}"
if ! check_cmd "gh" \
    "gh CLI 未安装。" \
    "curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg | sudo dd of=/usr/share/keyrings/githubcli-archive-keyring.gpg
sudo apt-get install -y gh   # Debian/Ubuntu
# 或: brew install gh        # macOS"; then
    exit 1
fi

# Check gh auth
if ! gh auth status &>/dev/null; then
    echo -e "${YELLOW}[WARN] gh 未登录${NC}"
    if ask_fix "gh CLI 未登录 GitHub 账户。" "gh auth login --web"; then
        echo -e "${YELLOW}  请完成浏览器登录后按回车继续...${NC}"
        read -r
    else
        echo -e "${RED}[ABORT] 需要登录 gh${NC}"
        exit 1
    fi
fi
echo -e "  ${GRAY}gh 已就绪${NC}"

# ===== 6b. push & tag =====
echo -e "${YELLOW}[GIT] 推送代码...${NC}"
if ! git push origin "$BRANCH" 2>&1; then
    if ! ask_fix "推送失败。请检查网络或仓库权限。" \
        "echo '检查: 1) 网络连接  2) GitHub 访问权限'"; then
        echo -e "${RED}[ABORT] 推送失败${NC}"
        exit 1
    fi
fi

if git tag -l "V$VERSION" | grep -q . ; then
    echo -e "${YELLOW}[WARN] 标签 V$VERSION 已存在，删除并重建...${NC}"
    git tag -d "V$VERSION" 2>/dev/null || true
    git push origin ":refs/tags/V$VERSION" 2>/dev/null || true
fi

git tag "V$VERSION"
if ! git push origin "V$VERSION" 2>&1; then
    if ! ask_fix "标签推送失败。" \
        "echo '检查: 1) 网络连接  2) 仓库写权限'"; then
        echo -e "${RED}[ABORT] 标签推送失败${NC}"
        exit 1
    fi
fi
echo -e "  ${GREEN}标签 V$VERSION 已推送${NC}"

# ===== 6c. create release =====
echo ""
echo -e "${YELLOW}[RELEASE] 创建 GitHub Release ...${NC}"

read -r -p "Release 标题 (回车使用默认): " RELEASE_TITLE
RELEASE_TITLE="${RELEASE_TITLE:-V$VERSION}"

read -r -p "Release 说明 (回车使用模板): " RELEASE_BODY
RELEASE_BODY="${RELEASE_BODY:-Polish V$VERSION 发布}"

echo -e "${GRAY}[UPLOAD] 正在上传 APK ...${NC}"

set +e
RELEASE_OUTPUT=$(gh release create "V$VERSION" "$APK_PATH" --title "$RELEASE_TITLE" --notes "$RELEASE_BODY" 2>&1)
RELEASE_EXIT=$?
set -e

if [ $RELEASE_EXIT -eq 0 ]; then
    echo ""
    echo -e "${GREEN}========================================"
    echo -e "  发布成功!"
    echo -e "  $RELEASE_OUTPUT"
    echo -e "========================================${NC}"
else
    echo ""
    echo -e "${RED}[FAIL] Release 创建失败${NC}"

    if echo "$RELEASE_OUTPUT" | grep -qE 'already exists|409'; then
        echo -e "${RED}[原因] Release V$VERSION 已存在${NC}"
        if ask_fix "同名 Release 已存在。是否删除后重试？" \
            "gh release delete V$VERSION --yes"; then
            if gh release create "V$VERSION" "$APK_PATH" --title "$RELEASE_TITLE" --notes "$RELEASE_BODY"; then
                echo -e "${GREEN}[OK] 发布成功!${NC}"
            else
                echo -e "${RED}[FAIL] 仍然失败${NC}"; exit 1
            fi
        fi

    elif echo "$RELEASE_OUTPUT" | grep -qE 'validation|422'; then
        echo -e "${RED}[原因] 标签或参数格式有误${NC}"
        echo -e "${GRAY}$RELEASE_OUTPUT${NC}"

    elif echo "$RELEASE_OUTPUT" | grep -qE 'Could not resolve|connect|timeout|SSL'; then
        echo -e "${RED}[原因] 网络连接失败${NC}"
        ask_fix "网络异常，请检查网络/代理后重试。" \
            "echo '检查: 1) 网络连接  2) HTTPS_PROXY 环境变量'"

    else
        echo -e "${RED}[原因] 未知错误:${NC}"
        echo -e "${GRAY}$RELEASE_OUTPUT${NC}"
    fi
    exit 1
fi
