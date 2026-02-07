# Druid 数据库密码加密存储工具

## 项目概述

本项目提供了一个完整的Druid JDBC连接池密码加密存储和多连接池管理解决方案，主要特性包括：

1. **密码加密支持**: 扩展Druid密码加密机制，支持国密SM4（默认）和AES算法
2. **多连接池管理**: 支持应用配置多个数据库连接池，包括读写分离、分库分表等场景
3. **智能配置文件命名**: 根据数据库类型、IP、端口、数据库名自动生成唯一配置文件名
4. **配置中心集成**: 支持通过HTTP API上传下载配置文件
5. **自动化脚本**: 提供Bash脚本生成配置、调用API

## 环境要求 - **重要**

### JDK版本要求（强制）
本项目**强制要求使用 JDK 8** 进行构建和运行。

| JDK版本 | 支持状态 | 说明 |
|---------|---------|------|
| JDK 8   | ✅ 完全支持 | **唯一支持的版本** |
| JDK 11+ | ❌ 不支持 | 代码使用了JDK 8特定语法，不兼容 |

**重要说明**：
- 项目源代码使用 JDK 8 语法编写
- `pom.xml` 配置了编译目标为 Java 1.8
- 请确保构建环境使用的是 JDK 8 (1.8.x)
- 如需检查当前 JDK 版本，请运行：`java -version`

## 目录结构

```
dbpwd/
├── config/                          # 配置文件目录
│   ├── druid.properties.template   # 配置文件模板
│   └── druid.properties.example    # 示例配置文件
├── scripts/                        # 脚本目录
│   ├── setup-druid-config.sh      # 配置生成和上传脚本
│   ├── setup-druid-config.bat     # Windows版本脚本
│   ├── test-connection.sh         # 连接测试脚本
│   └── test-connection.bat        # Windows版本测试脚本
├── src/                           # 源代码目录
│   ├── main/java/com/dbpwd/
│   │   ├── DruidPasswordCallback.java       # 密码加密解密回调（支持SM4/AES）
│   │   ├── DruidConfigGenerator.java        # 配置生成器
│   │   ├── DruidConfigUploader.java         # 配置中心上传下载
│   │   ├── DruidConnectionPool.java         # 连接池工厂
│   │   ├── DruidConnectionPoolManager.java  # 多连接池管理器
│   │   ├── DruidConfigGeneratorCLI.java     # 命令行工具
│   │   ├── JdbcUrlParser.java              # JDBC URL解析器
│   │   ├── ConfigFileNameGenerator.java     # 配置文件名生成器
│   │   └── MultiDataSourceExample.java      # 多数据源使用示例
│   └── test/java/com/dbpwd/
│       └── DruidConnectionTest.java         # 连接池测试
├── docs/                           # 文档目录
│   └── CONFIG_CENTER_DIR_STRUCTURE.md       # 配置中心目录结构文档
├── pom.xml                         # Maven配置
└── README.md                       # 本文件
```

## 核心实现说明

### 1. Druid密码加密扩展机制

本项目通过实现自定义的`DruidPasswordCallback`类扩展加密能力：

#### 支持的加密算法

1. **SM4/GCM**: 国密算法（默认），128位密钥
2. **AES/GCM**: AES算法，256位密钥

配置示例：
```properties
password=[16]Base64(IV+Ciphertext)
druid.connectionProperties=config.decrypt=true;config.decrypt.key=your_key;config.decrypt.algorithm=SM4
```

**注意**: 默认使用国密SM4算法，如需使用AES显式指定`algorithm=AES`

### 2. 智能配置文件命名

为支持多连接池，系统根据数据库信息自动生成唯一配置文件名：

#### 命名策略

| 策略 | 格式 | 示例 |
|------|------|------|
| HOST_PORT_DB | `{host}_{port}_{database}.properties` | `192_168_1_100_3306_userdb.properties` |
| ENV_HOST_PORT_DB | `{env}_{host}_{port}_{database}.properties` | `prod_192_168_1_100_3306_userdb.properties` |
| DB_HOST_PORT | `{database}_{host}_{port}.properties` | `userdb_192_168_1_100_3306.properties` |

