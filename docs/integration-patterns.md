# Database Password Manager - 集成模式设计文档

> 文档版本: v2.0
> 最后更新: 2026-02-14

## 概述

Database Password Manager 提供 6 种基层集成模式，覆盖主流 Java 技术栈的数据库密码管理场景。

---

## 集成模式总览

| 模式 | 名称 | 适用场景 | 核心机制 | 集成模块 |
|------|------|---------|---------|----------|
| 方案1 | Druid PasswordCallback | Druid连接池应用 | 原生密码回调 | db-pwd-druid |
| 方案2 | Spring Boot Starter | Spring Boot应用 | 自动配置装饰 | db-pwd-spring-boot-starter |
| 方案3 | Spring BeanPostProcessor | 传统Spring应用 | Bean后置处理 | db-pwd-spring |
| 方案4 | MyBatis DataSourceFactory | 纯MyBatis应用 | 工厂类扩展 | db-pwd-mybatis |
| 方案5 | StandalonePasswordDecryptor | 独立应用/工具 | 工具类API | db-pwd-core |
| 方案6 | Servlet InitListener | Servlet/Web应用 | 容器监听器 | db-pwd-web |

---

## 方案1: Druid PasswordCallback

### 设计思路

利用 Druid 连接池原生的 `PasswordCallback` 机制，在获取连接时自动解密密码。

### 核心实现

```java
// DruidPasswordCallback.java
public class DruidPasswordCallback implements PasswordCallback {

    private String passwordFile;
    private String keyFile;
    private String placeholder;

    @Override
    public void init(Properties properties) {
        this.passwordFile = properties.getProperty("passwordFile");
        this.keyFile = properties.getProperty("keyFile");
        this.placeholder = properties.getProperty("placeholder");
    }

    @Override
    public String getPassword() {
        // 从db.pwd获取加密密码
        String encryptedPassword = PasswordFileManager
            .load(passwordFile)
            .getDatabaseById(placeholder)
            .getPassword();

        // 使用SM4密钥解密
        byte[] key = Sm4KeyLoader.loadKeyFromFile(keyFile);
        return new PasswordDecryptor().decrypt(encryptedPassword, key);
    }
}
```

### 配置方式

```java
// 创建Druid数据源
DruidDataSource dataSource = new DruidDataSource();
datasource.setUrl("jdbc:mysql://localhost:3306/mydb");
datasource.setUsername("sa");
datasource.setPassword("@druid_demo_db@");  // 占位符格式: @db_name@

// 设置密码回调（无需指定文件路径，由PasswordService内部处理）
Properties callbackProps = new Properties();
callbackProps.setProperty("placeholder", "druid_demo_db");

dataSource.setPasswordCallback(
    new DruidPasswordCallback(callbackProps)
);
```

**说明**: 文件路径 `~/.dbpwd/` 由 `PasswordService` 内部固定，应用无需也无需知道文件位置，增强安全性。

### 验证点

- [x] Druid连接池正常初始化
- [x] 密码在getConnection()时自动解密
- [x] H2 FILE数据库密码验证通过
- [x] 连接池性能正常

---

## 方案2: Spring Boot Starter

### 设计思路

通过 Spring Boot 自动配置机制，在 DataSource 初始化后自动装饰，注入密码解密逻辑。

### 核心实现

```java
// DbPwdAutoConfiguration.java
@Configuration
@ConditionalOnProperty(prefix = "dbpwd", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DbPwdAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public PasswordService passwordService(
        @Value("${dbpwd.password-file:~/.dbpwd/db.pwd}") String passwordFile,
        @Value("${dbpwd.key-file:~/.dbpwd/sm4.key}") String keyFile
    ) {
        return new PasswordService(passwordFile, keyFile);
    }

    @Bean
    public DataSourcePasswordProcessor dataSourcePasswordProcessor(PasswordService passwordService) {
        return new DataSourcePasswordProcessor(passwordService);
    }
}
```

### 配置方式

```yaml
# application.yml（应用无需配置文件路径）
spring:
  datasource:
    url: jdbc:h2:file:~/.dbpwd/h2_data/dbpwd_spring_boot_demo
    username: sa
    password: "@spring_boot_db@"  # 占位符格式: @db_name@
```

**说明**: `PasswordService` 自动从 `~/.dbpwd/` 目录读取配置文件，应用完全屏蔽文件位置信息。

### 验证点

- [x] Spring Boot自动配置生效
- [x] DataSource Bean自动装饰
- [x] 密码自动解密
- [x] REST API正常工作

---

## 方案3: Spring BeanPostProcessor

### 设计思路

针对传统 Spring 应用（非 Boot），通过 `BeanPostProcessor` 拦截 DataSource Bean，实现密码自动解密。

