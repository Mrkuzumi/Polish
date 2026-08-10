# Polish - Build & Release Script (Windows PowerShell 5.1+)
# Usage: .\build.ps1 [-JAVA_HOME "path\to\jdk17"] [-SkipRelease]
param(
    [string]$JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot",
    [switch]$SkipRelease
)

$ErrorActionPreference = "Continue"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

# ===== helpers =====
function AskFix($reason, $cmd) {
    Write-Host "  [REASON] $reason" -ForegroundColor Red
    Write-Host "  [FIX CMD] $cmd" -ForegroundColor Yellow
    # Handle non-interactive environments (CI / automated tests)
    try {
        $a = Read-Host "  Run this fix command? [y/N]"
    } catch {
        Write-Host "  Non-interactive mode - skipping" -ForegroundColor Gray
        return $false
    }
    if ($a -match '^[yY]') {
        Write-Host "  [EXEC] $cmd" -ForegroundColor Gray
        Invoke-Expression $cmd
        return $LASTEXITCODE -eq 0
    }
    return $false
}

function CheckCmd($name, $reason, $fix) {
    $c = Get-Command $name -ErrorAction SilentlyContinue
    if (-not $c) {
        Write-Host "[WARN] '$name' not found" -ForegroundColor Yellow
        if (AskFix $reason $fix) {
            return (Get-Command $name -ErrorAction SilentlyContinue) -ne $null
        }
        return $false
    }
    return $true
}

# ===== banner =====
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Polish Build & Release Script" -ForegroundColor Cyan
Write-Host "  (Polish - Jian / Mining Tracker)" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# ===== extract version =====
$gradleContent = Get-Content "app\build.gradle.kts" -Raw
if ($gradleContent -match 'versionName\s*=\s*\x22(.+?)\x22') {
    $VERSION = $matches[1]
} else {
    Write-Host "[ERROR] Cannot find versionName in build.gradle.kts" -ForegroundColor Red
    exit 1
}
Write-Host "[INFO] Current version: V$VERSION" -ForegroundColor Green

# ===== 0. pre-flight: git =====
Write-Host ""
Write-Host "[CHECK] Git ..." -ForegroundColor Yellow
$gitOk = Get-Command git -ErrorAction SilentlyContinue
if (-not $gitOk) {
    Write-Host "[WARN] Git not found" -ForegroundColor Yellow
    if (AskFix "Git is not installed or not in PATH. Required for version control." "winget install --id Git.Git -e --source winget") {
        $env:PATH = [Environment]::GetEnvironmentVariable("PATH", "Machine") + ";" + [Environment]::GetEnvironmentVariable("PATH", "User")
    } else {
        Write-Host "[ABORT] Git is required to continue" -ForegroundColor Red
        exit 1
    }
}
Write-Host "  git ready" -ForegroundColor Gray

# ===== 1. pre-flight: JDK =====
Write-Host "[CHECK] JDK 17 ..." -ForegroundColor Yellow

$javaExe = "$JAVA_HOME\bin\java.exe"
if (-not (Test-Path $javaExe)) {
    Write-Host "[WARN] Default JAVA_HOME not found: $JAVA_HOME" -ForegroundColor Yellow
    $found = $false
    $commonPaths = @(
        "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot",
        "C:\Program Files\Eclipse Adoptium\jdk-17.0.13.11-hotspot",
        "C:\Program Files\Java\jdk-17",
        "C:\Program Files\Microsoft\jdk-17.0.12.7-hotspot"
    )
    foreach ($p in $commonPaths) {
        if (Test-Path "$p\bin\java.exe") { $JAVA_HOME = $p; $found = $true; break }
    }
    if (-not $found) {
        Write-Host "[WARN] No JDK 17 found on this system" -ForegroundColor Yellow
        if (AskFix "JDK 17 is not installed or path is incorrect. Install via winget or manually." "winget install EclipseAdoptium.Temurin.17.JDK") {
            $foundDirs = @(Get-ChildItem "C:\Program Files\Eclipse Adoptium" -Directory -ErrorAction SilentlyContinue | Where-Object { Test-Path "$_\bin\java.exe" } | Select-Object -First 1)
            if ($foundDirs) { $JAVA_HOME = $foundDirs.FullName; $found = $true }
        }
    }
    if (-not $found) {
        Write-Host "[ABORT] Cannot proceed without JDK 17. Install manually and retry." -ForegroundColor Red
        exit 1
    }
    Write-Host "  Switched to: $JAVA_HOME" -ForegroundColor Green
}
$env:JAVA_HOME = $JAVA_HOME
$env:PATH = "$JAVA_HOME\bin;$env:PATH"