#### 目录结构

推荐使用以下目录结构：

```
/druid/config/
├── MySQL/
│   ├── prod_192_168_1_100_3306_userdb.properties
│   ├── prod_192_168_1_100_3306_orderdb.properties
│   ├── test_192_168_1_101_3306_userdb.properties
│   └── dev_localhost_3306_testdb.properties
├── PostgreSQL/
│   ├── prod_192_168_1_50_5432_analytics.properties
│   └── dev_localhost_5432_testdb.properties
└── Oracle/
    └── prod_192_168_1_60_1521_hrm.properties
```

### 3. 配置中心API集成

#### 上传配置 API

```bash
POST /upload
Headers:
  X-API-Key: {api_key}
Content-Type: multipart/form-data

Form Data:
  directory={config_directory}
  path={file_name}
  file=@{file_content}
```

#### 下载配置 API

```bash
GET /download?directory={dir}&path={file}
Headers:
  X-API-Key: {api_key}
```

## 快速开始

### 1. 环境要求

- Java 8+ (推荐Java 11或17)
- Maven 3.6+
- Bash shell (Linux/macOS) 或 Git Bash (Windows)
- MySQL/PostgreSQL等数据库

### 2. 编译项目

```bash
mvn clean package
```

### 3. 生成配置并上传

Linux/macOS:
```bash
bash scripts/setup-druid-config.sh
```

Windows:
```cmd
scripts\setup-druid-config.bat
```

### 4. 测试连接

Linux/macOS:
```bash
bash scripts/test-connection.sh
```

Windows:
```cmd
scripts\test-connection.bat
```

## 多数据源配置

### 场景1: 基本多数据源

```java
// 创建连接池管理器
DruidConnectionPoolManager manager = new DruidConnectionPoolManager();

// 添加MySQL用户数据库
manager.addDataSource("userdb",
    "config/druid/MySQL/prod/192_168_1_100_3306_userdb.properties");

// 添加PostgreSQL分析数据库
manager.addDataSource("analytics",
    "config/druid/PostgreSQL/prod/192_168_1_50_5432_analytics.properties");

// 使用指定数据源
try (Connection conn = manager.getConnection("userdb")) {
    // 执行用户相关操作
}
```

### 场景2: 读写分离

```java
DruidConnectionPoolManager manager = new DruidConnectionPoolManager();

// 创建订单组
DataSourceGroup orderGroup = manager.createGroup("order");

// 添加主库（写操作）
manager.addDataSource("order_primary",
    "config/druid/MySQL/prod/192_168_1_100_3306_order.properties", "order");
orderGroup.setPrimary("order_primary");

// 添加从库（读操作）
manager.addDataSource("order_slave1",
    "config/druid/MySQL/prod/192_168_1_101_3306_order.properties", "order");
orderGroup.addSecondary("order_slave1");

// 写操作 - 使用主库
try (Connection conn = manager.getPrimaryConnection("order")) {
    // 执行insert/update/delete
}

// 读操作 - 使用从库（自动负载均衡）
try (Connection conn = manager.getSecondaryConnection("order")) {
    // 执行select查询
}
```

### 场景3: 分库分表

```java
// 使用建造者模式配置
DruidConnectionPoolManager manager = DruidConnectionPoolManager.builder()
    .addDataSource("user_db",
        "config/druid/MySQL/prod/user/userdb.properties", "user")
    .addDataSource("order_db",
        "config/druid/MySQL/prod/order/orderdb.properties", "order")
    .addDataSource("product_db",
        "config/druid/MySQL/prod/product/productdb.properties", "product")
    .build();

// 使用不同业务库
try (Connection conn = manager.getConnection("user_db")) {
    // 用户操作
}
try (Connection conn = manager.getConnection("order_db")) {
    // 订单操作
}
```

## 配置文件示例

### 单数据库配置

```properties
# Basic Configuration
driverClassName=com.mysql.cj.jdbc.Driver
url=jdbc:mysql://192.168.1.100:3306/userdb?useSSL=false
username=root
password=[16]encrypted_password_here

# Password Configuration (默认使用SM4国密算法)
druid.connectionProperties=config.decrypt=true;config.decrypt.key=your_key;config.decrypt.algorithm=SM4

# Connection Pool
initialSize=5
minIdle=5
maxActive=20
maxWait=60000
```

