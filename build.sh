#!/usr/bin/env bash
# Polish（磨剑）构建 + 发布脚本（Linux / macOS）
# 用法：./build.sh
#       或带参数：./build.sh -j /path/to/jdk17 --skip-release

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# ======== 参数解析 ========
JAVA_HOME_ARG=""
SKIP_RELEASE=false

while [[ $# -gt 0 ]]; do
    case "$1" in
        -j|--java-home) JAVA_HOME_ARG="$2"; shift 2 ;;
        --skip-release) SKIP_RELEASE=true; shift ;;
        *) echo "[ERROR] 未知参数: $1"; exit 1 ;;
    esac
done

# ======== 颜色输出 ========
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
CYAN='\033[0;36m'
GRAY='\033[0;90m'
NC='\033[0m' # No Color

echo ""
echo -e "${CYAN}========================================"
echo -e "  Polish 构建脚本（磨剑 / 挖矿）"
echo -e "========================================${NC}"

# ======== 0. JAVA_HOME ========
if [ -n "$JAVA_HOME_ARG" ]; then
    export JAVA_HOME="$JAVA_HOME_ARG"
elif [ -z "${JAVA_HOME:-}" ]; then
    # 尝试自动探测
    if [ -d "/usr/lib/jvm/java-17-openjdk" ]; then
        export JAVA_HOME="/usr/lib/jvm/java-17-openjdk"
    elif [ -d "/usr/lib/jvm/java-17-openjdk-amd64" ]; then
        export JAVA_HOME="/usr/lib/jvm/java-17-openjdk-amd64"
    elif command -v java &>/dev/null; then
        JAVA_BIN="$(readlink -f "$(command -v java)")"
        export JAVA_HOME="$(dirname "$(dirname "$JAVA_BIN")")"
    fi
fi

if [ -z "${JAVA_HOME:-}" ] || [ ! -d "$JAVA_HOME" ]; then
    echo -e "${RED}[ERROR] 未找到 JDK 17，请设置 JAVA_HOME 或用 -j 指定${NC}"
    exit 1
fi
echo -e "${GREEN}[INFO] JAVA_HOME = $JAVA_HOME${NC}"

# ======== 1. 提取版本号 ========
BUILD_GRADLE="app/build.gradle.kts"
VERSION=$(grep -oP 'versionName\s*=\s*"\K[^"]+' "$BUILD_GRADLE" | head -1)
if [ -z "$VERSION" ]; then
    echo -e "${RED}[ERROR] 无法从 $BUILD_GRADLE 提取 versionName${NC}"
    exit 1
fi
echo -e "${GREEN}[INFO] 当前版本：V$VERSION${NC}"

# ======== 2. 编译构建 ========
echo ""
echo -e "${YELLOW}[BUILD] 开始编译...${NC}"

if [ -f "./gradlew" ]; then
    GRADLE="./gradlew"
elif [ -f "./gradlew.bat" ]; then
    GRADLE="./gradlew.bat"
else
    echo -e "${RED}[ERROR] 未找到 gradlew${NC}"
    exit 1
fi

chmod +x "$GRADLE" 2>/dev/null || true

# 实时输出编译日志
"$GRADLE" assembleDebug --console=plain --no-daemon
BUILD_EXIT=$?

if [ $BUILD_EXIT -ne 0 ]; then
    echo ""
    echo -e "${RED}[FAIL] 编译失败，退出码 $BUILD_EXIT${NC}"
    exit $BUILD_EXIT
fi

APK_PATH="app/build/outputs/apk/debug/Polish_V${VERSION}.apk"
if [ ! -f "$APK_PATH" ]; then
    echo -e "${RED}[FAIL] APK 未找到：$APK_PATH${NC}"
    exit 1
fi

APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
echo ""
echo -e "${GREEN}[OK] 编译成功！${NC}"
echo -e "${GREEN}[APK] $APK_PATH ($APK_SIZE)${NC}"

# ======== 3. Git 状态检查 ========
echo ""
echo -e "${YELLOW}[GIT] 状态检查...${NC}"
BRANCH=$(git branch --show-current 2>/dev/null || echo "unknown")
STATUS=$(git status --porcelain 2>/dev/null || true)
echo -e "${GRAY}[GIT] 当前分支：$BRANCH${NC}"
if [ -n "$STATUS" ]; then
    echo -e "${YELLOW}[WARN] 有未提交的改动：${NC}"
    echo -e "${GRAY}$STATUS${NC}"
fi

# ======== 4. 是否发布 Release ========
if [ "$SKIP_RELEASE" = true ]; then
    echo ""
    echo -e "${CYAN}[DONE] 跳过发布（--skip-release）${NC}"
    exit 0
fi

echo ""
read -r -p "是否发布 GitHub Release V$VERSION？[y/N] " ANSWER
if [[ ! "$ANSWER" =~ ^[Yy] ]]; then
    echo -e "${CYAN}[DONE] 跳过发布，构建产物：$APK_PATH${NC}"
    exit 0
fi

# 检查 gh CLI
if ! command -v gh &>/dev/null; then
    echo -e "${RED}[FAIL] 未找到 gh CLI，请先安装：https://cli.github.com/${NC}"
    exit 1
fi

# 推送标签
if ! git tag -l "V$VERSION" | grep -q . ; then
    echo -e "${YELLOW}[GIT] 创建标签 V$VERSION...${NC}"
    git tag "V$VERSION"
    git push origin "V$VERSION"
else
    echo -e "${GRAY}[GIT] 标签 V$VERSION 已存在，跳过${NC}"
fi

# 创建 Release
echo -e "${YELLOW}[RELEASE] 正在上传 APK 并创建 Release...${NC}"

read -r -p "Release 标题（回车使用默认）：" RELEASE_TITLE
RELEASE_TITLE="${RELEASE_TITLE:-V$VERSION}"

read -r -p "Release 说明（回车使用模板）：" RELEASE_BODY
RELEASE_BODY="${RELEASE_BODY:-Polish V$VERSION 发布}"

if gh release create "V$VERSION" "$APK_PATH" --title "$RELEASE_TITLE" --notes "$RELEASE_BODY"; then
    echo ""
    echo -e "${GREEN}[DONE] 发布成功！${NC}"
else
    echo -e "${RED}[FAIL] 发布失败${NC}"
    exit 1
fi
