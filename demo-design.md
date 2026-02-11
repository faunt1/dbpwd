# Database Password Manager - Demo应用设计文档

> 文档版本: 2.0
> 最后更新: 2026-02-11

## 概述

本文档详细描述了Database Password Manager 6个Demo应用的设计架构、工作机制和适用场景。每个Demo对应一种集成方案，展示不同场景下的密码管理集成方式。

---

## Demo应用概览

| Demo应用 | 集成方案 | 集成模块 | 占位符 | 状态 |
|---------|---------|---------|--------|------|
| db-pwd-simple-jdbc-demo | 方案5: StandalonePasswordDecryptor | db-pwd-core | `simple_jdbc_db` | ✅ 已更新 |
| db-pwd-druid-demo | 方案1: Druid PasswordCallback | db-pwd-druid | `druid_demo_db` | ⚪ 待开发 |
| db-pwd-spring-boot-demo | 方案2: Spring Boot Starter | db-pwd-spring-boot-starter | `spring_boot_db` | ✅ 已有 |
| db-pwd-spring-demo | 方案3: Spring BeanPostProcessor | db-pwd-spring | `spring_demo_db` | ⚪ 待开发 |
| db-pwd-mybatis-demo | 方案4: MyBatis DataSourceFactory | db-pwd-mybatis | `mybatis_demo_db` | ⚪ 待开发 |
| db-pwd-servlet-demo | 方案6: Servlet InitListener | db-pwd-web | `servlet_demo_db` | ⚪ 待开发 |

---

## 统一技术栈

所有Demo应用使用统一的技术栈：

| 组件 | 版本 |
|-----|------|
| JDK | 8+ |
| Maven | 3.6+ |
| 数据库 | H2 内存数据库 |
| 加密算法 | SM4-GCM (国密认证加密) |

---

## 统一数据库设计

### H2数据库密码配置

| 配置项 | 值 |
|-------|-----|
| H2用户名 | `sa` |
| H2密码（明文） | `dbpwd_h2_demo_password_2024` |

**注意**: 所有Demo在 db.pwd 中存储的是连接H2数据库的密码（SM4加密）。

### 数据库表结构

所有Demo共享相同的H2数据库结构：

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

## 1. Simple JDBC Demo（方案5）

**位置**: `db-pwd-simple-jdbc-demo/`

### 1.1 设计概述

Simple JDBC Demo 采用**方案5 - StandalonePasswordDecryptor**，展示纯JDBC应用中如何使用独立的密码管理工具类。

### 1.2 集成模式：程序直接操作模式

**核心特点**:
- 使用 `StandalonePasswordDecryptor` 工具类获取解密后的密码
- 程序自己调用密码管理API
- 无框架依赖，适合简单应用

### 1.3 设计架构

```
┌─────────────────────────────────────────────────────────────┐
│                  Simple JDBC Demo                         │
│                  (主类: SimpleJdbcDemo)                     │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────┐   ┌─────────────┐   ┌─────────────┐      │
│  │ 密码工具类  │   │  数据库操作  │   │  H2数据库   │      │
│  │ Standalone  │   │   JDBC API   │   │  内存存储   │      │
│  │ Password   │   │             │   │             │      │
│  │ Decryptor   │   │             │   │ - users表  │      │
│  │ ├─────────  │   │ - insert    │   │ - posts表  │      │
│  │ │getConn()   │   │ - select    │   │             │      │
│  │ │decryptPwd()│   │ - update    │   │             │      │
│  │ └─────────  │   │             │   │             │      │
│  └─────────────┘   └─────────────┘   └─────────────┘      │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 1.4 关键代码

```java
// 方式1: 直接获取连接
Connection conn = StandalonePasswordDecryptor.getConnection("simple_jdbc_db");

