package com.dbpwd.core;

import com.dbpwd.model.DatabaseEntry;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 数据库连接测试工具
 *
 * 用于测试数据库连接是否有效
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class ConnectionTester {

    private static final Logger logger = Logger.getLogger(ConnectionTester.class.getName());

    private static final int DEFAULT_TIMEOUT = 5; // 默认5秒超时
    private static final int DEFAULT_QUERY_TIMEOUT = 3; // 查询验证超时

    /**
     * 默认的JDBC驱动类名映射
     */
    private static final Map<String, String> DEFAULT_DRIVERS = new HashMap<>();

    static {
        DEFAULT_DRIVERS.put("mysql", "com.mysql.cj.jdbc.Driver");
        DEFAULT_DRIVERS.put("postgresql", "org.postgresql.Driver");
        DEFAULT_DRIVERS.put("oracle", "oracle.jdbc.OracleDriver");
        DEFAULT_DRIVERS.put("sqlserver", "com.microsoft.sqlserver.jdbc.SQLServerDriver");
        DEFAULT_DRIVERS.put("db2", "com.ibm.db2.jcc.DB2Driver");
        DEFAULT_DRIVERS.put("sybase", "com.sybase.jdbc4.jdbc.SybDriver");
        DEFAULT_DRIVERS.put("h2", "org.h2.Driver");
        DEFAULT_DRIVERS.put("hsqldb", "org.hsqldb.jdbcDriver");
    }

    /**
     * 连接测试结果
     */
    public static class TestResult {
        private final boolean success;
        private final String message;
        private final long duration;
        private final Throwable error;

        public TestResult(boolean success, String message, long duration, Throwable error) {
            this.success = success;
            this.message = message;
            this.duration = duration;
            this.error = error;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public long getDuration() {
            return duration;
        }

        public Throwable getError() {
            return error;
        }

        @Override
        public String toString() {
            if (success) {
                return String.format("连接成功 (耗时: %dms)", duration);
            } else {
                return String.format("连接失败: %s (耗时: %dms)", message, duration);
            }
        }
    }

    /**
     * 测试数据库连接
     *
     * @param entry 数据库条目
     * @return 测试结果
     */
    public static TestResult test(DatabaseEntry entry) {
        return test(entry, null);
    }

    /**
     * 测试数据库连接
     *
     * @param entry 数据库条目
     * @param password 明文密码（如果已解密）
     * @return 测试结果
     */
    public static TestResult test(DatabaseEntry entry, String password) {
        return test(entry, password, DEFAULT_TIMEOUT);
    }

    /**
     * 测试数据库连接
     *
     * @param entry 数据库条目
     * @param password 明文密码（如果已解密）
     * @param timeoutSeconds 超时时间（秒）
     * @return 测试结果
     */
    public static TestResult test(DatabaseEntry entry, String password, int timeoutSeconds) {
        long startTime = System.currentTimeMillis();

        try {
            // 获取连接信息
            String url = entry.getUrl() != null ? entry.getUrl() : entry.getJdbcUrl();
            String username = entry.getUsername();
            String pwd = password != null ? password : entry.getPassword();

            if (url == null || url.isEmpty()) {
                return new TestResult(false, "JDBC URL为空",
                    System.currentTimeMillis() - startTime, null);
            }

            if (username == null || username.isEmpty()) {
                return new TestResult(false, "用户名为空",
                    System.currentTimeMillis() - startTime, null);
            }

            if (pwd == null || pwd.isEmpty()) {
                return new TestResult(false, "密码为空",
                    System.currentTimeMillis() - startTime, null);
            }

            // 加载驱动
            loadDriver(entry);

            // 设置连接超时
            DriverPropertyInfoUtil.setTimeout(url, timeoutSeconds);

            // 尝试连接
            try (Connection conn = DriverManager.getConnection(url, username, pwd)) {
                // 验证连接
                if (conn.isValid(DEFAULT_QUERY_TIMEOUT)) {
                    long duration = System.currentTimeMillis() - startTime;
                    String databaseName = conn.getMetaData().getDatabaseProductName();
                    String message = "连接成功 - 数据库: " + databaseName;
                    return new TestResult(true, message, duration, null);
                } else {
                    long duration = System.currentTimeMillis() - startTime;
                    return new TestResult(false, "连接验证失败",
                        duration, null);
                }
            }
        } catch (SQLException e) {
            long duration = System.currentTimeMillis() - startTime;
            return new TestResult(false, "SQL异常: " + e.getMessage(), duration, e);
        } catch (ClassNotFoundException e) {
            long duration = System.currentTimeMillis() - startTime;
            return new TestResult(false, "驱动类未找到: " + e.getMessage(), duration, e);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            return new TestResult(false, "异常: " + e.getMessage(), duration, e);
        }
    }

    /**
     * 通过ID测试数据库连接
     *
     * @param manager 密码文件管理器
     * @param sm4Key SM4密钥
     * @param dbId 数据库ID
     * @return 测试结果
     */
    public static TestResult test(PasswordFileManager manager, String sm4Key, String dbId) {
        DatabaseEntry entry = manager.getById(dbId);
        if (entry == null) {
            return new TestResult(false, "未找到数据库条目: " + dbId, 0, null);
        }

        try {
            String decryptedPwd = PasswordFileManager.decryptPassword(sm4Key, entry.getPassword());
            return test(entry, decryptedPwd);
        } catch (Exception e) {
            return new TestResult(false, "解密密码失败: " + e.getMessage(), 0, e);
        }
    }

    /**
     * 测试所有数据库连接
     *
     * @param manager 密码文件管理器
     * @param sm4Key SM4密钥
     * @return 测试结果映射（数据库ID -> 测试结果）
     */
    public static Map<String, TestResult> testAll(PasswordFileManager manager, String sm4Key) {
        Map<String, TestResult> results = new HashMap<>();

        for (DatabaseEntry entry : manager.getAllDatabases()) {
            String dbId = entry.getId();
            TestResult result = test(manager, sm4Key, dbId);
            results.put(dbId, result);
        }

        return results;
    }

    /**
     * 批量测试数据库连接，返回汇总信息
     *
     * @param manager 密码文件管理器
     * @param sm4Key SM4密钥
     * @return 汇总信息
     */
    public static String testAllWithSummary(PasswordFileManager manager, String sm4Key) {
        Map<String, TestResult> results = testAll(manager, sm4Key);

        StringBuilder summary = new StringBuilder();
        summary.append("数据库连接测试汇总:\n");
        summary.append("-".repeat(60)).append("\n");

        int successCount = 0;
        int failCount = 0;

        for (Map.Entry<String, TestResult> entry : results.entrySet()) {
            String dbId = entry.getKey();
            TestResult result = entry.getValue();

            DatabaseEntry dbEntry = manager.getById(dbId);
            String dbName = dbEntry != null ? dbEntry.getName() : dbId;

            summary.append(String.format("%-20s | ", dbName));

            if (result.isSuccess()) {
                summary.append("成功  ");
                successCount++;
            } else {
                summary.append("失败  ");
                failCount++;
            }

            summary.append(String.format("| %4dms | %s\n",
                result.getDuration(),
                result.getMessage()));
        }

        summary.append("-".repeat(60)).append("\n");
        summary.append(String.format("总计: %d个数据库, 成功: %d, 失败: %d\n",
            results.size(), successCount, failCount));

        return summary.toString();
    }

    /**
     * 加载JDBC驱动
     */
    private static void loadDriver(DatabaseEntry entry) throws ClassNotFoundException {
        String driverClass = entry.getDriver();

        if (driverClass == null || driverClass.isEmpty()) {
            // 从数据库类型推断驱动
            String dbType = entry.getType();
            driverClass = DEFAULT_DRIVERS.get(dbType != null ? dbType.toLowerCase() : null);
        }

        if (driverClass != null && !driverClass.isEmpty()) {
            try {
                Class.forName(driverClass);
                logger.fine("已加载JDBC驱动: " + driverClass);
            } catch (ClassNotFoundException e) {
                logger.warning("无法加载JDBC驱动: " + driverClass + " - " + e.getMessage());
                throw new ClassNotFoundException("无法加载JDBC驱动: " + driverClass, e);
            }
        } else {
            logger.warning("未指定JDBC驱动类，将尝试自动检测");
        }
    }

    /**
     * JDBC URL工具类
     */
    private static class DriverPropertyInfoUtil {

        /**
         * 设置连接超时（通过URL参数）
         */
        static void setTimeout(String url, int timeoutSeconds) {
            // 大多数数据库驱动支持connectTimeout或loginTimeout参数
            // 这里简化处理，实际驱动可能需要特定参数
            // 例如MySQL: jdbc:mysql://host:port/db?connectTimeout=5000
        }
    }

    /**
     * 获取数据库类型的默认端口
     */
    public static int getDefaultPort(String dbType) {
        if (dbType == null) {
            return 3306;
        }

        switch (dbType.toLowerCase()) {
            case "mysql":
                return 3306;
            case "postgresql":
                return 5432;
            case "oracle":
                return 1521;
            case "sqlserver":
                return 1433;
            case "db2":
                return 50000;
            case "sybase":
                return 5000;
            case "h2":
                return 9092;
            case "hsqldb":
                return 9001;
            default:
                return 3306;
        }
    }

    /**
     * 验证JDBC URL格式
     */
    public static boolean isValidJdbcUrl(String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }

        return url.startsWith("jdbc:");
    }

    /**
     * 从JDBC URL提取数据库类型
     */
    public static String extractDbTypeFromUrl(String url) {
        if (!isValidJdbcUrl(url)) {
            return null;
        }

        try {
            // 格式：jdbc:mysql://host:port/db
            int colonPos = url.indexOf(':', 5); // 跳过jdbc:
            int slashPos = url.indexOf("://", colonPos + 1);

            if (slashPos > 0) {
                return url.substring(colonPos + 1, slashPos).toLowerCase();
            }
        } catch (Exception e) {
            logger.log(Level.FINE, "从URL提取数据库类型失败", e);
        }

        return null;
    }
}
