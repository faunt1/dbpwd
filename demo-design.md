# Database Password Manager - Demo应用设计文档

> 文档版本: 1.0
> 最后更新: 2026-02-10

## 概述

本文档详细描述了Database Password Manager三个Demo应用的设计架构、工作机制和适用场景。

## Demo应用概览

本项目包含三个演示应用，展示了在不同场景下使用Database Password Manager进行密码加密和管理的方式：

| Demo应用 | 集成模式 | 解密实现方式 | 占位符 |
|---------|---------|------------|--------|
| Simple JDBC Demo | 模式1 | 程序自己实现 | `simple_jdbc_db` |
| Spring Boot Demo | 模式2 | 框架实现 | `spring_boot_db` |
| Struts Demo | 模式1 | 程序自己实现 | `struts_db` |

---

## 1. Simple JDBC Demo（模式1示例）

**位置**: `db-pwd-simple-jdbc-demo/`

### 1.1 设计概述

Simple JDBC Demo 采用**模式1（程序直接操作模式）**，是三个Demo中最基础的应用，展示如何在没有框架依赖的情况下直接使用 Database Password Manager 的密码管理功能。

### 1.2 集成模式：模式1（程序直接操作模式）

**核心特点**:
- 解密和替换工作由**程序自己实现**
- 代码直接调用 `Sm4GCMCipher` API
- 适合简单场景或不想引入框架依赖的应用
- 完全控制加密解密流程

### 1.3 设计架构

```
┌─────────────────────────────────────────────────────────────┐
│                    Simple JDBC Demo                         │
│                  (主类: SimpleJdbcDemo)                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐      │
│  │  加密流程   │   │  解密流程   │   │  数据库操作 │      │
│  ├─────────────┤   ├─────────────┤   ├─────────────┤      │
│  │ 1. 调用     │   │ 1. 加载     │   │ 1. JDBC连接 │      │
│  │    Sm4GCM   │   │    密钥     │   │ 2. 创建表   │      │
│  │    .encrypt │   │ (sm4.key)  │   │ 3. 插入数据 │      │
│  │ 2. 格式化   │   │ 2. 调用     │   │ 4. 查询数据 │      │
│  │    ENC格式  │   │    Sm4GCM   │   │ 5. 更新数据 │      │
│  │ 3. 输出     │   │    .decrypt │   │ 6. 删除数据 │      │
│  │    密文密码 │   │ 3. 构建     │   │             │      │
│  │             │   │    JDBC串   │   │             │      │
│  └─────────────┘   └─────────────┘   └─────────────┘      │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 1.4 工作机制详解

#### 步骤1: 密码加密环节（演示阶段）
```java
// 直接调用 Sm4GCMCipher API 加密密码
String cipherText = Sm4GCMCipher.encrypt("dbpwd123", "0123456789abcdef0123456789abcdef");
String formatted = ENCFormat.formatSM4GCM(cipherText);
// 输出: ENC(SM4-GCM:xxx:yyyy)
```

#### 步骤2: 密码解密环节
```java
// 1. 加载密钥文件 sm4.key
// 2. 从 DB_PASSWORD 常量或配置获取密文密码
// 3. 调用解密API
String decrypted = Sm4GCMCipher.decrypt(encryptedPassword, sm4Key);
```

#### 步骤3: 构建JDBC连接串
```java
String jdbcUrl = "jdbc:mysql://localhost:3306/dbpwd_demo?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
String username = "dbpwd";
String password = decrypted; // 使用解密后的明文密码

Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
```

### 1.5 占位符设计

| 配置项 | 值 |
|--------|-----|
| 占位符(数据库名) | `simple_jdbc_db` |
| 实际MySQL数据库 | `dbpwd_demo` |
| 说明 | 模式1示例，程序直接操作 |

**占位符使用方式**:
- 在代码中硬编码占位符名称 `simple_jdbc_db`
- 占位符用于标识不同的数据库配置（虽然此Demo实际连接到 `dbpwd_demo`）
- 未来可扩展为从 `db.pwd` 文件中根据占位符查找对应配置

### 1.6 功能特性

| 功能模块 | 说明 |
|---------|------|
| SM4-GCM加密功能 | 演示加密API的调用和随机IV特性 |
| MySQL数据库连接 | 展示使用解密后密码建立连接的过程 |
| 密码格式化 | ENC(SM4-GCM:iv:ciphertext) 格式输出 |
| 加密解密验证 | 验证加密解密的完整性和正确性 |
| 随机IV验证 | 证明相同明文加密后密文不同 |
| 数据库CRUD操作 | 完整的增删改查演示 |

### 1.7 代码关键点

```java
// 关键1: 加密演示
String encrypted = Sm4GCMCipher.encrypt(DB_PASSWORD, SM4_KEY);
System.out.println("Encrypted: ENC(SM4-GCM:" + encrypted + ")");

