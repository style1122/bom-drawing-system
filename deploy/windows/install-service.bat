@echo off
chcp 65001 >nul
setlocal
set APP_HOME=%~dp0

if not exist "%APP_HOME%bom-drawing-system.jar" (
    echo [错误] 未找到 bom-drawing-system.jar，请先将构建产物放到本目录（%APP_HOME%）
    exit /b 1
)
if not exist "%APP_HOME%bom-drawing-system.exe" (
    echo [错误] 未找到 WinSW 可执行文件 bom-drawing-system.exe
    echo 请从 https://github.com/winsw/winsw/releases 下载 WinSW-x64.exe
    echo 并重命名为 bom-drawing-system.exe 放在本目录。
    exit /b 1
)

"%APP_HOME%bom-drawing-system.exe" install
if errorlevel 1 ( echo [失败] 服务注册失败 & exit /b 1 )

echo 服务已注册成功。可启动：
echo   "%APP_HOME%bom-drawing-system.exe" start
echo 或在“服务(services.msc)”中启动 “BOM Drawing System”。
endlocal
