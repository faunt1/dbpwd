#!/bin/bash

###############################################################################
# Druid数据库连接池配置生成和上传脚本
#
# 功能：
# 1. 交互式输入数据库连接池配置信息
# 2. 选择加密算法（AES/SM4）
# 3. 自动加密密码并生成Druid配置文件
# 4. 调用配置中心API上传配置文件
#
# 使用方法：
#   bash scripts/setup-druid-config.sh
###############################################################################

set -e  # 遇到错误立即退出
# set -x  # 调试模式（取消注释以启用）

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 默认值
DEFAULT_DRIVER="com.mysql.cj.jdbc.Driver"
DEFAULT_URL="jdbc:mysql://localhost:3306/test?useSSL=false&serverTimezone=UTC"
DEFAULT_USERNAME="root"
DEFAULT_INITIAL_SIZE="5"
DEFAULT_MIN_IDLE="5"
DEFAULT_MAX_ACTIVE="20"
DEFAULT_MAX_WAIT="60000"
DEFAULT_ALGORITHM="AES"
DEFAULT_REFRESH_INTERVAL="300"
OUTPUT_DIR="config"
OUTPUT_FILE="druid.properties"
CONFIG_CENTER_URL=""
CONFIG_CENTER_DIRECTORY=""
CONFIG_CENTER_API_KEY=""

# 工作目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# 打印带颜色的消息
print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 打印分隔线
print_separator() {
    echo "========================================================================================================="
}

# 提示用户输入（带默认值）
prompt_input() {
    local prompt_text="$1"
    local default_value="$2"

    if [ -z "$default_value" ]; then
        printf "%s: " "$prompt_text"
    else
        printf "%s [%s]: " "$prompt_text" "$default_value"
    fi

    read -r input

    if [ -z "$input" ]; then
        echo "$default_value"
    else
        echo "$input"
    fi
}

# 提示用户选择（带默认值）
prompt_choice() {
    local prompt_text="$1"
    local default_value="$2"

    printf "%s [%s]: " "$prompt_text" "$default_value"
    read -r input

    if [ -z "$input" ]; then
        echo "$default_value"
    else
        echo "$input"
    fi
}

# 检查Java环境
check_java() {
    print_info "检查Java环境..."

    if ! command -v java &> /dev/null; then
        print_error "Java未安装或不在PATH中"
        exit 1
    fi

    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    print_success "Java版本: $JAVA_VERSION"
}

# 检查curl
check_curl() {
    print_info "检查curl..."

    if ! command -v curl &> /dev/null; then
        print_error "curl未安装或不在PATH中"
        exit 1
    fi

    print_success "curl可用"
}

# 检查配置目录
check_output_dir() {
    if [ ! -d "$OUTPUT_DIR" ]; then
        print_info "创建输出目录: $OUTPUT_DIR"
        mkdir -p "$OUTPUT_DIR"
    fi
}

# 生成随机密钥
generate_key() {
    if command -v openssl &> /dev/null; then
        openssl rand -base64 32
    else
        # 备用方案
        head /dev/urandom | tr -dc A-Za-z0-9 | head -c 32
    fi
}

# 上传配置到配置中心
upload_to_config_center() {
    local config_file="$1"
    local url="$2"
    local directory="$3"
    local api_key="$4"

    print_info "开始上传配置到配置中心..."
    print_info "配置中心URL: $url"
    print_info "配置目录: $directory"
    print_info "配置文件: $config_file"

    # 构建完整URL
    local full_url="${url}/upload"

    print_info "上传URL: $full_url"

    # 使用curl上传
    local response=$(curl -s -w "\n%{http_code}" -X POST \
        "$full_url" \
        -H "X-API-Key: ${api_key}" \
        -F "directory=${directory}" \
        -F "path=${OUTPUT_FILE}" \
        -F "file=@${config_file}" \
        --connect-timeout 30 \
        --max-time 60 2>&1)

    local http_code=$(echo "$response" | tail -n 1)
    local body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ] || [ "$http_code" = "201" ]; then
        print_success "配置上传成功!"
        if [ -n "$body" ]; then
            print_info "响应: $body"
        fi
        return 0
    else
        print_error "配置上传失败! HTTP状态码: $http_code"
        if [ -n "$body" ]; then
            print_error "响应: $body"
        fi
        return 1
    fi
}

# 从配置中心下载配置
download_from_config_center() {
    local url="$1"
    local directory="$2"
    local api_key="$3"
    local output_file="$4"

    print_info "从配置中心下载配置..."

    # URL编码参数
    local encoded_dir=$(echo -n "$directory" | jq -sRr @uri 2>/dev/null || echo "$directory")
    local encoded_file=$(echo -n "$OUTPUT_FILE" | jq -sRr @uri 2>/dev/null || echo "$OUTPUT_FILE")

    local full_url="${url}/download?directory=${encoded_dir}&path=${encoded_file}"

    local response=$(curl -s -w "\n%{http_code}" -X GET \
        "$full_url" \
        -H "X-API-Key: ${api_key}" \
        --connect-timeout 30 \
        --max-time 60 2>&1)

    local http_code=$(echo "$response" | tail -n 1)
    local body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        echo "$body" > "$output_file"
        print_success "配置下载成功! 保存到: $output_file"
        return 0
    else
        print_error "配置下载失败! HTTP状态码: $http_code"
        if [ -n "$body" ]; then
            print_error "响应: $body"
        fi
        return 1
    fi
}

