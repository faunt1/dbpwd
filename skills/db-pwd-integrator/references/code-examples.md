# 代码修改对比示例

本文档提供各集成模式的代码修改前后对比，供 skill 执行时参考。

---

## 方案2: Spring Boot Starter

### 配置文件修改

**application.yml**
```yaml
# ========== BEFORE ==========
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: sa
    driver-class-name: com.mysql.cj.jdbc.Driver

# ========== AFTER ==========
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: "@my_db@"  # 改为占位符
    driver-class-name: com.mysql.cj.jdbc.Driver
```

**application.properties**
```properties
# ========== BEFORE ==========
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=sa

# ========== AFTER ==========
spring.datasource.url=jdbc:mysql://localhost:3306/mydb
spring.datasource.username=root
spring.datasource.password=@my_db@
```

### pom.xml 修改

```xml
<!-- ========== 添加到 <dependencies> 中 ========== -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 方案3: Spring BeanPostProcessor

### Java Config 修改

```java
// ========== BEFORE ==========
package com.example.config;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {
    
    @Bean
    public DataSource dataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:mysql://localhost:3306/mydb");
        ds.setUsername("root");
        ds.setPassword("sa");  // 明文密码
        return ds;
    }
}

// ========== AFTER ==========
package com.example.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.spring.DataSourcePasswordProcessor;  // 新增导入
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {
    
    // 新增 Bean
    @Bean
    public DataSourcePasswordProcessor dataSourcePasswordProcessor() {
        return new DataSourcePasswordProcessor();
    }
    
    @Bean
    public DataSource dataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:mysql://localhost:3306/mydb");
        ds.setUsername("root");
        ds.setPassword("@my_db@");  // 改为占位符
        return ds;
    }
}
```

### XML Config 修改

```xml
<!-- ========== BEFORE ========== -->
<bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource">
    <property name="url" value="jdbc:mysql://localhost:3306/mydb"/>
    <property name="username" value="root"/>
    <property name="password" value="sa"/>
</bean>

<!-- ========== AFTER ========== -->
<!-- 新增 BeanPostProcessor -->
<bean class="com.dbpwd.spring.DataSourcePasswordProcessor"/>

<bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource">
    <property name="url" value="jdbc:mysql://localhost:3306/mydb"/>
    <property name="username" value="root"/>
    <property name="password" value="@my_db@"/>
</bean>
```

---

## 方案1: Druid PasswordCallback

### 直接使用 PasswordService

```java
// ========== BEFORE ==========
package com.example;

import com.alibaba.druid.pool.DruidDataSource;
import javax.sql.DataSource;

public class DatabaseConfig {
    
    private static final String DB_PASSWORD = "sa";
    
    public DataSource createDataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:mysql://localhost:3306/mydb");
        ds.setUsername("root");
        ds.setPassword(DB_PASSWORD);
        return ds;
    }
}

// ========== AFTER ==========
package com.example;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.core.PasswordService;  // 新增导入
import javax.sql.DataSource;

public class DatabaseConfig {
    
    private static final String DB_PLACEHOLDER = "@my_db@";  // 改为占位符
    
    public DataSource createDataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:mysql://localhost:3306/mydb");
        ds.setUsername("root");
        ds.setPassword(PasswordService.getDatabasePassword(DB_PLACEHOLDER));  // 调用 API
        return ds;
    }
}
```

---

## 方案4: MyBatis DataSourceFactory

### mybatis-config.xml 修改

```xml
<!-- ========== BEFORE ========== -->
<configuration>
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
                <property name="url" value="jdbc:mysql://localhost:3306/mydb"/>
                <property name="username" value="root"/>
                <property name="password" value="sa"/>
            </dataSource>
        </environment>
    </environments>
</configuration>

<!-- ========== AFTER ========== -->
<configuration>
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <!-- 改变 type 为自定义 Factory -->
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

---

## 方案5: StandalonePasswordDecryptor (纯 JDBC)

### 简单 JDBC 应用

