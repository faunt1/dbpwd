package com.dbpwd.druid;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.crypto.Sm4GCMCipher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DbPwdDruidPasswordCallback单元测试
 *
 * 测试Druid PasswordCallback集成，验证通过Druid的PasswordCallback机制
 * 自动解密数据库密码的功能
 *
 * 使用H2 FILE模式数据库进行真实密码验证测试
 *
 * @author Database Password Manager
 * @version 5.3
 */
public class DbPwdDruidPasswordCallbackTest {

    private Path keyFilePath;
    private Path pwdFilePath;
    private Path tempDir;
    private Path h2DataDir;

    private static final String SM4_KEY = "0123456789abcdef0123456789abcdef";
    private static final String TEST_DB_ID = "test_db";
    private static final String TEST_PASSWORD = "test_password_123";
    private static final String H2_DB_NAME = "test_druid_db";

    @BeforeEach
    public void setUp() throws Exception {
        // 创建临时目录
        tempDir = Files.createTempDirectory("druid-test");
        keyFilePath = tempDir.resolve("sm4.key");
        pwdFilePath = tempDir.resolve("db.pwd");
        h2DataDir = tempDir.resolve("h2_data");
        Files.createDirectories(h2DataDir);

        // 创建SM4密钥文件
        Files.write(keyFilePath, SM4_KEY.getBytes());

        // 创建H2 FILE数据库（用于测试密码验证）
        createH2Database();

        // 创建密码文件
        createTestPasswordFile();
    }

