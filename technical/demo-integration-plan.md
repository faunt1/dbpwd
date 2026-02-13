# Demo集成方案规划

> 文档版本: v1.1
> 最后更新: 2026-02-13

## 概述

本文档定义了7个Demo应用方案，包括一个初始化工具和6个集成场景Demo，每个集成模块都配有对应的Demo应用来验证集成功能。

---

## Demo列表

| 序号 | Demo应用 | 类型 | 说明 |
|-----|---------|------|------|
| 0 | db-pwd-demo-init | 初始化工具 | Demo数据库初始化工具（必须首先运行）|
| 1 | db-pwd-simple-jdbc-demo | 集成Demo | 纯JDBC应用，演示独立工具类使用 |
| 2 | db-pwd-druid-demo | 集成Demo | Druid连接池演示PasswordCallback集成 |
| 3 | db-pwd-spring-boot-demo | 集成Demo | Spring Boot Web应用演示自动配置 |
| 4 | db-pwd-spring-demo | 集成Demo | 传统Spring应用演示BeanPostProcessor |
| 5 | db-pwd-mybatis-demo | 集成Demo | 纯MyBatis应用演示工厂模式 |
| 6 | db-pwd-servlet-demo | 集成Demo | 纯Servlet应用演示监听器 |

---

## Demo 0: db-pwd-demo-init （初始化工具）

**类型**: 命令行工具

**目标**: 为所有其他Demo创建H2 FILE数据库，支持密码验证

**技术栈**:
- JDK 8
- H2 Database 2.2.x
- db-pwd-core (密码管理核心)

**功能**:
- 从 `~/.dbpwd/db.pwd` 读取数据库配置
- 使用 `~/.dbpwd/sm4.key` 解密密码
- 创建 H2 FILE 数据库（支持密码验证）
- 初始化统一的数据库表结构

**使用步骤**:
```bash
# 1. 生成 db.pwd 和 sm4.key
mvn exec:java -pl db-pwd-core \
  -Dexec.mainClass="com.dbpwd.cli.DBPasswordCLI" \
  --demo --generate-key

# 2. 运行demo-init初始化H2 FILE数据库
mvn exec:java -pl db-pwd-demo-init \
  -Dexec.mainClass="com.dbpwd.init.DatabaseInitializer"

# 3. 验证数据库文件
ls ~/.dbpwd/h2_data/
```

**执行示例**:
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
=========================================
   Initialization Summary
=========================================
  Total databases: 7
  Successfully initialized: 7
  H2 data directory: C:\Users\faunt\.dbpwd\h2_data
```

**创建的数据库文件**:
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

**关键代码**:
```java
// DatabaseInitializer.java
public class DatabaseInitializer {