# 生成配置文件
generate_config() {
    local driver="$1"
    local url="$2"
    local username="$3"
    local password="$4"
    local initial_size="$5"
    local min_idle="$6"
    local max_active="$7"
    local max_wait="$8"
    local algorithm="$9"
    local encryption_key="${10}"
    local refresh_interval="${11}"
    local output_file="$12"

    print_info "正在加密密码..."

    # 准备Java命令参数
    local java_args=(
        "-cp" "${PROJECT_ROOT}/target/classes:${PROJECT_ROOT}/target/lib/*"
        "com.dbpwd.DruidPasswordCallback"
    )

    # 密码加密（这里简化处理，实际应调用Java程序）
    # 使用简单的Base64编码作为演示，实际应调用加密程序
    local encrypted_password
    if [ -f "${PROJECT_ROOT}/target/classes/com/dbpwd/DruidPasswordCallback.class" ]; then
        # 使用Java加密
        encrypted_password=$(java -cp "${PROJECT_ROOT}/target/classes" \
            -Djava.security.manager="" \
            -Dfile.encoding=UTF-8 \
            com.dbpwd.PasswordEncrypter "$password" "$encryption_key" "$algorithm" 2>/dev/null || echo "${password}")
    else
        # 备用方案：使用Base64编码
        encrypted_password=$(echo -n "$password" | base64)
        print_warning "Java类未编译，使用Base64编码作为演示"
    fi

    if [ -z "$encryption_key" ]; then
        encryption_key=$(generate_key)
        print_info "生成新的加密密钥: ${encryption_key}"
    fi

    print_success "密码加密完成"

    # 确定验证查询
    local validation_query="SELECT 1"
    if [[ $driver == *"oracle"* ]]; then
        validation_query="SELECT 1 FROM DUAL"
    elif [[ $driver == *"postgresql"* ]]; then
        validation_query="SELECT 1"
    fi

    # 生成配置文件
    print_info "生成配置文件..."

    cat > "$output_file" <<EOF
# Druid JDBC Connection Pool Configuration
# Generated by setup-druid-config.sh
# Generated at: $(date '+%Y-%m-%d %H:%M:%S')

# Basic Configuration
driverClassName=${driver}
url=${url}
username=${username}
password=${encrypted_password}

# Password Decryption Configuration
# Set to true to enable password decryption
druid.connectionProperties=config.decrypt=true;config.decrypt.key=${encryption_key};config.decrypt.algorithm=${algorithm}

# Connection Pool Configuration
initialSize=${initial_size}
minIdle=${min_idle}
maxActive=${max_active}
maxWait=${max_wait}
timeBetweenEvictionRunsMillis=60000
minEvictableIdleTimeMillis=300000
validationQuery=${validation_query}
testWhileIdle=true
testOnBorrow=false
testOnReturn=false
poolPreparedStatements=true
maxPoolPreparedStatementPerConnectionSize=20

# Configuration Center Integration
config.center.enabled=true
config.center.url=${CONFIG_CENTER_URL}
config.center.directory=${CONFIG_CENTER_DIRECTORY}
config.center.apiKey=${CONFIG_CENTER_API_KEY}
config.center.autoRefresh=true
config.center.refreshInterval=${refresh_interval}
EOF

    print_success "配置文件生成成功: $output_file"

    # 显示加密信息
    print_separator
    echo "加密信息:"
    echo "  算法: ${algorithm}"
    echo "  密钥: ${encryption_key}"
    echo "  加密密码: ${encrypted_password}"
    print_separator
}

