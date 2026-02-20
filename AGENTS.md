# AGENTS.md - Database Password Manager 项目上下文

> 本文档为 AI Agent 提供项目上下文，帮助理解和操作本代码库。
>
> 最后更新: 2026-02-20

## 项目概述

**Database Password Manager** 是一个基于 SM4 国密算法的数据库密码管理系统，提供 6 种核心集成模式，覆盖主流 Java 技术栈。核心目标是确保生产环境数据库密码不在代码库和配置文件中明文出现。

### 核心特性

- **SM4-GCM 认证加密**: 采用国密算法，支持认证加密防篡改
- **6 种集成模式**: Druid/Spring Boot/Spring/MyBatis/Servlet/纯JDBC
- **应用无感知**: 文件路径由 PasswordService 内部固定为 `~/.dbpwd/`
- **占位符格式**: 使用 `@db_name@` 格式，应用配置无需知道文件位置

---

## 业务需求

### 密码安全需求

- 生产环境数据库密码不在代码库中出现
- 生产环境数据库密码不在开放测试环境的各类配置中出现
- 密码在传输、存储、使用过程中全程加密保护

### 安全增强要求

**应用无感知设计**：
- `db.pwd` 和 `sm4.key` 文件路径对应用屏蔽
- 文件路径由 `PasswordService` 内部固定为 `~/.dbpwd/`
- 应用无需知道文件位置，增强安全性

**占位符格式**：
- 使用 `@db_name@` 格式作为密码占位符
- 应用配置中只使用占位符，不暴露文件路径

---

## 核心设计决策

| 决策点 | 选择 | 理由 |
|-------|------|------|
| 文件路径 | 固定为 `~/.dbpwd/` | 参考SSH机制，应用无感知 |
| 占位符格式 | `@db_name@` | 明确标识，与配置格式区分 |
| 加密算法 | SM4-GCM | 国密标准，认证加密防篡改 |
| 应用集成 | 装饰器模式 | 最小改动，自动解密 |

---

## 技术栈

| 组件 | 版本 |
|------|------|
| JDK | 8 (必须兼容) / 推荐 11+ |
| Maven | 3.6+ |
| **加密算法** | SM4-GCM 认证加密 |
| **加密库** | Bouncy Castle 1.78+ |
| **JSON库** | Jackson 2.12+ |
| Spring Boot | 2.7.18 |
| Spring Framework | 5.3.31 |
| Druid | 1.2.20 |
| JUnit | 5.9.3 |

---

## 项目结构

```
db-pwd/
├── db-pwd-core/                    # 核心模块 - 加解密、密码服务、文件管理
├── db-pwd-druid/                   # Druid PasswordCallback 集成
├── db-pwd-spring/                  # Spring BeanPostProcessor 集成
├── db-pwd-spring-boot-starter/     # Spring Boot Starter 自动配置
├── db-pwd-mybatis/                 # MyBatis DataSourceFactory 集成
├── db-pwd-web/                     # Servlet ContextListener 集成
│
├── db-pwd-demo-init/               # Demo 初始化工具
│
├── demos/                          # Demo 应用集合
│   ├── simple-jdbc/               # 方案5: 纯 JDBC
│   │   ├── demo/                   # 【集成版本】
│   │   └── original/               # 【原始版本】
│   ├── druid/                     # 方案1: Druid
│   │   ├── demo/                   # 【集成版本】
│   │   └── original/               # 【原始版本】
│   ├── druid-spring/              # 方案1+3: Druid + Spring XML
│   │   ├── demo/                   # 【集成版本】
│   │   └── original/               # 【原始版本】
│   ├── spring-boot/                # 方案2: Spring Boot Starter
│   │   ├── demo/                   # 【集成版本】
│   │   └── original/               # 【原始版本】
│   ├── spring-boot-api/            # 方案2变体: Spring Boot API
│   │   ├── demo/                   # 【集成版本】
│   │   └── original/               # 【原始版本】
│   ├── spring/                     # 方案3: Spring Java Config
│   │   ├── demo/                   # 【集成版本】
│   │   └── original/               # 【原始版本】
│   ├── spring-xml/                 # 方案3: Spring XML Config
│   │   ├── demo/                   # 【集成版本】
│   │   └── original/               # 【原始版本】
│   ├── mybatis/                    # 方案4: MyBatis
│   │   ├── demo/                   # 【集成版本】
│   │   └── original/               # 【原始版本】
│   ├── servlet/                    # 方案6: Servlet (web.xml)
│   │   ├── demo/                   # 【集成版本】
│   │   └── original/               # 【原始版本】
│   ├── servlet-annotation/         # 方案6变体: Servlet (注解)
│   │   ├── demo/                   # 【集成版本】
│   │   └── original/               # 【原始版本】
│   └── struts/                     # Struts 1.x
│       ├── demo/                   # 【集成版本】
│       └── original/               # 【原始版本】
│
├── docs/                           # 文档目录
├── skills/                         # 自动化集成 Skill
├── config/                         # 配置模板
├── scripts/                        # 脚本目录
├── .mvn/                           # Maven Wrapper
└── .github/                        # GitHub 工作流
```

