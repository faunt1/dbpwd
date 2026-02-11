package com.dbpwd.core;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 数据库工具类
 *
 * 简化的JDBC操作工具，提供常用的query和update方法
 * 配合StandalonePasswordDecryptor使用，提供纯JDBC应用的便捷API
 *
 * 设计原则：
 * - 简单易用：提供直观的API接口
 * - 轻量级：不依赖任何框架，纯JDBC实现
 * - 资源安全：自动管理Statement和ResultSet资源
 * - 高度灵活：支持参数绑定、结果集映射等特性
 *
 * 使用场景：
 * - 无框架依赖的独立JDBC应用
 * - 批处理脚本和工具
 * - 测试工具和数据迁移脚本
 *
 * @author Database Password Manager
 * @version 5.3
 */
public class DbUtil {

    private static final Logger logger = Logger.getLogger(DbUtil.class.getName());

    /**
     * 执行查询并返回结果列表
     *
     * 每个结果行映射为Map<String, Object>，列名作为键，值作为值
     *
     * @param connection 数据库连接
     * @param sql SQL查询语句
     * @return 查询结果列表
     * @throws SQLException SQL执行异常
     */
    public static List<Map<String, Object>> query(Connection connection, String sql) throws SQLException {
        return query(connection, sql, (Object[]) null);
    }

    /**
     * 执行查询并返回结果列表
     *
     * 每个结果行映射为Map<String, Object>，列名作为键，值作为值
     *
     * @param connection 数据库连接
     * @param sql SQL查询语句（可包含占位符?）
     * @param params SQL参数
     * @return 查询结果列表
     * @throws SQLException SQL执行异常
     */
    public static List<Map<String, Object>> query(Connection connection, String sql, Object... params)
            throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            stmt = connection.prepareStatement(sql);

