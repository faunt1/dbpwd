# Database Password Manager - Demo应用演示步骤

> 文档版本: 1.0
> 最后更新: 2026-02-10

## 概述

本文档描述了Database Password Manager三个Demo应用的完整演示执行步骤。

## 前置要求

- JDK 8+
- Maven 3.6+
- Docker Desktop（用于运行MySQL数据库）

---

## MySQL数据库设置

在运行Demo应用之前，需要先启动MySQL容器。

### 启动MySQL容器

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

### 数据库连接信息

| 参数 | 值 |
|------|-----|
| 主机 | localhost |
| 端口 | 3306 |
| 数据库 | dbpwd_demo |
| 用户名 | dbpwd |
| 密码 | dbpwd123 |

### Demo占位符映射

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

## Demo验证步骤

### 步骤1: 构建项目

```bash
mvn clean package -DskipTests
```

---

### 步骤2: 验证Simple JDBC Demo

**Demo信息**:
- **集成模式**: 模式1（程序直接操作模式）
- **解密实现方式**: 程序自己实现（直接调用Sm4GCMCipher API）
- **占位符**: `simple_jdbc_db`（指向实际MySQL数据库 `dbpwd_demo`）
- **实际数据库**: `dbpwd_demo` (MySQL)

**工作机制**:
1. 程序直接调用 `Sm4GCMCipher.encrypt()` 加密密码
2. 程序直接调用 `Sm4GCMCipher.decrypt()` 解密密码
3. 使用明文密码构建JDBC连接串
4. 建立数据库连接并执行CRUD操作

#### 执行命令

```bash
cd db-pwd-simple-jdbc-demo
mvn clean package -DskipTests
java -jar target/db-pwd-simple-jdbc-demo-1.0.0-jar-with-dependencies.jar
```

#### 预期输出

```
=== Simple JDBC Demo Application ===
Demo: Database Password Manager - SM4-GCM Encryption
Database: MySQL
SM4 key: 0123456789abcdef0123456789abcdef

Step 1: Encrypt database password
  Original password: dbpwd123
  Encrypted password: ENC(SM4-GCM:xxx)
  Note: 每次加密结果不同（随机 IV）

Step 2: Decrypt password and connect to MySQL
  Decrypted password: dbpwd123
  Verification: SUCCESS

Step 3: Test MySQL database connection
  Connection successful!

Step 4: Create demo tables
  Tables created successfully!

Step 5: Insert test data
  Test data inserted successfully!

Step 6: Query and display data
  Users and Orders data displayed...

Step 7: Test SM4-GCM encryption properties
  SM4-GCM Properties verified

Step 8: Test ENC format handling
  ENC Format Validation verified

Step 9: Demonstrate different encrypted passwords produce different results
  GCM Random IV Property verified

=== Demo Completed Successfully ===
All SM4-GCM encryption and decryption tests passed!
MySQL database connection and CRUD operations successful!
GCM mode provides authenticated encryption with tamper protection.
```

**验证内容**:
- SM4-GCM加密解密
- MySQL数据库连接
- 数据库CRUD操作
- 随机IV验证（每次加密结果不同）
- ENC格式验证

**实际的详细执行输出请参考** [demo-run-result.md](./demo-run-result.md)

---

### 步骤3: 验证Spring Boot Demo

**Demo信息**:
- **集成模式**: 模式2（配置文件装饰器模式）
- **解密实现方式**: 框架实现（DataSourceDecorator自动拦截和替换）
- **占位符**: `spring_boot_db`（指向实际MySQL数据库 `dbpwd_demo`）
- **实际数据库**: `dbpwd_demo` (MySQL)

**工作机制**:
1. Spring Boot应用启动时加载配置文件
2. DataSourceDecorator拦截DataSource初始化
3. 根据占位符 `spring_boot_db` 查找 `db.pwd` 中的密文密码
4. 使用 `sm4.key` 解密密码
5. 将明文密码注入到DataSource配置中
6. 建立连接池和数据库连接
7. 应用正常使用数据库连接

#### 执行命令

```bash
cd db-pwd-spring-boot-demo
mvn clean package -DskipTests
# 启动应用
java -jar target/db-pwd-spring-boot-demo-1.0.0.jar
```

#### 测试API接口

等待应用启动后，在新终端中测试API接口：

```bash
# 测试应用根路径
curl http://localhost:8080/

# 测试健康检查
curl http://localhost:8080/health

# 测试获取用户列表
curl http://localhost:8080/users

# 测试获取订单列表
curl http://localhost:8080/orders

# 测试统计信息
curl http://localhost:8080/stats
```

#### 预期输出

