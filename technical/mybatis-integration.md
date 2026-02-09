# MyBatis集成指南

> 文档版本: v1.0
> 最后更新: 2026-02-09

## 概述

本文档介绍如何在MyBatis中集成Database Password Manager，实现数据库密码的安全存储和自动解密。

## 配置方式

### 方式1：使用装饰后的DataSource配置SqlSessionFactory

```java
import com.dbpwd.framework.DataSourceDecorator;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

@Configuration
public class MyBatisConfig {

    @Bean
    public SqlSessionFactory sqlSessionFactory(
            DataSourceDecorator dataSourceDecorator) throws Exception {

        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSourceDecorator);

        // 设置Mapper XML位置
        factory.setMapperLocations(
            new PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/**/*.xml")
        );

        // 设置类型别名包
        factory.setTypeAliasesPackage("com.example.model");

        // 添加MyBatis配置
        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        configuration.setCacheEnabled(true);
        configuration.setLazyLoadingEnabled(true);
        configuration.setAggressiveLazyLoading(false);
        factory.setConfiguration(configuration);

        return factory.getObject();
    }

    @Bean
    public DataSourceTransactionManager transactionManager(
            DataSourceDecorator dataSourceDecorator) {
        return new DataSourceTransactionManager(dataSourceDecorator);
    }
}
```

### 方式2：使用application.yml配置

```yaml
# dbpwd配置
dbpwd:
  enabled: true
  password-file: ~/.dbpwd/db.pwd
  key-file: ~/.dbpwd/sm4.key

# MyBatis配置
mybatis:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.example.model
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: true
    lazy-loading-enabled: true
    aggressive-lazy-loading: false

# MyBatis-Plus配置（可选）
mybatis-plus:
  mapper-locations: classpath:mapper/**/*.xml
  type-aliases-package: com.example.model
  configuration:
    map-underscore-to-camel-case: true
    cache-enabled: true
```

### 方式3：使用@MapperScan注解

```java
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.example.mapper")
public class MyBatisConfig {
    // 配置SqlSessionFactory方式同时方式1
}
```

## Mapper示例

### 基础Mapper接口

```java
import com.example.model.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Long id);

    @Select("SELECT * FROM users")
    List<User> findAll();

    @Insert("INSERT INTO users(name, email, created_at) " +
            "VALUES(#{name}, #{email}, #{createdAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("UPDATE users SET name = #{name}, email = #{email}, " +
            "updated_at = #{updatedAt} WHERE id = #{id}")
    int update(User user);

    @Delete("DELETE FROM users WHERE id = #{id}")
    int delete(Long id);
}
```

### 使用XML的Mapper

```java
import com.example.model.User;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface UserMapperXml {

    User findById(Long id);

    List<User> findByCriteria(UserCriteria criteria);

    int insert(User user);

    int update(User user);

    int delete(Long id);
}
```

对应的XML文件 `src/main/resources/mapper/UserMapper.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper namespace="com.example.mapper.UserMapperXml">

    <resultMap id="BaseResultMap" type="com.example.model.User">
        <id column="id" property="id" jdbcType="BIGINT"/>
        <result column="name" property="name" jdbcType="VARCHAR"/>
        <result column="email" property="email" jdbcType="VARCHAR"/>
        <result column="created_at" property="createdAt" jdbcType="TIMESTAMP"/>
        <result column="updated_at" property="updatedAt" jdbcType="TIMESTAMP"/>
    </resultMap>

    <select id="findById" resultMap="BaseResultMap">
        SELECT * FROM users WHERE id = #{id}
    </select>

    <select id="findByCriteria" resultMap="BaseResultMap">
        SELECT * FROM users
        <where>
            <if test="name != null and name != ''">
                AND name LIKE CONCAT('%', #{name}, '%')
            </if>
            <if test="email != null and email != ''">
                AND email LIKE CONCAT('%', #{email}, '%')
            </if>
        </where>
        ORDER BY id DESC
    </select>

    <insert id="insert" parameterType="com.example.model.User" useGeneratedKeys="true" keyProperty="id">
        INSERT INTO users (name, email, created_at, updated_at)
        VALUES (#{name}, #{email}, #{createdAt}, #{updatedAt})
    </insert>

    <update id="update" parameterType="com.example.model.User">
        UPDATE users
        <set>
            <if test="name != null">name = #{name},</if>
            <if test="email != null">email = #{email},</if>
            <if test="updatedAt != null">updated_at = #{updatedAt},</if>
        </set>
        WHERE id = #{id}
    </update>

    <delete id="delete">
        DELETE FROM users WHERE id = #{id}
    </delete>

</mapper>
```