$javaVer = & "$JAVA_HOME\bin\java" -version 2>&1 | Select-Object -First 1
Write-Host "  $javaVer" -ForegroundColor Gray

# ===== 2. pre-flight: Android SDK =====
Write-Host "[CHECK] Android SDK ..." -ForegroundColor Yellow

$sdkPath = $null
if (Test-Path "local.properties") {
    $lp = Get-Content "local.properties" -Raw
    if ($lp -match 'sdk\.dir\s*=\s*(.+)') {
        # Unescape Java properties format: \: -> :, \\ -> \
        $sdkPath = $matches[1].Trim() -replace '\\:', ':' -replace '\\\\', '\' -replace '/', '\'
    }
}
if ((-not $sdkPath) -or (-not (Test-Path $sdkPath))) {
    $candidates = @(
        "$env:LOCALAPPDATA\Android\Sdk",
        "D:\app\AndroidStudioSDK",
        "$env:ANDROID_SDK_ROOT",
        "$env:ANDROID_HOME"
    )
    foreach ($c in $candidates) {
        if ($c -and (Test-Path $c)) { $sdkPath = $c; break }
    }
}

if ($sdkPath -and (Test-Path $sdkPath)) {
    Write-Host "  SDK = $sdkPath" -ForegroundColor Gray
    if (-not (Test-Path "local.properties")) {
        $sdkDirNorm = $sdkPath -replace '\\', '\\'
        Set-Content -Path "local.properties" -Value "sdk.dir=$sdkDirNorm" -Encoding utf8
        Write-Host "  Wrote local.properties" -ForegroundColor Gray
    }
    $env:ANDROID_HOME = $sdkPath
} else {
    if (-not (AskFix "Android SDK not found. Install Android Studio and API 35, or set ANDROID_HOME." "Write-Host 'Please install Android Studio: https://developer.android.com/studio' -ForegroundColor Yellow")) {
        Write-Host "[ABORT] Android SDK is required" -ForegroundColor Red
        exit 1
    }
    exit 1
}

# ===== 3. pre-flight: gradlew =====
Write-Host "[CHECK] Gradle wrapper ..." -ForegroundColor Yellow
if (-not (Test-Path ".\gradlew.bat")) {
    Write-Host "[ERROR] gradlew.bat missing" -ForegroundColor Red
    if (AskFix "Gradle wrapper (gradlew.bat) not found. This project requires it to build." "Write-Host 'Please clone the repository again or restore gradlew.bat from git.' -ForegroundColor Yellow") {
        exit 1
    }
    exit 1
}
Write-Host "  gradlew.bat ready" -ForegroundColor Gray

# ===== 4. build =====
Write-Host ""
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Building (assembleDebug) ..." -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$buildOutput = & .\gradlew.bat assembleDebug --console=plain --stacktrace 2>&1
$exitCode = $LASTEXITCODE

# Show output
$buildOutput | ForEach-Object { Write-Host $_ -ForegroundColor Gray }