### 原始版本 vs 集成版本

项目为每种集成模式提供了 **原始版本** 和 **集成版本** 的对比：

| 集成模式 | 原始版本 | 集成版本 |
|---------|---------|---------|
| 方案5: 纯JDBC | `demos/simple-jdbc/original` | `demos/simple-jdbc/demo` |
| 方案1: Druid | `demos/druid/original` | `demos/druid/demo` |
| 方案1+3: Druid+Spring | `demos/druid-spring/original` | `demos/druid-spring/demo` |
| 方案2: Spring Boot Starter | `demos/spring-boot/original` | `demos/spring-boot/demo` |
| 方案2变: Spring Boot API | `demos/spring-boot-api/original` | `demos/spring-boot-api/demo` |
| 方案3: Spring Java Config | `demos/spring/original` | `demos/spring/demo` |
| 方案3: Spring XML | `demos/spring-xml/original` | `demos/spring-xml/demo` |
| 方案4: MyBatis | `demos/mybatis/original` | `demos/mybatis/demo` |
| 方案6: Servlet (web.xml) | `demos/servlet/original` | `demos/servlet/demo` |
| 方案6变: Servlet (注解) | `demos/servlet-annotation/original` | `demos/servlet-annotation/demo` |
| Struts 1.x | `demos/struts/original` | `demos/struts/demo` |

详细的代码差异对比请参考：[应用集成参考文档](docs/integration-guide.md)

---

## 核心模块详解

### db-pwd-core

核心模块，包含所有基础功能：

**包结构**:
- `com.dbpwd.crypto` - SM4 加解密实现 (Sm4GCMCipher, PasswordEncryptor, PasswordDecryptor, ENCFormat)
- `com.dbpwd.core` - 核心服务 (PasswordService, PasswordFileManager, Sm4KeyLoader, PlaceholderParser)
- `com.dbpwd.model` - 数据模型 (DatabaseEntry, PasswordFile)
- `com.dbpwd.framework` - 框架集成支持 (DataSourceDecorator)
- `com.dbpwd.spring` - Spring 自动配置 (DbPwdAutoConfiguration, DbPwdProperties)
- `com.dbpwd.cli` - 命令行工具 (DBPasswordCLI)

**核心类**:
- `PasswordService`: 一站式密码获取服务，内部固定使用 `~/.dbpwd/` 路径
- `Sm4GCMCipher`: SM4-GCM 认证加密实现
- `StandalonePasswordDecryptor`: 独立工具类，用于无框架场景

---

## 构建和运行

### 构建项目

```bash
# 完整构建（跳过测试）
mvn clean install -DskipTests

# 完整构建（含测试）
mvn clean install

# 仅构建核心模块
mvn clean install -pl db-pwd-core

# 使用 Maven Wrapper
./mvnw clean install -DskipTests    # Linux/Mac
mvnw.cmd clean install -DskipTests  # Windows
```

### 运行测试

```bash
# 运行所有测试
mvn test

# 运行指定模块测试
mvn test -pl db-pwd-core

# 运行单个测试类
mvn test -Dtest=Sm4GCMCipherTest -pl db-pwd-core
```

### 运行 Demo

#### 初始化数据库

DatabaseInitializer 统一管理所有 Demo 数据库的初始化：

