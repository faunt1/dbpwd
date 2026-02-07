#!/bin/bash

###############################################################################
# Druid连接池测试脚本
#
# 功能：
# 1. 测试Druid连接池配置是否正确
# 2. 验证密码加密/解密功能
# 3. 测试数据库连接
#
# 使用方法：
#   bash scripts/test-connection.sh [config_file_path]
###############################################################################

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 脚本目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# 打印函数
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

print_separator() {
    echo "========================================================================================================="
}

# 检查Java
check_java() {
    if ! command -v java &> /dev/null; then
        print_error "Java未安装或不在PATH中"
        exit 1
    fi
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    print_success "Java版本: $JAVA_VERSION"
}

# 检查配置文件
check_config_file() {
    local config_file="$1"

    if [ ! -f "$config_file" ]; then
        print_error "配置文件不存在: $config_file"
        print_info "请先运行: bash scripts/setup-druid-config.sh"
        exit 1
    fi

    print_success "使用配置文件: $config_file"
}

# 编译项目（如果需要）
build_project() {
    print_info "检查项目编译状态..."

    if [ ! -d "${PROJECT_ROOT}/target/classes/com/dbpwd" ]; then
        print_info "项目未编译，开始编译..."
        cd "$PROJECT_ROOT"

        if command -v mvn &> /dev/null; then
            mvn clean compile -DskipTests
        else
            print_error "Maven未安装，无法编译项目"
            exit 1
        fi

        print_success "项目编译完成"
    else
        print_success "项目已编译"
    fi

    cd "$PROJECT_ROOT"
}

# 运行连接测试
run_connection_test() {
    local config_file="$1"

    print_separator
    print_info "开始连接测试..."
    print_separator

    # 设置类路径
    local classpath="${PROJECT_ROOT}/target/classes"

    # 添加依赖
    if [ -d "${PROJECT_ROOT}/target/lib" ]; then
        classpath="${classpath}:${PROJECT_ROOT}/target/lib/*"
    fi

    # 检查依赖
    if ! ls ${PROJECT_ROOT}/target/lib/druid*.jar 1> /dev/null 2>&1; then
        print_info "下载依赖..."
        cd "$PROJECT_ROOT"
        mvn dependency:copy-dependencies -DoutputDirectory=target/lib 2>/dev/null || true
        classpath="${classpath}:${PROJECT_ROOT}/target/lib/*"
    fi

    # 运行测试
    cd "$PROJECT_ROOT"

    # 检查MySQL驱动
    if ls ${PROJECT_ROOT}/target/lib/mysql-connector-*.jar 1> /dev/null 2>&1 || \
       ls ~/.m2/repository/com/mysql/mysql-connector-j/*/mysql-connector-j-*.jar 1> /dev/null 2>&1; then
        print_info "MySQL驱动可用"
    else
        print_warning "MySQL驱动未找到，下载中..."
        mvn dependency:resolve -DincludeArtifactIds=mysql-connector-j 2>/dev/null || true
    fi

    # 运行测试主类
    java -cp "$classpath" \
        -Dtest.config="$config_file" \
        com.dbpwd.DruidConnectionTest "$config_file"

    local exit_code=$?

    echo
    print_separator

    if [ $exit_code -eq 0 ]; then
        print_success "所有测试通过!"
    else
        print_error "测试失败，退出码: $exit_code"
    fi

    print_separator

    return $exit_code
}

# 运行单元测试
run_unit_tests() {
    print_separator
    print_info "运行单元测试..."
    print_separator

    cd "$PROJECT_ROOT"

    if command -v mvn &> /dev/null; then
        mvn test
    else
        print_error "Maven未安装，无法运行单元测试"
        return 1
    fi
}

# 显示帮助信息
show_help() {
    cat << EOF
用法: $0 [选项] [配置文件路径]

选项:
    -h, --help          显示帮助信息
    -u, --unit-test     仅运行单元测试（不进行连接测试）
    -c, --config PATH   指定配置文件路径

示例:
    $0                                          # 使用默认配置文件 config/druid.properties
    $0 config/my-druid.properties               # 使用指定配置文件
    $0 --unit-test                              # 仅运行单元测试
    $0 -u                                       # 仅运行单元测试

环境变量:
    TEST_DB_URL         测试数据库URL（用于单元测试）
    TEST_DB_USERNAME    测试数据库用户名
    TEST_DB_PASSWORD    测试数据库密码
    TEST_DB_DRIVER      测试数据库驱动类名
    TEST_DB_KEY         加密密钥

EOF
}

# 主函数
main() {
    local unit_test_only=false
    local config_file=""

    # 解析参数
    while [[ $# -gt 0 ]]; do
        case "$1" in
            -h|--help)
                show_help
                exit 0
                ;;
            -u|--unit-test)
                unit_test_only=true
                shift
                ;;
            -c|--config)
                config_file="$2"
                shift 2
                ;;
            *.properties)
                config_file="$1"
                shift
                ;;
            *)
                print_error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done

    # 默认配置文件
    if [ -z "$config_file" ]; then
        if [ -f "${PROJECT_ROOT}/config/druid.properties" ]; then
            config_file="${PROJECT_ROOT}/config/druid.properties"
        elif [ -f "${PROJECT_ROOT}/config/druid_test.properties" ]; then
            config_file="${PROJECT_ROOT}/config/druid_test.properties"
        else
            config_file="${PROJECT_ROOT}/config/druid.properties"
        fi
    fi

    # 处理相对路径
    if [[ "$config_file" != /* ]]; then
        config_file="${PROJECT_ROOT}/${config_file}"
    fi

    print_separator
    echo -e "${GREEN}Druid 连接池测试工具${NC}"
    print_separator
    echo

    # 检查环境
    check_java

    # 运行单元测试
    if [ "$unit_test_only" = true ]; then
        run_unit_tests
        exit $?
    fi

    # 运行连接测试
    check_config_file "$config_file"
    build_project
    run_connection_test "$config_file"

    exit $?
}

# 启动程序
main "$@"
