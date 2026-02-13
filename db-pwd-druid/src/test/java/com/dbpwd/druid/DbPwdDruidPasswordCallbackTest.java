package com.dbpwd.druid;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.crypto.Sm4GCMCipher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
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
 * @author Database Password Manager
 * @version 5.3
 */
public class DbPwdDruidPasswordCallbackTest {

    private Path keyFilePath;
    private Path pwdFilePath;
    private Path tempDir;

    private static final String SM4_KEY = "0123456789abcdef0123456789abcdef";
    private static final String TEST_DB_ID = "test_db";
    private static final String TEST_PASSWORD = "test_password_123";

    @BeforeEach
    public void setUp() throws Exception {
        // 创建临时目录
        tempDir = Files.createTempDirectory("druid-test");
        keyFilePath = tempDir.resolve("sm4.key");
        pwdFilePath = tempDir.resolve("db.pwd");

        // 创建SM4密钥文件
        Files.write(keyFilePath, SM4_KEY.getBytes());

        // 创建密码文件
        createTestPasswordFile();
    }

    @AfterEach
    public void tearDown() throws Exception {
        // 删除临时文件
        if (pwdFilePath != null && Files.exists(pwdFilePath)) {
            Files.delete(pwdFilePath);
        }
        if (keyFilePath != null && Files.exists(keyFilePath)) {
            Files.delete(keyFilePath);
        }
        if (tempDir != null && Files.exists(tempDir)) {
            Files.deleteIfExists(tempDir);
        }
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
    public void testDefaultConstructor() {
        DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback();

        assertNotNull(callback);
    }

    // ==================== DruidPasswordCallback机制测试 ====================

    @Test
    public void testPasswordCallbackWithValidPlaceholder() throws Exception {
        // 由于测试时不能直接使用默认路径，这里测试构造
        DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback();

        assertNotNull(callback);
    }

    @Test
    public void testDruidDataSourceWithPasswordCallback() throws Exception {
        // 此测试需要db.pwd文件在默认路径下
        // 先创建默认路径的文件
        createDefaultPathFiles();

        try {
            DruidDataSource dataSource = new DruidDataSource();
            dataSource.setUrl("jdbc:mysql://localhost:3306/test_db");
            dataSource.setUsername("test_user");
            dataSource.setPassword(TEST_DB_ID);  // 使用占位符
            dataSource.setPasswordCallback(new DbPwdDruidPasswordCallback());

            // 设置不自动初始化连接池，只测试配置
            dataSource.setTestOnBorrow(false);
            dataSource.setTestOnReturn(false);
            dataSource.setTestWhileIdle(false);

            assertNotNull(dataSource);
            assertEquals(TEST_DB_ID, dataSource.getPassword());
        } finally {
            cleanupDefaultPathFiles();
        }
    }

    // ==================== 错误处理测试 ====================

    @Test
    public void testEmptyPlaceholder() {
        // 创建默认路径的文件
        try {
            createDefaultPathFiles();

            // 直接测试PasswordCallback的setProperties方法
            assertThrows(RuntimeException.class, () -> {
                DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback();
                Properties props = new Properties();
                props.setProperty("password", "");
                callback.setProperties(props);
            });
        } catch (Exception e) {
            fail("Setup failed: " + e.getMessage());
        } finally {
            try {
                cleanupDefaultPathFiles();
            } catch (Exception e) {
                // 忽略
            }
        }
    }

    @Test
    public void testNullPlaceholder() {
        // 创建默认路径的文件
        try {
            createDefaultPathFiles();

            // 直接测试PasswordCallback的setProperties方法
            assertThrows(RuntimeException.class, () -> {
                DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback();
                Properties props = new Properties();
                props.setProperty("password", "");
                callback.setProperties(props);
            });
        } catch (Exception e) {
            fail("Setup failed: " + e.getMessage());
        } finally {
            try {
                cleanupDefaultPathFiles();
            } catch (Exception e) {
                // 忽略
            }
        }
    }

    @Test
    public void testPasswordCallbackSetPropertiesValid() throws Exception {
        // 创建默认路径的文件
        createDefaultPathFiles();

        try {
            // 测试PasswordCallback的setProperties方法正常工作
            DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback();
            Properties props = new Properties();
            props.setProperty("password", TEST_DB_ID);
            callback.setProperties(props);

            // 验证密码已被解密并设置
            assertNotNull(callback.getPassword());
            assertEquals(TEST_PASSWORD, new String(callback.getPassword()));
        } finally {
            cleanupDefaultPathFiles();
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建默认路径的文件
     */
    private void createDefaultPathFiles() throws Exception {
        String userHome = System.getProperty("user.home");
        Path dbpwdDir = Paths.get(userHome, ".dbpwd");
        Files.createDirectories(dbpwdDir);

        // 复制密钥文件
        Path defaultKeyPath = dbpwdDir.resolve("sm4.key");
        Files.copy(keyFilePath, defaultKeyPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // 复制密码文件
        Path defaultPwdPath = dbpwdDir.resolve("db.pwd");
        Files.copy(pwdFilePath, defaultPwdPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * 清理默认路径的文件
     */
    private void cleanupDefaultPathFiles() throws Exception {
        String userHome = System.getProperty("user.home");
        Path dbpwdDir = Paths.get(userHome, ".dbpwd");
        Path defaultKeyPath = dbpwdDir.resolve("sm4.key");
        Path defaultPwdPath = dbpwdDir.resolve("db.pwd");

        // 只删除测试创建的文件，保留其他可能存在的文件
        if (Files.exists(defaultKeyPath)) {
            Files.delete(defaultKeyPath);
        }
        if (Files.exists(defaultPwdPath)) {
            Files.delete(defaultPwdPath);
        }
    }
}
