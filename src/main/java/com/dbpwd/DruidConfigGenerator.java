package com.dbpwd;

import java.io.*;
import java.util.Properties;

/**
 * Druid配置文件生成器
 * 支持密码加密和配置文件生成
 */
public class DruidConfigGenerator {

    private String driverClassName;
    private String url;
    private String username;
    private String password;
    private String encryptedPassword;
    private String encryptionKey;
    private String algorithm = "SM4";  // 默认使用国密SM4

    private int initialSize = 5;
    private int minIdle = 5;
    private int maxActive = 20;
    private long maxWait = 60000;

    private long timeBetweenEvictionRunsMillis = 60000;
    private long minEvictableIdleTimeMillis = 300000;

    private boolean testWhileIdle = true;
    private boolean testOnBorrow = false;
    private boolean testOnReturn = false;

    private String validationQuery = "SELECT 1";

    private boolean poolPreparedStatements = true;
    private int maxPoolPreparedStatementPerConnectionSize = 20;

    private boolean removeAbandoned = false;
    private int removeAbandonedTimeout = 1800;
    private boolean logAbandoned = true;

    private static final String DEFAULT_TEMPLATE = "config/druid.properties.template";

    /**
     * 默认构造函数
     */
    public DruidConfigGenerator() {
    }

    /**
     * 全参构造函数
     */
    public DruidConfigGenerator(String driverClassName, String url, String username, String password) {
        this.driverClassName = driverClassName;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    /**
     * 加密密码
     */
    public void encryptPassword() throws Exception {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("密码不能为空");
        }

        // 如果没有指定密钥，生成一个随机密钥
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            encryptionKey = DruidEncryptedPasswordCallback.generateRandomKey(16);
        }

