# Database Password Manager - Demo应用设计文档

> 文档版本: v2.1
> 最后更新: 2026-02-15

## 概述

本文档描述 6 个 Demo 应用的设计、实现和验证点，每个 Demo 对应一种集成模式。

---

## 单元测试 vs Demo应用

| 场景 | 文件路径 | 配置文件位置 | 说明 |
|------|---------|------------|------|
| **单元测试** | 临时目录（@TempDir） | 测试代码中创建 | 自包含、自解释，不污染用户环境 |
| **Demo应用** | `~/.dbpwd/` | 用户主目录 | 模拟真实生产环境配置 |

### 单元测试设计原则

1. **自包含**: 所有测试数据在 `@TempDir` 临时目录中创建
2. **自解释**: 通过 API 参数传递路径，不依赖外部配置
3. **可隔离**: 测试之间互不影响，测试后自动清理

### Demo应用设计原则

1. **模拟生产**: 使用 `~/.dbpwd/` 作为配置目录
2. **无感知**: 应用只需使用占位符 `@db_name@`
3. **易迁移**: Demo可直接作为生产参考

---

## Demo应用总览

| Demo | 集成模式 | 占位符 | H2数据库 | 验证重点 |
|------|---------|--------|----------|----------|
| db-pwd-demo-init | - | - | 所有Demo数据库 | 数据库初始化 |
| db-pwd-simple-jdbc-demo | 方案5 | `@simple_jdbc_db@` | dbpwd_simple_demo | 工具类API调用 |
| db-pwd-druid-demo | 方案1 | `@druid_demo_db@` | dbpwd_druid_demo | 密码回调机制 |
| db-pwd-spring-boot-demo | 方案2 | `@spring_boot_db@` | dbpwd_spring_boot_demo | 自动配置 |
| db-pwd-spring-demo | 方案3 | `@spring_framework_db@` | dbpwd_spring_demo | BeanPostProcessor |
| db-pwd-mybatis-demo | 方案4 | `@mybatis_demo_db@` | dbpwd_mybatis_demo | DataSourceFactory |
| db-pwd-servlet-demo | 方案6 | `@servlet_demo_db@` | dbpwd_servlet_demo | InitListener |

---

## Demo 0: db-pwd-demo-init（初始化工具）

### 功能说明

为所有其他 Demo 创建 H2 FILE 数据库，支持密码验证。

### 核心实现

```java
// DatabaseInitializer.java
public class DatabaseInitializer {

    private static final String DATA_DIR = "/.dbpwd/h2_data";

    public static void main(String[] args) {
        // 从 db.pwd 获取密码
        String plainPassword = PasswordService.getDatabasePassword(placeholder);

        // 连接 H2 FILE 数据库（首次连接创建并绑定密码）
        String url = "jdbc:h2:file:" + dataDir + "/" + dbName + ";MODE=MySQL";
        Connection conn = DriverManager.getConnection(url, "sa", plainPassword);

        // 创建表结构
        stmt.execute(tableSql);
    }
}
```

### 使用步骤

```bash
# 1. 构建
mvn clean install -DskipTests

# 2. 运行初始化工具
java -jar db-pwd-demo-init/target/db-pwd-demo-init-1.0.0.jar
```

### 验证点

- [x] 读取 `~/.dbpwd/db.pwd` 获取密码配置
- [x] 使用 `~/.dbpwd/sm4.key` 解密密码
- [x] 创建 H2 FILE 数据库文件
- [x] 数据库绑定密码，支持后续密码验证

---

## Demo 1: db-pwd-simple-jdbc-demo

### 集成模式

方案5 - StandalonePasswordDecryptor

### 设计说明

纯 JDBC 应用，演示独立工具类使用，无框架依赖。

### 核心代码

```java
// SimpleJdbcDemo.java
public class SimpleJdbcDemo {

    public static void main(String[] args) {
        // 方式1: 直接获取密码
        String password = StandalonePasswordDecryptor
            .getDatabasePassword("@simple_jdbc_db@");

        // 方式2: 获取连接
        try (Connection conn = StandalonePasswordDecryptor
                .getConnection("@simple_jdbc_db@")) {
            // 执行CRUD操作
        }

        // 方式3: 使用DbUtil简化
        DbUtil.query("@simple_jdbc_db@", "SELECT * FROM users", rs -> {
            while (rs.next()) {
                System.out.println(rs.getString("username"));
            }
        });
    }
}
```

