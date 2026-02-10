# Database Password Manager - Demo Applications

这是一个数据库密码管理系统的演示应用集合，展示了SM4-GCM国密认证加解密功能。

## 概述

本项目包含三个演示应用，展示了在不同场景下使用Database Password Manager进行密码加密和管理的方式：

1. **Simple JDBC Demo** - 基础JDBC应用，演示核心SM4-GCM加解密功能
2. **Spring Boot Demo** - Spring Boot Web应用，演示与Spring框架集成
3. **Struts Demo** - 独立应用，演示密码加密解密验证

## 为什么使用SM4-GCM？

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
docker logs -f dbpwd-mysql
```

数据库连接信息：

| 参数 | 值 |
|------|-----|
| 主机 | localhost |
| 端口 | 3306 |
| 数据库 | dbpwd_demo |
| 用户名 | dbpwd |
| 密码 | dbpwd123 |

### 停止MySQL数据库

```bash
docker stop dbpwd-mysql
```

### 完整Demo演示流程

本Demo演示完整的Database Password Manager使用流程，从命令行工具生成配置文件开始。

#### 步骤1: 构建项目

```bash
mvn clean install -DskipTests
```

#### 步骤2: 使用命令行工具生成配置文件

```bash
# 进入命令行工具目录
cd db-pwd-core/target

# 运行命令行工具
java -jar db-pwd-core-1.0.0.jar --interactive

# 进入交互模式后，执行以下命令：

# 1. 列出当前数据库
list

# 2. 添加MySQL数据库配置
add
# 按提示输入：
# - ID: demo-mysql
# - 名称: MySQL Demo Database
# - 类型: MySQL
# - 主机: localhost
# - 端口: 3306
# - 数据库: dbpwd_demo
# - 用户名: dbpwd
# - 密码: dbpwd123

# 3. 测试数据库连接
test demo-mysql

# 4. 列出配置确认
list

# 5. 退出
exit
```

生成的配置文件位置：
- 密钥文件: `~/.dbpwd/sm4.key`
- 密码文件: `~/.dbpwd/db.pwd`

#### 步骤3: 验证配置文件

```bash
# 查看生成的配置文件内容（可选）
cat ~/.dbpwd/db.pwd
cat ~/.dbpwd/sm4.key
```

#### 步骤4: 运行Demo应用

配置文件生成后，可以运行各个Demo应用验证功能：

```bash
# 运行Simple JDBC Demo
cd db-pwd-simple-jdbc-demo
mvn clean package java -jar target/db-pwd-simple-jdbc-demo-1.0.0-jar-with-dependencies.jar

# 运行Spring Boot Demo
cd db-pwd-spring-boot-demo
mvn clean package
java -jar target/db-pwd-spring-boot-demo-1.0.0.jar

# 运行Struts Demo
cd db-pwd-struts-demo
mvn clean package
mvn exec:java -Dexec.mainClass="com.dbpwd.demo.StrutsDemo"
```

### 构建所有模块

```bash
mvn clean install -DskipTests
```

## Demo应用详情

### 1. Simple JDBC Demo

**位置**: `db-pwd-simple-jdbc-demo/`

**功能**:
- SM4-GCM加密功能演示
- MySQL数据库连接测试
- 密码格式化（ENC格式）
- 加密解密验证
- 随机IV验证
- 完整的数据库CRUD操作演示

**数据库**: MySQL 8.0

**运行方式**:

```bash
# 确保MySQL容器正在运行
docker ps | grep dbpwd-mysql