// 关键2: 解密演示
String decrypted = Sm4GCMCipher.decrypt(encrypted, SM4_KEY);

// 关键3: 建立连接
Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, decrypted);
```

### 1.8 适用场景

- 简单的命令行工具
- 无框架依赖的独立应用
- 需要完全控制加密解密流程的场景
- 学习和研究Database Password Manager的基础用法

---

## 2. Spring Boot Demo（模式2示例）

**位置**: `db-pwd-spring-boot-demo/`

### 2.1 设计概述

Spring Boot Demo 采用**模式2（配置文件装饰器模式）**，展示如何在Spring Boot Web应用中集成Database Password Manager，实现密码管理的无缝集成和自动化处理。

### 2.2 集成模式：模式2（配置文件装饰器模式）

**核心特点**:
- 解密和替换工作由**框架(DataSourceDecorator)**实现
- 配置文件使用占位符而非真实密码
- 适合需要最小化应用代码改动的场景
- 声明式配置，自动化处理

### 2.3 设计架构

```
┌─────────────────────────────────────────────────────────────┐
│                  Spring Boot Demo                           │
│                  (主类: SpringBootDemo)                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │              Spring Boot 框架层                     │   │
│  ├─────────────────────────────────────────────────────┤   │
│  │                                                      │   │
│  │  ┌──────────────┐                                   │   │
│  │  │ application  │──┐                                │   │
│  │  │ .properties  │  │                                │   │
│  │  │   密码:      │  │                                │   │
│  │  │  @spring_    │  │ ┌─────────────────────────┐   │   │
│  │  │   boot_db@   │  │ │   DataSourceDecorator  │   │   │
│  │  └──────────────┘  │ ├─────────────────────────┤   │   │
│  │                    │ │ 1. 拦截初始化           │   │   │
│  │                    │ │ 2. 读取占位符          │   │   │
│  │                    │ │ 3. 查找db.pwd          │   │   │
│  │                    │ │ 4. 解密密码            │   │   │
│  │                    │ │ 5. 注入明文密码        │   │   │
│  │                    │ └─────────────────────────┘   │   │
│  │                    │              │                 │   │
│  │                    │              ▼                 │   │
│  │  ┌────────────────────────────────────────┐      │   │
│  │  │       REST API 层                      │      │   │
│  │  │ ├─ / (应用信息)                        │      │   │
│  │  │ ├─ /health (健康检查)                  │      │   │
│  │  │ ├─ /users (用户列表)                   │      │   │
│  │  │ ├─ /orders (订单列表)                  │      │   │
│  │  │ └─ /stats (统计信息)                   │      │   │
│  │  └────────────────────────────────────────┘      │   │
│  │                                                      │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 2.4 工作机制详解

#### 步骤1: 配置文件中的占位符
```yaml
# application.properties 或 application.yml
spring.datasource.url=jdbc:mysql://localhost:3306/dbpwd_demo?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=dbpwd
spring.datasource.password=@spring_boot_db@  # 占位符，非真实密码
```

#### 步骤2: DataSourceDecorator 拦截
```java
// DataSourceDecorator 在 DataSource Bean 初始化时自动拦截
// 检测到密码值为 @spring_boot_db@ 格式的占位符
```

#### 步骤3: 从 db.pwd 中查找和解密
```java
// 1. 从 db.pwd 中查找 id="spring_boot_db" 的配置
// 2. 读取加密后的密码字段
// 3. 使用 sm4.key 解密密码
// 4. 将明文密码注入到 DataSource 配置中
```

#### 步骤4: 应用正常使用
```java
// 应用代码无需修改，直接使用 DataSource
@Autowired
DataSource dataSource;

@Repository
public class UserRepository {
    @Autowired
    JdbcTemplate jdbcTemplate;
    // 正常使用数据库连接
}
```

### 2.5 占位符设计

| 配置项 | 值 |
|--------|-----|
| 占位符(数据库名) | `spring_boot_db` |
| 实际MySQL数据库 | `dbpwd_demo` |
| 占位符格式 | `@spring_boot_db@` |
| 说明 | 模式2示例，框架自动处理 |

