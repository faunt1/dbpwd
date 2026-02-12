# Database Password Manager - Demo应用演示步骤

> 文档版本: 2.0
> 最后更新: 2026-02-12

## 概述

本文档描述了Database Password Manager 6个Demo应用的完整演示执行步骤。每个Demo展示一种特定的集成方式，涵盖主流Java技术栈的集成场景。

## 前置要求

- JDK 8+
- Maven 3.6+

---

## H2数据库设置

所有Demo使用H2内存数据库，无需外部数据库服务。启动Demo时会自动创建内存数据库和表结构。

**H2数据库通用配置**:

| 参数 | 值 |
|------|-----|
| 用户名 | sa |
| 密码（明文） | `dbpwd_h2_demo_password_2024` |
| 存储方式 | 在db.pwd中经过SM4加密 |
| 连接超时 | 5秒 |

---

## Demo占位符映射

| Demo应用 | 占位符(在db.pwd中) | H2数据库名 | 集成方案 | 说明 |
|---------|------------------|-----------|---------|------|
| Simple JDBC Demo | `simple_jdbc_db` | `dbpwd_simple_demo` | 模式1: 纯JDBC | 程序直接调用API |
| Druid Demo | `druid_demo_db` | `dbpwd_druid_demo` | 方案1: Druid Callback | 利用Druid原生机制 |
| Spring Boot Demo | `spring_boot_db` | `dbpwd_spring_boot_demo` | 方案2: Spring Boot Starter | 自动装饰DataSource |
| Spring Demo | `spring_framework_db` | `dbpwd_spring_demo` | 方案3: BeanPostProcessor | 拦截DataSource Bean |
| MyBatis Demo | `mybatis_demo_db` | `dbpwd_mybatis_demo` | 方案4: MyBatis Factory | MyBatis工厂类 |
| Servlet Demo | `servlet_demo_db` | `dbpwd_servlet_demo` | 方案6: InitListener | Servlet初始化监听器 |

**说明**: 每个Demo使用独立的H2内存数据库，演示不同的技术栈集成方案。

---

## Demo验证步骤

### 步骤1: 构建项目

```bash
mvn clean package -DskipTests
```

---

### 步骤2: 验证 Simple JDBC Demo

**Demo信息**:
- **项目**: `db-pwd-simple-jdbc-demo`
- **集成方案**: 模式1 - 纯JDBC API
- **占位符**: `simple_jdbc_db`
- **实际数据库**: `dbpwd_simple_demo` (H2内存)
- **工作机制**: 程序直接调用PasswordProvider API获取解密密码

#### 执行命令

```bash
cd db-pwd-simple-jdbc-demo
mvn clean package -DskipTests
java -jar target/db-pwd-simple-jdbc-demo-1.0.0-jar-with-dependencies.jar
```

#### 预期输出

```
=========================================
   Database Password Manager - Simple JDBC Demo
=========================================

Testing database connection...
Initializing database tables...

=========================================
   Simple JDBC Demo CRUD Operations
=========================================

1. Creating users...
   Created user: alice (user1@example.com, hashed_password1)

2. Creating products...
   Created product: Laptop (1299.99, 50)
   Created product: Smartphone (899.99, 100)

3. Querying all products...
   Found 2 product(s):
   - ID: 1, Name: Laptop, Price: 1299.99, Stock: 50
   - ID:2, Name: Smartphone, Price: 899.99, Stock: 100

4. Querying user by ID...
   Found user: alice

5. Multiple password encryptions (encryptions produce different ciphers): CONFIRMED
    ENC decryption test: PASSED

6. DbUtil query example:
   Query executed successfully, returned 2 row(s)

=========================================
   Demo completed successfully!
=========================================

Key points demonstrated:
- PasswordProvider.getPlainPassword() for direct decryption
- StandalonePasswordDecryptor.getConnection() for database connections
- DbUtil.query() for simple database queries
- ENC(SM4-GCM:xxx) format with random IV
- H2 in-memory database for demo
```

**验证内容**:
- SM4-GCM加密解密功能
- 密码占位符自动解密
- 纯JDBC数据库连接
- 增删改查操作
- 随机IV验证

---

### 步骤3: 验证 Druid Demo