## 多数据源配置

### 配置多个SqlSessionFactory

```java
import com.dbpwd.framework.DataSourceDecorator;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class MultiMyBatisConfig {

    // 主数据源配置
    @Bean(name = "primarySqlSessionFactory")
    @Primary
    public SqlSessionFactory primarySqlSessionFactory(
            @Qualifier("primaryDataSource") DataSourceDecorator dataSourceDecorator)
            throws Exception {

        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSourceDecorator);
        factory.setMapperLocations(
            new PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/primary/**/*.xml")
        );
        factory.setTypeAliasesPackage("com.example.model");

        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        factory.setConfiguration(configuration);

        return factory.getObject();
    }

    @Bean(name = "primarySqlSessionTemplate")
    @Primary
    public SqlSessionTemplate primarySqlSessionTemplate(
            @Qualifier("primarySqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }

    // 从数据源配置
    @Bean(name = "slaveSqlSessionFactory")
    public SqlSessionFactory slaveSqlSessionFactory(
            @Qualifier("slaveDataSource") DataSourceDecorator dataSourceDecorator)
            throws Exception {

        SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
        factory.setDataSource(dataSourceDecorator);
        factory.setMapperLocations(
            new PathMatchingResourcePatternResolver()
                .getResources("classpath:mapper/slave/**/*.xml")
        );
        factory.setTypeAliasesPackage("com.example.model");

        org.apache.ibatis.session.Configuration configuration = new org.apache.ibatis.session.Configuration();
        configuration.setMapUnderscoreToCamelCase(true);
        factory.setConfiguration(configuration);

        return factory.getObject();
    }

    @Bean(name = "slaveSqlSessionTemplate")
    public SqlSessionTemplate slaveSqlSessionTemplate(
            @Qualifier("slaveSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
```

### 使用多数据源的Mapper

```java
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;

@Mapper
public interface PrimaryUserMapper {

    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Long id);
}

@Mapper
public interface SlaveUserMapper {

    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Long id);
}

@Service
public class UserQueryService {

    private final JdbcTemplate primaryJdbcTemplate;
    private final JdbcTemplate slaveJdbcTemplate;

    @Autowired
    public UserQueryService(
            @Qualifier("primaryDataSource") JdbcTemplate primaryJdbcTemplate,
            @Qualifier("slaveDataSource") JdbcTemplate slaveJdbcTemplate) {
        this.primaryJdbcTemplate = primaryJdbcTemplate;
        this.slaveJdbcTemplate = slaveJdbcTemplate;
    }

    // 读写分离示例
    public User query(Long id) {
        // 从从库查询
        return slaveJdbcTemplate.queryForObject(
            "SELECT * FROM users WHERE id = ?",
            (rs, rowNum) -> new User(rs.getLong("id"), rs.getString("name")),
            id
        );
    }
}
```

## 动态数据源

### 动态切换数据源

```java
import com.dbpwd.framework.DataSourceDecorator;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class DynamicDataSource extends AbstractRoutingDataSource {

    private static final ThreadLocal<String> contextHolder = new ThreadLocal<>();

    public static void setDataSourceKey(String key) {
        contextHolder.set(key);
    }

    public static String getDataSourceKey() {
        return contextHolder.get();
    }

    public static void clearDataSourceKey() {
        contextHolder.remove();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return getDataSourceKey();
    }
}

@Configuration
public class DynamicDataSourceConfig {

    @Bean
    public DynamicDataSource dynamicDataSource(
            @Qualifier("primaryDataSource") DataSourceDecorator primaryDataSource,
            @Qualifier("slaveDataSource") DataSourceDecorator slaveDataSource) {

        DynamicDataSource dynamicDataSource = new DynamicDataSource();

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put("primary", primaryDataSource);
        targetDataSources.put("slave", slaveDataSource);

        dynamicDataSource.setTargetDataSources(targetDataSources);
        dynamicDataSource.setDefaultTargetDataSource(primaryDataSource);

        return dynamicDataSource;
    }
}

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    public User findById(Long id) {
        // 使用从库
        DynamicDataSource.setDataSourceKey("slave");
        try {
            return userMapper.findById(id);
        } finally {
            DynamicDataSource.clearDataSourceKey();
        }
    }

    public void save(User user) {
        // 使用主库
        DynamicDataSource.setDataSourceKey("primary");
        try {
            userMapper.insert(user);
        } finally {
            DynamicDataSource.clearDataSourceKey();
        }
    }
}
```

