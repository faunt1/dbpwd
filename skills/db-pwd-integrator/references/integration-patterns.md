# 集成模式详细说明

本文档详细说明 db-pwd 的 6 种集成模式，包括适用场景、配置方式和实现原理。

## 模式总览

| 模式 | 适用场景 | 核心机制 | 自动解密 |
|------|---------|---------|----------|
| 方案1 | Druid 连接池 | PasswordCallback | ✅ |
| 方案2 | Spring Boot | Starter 自动配置 | ✅ |
| 方案3 | Spring Framework | BeanPostProcessor | ✅ |
| 方案4 | MyBatis | DataSourceFactory | ✅ |
| 方案5 | 纯 JDBC | API 调用 | ❌ 手动 |
| 方案6 | Servlet/Web | ContextListener | ✅ |

---

## 方案1: Druid PasswordCallback

### 适用场景
- 使用 Alibaba Druid 连接池的应用
- 支持 Druid 1.0.x ~ 1.2.x

### 核心机制
利用 Druid 原生的 `PasswordCallback` 机制，在获取连接时自动解密密码。

### 依赖
```xml
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-druid</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 配置方式

**方式 A: 直接使用 PasswordService（推荐）**
```java
import com.dbpwd.core.PasswordService;

DruidDataSource dataSource = new DruidDataSource();
dataSource.setUrl("jdbc:mysql://localhost:3306/mydb");
dataSource.setUsername("root");
dataSource.setPassword(PasswordService.getDatabasePassword("@my_db@"));
```

**方式 B: 使用 DbPwdDruidPasswordCallback**
```java
import com.dbpwd.druid.DbPwdDruidPasswordCallback;

DruidDataSource dataSource = new DruidDataSource();
dataSource.setPasswordCallback(new DbPwdDruidPasswordCallback());
dataSource.setPassword("@my_db@");  // 占位符
```

### 检测信号
```bash
grep -r "DruidDataSource" src/ --include="*.java"
```

---

## 方案2: Spring Boot Starter

### 适用场景
- Spring Boot 应用
- 支持 Spring Boot 2.x（如需 3.x 需升级 Java 17+）

### 核心机制
通过 Spring Boot 自动配置，在 DataSource 初始化后自动装饰，注入密码解密逻辑。

### 依赖
```xml
<!-- 仅需此一个依赖，会自动传递 db-pwd-core -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

> **注意**: `db-pwd-spring-boot-starter` 已包含 `db-pwd-core` 传递依赖，无需单独添加。

### 配置方式
```yaml
# application.yml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: "@my_db@"  # 占位符格式
```

**无需额外代码**，Starter 会自动：
1. 检测所有 DataSource Bean
2. 替换占位符为解密后的密码
3. 支持多数据源

### 检测信号
```bash
grep -r "@SpringBootApplication" src/ --include="*.java"
grep "spring-boot-starter" pom.xml
```

---

## 方案3: Spring BeanPostProcessor

### 适用场景
- 传统 Spring 应用（非 Boot）
- 支持 Spring 4.x ~ 5.x

### 核心机制
通过 `BeanPostProcessor` 拦截 DataSource Bean，实现密码自动解密。

### 依赖
```xml
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-spring</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 配置方式

**Java Config**:
```java
import com.dbpwd.spring.DataSourcePasswordProcessor;

@Configuration
public class DatabaseConfig {
    
    @Bean
    public DataSourcePasswordProcessor dataSourcePasswordProcessor() {
        return new DataSourcePasswordProcessor();
    }
    
    @Bean
    public DataSource dataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:mysql://localhost:3306/mydb");
        ds.setUsername("root");
        ds.setPassword("@my_db@");  // 占位符格式
        return ds;
    }
}
```

**XML Config**:
```xml
<bean class="com.dbpwd.spring.DataSourcePasswordProcessor"/>
<bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource">
    <property name="url" value="jdbc:mysql://localhost:3306/mydb"/>
    <property name="username" value="root"/>
    <property name="password" value="@my_db@"/>
</bean>
```

### 检测信号
```bash
grep -r "@Configuration" src/ --include="*.java"
grep "spring-context" pom.xml
```

---

## 方案4: MyBatis集成

### 适用场景
- 纯 MyBatis 应用（无 Spring）
- 支持 MyBatis 3.x

### 核心机制
在构建 SqlSessionFactory 时，使用 PasswordService API 获取密码。

### 依赖
```xml
<!-- 仅需核心依赖 -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 配置方式