**占位符使用方式**:
- 在 Spring Boot 配置文件中使用 `@spring_boot_db@` 作为密码值
- DataSourceDecorator 自动识别 `@...@` 格式的占位符
- 根据占位符名称从 `db.pwd` 中查找对应的数据库配置

### 2.6 功能特性

| 功能模块 | 说明 |
|---------|------|
| Spring Boot Web应用 | 完整的Web应用框架 |
| REST API接口 | 提供RESTful风格的数据访问接口 |
| MySQL数据库连接 | 自动化的连接池管理 |
| SM4-GCM加密功能 | 框架级别的密码加密解密处理 |
| 数据库初始化 | 应用启动时自动初始化测试数据 |
| CRUD操作通过API | 通过REST接口进行数据操作 |

### 2.7 REST API 接口

| 接口 | 方法 | 描述 | 返回内容 |
|-----|------|------|----------|
| `/` | GET | 应用信息 | 数据库类型、应用名、加密方式、状态 |
| `/health` | GET | 健康检查 | 数据库连接状态、用户数量 |
| `/users` | GET | 用户列表 | 用户ID、姓名、邮箱、创建时间 |
| `/orders` | GET | 订单列表 | 订单ID、用户名、产品名称、金额 |
| `/stats` | GET | 统计信息 | 用户数、订单数、订单总金额 |

### 2.8 代码关键点

```java
// 关键1: 配置文件中设置占位符
spring.datasource.password=@spring_boot_db@

// 关键2: DataSourceDecorator 自动拦截配置（框架实现）
// 应用代码无需任何修改，直接使用DataSource

// 关键3: REST Controller 正常使用数据库
@GetMapping("/users")
public List<User> getUsers() {
    return jdbcTemplate.query("SELECT * FROM users", userRowMapper);
}
```

### 2.9 适用场景

- Spring Boot/Spring Framework 应用
- 需要最小化代码改动的现有系统
- 企业级应用架构
- 需要声明式配置和自动化的场景
- 多环境部署（开发、测试、生产）

---

## 3. Struts Demo（模式1扩展示例）

**位置**: `db-pwd-struts-demo/`

### 3.1 设计概述

Struts Demo 采用**模式1（程序直接操作模式）**，作为独立应用的演示，展示如何在没有任何框架依赖的情况下，完全手动处理加密解密流程。与Simple JDBC Demo相比，它提供了更全面的测试用例。

### 3.2 集成模式：模式1（程序直接操作模式）

**核心特点**:
- 解密和替换工作由**程序自己实现**
- 代码直接调用 `Sm4GCMCipher` API
- 完全手动处理加密解密流程
- 包含更全面的加密解密测试

### 3.3 设计架构

```
┌───────────────────────────────────────────────────────────────────┐
│                     Struts Demo                                    │
│                 (主类: StrutsDemo)                                 │
├───────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │               DbConfig (密码工具类)                         │ │
│  ├─────────────────────────────────────────────────────────────┤ │
│  │  - getEncryptedPassword()  加密方法                         │ │
│  │  - decryptPassword()        解密方法（支持GCM和ECB）         │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                          │                                        │
│                          ▼                                        │
│  ┌─────────────────────────────────────────────────────────────┐ │
│  │                  测试套件                                   │ │
│  ├─────────────────────────────────────────────────────────────┤ │
│  │  Test 1: 基础SM4-GCM加密解密                                │ │
│  │  Test 2: 多密码加密测试                                     │ │
│  │  Test 3: 不同SM4密钥测试                                     │ │
│  │  Test 4: GCM随机IV特性                                      │ │
│  │  Test 5: ECB格式向后兼容                                    │ │
│  │  Test 6: MySQL数据库连接与CRUD操作                          │ │
│  └─────────────────────────────────────────────────────────────┘ │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘
```

### 3.4 工作机制详解

#### 步骤1: 密码工具类设计
```java
public static class DbConfig {
    private static final String SM4_KEY = "0123456789abcdef0123456789abcdef";

    // 加密方法
    public static String getEncryptedPassword(String plainPassword) {
        String cipherText = Sm4GCMCipher.encrypt(plainPassword, SM4_KEY);
        return ENCFormat.formatSM4GCM(cipherText);
    }

    // 解密方法（同时支持GCM和ECB格式）
    public static String decryptPassword(String encryptedPassword) {
        String algorithm = ENCFormat.extractAlgorithm(encryptedPassword);
        String cipherText = ENCFormat.extractCipherText(encryptedPassword);

        if ("SM4".equalsIgnoreCase(algorithm)) {
            return Sm4Cipher.decrypt(cipherText, SM4_KEY); // ECB格式
        } else if ("SM4-GCM".equalsIgnoreCase(algorithm)) {
            return Sm4GCMCipher.decrypt(cipherText, SM4_KEY); // GCM格式
        }
    }
}
```

