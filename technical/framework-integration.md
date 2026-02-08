# 框架集成

> 文档版本: v3.0
最后更新: 2026-02-08

## Spring Boot集成

### 配置方式

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
    connection:
      timeout: 5000
```

### 自动配置类

```java
@Configuration
@EnableAutoConfiguration
@ConditionalOnProperty(name = "dbpwd.enabled", havingValue = "true")
public class DbPwdAutoConfiguration {

    @Bean
    public PasswordService passwordService(
            @Value("${dbpwd.password-file:~/.dbpwd/db.pwd}") String passwordFile,
            @Value("${dbpwd.key-file:~/.dbpwd/sm4.key}") String keyFile) {
        return new PasswordServiceImpl(passwordFile, keyFile);
    }

    @Bean
    @ConditionalOnClass(DruidDataSource.class)
    public DruidWrap druidWrap(PasswordService passwordService) {
        return new DruidWrap(passwordService);
    }

    @Bean
    @ConditionalOnClass(HikariDataSource.class)
    public HikariWrap hikariWrap(PasswordService passwordService) {
        return new HikariWrap(passwordService);
    }
}
```

## MyBatis集成

### 配置方式

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

### 数据源构建器

```java
@Configuration
public class MyBatisDataSourceConfig {

    private final PasswordService passwordService;

    @Autowired
    public MyBatisDataSourceConfig(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    @Bean
    @Primary
    public DataSource mybatisDataSource() {
        DatabaseConfig config = passwordService.getConfig("db001");
        return DataSourceBuilder.create()
                .url(config.getJdbcUrl())
                .username(config.getUsername())
                .password(passwordService.getDecryptedPassword("db001"))
                .type(com.alibaba.druid.pool.DruidDataSource.class)
                .build();
    }
}
```

## PasswordService接口定义

```java
/**
 * 密码服务接口
 *
 * 统一的数据库密码管理接口
 */
public interface PasswordService {

    /**
     * 获取解密后的密码
     *
     * @param dbId 数据库ID
     * @return 明文密码
     * @throws Exception 解密失败
     */
    String getDecryptedPassword(String dbId) throws Exception;

    /**
     * 获取数据库配置
     *
     * @param dbId 数据库ID
     * @return 数据库配置
     */
    DatabaseEntry getConfig(String dbId);

    /**
     * 包装数据源并替换密码
     *
     * @param dataSource 原始数据源
     * @param dbId 数据库ID
     * @return 包装后的数据源
     * @throws Exception 包装失败
     */
    DataSource wrapDataSource(DataSource dataSource, String dbId) throws Exception;

    /**
     * 创建数据源
     *
     * @param dbId 数据库ID
     * @param config 数据源配置
     * @return 数据源
     * @throws Exception 创建失败
     */
    DataSource createDataSource(String dbId, DataSourceConfig config) throws Exception;
}
```

## PasswordService实现

```java
/**
 * 密码服务实现
 *
 * 负责加解密数据库密码
 */
@Service
public class PasswordServiceImpl implements PasswordService {

    private static final Logger logger = Logger.getLogger(PasswordServiceImpl.class.getName());

    private final String sm4Key;
    private final String passwordFilePath;
    private PasswordFileManager passwordFileManager;

    public PasswordServiceImpl(String passwordFilePath, String keyFile) throws Exception {
        this.passwordFilePath = passwordFilePath;

        // 加载SM4密钥
        Sm4KeyLoader keyLoader = new Sm4KeyLoader(keyFile);
        this.sm4Key = keyLoader.loadKey();

        // 加载密码文件
        this.passwordFileManager = PasswordFileManager.loadFromFile(passwordFilePath);

        logger.info("密码服务初始化完成，密钥文件: " + keyFile + "，密码文件: " + passwordFilePath);
    }

    @Override
    public String getDecryptedPassword(String dbId) throws Exception {
        DatabaseEntry entry = passwordFileManager.getById(dbId);
        if (entry == null) {
            throw new IllegalArgumentException("未找到数据库配置: " + dbId);
        }

        String password = entry.getPassword();
        if (ENCFormat.isEncrypted(password)) {
            return PasswordDecryptor.decrypt(password, sm4Key);
        }

        return password;
    }

    @Override
    public DatabaseEntry getConfig(String dbId) {
        return passwordFileManager.getById(dbId);
    }

    @Override
    public DataSource wrapDataSource(DataSource dataSource, String dbId) throws Exception {
        if (dataSource instanceof DruidDataSource) {
            DruidWrap wrap = new DruidWrap(this);
            return wrap.wrap(dataSource, dbId);
        } else if (dataSource instanceof HikariDataSource) {
            HikariWrap wrap = new HikariWrap(this);
            return wrap.wrap(dataSource, dbId);
        } else {
            throw new IllegalArgumentException("不支持的数据源类型: " +
                dataSource.getClass().getName());
        }
    }