**Demo信息**:
- **项目**: `db-pwd-druid-demo`
- **集成方案**: 方案1 - Druid PasswordCallback
- **占位符**: `druid_demo_db`
- **实际数据库**: `dbpwd_druid_demo` (H2内存)
- **工作机制**: Druid连接池 + ENC格式密码自动解密

#### 执行命令

```bash
cd db-pwd-druid-demo
mvn clean package -DskipTests
java -jar target/db-pwd-druid-demo-1.0.0.jar
```

#### 预期输出

```
========================================
   Database Password Manager - Druid Demo
========================================

Testing database connection...
Initializing database tables...

========================================
   Druid CRUD Operations Demo
========================================

1. Creating products...
   Created product: Laptop (1299.99, 50)
   Created product: Smartphone (899.99, 100)
   Created product: Book (49.99, 200)

2. Querying all products...
   Found 3 product(s):
   - ID: 1, Name: Laptop, Price: 1299.99, Stock: 50
   - ID: 2, Name: Smartphone, Price: 899.99, Stock: 100
   - ID: 3, Name: Book, Price: 49.99, Stock: 5)

3. Querying product by ID: 1...
   Found: Laptop - High-performance laptop

4. Updating product ID: 1...
   Updated 1 row(s).

5. Deleting product ID: 2...
   Deleted 1 row(s).

6. Querying remaining products...
   Found 2 product(s):
   - ID: 1, Name: Laptop, Price: 1199.99, Stock: 45
   - ID:3, Name: Book, Price: 49.99, Stock: 5

========================================
   Demo completed successfully!
=========================================

Key points demonstrated:
- ENC(SM4-GCM:xxx) format for encrypted password
- Druid connection pool manages connections
- Password automatically decrypted without callback (ENC format)
- H2 in-memory database for demo
```

**验证内容**:
- ENC格式密码在Druid连接池中生效
- Druid连接池配置和使用方法
- 完整的CRUD操作流程
- 连接池自动资源管理

---

### 步骤4: 验证 Spring Boot Demo

**Demo信息**:
- **项目**: `db-pwd-spring-boot-demo`
- **集成方案**: 方案2 - Spring Boot Starter
- **占位符**: `spring_boot_db`
- **实际数据库**: `dbpwd_spring_boot_demo` (H2内存)
- **工作机制**: Spring Boot自动配置 + DataSourceDecorator自动拦截

#### 执行命令

```bash
cd db-pwd-spring-boot-demo
mvn clean package -DskipTests
# 启动应用
java -jar target/db-pwd-spring-boot-demo-1.0.0.jar
```

#### 测试API接口

应用启动后，在新终端中测试：

```bash
# 测试应用信息接口
curl http://localhost:8080/

# 测试健康检查
curl http://localhost:8080/health

# 测试用户列表接口
curl http://localhost:8080/users

# 测试产品列表接口
curl http://localhost:8080/products

# 测试创建用户接口
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"username":"newuser","email":"newuser@example.com","password":"pwd123"}'
```

#### 预期输出

```
========================================
   Database Password Manager - Spring Boot Starter Demo
========================================

Starting Spring Boot application...
...
Loading password file from: C:\Users\faunt\.dbpwd\demo_sm4.key
DataSourcePasswordProcessor configured with key file: C:\Users\faunt\.dbpwd\demo_sm4.key
Druid dataSource configured with URL: jdbc:h2:mem:dbpwd_spring_boot_demo...
DatabasePasswordManager Starter initialized!

=========================================
   Spring Boot Starter CRUD Demo
========================================

Users API:

1. Getting all users...
GET /users - 200 OK
[
  {"id":1,"username":"alice","email":"alice@example.com","password":"***"},
  {"id":2,"username":"bob","email":"bob@example.com","password":"***"},
  {"id":3,"username":"charlie","email":"charlie@example.com","password":"***"}
]
========================================
```

**验证内容**:
- Spring Boot自动配置机制
- spring.factories自动发现
- DataSourceDecorator密码自动解密
- RESTful API接口
- 健康检查接口

---

### 步骤5: 验证 Spring Demo

**Demo信息**:
- **项目**: `db-pwd-spring-demo`
- **集成方案**: 方案3 - BeanPostProcessor
- **占位符**: `spring_framework_db`
- **实际数据库**: `dbpwd_spring_demo` (H2内存)
- **工作机制**: DataSourcePasswordProcessor拦截DataSource Bean，自动解密ENC格式密码

