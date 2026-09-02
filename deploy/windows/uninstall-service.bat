@echo off
setlocal
set APP_HOME=%~dp0

if not exist "%APP_HOME%bom-drawing-system.exe" (
    echo [错误] 未找到 bom-drawing-system.exe
    exit /b 1
)

"%APP_HOME%bom-drawing-system.exe" stop
"%APP_HOME%bom-drawing-system.exe" uninstall
echo 服务已停止并卸载。
endlocal