    @Override
    public DataSource createDataSource(String dbId, DataSourceConfig config) throws Exception {
        DatabaseEntry entry = getConfig(dbId);
        String password = getDecryptedPassword(dbId);

        // 根据配置的type选择合适的连接池
        String poolType = config.getPoolType() != null ?
                              config.getPoolType() : "druid";

        if ("druid".equalsIgnoreCase(poolType)) {
            DruidDataSource dataSource = new DruidDataSource();
            dataSource.setUrl(entry.getJdbcUrl());
            dataSource.setUsername(entry.getUsername());
            dataSource.setPassword(password);
            return dataSource;
        } else if ("hikari".equalsIgnoreCase(poolType)) {
            HikariConfig configClass = new HikariConfig();
            configClass.setJdbcUrl(entry.getJdbcUrl());
            configClass.setUsername(entry.getUsername());
            configClass.setPassword(password);
            return new HikariDataSource(configClass);
        } else {
            throw new IllegalArgumentException("不支持的连接池类型: " + poolType);
        }
    }
}
```

## 使用示例

### 在Service中使用密码服务

```java
@Service
public class UserService {

    @Autowired
    private PasswordService passwordService;

    public void connectToDatabase() {
        try {
            // 方式1: 获取解密后的密码
            String password = passwordService.getDecryptedPassword("db001");

            // 方式2: 获取完整数据库配置
            DatabaseConfig config = passwordService.getConfig("db001");
            String dbType = config.getType();
            String jdbcUrl = config.getJdbcUrl();
            String username = config.getUsername();

            // 方式3: 使用连接池包装
            DataSource dataSource = passwordService.createDataSource("db001", new DataSourceConfig());

            // 使用数据源...
            Connection conn = dataSource.getConnection();
            // ...
        } catch (Exception e) {
            throw new RuntimeException("数据库连接失败", e);
        }
    }

    public void updateDatabasePassword(String dbId, String newPassword) throws Exception {
        DatabaseEntry entry = passwordService.getConfig(dbId);
        if (entry == null) {
            throw new IllegalArgumentException("未找到数据库配置: " + dbId);
        }

        // 加密新密码
        String encryptedPassword = PasswordEncryptor.encryptSM4ENC(newPassword, sm4Key);
        entry.setPassword(encryptedPassword);
        entry.setEncrypted(true);
        entry.touchUpdateTime();

        // 保存
        passwordFileManager.updateDatabase(entry);
        passwordFileManager.save();

        logger.info("数据库密码已更新: " + dbId);
    }
}
```

### 多数据源配置

```java
@Configuration
public class MultiDataSourceConfig {

    @Autowired
    private PasswordService passwordService;

    @Bean
    @Primary
    public DataSource primaryDataSource() {
        DataSourceConfig config = new DataSourceConfig();
        return passwordService.createDataSource("db001", config);
    }

    @Bean
    public DataSource secondaryDataSource() {
        DataSourceConfig config = new DataSourceConfig();
        config.setPoolType("hikari");
        return passwordService.createDataSource("db002", config);
    }
}
```

## 框架集成最佳实践

### 1. 密钥配置

**推荐方式**: 将密钥文件分离存储，设置600权限

```yaml
# 密钥文件配置
dbpwd:
  key-file: /secure/.dbpwd/sm4.key
```

### 2. 配置中心集成

**推荐**: 应用只使用本地文件，通过独立工具同步

```yaml
# 应用配置：本地路径
dbpwd:
  enabled: true
  password-file: /app/.dbpwd/db.pwd
  key-file: /app/.dbpwd/sm4.key

# 同步工具：配置中心
config-center:
  url: http://config-center.com
  api-key: ${CONFIG_CENTER_API_KEY}
  environment: ${APP_ENV:prod}
```

### 3. 连接池配置

**推荐默认值**:
```yaml
spring:
  datasource:
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 60000
      test-while-idle: true
      time-between-eviction-runs: 60000
```

### 4. 错误处理

**推荐**: 添加适当的异常处理

```java
try {
    PasswordService passwordService = new PasswordServiceImpl(file, key);
    // 使用密码服务...
} catch (InvalidKeyException e) {
    logger.error("密钥文件无效", e);
    throw new IllegalStateException("密钥配置错误");
} catch (IOException e) {
    logger.error("加载密码文件失败", e);
    throw new IllegalStateException("密码文件不存在或格式错误");
}
```

## 相关文档

- [连接池包装类](connection-pool.md) - Druid/HikariCP包装实现
- [安全设计](security.md) - 审计日志和密钥轮换
- [密钥管理规范](../overview.md#sm4密钥管理) - 密钥文件格式