#### 执行命令

```bash
cd db-pwd-spring-demo
mvn clean package -DskipTests
java -jar target/db-pwd-spring-demo.jar
```

#### 预期输出

```
========================================
   Database Password Manager - Spring Framework Demo
========================================

Starting Spring container...
Loading password file from C:\Users\faunt\.dbpwd\demo_sm4.key
DataSourcePasswordProcessor configured...
Druid dataSource configured...

========================================
   Spring Framework CRUD Demo
========================================

1. Creating users...
   Created user: john_doe (john@example.com)
   Created user: jane_smith (jane@example.com)

2. Querying all users...
   Found 2 user(s):
   - ID: 1, Username: john_doe, Email: john@example.com
   - ID: 2, Username: jane_smith, Email: jane@example.com

3. Updating user ID: 1...
   Updated 1 row(s).

4. Deleting user ID: 2...
   Deleted 1 row(s).

5. Querying remaining users...
   Found 1 user(s):
   - ID: 1, Username: john.doe, Email: john.updated@example.com

========================================
   Demo completed successfully!
=========================================

Key points demonstrated:
- BeanPostProcessor automatically decrypts ENC passwords
- Spring BeanPostProcessor intercepts DataSource initialization
- JdbcTemplate for database operations
- Database connection without manual password management
```

**验证内容**:
- BeanPostProcessor自动检测并解密密码
- Spring容器级别的Bean拦截
- JdbcTemplate数据库访问
- 无需修改原始业务代码

---

### 步骤6: 验证 MyBatis Demo

**Demo信息**:
- **项目**: `db-pwd-mybatis-demo`
- **集成方案**: 方案4 - MyBatis DataSourceFactory
- **占位符**: `mybatis_demo_db`
- **实际数据库**: `dbpwd_mybatis_demo` (H2内存)
- **工作机制**: DbPwdDataSourceFactory配合MyBatis配置，自动解密密码

#### 执行命令

```bash
cd db-pwd-mybatis-demo
mvn clean package -DskipTests
java -jar target/db-pwd-mybatis-demo-1.0.0.jar
```

#### 预期输出

```
========================================
   Database Password Manager - MyBatis Integration Demo
========================================

Testing database connection...
Initializing database tables...

========================================
   MyBatis Mapper CRUD Demo
========================================

1. Creating products...
   Created product: Laptop (1299.99, 50)
   Created product: Smartphone (899.99, 100)
   Created product: Book (49.99, 200)

2. Querying all products...
   Found 3 product(s):
   - ID: 1, Name: Laptop, Price: 1299.99, Stock: 50
   - ID: 2, Name: Smartphone, Price: 899.99, Stock: 100
   - ID: 3, Name: Book, Price: 49.99, Stock: 200)

3. Querying product by ID: 1...
   Found: Laptop - High-performance laptop

4. Updating product ID: 1...
   Updated 1 row(s).

5. Deleting product ID: 2...
   Deleted 1 row(s).

6. Querying remaining products...
   Found 2 product(s):
   - ID: 1, Name: Laptop, Price: 1199.99, Stock: 45
   - ID: 3, Name: Book, Price: 49.99, Stock: 200

========================================
   Demo completed successfully!
=========================================

Key points demonstrated:
- DbPwdDataSourceFactory for MyBatis integration
- Mapper interface with @Annotations
- SM4-GCM password decryption with MyBatis
- H2 in-memory database for demo
- Automatic password lookup from db.pwd
```

**验证内容**:
- MyBatis Mapper接口定义和使用
- MyBatis XML配置方式
- 数据库连接池配置
- 完整的数据库操作流程

---

### 步骤7: 验证 Servlet Demo

**Demo信息**:
- **项目**: `db-pwd-servlet-demo`
- **集成方案**: 方案6 - Servlet InitListener
- **占位符**: `servlet_demo_db`
- **实际数据库**: `dbpwd_servlet_demo` (H2内存)
- **打包方式**: WAR包
- **工作机制**: ServletContextListener初始化密码服务，WebPasswordUtil统一访问

#### 执行命令

```bash
cd db-pwd-servlet-demo
mvn clean package -DskipTests
# 使用Tomcat Maven Plugin运行（需要本地Tomcat）
mvn tomcat7:run -pl db-pwd-servlet-demo
```

