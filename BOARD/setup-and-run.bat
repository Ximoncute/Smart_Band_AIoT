@echo off
setlocal EnableExtensions
cd /d "%~dp0"
set "PROJECT_ROOT=%CD%"
set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"
set "ANDROID_SDK_ROOT=%ANDROID_HOME%"
set "MONGO_EXE="
set "JAVA_EXE="

where py >nul 2>&1
if errorlevel 1 call :install Python.Python.3.10
where py >nul 2>&1
if errorlevel 1 (
  echo Python was installed. Open a new terminal and run this file again.
  exit /b 1
)
py -3.10 --version >nul 2>&1
if errorlevel 1 call :install Python.Python.3.10
py -3.10 --version >nul 2>&1
if errorlevel 1 (
  echo Python 3.10 is not available yet. Open a new terminal and rerun.
  exit /b 1
)

set "MONGO_EXE="
for /d %%D in ("%ProgramFiles%\MongoDB\Server\*") do if exist "%%~fD\bin\mongod.exe" set "MONGO_EXE=%%~fD\bin\mongod.exe"
if not defined MONGO_EXE (
  call :install MongoDB.Server
  if errorlevel 1 exit /b 1
  for /d %%D in ("%ProgramFiles%\MongoDB\Server\*") do if exist "%%~fD\bin\mongod.exe" set "MONGO_EXE=%%~fD\bin\mongod.exe"
)
if not defined MONGO_EXE (
  echo MongoDB Server was not found. Check the MongoDB installation and rerun.
  exit /b 1
)

if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_EXE for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-17*" "%ProgramFiles%\Java\jdk-17*" "%ProgramFiles%\Java\jdk-24*") do if exist "%%~fD\bin\java.exe" set "JAVA_HOME=%%~fD"
if not defined JAVA_EXE if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_EXE (
  call :install EclipseAdoptium.Temurin.17.JDK
  if errorlevel 1 exit /b 1
  for /d %%D in ("%ProgramFiles%\Eclipse Adoptium\jdk-17*" "%ProgramFiles%\Java\jdk-17*" "%ProgramFiles%\Java\jdk-24*") do if exist "%%~fD\bin\java.exe" set "JAVA_HOME=%%~fD"
)
if not defined JAVA_EXE if defined JAVA_HOME if exist "%JAVA_HOME%\bin\java.exe" set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if not defined JAVA_EXE (
  echo JDK 17 was not found. Rerun after installation.
  exit /b 1
)
set "PATH=%JAVA_HOME%\bin;%PATH%"

if not exist "%ANDROID_HOME%\cmdline-tools\latest\bin\sdkmanager.bat" (
  echo Downloading Android SDK command-line tools...
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $sdk=$env:ANDROID_HOME; $zip=Join-Path $env:TEMP 'smartband-android-tools.zip'; $tmp=Join-Path $env:TEMP 'smartband-android-tools'; Invoke-WebRequest 'https://dl.google.com/android/repository/commandlinetools-win-15859902_latest.zip' -OutFile $zip; if(Test-Path $tmp){Remove-Item $tmp -Recurse -Force}; Expand-Archive $zip $tmp; New-Item -ItemType Directory (Join-Path $sdk 'cmdline-tools') -Force | Out-Null; Move-Item (Join-Path $tmp 'cmdline-tools') (Join-Path $sdk 'cmdline-tools\latest'); Remove-Item $zip -Force; Remove-Item $tmp -Recurse -Force"
  if errorlevel 1 exit /b 1
)
if not exist "%ANDROID_HOME%\platform-tools\adb.exe" goto :install_android_packages
if not exist "%ANDROID_HOME%\platforms\android-35" goto :install_android_packages
if not exist "%ANDROID_HOME%\build-tools\35.0.0" goto :install_android_packages
goto :android_ready
:install_android_packages
echo Installing Android SDK packages...
powershell -NoProfile -ExecutionPolicy Bypass -Command "1..30 | ForEach-Object {'y'} | & '%ANDROID_HOME%\cmdline-tools\latest\bin\sdkmanager.bat' --sdk_root='%ANDROID_HOME%' 'platform-tools' 'platforms;android-35' 'build-tools;35.0.0'; exit $LASTEXITCODE"
if errorlevel 1 exit /b 1
:android_ready
set "PATH=%ANDROID_HOME%\platform-tools;%PATH%"

