# Database Password Manager - Demo Applications

这是一个数据库密码管理系统的演示应用集合，展示了SM4国密加解密功能。

## 概述

本项目包含三个演示应用，展示了在不同场景下使用Database Password Manager进行密码加密和管理的方式：

1. **Simple JDBC Demo** - 基础JDBC应用，演示核心SM4加解密功能
2. **Spring Boot Demo** - Spring Boot Web应用，演示与Spring框架集成
3. **Struts Demo** - 独立应用，演示密码加密解密验证

## 快速开始

### 前置要求

- JDK 8+
- Maven 3.6+

### 构建所有模块

```bash
mvn clean install -DskipTests
```

## Demo应用详情

### 1. Simple JDBC Demo

**位置**: `db-pwd-simple-jdbc-demo/`

**功能**:
- SM4加密功能演示
- 密码格式化（ENC格式）
- 加密解密验证

**运行方式**:

```bash
cd db-pwd-simple-jdbc-demo
mvn clean package -DskipTests
java -jar target/db-pwd-simple-jdbc-demo-1.0.0-jar-with-dependencies.jar
```

**预期输出**:
```
=== Simple JDBC Demo Application ===
Demo: Database Password Manager - SM4 Encryption
SM4 key: 0123456789abcdef0123456789abcdef

Step 1: Test SM4 Encryption
  Original password: myDatabasePassword123
  Encrypted password: ENC(SM4:wTfeDzh58xG9hPbocCV5F/njQAD4DJMDHzCYplxHzDo=)

Step 2: Test SM4 Decryption
  Decrypted password: myDatabasePassword123
  Verification: SUCCESS

Step 3: Test ENC Format
  Database password: postgres_password
  Formatted encrypted: ENC(SM4:zGpkBpGOcH4K2I93lphxbDM0aczSrY6EMOoXPnIYuSI=)
  Decrypted: postgres_password
  Verification: SUCCESS

=== Demo Completed Successfully ===
All SM4 encryption and decryption tests passed!
```

### 2. Spring Boot Demo

**位置**: `db-pwd-spring-boot-demo/`

**功能**:
- Spring Boot Web应用
- REST API接口
- H2内存数据库
- SM4加密功能演示

**运行方式**:

```bash
cd db-pwd-spring-boot-demo
mvn clean package -DskipTests
java -jar target/db-pwd-spring-boot-demo-1.0.0.jar
```

**API接口**:

| 接口 | 方法 | 描述 |
|-----|------|------|
| `/` | GET | 应用信息 |
| `/health` | GET | 健康检查和用户数量 |
| `/users` | GET | 用户列表 |
| `/orders` | GET | 订单列表 |

**测试示例**:

```bash
# 应用状态
curl http://localhost:8080/

# 健康检查
curl http://localhost:8080/health

# 获取用户列表
curl http://localhost:8080/users

# 获取订单列表
curl http://localhost:8080/orders
```

**预期输出**:

```json
{
  "application": "Database Password Manager - Spring Boot Demo",
  "status": "running",
  "database": "SQLite",
  "description": "Demo application with encrypted database password"
}
```

### 3. Struts Demo

**位置**: `db-pwd-struts-demo/`

**功能**:
- SM4加密解密验证
- 多密码加密测试
- 不同密钥加密测试

**运行方式**:

```bash
cd db-pwd-struts-demo
mvn clean package -DskipTests
mvn exec:java -Dexec.mainClass="com.dbpwd.demo.StrutsDemo"
```

**预期输出**:
```
=== Struts Demo Application (Standalone) ===
Demo: Database Password Manager - SM4 Encryption
SM4 key: 0123456789abcdef0123456789abcdef

Test 1: Basic SM4 Encryption/Decryption
  Password: struts_password_123
  Encrypted: ENC(SM4:rLDUuaoU0Inbi+dFJkqZgPcYgyz7NGG5Rxeir1Dn5GE=)
  Decrypted: struts_password_123
  Result: SUCCESS

Test 2: Multiple Password Encoding
  admin_pass => OK
  user_db_connection => OK
  migration_key => OK
  production_db => OK

Test 3: Different SM4 Keys
  Test password: test_db_password
  Encrypted with key1: ENC(SM4:+wss0V+L9M6Qo6Wtm09Il0F+ZGzthM9TtQywULsvSGI=)
  Decrypted with key1: test_db_password => OK
  Encrypted with key2: ENC(SM4:OCQ3/L/eOYNbCotPG72IsJ52L2Q3fX3e2GYNViahQbQ=)
  Decrypted with key2: test_db_password => OK
  Keys produce different ciphers: YES

=== Demo Completed Successfully ===
All SM4 encryption and decryption tests passed!
```

