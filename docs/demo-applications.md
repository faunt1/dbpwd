# Database Password Manager - Demo应用设计文档

> 文档版本: v3.1
> 最后更新: 2026-02-19

## 目录结构说明

**注意**: Demo 应用已重组到 `demos/` 目录下，采用分类子目录结构：

```
demos/
├── simple-jdbc/    # 方案5
├── druid/         # 方案1
├── spring-boot/   # 方案2
├── spring/        # 方案3
├── mybatis/       # 方案4
├── servlet/       # 方案6
└── struts/        # Struts 1.x
```

运行命令示例（以 simple-jdbc 为例）：
```bash
# 构建
mvn package -pl demos/simple-jdbc/demo -am -DskipTests

# 运行
java -Dfile.encoding=UTF-8 -jar demos/simple-jdbc/demo/target/simple-jdbc-demo-1.0.0-jar-with-dependencies.jar
```

---

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

| Demo | 集成模式 | 配置方式 | 占位符 | H2数据库 | 验证重点 |
|------|---------|---------|--------|----------|----------|
| db-pwd-demo-init | - | - | - | 所有Demo数据库 | 数据库初始化 |
| demos/simple-jdbc/demo | 方案5 | 编程API | `@simple_jdbc_db@` | dbpwd_simple_demo | 工具类API调用 |
| demos/druid/demo | 方案1 | Java代码 | `@druid_demo_db@` | dbpwd_druid_demo | 密码回调机制 |
| demos/druid/demo-spring | 方案1 | Spring XML | `@druid_demo_spring_db@` | dbpwd_druid_demo_spring | XML配置方式 |
| demos/spring-boot/demo | 方案2 | Starter自动配置 | `@spring_boot_db@` | dbpwd_spring_boot_demo | 自动配置 |
| demos/spring-boot/demo-api | 方案2 | 编程API | `@spring_boot_api_db@` | dbpwd_spring_boot_api_demo | 手动配置DataSource |
| db-pwd-spring-demo | 方案3 | Java Config | `@spring_framework_db@` | dbpwd_spring_demo | BeanPostProcessor |
| db-pwd-spring-demo-xml | 方案3 | Spring XML | `@spring_demo_xml_db@` | dbpwd_spring_xml_demo | XML配置方式 |
| db-pwd-mybatis-demo | 方案4 | mybatis-config.xml | `@mybatis_demo_db@` | dbpwd_mybatis_demo | DataSourceFactory |
| db-pwd-servlet-demo | 方案6 | web.xml | `@servlet_demo_db@` | dbpwd_servlet_demo | InitListener |
| db-pwd-servlet-demo-annotation | 方案6 | 注解方式 | `@servlet_demo_annotation_db@` | dbpwd_servlet_annotation_demo | @WebListener |
| db-pwd-struts-demo | 方案6 | web.xml | `@struts_demo_db@` | dbpwd_struts_demo | Struts集成 |

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

## Demo 1: demos/simple-jdbc/demo

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
java -jar demos/simple-jdbc/demo/target/simple-jdbc-demo-1.0.0-jar-with-dependencies.jar
```

### 验证点

- [x] 密码正确解密
- [x] H2 FILE 数据库密码验证通过
- [x] CRUD 操作正常
- [x] 无框架依赖

---

## Demo 2: demos/druid/demo

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
java -jar demos/druid/demo/target/druid-demo-1.0.0.jar
```

### 验证点

- [x] Druid 连接池正常初始化
- [x] 密码在 getConnection() 时自动解密
- [x] H2 FILE 数据库密码验证通过
- [x] 连接池性能正常

---

## Demo 3: demos/spring-boot/demo

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
java -jar demos/spring-boot/demo/target/spring-boot-demo-1.0.0.jar
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
java -jar demos/spring/demo/target/spring-demo-1.0.0.jar
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
java -jar demos/mybatis/demo/target/mybatis-demo-1.0.0.jar
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
mvn tomcat7:run -pl demos/servlet/demo
```

### 验证点

- [x] ServletContextListener 正确注册
- [x] 密码服务正确初始化
- [x] WebPasswordUtil 正常工作
- [x] Servlet API 正常访问

---

## Demo 7: demos/druid/demo-spring

### 集成模式

方案1 - Druid PasswordCallback (Spring XML 配置方式)

### 设计说明

演示通过 Spring XML 配置文件集成 Druid PasswordCallback，无需修改 Java 代码。

### 核心配置

```xml
<!-- druid-datasource.xml -->
<bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource"
      init-method="init" destroy-method="close">
    <property name="url" value="jdbc:h2:file:~/.dbpwd/h2_data/druid_demo_spring_db;MODE=MySQL"/>
    <property name="username" value="sa"/>
    <property name="password" value="@druid_demo_spring_db@"/>
    <!-- 关键配置: PasswordCallback类名 -->
    <property name="passwordCallbackClassName" 
              value="com.dbpwd.druid.DbPwdDruidPasswordCallback"/>