if ($exitCode -ne 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Red
    Write-Host "  BUILD FAILED (exit $exitCode)" -ForegroundColor Red
    Write-Host "========================================" -ForegroundColor Red
    Write-Host ""

    $outputStr = $buildOutput -join ' '

    if ($outputStr -match 'Could not resolve|Could not get|Connect exception|UnknownHost|timeout|Network') {
        Write-Host "[ANALYSIS] Network / dependency download failure" -ForegroundColor Red
        if (AskFix "Gradle cannot download dependencies. Check network or proxy config in ~/.gradle/gradle.properties." "Write-Host 'Check: 1) Internet connection  2) Proxy settings in ~/.gradle/gradle.properties (systemProp.https.proxyHost/Port)' -ForegroundColor Yellow") { exit 0 }
    }
    elseif ($outputStr -match 'Unsupported class file|UnsupportedClassVersionError|invalid source release') {
        Write-Host "[ANALYSIS] JDK version mismatch" -ForegroundColor Red
        if (AskFix "Wrong JDK version (requires JDK 17). Current JH=$JAVA_HOME." "Write-Host 'Install JDK 17: winget install EclipseAdoptium.Temurin.17.JDK' -ForegroundColor Yellow") { exit 0 }
    }
    elseif ($outputStr -match 'Android SDK|android\.jar|SDK location|compileSdk|sdkmanager') {
        Write-Host "[ANALYSIS] Android SDK config issue" -ForegroundColor Red
        if (AskFix "SDK path is wrong or API 35 is missing. Use SDK Manager to install API 35." "Write-Host 'Open Android Studio > SDK Manager > install Android API 35' -ForegroundColor Yellow") { exit 0 }
    }
    elseif ($outputStr -match 'FileNotFoundException|Access is denied') {
        Write-Host "[ANALYSIS] File permission or lock issue" -ForegroundColor Red
        if (AskFix "File is locked or access denied. Close programs using build/ directory." "Remove-Item -Recurse -Force app\build -ErrorAction SilentlyContinue; Write-Host 'Cleaned app/build directory' -ForegroundColor Green") { exit 0 }
    }
    elseif ($outputStr -match 'OutOfMemoryError|GC overhead') {
        Write-Host "[ANALYSIS] Gradle out of memory" -ForegroundColor Red
        if (AskFix "Gradle heap size is too small. Increase to 2048m." "Add-Content gradle.properties 'org.gradle.jvmargs=-Xmx2048m'") { exit 0 }
    }
    else {
        Write-Host "[ANALYSIS] Unknown build error. Check output above." -ForegroundColor Red
    }
    exit $exitCode
}

$apkPath = "app\build\outputs\apk\debug\Polish_V$VERSION.apk"
if (-not (Test-Path $apkPath)) {
    Write-Host "[ERROR] APK not generated: $apkPath" -ForegroundColor Red
    exit 1
}

$apkSize = [math]::Round((Get-Item $apkPath).Length / 1MB, 2)
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  BUILD SUCCESS!" -ForegroundColor Green
Write-Host "  APK: $apkPath" -ForegroundColor Green
Write-Host "  Size: $apkSize MB" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

# ===== 5. git status =====
Write-Host ""
Write-Host "[GIT] Status check ..." -ForegroundColor Yellow
$branch = git branch --show-current
Write-Host "  Branch: $branch" -ForegroundColor Gray

$remote = git remote get-url origin 2>$null
if (-not $remote) {
    if (-not (AskFix "No git remote 'origin' configured. Cannot push or release." "git remote add origin https://github.com/Mrkuzumi/Polish.git")) {
        Write-Host "[WARN] Skipping Git operations" -ForegroundColor Yellow
        $SkipRelease = $true
    }
}

$dirty = git status --porcelain
if ($dirty) {
    Write-Host "[WARN] Uncommitted changes:" -ForegroundColor Yellow
    Write-Host $dirty -ForegroundColor Gray
    if (AskFix "There are uncommitted changes. Commit all now?" "git add -A; git commit -m 'build: V$VERSION'") {
        Write-Host "  Committed" -ForegroundColor Green
    }
}

# ===== 6. release =====
if ($SkipRelease) {
    Write-Host ""
    Write-Host "[DONE] Skipping release (--SkipRelease)" -ForegroundColor Cyan
    Write-Host "  APK: $apkPath" -ForegroundColor Cyan
    exit 0
}

Write-Host ""
$answer = Read-Host "Publish GitHub Release V$VERSION? [y/N]"
if ($answer -notmatch '^[yY]') {
    Write-Host "[DONE] Skipped. APK: $apkPath" -ForegroundColor Cyan
    exit 0
}

