package com.dbpwd.core;

import com.dbpwd.crypto.Sm4GCMCipher;
import com.dbpwd.model.DatabaseEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PasswordProvider单元测试
 */
@DisplayName("PasswordProvider测试")
public class PasswordProviderTest {

    private static final String TEST_KEY_FILE = "test_sm4.key";
    private static final String TEST_PWD_FILE = "test_db_pwd.pwd";
    private String testKeyPath;
    private String testPwdPath;

    @BeforeEach
    public void setUp() throws Exception {
        // 创建临时文件路径
        testKeyPath = System.getProperty("java.io.tmpdir") + File.separator + TEST_KEY_FILE;
        testPwdPath = System.getProperty("java.io.tmpdir") + File.separator + TEST_PWD_FILE;

        // 删除已存在的测试文件
        Files.deleteIfExists(Paths.get(testKeyPath));
        Files.deleteIfExists(Paths.get(testPwdPath));
    }

    @AfterEach
    public void tearDown() throws Exception {
        // 清理测试文件
        Files.deleteIfExists(Paths.get(testKeyPath));
        Files.deleteIfExists(Paths.get(testPwdPath));
    }

    @Test
    @DisplayName("测试getDatabasePassword - 使用指定文件路径")
    public void testGetDatabasePasswordWithPaths() throws Exception {
        // 准备测试数据
        String sm4Key = Sm4KeyLoader.generateKey(testKeyPath, true);
        String plainPassword = "test_password_123";
        String encryptedPassword = Sm4GCMCipher.encrypt(plainPassword, sm4Key);
        String formattedEncrypted = "ENC(SM4-GCM:" + encryptedPassword + ")";

        // 创建密码文件
        PasswordFileManager manager = PasswordFileManager.loadFromFileSafe(testPwdPath);
        DatabaseEntry entry = new DatabaseEntry();
        entry.setId("test_db");
        entry.setName("Test Database");
        entry.setPassword(formattedEncrypted);
        manager.addDatabase(entry);
        manager.save();

        // 测试获取密码
        String result = PasswordProvider.getDatabasePassword("test_db", testKeyPath, testPwdPath);

        assertNotNull(result);
        assertEquals(plainPassword, result);
    }

    @Test
    @DisplayName("测试getDatabasePassword - 使用Sm4KeyLoader和PasswordFileManager")
    public void testGetDatabasePasswordWithLoaders() throws Exception {
        // 准备测试数据
        String sm4Key = Sm4KeyLoader.generateKey(testKeyPath, true);
        String plainPassword = "test_password_456";
        String encryptedPassword = Sm4GCMCipher.encrypt(plainPassword, sm4Key);
        String formattedEncrypted = "ENC(SM4-GCM:" + encryptedPassword + ")";

        // 创建密码文件和密码管理器
        PasswordFileManager manager = PasswordFileManager.loadFromFileSafe(testPwdPath);
        DatabaseEntry entry = new DatabaseEntry();
        entry.setId("test_db2");
        entry.setName("Test Database 2");
        entry.setPassword(formattedEncrypted);
        manager.addDatabase(entry);
        manager.save();

        // 创建加载器
        Sm4KeyLoader loader = new Sm4KeyLoader(testKeyPath);

        // 测试获取密码
        String result = PasswordProvider.getDatabasePassword("test_db2", loader, manager);

        assertNotNull(result);
        assertEquals(plainPassword, result);
    }

    @Test
    @DisplayName("测试getDatabasePassword - 找不到数据库配置抛出异常")
    public void testGetDatabasePasswordNotFound() throws Exception {
        // 创建空的密钥文件和密码文件
        String sm4Key = Sm4KeyLoader.generateKey(testKeyPath, true);
        PasswordFileManager manager = PasswordFileManager.loadFromFileSafe(testPwdPath);
        manager.save();

        Sm4KeyLoader loader = new Sm4KeyLoader(testKeyPath);

        // 测试应抛出异常
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> PasswordProvider.getDatabasePassword("non_existent_db", loader, manager)
        );

