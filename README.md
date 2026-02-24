# Database Password Manager

基于 SM4 国密算法的数据库密码管理系统，提供 7 种集成模式，覆盖主流 Java 技术栈。

## 背景与需求

### 问题背景

在企业应用开发中，数据库密码管理面临以下安全挑战：

- **密码泄露风险**: 数据库密码明文存储在配置文件中，容易泄露
- **代码库风险**: 生产环境密码可能被提交到版本控制系统
- **多环境管理困难**: 开发、测试、生产环境密码管理混乱

### 核心需求

1. **密码安全存储**: 生产环境数据库密码不在代码库和配置文件中明文出现
2. **应用无感知**: 应用代码无需关心密码文件的具体位置
3. **多框架支持**: 支持主流 Java 技术栈的集成

### 解决方案

- **SM4-GCM 认证加密**: 采用国密算法，支持认证加密防篡改
- **占位符格式**: 应用配置中使用 `@db_name@` 格式，由系统自动解密
- **固定路径**: 密码文件路径固定为 `~/.dbpwd/`，应用无需配置

## 核心特性

- **SM4-GCM 认证加密**: 采用国密算法，支持认证加密防篡改
 **7 种集成模式**: Druid/Spring Boot/Spring/MyBatis/Servlet/纯JDBC/**ServletContext共享DataSource**
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
| 方案7: ServletContext共享DataSource | Struts/Servlet (零代码) | web.xml | ✅ |

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

### 设计文档

| 文档 | 说明 |
|------|------|
| [AGENTS.md](AGENTS.md) | 项目上下文（AI Agent 参考） |
| [架构设计](docs/architecture.md) | 系统架构和核心设计 |
| [应用集成指南](docs/integration-guide.md) | **完整集成参考**：代码修改详解 |
| [Demo应用设计](docs/demo-applications.md) | Demo 应用设计和验证 |

### 用户文档

| 文档 | 说明 |
|------|------|
| [用户使用手册](docs/user-guide.md) | 完整操作指南 |
| [系统部署文档](docs/deployment-guide.md) | 部署运维指南 |
| [API参考文档](docs/api-reference.md) | 核心 API 使用指南 |
| [常见问题 FAQ](docs/FAQ.md) | 常见问题解答 |

### 自动化工具

| 文档 | 说明 |
|------|------|
| [db-pwd-integrator Skill](skills/db-pwd-integrator/SKILL.md) | 自动扫描并集成密码加密功能 |

## 技术要求

- **JDK 版本**: JDK 8+ (推荐 JDK 11)
- **加密库**: Bouncy Castle 1.69+ (JDK 8)
- **构建工具**: Maven 3.6+

---

> 文档版本: v5.0
> 最后更新: 2026-02-20