    public static void main(String[] args) {
        // 1. 从 db.pwd 获取密码
        String plainPassword = PasswordProvider.getDatabasePassword(placeholder);

        // 2. 连接 H2 FILE 数据库（首次连接创建并绑定密码）
        String url = "jdbc:h2:file:" + dataDir + "/" + dbName + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        Connection conn = DriverManager.getConnection(url, "sa", plainPassword);

        // 3. 创建表结构
        stmt.execute(tableSql);
    }
}
```

**H2 FILE模式优势**:
- 首次连接时创建数据库并绑定密码
- 后续连接必须使用相同密码（真正验证密码有效性）
- 持久化存储，数据在程序关闭后仍保留
- 模拟生产环境数据库密码验证机制

状态: ✅ 已完成

---

## 集成模块与Demo对应关系

| 序号 | 集成模块 | 集成方案 | Demo应用 | 状态 | 说明 |
|-----|---------|---------|---------|------|------|
| 0 | db-pwd-demo-init | - | db-pwd-demo-init | ✅ 已完成 | Demo数据库初始化工具（所有Demo必须先运行）|
| 1 | db-pwd-core | 方案5: StandalonePasswordDecryptor | db-pwd-simple-jdbc-demo | ✅ 已完成 | 纯JDBC应用，演示独立工具类使用 |
| 2 | db-pwd-druid | 方案1: Druid PasswordCallback | db-pwd-druid-demo | ✅ 已完成 | Druid连接池演示PasswordCallback集成 |
| 3 | db-pwd-spring-boot-starter | 方案2: Spring Boot Starter | db-pwd-spring-boot-demo | ✅ 已完成 | Spring Boot Web应用演示自动配置 |
| 4 | db-pwd-spring | 方案3: Spring BeanPostProcessor | db-pwd-spring-demo | ✅ 已完成 | 传统Spring应用演示BeanPostProcessor |
| 5 | db-pwd-mybatis | 方案4: MyBatis DataSourceFactory | db-pwd-mybatis-demo | ✅ 已完成 | 纯MyBatis应用演示工厂模式 |
| 6 | db-pwd-web | 方案6: Servlet InitListener | db-pwd-servlet-demo | ✅ 已完成 | 纯Servlet应用演示监听器 |

---

## Demo应用详细设计

### Demo 1: db-pwd-simple-jdbc-demo (已有)

**集成方案**: 方案5 - StandalonePasswordDecryptor

**目标**: 演示使用StandalonePasswordDecryptor工具类的纯JDBC应用

**技术栈**:
- JDK 8
- JDBC
- H2内存数据库（避免依赖外部MySQL）

**功能**:
- 使用`StandalonePasswordDecryptor.getConnection()`获取连接
- 使用`StandalonePasswordDecryptor.DbUtil`简化操作
- 演示基本的CRUD操作

**关键代码**:
```java
// 方式1: 直接获取连接
Connection conn = StandalonePasswordDecryptor.getConnection("demo_db");

// 方式2: 使用DbUtil简化操作
DbUtil.query("demo_db", "SELECT * FROM users", rs -> { ... });

// 方式3: 获取解密密码
String password = StandalonePasswordDecryptor.decrypt("demo_db");
```

**占位符**: `simple_jdbc_db`

---

### Demo 2: db-pwd-druid-demo (新建)

**集成方案**: 方案1 - Druid DruidPasswordCallback

**目标**: 演示Druid连接池的PasswordCallback集成方式

**技术栈**:
- JDK 8
- JDBC
- Druid 1.2.x
- H2内存数据库

**功能**:
- 使用`DruidPasswordCallback.createDataSource()`创建数据源
- 配置密码占位符而非明文密码
- 演示Druid连接池的使用
- 展示密码回调解密过程

**关键代码**:
```java
// 创建Druid数据源，使用密码占位符
DruidDataSource dataSource = DruidPasswordCallback.createDataSource(
    "druid_demo_db",  // 密码占位符
    new DatabaseConfig("jdbc:h2:mem:demo", "sa", "", "org.h2.Driver"),
    "~/.dbpwd/db.pwd",
    "~/.dbpwd/sm4.key"
);

// 使用数据源获取连接
try (Connection conn = dataSource.getConnection()) {
    // 执行数据库操作
}
```

**占位符**: `druid_demo_db`

**配置方式**: Java配置（无Spring依赖）

---

### Demo 3: db-pwd-spring-boot-demo (已有)

**集成方案**: 方案2 - Spring Boot Starter

**目标**: 演示Spring Boot Auto Configuration的自动装配

**技术栈**:
- JDK 8
- Spring Boot 2.7.x
- H2内存数据库
- Maven

**功能**:
- 通过`application.yml`配置占位符
- Spring Boot自动装配`PasswordService`
- 自动装饰DataSource
- 提供REST API进行CRUD操作

**application.yml配置**:
```yaml
dbpwd:
  enabled: true
  auto-decorate: true
  password-file: ~/.dbpwd/db.pwd
  key-file: ~/.dbpwd/sm4.key

spring:
  datasource:
    url: jdbc:h2:mem:demo
    username: sa
    password: spring_boot_demo_db  # 占位符
