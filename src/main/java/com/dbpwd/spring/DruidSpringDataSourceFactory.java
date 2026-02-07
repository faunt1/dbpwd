package com.dbpwd.spring;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.configcenter.ConfigCenterFallbackManager;
import com.dbpwd.universal.PasswordPropertiesInterceptor;

import org.springframework.jdbc.datasource.DelegatingDataSource;

import javax.sql.DataSource;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Druid Spring 数据源工厂
 *
 * 提供基于 Spring 的 Druid 数据源创建和管理，
 * 支持配置中心下载优先、本地文件兜底的策略
 *
 * 使用方式：
 * 1. 在 Spring 配置中使用此工厂创建 DataSource
 * 2. 配置文件中的密码使用 @config(uid) 占位符
 *
 * @author Druid DB Password Manager
 * @version 2.0
 */
public class DruidSpringDataSourceFactory {

    private static final Logger logger = Logger.getLogger(DruidSpringDataSourceFactory.class.getName());

    private ConfigCenterFallbackManager fallbackManager;
    private PasswordPropertiesInterceptor interceptor;

    /**
     * 默认构造函数
     */
    public DruidSpringDataSourceFactory() {
    }

    /**
     * 使用回退管理器构造
     *
     * @param fallbackManager 配置中心回退管理器
     */
    public DruidSpringDataSourceFactory(ConfigCenterFallbackManager fallbackManager) {
        this.fallbackManager = fallbackManager;
        this.interceptor = new PasswordPropertiesInterceptor(fallbackManager);
    }

    /**
     * 创建带密码解析的数据源
     *
     * @param props 配置属性（密码可以是 @config(uid) 占位符）
     * @return Druid 数据源
     * @throws Exception 创建异常
     */
    public DataSource createDataSource(Properties props) throws Exception {
        // 使用拦截器处理占位符
        if (interceptor != null) {
            props = interceptor.intercept(props);
        }

        // 创建 Druid 数据源
        DruidDataSource dataSource = createDruidDataSource(props);

        // 包装为 DelegatingDataSource 以支持刷新
        return new DelegatingDataSource(dataSource) {
            @Override
            protected DataSource obtainTargetDataSource() {
                // 如果需要刷新，可以在这里处理
                return super.getTargetDataSource();
            }
        };
    }

    /**
     * 从配置中心创建数据源
     *
     * @param configCenterUrl 配置中心 URL
     * @param apiKey API 密钥
     * @param templateName 模板名称
     * @param localPasswordFile 本地密码文件路径（兜底）
     * @return Druid 数据源
     * @throws Exception 创建异常
     */
    public DataSource createFromConfigCenter(
            String configCenterUrl,
            String apiKey,
            String templateName,
            String localPasswordFile) throws Exception {

        // 创建回退管理器
        ConfigCenterFallbackManager manager =
            new ConfigCenterFallbackManager(configCenterUrl, apiKey, "/druid/config", localPasswordFile);

        return createFromConfigCenterWithManager(manager, templateName);
    }

    /**
     * 使用回退管理器从配置中心创建数据源
     *
     * @param fallbackManager 回退管理器
     * @param templateName 模板名称
     * @return Druid 数据源
     * @throws Exception 创建异常
     */
    public DataSource createFromConfigCenterWithManager(
            ConfigCenterFallbackManager fallbackManager,
            String templateName) throws Exception {

        this.fallbackManager = fallbackManager;
        this.interceptor = new PasswordPropertiesInterceptor(fallbackManager);

        // 从配置中心下载模板
        com.dbpwd.DruidConfigUploader uploader =
            new com.dbpwd.DruidConfigUploader(
                    fallbackManager.getConfigCenterUrl(),
                    fallbackManager.getApiKey());

        String templateContent = uploader.downloadTemplate(templateName);
        Properties props = new Properties();
        try (java.io.StringReader reader = new java.io.StringReader(templateContent)) {
            props.load(reader);
        }

        // 使用拦截器处理占位符
        props = interceptor.intercept(props);

        // 创建 Druid 数据源
        return new DelegatingDataSource(createDruidDataSource(props));
    }

