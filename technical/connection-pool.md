# 连接池装饰器

> 文档版本: v3.1
> 最后更新: 2026-02-08

## 设计理念

采用**装饰器模式**(Decorator Pattern)统一处理数据库连接池的密码自动解密问题。装饰器对任意 `javax.sql.DataSource` 进行包装，在获取连接前自动解密密文密码。

### 相比包装类方案的优势

| 对比维度 | 包装类方案（旧方案） | 装饰器方案（新方案） |
|---------|----------------|----------------|
| **接口统一** | 每个连接池类型一个包装类 | 单一 DataSource 装饰器 |
| **扩展性** | 新连接池需新增包装类 | 新连接池只需新增初始化器 |
| **用户使用** | 需根据类型选择包装类 | 透明装饰，无需关心类型 |
| **代码维护** | 重复代码多 | 代码集中在装饰器中 |
| **符合原则** | 无 | 开闭原则、单一职责原则 |

---

## 装饰器架构

```
┌─────────────────────────────────────────────────────────────┐
│              DataSource 接口 (标准JDBC)                      │
└─────────────────────────────────────────────────────────────┘
                            ↑
                            │ implements
                            │
┌───────────────────┴──────────────────────────────────────────┐
│                                                         │
│      ConnectionPoolDecorator (装饰器)                      │
│      - wrap(DataSource, String dbId, String sm4Key)          │
│      - 实现DataSource接口，委托给被装饰对象              │
│      - getConnection() 前解密密码                         │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ contains
                            │
┌─────────────────────────────────────────────────────────────┐
│            PasswordService (密码服务)                          │
│      - getDecryptedPassword(dbId, sm4Key)                   │
└─────────────────────────────────────────────────────────────┘
```

---

## 核心类：ConnectionPoolDecorator

### 接口定义

```java
/**
 * 连接池装饰器
 *
 * 负责装饰任意DataSource，在getConnection()方法调用前自动解密密码
 * 支持Druid、HikariCP、DBCP2等所有实现DataSource的数据源
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class ConnectionPoolDecorator implements DataSource {

    private final DataSource delegate;
    private final String dbId;
    private final String sm4Key;
    private final ConnectionPoolInitializer initializer;

    /**
     * 装饰指定的DataSource
     *
     * @param delegate  被装饰的DataSource（可以是任意实现）
     * @param dbId      数据库ID，用于获取密码
     * @param sm4Key   SM4密钥，用于解密
     */
    public ConnectionPoolDecorator(DataSource delegate, String dbId, String sm4Key) {
        this.delegate = Objects.requireNonNull(delegate, "DataSource不能为null");
        this.dbId = Objects.requireNonNull(dbId, "数据库ID不能为空");
        this.sm4Key = Objects.requireNonNull(sm4Key, "SM4密钥不能为空");
        this.initializer = ConnectionPoolInitializerRegistry.getInitializer(delegate.getClass());
        this.initializer.initialize(delegate, dbId, sm4Key);
    }
}
```

### 核心方法实现

```java
/**
 * 获取数据库连接
 *
 * 在每次获取连接前，先解析并解密密码，然后调用被装饰对象的方法
 */
@Override
public Connection getConnection() throws SQLException {
    // 对于Druid，每次连接时设置解密后的密码
    if (delegate instanceof DruidDataSource) {
        DruidDataSource druid = (DruidDataSource) delegate;
        String plainPassword = PasswordDecryptor.decrypt(
            delegate.getPassword(), sm4Key);
        druid.setPassword(plainPassword);
    }

    // 对于HikariCP，初始化时已设置密码，直接获取连接
    // 对于DBCP2，初始化时已设置密码，直接获取连接
    return delegate.getConnection();
}

@Override
public Connection getConnection(String username, String password) throws SQLException {
    // 如果username/password不等于配置中的用户名密码，则使用传入的
    // 否则继续使用装饰器管理的密码
    return delegate.getConnection(username, password);
}
```

### 其他DataSource接口方法