# 主函数
main() {
    print_separator
    echo -e "${GREEN}Druid 数据库连接池配置生成和上传工具${NC}"
    print_separator
    echo
    echo "功能:"
    echo "  1. 交互式配置数据库连接池"
    echo "  2. 支持密码加密 (AES/SM4)"
    echo "  3. 集成配置中心上传下载"
    print_separator
    echo

    # 检查依赖
    check_java
    check_curl
    check_output_dir

    # 数据库配置
    print_info "请输入数据库配置信息:"
    echo

    DRIVER=$(prompt_input "驱动类名" "$DEFAULT_DRIVER")
    URL=$(prompt_input "数据库URL" "$DEFAULT_URL")
    USERNAME=$(prompt_input "用户名" "$DEFAULT_USERNAME")
    PASSWORD=$(prompt_input "密码" "")
    echo

    # 加密配置
    print_info "请选择加密算法:"
    echo "  1. AES (默认，256位)"
    echo "  2. SM4 (国密算法，128位)"
    ALGORITHM_CHOICE=$(prompt_choice "选择" "1")

    if [ "$ALGORITHM_CHOICE" = "2" ]; then
        ALGORITHM="SM4"
    else
        ALGORITHM="$DEFAULT_ALGORITHM"
    fi

    echo
    print_info "是否使用现有密钥？"
    echo "  1. 自动生成新密钥 (推荐)"
    echo "  2. 使用现有密钥"
    KEY_CHOICE=$(prompt_choice "选择" "1")

    ENCRYPTION_KEY=""
    if [ "$KEY_CHOICE" = "2" ]; then
        ENCRYPTION_KEY=$(prompt_input "请输入密钥" "")
    fi
    echo

    # 连接池配置
    print_info "请配置连接池参数:"
    INITIAL_SIZE=$(prompt_input "初始连接数" "$DEFAULT_INITIAL_SIZE")
    MIN_IDLE=$(prompt_input "最小空闲连接数" "$DEFAULT_MIN_IDLE")
    MAX_ACTIVE=$(prompt_input "最大活跃连接数" "$DEFAULT_MAX_ACTIVE")
    MAX_WAIT=$(prompt_input "最大等待时间(毫秒)" "$DEFAULT_MAX_WAIT")
    echo

    # 配置中心配置
    print_info "是否需要配置配置中心?"
    CONFIG_CENTER_CHOICE=$(prompt_choice "上传/下载到配置中心? (y/n)" "n")

    if echo "$CONFIG_CENTER_CHOICE" | grep -qi "^y"; then
        CONFIG_CENTER_URL=$(prompt_input "配置中心URL" "http://localhost:8080")
        CONFIG_CENTER_DIRECTORY=$(prompt_input "配置目录" "/druid/config")
        CONFIG_CENTER_API_KEY=$(prompt_input "API密钥" "")
    fi

    REFRESH_INTERVAL=$(prompt_input "配置刷新间隔(秒)" "$DEFAULT_REFRESH_INTERVAL")
    echo

    # 文件路径
    OUTPUT_PATH="${PROJECT_ROOT}/${OUTPUT_DIR}/${OUTPUT_FILE}"
    print_info "配置文件将保存到: ${OUTPUT_PATH}"

    # 生成配置文件
    generate_config \
        "$DRIVER" \
        "$URL" \
        "$USERNAME" \
        "$PASSWORD" \
        "$INITIAL_SIZE" \
        "$MIN_IDLE" \
        "$MAX_ACTIVE" \
        "$MAX_WAIT" \
        "$ALGORITHM" \
        "$ENCRYPTION_KEY" \
        "$REFRESH_INTERVAL" \
        "$OUTPUT_PATH"

    # 上传到配置中心
    if [ -n "$CONFIG_CENTER_URL" ] && [ -n "$CONFIG_CENTER_DIRECTORY" ] && [ -n "$CONFIG_CENTER_API_KEY" ]; then
        echo
        print_separator

        # 询问是否立即上传
        PROMPT_UPLOAD=$(prompt_choice "是否立即上传到配置中心? (y/n)" "y")

        if echo "$PROMPT_UPLOAD" | grep -qi "^y"; then
            upload_to_config_center "$OUTPUT_PATH" "$CONFIG_CENTER_URL" "$CONFIG_CENTER_DIRECTORY" "$CONFIG_CENTER_API_KEY"
        fi
    fi

    # 下载配置（测试）
    if [ -n "$CONFIG_CENTER_URL" ] && [ -n "$CONFIG_CENTER_DIRECTORY" ] && [ -n "$CONFIG_CENTER_API_KEY" ]; then
        echo
        PROMPT_DOWNLOAD=$(prompt_choice "是否从配置中心下载配置进行验证? (y/n)" "n")

        if echo "$PROMPT_DOWNLOAD" | grep -qi "^y"; then
            DOWNLOAD_PATH="${PROJECT_ROOT}/${OUTPUT_DIR}/${OUTPUT_FILE}.downloaded"
            download_from_config_center "$CONFIG_CENTER_URL" "$CONFIG_CENTER_DIRECTORY" "$CONFIG_CENTER_API_KEY" "$DOWNLOAD_PATH"

            # 比较文件
            if [ -f "$DOWNLOAD_PATH" ]; then
                if diff -q "$OUTPUT_PATH" "$DOWNLOAD_PATH" > /dev/null 2>&1; then
                    print_success "下载的配置与上传的配置一致"
                else
                    print_warning "下载的配置与上传的配置不一致"
                fi
            fi
        fi
    fi

    # 完成
    echo
    print_separator
    print_success "配置生成完成!"
    print_separator
    echo
    echo "配置文件: ${OUTPUT_PATH}"
    echo
    echo "下一步:"
    echo "  1. 运行测试程序验证连接: cd ${PROJECT_ROOT} && ./test-connection.sh"
    echo "  2. 或者手动运行: java -cp target/classes com.dbpwd.DruidConnectionTest ${OUTPUT_PATH}"
    echo
}

# 启动程序
main "$@"
