# ============================================================
#    Pocket Ledger - بناء ملف APK للتثبيت المباشر
# ============================================================

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "   Pocket Ledger - APK Builder" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# --- Check for Java ---
$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCmd) {
    if ($env:JAVA_HOME) {
        $javaPath = Join-Path $env:JAVA_HOME "bin\java.exe"
        if (Test-Path $javaPath) {
            Write-Host "[OK] Java found at JAVA_HOME: $env:JAVA_HOME" -ForegroundColor Green
        } else {
            Write-Host "[ERROR] JAVA_HOME is set but java.exe not found!" -ForegroundColor Red
            Write-Host ""
            Write-Host "Please install JDK 17+ from: https://adoptium.net/temurin/releases/" -ForegroundColor Yellow
            exit 1
        }
    } else {
        Write-Host "[ERROR] Java not found!" -ForegroundColor Red
        Write-Host ""
        Write-Host "Please install JDK 17+ from:" -ForegroundColor Yellow
        Write-Host "  https://adoptium.net/temurin/releases/" -ForegroundColor White
        Write-Host ""
        Write-Host "Or use Android Studio's bundled JDK:" -ForegroundColor Yellow
        Write-Host "  File > Settings > Build > Gradle > Gradle JDK" -ForegroundColor White
        exit 1
    }
} else {
    $javaVersion = & java -version 2>&1 | Select-Object -First 1
    Write-Host "[OK] Java found: $javaVersion" -ForegroundColor Green
}

# --- Check for Android SDK ---
$androidHome = $env:ANDROID_HOME
if (-not $androidHome) { $androidHome = $env:ANDROID_SDK_ROOT }
if (-not $androidHome) {
    $defaultPaths = @(
        "$env:LOCALAPPDATA\Android\Sdk",
        "$env:USERPROFILE\AppData\Local\Android\Sdk",
        "C:\Android\Sdk"
    )
    foreach ($path in $defaultPaths) {
        if (Test-Path $path) {
            $androidHome = $path
            $env:ANDROID_HOME = $path
            break
        }
    }
}

if ($androidHome -and (Test-Path $androidHome)) {
    Write-Host "[OK] Android SDK found: $androidHome" -ForegroundColor Green
} else {
    Write-Host "[WARN] Android SDK not found in standard locations" -ForegroundColor Yellow
    Write-Host "  The build may still work if local.properties points to the SDK" -ForegroundColor Yellow
}

Write-Host ""

# --- Check local.properties ---
$localProps = Join-Path $PSScriptRoot "local.properties"
if (-not (Test-Path $localProps)) {
    if ($androidHome) {
        Write-Host "[INFO] Creating local.properties with SDK path..." -ForegroundColor Cyan
        $sdkDir = $androidHome -replace '\\', '/'
        "sdk.dir=$sdkDir" | Out-File -FilePath $localProps -Encoding UTF8 -NoNewline
        Write-Host "[OK] local.properties created" -ForegroundColor Green
    } else {
        Write-Host "[WARN] No local.properties and no ANDROID_HOME found" -ForegroundColor Yellow
    }
}

# --- Check Gradle Wrapper ---
$wrapperJar = Join-Path $PSScriptRoot "gradle\wrapper\gradle-wrapper.jar"
if (-not (Test-Path $wrapperJar)) {
    Write-Host "[WARN] gradle-wrapper.jar not found. Attempting download..." -ForegroundColor Yellow
    
    $wrapperUrl = "https://services.gradle.org/distributions/gradle-9.2.0-bin.zip"
    Write-Host "  If the build fails, install Gradle 9.2.0 and run:" -ForegroundColor Yellow
    Write-Host "  gradle wrapper --gradle-version=9.2.0" -ForegroundColor White
    Write-Host ""
}

# --- Build ---
Write-Host "[1/3] Cleaning project..." -ForegroundColor Cyan
$gradlew = Join-Path $PSScriptRoot "gradlew.bat"

& $gradlew clean 2>&1 | ForEach-Object { Write-Host "  $_" }

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[ERROR] Clean failed. Check errors above." -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "[2/3] Building Debug APK..." -ForegroundColor Cyan
Write-Host "  This may take several minutes on first build..." -ForegroundColor Yellow
Write-Host ""

& $gradlew assembleDebug --no-daemon --stacktrace 2>&1 | ForEach-Object { Write-Host "  $_" }

if ($LASTEXITCODE -ne 0) {
    Write-Host ""
    Write-Host "[ERROR] Build failed. Check errors above." -ForegroundColor Red
    exit 1
}

# --- Find APK ---
Write-Host ""
Write-Host "[3/3] Locating APK..." -ForegroundColor Cyan

$apkPath = Join-Path $PSScriptRoot "app\build\outputs\apk\debug\app-debug.apk"

if (Test-Path $apkPath) {
    $destPath = Join-Path $PSScriptRoot "PocketLedger-debug.apk"
    Copy-Item $apkPath $destPath -Force
    
    $fileSize = [math]::Round((Get-Item $destPath).Length / 1MB, 2)
    
    Write-Host ""
    Write-Host "============================================================" -ForegroundColor Green
    Write-Host "   ✅ APK Built Successfully!" -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "   File: PocketLedger-debug.apk" -ForegroundColor White
    Write-Host "   Size: ${fileSize} MB" -ForegroundColor White
    Write-Host "   Path: $destPath" -ForegroundColor White
    Write-Host ""
    Write-Host "   Installation Methods:" -ForegroundColor Cyan
    Write-Host "   ─────────────────────" -ForegroundColor DarkCyan
    Write-Host ""
    Write-Host "   1. USB (via ADB):" -ForegroundColor Yellow
    Write-Host "      adb install PocketLedger-debug.apk" -ForegroundColor White
    Write-Host ""
    Write-Host "   2. File Transfer:" -ForegroundColor Yellow
    Write-Host "      Copy PocketLedger-debug.apk to your phone" -ForegroundColor White
    Write-Host "      Open it from the file manager" -ForegroundColor White
    Write-Host "      (Enable 'Install from unknown sources')" -ForegroundColor DarkYellow
    Write-Host ""
    Write-Host "   3. Emulator:" -ForegroundColor Yellow
    Write-Host "      adb -e install PocketLedger-debug.apk" -ForegroundColor White
    Write-Host ""
} else {
    Write-Host "[ERROR] APK not found at expected path" -ForegroundColor Red
    Write-Host "  Looking for APK files..." -ForegroundColor Yellow
    Get-ChildItem -Path (Join-Path $PSScriptRoot "app\build\outputs") -Filter "*.apk" -Recurse -ErrorAction SilentlyContinue | ForEach-Object {
        Write-Host "  Found: $($_.FullName)" -ForegroundColor White
    }
}
