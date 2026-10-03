@echo off
setlocal
echo ========================================================
echo   CSMS Backend Run Script (Windows)
echo ========================================================
echo [INFO] JAVA_HOME = %JAVA_HOME%
echo.

:: 数据库密码不写入仓库，改为运行时输入
if "%CSMS_DB_PASSWORD%"=="" (
    set /p "CSMS_DB_PASSWORD=请输入 MySQL root 密码（输入内容不回显属正常，密码仅存于本次会话）: "
    echo.
)
if "%CSMS_DB_USERNAME%"=="" set "CSMS_DB_USERNAME=root"

echo [INFO] 连接目标: %CSMS_DB_USERNAME%@localhost:3306/csms
echo [INFO] 库不存在时会自动创建，Flyway 随后执行 V1/V2 迁移
echo.

call mvn spring-boot:run

endlocal