            // 绑定参数
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
            }

            rs = stmt.executeQuery();
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            // 遍历结果集
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = metaData.getColumnLabel(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                result.add(row);
            }

            logger.fine("Query executed successfully, returned " + result.size() + " rows");
            return result;

        } finally {
            closeQuietly(rs);
            closeQuietly(stmt);
        }
    }

    /**
     * 执行查询并返回单个结果
     *
     * 如果查询返回多行，只返回第一行
     * 如果查询返回0行，返回null
     *
     * @param connection 数据库连接
     * @param sql SQL查询语句
     * @return 查询结果（null表示无结果）
     * @throws SQLException SQL执行异常
     */
    public static Map<String, Object> queryOne(Connection connection, String sql) throws SQLException {
        return queryOne(connection, sql, (Object[]) null);
    }

    /**
     * 执行查询并返回单个结果
     *
     * 如果查询返回多行，只返回第一行
     * 如果查询返回0行，返回null
     *
     * @param connection 数据库连接
     * @param sql SQL查询语句（可包含占位符?）
     * @param params SQL参数
     * @return 查询结果（null表示无结果）
     * @throws SQLException SQL执行异常
     */
    public static Map<String, Object> queryOne(Connection connection, String sql, Object... params)
            throws SQLException {
        List<Map<String, Object>> result = query(connection, sql, params);
        return result.isEmpty() ? null : result.get(0);
    }

    /**
     * 执行查询并返回单个列值（单行单列）
     *
     * @param connection 数据库连接
     * @param sql SQL查询语句
     * @param params SQL参数（可选）
     * @return 列值（null表示无结果或值为null）
     * @throws SQLException SQL执行异常
     */
    public static Object querySingleValue(Connection connection, String sql, Object... params)
            throws SQLException {
        Map<String, Object> row = queryOne(connection, sql, params);
        if (row == null || row.isEmpty()) {
            return null;
        }

        // 返回第一个列的值
        return row.values().iterator().next();
    }

    /**
     * 执行更新操作（INSERT/UPDATE/DELETE）
     *
     * @param connection 数据库连接
     * @param sql SQL更新语句（可包含占位符?）
     * @return 影响的行数
     * @throws SQLException SQL执行异常
     */
    public static int update(Connection connection, String sql) throws SQLException {
        return update(connection, sql, (Object[]) null);
    }

    /**
     * 执行更新操作（INSERT/UPDATE/DELETE）
     *
     * @param connection 数据库连接
     * @param sql SQL更新语句（可包含占位符?）
     * @param params SQL参数
     * @return 影响的行数
     * @throws SQLException SQL执行异常
     */
    public static int update(Connection connection, String sql, Object... params) throws SQLException {
        PreparedStatement stmt = null;

        try {
            stmt = connection.prepareStatement(sql);

            // 绑定参数
            if (params != null) {
                for (int i = 0; i < params.length; i++) {
                    stmt.setObject(i + 1, params[i]);
                }
            }

            int rowCount = stmt.executeUpdate();
            logger.fine("Update executed successfully, affected " + rowCount + " rows");
            return rowCount;

        } finally {
            closeQuietly(stmt);
        }
    }

    /**
     * 执行批更新操作
     *
     * @param connection 数据库连接
     * @param sql SQL更新语句
     * @param paramsList 参数列表（每组参数对应一次执行）
     * @return 每次执行影响的行数数组
     * @throws SQLException SQL执行异常
     */
    public static int[] batchUpdate(Connection connection, String sql, List<Object[]> paramsList)
            throws SQLException {
        PreparedStatement stmt = null;

        try {
            stmt = connection.prepareStatement(sql);

            // 添加批处理
            for (Object[] params : paramsList) {
                if (params != null) {
                    for (int i = 0; i < params.length; i++) {
                        stmt.setObject(i + 1, params[i]);
                    }
                }
                stmt.addBatch();
            }

            // 执行批处理
            int[] result = stmt.executeBatch();
            logger.fine("Batch update executed successfully, " + result.length + " batches");
            return result;

        } finally {
            closeQuietly(stmt);
        }
    }

    /**
     * 执行SQL语句（适用于DDL语句）
     *
     * @param connection 数据库连接
     * @param sql SQL执行语句
     * @throws SQLException SQL执行异常
     */
    public static void execute(Connection connection, String sql) throws SQLException {
        Statement stmt = null;

        try {
            stmt = connection.createStatement();
            stmt.execute(sql);
            logger.fine("SQL executed successfully: " + sql);
        } finally {
            closeQuietly(stmt);
        }
    }

    /**
     * 静默关闭ResultSet
     *
     * @param rs ResultSet对象
     */
    public static void closeQuietly(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                // 静默关闭，记录日志
                logger.warning("Failed to close ResultSet: " + e.getMessage());
            }
        }
    }

    /**
     * 静默关闭Statement
     *
     * @param stmt Statement对象
     */
    public static void closeQuietly(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            } catch (SQLException e) {
                // 静默关闭，记录日志
                logger.warning("Failed to close Statement: " + e.getMessage());
            }
        }
    }

    /**
     * 静默关闭Connection
     *
     * @param conn Connection对象
     */
    public static void closeQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                // 静默关闭，记录日志
                logger.warning("Failed to close Connection: " + e.getMessage());
            }
        }
    }

    /**
     * 执行事务
     *
     * @param connection 数据库连接
     * @param transaction 事务代码块
     * @param <T> 返回类型
     * @return 事务执行结果
     * @throws Exception 事务执行异常
     */
    public static <T> T executeInTransaction(Connection connection, TransactionCallback<T> transaction)
            throws Exception {
        boolean autoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(false);
            T result = transaction.execute(connection);
            connection.commit();
            logger.fine("Transaction committed successfully");
            return result;
        } catch (Exception e) {
            try {
                connection.rollback();
                logger.warning("Transaction rolled back due to exception: " + e.getMessage());
            } catch (SQLException rollbackEx) {
                logger.severe("Failed to rollback transaction: " + rollbackEx.getMessage());
            }
            throw e;
        } finally {
            try {
                connection.setAutoCommit(autoCommit);
            } catch (SQLException e) {
                logger.warning("Failed to restore autoCommit: " + e.getMessage());
            }
        }
    }

    /**
     * 事务回调接口
     *
     * @param <T> 返回类型
     */
    public interface TransactionCallback<T> {
        /**
         * 在事务中执行
         *
         * @param connection 数据库连接
         * @return 执行结果
         * @throws Exception 执行异常
         */
        T execute(Connection connection) throws Exception;
    }
}
