# Database Password Manager - Demo应用集成指南

这是一个数据库密码管理系统的演示应用集合，展示了6种集成场景下的SM4-GCM国密认证加解密功能。

## 概述

本项目包含6个演示应用，涵盖了主流Java应用技术栈的集成场景：

| Demo应用 | 集成方案 | 集成模块 | 状态 |
|---------|---------|---------|------|
| **Simple JDBC Demo** | 方案5: StandalonePasswordDecryptor | db-pwd-core | ✅ 完成 |
| **Druid Demo** | 方案1: Druid PasswordCallback | db-pwd-druid | ✅ 完成 |
| **Spring Boot Demo** | 方案2: Spring Boot Starter | db-pwd-spring-boot-starter | ✅ 完成 |
| **Spring Demo** | 方案3: Spring BeanPostProcessor | db-pwd-spring | ✅ 完成 |
| **MyBatis Demo** | 方案4: MyBatis DataSourceFactory | db-pwd-mybatis | ✅ 完成 |
| **Servlet Demo** | 方案6: Servlet InitListener | db-pwd-web | ✅ 完成 |

> **详细规划**：请查看 [Demo集成方案规划](./technical/demo-integration-plan.md)

---

## Demo初始化工具 (db-pwd-demo-init)

所有 Demo 运行前必须先执行 demo-init 工具初始化 H2 FILE 数据库。

### 为什么需要初始化工具？

H2 内存数据库不验证密码，任何密码都可以连接。无法真正验证密码解密的有效性。

初始化工具创建 H2 FILE 数据库，首次连接时绑定密码，后续连接必须使用相同的密码。

### 使用步骤

```bash
# Step 1: 生成 db.pwd 和 sm4.key
mvn exec:java -pl db-pwd-core \
  -Dexec.mainClass="com.dbpwd.cli.DBPasswordCLI" \
  --demo --generate-key

# Step 2: 运行demo-init初始化H2 FILE数据库
mvn exec:java -pl db-pwd-demo-init \
  -Dexec.mainClass="com.dbpwd.init.DatabaseInitializer"

# Step 3: 运行具体Demo程序
```

### 执行示例

```
=========================================
   Database Password Manager - Demo Database Initialization
=========================================

Initializing: simple_jdbc_db → dbpwd_simple_demo
  Password retrieved from db.pwd: ✓
  H2 FILE database created at: jdbc:h2:file:~/.dbpwd/h2_data/dbpwd_simple_demo
  Database will validate password on subsequent connections
  Table(s) created successfully: dbpwd_simple_demo

...
Successfully initialized: 7
H2 data directory: C:\Users\faunt\.dbpwd\h2_data
```

### 创建的数据库

```
~/.dbpwd/h2_data/
├── dbpwd_simple_demo.mv.db
├── dbpwd_druid_demo.mv.db
├── dbpwd_spring_boot_demo.mv.db
├── dbpwd_spring_demo.mv.db
├── dbpwd_mybatis_demo.mv.db
├── dbpwd_servlet_demo.mv.db
└── dbpwd_struts_demo.mv.db
```

**详细说明**: 请查看 [Demo应用设计文档](./demo-design.md) - Demo初始化工具章节

---

## 相关文档

- [Demo集成方案规划](./technical/demo-integration-plan.md) - 6个Demo的详细设计和技术方案
- [集成场景和方案](./technical/integration-scenarios.md) - 6种集成场景详解
- [Demo应用设计文档](./demo-design.md) - Demo的详细设计架构、工作机制和适用场景
- [Demo应用演示步骤](./demo-run-step.md) - Demo应用的完整演示执行步骤
- [demo-run-result.md](./demo-run-result.md) - 所有Demo的详细执行输出

## 快速开始

### 前置要求

- JDK 8+
- Maven 3.6+
- H2内存数据库（Demo内置，无需外部数据库）

### 数据库配置

所有Demo使用H2内存数据库，无需外部MySQL或PostgreSQL。启动Demo时会自动创建内存数据库和表结构。

**H2数据库密码配置**:

所有Demo使用相同的H2数据库密码（经过SM4加密存储）：

| 配置项 | 值 |
|-------|-----|
| H2用户名 | `sa` |
| H2密码（明文） | `dbpwd_h2_demo_password_2024` |
| 存储方式 | 在db.pwd中经过SM4加密 |

**Demo占位符映射**:

