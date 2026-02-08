package com.dbpwd.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PasswordFile单元测试
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class PasswordFileTest {

    @Test
    public void testDefaultConstructor() {
        PasswordFile file = new PasswordFile();

        assertNotNull(file, "PasswordFile不应为null");
        assertEquals("1.0", file.getVersion(), "默认版本应为1.0");
        assertNotNull(file.getLastModified(), "最后修改时间不应为null");
        assertNotNull(file.getDatabases(), "数据库列表不应为null");
        assertTrue(file.getDatabases().isEmpty(), "数据库列表应为空");
    }

    @Test
    public void testAddDatabase() {
        PasswordFile file = new PasswordFile();
        DatabaseEntry entry = createTestEntry("db001", "Test DB", "mysql");

        file.addDatabase(entry);

        assertEquals(1, file.size(), "数据库数量应为1");
        assertEquals("db001", file.getById("db001").getId(), "应能添加并获取数据库条目");
    }

    @Test
    public void testAddDatabaseReplacesExistingEntry() {
        PasswordFile file = new PasswordFile();
        DatabaseEntry entry1 = createTestEntry("db001", "Test DB", "mysql");
        DatabaseEntry entry2 = createTestEntry("db001", "Updated DB", "postgresql");

        file.addDatabase(entry1);
        assertEquals("Test DB", file.getById("db001").getName());

        file.addDatabase(entry2);
        assertEquals(1, file.size(), "数据库数量应仍为1");
        assertEquals("Updated DB", file.getById("db001").getName(), "应替换原有条目");
    }

    @Test
    public void testUpdateDatabase() {
        PasswordFile file = new PasswordFile();
        DatabaseEntry entry = createTestEntry("db001", "Test DB", "mysql");

        file.addDatabase(entry);

        // 更新条目
        DatabaseEntry updateEntry = new DatabaseEntry();
        updateEntry.setId("db001");
        updateEntry.setName("Updated Name");

        file.updateDatabase(updateEntry);

        assertEquals("Updated Name", file.getById("db001").getName());
    }

    @Test
    public void testUpdateDatabaseAddsIfNotExists() {
        PasswordFile file = new PasswordFile();
        DatabaseEntry updateEntry = createTestEntry("db001", "Test DB", "mysql");

        file.updateDatabase(updateEntry);

        assertEquals(1, file.size(), "应添加新条目");
        assertEquals("db001", file.getById("db001").getId());
    }

    @Test
    public void testRemoveDatabase() {
        PasswordFile file = new PasswordFile();
        file.addDatabase(createTestEntry("db001", "Test DB", "mysql"));
        file.addDatabase(createTestEntry("db002", "Test DB2", "postgresql"));

        assertTrue(file.removeDatabase("db001"), "删除应返回true");
        assertEquals(1, file.size(), "数据库数量应为1");
        assertFalse(file.removeDatabase("db001"), "删除不存在的应返回false");
    }

    @Test
    public void testGetById() {
        PasswordFile file = new PasswordFile();
        file.addDatabase(createTestEntry("db001", "Test DB", "mysql"));

        DatabaseEntry entry = file.getById("db001");
        assertNotNull(entry, "应能获取条目");
        assertEquals("db001", entry.getId());

        assertNull(file.getById("nonexistent"), "不存在的ID应返回null");
    }

    @Test
    public void testGetByIdWithNull() {
        PasswordFile file = new PasswordFile();

        assertNull(file.getById(null), "null ID应返回null");
    }

    @Test
    public void testGetByUrl() {
        PasswordFile file = new PasswordFile();
        String url = "jdbc:mysql://localhost:3306/testdb";
        DatabaseEntry entry = createTestEntry("db001", "Test DB", "mysql");
        entry.setUrl(url);

        file.addDatabase(entry);

        DatabaseEntry foundEntry = file.getByUrl(url);
        assertNotNull(foundEntry, "应能通过URL获取条目");
        assertEquals("db001", foundEntry.getId());

        assertNull(file.getByUrl("jdbc:mysql://nonexistent"), "不存在的URL应返回null");
    }

    @Test
    public void testGetByUsername() {
        PasswordFile file = new PasswordFile();
        file.addDatabase(createTestEntry("db001", "Test DB1", "mysql"));
        file.addDatabase(createTestEntry("db002", "Test DB2", "postgresql"));
        DatabaseEntry entry3 = createTestEntry("db003", "Test DB3", "mysql");
        entry3.setUsername("sameuser");

        file.addDatabase(entry3);

        List<DatabaseEntry> results = file.getByUsername("sameuser");
        assertEquals(1, results.size(), "应找到一个条目");
        assertEquals("db003", results.get(0).getId());
    }

    @Test
    public void testGetByTag() {
        PasswordFile file = new PasswordFile();
        DatabaseEntry entry1 = createTestEntry("db001", "Test DB1", "mysql");
        entry1.getTags().add("production");
        entry1.getTags().add("mysql");
        entry1.getTags().add("test");
        file.addDatabase(entry1);

        DatabaseEntry entry2 = createTestEntry("db002", "Test DB2", "postgresql");
        entry2.getTags().add("production");
        entry2.getTags().add("test");
        file.addDatabase(entry2);

        List<DatabaseEntry> tagResults = file.getByTag("production");
        assertEquals(2, tagResults.size(), "应找到两个production标签的条目");

        List<DatabaseEntry> mysqlTags = file.getByTag("mysql");
        assertEquals(1, mysqlTags.size(), "应找到一个mysql标签的条目");
    }

    @Test
    public void testGetAllDatabases() {
        PasswordFile file = new PasswordFile();
        file.addDatabase(createTestEntry("db001", "DB1", "mysql"));
        file.addDatabase(createTestEntry("db002", "DB2", "postgresql"));

        List<DatabaseEntry> all = file.getAllDatabases();
        assertEquals(2, all.size(), "应获取所有数据库");
    }

    @Test
    public void testSize() {
        PasswordFile file = new PasswordFile();
        assertEquals(0, file.size(), "空文件大小应为0");

        file.addDatabase(createTestEntry("db001", "DB1", "mysql"));
        assertEquals(1, file.size(), "大小应为1");

        file.addDatabase(createTestEntry("db002", "DB2", "postgresql"));
        assertEquals(2, file.size(), "大小应为2");
    }

    @Test
    public void testIsEmpty() {
        PasswordFile file = new PasswordFile();
        assertTrue(file.isEmpty(), "新文件应为空");

        file.addDatabase(createTestEntry("db001", "DB1", "mysql"));
        assertFalse(file.isEmpty(), "添加条目后不应为空");
    }

    @Test
    public void testClear() {
        PasswordFile file = new PasswordFile();
        file.addDatabase(createTestEntry("db001", "DB1", "mysql"));
        file.addDatabase(createTestEntry("db002", "DB2", "postgresql"));

       assertFalse(file.isEmpty(), "添加条目后不应为空");

        file.clear();

        assertTrue(file.isEmpty(), "清空后应为空");
        assertEquals(0, file.size(), "清空后大小应为0");
    }

    @Test
    public void testTouchLastModified() {
        PasswordFile file = new PasswordFile();
        String before = file.getLastModified();

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        file.touchLastModified();
        String after = file.getLastModified();

        assertNotNull(after, "更新时间不应为null");
        assertNotEquals(before, after, "更新时间应发生变化");
    }

    @Test
    public void testSetAndGetVersion() {
        PasswordFile file = new PasswordFile();

        file.setVersion("2.0");
        assertEquals("2.0", file.getVersion());

        file.setVersion(null);
        assertNull(file.getVersion());
    }

    @Test
    public void testSetAndGetLastModified() {
        PasswordFile file = new PasswordFile();
        String newTime = java.time.format.DateTimeFormatter.ISO_INSTANT.format(
            java.time.Instant.now());
        file.setLastModified(newTime);

        assertEquals(newTime, file.getLastModified());
    }

    @Test
    public void testSetAndGetDatabases() {
        PasswordFile file = new PasswordFile();
        List<DatabaseEntry> newDatabases = new ArrayList<>();
        newDatabases.add(createTestEntry("db001", "DB1", "mysql"));

        file.setDatabases(newDatabases);

        assertEquals(1, file.getDatabases().size());
        assertEquals("db001", file.getDatabases().get(0).getId());
    }

    @Test
    public void testSetDatabasesWithNull() {
        PasswordFile file = new PasswordFile();
        file.addDatabase(createTestEntry("db001", "DB1", "mysql"));

        file.setDatabases(null);

        assertNotNull(file.getDatabases(), "设置为null后应初始化为空列表");
        assertTrue(file.getDatabases().isEmpty());
    }

    @Test
    public void testFromMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("version", "2.0");
        map.put("lastModified", "2026-02-08T10:00:00:00Z");

        List<Map<String, Object>> dbList = new ArrayList<>();
        Map<String, Object> dbMap = new HashMap<>();
        dbMap.put("id", "db001");
        dbMap.put("name", "Test DB");
        dbMap.put("type", "mysql");
        dbMap.put("url", "jdbc:mysql://localhost:3306/test");
        dbMap.put("username", "testuser");
        dbMap.put("password", "testpass");
        dbList.add(dbMap);
        map.put("databases", dbList);

        PasswordFile file = PasswordFile.fromMap(map);

        assertEquals("2.0", file.getVersion());
        assertNotNull(file.getLastModified());
        assertEquals(1, file.size());
        assertEquals("db001", file.getById("db001").getId());
    }

    @Test
    public void testFromMapWithEmptyDatabases() {
        Map<String, Object> map = new HashMap<>();
        map.put("version", "1.0");
        map.put("lastModified", "2026-02-08T10:00:00:00Z");
        map.put("databases", new ArrayList<>());

        PasswordFile file = PasswordFile.fromMap(map);

        assertEquals(0, file.size());
        assertTrue(file.isEmpty());
    }

    @Test
    public void testFromMapWithNullDatabases() {
        Map<String, Object> map = new HashMap<>();
        map.put("version", "1.0");
        map.put("lastModified", "2026-02-08T10:00:00:00Z");
        map.put("databases", null);

        PasswordFile file = PasswordFile.fromMap(map);

        assertNotNull(file);
        assertTrue(file.isEmpty());
    }

    @Test
    public void testToMap() {
        PasswordFile file = new PasswordFile();
        file.addDatabase(createTestEntry("db001", "Test DB", "mysql"));

        Map<String, Object> map = file.toMap();

        assertNotNull(map);
        assertEquals("1.0", map.get("version"));
        assertNotNull(map.get("lastModified"));
        assertNotNull(map.get("databases"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dbList = (List<Map<String, Object>>) map.get("databases");
        assertEquals(1, dbList.size());
        assertEquals("db001", dbList.get(0).get("id"));
    }

    // --- 辅助方法 ---

    private DatabaseEntry createTestEntry(String id, String name, String type) {
        DatabaseEntry entry = new DatabaseEntry();
        entry.setId(id);
        entry.setName(name);
        entry.setType(type);
        entry.setUrl("jdbc:" + type + "://localhost:3306/test");
        entry.setUsername("testuser");
        entry.setPassword("testpass");
        return entry;
    }
}