// 方式2: 获取明文密码后构建连接
String plainPassword = StandalonePasswordDecryptordecrypt("simple_jdbc_db");
String url = "jdbc:h2:mem:dbpwd_demo;DB_CLOSE_DELAY=-1";
Connection conn = DriverManager.getConnection(url, "sa", plainPassword);
```

### 1.5 占位符设计

| 配置项 | 值 |
|--------|-----|
| 占位符 | `simple_jdbc_db` |
| H2数据库 | `dbpwd_demo` |
| JDBC URL | `jdbc:h2:mem:dbpwd_demo;DB_CLOSE_DELAY=-1` |

### 1.6 适用场景

- 简单的JDBC命令行工具
- 无框架依赖的独立应用
- 学习密码管理基础用法
- 完全控制加密解密流程

---

## 2. Druid Demo（方案1）

**位置**: `db-pwd-druid-demo/`

### 2.1 设计概述

Druid Demo 采用**方案1 - Druid PasswordCallback**，展示如何在Druid连接池中使用原生PasswordCallback机制实现密码自动解密。

### 2.2 集成模式：框架回调模式

**核心特点**:
- 利用Druid原生PasswordCallback机制
- 零侵入，符合Druid设计模式
- 在连接池创建时自动解密密码

### 2.3 设计架构

```
┌─────────────────────────────────────────────────────────────┐
│                      Druid Demo                            │
│                  (主类: DruidDemo)                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │           Druid连接池 (DruidDataSource)             │   │
│  ├─────────────────────────────────────────────────────┤   │
│  │                                                      │   │
│  │  ┌─────────────────────────────────────────────┐   │   │
│  │  │  DruidPasswordCallback (db-pwd-druid)       │   │   │
│  │  ├─────────────────────────────────────────────┤   │   │
│  │  │  - createDataSource() 工厂方法              │   │   │
│  │  │  - 解密密码                                  │   │   │
│  │  │  - 设置到DataSource                          │   │   │
│  │  └─────────────────────────────────────────────┘   │   │
│  │                      ↓                              │   │
│  │  ┌─────────────────────────────────────────────┐   │   │
│  │  │       自动解密流程                            │   │   │
│  │  │  1. 读取db.pwd                               │   │   │
│  │  │  2. 使用sm4.key解密                          │   │   │
│  │  │  3. 注入到DataSource                        │   │   │
│  │  └─────────────────────────────────────────────┘   │   │
│  │                                                      │   │
│  └─────────────────────────────────────────────────────┘   │
│                              ↓                              │
│         ┌────────────────────────────────────────┐     │
│         │           JDBC连接池                         │     │
│         │         数据库操作层                       │     │
│         └────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────────────┘
```

### 2.4 关键代码

```java
// 创建Druid数据源，使用密码占位符自动解密
DruidDataSource dataSource = DruidPasswordCallback.createDataSource(
    "druid_demo_db",  // 占位符，自动从db.pwd查找并解密
    true  // autoDecrypt=true
);

// 使用数据源
try (Connection conn = dataSource.getConnection()) {
    // 执行数据库操作
}
```

### 2.5 占位符设计

| 配置项 | 值 |
|--------|-----|
| 占位符 | `druid_demo_db` |
| H2数据库 | `druid_demo` |
| JDBC URL | `jdbc:h2:mem:druid_demo;DB_CLOSE_DELAY=-1` |

### 2.6 适用场景

- 使用Druid连接池的应用
- 需要零侵入集成密码管理
- 遵循Druid最佳实践

---

## 3. Spring Boot Demo（方案2）

**位置**: `db-pwd-spring-boot-demo/`

### 3.1 设计概述

Spring Boot Demo 采用**方案2 - Spring Boot Starter**，展示Spring Boot自动配置的密码管理集成。

### 3.2 集成模式：Spring Boot自动配置模式

**核心特点**:
- Spring Boot Auto Configuration
- 完全自动装配，最小配置
- 占位符格式 `@db_id@`

### 3.3 设计架构

```
┌─────────────────────────────────────────────────────────────┐
│                   Spring Boot Demo                           │
│                (主类: SpringBootApplication)                 │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │         Spring Boot 框架层                          │   │
│  ├─────────────────────────────────────────────────────┤   │
│  │  application.yml:                                    │   │
│  │  spring:                                            │   │
│  │    datasource:                                      │   │
│  │      password: @spring_boot_db@  (占位符)        │   │
│  │  dbpwd:                                             │   │
│  │    enabled: true                                    │   │
│  │                                                      │   │
│  │  ┌─────────────────────────────────────────┐        │   │
│  │  │  DbPwdAutoConfiguration           │        │   │
│  │  ├─────────────────────────────────────────┤        │   │
│  │  │ - DbPwdProperties                    │        │   │
│  │  │ - DbPwdPasswordService              │        │   │
│  │  │ - DbPwdPlaceholderResolver           │        │   │
│  │  └─────────────────────────────────────────┘        │   │
│  │                      ↓                              │   │
│  │  ┌─────────────────────────────────────────┐        │   │
│  │  │  自动解密流程                            │        │   │
│  │  │  1. 拦截@db_id@占位符                  │        │   │
│  │  │  2. 从db.pwd查找配置                 │        │   │
│  │  │  3. 使用sm4.key解密                    │        │   │
│  │  │  4. 注入明文密码到DataSource          │        │   │
│  │  └─────────────────────────────────────────┘        │   │
│  │                                                      │   │
│  └─────────────────────────────────────────────────────┘   │
│                              ↓                              │
│  ┌─────────────────────────────────────────────────────┐ │
│  │         REST API 层 (@RestController)                   │ │
│  │  ├─ GET  /api/users  (用户列表)                      │ │
│  │  ├─ POST /api/users (创建用户)                      │ │
│  │  └─ GET  /api/health (健康检查)                     │ │
│  └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 3.4 application.yml配置