| Demo应用 | 占位符(在db.pwd中) | H2数据库名 | JDBC URL |
|---------|------------------|-----------|----------|
| Simple JDBC Demo | `simple_jdbc_db` | `dbpwd_demo` | `jdbc:h2:mem:dbpwd_demo;DB_CLOSE_DELAY=-1` |
| Druid Demo | `druid_demo_db` | `druid_demo` | `jdbc:h2:mem:druid_demo;DB_CLOSE_DELAY=-1` |
| Spring Boot Demo | `spring_boot_db` | `spring_demo` | `jdbc:h2:mem:spring_boot_demo;DB_CLOSE_DELAY=-1` |
| Spring Demo | `spring_framework_db` | `spring_fwk_demo` | `jdbc:h2:mem:spring_framework_demo;DB_CLOSE_DELAY=-1` |
| MyBatis Demo | `mybatis_demo_db` | `mybatis_demo` | `jdbc:h2:mem:mybatis_demo;DB_CLOSE_DELAY=-1` |
| Servlet Demo | `servlet_demo_db` | `servlet_demo` | `jdbc:h2:mem:servlet_demo;DB_CLOSE_DELAY=-1` |

**生成db.pwd配置的命令**:

```bash
# 方式1：逐个生成（推荐）
java -jar db-pwd-cli.jar encrypt \
  --id simple_jdbc_db \
  --password dbpwd_h2_demo_password_2024 \
  --url jdbc:h2:mem:dbpwd_demo;DB_CLOSE_DELAY=-1 \
  --username sa \
  --driver org.h2.Driver

# 方式2：使用CLI交互模式
java -jar db-pwd-cli.jar
# 输入: add
# 然后按提示输入每个Demo的配置

# 方式3：批量生成（需要手动编辑输出文件）
java -jar db-pwd-cli.jar encrypt \
  --id demo_general \
  --password dbpwd_h2_demo_password_2024 \
  --url jdbc:h2:mem:demo;DB_CLOSE_DELAY=-1 \
  --username sa \
  --driver org.h2.Driver
```

**H2内存数据库参数说明**:

| 参数 | 值 | 说明 |
|------|---|------|
| `DB_CLOSE_DELAY=-1` | 保持数据库直到JVM关闭 | 避免内存数据库自动关闭 |
| `;` | 分隔URL参数 | H2使用分号分隔多个参数 |
| `sa` | H2默认管理员用户 | H2默认用户名 |

---

## Demo应用说明

### 1. Simple JDBC Demo (方案5)

**集成方式**: StandalonePasswordDecryptor工具类

**特点**: 无框架依赖，纯JDBC API

**运行方式**:
```bash
# 进入demo目录
cd db-pwd-simple-jdbc-demo

# 编译打包
mvn clean package

# 运行（确保db.pwd和sm4.key已生成）
mvn exec:java
```

**关键代码**:
```java
// 使用工具类获取连接
Connection conn = StandalonePasswordDecryptor.getConnection("simple_jdbc_db");

// 使用DbUtil简化查询
DbUtil.query("simple_jdbc_db", "SELECT * FROM users", rs -> { ... });
```

---

### 2. Druid Demo (方案1)

**集成方式**: Druid PasswordCallback

**特点**: 利用Druid原生回调机制，零侵入

**状态**: ⚪ 待开发

---

### 3. Spring Boot Demo (方案2)

**集成方式**: Spring Boot Starter自动配置

**特点**: 最小配置，自动装饰DataSource

**运行方式**:
```bash
# 进入demo目录
cd db-pwd-spring-boot-demo

# 编译打包
mvn clean package

# 运行
mvn spring-boot:run

# 或使用jar包
java -jar target/db-pwd-spring-boot-demo.jar
```

**API测试**:
```bash
# 查询用户列表
curl http://localhost:8080/api/users

# 创建用户
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"test","email":"test@example.com"}'
```

---

### 4. Spring Demo (方案3)

**集成方式**: Spring BeanPostProcessor

**特点**: 传统Spring应用（非Boot），拦截DataSource

**状态**: ⚪ 待开发

---

### 5. MyBatis Demo (方案4)

**集成方式**: MyBatis DataSourceFactory

**特点**: 纯MyBatis应用，无Spring依赖

**状态**: ⚪ 待开发

---

### 6. Servlet Demo (方案6)

**集成方式**: ServletContextListener

**特点**: 纯Servlet应用，通过ServletContext共享密码服务

**状态**: ⚪ 待开发

---

Database Password Manager 支持两种集成模式：

### 模式1: 程序直接操作模式

**核心特点**:
- 解密和替换工作由**程序自己实现**
- 代码直接调用 `Sm4GCMCipher` API
- 适合简单场景或不想引入框架依赖的应用