```bash
# 初始化所有数据库 (注意: Windows需设置 UTF-8 编码)
java -Dfile.encoding=UTF-8 -jar db-pwd-demo-init/target/db-pwd-demo-init-1.0.0-jar-with-dependencies.jar --all

# 仅创建单个测试库
java -Dfile.encoding=UTF-8 -jar db-pwd-demo-init/target/db-pwd-demo-init-1.0.0-jar-with-dependencies.jar --single

# 仅验证现有配置
java -Dfile.encoding=UTF-8 -jar db-pwd-demo-init/target/db-pwd-demo-init-1.0.0-jar-with-dependencies.jar --verify

# 初始化指定数据库
java -Dfile.encoding=UTF-8 -jar db-pwd-demo-init/target/db-pwd-demo-init-1.0.0-jar-with-dependencies.jar --db mybatis_demo_db
```

**重要**: Windows 环境必须设置 `-Dfile.encoding=UTF-8`，否则中文输出乱码。

**数据库凭据**（所有 Demo 统一）：

| 项目 | 值 |
|------|-----|
| 用户名 | `sa` |
| 密码 | `sa` |
| 存储格式 | `ENC(SM4-GCM:xxx)` |

#### 运行各 Demo

```bash
# 方案5: 纯JDBC
java -Dfile.encoding=UTF-8 -jar db-pwd-simple-jdbc-demo/target/db-pwd-simple-jdbc-demo-1.0.0.jar

# 方案1: Druid
java -Dfile.encoding=UTF-8 -jar db-pwd-druid-demo/target/db-pwd-druid-demo-1.0.0.jar

# 方案2: Spring Boot (随机端口)
java -Dfile.encoding=UTF-8 -jar db-pwd-spring-boot-demo/target/db-pwd-spring-boot-demo-1.0.0.jar

# 方案4: MyBatis
java -Dfile.encoding=UTF-8 -jar db-pwd-mybatis-demo/target/db-pwd-mybatis-demo-1.0.0.jar

# 方案6: Servlet (需要Tomcat)
mvn tomcat7:run -pl db-pwd-servlet-demo
```

---

## 6 种集成模式

| 模式 | 适用场景 | 核心机制 | 自动解密 |
|------|---------|---------|----------|
| 方案1: Druid PasswordCallback | Druid连接池 | 原生密码回调 | ✅ |
| 方案2: Spring Boot Starter | Spring Boot | 自动配置装饰 | ✅ |
| 方案3: Spring BeanPostProcessor | 传统Spring | Bean后置处理 | ✅ |
| 方案4: MyBatis DataSourceFactory | MyBatis | 工厂类扩展 | ✅ |
| 方案5: StandalonePasswordDecryptor | 纯JDBC | API调用 | ❌ 手动 |
| 方案6: Servlet InitListener | Servlet/Web | 容器监听器 | ✅ |

### Demo 应用对照表

| Demo 模块 | 集成方案 | 配置方式 | 占位符 |
|-----------|---------|---------|--------|
| db-pwd-simple-jdbc-demo | 方案5 | API 调用 | `@simple_jdbc_db@` |
| db-pwd-druid-demo | 方案1 | Java 配置 | `@druid_demo_db@` |
| db-pwd-druid-demo-spring | 方案1+3 | Spring + Druid | `@druid_spring_db@` |
| db-pwd-spring-boot-demo | 方案2 | Starter 自动配置 | `@spring_boot_db@` |
| db-pwd-spring-boot-demo-api | 方案2变体 | 手动 API 配置 | `@spring_boot_api_db@` |
| db-pwd-spring-demo | 方案3 | Java Config | `@spring_framework_db@` |
| db-pwd-spring-demo-xml | 方案3 | XML Config | `@spring_xml_db@` |
| db-pwd-mybatis-demo | 方案4 | MyBatis XML | `@mybatis_demo_db@` |
| db-pwd-servlet-demo | 方案6 | web.xml | `@servlet_demo_db@` |
| db-pwd-servlet-demo-annotation | 方案6 | 注解配置 | `@servlet_annotation_db@` |
| db-pwd-struts-demo | 方案6扩展 | Struts + Servlet | `@struts_demo_db@` |

---

## 开发约定

### 代码规范

- **Java 版本**: 源码和目标均为 JDK 8
- **编码**: UTF-8
- **包命名**: `com.dbpwd.*`
- **日志**: 使用 SLF4J + Logback