应用启动输出：
```
=== Spring Boot Demo Application ===
Database: MySQL
Database URL: jdbc:mysql://localhost:3306/dbpwd_demo?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
Database password: dbpwd123
SM4 key: 0123456789abcdef0123456789abcdef
SM4-GCM Encryption Test:
  Original password: dbpwd123
  Encrypted password: ENC(SM4-GCM:xxx)

Starting Spring Boot application...
...
Database initialized with 3 users and 3 orders

SM4-GCM Decryption Test:
  Algorithm: SM4-GCM
  Decrypted password: dbpwd123
  Verification: SUCCESS

=== Demo Completed Successfully ===
```

#### API测试接口

| 接口 | 方法 | 描述 |
|-----|------|------|
| `/` | GET | 应用信息 |
| `/health` | GET | 健康检查和用户数量 |
| `/users` | GET | 用户列表 |
| `/orders` | GET | 订单列表 |
| `/stats` | GET | 统计信息 |

**验证内容**:
- Spring Boot应用启动
- SM4-GCM加密解密
- MySQL数据库连接
- REST API接口
- 数据库初始化

**实际的详细执行输出和API测试结果请参考** [demo-run-result.md](./demo-run-result.md)

---

### 步骤4: 验证Struts Demo

**Demo信息**:
- **集成模式**: 模式1（程序直接操作模式）
- **解密实现方式**: 程序自己实现（独立应用，完全手动处理加密解密流程）
- **占位符**: `struts_db`（指向实际MySQL数据库 `dbpwd_demo`）
- **实际数据库**: `dbpwd_demo` (MySQL)

**工作机制**:
1. 程序独立启动，不依赖Spring框架
2. 直接调用 `Sm4GCMCipher.encrypt()` 和 `Sm4GCMCipher.decrypt()` API
3. 手动处理加密解密流程
4. 使用明文密码构建JDBC连接串
5. 建立数据库连接并执行CRUD操作

#### 执行命令

```bash
cd db-pwd-struts-demo
mvn clean package -DskipTests
mvn exec:java -Dexec.mainClass="com.dbpwd.demo.StrutsDemo"
```

#### 预期输出

```
=== Struts Demo Application (Standalone) ===
Demo: Database Password Manager - SM4-GCM Encryption
Database: MySQL
SM4 key: 0123456789abcdef0123456789abcdef

Test 1: Basic SM4-GCM Encryption/Decryption
  Password: struts_password_123
  Encrypted: ENC(SM4-GCM:xxx)
  Decrypted: struts_password_123
  Result: SUCCESS

Test 2: Multiple Password Encoding
  admin_pass, user_db_connection, migration_key, production_db => OK

Test 3: Different SM4-GCM Keys
  Keys produce different ciphers: YES

Test 4: GCM Random IV Property
  Different ciphertexts: YES (GCM property)

Test 5: Backward Compatibility with ECB Format
  ECB decryption: OK

Test 6: MySQL Database Connection
  MySQL connection successful!
  Demo tables created!
  Test data inserted!
  Query Results:
    Users and Orders data displayed...

=== Demo Completed Successfully ===
All SM4-GCM encryption and decryption tests passed!
MySQL database connection and CRUD operations successful!
GCM mode provides authenticated encryption and tamper protection.
```

**验证内容**:
- SM4-GCM加密解密
- 多密码加密测试
- 随机IV验证
- ECB格式向后兼容
- MySQL数据库连接
- 数据库CRUD操作

**实际的详细执行输出请参考** [demo-run-result.md](./demo-run-result.md)

---

## 验证总结

| Demo应用 | 占位符 | 集成模式 | 解密实现方式 | 状态 |
|---------|--------|---------|------------|------|
| Simple JDBC Demo | `simple_jdbc_db` | 模式1 | 程序自己实现 | ✅ 成功 |
| Spring Boot Demo | `spring_boot_db` | 模式2 | 框架实现 | ✅ 成功 |
| Struts Demo | `struts_db` | 模式1 | 程序自己实现 | ✅ 成功 |

**详细验证结果**请参考 [demo-run-result.md](./demo-run-result.md)。

**结论**: 三个Demo应用均验证通过，SM4-GCM认证加密功能正常，MySQL数据库连接和CRUD操作正常。不同集成模式（模式1和模式2）都得到了有效验证。

---

## 相关文档

- [Demo应用设计文档](./demo-design.md) - 三个Demo的详细设计架构和机制说明
- [Demo应用集成指南](./demo-README.md) - 如何将Database Password Manager集成到您的项目中
- [demo-run-result.md](./demo-run-result.md) - 所有Demo的详细执行输出
