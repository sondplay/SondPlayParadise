@echo off
setlocal enabledelayedexpansion

echo ============================================
echo   $ond'Play Paradise - Build Script
echo ============================================
echo.

set "PROJECT_DIR=%~dp0"
set "SRC_DIR=%PROJECT_DIR%src\main\java"
set "RES_DIR=%PROJECT_DIR%src\main\resources"
set "BUILD_DIR=%PROJECT_DIR%build"
set "OUT_JAR=%BUILD_DIR%\SondPlayParadise-1.0.0.jar"

:: Dependencies
set "ASM_JAR=C:\PolyMC\libraries\org\ow2\asm\asm-all\5.0.3\asm-all-5.0.3.jar"
set "FORGE_JAR=C:\PolyMC\libraries\net\minecraftforge\forge\1.7.10-10.13.4.1614-1.7.10\forge-1.7.10-10.13.4.1614-1.7.10-universal.jar"
set "LAUNCHWRAPPER=C:\PolyMC\libraries\net\minecraft\launchwrapper\1.12\launchwrapper-1.12.jar"
set "LOG4J_API=C:\PolyMC\libraries\org\apache\logging\log4j\log4j-api\2.0-beta9-fixed\log4j-api-2.0-beta9-fixed.jar"
set "UNIMIXINS=C:\PolyMC\instances\Modpack Edredom\.minecraft\mods\+unimixins-all-1.7.10-0.3.1.jar"
set "STUBS_DIR=C:\orespawn-patch\suspatch-stubs"

:: Classpath
set "CP=%ASM_JAR%;%FORGE_JAR%;%LAUNCHWRAPPER%;%LOG4J_API%;%UNIMIXINS%;%STUBS_DIR%"

:: Clean
echo [1/4] Cleaning build directory...
if exist "%BUILD_DIR%\classes" rd /s /q "%BUILD_DIR%\classes"
mkdir "%BUILD_DIR%\classes" 2>nul

:: Compile
echo [2/4] Compiling Java sources...
dir /s /b "%SRC_DIR%\*.java" > "%BUILD_DIR%\sources.txt"

javac -source 1.8 -target 1.8 -encoding UTF-8 -proc:none ^
  -cp "%CP%" ^
  -d "%BUILD_DIR%\classes" ^
  @"%BUILD_DIR%\sources.txt"

if errorlevel 1 (
    echo.
    echo [ERROR] Compilation failed!
    exit /b 1
)
echo    Compiled successfully.

:: Copy resources
echo [3/4] Copying resources...
xcopy /s /y /q "%RES_DIR%\*" "%BUILD_DIR%\classes\" >nul

:: Package JAR
echo [4/4] Packaging JAR...
if exist "%OUT_JAR%" del "%OUT_JAR%"
cd /d "%BUILD_DIR%\classes"
jar cfm "%OUT_JAR%" META-INF\MANIFEST.MF .

if errorlevel 1 (
    echo.
    echo [ERROR] JAR packaging failed!
    exit /b 1
)

echo.
echo ============================================
echo   BUILD SUCCESSFUL
echo   Output: %OUT_JAR%
echo ============================================
echo.
echo To install:
echo   1. Copy %OUT_JAR% to .minecraft\mods\
echo   2. Remove: suspatch-1.0.jar, orespawntweaks-v5.3.jar
echo   3. Restore original mod jars (backups in C:\orespawn-patch\)
echo.

endlocal
