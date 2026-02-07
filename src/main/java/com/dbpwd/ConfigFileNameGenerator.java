package com.dbpwd;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 配置文件名生成器
 * 根据数据库连接信息自动生成唯一的配置文件名
 */
public class ConfigFileNameGenerator {

    /**
     * 文件命名策略枚举
     */
    public enum NamingStrategy {
        HOST_PORT_DB,           // 192_168_1_100_3306_userdb.properties
        ENV_HOST_PORT_DB,       // prod_192_168_1_100_3306_userdb.properties
        DB_HOST_PORT,           // userdb_192_168_1_100_3306.properties
        ENV_DB_HOST_PORT        // prod_userdb_192_168_1_100_3306.properties
    }

    private String environment = "prod";
    private NamingStrategy namingStrategy = NamingStrategy.HOST_PORT_DB;
    private String fileType = "properties";

    private static final String DEFAULT_DIR_TEMPLATE = "{dbType}";
    private static final String DEFAULT_FILE_TEMPLATE = "{env}_{host}_{port}_{database}.{type}";

    /**
     * 默认构造函数
     */
    public ConfigFileNameGenerator() {
    }

    /**
     * 指定环境构造函数
     */
    public ConfigFileNameGenerator(String environment) {
        this.environment = environment;
    }

    /**
     * 指定环境和命名策略构造函数
     */
    public ConfigFileNameGenerator(String environment, NamingStrategy namingStrategy) {
        this.environment = environment;
        this.namingStrategy = namingStrategy;
    }

    /**
     * 根据JDBC URL生成文件名
     */
    public String generate(String jdbcUrl) {
        JdbcUrlParser.DatabaseInfo dbInfo = JdbcUrlParser.parse(jdbcUrl);
        if (dbInfo == null) {
            throw new IllegalArgumentException("无法解析JDBC URL: " + jdbcUrl);
        }
        return generate(dbInfo);
    }

    /**
     * 根据数据库信息生成文件名
     */
    public String generate(JdbcUrlParser.DatabaseInfo dbInfo) {
        if (dbInfo == null) {
            throw new IllegalArgumentException("数据库信息不能为空");
        }

        switch (namingStrategy) {
            case HOST_PORT_DB:
                return generateHostPortDb(dbInfo);
            case ENV_HOST_PORT_DB:
                return generateEnvHostPortDb(dbInfo);
            case DB_HOST_PORT:
                return generateDbHostPort(dbInfo);
            case ENV_DB_HOST_PORT:
                return generateEnvDbHostPort(dbInfo);
            default:
                return generateHostPortDb(dbInfo);
        }
    }

    /**
     * 生成完整路径（包含目录）
     */
    public String getFullPath(String jdbcUrl) {
        JdbcUrlParser.DatabaseInfo dbInfo = JdbcUrlParser.parse(jdbcUrl);
        if (dbInfo == null) {
            throw new IllegalArgumentException("无法解析JDBC URL: " + jdbcUrl);
        }
        return getFullPath(dbInfo);
    }

    /**
     * 生成完整路径（包含目录）
     */
    public String getFullPath(JdbcUrlParser.DatabaseInfo dbInfo) {
        String dbType = dbInfo.getDbType();
        if (dbType == null) {
            dbType = "default";
        }
        return dbType + "/" + generate(dbInfo);
    }

    /**
     * 生成格式: {host}_{port}_{database}.properties
     */
    private String generateHostPortDb(JdbcUrlParser.DatabaseInfo dbInfo) {
        String safeHost = sanitizeString(dbInfo.getHost());
        int port = dbInfo.getPort();
        String db = sanitizeString(dbInfo.getDatabase());
        return safeHost + "_" + port + "_" + db + "." + fileType;
    }

    /**
     * 生成格式: {env}_{host}_{port}_{database}.properties
     */
    private String generateEnvHostPortDb(JdbcUrlParser.DatabaseInfo dbInfo) {
        String safeEnv = sanitizeString(environment);
        String safeHost = sanitizeString(dbInfo.getHost());
        int port = dbInfo.getPort();
        String db = sanitizeString(dbInfo.getDatabase());
        return safeEnv + "_" + safeHost + "_" + port + "_" + db + "." + fileType;
    }

    /**
     * 生成格式: {database}_{host}_{port}.properties
     */
    private String generateDbHostPort(JdbcUrlParser.DatabaseInfo dbInfo) {
        String db = sanitizeString(dbInfo.getDatabase());
        String safeHost = sanitizeString(dbInfo.getHost());
        int port = dbInfo.getPort();
        return db + "_" + safeHost + "_" + port + "." + fileType;
    }

    /**
     * 生成格式: {env}_{database}_{host}_{port}.properties
     */
    private String generateEnvDbHostPort(JdbcUrlParser.DatabaseInfo dbInfo) {
        String safeEnv = sanitizeString(environment);
        String db = sanitizeString(dbInfo.getDatabase());
        String safeHost = sanitizeString(dbInfo.getHost());
        int port = dbInfo.getPort();
        return safeEnv + "_" + db + "_" + safeHost + "_" + port + "." + fileType;
    }

    /**
     * 清理字符串，替换非法字符
     */
    private String sanitizeString(String input) {
        if (input == null || input.isEmpty()) {
            return "default";
        }

        return input.replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    /**
     * 添加时间戳到文件名
     */
    public String generateWithTimestamp(String jdbcUrl) {
        String baseName = generate(jdbcUrl);
        int dotIndex = baseName.lastIndexOf('.');
        if (dotIndex > 0) {
            return baseName.substring(0, dotIndex) + "_" + getCurrentTimestamp() + "." + fileType;
        }
        return baseName + "_" + getCurrentTimestamp() + "." + fileType;
    }

    /**
     * 获取当前时间戳（yyyyMMddHHmmss）
     */
    private String getCurrentTimestamp() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        return sdf.format(new Date());
    }

    /**
     * 使用模板生成文件名
     */
    public String generateFromTemplate(String jdbcUrl, String fileTemplate, String dirTemplate) {
        JdbcUrlParser.DatabaseInfo dbInfo = JdbcUrlParser.parse(jdbcUrl);
        if (dbInfo == null) {
            throw new IllegalArgumentException("无法解析JDBC URL: " + jdbcUrl);
        }

        String fileName = fileTemplate.replace("{dbType}", nullToEmpty(dbInfo.getDbType()))
                .replace("{env}", sanitizeString(environment))
                .replace("{host}", sanitizeString(dbInfo.getHost()))
                .replace("{port}", String.valueOf(dbInfo.getPort()))
                .replace("{database}", sanitizeString(dbInfo.getDatabase()))
                .replace("{timestamp}", getCurrentTimestamp())
                .replace("{type}", fileType);

        String directory = dirTemplate.replace("{dbType}", nullToEmpty(dbInfo.getDbType()))
                .replace("{env}", sanitizeString(environment));

        return directory + "/" + fileName;
    }

    private String nullToEmpty(String str) {
        return str == null ? "default" : str;
    }

    // ==================== Getters and Setters ====================

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public NamingStrategy getNamingStrategy() {
        return namingStrategy;
    }

    public void setNamingStrategy(NamingStrategy namingStrategy) {
        this.namingStrategy = namingStrategy;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }
}