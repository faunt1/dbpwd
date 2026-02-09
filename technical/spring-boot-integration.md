# Spring Boot集成指南

> 文档版本: v1.0
> 最后更新: 2026-02-09

## 概述

本文档介绍如何在Spring Boot应用中集成Database Password Manager (dbpwd)，实现数据库密码的安全存储和自动解密。

## Maven依赖

在pom.xml中添加dbpwd依赖：

```xml
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd</artifactId>
    <version>1.0.0</version>
</dependency>
```

可选依赖 - Spring Boot自动配置支持：

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-autoconfigure</artifactId>
</dependency>
```

## 配置文件

### 1. 创建密码文件和密钥文件

使用命令行工具创建加密的密码文件：

```bash
# 生成SM4密钥
java -jar db-pwd.jar generate-key

# 加密密码并生成配置
java -jar db-pwd.jar encrypt --database=mysql_prod \
    --url="jdbc:mysql://localhost:3306/prod" \
    --username=prod_user \
    --password="your_password" \
    --driver="com.mysql.cj.jdbc.Driver"

# 显示生成的加密密码
java -jar db-pwd.jar show --id=mysql_prod
```

### 2. 配置application.yml

在Spring Boot应用的application.yml中配置：

```yaml
# dbpwd配置
dbpwd:
  enabled: true                                    # 启用dbpwd功能
  password-file: ~/.dbpwd/db.pwd                   # 密码文件路径
  key-file: ~/.dbpwd/sm4.key                      # SM4密钥文件路径
  validate-key-on-startup: true                   # 启动时验证密钥文件
  validate-password-file-on-startup: true         # 启动时验证密码文件

# Spring DataSource配置
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/prod
    username: prod_user
    # 密码配置为加密格式，会在连接时自动解密
    password: ENC(SM4:密文)
    driver-class-name: com.mysql.cj.jdbc.Driver

    # Druid连接池配置（示例）
    druid:
      initial-size: 5
      max-active: 20
      max-wait: 60000
      validation-query: SELECT 1
      test-while-idle: true
      test-on-borrow: false
```

## 使用方式

### 方式1：使用dbpwd自动装饰DataSource（推荐）

配置完成后，Spring Boot会自动创建DataSource，dbpwd会装饰该DataSource：

```java
@Service
public class UserService {

    private final DataSource dataSource;

    @Autowired
    public UserService(DataSource dataSource) {
        this.dataSource = dataSource;
        // dataSource已经自动被装饰，密码会自动解密
    }

    public User findUser(Long id) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            // 此时密码已自动解密
            // 使用conn查询数据库
            // ...
        }
    }
}
```

### 方式2：手动装饰DataSource

如果需要在代码中手动装饰DataSource：

```java
import com.dbpwd.framework.DataSourceDecorator;
import com.dbpwd.framework.DataSourceDecoratorFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSourceDecorator dataSourceDecorator(
            DataSource dataSource) throws Exception {

        // 方式1: 从文件加载SM4密钥
        String sm4Key = new com.dbpwd.core.Sm4KeyLoader("~/.dbpwd/sm4.key").loadKey();

        // 方式2: 使用固定密钥（不推荐）
        // String sm4Key = "0123456789abcdef0123456789abcdef";

        // 装饰DataSource
        return DataSourceDecoratorFactory.decorate(
                dataSource,
                "mysql_prod",    // 数据库ID
                sm4Key
        );
    }
}
```

### 方式3：从DatabaseEntry创建DataSourceDecorator

```java
import com.dbpwd.framework.DataSourceDecorator;
import com.dbpwd.framework.DataSourceDecoratorFactory;
import com.dbpwd.model.DatabaseEntry;

@Service
public class DataSourceService {

