# Spring Framework 集成 Demo

> 集成模块: db-pwd-spring (方案3: Spring BeanPostProcessor集成)
> 版本: 5.3
> 更新时间: 2026-02-12

## 概述

本Demo演示如何在传统Spring应用（非Spring Boot）中使用 `db-pwd-spring` 模块，通过 `BeanPostProcessor` 机制自动解密数据库密码。

## 功能演示

- **自动密码解密**: DataSourcePasswordProcessor 自动拦截并解密 ENC(SM4-GCM:xxx) 格式密码
- **Druid连接池**: 使用 Druid 连接池管理数据库连接
- **H2内存数据库**: 使用 H2 内存数据库，无需外部数据库
- **完整CRUD操作**: 演示创建、查询、更新、删除用户的完整流程

## 项目结构

```
db-pwd-spring-demo/
├── src/main/java/com/dbpwd/demo/spring/
│   ├── SpringDemoApplication.java      # 主程序入口
│   ├── DemoConfig.java                 # Spring配置类
│   ├── model/
│   │   └── User.java                   # 用户实体类
│   └── dao/
│       └── UserDao.java                # 数据访问对象
└── pom.xml                             # Maven配置
```

## 运行Demo

### 1. 编译项目

```bash
# 安装核心模块和Spring集成模块
mvn install -pl db-pwd-core,db-pwd-spring -am -DskipTests

# 编译Demo
mvn compile -pl db-pwd-spring-demo
```

### 2. 运行Demo

```bash
mvn exec:java -Dexec.mainClass="com.dbpwd.demo.spring.SpringDemoApplication" -pl db-pwd-spring-demo
```

或打包后运行：

```bash
mvn package -pl db-pwd-spring-demo
java -jar db-pwd-spring-demo/target/db-pwd-spring-demo-1.0.0.jar
```

## 输出示例

```
========================================
   Database Password Manager - Spring Demo
========================================

Starting Spring container...
DataSourcePasswordProcessor configured with key file: C:\Users\faunt\.dbpwd/demo_sm4.key
Druid dataSource configured with URL: jdbc:h2:mem:dbpwd_spring_demo;DB_CLOSE_DELAY=-1
Processing DataSource [dataSource] with password
Successfully decrypted password for DataSource [dataSource]

========================================
   CRUD Operations Demo
========================================

1. Creating users...
   Created user: john_doe
   Created user: jane_smith

2. Querying all users...
   Found 2 user(s):
   - ID: 1, Username: john_doe, Email: john@example.com
   - ID: 2, Username: jane_smith, Email: jane@example.com

3. Querying user by ID: 1...
   Found user: john_doe (john@example.com)

4. Updating user ID: 1...
   Updated 1 row(s).

5. Deleting user ID: 2...
   Deleted 1 row(s).

6. Querying remaining users...
   Found 1 user(s):
   - ID: 1, Username: john_doe, Email: john.updated@example.com

========================================
   Demo completed successfully!
========================================

Key points demonstrated:
- DataSourcePasswordProcessor automatically decrypts password
- SM4-GCM encryption for secure password storage
- Encrypted password format: ENC(SM4-GCM:xxx)
- No code changes needed in business logic
```

## 核心代码说明

### 1. Spring配置类 (DemoConfig.java)

```java
@Configuration
public class DemoConfig {

    @Bean
    public DataSourcePasswordProcessor dataSourcePasswordProcessor() {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();
        processor.setKeyFilePath(keyFilePath);
        return processor;
    }

    @Bean
    public DataSource dataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:mem:dbpwd_spring_demo");
        dataSource.setUsername("sa");
        // ENC格式密码 - DataSourcePasswordProcessor会自动解密
        dataSource.setPassword("ENC(SM4-GCM:..." + encryptedPassword + ")");
        return dataSource;
    }
}
```

### 2. 密码解密流程

1. Spring 容器创建 DataSource Bean
2. `DataSourcePasswordProcessor` 拦截 Bean 初始化
3. 读取 ENC(SM4-GCM:xxx) 格式密码
4. 使用 `sm4.key` 文件中的密钥解密
5. 将明文密码设置回 DataSource
6. DataSource 正常建立数据库连接

### 3. UserDao使用示例

```java
@Repository
public class UserDao {

    private final JdbcTemplate jdbcTemplate;

    public Long insert(User user) {
        String sql = "INSERT INTO users (username, email, password) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, user.getUsername(), user.getEmail(), user.getPassword());
        // ...
    }

    public List<User> findAll() {
        String sql = "SELECT * FROM users";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class));
    }
}
```

## 生成加密密码

### 使用命令行工具

```bash
# 加密明文密码
java -jar db-pwd-cli.jar encrypt --password "yourPassword"

# 输出示例:
# ENC(SM4-GCM:abc123def456...)
```

### 使用代码生成

```java
import com.dbpwd.crypto.Sm4GCMCipher;
import com.dbpwd.core.Sm4KeyLoader;

// 从文件加载密钥
String sm4Key = Sm4KeyLoader.loadKeyFromFile("~/.dbpwd/sm4.key");

// 加密密码
String plainPassword = "yourPassword";
String encryptedPassword = Sm4GCMCipher.encrypt(plainPassword, sm4Key);
String encPassword = "ENC(SM4-GCM:" + encryptedPassword + ")";
```

## 技术栈

| 组件 | 版本 | 说明 |
|-----|------|------|
| JDK | 8+ | Java开发环境 |
| Spring Framework | 5.3.31 | Spring框架（非Spring Boot） |
| Druid | 1.2.20 | 数据库连接池 |
| H2 | 2.1.214 | 内存数据库（Demo用） |
| db-pwd-core | 1.0.0 | 核心加解密模块 |
| db-pwd-spring | 1.0.0 | Spring集成模块 |

## 配置说明

### 1. 密钥文件路径

- 默认路径: `~/.dbpwd/sm4.key`
- 自定义路径: 通过 `setKeyFilePath()` 设置

### 2. 密码文件路径

- 默认路径: `~/.dbpwd/db.pwd`
- 自定义路径: 通过 `setPwdFilePath()` 设置
- 如果使用ENC格式密码，密码文件可选

### 3. 排除特定DataSource

```java
DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();
processor.addExcludedBeanName("excludedDataSource");
```

## 注意事项

1. **演示用密钥**: Demo自动生成测试密钥文件，生产环境应通过命令行工具生成安全密钥
2. **H2数据库**: H2 内存数据库仅用于演示，生产环境应使用实际数据库
3. **ENC格式**: 新应用必须使用 `ENC(SM4-GCM:xxx)` 格式以获取完整性保护
4. **密钥文件权限**: 生产环境确保密钥文件权限为 600

## 与其他集成模块的对比

| 特性 | Spring Demo | Spring Boot Demo | Druid Demo | Simple JDBC Demo |
|-----|------------|------------------|------------|------------------|
| 集成方式 | BeanPostProcessor | Auto Configuration | PasswordCallback | API直接调用 |
| 适用场景 | 传统Spring应用 | Spring Boot应用 | Druid连接池应用 | 纯JDBC应用/工具类 |
| 配置方式 | Java/XML配置 | application.yml | API调用 | 代码配置 |
| 框架依赖 | Spring Framework | Spring Boot | Druid | 无 |

## 相关文档

- [Spring集成模块 README](../db-pwd-spring/README.md)
- [核心模块 README](../db-pwd-core/README.md)
- [集成场景和方案](../technical/integration-scenarios.md)
- [Druid集成示例](../db-pwd-druid/README.md)

## 许可证

MIT License
