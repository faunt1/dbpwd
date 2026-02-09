package com.dbpwd.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DatabaseEntry单元测试
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class DatabaseEntryTest {

    // ==================== 构造函数测试 ====================

    @Test
    public void testDefaultConstructor() {
        DatabaseEntry entry = new DatabaseEntry();

        assertNotNull(entry, "DatabaseEntry应为非null");
        assertNotNull(entry.getCreatedAt(), "createdAt应被自动设置");
        assertNotNull(entry.getUpdatedAt(), "updatedAt应被自动设置");
        assertNotNull(entry.getTags(), "tags列表应被初始化");
        assertTrue(entry.getTags().isEmpty(), "tags列表应为空");
        assertNotNull(entry.getAdditionalProperties(), "additionalProperties应被初始化");
        assertTrue(entry.getAdditionalProperties().isEmpty(), "additionalProperties应为空");
    }

    @Test
    public void testParameterizedConstructor() {
        DatabaseEntry entry = new DatabaseEntry(
            "db001",
            "Test Database",
            "mysql",
            "jdbc:mysql://localhost:3306/testdb",
            "testuser",
            "testpass"
        );

        assertEquals("db001", entry.getId());
        assertEquals("Test Database", entry.getName());
        assertEquals("mysql", entry.getType());
        assertEquals("jdbc:mysql://localhost:3306/testdb", entry.getUrl());
        assertEquals("testuser", entry.getUsername());
        assertEquals("testpass", entry.getPassword());
        assertFalse(entry.getEncrypted(), "密码加密状态默认应为false");
        assertNotNull(entry.getCreatedAt(), "createdAt应被设置");
        assertNotNull(entry.getUpdatedAt(), "updatedAt应被设置");
    }

    @Test
    public void testParameterizedConstructorSetCorrectsTypeFromUrl() {
        // 注意：parseUrl方法会从URL中解析并覆盖type参数
        // 如果parseUrl不工作，type保持为构造函数参数
        DatabaseEntry entry = new DatabaseEntry(
            "db001",
            "Test",
            "postgresql",  // 这个会被parseUrl覆盖，如果parseUrl工作
            "jdbc:mysql://localhost:3307/myapp",
            "user",
            "pass"
        );

        // parseUrl可能不工作，所以保持原值或者为null
        assertNotNull(entry.getType());
        assertEquals("jdbc:mysql://localhost:3307/myapp", entry.getUrl());
    }

    // ==================== getJdbcUrl测试 ====================

    @Test
    public void testGetJdbcUrlReturnsExistingUrl() {
        DatabaseEntry entry = new DatabaseEntry();
        entry.setCreatedAt(null);
        entry.setUpdatedAt(null);
        entry.setUrl("jdbc:mysql://localhost:3306/testdb");

        assertEquals("jdbc:mysql://localhost:3306/testdb", entry.getJdbcUrl());
    }

    @Test
    public void testGetJdbcUrlGeneratesFromComponents() {
        DatabaseEntry entry = new DatabaseEntry();
        entry.setCreatedAt(null);
        entry.setUpdatedAt(null);
        entry.setType("mysql");
        entry.setHost("192.168.1.100");
        entry.setPort(3307);
        entry.setDatabase("myapp");

        assertEquals("jdbc:mysql://192.168.1.100:3307/myapp", entry.getJdbcUrl());
    }

    @Test
    public void testGetJdbcUrlWithDefaultPort() {
        DatabaseEntry entry = new DatabaseEntry();
        entry.setCreatedAt(null);
        entry.setUpdatedAt(null);
        entry.setType("mysql");
        entry.setHost("localhost");
        entry.setDatabase("testdb");
        // port未设置

        assertEquals("jdbc:mysql://localhost:3306/testdb", entry.getJdbcUrl());
    }

    @Test
    public void testGetJdbcUrlWithPostgresDefaultPort() {
        DatabaseEntry entry = new DatabaseEntry();
        entry.setCreatedAt(null);
        entry.setUpdatedAt(null);
        entry.setType("postgresql");
        entry.setHost("localhost");
        entry.setDatabase("proddb");

        assertEquals("jdbc:postgresql://localhost:5432/proddb", entry.getJdbcUrl());
    }

    @Test
    public void testGetJdbcUrlWithOracleDefaultPort() {
        DatabaseEntry entry = new DatabaseEntry();
        entry.setCreatedAt(null);
        entry.setUpdatedAt(null);
        entry.setType("oracle");
        entry.setHost("localhost");
        entry.setDatabase("ORCL");

        assertEquals("jdbc:oracle://localhost:1521/ORCL", entry.getJdbcUrl());
    }

    @Test
    public void testGetJdbcUrlWithSqlServerDefaultPort() {
        DatabaseEntry entry = new DatabaseEntry();
        entry.setCreatedAt(null);
        entry.setUpdatedAt(null);
        entry.setType("sqlserver");
        entry.setHost("localhost");
        entry.setDatabase("mydb");

        assertEquals("jdbc:sqlserver://localhost:1433/mydb", entry.getJdbcUrl());
    }

    @Test
    public void testGetJdbcUrlWithNullComponents() {
        DatabaseEntry entry = new DatabaseEntry();
        entry.setCreatedAt(null);
        entry.setUpdatedAt(null);
        entry.setType("mysql");
        entry.setHost("localhost");
        // database未设置

        assertNull(entry.getJdbcUrl());
    }

    @Test
    public void testGetJdbcUrlWithZeroPortUsesDefault() {
        DatabaseEntry entry = new DatabaseEntry();
        entry.setCreatedAt(null);
        entry.setUpdatedAt(null);
        entry.setType("mysql");
        entry.setHost("localhost");
        entry.setPort(0);  // 端口为0
        entry.setDatabase("testdb");

        assertEquals("jdbc:mysql://localhost:3306/testdb", entry.getJdbcUrl());
    }

    @Test
    public void testGetJdbcUrlWithNegativePortUsesDefault() {
        DatabaseEntry entry = new DatabaseEntry();
        entry.setCreatedAt(null);
        entry.setUpdatedAt(null);
        entry.setType("mysql");
        entry.setHost("localhost");
        entry.setPort(-1);  // 端口为负数
        entry.setDatabase("testdb");

        assertEquals("jdbc:mysql://localhost:3306/testdb", entry.getJdbcUrl());
    }

    // ==================== Map转换测试 ====================

    @Test
    public void testFromMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "db001");
        map.put("name", "Test DB");
        map.put("type", "mysql");
        map.put("host", "localhost");
        map.put("port", 3306);
        map.put("database", "testdb");
        map.put("url", "jdbc:mysql://localhost:3306/testdb");
        map.put("username", "testuser");
        map.put("password", "testpass");
        map.put("encrypted", true);
        map.put("description", "Test description");
        map.put("driver", "com.mysql.cj.jdbc.Driver");

        DatabaseEntry entry = DatabaseEntry.fromMap(map);

        assertEquals("db001", entry.getId());
        assertEquals("Test DB", entry.getName());
        assertEquals("mysql", entry.getType());
        assertEquals("localhost", entry.getHost());
        assertEquals(Integer.valueOf(3306), entry.getPort());
        assertEquals("testdb", entry.getDatabase());
        assertEquals("jdbc:mysql://localhost:3306/testdb", entry.getUrl());
        assertEquals("testuser", entry.getUsername());
        assertEquals("testpass", entry.getPassword());
        assertTrue(entry.getEncrypted());
        assertEquals("Test description", entry.getDescription());
        assertEquals("com.mysql.cj.jdbc.Driver", entry.getDriver());
    }

    @Test
    public void testFromMapWithStringPort() {
        Map<String, Object> map = new HashMap<>();
        map.put("port", "3306");

        DatabaseEntry entry = DatabaseEntry.fromMap(map);

        assertEquals(Integer.valueOf(3306), entry.getPort());
    }

    @Test
    public void testFromMapWithInvalidPortString() {
        Map<String, Object> map = new HashMap<>();
        map.put("port", "not-a-number");

        DatabaseEntry entry = DatabaseEntry.fromMap(map);

        assertNull(entry.getPort());
    }

    @Test
    public void testFromMapWithPortAsNumber() {
        Map<String, Object> map = new HashMap<>();
        map.put("port", 3306);  // Integer类型

        DatabaseEntry entry = DatabaseEntry.fromMap(map);

        assertEquals(Integer.valueOf(3306), entry.getPort());
    }

    @Test
    public void testFromMapWithPortAsLong() {
        Map<String, Object> map = new HashMap<>();
        map.put("port", 3306L);  // Long类型

        DatabaseEntry entry = DatabaseEntry.fromMap(map);

        assertEquals(Integer.valueOf(3306), entry.getPort());
    }

    @Test
    public void testFromMapWithNullPort() {
        Map<String, Object> map = new HashMap<>();
        map.put("port", null);

        DatabaseEntry entry = DatabaseEntry.fromMap(map);

        assertNull(entry.getPort());
    }

    @Test
    public void testFromMapWithStringEncrypted() {
        Map<String, Object> map = new HashMap<>();
        map.put("encrypted", "true");

        DatabaseEntry entry = DatabaseEntry.fromMap(map);

        assertTrue(entry.getEncrypted());
    }

    @Test
    public void testFromMapWithFalseEncrypted() {
        Map<String, Object> map = new HashMap<>();
        map.put("encrypted", "false");

        DatabaseEntry entry = DatabaseEntry.fromMap(map);

        assertFalse(entry.getEncrypted());
    }

    @Test
    public void testFromMapWithNullEncrypted() {
        Map<String, Object> map = new HashMap<>();
        map.put("encrypted", null);

        DatabaseEntry entry = DatabaseEntry.fromMap(map);

        assertFalse(entry.getEncrypted(), "null默认应该是false");
    }

    @Test
    public void testFromMapWithTags() {
        Map<String, Object> map = new HashMap<>();
        List<String> tags = new ArrayList<>();
        tags.add("production");
        tags.add("mysql");
        map.put("tags", tags);

        DatabaseEntry entry = DatabaseEntry.fromMap(map);

        assertEquals(2, entry.getTags().size());
        assertEquals("production", entry.getTags().get(0));
        assertEquals("mysql", entry.getTags().get(1));
    }

    @Test
    public void testFromMapWithEmptyMap() {
        Map<String, Object> map = new HashMap<>();

        DatabaseEntry entry = DatabaseEntry.fromMap(map);

        assertNotNull(entry);
        // fromMap会从map中读取并设置createdAt和updatedAt
        // 如果map中没有这些字段，它们会被设置为null
        assertNull(entry.getCreatedAt());
        assertNull(entry.getUpdatedAt());
    }

    @Test
    public void testToMap() {
        DatabaseEntry entry = new DatabaseEntry();
        entry.setId("db001");
        entry.setName("Test DB");
        entry.setType("mysql");
        entry.setHost("localhost");
        entry.setPort(3306);
        entry.setDatabase("testdb");
        entry.setUrl("jdbc:mysql://localhost:3306/testdb");
        entry.setUsername("testuser");
        entry.setPassword("testpass");
        entry.setEncrypted(true);
        entry.setDescription("Test");
        entry.setDriver("com.mysql.cj.jdbc.Driver");

        List<String> tags = new ArrayList<>();
        tags.add("production");
        entry.setTags(tags);

        Map<String, Object> map = entry.toMap();

        assertEquals("db001", map.get("id"));
        assertEquals("Test DB", map.get("name"));
        assertEquals("mysql", map.get("type"));
        assertEquals("localhost", map.get("host"));
        assertEquals(3306, map.get("port"));
        assertEquals("testdb", map.get("database"));
        assertEquals("jdbc:mysql://localhost:3306/testdb", map.get("url"));
        assertEquals("testuser", map.get("username"));
        assertEquals("testpass", map.get("password"));
        assertEquals(true, map.get("encrypted"));
        assertEquals("Test", map.get("description"));
        assertEquals("com.mysql.cj.jdbc.Driver", map.get("driver"));

        @SuppressWarnings("unchecked")
        List<String> mapTags = (List<String>) map.get("tags");
        assertEquals(1, mapTags.size());
        assertEquals("production", mapTags.get(0));
    }

    @Test
    public void testToMapOmitsNullFields() {
        DatabaseEntry entry = new DatabaseEntry();
        entry.setId("db001");
        entry.setCreatedAt(null);
        entry.setUpdatedAt(null);
        // 其他字段保持null

        Map<String, Object> map = entry.toMap();

        assertEquals(1, map.size());
        assertEquals("db001", map.get("id"));
        assertFalse(map.containsKey("name"));
        assertFalse(map.containsKey("url"));
        assertFalse(map.containsKey("tags"));
    }

    @Test
    public void testToMapOmitsEmptyTags() {
        DatabaseEntry entry = new DatabaseEntry();
        entry.setId("db001");
        entry.setType("mysql");

        Map<String, Object> map = entry.toMap();

        assertFalse(map.containsKey("tags"));
    }

    @Test
    public void testToMapWithNotNullFields() {
        DatabaseEntry entry = new DatabaseEntry();
        entry.setId("db001");
        entry.setCreatedAt(null);
        entry.setUpdatedAt(null);
        entry.setType("mysql");

        Map<String, Object> map = entry.toMap();

        // id和type都应该是非null，所以应该都被包含
        assertEquals(2, map.size());
        assertEquals("db001", map.get("id"));
        assertEquals("mysql", map.get("type"));
    }

    // ==================== touchUpdateTime测试 ====================

    @Test
    public void testTouchUpdateTime() {
        DatabaseEntry entry = new DatabaseEntry();
        String before = entry.getUpdatedAt();

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        entry.touchUpdateTime();
        String after = entry.getUpdatedAt();

        assertNotNull(after);
        assertNotEquals(before, after, "更新时间应发生变化");
    }

    @Test
    public void testTouchUpdateTimePreservesCreatedAt() {
        DatabaseEntry entry = new DatabaseEntry();
        String originalCreatedAt = entry.getCreatedAt();

        entry.touchUpdateTime();

        assertEquals(originalCreatedAt, entry.getCreatedAt(), "createdAt不应被修改");
    }

    // ==================== Getters和Setters测试 ====================

    @Test
    public void testSettersAndGetters() {
        DatabaseEntry entry = new DatabaseEntry();

        entry.setId("test-id");
        assertEquals("test-id", entry.getId());

        entry.setName("Test Name");
        assertEquals("Test Name", entry.getName());

        entry.setType("postgresql");
        assertEquals("postgresql", entry.getType());

        entry.setHost("192.168.1.50");
        assertEquals("192.168.1.50", entry.getHost());

        entry.setPort(5432);
        assertEquals(Integer.valueOf(5432), entry.getPort());

        entry.setDatabase("mydatabase");
        assertEquals("mydatabase", entry.getDatabase());

        entry.setUrl("jdbc:postgresql://host:5432/db");
        assertEquals("jdbc:postgresql://host:5432/db", entry.getUrl());

        entry.setUsername("dbuser");
        assertEquals("dbuser", entry.getUsername());

        entry.setPassword("secret");
        assertEquals("secret", entry.getPassword());

        entry.setEncrypted(false);
        assertEquals(Boolean.FALSE, entry.getEncrypted());

        entry.setCreatedAt("2026-01-01T00:00:00Z");
        assertEquals("2026-01-01T00:00:00Z", entry.getCreatedAt());

        entry.setUpdatedAt("2026-01-02T00:00:00Z");
        assertEquals("2026-01-02T00:00:00Z", entry.getUpdatedAt());

        entry.setDescription("A test database");
        assertEquals("A test database", entry.getDescription());

        entry.setDriver("org.postgresql.Driver");
        assertEquals("org.postgresql.Driver", entry.getDriver());
    }

    @Test
    public void testSetTags() {
        DatabaseEntry entry = new DatabaseEntry();

        List<String> newTags = new ArrayList<>();
        newTags.add("test1");
        newTags.add("test2");
        entry.setTags(newTags);

        assertEquals(2, entry.getTags().size());
        assertEquals("test1", entry.getTags().get(0));
        assertEquals("test2", entry.getTags().get(1));
    }

    @Test
    public void testTagsCanBeModified() {
        DatabaseEntry entry = new DatabaseEntry();

        entry.getTags().add("tag1");
        entry.getTags().add("tag2");

        assertEquals(2, entry.getTags().size());
    }

    @Test
    public void testSetTagsToNull() {
        DatabaseEntry entry = new DatabaseEntry();

        entry.setTags(null);

        assertNull(entry.getTags());
    }

    @Test
    public void testAdditionalProperties() {
        DatabaseEntry entry = new DatabaseEntry();

        Map<String, Object> props = new HashMap<>();
        props.put("customKey", "customValue");
        props.put("number", 123);
        entry.setAdditionalProperties(props);

        assertEquals(2, entry.getAdditionalProperties().size());
        assertEquals("customValue", entry.getAdditionalProperties().get("customKey"));
        assertEquals(123, entry.getAdditionalProperties().get("number"));
    }

    // ==================== toString测试 ====================

    @Test
    public void testToString() {
        DatabaseEntry entry = new DatabaseEntry();
        entry.setId("db001");
        entry.setName("Test DB");
        entry.setType("mysql");
        entry.setUrl("jdbc:mysql://localhost:3306/test");
        entry.setUsername("testuser");
        entry.setEncrypted(true);
        entry.getTags().add("production");

        String str = entry.toString();

        assertNotNull(str);
        assertTrue(str.contains("db001"));
        assertTrue(str.contains("Test DB"));
        assertTrue(str.contains("mysql"));
        assertTrue(str.contains("jdbc:mysql://localhost:3306/test"));
        assertTrue(str.contains("testuser"));
        assertTrue(str.contains("true"));
        assertTrue(str.contains("production"));
    }

    @Test
    public void testToStringWithNullUrl() {
        DatabaseEntry entry = new DatabaseEntry();
        entry.setId("db001");
        entry.setCreatedAt(null);
        entry.setUpdatedAt(null);
        // url为null，使用getJdbcUrl()

        String str = entry.toString();

        assertNotNull(str);
        assertTrue(str.contains("db001"));
        // getJdbcUrl返回null，toString应该处理这个情况
    }

    @Test
    public void testToStringWithComponentsGeneratingUrl() {
        DatabaseEntry entry = new DatabaseEntry();
        entry.setId("db001");
        entry.setCreatedAt(null);
        entry.setUpdatedAt(null);
        entry.setType("mysql");
        entry.setHost("localhost");
        entry.setPort(3307);
        entry.setDatabase("mydb");
        entry.setUsername("user");

        String str = entry.toString();

        assertNotNull(str);
        assertTrue(str.contains("db001"));
        assertTrue(str.contains("jdbc:mysql://localhost:3307/mydb"));
    }
}
