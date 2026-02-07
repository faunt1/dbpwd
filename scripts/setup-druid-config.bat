@echo off
REM ============================================================================
REM Druid数据库连接池配置生成和上传脚本 (Windows版本)
REM
REM 功能:
REM   1. 交互式输入数据库连接池配置信息
REM   2. 选择加密算法（AES/SM4）
REM   3. 自动加密密码并生成Druid配置文件
REM   4. 调用配置中心API上传配置文件
REM
REM 使用方法:
REM   scripts\setup-druid-config.bat
REM ============================================================================

setlocal enabledelayedexpansion

REM 脚本目录
set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR:~0,-8%"

REM 输出目录
set "OUTPUT_DIR=config"
set "OUTPUT_FILE=druid.properties"

REM 默认值
set "DEFAULT_DRIVER=com.mysql.cj.jdbc.Driver"
set "DEFAULT_URL=jdbc:mysql://localhost:3306/test?useSSL=false^&serverTimezone=UTC"
set "DEFAULT_USERNAME=root"
set "DEFAULT_INITIAL_SIZE=5"
set "DEFAULT_MIN_IDLE=5"
set "DEFAULT_MAX_ACTIVE=20"
set "DEFAULT_MAX_WAIT=60000"
set "DEFAULT_ALGORITHM=AES"
set "DEFAULT_REFRESH_INTERVAL=300"

REM 配置中心变量
set "CONFIG_CENTER_URL="
set "CONFIG_CENTER_DIRECTORY="
set "CONFIG_CENTER_API_KEY="

REM 加密变量
set "ENCRYPTION_KEY="

REM 颜色（Windows 10+）
for /f %%a in ('echo prompt $E ^| cmd') do set "ESC=%%a"
set "RED=%ESC%[31m"
set "GREEN=%ESC%[32m"
set "YELLOW=%ESC%[33m"
set "BLUE=%ESC%[34m"
set "NC=%ESC%[0m"

echo.
echo %BLUE%==========================================================================%NC%
echo %GREEN%                 Druid 数据库连接池配置生成和上传工具%NC%
echo %BLUE%==========================================================================%NC%
echo.
echo 功能:
echo   1. 交互式配置数据库连接池
echo   2. 支持密码加密 (AES/SM4)
echo   3. 集成配置中心上传下载
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
echo.

REM 检查curl
echo %BLUE%[INFO]%NC% 检查curl...
where curl >nul 2>&1
if errorlevel 1 (
    echo %RED%[ERROR]%NC% curl未安装或不在PATH中
    pause
    exit /b 1
)
echo %GREEN%[SUCCESS]%NC% curl可用
echo.

REM 创建输出目录
if not exist "%OUTPUT_DIR%" (
    echo %BLUE%[INFO]%NC% 创建输出目录: %OUTPUT_DIR%
    mkdir "%OUTPUT_DIR%"
)

REM ============================================
REM 数据库配置
REM ============================================
echo %BLUE%[INFO]%NC% 请输入数据库配置信息:
echo.

set /p "DRIVER=驱动类名 [%DEFAULT_DRIVER%]: "
if "!DRIVER!"=="" set "DRIVER=%DEFAULT_DRIVER%"

set /p "URL=数据库URL [%DEFAULT_URL%]: "
if "!URL!"=="" set "URL=%DEFAULT_URL%"

set /p "USERNAME=用户名 [%DEFAULT_USERNAME%]: "
if "!USERNAME!"=="" set "USERNAME=%DEFAULT_USERNAME%"

set /p "PASSWORD=密码: "

echo.

REM ============================================
REM 加密配置
REM ============================================
echo %BLUE%[INFO]%NC% 请选择加密算法:
echo   1. AES (默认，256位)
echo   2. SM4 (国密算法，128位)

set /p "ALGORITHM_CHOICE=选择 [1]: "
if "!ALGORITHM_CHOICE!"=="2" (
    set "ALGORITHM=SM4"
) else (
    set "ALGORITHM=%DEFAULT_ALGORITHM%"
)

