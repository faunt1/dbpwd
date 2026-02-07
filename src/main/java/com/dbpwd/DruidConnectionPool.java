package com.dbpwd;

import com.alibaba.druid.pool.DruidDataSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Druid连接池工厂类
 * 用于创建和管理Druid数据源
 */
public class DruidConnectionPool {

    private DruidDataSource dataSource;
    private String configFilePath;

    /**
     * 默认构造函数
     */
    public DruidConnectionPool() {
    }

    /**
     * 使用配置文件路径构造
     */
    public DruidConnectionPool(String configFilePath) throws Exception {
        this.configFilePath = configFilePath;
        initialize(configFilePath);
    }

    /**
     * 使用Properties初始化
     */
    public DruidConnectionPool(Properties props) throws Exception {
        initialize(props);
    }

    /**
     * 使用DruidConfigGenerator初始化
     */
    public DruidConnectionPool(DruidConfigGenerator generator) throws Exception {
        initialize(generator.generateProperties());
    }

    /**
     * 使用配置文件初始化
     */
    public void initialize(String configFilePath) throws Exception {
        this.configFilePath = configFilePath;
        Properties props = new Properties();

        File configFile = new File(configFilePath);
        if (!configFile.exists()) {
            throw new IllegalArgumentException("配置文件不存在: " + configFilePath);
        }

        try (FileInputStream fis = new FileInputStream(configFile);
             InputStreamReader reader = new InputStreamReader(fis, "UTF-8")) {
            props.load(reader);
        }

        initialize(props);
    }

    /**
     * 使用Properties初始化
     */
    public void initialize(Properties props) throws SQLException {
        dataSource = new DruidDataSource();

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
        dataSource.setTimeBetweenEvictionRunsMillis(Long.parseLong(props.getProperty("timeBetweenEvictionRunsMillis", "60000")));
        dataSource.setMinEvictableIdleTimeMillis(Long.parseLong(props.getProperty("minEvictableIdleTimeMillis", "300000")));

        // 连接验证
        dataSource.setTestWhileIdle(Boolean.parseBoolean(props.getProperty("testWhileIdle", "true")));
        dataSource.setTestOnBorrow(Boolean.parseBoolean(props.getProperty("testOnBorrow", "false")));
        dataSource.setTestOnReturn(Boolean.parseBoolean(props.getProperty("testOnReturn", "false")));

        String validationQuery = props.getProperty("validationQuery");
        if (validationQuery != null && !validationQuery.isEmpty()) {
            dataSource.setValidationQuery(validationQuery);
        }

        // 预编译语句缓存
        dataSource.setPoolPreparedStatements(Boolean.parseBoolean(props.getProperty("poolPreparedStatements", "true")));
        dataSource.setMaxPoolPreparedStatementPerConnectionSize(Integer.parseInt(props.getProperty("maxPoolPreparedStatementPerConnectionSize", "20")));

        // 连接回收
        dataSource.setRemoveAbandoned(Boolean.parseBoolean(props.getProperty("removeAbandoned", "false")));
        dataSource.setRemoveAbandonedTimeout(Integer.parseInt(props.getProperty("removeAbandonedTimeout", "1800")));
        dataSource.setLogAbandoned(Boolean.parseBoolean(props.getProperty("logAbandoned", "true")));

        // 设置密码回调（用于解密）
        String connectionProperties = props.getProperty("druid.connectionProperties", "");
        if (connectionProperties.contains("config.decrypt=true")) {
            dataSource.setPasswordCallback(new DruidEncryptedPasswordCallback());
        }

        // 初始化连接池
        dataSource.init();
    }

    /**
     * 获取连接
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("连接池未初始化");
        }
        return dataSource.getConnection();
    }

    /**
     * 获取连接（指定校验SQL）
     */
    public Connection getConnection(String sql) throws SQLException {
        if (dataSource == null) {
            throw new SQLException("连接池未初始化");
        }
        dataSource.setConnectionProperties("druid.validationQuery=" + sql);
        return dataSource.getConnection();
    }

    /**
     * 获取Druid数据源
     */
    public DruidDataSource getDataSource() {
        return dataSource;
    }

    /**
     * 关闭连接池
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    /**
     * 是否已关闭
     */
    public boolean isClosed() {
        return dataSource == null || dataSource.isClosed();
    }

    /**
     * 从配置中心初始化连接池
     *
     * @param configCenterUrl 配置中心 URL
     * @param apiKey API 密钥
     * @param templateName 模板名称
     * @throws Exception 初始化异常
     */
    public void initializeFromConfigCenter(String configCenterUrl, String apiKey, String templateName) throws Exception {
        initializeFromConfigCenter(configCenterUrl, apiKey, templateName, null);
    }