#### 部署和测试

```bash
# 部署到Tomcat服务器
cp target/db-pwd-servlet-demo-1.0.0.war $TOMCAT_HOME/webapps/

# 测试根路径
curl http://localhost:8081/dbpwd-servlet-demo/

# 测试API接口
curl http://localhost:8081/dbpwd-servlet-demo/api/users

# 测试创建用户
curl -X POST http://localhost:8081/dbpwd-servlet-demo/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","email":"test@example.com"}'
```

#### 预期输出

```
========================================
   Database Password Manager - Servlet Integration Demo
========================================

Initializing database tables...

========================================
   Servlet CRUD Operations Demo
========================================

1. Creating users...
   Created user: servlet_user (test@example.com, hashed_password1)

2. Getting all users...
   Found 1 user(s):
   - ID: 1, Username: servlet_user, Email: test@example.com

3. Getting user by ID: 1...
   Found user: servlet_user (test@example.com)

4. Updating user ID: 1...
   Updated 1 row(s).

5. Deleting user ID: 1...
   Deleted 0 row(s).

6. Querying remaining users...
   Found 0 user(s).

========================================
   Demo completed successfully!
=========================================

Key points demonstrated:
- WebPasswordUtil for password access in Servlet
- DatabasePasswordInitListener for Web container initialization
- ServletContext password service caching
- REST API endpoints implementation
- HTTP POST/DELETE methods
```

**验证内容**:
- ServletContextListener初始化机制
- WebPasswordUtil统一访问密码
- REST API CRUD操作
- Servlet请求处理和响应
- 容器部署配置

---

### 步骤8: 验证Struts Demo

**Demo信息**:
- **项目**: `db-pwd-struts-demo`
- **集成方案**: 模式1 - 独立应用
- **占位符**: `struts_demo_db`
- **实际数据库**: `dbpwd_struts_demo` (H2内存)
- **工作机制**: 程序完全独立，手动处理加密解密流程

#### 执行命令

```bash
cd db-pwd-struts-demo
mvn clean package -DskipTests
mvn exec:java -Dexec.mainClass="com.dbpwd.demo.StrutsDemo"
```

#### 预期输出

```
========================================
   Database Password Manager - Struts Demo (Standalone)
========================================

Testing database connection...
Initializing database tables...

========================================
   Struts Password Encoding Test
========================================

Test 1: Basic SM4-GCM Encryption/Decryption
  Password: struts_password
  Encrypted: ENC(SM4-GCM:xxx)
  Decrypted: struts_password
  Result: SUCCESS

Test 2: Multiple Password Encoding
  struts_db_connection, migration_key, production_db => OK

Test 3: Different SM4-GCM Keys
  Keys produce different ciphers: YES

Test 4: GCM Random IV Property
  Different ciphertexts: YES (GCM property)

Test 5: Backward Compatibility with ECB Format
  ECB decryption: OK

========================================
   Struts CRUD Operations Demo
========================================

Test 6: Creating users...
   Created user: struts_user (struts@example.com)
   Created user: admin_user (admin@example.com)

Test 7: Querying all users...
   Found 2 user(s):
   - ID: 1, Username: struts_user, Email: struts@example.com
   - ID: 2, Username: admin_user, Email: admin@example.com

Test 8: Creating products...
   Created product: Laptop (1299.99, 50)
   Created product: Smartphone (899.99, 100)
   Created product: Book (49.99, 200)

Test 9: Querying products...
   Found 3 product(s):
   - ID: 1, Name: Laptop, Price: 1299.99, Stock: 50
   - ID:2, Name: Smartphone, Price: 899.99, Stock: 100
   - ID: 3, Name: Book, Price: 49.99, Stock: 200

========================================
   Demo completed successfully!
=========================================

Key points demonstrated:
- Standalone SM4-GCM encryption implementation
- MyBatis SQL mapping in independent application
- Struts 1.x Action form handling
- Complete SM4-GCM decryption workflow
- Password file management integration
- H2 in-memory database for demo
```

**验证内容**:
- 独立应用中SM4解密完整流程
- MyBatis XML配置和映射
- Struts框架表单处理
- 文件I/O操作
- 密码管理集成

---

## 验证总结

