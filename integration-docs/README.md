# Database Password Manager - 应用集成参考文档

> 本文档帮助用户根据应用类型选择正确的集成模式，并提供详细的代码修改指南。
>
> 最后更新: 2026-02-20

## 目录

1. [快速选择指南](#快速选择指南)
2. [集成模式详解](#集成模式详解)
3. [代码修改详解](#代码修改详解)
4. [常见问题](#常见问题)

---

## 快速选择指南

### 根据应用类型选择集成模式

| 应用类型 | 推荐集成模式 | 核心集成类 | 难度 |
|---------|------------|----------|-----|
| 纯JDBC应用 | 模式5: Standalone | `PasswordService` | ⭐ |
| Druid连接池 | 模式1: Druid PasswordCallback | `DbPwdDruidPasswordCallback` | ⭐⭐ |
| Spring Boot应用 | 模式2: Spring Boot Starter | `db-pwd-spring-boot-starter` | ⭐ |
| Spring Boot (编程方式) | 模式2变体: 编程API | `PasswordService` | ⭐⭐ |
| Spring (Java Config) | 模式3: Spring BeanPostProcessor | `DataSourcePasswordProcessor` | ⭐⭐ |
| Spring (XML Config) | 模式3: Spring BeanPostProcessor | `DataSourcePasswordProcessor` | ⭐⭐ |
| MyBatis | 模式4: 自定义集成 | `PasswordService` | ⭐⭐ |
| Servlet/Struts应用 | 模式6: Servlet ContextListener | `DatabasePasswordInitListener` | ⭐⭐ |

---

## 集成模式详解

### 模式1: Druid PasswordCallback

**适用场景**: 使用Druid连接池的应用

**集成原理**: Druid的PasswordCallback机制在获取连接时自动调用回调函数解密密码

**核心类**: `com.dbpwd.druid.DbPwdDruidPasswordCallback`

---

### 模式2: Spring Boot Starter

**适用场景**: Spring Boot应用

**集成原理**: 自动配置DataSource的BeanPostProcessor

**核心类**: `com.dbpwd.spring.DbPwdAutoConfiguration`

---

### 模式3: Spring BeanPostProcessor

**适用场景**: 传统Spring应用（Java Config或XML Config）

**集成原理**: BeanPostProcessor在DataSource初始化后自动解密密码

**核心类**: `com.dbpwd.spring.DataSourcePasswordProcessor`

---

### 模式4: MyBatis集成

**适用场景**: 纯MyBatis应用（无Spring依赖）

**集成原理**: 在构建SqlSessionFactory时，使用PasswordService API获取密码

**核心类**: `com.dbpwd.core.PasswordService`

---

### 模式5: Standalone (纯JDBC)

**适用场景**: 不使用任何框架的纯JDBC应用

**集成原理**: 直接调用PasswordService API

**核心类**: `com.dbpwd.core.PasswordService`

---

### 模式6: Servlet ContextListener

**适用场景**: Servlet/Struts等传统Web应用

**集成原理**: ServletContextListener在容器启动时初始化密码服务

**核心类**: `com.dbpwd.web.DatabasePasswordInitListener`

---

## 代码修改详解


---

## 一、模式5: 纯JDBC集成

### 1.1 适用场景

- 不使用任何框架的纯JDBC应用
- 需要完全控制密码获取过程

### 1.2 目录结构变化

```
无新增文件，仅修改:
├── pom.xml                          [修改]
└── src/main/java/.../XxxDemo.java   [修改]
```

### 1.3 POM文件修改

**添加依赖**:
```xml
<!-- 添加核心依赖 -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 1.4 Java代码修改

**原始代码**:
```java
public class SimpleJdbcDemo {
    // 明文密码常量
    private static final String DB_PASSWORD = "sa";

    public static void main(String[] args) {
        // 直接使用明文密码
        String plainPassword = DB_PASSWORD;
        Connection conn = DriverManager.getConnection(url, username, plainPassword);
    }
}
```

**集成后代码**:
```java
// 添加import
import com.dbpwd.core.PasswordService;

public class SimpleJdbcDemo {
    // 改为占位符常量
    private static final String DB_PLACEHOLDER = "simple_jdbc_db";

    public static void main(String[] args) {
        // 使用PasswordService获取解密后的密码
        String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
        Connection conn = DriverManager.getConnection(url, username, plainPassword);
    }
}
```

**关键改动点**:
1. 添加 `import com.dbpwd.core.PasswordService;`
2. 将密码常量从明文 `"sa"` 改为占位符 `"simple_jdbc_db"`
3. 使用 `PasswordService.getDatabasePassword()` 获取密码

---

## 二、模式1: Druid PasswordCallback集成

### 2.1 适用场景

- 使用Druid连接池的应用
- 希望密码解密过程自动化

### 2.2 目录结构变化

```
无新增文件，仅修改:
├── pom.xml                          [修改]
└── src/main/java/.../XxxApp.java    [修改]
```

### 2.3 POM文件修改

**添加依赖**:
```xml
<!-- 添加核心依赖 -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 添加Druid集成依赖 -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-druid</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2.4 Java代码修改

**原始代码**:
```java
public class DruidDemoApplication {
    public static void main(String[] args) {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:file:...");
        dataSource.setUsername("sa");
        
        // 直接设置明文密码
        String password = "sa";
        dataSource.setPassword(password);
        
        dataSource.setDriverClassName("org.h2.Driver");
    }
}
```

**集成后代码**:
```java
// 添加import
import com.dbpwd.druid.DbPwdDruidPasswordCallback;

public class DruidDemoApplication {
    public static void main(String[] args) {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:file:...");
        dataSource.setUsername("sa");
        
        // 设置占位符格式密码
        dataSource.setPassword("@druid_demo_db@");
        dataSource.setDriverClassName("org.h2.Driver");

        // 配置PasswordCallback - 核心集成步骤
        DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback();
        callback.setDataSource(dataSource);  // 关键：让callback能获取原始密码
        dataSource.setPasswordCallback(callback);
    }
}
```

**关键改动点**:
1. 添加 `import com.dbpwd.druid.DbPwdDruidPasswordCallback;`
2. 密码从明文 `"sa"` 改为占位符 `"@druid_demo_db@"`
3. 创建 `DbPwdDruidPasswordCallback` 并设置到DataSource
4. **重要**: 调用 `callback.setDataSource(dataSource)`

### 2.5 工作原理

1. DataSource的password设置为占位符 `@druid_demo_db@`
2. Druid在获取连接时自动调用 `callback.getPassword()`
3. Callback从DataSource获取原始密码(占位符)
4. Callback内部调用 `PasswordService.getDatabasePassword()` 解密
5. 返回明文密码给Druid

---

## 三、模式1+Spring: Druid + Spring XML集成

### 3.1 适用场景

- 使用Druid连接池 + Spring XML配置
- 希望通过配置文件管理DataSource

### 3.2 目录结构变化

```
修改文件:
├── pom.xml                              [修改]
├── src/main/resources/druid-datasource.xml  [修改]
└── src/main/java/.../XxxApplication.java    [修改]
```

### 3.3 POM文件修改

```xml
<!-- 添加核心和Druid集成依赖 -->
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

### 3.4 XML配置修改

**原始XML**:
```xml
<bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource">
    <property name="url" value="jdbc:h2:file:..."/>
    <property name="username" value="sa"/>
    <!-- 明文密码 -->
    <property name="password" value="sa"/>
</bean>
```

**集成后XML**:
```xml
<bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource">
    <property name="url" value="jdbc:h2:file:..."/>
    <property name="username" value="sa"/>
    <!-- 占位符格式密码 -->
    <property name="password" value="@druid_demo_spring_db@"/>
</bean>
```

### 3.5 Java代码修改

**原始代码**:
```java
public class DruidSpringDemoApplication {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = 
            new ClassPathXmlApplicationContext("druid-datasource.xml");
        DruidDataSource dataSource = context.getBean(DruidDataSource.class);
        // 直接使用，密码已在XML中配置为明文
    }
}
```

**集成后代码**:
```java
// 添加import
import com.dbpwd.druid.DbPwdDruidPasswordCallback;

public class DruidSpringDemoApplication {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = 
            new ClassPathXmlApplicationContext("druid-datasource.xml");
        DruidDataSource dataSource = context.getBean(DruidDataSource.class);
        
        // 配置PasswordCallback
        DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback();
        callback.setDataSource(dataSource);
        dataSource.setPasswordCallback(callback);
    }
}
```

**关键改动点**:
1. XML中password从 `"sa"` 改为 `"@druid_demo_spring_db@"`
2. Java代码中创建并设置PasswordCallback

---

## 四、模式2: Spring Boot Starter集成

### 4.1 适用场景

- Spring Boot应用
- 希望零配置自动集成

### 4.2 目录结构变化

```
修改文件:
├── pom.xml                              [修改]
└── src/main/resources/application.properties  [可选修改]
```

### 4.3 POM文件修改

**原始POM**:
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
</dependencies>
```

**集成后POM**:
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
    <!-- 添加Starter依赖 -->
    <dependency>
        <groupId>com.dbpwd</groupId>
        <artifactId>db-pwd-spring-boot-starter</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

### 4.4 配置文件修改

**原始application.properties**:
```properties
spring.datasource.url=jdbc:h2:file:...
spring.datasource.username=sa
spring.datasource.password=sa
```

**集成后application.properties**:
```properties
spring.datasource.url=jdbc:h2:file:...
spring.datasource.username=sa
# 使用占位符格式密码
spring.datasource.password=@spring_boot_db@
```

### 4.5 Java代码修改

**无需修改Java代码！**Starter会自动配置。

**可选**: 如果需要手动配置DataSource:

**原始代码**:
```java
@Bean
public DataSource dataSource() {
    DriverManagerDataSource ds = new DriverManagerDataSource();
    ds.setPassword("sa");  // 明文密码
    return ds;
}
```

**集成后代码**:
```java
// 添加import
import com.dbpwd.core.PasswordService;

@Bean
public DataSource dataSource() {
    DriverManagerDataSource ds = new DriverManagerDataSource();
    String password = PasswordService.getDatabasePassword("@spring_boot_db@");
    ds.setPassword(password);
    return ds;
}
```

---

## 五、模式3: Spring BeanPostProcessor集成

### 5.1 适用场景

- 传统Spring应用（非Boot）
- Java Config或XML Config

### 5.2 Java Config方式

#### 目录结构变化
```
修改文件:
├── pom.xml                              [修改]
└── src/main/java/.../DemoConfig.java    [修改]
```

#### POM文件修改
```xml
<!-- 添加核心和Spring集成依赖 -->
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

#### Java代码修改

**原始代码**:
```java
@Configuration
public class DemoConfig {
    @Bean
    public DataSource dataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:h2:file:...");
        ds.setUsername("sa");
        ds.setPassword("sa");  // 明文密码
        ds.setDriverClassName("org.h2.Driver");
        return ds;
    }
}
```

**集成后代码**:
```java
// 添加import
import com.dbpwd.core.PasswordService;

@Configuration
public class DemoConfig {

    private static final String DB_PLACEHOLDER = "@spring_framework_db@";

    @Bean
    public DataSource dataSource() {
        // 使用PasswordService获取密码
        String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);

        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:h2:file:...");
        ds.setUsername("sa");
        ds.setPassword(plainPassword);  // 解密后的密码
        ds.setDriverClassName("org.h2.Driver");
        return ds;
    }
}
```

**关键改动点**:
1. 添加 `import com.dbpwd.core.PasswordService;`
2. 定义占位符常量 `DB_PLACEHOLDER`
3. 使用 `PasswordService.getDatabasePassword()` 获取密码

### 5.3 XML Config方式

#### 目录结构变化
```
修改文件:
├── pom.xml                                  [修改]
└── src/main/resources/applicationContext.xml [修改]
```

#### POM文件修改
```xml
<!-- 添加核心和Spring集成依赖 -->
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

