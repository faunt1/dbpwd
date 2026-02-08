@echo off
REM ============================================================================
REM Druid数据库密码管理工具测试脚本 (Windows版本)
REM
REM 功能:
REM   1. 测试数据库连接
REM   2. 验证密码加密/解密功能
REM
REM 使用方法:
REM   scripts\test-connection.bat
REM ============================================================================

setlocal enabledelayedexpansion

REM 脚本目录
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR:~0,-8%"

REM 颜色
for /f %%a in ('echo prompt $E ^| cmd') do set "ESC=%%a"
set "RED=%ESC%[31m"
set "GREEN=%ESC%[32m"
set "YELLOW=%ESC%[33m"
set "BLUE=%ESC%[34m"
set "NC=%ESC%[0m"

echo.
echo %BLUE%==========================================================================%NC%
echo %GREEN%              Druid数据库密码管理工具 v3.1%NC%
echo %BLUE%==========================================================================%NC%
echo.

REM 检查Java
echo %BLUE%[INFO]%NC% 检查Java环境...
where java >nul 2>&1
if errorlevel 1 (
    echo %RED%[ERROR]%NC% Java未安装或不在PATH中
    pause
    exit /b 1
)
for /f "tokens=3" %%i in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set "JAVA_VER=%%i"
)
echo %GREEN%[SUCCESS]%NC% Java版本: !JAVA_VER!

REM 检查项目编译
echo %BLUE%[INFO]%NC% 检查项目编译状态...
if not exist "%PROJECT_ROOT%\target\classes\com\dbpwd\cli\DBPasswordCLI.class" (
    echo %BLUE%[INFO]%NC% 项目未编译，开始编译...
    cd /d "%PROJECT_ROOT%"

    where mvn >nul 2>&1
    if errorlevel 1 (
        echo %RED%[ERROR]%NC% Maven未安装，无法编译项目
        echo.
        echo %BLUE%[INFO]%NC% 可以使用以下方式安装Maven:
        echo   - 下载: https://maven.apache.org/download.cgi
        echo   - 解压并配置PATH环境变量
        echo.
        pause
        exit /b 1
    )

    call mvn clean compile -DskipTests
    if errorlevel 1 (
        echo %RED%[ERROR]%NC% 编译失败
        pause
        exit /b 1
    )
    echo %GREEN%[SUCCESS]%NC% 项目编译完成
) else (
    echo %GREEN%[SUCCESS]%NC% 项目已编译
)
echo.

REM 编译并运行测试
echo %BLUE%==========================================================================%NC%
echo %BLUE%[INFO]%NC% 运行连接测试...
echo %BLUE%==========================================================================%NC%
echo.

cd /d "%PROJECT_ROOT%"

REM 运行单元测试
echo %BLUE%[INFO]%NC% 运行单元测试...
echo.
call mvn test
if errorlevel 1 (
    echo %RED%[WARNING]%NC% 单元测试未通过，可能需要配置测试数据库
    echo.
)

echo.
echo %BLUE%[INFO]%NC% 启动交互式命令行工具...
echo.

REM 设置类路径
set "CP=%PROJECT_ROOT%\target\classes"
set "LOCAL_MAVEN_REPO=%USERPROFILE%\.m2\repository"

REM 添加依赖
for /r "%LOCAL_MAVEN_REPO%\com\dbpwd" %%j in (*.jar) do (
    set "CP=!CP!;%%j"
)

REM 运行CLI (交互模式)
java -cp "%CP%" com.dbpwd.cli.DBPasswordCLI

set "EXIT_CODE=%errorlevel%"

echo.
echo %BLUE%==========================================================================%NC%
echo %GREEN%[SUCCESS]%NC% 工具已退出
echo %BLUE%==========================================================================%NC%

echo.
pause
