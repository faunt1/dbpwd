package com.dbpwd.core;

import com.dbpwd.crypto.Sm4GCMCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StandalonePasswordDecryptor单元测试
 *
 * 测试纯JDBC密码解密工具类的功能完整性
 *
 * @author Database Password Manager
 * @version 5.3
 */
public class StandalonePasswordDecryptorTest {

    @TempDir
    Path tempDir;

    private Path keyFilePath;
    private Path pwdFilePath;

    private static final String SM4_KEY = "0123456789abcdef0123456789abcdef";
    private static final String TEST_DB_ID = "test_db";
    private static final String TEST_PASSWORD = "test_password_123";

    @BeforeEach
    public void setUp() throws Exception {
        keyFilePath = tempDir.resolve("sm4.key");
        pwdFilePath = tempDir.resolve("db.pwd");

        // 创建SM4密钥文件
        Files.write(keyFilePath, SM4_KEY.getBytes());

        // 创建密码文件
        createTestPasswordFile();
    }

    /**
     * 创建测试密码文件
     */
    private void createTestPasswordFile() throws Exception {
        String encryptedPassword = Sm4GCMCipher.encrypt(TEST_PASSWORD, SM4_KEY);
        String formattedPassword = com.dbpwd.crypto.ENCFormat.formatSM4GCM(encryptedPassword);

        String json = "{\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"lastModified\": \"2026-02-11T00:00:00Z\",\n" +
                "  \"databases\": [\n" +
                "    {\n" +
                "      \"id\": \"" + TEST_DB_ID + "\",\n" +
                "      \"name\": \"Test Database\",\n" +
                "      \"type\": \"mysql\",\n" +
                "      \"host\": \"localhost\",\n" +
                "      \"port\": 3306,\n" +
                "      \"database\": \"test_db\",\n" +
                "      \"url\": \"jdbc:mysql://localhost:3306/test_db\",\n" +
                "      \"username\": \"test_user\",\n" +
                "      \"password\": \"" + formattedPassword + "\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Files.write(pwdFilePath, json.getBytes());
    }

    // ==================== getPlainPassword() 方法测试 ====================

    @Test
    public void testGetPlainPassword() throws Exception {
        String plainPassword = StandalonePasswordDecryptor.getPlainPassword(
                TEST_DB_ID, keyFilePath.toString(), pwdFilePath.toString());

        assertEquals(TEST_PASSWORD, plainPassword);
    }

    @Test
    public void testGetPlainPasswordWithNullPaths() throws Exception {
        // 临时设置系统属性
        String originalHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.toString());

