package com.dbpwd.core;

import com.dbpwd.model.DatabaseEntry;
import com.dbpwd.model.PasswordFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PasswordFileManager单元测试
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class PasswordFileManagerTest {

    @TempDir
    Path tempDir;

    private Path testFilePath;

    @BeforeEach
    public void setUp() {
        testFilePath = tempDir.resolve("test-password.json");
    }

    // ==================== 构造函数测试 ====================

    @Test
    public void testConstructor() {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        assertNotNull(manager);
        assertEquals(testFilePath.toString(), manager.getFilePath());
        assertNull(manager.getPasswordFile());
    }

    @Test
    public void testConstructorWithNullPath() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> new PasswordFileManager(null)
        );

        assertTrue(e.getMessage().contains("不能为空"));
    }

    @Test
    public void testConstructorWithEmptyPath() {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> new PasswordFileManager("")
        );

        assertTrue(e.getMessage().contains("不能为空"));
    }

    @Test
    public void testConstructorWithTildePath() {
        PasswordFileManager manager = new PasswordFileManager("~/.dbpwd/test.json");
        String expected = System.getProperty("user.home") + "/.dbpwd/test.json";

        assertEquals(expected, manager.getFilePath());
    }

    // ==================== load() 测试 ====================

    @Test
    public void testLoadCreatesNewFile() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        PasswordFileManager result = manager.load();

        assertSame(manager, result);
        assertNotNull(manager.getPasswordFile());
        assertTrue(manager.isEmpty());
        assertFalse(Files.exists(testFilePath)); // 不存在时不创建文件
    }

    @Test
    public void testLoadExistingFile() throws IOException {
        // 创建测试文件
        String json = "{\n" +
            "  \"version\": \"1.0\",\n" +
            "  \"databases\": []\n" +
            "}";
        Files.write(testFilePath, json.getBytes());

        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        PasswordFileManager result = manager.load();

        assertSame(manager, result);
        assertNotNull(manager.getPasswordFile());
        assertTrue(manager.isEmpty());
    }

    @Test
    public void testLoadEmptyFile() throws IOException {
        Files.write(testFilePath, "".getBytes());

        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        manager.load();

        assertNotNull(manager.getPasswordFile());
        assertTrue(manager.isEmpty());
    }

    @Test
    public void testLoadWhitespaceFile() throws IOException {
        Files.write(testFilePath, "   \n\n  \t  ".getBytes());

        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        manager.load();

        assertNotNull(manager.getPasswordFile());
        assertTrue(manager.isEmpty());
    }

    @Test
    public void testLoadWithDatabases() throws IOException {
        String json = "{\n" +
            "  \"version\": \"1.0\",\n" +
            "  \"databases\": [\n" +
            "    {\n" +
            "      \"id\": \"db001\",\n" +
            "      \"name\": \"Test DB\",\n" +
            "      \"type\": \"mysql\",\n" +
            "      \"url\": \"jdbc:mysql://localhost:3306/test\",\n" +
            "      \"username\": \"user\",\n" +
            "      \"password\": \"pass\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";
        Files.write(testFilePath, json.getBytes());

        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        manager.load();

        assertEquals(1, manager.size());
        assertNotNull(manager.getById("db001"));
    }

    @Test
    public void testLoadInvalidJson() throws IOException {
        Files.write(testFilePath, "{ invalid json }".getBytes());

        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        IOException e = assertThrows(
            IOException.class,
            () -> manager.load()
        );

        assertTrue(e.getMessage().contains("解析密码文件失败"));
    }

    @Test
    public void testLoadDirectoryInsteadOfFile() throws IOException {
        Path dirPath = tempDir.resolve("testdir");
        Files.createDirectory(dirPath);

        PasswordFileManager manager = new PasswordFileManager(dirPath.toString());

        IOException e = assertThrows(
            IOException.class,
            () -> manager.load()
        );

        assertTrue(e.getMessage().contains("目录而非文件"));
    }

    // ==================== save() 测试 ====================

    @Test
    public void testSaveEmptyManager() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());
        manager.load();

        manager.save();

        assertTrue(Files.exists(testFilePath));

        String content = new String(Files.readAllBytes(testFilePath));
        assertTrue(content.contains("\"version\""));
        assertTrue(content.contains("\"databases\""));
    }

    @Test
    public void testSaveWithEntries() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());
        manager.load();

        DatabaseEntry entry = new DatabaseEntry();
        entry.setId("db001");
        entry.setName("Test DB");
        entry.setType("mysql");
        entry.setUrl("jdbc:mysql://localhost/db");
        entry.setUsername("user");
        entry.setPassword("pass");
        manager.addDatabase(entry);

        manager.save();

        String content = new String(Files.readAllBytes(testFilePath));
        assertTrue(content.contains("db001"));
        assertTrue(content.contains("Test DB"));
    }

    @Test
    public void testSaveUpdatesLastModified() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        manager.save();

        String content = new String(Files.readAllBytes(testFilePath));
        assertTrue(content.contains("\"lastModified\""));
    }

    @Test
    public void testSaveOverwritesExisting() throws IOException {
        // 创建有有效JSON的现有文件
        String existingJson = "{\"version\":\"1.0\",\"databases\":[]}";
        Files.write(testFilePath, existingJson.getBytes());

        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());
        manager.load();

        // 添加一个条目
        DatabaseEntry entry = createTestEntry();
        manager.addDatabase(entry);

        manager.save();

        String content = new String(Files.readAllBytes(testFilePath));
        assertFalse(content.contains("databases\":[]"));
        assertTrue(content.contains("db001"));
    }

    // ==================== CRUD 操作测试 ====================

    @Test
    public void testAddDatabase() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        DatabaseEntry entry = createTestEntry();

        manager.addDatabase(entry);

        assertEquals(1, manager.size());
        assertEquals("db001", manager.getById("db001").getId());
    }

    @Test
    public void testAddDatabaseNull() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> manager.addDatabase(null)
        );

        assertTrue(e.getMessage().contains("不能为null"));
    }

    @Test
    public void testAddDatabaseReplacesExisting() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());
        manager.load();

        DatabaseEntry entry1 = new DatabaseEntry();
        entry1.setId("db001");
        entry1.setName("DB 1");
        manager.addDatabase(entry1);

        DatabaseEntry entry2 = new DatabaseEntry();
        entry2.setId("db001");
        entry2.setName("DB 2");
        manager.addDatabase(entry2);

        assertEquals(1, manager.size());
        assertEquals("DB 2", manager.getById("db001").getName());
    }

    @Test
    public void testUpdateDatabase() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        DatabaseEntry entry = createTestEntry();
        manager.addDatabase(entry);

        DatabaseEntry update = new DatabaseEntry();
        update.setId("db001");
        update.setName("Updated Name");

        manager.updateDatabase(update);

        assertEquals("Updated Name", manager.getById("db001").getName());
    }

    @Test
    public void testUpdateDatabaseNull() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> manager.updateDatabase(null)
        );

        assertTrue(e.getMessage().contains("不能为null"));
    }

    @Test
    public void testUpdateDatabaseAddsIfNotExists() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        DatabaseEntry entry = createTestEntry();
        manager.updateDatabase(entry);

        assertEquals(1, manager.size());
        assertEquals("db001", manager.getById("db001").getId());
    }

    @Test
    public void testRemoveDatabase() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());
        manager.addDatabase(createTestEntry());

        boolean result = manager.removeDatabase("db001");

        assertTrue(result);
        assertEquals(0, manager.size());
        assertNull(manager.getById("db001"));
    }

    @Test
    public void testRemoveDatabaseNonExistent() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        boolean result = manager.removeDatabase("nonexistent");

        assertFalse(result);
    }

    @Test
    public void testGetById() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        DatabaseEntry entry = createTestEntry();
        manager.addDatabase(entry);

        DatabaseEntry result = manager.getById("db001");

        assertNotNull(result);
        assertEquals("db001", result.getId());
    }

    @Test
    public void testGetByIdNonExistent() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        DatabaseEntry result = manager.getById("nonexistent");

        assertNull(result);
    }

    @Test
    public void testGetByIdNull() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        DatabaseEntry result = manager.getById(null);

        assertNull(result);
    }

    @Test
    public void testGetByUrl() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        DatabaseEntry entry = createTestEntry();
        manager.addDatabase(entry);

        DatabaseEntry result = manager.getByUrl("jdbc:mysql://localhost:3306/testdb");

        assertNotNull(result);
        assertEquals("db001", result.getId());
    }

    @Test
    public void testGetByUrlNonExistent() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        DatabaseEntry result = manager.getByUrl("jdbc:mysql://nonexistent");

        assertNull(result);
    }

    @Test
    public void testGetByUsername() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        manager.addDatabase(createTestEntry("db1", "DB1", "mysql", "user1"));
        manager.addDatabase(createTestEntry("db2", "DB2", "mysql", "user2"));
        manager.addDatabase(createTestEntry("db3", "DB3", "mysql", "user1"));

        List<DatabaseEntry> results = manager.getByUsername("user1");

        assertEquals(2, results.size());
        // 验证结果包含db1和db3
        List<String> ids = new ArrayList<>();
        for (DatabaseEntry e : results) {
            ids.add(e.getId());
        }
        assertTrue(ids.contains("db1"));
        assertTrue(ids.contains("db3"));
    }

    @Test
    public void testGetByUsernameNonExistent() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        List<DatabaseEntry> results = manager.getByUsername("nonexistent");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testGetByUsernameNull() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        List<DatabaseEntry> results = manager.getByUsername(null);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testGetByTag() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        DatabaseEntry entry1 = createTestEntry("db1", "DB1", "mysql");
        entry1.getTags().add("production");
        manager.addDatabase(entry1);

        DatabaseEntry entry2 = createTestEntry("db2", "DB2", "mysql");
        entry2.getTags().add("staging");
        manager.addDatabase(entry2);

        DatabaseEntry entry3 = createTestEntry("db3", "DB3", "mysql");
        entry3.getTags().add("production");
        manager.addDatabase(entry3);

        List<DatabaseEntry> results = manager.getByTag("production");

        assertEquals(2, results.size());
        List<String> ids = new ArrayList<>();
        for (DatabaseEntry e : results) {
            ids.add(e.getId());
        }
        assertTrue(ids.contains("db1"));
        assertTrue(ids.contains("db3"));
    }

    @Test
    public void testGetByTagNonExistent() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        List<DatabaseEntry> results = manager.getByTag("nonexistent");

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    @Test
    public void testGetAllDatabases() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        manager.addDatabase(createTestEntry("db1", "DB1", "mysql"));
        manager.addDatabase(createTestEntry("db2", "DB2", "postgresql"));

        List<DatabaseEntry> all = manager.getAllDatabases();

        assertEquals(2, all.size());
    }

    @Test
    public void testGetAllDatabasesEmpty() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        List<DatabaseEntry> all = manager.getAllDatabases();

        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    // ==================== size/isEmpty/clear 测试 ====================

    @Test
    public void testSize() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        assertEquals(0, manager.size());

        manager.addDatabase(createTestEntry());
        assertEquals(1, manager.size());

        manager.addDatabase(createTestEntry("db2", "DB2", "mysql"));
        assertEquals(2, manager.size());

        manager.removeDatabase("db001");
        assertEquals(1, manager.size());
    }

    @Test
    public void testIsEmpty() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        assertTrue(manager.isEmpty());

        manager.addDatabase(createTestEntry());
        assertFalse(manager.isEmpty());

        manager.clear();
        assertTrue(manager.isEmpty());
    }

    @Test
    public void testClear() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        manager.addDatabase(createTestEntry("db1", "DB1", "mysql"));
        manager.addDatabase(createTestEntry("db2", "DB2", "postgresql"));

        manager.clear();

        assertTrue(manager.isEmpty());
        assertEquals(0, manager.size());
    }

    // ==================== 密码加解密测试 ====================

    @Test
    public void testGetDecryptedPasswordPlain() throws Exception {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        DatabaseEntry entry = createTestEntry();
        entry.setPassword("plaintext_password");
        manager.addDatabase(entry);

        String decrypted = manager.getDecryptedPassword("db001", "dummy_key");

        assertEquals("plaintext_password", decrypted);
    }

    @Test
    public void testGetDecryptedPasswordNonExistent() {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> manager.getDecryptedPassword("nonexistent", "dummy_key")
        );

        assertTrue(e.getMessage().contains("未找到"));
    }

    @Test
    public void testGetDecryptedPasswordEmptyPassword() throws Exception {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        DatabaseEntry entry = createTestEntry();
        entry.setPassword("");
        manager.addDatabase(entry);

        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> manager.getDecryptedPassword("db001", "dummy_key")
        );

        assertTrue(e.getMessage().contains("密码为空"));
    }

    @Test
    public void testEncryptPasswordAlreadyEncrypted() throws Exception {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        DatabaseEntry entry = createTestEntry();
        entry.setPassword("ENC(encrypted_text)");
        entry.setEncrypted(true);
        manager.addDatabase(entry);

        // 不应该抛出异常
        manager.encryptPassword("db001", "dummy_key");

        // 密码应该保持不变
        assertEquals("ENC(encrypted_text)", entry.getPassword());
    }

    @Test
    public void testEncryptPasswordNonExistent() {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> manager.encryptPassword("nonexistent", "dummy_key")
        );

        assertTrue(e.getMessage().contains("未找到"));
    }

    @Test
    public void testEncryptPasswordEmptyPassword() throws Exception {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());

        DatabaseEntry entry = createTestEntry();
        entry.setPassword("");
        manager.addDatabase(entry);

        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> manager.encryptPassword("db001", "dummy_key")
        );

        assertTrue(e.getMessage().contains("密码为空"));
    }

    // ==================== Getter测试 ====================

    @Test
    public void testGetFilePath() {
        String customPath = "/path/to/custom.json";
        PasswordFileManager manager = new PasswordFileManager(customPath);

        assertEquals(customPath, manager.getFilePath());
    }

    @Test
    public void testGetPasswordFile() throws IOException {
        PasswordFileManager manager = new PasswordFileManager(testFilePath.toString());
        manager.load();

        PasswordFile file = manager.getPasswordFile();

        assertNotNull(file);
    }

    // ==================== 静态工厂方法测试 ====================

    @Test
    public void testLoadFromFile() throws IOException {
        String json = "{\n" +
            "  \"version\": \"1.0\",\n" +
            "  \"databases\": []\n" +
            "}";
        Files.write(testFilePath, json.getBytes());

        PasswordFileManager manager = PasswordFileManager.loadFromFile(testFilePath.toString());

        assertNotNull(manager);
        assertNotNull(manager.getPasswordFile());
    }

    @Test
    public void testLoadFromFileDoesNotExist() throws IOException {
        Path nonExistent = tempDir.resolve("nonexistent.json");

        PasswordFileManager manager = PasswordFileManager.loadFromFile(nonExistent.toString());

        // 文件不存在时创建新的空PasswordFileManager
        assertNotNull(manager);
        assertNotNull(manager.getPasswordFile());
        assertTrue(manager.isEmpty());
    }

    @Test
    public void testLoadFromFileInvalidJson() throws IOException {
        Files.write(testFilePath, "{ invalid".getBytes());

        IOException e = assertThrows(
            IOException.class,
            () -> PasswordFileManager.loadFromFile(testFilePath.toString())
        );

        assertTrue(e.getMessage().contains("解析") || e.getMessage().contains("失败"));
    }

    @Test
    public void testLoadFromFileSafeHandlesErrors() throws IOException {
        Path invalidPath = tempDir.resolve("invalid");

        PasswordFileManager manager = PasswordFileManager.loadFromFileSafe(invalidPath.toString());

        assertNotNull(manager);
        assertNotNull(manager.getPasswordFile());
        assertTrue(manager.isEmpty());
    }

    @Test
    public void testLoadFromFileSafeSuccess() throws IOException {
        String json = "{\n" +
            "  \"version\": \"1.0\",\n" +
            "  \"databases\": []\n" +
            "}";
        Files.write(testFilePath, json.getBytes());

        PasswordFileManager manager = PasswordFileManager.loadFromFileSafe(testFilePath.toString());

        assertNotNull(manager);
        assertNotNull(manager.getPasswordFile());
    }

    @Test
    public void testNeedsReload() throws IOException {
        Files.write(testFilePath, "test".getBytes());
        long before = System.currentTimeMillis();

        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Files.write(testFilePath, "updated".getBytes());

        assertTrue(PasswordFileManager.needsReload(testFilePath.toString(), before));
    }

    @Test
    public void testNeedsReloadNoChange() throws IOException {
        Files.write(testFilePath, "test".getBytes());
        long timestamp = Files.getLastModifiedTime(testFilePath).toMillis();

        assertFalse(PasswordFileManager.needsReload(testFilePath.toString(), timestamp));
    }

    @Test
    public void testNeedsReloadNonExistentFile() {
        Path nonExistent = tempDir.resolve("nonexistent.json");
        long timestamp = System.currentTimeMillis();

        assertFalse(PasswordFileManager.needsReload(nonExistent.toString(), timestamp));
    }

    @Test
    public void testGetFileModifiedTime() throws IOException {
        Files.write(testFilePath, "test".getBytes());

        long timestamp = PasswordFileManager.getFileModifiedTime(testFilePath.toString());

        assertTrue(timestamp > 0);
        assertEquals(timestamp, new File(testFilePath.toString()).lastModified());
    }

    @Test
    public void testGetFileModifiedTimeNonExistent() {
        Path nonExistent = tempDir.resolve("nonexistent.json");

        assertEquals(0, PasswordFileManager.getFileModifiedTime(nonExistent.toString()));
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
        return createTestEntry(id, name, type, "testuser");
    }

    private DatabaseEntry createTestEntry(String id, String name, String type, String username) {
        DatabaseEntry entry = new DatabaseEntry();
        entry.setId(id);
        entry.setName(name);
        entry.setType(type);
        entry.setUrl("jdbc:" + type + "://localhost:3306/test");
        entry.setUsername(username);
        entry.setPassword("testpass");
        return entry;
    }
}
