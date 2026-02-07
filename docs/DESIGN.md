# Druid 数据库密码加密存储工具 - 设计文档

## 文档信息

| 项目 | 内容 |
|------|------|
| 文档版本 | 1.0 |
| 创建日期 | 2026-02-07 |
| 最后更新 | 2026-02-07 |
| 作者 | Claude Code |
| JDK支持 | 8, 11, 17+ |

## 目录

1. [项目背景](#1-项目背景)
2. [系统架构](#2-系统架构)
3. [核心模块设计](#3-核心模块设计)
4. [密码加密设计](#4-密码加密设计)
5. [配置中心集成设计](#5-配置中心集成设计)
6. [多数据源管理设计](#6-多数据源管理设计)
7. [配置文件命名规范](#7-配置文件命名规范)
8. [安全设计](#8-安全设计)
9. [性能设计](#9-性能设计)
10. [兼容性设计](#10-兼容性设计)

---

## 1. 项目背景

### 1.1 需求概述

在现代分布式系统中，数据库连接池配置管理面临以下挑战：

1. **密码安全**: 数据库密码明文存储在配置文件中存在安全风险
2. **国密合规**: 需要支持符合国密标准的加密算法
3. **多数据源**: 应用通常需要访问多个数据库，包括读写分离、分库分表等场景
4. **配置管理**: 集中管理多个环境的数据库配置
5. **版本兼容**: 支持JDK 8到JDK 17+的广泛版本

### 1.2 解决方案

本项目提供端到端的解决方案：

| 需求 | 解决方案 |
|------|----------|
| 密码安全 | 支持SM4/AES加密，支持密码加密解密 |
| 国密合规 | 默认使用SM4国密算法，支持国密标准的密钥管理 |
| 多数据源 | 提供连接池管理器，支持读写分离、负载均衡 |
| 配置管理 | 集成配置中心API，支持配置的上传下载和动态刷新 |
| 版本兼容 | 使用JDK 8语法编译，支持JDK 8+所有版本 |

---

## 2. 系统架构

### 2.1 总体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                         应用层                                   │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐        │
│  │  DAO层       │  │  Service层   │  │  Web层       │        │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘        │
└─────────┼─────────────────┼──────────────────┼─────────────────┘
          │                 │                  │
┌─────────┼─────────────────┼──────────────────┼─────────────────┐
│         │    多数据源管理层   │                  │                 │
│  ┌──────▼──────┐  ┌──────────▼───────────┐                    │
│  │Connection    │  │DruidConnectionPool  │                    │
│  │接口实现      │  │Manager              │                    │
│  └─────────────┘  └──────────┬───────────┘                    │
│                                    │                            │
│  ┌───────────────────────┬────────┴─────────────┐              │
│  │  连接池1              │ 连接池2             │              │
│  │  ┌─────────────────┐ │ ┌─────────────────┐ │              │
│  │  │DruidConnection  │ │ │DruidConnection  │ │              │
│  │  │Pool             │ │ │Pool             │ │              │
│  │  └────────┬────────┘ │ └────────┬────────┘ │              │
│  └───────────┼──────────┘ └─────────┼────────┘              │
└──────────────┼──────────────────────┼────────────────────────┘
               │                      │
┌──────────────┼──────────────────────┼────────────────────────┐
│              │   Druid连接池层       │                        │
│  ┌───────────▼──────────────────────▼──────────┐            │
│  │           DruidDataSource                    │            │
│  │  ┌─────────────────────────────────────┐    │            │
│  │  │    DruidPasswordCallback            │    │            │
│  │  │    (密码解密回调)                    │    │            │
│  │  └─────────────────────────────────────┘    │            │
│  └────────────────────────────────────┬────────┘            │
└───────────────────────────────────────┼────────────────────┘
                                        │
┌───────────────────────────────────────┼────────────────────┐
│               配置管理层               │                    │
│  ┌─────────────────────────────────────▼─────────┐         │
│  │       DruidConfigGenerator               │         │
│  │     ┌───────────────────────────────┐      │         │
│  │     │  JdbcUrlParser               │      │         │
│  │     │  ConfigFileNameGenerator      │      │         │
│  │     └───────────────────────────────┘      │         │
│  └────────────────────────────────────┬────────┘         │
│                                         │                 │
│  ┌────────────────────────────────────▼────────┐         │
│  │        DruidConfigUpload/Download           │         │
│  └─────────────────────────────────────────────┘         │
└─────────────────────────────────────┬────────────────────┘
                                      │
┌─────────────────────────────────────▼────────────────────┐
│                   配置中心                     │        │
│  ┌──────────────────────────────────────────┐            │
│  │  配置存储 (目录结构)                      │            │
│  │  /druid/config/                           │            │
│  │    ├── MySQL/                            │            │
│  │    │   ├── prod_192_168_1_100_3306_*.properties  │  │
│  │    │   └── test_192_168_1_101_3306_*.properties   │  │
│  │    └── PostgreSQL/                        │            │
│  └──────────────────────────────────────────┘            │
└───────────────────────────────────────────────────────────┘
```

### 2.2 模块依赖关系

```
DruidConnectionPoolManager
    ├── DruidConnectionPool
    │   └── DruidDataSource (Druid提供)
    └── DruidPasswordCallback
        └── 加密算法 (SM4/AES)

DruidConfigGenerator
    ├── JdbcUrlParser
    └── ConfigFileNameGenerator

DruidConfigUploader
    └── HTTP Client / curl
```

---

## 3. 核心模块设计

### 3.1 密码加密解密回调

**类名**: `DruidPasswordCallback`

**职责**:
- 解密配置文件中的加密密码
- 支持SM4和AES算法
- 根据配置动态选择解密算法

**设计要点**:
```java
public class DruidPasswordCallback extends DruidPasswordCallback {
    @Override
    public void setProperties(Properties properties) {
        // 1. 读取解密配置
        boolean decrypt = Boolean.parseBoolean(
            properties.getProperty("config.decrypt"));

        // 2. 读取密钥
        String key = properties.getProperty("config.decrypt.key");

        // 3. 读取算法
        String algorithm = properties.getProperty(
            "config.decrypt.algorithm", "SM4");

        // 4. 执行解密
        if (decrypt) {
            String decrypted = decrypt(encryptedPassword, key, algorithm);
            setPassword(decrypted.toCharArray());
        }
    }
}
```

### 3.2 JDBC URL解析器

**类名**: `JdbcUrlParser`

**职责**:
- 解析JDBC连接URL
- 提取数据库类型、主机、端口、数据库名等信息
- 支持多种数据库类型的URL格式

**支持的数据库类型**:

| 数据库类型 | JDBC前缀 | 默认端口 |
|-----------|---------|---------|
| MySQL | `jdbc:mysql://` | 3306 |
| PostgreSQL | `jdbc:postgresql://` | 5432 |
| Oracle | `jdbc:oracle:thin:@` | 1521 |
| SQL Server | `jdbc:sqlserver://` | 1433 |
| DB2 | `jdbc:db2://` | 50000 |

**设计要点**:
```java
public class DatabaseInfo {
    private String dbType;      // 数据库类型
    private String host;        // 主机地址
    private int port;           // 端口号
    private String database;    // 数据库名
    private String originalUrl; // 原始URL

    // 生成配置文件识别标识
    public String generateIdentifier() {
        return host + "_" + port + "_" + database;
    }
}
```

### 3.3 配置文件名生成器

**类名**: `ConfigFileNameGenerator`

**职责**:
- 根据数据库信息生成唯一的配置文件名
- 支持多种命名策略
- 生成配置中心目录路径

**命名策略**:

| 策略枚举 | 说明 | 示例 |
|---------|------|------|
| HOST_PORT_DB | host_port_db.properties | 192_168_1_100_3306_userdb.properties |
| ENV_HOST_PORT_DB | 环境前缀 + host + port + db | prod_192_168_1_100_3306_userdb.properties |
| DB_HOST_PORT | db_host_port.properties | userdb_192_168_1_100_3306.properties |
| DATABASE_NAME | 仅使用数据库名 | userdb.properties |

**目录组织策略**:

| 策略枚举 | 路径格式 |
|---------|---------|
| DATABASE_TYPE | /druid/config/{数据库类型}/ |
| ENVIRONMENT | /druid/config/{环境}/{数据库类型}/ |
| BUSINESS | /druid/config/{业务名称}/{环境}/{数据库类型}/ |

### 3.4 配置生成器

**类名**: `DruidConfigGenerator`

**职责**:
- 设置数据库配置信息
- 加密密码
- 生成配置文件内容
- 生成配置文件名和路径

**工作流程**:
```
1. 输入: JDBC URL, 用户名, 密码
   ↓
2. 解析JDBC URL → 提取数据库信息
   ↓
3. 加密密码 → 生成加密密钥和加密密码
   ↓
4. 生成配置文件内容 → 模板填充
   ↓
5. 生成文件名和路径 → 命名策略应用
   ↓
6. 输出: 配置文件, 上传到配置中心
```

### 3.5 配置上传下载器

**类名**: `DruidConfigUploader`

**职责**:
- 上传配置文件到配置中心
- 从配置中心下载配置文件
- 支持标准HTTP和curl两种方式

**API设计**:

**上传API**:
```http
POST /upload HTTP/1.1
Host: {config_center_url}
X-API-Key: {api_key}
Content-Type: multipart/form-data; boundary=---BOUNDARY

-----BOUNDARY
Content-Disposition: form-data; name="directory"

/druid/config/MySQL
-----BOUNDARY
Content-Disposition: form-data; name="path"

prod_192_168_1_100_3306_userdb.properties
-----BOUNDARY
Content-Disposition: form-data; name="file"; filename="config.properties"

[配置文件内容]
-----BOUNDARY--
```

**下载API**:
```http
GET /download?directory=/druid/config/MySQL&path=prod_192_168_1_100_3306_userdb.properties HTTP/1.1
Host: {config_center_url}
X-API-Key: {api_key}
```

### 3.6 多数据源管理器

**类名**: `DruidConnectionPoolManager`

**职责**:
- 管理多个数据库连接池
- 支持数据源分组（读写分离）
- 支持负载均衡策略
- 提供连接获取接口

**核心数据结构**:

```java
public class DruidConnectionPoolManager {
    // 所有连接池的存储
    private Map<String, PoolHolder> pools;

    // 分组存储（用于读写分离等场景）
    private Map<String, DataSourceGroup> groups;

    // 默认数据源
    private String defaultDataSource;
}

public class PoolHolder {
    private String name;
    private DruidConnectionPool pool;
    private JdbcUrlParser.DatabaseInfo dbInfo;
    private long lastUsedTime;
    private boolean active;
}

public class DataSourceGroup {
    private String groupName;
    private Map<String, PoolHolder> dataSources;
    private String primaryDataSource;
    private List<String> secondaryDataSources;
}
```

**负载均衡策略**:

| 策略 | 说明 | 适用场景 |
|------|------|---------|
| ROUND_ROBIN | 轮询 | 请求均衡分布 |
| RANDOM | 随机 | 简单随机分配 |
| LEAST_CONNECTIONS | 最少连接 | 根据连接数分配 |

---

## 4. 密码加密设计

### 4.1 加密算法选择

| 算法 | 类型 | 密钥长度 | IV长度 | Tag长度 | 默认 |
|------|------|---------|-------|--------|------|
| SM4/GCM | 国密 | 128位 | 96位 | 128位 | ✅ |
| AES/GCM | AES | 256位 | 96位 | 128位 | ❌ |

**默认使用SM4国密算法**

### 4.2 加密流程

```
原始密码 + 密钥
    ↓
密钥派生 (PBKDF2WithHmacSHA256)
    ↓
生成IV (12字节随机数)
    ↓
GCM模式加密
    ↓
组合 IV + Ciphertext + Tag
    ↓
Base64编码
    ↓
格式: [keyLength]Base64(IV+Ciphertext)
    ↓
加密后密码
```

### 4.3 解密流程

```
加密密码: [16]Base64(...)
    ↓
读取keyLength
    ↓
Base64解码
    ↓
分离 IV + Ciphertext
    ↓
密钥派生 (PBKDF2WithHmacSHA256)
    ↓
GCM模式解密
    ↓
验证Tag
    ↓
原始密码
```

### 4.4 密钥派生

**算法**: PBKDF2WithHmacSHA256

**参数**:
- 迭代次数: 10,000
- 盐值: 密钥的前16字节
- 输出长度: 16字节 (SM4) 或 32字节 (AES-256)

### 4.5 加密格式

```plaintext
[keyLength]Base64(IV+Ciphertext)
```

字段说明:
- `keyLength`: 密钥长度（字节），用于密钥派生
- `IV`: 初始化向量
- `Ciphertext`: 加密后的密文

示例:
```properties
# SM4加密
password=[16]r7X...K9v=...

# AES-256加密
password=[32]aBc...XyZ=...
```

### 4.6 JDK版本兼容

| JDK版本 | SM4支持 | AES支持 |
|---------|---------|---------|
| JDK 8 | ❌ 需要BouncyCastle | ✅ 原生支持 |
| JDK 11+ | ✅ 原生支持 | ✅ 原生支持 |

**JDK 8配置**:
```xml
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk15on</artifactId>
    <version>1.70</version>
</dependency>
```

---

## 5. 配置中心集成设计

### 5.1 配置中心目录结构

**推荐结构**:
```
/druid/config/
├── MySQL/
│   ├── prod/
│   │   ├── prod_192_168_1_100_3306_userdb.properties
│   │   ├── prod_192_168_1_100_3306_orderdb.properties
│   │   └── prod_192_168_1_100_3306_productdb.properties
│   ├── test/
│   │   └── test_192_168_1_101_3306_userdb.properties
│   └── dev/
│       └── dev_localhost_3306_testdb.properties
├── PostgreSQL/
│   └── prod/
│       └── prod_192_168_1_50_5432_analytics.properties
└── Oracle/
    └── prod/
        └── prod_192_168_1_60_1521_hrm.properties
```

### 5.2 配置文件命名规则

**标准格式**: `{env}_{host}_{port}_{database}.properties`

**组成部分**:
| 字段 | 说明 | 示例 |
|------|------|------|
| env | 环境标识 | prod, test, dev, uat |
| host | 主机地址 | 192_168_1_100 (IP中的.替换为_) |
| port | 端口号 | 3306 |
| database | 数据库名 | userdb |

**去重保证**: 通过 `{host}_{port}_{database}` 唯一标识数据库

### 5.3 API设计

#### 5.3.1 上传配置

**请求**:
```http
POST {base_url}/upload HTTP/1.1
X-API-Key: {api_key}
Content-Type: multipart/form-data

--boundary
Content-Disposition: form-data; name="directory"

{directory_path}
--boundary
Content-Disposition: form-data; name="path"

{file_name}
--boundary
Content-Disposition: form-data; name="file"; filename="{file_name}"

{file_content}
--boundary--
```

**响应**:
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "path": "/druid/config/MySQL/prod_192_168_1_100_3306_userdb.properties",
        "version": 1
    }
}
```

#### 5.3.2 下载配置

**请求**:
```http
GET {base_url}/download?directory={dir}&path={file} HTTP/1.1
X-API-Key: {api_key}
```

**响应**:
```http
HTTP/1.1 200 OK
Content-Type: text/plain

[配置文件内容]
```

#### 5.3.3 列出配置

**请求**:
```http
GET {base_url}/list?directory={dir} HTTP/1.1
X-API-Key: {api_key}
```

**响应**:
```json
{
    "code": 200,
    "message": "success",
    "data": {
        "files": [
            "prod_192_168_1_100_3306_userdb.properties",
            "prod_192_168_1_100_3306_orderdb.properties"
        ],
        "count": 2
    }
}
```

### 5.4 配置同步策略

| 策略 | 描述 | 优势 |
|------|------|------|
| 手动触发 | 通过API手动上传下载 | 精确控制 |
| 定时刷新 | 定期检查配置中心更新 | 自动化 |
| Webhook | 配置中心变更时推送 | 实效性好 |

---

## 6. 多数据源管理设计

### 6.1 应用场景

#### 6.1.1 基本多数据源

**场景**: 应用需要访问多个不同类型的数据库

```java
DruidConnectionPoolManager manager = new DruidConnectionPoolManager();
manager.addDataSource("userdb", "config/userdb.properties");
manager.addDataSource("analytics", "config/analytics.properties");

Connection conn1 = manager.getConnection("userdb");
Connection conn2 = manager.getConnection("analytics");
```

#### 6.1.2 读写分离

**场景**: 主库处理写操作，从库处理读操作

```java
DataSourceGroup orderGroup = manager.createGroup("order");
orderGroup.setPrimary("order_primary");
orderGroup.addSecondary("order_slave1");
orderGroup.addSecondary("order_slave2");

// 写操作
Connection writeConn = manager.getPrimaryConnection("order");

// 读操作（负载均衡）
Connection readConn = manager.getSecondaryConnection("order");
```

#### 6.1.3 分库分表

**场景**: 不同业务模块使用不同的数据库

```java
manager.addDataSource("user_db", "config/userdb.properties");
manager.addDataSource("order_db", "config/orderdb.properties");
manager.addDataSource("product_db", "config/productdb.properties");
```

#### 6.1.4 多租户

**场景**: 不同租户使用独立的数据库

```java
for (String tenant : tenants) {
    String dsName = "tenant_" + tenant;
    String configPath = "config/" + tenant + ".properties";
    manager.addDataSource(dsName, configPath);
}
```

### 6.2 连接池管理

#### 6.2.1 数据源注册

```java
public void addDataSource(String name, String configPath) {
    // 1. 加载配置
    DrakeConfigGenerator generator = new DrakeConfigGenerator();
    generator.loadConfig(configPath);

    // 2. 创建连接池
    DrakeConnectionPool pool = DrakeConnectionPool.create(configPath);

    // 3. 解析数据库信息
    DatabaseInfo dbInfo = JdbcUrlParser.parse(pool.getDataSource().getUrl());

    // 4. 包装并注册
    PoolHolder holder = new PoolHolder(name, pool, dbInfo);
    pools.put(name, holder);
}
```

#### 6.2.2 连接获取策略

| 策略 | 方法 | 说明 |
|------|------|------|
| 指定名称 | `getConnection(name)` | 使用指定名称的数据源 |
| 默认数据源 | `getConnection()` | 使用默认数据源 |
| 主库连接 | `getPrimary(group)` | 从组中获取主库 |
| 从库连接 | `getSecondary(group)` | 从组中获取从库（负载均衡） |

#### 6.2.3 负载均衡实现

```java
public enum LoadBalanceStrategy {
    ROUND_ROBIN {
        public String select(List<String> secondaries, DataSourceGroup group) {
            int index = counter.getAndIncrement() % secondaries.size();
            return secondaries.get(index);
        }
    },
    RANDOM {
        public String select(List<String> secondaries, DataSourceGroup group) {
            return secondaries.get(random.nextInt(secondaries.size()));
        }
    },
    LEAST_CONNECTIONS {
        public String select(List<String> secondaries, DataSourceGroup group) {
            String least = null;
            int minConnections = Integer.MAX_VALUE;
            for (String name : secondaries) {
                int active = group.get(name).getPool()
                    .getDataSource().getActiveCount();
                if (active < minConnections) {
                    minConnections = active;
                    least = name;
                }
            }
            return least;
        }
    }
}
```

---

## 7. 配置文件命名规范

### 7.1 命名规范标准

```
{环境}_{主机}_{端口}_{数据库}.properties
```

**各字段说明**:
- 环境标识：2-8位，大写，如：PROD, TEST, DEV
- 主机地址：将IP地址中的 `.` 替换为 `_`
- 端口号：标准端口号，如：3306, 5432
- 数据库名：字母数字下划线，如：user_db, order_db

### 7.2 命名示例

| 场景 | 文件名配置 |
|------|-----------|
| 生产环境MySQL用户库 | `prod_192_168_1_100_3306_userdb.properties` |
| 生产环境PostgreSQL分析库 | `prod_192_168_1_50_5432_analytics.properties` |
| 测试环境MySQL订单库 | `test_192_168_1_101_3306_orderdb.properties` |
| 开发环境本地数据库 | `dev_localhost_3306_testdb.properties` |

### 7.3 目录路径生成

```
/druid/config/{数据库类型}/{文件名}
```

示例:
```
/druid/config/MySQL/prod_192_168_1_100_3306_userdb.properties
/druid/config/PostgreSQL/prod_192_168_1_50_5432_analytics.properties
```

---

## 8. 安全设计

### 8.1 密码安全

#### 8.1.1 加密强度

| 算法 | 密钥长度 | 加密模式 | 安全等级 |
|------|---------|---------|---------|
| SM4 | 128位 | GCM | 符合国密标准 |
| AES | 256位 | GCM | 高安全性 |

#### 8.1.2 密钥管理

**密钥存储**:
- 不在代码中硬编码
- 使用环境变量或配置中心管理
- 定期轮换密钥

**密钥派生**:
```
密钥 → PBKDF2(10000次迭代) → 派生密钥 → 加密/解密
```

### 8.2 传输安全

- 配置中心使用HTTPS协议传输
- API请求使用API Key认证
- 敏感配置字段不记录日志

### 8.3 访问控制

```
配置中心
├── 管理员: 读写所有配置
├── 运维: 读写指定环境配置
└── 应用: 只读指定应用配置
```

### 8.4 审计日志

```
[时间] [用户] [操作] [资源] [结果]
2026-02-07 10:00:00 admin upload /druid/config/MySQL/xxx.properties success
2026-02-07 10:05:00 app download /druid/config/MySQL/xxx.properties success
```

---

## 9. 性能设计

### 9.1 连接池配置

| 参数 | 默认值 | 说明 |
|------|-------|------|
| initialSize | 5 | 初始连接数 |
| minIdle | 5 | 最小空闲连接 |
| maxActive | 20 | 最大活跃连接 |
| maxWait | 60000ms | 最大等待时间 |

### 9.2 配置缓存

- 本地缓存配置文件内容
- 支持配置的定时刷新
- 减少对配置中心的请求

### 9.3 压缩传输

- 配置文件压缩传输
- 减少网络带宽消耗

---

## 10. 兼容性设计

### 10.1 JDK版本兼容

| 特性 | JDK 8 | JDK 11 | JDK 17+ |
|------|-------|--------|---------|
| 代码语法 | ✅ | ✅ | ✅ |
| SM4算法 | ⚠️ | ✅ | ✅ |
| AES算法 | ✅ | ✅ | ✅ |
| HTTP Client | ❌ | ✅ | ✅ |
| 模块化 | ❌ | ❌ | ✅ |

**JDK 8限制**:
- SM4算法需要额外依赖
- 使用传统HttpURLConnection

### 10.2 数据库兼容

| 数据库 | 支持版本 | 测试状态 |
|--------|---------|---------|
| MySQL | 5.7, 8.0+ | ✅ 已测试 |
| PostgreSQL | 12+ | ✅ 已测试 |
| Oracle | 11g, 19c | ✅ 已测试 |
| SQL Server | 2017 | ✅ 已测试 |

### 10.3 Druid版本兼容

| Druid版本 | 本项目验证 |
|----------|-----------|
| 1.1.x | ✅ 支持 |
| 1.2.x | ✅ 推荐 |
| 1.3.x | ✅ 支持 |

---

## 附录

### A. 配置文件完整示例

```properties
# Druid JDBC Connection Pool Configuration
# Generated at: 2026-02-07 10:00:00

# Database Information
config.center.db.type=MySQL
config.center.db.host=192.168.1.100
config.center.db.port=3306
config.center.db.name=userdb

# Basic Configuration
driverClassName=com.mysql.cj.jdbc.Driver
url=jdbc:mysql://192.168.1.100:3306/userdb?useSSL=false&serverTimezone=UTC
username=root
password=[16]encrypted_password_here

# Password Decryption Configuration (默认使用SM4国密算法)
druid.connectionProperties=config.decrypt=true;config.decrypt.key=your_key;config.decrypt.algorithm=SM4

# Connection Pool Configuration
initialSize=5
minIdle=5
maxActive=20
maxWait=60000
timeBetweenEvictionRunsMillis=60000
minEvictableIdleTimeMillis=300000
validationQuery=SELECT 1
testWhileIdle=true
testOnBorrow=false
testOnReturn=false
poolPreparedStatements=true
maxPoolPreparedStatementPerConnectionSize=20

# Configuration Center Integration
config.center.enabled=true
config.center.autoRefresh=true
config.center.refreshInterval=300
```

### B. 使用示例代码

```java
// 1. 生成配置
DruidConfigGenerator generator = new DruidConfigGenerator();
generator.setDatabaseConfig("com.mysql.cj.jdbc.Driver",
    "jdbc:mysql://192.168.1.100:3306/userdb",
    "root",
    "password");
generator.setEnvironment("prod");
generator.setAlgorithm("SM4"); // 默认就是SM4
generator.encryptPassword();
generator.saveConfig("config/prod_192_168_1_100_3306_userdb.properties");

// 2. 上传到配置中心
DruidConfigUploader uploader = new DruidConfigUploader(
    "http://config-center:8080",
    "/druid/config/MySQL",
    "api_key_here"
);
uploader.upload("config/prod_192_168_1_100_3306_userdb.properties");

// 3. 在应用中使用
DruidConnectionPool manager = DruidConnectionPoolManager.create(
    "config/prod_192_168_1_100_3306_userdb.properties"
);
```

---

## 版本历史

| 版本 | 日期 | 变更内容 |
|------|------|---------|
| 1.0 | 2026-02-07 | 初始版本 |