### 运行命令

```bash
java -jar db-pwd-simple-jdbc-demo/target/db-pwd-simple-jdbc-demo-1.0.0.jar
```

### 验证点

- [x] 密码正确解密
- [x] H2 FILE 数据库密码验证通过
- [x] CRUD 操作正常
- [x] 无框架依赖

---

## Demo 2: db-pwd-druid-demo

### 集成模式

方案1 - Druid PasswordCallback

### 设计说明

Druid 连接池应用，利用原生 PasswordCallback 机制。

### 核心代码

```java
// DruidDemo.java
public class DruidDemo {

    public static void main(String[] args) {
        // 创建 Druid 数据源
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:file:~/.dbpwd/h2_data/dbpwd_druid_demo");
        dataSource.setUsername("sa");
        dataSource.setPassword("@druid_demo_db@");  // 占位符

        // 设置密码回调
        Properties props = new Properties();
        props.setProperty("placeholder", "druid_demo_db");
        dataSource.setPasswordCallback(new DruidPasswordCallback(props));

        // 获取连接（自动解密密码）
        try (Connection conn = dataSource.getConnection()) {
            // 执行CRUD操作
        }
    }
}
```

### 运行命令

```bash
java -jar db-pwd-druid-demo/target/db-pwd-druid-demo-1.0.0.jar
```

### 验证点

- [x] Druid 连接池正常初始化
- [x] 密码在 getConnection() 时自动解密
- [x] H2 FILE 数据库密码验证通过
- [x] 连接池性能正常

---

## Demo 3: db-pwd-spring-boot-demo

### 集成模式

方案2 - Spring Boot Starter

### 设计说明

Spring Boot Web 应用，通过自动配置实现密码解密。

### 核心配置

```yaml
# application.yml（应用无需配置文件路径）
spring:
  datasource:
    url: jdbc:h2:file:~/.dbpwd/h2_data/dbpwd_spring_boot_demo
    username: sa
    password: "@spring_boot_db@"  # 占位符格式
```

### 核心代码

```java
// SpringBootDemo.java
@SpringBootApplication
public class SpringBootDemo {
    public static void main(String[] args) {
        SpringApplication.run(SpringBootDemo.class, args);
    }
}

@RestController
class DemoController {
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/users")
    public List<User> getUsers() {
        return jdbcTemplate.query("SELECT * FROM users", ...);
    }
}
```

### 运行命令

```bash
java -jar db-pwd-spring-boot-demo/target/db-pwd-spring-boot-demo-1.0.0.jar
```

### 验证点

- [x] Spring Boot 自动配置生效
- [x] DataSource Bean 自动装饰
- [x] 密码自动解密
- [x] REST API 正常工作

---

## Demo 4: db-pwd-spring-demo

### 集成模式

方案3 - Spring BeanPostProcessor

### 设计说明

传统 Spring 应用，通过 BeanPostProcessor 拦截 DataSource。

### 核心配置

```java
@Configuration
public class SpringAppConfig {

    @Bean
    public DataSourcePasswordProcessor dataSourcePasswordProcessor() {
        // 无需指定文件路径
        return new DataSourcePasswordProcessor();
    }

    @Bean
    public DataSource dataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:h2:file:~/.dbpwd/h2_data/dbpwd_spring_demo");
        ds.setUsername("sa");
        ds.setPassword("@spring_framework_db@");  // 占位符
        return ds;
    }
}
```

### 运行命令

```bash
java -jar db-pwd-spring-demo/target/db-pwd-spring-demo-1.0.0.jar
```

### 验证点

- [x] BeanPostProcessor 正确注册
- [x] DataSource Bean 被正确拦截
- [x] 密码自动解密
- [x] JdbcTemplate 正常工作

---

## Demo 5: db-pwd-mybatis-demo

### 集成模式

方案4 - MyBatis DataSourceFactory

### 设计说明

