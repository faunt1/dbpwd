package com.dbpwd.core;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * DbUtil单元测试
 *
 * 测试数据库工具类的功能完整性
 *
 * @author Database Password Manager
 * @version 5.3
 */
public class DbUtilTest {

    @Test
    public void testCloseQuietlyWithNull() {
        // 测试静默关闭方法不会抛出异常
        assertDoesNotThrow(() -> DbUtil.closeQuietly((ResultSet) null));
        assertDoesNotThrow(() -> DbUtil.closeQuietly((Statement) null));
        assertDoesNotThrow(() -> DbUtil.closeQuietly((Connection) null));
    }

    @Test
    public void testCloseQuietlyWithException() throws SQLException {
        // 创建一个会抛出异常的Statement模拟对象
        Statement statement = mock(Statement.class);
        doThrow(new SQLException("Test exception")).when(statement).close();

        // 静默关闭应该捕获异常
        assertDoesNotThrow(() -> DbUtil.closeQuietly(statement));
    }

    @Test
    public void testCloseQuietlyWithConnection() throws SQLException {
        // 创建连接模拟对象
        Connection connection = mock(Connection.class);

        // 静默关闭应该正常调用close方法
        assertDoesNotThrow(() -> DbUtil.closeQuietly(connection));

        verify(connection).close();
    }

    @Test
    public void testQueryReturnsEmptyList() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(connection.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false); // 空结果集
        when(rs.getMetaData()).thenReturn(mock(java.sql.ResultSetMetaData.class));

        List<Map<String, Object>> result = DbUtil.query(connection, "SELECT * FROM test_table");

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(rs).close();
        verify(stmt).close();
    }

    @Test
    public void testQueryWithParams() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        java.sql.ResultSetMetaData metaData = mock(java.sql.ResultSetMetaData.class);

        when(connection.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.getMetaData()).thenReturn(metaData);
        when(rs.next()).thenReturn(true, false); // 一行数据然后结束
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(anyInt())).thenReturn("ID", "NAME");
        when(rs.getObject(anyInt())).thenReturn(1, "Alice");

        List<Map<String, Object>> result = DbUtil.query(connection, "SELECT * FROM test WHERE id > ?", 0);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).get("ID"));
        assertEquals("Alice", result.get(0).get("NAME"));

        verify(stmt).setObject(1, 0);
        verify(rs).close();
        verify(stmt).close();
    }

    @Test
    public void testQueryOneReturnsNullWhenEmpty() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(connection.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);
        when(rs.getMetaData()).thenReturn(mock(java.sql.ResultSetMetaData.class));

        Map<String, Object> result = DbUtil.queryOne(connection, "SELECT * FROM test WHERE id = ?", 999);

        assertNull(result);

        verify(rs).close();
        verify(stmt).close();
    }

    @Test
    public void testQueryOneReturnsFirstRow() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        java.sql.ResultSetMetaData metaData = mock(java.sql.ResultSetMetaData.class);

        when(connection.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.getMetaData()).thenReturn(metaData);
        when(rs.next()).thenReturn(true, false);
        when(metaData.getColumnCount()).thenReturn(1);
        when(metaData.getColumnLabel(anyInt())).thenReturn("ID");
        when(rs.getObject(anyInt())).thenReturn(100);

        Map<String, Object> result = DbUtil.queryOne(connection, "SELECT * FROM test WHERE id = ?", 100);

        assertNotNull(result);
        assertEquals(100, result.get("ID"));

        verify(stmt).setObject(1, 100);
        verify(rs).close();
        verify(stmt).close();
    }

    @Test
    public void testQuerySingleValueReturnsNullWhenEmpty() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(connection.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(false);

        Object result = DbUtil.querySingleValue(connection, "SELECT name FROM test WHERE id = ?", 999);

        assertNull(result);
    }

    @Test
    public void testQuerySingleValueReturnsValue() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        when(connection.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(true, false);

        Map<String, Object> row = new java.util.HashMap<>();
        row.put("COUNT", 42);
        row.put("OTHER", "ignored");

        ResultSetMetaData metaData = mock(ResultSetMetaData.class);
        when(rs.getMetaData()).thenReturn(metaData);
        when(metaData.getColumnCount()).thenReturn(2);
        when(metaData.getColumnLabel(1)).thenReturn("COUNT");
        when(rs.getObject(1)).thenReturn(42);

        Object result = DbUtil.querySingleValue(connection, "SELECT COUNT(*) FROM test");

        assertEquals(42, result);
    }

    @Test
    public void testUpdate() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);

        when(connection.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeUpdate()).thenReturn(1);

        int count = DbUtil.update(connection, "UPDATE test SET name = ?", "Alice");

        assertEquals(1, count);
        verify(stmt).setObject(1, "Alice");
        verify(stmt).executeUpdate();
        verify(stmt).close();
    }

    @Test
    public void testUpdateWithMultipleParams() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);

        when(connection.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeUpdate()).thenReturn(1);

        int count = DbUtil.update(connection,
                "UPDATE test SET name = ?, age = ? WHERE id = ?",
                "Alice", 30, 1);

        assertEquals(1, count);
        verify(stmt).setObject(1, "Alice");
        verify(stmt).setObject(2, 30);
        verify(stmt).setObject(3, 1);
        verify(stmt).executeUpdate();
        verify(stmt).close();
    }

    @Test
    public void testBatchUpdate() throws Exception {
        Connection connection = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);

        when(connection.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeBatch()).thenReturn(new int[]{1, 1, 1});

        List<Object[]> params = new ArrayList<>();
        params.add(new Object[]{1, "Alice"});
        params.add(new Object[]{2, "Bob"});
        params.add(new Object[]{3, "Charlie"});

        int[] results = DbUtil.batchUpdate(connection, "INSERT INTO test VALUES (?, ?)", params);

        assertEquals(3, results.length);
        assertEquals(1, results[0]);
        assertEquals(1, results[1]);
        assertEquals(1, results[2]);

        verify(stmt, times(3)).addBatch();
        verify(stmt).executeBatch();
        verify(stmt).close();
    }

    @Test
    public void testExecute() throws Exception {
        Connection connection = mock(Connection.class);
        Statement stmt = mock(Statement.class);

        when(connection.createStatement()).thenReturn(stmt);
        when(stmt.execute(anyString())).thenReturn(true);

        DbUtil.execute(connection, "CREATE TABLE test (id INT)");

        verify(stmt).execute("CREATE TABLE test (id INT)");
        verify(stmt).close();
    }

    @Test
    public void testTransactionSuccess() throws Exception {
        Connection connection = mock(Connection.class);
        connection.setAutoCommit(true);

        DbUtil.executeInTransaction(connection, conn -> {
            // 在事务中执行
            return "success";
        });

        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(connection).setAutoCommit(true);
    }

    @Test
    public void testTransactionRollback() {
        Connection connection = mock(Connection.class);

        assertThrows(RuntimeException.class, () -> {
            DbUtil.executeInTransaction(connection, conn -> {
                if (true) {
                    throw new RuntimeException("Test error");
                }
                return null;
            });
        });

        verify(connection).setAutoCommit(false);
        verify(connection).rollback();
        verify(connection).setAutoCommit(anyBoolean());
    }

    @Test
    public void testTransactionRestoresAutoCommit() throws Exception {
        Connection connection = mock(Connection.class);

        when(connection.getAutoCommit()).thenReturn(true);

        DbUtil.executeInTransaction(connection, conn -> null);

        verify(connection).setAutoCommit(false);
        verify(connection).getAutoCommit();
        verify(connection).commit();
        verify(connection).setAutoCommit(true);
    }
}
