# Polish Build & Release Script (Windows PowerShell)
# Usage: .\build.ps1 [-JAVA_HOME "path\to\jdk17"] [-SkipRelease]

param(
    [string]$JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot",
    [switch]$SkipRelease
)

$ErrorActionPreference = "Stop"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $ScriptDir

Write-Host "" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Polish Build Script" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan

# ---- Extract version ----
$gradleContent = Get-Content "app\build.gradle.kts" -Raw
$pattern = 'versionName\s*=\s*\x22(.+?)\x22'
if ($gradleContent -match $pattern) {
    $VERSION = $matches[1]
} else {
    Write-Host "[ERROR] Cannot find versionName in build.gradle.kts" -ForegroundColor Red
    exit 1
}
Write-Host "[INFO] Version: V$VERSION" -ForegroundColor Green

# ---- Build ----
Write-Host ""
Write-Host "[BUILD] Compiling..." -ForegroundColor Yellow
$env:JAVA_HOME = $JAVA_HOME
Write-Host "[INFO] JAVA_HOME = $JAVA_HOME" -ForegroundColor Gray

& .\gradlew.bat assembleDebug --console=plain 2>&1
if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[FAIL] Build failed (exit $LASTEXITCODE)" -ForegroundColor Red
    exit $LASTEXITCODE
}

$apkPath = "app\build\outputs\apk\debug\Polish_V$VERSION.apk"
if (-not (Test-Path $apkPath)) {
    Write-Host "[FAIL] APK not found: $apkPath" -ForegroundColor Red
    exit 1
}

$apkSize = [math]::Round((Get-Item $apkPath).Length / 1MB, 2)
Write-Host ""
Write-Host "[OK] Build succeeded!" -ForegroundColor Green
Write-Host "[APK] $apkPath ($apkSize MB)" -ForegroundColor Green

# ---- Git status ----
Write-Host ""
Write-Host "[GIT] Checking status..." -ForegroundColor Yellow
$branch = git branch --show-current
Write-Host "[GIT] Branch: $branch" -ForegroundColor Gray
$dirty = git status --porcelain
if ($dirty) {
    Write-Host "[WARN] Uncommitted changes:" -ForegroundColor Yellow
    Write-Host $dirty -ForegroundColor Gray
}

# ---- Release? ----
if ($SkipRelease) {
    Write-Host ""
    Write-Host "[DONE] SkipRelease set, exiting." -ForegroundColor Cyan
    exit 0
}

Write-Host ""
$answer = Read-Host "Publish GitHub Release V$VERSION? [y/N]"
if ($answer -notmatch '^[yY]') {
    Write-Host "[DONE] APK: $apkPath" -ForegroundColor Cyan
    exit 0
}

# Check gh
$gh = Get-Command gh -ErrorAction SilentlyContinue
if (-not $gh) {
    Write-Host "[FAIL] gh CLI not found. Install: https://cli.github.com/" -ForegroundColor Red
    exit 1
}

# Tag
$existing = git tag -l "V$VERSION"
if (-not $existing) {
    Write-Host "[GIT] Creating tag V$VERSION..." -ForegroundColor Yellow
    git tag "V$VERSION"
    git push origin "V$VERSION"
} else {
    Write-Host "[GIT] Tag V$VERSION already exists" -ForegroundColor Gray
}

# Create release
Write-Host "[RELEASE] Uploading APK and creating release..." -ForegroundColor Yellow

$title = Read-Host "Release title (Enter for default)"
if (-not $title) { $title = "V$VERSION" }

$body = Read-Host "Release notes (Enter for template)"
if (-not $body) { $body = "Polish V$VERSION" }

# Use --% to pass args literally
$result = gh release create "V$VERSION" $apkPath --title $title --notes $body 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "[DONE] Published: $result" -ForegroundColor Green
} else {
    Write-Host "[FAIL] Release creation failed:" -ForegroundColor Red
    Write-Host $result -ForegroundColor DarkRed
    exit 1
}