| Demo应用 | 占位符 | 集成方案 | 数据库名 | 状态 |
|---------|--------|---------|----------|------|
| Simple JDBC Demo | `simple_jdbc_db` | 模式1: StandalonePasswordDecryptor | `dbpwd_simple_demo` | ✅ 验证通过 |
| Druid Demo | `druid_demo_db` | 方案1: Druid PasswordCallback | `dbpwd_druid_demo` | ✅ 验证通过 |
| Spring Boot Demo | `spring_boot_db` | 方案2: Spring Boot Starter | `dbpwd_spring_boot_demo` | ✅ 验证通过 |
| Spring Demo | `spring_framework_db` | 方案3: BeanPostProcessor | `dbpwd_spring_demo` | ✅ 验证通过 |
| MyBatis Demo | `mybatis_demo_db` | 方案4: MyBatis DataSourceFactory | `dbpwd_mybatis_demo` | ✅ 验证通过 |
| Servlet Demo | `servlet_demo_db` | 方案6: Servlet InitListener | `dbpwd_servlet_demo` | ✅ 验证通过 |

---

### 集成方案对比

| 特性 | 模式1: 纯JDBC | 方案1: Druid | 方案2: Spring Boot | 方案3: Spring | 方案4: MyBatis | 方案6: Servlet |
|-----|-----------------|--------|-------------|---------|---------|
| **适用场景** | 工具/独立应用 | Druid连接池 | Spring Boot应用 | 传统Spring应用 | MyBatis应用 | Servlet/Struts |
| **配置方式** | 代码中调用 | API配置 | application.yml | Java/XML配置 | XML配置 | web.xml |
| **框架依赖** | 无 | Druid | Spring Boot | Spring Framework | MyBatis | Servlet API |
| **自动解密** | **手动调用API** | **ENC格式检测** | **自动装饰** | **Processor拦截** | **工厂类查找** | **InitListener初始化** |
| **启动方式** | java -jar | java -jar | java -jar | java -jar | java -war | tomcat run |
| **API访问** | 无 | 无 | REST API | 无 | Mapper接口 | Servlet API |

---

## 快速开始

### 环境准备

```bash
# 安装核心模块和集成模块
mvn clean install -DskipTests
```

### 运行指定Demo

```bash
# 1. Simple JDBC Demo (无需框架，最简单)
java -jar db-pwd-simple-jdbc-demo/target/db-pwd-simple-jdbc-demo.jar

# 2. Spring Boot Demo (REST API)
java -jar db-pwd-spring-boot-demo/target/db-pwd-spring-boot-demo.jar

# 3. Spring Demo (传统Spring应用)
java -jar db-pwd-spring-demo/target/db-pwd-spring-demo.jar

# 4. MyBatis Demo (MyBatis应用)
java -jar db-pwd-mybatis-demo/target/db-pwd-mybatis-demo.jar

# 5. Druid Demo (Druid连接池)
java -jar db-pwd-druid-demo/target/db-pwd-druid-demo.jar

# 6. Servlet Demo (Web应用)
mvn tomcat7:run -pl db-pwd-servlet-demo
curl http://localhost:8081/dbpwd-servlet-demo/api/users
```

---

## 相关文档

- [Demo应用集成指南](./demo-README.md) - 如何将Database Password Manager集成到您的项目中
- [集成场景和方案](../technical/integration-scenarios.md) - 6种集成场景详解
- [集成模块README](../db-pwd-*/README.md) - 各集成模块详细文档

---

## 技术栈

| 组件 | 版本 | 说明 |
|-----|------|------|
| JDK | 8+ | Java开发环境 |
| SM4算法 | Bouncy Castle 1.69+ | 国密算法实现 |
| MyBatis | 3.5.13 | ORM框架 |
| Druid | 1.2.20+ | 数据库连接池（可选）|
| HikariCP | 5.0.0+ | 连接池（可选）|
| Spring Framework | 5.3.31 | Spring框架（传统Spring应用）|
| Spring Boot | 2.7.x+ | Spring Boot自动配置|
| Servlet API | 4.0+ | Web层接口 |
| Maven | 3.6+ | 构建工具 |

---

## 版本信息

- 文档版本: 2.0
- 最后更新: 2026-02-12
- Java版本: JDK 8+
- 构建工具: Maven 3.6+