纯 MyBatis 应用，通过工厂类扩展实现密码解密。

### 核心配置

```xml
<!-- mybatis-config.xml（应用无需配置文件路径） -->
<configuration>
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="com.dbpwd.mybatis.factory.DbPwdDataSourceFactory">
                <property name="driver" value="org.h2.Driver"/>
                <property name="url" value="jdbc:h2:file:~/.dbpwd/h2_data/dbpwd_mybatis_demo"/>
                <property name="username" value="sa"/>
                <property name="password" value="@mybatis_demo_db@"/>
            </dataSource>
        </environment>
    </environments>
</configuration>
```

### 运行命令

```bash
java -jar db-pwd-mybatis-demo/target/db-pwd-mybatis-demo-1.0.0.jar
```

### 验证点

- [x] MyBatis 配置正确解析
- [x] DataSourceFactory 正确创建
- [x] 密码自动解密
- [x] Mapper 接口正常工作

---

## Demo 6: db-pwd-servlet-demo

### 集成模式

方案6 - Servlet InitListener

### 设计说明

纯 Servlet 应用，通过 ServletContextListener 初始化密码服务。

### 核心配置

```xml
<!-- web.xml（应用无需配置文件路径） -->
<listener>
    <listener-class>com.dbpwd.web.listener.DatabasePasswordInitListener</listener-class>
</listener>
```

### 核心代码

```java
@WebServlet("/api/users")
public class UserServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        // 使用 WebPasswordUtil 获取连接
        try (Connection conn = WebPasswordUtil.getConnection(
                getServletContext(), "@servlet_demo_db@")) {
            // 执行数据库操作
        }
    }
}
```

### 运行命令

```bash
# 部署到 Tomcat
cp target/db-pwd-servlet-demo-1.0.0.war $TOMCAT_HOME/webapps/

# 或使用 Maven 插件
mvn tomcat7:run -pl db-pwd-servlet-demo
```

### 验证点

- [x] ServletContextListener 正确注册
- [x] 密码服务正确初始化
- [x] WebPasswordUtil 正常工作
- [x] Servlet API 正常访问

---

## 统一数据库设计

### H2 FILE 模式

所有 Demo 使用 H2 FILE 数据库，支持真正的密码验证：

- 首次连接时创建数据库并绑定密码
- 后续连接必须使用相同密码
- 模拟生产环境数据库密码验证机制

### 数据库表结构

```sql
CREATE TABLE users (
    id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE products (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(200),
    price DECIMAL(10,2),
    stock INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 运行验证流程

### 完整验证步骤

```bash
# 1. 构建所有模块
mvn clean install -DskipTests

# 2. 初始化 H2 FILE 数据库（必须首先执行）
java -jar db-pwd-demo-init/target/db-pwd-demo-init-1.0.0.jar

# 3. 运行各 Demo 应用
java -jar db-pwd-simple-jdbc-demo/target/db-pwd-simple-jdbc-demo-1.0.0.jar
java -jar db-pwd-druid-demo/target/db-pwd-druid-demo-1.0.0.jar
java -jar db-pwd-spring-boot-demo/target/db-pwd-spring-boot-demo-1.0.0.jar
java -jar db-pwd-spring-demo/target/db-pwd-spring-demo-1.0.0.jar
java -jar db-pwd-mybatis-demo/target/db-pwd-mybatis-demo-1.0.0.jar
```

### 验证检查清单

| Demo | 密码解密 | 数据库连接 | CRUD操作 | 特定验证 |
|------|---------|-----------|---------|----------|
| simple-jdbc | ✅ | ✅ | ✅ | API调用方式 |
| druid | ✅ | ✅ | ✅ | 回调机制 |
| spring-boot | ✅ | ✅ | ✅ | REST API |
| spring | ✅ | ✅ | ✅ | Bean拦截 |
| mybatis | ✅ | ✅ | ✅ | Mapper接口 |
| servlet | ✅ | ✅ | ✅ | Servlet API |

---

## 相关文档

- [集成模式设计文档](integration-patterns.md) - 6种集成模式的详细设计
- [API参考文档](api-reference.md) - 核心API使用指南