```yaml
dbpwd:
  enabled: true
  placeholder-prefix: "@"
  placeholder-suffix: "@"
  password-file: ~/.dbpwd/db.pwd
  key-file: ~/.dbpwd/sm4.key

spring:
  datasource:
    url: jdbc:h2:mem:spring_boot_demo;DB_CLOSE_DELAY=-1
    username: sa
    password: "@spring_boot_db@"  # 占位符
```

### 3.5 占位符设计

| 配置项 | 值 |
|--------|-----|
| 占位符 | `@spring_boot_db@` |
| 占位符格式 | `@` + db_id + `@` |
| H2数据库 | `spring_boot_demo` |

### 3.6 适用场景

- Spring Boot应用
- 最小化配置
- 自动化集成

---

## 4. Spring Demo（方案3）

**位置**: `db-pwd-spring-demo/`

### 4.1 设计概述

Spring Demo 采用**方案3 - Spring BeanPostProcessor**，展示传统Spring应用（非Boot）中如何使用BeanPostProcessor实现密码管理。

### 4.2 集成模式：Spring后处理器模式

**核心特点**:
- 利用Spring BeanPostProcessor机制
- 拦截所有DataSource Bean创建
- 自动解密密码注入

### 4.3 设计架构

```
┌─────────────────────────────────────────────────────────────┐
│                   Spring Framework Demo                      │
│                  (主类: SpringDemo)                           │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │         Spring 框架层（传统Spring）                       │   │
│  ├─────────────────────────────────────────────────────┤   │
│  │  JavaConfig (@Configuration):                           │   │
│  │  ├─ DataSourcePasswordProcessor Bean             │   │
│  │  ├─ DataSource Bean                              │   │   │
│  │  └─ JdbcTemplate Bean                           │   │   │
│  │                                                      │   │
│  │  ┌─────────────────────────────────────────┐        │   │
│  │  │  DataSourcePasswordProcessor          │        │   │
│  │  ├─────────────────────────────────────────┤        │   │
│  │  │  - postProcessAfterInitialization()      │        │   │
│  │  │  - 拦截DataSource Bean                   │        │   │
│  │  │  - 检测ENC格式密码                        │        │   │
│  │  │  - 从db.pwd解密并注入                    │        │   │
│  │  └─────────────────────────────────────────┘        │   │
│  │                      ↓                              │   │
│  │  ┌─────────────────────────────────────────┐        │   │
│  │  │  自动解密流程                            │        │   │
│  │  │  1. Bean创建后拦截                       │        │   │
│  │  │  2. 检测密码字段                          │        │   │
│  │   │  3. 解密并设置                            │        │   │
│  │  └─────────────────────────────────────────┘        │   │
│  │                                                      │   │
│  └─────────────────────────────────────────────────────┘   │
│                              ↓                              │
│  ┌─────────────────────────────────────────────────────┐ │
│  │            数据访问层 (@Repository)                     │ │
│  │  └───────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 4.4 Java配置

```java
@Configuration
public class SpringAppConfig {

    @Bean
    public DataSourcePasswordProcessor dataSourcePasswordProcessor() {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();
        processor.setKeyFilePath("~/.dbpwd/sm4.key");
        processor.setPwdFilePath("~/.dbpwd/db.pwd");
        return processor;
    }

