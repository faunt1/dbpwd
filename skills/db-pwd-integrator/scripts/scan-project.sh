#!/bin/bash
# db-pwd-integrator 项目扫描脚本
# 用于检测 Java 项目类型和数据库配置

set -e

echo "🔍 db-pwd-integrator 项目扫描"
echo "================================"

PROJECT_ROOT="${1:-.}"
cd "$PROJECT_ROOT"

# 1. 检测构建工具
echo ""
echo "📦 构建工具检测"
echo "----------------"

if [ -f "pom.xml" ]; then
    echo "✅ Maven 项目 (pom.xml)"
    BUILD_TOOL="maven"
    
    # 提取关键依赖
    echo ""
    echo "📚 关键依赖"
    echo "-----------"
    grep -E "<artifactId>(spring-boot|druid|mybatis|servlet)" pom.xml 2>/dev/null | head -10 || echo "未检测到框架依赖"
    
elif [ -f "build.gradle" ] || [ -f "build.gradle.kts" ]; then
    echo "✅ Gradle 项目"
    BUILD_TOOL="gradle"
else
    echo "❌ 未检测到构建文件"
    BUILD_TOOL="unknown"
fi

# 2. 检测框架类型
echo ""
echo "🔧 框架类型检测"
echo "----------------"

FRAMEWORK="unknown"

# Spring Boot
if grep -r "@SpringBootApplication" src/ --include="*.java" 2>/dev/null | head -1 > /dev/null; then
    echo "✅ Spring Boot"
    FRAMEWORK="spring-boot"
fi

# Spring Framework
if grep -r "@Configuration" src/ --include="*.java" 2>/dev/null | head -1 > /dev/null; then
    if [ "$FRAMEWORK" = "unknown" ]; then
        echo "✅ Spring Framework"
        FRAMEWORK="spring"
    fi
fi

# Druid
if grep -r "DruidDataSource" src/ --include="*.java" 2>/dev/null | head -1 > /dev/null; then
    echo "✅ Druid 连接池"
    [ "$FRAMEWORK" = "spring-boot" ] && FRAMEWORK="spring-boot-druid"
    [ "$FRAMEWORK" = "spring" ] && FRAMEWORK="spring-druid"
    [ "$FRAMEWORK" = "unknown" ] && FRAMEWORK="druid"
fi

# MyBatis
if [ -f "src/main/resources/mybatis-config.xml" ] || find src/ -name "*Mapper.xml" 2>/dev/null | head -1 > /dev/null; then
    echo "✅ MyBatis"
    [ "$FRAMEWORK" = "unknown" ] && FRAMEWORK="mybatis"
fi

# Servlet
if [ -f "src/main/webapp/WEB-INF/web.xml" ] || grep -r "@WebServlet\|@WebListener" src/ --include="*.java" 2>/dev/null | head -1 > /dev/null; then
    echo "✅ Servlet/Web"
    [ "$FRAMEWORK" = "unknown" ] && FRAMEWORK="servlet"
fi

# 纯 JDBC
if grep -r "DriverManager.getConnection" src/ --include="*.java" 2>/dev/null | head -1 > /dev/null; then
    if [ "$FRAMEWORK" = "unknown" ]; then
        echo "✅ 纯 JDBC"
        FRAMEWORK="jdbc"
    fi
fi

[ "$FRAMEWORK" = "unknown" ] && echo "❌ 未检测到已知框架"

# 3. 检测配置文件
echo ""
echo "📄 配置文件"
echo "------------"

for pattern in "application.yml" "application.yaml" "application.properties" "applicationContext.xml" "mybatis-config.xml"; do
    if find . -name "$pattern" -not -path "*/target/*" 2>/dev/null | head -1 > /dev/null; then
        find . -name "$pattern" -not -path "*/target/*" 2>/dev/null | head -5
    fi
done

# 4. 检测 DataSource 配置
echo ""
echo "🔌 DataSource 配置位置"
echo "----------------------"

grep -rn "DataSource\|@Bean.*dataSource\|setPassword" src/ --include="*.java" 2>/dev/null | head -10 || echo "未检测到 DataSource 配置"

# 5. 检测明文密码
echo ""
echo "🔐 明文密码检测"
echo "----------------"

PASSWORD_FILES=$(grep -rn 'password\s*=\s*"[^"@]' src/ --include="*.java" 2>/dev/null | grep -v "test" | head -5)
if [ -n "$PASSWORD_FILES" ]; then
    echo "⚠️  发现可能的明文密码："
    echo "$PASSWORD_FILES"
else
    echo "✅ 未检测到明显的明文密码"
fi

# 6. 检测是否已集成 db-pwd
echo ""
echo "🔑 db-pwd 集成状态"
echo "------------------"

if grep -r "db-pwd" pom.xml 2>/dev/null | head -1 > /dev/null; then
    echo "✅ 已集成 db-pwd"
    grep "db-pwd" pom.xml 2>/dev/null | head -5
else
    echo "❌ 未集成 db-pwd"
fi

# 7. 输出推荐方案
echo ""
echo "================================"
echo "📋 扫描结果摘要"
echo "================================"
echo "构建工具: $BUILD_TOOL"
echo "项目类型: $FRAMEWORK"

case "$FRAMEWORK" in
    spring-boot|spring-boot-druid)
        echo "推荐方案: 方案2 - Spring Boot Starter (db-pwd-spring-boot-starter)"
        ;;
    spring|spring-druid)
        echo "推荐方案: 方案3 - Spring BeanPostProcessor (db-pwd-spring)"
        ;;
    druid)
        echo "推荐方案: 方案1 - Druid PasswordCallback (db-pwd-druid)"
        ;;
    mybatis)
        echo "推荐方案: 方案4 - MyBatis DataSourceFactory (db-pwd-mybatis)"
        ;;
    servlet)
        echo "推荐方案: 方案6 - Servlet InitListener (db-pwd-web)"
        ;;
    jdbc)
        echo "推荐方案: 方案5 - StandalonePasswordDecryptor (db-pwd-core)"
        ;;
    *)
        echo "推荐方案: 无法自动识别，请手动选择"
        ;;
esac

echo ""
echo "运行 'db-pwd-integrator' skill 开始集成"
