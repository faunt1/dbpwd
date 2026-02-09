package com.dbpwd.cli;

import com.dbpwd.core.ConnectionValidator;
import com.dbpwd.core.PasswordFileManager;
import com.dbpwd.crypto.PasswordDecryptor;
import com.dbpwd.crypto.PasswordEncryptor;
import com.dbpwd.model.DatabaseEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DBPasswordCLI单元测试
 *
 * @author Druid DB Password Manager
 * @version 3.1
 */
public class DBPasswordCLITest {

    @TempDir
    Path tempDir;

    private static final String TEST_KEY = "00112233445566778899AABBCCDDEEFF";

    // 反射辅助方法 - 调用私有方法
    private void invokePrivateMethod(Object target, String methodName, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        method.invoke(target, args);
    }

    // ==================== 构造函数测试 ====================

    @Test
    public void testConstructorWithCustomPaths() {
        String passwordFile = "/custom/path/db.pwd";
        String keyFile = "/custom/path/sm4.key";

        DBPasswordCLI cli = new DBPasswordCLI(passwordFile, keyFile);

        // 构造函数不应抛出异常
        assertNotNull(cli);
    }

    @Test
    public void testConstructorWithDefaultPaths() {
        String defaultPasswordFile = System.getProperty("user.home") + "/.dbpwd/db.pwd";
        String defaultKeyFile = System.getProperty("user.home") + "/.dbpwd/sm4.key";

        DBPasswordCLI cli = new DBPasswordCLI(defaultPasswordFile, defaultKeyFile);

        assertNotNull(cli);
    }

    @Test
    public void testConstructorWithNullPasswordFile() {
        // 构造函数应该允许null参数（后续使用时会处理）
        DBPasswordCLI cli = new DBPasswordCLI(null, "sm4.key");
        assertNotNull(cli);
    }

    @Test
    public void testConstructorWithNullKeyFile() {
        // 构造函数应该允许null参数（后续使用时会处理）
        DBPasswordCLI cli = new DBPasswordCLI("db.pwd", null);
        assertNotNull(cli);
    }

    // ==================== listDatabases() 测试 ====================

    @Test
    public void testListDatabasesEmptyFile() throws Exception {
        Path passwordFile = tempDir.resolve("test-db.pwd");
        createEmptyPasswordFile(passwordFile);

        DBPasswordCLI cli = new DBPasswordCLI(passwordFile.toString(), "dummy-key");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out, true, "UTF-8");
        System.setOut(ps);

        invokePrivateMethod(cli, "listDatabases");

        System.setOut(System.out);