    public DataSourceDecorator createDecoratedDataSource() throws Exception {
        // 创建DatabaseEntry
        DatabaseEntry entry = new DatabaseEntry(
            "mysql_prod",
            "Production MySQL",
            "druid",
            "jdbc:mysql://localhost:3306/prod",
            "prod_user",
            "ENC(SM4:密文)"    // 加密的密码
        );
        entry.setDriver("com.mysql.cj.jdbc.Driver");

        // 设置连接池配置
        Map<String, Object> poolConfig = new HashMap<>();
        poolConfig.put("initialSize", 5);
        poolConfig.put("maxActive", 20);
        poolConfig.put("maxWait", 60000L);
        poolConfig.put("validationQuery", "SELECT 1");
        entry.setAdditionalProperties(poolConfig);

        // 创建装饰器
        String sm4Key = new com.dbpwd.core.Sm4KeyLoader("~/.dbpwd/sm4.key").loadKey();
        return DataSourceDecoratorFactory.create(entry, sm4Key);
    }
}
```

## MyBatis集成

### 配置MyBatis使用装饰后的DataSource

```java
import com.dbpwd.framework.DataSourceDecorator;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@Configuration
public class MyBatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(DataSourceDecorator dataSourceDecorator)
            throws Exception {

        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSourceDecorator);
        factory.setMapperLocations(
            new PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/**/*.xml")
        );

        return factory.getObject();
    }
}
```

### MyBatis Mapper示例

```java
@Mapper
public interface UserMapper {

    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Long id);

    @Insert("INSERT INTO users(name, email) VALUES(#{name}, #{email})")
    int insert(User user);

    @Update("UPDATE users SET name = #{name}, email = #{email} WHERE id = #{id}")
    int update(User user);

    @Delete("DELETE FROM users WHERE id = #{id}")
    int delete(Long id);
}
```

## 多数据源配置

### 配置多个DataSource

```java
import com.dbpwd.framework.DataSourceDecorator;
import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MultiDataSourceConfig {

    // 主数据源
    @Bean(name = "primaryDataSource")
    @Primary
    public DataSourceDecorator primaryDataSource() throws Exception {
        DruidDataSource druid = new DruidDataSource();
        druid.setUrl("jdbc:mysql://localhost:3306/prod");
        druid.setUsername("prod_user");
        druid.setPassword("ENC(SM4:密文)");
        druid.setDriverClassName("com.mysql.cj.jdbc.Driver");

        String sm4Key = new com.dbpwd.core.Sm4KeyLoader("~/.dbpwd/sm4.key").loadKey();

        return DataSourceDecoratorFactory.decorate(druid, "mysql_prod", sm4Key);
    }

    // 从数据源
    @Bean(name = "slaveDataSource")
    public DataSourceDecorator slaveDataSource() throws Exception {
        DruidDataSource druid = new DruidDataSource();
        druid.setUrl("jdbc:mysql://localhost:3306/slave");
        druid.setUsername("slave_user");
        druid.setPassword("ENC(SM4:密文)");
        druid.setDriverClassName("com.mysql.cj.jdbc.Driver");

        String sm4Key = new com.dbpwd.core.Sm4KeyLoader("~/.dbpwd/sm4.key").loadKey();

        return DataSourceDecoratorFactory.decorate(druid, "mysql_slave", sm4Key);
    }
}
```

### 使用多数据源的Repository

```java
@Repository
public class PrimaryUserRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PrimaryUserRepository(@Qualifier("primaryDataSource") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<User> findAll() {
        return jdbcTemplate.query("SELECT * FROM users",
            (rs, rowNum) -> new User(rs.getLong("id"), rs.getString("name"))
        );
    }
}
```

## 支持的连接池

### Druid

```java
// Druid配置示例
DruidDataSource druid = new DruidDataSource();
druid.setUrl(jdbcUrl);
druid.setUsername(username);
druid.setPassword(password);
druid.setDriverClassName(driverClass);

druid.setInitialSize(5);
druid.setMaxActive(20);
druid.setMaxIdle(10);
druid.setMinIdle(5);
druid.setMaxWait(60000);
druid.setValidationQuery("SELECT 1");
druid.setTestWhileIdle(true);
druid.setTestOnBorrow(false);
druid.setTestOnReturn(false);
```

### HikariCP

```java
// HikariCP配置示例
HikariConfig config = new HikariConfig();
config.setJdbcUrl(jdbcUrl);
config.setUsername(username);
config.setPassword(password);
config.setDriverClassName(driverClass);

config.setMaximumPoolSize(20);
config.setMinimumIdle(5);
config.setConnectionTimeout(30000);
config.setIdleTimeout(600000);
config.setMaxLifetime(1800000);
```

### DBCP2

```java
// DBCP2配置示例
BasicDataSource dbcp2 = new BasicDataSource();
dbcp2.setUrl(jdbcUrl);
dbcp2.setUsername(username);
dbcp2.setPassword(password);
dbcp2.setDriverClassName(driverClass);

dbcp2.setInitialSize(5);
dbcp2.setMaxTotal(20);
dbcp2.setMaxIdle(10);
dbcp2.setMinIdle(5);
dbcp2.setMaxWaitMillis(60000);
dbcp2.setValidationQuery("SELECT 1");
```

## 密码刷新

当密码文件更新后，可以通过以下方式刷新密码：

```java
import com.dbpwd.framework.DataSourceDecorator;

@Service
public class DataSourceService {

    @Autowired
    private DataSourceDecorator dataSourceDecorator;

    @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨2点刷新
    public void refreshDataSourcePassword() {
        dataSourceDecorator.refreshPassword();
        // 下次getConnection时会使用新的密码
    }
}
```

## 故障排查

### 问题1: 密钥文件不存在

```
java.io.FileNotFoundException: SM4密钥文件不存在: ~/.dbpwd/sm4.key
```

**解决方案**:
- 确保密钥文件存在
- 检查配置文件中的key-file路径是否正确
- 使用`generate-key`命令生成密钥

### 问题2: 密码解密失败

```
java.lang.RuntimeException: 解密数据源密码失败
```

**解决方案**:
- 确保密码文件中的密文格式正确
- 检查密钥文件内容是否正确
- 确认使用的加密算法与密文格式匹配

### 问题3: 连接池配置丢失

**解决方案**:
- 优先使用方式1（装饰已配置的DataSource）
- 从DatabaseEntry创建时，确保在additionalProperties中配置连接池参数

## 安全建议

1. **密钥文件权限**: 确保密钥文件权限为600（仅所有者读写）
2. **密码文件权限**: 确保密码文件权限为600（仅所有者读写）
3. **密钥轮换**: 定期更换SM4密钥
4. **不硬编码密钥**: 不要在代码中硬编码SM4密钥
5. **环境变量**: 在生产环境使用环境变量或配置中心管理路径
6. **日志脱敏**: 确保日志中不输出明文密码

## 参考文档

- [SM4加密详解](sm4-encryption.md)
- [密码文件格式](password-format.md)
- [密钥管理](key-management.md)
- [连接池装饰器](connection-pool.md)