echo Installing backend Python dependencies (including pymongo)...
py -3.10 -m pip install -r "%PROJECT_ROOT%\backend\requirements.txt"
if errorlevel 1 exit /b 1
py -3.10 -m pip install platformio
if errorlevel 1 exit /b 1

echo Building ESP32 firmware dependencies and source...
py -3.10 -m platformio run -d "%PROJECT_ROOT%"
if errorlevel 1 (
  echo Firmware build failed. Check the PlatformIO output above.
  exit /b 1
)

set "ESP_PORT="
set "ESP_PORT_COUNT=0"
for /f "delims=" %%P in ('powershell -NoProfile -ExecutionPolicy Bypass -File "%PROJECT_ROOT%\tools\find_esp_port.ps1"') do (
  set "ESP_PORT=%%P"
  set /a ESP_PORT_COUNT+=1
)
if "%ESP_PORT_COUNT%"=="1" (
  echo ESP32 detected on %ESP_PORT%. Uploading firmware...
  py -3.10 -m platformio run -d "%PROJECT_ROOT%" -t upload --upload-port "%ESP_PORT%"
  if errorlevel 1 (
    echo ESP32 upload failed on %ESP_PORT%. Check USB cable and boot mode.
    exit /b 1
  )
) else if "%ESP_PORT_COUNT%"=="0" (
  echo No ESP32 serial port detected. Firmware is built; connect the board and rerun to upload.
) else (
  echo Multiple ESP32 serial ports detected. Skipping upload to avoid flashing the wrong board.
)

echo Starting MongoDB...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$dir=Join-Path $env:LOCALAPPDATA 'SmartbandMongo'; New-Item -ItemType Directory $dir -Force | Out-Null; if(-not (Test-NetConnection 127.0.0.1 -Port 27017 -InformationLevel Quiet -WarningAction SilentlyContinue)){Start-Process -FilePath $env:MONGO_EXE -ArgumentList @('--dbpath',$dir,'--bind_ip','127.0.0.1','--port','27017') -WindowStyle Hidden; Start-Sleep -Seconds 3}; if(-not (Test-NetConnection 127.0.0.1 -Port 27017 -InformationLevel Quiet -WarningAction SilentlyContinue)){exit 1}"
if errorlevel 1 (
  echo MongoDB did not start.
  exit /b 1
)

echo Building Android APK (EmailJS OTP)...
pushd "%PROJECT_ROOT%\program\android-app"
call gradlew.bat :app:assembleDebug
if errorlevel 1 (
  popd
  exit /b 1
)
popd

echo Checking connected Android device...
set "APK=%PROJECT_ROOT%\program\android-app\app\build\outputs\apk\debug\app-debug.apk"
adb devices
echo Installing APK on the connected Android device. Check the phone for USB installation prompts...
adb install -r "%APK%"
if errorlevel 1 (
  echo APK is ready at %APK%. Authorize USB debugging and rerun to install.
) else (
  echo APK installed successfully. Opening Smartband...
  adb shell monkey -p com.smartband.iotcare 1 >nul
)

echo Backend: http://localhost:8787/admin
echo Admin key: %PROJECT_ROOT%\backend\data\admin.key
echo Keep this window open while using the app.
curl.exe --silent --max-time 2 http://127.0.0.1:8787/health >nul 2>&1
if not errorlevel 1 (
  echo Backend is already running. Open http://localhost:8787/admin
  exit /b 0
)
py -3.10 "%PROJECT_ROOT%\backend\server.py"
exit /b %ERRORLEVEL%

:install
where winget >nul 2>&1
if errorlevel 1 (
  echo Windows Package Manager is required to install %~1.
  echo Install App Installer from Microsoft Store and rerun this file.
  exit /b 1
)
echo Installing %~1 ...
winget install --id %~1 --exact --accept-package-agreements --accept-source-agreements
if errorlevel 1 exit /b 1
exit /b 0
