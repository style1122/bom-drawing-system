@echo off
chcp 65001 >nul
setlocal
cd /d %~dp0

echo ============================================
echo [1/2] 构建后端 (Maven) ...
echo ============================================
call mvn clean package -DskipTests
if errorlevel 1 ( echo [失败] 后端构建出错 & exit /b 1 )

echo ============================================
echo [2/2] 构建前端 (Vue 3 + Vite) ...
echo ============================================
cd frontend
call npm install
if errorlevel 1 ( echo [失败] npm install 出错 & exit /b 1 )
call npm run build
if errorlevel 1 ( echo [失败] 前端构建出错 & exit /b 1 )

cd /d %~dp0
echo.
echo 构建完成：
echo   后端 jar : backend\target\bom-drawing-system.jar
echo   前端 dist: frontend\dist
echo.
echo 部署步骤见 DEPLOY.md
endlocal
