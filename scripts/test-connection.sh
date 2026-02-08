#!/bin/bash

###############################################################################
# Druid数据库密码管理工具测试脚本
#
# 功能：
#   1. 测试数据库连接
#   2. 验证密码加密/解密功能
#
# 使用方法：
#   bash scripts/test-connection.sh
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

# 编译项目（如果需要）
build_project() {
    print_info "检查项目编译状态..."

    if [ ! -d "${PROJECT_ROOT}/target/classes/com/dbpwd/cli" ]; then
        print_info "项目未编译，开始编译..."
        cd "$PROJECT_ROOT"

        if command -v mvn &> /dev/null; then
            mvn clean compile -DskipTests
        else
            print_error "Maven未安装，无法编译项目"
            print_info "可以参考以下方式安装Maven："
            print_info "  - 下载: https://maven.apache.org/download.cgi"
            print_info "  - 解压并配置PATH环境变量"
            exit 1
        fi

        print_success "项目编译完成"
    else
        print_success "项目已编译"
    fi

    cd "$PROJECT_ROOT"
}

# 运行单元测试
run_unit_tests() {
    print_separator
    print_info "运行单元测试..."
    print_separator

    cd "$PROJECT_ROOT"

    if command -v mvn &> /dev/null; then
        mvn test || true
        print_info "单元测试执行完成"
    else
        print_error "Maven未安装，无法运行单元测试"
        return 1
    fi
}

# 运行CLI工具
run_cli() {
    print_separator
    print_info "启动交互式命令行工具..."
    print_separator

    # 设置类路径
    local classpath="${PROJECT_ROOT}/target/classes"

    # 添加Maven本地仓库的依赖
    local m2_home="${HOME}/.m2/repository"
    if [ -d "$m2_home" ]; then
        # 添加项目相关的依赖
        find "$m2_home" -name "bcprov-*.jar" -exec echo -n "{}:" \; 2>/dev/null | sed 's/:$//' | xargs -I {} classpath="{}:$classpath" 2>/dev/null || true
        find "$m2_home" -name "jackson-*.jar" -exec echo -n "{}:" \; 2>/dev/null | sed 's/:$//' | xargs -I {} classpath="{}:$classpath" 2>/dev/null || true
        find "$m2_home" -name "commons-lang3-*.jar" -exec echo -n "{}:" \; 2>/dev/null | sed 's/:$//' | xargs -I {} classpath="{}:$classpath" 2>/dev/null || true
    fi

    # 运行CLI
    cd "$PROJECT_ROOT"
    java -cp "$classpath" com.dbpwd.cli.DBPasswordCLI

    local exit_code=$?

    print_separator

    if [ $exit_code -eq 0 ]; then
        print_success "工具已退出"
    else
        print_warning "工具退出码: $exit_code"
    fi

    print_separator

    return $exit_code
}

# 显示帮助信息
show_help() {
    cat << EOF
用法: $0 [选项]

选项:
    -h, --help          显示帮助信息
    -u, --unit-test     仅运行单元测试（不启动CLI）

示例:
    $0                                          # 编译项目，运行测试并启动CLI
    $0 --unit-test                              # 仅运行单元测试
    $0 -u                                       # 仅运行单元测试

EOF
}

# 主函数
main() {
    local unit_test_only=false

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
            *)
                print_error "未知参数: $1"
                show_help
                exit 1
                ;;
        esac
    done

    print_separator
    echo -e "${GREEN}        Druid数据库密码管理工具 v3.1${NC}"
    print_separator
    echo

    # 检查环境
    check_java

    # 编译项目
    build_project

    # 运行单元测试
    run_unit_tests

    # 如果不是只运行单元测试，则启动CLI
    if [ "$unit_test_only" != true ]; then
        run_cli
    fi

    exit 0
}

# 启动程序
main "$@"