        try {
            // 在临时目录创建.dbpwd子目录
            Path dbpwdDir = tempDir.resolve(".dbpwd");
            Files.createDirectories(dbpwdDir);

            // 复制文件到标准位置
            Files.copy(keyFilePath, dbpwdDir.resolve("sm4.key"));
            Files.copy(pwdFilePath, dbpwdDir.resolve("db.pwd"));

            String plainPassword = StandalonePasswordDecryptor.getPlainPassword(
                    TEST_DB_ID, null, null);

            assertEquals(TEST_PASSWORD, plainPassword);
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    public void testGetPlainPasswordWithEmptyPaths() throws Exception {
        // 空字符串应使用默认路径，如果没有默认文件会抛出异常
        // 临时设置user.home到不存在的目录，确保默认路径没有文件
        String originalHome = System.getProperty("user.home");
        System.setProperty("user.home", tempDir.resolve("nonexistent").toString());

        try {
            assertThrows(Exception.class, () -> {
                StandalonePasswordDecryptor.getPlainPassword(
                        TEST_DB_ID, "", "");
            });
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }

    @Test
    public void testGetPlainPasswordWithInvalidDbId() throws Exception {
        Exception e = assertThrows(Exception.class, () -> {
            StandalonePasswordDecryptor.getPlainPassword(
                    "invalid_db_id", keyFilePath.toString(), pwdFilePath.toString());
        });

        assertTrue(e.getMessage().contains("未找到数据库配置") ||
                   e.getMessage().contains("not found"));
    }

    @Test
    public void testGetPlainPasswordWithNullDbId() throws Exception {
        Exception e = assertThrows(Exception.class, () -> {
            StandalonePasswordDecryptor.getPlainPassword(
                    null, keyFilePath.toString(), pwdFilePath.toString());
        });

        // 预期会有某种检查失败
        assertNotNull(e);
    }

    // ==================== getConnection() 方法测试 ====================

    @Test
    public void testGetConnectionWithInvalidDbId() throws Exception {
        Exception e = assertThrows(Exception.class, () -> {
            StandalonePasswordDecryptor.getConnection(
                    "invalid_db_id", keyFilePath.toString(), pwdFilePath.toString());
        });

        assertTrue(e.getMessage().contains("未找到数据库配置") ||
                   e.getMessage().contains("not found"));
    }

    // ==================== 密钥缓存测试 ====================

    @Test
    public void testKeyCaching() throws Exception {
        // 第一次调用会加载并缓存密钥
        String password1 = StandalonePasswordDecryptor.getPlainPassword(
                TEST_DB_ID, keyFilePath.toString(), pwdFilePath.toString());

        // 第二次调用应该使用缓存的密钥
        String password2 = StandalonePasswordDecryptor.getPlainPassword(
                TEST_DB_ID, keyFilePath.toString(), pwdFilePath.toString());

        assertEquals(password1, password2);
        assertEquals(TEST_PASSWORD, password2);
    }

    @Test
    public void testKeyCachingWithDifferentDbIds() throws Exception {
        // 创建第二个数据库配置
        createMultiDbPasswordFile();

        // 测试不同数据库ID共享同一密钥
        String password1 = StandalonePasswordDecryptor.getPlainPassword(
                TEST_DB_ID, keyFilePath.toString(), pwdFilePath.toString());
        String password2 = StandalonePasswordDecryptor.getPlainPassword(
                "test_db_2", keyFilePath.toString(), pwdFilePath.toString());

        // 两个密码应该不同
        assertNotEquals(password1, password2);
    }

    // ==================== 加密格式测试 ====================

    @Test
    public void testOnlyGCMFormatSupported() throws Exception {
        // 创建使用ECB格式的密码文件
        createECBPasswordFile();

        Exception e = assertThrows(Exception.class, () -> {
            StandalonePasswordDecryptor.getPlainPassword(
                    "ecb_db", keyFilePath.toString(), pwdFilePath.toString());
        });

        assertTrue(e.getMessage().contains("不支持的加密算法") ||
                   e.getMessage().contains("Unsupported"));
    }

    @Test
    public void testEmptyPassword() throws Exception {
        createEmptyPasswordFile();

        Exception e = assertThrows(Exception.class, () -> {
            StandalonePasswordDecryptor.getPlainPassword(
                    "empty_password_db", keyFilePath.toString(), pwdFilePath.toString());
        });

        assertTrue(e.getMessage().contains("密码为空") ||
                   e.getMessage().contains("empty"));
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建H2数据库配置的密码文件
     */
    private void createH2PasswordFile() throws Exception {
        String h2Password = "h2_test_password";
        String encryptedPassword = Sm4GCMCipher.encrypt(h2Password, SM4_KEY);
        String formattedPassword = com.dbpwd.crypto.ENCFormat.formatSM4GCM(encryptedPassword);

        String json = "{\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"lastModified\": \"2026-02-11T00:00:00Z\",\n" +
                "  \"databases\": [\n" +
                "    {\n" +
                "      \"id\": \"h2_test_db\",\n" +
                "      \"name\": \"H2 Test Database\",\n" +
                "      \"type\": \"h2\",\n" +
                "      \"url\": \"jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1\",\n" +
                "      \"username\": \"sa\",\n" +
                "      \"password\": \"" + formattedPassword + "\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Files.write(pwdFilePath, json.getBytes());
    }

    /**
     * 创建多数据库配置的密码文件
     */
    private void createMultiDbPasswordFile() throws Exception {
        String password1 = "password_1";
        String password2 = "password_2";

        String encrypted1 = Sm4GCMCipher.encrypt(password1, SM4_KEY);
        String encrypted2 = Sm4GCMCipher.encrypt(password2, SM4_KEY);

        String formatted1 = com.dbpwd.crypto.ENCFormat.formatSM4GCM(encrypted1);
        String formatted2 = com.dbpwd.crypto.ENCFormat.formatSM4GCM(encrypted2);

        String json = "{\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"lastModified\": \"2026-02-11T00:00:00Z\",\n" +
                "  \"databases\": [\n" +
                "    {\n" +
                "      \"id\": \"" + TEST_DB_ID + "\",\n" +
                "      \"name\": \"Test Database 1\",\n" +
                "      \"type\": \"mysql\",\n" +
                "      \"url\": \"jdbc:mysql://localhost:3306/test_db1\",\n" +
                "      \"username\": \"user1\",\n" +
                "      \"password\": \"" + formatted1 + "\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"id\": \"test_db_2\",\n" +
                "      \"name\": \"Test Database 2\",\n" +
                "      \"type\": \"mysql\",\n" +
                "      \"url\": \"jdbc:mysql://localhost:3306/test_db2\",\n" +
                "      \"username\": \"user2\",\n" +
                "      \"password\": \"" + formatted2 + "\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Files.write(pwdFilePath, json.getBytes());
    }

    /**
     * 创建ECB格式的密码文件
     */
    private void createECBPasswordFile() throws Exception {
        // ECB格式（已弃用）
        String ecbCipherText = "9berVE9w9JCDneRc:Aw5LcgBid5Ezfv5tOh99Cqc/PRQFOFIv"; // 假的ECB密文
        String ecbFormatted = "ENC(SM4-ECB:" + ecbCipherText + ")";

        String json = "{\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"lastModified\": \"2026-02-11T00:00:00Z\",\n" +
                "  \"databases\": [\n" +
                "    {\n" +
                "      \"id\": \"ecb_db\",\n" +
                "      \"name\": \"ECB Format Database\",\n" +
                "      \"type\": \"mysql\",\n" +
                "      \"url\": \"jdbc:mysql://localhost:3306/ecb_db\",\n" +
                "      \"username\": \"ecb_user\",\n" +
                "      \"password\": \"" + ecbFormatted + "\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Files.write(pwdFilePath, json.getBytes());
    }

    /**
     * 创建空密码的密码文件
     */
    private void createEmptyPasswordFile() throws Exception {
        String json = "{\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"lastModified\": \"2026-02-11T00:00:00Z\",\n" +
                "  \"databases\": [\n" +
                "    {\n" +
                "      \"id\": \"empty_password_db\",\n" +
                "      \"name\": \"Empty Password Database\",\n" +
                "      \"type\": \"mysql\",\n" +
                "      \"url\": \"jdbc:mysql://localhost:3306/empty_db\",\n" +
                "      \"username\": \"empty_user\",\n" +
                "      \"password\": \"\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Files.write(pwdFilePath, json.getBytes());
    }
}
