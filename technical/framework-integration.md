# 框架集成

> 文档版本: v3.1
> 最后更新: 2026-02-08

## 装饰器模式集成说明

本文档详细说明如何在 Spring Boot、MyBatis、Hibernate、JDBC 等框架中使用**连接池装饰器**来实现数据库密码的自动解密。

装饰器模式相比以前的包装类方案的优势：
- ✅ **统一接口**：所有连接池使用同一个 `ConnectionPoolDecorator`
- ✅ **扩展性强**：新增连接池支持只需新增初始化器注册
- ✅ **用户透明**：装饰过程对使用方完全透明
- ✅ **符合设计原则**：开闭原则、单一职责原则

---

## Spring Boot集成

### 配置方式

```yaml
# application.yml
spring:
  datasource:
    druid:
      url: jdbc:mysql://192.168.1.100:3306/acdb_main
      username: app_user
      password: ENC(SM4:AABBCCDD1234567890ABCDEF12345678)  # 密文密码
      driver-class-name: com.mysql.cj.jdbc.Driver
      # 其他Druid配置...
    # 或者使用HikariCP...

dbpwd:
  enabled: true
  password-file: ~/.dbpwd/db.pwd
  key-file: ~/.dbpwd/sm4.key
```

### 自动配置类（装饰器版本）

```java
@Configuration
@ConditionalOnProperty(name = "dbpwd.enabled", havingValue = "true")
public class ConnectionPoolDecoratorAutoConfiguration {

    /**
     * 密码服务Bean
     */
    @Bean
    public PasswordService passwordService(
            @Value("${dbpwd.password-file:~/.dbpwd/db.pwd}") String passwordFile,
            @Value("${dbpwd.key-file:~/.dbpwd/sm4.key}") String keyFile) {
        return new PasswordServiceImpl(passwordFile, keyFile);
    }

    /**
     * 装饰器工厂Bean
     */
    @Bean
    public ConnectionPoolDecoratorFactory connectionPoolDecoratorFactory() {
        return new ConnectionPoolDecoratorFactory();
    }

    /**
     * 支持动态刷新密码的注册表
     */
    @Bean
    public ConnectionPoolDecoratorRegistry connectionPoolDecoratorRegistry() {
        return new ConnectionPoolInitializerRegistry();
    }
}
```

### 装饰器自动装配Bean

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
    public DataSource passwordManagedDataSource(DataSource dataSource) {
        String dbId = extractDbIdFromConfig();
        String sm4Key = passwordService.getSm4Key();

        // 装饰DataSource
        return ConnectionPoolDecoratorFactory.decorate(dataSource, dbId, sm4Key);
    }

    /**
     * 直接创建带有解密密码的DataSource
     */
    @Bean
    public DataSource autoManagedDataSource() {
        DatabaseConfig dbConfig = passwordService.getConfig("db001");
        String sm4Key = passwordService.getSm4Key();
        return ConnectionPoolDecoratorFactory.create(dbConfig, sm4Key);
    }

    /**
     * 多数据源支持
     */
    @Bean
    @Primary
    public DataSource primaryDataSource() {
        return ConnectionPoolDecoratorFactory.create(
            passwordService.getConfig("db001"), passwordService.getSm4Key());
    }

    @Bean
    public DataSource secondaryDataSource() {
        return ConnectionPoolDecoratorFactory.create(
            passwordService.getConfig("db002"), passwordService.getSm4Key());
    }
}
```

### 编程式配置

```java
@Configuration
public class ProgrammaticDataSourceConfig {

    @Bean
    public DataSource programmaticallyDataSource(DataSource dataSource,
                                              PasswordService passwordService) {
        String dbId = "db001";
        String sm4Key = passwordService.getSm4Key();

        // 判断密码是否为加密格式，否则无需装饰
        if (PasswordEncryptor.isEncrypted(dataSource.getPassword())) {
            return ConnectionPoolDecoratorFactory.decorate(dataSource, dbId, sm4Key);
        } else {
            return dataSource; // 明文密码，无需装饰
        }
    }
}
```

---

## MyBatis集成

### SqlSessionFactory集成（装饰器集成）

```java
@Configuration
@MapperScan("com.example.mapper")
public class MyBatisConfig {

    @Autowired
    private PasswordService passwordService;

    // 自动注入装饰后的DataSource
    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
        // 如果dataSource密码为密文，装饰它
        if (isEncryptedPassword(dataSource.getPassword())) {
            String dbId = extractDbIdFromUrl(dataSource.getUrl());
            String sm4Key = passwordService.getSm4Key();
            dataSource = ConnectionPoolDecoratorFactory.decorate(dataSource, dbId, sm4Key);
        }

        SqlSessionFactoryBean factoryBean = new SqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setTypeAliasesPackage("com.example.entity");
        return factoryBean.getObject();
    }

    private boolean isEncryptedPassword(String password) {
        return password != null && password.startsWith("ENC(SM4:");
    }
}
```

### SqlSessionTemplate集成

```java
@Component
public class MyBatisService {

    @Autowired
    private SqlSessionTemplate sqlSessionTemplate;