#### XML配置修改

**原始XML**:
```xml
<bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource">
    <property name="driverClassName" value="org.h2.Driver"/>
    <property name="url" value="jdbc:h2:file:..."/>
    <property name="username" value="sa"/>
    <!-- 明文密码 -->
    <property name="password" value="sa"/>
</bean>
```

**集成后XML**:
```xml
<!-- 添加DataSourcePasswordProcessor Bean - 核心步骤 -->
<bean class="com.dbpwd.spring.DataSourcePasswordProcessor"/>

<bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource">
    <property name="driverClassName" value="org.h2.Driver"/>
    <property name="url" value="jdbc:h2:file:..."/>
    <property name="username" value="sa"/>
    <!-- 占位符格式密码 - BeanPostProcessor会自动解密 -->
    <property name="password" value="@spring_demo_xml_db@"/>
</bean>
```

**关键改动点**:
1. 添加 `DataSourcePasswordProcessor` Bean声明（必须在DataSource之前）
2. 将password从 `"sa"` 改为 `"@spring_demo_xml_db@"`

### 5.4 工作原理

1. Spring容器启动，创建 `DataSourcePasswordProcessor` Bean
2. `DataSourcePasswordProcessor` 实现 `BeanPostProcessor` 接口
3. 每个DataSource Bean初始化后，Processor检查password属性
4. 如果password是占位符格式（以`@`开头和结尾），调用 `PasswordService` 解密
5. 将解密后的明文密码设置回DataSource