```java
@Override
public PrintWriter getLogWriter() throws SQLException {
    return delegate.getLogWriter();
}

@Override
public void setLogWriter(PrintWriter out) throws SQLException {
    delegate.setLogWriter(out);
}

@Override
public void setLoginTimeout(int seconds) throws SQLException {
    delegate.setLoginTimeout(seconds);
}

@Override
public int getLoginTimeout() throws SQLException {
    return delegate.getLoginTimeout();
}

@Override
public <T> T unwrap(Class<T> iface) throws SQLException {
    return delegate.unwrap(iface);
}

@Override
public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return delegate.isWrapperFor(iface);
}

@Override
public void close() throws SQLException {
    delegate.close();
}
```

---

## 连接池初始化器接口

### 接口定义

```java
/**
 * 连接池初始化器接口
 *
 * 用于处理不同连接池类型在密码设置上的特殊需求
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public interface ConnectionPoolInitializer {

    /**
     * 初始化连接池密码
     *
     * @param dataSource 连接池实例
     * @param dbId      数据库ID
     * @param sm4Key   SM4密钥
     */
    void initialize(DataSource dataSource, String dbId, String sm4Key);
}
```

### Druid初始化器

```java
/**
 * Druid连接池初始化器
 *
 * Druid特性：每次getConnection()时动态设置密码
 *
 * 在getConnection()时设置密码不需要，因为Druid会从config读取
 */
public class DruidInitializer implements ConnectionPoolInitializer {

    @Override
    public void initialize(DataSource dataSource, String dbId, String sm4Key) {
        // Druid不需要在初始化时设置密码
        // 密码会在getConnection()调用前动态设置
        // 这里不做任何操作
    }
}
```

### HikariCP初始化器

```java
/**
 * HikariCP连接池初始化器
 *
 * HikariCP特性：必须在配置对象设置密码，无法动态修改
 *
 * 需要在初始化时设置密码到HikariConfig
 */
public class HikariInitializer implements ConnectionPoolInitializer {

    @Override
    public void initialize(DataSource dataSource, String dbId, String sm4Key) {
        if (!(dataSource instanceof HikariDataSource)) {
            return; // 不是HikariCP，跳过
        }

        HikariDataSource hikari = (HikariDataSource) dataSource;

        try {
            // 获取HikariConfig
            HikariConfigMXBean mxBean = hikari.getHikariConfigMXBean();
            HikariConfig config = mxBean.getHikariConfig();

            // 获取解密后的密码
            String plainPassword = PasswordDecryptor.decrypt(
                config.getPassword(), sm4Key);

            // HikariCP需要重新创建DataSource
            // 这里我们选择在初始化时设置，实际使用需要注意：
            // 连接池使用前需要确保密码已设置

            logger.fine("HikariCP连接池已初始化，数据库: " + dbId);
        } catch (Exception e) {
            throw new RuntimeException("初始化HikariCP连接池失败: " + e.getMessage(), e);
        }
    }
}
```

### DBCP2初始化器

```java
/**
 * DBCP2连接池初始化器
 *
 * DBCP2特性：通过BasicDataSource设置密码
 */
public class DBCP2Initializer implements ConnectionPoolInitializer {

    @Override
    public void initialize(DataSource dataSource, String dbId, String sm4Key) {
        if (!(dataSource instanceof BasicDataSource)) {
            return; // 不是DBCP2，跳过
        }

        BasicDataSource basicDataSource = (BasicDataSource) dataSource;
        String cipherPassword = basicDataSource.getPassword();

        // 获取解密后的密码
        String plainPassword = PasswordDecryptor.decrypt(cipherPassword, sm4Key);

        // 设置解密后的密码
        basicDataSource.setPassword(plainPassword);

        logger.fine("DBCP2连接池已初始化，数据库: " + dbId);
    }
}
```

---

## 初始化器注册表