```

**占位符**: `spring_boot_demo_db`

---

### Demo 4: db-pwd-spring-demo (新建)

**集成方案**: 方案3 - Spring BeanPostProcessor

**目标**: 演示传统Spring应用（无Boot）的BeanPostProcessor集成

**技术栈**:
- JDK 8
- Spring Framework 5.3.x
- H2内存数据库
- Maven

**功能**:
- Java配置方式（非Spring Boot）
- `DataSourcePasswordProcessor`拦截DataSource Bean
- 自动解密密码
- 演示JdbcTemplate使用

**Java配置**:
```java
@Configuration
public class SpringAppConfig {

    @Bean
    public DataSourcePasswordProcessor dataSourcePasswordProcessor() {
        return new DataSourcePasswordProcessor("~/.dbpwd/db.pwd", "~/.dbpwd/sm4.key");
    }

    @Bean
    public DataSource dataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:h2:mem:demo");
        ds.setUsername("sa");
        ds.setPassword("spring_demo_db");  // 占位符
        return ds;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
```

**占位符**: `spring_demo_db`

**运行方式**: Java应用或WAR部署

---

### Demo 5: db-pwd-mybatis-demo (新建)

**集成方案**: 方案4 - MyBatis DataSourceFactory

**目标**: 演示纯MyBatis应用（无Spring）的DataSourceFactory集成

**技术栈**:
- JDK 8
- MyBatis 3.5.x
- H2内存数据库
- Maven

**功能**:
- 使用`DbPwdDataSourceFactory`（继承UnpooledDataSourceFactory）
- mybatis-config.xml配置
- 纯MyBatis操作（无Spring依赖）
- 演示Mapper接口使用

mybatis-config.xml:
```xml
<configuration>
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="com.dbpwd.mybatis.factory.DbPwdDataSourceFactory">
                <property name="driver" value="org.h2.Driver"/>
                <property name="url" value="jdbc:h2:mem:demo"/>
                <property name="username" value="sa"/>
                <property name="password" value="mybatis_demo_db"/>

                <!-- 密钥文件配置 -->
                <property name="passwordFile" value="~/.dbpwd/db.pwd"/>
                <property name="keyFile" value="~/.dbpwd/sm4.key"/>
            </dataSource>
        </environment>
    </environments>

    <mappers>
        <mapper resource="mapper/UserMapper.xml"/>
    </mappers>
</configuration>
```

**占位符**: `mybatis_demo_db`

**运行方式**: Java应用或WAR部署

---

### Demo 6: db-pwd-servlet-demo (新建)

**集成方案**: 方案6 - Struts/Servlet InitListener

**目标**: 演示纯Servlet应用的ServletContextListener集成

**技术栈**:
- JDK 8
- Servlet 3.1+
- H2内存数据库
- Maven

**功能**:
- `DatabasePasswordInitListener`初始化密码服务
- 将密码服务存入ServletContext
- Servlet中通过`WebPasswordUtil`获取连接
- 提供简单的REST接口

web.xml配置:
```xml
<context-param>
    <param-name>dbpwd.passwordFile</param-name>
    <param-value>~/.dbpwd/db.pwd</param-value>
</context-param>
<context-param>
    <param-name>dbpwd.keyFile</param-name>
    <param-value>~/.dbpwd/sm4.key</param-value>
</context-param>
<context-param>
    <param-name>dbpwd.preloadAll</param-name>
    <param-value>true</param-value>
</context-param>

<listener>
    <listener-class>com.dbpwd.web.listener.DatabasePasswordInitListener</listener-class>
</listener>
```

**Servlet使用示例**:
```java
@WebServlet("/api/users")
public class UserServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {

        ServletContext servletContext = getServletContext();

        // 获取数据库连接
        Connection conn = WebPasswordUtil.getConnection(servletContext, "servlet_demo_db");

