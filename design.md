# 数据库密码管理系统设计文档

## 目录
- [1. 整体架构](#1-整体架构)
- [2. 数据库密码管理](#2-数据库密码管理)
- [3. 命令行工具](#3-命令行工具)
- [4. 配置中心通讯](#4-配置中心通讯)
- [5. 框架集成](#5-框架集成)
- [6. 连接池包装类](#6-连接池包装类)
- [7. 安全机制设计](#7-安全机制设计)
- [8. 开发计划](#8-开发计划)

---

## 1. 整体架构

### 1.1 系统目标
提供统一的数据库密码管理方案，解决以下问题：
- 数据库密码集中化管理
- 密码加密存储和传输
- 密码安全分发和更新
- 连接池集成无缝使用

### 1.2 架构组件
```
┌─────────────────────────────────────────────────────────────┐
│                    应用层 (Application)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  SpringBoot  │  │   MyBatis    │  │   原生JDBC    │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
└─────────┼─────────────────┼─────────────────┼──────────────┘
          │                 │                 │
          └─────────────────┴─────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────────┐
│              连接池包装层 (Connection Pool Wrapper)          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  DruidWrap   │  │  HikariWrap  │  │ DBCP2Wrap    │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
└─────────┼─────────────────┼─────────────────┼──────────────┘
          │                 │                 │
          └─────────────────┴─────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────────┐
│              密码服务核心层 (Password Service Core)          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ SM4加解密     │  │ 密钥管理      │  │ 本地文件管理  │      │
│  │  Service     │  │  Service     │  │  Service     │      │
│  └──────┴───────┘  └──────┴───────┘  └──────┴───────┘      │
└─────────────────────────────────────────────────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────────┐
│              存储层 (Storage)                                 │
│  ┌──────────────────────────────┐  ┌──────────────┐        │
│  │  ~/.dbpwd/db.pwd             │  │ ~/.dbpwd/    │        │
│  │  (密码文件 - JSON格式)        │  │ sm4.key      │        │
│  │  权限: 600                    │  │ 权限: 600    │        │
│  └──────────────────────────────┘  └──────────────┘        │
│                                                                    │
│  容器部署: 挂载目录 /app/.dbpwd (外设600权限)                      │
└────────────────────────────────────────────────────────────┘
```

### 1.3 设计原则
1. **最小权限原则**: 文件权限严格设置为600
2. **职责分离**: 系统管理密钥，应用管理密码文件
3. **可审计**: 所有密文操作记录更新时间
4. **兼容性**: 支持Druid ENC_格式明文/密文混用
5. **可测试**: 仅需sm4.key即可测试所有数据库连接

---

## 2. 数据库密码管理

### 2.1 密码文件格式设计

**设计选择**: 采用JSON格式存储密码文件

**选择理由**:
- **可读性**: JSON格式清晰易读，便于审计和问题排查
- **可扩展性**: 新增字段不破坏向后兼容性
- **工具友好**: 各语言原生支持，便于工具开发
- **完整性**: 每条记录独立，格式验证简单
- **对比编码方式**: 将信息编码到密码字段会导致密码字段过于复杂，维护困难，且不支持增量更新验证

**密码文件结构** (`~/.dbpwd/db.pwd`):
```json
{
  "version": "1.0",
  "lastModified": "2026-02-08T10:30:45Z",
  "databases": [
    {
      "id": "db001",
      "name": "主账务数据库",
      "type": "mysql",
      "host": "192.168.1.100",
      "port": 3306,
      "database": "acdb_main",
      "username": "app_user",
      "password": "ENC(SM4:AABBCCDD1234567890...)",
      "encrypted": true,
      "createdAt": "2026-01-15T08:00:00Z",
      "updatedAt": "2026-02-08T10:30:45Z"
    },
    {
      "id": "db002",
      "name": "只读从库",
      "type": "postgresql",
      "host": "192.168.1.101",
      "port": 5432,
      "database": "analytics",
      "username": "readonly",
      "password": "plain_text_password",  // 特殊情况支持明文
      "encrypted": false,
      "createdAt": "2026-01-20T09:00:00Z",
      "updatedAt": "2026-01-20T09:00:00Z"
    }
  ]
}
```

### 2.2 密码字段格式规范

**设计参考**: Apache Druid密文格式，兼容扩展

**格式定义**:
```
ENC(<algorithm>:<encrypted_value>)
```

**支持格式**:
1. **SM4加密密文**: `ENC(SM4:AABBCCDD1234567890ABCDEF12345678)`
2. **明文密码**: 直接存储明文字符串（不推荐，仅用于特殊场景）
3. **其他算法预留**: `ENC(AES:...)` / `ENC(RSA:...)`

**格式示例**:
```json
{
  "password": "ENC(SM4:8a7b6c5d4e3f2a1b0c9d8e7f6a5b4c3d)"  // SM4加密
}
{
  "password": "temp_pass_123"  // 明文（紧急情况）
}
```

### 2.3 SM4密钥管理

**密钥存储位置**: `~/.dbpwd/sm4.key`

**密钥格式**:
```
# SM4密钥文件（128位密钥，16字节hex编码）
AABBCCDD1234567890ABCDEF12345678
```

**权限要求**:
- 文件权限: `600` (仅所有者读写)
- 所属用户: 应用执行用户

### 2.4 密码更新流程

**设计选择**: 取消配置中心上传能力，采用线下评审+手工更新模式

**更新流程**:
```
1. 系统经理/安全管理员更新密码文件（线下评审）
   ↓
2. 将更新后的db.pwd文件安全传输到目标服务器
   ↓
3. 本地命令行工具自动检测文件变化，对比更新时间
   ↓
4. 如需生成新密文，工具自动下载最新的sm4.key
   ↓
5. 加密生成新密码密文（使用本地sm4.key）
   ↓
6. 更新本地db.pwd文件
   ↓
7. 应用自动重新加载（或触发reload）
```

### 2.5 密码文件字段说明

| 字段名称 | 类型 | 必填 | 说明 |
|---------|------|------|------|
| id | String | 是 | 数据库唯一标识 |
| name | String | 是 | 数据库描述名称 |
| type | String | 是 | 数据库类型：mysql/postgresql/oracle/sqlserver |
| host | String | 是 | 数据库主机地址 |
| port | Integer | 是 | 数据库端口 |
| database | String | 是 | 数据库名称 |
| username | String | 是 | 连接用户名 |
| password | String | 是 | 密码（支持密文或明文） |
| encrypted | Boolean | 是 | 是否加密存储 |
| createdAt | DateTime | 是 | 创建时间（ISO8601） |
| updatedAt | DateTime | 是 | 最后更新时间（ISO8601） |

### 2.6 配置中心集成方式简化

**设计变更**: 配置中心仅提供统一的密码文件模板

**配置中心内容**:
```yaml
# 配置中心配置 (配置中心不做加密，只提供模板)
database:
  passwordFile: /app/.dbpwd/db.pwd  # 指向本地密码文件路径
  keyFile: /app/.dbpwd/sm4.key      # 指向本地密钥文件路径
```

**应用加载逻辑**:
1. 从配置中心获取密码文件路径
2. 读取本地db.pwd文件
3. 根据应用需要查询对应数据库配置
4. 使用本地sm4.key解密密码
5. 注入到数据源配置

---

## 3. 命令行工具

### 3.1 工具功能模块

```
dbpwd-cli/
├── download/           # 下载功能
│   ├── downloadKey()    # 下载sm4.key
│   ├── downloadPwd()    # 下载db.pwd
│   └── compare()        # 对比更新时间
├── encrypt/            # 加密功能
│   ├── encryptPwd()     # 加密单个密码
│   ├── encryptFile()    # 加密密码文件
│   └── autoUpdate()     # 自动更新密文
├── decrypt/            # 解密功能
│   ├── decryptPwd()     # 解密单个密码
│   ├── decryptFile()    # 解密密码文件
│   └── decryptDb()      # 解密指定数据库密码
├── test/               # 测试功能
│   ├── testConnection() # 测试单个连接
│   ├── testAll()        # 测试所有连接
│   └── validateFile()   # 验证文件格式
└── admin/               # 管理功能
    ├── addDb()          # 添加数据库
    ├── updateDb()       # 更新数据库
    ├── deleteDb()       # 删除数据库
    └── listDb()         # 列出所有数据库
```

### 3.2 命令行接口设计

```bash
# dbpwd-cli --help
数据库密码管理命令行工具 v1.0

用法:
  dbpwd-cli [command] [options]

命令:
  download    下载密钥或密码文件
  encrypt     加密密码
  decrypt     解密密码
  test        测试数据库连接
  admin       管理数据库配置
  version     显示版本信息

# dbpwd-cli download --help
下载密钥或密码文件

用法:
  dbpwd-cli download [key|pwd] [options]

选项:
  -v, --verify    下载后验证文件完整性
  -o, --output    指定输出路径（默认: ~/.dbpwd/）
  --compare       下载后与本地文件对比更新时间

示例:
  dbpwd-cli download key          # 下载sm4.key
  dbpwd-cli download pwd --compare # 下载db.pwd并对比
  dbpwd-cli download key -v       # 下载并验证密钥

# dbpwd-cli encrypt --help
加密密码

用法:
  dbpwd-cli encrypt [password] [options]

选项:
  -f, --file      从文件读取密码
  -d, --db-id     为指定数据库ID加密
  --interactive   交互式输入密码（隐藏输入）

示例:
  dbpwd-cli encrypt "myPassword"           # 直接加密
  dbpwd-cli encrypt -d db001                  # 为db001更新密码
  dbpwd-cli encrypt --interactive             # 交互式加密

# dbpwd-cli test --help
测试数据库连接

用法:
  dbpwd-cli test [db-id|all] [options]

选项:
  -v, --verbose   显示详细信息
  --timeout       连接超时时间（秒，默认30）

示例:
  dbpwd-cli test db001              # 测试db001连接
  dbpwd-cli test all                    # 测试所有连接
  dbpwd-cli test db001 --verbose       # 详细测试信息

# dbpwd-cli admin --help
管理数据库配置

用法:
  dbpwd-cli admin [action] [options]

动作:
  add         添加数据库
  update      更新数据库
  delete      删除数据库
  list        列出所有数据库

示例:
  dbpwd-cli admin add                  # 交互式添加
  dbpwd-cli admin update db001         # 更新db001
  dbpwd-cli admin delete db001         # 删除db001
  dbpwd-cli admin list                 # 列出所有
```

### 3.3 自动下载和对比功能

**实现逻辑**:
```python
def download_with_compare(source_url, local_path):
    # 1. 下载远端文件到临时位置
    temp_file = download_to_temp(source_url)
    remote_json = parse_json(temp_file)

    # 2. 读取本地文件
    local_json = parse_json(local_path) if exists(local_path) else None

    # 3. 对比更新时间
    if local_json:
        remote_time = parse_datetime(remote_json['lastModified'])
        local_time = parse_datetime(local_json['lastModified'])

        if remote_time <= local_time:
            print(f"本地文件已是最新 (更新时间: {local_time})")
            return False

    # 4. 替换本地文件
    atomic_move(temp_file, local_path)
    set_file_permissions(local_path, 0o600)
    print(f"文件已更新: {remote_json['lastModified']}")
    return True
```

### 3.4 密钥文件验证

**验证规则**:
```python
def validate_sm4_key(key_file_path):
    # 1. 检查文件权限
    if not check_permissions(key_file_path, 0o600):
        raise SecurityError("密钥文件权限不正确，应为600")

    # 2. 验证密钥格式（16字节hex编码，32字符）
    key_content = read_file(key_file_path).strip()
    if len(key_content) != 32 or not is_hex_string(key_content):
        raise InvalidKeyError("SM4密钥格式不正确，应为16字节hex编码")

    # 3. 验证密钥长度（16字节）
    key_bytes = bytes.fromhex(key_content)
    if len(key_bytes) != 16:
        raise InvalidKeyError("SM4密钥长度不正确，应为16字节")

    return True
```

---

## 4. 配置中心通讯

### 4.1 通讯设计简化

**设计变更**: 配置中心仅作为密码文件路径的配置源

**配置中心集成点**:
1. **应用启动时**: 从配置中心读取 `database.passwordFile` 和 `database.keyFile` 路径
2. **无需加密传输**: 密码文件和密钥文件均存储在应用本地，不通过配置中心传输
3. **无需动态刷新**: 配置文件路径变更时，应用需重启加载

### 4.2 配置中心数据结构

```yaml
# application.yml
database:
  password:
    # 从配置中心读取的密码文件路径
    file: ${config.center.db.password.file:/app/.dbpwd/db.pwd}
    # 从配置中心读取的密钥文件路径
    keyFile: ${config.center.db.password.key:/app/.dbpwd/sm4.key}
```

### 4.3 配置加载流程

```
应用启动
   ↓
1. 从配置中心获取路径配置
   ↓
2. 验证文件存在和权限（600）
   ↓
3. 加载密码文件（JSON格式）
   ↓
4. 加载密钥文件（sm4.key）
   ↓
5. 初始化密码服务
   ↓
6. 连接池使用密码服务获取数据库密码
```

### 4.4 本地文件加载和监控

**文件加载**:
```java
@Service
public class PasswordFileLoader {

    public PasswordFile load(String filePath) {
        // 1. 验证文件权限
        validatePermissions(filePath, 0o600);

        // 2. 读取文件内容
        String content = Files.readString(Paths.get(filePath));

        // 3. 解析JSON
        PasswordFile passwordFile = parseJson(content);

        // 4. 验证格式和版本
        validateFormat(passwordFile);

        return passwordFile;
    }

    public void reload(String filePath) {
        // 触发重新加载密码文件
    }
}
```

---

## 5. 框架集成

### 5.1 Spring Boot集成

**配置方式**:
```yaml
# application.yml
spring:
  datasource:
    druid:
      url: jdbc:mysql://192.168.1.100:3306/acdb_main
      username: app_user
      password: ${dbpwd:password:db001}  # 使用密码服务

  dbpwd:
    enabled: true
    password-file: /app/.dbpwd/db.pwd
    key-file: /app/.dbpwd/sm4.key
```

**自动配置类**:
```java
@Configuration
@EnableAutoConfiguration
@ConditionalOnProperty(name = "spring.dbpwd.enabled", havingValue = "true")
public class DbPwdAutoConfiguration {

    @Bean
    public PasswordService passwordService(
            @Value("${spring.dbpwd.password-file}") String passwordFile,
            @Value("${spring.dbpwd.key-file}") String keyFile) {
        return new PasswordServiceImpl(passwordFile, keyFile);
    }

    @Bean
    @ConditionalOnClass(DruidDataSource.class)
    public DruidWrap druidWrap(PasswordService passwordService) {
        return new DruidWrap(passwordService);
    }
}
```

### 5.2 MyBatis集成

**配置方式**:
```yaml
# application.yml
mybatis:
  datasource:
    db-id: db001  # 指定数据库ID

spring:
  dbpwd:
    enabled: true
    password-file: /app/.dbpwd/db.pwd
    key-file: /app/.dbpwd/sm4.key
```

**数据源构建器**:
```java
@Configuration
public class MyBatisDataSourceConfig {

    @Bean
    public DataSource mybatisDataSource(PasswordService passwordService) {
        DatabaseConfig config = passwordService.getConfig("db001");
        return DataSourceBuilder.create()
                .url(config.getJdbcUrl())
                .username(config.getUsername())
                .password(passwordService.getDecryptedPassword("db001"))
                .type(DruidDataSource.class)
                .build();
    }
}
```

### 5.3 使用示例

```java
@Service
public class UserService {

    @Autowired
    private PasswordService passwordService;

    public void connectToDatabase() {
        // 方式1: 获取解密后的密码
        String password = passwordService.getDecryptedPassword("db001");

        // 方式2: 获取完整数据库配置
        DatabaseConfig config = passwordService.getConfig("db001");

        // 方式3: 使用连接池包装
        DataSource dataSource = passwordService.wrapDataSource("db001", dataSourceConfig);

        // 使用数据源...
    }
}
```

---

## 6. 连接池包装类

### 6.1 包装类架构

```
ConnectionPoolWrapper (接口)
         ↑
         │ implements
         │
┌────────┴────────┐
│                 │
DruidWrap         HikariWrap    DBCP2Wrap
```

### 6.2 包装类接口定义

```java
public interface ConnectionPoolWrapper {

    /**
     * 包装现有数据源，使用密码服务自动处理密码
     */
    DataSource wrap(DataSource originalDataSource, String dbId);

    /**
     * 创建新数据源并配置密码
     */
    DataSource create(String dbId, DataSourceConfig config);

    /**
     * 刷新数据源密码
     */
    void refreshPassword(DataSource dataSource, String dbId);

    /**
     * 验证数据源配置
     */
    boolean validate(DataSource dataSource, String dbId);
}
```

### 6.3 Druid包装实现

```java
public class DruidWrap implements ConnectionPoolWrapper {

    private final PasswordService passwordService;

    public DruidWrap(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    @Override
    public DataSource wrap(DataSource originalDataSource, String dbId) {
        if (!(originalDataSource instanceof DruidDataSource)) {
            throw new IllegalArgumentException("Expected DruidDataSource");
        }

        DruidDataSource druid = (DruidDataSource) originalDataSource;
        String password = passwordService.getDecryptedPassword(dbId);
        druid.setPassword(password);

        return druid;
    }

    @Override
    public DataSource create(String dbId, DataSourceConfig config) {
        DatabaseConfig dbConfig = passwordService.getConfig(dbId);

        DruidDataSource druid = new DruidDataSource();
        druid.setUrl(dbConfig.getJdbcUrl());
        druid.setUsername(dbConfig.getUsername());
        druid.setPassword(passwordService.getDecryptedPassword(dbId));

        // 设置其他连接池属性
        druid.setInitialSize(config.getInitialSize());
        druid.setMinIdle(config.getMinIdle());
        druid.setMaxActive(config.getMaxActive());

        return druid;
    }

    @Override
    public void refreshPassword(DataSource dataSource, String dbId) {
        if (!(dataSource instanceof DruidDataSource)) {
            throw new IllegalArgumentException("Expected DruidDataSource");
        }

        DruidDataSource druid = (DruidDataSource) dataSource;
        String newPassword = passwordService.getDecryptedPassword(dbId);
        druid.setPassword(newPassword);
    }

    @Override
    public boolean validate(DataSource dataSource, String dbId) {
        if (!(dataSource instanceof DruidDataSource)) {
            return false;
        }

        try {
            DruidDataSource druid = (DruidDataSource) dataSource;
            return druid.getConnection().isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }
}
```

### 6.4 HikariCP包装实现

```java
public class HikariWrap implements ConnectionPoolWrapper {

    private final PasswordService passwordService;

    public HikariWrap(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    @Override
    public DataSource wrap(DataSource originalDataSource, String dbId) {
        if (!(originalDataSource instanceof HikariDataSource)) {
            throw new IllegalArgumentException("Expected HikariDataSource");
        }

        HikariConfig config = ((HikariDataSource) originalDataSource).getHikariConfigMXBean().getHikariConfig();
        DatabaseConfig dbConfig = passwordService.getConfig(dbId);

        config.setUsername(dbConfig.getUsername());
        config.setPassword(passwordService.getDecryptedPassword(dbId));

        return new HikariDataSource(config);
    }

    @Override
    public DataSource create(String dbId, DataSourceConfig config) {
        DatabaseConfig dbConfig = passwordService.getConfig(dbId);

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(dbConfig.getJdbcUrl());
        hikariConfig.setUsername(dbConfig.getUsername());
        hikariConfig.setPassword(passwordService.getDecryptedPassword(dbId));

        // 连接池属性
        hikariConfig.setMinimumIdle(config.getMinIdle());
        hikariConfig.setMaximumPoolSize(config.getMaxActive());

        return new HikariDataSource(hikariConfig);
    }

    @Override
    public void refreshPassword(DataSource dataSource, String dbId) {
        // HikariCP不支持动态修改密码，需要重建数据源
        HikariConfig config = ((HikariDataSource) dataSource).getHikariConfigMXBean().getHikariConfig();
        config.setPassword(passwordService.getDecryptedPassword(dbId));
    }

    @Override
    public boolean validate(DataSource dataSource, String dbId) {
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            return false;
        }
    }
}
```

### 6.5 使用示例

```java
@Configuration
public class DataSourceConfig {

    @Autowired
    private PasswordService passwordService;

    @Bean
    public DruidWrap druidWrap() {
        return new DruidWrap(passwordService);
    }

    @Bean
    public DataSource primaryDataSource(DruidWrap druidWrap) {
        DataSourceConfig config = new DataSourceConfig();
        config.setInitialSize(5);
        config.setMinIdle(5);
        config.setMaxActive(20);

        return druidWrap.create("db001", config);
    }
}
```

---

## 7. 安全机制设计

### 7.1 SSH vs SM4密钥机制对比

| 对比项 | SSH私钥 | SM4密钥 | 建议 |
|-------|---------|---------|------|
| **密钥长度** | 2048/4096位（RSA/ECDSA） | 128位（SM4） | SM4满足对称加密需求 |
| **密钥类型** | 非对称（一对） | 对称（单个） | 符合使用场景 |
| **文件权限** | 600或400 | 600 | 保持一致 |
| **密钥保护** | 密码短语可选 | 密码文件独立存储 | 可选增加密码短语 |
| **密钥分发** | 公钥公开，私钥保密 | 密钥不通过网络传输 | 保持安全 |
| **密钥轮换** | 生成新密钥对 | 生成新密钥并重新加密 | 记录轮换历史 |
| **备份策略** | 安全离线备份 | 安全离线备份 | 多级备份 |
| **使用审计** | 日志记录公钥使用 | 日志记录解密操作 | 全程审计 |

### 7.2 安全增强建议

#### 7.2.1 密钥保护机制增强

**1. 密钥密码短语（可选）**
```
# sm4.key 支持密码短语保护
~/.dbpwd/sm4.key (加密存储，使用密码短语解锁)
~/.dbpwd/sm4.key.passphrase (可选，文件存储密码短语)
```

**2. 密钥拆分（Shamir秘密共享）**
```
# 将SM4密钥拆分为N份，需要M份才能重构
~/.dbpwd/sm4.key.shard1 (管理员1持有)
~/.dbpwd/sm4.key.shard2 (管理员2持有)
~/.dbpwd/sm4.key.shard3 (备份持有)
```

**3. 硬件安全模块（HSM）集成（未来）**
```
# 将SM4密钥存储在HSM中，密码文件使用HSM加密
```

#### 7.2.2 文件权限控制

**Linux/Unix权限设置**:
```bash
# 设置600权限
chmod 600 ~/.dbpwd/sm4.key
chmod 600 ~/.dbpwd/db.pwd

# 设置所有者
chown appuser:appgroup ~/.dbpwd/sm4.key
chown appuser:appgroup ~/.dbpwd/db.pwd

# 验证权限
ls -l ~/.dbpwd/
```

**容器部署权限设置**:
```yaml
# docker-compose.yml
volumes:
  - /host/.dbpwd:/app/.dbpwd
command: >
  sh -c "chmod 600 /app/.dbpwd/* && ls -la /app/.dbpwd/"
```

**NAS权限设置**:
```bash
# NAS宿主机上设置权限
chmod 600 /nas/volume/app/.dbpwd/sm4.key
chmod 600 /nas/volume/app/.dbpwd/db.pwd

# 确保NAS挂载时不改变权限
mount -t nfs nas-server:/volume/app/.dbpwd /app/.dbpwd -o rw,nosuid,nodev,noexec,noatime
```

#### 7.2.3 审计日志

**审计字段**:
```json
{
  "timestamp": "2026-02-08T10:30:45Z",
  "action": "decrypt",
  "dbId": "db001",
  "userId": "appuser",
  "sourceIp": "192.168.1.50",
  "result": "success",
  "details": "Password decrypted for database connection"
}
```

#### 7.2.4 密钥轮换流程

```
1. 系统经理生成新SM4密钥
   ↓
2. 使用新密钥重新加密所有密码
   ↓
3. 备份旧密钥（安全离线存储）
   ↓
4. 部署新密钥文件到生产环境
   ↓
5. 应用重新加载并验证
   ↓
6. 确认无误后安全删除旧密钥
```

### 7.3 安全合规建议

| 合规项 | 建议措施 |
|-------|---------|
| **密钥存储** | 600权限，独立文件存储 |
| **密码轮换** | 每90天至少一次 |
| **密钥轮换** | 每年至少一次 |
| **审计日志** | 保留至少180天 |
| **备份策略** | 三地备份（生产、同城、异地） |
| **访问控制** | 最小权限原则，职责分离 |
| **传输安全** | 本地文件存储，不通过网络传输密钥 |

---

## 8. 开发计划

### 8.1 开发阶段划分

#### 阶段1: 核心功能开发（2周）

**Sprint 1.1: 密码服务核心（3天）**
- [ ] SM4加解密服务实现
- [ ] 密钥文件管理服务
- [ ] 密码文件JSON解析服务
- [ ] 密码字段格式解析（ENC格式）
- [ ] 单元测试（85%覆盖率）

**Sprint 1.2: 文件权限验证（1天）**
- [ ] 文件权限检查工具
- [ ] 容器环境权限验证
- [ ] NAS环境权限验证
- [ ] 集成测试

**Sprint 1.3: 连接池包装类（2天）**
- [ ] ConnectionPoolWrapper接口定义
- [ ] DruidWrap实现
- [ ] HikariWrap实现
- [ ] DBCP2Wrap实现（可选）
- [ ] 单元测试

#### 阶段2: 命令行工具开发（1.5周）

**Sprint 2.1: 下载和对比功能（2天）**
- [ ] 下载远端文件功能
- [ ] 更新时间对比逻辑
- [ ] 文件完整性验证
- [ ] 集成测试

**Sprint 2.2: 加密解密功能（2天）**
- [ ] 密码加密命令
- [ ] 密码解密命令
- [ ] 文件批量处理
- [ ] 交互式输入（隐藏密码）
- [ ] 单单元测试

**Sprint 2.3: 测试和管理功能（2天）**
- [ ] 数据库连接测试
- [ ] 数据库配置管理
- [ ] 列表和查询功能
- [ ] 密钥文件验证工具
- [ ] 集成测试

#### 阶段3: 框架集成开发（1周）

**Sprint 3.1: Spring Boot集成（2天）**
- [ ] 自动配置类
- [ ] 配置属性绑定
- [ ] 条件化配置逻辑
- [ ] 集成测试

**Sprint 3.2: MyBatis集成（1天）**
- [ ] MyBatis数据源构建器
- [ ] 数据库ID映射逻辑
- [ ] 集成测试

**Sprint 3.3: 文档和示例（2天）**
- [ ] 用户使用文档
- [ ] 示例项目
- [ ] 故障排查指南

#### 阶段4: 测试和优化（1周）

**Sprint 4.1: 集成测试（2天）**
- [ ] 完整功能集成测试
- [ ] 端到端测试
- [ ] 性能测试

**Sprint 4.2: 安全测试（2天）**
- [ ] 渗透测试
- [ ] 权限验证测试
- [ ] 密钥安全测试

**Sprint 4.3: 文档和发布（1天）**
- [ ] API文档完善
- [ ] 用户手册
- [ ] 运维文档
- [ ] 发版准备

### 8.2 里程碑

| 里程碑 | 日期 | 交付物 |
|-------|------|-------|
| M1: 核心功能完成 | Sprint 1.3 | SM4服务、连接池包装 |
| M2: 命令行工具完成 | Sprint 2.3 | dbpwd-cli工具 |
| M3: 框架集成完成 | Sprint 3.2 | Spring Boot/MyBatis集成 |
| M4: 测试完成 | Sprint 4.2 | 测试报告 |
| M5: 发布准备 | Sprint 4.3 | 完整文档、发版 |

### 8.3 技术栈

| 组件 | 技术选择 |
|-----|---------|
| **核心语言** | Java 11+ |
| **加密库** | Bouncy Castle SM4实现 |
| **JSON库** | Jackson |
| **命令行框架** | Picocli |
| **测试框架** | JUnit 5 + Mockito |
| **构建工具** | Maven 3.6+ |
| **文档工具** | Javadoc + MkDocs |

### 8.4 风险和应对

| 风险 | 等级 | 应对措施 |
|-----|------|---------|
| SM4加密性能 | 中 | 预缓存解密结果，异步加解密 |
| 容器权限问题 | 高 | 提前验证不同容器环境 |
| 连接池兼容性 | 中 | 重点测试主流连接池 |
| 密钥安全泄露 | 高 | 严格权限控制，审计日志 |

---

## 设计决策总结

| 决策点 | 选项 | 选择理由 |
|-------|------|---------|
| 密码文件格式 | JSON格式 | 可读性强，易扩展，工具友好 |
| 密码字段格式 | Druid ENC格式 | 兼容性强，可扩展其他算法 |
| 配置中心使用 | 仅存储路径 | 简化架构，无需加密传输 |
| 刷新模式 | 线下评审+手工更新 | 安全可控，职责清晰 |
| 密钥存储位置 | ~/.dbpwd/sm4.key | 标准化路径，权限可控 |
| 密钥保护 | 600权限，可选密码短语 | 参考SSH机制，增强安全 |
| 容器部署 | 外挂文件600权限 | 灵活部署，安全可控 |

---

> 文档版本: v1.0
> 最后更新: 2026-02-08
> 状态: 设计阶段