    /**
     * 执行查询
     */
    public List<Map<String, Object>> query(String sql) {
        return sqlSessionTemplate.queryForList(sql, new HashMap<>());
    }

    /**
     * 执行更新
     */
    public int update(String sql) {
        return sqlSessionTemplate.update(sql);
    }

    /**
     * 使用连接获取原生Connection执行操作
     */
    public void executeWithConnection(ConnectionCallback<Void> callback) throws Exception {
        sqlSessionTemplate.execute(callback);
    }
}
```

---

## Spring Data JPA集成

### LocalContainerEntityManagerFactoryBean集成

```java
@Configuration
@EnableJpaRepositories(basePackages = "com.example.repository")
public class JpaConfig {

    @Autowired
    private PasswordService passwordService;

    /**
     * 装饰LocalContainerEntityManagerFactoryBean的数据源
     * JPA内部会使用这个DataSource创建连接，密码会通过装饰器自动解密
     */
    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactoryBean(
            DataSource dataSource) {

        // 如果密码为加密格式，装饰DataSource
        if (isEncryptedPassword(dataSource.getPassword())) {
            String dbId = extractDbIdFromUrl(dataSource.getUrl());
            String sm4Key = passwordService.getSm4Key();
            dataSource = ConnectionPoolDecoratorFactory.decorate(dataSource, dbId, sm4Key);
        }

        return LocalContainerEntityManagerFactoryBean.from(dataSource);
    }

    private boolean isEncryptedPassword(String password) {
        return password != null && password.startsWith("ENCC(SM4:");
    }
}
```

### 自定义Repository示例

```java
@Repository
public class CustomRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private PasswordService passwordService;

    /**
     * 查询时，使用装饰器DataSource的连接
     * JPA内部会自动使用装饰后的DataSource获取连接
     */
    @Transactional
    public List<User> findActiveUsers() {
        TypedQuery<User> query = entityManager.createQuery(
            "SELECT u FROM User u WHERE u.status = :status", User.class);
        query.setParameter("status", "ACTIVE");
        return query.getResultList();
    }

    /**
     * 生成审计日志（装饰器DataSource的连接会自动记录密码解密操作）
     */
    @Transactional
    public void saveUser(User user) {
        entityManager.persist(user);
    }
}
```

---

## Spring Data JDBC集成（原生JDBC）

### JdbcTemplate集成（装饰器集成）

```java
@Configuration
public class JdbcTemplateConfig {

    @Autowired
    private DataSource dataSource;

    @Bean
    public JdbcTemplate jdbcTemplate() {
        // dataSource此时已被修饰器装饰
        // 如果密码为密文，装饰器会自动在getConnection()前解密
        return new JdbcTemplate(dataSource);
    }
}
```

### 编程式手动获取连接

```java
@Component
public class JdbcNativeService {

    @Autowired
    private DataSource decoratedDataSource;

    public void executeNativeOperation(String sql) throws Exception {
        try (Connection conn = decoratedDataSource.getConnection()) {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);

            // 处理ResultSet...

        } catch (SQLException e) {
            throw new RuntimeException("数据库操作失败", e);
        }
    }
}
```

---

## Hibernate集成

### SessionFactory集成（装饰器集成）

```java
@Configuration
@EnableTransactionManagement
public class HibernateConfig {

    @Autowired
    private PasswordService passwordService;

    /**
     * 配置SessionFactory
     * Hibernate SessionFactory内部使用的DataSource通过装饰器包装
     */
    @Bean
    public LocalSessionFactoryBean sessionFactory(DataSource dataSource) {
        // 装饰DataSource，Hibernate内部会使用它获取连接
        if (isEncryptedPassword(dataSource.getPassword())) {
            String dbId = extractDbIdFromConfig();
            String sm4Key = passwordService.getSm4Key();
            dataSource = ConnectionPoolDecoratorFactory.decorate(dataSource, dbId, sm4Key);
        }

        Properties hibernateProperties = new Properties();
        hibernateProperties.put("hibernate.dialect",
            getHibernateDialect(dataSource));
        hibernateProperties.put("hibernate.show_sql", "true");

        LocalSessionFactoryBean factoryBean = new LocalSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setHibernateProperties(hibernateProperties);
        factoryBean.setPackagesToScan("com.example.domain");

        return factoryBean;
    }

    private String getHibernateDialect(DataSource dataSource) {
        String url = dataSource.getUrl().toLowerCase();
        if (url.contains("mysql")) return "org.hibernate.dialect.MySQL8Dialect";
        if (url.contains("postgresql")) return "org.hibernate.dialect.PostgreSQLDialect";
        if (url.contains("oracle")) return "org.hibernate.dialect.Oracle12cDialect";
        if (url.contains("sqlserver")) return "org.hibernate.dialect.SQLServer2012Dialect";
        if (url.contains("db2")) return "org.hibernate.dialect.DB2400Dialect";
        if (url.contains("h2")) return "org.hibernate.dialect.H2Dialect";
        return "org.hibernate.dialect.H2Dialect";
    }

