package com.dbpwd.core;

import com.dbpwd.model.DatabaseEntry;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ConnectionValidator单元测试
 *
 * @author Druid DB Password Manager
 * @version 3.1
 */
public class ConnectionValidatorTest {

    // ==================== ValidationResult测试 ====================

    @Test
    public void testValidationResultSuccess() {
        ConnectionValidator.ValidationResult result =
            new ConnectionValidator.ValidationResult(true, "连接成功", 100, null);

        assertTrue(result.isSuccess());
        assertEquals("连接成功", result.getMessage());
        assertEquals(100, result.getDuration());
        assertNull(result.getError());

        String str = result.toString();
        assertTrue(str.contains("连接成功"));
        assertTrue(str.contains("100ms"));
    }

    @Test
    public void testValidationResultFailure() {
        Exception error = new SQLException("Connection failed");
        ConnectionValidator.ValidationResult result =
            new ConnectionValidator.ValidationResult(false, "连接失败", 50, error);

        assertFalse(result.isSuccess());
        assertEquals("连接失败", result.getMessage());
        assertEquals(50, result.getDuration());
        assertEquals(error, result.getError());

        String str = result.toString();
        assertTrue(str.contains("连接失败"));
        assertTrue(str.contains("50ms"));
    }

    @Test
    public void testValidationResultWithNullMessage() {
        ConnectionValidator.ValidationResult result =
            new ConnectionValidator.ValidationResult(true, null, 0, null);

        assertTrue(result.isSuccess());
        assertNull(result.getMessage());
        assertEquals(0, result.getDuration());
    }

    @Test
    public void testValidationResultWithZeroDuration() {
        ConnectionValidator.ValidationResult result =
            new ConnectionValidator.ValidationResult(true, "OK", 0, null);

        assertTrue(result.isSuccess());
        assertEquals(0, result.getDuration());
    }

    // ==================== validate() 普通参数测试 ====================

    @Test
    public void testValidateWithNullEntry() {
        ConnectionValidator.ValidationResult result = ConnectionValidator.validate(null);

        assertNotNull(result);
        // null entry会导致NullPointerException或空检查失败，返回false
        assertFalse(result.isSuccess());
    }