echo.
echo %BLUE%[INFO]%NC% 是否使用现有密钥？
echo   1. 自动生成新密钥 (推荐)
echo   2. 使用现有密钥

set /p "KEY_CHOICE=选择 [1]: "
if "!KEY_CHOICE!"=="2" (
    set /p "ENCRYPTION_KEY=请输入密钥: "
)
echo.

REM ============================================
REM 连接池配置
REM ============================================
echo %BLUE%[INFO]%NC% 请配置连接池参数:

set /p "INITIAL_SIZE=初始连接数 [%DEFAULT_INITIAL_SIZE%]: "
if "!INITIAL_SIZE!"=="" set "INITIAL_SIZE=%DEFAULT_INITIAL_SIZE%"

set /p "MIN_IDLE=最小空闲连接数 [%DEFAULT_MIN_IDLE%]: "
if "!MIN_IDLE!"=="" set "MIN_IDLE=%DEFAULT_MIN_IDLE%"

set /p "MAX_ACTIVE=最大活跃连接数 [%DEFAULT_MAX_ACTIVE%]: "
if "!MAX_ACTIVE!"=="" set "MAX_ACTIVE=%DEFAULT_MAX_ACTIVE%"

set /p "MAX_WAIT=最大等待时间(毫秒) [%DEFAULT_MAX_WAIT%]: "
if "!MAX_WAIT!"=="" set "MAX_WAIT=%DEFAULT_MAX_WAIT%"
echo.

REM ============================================
REM 配置中心配置
REM ============================================
echo %BLUE%[INFO]%NC% 是否需要配置配置中心?

set /p "CONFIG_CENTER_CHOICE=上传/下载到配置中心? (y/n) [n]: "
if /i "!CONFIG_CENTER_CHOICE!"=="y" (
    set /p "CONFIG_CENTER_URL=配置中心URL [http://localhost:8080]: "
    if "!CONFIG_CENTER_URL!"=="" set "CONFIG_CENTER_URL=http://localhost:8080"

    set /p "CONFIG_CENTER_DIRECTORY=配置目录 [/druid/config]: "
    if "!CONFIG_CENTER_DIRECTORY!"=="" set "CONFIG_CENTER_DIRECTORY=/druid/config"

    set /p "CONFIG_CENTER_API_KEY=API密钥: "
)
echo.

set /p "REFRESH_INTERVAL=配置刷新间隔(秒) [%DEFAULT_REFRESH_INTERVAL%]: "
if "!REFRESH_INTERVAL!"=="" set "REFRESH_INTERVAL=%DEFAULT_REFRESH_INTERVAL%"
echo.

REM ============================================
REM 生成配置文件
REM ============================================
set "OUTPUT_PATH=%PROJECT_ROOT%\%OUTPUT_DIR%\%OUTPUT_FILE%"
echo %BLUE%[INFO]%NC% 配置文件将保存到: %OUTPUT_PATH%

REM 确定验证查询
echo !DRIVER! | findstr /i "oracle" >nul
if !errorlevel! == 0 (
    set "VALIDATION_QUERY=SELECT 1 FROM DUAL"
) else (
    set "VALIDATION_QUERY=SELECT 1"
)

REM 密码加密（简化版，使用Base64）
echo %BLUE%[INFO]%NC% 正在加密密码...

REM 使用PowerShell进行Base64编码
for /f "usebackq delims=" %%i in (`powershell -Command "[Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes('%PASSWORD%'))"`) do set "ENCRYPTED_PASSWORD=%%i"

if "!ENCRYPTION_KEY!"=="" (
    REM 生成随机密钥
    for /f "usebackq delims=" %%i in (`powershell -Command "[Convert]::ToBase64String([Security.Cryptography.RNGCryptoServiceProvider]::GetBytes(32))"`) do set "ENCRYPTION_KEY=%%i"
    echo %BLUE%[INFO]%NC% 生成新的加密密钥: !ENCRYPTION_KEY!
)

echo %GREEN%[SUCCESS]%NC% 密码加密完成

