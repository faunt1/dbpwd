package com.dbpwd.framework;

import com.dbpwd.model.DatabaseEntry;
import com.dbpwd.core.PasswordFileManager;
import com.dbpwd.core.Sm4KeyLoader;

import javax.sql.DataSource;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * 数据源装饰器工厂
 *
 * 统一管理DataSourceDecorator的创建和装饰。
 *
 * 提供两种使用方式：
 * 1. 装饰已配置的DataSource（推荐 - 保留所有连接池配置）
 * 2. 从DatabaseEntry创建DataSource
 *
 * @author Database Password Manager
 * @version 4.2
 */
public class DataSourceDecoratorFactory {

    private static final Logger logger = Logger.getLogger(DataSourceDecoratorFactory.class.getName());

    /**
     * 装饰指定的数据源（推荐方式）
     *
     * 此方式保留原始数据源的所有配置参数（最大连接数、超时时间等）
     * 装饰器只负责密码解密，不修改连接池配置
     *
     * 使用示例：
     * <pre>
     * DruidDataSource druid = new DruidDataSource();
     * druid.setUrl("jdbc:mysql://localhost:3306/test");
     * druid.setUsername("user");
     * druid.setPassword(encryptedPassword);
     * druid.setInitialSize(5);
     * druid.setMaxActive(20);
     * druid.setMaxWait(60000);
     *
     * DataSourceDecorator decorator = DataSourceDecoratorFactory.decorate(druid, "db1", sm4Key);
     * Connection conn = decorator.getConnection(); // 密码自动解密
     * </pre>
     *
     * @param dataSource 已配置完成的数据源（包含所有连接池参数）
     * @param dbId       数据库ID，用于查找密码文件中的条目
     * @param sm4Key     SM4密钥，用于解密密码
     * @return 装饰后的数据源
     */
    public static DataSourceDecorator decorate(DataSource dataSource, String dbId, String sm4Key) {
        Objects.requireNonNull(dataSource, "数据源不能为null");
        Objects.requireNonNull(dbId, "数据库ID不能为空");
        Objects.requireNonNull(sm4Key, "SM4密钥不能为空");

        logger.info("装饰数据源: " + dataSource.getClass().getSimpleName() + ", dbId: " + dbId);

        try {
            return new DataSourceDecorator(dataSource, dbId, sm4Key);
        } catch (IOException e) {
            throw new RuntimeException("创建装饰器失败: " + e.getMessage(), e);
        }
    }