**适用Demo**: Simple JDBC Demo, Struts Demo

### 模式2: 配置文件装饰器模式

**核心特点**:
- 解密和替换工作由**框架(DataSourceDecorator)**实现
- 配置文件使用占位符而非真实密码
- 适合需要最小化应用代码改动的场景

**适用Demo**: Spring Boot Demo

---

## 集成到您的项目

### 模式1集成（程序直接操作）

#### Maven依赖

```xml
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### 基本用法

```java
import com.dbpwd.crypto.Sm4GCMCipher;
import com.dbpwd.crypto.ENCFormat;

// 加密密码
String plainPassword = "myDatabasePassword";
String sm4Key = "0123456789abcdef0123456789abcdef";
String cipherText = Sm4GCMCipher.encrypt(plainPassword, sm4Key);
String formattedEncrypted = ENCFormat.formatSM4GCM(cipherText);
System.out.println("Encrypted: " + formattedEncrypted);

// 解密密码
String decrypted = Sm4GCMCipher.decrypt(cipherText, sm4Key);

// 使用解密后的密码建立数据库连接
String jdbcUrl = "jdbc:h2:mem:mydb;DB_CLOSE_DELAY=-1";
Connection conn = DriverManager.getConnection(jdbcUrl, username, decrypted);
```

#### 完整示例

```java
import com.dbpwd.crypto.Sm4GCMCipher;
import com.dbpwd.crypto.ENCFormat;

import java.sql.Connection;
import java.sql.DriverManager;

public class DatabaseManager {
    private static final String SM4_KEY = "0123456789abcdef0123456789abcdef";
    private static final String ENCRYPTED_PASSWORD = "ENC(SM4-GCM:您的密文)";

    public Connection getConnection() throws Exception {
        // 解密密码
        String cipherText = ENCFormat.extractCipherText(ENCRYPTED_PASSWORD);
        String decryptedPassword = Sm4GCMCipher.decrypt(cipherText, SM4_KEY);

        // 建立连接
        String url = "jdbc:mysql://localhost:3306/mydb?useSSL=false";
        return DriverManager.getConnection(url, "username", decryptedPassword);
    }
}
```

### 模式2集成（配置文件装饰器）

#### Maven依赖

```xml
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### 配置文件设置

**application.properties**:
```properties
spring.datasource.url=jdbc:h2:mem:mydb;DB_CLOSE_DELAY=-1
spring.datasource.username=sa
spring.datasource.password=@my_database@  # 使用占位符
```

**application.yml**:
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:mydb;DB_CLOSE_DELAY=-1
    username: sa
    password: "@my_database@"  # 使用占位符
```

#### 应用代码

```java
@RestController
public class MyController {
    @Autowired
    private DataSource dataSource;

    @GetMapping("/data")
    public List<Data> getData() {
        // 直接使用DataSource，密码已自动解密
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
        return jdbcTemplate.query("SELECT * FROM data", rowMapper);
    }
}
```

#### 占位符配置

在 `~/.dbpwd/db.pwd` 文件中配置对应的数据库：

```json
{
  "version": "1.0",
  "databases": [
    {
      "id": "my_database",
      "name": "My Application Database",
      "type": "mysql",
      "host": "localhost",
      "port": 3306,
      "database": "mydb",
      "url": "jdbc:mysql://localhost:3306/mydb",
      "username": "dbuser",
      "password": "ENC(SM4-GCM:您的密文)"
    }
  ]
}
```

---

## 加密数据库密码

### 使用命令行工具生成密文

```bash
# 加密密码
java -jar db-pwd-cli.jar encrypt --id my_database --password myPassword123