    /**
     * 创建 Druid 数据源（内部方法）
     *
     * @param props 配置属性（密码已解密）
     * @return Druid 数据源
     */
    private DruidDataSource createDruidDataSource(Properties props) throws Exception {
        DruidDataSource dataSource = new DruidDataSource();

        // 基本配置
        dataSource.setDriverClassName(props.getProperty("driverClassName"));
        dataSource.setUrl(props.getProperty("url"));
        dataSource.setUsername(props.getProperty("username"));
        dataSource.setPassword(props.getProperty("password"));

        // 连接池配置
        dataSource.setInitialSize(Integer.parseInt(props.getProperty("initialSize", "5")));
        dataSource.setMinIdle(Integer.parseInt(props.getProperty("minIdle", "5")));
        dataSource.setMaxActive(Integer.parseInt(props.getProperty("maxActive", "20")));
        dataSource.setMaxWait(Long.parseLong(props.getProperty("maxWait", "60000")));

        // 空闲连接检查
        dataSource.setTimeBetweenEvictionRunsMillis(
            Long.parseLong(props.getProperty("timeBetweenEvictionRunsMillis", "60000")));
        dataSource.setMinEvictableIdleTimeMillis(
            Long.parseLong(props.getProperty("minEvictableIdleTimeMillis", "300000")));

        // 连接验证
        dataSource.setTestWhileIdle(
            Boolean.parseBoolean(props.getProperty("testWhileIdle", "true")));
        dataSource.setTestOnBorrow(
            Boolean.parseBoolean(props.getProperty("testOnBorrow", "false")));
        dataSource.setTestOnReturn(
            Boolean.parseBoolean(props.getProperty("testOnReturn", "false")));

        String validationQuery = props.getProperty("validationQuery");
        if (validationQuery != null && !validationQuery.isEmpty()) {
            dataSource.setValidationQuery(validationQuery);
        }

        // 预编译语句缓存
        dataSource.setPoolPreparedStatements(
            Boolean.parseBoolean(props.getProperty("poolPreparedStatements", "true")));
        dataSource.setMaxPoolPreparedStatementPerConnectionSize(
            Integer.parseInt(props.getProperty("maxPoolPreparedStatementPerConnectionSize", "20")));

        // 连接回收
        dataSource.setRemoveAbandoned(
            Boolean.parseBoolean(props.getProperty("removeAbandoned", "false")));
        dataSource.setRemoveAbandonedTimeout(
            Integer.parseInt(props.getProperty("removeAbandonedTimeout", "1800")));
        dataSource.setLogAbandoned(
            Boolean.parseBoolean(props.getProperty("logAbandoned", "true")));

        // 初始化连接池
        dataSource.init();

        logger.info("Druid 数据源创建成功");
        return dataSource;
    }

    /**
     * 创建可刷新的数据源
     *
     * @param props 配置属性
     * @return 可刷新的数据源包装
     */
    public RefreshableDataSource createRefreshableDataSource(Properties props) throws Exception {
        return new RefreshableDataSource(props, this);
    }

    /**
     * 可刷新的数据源包装类
     */
    public static class RefreshableDataSource extends DelegatingDataSource {

        private final Properties originalProps;
        private final DruidSpringDataSourceFactory factory;

        public RefreshableDataSource(Properties props, DruidSpringDataSourceFactory factory) throws Exception {
            super(factory.createDataSource(props));
            this.originalProps = new Properties();
            this.originalProps.putAll(props);
            this.factory = factory;
        }

        /**
         * 刷新数据源（重新从配置中心加载）
         */
        public void refresh() throws Exception {
            logger.info("刷新数据源...");

            DataSource newDataSource = factory.createDataSource(originalProps);
            setTargetDataSource(newDataSource);

            logger.info("数据源刷新完成");
        }

        /**
         * 获取原始配置
         */
        public Properties getOriginalProps() {
            Properties copy = new Properties();
            copy.putAll(originalProps);
            return copy;
        }
    }

    // ==================== Getters and Setters ====================

    public ConfigCenterFallbackManager getFallbackManager() {
        return fallbackManager;
    }

    public void setFallbackManager(ConfigCenterFallbackManager fallbackManager) {
        this.fallbackManager = fallbackManager;
        this.interceptor = new PasswordPropertiesInterceptor(fallbackManager);
    }

    public PasswordPropertiesInterceptor getInterceptor() {
        return interceptor;
    }
}
