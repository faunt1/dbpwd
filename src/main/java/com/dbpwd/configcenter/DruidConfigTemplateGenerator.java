package com.dbpwd.configcenter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Druid 配置模板生成器
 * 负责生成带 placeholder 的配置模板文件
 *
 * 功能：
 * - 生成带 placeholder 的配置模板
 * - 将密码字段替换为 @config(uid) placeholder
 * - 支持连接池的所有配置项
 * - 保存模板到文件
 */
public class DruidConfigTemplateGenerator {

    private static final Logger logger = Logger.getLogger(DruidConfigTemplateGenerator.class.getName());
    private static final String DEFAULT_ALGORITHM = "SM4";

    // 基本配置
    private String driverClassName;
    private String url;
    private String username;
    private String passwordUid;  // 用于 placeholder 的 uid

    // 连接池配置
    private int initialSize = 5;
    private int minIdle = 5;
    private int maxActive = 20;
    private long maxWait = 60000L;
    private long timeBetweenEvictionRunsMillis = 60000L;
    private long minEvictableIdleTimeMillis = 300000L;

    // 连接验证
    private String validationQuery = "SELECT 1";
    private boolean testWhileIdle = true;
    private boolean testOnBorrow = false;
    private boolean testOnReturn = false;

    // 预编译语句缓存
    private boolean poolPreparedStatements = true;
    private int maxPoolPreparedStatementPerConnectionSize = 20;

    // 连接回收
    private boolean removeAbandoned = false;
    private int removeAbandonedTimeout = 1800;
    private boolean logAbandoned = true;

    // 加密配置
    private String encryptionKey;
    private String encryptionKeyId;  // 密钥 ID，用于 @key() placeholder
    private String algorithm = DEFAULT_ALGORITHM;

    /**
     * 默认构造函数
     */
    public DruidConfigTemplateGenerator() {
    }

    /**
     * 生成模板 Properties
     *
     * @return Properties
     */
    public Properties generateTemplate() {
        if (passwordUid == null || passwordUid.isEmpty()) {
            throw new IllegalStateException("必须设置 passwordUid 才能生成模板");
        }

        Properties template = new Properties();

        // 基本配置
        if (driverClassName != null && !driverClassName.isEmpty()) {
            template.setProperty("driverClassName", driverClassName);
        }
        if (url != null && !url.isEmpty()) {
            template.setProperty("url", url);
        }
        if (username != null && !username.isEmpty()) {
            template.setProperty("username", username);
        }

        // 使用 placeholder 替代密码
        template.setProperty("password", "@config(" + passwordUid + ")");

        // 连接池配置
        template.setProperty("initialSize", String.valueOf(initialSize));
        template.setProperty("minIdle", String.valueOf(minIdle));
        template.setProperty("maxActive", String.valueOf(maxActive));
        template.setProperty("maxWait", String.valueOf(maxWait));
        template.setProperty("timeBetweenEvictionRunsMillis", String.valueOf(timeBetweenEvictionRunsMillis));
        template.setProperty("minEvictableIdleTimeMillis", String.valueOf(minEvictableIdleTimeMillis));

        // 连接验证
        if (validationQuery != null && !validationQuery.isEmpty()) {
            template.setProperty("validationQuery", validationQuery);
        }
        template.setProperty("testWhileIdle", String.valueOf(testWhileIdle));
        template.setProperty("testOnBorrow", String.valueOf(testOnBorrow));
        template.setProperty("testOnReturn", String.valueOf(testOnReturn));

        // 预编译语句缓存
        template.setProperty("poolPreparedStatements", String.valueOf(poolPreparedStatements));
        template.setProperty("maxPoolPreparedStatementPerConnectionSize", String.valueOf(maxPoolPreparedStatementPerConnectionSize));

        // 连接回收
        template.setProperty("removeAbandoned", String.valueOf(removeAbandoned));
        template.setProperty("removeAbandonedTimeout", String.valueOf(removeAbandonedTimeout));
        template.setProperty("logAbandoned", String.valueOf(logAbandoned));

        // 密码解密配置 - 使用密钥 placeholder 或直接使用密钥
        StringBuilder connectionProps = new StringBuilder();
        connectionProps.append("config.decrypt=true;");

        if (encryptionKeyId != null && !encryptionKeyId.isEmpty()) {
            // 使用 placeholder 形式
            connectionProps.append("config.decrypt.key=@key(").append(encryptionKeyId).append(");");
        } else if (encryptionKey != null && !encryptionKey.isEmpty()) {
            // 直接使用密钥
            connectionProps.append("config.decrypt.key=").append(encryptionKey).append(";");
        }

        connectionProps.append("config.decrypt.algorithm=").append(algorithm);
        template.setProperty("druid.connectionProperties", connectionProps.toString());

        return template;
    }

    /**
     * 保存模板到文件
     *
     * @param filePath 文件路径
     * @throws IOException 写入异常
     */
    public void saveTemplate(String filePath) throws IOException {
        Properties template = generateTemplate();
        saveTemplate(filePath, template);
    }

    /**
     * 保存指定的模板到文件
     *
     * @param filePath 文件路径
     * @param template Properties 对象
     * @throws IOException 写入异常
     */
    private void saveTemplate(String filePath, Properties template) throws IOException {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // 添加注释信息
        StringBuilder comments = new StringBuilder();
        comments.append("Druid Connection Pool Configuration - Template\n");
        comments.append("Password UID: ").append(passwordUid).append("\n");
        comments.append("Algorithm: ").append(algorithm).append("\n");
        comments.append("Generated: ").append(new java.util.Date()).append("\n");
        comments.append("Note: password field uses @config(uid) placeholder\n");

        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(file), StandardCharsets.UTF_8)) {
            template.store(writer, comments.toString());
        }

        logger.info("模板文件已保存: " + filePath);
    }

    /**
     * 生成并保存模板文件
     *
     * @param filePath 文件路径
     * @param passwordUid 密码 UID
     * @throws IOException 写入异常
     */
    public void generateAndSave(String filePath, String passwordUid) throws IOException {
        this.passwordUid = passwordUid;
        saveTemplate(filePath);
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

    public String getPasswordUid() {
        return passwordUid;
    }

    public void setPasswordUid(String passwordUid) {
        this.passwordUid = passwordUid;
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

    public String getValidationQuery() {
        return validationQuery;
    }

    public void setValidationQuery(String validationQuery) {
        this.validationQuery = validationQuery;
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

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public void setEncryptionKey(String encryptionKey) {
        this.encryptionKey = encryptionKey;
    }

    public String getEncryptionKeyId() {
        return encryptionKeyId;
    }

    public void setEncryptionKeyId(String encryptionKeyId) {
        this.encryptionKeyId = encryptionKeyId;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
}