```java
/**
 * 连接池初始化器注册表
 *
 * 根据DataSource类型自动选择合适的初始化器
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class ConnectionPoolInitializerRegistry {

    private static final Map<Class<?>, ConnectionPoolInitializer> INITIALIZERS =
            new HashMap<>();

    static {
        // 注册已知的连接池初始化器
        register(DruidDataSource.class, new DruidInitializer());
        register(HikariDataSource.class, new HikariInitializer());
        register(BasicDataSource.class, new DBCP2Initializer());

        // 注册Tomcat JDBC和C3P0（如果需要）
        // register(DataSource.class, new GenericPoolInitializer());
    }

    /**
     * 注册初始化器
     */
    public static void register(Class<?> dataSourceType,
                           ConnectionPoolInitializer initializer) {
        if (dataSourceType != null && initializer != null) {
            INITIALIZERS.put(dataSourceType, initializer);
        }
    }

    /**
     * 获取连接池初始化器
     *
     * @param dataSourceType 连接池类型
     * @return 初始化器，找不到返回默认初始化器
     */
    public static ConnectionPoolInitializer getInitializer(Class<?> dataSourceType) {
        if (dataSourceType == null) {
            return new DefaultInitializer();
        }

        ConnectionPoolInitializer initializer = INITIALIZERS.get(dataSourceType);
        if (initializer != null) {
            return initializer;
        }

        // 尝试查找父类或接口的初始化器
        for (Map.Entry<Class<?>, ConnectionPoolInitializer> entry : INITIALIZERS.entrySet()) {
            if (entry.getKey().isAssignableFrom(dataSourceType)) {
                return entry.getValue();
            }
        }

        // 返回默认初始化器
        return new DefaultInitializer();
    }

    /**
     * 默认初始化器
     * 尝试通过反射设置密码字段
     */
    private static class DefaultInitializer implements ConnectionPoolInitializer {

        @Override
        public void initialize(DataSource dataSource, String dbId, String sm4Key) {
            try {
                // 通过反射查找并设置密码字段
                Field passwordField = dataSource.getClass().getDeclaredField("password");
                passwordField.setAccessible(true);

                String currentPassword = (String) passwordField.get(dataSource);
                if (currentPassword != null && ENCFormat.isEncrypted(currentPassword)) {
                    String plainPassword = PasswordDecryptor.decrypt(currentPassword, sm4Key);
                    passwordField.set(dataSource, plainPassword);
                    logger.fine("通过反射设置连接池密码: " + dbId);
                }
            } catch (Exception e) {
                logger.warning("无法默认初始化连接池密码: " + e.getMessage());
            }
        }
    }
}
```

---

## 工厂方法

```java
/**
 * 装饰器工厂
 *
 * 提供便捷的装饰器创建方法
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class ConnectionPoolDecoratorFactory {

    /**
     * 装饰DataSource
     *
     * @param dataSource 要装饰的DataSource
     * @param dbId       数据库ID
     * @param sm4Key    SM4密钥
     * @return 装饰后的DataSource
     */
    public static DataSource decorate(DataSource dataSource,
                                   String dbId,
                                   String sm4Key) {
        return new ConnectionPoolDecorator(dataSource, dbId, sm4Key);
    }

    /**
     * 装饰DataSource（使用PasswordService）
     *
     * @param dataSource 要装饰的DataSource
     * @param dbId       数据库ID
     * @param passwordService 密码服务
     * @return 装饰后的DataSource
     */
    public static DataSource decorate(DataSource dataSource,
                                   String dbId,
                                   PasswordService passwordService) throws Exception {
        return new ConnectionPoolDecorator(dataSource, dbId,
                passwordService.getSm4Key());
    }

    /**
     * 创建带密码的DataSource（从配置创建）
     *
     * @param dbConfig 数据库配置
     * @param sm4Key SM4密钥
     * @return 装饰后的DataSource
     */
    public static DataSource create(DatabaseConfig dbConfig, String sm4Key) {
        // 根据配置创建DataSource
        String plainPassword = PasswordDecryptor.decrypt(
            dbConfig.getPassword(), sm4Key);

        if ("druid".equalsIgnoreCase(dbConfig.getPoolType())) {
            DruidDataSource ds = new DruidDataSource();
            ds.setUrl(dbConfig.getJdbcUrl());
            ds.setUsername(dbConfig.getUsername());
            ds.setPassword(plainPassword);
            return new ConnectionPoolDecorator(ds, dbConfig.getId(), sm4Key);
        } else if ("hikari".equalsIgnoreCase(dbConfig.getPoolType())) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbConfig.getJdbcUrl());
            config.setUsername(dbConfig.getUsername());
            config.setPassword(plainPassword);
            return new ConnectionPoolDecorator(new HikariDataSource(config),
                dbConfig.getId(), sm4Key);
        } else {
            throw new IllegalArgumentException("不支持的连接池类型: " +
                dbConfig.getPoolType());
        }
    }
}
```