---

## 六、模式2变体: Spring Boot API集成

### 6.1 适用场景

- Spring Boot应用，需要完全控制DataSource配置
- 不想使用自动配置
- 需要在配置过程中执行额外逻辑

### 6.2 目录结构变化

```
修改文件:
├── pom.xml                                    [修改]
└── src/main/java/.../XxxApplication.java      [修改]
```

### 6.3 POM文件修改

**添加依赖**:
```xml
<!-- 仅添加核心依赖，不需要Starter -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 6.4 Java代码修改

**原始代码**:
```java
@SpringBootApplication
public class SpringBootApiDemoApplication {

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:file:...");
        dataSource.setUsername("sa");
        dataSource.setPassword("sa");  // 明文密码
        return dataSource;
    }
}
```

**集成后代码**:
```java
// 添加import
import com.dbpwd.core.PasswordService;

@SpringBootApplication
public class SpringBootApiDemoApplication {

    private static final String DB_PLACEHOLDER = "@spring_boot_api_db@";

    @Bean
    public DataSource dataSource() {
        // 使用PasswordService API解密密码
        String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:file:...");
        dataSource.setUsername("sa");
        dataSource.setPassword(plainPassword);
        return dataSource;
    }
}
```

**关键改动点**:
1. 添加 `import com.dbpwd.core.PasswordService;`
2. 定义占位符常量 `DB_PLACEHOLDER`
3. 使用 `PasswordService.getDatabasePassword()` 获取密码

### 6.5 与Starter方式的区别

| 特性 | Starter方式 | API方式 |
|-----|-----------|--------|
| 配置方式 | application.properties | Java代码 |
| 依赖模块 | db-pwd-spring-boot-starter | db-pwd-core |
| 自动配置 | 是 | 否 |
| 控制程度 | 低 | 高 |
| 适用场景 | 常规应用 | 复杂配置需求 |

---

## 七、模式4: MyBatis集成

### 7.1 适用场景

- 纯MyBatis应用（无Spring依赖）
- 需要手动构建SqlSessionFactory

### 7.2 目录结构变化

```
修改文件:
├── pom.xml                                    [修改]
└── src/main/java/.../MybatisDemoApplication.java  [修改]
```

### 7.3 POM文件修改

**添加依赖**:
```xml
<!-- 添加核心依赖 -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 7.4 Java代码修改

