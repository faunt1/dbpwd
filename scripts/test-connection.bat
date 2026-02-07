@echo off
REM ============================================================================
REM Druid连接池测试脚本 (Windows版本)
REM
REM 功能:
REM   1. 测试Druid连接池配置是否正确
REM   2. 验证密码加密/解密功能
REM   3. 测试数据库连接
REM
REM 使用方法:
REM   scripts\test-connection.bat [config_file_path]
REM ============================================================================

setlocal enabledelayedexpansion

REM 脚本目录
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR:~0,-8%"

REM 默认配置文件
set "DEFAULT_CONFIG=config\druid.properties"
set "TEST_CONFIG=config\druid_test.properties"

REM 处理命令行参数
if "%~1"=="" (
    if exist "%PROJECT_ROOT%\%DEFAULT_CONFIG%" (
        set "CONFIG_FILE=%PROJECT_ROOT%\%DEFAULT_CONFIG%"
    ) else if exist "%PROJECT_ROOT%\%TEST_CONFIG%" (
        set "CONFIG_FILE=%PROJECT_ROOT%\%TEST_CONFIG%"
    ) else (
        set "CONFIG_FILE=%PROJECT_ROOT%\%DEFAULT_CONFIG%"
    )
) else (
    if "%~1:~1,1%"=="\" (
        set "CONFIG_FILE=%~1"
    ) else (
        set "CONFIG_FILE=%PROJECT_ROOT%\%~1"
    )
)

REM 颜色
for /f %%a in ('echo prompt $E ^| cmd') do set "ESC=%%a"
set "RED=%ESC%[31m"
set "GREEN=%ESC%[32m"
set "YELLOW=%ESC%[33m"
set "BLUE=%ESC%[34m"
set "NC=%ESC%[0m"

echo.
echo %BLUE%==========================================================================%NC%
echo %GREEN%                    Druid 连接池测试工具%NC%
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

REM 检查配置文件
echo %BLUE%[INFO]%NC% 检查配置文件...
if not exist "%CONFIG_FILE%" (
    echo %RED%[ERROR]%NC% 配置文件不存在: %CONFIG_FILE%
    echo.
    echo %BLUE%[INFO]%NC% 请先运行以下命令生成配置:
    echo   scripts\setup-druid-config.bat
    echo.
    pause
    exit /b 1
)
echo %GREEN%[SUCCESS]%NC% 使用配置文件: %CONFIG_FILE%
echo.

REM 检查项目编译
echo %BLUE%[INFO]%NC% 检查项目编译状态...
if not exist "%PROJECT_ROOT%\target\classes\com\dbpwd\DruidConnectionPool.class" (
    echo %BLUE%[INFO]%NC% 项目未编译，开始编译...
    cd /d "%PROJECT_ROOT%"

    where mvn >nul 2>&1
    if errorlevel 1 (
        echo %RED%[ERROR]%NC% Maven未安装，无法编译项目
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

REM 运行测试
echo %BLUE%==========================================================================%NC%
echo %BLUE%[INFO]%NC% 开始连接测试...
echo %BLUE%==========================================================================%NC%
echo.

REM 设置类路径
set "CP=%PROJECT_ROOT%\target\classes"

REM 添加Maven依赖
set "LOCAL_MAVEN_REPO=%USERPROFILE%\.m2\repository"

REM 添加Druid依赖
if exist "%LOCAL_MAVEN_REPO%\com\alibaba\druid\1.2.20\druid-1.2.20.jar" (
    set "CP=!CP!;%LOCAL_MAVEN_REPO%\com\alibaba\druid\1.2.20\druid-1.2.20.jar"
)

REM 添加Commons Lang依赖
if exist "%LOCAL_MAVEN_REPO%\commons-lang\commons-lang\2.6\commons-lang-2.6.jar" (
    set "CP=!CP!;%LOCAL_MAVEN_REPO%\commons-lang\commons-lang\2.6\commons-lang-2.6.jar"
)

REM 添加SLF4J依赖
if exist "%LOCAL_MAVEN_REPO%\org\slf4j\slf4j-api\2.0.9\slf4j-api-2.0.9.jar" (
    set "CP=!CP!;%LOCAL_MAVEN_REPO%\org\slf4j\slf4j-api\2.0.9\slf4j-api-2.0.9.jar"
)

REM 添加Logback依赖
if exist "%LOCAL_MAVEN_REPO%\ch\qos\logback\logback-classic\1.4.11\logback-classic-1.4.11.jar" (
    set "CP=!CP!;%LOCAL_MAVEN_REPO%\ch\qos\logback\logback-classic\1.4.11\logback-classic-1.4.11.jar"
)

REM 检查MySQL驱动
set "HAS_MYSQL_DRIVER=0"
if exist "%LOCAL_MAVEN_REPO%\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar" (
    set "CP=!CP!;%LOCAL_MAVEN_REPO%\com\mysql\mysql-connector-j\8.0.33\mysql-connector-j-8.0.33.jar"
    set "HAS_MYSQL_DRIVER=1"
)

if "!HAS_MYSQL_DRIVER!"=="0" (
    echo %YELLOW%[WARNING]%NC% MySQL驱动未找到，数据库测试可能失败
    echo %BLUE%[INFO]%NC% 如需测试数据库连接，请运行: mvn dependency:resolve
    echo.
)

cd /d "%PROJECT_ROOT%"

REM 运行测试
echo %BLUE%[INFO]%NC% 执行测试...
echo.

java -cp "%CP%" ^
    -Dtest.config="%CONFIG_FILE%" ^
    com.dbpwd.DruidConnectionTest "%CONFIG_FILE%"

set "EXIT_CODE=%errorlevel%"

echo.
echo %BLUE%==========================================================================%NC%
if !EXIT_CODE! == 0 (
    echo %GREEN%[SUCCESS]%NC% 所有测试通过!
) else (
    echo %RED%[ERROR]%NC% 测试失败，退出码: !EXIT_CODE!
)
echo %BLUE%==========================================================================%NC%

echo.
pause
exit /b %EXIT_CODE%