</bean>
```

### 核心代码

```java
// DruidSpringDemoApplication.java
public class DruidSpringDemoApplication {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = 
            new ClassPathXmlApplicationContext("druid-datasource.xml");
        DataSource dataSource = context.getBean(DataSource.class);
        // 密码已自动解密
    }
}
```

### 运行命令

```bash
java -jar demos/druid/spring/target/druid-spring-1.0.0.jar
```

### 验证点

- [x] Spring XML 配置正确加载
- [x] PasswordCallback 通过配置生效
- [x] 密码自动解密
- [x] 无需修改 Java 代码

---

## Demo 8: demos/spring-boot/demo-api

### 集成模式

方案2 - Spring Boot (编程 API 方式)

### 设计说明

演示不使用 Spring Boot Starter 自动配置，而是手动配置 DataSource，适用于需要完全控制的场景。

### 核心配置

```java
// SpringBootApiDemoApplication.java
@SpringBootApplication
public class SpringBootApiDemoApplication {

    @Bean
    public DataSource dataSource() {
        // 使用 PasswordService API 手动获取密码
        String plainPassword = PasswordService.getDatabasePassword("@spring_boot_api_db@");
        
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl("jdbc:h2:file:~/.dbpwd/h2_data/spring_boot_api_db;MODE=MySQL");
        dataSource.setUsername("sa");
        dataSource.setPassword(plainPassword);
        return dataSource;
    }
}
```

### 运行命令

```bash
java -jar demos/spring-boot/api/target/spring-boot-api-1.0.0.jar
# 访问 http://localhost:8082/api/test
```

### 验证点

- [x] 手动配置 DataSource 生效
- [x] PasswordService API 正确调用
- [x] 密码成功解密
- [x] REST API 正常工作

---

## Demo 9: db-pwd-spring-demo-xml

### 集成模式

方案3 - Spring BeanPostProcessor (XML 配置方式)

### 设计说明

演示通过 Spring XML 配置文件集成 BeanPostProcessor，自动解密 DataSource 密码。

### 核心配置

```xml
<!-- applicationContext.xml -->
<bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource">
    <property name="url" value="jdbc:h2:file:~/.dbpwd/h2_data/spring_demo_xml_db;MODE=MySQL"/>
    <property name="username" value="sa"/>
    <!-- 密码占位符会被 BeanPostProcessor 自动解密 -->
    <property name="password" value="@spring_demo_xml_db@"/>
</bean>

<!-- DataSourcePasswordPostProcessor 由 db-pwd-spring 模块自动提供 -->
```

### 核心代码

```java
// SpringXmlDemoApplication.java
public class SpringXmlDemoApplication {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = 
            new ClassPathXmlApplicationContext("applicationContext.xml");
        DataSource dataSource = context.getBean(DataSource.class);
        // 密码已被 BeanPostProcessor 自动解密
    }
}
```

### 运行命令

```bash
java -jar db-pwd-spring-demo-xml/target/db-pwd-spring-demo-xml-1.0.0.jar
```

### 验证点

- [x] Spring XML 配置正确加载
- [x] BeanPostProcessor 自动拦截 DataSource
- [x] 密码自动解密
- [x] 数据库连接正常

---

## Demo 10: db-pwd-servlet-demo-annotation

### 集成模式

方案6 - Servlet (注解配置方式)

### 设计说明

演示使用 Servlet 3.0+ 注解方式配置，无需 web.xml 文件。

### 核心代码

```java
// AnnotationDemoInitializer.java
@WebListener
public class AnnotationDemoInitializer implements ServletContextListener {
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 使用 PasswordService API 解密密码
        String plainPassword = PasswordService.getDatabasePassword("@servlet_demo_annotation_db@");
        
        // 创建 DataSource
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:file:~/.dbpwd/h2_data/servlet_demo_annotation_db;MODE=MySQL");
        dataSource.setPassword(plainPassword);
        
        // 存入 ServletContext
        sce.getServletContext().setAttribute("dataSource", dataSource);
    }
}

// ProductApiServlet.java
@WebServlet("/api/products")
public class ProductApiServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        DataSource ds = (DataSource) getServletContext().getAttribute("dataSource");
        // 执行数据库操作
    }
}
```

### 运行命令

```bash
mvn tomcat7:run -pl demos/servlet/annotation
# 访问 http://localhost:8083/dbpwd-servlet-annotation-demo/api/products
```

### 验证点

- [x] @WebListener 注解正确识别
- [x] @WebServlet 注解正确识别
- [x] 无需 web.xml 配置文件
- [x] 数据库连接正常

---

## Demo 11: db-pwd-struts-demo

### 集成模式

方案6 - Servlet InitListener (Struts 框架)

### 设计说明

演示在 Struts 框架中集成密码管理，通过 ServletContextListener 初始化。

### 核心配置

```xml
<!-- web.xml -->
<listener>
    <listener-class>com.dbpwd.web.DatabasePasswordInitListener</listener-class>
</listener>

<filter>
    <filter-name>struts2</filter-name>
    <filter-class>org.apache.struts2.dispatcher.filter.StrutsPrepareAndExecuteFilter</filter-class>