# ===== 6a. check gh =====
Write-Host ""
Write-Host "[CHECK] gh CLI ..." -ForegroundColor Yellow
if (-not (CheckCmd "gh" "gh CLI is not installed. Required for creating GitHub Releases." "winget install --id GitHub.cli -e")) {
    exit 1
}

$ghAuth = gh auth status 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host "[WARN] gh not authenticated" -ForegroundColor Yellow
    if (AskFix "gh CLI is not logged into GitHub. Login via browser?" "gh auth login --web") {
        Write-Host "  Complete login in browser, then press Enter..." -ForegroundColor Yellow
        Read-Host
    } else {
        Write-Host "[ABORT] gh login required" -ForegroundColor Red
        exit 1
    }
}
Write-Host "  gh ready" -ForegroundColor Gray

# ===== 6b. push & tag =====
Write-Host "[GIT] Pushing code ..." -ForegroundColor Yellow
git push origin $branch 2>&1
if ($LASTEXITCODE -ne 0) {
    if (-not (AskFix "Git push failed. Check network and repository permissions." "Write-Host 'Check: 1) Internet connection  2) GitHub repo write access' -ForegroundColor Yellow")) {
        Write-Host "[ABORT] Push failed" -ForegroundColor Red
        exit 1
    }
}

$existing = git tag -l "V$VERSION"
if ($existing) {
    Write-Host "[WARN] Tag V$VERSION already exists, removing and re-creating..." -ForegroundColor Yellow
    git tag -d "V$VERSION" 2>$null
    git push origin ":refs/tags/V$VERSION" 2>$null
}

git tag "V$VERSION"
git push origin "V$VERSION" 2>&1
if ($LASTEXITCODE -ne 0) {
    if (-not (AskFix "Tag push failed. Check network and repository permissions." "Write-Host 'Check: 1) Internet connection  2) GitHub repo write access' -ForegroundColor Yellow")) {
        Write-Host "[ABORT] Tag push failed" -ForegroundColor Red
        exit 1
    }
}
Write-Host "  Tag V$VERSION pushed" -ForegroundColor Green

# ===== 6c. create release =====
Write-Host ""
Write-Host "[RELEASE] Creating GitHub Release ..." -ForegroundColor Yellow

$title = Read-Host "Release title (Enter for default)"
if (-not $title) { $title = "V$VERSION" }

$body = Read-Host "Release notes (Enter for template)"
if (-not $body) { $body = "Polish V$VERSION release" }

Write-Host "[UPLOAD] Uploading APK ..." -ForegroundColor Gray
$result = gh release create "V$VERSION" $apkPath --title $title --notes $body 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "  RELEASE PUBLISHED!" -ForegroundColor Green
    Write-Host "  $result" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "[FAIL] Release creation failed" -ForegroundColor Red
    $resultStr = $result -join ' '
    if ($resultStr -match 'already exists|409') {
        Write-Host "[REASON] Release V$VERSION already exists" -ForegroundColor Red
        if (AskFix "A release with this tag already exists. Delete old release and retry?" "gh release delete V$VERSION --yes") {
            $result2 = gh release create "V$VERSION" $apkPath --title $title --notes $body 2>&1
            if ($LASTEXITCODE -eq 0) { Write-Host "[OK] Published!" -ForegroundColor Green } else { Write-Host "[FAIL] Still failed" -ForegroundColor Red; exit 1 }
        }
    } elseif ($resultStr -match 'validation|422') {
        Write-Host "[REASON] Invalid tag name or parameters" -ForegroundColor Red
        Write-Host $result -ForegroundColor DarkRed
    } elseif ($resultStr -match 'Could not resolve|connect|timeout|SSL') {
        Write-Host "[REASON] Network connection failure" -ForegroundColor Red
        if (AskFix "Network error. Check internet/proxy and retry." "Write-Host 'Check Clash/proxy settings, then run the release step again.' -ForegroundColor Yellow") { exit 0 }
    } else {
        Write-Host "[REASON] Unknown error:" -ForegroundColor Red
        Write-Host $result -ForegroundColor DarkRed
    }
    exit 1
}