**原始代码**:
```java
public class MybatisDemoApplication {

    private static final String H2_PASSWORD = "sa";  // 明文密码常量

    public static void main(String[] args) throws Exception {
        SqlSessionFactory sqlSessionFactory = buildSqlSessionFactory(H2_PASSWORD);
        // ...
    }

    private static SqlSessionFactory buildSqlSessionFactory(String password) throws Exception {
        DataSource dataSource = new UnpooledDataSource();
        ((UnpooledDataSource) dataSource).setUrl("jdbc:h2:file:...");
        ((UnpooledDataSource) dataSource).setUsername("sa");
        ((UnpooledDataSource) dataSource).setPassword(password);
        ((UnpooledDataSource) dataSource).setDriver("org.h2.Driver");
        // ...
    }
}
```

**集成后代码**:
```java
// 添加import
import com.dbpwd.core.PasswordService;

public class MybatisDemoApplication {

    private static final String DB_PLACEHOLDER = "mybatis_demo_db";  // 占位符

    public static void main(String[] args) throws Exception {
        // 使用PasswordService获取密码
        String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
        SqlSessionFactory sqlSessionFactory = buildSqlSessionFactory(plainPassword);
        // ...
    }

    private static SqlSessionFactory buildSqlSessionFactory(String password) throws Exception {
        DataSource dataSource = new UnpooledDataSource();
        ((UnpooledDataSource) dataSource).setUrl("jdbc:h2:file:...");
        ((UnpooledDataSource) dataSource).setUsername("sa");
        ((UnpooledDataSource) dataSource).setPassword(password);
        ((UnpooledDataSource) dataSource).setDriver("org.h2.Driver");
        // ...
    }
}
```