### 多数据库配置

每个数据库独立的配置文件，通过文件名区分：

```
/druid/config/MySQL/prod/
  ├── prod_192_168_1_100_3306_userdb.properties   # 用户数据库
  ├── prod_192_168_1_100_3306_orderdb.properties  # 订单数据库
  └── prod_192_168_1_100_3306_productdb.properties # 商品数据库
```

## 加密算法配置

### 使用国密SM4（默认）

```java
DruidConfigGenerator generator = new DruidConfigGenerator();
// 默认使用SM4，无需指定
generator.encryptPassword();
```

配置文件：
```properties
druid.connectionProperties=config.decrypt=true;config.decrypt.key=your_key;config.decrypt.algorithm=SM4
```

### 使用AES

```java
DruidConfigGenerator generator = new DruidConfigGenerator();
generator.setAlgorithm("AES");
generator.encryptPassword();
```

配置文件：
```properties
druid.connectionProperties=config.decrypt=true;config.decrypt.key=your_key;config.decrypt.algorithm=AES
```

## 依赖管理

### Maven依赖

```xml
<dependencies>
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>druid</artifactId>
        <version>1.2.20</version>
    </dependency>
    <dependency>
        <groupId>commons-lang</groupId>
        <artifactId>commons-lang</artifactId>
        <version>2.6</version>
    </dependency>
    <dependency>
        <groupId>org.slf4j</groupId>
        <artifactId>slf4j-api</artifactId>
        <version>1.7.36</version>
    </dependency>
    <dependency>
        <groupId>ch.qos.logback</groupId>
        <artifactId>logback-classic</artifactId>
        <version>1.2.11</version>
    </dependency>
</dependencies>
```

### JDK版本兼容性

- **JDK 8**: 完全支持，使用Java 8 API
- **JDK 11**: 推荐，支持HTTP Client
- **JDK 17+**: 支持，兼容模块系统

### 国密算法支持

SM4算法需要额外依赖（JDK 8需要添加BouncyCastle）：

**JDK 8 配置**:
```bash
mvndependency:copy -Dartifact=org.bouncycastle:bcprov-jdk15on:1.70 -DoutputDirectory=lib
```

**JDK 11+**: SM4算法由系统提供，无需额外配置

## 常见问题

### 1. 多连接池配置如何避免混淆？

使用智能文件名和目录结构：
```
/druid/config/{数据库类型}/{环境}_{IP}_{端口}_{数据库}.properties
```

示例：
```
/druid/config/MySQL/prod_192_168_1_100_3306_userdb.properties
/druid/config/MySQL/prod_192_168_1_100_3306_orderdb.properties
```

### 2. 如何切换加密算法？

在配置文件中修改`algorithm`参数：
```properties
druid.connectionProperties=config.decrypt=true;config.decrypt.key=your_key;config.decrypt.algorithm=SM4  # 或 AES
```

### 3. JKD 8是否支持SM4国密？

JDK 8本身不支持SM4算法，需要添加BouncyCastle依赖：
```xml
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk15on</artifactId>
    <version>1.70</version>
</dependency>
```

JDK 11+原生支持SM4算法。

### 4. 配置中心如何支持多环境？

在目录结构中添加环境前缀：
```
/druid/config/{数据库类型}/prod/...
/druid/config/{数据库类型}/test/...
/druid/config/{数据库类型}/dev/...
```

## 安全注意事项

1. **密钥管理**: 加密密钥应妥善保管，不要硬编码在代码中
2. **传输安全**: 配置中心应使用HTTPS协议
3. **权限控制**: 配置中心API应实施严格的访问控制
4. **密钥轮换**: 定期更换加密密钥
5. **访问日志**: 记录配置的访问和修改日志

## 许可证

本项目采用MIT许可证。

## 技术支持

- 问题反馈: [GitHub Issues](https://github.com/yourusername/dbpwd/issues)
- 更多文档: [docs/](./docs/)
