# 连接池包装类

> 文档版本: v3.0
最后更新: 2026-02-08

## 包装类架构

```
ConnectionPoolWrapper (接口)
         ↑
         │ implements
         │
┌────────┴────────┐
│                 │
DruidWrap         HikariWrap    DBCP2Wrap
```

## 包装类接口定义

```java
/**
 * 连接池包装接口
 *
 * @see DruidWrap
 * @see HikariWrap
 */
public interface ConnectionPoolWrapper {

    /**
     * 包装现有数据源，使用密码服务自动处理密码
     *
     * @param originalDataSource 原始数据源
     * @param dbId 数据库ID
     * @return 包装后的数据源
     * @throws Exception 包装失败
     */
    DataSource wrap(DataSource originalDataSource, String dbId) throws Exception;

    /**
     * 创建新数据源并配置密码
     *
     * @param dbId 数据库ID
     * @param config 数据源配置
     * @return 数据源
     * @throws Exception 创建失败
     */
    DataSource create(String dbId, DataSourceConfig config) throws Exception;

    /**
     * 刷新数据源密码
     *
     * @param dataSource 数据源
     * @param dbId 数据库ID
     * @throws Exception 刷新失败
     */
    void refreshPassword(DataSource dataSource, String dbId) throws Exception;

    /**
     * 验证数据源配置
     *
     * @param dataSource 数据源
     * @param dbId 数据库ID
     * @return 验证结果
     */
    boolean validate(DataSource dataSource, String dbId) throws Exception;
}
```

### DataSourceConfig

```java
/**
 * 数据源配置
 */
public class DataSourceConfig {
    private int initialSize = 5;
    private int minIdle = 5;
    private int maxActive = 20;
    private long maxWait = 60000;
    private String validationQuery;
    private boolean testWhileIdle = true;
    private int timeBetweenEvictionRuns = 60000;

    // 使用连接池类型
    private String poolType;

    // 可选的其他配置
    private boolean poolPreparedStatements;
    private int maxPoolPreparedStatementPerConnectionSize;

    // getters and setters
}
```

## Druid包装实现

```java
/**
 * Druid连接池包装实现
 *
 * 支持Druid数据源的密码自动处理
 */
public class DruidWrap implements ConnectionPoolWrapper {

    private static final Logger logger = Logger.getLogger(DruidWrap.class.getName());
    private final PasswordService passwordService;

    public DruidWrap(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    @Override
    public DataSource wrap(DataSource originalDataSource, String dbId) throws Exception {
        if (!(originalDataSource instanceof DruidDataSource)) {
            throw new IllegalArgumentException("Expected DruidDataSource, got: " +
                originalDataSource.getClass().getName());
        }

        DruidDataSource druid = (DruidDataSource) originalDataSource;
        String password = passwordService.getDecryptedPassword(dbId);
        druid.setPassword(password);

        logger.info("已更新Druid数据源密码: " + dbId);
        return druid;
    }

    public DataSource create(String dbId, DataSourceConfig config) throws Exception {
        DatabaseEntry dbEntry = passwordService.getConfig(dbId);
        if (dbEntry == null) {
            throw new IllegalArgumentException("未找到数据库配置: " + dbId);
        }

        DruidDataSource druid = new DruidDataSource();
        druid.setUrl(dbEntry.getJdbcUrl());
        druid.setUsername(dbEntry.getUsername());
        druid.setPassword(passwordService.getDecryptedPassword(dbId));

        // 连接池参数
        druid.setInitialSize(config.getInitialSize());
        druid.setMinIdle(config.getMinIdle());
        druid.setMaxActive(config.getMaxActive());
        druid.setMaxWait(config.getMaxWait());

        if (config.getValidationQuery() != null) {
            druid.setValidationQuery(config.getValidationQuery());
        } else {
            // 根据数据库类型设置默认验证查询
            druid.setValidationQuery(getDefaultValidationQuery(dbEntry.getType()));
        }

        druid.setTestWhileIdle(config.isTestWhileIdle());
        druid.setTimeBetweenEvictionRuns(config.getTimeBetweenEvictionRuns());

        logger.info("已创建Druid数据源: " + dbId);
        return druid;
    }

    public void refreshPassword(DataSource dataSource, String dbId) throws Exception {
        if (!(dataSource instanceof DruidDataSource)) {
            throw new IllegalArgumentException("Expected DruidDataSource");
        }

        DruidDataSource druid = (DruidDataSource) dataSource;
        String newPassword = passwordService.getDecryptedPassword(dbId);
        druid.setPassword(newPassword);

        // 关闭活跃连接，强制重新连接
        closeConnections(druid);

        logger.info("已刷新Druid数据源密码: " + dbId);
    }

    public boolean validate(DataSource dataSource, String dbId) throws Exception {
        if (!(dataSource instanceof DruidDataSource)) {
            return false;
        }

        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            logger.warning("验证数据源失败: " + dbId + ", 错误: " + e.getMessage());
            return false;
        }
    }

    private void closeConnections(DruidDataSource dataSource) throws Exception {
        try {
            dataSource.close();
        } catch (Exception e) {
            logger.warning("关闭连接失败: " + e.getMessage());
        }
    }

    private String getDefaultValidationQuery(String dbType) {
        if (dbType == null) {
            return "SELECT 1";
        }
        switch (dbType.toLowerCase()) {
            case "mysql":
                return "SELECT 1";
            case "postgresql":
            case "oracle":
            case "sqlserver":
                return "SELECT 1";
            case "db2":
                return "SELECT 1 FROM sysibm.sysdummy1";
            case "sybase":
                return "SELECT 1";
            case "h2":
                return "SELECT 1";
            default:
                return "SELECT 1";
        }
    }
}
```

