# 数据库密码加密集成代码细节说明

> 本文档详细说明如何在现有应用中集成数据库密码加密功能
>
> 最后更新: 2026-02-15

## 目录

1. [概述](#概述)
2. [项目结构对比](#项目结构对比)
3. [集成模式详解](#集成模式详解)
4. [集成检查清单](#集成检查清单)
5. [常见问题](#常见问题)

---

## 概述

本文档通过对比 **原始版本 (xxx-original)** 和 **集成版本 (xxx-demo)** 的代码差异，清晰展示集成数据库密码加密功能所需的具体改动。

### 核心设计原则

- **最小改动**: 原始版本和集成版本的类名、包名、方法签名保持一致
- **改动集中**: 密码相关的改动集中在 1-3 行代码
- **无侵入**: 业务逻辑代码完全不需要修改

### 核心改动点

所有集成模式的核心改动都围绕以下三点：

1. **添加依赖** - 引入 `db-pwd-core` 或对应的集成模块
2. **替换明文密码** - 使用 `PasswordService` API 或占位符格式
3. **配置密钥文件** - 确保 `~/.dbpwd/` 目录下的密钥和密码文件存在

---

## 项目结构对比

| 集成模式 | 原始版本 | 集成版本 |
|---------|---------|---------|
| 方案5: 纯JDBC | `db-pwd-simple-jdbc-demo-original` | `db-pwd-simple-jdbc-demo` |
| 方案2: Spring Boot | `db-pwd-spring-boot-demo-original` | `db-pwd-spring-boot-demo` |
| 方案1: Druid | `db-pwd-druid-demo-original` | `db-pwd-druid-demo` |
| 方案3: Spring | `db-pwd-spring-demo-original` | `db-pwd-spring-demo` |
| 方案6: Servlet | `db-pwd-servlet-demo-original` | `db-pwd-servlet-demo` |

### 类名和包名保持一致

原始版本和集成版本使用完全相同的类名和包名，便于对比：

```
# 原始版本和集成版本的文件路径完全一致
db-pwd-simple-jdbc-demo-original/src/main/java/com/dbpwd/demo/SimpleJdbcDemo.java
db-pwd-simple-jdbc-demo/src/main/java/com/dbpwd/demo/SimpleJdbcDemo.java
```

---

## 集成模式详解

### 方案5: 纯 JDBC (StandalonePasswordDecryptor)

**适用场景**: 无框架的独立 Java 应用、命令行工具

#### 代码对比

**原始版本** (`SimpleJdbcDemo.java`):
```java
package com.dbpwd.demo;

import java.sql.Connection;
import java.sql.DriverManager;
// ... 其他导入

public class SimpleJdbcDemo {

    // 【原始版本】明文密码
    private static final String DB_PASSWORD = "sa";

    public static void main(String[] args) {
        try {
            // 【原始版本】直接使用明文密码
            String plainPassword = DB_PASSWORD;
            Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, plainPassword);
            // ...
        }
    }
}
```

**集成版本** (`SimpleJdbcDemo.java`):
```java
package com.dbpwd.demo;

import com.dbpwd.core.PasswordService;  // 【新增导入】
import java.sql.Connection;
import java.sql.DriverManager;
// ... 其他导入

public class SimpleJdbcDemo {

    // 【集成版本】使用占位符
    private static final String DB_PLACEHOLDER = "simple_jdbc_db";

    public static void main(String[] args) {
        try {
            // 【集成版本】调用 PasswordService 获取解密密码
            String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
            Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, plainPassword);
            // ...
        }
    }
}
```

#### 改动点总结

| 改动位置 | 原始代码 | 集成后代码 |
|---------|---------|-----------|
| 导入 | 无 | `import com.dbpwd.core.PasswordService;` |
| 常量 | `String DB_PASSWORD = "sa"` | `String DB_PLACEHOLDER = "simple_jdbc_db"` |
| 获取密码 | `plainPassword = DB_PASSWORD` | `plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER)` |

**集成改动量**: **3 处改动，约 3 行代码**

---

### 方案2: Spring Boot

**适用场景**: Spring Boot 应用

#### 代码对比

**原始版本** (`SpringBootDemo.java`):
```java
@SpringBootApplication
public class SpringBootDemo {

    // 【原始版本】明文密码
    private static final String DB_PASSWORD = "sa";

    @Bean
    public DataSource dataSource() throws Exception {
        // 【原始版本】直接使用明文密码
        String plainPassword = DB_PASSWORD;

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(DB_URL);
        dataSource.setUsername(DB_USERNAME);
        dataSource.setPassword(plainPassword);
        return dataSource;
    }
}
```

**集成版本** (`SpringBootDemo.java`):
```java
import com.dbpwd.core.PasswordService;  // 【新增导入】

@SpringBootApplication
public class SpringBootDemo {

    // 【集成版本】使用占位符
    private static final String DB_PLACEHOLDER = "spring_boot_db";

    @Bean
    public DataSource dataSource() throws Exception {
        // 【集成版本】调用 PasswordService 获取解密密码
        String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setUrl(DB_URL);
        dataSource.setUsername(DB_USERNAME);
        dataSource.setPassword(plainPassword);
        return dataSource;
    }
}
```

#### 改动点总结

| 改动位置 | 原始代码 | 集成后代码 |
|---------|---------|-----------|
| 导入 | 无 | `import com.dbpwd.core.PasswordService;` |
| 常量 | `String DB_PASSWORD = "sa"` | `String DB_PLACEHOLDER = "spring_boot_db"` |
| DataSource Bean | `plainPassword = DB_PASSWORD` | `plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER)` |

**集成改动量**: **3 处改动，约 3 行代码**

---

### 方案1: Druid

**适用场景**: 使用 Druid 连接池的应用

#### 代码对比

**原始版本** (`DruidDemoApplication.java`):
```java
public class DruidDemoApplication {

    private static DruidDataSource createDataSource() throws Exception {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(DB_URL);
        dataSource.setUsername("sa");

        // 【原始版本】直接使用明文密码
        String password = "sa";
        dataSource.setPassword(password);
        return dataSource;
    }
}
```

**集成版本** (`DruidDemoApplication.java`):
```java
import com.dbpwd.core.PasswordService;  // 【新增导入】

public class DruidDemoApplication {

    private static DruidDataSource createDataSource() throws Exception {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(DB_URL);
        dataSource.setUsername("sa");

        // 【集成版本】使用 PasswordService 获取解密密码
        String password = PasswordService.getDatabasePassword("@druid_demo_db@");
        dataSource.setPassword(password);
        return dataSource;
    }
}
```

#### 改动点总结

| 改动位置 | 原始代码 | 集成后代码 |
|---------|---------|-----------|
| 导入 | 无 | `import com.dbpwd.core.PasswordService;` |
| 密码获取 | `String password = "sa"` | `String password = PasswordService.getDatabasePassword("@druid_demo_db@")` |

**集成改动量**: **2 处改动，约 2 行代码**

---

### 方案3: Spring BeanPostProcessor

**适用场景**: 传统 Spring 应用（非 Boot）

#### 代码对比

**原始版本** (`DemoConfig.java`):
```java
@Configuration
public class DemoConfig {

    @Bean
    public DataSource dataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(DB_URL);
        dataSource.setUsername("sa");
        // 【原始版本】直接使用明文密码
        dataSource.setPassword("sa");
        return dataSource;
    }
}
```

**集成版本** (`DemoConfig.java`):
```java
import com.dbpwd.spring.DataSourcePasswordProcessor;  // 【新增导入】

@Configuration
public class DemoConfig {

    // 【集成版本】新增 Bean - DataSourcePasswordProcessor
    @Bean
    public DataSourcePasswordProcessor dataSourcePasswordProcessor() {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();
        processor.setKeyFilePath("~/.dbpwd/sm4.key");
        return processor;
    }

    @Bean
    public DataSource dataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(DB_URL);
        dataSource.setUsername("sa");
        // 【集成版本】使用 ENC 格式的加密密码
        dataSource.setPassword("ENC(SM4-GCM:...)");
        return dataSource;
    }
}
```

#### 改动点总结

| 改动位置 | 原始代码 | 集成后代码 |
|---------|---------|-----------|
| 导入 | 无 | `import com.dbpwd.spring.DataSourcePasswordProcessor;` |
| 新增 Bean | 无 | `dataSourcePasswordProcessor()` 方法 |
| 密码格式 | `"sa"` | `"ENC(SM4-GCM:...)"` |

**集成改动量**: **3 处改动，约 10 行代码**

---

### 方案6: Servlet

**适用场景**: 传统 Servlet/Web 应用

#### 代码对比

**原始版本** (`UserApiServlet.java`):
```java
@WebServlet("/api/users/*")
public class UserApiServlet extends HttpServlet {

    @Override
    public void init() throws ServletException {
        try {
            // 【原始版本】直接使用明文密码
            String plainPassword = "sa";

            dataSource = new DruidDataSource();
            dataSource.setUrl(H2_URL);
            dataSource.setUsername(H2_USERNAME);
            dataSource.setPassword(plainPassword);
            // ...
        }
    }
}
```

**集成版本** (`UserApiServlet.java`):
```java
import com.dbpwd.core.PasswordService;  // 【新增导入】

@WebServlet("/api/users/*")
public class UserApiServlet extends HttpServlet {

    @Override
    public void init() throws ServletException {
        try {
            // 【集成版本】使用 PasswordService 获取解密密码
            String plainPassword = PasswordService.getDatabasePassword("@servlet_demo_db@");

            dataSource = new DruidDataSource();
            dataSource.setUrl(H2_URL);
            dataSource.setUsername(H2_USERNAME);
            dataSource.setPassword(plainPassword);
            // ...
        }
    }
}
```

#### 改动点总结

| 改动位置 | 原始代码 | 集成后代码 |
|---------|---------|-----------|
| 导入 | 无 | `import com.dbpwd.core.PasswordService;` |
| 密码获取 | `String plainPassword = "sa"` | `String plainPassword = PasswordService.getDatabasePassword("@servlet_demo_db@")` |

**集成改动量**: **2 处改动，约 2 行代码**

---

## 集成检查清单

### 必须完成

- [ ] **添加 Maven 依赖**
  ```xml
  <!-- 通用场景 -->
  <dependency>
      <groupId>com.dbpwd</groupId>
      <artifactId>db-pwd-core</artifactId>
      <version>${project.version}</version>
  </dependency>

  <!-- Spring 场景 -->
  <dependency>
      <groupId>com.dbpwd</groupId>
      <artifactId>db-pwd-spring</artifactId>
      <version>${project.version}</version>
  </dependency>
  ```

- [ ] **创建密钥文件**
  ```bash
  java -jar db-pwd-demo-init/target/db-pwd-demo-init-1.0.0.jar
  ```

- [ ] **修改代码** - 参考上述各模式的改动点

### 建议完成

- [ ] **设置文件权限** (Linux/Mac)
  ```bash
  chmod 700 ~/.dbpwd
  chmod 600 ~/.dbpwd/sm4.key
  chmod 600 ~/.dbpwd/db.pwd
  ```

- [ ] **更新 .gitignore**
  ```
  .dbpwd/
  sm4.key
  db.pwd
  ```

---

## 常见问题

### Q1: 如何验证集成是否成功？

运行应用并检查日志，确认没有 "PLAINTEXT password" 警告。

### Q2: 密码文件放在哪里？

- 密码文件: `~/.dbpwd/db.pwd`
- 密钥文件: `~/.dbpwd/sm4.key`

### Q3: 如何为新数据库添加加密密码？

```bash
java -jar db-pwd-core/target/db-pwd-core-1.0.0.jar add mydb
```

---

## 附录: 快速对比脚本

```bash
# 对比 Simple JDBC Demo
diff db-pwd-simple-jdbc-demo-original/src/main/java/com/dbpwd/demo/SimpleJdbcDemo.java \
     db-pwd-simple-jdbc-demo/src/main/java/com/dbpwd/demo/SimpleJdbcDemo.java

# 对比 Spring Boot Demo
diff db-pwd-spring-boot-demo-original/src/main/java/com/dbpwd/demo/SpringBootDemo.java \
     db-pwd-spring-boot-demo/src/main/java/com/dbpwd/demo/SpringBootDemo.java

# 对比 Druid Demo
diff db-pwd-druid-demo-original/src/main/java/com/dbpwd/demo/druid/DruidDemoApplication.java \
     db-pwd-druid-demo/src/main/java/com/dbpwd/demo/druid/DruidDemoApplication.java

# 对比 Spring Demo (配置类)
diff db-pwd-spring-demo-original/src/main/java/com/dbpwd/demo/spring/DemoConfig.java \
     db-pwd-spring-demo/src/main/java/com/dbpwd/demo/spring/DemoConfig.java
```

---

## 集成改动量总结

| 集成模式 | 改动文件 | 改动行数 | 改动复杂度 |
|---------|---------|---------|-----------|
| 方案5: 纯JDBC | 1 个文件 | ~3 行 | 简单 |
| 方案2: Spring Boot | 1 个文件 | ~3 行 | 简单 |
| 方案1: Druid | 1 个文件 | ~2 行 | 简单 |
| 方案3: Spring | 1 个文件 | ~10 行 | 中等 |
| 方案6: Servlet | 1 个文件 | ~2 行 | 简单 |

**结论**: 所有集成模式的改动量都很小，核心改动集中在密码获取方式，业务逻辑代码无需修改。