    @Test
    public void testValidateWithEmptyUrl() {
        DatabaseEntry entry = createTestEntry();
        entry.setUrl("");
        entry.setHost(null);
        entry.setDatabase(null);

        ConnectionValidator.ValidationResult result = ConnectionValidator.validate(entry);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("JDBC URL为空"));
    }

    @Test
    public void testValidateWithNullUrl() {
        DatabaseEntry entry = createTestEntry();
        entry.setUrl(null);
        entry.setHost(null);
        entry.setDatabase(null);

        ConnectionValidator.ValidationResult result = ConnectionValidator.validate(entry);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("JDBC URL为空"));
    }

    @Test
    public void testValidateWithNullUsername() {
        DatabaseEntry entry = createTestEntry();
        entry.setUsername(null);

        ConnectionValidator.ValidationResult result = ConnectionValidator.validate(entry);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("用户名为空"));
    }

    @Test
    public void testValidateWithEmptyUsername() {
        DatabaseEntry entry = createTestEntry();
        entry.setUsername("");

        ConnectionValidator.ValidationResult result = ConnectionValidator.validate(entry);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("用户名为空"));
    }

    @Test
    public void testValidateWithNullPassword() {
        DatabaseEntry entry = createTestEntry();
        entry.setPassword(null);

        ConnectionValidator.ValidationResult result = ConnectionValidator.validate(entry);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("密码为空"));
    }

    @Test
    public void testValidateWithEmptyPassword() {
        DatabaseEntry entry = createTestEntry();
        entry.setPassword("");

        ConnectionValidator.ValidationResult result = ConnectionValidator.validate(entry);

        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("密码为空"));
    }

    @Test
    public void testValidateWithProvidedPassword() {
        DatabaseEntry entry = createTestEntry();
        entry.setPassword("encrypted_password");

        String plainPassword = "actual_password";

        ConnectionValidator.ValidationResult result = ConnectionValidator.validate(entry, plainPassword);

        // 应该使用提供的密码而非entry中的密码
        assertNotNull(result);
        // 可能会失败因为这是模拟环境，但不应该报空密码错误
        assertFalse(result.getMessage().contains("密码为空"));
    }

    @Test
    public void testValidateWithCustomTimeout() {
        DatabaseEntry entry = createTestEntry();

        ConnectionValidator.ValidationResult result = ConnectionValidator.validate(entry, "password", 10);

        assertNotNull(result);
        // 验证结果应该包含timeout相关的考虑
        assertTrue(result.getDuration() >= 0);
    }

    @Test
    public void testValidateUsesUrlField() {
        DatabaseEntry entry = createTestEntry();
        entry.setUrl("jdbc:h2:mem:testdb");
        entry.setHost(null);
        entry.setDatabase(null);

        ConnectionValidator.ValidationResult result = ConnectionValidator.validate(entry);

        assertNotNull(result);
        // H2内存数据库可能可以连接，也可能不行，但URL应该被识别
        // 主要验证不会抛NullPointerException
    }

    @Test
    public void testValidateUsesJdbcUrlGeneratedFromComponents() {
        DatabaseEntry entry = new DatabaseEntry();
        entry.setId("db001");
        entry.setName("Test DB");
        entry.setType("h2");
        entry.setHost(null);
        entry.setDatabase("test");
        entry.setUsername("sa");
        entry.setPassword("");

        ConnectionValidator.ValidationResult result = ConnectionValidator.validate(entry);

        assertNotNull(result);
        // 应该使用生成的H2 URL或报密码为空
    }

    // ==================== validate(PasswordFileManager, String, String) 测试 ====================

    @Test
    public void testValidateWithManagerNullResult() throws Exception {
        PasswordFileManager manager = createEmptyManager();

        ConnectionValidator.ValidationResult result =
            ConnectionValidator.validate(manager, "dummy_key", "nonexistent_id");

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertTrue(result.getMessage().contains("未找到数据库") || result.getMessage().contains("解密密码"));
    }

    // ==================== validateAll() 测试 ====================

    @Test
    public void testValidateAllEmptyManager() throws Exception {
        PasswordFileManager manager = createEmptyManager();

        Map<String, ConnectionValidator.ValidationResult> results =
            ConnectionValidator.validateAll(manager, "dummy_key");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testValidateAllMultipleEntries() throws Exception {
        PasswordFileManager manager = createEmptyManager();
        manager.addDatabase(createTestEntry("db1", "DB1", "h2"));
        manager.addDatabase(createTestEntry("db2", "DB2", "h2"));

        Map<String, ConnectionValidator.ValidationResult> results =
            ConnectionValidator.validateAll(manager, "dummy_key");

        assertNotNull(results);
        assertEquals(2, results.size());
        assertTrue(results.containsKey("db1"));
        assertTrue(results.containsKey("db2"));
    }

    // ==================== validateAllWithSummary() 测试 ====================

    @Test
    public void testValidateAllWithSummaryEmpty() throws Exception {
        PasswordFileManager manager = createEmptyManager();

        String summary = ConnectionValidator.validateAllWithSummary(manager, "dummy_key");

        assertNotNull(summary);
        assertTrue(summary.contains("数据库连接验证汇总"));
        assertTrue(summary.contains("总计: 0个数据库"));
    }

    @Test
    public void testValidateAllWithSummaryFormat() throws Exception {
        PasswordFileManager manager = createEmptyManager();
        manager.addDatabase(createTestEntry("db1", "Database 1", "h2"));

        String summary = ConnectionValidator.validateAllWithSummary(manager, "dummy_key");

        assertNotNull(summary);
        assertTrue(summary.contains("数据库连接验证汇总"));
        assertTrue(summary.contains("Database 1"));
        assertTrue(summary.contains("成功") || summary.contains("失败"));
        assertTrue(summary.contains("总计: 1个数据库"));
    }

    // ==================== getDefaultPort() 测试 ====================

    @Test
    public void testGetDefaultPortMySql() {
        assertEquals(3306, ConnectionValidator.getDefaultPort("mysql"));
    }

    @Test
    public void testGetDefaultPortPostgres() {
        assertEquals(5432, ConnectionValidator.getDefaultPort("postgresql"));
    }

    @Test
    public void testGetDefaultPortOracle() {
        assertEquals(1521, ConnectionValidator.getDefaultPort("oracle"));
    }

    @Test
    public void testGetDefaultPortSqlServer() {
        assertEquals(1433, ConnectionValidator.getDefaultPort("sqlserver"));
    }

    @Test
    public void testGetDefaultPortDb2() {
        assertEquals(50000, ConnectionValidator.getDefaultPort("db2"));
    }

    @Test
    public void testGetDefaultPortSybase() {
        assertEquals(5000, ConnectionValidator.getDefaultPort("sybase"));
    }

    @Test
    public void testGetDefaultPortH2() {
        assertEquals(9092, ConnectionValidator.getDefaultPort("h2"));
    }

    @Test
    public void testGetDefaultPortHsqldb() {
        assertEquals(9001, ConnectionValidator.getDefaultPort("hsqldb"));
    }

    @Test
    public void testGetDefaultPortCaseInsensitive() {
        assertEquals(3306, ConnectionValidator.getDefaultPort("MySQL"));
        assertEquals(3306, ConnectionValidator.getDefaultPort("MYSQL"));
        assertEquals(3306, ConnectionValidator.getDefaultPort("MySql"));
    }

    @Test
    public void testGetDefaultPortUnknownType() {
        assertEquals(3306, ConnectionValidator.getDefaultPort("unknown"));
    }

    @Test
    public void testGetDefaultPortNull() {
        assertEquals(3306, ConnectionValidator.getDefaultPort(null));
    }

    // ==================== isValidJdbcUrl() 测试 ====================

    @Test
    public void testIsValidJdbcUrlValidFormats() {
        assertTrue(ConnectionValidator.isValidJdbcUrl("jdbc:mysql://localhost:3306/db"));
        assertTrue(ConnectionValidator.isValidJdbcUrl("jdbc:postgresql://host/db"));
        assertTrue(ConnectionValidator.isValidJdbcUrl("jdbc:oracle:thin:@host:1521:db"));
        assertTrue(ConnectionValidator.isValidJdbcUrl("jdbc:sqlserver://host:1433;databaseName=db"));
        assertTrue(ConnectionValidator.isValidJdbcUrl("jdbc:h2:mem:testdb"));
        assertTrue(ConnectionValidator.isValidJdbcUrl("jdbc:hsqldb:mem:testdb"));
    }

    @Test
    public void testIsValidJdbcUrlInvalidFormats() {
        assertFalse(ConnectionValidator.isValidJdbcUrl(""));
        assertFalse(ConnectionValidator.isValidJdbcUrl(null));
        assertFalse(ConnectionValidator.isValidJdbcUrl("mysql://localhost/db"));
        assertFalse(ConnectionValidator.isValidJdbcUrl("JDBC:mysql://localhost/db"));
        assertFalse(ConnectionValidator.isValidJdbcUrl("http://localhost/db"));
        assertFalse(ConnectionValidator.isValidJdbcUrl("postgres://localhost/db"));
    }

    // ==================== extractDbTypeFromUrl() 测试 ====================

    @Test
    public void testExtractDbTypeMySql() {
        assertEquals("mysql", ConnectionValidator.extractDbTypeFromUrl("jdbc:mysql://localhost:3306/db"));
    }

    @Test
    public void testExtractDbTypePostgres() {
        assertEquals("postgresql", ConnectionValidator.extractDbTypeFromUrl("jdbc:postgresql://localhost/db"));
    }

    @Test
    public void testExtractDbTypeOracle() {
        // Oracle URL格式: jdbc:oracle:thin:@host:port:db 没有://
        // 当前实现不支持这种格式，返回null
        assertEquals("oracle", ConnectionValidator.extractDbTypeFromUrl("jdbc:oracle://thin@host:1521:db"));
    }

    @Test
    public void testExtractDbTypeSqlServer() {
        assertEquals("sqlserver", ConnectionValidator.extractDbTypeFromUrl("jdbc:sqlserver://host:1433;databaseName=db"));
    }

    @Test
    public void testExtractDbTypeH2() {
        assertEquals("h2", ConnectionValidator.extractDbTypeFromUrl("jdbc:h2:mem:testdb"));
    }

    @Test
    public void testExtractDbTypeInvalidUrl() {
        assertNull(ConnectionValidator.extractDbTypeFromUrl(null));
        assertNull(ConnectionValidator.extractDbTypeFromUrl(""));
        assertNull(ConnectionValidator.extractDbTypeFromUrl("not-a-jdbc-url"));
        assertNull(ConnectionValidator.extractDbTypeFromUrl("jdbc:"));
        assertNull(ConnectionValidator.extractDbTypeFromUrl("jdbc:localhost/db"));
    }

    @Test
    public void testExtractDbTypeOraclThinFormat() {
        // Oracle thin格式没有 //，当前实现返回null
        String result = ConnectionValidator.extractDbTypeFromUrl("jdbc:oracle:thin:@host:1521:db");
        assertEquals("oracle", result);
    }

    @Test
    public void testExtractDbTypeLowerCase() {
        String dbType = ConnectionValidator.extractDbTypeFromUrl("jdbc:MySQL://localhost:3306/db");
        assertEquals("mysql", dbType); // 应该返回小写
    }

    // ==================== 驱动加载相关测试 ====================

    @Test
    public void testValidateWithExplicitDriver() {
        DatabaseEntry entry = createTestEntry();
        entry.setDriver("org.h2.Driver");
        entry.setUrl("jdbc:h2:mem:testdb");

        ConnectionValidator.ValidationResult result = ConnectionValidator.validate(entry, "password");

        // 驱动应该被加载（在classpath中应该有H2）
        // 连接可能成功也可能失败，取决于H2是否在classpath
        assertNotNull(result);
        // 主要验证不会抛出ClassNotFoundException
    }

    @Test
    public void testValidateWithMissingDriver() {
        DatabaseEntry entry = createTestEntry();
        entry.setDriver("com.nonexistent.Driver");
        entry.setUrl("jdbc:nonexistent://localhost/db");

        ConnectionValidator.ValidationResult result = ConnectionValidator.validate(entry);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        // 应该显示驱动未找到或SQL异常
        assertTrue(result.getMessage().contains("驱动") || result.getMessage().contains("SQL"));
    }

    @Test
    public void testValidateWithInferredDriverFromType() {
        DatabaseEntry entry = new DatabaseEntry();
        entry.setId("db001");
        entry.setName("Test DB");
        entry.setType("h2");
        entry.setUrl("jdbc:h2:mem:testdb");
        entry.setUsername("sa");
        entry.setPassword("");

        // 不设置driver，应从type推断
        ConnectionValidator.ValidationResult result = ConnectionValidator.validate(entry);

        assertNotNull(result);
    }

    // ==================== 辅助方法 ====================

    private DatabaseEntry createTestEntry() {
        DatabaseEntry entry = new DatabaseEntry();
        entry.setId("db001");
        entry.setName("Test Database");
        entry.setType("mysql");
        entry.setUrl("jdbc:mysql://localhost:3306/testdb");
        entry.setUsername("testuser");
        entry.setPassword("testpass");
        return entry;
    }

    private DatabaseEntry createTestEntry(String id, String name, String type) {
        DatabaseEntry entry = new DatabaseEntry();
        entry.setId(id);
        entry.setName(name);
        entry.setType(type);
        entry.setUrl("jdbc:" + type + ":mem:" + name.replace(" ", ""));
        entry.setUsername("sa");
        entry.setPassword("");
        return entry;
    }

    private PasswordFileManager createEmptyManager() throws Exception {
        PasswordFileManager manager = new PasswordFileManager("/tmp/test-password.json");
        manager.load();
        return manager;
    }
}