## HikariCP包装实现

```java
/**
 * HikariCP连接池包装实现
 *
 * 支持HikariCP数据源的密码自动处理
 * 注意：HikariCP不支持动态修改密码，需要重建数据源
 */
public class HikariWrap implements ConnectionPoolWrapper {

    private final Logger logger = Logger.getLogger(HikariWrap.class.getName());
    private final PasswordService passwordService;

    public HikariWrap(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    public DataSource wrap(DataSource originalDataSource, String dbId) throws Exception {
        if (!(originalDataSource instanceof HikariDataSource)) {
            throw new IllegalArgumentException("Expected HikariDataSource");
        }

        // HikariCP不支持动态修改配置，需要重建数据源
        DatabaseEntry dbEntry = passwordService.getConfig(dbId);
        if (dbEntry == null) {
            throw new IllegalArgumentException("未找到数据库配置: " + dbId);
        }

        HikariDataSource original = (HikariDataSource) originalDataSource;
        HikariConfig config = original.getHikariConfigMXBean().getHikariConfig();

        // 更新密码
        config.setUsername(dbEntry.getUsername());
        config.setPassword(passwordService.getDecryptedPassword(dbId));

        logger.info("已更新HikariCP数据源密码: " + dbId);
        return new HikariDataSource(config);
    }

    public DataSource create(String dbId, DataSourceConfig config) throws Exception {
        DatabaseEntry dbEntry = passwordService.getConfig(dbId);
        if (dbEntry == null) {
            throw new IllegalArgumentException("未找到数据库配置: " + dbId);
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(dbEntry.getJdbcUrl());
        hikariConfig.setUsername(dbEntry.getUsername());
        hikariConfig.setPassword(passwordService.getDecryptedPassword(dbId));

        // 连接池属性
        hikariConfig.setMinimumIdle(config.getMinIdle());
        hikariConfig.setMaximumPoolSize(config.getMaxActive());
        hikariConfig.setConnectionTimeout((int) config.getMaxWait());
        hikariConfig.setIdleTimeout(config.getTimeBetweenEvictionRuns());
        hikariConfig.setAutoCommit(true);

        if (config.getValidationQuery() != null) {
            hikariConfig.setConnectionTestQuery(config.getValidationQuery());
        } else {
            // HikariCP默认没有验证查询，需要显式设置
            hikariConfig.setConnectionTestQuery("SELECT 1");
        }

        logger.info("已创建HikariCP数据源: " + dbId);
        return new HikariDataSource(hikariConfig);
    }

    public void refreshPassword(DataSource dataSource, String dbId) throws Exception {
        // HikariCP不支持动态修改密码，需要重建数据源
        DatabaseEntry dbEntry = passwordService.getConfig(dbId);
        if (dbEntry == null) {
            throw new IllegalArgumentException("未找到数据库配置: " + dbId);
        }

        if (!(dataSource instanceof HikariDataSource)) {
            throw new IllegalArgumentException("Expected HikariDataSource");
        }

        HikariDataSource hikari = (HikariDataSource) dataSource;
        HikariConfig config = hikari.getHikariConfigMXBean().getHikariConfig();
        config.setPassword(passwordService.getDecryptedPassword(dbId));

        // 关闭原数据源
        hikari.close();

        logger.info("已刷新HikariCP数据源密码（需重建数据源）: " + dbId);
    }

    public boolean validate(DataSource dataSource, String dbId) throws Exception {
        if (!(dataSource instanceof HikariDataSource)) {
            return false;
        }

        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            logger.warning("验证数据源失败: " + dbId + ", 错误: " + e.getMessage());
            return false;
        }
    }
}
```

## 使用示例

### Spring配置

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
    public HikariWrap hikariWrap() {
        return new HikariWrap(passwordService);
    }

    @Bean
    @Primary
    public DataSource primaryDataSource(DruidWrap druidWrap) {
        DataSourceConfig config = new DataSourceConfig();
        config.setInitialSize(5);
        config.setMinIdle(5);
        config.setMaxActive(20);
        config.setMaxWait(60000);
        config.setTestWhileIdle(true);
        config.setTimeBetweenEvictionRuns(60000);

        return druidWrap.create("db001", config);
    }

    @Bean
    public DataSource secondaryDataSource(HikariWrap hikariWrap) {
        DataSourceConfig config = new DataSourceConfig();
        config.setInitialSize(3);
        config.setMinIdle(2);
        config.setMaxActive(10);

        return hikariWrap.create("db002", config);
    }
}
```

### 使用包装类

```java
@Service
public class UserService {

    @Autowired
    private PasswordService passwordService;

    public void connectToDatabase() {
        try {
            // 方式1: 获取解密后的密码
            String password = passwordService.getDecryptedPassword("db001");

            // 方式2: 使用连接池包装
            DataSource dataSource = passwordService.createDataSource("db001", new DataSourceConfig());

            Connection conn = dataSource.getConnection();
            // ...
        } catch (Exception e) {
            throw new RuntimeException("数据库连接失败", e);
        }
    }
}
```

## 连接池特性对比

| 特性 | Druid | HikariCP | 说明 |
|------|-------|----------|------|
| **密码刷新** | ✅ 支持 | ⚠️ 需重建 | Druid支持动态刷新密码 |
| **性能** | 通用 | 高 | HikariCP性能更优 |
| **功能** | 丰富 | 基础 | Druid功能更全面 |
| **配置简单** | 中 | 简单 | HikariCP配置更简单 |

## 相关文档

- [框架集成](framework-integration.md) - Spring/MyBatis集成
- [安全设计](security.md) - 审计日志和密钥轮换
- [技术栈](../overview.md#技术栈) - 依赖说明