    @Bean
    public DataSource dataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:h2:mem:spring_demo;DB_CLOSE_DELAY=-1");
        ds.setUsername("sa");
        ds.setPassword("spring_demo_db");  // 占位符，会被自动解密
        return ds;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
```

### 4.5 占位符设计

| 配置项 | 值 |
|--------|-----|
| 占位符 | `spring_demo_db` |
| H2数据库 | `spring_framework_demo` |
| JDBC URL | `jdbc:h2:mem:spring_framework_demo;DB_CLOSE_DELAY=-1` |

### 4.6 适用场景

- 传统Spring应用（非Boot）
- XML配置或Java配置
- 需要拦截所有DataSource

---

## 5. MyBatis Demo（方案4）

**位置**: `db-pwd-mybatis-demo/`

### 5.1 设计概述

MyBatis Demo 采用**方案4 - MyBatis DataSourceFactory**，展示纯MyBatis应用（无Spring）中如何使用自定义DataSourceFactory。

### 5.2 集成模式：MyBatis工厂模式

**核心特点**:
- 继承UnpooledDataSourceFactory
- 支持MyBatis xml配置
- 纯MyBatis应用，无Spring依赖

### 5.3 设计架构

```
┌─────────────────────────────────────────────────────────────┐
│                     MyBatis Demo                             │
│                  (主类: MybatisDemo)                          │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │          MyBatis 框架层 (xml/sql)                    │   │
│  ├─────────────────────────────────────────────────────┤   │
│  │  mybatis-config.xml:                                  │   │
│  │  <dataSource type="com.dbpwd.mybatis...">        │   │
│  │    <property name="dbId" value="mybatis_demo_db"/>   │   │
│  │  </dataSource>                                       │   │
│  │                                                      │   │
│  │  ┌─────────────────────────────────────────┐        │   │
│  │  │  DbPwdDataSourceFactory               │        │   │
│  │  ├─────────────────────────────────────────┤        │   │
│  │  │  - setProperties()                     │        │   │
│  │  │  - getDataSource()                      │        │   │
│  │  │  - 读取db.pwd                          │        │   │
│  │  │  - 解密password并设置                    │        │   │
│  │  └─────────────────────────────────────────┘        │   │
│  │                      ↓                              │   │
│  │  ┌─────────────────────────────────────────┐        │   │
│  │  │  Mapper层 (XML + 接口)                   │        │   │
│  │  │  ├─ UserMapper.java                    │        │   │
│  │  │  └─ UserMapper.xml                     │        │   │
│  │  └─────────────────────────────────────────┘        │   │
│  └─────────────────────────────────────────────────────┘   │
│                              ↓                              │
│  ┌─────────────────────────────────────────────────────┐ │
│  │         应用层                                    │ │
│  │  └─ MyBatis操作                               │ │
│  └─────────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────────────┘
```

### 5.4 mybatis-config.xml配置

```xml
<configuration>
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="com.dbpwd.mybatis.DbPwdDataSourceFactory">
                <property name="dbId" value="mybatis_demo_db"/>
                <property name="keyFile" value="~/.dbpwd/sm4.key"/>
                <property name="pwdFile" value="~/.dbpwd/db.pwd"/>
                <property name="url" value="jdbc:h2:mem:mybatis_demo;DB_CLOSE_DELAY=-1"/>
                <property name="username" value="sa"/>
                <property name="driverClassName" value="org.h2.Driver"/>
            </dataSource>
        </environment>
    </environments>
    <mappers>
        <mapper resource="mapper/UserMapper.xml"/>
    </mappers>
</configuration>
```

### 5.5 占位符设计

| 配置项 | 值 |
|--------|-----|
| 占位符 | `mybatis_demo_db` |
| H2数据库 | `mybatis_demo` |
| JDBC URL | `jdbc:h2:mem:mybatis_demo;DB_CLOSE_DELAY=-1` |

### 5.6 适用场景

- 纯MyBatis应用
- 无Spring依赖
- xml配置方式

---

## 6. Servlet Demo（方案6）

**位置**: `db-pwd-servlet-demo/`

### 6.1 设计概述

Servlet Demo 采用**方案6 - Servlet InitListener**，展示纯Servlet应用中如何使用ServletContextListener初始化密码管理服务。

### 6.2 集成模式：Web监听器模式

**核心特点**:
- Application启动时初始化监听器
- ServletContext存储密码服务
- Servlet通过工具类访问

### 6.3 设计架构

```
┌─────────────────────────────────────────────────────────────┐
│                      Servlet Demo                             │
│                  (Web应用: WAR包)                              │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  web.xml 配置:                                         │   │
│  ├─────────────────────────────────────────────────────┤   │
│  │  <context-param>                                      │   │
│  │    dbpwd.passwordFile=~/.dbpwd/db.pwd          │   │
│  │    dbpwd.keyFile=~/.dbpwd/sm4.key              │   │
│  │  </context-param>                                      │   │
│  │                                                      │   │
│  │  <listener>                                            │   │
│  │    <listener-class>com.dbpwd.web.               │   │
│  │      DatabasePasswordInitListener                │   │
│  │    </listener>                                         │   │
│  │                                                      │   │
│  │  ┌─────────────────────────────────────────┐        │   │
│  │  │  DatabasePasswordInitListener        │        │   │
│  │  ├─────────────────────────────────────────┤        │   │
│  │  │  - contextInitialized()                │        │   │
│  │  │  - 初始化WebPasswordService            │        │   │
│  │  │  - 存入ServletContext               │        │   │
│  │  │  - 预加载密码（可选）                  │        │   │
│  │  └─────────────────────────────────────────┘        │   │
│  └─────────────────────────────────────────────────────┘   │
│                      ↓                              │
│  ┌─────────────────────────────────────────────┐   │
│  │  ServletContext (应用级存储)                    │   │
│  │  - WebPasswordService实例                       │   │
│  │  - 缓存的明文密码                               │   │
│  └─────────────────────────────────────────────┘   │
│                      ↓                              │
│  ┌─────────────────────────────────────────────┐   │
│  │  Servlet (@WebServlet):                            │   │
│  ├─────────────────────────────────────────────┤   │
│  │  @WebServlet("/api/users")                     │   │
│  │  protected void doGet(HttpServletRequest, HttpServletResponse)│   │
│  │     ServletContext context = getServletContext(); │   │
│  │     String password = WebPasswordUtil.           │   │
│  │       getPlainPassword(context, "servlet_demo_db");│   │
│  │                                                      │   │
│  │ - @WebServlet("/api/posts")                     │   │
│  │ - @WebServlet("/api/health")                    │   │
│  └─────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

### 6.4 web.xml配置

```xml
<context-param>
    <param-name>dbpwd.keyFile</param-name>
    <param-value>~/.dbpwd/sm4.key</param-value>
</context-param>
<context-param>
    <param-name>dbpwd.pwdFile</param-name>
    <param-value>~/.dbpwd/db.pwd</param-value>
</context-param>
<context-param>
    <param-name>dbpwd.preload</param-name>
    <param-value>true</param-value>
</context-param>

<listener>
    <listener-class>com.dbpwd.web.DatabasePasswordInitListener</listener-class>
</listener>
```

### 6.5 Servlet使用示例

```java
@WebServlet("/api/users")
public class UserServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        ServletContext context = getServletContext();

        // 获取明文密码
        String password = WebPasswordUtil.getPlainPassword(
            context, "servlet_demo_db"
        );

        // 构建连接
        String url = "jdbc:h2:mem:servlet_demo;DB_CLOSE_DELAY=-1";
        try (Connection conn = DriverManager.getConnection(url, "sa", password)) {
            // 执行查询...
        }
    }
}
```

### 6.6 占位符设计

| 配置项 | 值 |
|--------|-----|
| 占位符 | `servlet_demo_db` |
| H2数据库 | `servlet_demo` |
| JDBC URL | `jdbc:h2:mem:servlet_demo;DB_CLOSE_DELAY=-1` |

### 6.7 适用场景

- 纯Servlet应用
- 需要应用级密码缓存
- WAR部署到Tomcat/Jetty

---

## 统一db.pwd配置

所有Demo共享一个db.pwd配置文件：

| 数据库ID | Demo应用 | 说明 |
|---------|---------|------|
| `simple_jdbc_db` | Simple JDBC Demo | 纯JDBC工具 |
| `druid_demo_db` | Druid Demo | Druid连接池 |
| `spring_boot_db` | Spring Boot Demo | Spring Boot自动配置 |
| `spring_demo_db` | Spring Demo | Spring BeanPostProcessor |
| `mybatis_demo_db` | MyBatis Demo | MyBatis DataSourceFactory |
| `servlet_demo_db` | Servlet Demo | Servlet InitListener |

### 生成db.pwd命令示例

```bash
# 方式1: 使用CLI工具逐个生成
java -jar db-pwd-cli.jar encrypt \
  --id simple_jdbc_db \
  --password dbpwd_h2_demo_password_2024 \
  --url jdbc:h2:mem:dbpwd_demo;DB_CLOSE_DELAY=-1 \
  --username sa \
  --driver org.h2.Driver

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
  --id spring_demo_db \
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

---

## 相关文档

- [Demo集成方案规划](./technical/demo-integration-plan.md) - 6个Demo的详细规划
- [Demo应用使用指南](./demo-README.md) - Demo使用和运行指南
- [开发计划](./technical/development-plan.md) - Sprint 6.5 Demo开发进度