cd db-pwd-simple-jdbc-demo
mvn clean package -DskipTests
java -jar target/db-pwd-simple-jdbc-demo-1.0.0-jar-with-dependencies.jar
```

**预期输出**:
```
=== Simple JDBC Demo Application ===
Demo: Database Password Manager - SM4 Encryption
Database: MySQL (jdbc:mysql://localhost:3306/dbpwd_demo)
SM4 key: 0123456789abcdef0123456789abcdef

Step 1: Test SM4 Encryption
  Original password: myDatabasePassword123
  Encrypted password: ENC(SM4-GCM:xxx)
  Note: 每次加密结果不同（随机IV）

Step 2: Test SM4 Decryption
  Decrypted password: myDatabasePassword123
  Verification: SUCCESS

Step 3: Test ENC Format
  Database password: mysql_password
  Formatted encrypted: ENC(SM4-GCM:xxx)
  Decrypted: mysql_password
  Verification: SUCCESS

Step 4: Database Connection Test
  Connecting to MySQL database...
  Created demo tables
  Inserted test data
  Query results: [data from MySQL]

=== Demo Completed Successfully ===
All SM4 encryption and decryption tests passed!
MySQL database connection successful!
```

### 2. Spring Boot Demo

**位置**: `db-pwd-spring-boot-demo/`

**功能**:
- Spring Boot Web应用
- REST API接口
- MySQL数据库连接
- SM4-GCM加密功能演示
- 数据库初始化和CRUD操作

**数据库**: MySQL 8.0

**运行方式**:

```bash
# 确保MySQL容器正在运行
docker ps | grep dbpwd-mysql

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
  "database": "MySQL",
  "description": "Demo application with SM4-GCM encrypted password"
}
```

### 3. Struts Demo

**位置**: `db-pwd-struts-demo/`

**功能**:
- SM4-GCM加密解密验证
- MySQL数据库连接测试
- 多密码加密测试
- 随机IV验证
- 认证标签验证
- 数据库CRUD操作演示

**数据库**: MySQL 8.0

**运行方式**:

```bash
# 确保MySQL容器正在运行
docker ps | grep dbpwd-mysql

