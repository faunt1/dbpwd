# Database Password Manager - 设计文档

> 文档版本: v4.0
> 最后更新: 2026-02-14

## 目录

1. [概述](#概述)
2. [业务需求](#业务需求)
3. [技术需求](#技术需求)
4. [整体方案](#整体方案)
5. [核心设计决策](#核心设计决策)
6. [相关文档](#相关文档)

---

## 概述

### 项目简介

基于 SM4 国密算法的数据库密码管理系统，提供 6 种集成模式，覆盖主流 Java 技术栈。

### 技术栈

| 组件 | 技术选择 |
|------|----------|
| **JDK版本** | JDK 8+ (推荐JDK 11) |
| **加密算法** | SM4-GCM 认证加密 |
| **加密库** | Bouncy Castle (1.69+) |
| **JSON库** | Jackson (2.12+) |
| **构建工具** | Maven 3.6+ |

---

## 业务需求

### 1. 密码安全需求

- 生产环境数据库密码不在代码库中出现
- 生产环境数据库密码不在开放测试环境的各类配置中出现
- 密码在传输、存储、使用过程中全程加密保护

### 2. 安全增强要求

**应用无感知设计**：
- `db.pwd` 和 `sm4.key` 文件路径对应用屏蔽
- 文件路径由 `PasswordService` 内部固定为 `~/.dbpwd/`
- 应用无需也无需知道文件位置，增强安全性

**占位符格式**：
- 使用 `@db_name@` 格式作为密码占位符
- 应用配置中只使用占位符，不暴露文件路径

---

## 技术需求

### 1. 系统分解

#### 1.1 密码加解密工具
- 对数据库用户名、明文密码进行SM4加密
- 生成 `db.pwd` 和 `sm4.key` 文件
- 存储位置固定为 `~/.dbpwd/`

#### 1.2 应用侧密码处理
- 应用配置使用 `@db_name@` 占位符
- `PasswordService` 自动从 `~/.dbpwd/` 读取配置文件
- 应用完全不知道文件位置

---

## 整体方案

### 1. 安全保护机制

参考SSH实现机制，确保文件安全性：

| 文件 | 路径 | 权限 |
|------|------|------|
| 密钥文件 | `~/.dbpwd/sm4.key` | 600 |
| 密码文件 | `~/.dbpwd/db.pwd` | 600 |
| 配置目录 | `~/.dbpwd/` | 700 |

### 2. 占位符格式

应用配置中使用 `@db_name@` 格式：

```yaml
# Spring Boot 示例
spring:
  datasource:
    password: "@spring_boot_db@"  # 占位符
```

**说明**: 文件路径由 `PasswordService` 内部固定，应用无需知道文件位置。

### 3. 集成模式

| 模式 | 适用场景 | 核心机制 |
|------|---------|---------|
| 方案1: Druid PasswordCallback | Druid连接池 | 原生密码回调 |
| 方案2: Spring Boot Starter | Spring Boot | 自动配置装饰 |
| 方案3: Spring BeanPostProcessor | 传统Spring | Bean后置处理 |
| 方案4: MyBatis DataSourceFactory | MyBatis | 工厂类扩展 |
| 方案5: StandalonePasswordDecryptor | 纯JDBC | 工具类API |
| 方案6: Servlet InitListener | Servlet/Web | 容器监听器 |

---

## 核心设计决策

| 决策点 | 选择 | 理由 |
|-------|------|------|
| 文件路径 | 固定为 `~/.dbpwd/` | 参考SSH机制，应用无感知 |
| 占位符格式 | `@db_name@` | 明确标识，与配置格式区分 |
| 加密算法 | SM4-GCM | 国密标准，认证加密防篡改 |
| 应用集成 | 装饰器模式 | 最小改动，自动解密 |

---

## 单元测试 vs Demo应用

| 场景 | 文件路径 | 配置方式 | 说明 |
|------|---------|---------|------|
| **单元测试** | 临时目录（@TempDir） | 通过API参数传递 | 自包含、自解释、可隔离 |
| **Demo应用** | `~/.dbpwd/` | 使用默认路径 | 模拟生产环境 |

### 单元测试设计原则

1. **自包含**: 所有测试数据在 `@TempDir` 临时目录中创建
2. **自解释**: 通过 API 参数传递路径，不依赖外部配置
3. **可隔离**: 测试之间互不影响，测试后自动清理

### Demo应用设计原则

1. **模拟生产**: 使用 `~/.dbpwd/` 作为配置目录
2. **无感知**: 应用只需使用占位符 `@db_name@`
3. **易迁移**: Demo可直接作为生产参考

---

## 相关文档

### 核心文档
- [集成模式设计](docs/integration-patterns.md) - 6种集成模式详细设计
- [Demo应用设计](docs/demo-applications.md) - 6个Demo应用的设计和验证
- [API参考文档](docs/api-reference.md) - 核心API使用指南

### 用户文档
- [用户使用手册](docs/user-guide.md) - 完整操作指南
- [系统部署文档](docs/deployment-guide.md) - 部署运维指南
- [常见问题FAQ](docs/FAQ.md) - 常见问题解答

---

## Demo应用验证

### Demo列表

| Demo | 集成模式 | 占位符 |
|------|---------|--------|
| db-pwd-demo-init | - | - |
| db-pwd-simple-jdbc-demo | 方案5 | `@simple_jdbc_db@` |
| db-pwd-druid-demo | 方案1 | `@druid_demo_db@` |
| db-pwd-spring-boot-demo | 方案2 | `@spring_boot_db@` |
| db-pwd-spring-demo | 方案3 | `@spring_framework_db@` |
| db-pwd-mybatis-demo | 方案4 | `@mybatis_demo_db@` |
| db-pwd-servlet-demo | 方案6 | `@servlet_demo_db@` |

### 验证流程

```bash
# 1. 构建
mvn clean install -DskipTests

# 2. 初始化H2 FILE数据库
java -jar db-pwd-demo-init/target/db-pwd-demo-init-1.0.0.jar

# 3. 运行Demo
java -jar db-pwd-simple-jdbc-demo/target/db-pwd-simple-jdbc-demo-1.0.0.jar
```