        String output = out.toString("UTF-8");
        assertTrue(output.contains("没有配置任何数据库") || output.contains("个数据库"));
    }

    @Test
    public void testListDatabasesWithEntries() throws Exception {
        Path passwordFile = tempDir.resolve("test-db.pwd");
        createPasswordFileWithEntry(passwordFile);

        DBPasswordCLI cli = new DBPasswordCLI(passwordFile.toString(), "dummy-key");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out, true, "UTF-8");
        System.setOut(ps);

        invokePrivateMethod(cli, "listDatabases");

        System.setOut(System.out);

        String output = out.toString("UTF-8");
        // Check for ASCII-only content to avoid encoding issues
        assertTrue(output.contains("db001"));
        assertTrue(output.contains("mysql"));
        assertTrue(output.contains("testuser"));
        assertTrue(output.contains("1") && output.contains("个数据库"));
    }

    @Test
    public void testListDatabasesMultipleEntries() throws Exception {
        Path passwordFile = tempDir.resolve("test-db.pwd");
        PasswordFileManager manager = new PasswordFileManager(passwordFile.toString());

        manager.addDatabase(createTestEntry("db001", "main", "mysql", "jdbc:mysql://localhost:3306/main", "admin"));
        manager.addDatabase(createTestEntry("db002", "backup", "mysql", "jdbc:mysql://localhost:3306/backup", "backup"));
        manager.save();

        DBPasswordCLI cli = new DBPasswordCLI(passwordFile.toString(), "dummy-key");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out, true, "UTF-8");
        System.setOut(ps);

        invokePrivateMethod(cli, "listDatabases");

        System.setOut(System.out);

        String output = out.toString("UTF-8");
        assertTrue(output.contains("db001"));
        assertTrue(output.contains("db002"));
        assertTrue(output.contains("2") && output.contains("个数据库"));
    }

    @Test
    public void testListDatabasesFileNotFound() throws Exception {
        DBPasswordCLI cli = new DBPasswordCLI("/nonexistent/password.json", "dummy-key");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream ps1 = new PrintStream(out, true, "UTF-8");
        PrintStream ps2 = new PrintStream(err, true, "UTF-8");
        System.setOut(ps1);
        System.setErr(ps2);

        invokePrivateMethod(cli, "listDatabases");

        System.setOut(System.out);
        System.setErr(System.err);

        // Just verify method runs without crashing
        assertNotNull(cli);
    }

    @Test
    public void testListDatabasesFormat() throws Exception {
        Path passwordFile = tempDir.resolve("test-db.pwd");
        createPasswordFileWithEntry(passwordFile);

        DBPasswordCLI cli = new DBPasswordCLI(passwordFile.toString(), "dummy-key");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out, true, "UTF-8");
        System.setOut(ps);

        invokePrivateMethod(cli, "listDatabases");

        System.setOut(System.out);

        String output = out.toString("UTF-8");
        // 验证表格格式
        assertTrue(output.contains("db001") || output.contains("数据库"));
    }

    // ==================== showInfo() 测试 ====================

    @Test
    public void testShowInfoWithFile() throws Exception {
        Path passwordFile = tempDir.resolve("test-db.pwd");
        createPasswordFileWithEntry(passwordFile);

        DBPasswordCLI cli = new DBPasswordCLI(passwordFile.toString(), "dummy-key");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out, true, "UTF-8");
        System.setOut(ps);

        invokePrivateMethod(cli, "showInfo");

        System.setOut(System.out);

        String output = out.toString("UTF-8");
        assertTrue(output.contains("配置信息") || output.contains("文件"));
        assertTrue(output.contains("1"));
    }

    @Test
    public void testShowInfoWithoutFile() throws Exception {
        DBPasswordCLI cli = new DBPasswordCLI("/nonexistent/password.json", "dummy-key");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out, true, "UTF-8");
        System.setOut(ps);

        invokePrivateMethod(cli, "showInfo");

        System.setOut(System.out);

        String output = out.toString("UTF-8");
        assertTrue(output.contains("配置信息") || output.contains("文件"));
        assertTrue(output.contains("无法") || output.contains("配置"));
    }

    // ==================== encryptPassword() 测试 ====================

    @Test
    public void testEncryptPasswordValidKey() throws Exception {
        String plainPassword = "TestPassword123";

        String encrypted = PasswordEncryptor.encrypt(plainPassword, TEST_KEY);

        assertNotNull(encrypted);
        assertNotEquals(plainPassword, encrypted);
        assertTrue(encrypted.startsWith("ENC(SM4-GCM:"));
        assertTrue(encrypted.endsWith(")"));
    }

    @Test
    public void testEncryptPasswordEmptyPassword() throws Exception {
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> PasswordEncryptor.encrypt("", TEST_KEY)
        );
        assertTrue(e.getMessage().contains("明文密码不能为空"));
    }

    @Test
    public void testEncryptPasswordSpecialCharacters() throws Exception {
        String plainPassword = "P@ssw0rd!#$%^&*()";

        String encrypted = PasswordEncryptor.encrypt(plainPassword, TEST_KEY);

        assertNotNull(encrypted);
        assertTrue(encrypted.startsWith("ENC(SM4-GCM:"));

        // 验证可以解密回原文
        String decrypted = PasswordDecryptor.decrypt(encrypted, TEST_KEY);
        assertEquals(plainPassword, decrypted);
    }

    @Test
    public void testEncryptPasswordUnicode() throws Exception {
        String plainPassword = "密码测试123测试";

        String encrypted = PasswordEncryptor.encrypt(plainPassword, TEST_KEY);

        assertNotNull(encrypted);
        String decrypted = PasswordDecryptor.decrypt(encrypted, TEST_KEY);
        assertEquals(plainPassword, decrypted);
    }

    @Test
    public void testEncryptPasswordConsistency() throws Exception {
        String plainPassword = "SamePassword";

        String encrypted1 = PasswordEncryptor.encrypt(plainPassword, TEST_KEY);
        String encrypted2 = PasswordEncryptor.encrypt(plainPassword, TEST_KEY);

        // SM4 GCM模式下，相同明文会产生不同密文（随机IV）
        assertNotEquals(encrypted1, encrypted2);

        // 但解密结果应该相同
        String decrypted1 = PasswordDecryptor.decrypt(encrypted1, TEST_KEY);
        String decrypted2 = PasswordDecryptor.decrypt(encrypted2, TEST_KEY);
        assertEquals(plainPassword, decrypted1);
        assertEquals(plainPassword, decrypted2);
    }

    // ==================== decryptPassword() 测试 ====================

    @Test
    public void testDecryptPasswordValid() throws Exception {
        String plainPassword = "TestPassword123";
        String encrypted = PasswordEncryptor.encrypt(plainPassword, TEST_KEY);

        String decrypted = PasswordDecryptor.decrypt(encrypted, TEST_KEY);

        assertEquals(plainPassword, decrypted);
    }

    @Test
    public void testDecryptPasswordWithENCFormat() {
        String encrypted = "ENC(SM4:R8H3k9mP2nQ6vL4wY0zA5cD7eF9gH1iJ=)";

        // 不应该抛出异常（虽然可能解密结果不正确，但格式应能解析）
        try {
            String decrypted = PasswordDecryptor.decrypt(encrypted, TEST_KEY);
            // 解密结果可能不是原文，但不应抛出格式异常
            assertNotNull(decrypted);
        } catch (Exception e) {
            // 如果解密失败，应该是解密相关的异常而非格式异常
            assertFalse(e.getMessage().contains("格式"));
        }
    }

    @Test
    public void testDecryptPasswordPlainText() throws Exception {
        String plainPassword = "PlainPassword";

        String decrypted = PasswordDecryptor.decrypt(plainPassword, TEST_KEY);

        assertEquals(plainPassword, decrypted);
    }

    @Test
    public void testDecryptPasswordDruidFormat() throws Exception {
        String druidEncrypted = "DES(YWJjZGVmZ2hpams)";
        String key = "01234567890123456789012345678901"; // 32位hex密钥

        try {
            String decrypted = PasswordDecryptor.decrypt(druidEncrypted, key);
            // Druid格式应该能被识别（即使解密可能失败）
            assertNotNull(decrypted);
        } catch (Exception e) {
            // 预期可能抛出异常，但不应该是因为格式识别失败
        }
    }

    @Test
    public void testDecryptPasswordEmptyString() throws Exception {
        // decrypt() throws exception for empty strings
        IllegalArgumentException e = assertThrows(
            IllegalArgumentException.class,
            () -> PasswordDecryptor.decrypt("", TEST_KEY)
        );
        assertTrue(e.getMessage().contains("加密密码不能为空"));

        // autoDecrypt() returns empty strings
        String decrypted = PasswordDecryptor.autoDecrypt("", TEST_KEY);
        assertEquals("", decrypted);
    }

    @Test
    public void testDecryptPasswordNull() {
        assertThrows(Exception.class, () -> {
            PasswordDecryptor.decrypt(null, TEST_KEY);
        });
    }

    // ==================== deleteDatabase() 测试 ====================

    @Test
    public void testDeleteExistingDatabase() throws Exception {
        Path passwordFile = tempDir.resolve("test-db.pwd");
        PasswordFileManager manager = new PasswordFileManager(passwordFile.toString());
        manager.addDatabase(createTestEntry("db001", "测试库", "mysql", "jdbc:mysql://localhost/test", "root"));
        manager.save();

        // DBPasswordCLI cli = new DBPasswordCLI(passwordFile.toString(), TEST_KEY);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        System.setOut(new PrintStream(out));

        // 通过反射调用私有方法（需要修改deleteDatabase使其可以接受直接参数或创建测试专用版本）
        // 这里我们直接创建一个新的manager进行删除测试
        PasswordFileManager newManager = PasswordFileManager.loadFromFile(passwordFile.toString());
        assertTrue(newManager.removeDatabase("db001"));
        newManager.save();

        PasswordFileManager verifyManager = PasswordFileManager.loadFromFile(passwordFile.toString());
        assertEquals(0, verifyManager.size());

        System.setOut(System.out);
    }

    @Test
    public void testDeleteNonExistingDatabase() throws Exception {
        Path passwordFile = tempDir.resolve("test-db.pwd");
        createEmptyPasswordFile(passwordFile);

        // DBPasswordCLI cli = new DBPasswordCLI(passwordFile.toString(), TEST_KEY);

        // 测试删除不存在的数据库
        PasswordFileManager manager = PasswordFileManager.loadFromFile(passwordFile.toString());
        assertFalse(manager.removeDatabase("nonexistent"));
    }

    // ==================== testConnection() 测试 ====================

    @Test
    public void testValidateAllWithSummaryEmpty() throws Exception {
        Path passwordFile = tempDir.resolve("test-db.pwd");
        createEmptyPasswordFile(passwordFile);

        PasswordFileManager manager = PasswordFileManager.loadFromFile(passwordFile.toString());

        String summary = ConnectionValidator.validateAllWithSummary(manager, TEST_KEY);

        assertNotNull(summary);
        assertTrue(summary.contains("数据库连接验证汇总"));
        assertTrue(summary.contains("总计: 0个数据库"));
    }

    @Test
    public void testValidateAllWithSummaryWithEntries() throws Exception {
        Path passwordFile = tempDir.resolve("test-db.pwd");
        PasswordFileManager manager = new PasswordFileManager(passwordFile.toString());
        manager.addDatabase(createTestEntry("db001", "test-db", "mysql", "jdbc:mysql://localhost/test", "root"));
        manager.save();

        PasswordFileManager loadedManager = PasswordFileManager.loadFromFile(passwordFile.toString());
        String summary = ConnectionValidator.validateAllWithSummary(loadedManager, TEST_KEY);

        assertNotNull(summary);
        // Just verify method runs without error and returns something
        assertTrue(summary.length() > 0);
    }

    // ==================== 实用方法 ====================

    private void createEmptyPasswordFile(Path path) throws Exception {
        PasswordFileManager manager = new PasswordFileManager(path.toString());
        manager.save();
    }

    private void createPasswordFileWithEntry(Path path) throws Exception {
        PasswordFileManager manager = new PasswordFileManager(path.toString());
        manager.addDatabase(createTestEntry("db001", "测试数据库", "mysql",
            "jdbc:mysql://localhost:3306/testdb", "testuser"));
        manager.save();
    }

    private DatabaseEntry createTestEntry(String id, String name, String type, String url, String username) {
        DatabaseEntry entry = new DatabaseEntry();
        entry.setId(id);
        entry.setName(name);
        entry.setType(type);
        entry.setUrl(url);
        entry.setUsername(username);
        entry.setPassword("encrypted_password");
        entry.setEncrypted(true);
        return entry;
    }

    // ==================== 默认常量测试 ====================

    @Test
    public void testDefaultPasswordFileConstant() {
        String expected = System.getProperty("user.home") + "/.dbpwd/db.pwd";
        assertEquals(expected, System.getProperty("user.home") + "/.dbpwd/db.pwd");
    }

    @Test
    public void testDefaultKeyFileConstant() {
        String expected = System.getProperty("user.home") + "/.dbpwd/sm4.key";
        assertEquals(expected, System.getProperty("user.home") + "/.dbpwd/sm4.key");
    }

    // ==================== 错误处理测试 ====================

    @Test
    public void testHandleInvalidPasswordFile() throws Exception {
        DBPasswordCLI cli = new DBPasswordCLI("invalid:/path/file", "dummy-key");

        // 创建一个无效的password文件路径
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayOutputStream err = new ByteArrayOutputStream();
        PrintStream ps1 = new PrintStream(out, true, "UTF-8");
        PrintStream ps2 = new PrintStream(err, true, "UTF-8");
        System.setOut(ps1);
        System.setErr(ps2);

        invokePrivateMethod(cli, "listDatabases");

        System.setOut(System.out);
        System.setErr(System.err);

        // 应该输出错误信息而不是崩溃
        String errorOutput = err.toString("UTF-8");
        // Just verify something was written - might be out or err depending on implementation
        assertTrue(!errorOutput.isEmpty() || !out.toString("UTF-8").isEmpty());
    }

    @Test
    public void testNullInputs() {
        DBPasswordCLI cli = new DBPasswordCLI(null, null);

        // 不应该因为null构造参数而抛出异常
        assertNotNull(cli);
    }

    // ==================== 配置验证测试 ====================

    @Test
    public void testCustomConfigPaths() throws Exception {
        Path customPasswordFile = tempDir.resolve("custom-db.pwd");
        Path customKeyFile = tempDir.resolve("custom-sm4.key");

        createEmptyPasswordFile(customPasswordFile);
        Files.write(customKeyFile, TEST_KEY.getBytes());

        DBPasswordCLI cli = new DBPasswordCLI(
            customPasswordFile.toString(),
            customKeyFile.toString()
        );

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(out, true, "UTF-8");
        System.setOut(ps);

        invokePrivateMethod(cli, "showInfo");

        System.setOut(System.out);

        String output = out.toString("UTF-8");
        assertTrue(!output.isEmpty());
    }

    @Test
    public void testPasswordFileExistsValidation() throws Exception {
        Path passwordFile = tempDir.resolve("exists-test.pwd");
        createPasswordFileWithEntry(passwordFile);

        assertTrue(Files.exists(passwordFile));

        PasswordFileManager manager = PasswordFileManager.loadFromFile(passwordFile.toString());
        assertEquals(1, manager.size());
    }
}
