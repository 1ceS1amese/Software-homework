@echo off
setlocal
echo ========================================================
echo   CSMS 开发数据库 (原生 MySQL 8)
echo ========================================================
echo.
echo 说明：
echo   MySQL 安装路径必须通过本地环境变量 CSMS_MYSQL_HOME 配置。
echo   数据目录可通过 CSMS_MYSQL_DATA 配置，默认保存在项目 .local 下。
echo   首次使用须自行初始化独立数据目录并设置数据库密码。
echo   关闭本窗口即停止数据库服务。
echo.

if "%CSMS_MYSQL_HOME%"=="" (
    echo [ERROR] 请在本地设置 CSMS_MYSQL_HOME，禁止将实际路径或凭据写入脚本。
    exit /b 1
)
if "%CSMS_MYSQL_DATA%"=="" set "CSMS_MYSQL_DATA=%~dp0.local\mysql-win\data"
if "%CSMS_DB_PORT%"=="" set "CSMS_DB_PORT=3307"

if not exist "%CSMS_MYSQL_HOME%\bin\mysqld.exe" (
    echo [ERROR] CSMS_MYSQL_HOME 中没有 mysqld.exe。
    exit /b 1
)
if not exist "%CSMS_MYSQL_DATA%\auto.cnf" (
    echo [ERROR] 数据目录尚未初始化；本脚本不会覆盖已有数据。
    exit /b 1
)

"%CSMS_MYSQL_HOME%\bin\mysqld.exe" --basedir="%CSMS_MYSQL_HOME%" --datadir="%CSMS_MYSQL_DATA%" --port=%CSMS_DB_PORT% --bind-address=127.0.0.1 --mysqlx=OFF --console

endlocal
