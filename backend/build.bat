@echo off
setlocal
echo ========================================================
echo   CSMS Backend Build Script (Windows)
echo ========================================================

:: 环境要求：JDK 17 或更高版本。
:: 工程已在 Lombok 1.18.46 下验证过 JDK 17 与 JDK 26 均可正常编译，
:: 无需再手动切换 JAVA_HOME。
echo [INFO] JAVA_HOME = %JAVA_HOME%
echo [INFO] java -version:
java -version 2>&1
echo.

call mvn clean compile -DskipTests

if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] Backend compiled successfully!
) else (
    echo [ERROR] Build failed. Please check that JDK is 17 or newer.
)

endlocal