## 架构说明

### 多模块Maven项目结构

```
db-pwd/
├── pom.xml                 # 父POM
├── db-pwd-core/           # 核心模块
│   ├── src/main/java/com/dbpwd/
│   │   ├── crypto/       # SM4加密解密
│   │   ├── core/         # 核心功能
│   │   ├── model/        # 数据模型
│   │   ├── cli/          # 命令行工具
│   │   └── framework/    # 框架集成
│   └── pom.xml
├── db-pwd-simple-jdbc-demo/    # JDBC演示
│   ├── src/main/java/com/dbpwd/demo/
│   ├── pom.xml
│   └── target/
├── db-pwd-spring-boot-demo/    # Spring Boot演示
│   ├── src/main/java/com/dbpwd/demo/
│   ├── pom.xml
│   └── target/
└── db-pwd-struts-demo/         # Struts演示
    ├── src/main/java/com/dbpwd/demo/
    ├── pom.xml
    └── target/
```

### 核心功能

#### SM4加密

- 算法：SM4国密标准（GM/T 0002-2012）
- 密钥长度：128位（16字节）
- 模式：ECB/PKCS7Padding
- 格式：`ENC(SM4:密文)`

#### 核心类

| 类名 | 功能 |
|-----|------|
| `Sm4Cipher` | SM4加解密核心 |
| `ENCFormat` | 密码格式化工具 |
| `PasswordEncryptor` | 密码加密器 |
| `PasswordDecryptor` | 密码解密器 |
| `Sm4KeyLoader` | 密钥加载器 |

## 集成到您的项目

### Maven依赖

```xml
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 使用示例

#### 加密密码

```java
import com.dbpwd.crypto.Sm4Cipher;
import com.dbpwd.crypto.ENCFormat;

String plainPassword = "myDatabasePassword";
String sm4Key = "0123456789abcdef0123456789abcdef";

// 加密
String cipherText = Sm4Cipher.encrypt(plainPassword, sm4Key);
String formattedEncrypted = ENCFormat.formatSM4(cipherText);
System.out.println("Encrypted: " + formattedEncrypted);
// 输出: Encrypted: ENC(SM4:xxx)
```

#### 解密密码

```java
import com.dbpwd.crypto.Sm4Cipher;
import com.dbpwd.crypto.ENCFormat;

String encryptedPassword = "ENC(SM4:xxx)";
String sm4Key = "0123456789abcdef0123456789abcdef";

// 解密
String algorithm = ENCFormat.extractAlgorithm(encryptedPassword);
String cipherText = ENCFormat.extractCipherText(encryptedPassword);
String plainPassword = Sm4Cipher.decrypt(cipherText, sm4Key);
System.out.println("Decrypted: " + plainPassword);
```

## 常见问题

### Q1: 编译出现Java版本兼容错误？

**A**: 确保使用JDK 8+. 如果使用更高版本JDK，需要调整pom.xml中的javaee-api依赖。

### Q2: 如何更换SM4密钥？

**A**: 修改代码中的`SM4_KEY`常量，密钥必须是16字节（32个十六进制字符）的十六进制字符串。

### Q3: 密码格式ENC(SM4:xxx)是什么？

**A**: 这是标准加密密码格式，格式为`ENC(算法:密文)`。支持自动检测SM4、Druid、明文等多种格式。

### Q4: 示例中使用的数据库是什么？

**A**:
- Simple JDBC Demo: 仅演示加密功能，不涉及数据库连接
- Spring Boot Demo: 使用H2内存数据库
- Struts Demo: 仅演示加密功能，不涉及数据库连接

## 技术支持

如有问题，请参考主项目文档：
- [设计文档](./CLAUDE.md)
- [技术文档](./technical/)
- [项目README](./README.md)

## 版本信息

- 版本: 1.0.0
- 最后更新: 2026-02-09
- Java版本: JDK 8+
- 构建工具: Maven 3.6+