    /**
     * 从配置中心初始化连接池（指定密钥映射）
     *
     * @param configCenterUrl 配置中心 URL
     * @param apiKey API 密钥
     * @param templateName 模板名称
     * @param keyMap 密钥映射（keyId -> key）
     * @throws Exception 初始化异常
     */
    public void initializeFromConfigCenter(String configCenterUrl, String apiKey, String templateName, java.util.Map<String, String> keyMap) throws Exception {
        com.dbpwd.configcenter.ConfigCenterTemplateResolver resolver =
            new com.dbpwd.configcenter.ConfigCenterTemplateResolver(configCenterUrl, apiKey);

        // 解析模板并替换 placeholder
        Properties resolvedProps = resolver.resolveTemplate(templateName, keyMap);

        // 调用原有初始化方法
        initialize(resolvedProps);
    }

    /**
     * 从本地配置模板初始化连接池
     *
     * @param templateFilePath 模板文件路径
     * @param passwordFilePath 密码文件路径
     * @throws Exception 初始化异常
     */
    public void initializeFromLocalTemplate(String templateFilePath, String passwordFilePath) throws Exception {
        initializeFromLocalTemplate(templateFilePath, passwordFilePath, null);
    }

    /**
     * 从本地配置模板初始化连接池（指定密钥映射）
     *
     * @param templateFilePath 模板文件路径
     * @param passwordFilePath 密码文件路径
     * @param keyMap 密钥映射（keyId -> key）
     * @throws Exception 初始化异常
     */
    public void initializeFromLocalTemplate(String templateFilePath, String passwordFilePath, java.util.Map<String, String> keyMap) throws Exception {
        // 解析本地模板和密码文件
        Properties resolvedProps = com.dbpwd.configcenter.ConfigCenterTemplateResolver
            .resolveTemplateLocal(templateFilePath, passwordFilePath, keyMap);

        // 调用原有初始化方法
        initialize(resolvedProps);
    }

    /**
     * 重新加载配置
     */
    public void reload() throws Exception {
        if (configFilePath == null) {
            throw new IllegalStateException("未设置配置文件路径，无法重新加载");
        }
        close();
        initialize(configFilePath);
    }

    /**
     * 获取连接池状态信息
     */
    public String getStatusInfo() {
        if (dataSource == null) {
            return "连接池未初始化";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("=== Druid连接池状态 ===\n");
        sb.append("URL: ").append(dataSource.getUrl()).append("\n");
        sb.append("用户名: ").append(dataSource.getUsername()).append("\n");
        sb.append("活跃连接数: ").append(dataSource.getActiveCount()).append("\n");
        sb.append("空闲连接数: ").append(dataSource.getPoolingCount()).append("\n");
        sb.append("初始连接数: ").append(dataSource.getInitialSize()).append("\n");
        sb.append("最小空闲连接: ").append(dataSource.getMinIdle()).append("\n");
        sb.append("最大活跃连接: ").append(dataSource.getMaxActive()).append("\n");
        sb.append("等待获取连接的线程数: ").append(dataSource.getWaitThreadCount()).append("\n");

        return sb.toString();
    }

    /**
     * 测试连接
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * 创建建造者
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 建造者模式
     */
    public static class Builder {
        private DruidConfigGenerator generator;

        public Builder(DruidConfigGenerator generator) {
            this.generator = generator;
        }

        public Builder() {
            this.generator = new DruidConfigGenerator();
        }

        public Builder driverClass(String driverClass) {
            generator.setDriverClassName(driverClass);
            return this;
        }

        public Builder url(String url) {
            generator.setUrl(url);
            return this;
        }

        public Builder username(String username) {
            generator.setUsername(username);
            return this;
        }

        public Builder password(String password) {
            generator.setPassword(password);
            return this;
        }

        public Builder encryptKey(String key) {
            generator.setEncryptionKey(key);
            return this;
        }

        public Builder algorithm(String algorithm) {
            generator.setAlgorithm(algorithm);
            return this;
        }

        public Builder initialSize(int size) {
            generator.setInitialSize(size);
            return this;
        }

        public Builder minIdle(int idle) {
            generator.setMinIdle(idle);
            return this;
        }

        public Builder maxActive(int active) {
            generator.setMaxActive(active);
            return this;
        }

        public Builder autoDetectDatabase() {
            generator.autoDetectDatabase();
            return this;
        }

        public DruidConnectionPool build() throws Exception {
            return new DruidConnectionPool(generator);
        }
    }
}