        // 执行查询...
    }
}
```

**占位符**: `servlet_demo_db`

**运行方式**: WAR包部署到Tomcat/Jetty

---

## 统一数据库设计

### H2数据库模式选择

**重要决策**: 所有Demo使用 **H2 FILE 数据库模式**，提供真正的密码验证。

**H2数据库模式对比**:

| 模式 | URL 格式 | 密码验证 | 适用场景 |
|------|----------|---------|---------|
| **内存模式** | `jdbc:h2:mem:dbname;DB_CLOSE_DELAY=-1` | ❌ 不验证 | 快速测试，开发 |
| **文件模式** | `jdbc:h2:file:path/dbname;MODE=MySQL` | ✅ 验证密码 | 真实环境测试，持久化 |

**选择 FILE 模式的原因**:
1. 真正验证密码解密的有效性
2. 模拟生产环境数据库连接
3. 数据持久化，方便调试和验证
4. 密码绑定机制确保安全性

### Demo运行前准备

**所有Demo运行前必须执行以下步骤**:

```bash
# Step 0: 生成 db.pwd 和 sm4.key（如果尚未生成）
mvn exec:java -pl db-pwd-core \
  -Dexec.mainClass="com.dbpwd.cli.DBPasswordCLI" \
  --demo --generate-key

# Step 1: 运行demo-init初始化H2 FILE数据库
mvn exec:java -pl db-pwd-demo-init \
  -Dexec.mainClass="com.dbpwd.init.DatabaseInitializer"

# Step 2: 运行具体Demo程序
# 例如：
mvn exec:java -pl db-pwd-simple-jdbc-demo \
  -Dexec.mainClass="com.dbpwd.demo.SimpleJdbcDemo"