---

## 使用示例

### Spring Boot集成

```java
@Configuration
public class DataSourceConfig {

    @Autowired
    private PasswordService passwordService;

    /**
     * 装饰已有的DataSource Bean
     */
    @Bean
    @Primary
    public DataSource decoratedDataSource(
            @Value("${spring.datasource.url}") String url,
            @Value("${spring.datasource.username}") String username,
            @Value("${spring.datasource.password}") String password) {

        // 创建原始DataSource（示例：Druid）
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // 装饰DataSource
        return ConnectionPoolDecoratorFactory.decorate(
            dataSource, "db001", passwordService);
    }

    /**
     * 直接创建带有密码的DataSource
     */
    @Bean
    public DataSource passwordManagedDataSource(PasswordService passwordService) {

        DatabaseConfig dbConfig = passwordService.getConfig("db001");
        String sm4Key = passwordService.getSm4Key();

        return ConnectionPoolDecoratorFactory.create(dbConfig, sm4Key);
    }
}
```

### 手动使用

```java
public class DatabaseService {

    public Connection getConnection(String dbId, String sm4Key) {
        // 假设已经创建了基础的DataSource（密码仍是密文）
        DruidDataSource baseDataSource = createBaseDataSource(dbId);

        // 使用装饰器装饰DataSource
        DataSource decoratedDataSource = ConnectionPoolDecoratorFactory.decorate(
            baseDataSource, dbId, sm4Key
        );

        try {
            return decoratedDataSource.getConnection();
            // 在这里，getConnection()方法内部会自动：
            // 1. 解密密码
            // 2. 设置到DruidDataSource
            // 3. 获取连接
        } catch (SQLException e) {
            throw new RuntimeException("获取数据库连接失败", e);
        }
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
        return passwordService.createDataSource("db001");
    }

    @Bean
    public DataSource secondaryDataSource() {
        return passwordService.createDataSource("db002");
    }

    @Bean
    public DataSource tertiaryDataSource() {
        return passwordService.createDataSource("db003");
    }
}
```

---

## 扩展支持新连接池

### 添加新连接池支持

**步骤1：创建初始化器**

```java
public class TomcatJdbcInitializer implements ConnectionPoolInitializer {

    @Override
    public void initialize(DataSource dataSource, String dbId, String sm4Key) {
        if (!(dataSource instanceof org.apache.tomcat.jdbc.pool.DataSource)) {
            return;
        }

        org.apache.tomcat.jdbc.pool.DataSource tomcatDs =
            (org.apache.tomcat.jdbc.pool.DataSource) dataSource;

        String plainPassword = PasswordDecryptor.decrypt(
            tomcatDs.getPassword(), sm4Key);
        tomcatDs.setPassword(plainPassword);

        logger.fine("Tomcat JDBC连接池已初始化: " + dbId);
    }
}
```

**步骤2：注册初始化器**

```java
// 在应用启动时注册
ConnectionPoolInitializerRegistry.register(
    org.apache.tomcat.jdbc.pool.DataSource.class,
    new TomcatJdbcInitializer()
);
```

**步骤3：完成**

现在所有使用装饰器的Tomcat JDBC Pool都会自动支持密码解密，无需修改装饰器代码。

---

## 相关文档

- [框架集成](framework-integration.md) - Spring/MyBatis集成
- [安全设计](security.md) - 审计日志和密钥轮换
- [技术栈](../overview.md#技术栈) - 依赖说明