#### 步骤2: 数据库密码加密和解密
```java
// 加密数据库密码
String encryptedDbPassword = Sm4GCMCipher.encrypt(DB_PASSWORD, TEST_SM4_KEY);

// 解密数据库密码
String decryptedDbPassword = Sm4GCMCipher.decrypt(encryptedDbPassword, TEST_SM4_KEY);

// 构建JDBC连接
Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, decryptedDbPassword);
```

#### 步骤3: 向后兼容支持
```java
// 支持ECB格式（旧格式）
String ecbEncrypted = ENCFormat.formatSM4(Sm4Cipher.encrypt(ecbPassword, key1));
String ecbDecrypted = DbConfig.decryptPassword(ecbEncrypted); // 自动识别ECB格式
```

### 3.5 占位符设计

| 配置项 | 值 |
|--------|-----|
| 占位符(数据库名) | `struts_db` |
| 实际MySQL数据库 | `dbpwd_demo` |
| 说明 | 模式1示例，独立应用完全手动处理 |

**占位符使用方式**:
- 与 Simple JDBC Demo 类似，占位符在代码中作为标识符
- 此Demo更侧重于加密解密功能的全面测试
- 支持向后兼容ECB格式

### 3.6 功能特性

| 功能模块 | 说明 |
|---------|------|
| SM4-GCM加密解密 | 基础加密解密功能验证 |
| 多密码加密测试 | 验证多个密码的加密解密正确性 |
| 不同密钥测试 | 验证不同密钥产生不同密文 |
| GCM随机IV验证 | 验证相同明文产生不同密文 |
| ECB格式向后兼容 | 支持解密旧的ECB格式密码 |
| MySQL数据库连接 | 使用解密后密码建立连接 |
| 数据库CRUD操作 | 完整的增删改查演示 |

### 3.7 测试套件结构

```java
Main() {
    // Test 1: 基础SM4-GCM加密解密
    test1_BasicEncryption();

    // Test 2: 多密码编码测试
    test2_MultiplePasswords();

    // Test 3: 不同SM4密钥测试
    test3_DifferentKeys();

    // Test 4: GCM随机IV特性
    test4_RandomIV();

    // Test 5: ECB格式向后兼容
    test5_ECBCompatibility();

    // Test 6: MySQL数据库连接与CRUD操作
    test6_DatabaseConnection();
}
```

### 3.8 代码关键点

```java
// 关键1: 加密（GCM格式）
String encrypted1 = Sm4GCMCipher.encrypt(password, key);

// 关键2: 解密（兼容GCM和ECB）
String algorithm = ENCFormat.extractAlgorithm(encryptedPassword);
if ("SM4".equalsIgnoreCase(algorithm)) {
    return Sm4Cipher.decrypt(cipherText, SM4_KEY); // ECB
} else if ("SM4-GCM".equalsIgnoreCase(algorithm)) {
    return Sm4GCMCipher.decrypt(cipherText, SM4_KEY); // GCM
}

// 关键3: 数据库连接
Connection connection = DriverManager.getConnection(url, username, decrypted);
```

### 3.9 适用场景

- 独立运行的命令行工具
- 需要全面测试加密解密功能的场景
- 需要向后兼容旧格式的系统
- 学习和研究密码管理完整流程
- 密码管理功能的单元测试

---

## Demo占位符汇总

| Demo应用 | 占位符(数据库名) | 集成模式 | 解密实现 | 实际数据库 |
|---------|----------------|---------|---------|-----------|
| Simple JDBC Demo | `simple_jdbc_db` | 模式1 | 程序自己实现 | `dbpwd_demo` |
| Spring Boot Demo | `spring_boot_db` | 模式2 | 框架实现 | `dbpwd_demo` |
| Struts Demo | `struts_db` | 模式1 | 程序自己实现 | `dbpwd_demo` |

---

## 相关文档

- [Demo应用演示步骤](./demo-run-step.md) - Demo应用的完整演示执行步骤
- [Demo应用集成指南](./demo-README.md) - 如何将Database Password Manager集成到您的项目中
- [demo-run-result.md](./demo-run-result.md) - 所有Demo的详细执行输出
- [设计文档](./claude.md) - 整体设计文档