### 核心实现

```java
// DataSourcePasswordProcessor.java
public class DataSourcePasswordProcessor implements BeanPostProcessor, Ordered {

    private final PasswordService passwordService;

    public DataSourcePasswordProcessor(String passwordFile, String keyFile) {
        this.passwordService = new PasswordService(passwordFile, keyFile);
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        if (bean instanceof DataSource) {
            decorateDataSource((DataSource) bean);
        }
        return bean;
    }
}
```

### 配置方式

```java
@Configuration
public class SpringAppConfig {

    @Bean
    public DataSourcePasswordProcessor dataSourcePasswordProcessor() {
        // 无需指定文件路径，PasswordService内部固定为~/.dbpwd/
        return new DataSourcePasswordProcessor();
    }

    @Bean
    public DataSource dataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:h2:file:~/.dbpwd/h2_data/dbpwd_spring_demo");
        ds.setUsername("sa");
        ds.setPassword("@spring_framework_db@");  // 占位符格式: @db_name@
        return ds;
    }
}
```

**说明**: 文件路径由 `PasswordService` 内部固定，应用配置中不再暴露文件位置。

### 验证点

- [x] BeanPostProcessor正确注册
- [x] DataSource Bean被正确拦截
- [x] 密码自动解密
- [x] JdbcTemplate正常工作

---

## 方案4: MyBatis DataSourceFactory

### 设计思路

扩展 MyBatis 的 `UnpooledDataSourceFactory`，在创建 DataSource 时自动注入解密逻辑。

### 配置方式

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
                <property name="password" value="@mybatis_demo_db@"/>  <!-- 占位符格式: @db_name@ -->
            </dataSource>
        </environment>
    </environments>
</configuration>
```

**说明**: `DbPwdDataSourceFactory` 自动从 `~/.dbpwd/` 目录读取配置文件，应用无需知道文件位置。

### 验证点

- [x] MyBatis配置正确解析
- [x] DataSourceFactory正确创建
- [x] 密码自动解密
- [x] Mapper接口正常工作

---

## 方案5: StandalonePasswordDecryptor

### 设计思路

提供独立的工具类，适合无框架的纯 JDBC 应用或命令行工具使用。

### 使用方式

```java
// 方式1: 直接获取密码（占位符格式: @db_name@）
String password = StandalonePasswordDecryptor.getDatabasePassword("@simple_jdbc_db@");

// 方式2: 获取连接
Connection conn = StandalonePasswordDecryptor.getConnection("@simple_jdbc_db@");

// 方式3: 使用DbUtil简化操作
StandalonePasswordDecryptor.DbUtil.query(
    "@simple_jdbc_db@",
    "SELECT * FROM users",
    rs -> { while (rs.next()) System.out.println(rs.getString("username")); }
);
```

**说明**: `StandalonePasswordDecryptor` 内部自动从 `~/.dbpwd/` 目录读取配置文件，调用方无需知道文件位置。

### 验证点

- [x] 密码正确解密
- [x] 数据库连接成功
- [x] DbUtil操作正常
- [x] 无框架依赖

---

## 方案6: Servlet InitListener

### 设计思路

通过 Servlet 容器的 `ServletContextListener`，在应用启动时初始化密码服务。

### 配置方式

```xml
<!-- web.xml（应用无需配置文件路径） -->
<listener>
    <listener-class>com.dbpwd.web.listener.DatabasePasswordInitListener</listener-class>
</listener>
```

**说明**: `DatabasePasswordInitListener` 自动从 `~/.dbpwd/` 目录读取配置文件，应用无需知道文件位置。

### 使用方式

```java
@WebServlet("/api/users")
public class UserServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        try (Connection conn = WebPasswordUtil.getConnection(
                getServletContext(), "servlet_demo_db")) {
            // 执行数据库操作
        }
    }
}
```

### 验证点

- [x] ServletContextListener正确注册
- [x] 密码服务正确初始化
- [x] WebPasswordUtil正常工作
- [x] Servlet API正常访问

---

## 模式对比总结

| 特性 | 方案1 | 方案2 | 方案3 | 方案4 | 方案5 | 方案6 |
|------|-------|-------|-------|-------|-------|-------|
| **适用框架** | Druid | Spring Boot | Spring | MyBatis | 无框架 | Servlet |
| **配置方式** | Java配置 | YAML配置 | Java/XML | XML配置 | 代码调用 | web.xml |
| **自动解密** | ✅ | ✅ | ✅ | ✅ | ❌ 手动 | ✅ |
| **框架依赖** | Druid | Spring Boot | Spring | MyBatis | 无 | Servlet API |
| **集成复杂度** | 低 | 最低 | 低 | 低 | 最低 | 低 |