```

### H2数据库密码配置

**H2密码方案说明**:
- H2数据库设置密码（两层保护）
- db.pwd中存储的是连接H2数据库的密码（SM4加密）
- 更接近真实生产场景

**H2内存数据库密码特点**:
- 虽然是内存数据库，但仍需连接密码
- JDBC连接时需要提供用户名和密码
- 密码通过dbpwd管理，实现安全解密

### 数据库表结构

所有Demo共享相同的H2内存数据库结构：

```sql
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE posts (
    id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT,
    title VARCHAR(200),
    content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

## 统一db.pwd配置示例

### H2数据库密码

所有Demo使用相同的H2数据库密码（便于统一管理）：

| 配置项 | 值 |
|-------|-----|
| H2用户名 | `sa` |
| H2密码（明文） | `dbpwd_h2_demo_password_2024` |

**注意**: H2密码需要在命令行工具中通过encrypt加密后存储到db.pwd中。

### db.pwd完整配置示例

```bash
# 使用命令行工具加密6个Demo的H2数据库密码
java -jar db-pwd-cli.jar encrypt \
  --id simple_jdbc_db \
  --password dbpwd_h2_demo_password_2024 \
  --url jdbc:h2:mem:dbpwd_demo;DB_CLOSE_DELAY=-1 \
  --username sa \
  --driver org.h2.Driver

# 对其他5个Demo重复执行相同操作
java -jar db-pwd-cli.jar encrypt \
  --id druid_demo_db \
  --password dbpwd_h2_demo_password_2024 \
  --url jdbc:h2:mem:druid_demo;DB_CLOSE_DELAY=-1 \
  --username sa \
  --driver org.h2.Driver

java -jar db-pwd-cli.jar encrypt \
  --id spring_boot_db \
  --password dbpwd_h2_demo_password_2024 \
  --url jdbc:h2:mem:spring_boot_demo;DB_CLOSE_DELAY=-1 \
  --username sa \
  --driver org.h2.Driver

java -jar db-pwd-cli.jar encrypt \
  --id spring_framework_db \
  --password dbpwd_h2_demo_password_2024 \
  --url jdbc:h2:mem:spring_framework_demo;DB_CLOSE_DELAY=-1 \
  --username sa \
  --driver org.h2.Driver

java -jar db-pwd-cli.jar encrypt \
  --id mybatis_demo_db \
  --password dbpwd_h2_demo_password_2024 \
  --url jdbc:h2:mem:mybatis_demo;DB_CLOSE_DELAY=-1 \
  --username sa \
  --driver org.h2.Driver

java -jar db-pwd-cli.jar encrypt \
  --id servlet_demo_db \
  --password dbpwd_h2_demo_password_2024 \
  --url jdbc:h2:mem:servlet_demo;DB_CLOSE_DELAY=-1 \
  --username sa \
  --driver org.h2.Driver
```

### 生成的db.pwd配置文件

```json
{
  "version": "1.0",
  "databases": [
    {
      "id": "simple_jdbc_db",
      "name": "Simple JDBC Demo Database",
      "type": "h2",
      "driverClassName": "org.h2.Driver",
      "jdbcUrl": "jdbc:h2:mem:dbpwd_demo;DB_CLOSE_DELAY=-1",
      "hostname": "mem",
      "port": 0,
      "databaseName": "dbpwd_demo",
      "url": "jdbc:h2:mem:dbpwd_demo;DB_CLOSE_DELAY=-1",
      "username": "sa",
      "password": "ENC(SM4:密文)",
      "encrypted": true,
      "createdAt": "2026-02-11T00:00:00Z",
      "updatedAt": "2026-02-11T00:00:00Z"
    },
    {
      "id": "druid_demo_db",
      "name": "Druid Demo Database",
      "type": "h2",
      "driverClassName": "org.h2.Driver",
      "jdbcUrl": "jdbc:h2:mem:druid_demo;DB_CLOSE_DELAY=-1",
      "hostname": "mem",
      "port": 0,
      "databaseName": "druid_demo",
      "url": "jdbc:h2:mem:druid_demo;DB_CLOSE_DELAY=-1",
      "username": "sa",
      "password": "ENC(SM4:密文)",
      "encrypted": true,
      "createdAt": "2026-02-11T00:00:00Z",
      "updatedAt": "2026-02-11T00:00:00Z"
    },
    {
      "id": "spring_boot_db",
      "name": "Spring Boot Demo Database",
      "type": "h2",
      "driverClassName": "org.h2.Driver",
      "jdbcUrl": "jdbc:h2:mem:spring_boot_demo;DB_CLOSE_DELAY=-1",
      "hostname": "mem",
      "port": 0,
      "databaseName": "spring_boot_demo",
      "url": "jdbc:h2:mem:spring_boot_demo;DB_CLOSE_DELAY=-1",
      "username": "sa",
      "password": "ENC(SM4:密文)",
      "encrypted": true,
      "createdAt": "2026-02-11T00:00:00Z",
      "updatedAt": "2026-02-11T00:00:00Z"
    },
    {
      "id": "spring_framework_db",
      "name": "Spring Framework Demo Database",
      "type": "h2",
      "driverClassName": "org.h2.Driver",
      "jdbcUrl": "jdbc:h2:mem:spring_framework_demo;DB_CLOSE_DELAY=-1",
      "hostname": "mem",
      "port": 0,
      "databaseName": "spring_framework_demo",
      "url": "jdbc:h2:mem:spring_framework_demo;DB_CLOSE_DELAY=-1",
      "username": "sa",
      "password": "ENC(SM4:密文)",
      "encrypted": true,
      "createdAt": "2026-02-11T00:00:00Z",
      "updatedAt": "2026-02-11T00:00:00Z"
    },
    {
      "id": "mybatis_demo_db",
      "name": "MyBatis Demo Database",
      "type": "h2",
      "driverClassName": "org.h2.Driver",
      "jdbcUrl": "jdbc:h2:mem:mybatis_demo;DB_CLOSE_DELAY=-1",
      "hostname": "mem",
      "port": 0,
      "databaseName": "mybatis_demo",
      "url": "jdbc:h2:mem:mybatis_demo;DB_CLOSE_DELAY=-1",
      "username": "sa",
      "password": "ENC(SM4:密文)",
      "encrypted": true,
      "createdAt": "2026-02-11T00:00:00Z",
      "updatedAt": "2026-02-11T00:00:00Z"
    },
    {
      "id": "servlet_demo_db",
      "name": "Servlet Demo Database",
      "type": "h2",
      "driverClassName": "org.h2.Driver",
      "jdbcUrl": "jdbc:h2:mem:servlet_demo;DB_CLOSE_DELAY=-1",
      "hostname": "mem",
      "port": 0,
      "databaseName": "servlet_demo",
      "url": "jdbc:h2:mem:servlet_demo;DB_CLOSE_DELAY=-1",
      "username": "sa",
      "password": "ENC(SM4:密文)",
      "encrypted": true,
      "createdAt": "2026-02-11T00:00:00Z",
      "updatedAt": "2026-02-11T00:00:00Z"
    }
  ],
  "updatedAt": "2026-02-11T00:00:00Z"
}
```

### 密码保护机制

**两层保护设计**：
1. **第一层**: H2数据库密码本身（dbpwd_h2_demo_password_2024）
2. **第二层**: SM4加密存储在db.pwd中

**解密流程**:
```
应用启动
  → 读取db.pwd
  → 使用sm4.key解密密码
  → 获取明文密码 dbpwd_h2_demo_password_2024
  → 连接H2数据库
  → 验证用户名sa和密码
  → 连接成功
```

### H2内存数据库参数说明

| 参数 | 值 | 说明 |
|------|---|------|
| `DB_CLOSE_DELAY=-1` | 保持数据库直到JVM关闭 | 避免内存数据库自动关闭 |
| `;` | 分隔URL参数 | H2使用分号分隔多个参数 |
| `sa` | H2默认管理员用户 | H2默认用户名 |

### 密码一致性策略

**方案**: 所有Demo使用相同的H2密码

| 说明 | 理由 |
|-----|------|
| 相同密码 | 简化配置，便于管理 |
| 统一命名 | `dbpwd_h2_demo_password_2024` |
| 可选配置 | 生产环境可为每个Demo设置不同密码 |

如需为不同Demo设置不同密码，修改encrypt命令中的--password参数即可。

```json
{
  "version": "1.0",
  "databases": [
    {
      "id": "simple_jdbc_db",
      "name": "Simple JDBC Demo Database",
      "type": "h2",
      "driverClassName": "org.h2.Driver",
      "jdbcUrl": "jdbc:h2:mem:dbpwd_demo;DB_CLOSE_DELAY=-1",
      "hostname": "mem",
      "port": 0,
      "databaseName": "dbpwd_demo",
      "url": "jdbc:h2:mem:dbpwd_demo;DB_CLOSE_DELAY=-1",
      "username": "sa",
      "password": "ENC(SM4:密文)",
      "encrypted": true,
      "createdAt": "2026-02-11T00:00:00Z",
      "updatedAt": "2026-02-11T00:00:00Z"
    },
    {
      "id": "druid_demo_db",
      "name": "Druid Demo Database",
      "type": "h2",
      "driverClassName": "org.h2.Driver",
      "jdbcUrl": "jdbc:h2:mem:druid_demo;DB_CLOSE_DELAY=-1",
      "hostname": "mem",
      "port": 0,
      "databaseName": "druid_demo",
      "url": "jdbc:h2:mem:druid_demo;DB_CLOSE_DELAY=-1",
      "username": "sa",
      "password": "ENC(SM4:密文)",
      "encrypted": true,
      "createdAt": "2026-02-11T00:00:00Z",
      "updatedAt": "2026-02-11T00:00:00Z"
    },
    {
      "id": "spring_boot_demo_db",
      "name": "Spring Boot Demo Database",
      "type": "h2",
      "driverClassName": "org.h2.Driver",
      "jdbcUrl": "jdbc:h2:mem:spring_demo;DB_CLOSE_DELAY=-1",
      "hostname": "mem",
      "port": 0,
      "databaseName": "spring_demo",
      "url": "jdbc:h2:mem:spring_demo;DB_CLOSE_DELAY=-1",
      "username": "sa",
      "password": "ENC(SM4:密文)",
      "encrypted": true,
      "createdAt": "2026-02-11T00:00:00Z",
      "updatedAt": "2026-02-11T00:00:00Z"
    },
    {
      "id": "spring_demo_db",
      "name": "Spring Demo Database",
      "type": "h2",
      "driverClassName": "org.h2.Driver",
      "jdbcUrl": "jdbc:h2:mem:spring_framework_demo;DB_CLOSE_DELAY=-1",
      "hostname": "mem",
      "port": 0,
      "databaseName": "spring_framework_demo",
      "url": "jdbc:h2:mem:spring_framework_demo;DB_CLOSE_DELAY=-1",
      "username": "sa",
      "password": "ENC(SM4:密文)",
      "encrypted": true,
      "createdAt": "2026-02-11T00:00:00Z",
      "updatedAt": "2026-02-11T00:00:00Z"
    },
    {
      "id": "mybatis_demo_db",
      "name": "MyBatis Demo Database",
      "type": "h2",
      "driverClassName": "org.h2.Driver",
      "jdbcUrl": "jdbc:h2:mem:mybatis_demo;DB_CLOSE_DELAY=-1",
      "hostname": "mem",
      "port": 0,
      "databaseName": "mybatis_demo",
      "url": "jdbc:h2:mem:mybatis_demo;DB_CLOSE_DELAY=-1",
      "username": "sa",
      "password": "ENC(SM4:密文)",
      "encrypted": true,
      "createdAt": "2026-02-11T00:00:00Z",
      "updatedAt": "2026-02-11T00:00:00Z"
    },
    {
      "id": "servlet_demo_db",
      "name": "Servlet Demo Database",
      "type": "h2",
      "driverClassName": "org.h2.Driver",
      "jdbcUrl": "jdbc:h2:mem:servlet_demo;DB_CLOSE_DELAY=-1",
      "hostname": "mem",
      "port": 0,
      "databaseName": "servlet_demo",
      "url": "jdbc:h2:mem:servlet_demo;DB_CLOSE_DELAY=-1",
      "username": "sa",
      "password": "ENC(SM4:密文)",
      "encrypted": true,
      "createdAt": "2026-02-11T00:00:00Z",
      "updatedAt": "2026-02-11T00:00:00Z"
    }
  ],
  "updatedAt": "2026-02-11T00:00:00Z"
}
```

---

## Demo运行验证

每个Demo需要验证以下功能：

### 通用验证点
- ✅ 启动成功，无异常
- ✅ 密码自动解密正常
- ✅ 数据库连接成功
- ✅ 基本CRUD操作正常
- ✅ 密码占位符正确识别

### 特定验证点
- **simple-jdbc-demo**: StandalonePasswordDecryptor API调用
- **druid-demo**: DruidPasswordCallback触发
- **spring-boot-demo**: Spring Boot自动配置
- **spring-demo**: BeanPostProcessor拦截DataSource
- **mybatis-demo**: MyBatis Factory模式
- **servlet-demo**: ServletContextListener初始化

---

## Demo开发优先级

| 优先级 | Demo应用 | 状态 | 说明 |
|-------|---------|------|------|
| P0 | db-pwd-simple-jdbc-demo | ✅ 已有 | 需更新为使用StandalonePasswordDecryptor |
| P1 | db-pwd-druid-demo | ⚪ 待开发 | Druid集成，相对独立 |
| P1 | db-pwd-spring-demo | ⚪ 待开发 | 传统Spring，与Boot Demo代码差异大 |
| P2 | db-pwd-mybatis-demo | ⚪ 待开发 | 纯MyBatis，无Spring依赖 |
| P2 | db-pwd-servlet-demo | ⚪ 待开发 | 纯Servlet，需要WAR部署测试 |
| P3 | db-pwd-spring-boot-demo | ✅ 已有 | 已有Demo，需确认使用Starter方式 |

---

## 项目结构规划

```
db-pwd/
├── pom.xml (parent)
├── db-pwd-core/
├── db-pwd-druid/
├── db-pwd-spring/
├── db-pwd-spring-boot-starter/
├── db-pwd-mybatis/
├── db-pwd-web/
├── db-pwd-simple-jdbc-demo/      (Demo 1 - 已有，需更新)
├── db-pwd-druid-demo/              (Demo 2 - 新建)
├── db-pwd-spring-boot-demo/        (Demo 3 - 已有)
├── db-pwd-spring-demo/             (Demo 4 - 新建)
├── db-pwd-mybatis-demo/            (Demo 5 - 新建)
└── db-pwd-servlet-demo/            (Demo 6 - 新建)
```

---

## 相关文档

- [集成场景和方案](integration-scenarios.md) - 6种集成场景详解
- [Demo应用验证](../demo-README.md) - Demo使用指南
