# Druid PasswordCallback 集成 Demo

> 集成模块: db-pwd-druid (方案1: Druid PasswordCallback集成)
> 版本: 5.3
> 更新时间: 2026-02-12

## 概述

本Demo演示如何在Druid连接池中使用数据库密码加密管理。通过 `ENC(SM4-GCM:xxx)` 格式的加密密码，确保密码在配置文件中以加密形式存储，运行时自动解密。

## 功能演示

- **ENC格式密码**: 使用 ENC(SM4-GCM:xxx) 格式存储加密密码
- **Druid连接池**: 使用 Druid 连接池管理数据库连接
- **H2内存数据库**: 使用 H2 内存数据库，无需外部数据库
- **完整CRUD操作**: 演示产品创建、查询、更新、删除的完整流程

## 运行Demo

### 1. 编译项目

```bash
# 安装核心模块
mvn install -pl db-pwd-core -DskipTests

# 编译Demo
mvn compile -pl db-pwd-druid-demo
```

### 2. 运行Demo

```bash
mvn exec:java -Dexec.mainClass="com.dbpwd.demo.druid.DruidDemoApplication" -pl db-pwd-druid-demo
```

或打包后运行：

```bash
mvn package -pl db-pwd-druid-demo
java -jar db-pwd-druid-demo/target/db-pwd-druid-demo-1.0.0.jar
```

## 输出示例

```
========================================
   Database Password Manager - Druid Demo
========================================

Testing database connection...
Initializing database tables...
Products table created successfully.

========================================
   CRUD Operations Demo
========================================

1. Creating products...
   Created product: Laptop - 1299.99
   Created product: Smartphone - 899.99

2. Querying all products...
   Found 2 product(s):
   - ID: 1, Name: Laptop, Price: 1299.99, Stock: 50
   - ID: 2, Name: Smartphone, Price: 899.99, Stock: 100

3. Querying product by ID: 1...
   Found: Laptop - High-performance laptop (Stock: 50)

4. Updating product ID: 1...
   Updated product: Laptop - 1199.99

5. Deleting product ID: 2...
   Deleted 1 row(s).

6. Querying remaining products...
   Found 1 product(s):
   - ID: 1, Name: Laptop, Price: 1199.99, Stock: 45

========================================
   Demo completed successfully!
========================================

Key points demonstrated:
- ENC(SM4-GCM:xxx) format for encrypted password
- SM4-GCM encryption for secure password storage
- Druid connection pool manages connections
- No need for additional DruidPasswordCallback
```

## 核心代码说明

### Druid数据源配置

```java
DruidDataSource dataSource = new DruidDataSource();

// 数据库连接配置
dataSource.setUrl("jdbc:h2:mem:dbpwd_druid_demo;DB_CLOSE_DELAY=-1");
dataSource.setUsername("sa");

// 生成ENC格式密码
String encPassword = "ENC(SM4-GCM:" + Sm4GCMCipher.encrypt("sa", sm4Key) + ")";
dataSource.setPassword(encPassword);
dataSource.setDriverClassName("org.h2.Driver");

// 连接池配置
dataSource.setInitialSize(2);
dataSource.setMaxActive(10);
```

### 密码加密

```java
// 使用SM4-GCM加密
String encryptedPassword = Sm4GCMCipher.encrypt(plainPassword, sm4Key);

// 生成ENC格式
String encPassword = "ENC(SM4-GCM:" + encryptedPassword + ")";
```

## 技术栈

| 组件 | 版本 | 说明 |
|-----|------|------|
| JDK | 8+ | Java开发环境 |
| Druid | 1.2.20 | 数据库连接池 |
| H2 | 2.1.214 | 内存数据库（Demo用） |
| db-pwd-core | 1.0.0 | 核心加解密模块 |

## DruidPasswordCallback方式

本Demo使用ENC格式密码直接配置数据源。对于需要使用DruidPasswordCallback的场景（如从db.pwd查找密码），请参考：

```java
// 配置DruidPasswordCallback时的方式（从db.pwd查找）
DruidPasswordCallback callback = DruidPasswordCallback.builder()
    .keyFilePath("~/.dbpwd/sm4.key")
    .pwdFilePath("~/.dbpwd/db.pwd")
    .placeholder("your_db_placeholder")
    .build();

druidDataSource.setPasswordCallback(callback);
druidDataSource.setPassword("your_db_placeholder"); // 占位符
```

详细配置请参考：[Druid集成模块 README](../db-pwd-druid/README.md)

## 与其他集成模块的对比

| 特性 | Druid Demo | Spring Demo | Spring Boot Demo | Simple JDBC Demo |
|-----|------------|------------|------------------|------------------|
| 集成方式 | ENC格式密码 | BeanPostProcessor | Auto Configuration | API直接调用 |
| 适用场景 | Druid连接池应用 | 传统Spring应用 | Spring Boot应用 | 纯JDBC应用 |
| 配置方式 | API配置 | Java/XML配置 | application.yml | 代码配置 |
| 框架依赖 | Druid | Spring Framework | Spring Boot | 无 |

## 相关文档

- [Druid集成模块 README](../db-pwd-druid/README.md)
- [核心模块 README](../db-pwd-core/README.md)
- [集成场景和方案](../technical/integration-scenarios.md)

## 许可证

MIT License