    private boolean isEncryptedPassword(String password) {
        return password != null && password.startsWith("ENC(SM4:");
    }
}
```

---

## PasswordService接口和实现（装饰器版本）

### 接口定义

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
     * 获取SM4密钥
     *
     * @return SM4密钥
     */
    String getSm4Key();

    /**
     * 创建带解密密码的DataSource
     *
     * @param dbId 数据库ID
     * @param sm4Key SM4密钥
     * @return 装饰后的DataSource
     */
    DataSource createDataSource(String dbId, String sm4Key);
}
```

### 服务实现（装饰器版本）

```java
/**
 * 密码服务实现（装饰器版本）
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
        this.sm4Key = new Sm4KeyLoader(keyFile).loadKey();
        this.passwordFileManager = PasswordFileManager.loadFromFileSafe(passwordFilePath);
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
    public String getSm4Key() {
        return sm4Key;
    }

    @Override
    public DataSource createDataSource(String dbId, String sm4Key) {
        DatabaseEntry entry = getConfig(dbId);
        String plainPassword = getDecryptedPassword(dbId);

        // 根据数据库类型选择合适的连接池
        String poolType = entry.getType() != null ? entry.getType() : "druid";

        if ("druid".equalsIgnoreCase(poolType)) {
            DruidDataSource ds = new DruidDataSource();
            ds.setUrl(entry.getJdbcUrl());
            ds.setUsername(entry.getUsername());
            ds.setPassword(plainPassword);
            // 可选：设置连接池参数...
            return ds;
        } else if ("hikari".equalsIgnoreCase(poolType)) {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(entry.getJdbcUrl());
            config.setUsername(entry.getUsername());
            config.setPassword(plainPassword);
            // 可选：设置连接池参数...
            return new HikariDataSource(config);
        } else if ("dbcp2".equalsIgnoreCase(poolType)) {
            BasicDataSource ds = new BasicDataSource();
            ds.setUrl(entry.getJdbcUrl());
            ds.setUsername(entry.getUsername());
            ds.setPassword(plainPassword);
            // 可选：设置连接池参数...
            return ds;
        } else {
            throw new IllegalArgumentException("不支持的连接池类型: " + poolType);
        }
    }
}
```

---

## 使用示例

### 在Service中使用

```java
@Service
public class UserService {

    @Autowired
    private PasswordService passwordService;

    /**
     * 使用装饰器模式获取连接
     */
    public Connection getConnection() throws Exception {
        // 获取被装饰的DataSource
        DataSource ds = passwordService.createDataSource("db001");
        // 获取连接时，装饰器内部会自动解密密码
        return ds.getConnection();
    }

    /**
     * 更新数据库密码
     */
    public void updatePassword(String newPassword) throws Exception {
        DatabaseEntry entry = passwordService.getConfig("db001");
        String encryptedPassword = PasswordEncryptor.encryptSM4ENC(
            newPassword, passwordService.getSm4Key()
        );
        entry.setPassword(encryptedPassword);
        entry.setEncrypted(true);
        entry.touchUpdateTime();
        passwordFileManager.updateDatabase(entry);
        passwordFileManager.save();
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
}
```

---

## 连接池特性对比

| 特性 | Druid | HikariCP | DBCP2 | 说明 |
|------|-------|----------|--------|------|
| **装饰器支持** | ✅ 完全支持 | ✅ 完全支持 | ✅ 完全支持 | 装饰器统一支持 |
| **密码刷新** | ✅ 支持动态刷新 | ⚠️ 需重建 | ⚠️ 需重建 | 刷新方式不同 |
| **性能** | 通用 | 高 | 通用 | 高 | 各有优势 |
| **功能** | 丰富 | 基础 | 基础 | Druid功能最全面 |

---

## 框架集成最佳实践

### 1. 密钥配置

**推荐方式**：将密钥文件分离存储，设置600权限

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

### 3. 装饰器最佳实践

- ✅ **统一装饰**：所有连接池统一使用 `ConnectionPoolDecoratorFactory`
- ✅ **工厂方法**：使用工厂类，便于统一创建
- ✅ **自动刷新**：可配合注册表实现定时密码刷新
- ⚠️ **HikariCP限制**：密码变更后需重建连接池
- ⚠️ **事务一致**：装饰器不影响事务边界

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

---

## 扩展指南：添加对新框架/框架的装饰器支持

### 添加新连接池初始化器

```java
/**
 * 自定义连接池初始化器示例
 */
public class CustomPoolInitializer implements ConnectionPoolInitializer {

    @Override
    public void initialize(DataSource dataSource, String dbId, String sm4Key) {
        // 1. 实现必要的初始化逻辑
        // 2. 处理该连接池的特殊需求
        // 3. 记录初始化日志
    }
}

// 在应用启动时注册
ConnectionPoolInitializerRegistry.register(
    CustomDataSource.class,
    new CustomPoolInitializer()
);
```

---

## 相关文档

- [连接池装饰器](connection-pool.md) - 装饰器架构和实现
- [安全设计](security.md) - 审计日志和密钥轮换
- [密钥管理规范](../overview.md#sm4密钥管理) - 密钥文件格式
- [开发计划](development-plan.md) - Sprint 4框架集成阶段任务
