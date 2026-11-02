@echo off
echo ============================================================
echo    Pocket Ledger - بناء ملف APK للتثبيت المباشر
echo ============================================================
echo.

REM --- Check for Java ---
where java >NUL 2>&1
if %ERRORLEVEL% neq 0 (
    if defined JAVA_HOME (
        echo [INFO] Using JAVA_HOME: %JAVA_HOME%
    ) else (
        echo [ERROR] لم يتم العثور على Java!
        echo.
        echo الرجاء تثبيت JDK 17 أو أحدث من:
        echo   https://adoptium.net/temurin/releases/
        echo.
        echo أو تثبيته عبر Android Studio:
        echo   File ^> Settings ^> Build ^> Gradle ^> Gradle JDK
        echo.
        pause
        exit /b 1
    )
)

echo [1/4] جاري تنظيف المشروع...
call gradlew.bat clean
if %ERRORLEVEL% neq 0 (
    echo [ERROR] فشل في تنظيف المشروع
    echo.
    echo قد تحتاج لتحميل Gradle Wrapper أولاً:
    echo   gradle wrapper --gradle-version=9.2.0
    echo.
    pause
    exit /b 1
)

echo.
echo [2/4] جاري بناء ملف APK...
call gradlew.bat assembleDebug --no-daemon
if %ERRORLEVEL% neq 0 (
    echo [ERROR] فشل في بناء ملف APK
    echo.
    echo تأكد من وجود Android SDK مثبت
    echo وأن ANDROID_HOME أو ANDROID_SDK_ROOT معرّف
    echo.
    pause
    exit /b 1
)

echo.
echo [3/4] جاري البحث عن ملف APK...

set APK_PATH=app\build\outputs\apk\debug\app-debug.apk

if exist "%APK_PATH%" (
    echo.
    echo ============================================================
    echo    ✅ تم بناء ملف APK بنجاح!
    echo ============================================================
    echo.
    echo    📁 مسار الملف:
    echo    %CD%\%APK_PATH%
    echo.
    
    REM --- Copy APK to root for easy access ---
    echo [4/4] جاري نسخ APK إلى مجلد المشروع...
    copy "%APK_PATH%" "PocketLedger-debug.apk" >NUL 2>&1
    
    if exist "PocketLedger-debug.apk" (
        echo.
        echo    📱 تم نسخ الملف إلى:
        echo    %CD%\PocketLedger-debug.apk
    )
    
    echo.
    echo ============================================================
    echo    طرق التثبيت:
    echo ============================================================
    echo.
    echo    الطريقة 1: عبر USB
    echo      adb install PocketLedger-debug.apk
    echo.
    echo    الطريقة 2: عبر نقل الملف
    echo      انسخ PocketLedger-debug.apk إلى هاتفك
    echo      ثم افتحه من مدير الملفات
    echo      ^(تأكد من تفعيل "التثبيت من مصادر غير معروفة"^)
    echo.
    echo    الطريقة 3: عبر المحاكي
    echo      adb -e install PocketLedger-debug.apk
    echo.
) else (
    echo [ERROR] لم يتم العثور على ملف APK في المسار المتوقع
    echo جاري البحث في مجلدات أخرى...
    dir /s /b app\build\outputs\*.apk 2>NUL
)

echo.
pause