## MyBatis-Plus集成

### 基础配置

```java
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.dbpwd.framework.DataSourceDecorator;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@MapperScan("com.example.mapper")
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 分页插件
        PaginationInnerInterceptor paginationInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        paginationInterceptor.setMaxLimit(1000L);  // 最大单页限制
        paginationInterceptor.setOverflow(false);     // 溢出总页数后是否进行处理

        interceptor.addInnerInterceptor(paginationInterceptor);

        return interceptor;
    }
}
```

### 实体类示例

```java
import com.baomidou.mybatisplus.annotation.*;

import java.time.LocalDateTime;

@TableName("users")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("email")
    private String email;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;

    // getters and setters
}
```

### Mapper接口

```java
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 继承BaseMapper后自动拥有CRUD方法
    // 也可以添加自定义方法
}
```

### Service示例

```java
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

@Service
public class UserService extends ServiceImpl<UserMapper, User> implements IService<User> {

    // 继承ServiceImpl后自动拥有CRUD方法
    // 也可以添加自定义业务方法
}
```

## 事务管理

### 使用@Transactional注解

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private UserMapper userMapper;

    @Transactional(rollbackFor = Exception.class)
    public void createOrder(Order order) {
        // 事务内操作
        orderMapper.insert(order);

        // 扣减用户余额
        userMapper.deductBalance(order.getUserId(), order.getAmount());

        // 如果抛出异常，事务会回滚
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long orderId) {
        return orderMapper.selectById(orderId);
    }
}
```

### 编程式事务

```java
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

@Service
public class TransactionService {

    @Autowired
    private PlatformTransactionManager transactionManager;

    public void executeInTransaction() {
        // 定义事务属性
        DefaultTransactionDefinition definition = new DefaultTransactionDefinition();
        definition.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        definition.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        definition.setTimeout(30);

        // 开始事务
        TransactionStatus status = transactionManager.getTransaction(definition);

        try {
            // 执行业务逻辑
            // ...

            // 提交事务
            transactionManager.commit(status);
        } catch (Exception e) {
            // 回滚事务
            transactionManager.rollback(status);
            throw e;
        }
    }
}
```

## 缓存配置

### 启用MyBatis二级缓存

```java
import org.apache.ibatis.cache.impl.PerpetualCache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class MyBatisCacheConfig {

    @Bean
    public org.apache.ibatis.cache.Cache myBatisCache() {
        return new PerpetualCache("userCache");
    }
}
```

### 在Mapper中启用缓存

```java
import org.apache.ibatis.annotations.CacheNamespace;
import org.apache.ibatis.annotations.Mapper;

@Mapper
@CacheNamespace  // 启用二级缓存
public interface UserMapper {

    @Select("SELECT * FROM users WHERE id = #{id}")
    @Options(useCache = true)  // 启用查询缓存
    User findById(Long id);
}
```

## 注意事项

1. **密码解密时机**: 密码在获取数据库连接时才解密，确保密码文件和密钥文件在应用启动时可访问

2. **连接池配置**: 使用装饰器方式时，确保配置完整的连接池参数

3. **多数据源**: 多数据源配置时，注意SqlSessionFactory和MapperScan的包路径分离

4. **事务边界**: 确保事务管理器使用的是装饰后的DataSource

5. **MyBatis配置**: 检查map-underscore-to-camel-case等常用配置是否正确

## 参考文档

- [MyBatis官方文档](https://mybatis.org/mybatis-3/)
- [Spring Boot集成指南](spring-boot-integration.md)
- [连接池装饰器](connection-pool.md)