**关键改动点**:
1. 添加 `import com.dbpwd.core.PasswordService;`
2. 将密码常量从 `"sa"` 改为占位符 `"mybatis_demo_db"`
3. 使用 `PasswordService.getDatabasePassword()` 获取密码

---

## 八、模式6: Servlet ContextListener集成

### 8.1 适用场景

- Servlet/Struts等传统Web应用
- 使用web.xml配置

### 8.2 目录结构变化

```
修改文件:
├── pom.xml                                    [修改]
├── src/main/webapp/WEB-INF/web.xml            [修改]
└── src/main/java/.../UserApiServlet.java      [修改]
```

### 8.3 POM文件修改

**添加依赖**:
```xml
<!-- 添加核心依赖 -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 添加Web集成依赖 -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-web</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 8.4 web.xml配置修改

**原始web.xml**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app version="4.0">
    <display-name>Servlet Demo</display-name>
    <!-- 无Listener配置 -->
</web-app>
```

**集成后web.xml**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app version="4.0">
    <display-name>Servlet Demo</display-name>

    <!-- 配置DatabasePasswordInitListener - 核心步骤 -->
    <listener>
        <listener-class>com.dbpwd.web.DatabasePasswordInitListener</listener-class>
    </listener>

    <!-- 可选：自定义配置路径 -->
    <!--
    <context-param>
        <param-name>dbpwd.keyFile</param-name>
        <param-value>/path/to/sm4.key</param-value>
    </context-param>
    <context-param>
        <param-name>dbpwd.pwdFile</param-name>
        <param-value>/path/to/db.pwd</param-value>
    </context-param>
    -->
</web-app>
```

### 8.5 Servlet代码修改

**原始代码**:
```java
@WebServlet("/api/users/*")
public class UserApiServlet extends HttpServlet {

    @Override
    public void init() throws ServletException {
        String plainPassword = "sa";  // 明文密码

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:file:...");
        dataSource.setUsername("sa");
        dataSource.setPassword(plainPassword);
        // ...
    }
}
```

**集成后代码**:
```java
// 添加import
import com.dbpwd.core.PasswordService;

@WebServlet("/api/users/*")
public class UserApiServlet extends HttpServlet {

    private static final String DB_PLACEHOLDER = "@servlet_demo_db@";

    @Override
    public void init() throws ServletException {
        // 使用PasswordService获取密码
        String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:file:...");
        dataSource.setUsername("sa");
        dataSource.setPassword(plainPassword);
        // ...
    }
}
```

**关键改动点**:
1. web.xml添加 `DatabasePasswordInitListener`
2. 添加 `import com.dbpwd.core.PasswordService;`
3. 使用 `PasswordService.getDatabasePassword()` 获取密码

---

## 九、Struts 1.x集成

### 9.1 适用场景

- Struts 1.x传统Web应用
- 维护旧系统参考

### 9.2 目录结构变化

```
修改文件:
├── pom.xml                                    [修改]
└── src/main/java/.../UserListAction.java      [修改]
```

### 9.3 POM文件修改

**添加依赖**:
```xml
<!-- 添加核心依赖 -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 9.4 Action代码修改

**原始代码**:
```java
public class UserListAction extends Action {

    private static final String H2_PASSWORD = "sa";  // 明文密码常量

    @Override
    public ActionForward execute(...) throws Exception {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:file:...");
        dataSource.setUsername("sa");
        dataSource.setPassword(H2_PASSWORD);
        // ...
    }
}
```

**集成后代码**:
```java
// 添加import
import com.dbpwd.core.PasswordService;

public class UserListAction extends Action {

    private static final String DB_PLACEHOLDER = "@struts_demo_db@";  // 占位符

    @Override
    public ActionForward execute(...) throws Exception {
        // 使用PasswordService获取密码
        String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:file:...");
        dataSource.setUsername("sa");
        dataSource.setPassword(plainPassword);
        // ...
    }
}
```