```java
// ========== BEFORE ==========
package com.example;

import java.sql.Connection;
import java.sql.DriverManager;

public class SimpleJdbcApp {
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "sa";
    
    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
        // 使用连接...
        conn.close();
    }
}

// ========== AFTER ==========
package com.example;

import com.dbpwd.core.PasswordService;  // 新增导入
import java.sql.Connection;
import java.sql.DriverManager;

public class SimpleJdbcApp {
    
    private static final String DB_URL = "jdbc:mysql://localhost:3306/mydb";
    private static final String DB_USER = "root";
    private static final String DB_PLACEHOLDER = "@my_db@";  // 改为占位符
    
    public static void main(String[] args) throws Exception {
        // 调用 API 获取密码
        String password = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
        Connection conn = DriverManager.getConnection(DB_URL, DB_USER, password);
        // 使用连接...
        conn.close();
    }
}
```

---

## 方案6: Servlet InitListener

### web.xml 方式

```xml
<!-- ========== BEFORE ========== -->
<?xml version="1.0" encoding="UTF-8"?>
<web-app>
    <!-- 其他配置 -->
</web-app>

<!-- ========== AFTER ========== -->
<?xml version="1.0" encoding="UTF-8"?>
<web-app>
    <!-- 新增监听器 -->
    <listener>
        <listener-class>com.dbpwd.web.DatabasePasswordInitListener</listener-class>
    </listener>
    
    <!-- 其他配置 -->
</web-app>
```

### 注解方式

```java
// ========== BEFORE ==========
package com.example.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import com.alibaba.druid.pool.DruidDataSource;

@WebListener
public class AppInitializer implements ServletContextListener {
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:mysql://localhost:3306/mydb");
        ds.setUsername("root");
        ds.setPassword("sa");  // 明文密码
        sce.getServletContext().setAttribute("dataSource", ds);
    }
}

// ========== AFTER ==========
package com.example.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.core.PasswordService;  // 新增导入

@WebListener
public class AppInitializer implements ServletContextListener {
    
    private static final String DB_PLACEHOLDER = "@my_db@";  // 占位符
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        String password = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
        
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:mysql://localhost:3306/mydb");
        ds.setUsername("root");
        ds.setPassword(password);  // 使用解密后的密码
        sce.getServletContext().setAttribute("dataSource", ds);
    }
}
```

---

## .gitignore 修改

```gitignore
# ========== 在文件末尾添加 ==========
# db-pwd 密码管理
.dbpwd/
sm4.key
db.pwd
```

---

## 多数据源处理

```java
// ========== BEFORE ==========
@Configuration
public class DatabaseConfig {
    
    @Bean
    @Primary
    public DataSource primaryDataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:mysql://localhost:3306/db1");
        ds.setPassword("password1");
        return ds;
    }
    
    @Bean
    public DataSource secondaryDataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:mysql://localhost:3306/db2");
        ds.setPassword("password2");
        return ds;
    }
}

// ========== AFTER ==========
@Configuration
public class DatabaseConfig {
    
    @Bean
    public DataSourcePasswordProcessor dataSourcePasswordProcessor() {
        return new DataSourcePasswordProcessor();
    }
    
    @Bean
    @Primary
    public DataSource primaryDataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:mysql://localhost:3306/db1");
        ds.setPassword("@primary_db@");  // 第一个数据库
        return ds;
    }
    
    @Bean
    public DataSource secondaryDataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:mysql://localhost:3306/db2");
        ds.setPassword("@secondary_db@");  // 第二个数据库
        return ds;
    }
}
```

---

## 环境变量密码处理

```java
// ========== BEFORE ==========
@Value("${DB_PASSWORD}")
private String password;

// ========== AFTER (方案 A: 保留环境变量) ==========
// 环境变量值改为占位符: DB_PASSWORD=@my_db@
@Value("${DB_PASSWORD}")
private String placeholder;

public void connect() {
    String password = PasswordService.getDatabasePassword(placeholder);
    // 使用 password
}

// ========== AFTER (方案 B: 直接使用) ==========
// 删除 @Value 注入，直接使用 PasswordService
public void connect() {
    String password = PasswordService.getDatabasePassword("@my_db@");
    // 使用 password
}
```
