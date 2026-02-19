# MyBatis DataSourceFactory 集成 Demo

> 集成模块: db-pwd-mybatis (方案4: MyBatis DataSourceFactory集成)
> 版本: 5.3
> 更新时间: 2026-02-12

## 概述

本Demo演示如何在MyBatis应用中使用 `db-pwd-mybatis` 模块，通过 `DbPwdDataSourceFactory` 自动解密数据库密码。支持纯MyBatis应用（无Spring依赖）和MyBatis + Spring应用。

## 功能演示

- **自动密码解密**: DbPwdDataSourceFactory 自动解密 ENC(SM4-GCM:xxx) 格式密码
- **MyBatis Mapper**: 使用 MyBatis Mapper接口进行数据库操作
- **H2内存数据库**: 使用 H2 内存数据库，无需外部数据库
- **完整CRUD操作**: 演示产品创建、查询、更新、删除的完整流程
- **注解式Mapper**: 使用 MyBatis 注解简化配置

## 项目结构

```
db-pwd-mybatis-demo/
├── src/main/java/com/dbpwd/demo/mybatis/
│   ├── MybatisDemoApplication.java      # 主程序入口
│   ├── model/
│   │   └── Product.java                 # 产品实体类
│   └── dao/
│       └── ProductMapper.java           # Product Mapper接口
├── src/main/resources/
│   └── mybatis-config.xml               # MyBatis配置文件
└── pom.xml                              # Maven配置
```

## 运行Demo

### 1. 编译项目

```bash
# 安装核心模块和MyBatis集成模块
mvn install -pl db-pwd-core,db-pwd-mybatis -am -DskipTests

# 编译Demo
mvn compile -pl db-pwd-mybatis-demo
```

### 2. 运行Demo

```bash
mvn exec:java -Dexec.mainClass="com.dbpwd.demo.mybatis.MybatisDemoApplication" -pl db-pwd-mybatis-demo
```

或打包后运行：

```bash
mvn package -pl db-pwd-mybatis-demo
java -jar db-pwd-mybatis-demo/target/db-pwd-mybatis-demo-1.0.0.jar
```

## 输出示例

```
=========================================
   Database Password Manager - MyBatis Demo
=========================================

Creating demo key file...
Encrypting H2 password...
  Encrypted password: ENC(SM4-GCM:abc123def456...

Building MyBatis configuration...

Initializing database tables...
  Products table created successfully.

=========================================
   CRUD Operations Demo
=========================================

1. Creating products...
   Created product: Product{id=1, name='Laptop', description='High-performance laptop', price=1299.99, stock=50, category='Electronics'}
   Created product: Product{id=2, name='Smartphone', description='Latest smartphone model', price=899.99, stock=100, category='Electronics'}
   Created product: Product{id=3, name='Java Programming', description='Comprehensive Java guide', price=49.99, stock=200, category='Books'}

2. Querying all products...
   Found 3 product(s):
   - Product{id=1, name='Laptop', description='High-performance laptop', price=1299.99, stock=50, category='Electronics'}
   - Product{id=2, name='Smartphone', description='Latest smartphone model', price=899.99, stock=100, category='Electronics'}
   - Product{id=3, name='Java Programming', description='Comprehensive Java guide', price=49.99, stock=200, category='Books'}

3. Querying product by ID: 1...
   Found: Product{id=1, name='Laptop', description='High-performance laptop', price=1299.99, stock=50, category='Electronics'}

4. Updating product ID: 1...
   Updated 1 row(s).
   After update: Product{id=1, name='Laptop', description='High-performance laptop', price=1199.99, stock=45, category='Electronics'}

5. Querying products by category: Electronics...
   Found 2 product(s):
   - Product{id=1, name='Laptop', description='High-performance laptop', price=1199.99, stock=45, category='Electronics'}
   - Product{id=2, name='Smartphone', description='Latest smartphone model', price=899.99, stock=100, category='Electronics'}

6. Deleting product ID: 2...
   Deleted 1 row(s).

7. Querying remaining products...
   Found 2 product(s):
   - Product{id=1, name='Laptop', description='High-performance laptop', price=1199.99, stock=45, category='Electronics'}
   - Product{id=3, name='Java Programming', description='Comprehensive Java guide', price=49.99, stock=200, category='Books'}

=========================================
   Demo completed successfully!
=========================================

Key points demonstrated:
- ENC(SM4-GCM:xxx) format for encrypted password
- Password automatically decrypted in DbPwdDataSourceFactory
- MyBatis mapper with CRUD operations
- H2 in-memory database for demo
```

## 核心代码说明

### 1. MyBatis配置 (mybatis-config.xml)

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">
<configuration>
    <settings>
        <!-- 启用驼峰命名转换 -->
        <setting name="mapUnderscoreToCamelCase" value="true"/>
    </settings>

    <!-- Mapper注册 -->
    <mappers>
        <mapper class="com.dbpwd.demo.mybatis.dao.ProductMapper"/>
    </mappers>