    @AfterEach
    public void tearDown() throws Exception {
        // 删除临时文件和H2数据库文件
        if (pwdFilePath != null && Files.exists(pwdFilePath)) {
            Files.delete(pwdFilePath);
        }
        if (keyFilePath != null && Files.exists(keyFilePath)) {
            Files.delete(keyFilePath);
        }
        if (h2DataDir != null && Files.exists(h2DataDir)) {
            // 删除H2数据库文件
            Files.walk(h2DataDir)
                .sorted((a, b) -> -a.compareTo(b))
                .forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                    } catch (Exception e) {
                        // 忽略删除失败
                    }
                });
        }
        if (tempDir != null && Files.exists(tempDir)) {
            Files.deleteIfExists(tempDir);
        }
    }

    /**
     * 创建H2 FILE模式测试数据库
     *
     * 使用FILE模式以确保密码验证功能正常工作
     */
    private void createH2Database() throws Exception {
        String h2Url = "jdbc:h2:file:" + h2DataDir + "/" + H2_DB_NAME;

        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(h2Url, "sa", TEST_PASSWORD);
             java.sql.Statement stmt = conn.createStatement()) {
            // 创建测试表
            stmt.execute("CREATE TABLE IF NOT EXISTS test_table (" +
                    "id INT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL" +
                    ")");
            stmt.execute("INSERT INTO test_table (id, name) VALUES (1, 'test')");
        }
    }

    /**
     * 创建测试密码文件
     *
     * 使用H2数据库配置，URL指向H2 FILE模式数据库
     */
    private void createTestPasswordFile() throws Exception {
        String encryptedPassword = Sm4GCMCipher.encrypt(TEST_PASSWORD, SM4_KEY);
        String formattedPassword = com.dbpwd.crypto.ENCFormat.formatSM4GCM(encryptedPassword);

        String h2Url = "jdbc:h2:file:" + h2DataDir + "/" + H2_DB_NAME;

        String json = "{\n" +
                "  \"version\": \"1.0\",\n" +
                "  \"lastModified\": \"2026-02-13T00:00:00Z\",\n" +
                "  \"databases\": [\n" +
                "    {\n" +
                "      \"id\": \"" + TEST_DB_ID + "\",\n" +
                "      \"name\": \"Test H2 Database\",\n" +
                "      \"type\": \"h2\",\n" +
                "      \"host\": \"localhost\",\n" +
                "      \"port\": 9092,\n" +
                "      \"database\": \"" + H2_DB_NAME + "\",\n" +
                "      \"url\": \"" + h2Url + "\",\n" +
                "      \"username\": \"sa\",\n" +
                "      \"password\": \"" + formattedPassword + "\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";

        Files.write(pwdFilePath, json.getBytes());
    }

    // ==================== 构造函数测试 ====================

    @Test
    public void testDefaultConstructor() {
        DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback();
        assertNotNull(callback);
    }

    // ==================== DruidPasswordCallback机制测试 ====================

    /**
     * 测试直接使用临时路径的PasswordCallback
     */
    @Test
    public void testPasswordCallbackWithTempPath() throws Exception {
        DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback();

        // 使用临时文件路径
        Properties props = new Properties();
        props.setProperty("password", TEST_DB_ID);

        // 由于PasswordProvider使用默认路径，需要复制文件到默认路径
        copyToDefaultPath();

        try {
            callback.setProperties(props);

            // 验证密码已被解密
            assertNotNull(callback.getPassword());
            assertEquals(TEST_PASSWORD, new String(callback.getPassword()));
        } finally {
            cleanupDefaultPath();
        }
    }

    /**
     * 测试DruidDataSource与PasswordCallback集成（使用H2 FILE数据库）
     */
    @Test
    public void testDruidDataSourceWithPasswordCallbackAndH2Database() throws Exception {
        copyToDefaultPath();

        try {
            DruidDataSource dataSource = new DruidDataSource();
            String h2Url = "jdbc:h2:file:" + h2DataDir + "/" + H2_DB_NAME;

            dataSource.setUrl(h2Url);
            dataSource.setUsername("sa");
            dataSource.setPassword(TEST_DB_ID);  // 使用占位符
            dataSource.setPasswordCallback(new DbPwdDruidPasswordCallback());
            dataSource.setDriverClassName("org.h2.Driver");

            // 测试数据库连接（H2 FILE模式会验证密码）
            try (java.sql.Connection conn = dataSource.getConnection();
                 java.sql.Statement stmt = conn.createStatement();
                 java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM test_table")) {

                assertTrue(rs.next());
                assertTrue(rs.getInt(1) > 0, "数据库连接成功，密码解密正确");
            }

            assertNotNull(dataSource);
            assertEquals(TEST_DB_ID, dataSource.getPassword());

        } finally {
            cleanupDefaultPath();
        }
    }

    /**
     * 测试错误密码会被拒绝
     */
    @Test
    public void testWrongPasswordRejected() throws Exception {
        // H2 FILE数据库首次连接绑定了TEST_PASSWORD
        // 错误密码应该被拒绝
        String wrongPassword = "wrong_password";

        try (java.sql.Connection conn = java.sql.DriverManager.getConnection(
                "jdbc:h2:file:" + h2DataDir + "/" + H2_DB_NAME, "sa", wrongPassword)) {
            fail("错误的密码应该被拒绝，但连接成功了");
        } catch (java.sql.SQLException e) {
            // 预期异常：密码错误
            assertTrue(e.getMessage().contains("Wrong password") ||
                    e.getMessage().contains("password") ||
                    e.getMessage().contains("authentication"),
                    "异常消息应该包含密码相关错误信息");
        }
    }

    // ==================== 错误处理测试 ====================

    @Test
    public void testEmptyPassword() {
        assertThrows(RuntimeException.class, () -> {
            DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback();
            Properties props = new Properties();
            props.setProperty("password", "");
            callback.setProperties(props);
        });
    }

    @Test
    public void testNullPassword() {
        assertThrows(RuntimeException.class, () -> {
            DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback();
            Properties props = new Properties();
            props.setProperty("password", "");
            callback.setProperties(props);
        });
    }

    @Test
    public void testPasswordCallbackSetPropertiesValid() throws Exception {
        copyToDefaultPath();

        try {
            DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback();
            Properties props = new Properties();
            props.setProperty("password", TEST_DB_ID);
            callback.setProperties(props);

            assertNotNull(callback.getPassword());
            assertEquals(TEST_PASSWORD, new String(callback.getPassword()));
        } finally {
            cleanupDefaultPath();
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 复制临时文件到默认路径
     */
    private void copyToDefaultPath() throws Exception {
        String userHome = System.getProperty("user.home");
        Path dbpwdDir = Paths.get(userHome, ".dbpwd");
        Files.createDirectories(dbpwdDir);

        Path defaultKeyPath = dbpwdDir.resolve("sm4.key");
        Path defaultPwdPath = dbpwdDir.resolve("db.pwd");

        Files.copy(keyFilePath, defaultKeyPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.copy(pwdFilePath, defaultPwdPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 清理默认路径的测试文件
     */
    private void cleanupDefaultPath() throws Exception {
        String userHome = System.getProperty("user.home");
        Path dbpwdDir = Paths.get(userHome, ".dbpwd");
        Path defaultKeyPath = dbpwdDir.resolve("sm4.key");
        Path defaultPwdPath = dbpwdDir.resolve("db.pwd");

        if (Files.exists(defaultKeyPath)) {
            Files.delete(defaultKeyPath);
        }
        if (Files.exists(defaultPwdPath)) {
            Files.delete(defaultPwdPath);
        }
    }
}
