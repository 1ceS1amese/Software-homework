@echo off
setlocal
echo ========================================================
echo   CSMS 开发数据库 (MySQL 8.3 @ 127.0.0.1:3307)
echo ========================================================
echo.
echo 说明：
echo   本脚本使用你机器上已有的 MySQL 8.3 二进制，
echo   在独立数据目录中启动一个**独立实例**，端口 3307。
echo   不会影响你原有的 MySQL83 服务（3306）及其数据。
echo.
echo   数据目录: D:\code\homework\.csms-devdb\data
echo   账号密码: root / (空)
echo.
echo   首次使用需先初始化数据目录，见 README「开发数据库」一节。
echo   关闭本窗口即停止数据库服务。
echo.

if not exist "D:\code\homework\.csms-devdb\data" (
    echo [ERROR] 数据目录不存在，请先执行初始化命令（见 README）。
    pause
    exit /b 1
)

"D:\MYSQL Server\bin\mysqld.exe" --basedir="D:\MYSQL Server" --datadir="D:\code\homework\.csms-devdb\data" --port=3307 --bind-address=127.0.0.1 --skip-mysqlx --console

endlocal