</configuration>
```

### 2. Mapper接口 (ProductMapper.java)

```java
public interface ProductMapper {
    @Insert("INSERT INTO products (name, description, price, stock, category) VALUES (#{name}, #{description}, #{price}, #{stock}, #{category})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Product product);

    @Select("SELECT * FROM products WHERE id = #{id}")
    Product selectById(Long id);

    @Select("SELECT * FROM products ORDER BY id")
    List<Product> selectAll();
}
```

### 3. 密码解密流程

```java
// 1. 生成SM4密钥
String sm4Key = Sm4KeyLoader.generateKey();

// 2. 加密密码
String cipherText = Sm4GCMCipher.encrypt(H2_PASSWORD, sm4Key);
String encPassword = ENCFormat.formatSM4GCM(cipherText);

// 3. 解密密码（在DbPwdDataSourceFactory中自动完成）
String plainPassword = decryptPassword(encPassword, sm4Key);
```

## 使用DbPwdDataSourceFactory

### 方式1: 在mybatis-config.xml中配置

```xml
<configuration>
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="com.dbpwd.mybatis.DbPwdDataSourceFactory">
                <property name="dbId" value="my_database"/>
                <property name="keyFile" value="~/.dbpwd/sm4.key"/>
                <property name="pwdFile" value="~/.dbpwd/db.pwd"/>
            </dataSource>
        </environment>
    </environments>
</configuration>
```

### 方式2: 纯Java配置

```java
// 创建数据源工厂
DbPwdDataSourceFactory factory = new DbPwdDataSourceFactory();
factory.setDbId("my_database");
factory.setKeyFile("~/.dbpwd/sm4.key");
factory.setPwdFile("~/.dbpwd/db.pwd");

// 获取数据源
DataSource dataSource = factory.getDataSource();

// 构建MyBatis配置
Environment environment = new Environment("development",
    new JdbcTransactionFactory(),
    dataSource);

Configuration config = new Configuration(environment);
config.addMapper(ProductMapper.class);

SqlSessionFactory sessionFactory = new SqlSessionFactoryBuilder().build(config);
```

### 方式3: 使用Druid连接池

```java
// 使用Druid连接池工厂
DbPwdDruidDataSourceFactory factory = new DbPwdDruidDataSourceFactory();
factory.setDbId("my_database");
factory.setKeyFile("~/.dbpwd/sm4.key");
factory.setPwdFile("~/.dbpwd/db.pwd");

// 配置连接池参数
factory.setInitialSize(2);
factory.setMaxActive(10);

// 获取数据源
DataSource dataSource = factory.getDataSource();
```

## 技术栈

| 组件 | 版本 | 说明 |
|-----|------|------|
| JDK | 8+ | Java开发环境 |
| MyBatis | 3.5.13 | ORM框架 |
| Druid | 1.2.20 | 数据库连接池（可选） |
| H2 | 2.1.214 | 内存数据库（Demo用） |
| db-pwd-core | 1.0.0 | 核心加解密模块 |
| db-pwd-mybatis | 1.0.0 | MyBatis集成模块 |

## 配置说明

### 1. 密钥文件路径

- 默认路径: `~/.dbpwd/sm4.key`
- 自定义路径: 通过 `setKeyFile()` 设置

### 2. 密码文件路径

- 默认路径: `~/.dbpwd/db.pwd`
- 自定义路径: 通过 `setPwdFile()` 设置

### 3. 数据库ID占位符

- 必须设置 `dbId` 参数
- `dbId` 将在 `db.pwd` 文件中查找对应的数据库配置
- 支持覆盖 url、username、driverClassName 等参数

## 注意事项

1. **演示用密钥**: Demo自动生成测试密钥文件，生产环境应通过命令行工具生成安全密钥
2. **H2数据库**: H2 内存数据库仅用于演示，生产环境应使用实际数据库
3. **ENC格式**: 新应用必须使用 `ENC(SM4-GCM:xxx)` 格式以获取完整性保护
4. **密钥文件权限**: 生产环境确保密钥文件权限为 600

## 与其他集成模块的对比

| 特性 | MyBatis Demo | Spring Demo | Spring Boot Demo | Druid Demo | Simple JDBC Demo |
|-----|------------|------------|------------------|------------|------------------|
| 集成方式 | DataSourceFactory | BeanPostProcessor | Auto Configuration | PasswordCallback | API直接调用 |
| 适用场景 | MyBatis应用 | 传统Spring应用 | Spring Boot应用 | Druid连接池应用 | 纯JDBC应用/工具类 |
| 配置方式 | XML/Java配置 | Java/XML配置 | application.yml | API调用 | 代码配置 |
| 框架依赖 | MyBatis | Spring Framework | Spring Boot | Druid | 无 |

## 生成加密密码

### 使用命令行工具

```bash
# 加密明文密码
java -jar db-pwd-cli.jar encrypt --password "yourPassword"

# 输出示例:
# Database ID: my_database
# Encrypted Password: ENC(SM4-GCM:abc123def456...)
# Files generated: ~/.dbpwd/db.pwd, ~/.dbpwd/sm4.key
```

### 使用代码生成

```java
import com.dbpwd.crypto.Sm4GCMCipher;
import com.dbpwd.crypto.ENCFormat;

// 生成密钥
String sm4Key = Sm4KeyLoader.generateKey();

// 加密密码
String plainPassword = "yourPassword";
String cipherText = Sm4GCMCipher.encrypt(plainPassword, sm4Key);
String encPassword = ENCFormat.formatSM4GCM(cipherText);
```

## 相关文档

- [MyBatis集成模块 README](../db-pwd-mybatis/README.md)
- [核心模块 README](../db-pwd-core/README.md)
- [集成场景和方案](../technical/integration-scenarios.md)
- [Druid集成示例](../db-pwd-druid/README.md)
- [Spring集成示例](../db-pwd-spring/README.md)

## 许可证

MIT License
