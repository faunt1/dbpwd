# Database Password Manager

基于 SM4 国密算法的数据库密码管理系统，提供 6 种集成模式，覆盖主流 Java 技术栈。

## 核心特性

- **SM4-GCM 认证加密**: 采用国密算法，支持认证加密防篡改
- **6 种集成模式**: Druid/Spring Boot/Spring/MyBatis/Servlet/纯JDBC
- **应用无感知**: 文件路径由 PasswordService 内部固定为 `~/.dbpwd/`
- **占位符格式**: 使用 `@db_name@` 格式，应用配置无需知道文件位置
- **H2 FILE 验证**: 使用 H2 文件数据库支持真正的密码验证

## 快速开始

### 1. 安装

```bash
mvn clean install -DskipTests
```

### 2. 初始化数据库

```bash
java -jar db-pwd-demo-init/target/db-pwd-demo-init-1.0.0.jar
```

### 3. 运行 Demo

```bash
# Simple JDBC Demo
java -jar db-pwd-simple-jdbc-demo/target/db-pwd-simple-jdbc-demo-1.0.0.jar

# Spring Boot Demo
java -jar db-pwd-spring-boot-demo/target/db-pwd-spring-boot-demo-1.0.0.jar
```

## 集成模��

| 模式 | 适用场景 | 配置方式 | 自动解密 |
|------|---------|---------|----------|
| 方案1: Druid PasswordCallback | Druid连接池 | Java配置 | ✅ |
| 方案2: Spring Boot Starter | Spring Boot | YAML配置 | ✅ |
| 方案3: Spring BeanPostProcessor | 传统Spring | Java/XML | ✅ |
| 方案4: MyBatis DataSourceFactory | MyBatis | XML配置 | ✅ |
| 方案5: StandalonePasswordDecryptor | 纯JDBC | API调用 | ❌ 手动 |
| 方案6: Servlet InitListener | Servlet/Web | web.xml | ✅ |

## 占位符格式

应用配置中使用 `@db_name@` 格式的占位符：

```yaml
# Spring Boot 示例
spring:
  datasource:
    password: "@spring_boot_db@"  # 占位符
```

**说明**: 文件路径 `~/.dbpwd/` 由 `PasswordService` 内部固定，应用无需也无需知道文件位置。

## 项目结构

```
db-pwd/
├── db-pwd-core/                    # 核心模块
├── db-pwd-druid/                   # Druid集成
├── db-pwd-spring/                  # Spring集成
├── db-pwd-spring-boot-starter/     # Spring Boot Starter
├── db-pwd-mybatis/                # MyBatis集成
├── db-pwd-web/                    # Servlet集成
├── db-pwd-demo-init/              # Demo初始化工具
└── demos/                         # Demo应用集合
    ├── simple-jdbc/               # 方案5: 纯JDBC
    ├── druid/                    # 方案1: Druid
    ├── spring-boot/              # 方案2: Spring Boot
    ├── spring/                   # 方案3: Spring
    ├── mybatis/                  # 方案4: MyBatis
    ├── servlet/                  # 方案6: Servlet
    └── struts/                   # Struts 1.x
```

## 文档索引

### 核心文档
- [集成模式设计](docs/integration-patterns.md) - 6 种集成模式详细设计
- [Demo 应用设计](docs/demo-applications.md) - 6 个 Demo 应用的设计和验证
- [API 参考文档](docs/api-reference.md) - 核心 API 使用指南

### 用户文档
- [用户使用手册](docs/user-guide.md) - 完整操作指南
- [系统部署文档](docs/deployment-guide.md) - 部署运维指南
- [常见问题 FAQ](docs/FAQ.md) - 常见问题解答

## 技术要求

- **JDK 版本**: JDK 8+ (推荐 JDK 11)
- **加密库**: Bouncy Castle 1.69+ (JDK 8)
- **构建工具**: Maven 3.6+

---

> 文档版本: v4.0
> 最后更新: 2026-02-14