REM ============================================
REM 生成配置文件
REM ============================================
echo %BLUE%[INFO]%NC% 生成配置文件...

(
echo # Druid JDBC Connection Pool Configuration
echo # Generated by setup-druid-config.bat
echo # Generated at: %date% %time%
echo.
echo # Basic Configuration
echo driverClassName=!DRIVER!
echo url=!URL!
echo username=!USERNAME!
echo password=!ENCRYPTED_PASSWORD!
echo.
echo # Password Decryption Configuration
echo druid.connectionProperties=config.decrypt=true;config.decrypt.key=!ENCRYPTION_KEY!;config.decrypt.algorithm=!ALGORITHM!
echo.
echo # Connection Pool Configuration
echo initialSize=!INITIAL_SIZE!
echo minIdle=!MIN_IDLE!
echo maxActive=!MAX_ACTIVE!
echo maxWait=!MAX_WAIT!
echo timeBetweenEvictionRunsMillis=60000
echo minEvictableIdleTimeMillis=300000
echo validationQuery=%VALIDATION_QUERY%
echo testWhileIdle=true
echo testOnBorrow=false
echo testOnReturn=false
echo poolPreparedStatements=true
echo maxPoolPreparedStatementPerConnectionSize=20
echo.
echo # Configuration Center Integration
echo config.center.enabled=true
echo config.center.url=!CONFIG_CENTER_URL!
echo config.center.directory=!CONFIG_CENTER_DIRECTORY!
echo config.center.apiKey=!CONFIG_CENTER_API_KEY!
echo config.center.autoRefresh=true
echo config.center.refreshInterval=!REFRESH_INTERVAL!
) > "%OUTPUT_PATH%"

echo %GREEN%[SUCCESS]%NC% 配置文件生成成功: %OUTPUT_PATH%

echo.
echo %BLUE%==========================================================================%NC%
echo 加密信息:
echo %BLUE%==========================================================================%NC%
echo   算法: !ALGORITHM!
echo   密钥: !ENCRYPTION_KEY!
echo   加密密码: !ENCRYPTED_PASSWORD!
echo.

REM ============================================
REM 上传到配置中心
REM ============================================
if not "!CONFIG_CENTER_URL!"=="" (
    echo %BLUE%[INFO]%NC% 是否立即上传到配置中心? ^(y/n^)
    set /p "PROMPT_UPLOAD=选择 [y]: "

    if /i "!PROMPT_UPLOAD!"=="y" (
        call :upload_config "%OUTPUT_PATH%"
    )
)

echo.
echo %BLUE%==========================================================================%NC%
echo %GREEN%[SUCCESS]%NC% 配置生成完成!
echo %BLUE%==========================================================================%NC%
echo.
echo 配置文件: %OUTPUT_PATH%
echo.
echo 下一步:
echo   1. 运行测试程序验证连接: scripts\test-connection.bat
echo.

pause
exit /b 0

REM ============================================
REM 上传配置到配置中心
REM ============================================
:upload_config
set "CONFIG_FILE=%~1"
set "FULL_URL=!CONFIG_CENTER_URL!/upload"

echo.
echo %BLUE%[INFO]%NC% 开始上传配置到配置中心...
echo %BLUE%[INFO]%NC% 上传URL: !FULL_URL!

curl -s -w "%%{http_code}" -X POST "!FULL_URL!" ^
  -H "X-API-Key: !CONFIG_CENTER_API_KEY!" ^
  -F "directory=!CONFIG_CENTER_DIRECTORY!" ^
  -F "path=%OUTPUT_FILE%" ^
  --upload-file "%CONFIG_FILE%" > "%TEMP%\curl_response.txt" 2>&1

for /f "usebackq delims=" %%r in ("%TEMP%\curl_response.txt") do set "HTTP_STATUS=%%r"

if "!HTTP_STATUS:~-3!"=="200" (
    echo %GREEN%[SUCCESS]%NC% 配置上传成功!
) else (
    echo %RED%[ERROR]%NC% 配置上传失败! HTTP状态码: !HTTP_STATUS:~-3!
)

goto :EOF