        // 使用指定算法加密
        if ("SM4".equalsIgnoreCase(algorithm)) {
            encryptedPassword = DruidEncryptedPasswordCallback.encodeSM4(password, encryptionKey);
        } else {
            encryptedPassword = DruidEncryptedPasswordCallback.encodeAES(password, encryptionKey);
        }
    }

    /**
     * 解密密码（用于验证）
     */
    public String decryptPassword(String encryptedPwd, String key) throws Exception {
        return DruidEncryptedPasswordCallback.decryptPassword(encryptedPwd, key, algorithm);
    }

    /**
     * 生成Properties配置
     */
    public Properties generateProperties() {
        Properties props = new Properties();

        // 基本配置
        props.setProperty("driverClassName", driverClassName);
        props.setProperty("url", url);
        props.setProperty("username", username);

        if (encryptedPassword != null) {
            props.setProperty("password", encryptedPassword);
            // 添加密码解密配置
            StringBuilder connectionProps = new StringBuilder();
            connectionProps.append("config.decrypt=true;");
            connectionProps.append("config.decrypt.key=").append(encryptionKey).append(";");
            connectionProps.append("config.decrypt.algorithm=").append(algorithm);
            props.setProperty("druid.connectionProperties", connectionProps.toString());
        } else {
            props.setProperty("password", password);
        }

        // 连接池配置
        props.setProperty("initialSize", String.valueOf(initialSize));
        props.setProperty("minIdle", String.valueOf(minIdle));
        props.setProperty("maxActive", String.valueOf(maxActive));
        props.setProperty("maxWait", String.valueOf(maxWait));

        // 空闲连接检查
        props.setProperty("timeBetweenEvictionRunsMillis", String.valueOf(timeBetweenEvictionRunsMillis));
        props.setProperty("minEvictableIdleTimeMillis", String.valueOf(minEvictableIdleTimeMillis));

        // 连接验证
        props.setProperty("testWhileIdle", String.valueOf(testWhileIdle));
        props.setProperty("testOnBorrow", String.valueOf(testOnBorrow));
        props.setProperty("testOnReturn", String.valueOf(testOnReturn));

        if (validationQuery != null && !validationQuery.isEmpty()) {
            props.setProperty("validationQuery", validationQuery);
        }

        // 预编译语句缓存
        props.setProperty("poolPreparedStatements", String.valueOf(poolPreparedStatements));
        props.setProperty("maxPoolPreparedStatementPerConnectionSize", String.valueOf(maxPoolPreparedStatementPerConnectionSize));

        // 连接回收
        props.setProperty("removeAbandoned", String.valueOf(removeAbandoned));
        props.setProperty("removeAbandonedTimeout", String.valueOf(removeAbandonedTimeout));
        props.setProperty("logAbandoned", String.valueOf(logAbandoned));

        return props;
    }

    /**
     * 保存配置到文件
     */
    public void saveToFile(String filePath) throws IOException {
        Properties props = generateProperties();

        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(file);
             OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8")) {
            props.store(writer, "Druid Database Connection Pool Configuration");
        }
    }

    /**
     * 保存配置到properties格式（便于查看）
     */
    public void saveToPropertiesFile(String filePath) throws IOException {
        Properties props = generateProperties();

        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            props.store(fos, "Druid Database Connection Pool Configuration");
        }
    }

    /**
     * 从文件加载配置
     */
    public void loadFromFile(String filePath) throws IOException {
        Properties props = new Properties();

        try (FileInputStream fis = new FileInputStream(filePath);
             InputStreamReader reader = new InputStreamReader(fis, "UTF-8")) {
            props.load(reader);
        }

        // 加载基本配置
        this.driverClassName = props.getProperty("driverClassName");
        this.url = props.getProperty("url");
        this.username = props.getProperty("username");
        this.password = props.getProperty("password");
        this.encryptedPassword = props.getProperty("password");

        // 解析连接属性获取加密配置
        String connectionProperties = props.getProperty("druid.connectionProperties", "");
        if (connectionProperties.contains("config.decrypt.key=")) {
            String[] parts = connectionProperties.split(";");
            for (String part : parts) {
                if (part.startsWith("config.decrypt.key=")) {
                    this.encryptionKey = part.substring("config.decrypt.key=".length());
                } else if (part.startsWith("config.decrypt.algorithm=")) {
                    this.algorithm = part.substring("config.decrypt.algorithm=".length());
                }
            }
        }

        // 加载连接池配置
        this.initialSize = Integer.parseInt(props.getProperty("initialSize", "5"));
        this.minIdle = Integer.parseInt(props.getProperty("minIdle", "5"));
        this.maxActive = Integer.parseInt(props.getProperty("maxActive", "20"));
        this.maxWait = Long.parseLong(props.getProperty("maxWait", "60000"));

        this.timeBetweenEvictionRunsMillis = Long.parseLong(props.getProperty("timeBetweenEvictionRunsMillis", "60000"));
        this.minEvictableIdleTimeMillis = Long.parseLong(props.getProperty("minEvictableIdleTimeMillis", "300000"));

        this.testWhileIdle = Boolean.parseBoolean(props.getProperty("testWhileIdle", "true"));
        this.testOnBorrow = Boolean.parseBoolean(props.getProperty("testOnBorrow", "false"));
        this.testOnReturn = Boolean.parseBoolean(props.getProperty("testOnReturn", "false"));

        this.validationQuery = props.getProperty("validationQuery", "SELECT 1");

        this.poolPreparedStatements = Boolean.parseBoolean(props.getProperty("poolPreparedStatements", "true"));
        this.maxPoolPreparedStatementPerConnectionSize = Integer.parseInt(props.getProperty("maxPoolPreparedStatementPerConnectionSize", "20"));

        this.removeAbandoned = Boolean.parseBoolean(props.getProperty("removeAbandoned", "false"));
        this.removeAbandonedTimeout = Integer.parseInt(props.getProperty("removeAbandonedTimeout", "1800"));
        this.logAbandoned = Boolean.parseBoolean(props.getProperty("logAbandoned", "true"));
    }

    /**
     * 从JDBC URL自动推断数据库类型和validationQuery
     */
    public void autoDetectDatabase() {
        if (url == null || url.isEmpty()) {
            return;
        }

        // 解析数据库类型
        JdbcUrlParser.DatabaseInfo dbInfo = JdbcUrlParser.parse(url);

        if (dbInfo != null) {
            // 设置合适的validationQuery
            if (dbInfo.getDbType() != null) {
                switch (dbInfo.getDbType()) {
                    case "MySQL":
                        this.validationQuery = "SELECT 1";
                        if (this.driverClassName == null || this.driverClassName.isEmpty()) {
                            this.driverClassName = "com.mysql.cj.jdbc.Driver";
                        }
                        break;
                    case "PostgreSQL":
                        this.validationQuery = "SELECT 1";
                        if (this.driverClassName == null || this.driverClassName.isEmpty()) {
                            this.driverClassName = "org.postgresql.Driver";
                        }
                        break;
                    case "Oracle":
                        this.validationQuery = "SELECT 1 FROM DUAL";
                        if (this.driverClassName == null || this.driverClassName.isEmpty()) {
                            this.driverClassName = "oracle.jdbc.OracleDriver";
                        }
                        break;
                    case "SQLServer":
                        this.validationQuery = "SELECT 1";
                        if (this.driverClassName == null || this.driverClassName.isEmpty()) {
                            this.driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
                        }
                        break;
                    default:
                        this.validationQuery = "SELECT 1";
                }
            }
        }
    }

    /**
     * 创建配置文件目录
     */
    public void createConfigDirectory(String filePath) throws IOException {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
    }

    /**
     * 获取配置摘要信息
     */
    public String getConfigSummary() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Druid配置摘要 ===\n");
        sb.append("驱动类: ").append(driverClassName).append("\n");
        sb.append("连接URL: ").append(url).append("\n");
        sb.append("用户名: ").append(username).append("\n");
        sb.append("加密算法: ").append(algorithm).append("\n");
        sb.append("连接池配置:\n");
        sb.append("  初始大小: ").append(initialSize).append("\n");
        sb.append("  最小空闲: ").append(minIdle).append("\n");
        sb.append("  最大活跃: ").append(maxActive).append("\n");
        sb.append("  最大等待: ").append(maxWait).append("ms\n");
        return sb.toString();
    }

    // ==================== Getters and Setters ====================

    public String getDriverClassName() {
        return driverClassName;
    }

    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEncryptedPassword() {
        return encryptedPassword;
    }

    public void setEncryptedPassword(String encryptedPassword) {
        this.encryptedPassword = encryptedPassword;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public int getInitialSize() {
        return initialSize;
    }

    public void setInitialSize(int initialSize) {
        this.initialSize = initialSize;
    }

    public int getMinIdle() {
        return minIdle;
    }

    public void setMinIdle(int minIdle) {
        this.minIdle = minIdle;
    }

    public int getMaxActive() {
        return maxActive;
    }

    public void setMaxActive(int maxActive) {
        this.maxActive = maxActive;
    }

    public long getMaxWait() {
        return maxWait;
    }

    public void setMaxWait(long maxWait) {
        this.maxWait = maxWait;
    }

    public long getTimeBetweenEvictionRunsMillis() {
        return timeBetweenEvictionRunsMillis;
    }

    public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
        this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
    }

    public long getMinEvictableIdleTimeMillis() {
        return minEvictableIdleTimeMillis;
    }

    public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
        this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
    }

    public boolean isTestWhileIdle() {
        return testWhileIdle;
    }

    public void setTestWhileIdle(boolean testWhileIdle) {
        this.testWhileIdle = testWhileIdle;
    }

    public boolean isTestOnBorrow() {
        return testOnBorrow;
    }

    public void setTestOnBorrow(boolean testOnBorrow) {
        this.testOnBorrow = testOnBorrow;
    }

    public boolean isTestOnReturn() {
        return testOnReturn;
    }

    public void setTestOnReturn(boolean testOnReturn) {
        this.testOnReturn = testOnReturn;
    }

    public String getValidationQuery() {
        return validationQuery;
    }

    public void setValidationQuery(String validationQuery) {
        this.validationQuery = validationQuery;
    }

    public boolean isPoolPreparedStatements() {
        return poolPreparedStatements;
    }

    public void setPoolPreparedStatements(boolean poolPreparedStatements) {
        this.poolPreparedStatements = poolPreparedStatements;
    }

    public int getMaxPoolPreparedStatementPerConnectionSize() {
        return maxPoolPreparedStatementPerConnectionSize;
    }

    public void setMaxPoolPreparedStatementPerConnectionSize(int maxPoolPreparedStatementPerConnectionSize) {
        this.maxPoolPreparedStatementPerConnectionSize = maxPoolPreparedStatementPerConnectionSize;
    }

    public boolean isRemoveAbandoned() {
        return removeAbandoned;
    }

    public void setRemoveAbandoned(boolean removeAbandoned) {
        this.removeAbandoned = removeAbandoned;
    }

    public int getRemoveAbandonedTimeout() {
        return removeAbandonedTimeout;
    }

    public void setRemoveAbandonedTimeout(int removeAbandonedTimeout) {
        this.removeAbandonedTimeout = removeAbandonedTimeout;
    }

    public boolean isLogAbandoned() {
        return logAbandoned;
    }

    public void setLogAbandoned(boolean logAbandoned) {
        this.logAbandoned = logAbandoned;
    }
}