### 单元测试原则

- **自包含**: 使用 `@TempDir` 创建临时目录，测试数据隔离
- **自解释**: 通过 API 参数传递路径，不依赖外部配置
- **可隔离**: 测试之间互不影响，测试后自动清理

### Demo 应用原则

- **模拟生产**: 使用 `~/.dbpwd/` 作为配置目录
- **无感知**: 应用只需使用占位符 `@db_name@`
- **易迁移**: Demo 可直接作为生产参考

### 文件权限要求

| 文件 | 路径 | 权限 |
|------|------|------|
| 密钥文件 | `~/.dbpwd/sm4.key` | 600 |
| 密码文件 | `~/.dbpwd/db.pwd` | 600 |
| 配置目录 | `~/.dbpwd/` | 700 |

---

## 关键 API 使用示例

### PasswordService（推荐）

```java
// 一行代码获取明文密码
String password = PasswordService.getDatabasePassword("@my_db@");

// 解密任意格式密码（占位符或ENC格式）
String plain = PasswordService.decrypt("ENC(SM4-GCM:...)");
```

### StandalonePasswordDecryptor

```java
// 获取密码
String password = StandalonePasswordDecryptor.getDatabasePassword("@my_db@");

// 获取连接
Connection conn = StandalonePasswordDecryptor.getConnection("@my_db@");
```

### 占位符格式

应用配置中使用 `@db_name@` 格式：

```yaml
spring:
  datasource:
    password: "@spring_boot_db@"  # 占位符
```

---

## 文档索引

| 文档 | 说明 |
|------|------|
| [architecture.md](docs/architecture.md) | 架构设计文档 |
| [api-reference.md](docs/api-reference.md) | API 参考文档 |
| [integration-guide.md](docs/integration-guide.md) | **应用集成参考文档** |
| [demo-applications.md](docs/demo-applications.md) | Demo 应用设计 |
| [user-guide.md](docs/user-guide.md) | 用户使用手册 |
| [deployment-guide.md](docs/deployment-guide.md) | 部署指南 |
| [FAQ.md](docs/FAQ.md) | 常见问题 |
| [SKILL.md](skills/db-pwd-integrator/SKILL.md) | 自动化集成 Skill |

---

## 常见任务

### 添加新的数据库配置

1. 使用 CLI 工具或 API 添加加密密码到 `~/.dbpwd/db.pwd`
2. 在应用配置中使用对应的占位符 `@db_name@`

### 调试加密问题

1. 检查密钥文件格式（32字符 hex 编码）
2. 检查密码文件 JSON 格式
3. 使用 `Sm4GCMCipherTest` 验证加解密

### 添加新的集成模式

1. 在 `db-pwd-core` 中添加核心支持
2. 创建新的集成模块（参考现有模块结构）
3. 添加对应的 Demo 应用
4. 更新文档

---

## 注意事项

- **安全第一**: 密钥文件和密码文件不应提交到版本控制
- **权限检查**: Linux/Mac 环境需确保文件权限正确设置
- **Windows 兼容**: 项目支持 Windows 环境，使用 `System.getProperty("user.home")` 获取用户目录
- **H2 数据库**: Demo 使用 H2 FILE 模式进行真实密码验证
- **Struts 1.x**: `db-pwd-struts-demo` 使用 Struts 1.3.10，适合维护旧系统参考

---

## 版本兼容性

| 集成模式 | 模块 | 核心依赖 | 兼容范围 |
|----------|------|----------|----------|
| Druid PasswordCallback | `db-pwd-druid` | Druid 1.2.20 | Druid 1.0.x ~ 1.2.x |
| Spring BeanPostProcessor | `db-pwd-spring` | Spring 5.3.31 | Spring 4.x ~ 5.x |
| Spring Boot Starter | `db-pwd-spring-boot-starter` | Spring Boot 2.7.18 | Spring Boot 2.x |
| MyBatis DataSourceFactory | `db-pwd-mybatis` | MyBatis 3.5.13 | MyBatis 3.x |
| Servlet ContextListener | `db-pwd-web` | Servlet 3.1.0 | Servlet 3.0+ / 4.x |

> **注意**: 如需支持 Spring Boot 3.x，需升级到 Java 17+ 并将 `javax.*` 替换为 `jakarta.*`。