</filter>
```

### 运行命令

```bash
mvn tomcat7:run -pl demos/struts/demo
```

### 验证点

- [x] Struts 框架正常启动
- [x] 密码服务正确初始化
- [x] Action 中数据库操作正常

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

# 方案5: 独立JDBC应用
java -jar demos/simple-jdbc/demo/target/simple-jdbc-demo-1.0.0-jar-with-dependencies.jar

# 方案1: Druid PasswordCallback
java -jar demos/druid/demo/target/druid-demo-1.0.0.jar
java -jar demos/druid/spring/target/druid-spring-1.0.0.jar

# 方案2: Spring Boot
java -jar demos/spring-boot/demo/target/spring-boot-demo-1.0.0.jar
java -jar demos/spring-boot/api/target/spring-boot-api-1.0.0.jar

# 方案3: Spring Framework
java -jar demos/spring/demo/target/spring-demo-1.0.0.jar
java -jar db-pwd-spring-demo-xml/target/db-pwd-spring-demo-xml-1.0.0.jar

# 方案4: MyBatis
java -jar demos/mybatis/demo/target/mybatis-demo-1.0.0.jar

# 方案6: Servlet/Web (需要Servlet容器)
mvn tomcat7:run -pl demos/servlet/demo           # web.xml配置
mvn tomcat7:run -pl demos/servlet/annotation # 注解方式
mvn tomcat7:run -pl demos/struts/demo            # Struts集成
```

---

## Servlet Demo 测试命令

### 启动命令（后台运行）

使用 PowerShell 后台启动 Jetty 服务器：

```powershell
# Servlet Demo (集成版本) - 端口 8081
powershell -Command "Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', 'mvn jetty:run -pl demos/servlet/demo' -WorkingDirectory 'C:\gitprjs\dbpwd' -NoNewWindow"

# Servlet Demo (原始版本) - 端口 8082
powershell -Command "Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', 'mvn jetty:run -pl demos/servlet/original' -WorkingDirectory 'C:\gitprjs\dbpwd' -NoNewWindow"

# Servlet Demo (注解方式) - 端口 8083
powershell -Command "Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', 'mvn jetty:run -pl demos/servlet/annotation' -WorkingDirectory 'C:\gitprjs\dbpwd' -NoNewWindow"
```

### 等待服务器启动

```powershell
ping -n 15 127.0.0.1 >nul
```

### 测试 API

```powershell
# Servlet Demo (集成版本)
curl -s http://localhost:8081/dbpwd-servlet-demo/api/users

# Servlet Demo (原始版本)
curl -s http://localhost:8082/dbpwd-servlet-demo-original/api/users

# Servlet Demo (注解方式)
curl -s http://localhost:8083/dbpwd-servlet-annotation-demo/api/products
curl -s http://localhost:8083/dbpwd-servlet-annotation-demo/api/info
```

### 停止服务器

```powershell
# 停止指定模块的 Jetty
mvn jetty:stop -pl demos/servlet/demo
mvn jetty:stop -pl demos/servlet/original
mvn jetty:stop -pl demos/servlet/annotation

# 或者查找进程并手动停止
netstat -ano | findstr "8081"
taskkill /F /PID <PID>
```

### 完整测试流程

```powershell
# 1. 初始化数据库（首次或重新初始化）
java -Dfile.encoding=UTF-8 -jar db-pwd-demo-init/target/db-pwd-demo-init-1.0.0-jar-with-dependencies.jar --all

# 2. 启动 Servlet Demo
powershell -Command "Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', 'mvn jetty:run -pl demos/servlet/demo' -WorkingDirectory 'C:\gitprjs\dbpwd' -NoNewWindow"

# 3. 等待启动
ping -n 15 127.0.0.1 >nul

# 4. 测试 API
curl -s http://localhost:8081/dbpwd-servlet-demo/api/users

# 5. 停止服务器
mvn jetty:stop -pl demos/servlet/demo
```

> **注意**: Windows 环境必须使用 `-Dfile.encoding=UTF-8` 参数，否则中文输出会出现乱码。

### 验证检查清单

| Demo | 密码解密 | 数据库连接 | CRUD操作 | 特定验证 |
|------|---------|-----------|---------|----------|
| simple-jdbc | ✅ | ✅ | ✅ | API调用方式 |
| druid | ✅ | ✅ | ✅ | 回调机制 |
| druid-spring | ✅ | ✅ | ✅ | XML配置方式 |
| spring-boot | ✅ | ✅ | ✅ | REST API |
| spring-boot-api | ✅ | ✅ | ✅ | 手动配置DataSource |
| spring | ✅ | ✅ | ✅ | Bean拦截 |
| spring-xml | ✅ | ✅ | ✅ | XML配置方式 |
| mybatis | ✅ | ✅ | ✅ | Mapper接口 |
| servlet | ✅ | ✅ | ✅ | web.xml配置 |
| servlet-annotation | ✅ | ✅ | ✅ | 注解方式 |
| struts | ✅ | ✅ | ✅ | Struts集成 |

---

## 相关文档

- [集成模式设计文档](integration-patterns.md) - 6种集成模式的详细设计
- [API参考文档](api-reference.md) - 核心API使用指南