    /**
     * 装饰指定的数据源（指定密码文件路径）
     *
     * @param dataSource    已配置完成的数据源
     * @param dbId          数据库ID
     * @param sm4Key        SM4密钥
     * @param passwordFile  密码文件路径
     * @return 装饰后的数据源
     */
    public static DataSourceDecorator decorate(DataSource dataSource,
                                               String dbId,
                                               String sm4Key,
                                               String passwordFile) {
        Objects.requireNonNull(dataSource, "数据源不能为null");
        Objects.requireNonNull(dbId, "数据库ID不能为空");
        Objects.requireNonNull(sm4Key, "SM4密钥不能为空");

        logger.info("装饰数据源: " + dataSource.getClass().getSimpleName() + ", dbId: " + dbId);

        try {
            return new DataSourceDecorator(dataSource, dbId, sm4Key, passwordFile);
        } catch (IOException e) {
            throw new RuntimeException("创建装饰器失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从DatabaseEntry配置创建新的数据源并装饰
     *
     * 此方法使用DatabaseEntry的additionalProperties来设置连接池配置
     * 支持的连接池配置项：
     *
     * <b>Druid配置项：</b>
     * <ul>
     *   <li>initialSize: 初始连接数</li>
     *   <li>maxActive: 最大活跃连接数</li>
     *   <li>maxIdle: 最大空闲连接数</li>
     *   <li>maxWait: 获取连接等待超时时间(毫秒)</li>
     *   <li>validationQuery: 验证连接的SQL</li>
     *   <li>testWhileIdle: 空闲时检测连接是否有效</li>
     *   <li>testOnBorrow: 获取连接时检测</li>
     *   <li>testOnReturn: 归还连接时检测</li>
     *   <li>timeBetweenEvictionRunsMillis: 检测间隔时间(毫秒)</li>
     *   <li>minEvictableIdleTimeMillis: 最小空闲驱逐时间(毫秒)</li>
     *   <li>poolPreparedStatements: 是否缓存PreparedStatement</li>
     *   <li>maxPoolPreparedStatementPerConnectionSize: 每个连接上PSCache大小</li>
     * </ul>
     *
     * <b>HikariCP配置项：</b>
     * <ul>
     *   <li>maximumPoolSize: 最大线程池大小</li>
     *   <li>minimumIdle: 最小空闲连接</li>
     *   <li>connectionTimeout: 连接超时(毫秒)</li>
     *   <li>idleTimeout: 空闲超时(毫秒)</li>
     *   <li>maxLifetime: 连接最大生命周期(毫秒)</li>
     *   <li>connectionTestQuery: 测试查询</li>
     *   <li>poolName: 连接池名称</li>
     * </ul>
     *
     * <b>DBCP2配置项：</b>
     * <ul>
     *   <li>initialSize: 初始连接数</li>
     *   <li>maxTotal: 最大连接数</li>
     *   <li>maxIdle: 最大空闲连接数</li>
     *   <li>minIdle: 最小空闲连接数</li>
     *   <li>maxWaitMillis: 获取连接最大等待时间(毫秒)</li>
     *   <li>validationQuery: 验证查询</li>
     *   <li>testOnBorrow: 获取连接时检测</li>
     *   <li>testWhileIdle: 空闲时检测</li>
     *   <li>timeBetweenEvictionRunsMillis: 检测间隔(毫秒)</li>
     *   <li>minEvictableIdleTimeMillis: 最小空闲驱逐时间(毫秒)</li>
     * </ul>
     *
     * @param dbConfig 数据库配置条目
     * @param sm4Key   SM4密钥
     * @return 装饰后的数据源
     */
    public static DataSourceDecorator create(DatabaseEntry dbConfig, String sm4Key) {
        Objects.requireNonNull(dbConfig, "数据库配置不能为空");
        Objects.requireNonNull(sm4Key, "SM4密钥不能为空");

        String poolType = dbConfig.getType();
        if (poolType == null || poolType.isEmpty()) {
            poolType = "druid"; // 默认使用Druid
        }

        DataSource dataSource = createDataSourceByType(
                poolType,
                dbConfig.getJdbcUrl(),
                dbConfig.getUsername(),
                dbConfig.getPassword(),
                dbConfig.getDriver(),
                dbConfig.getAdditionalProperties()
        );

        try {
            return new DataSourceDecorator(dataSource, dbConfig.getId(), sm4Key);
        } catch (IOException e) {
            throw new RuntimeException("创建装饰器失败: " + e.getMessage(), e);
        }
    }

    /**
     * 配置Druid数据源
     */
    private static com.alibaba.druid.pool.DruidDataSource createDruidDataSource(
            String jdbcUrl, String username, String password, String driver,
            Map<String, Object> properties) {

        com.alibaba.druid.pool.DruidDataSource druid = new com.alibaba.druid.pool.DruidDataSource();
        druid.setUrl(jdbcUrl);
        druid.setUsername(username);
        druid.setPassword(password);
        druid.setDriverClassName(driver);

        // 应用连接池配置
        if (properties != null && !properties.isEmpty()) {
            setProperty(druid, properties, "initialSize", Integer.class);
            setProperty(druid, properties, "maxActive", Integer.class);
            setProperty(druid, properties, "maxIdle", Integer.class);
            setProperty(druid, properties, "maxWait", Long.class);
            setProperty(druid, properties, "minIdle", Integer.class);
            setProperty(druid, properties, "validationQuery", String.class);
            setProperty(druid, properties, "testWhileIdle", Boolean.class);
            setProperty(druid, properties, "testOnBorrow", Boolean.class);
            setProperty(druid, properties, "testOnReturn", Boolean.class);
            setProperty(druid, properties, "timeBetweenEvictionRunsMillis", Long.class);
            setProperty(druid, properties, "minEvictableIdleTimeMillis", Long.class);
            setProperty(druid, properties, "poolPreparedStatements", Boolean.class);
            setProperty(druid, properties, "maxPoolPreparedStatementPerConnectionSize", Integer.class);
        }

        return druid;
    }

    /**
     * 配置HikariCP数据源
     */
    private static com.zaxxer.hikari.HikariDataSource createHikariDataSource(
            String jdbcUrl, String username, String password, String driver,
            Map<String, Object> properties) {

        com.zaxxer.hikari.HikariConfig config = new com.zaxxer.hikari.HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName(driver);

        // 应用连接池配置
        if (properties != null && !properties.isEmpty()) {
            setProperty(config, properties, "maximumPoolSize", Integer.class);
            setProperty(config, properties, "minimumIdle", Integer.class);
            setProperty(config, properties, "connectionTimeout", Long.class);
            setProperty(config, properties, "idleTimeout", Long.class);
            setProperty(config, properties, "maxLifetime", Long.class);
            setProperty(config, properties, "connectionTestQuery", String.class);
            setProperty(config, properties, "poolName", String.class);
        }

        return new com.zaxxer.hikari.HikariDataSource(config);
    }

    /**
     * 配置DBCP2数据源
     */
    private static org.apache.commons.dbcp2.BasicDataSource createDbcp2DataSource(
            String jdbcUrl, String username, String password, String driver,
            Map<String, Object> properties) {

        org.apache.commons.dbcp2.BasicDataSource dbcp2 = new org.apache.commons.dbcp2.BasicDataSource();
        dbcp2.setUrl(jdbcUrl);
        dbcp2.setUsername(username);
        dbcp2.setPassword(password);
        dbcp2.setDriverClassName(driver);

        // 应用连接池配置
        if (properties != null && !properties.isEmpty()) {
            setProperty(dbcp2, properties, "initialSize", Integer.class);
            setProperty(dbcp2, properties, "maxTotal", Integer.class);
            setProperty(dbcp2, properties, "maxIdle", Integer.class);
            setProperty(dbcp2, properties, "minIdle", Integer.class);
            setProperty(dbcp2, properties, "maxWaitMillis", Long.class);
            setProperty(dbcp2, properties, "validationQuery", String.class);
            setProperty(dbcp2, properties, "testOnBorrow", Boolean.class);
            setProperty(dbcp2, properties, "testWhileIdle", Boolean.class);
            setProperty(dbcp2, properties, "timeBetweenEvictionRunsMillis", Long.class);
            setProperty(dbcp2, properties, "minEvictableIdleTimeMillis", Long.class);
        }

        return dbcp2;
    }

    /**
     * 设置属性的通用方法（使用反射）
     */
    private static void setProperty(Object target, Map<String, Object> properties,
                                     String propertyName, Class<?> expectedType) {
        Object value = properties.get(propertyName);
        if (value != null) {
            try {
                // 类型转换
                if (expectedType == Integer.class && value instanceof Number) {
                    value = ((Number) value).intValue();
                } else if (expectedType == Long.class && value instanceof Number) {
                    value = ((Number) value).longValue();
                } else if (expectedType == Boolean.class && value instanceof String) {
                    value = Boolean.parseBoolean((String) value);
                } else if (!expectedType.isInstance(value)) {
                    // 尝试字符串转换
                    if (expectedType == Integer.class) {
                        value = Integer.parseInt(value.toString());
                    } else if (expectedType == Long.class) {
                        value = Long.parseLong(value.toString());
                    } else if (expectedType == Boolean.class) {
                        value = Boolean.valueOf(value.toString());
                    }
                }

                // 调用setter方法
                String setterName = "set" + propertyName.substring(0, 1).toUpperCase() +
                        propertyName.substring(1);
                java.lang.reflect.Method setter = target.getClass().getMethod(setterName, expectedType);
                setter.invoke(target, value);
                logger.fine("设置属性: " + propertyName + " = " + value);
            } catch (Exception e) {
                logger.warning("设置属性 " + propertyName + " 失败: " + e.getMessage());
            }
        }
    }

    /**
     * 根据连接池类型创建数据源
     *
     * @param poolType  连接池类型（druid, hikari, dbcp2等）
     * @param jdbcUrl    JDBC URL
     * @param username   数据库用户名
     * @param password   密码（可以是密文）
     * @param driverClassName JDBC驱动类名
     * @param properties 连接池配置属性（可为null）
     * @return 原始数据源
     */
    public static DataSource createDataSourceByType(String poolType,
                                                     String jdbcUrl,
                                                     String username,
                                                     String password,
                                                     String driverClassName,
                                                     Map<String, Object> properties) {

        Objects.requireNonNull(jdbcUrl, "JDBC URL不能为空");
        Objects.requireNonNull(username, "用户名不能为空");
        Objects.requireNonNull(password, "密码不能为空");

        try {
            Class.forName(driverClassName);
            logger.info("创建数据源，类型: " + poolType + ", 驱动: " + driverClassName);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("未找到JDBC驱动: " + driverClassName, e);
        }

        switch (poolType.toLowerCase()) {
            case "druid":
                return createDruidDataSource(jdbcUrl, username, password, driverClassName, properties);

            case "hikari":
                return createHikariDataSource(jdbcUrl, username, password, driverClassName, properties);

            case "dbcp2":
                return createDbcp2DataSource(jdbcUrl, username, password, driverClassName, properties);

            default:
                throw new IllegalArgumentException("不支持的连接池类型: " + poolType);
        }
    }

    /**
     * 根据连接池类型创建数据源（无额外配置）
     */
    public static DataSource createDataSourceByType(String poolType,
                                                     String jdbcUrl,
                                                     String username,
                                                     String password,
                                                     String driverClassName) {
        return createDataSourceByType(poolType, jdbcUrl, username, password, driverClassName, null);
    }

    /**
     * 批量创建装饰器
     *
     * @param dataSources 数据源配置列表
     * @param sm4Key      SM4密钥
     * @return 装饰后的数据源集合
     */
    public static Map<String, DataSourceDecorator> batchCreate(
            List<DatabaseEntry> dataSources,
            String sm4Key) {

        Map<String, DataSourceDecorator> result = new HashMap<>();

        for (DatabaseEntry dbConfig : dataSources) {
            try {
                DataSourceDecorator decorated = create(dbConfig, sm4Key);
                result.put(dbConfig.getId(), decorated);
                logger.info("批量创建装饰器: " + dbConfig.getId());
            } catch (Exception e) {
                logger.warning("批量创建装饰器失败 [dbId=" + dbConfig.getId() + "]: " +
                        e.getMessage());
            }
        }

        return result;
    }

    /**
     * 验证装饰后的数据源密码是否为明文
     *
     * @param decorator 装饰器
     * @return true=密码已解密，false=密码仍是密文或无效
     */
    public static boolean isPasswordDecrypted(DataSourceDecorator decorator) {
        return decorator.getPasswordStatus() == DataSourceDecorator.PasswordStatus.PLAIN;
    }

    /**
     * 强制刷新装饰器中的密码
     *
     * @param decorator 装饰器
     */
    public static void forceRefreshPassword(DataSourceDecorator decorator) {
        decorator.refreshPassword();
    }

    /**
     * 从密钥文件和密码文件创建装饰器
     *
     * @param passwordFile 密码文件路径
     * @param keyFile     密钥文件路径
     * @param dataSource  基础数据源
     * @param dbId        数据库ID
     * @return 装饰器
     */
    public static DataSourceDecorator loadFromFileAndDecorate(
            String passwordFile,
            String keyFile,
            DataSource dataSource,
            String dbId) throws IOException {
        PasswordFileManager fileManager = PasswordFileManager.loadFromFileSafe(passwordFile);
        com.dbpwd.model.DatabaseEntry entry = fileManager.getById(dbId);

        if (entry == null) {
            throw new IllegalArgumentException("未找到数据库配置: " + dbId);
        }

        // 加载SM4密钥
        String sm4Key;
        try {
            sm4Key = new com.dbpwd.core.Sm4KeyLoader(keyFile).loadKey();
        } catch (Exception e) {
            throw new IOException("加载SM4密钥失败: " + e.getMessage(), e);
        }

        return new DataSourceDecorator(
                dataSource, dbId, sm4Key, passwordFile
        );
    }
}