**方式 A: 使用 PasswordService API（推荐）**
```java
import com.dbpwd.core.PasswordService;

public class MybatisDemoApplication {
    
    private static final String DB_PLACEHOLDER = "@my_db@";
    
    public static void main(String[] args) throws Exception {
        // 使用 PasswordService 获取密码
        String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
        
        DataSource dataSource = new UnpooledDataSource();
        ((UnpooledDataSource) dataSource).setUrl("jdbc:mysql://localhost:3306/mydb");
        ((UnpooledDataSource) dataSource).setUsername("root");
        ((UnpooledDataSource) dataSource).setPassword(plainPassword);
        
        SqlSessionFactory sqlSessionFactory = buildSqlSessionFactory(dataSource);
        // ...
    }
}
```

**方式 B: 使用 DbPwdDataSourceFactory（XML配置）**
```xml
<!-- mybatis-config.xml -->
<!-- 需要额外依赖: db-pwd-mybatis -->
<configuration>
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="com.dbpwd.mybatis.DbPwdDataSourceFactory">
                <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
                <property name="url" value="jdbc:mysql://localhost:3306/mydb"/>
                <property name="username" value="root"/>
                <property name="password" value="@my_db@"/>
            </dataSource>
        </environment>
    </environments>
</configuration>
```

### 检测信号
```bash
find . -name "mybatis-config.xml"
find . -name "*Mapper.xml"
```

---

## 方案5: StandalonePasswordDecryptor

### 适用场景
- 无框架的独立 Java 应用
- 命令行工具
- 测试代码

### 核心机制
提供独立的工具类，直接调用 API 获取解密密码。

### 依赖
```xml
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 使用方式
```java
import com.dbpwd.core.PasswordService;

// 方式1: 获取密码（推荐，支持占位符格式）
String password = PasswordService.getDatabasePassword("@my_db@");
Connection conn = DriverManager.getConnection(url, username, password);

// 方式2: 使用 StandalonePasswordDecryptor（需传入 dbId，不支持 @xx@ 格式）
import com.dbpwd.core.StandalonePasswordDecryptor;
String password = StandalonePasswordDecryptor.getPlainPassword("my_db", null, null);

// 方式3: 直接获取连接
Connection conn = StandalonePasswordDecryptor.getConnection("my_db", null, null);
```

### 检测信号
```bash
grep -r "DriverManager.getConnection" src/ --include="*.java"
```

---

## 方案6: Servlet InitListener

### 适用场景
- 传统 Servlet/Web 应用
- 支持 Servlet 3.0+ / 4.x

### 核心机制
通过 Servlet 容器的 `ServletContextListener`，在应用启动时初始化密码服务。

### 依赖
```xml
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-web</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 配置方式

**web.xml**:
```xml
<listener>
    <listener-class>com.dbpwd.web.DatabasePasswordInitListener</listener-class>
</listener>
```

**注解方式**:
```java
import com.dbpwd.core.PasswordService;
import javax.servlet.annotation.WebListener;
import javax.servlet.ServletContextListener;

@WebListener
public class MyContextListener implements ServletContextListener {
    
    private static final String DB_PLACEHOLDER = "@my_db@";
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        String password = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
        
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:mysql://localhost:3306/mydb");
        ds.setUsername("root");
        ds.setPassword(password);
        
        sce.getServletContext().setAttribute("dataSource", ds);
    }
}
```

### 检测信号
```bash
find . -name "web.xml"
grep -r "@WebServlet\|@WebListener" src/ --include="*.java"
```

---

## 模式选择决策树

```
项目使用 @SpringBootApplication?
├── YES → 方案2: Spring Boot Starter
│
└── NO → 项目使用 DruidDataSource?
    ├── YES → 项目有 @Configuration?
    │   ├── YES → 方案1+3: Druid + Spring
    │   └── NO → 方案1: Druid PasswordCallback
    │
    └── NO → 项目有 @Configuration?
        ├── YES → 方案3: Spring BeanPostProcessor
        │
        └── NO → 项目有 mybatis-config.xml?
            ├── YES → 方案4: MyBatis DataSourceFactory
            │
            └── NO → 项目有 web.xml 或 @WebServlet?
                ├── YES → 方案6: Servlet InitListener
                │
                └── NO → 方案5: StandalonePasswordDecryptor
```

---

## 依赖版本兼容性

| 集成方案 | 核心依赖 | 兼容范围 |
|----------|----------|----------|
| 方案1: Druid | Druid 1.2.20 | Druid 1.0.x ~ 1.2.x |
| 方案2: Spring Boot | Spring Boot 2.7.18 | Spring Boot 2.x |
| 方案3: Spring | Spring 5.3.31 | Spring 4.x ~ 5.x |
| 方案4: MyBatis | MyBatis 3.5.13 | MyBatis 3.x |
| 方案6: Servlet | Servlet 3.1.0 | Servlet 3.0+ / 4.x |

> **注意**: Spring Boot 3.x 需要 Java 17+ 并将 `javax.*` 替换为 `jakarta.*`
