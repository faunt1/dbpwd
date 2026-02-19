# Demo 应用修改对比报告

> 生成时间: 2026-02-19 12:25
> 修改原因: 数据库表和初始数据由 DatabaseInitializer 统一创建

## 执行摘要

### 修改内容

1. **新增 h2_init.sql** - 统一的数据库初始化脚本
2. **修改 DatabaseInitializer** - 从 SQL 文件加载并执行初始化
3. **修改 4 个原始版本 demo** - 删除建表和数据初始化代码
4. **修改 1 个集成版本 demo** - 同步删除建表和数据初始化代码

### 验证结果

| Demo | 原始版本 | 集成版本 | 验证状态 |
|------|---------|---------|---------|
| simple-jdbc-demo | ✅ 通过 | ✅ 通过 | 成功 |

---

## 详细修改记录

### 1. 新增文件

#### db-pwd-demo-init/src/main/resources/h2_init.sql

统一的数据库表结构和初始数据：

```sql
-- 用户表
CREATE TABLE IF NOT EXISTS users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 产品表
CREATE TABLE IF NOT EXISTS products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL,
    category VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    product_name VARCHAR(200) NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    quantity INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- 初始数据: 3 users, 3 products, 3 orders
```

### 2. 修改 DatabaseInitializer

**修改内容**:
- 添加 `loadInitSql()` 方法从 classpath 加载 h2_init.sql
- 修改 `executeInitSql()` 方法正确解析 SQL 语句（处理多行语句和注释）
- 修复更新现有条目时 URL 不更新的问题

### 3. 修改原始版本 Demo

| 文件 | 删除的代码 |
|------|-----------|
| SimpleJdbcDemo.java (original) | `createTables()`, `insertUser()`, `insertProduct()` 方法及调用 |
| SpringBootDemo.java (original) | `CommandLineRunner initDatabase()` 中的建表和插入代码 |
| UserDao.java (spring-original) | `createTables()`, `insert()` 方法 |
| UserDao.java (servlet-original) | `createTable()`, `insert()` 方法 |
| SpringDemoApplication.java | `userDao.createTables()`, `userDao.insert()` 调用 |

### 4. 修改集成版本 Demo

| 文件 | 删除的代码 |
|------|-----------|
| SimpleJdbcDemo.java | `createTables()`, `insertUser()`, `insertProduct()` 方法及调用 |

---

## 代码对比示例

### SimpleJdbcDemo.java (原始版本)

**修改前** (178 行):
```java
public static void main(String[] args) {
    // ... 连接数据库 ...
    
    // Step 2: Create demo tables first
    createTables(mainConnection);
    
    // Step 3: Insert users
    insertUser(mainConnection, "alice", ...);
    
    // Step 4: Insert products
    insertProduct(mainConnection, "Laptop", ...);
    
    // Query...
}

private static void createTables(Connection connection) throws Exception {
    stmt.execute("CREATE TABLE IF NOT EXISTS users (...)");
    stmt.execute("CREATE TABLE IF NOT EXISTS products (...)");
}

private static void insertUser(...) { ... }
private static void insertProduct(...) { ... }
```

**修改后** (约 100 行):
```java
public static void main(String[] args) {
    // ... 连接数据库 ...
    
    // 直接查询（表已由 DatabaseInitializer 创建）
    queryUsers(conn);
    queryProducts(conn);
    queryOrders(conn);
}

// 删除 createTables(), insertUser(), insertProduct() 方法
```

---

## 验证输出

### simple-jdbc-demo-original

```
=========================================
   Database Password Manager - Simple JDBC Demo
=========================================

Testing database connection...
  Password retrieved from db.pwd successfully
  Database connection established

=========================================
   Query Operations Demo
=========================================

1. Querying users...
   - ID: 1, Username: alice, Email: alice@example.com
   - ID: 2, Username: bob, Email: bob@example.com
   - ID: 3, Username: charlie, Email: charlie@example.com
   Found 3 user(s)

2. Querying products...
   - ID: 1, Name: Laptop, Price: 1299.99, Stock: 50, Category: Electronics
   - ID: 2, Name: Smartphone, Price: 899.99, Stock: 100, Category: Electronics
   - ID: 3, Name: Java Programming, Price: 49.99, Stock: 200, Category: Books
   Found 3 product(s)

3. Querying orders...
   - ID: 1, User: alice, Product: Laptop, Amount: 1299.99, Qty: 1
   - ID: 2, User: bob, Product: Smartphone, Amount: 899.99, Qty: 1
   - ID: 3, User: alice, Product: Java Programming, Amount: 49.99, Qty: 2
   Found 3 order(s)

=========================================
   Demo completed successfully!
=========================================

Security Issues in ORIGINAL version:
- Password is hardcoded in source code
- Password visible in configuration
```

### simple-jdbc-demo (集成版本)

```
=========================================
   Database Password Manager - Simple JDBC Demo
=========================================

Testing database connection...
  Password retrieved from db.pwd successfully
  Database connection established

=========================================
   Query Operations Demo
=========================================

1. Querying users...
   Found 3 user(s)

2. Querying products...
   Found 3 product(s)

3. Querying orders...
   Found 3 order(s)

=========================================
   Demo completed successfully!
=========================================

Key points demonstrated:
- PasswordService.getDatabasePassword() for password decryption
- H2 FILE database with IFEXISTS=TRUE
- Tables and data pre-initialized by DatabaseInitializer
```

---

## 待处理

以下 demo 文件仍需删除建表和数据初始化代码（集成版本）：

1. db-pwd-druid-demo/src/main/java/com/dbpwd/demo/druid/DruidDemoApplication.java
2. db-pwd-spring-boot-demo/src/main/java/com/dbpwd/demo/SpringBootDemo.java
3. db-pwd-spring-demo/src/main/java/com/dbpwd/demo/spring/dao/UserDao.java
4. db-pwd-servlet-demo/src/main/java/com/dbpwd/demo/servlet/dao/UserDao.java
5. db-pwd-mybatis-demo/src/main/java/com/dbpwd/demo/mybatis/MybatisDemoApplication.java
6. db-pwd-struts-demo/src/main/java/com/dbpwd/demo/StrutsDemo.java
7. db-pwd-druid-demo-spring/src/main/java/com/dbpwd/demo/druid/spring/DruidSpringDemoApplication.java
8. db-pwd-spring-demo-xml/src/main/java/com/dbpwd/demo/spring/xml/SpringXmlDemoApplication.java
9. db-pwd-servlet-demo-annotation/.../AnnotationDemoInitializer.java
10. db-pwd-spring-boot-demo-api/.../DemoController.java

---

## 重要提醒

### 执行 Java 时设置 UTF-8 编码

Windows 环境**必须**设置 `-Dfile.encoding=UTF-8`：

```bash
# 正确
java -Dfile.encoding=UTF-8 -jar demo.jar

# 错误 (中文乱码)
java -jar demo.jar
```

已在 AGENTS.md 中记录此要求。
