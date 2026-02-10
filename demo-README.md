# Database Password Manager - Demo应用集成指南

这是一个数据库密码管理系统的演示应用集合，展示了SM4-GCM国密认证加解密功能。

## 概述

本项目包含三个演示应用，展示了在不同场景下使用Database Password Manager进行密码加密和管理的方式：

1. **Simple JDBC Demo** - 基础JDBC应用，演示核心SM4-GCM加解密功能
2. **Spring Boot Demo** - Spring Boot Web应用，演示与Spring框架集成
3. **Struts Demo** - 独立应用，演示密码加密解密验证

## 相关文档

- [Demo应用设计文档](./demo-design.md) - 三个Demo的详细设计架构、工作机制和适用场景
- [Demo应用演示步骤](./demo-run-step.md) - Demo应用的完整演示执行步骤
- [demo-run-result.md](./demo-run-result.md) - 所有Demo的详细执行输出

## 快速开始

### 前置要求

- JDK 8+
- Maven 3.6+
- Docker Desktop（用于运行MySQL数据库）

### MySQL数据库设置

在运行Demo应用之前，需要先启动MySQL容器：

```bash
# 启动MySQL容器
docker run -d \
  --name dbpwd-mysql \
  --restart always \
  -e MYSQL_ROOT_PASSWORD=rootpass123 \
  -e MYSQL_DATABASE=dbpwd_demo \
  -e MYSQL_USER=dbpwd \
  -e MYSQL_PASSWORD=dbpwd123 \
  -p 3306:3306 \
  mysql:8.0

# 验证MySQL启动状态
docker logs dbpwd-mysql
```

数据库连接信息：

| 参数 | 值 |
|------|-----|
| 主机 | localhost |
| 端口 | 3306 |
| 数据库 | dbpwd_demo |
| 用户名 | dbpwd |
| 密码 | dbpwd123 |

**Demo占位符映射**:

| Demo应用 | 占位符(在db.pwd中) | 实际数据库 | 说明 |
|---------|------------------|-----------|------|
| Simple JDBC Demo | `simple_jdbc_db` | `dbpwd_demo` | 模式1示例 |
| Spring Boot Demo | `spring_boot_db` | `dbpwd_demo` | 模式2示例 |
| Struts Demo | `struts_db` | `dbpwd_demo` | 模式1示例 |

**说明**: 三个Demo使用不同的占位符，但实际都指向同一个MySQL数据库 `dbpwd_demo`。这种设计展示了多应用共享同一数据库的不同配置方式。

### 停止MySQL数据库

```bash
docker stop dbpwd-mysql
```

---

## 集成模式概述

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
String jdbcUrl = "jdbc:mysql://localhost:3306/mydb";
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
spring.datasource.url=jdbc:mysql://localhost:3306/mydb?useSSL=false
spring.datasource.username=dbuser
spring.datasource.password=@my_database@  # 使用占位符
```

**application.yml**:
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb?useSSL=false
    username: dbuser
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
