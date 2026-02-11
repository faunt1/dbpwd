package com.dbpwd.druid;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.crypto.Sm4GCMCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DruidPasswordCallback单元测试
 *
 * @author Database Password Manager
 * @version 5.3
 */
public class DruidPasswordCallbackTest {

    private Path keyFilePath;
    private Path pwdFilePath;

    private static final String SM4_KEY = "0123456789abcdef0123456789abcdef";
    private static final String TEST_DB_ID = "test_db";
    private static final String TEST_PASSWORD = "test_password_123";

    @BeforeEach
    public void setUp() throws Exception {
        // 创建临时目录
        Path tempDir = Files.createTempDirectory("druid-test");
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

    // ==================== 构造函数测试 ====================

    @Test
    public void testConstructorWithPlaceholder() {
        DruidPasswordCallback callback = new DruidPasswordCallback(TEST_DB_ID);

        assertNotNull(callback);
        assertEquals(TEST_DB_ID, callback.getDbPlaceholder());
    }

    @Test
    public void testConstructorWithCustomPaths() {
        DruidPasswordCallback callback = new DruidPasswordCallback(
                TEST_DB_ID,
                keyFilePath.toString(),
                pwdFilePath.toString());

        assertNotNull(callback);
        assertEquals(TEST_DB_ID, callback.getDbPlaceholder());
    }

    // ==================== 密码解密测试 ====================

    @Test
    public void testPasswordDecryption() throws Exception {
        DruidPasswordCallback callback = new DruidPasswordCallback(
                TEST_DB_ID, keyFilePath.toString(), pwdFilePath.toString());

        String plainPassword = callback.decryptPassword();

        assertEquals(TEST_PASSWORD, plainPassword);
    }

    @Test
    public void testPasswordDecryptionWithInvalidDbId() {
        DruidPasswordCallback callback = new DruidPasswordCallback(
                "invalid_db_id", keyFilePath.toString(), pwdFilePath.toString());

        assertThrows(IllegalArgumentException.class, callback::decryptPassword);
    }

    @Test
    public void testPasswordDecryptionWithECBFormat() throws Exception {
        // 创建使用ECB格式的密码文件
        createECBPasswordFile();

        DruidPasswordCallback callback = new DruidPasswordCallback(
                "ecb_db", keyFilePath.toString(), pwdFilePath.toString());

        assertThrows(IllegalArgumentException.class, callback::decryptPassword);
    }

    @Test
    public void testEmptyPassword() throws Exception {
        createEmptyPasswordFile();

        DruidPasswordCallback callback = new DruidPasswordCallback(
                "empty_password_db", keyFilePath.toString(), pwdFilePath.toString());

        assertThrows(IllegalArgumentException.class, callback::decryptPassword);
    }

    // ==================== 静态解密方法测试 ====================

    @Test
    public void testStaticDecryptPassword() throws Exception {
        String plainPassword = DruidPasswordCallback.decryptPassword(
                TEST_DB_ID, keyFilePath.toString(), pwdFilePath.toString());

        assertEquals(TEST_PASSWORD, plainPassword);
    }

    @Test
    public void testStaticDecryptPasswordWithSingleParam() throws Exception {
        DruidPasswordCallback callback = new DruidPasswordCallback(
                TEST_DB_ID, keyFilePath.toString(), pwdFilePath.toString());

        // 使用占位符
        String plainPassword = callback.decryptPassword();

        assertEquals(TEST_PASSWORD, plainPassword);
    }

    // ==================== createDataSource工厂方法测试 ====================

    @Test
    public void testCreateDataSourceWithAutoDecrypt() throws Exception {
        DruidDataSource dataSource = DruidPasswordCallback.createDataSource(
                TEST_DB_ID,
                keyFilePath.toString(),
                pwdFilePath.toString(),
                true);

        assertNotNull(dataSource);
        assertEquals("jdbc:mysql://localhost:3306/test_db", dataSource.getUrl());
        assertEquals("test_user", dataSource.getUsername());
        // 密码已解密
        assertEquals(TEST_PASSWORD, dataSource.getPassword());
    }

    @Test
    public void testCreateDataSourceWithoutAutoDecrypt() throws Exception {
        DruidDataSource dataSource = DruidPasswordCallback.createDataSource(
                TEST_DB_ID,
                keyFilePath.toString(),
                pwdFilePath.toString(),
                false);

        assertNotNull(dataSource);
        assertEquals("jdbc:mysql://localhost:3306/test_db", dataSource.getUrl());
        assertEquals("test_user", dataSource.getUsername());
        // 密码为null（未解密）
        assertEquals(null, dataSource.getPassword());
    }

    @Test
    public void testCreateDataSourceWithUrlOverride() throws Exception {
        DruidDataSource dataSource = DruidPasswordCallback.createDataSource(
                "test_db",
                "jdbc:h2:mem:overridden",
                keyFilePath.toString(),
                pwdFilePath.toString(),
                true);

        assertNotNull(dataSource);
        assertEquals("jdbc:h2:mem:overridden", dataSource.getUrl());  // URL被覆盖
        assertEquals("test_user", dataSource.getUsername());  // 用户名保留
        assertEquals(TEST_PASSWORD, dataSource.getPassword());  // 密码已解密
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建ECB格式的密码文件
     */
    private void createECBPasswordFile() throws Exception {
        String ecbCipherText = "9berVE9w9JCDneRc:Aw5LcgBid5Ezfv5tOh99Cqc/PRQFOFIv";
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