cd db-pwd-struts-demo
mvn clean package -DskipTests
mvn exec:java -Dexec.mainClass="com.dbpwd.demo.StrutsDemo"
```

**预期输出**:
```
=== Struts Demo Application (Standalone) ===
Demo: Database Password Manager - SM4-GCM Encryption
Database: MySQL (jdbc:mysql://localhost:3306/dbpwd_demo)
SM4 key: 0123456789abcdef0123456789abcdef

Test 1: Basic SM4-GCM Encryption/Decryption
  Password: struts_password_123
  Encrypted: ENC(SM4-GCM:xxx)
  Decrypted: struts_password_123
  Result: SUCCESS

Test 2: Multiple Password Encoding
  admin_pass => OK
  user_db_connection => OK
  migration_key => OK
  production_db => OK

Test 3: Different SM4-GCM Keys
  Test password: test_db_password
  Encrypted with key1: ENC(SM4-GCM:xxx)
  Decrypted with key1: test_db_password => OK
  Encrypted with key2: ENC(SM4-GCM:yyy)
  Decrypted with key2: test_db_password => OK
  Keys produce different ciphers: YES

Test 4: GCM Random IV Property
  Password: test_password_123
  Encryption A: ENC(SM4-GCM:aaa)
  Encryption B: ENC(SM4-GCM:bbb)
  Different ciphertexts: YES (GCM property)
  Both decrypt correctly: YES

Test 5: MySQL Database Connection
  Connected successfully to MySQL
  Created demo tables
  Inserted test data
  Query results: [data from MySQL]

=== Demo Completed Successfully ===
All SM4-GCM encryption and decryption tests passed!
GCM mode provides authenticated encryption and tamper protection.
MySQL database connection successful!
```

## GCM模式详解

### 加密流程

1. **生成随机IV**（12字节）：每次加密使用不同的初始化向量
2. **SM4加密**：使用GCM模式加密明文
3. **生成认证标签**（16字节）：对密文完整性进行认证
4. **Base64编码**：将`IV:密文`格式化为Base64编码

### 密文格式

```
ENC(SM4-GCM:Base64(IV):Base64(密文+认证标签))
```

### 解密流程

1. **解析ENC格式**：提取算法标识和密文数据
2. **Base64解码**：分别解码IV和密文
3. **GCM解密**：使用IV和GCM模式解密密文
4. **完整性验证**：验证认证标签，确保密文未篡改

### 安全特性

1. **完整性保护**：任何密文篡改都会导致解密失败
2. **抗重放攻击**：IV重放攻击被GCM认证标签阻止
3. **抗模式分析**：随机IV使相同明文产生不同密文
4. **选择明文攻击保护**：认证标签保护选择明文攻击

## 架构说明

### 多模块Maven项目结构

```
db-pwd/
├── pom.xml                 # 父POM
├── db-pwd-core/           # 核心模块
│   ├── src/main/java/com/dbpwd/
│   │   ├── crypto/       # SM4加密解密（支持ECB和GCM）
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

### 核心类

| 类名 | 功能 |
|-----|------|
| `Sm4GCMCipher` | SM4-GCM加解密 |
| `Sm4Cipher` | SM4-ECB加解密 |
| `ENCFormat` | 密码格式化工具 |
| `PasswordEncryptor` | 统一加密接口 |
| `PasswordDecryptor` | 统一解密接口 |

## 集成到您的项目

### Maven依赖

```xml
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 使用SM4-GCM加密密码

```java
import com.dbpwd.crypto.Sm4GCMCipher;
import com.dbpwd.crypto.ENCFormat;

String plainPassword = "myDatabasePassword";
String sm4Key = "0123456789abcdef0123456789abcdef";

// 使用SM4-GCM加密
String cipherText = Sm4GCMCipher.encrypt(plainPassword, sm4Key);
String formattedEncrypted = ENCFormat.formatSM4GCM(cipherText);
System.out.println("Encrypted: " + formattedEncrypted);
// 输出: Encrypted: ENC(SM4-GCM:xxx)
```

### 使用推荐的高级接口

```java
import com.dbpwd.crypto.PasswordEncryptor;
import com.dbpwd.crypto.PasswordDecryptor;

String plainPassword = "myDbPassword";
String sm4Key = "0123456789abcdef0123456789abcdef";

// 推荐：使用统一的加密接口（默认使用GCM）
String encrypted = PasswordEncryptor.encrypt(plainPassword, sm4Key);
// 得到: ENC(SM4-GCM:xxx)

// 推荐：使用统一的解密接口
String decrypted = PasswordDecryptor.decrypt(encrypted, sm4Key);
// 自动识别算法并正确解密
```

## 常见问题

### Q1: 如何更换SM4-GCM密钥？

**A**: 修改代码中的`SM4_KEY`常量，密钥必须是16字节（32个十六进制字符）的十六进制字符串。建议使用`Sm4GCMCipher.generateRandomHexKey()`生成随机安全密钥：

```java
String sm4Key = Sm4GCMCipher.generateRandomHexKey(); // 生成安全的随机密钥
```

### Q3: 密码格式ENC(SM4-GCM:xxx)是什么？

**A**:
- **ENC**：加密格式标识符
- **SM4-GCM**：算法标识（SM4国密 + GCM认证模式）
- **Base64(IV)**：12字节初始化向量的Base64编码
- **:**：分隔符
- **Base64(密文+认证标签)**：密文和16字节认证标签的Base64编码

### Q4: 示例中使用的数据库是什么？

**A**:
所有Demo应用都使用MySQL 8.0数据库：

- **Simple JDBC Demo**: MySQL数据库，演示加密和数据库连接
- **Spring Boot Demo**: MySQL数据库，Spring Boot + JdbcTemplate集成
- **Struts Demo**: MySQL数据库，演示加密和数据库连接

### Q5: 如何设置MySQL数据库？

**A**:
使用Docker运行MySQL容器（需要Docker Desktop已启动）：

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
docker logs -f dbpwd-mysql
```

### Q6: MySQL连接失败怎么办？

**A**:
1. 检查MySQL容器是否运行：`docker ps | grep dbpwd-mysql`
2. 等待MySQL启动完成（约30秒）：`docker logs dbpwd-mysql`
3. 检查端口3306是否被占用：`netstat -ano | findstr :3306` (Windows) 或 `lsof -i :3306` (Linux/Mac)
4. 手动测试连接：
   ```bash
   docker exec -it dbpwd-mysql mysql -u dbpwd -p dbpwd_demo
   # 输入密码: dbpwd123
   ```

## 技术支持

如有问题，请参考主项目文档：
- [设计文档](./CLAUDE.md)
- [技术文档](./technical/)
- [项目README](./README.md)

## 版本信息

- 版本: 1.0.0
- 最后更新: 2026-02-10
- Java版本: JDK 8+
- 构建工具: Maven 3.6+
- 加密算法: SM4-GCM（认证加密）