**关键改动点**:
1. 添加 `import com.dbpwd.core.PasswordService;`
2. 将密码常量从 `"sa"` 改为占位符 `"@struts_demo_db@"`
3. 使用 `PasswordService.getDatabasePassword()` 获取密码

---

## 十、依赖汇总表

| 集成模式 | 核心依赖 | 集成依赖 | 总依赖数 |
|---------|---------|---------|---------|
| 模式5: 纯JDBC | db-pwd-core | - | 1 |
| 模式1: Druid | db-pwd-core | db-pwd-druid | 2 |
| 模式2: Spring Boot Starter | db-pwd-spring-boot-starter | - | 1 |
| 模式2变体: Spring Boot API | db-pwd-core | - | 1 |
| 模式3: Spring | db-pwd-core | db-pwd-spring | 2 |
| 模式4: MyBatis | db-pwd-core | - | 1 |
| 模式6: Servlet | db-pwd-core | db-pwd-web | 2 |
| Struts | db-pwd-core | - | 1 |

---

## 十一、常见问题

### Q1: 占位符格式是什么？

**格式**: `@db_name@`

- 必须以 `@` 开头和结尾
- `db_name` 对应 `~/.dbpwd/db.pwd` 文件中的数据库名称
- 示例: `@spring_boot_db@`, `@mybatis_demo_db@`

### Q2: 密码文件在哪里？

- 默认路径: `~/.dbpwd/db.pwd` (用户主目录下的隐藏目录)
- 密钥文件: `~/.dbpwd/sm4.key`
- 应用无需知道文件位置，由 `PasswordService` 内部处理

### Q3: 如何选择集成模式？

| 你的应用类型 | 推荐模式 | 理由 |
|------------|---------|-----|
| Spring Boot新项目 | 模式2: Starter | 零配置，最简单 |
| Spring Boot + 复杂配置 | 模式2变体: API | 完全控制 |
| 传统Spring应用 | 模式3: BeanPostProcessor | 自动解密 |
| Druid连接池 | 模式1: PasswordCallback | 原生支持 |
| MyBatis纯应用 | 模式4: API | 直接调用 |
| Servlet/Struts | 模式6: ContextListener | 容器初始化 |
| 纯JDBC/工具类 | 模式5: Standalone | 最灵活 |

### Q4: 自动解密 vs 手动解密有什么区别？

| 方式 | 自动解密 | 手动解密 |
|-----|---------|---------|
| 配置位置 | 配置文件中使用占位符 | 代码中调用API |
| 代码改动 | 小 | 中等 |
| 适用模式 | 模式1/2/3/6 | 模式4/5 |
| 示例 | `password=@db@` | `PasswordService.getDatabasePassword("@db@")` |

### Q5: 如何生成加密密码？

使用CLI工具或API:

```bash
# CLI方式
java -jar db-pwd-cli.jar encrypt --password "yourPassword"
```

```java
// API方式
import com.dbpwd.core.PasswordService;

// 解密
String plain = PasswordService.decrypt("ENC(SM4-GCM:xxx)");
```

### Q6: 文件权限要求？

| 文件 | 权限 | 说明 |
|-----|-----|-----|
| `~/.dbpwd/` | 700 | 目录权限 |
| `~/.dbpwd/sm4.key` | 600 | 密钥文件 |
| `~/.dbpwd/db.pwd` | 600 | 密码文件 |

Windows环境无需设置，Linux/Mac环境建议设置:
```bash
chmod 700 ~/.dbpwd
chmod 600 ~/.dbpwd/sm4.key ~/.dbpwd/db.pwd
```

---

## 附录: 完整集成检查清单

### 集成前检查

- [ ] 确认应用类型（Spring Boot/Spring/Servlet等）
- [ ] 选择正确的集成模式
- [ ] 准备好 `~/.dbpwd/sm4.key` 和 `~/.dbpwd/db.pwd` 文件

### 集成步骤

- [ ] 添加Maven依赖
- [ ] 修改配置文件（XML/properties）
- [ ] 修改Java代码
- [ ] 测试连接

### 集成后验证

- [ ] 应用启动无报错
- [ ] 数据库连接成功
- [ ] 业务功能正常
- [ ] 配置文件中无明文密码

---

> 文档版本: 2.0
> 最后更新: 2026-02-20