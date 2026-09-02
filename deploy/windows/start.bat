@echo off
chcp 65001 >nul
setlocal
REM 手动前台启动（调试用）。生产环境请使用 WinSW 服务（install-service.bat）。
REM
REM 如需在本地覆盖密钥，可在此设置（建议改为 Windows“系统环境变量”以便服务也能读取）：
REM set JDBC_PASSWORD=YourStrongPassword
REM set ERP_APPSECRET=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
java -jar "%~dp0bom-drawing-system.jar"
endlocal