# 输出
# Database ID: my_database
# Encrypted Password: ENC(SM4-GCM:xxx:yyyy)
# Files generated: ~/.dbpwd/db.pwd, ~/.dbpwd/sm4.key
```

### 查看生成的配置文件

**~/.dbpwd/sm4.key**:
```
0123456789abcdef0123456789abcdef
```

**~/.dbpwd/db.pwd**:
```json
{
  "version": "1.0",
  "databases": [
    {
      "id": "my_database",
      "name": "My Database",
      "type": "mysql",
      "host": "localhost",
      "port": 3306,
      "database": "mydb",
      "url": "jdbc:mysql://localhost:3306/mydb",
      "username": "dbuser",
      "password": "ENC(SM4-GCM:您的密文)"
    }
  ],
  "updatedAt": "2026-02-10T00:00:00Z"
}
```

---

## 多环境配置

### 开发环境

```bash
# 生成开发环境配置
java -jar db-pwd-cli.jar encrypt --id my_app_dev --password dev_password_123
```

### 测试环境

```bash
# 生成测试环境配置
java -jar db-pwd-cli.jar encrypt --id my_app_test --password test_password_456
```

### 生产环境

```bash
# 生成生产环境配置
java -jar db-pwd-cli.jar encrypt --id my_app_prod --password prod_secure_pass
```

### 配置文件示例

```json
{
  "version": "1.0",
  "databases": [
    {
      "id": "my_app_dev",
      "name": "Dev Database",
      "type": "mysql",
      "host": "localhost",
      "port": 3306,
      "database": "myapp_dev",
      "url": "jdbc:mysql://localhost:3306/myapp_dev",
      "username": "dev_user",
      "password": "ENC(SM4-GCM:dev密文)"
    },
    {
      "id": "my_app_test",
      "name": "Test Database",
      "type": "mysql",
      "host": "test.db.example.com",
      "port": 3306,
      "database": "myapp_test",
      "url": "jdbc:mysql://test.db.example.com:3306/myapp_test",
      "username": "test_user",
      "password": "ENC(SM4-GCM:test密文)"
    },
    {
      "id": "my_app_prod",
      "name": "Production Database",
      "type": "mysql",
      "host": "prod.db.example.com",
      "port": 3306,
      "database": "myapp_prod",
      "url": "jdbc:mysql://prod.db.example.com:3306/myapp_prod",
      "username": "prod_user",
      "password": "ENC(SM4-GCM:prod密文)"
    }
  ],
  "updatedAt": "2026-02-10T00:00:00Z"
}
```

**应用配置文件**:
```properties
# 开发环境
spring.datasource.password=@my_app_dev@

# 测试环境
# spring.datasource.password=@my_app_test@

# 生产环境
# spring.datasource.password=@my_app_prod@
```

---

## 安全最佳实践

### 1. 密钥文件保护

确保密钥文件具有正确的权限：
```bash
chmod 600 ~/.dbpwd/sm4.key
chmod 600 ~/.dbpwd/db.pwd
chmod 700 ~/.dbpwd/
```

### 2. 密钥轮换

定期更换SM4密钥：
```bash
# 生成新密钥
java -jar db-pwd-cli.jar keygen

# 使用新密钥重新加密所有密码
java -jar db-pwd-cli.jar encrypt --id <database_id> --password <new_password>
```

### 3. 密码复杂度

使用强密码：
- 最少12个字符
- 包含大小写字母
- 包含数字和特殊字符
- 避免使用字典词汇

### 4. 生产环境部署

生产环境部署建议：
- 使用配置中心管理 `db.pwd` 和 `sm4.key`
- 定期审计文件访问日志
- 使用硬件安全模块(HSM)保护密钥
- 实施密钥备份和恢复策略

---

## 故障排查

### 常见问题

**问题1**: 解密失败，报错 `AEADBadTagException`

**原因**: 密文被篡改或使用了错误的密钥

**解决方案**:
- 检查 `sm4.key` 文件内容是否正确
- 确认 `db.pwd` 文件未被修改
- 重新生成密文和密钥

**问题2**: 连接MySQL 8.0+失败

**原因**: MySQL 8.0+默认使用`caching_sha2_password`认证方式

**解决方案**: 在JDBC URL中添加`allowPublicKeyRetrieval=true`参数
```
jdbc:mysql://localhost:3306/mydb?useSSL=false&allowPublicKeyRetrieval=true
```

**问题3**: 占位符未被识别

**原因**: 占位符格式不正确或DataSourceDecorator未正确配置

**解决方案**:
- 确认占位符格式为 `@database_id@`
- 检查 `db.pwd` 中是否存在对应的数据库配置
- 查看启动日志确认DataSourceDecorator是否已加载

---

## 技术支持

如有问题，请参考主项目文档：
- [设计文档](./claude.md) - 包含Demo应用密码管理集成模式和占位符设计原则
- [Demo应用设计](./demo-design.md) - 三个Demo的详细设计架构和机制说明
- [Demo演示步骤](./demo-run-step.md) - Demo应用的完整演示执行步骤
- [技术文档](./technical/) - 技术文档目录结构
- [项目README](./README.md) - 项目主README
- [demo-run-result.md](./demo-run-result.md) - 所有Demo的详细执行输出

## 版本信息

- 版本: 1.0.0
- 最后更新: 2026-02-10
- Java版本: JDK 8+
- 构建工具: Maven 3.6+
- 加密算法: SM4-GCM（认证加密）