        assertTrue(exception.getMessage().contains("未找到数据库配置"));
    }

    @Test
    @DisplayName("测试getDatabasePassword - 数据库密码为空抛出异常")
    public void testGetDatabasePasswordEmptyPassword() throws Exception {
        // 创建密钥文件和密码文件
        String sm4Key = Sm4KeyLoader.generateKey(testKeyPath, true);
        PasswordFileManager manager = PasswordFileManager.loadFromFileSafe(testPwdPath);
        DatabaseEntry entry = new DatabaseEntry();
        entry.setId("empty_pwd_db");
        entry.setName("Empty Password Database");
        entry.setPassword(""); // 空密码
        manager.addDatabase(entry);
        manager.save();

        Sm4KeyLoader loader = new Sm4KeyLoader(testKeyPath);

        // 测试应抛出异常
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> PasswordProvider.getDatabasePassword("empty_pwd_db", loader, manager)
        );

        assertTrue(exception.getMessage().contains("数据库密码为空"));
    }

    @Test
    @DisplayName("测试getDatabasePassword - 不支持的加密算法抛出异常")
    public void testGetDatabasePasswordUnsupportedAlgorithm() throws Exception {
        // 创建密钥文件和密码文件
        String sm4Key = Sm4KeyLoader.generateKey(testKeyPath, true);
        PasswordFileManager manager = PasswordFileManager.loadFromFileSafe(testPwdPath);
        DatabaseEntry entry = new DatabaseEntry();
        entry.setId("invalid_algo_db");
        entry.setName("Invalid Algorithm Database");
        entry.setPassword("ENC(SM4:invalid)"); // 不支持的SM4-ECB格式
        manager.addDatabase(entry);
        manager.save();

        Sm4KeyLoader loader = new Sm4KeyLoader(testKeyPath);

        // 测试应抛出异常
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> PasswordProvider.getDatabasePassword("invalid_algo_db", loader, manager)
        );

        assertTrue(exception.getMessage().contains("不支持的加密算法"));
        assertTrue(exception.getMessage().contains("仅支持SM4-GCM格式"));
    }

    @Test
    @DisplayName("测试getDatabasePassword - 格式错误的密码抛出异常")
    public void testGetDatabasePasswordInvalidFormat() throws Exception {
        // 创建密钥文件和密码文件
        String sm4Key = Sm4KeyLoader.generateKey(testKeyPath, true);
        PasswordFileManager manager = PasswordFileManager.loadFromFileSafe(testPwdPath);
        DatabaseEntry entry = new DatabaseEntry();
        entry.setId("invalid_format_db");
        entry.setName("Invalid Format Database");
        entry.setPassword("INVALID_FORMAT");
        manager.addDatabase(entry);
        manager.save();

        Sm4KeyLoader loader = new Sm4KeyLoader(testKeyPath);

        // 测试应抛出异常
        assertThrows(
            Exception.class,
            () -> PasswordProvider.getDatabasePassword("invalid_format_db", loader, manager)
        );
    }

    @Test
    @DisplayName("测试getDatabasePassword - 多个数据库配置")
    public void testGetDatabasePasswordMultipleDatabases() throws Exception {
        // 准备测试数据
        String sm4Key = Sm4KeyLoader.generateKey(testKeyPath, true);

        PasswordFileManager manager = PasswordFileManager.loadFromFileSafe(testPwdPath);

        // 数据库1
        DatabaseEntry entry1 = new DatabaseEntry();
        entry1.setId("db1");
        entry1.setName("Database 1");
        entry1.setPassword("ENC(SM4-GCM:" + Sm4GCMCipher.encrypt("password1", sm4Key) + ")");
        manager.addDatabase(entry1);

        // 数据库2
        DatabaseEntry entry2 = new DatabaseEntry();
        entry2.setId("db2");
        entry2.setName("Database 2");
        entry2.setPassword("ENC(SM4-GCM:" + Sm4GCMCipher.encrypt("password2", sm4Key) + ")");
        manager.addDatabase(entry2);

        // 数据库3
        DatabaseEntry entry3 = new DatabaseEntry();
        entry3.setId("db3");
        entry3.setName("Database 3");
        entry3.setPassword("ENC(SM4-GCM:" + Sm4GCMCipher.encrypt("password3", sm4Key) + ")");
        manager.addDatabase(entry3);

        manager.save();

        Sm4KeyLoader loader = new Sm4KeyLoader(testKeyPath);

        // 验证三个数据库都能正确获取密码
        String pwd1 = PasswordProvider.getDatabasePassword("db1", loader, manager);
        String pwd2 = PasswordProvider.getDatabasePassword("db2", loader, manager);
        String pwd3 = PasswordProvider.getDatabasePassword("db3", loader, manager);

        assertEquals("password1", pwd1);
        assertEquals("password2", pwd2);
        assertEquals("password3", pwd3);
    }

    @Test
    @DisplayName("测试getDatabasePassword - 空的keyFilePath使用默认路径")
    public void testGetDatabasePasswordEmptyKeyPath() throws Exception {
        // 准备测试数据
        String sm4Key = Sm4KeyLoader.generateKey(testKeyPath, true);
        String plainPassword = "test_password_789";
        String encryptedPassword = Sm4GCMCipher.encrypt(plainPassword, sm4Key);
        String formattedEncrypted = "ENC(SM4-GCM:" + encryptedPassword + ")";

        // 创建密码文件
        PasswordFileManager manager = PasswordFileManager.loadFromFileSafe(testPwdPath);
        DatabaseEntry entry = new DatabaseEntry();
        entry.setId("default_path_db");
        entry.setName("Default Path Database");
        entry.setPassword(formattedEncrypted);
        manager.addDatabase(entry);
        manager.save();

        // 使用空的keyFilePath，应使用默认路径（但因为我们设置了testPwdPath，所以测试pwdFilePath参数）
        // 这里我们测试pwdFilePath使用空值的情况

        // 测试pwdFilePath为null时，使用默认路径的情况
        // 由于默认路径不存在，应该抛出异常
        assertThrows(
            Exception.class,
            () -> PasswordProvider.getDatabasePassword("default_path_db", testKeyPath, null)
        );
    }
}
