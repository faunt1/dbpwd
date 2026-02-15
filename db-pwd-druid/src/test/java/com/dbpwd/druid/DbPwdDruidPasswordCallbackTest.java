package com.dbpwd.druid;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.core.Sm4KeyLoader;
import com.dbpwd.crypto.Sm4GCMCipher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DbPwdDruidPasswordCallback单元测试
 *
 * 测试Druid PasswordCallback集成，验证通过Druid的PasswordCallback机制
 * 自动解密数据库密码的功能
 *
 * 设计原则：
 * - 单元测试使用临时目录（@TempDir），自包含、自解释
 * - 通过 keyFilePath 和 pwdFilePath 属性指定自定义路径
 * - 不依赖用户主目录下的 ~/.dbpwd/ 配置
 *
 * 使用H2 FILE模式数据库进行真实密码验证测试
 *
 * @author Database Password Manager
 * @version 6.1
 */
public class DbPwdDruidPasswordCallbackTest {

    @TempDir
    Path tempDir;

    private Path keyFilePath;
    private Path pwdFilePath;
    private Path h2DataDir;

    private static final String SM4_KEY = "0123456789abcdef0123456789abcdef";
    private static final String TEST_DB_ID = "test_db";  // 数据库ID（不带@符号）
    private static final String TEST_PLACEHOLDER = "@test_db@";  // 占位符格式（带@符号）
    private static final String TEST_PASSWORD = "test_password_123";
    private static final String H2_DB_NAME = "test_druid_db";

    @BeforeEach
    public void setUp() throws Exception {
        // 使用临时目录下的路径
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
        // @TempDir 会自动清理临时目录，无需手动删除
    }

    /**
     * 创建H2 FILE模式测试数据库
     */
    private void createH2Database() throws Exception {
        // 添加 AUTO_SERVER=TRUE 避免锁问题
        String h2Url = "jdbc:h2:file:" + h2DataDir + "/" + H2_DB_NAME + ";AUTO_SERVER=TRUE;DB_CLOSE_ON_EXIT=TRUE";

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
     */
    private void createTestPasswordFile() throws Exception {
        String encryptedPassword = Sm4GCMCipher.encrypt(TEST_PASSWORD, SM4_KEY);
        String formattedPassword = com.dbpwd.crypto.ENCFormat.formatSM4GCM(encryptedPassword);

        // 添加 AUTO_SERVER=TRUE 避免锁问题
        String h2Url = "jdbc:h2:file:" + h2DataDir.toString().replace("\\", "/") + "/" + H2_DB_NAME + ";AUTO_SERVER=TRUE;DB_CLOSE_ON_EXIT=TRUE";

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

    @Test
    public void testConstructorWithProperties() {
        Properties props = new Properties();
        props.setProperty("keyFilePath", "/tmp/test/sm4.key");
        props.setProperty("pwdFilePath", "/tmp/test/db.pwd");

        DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback(props);
        assertNotNull(callback);
    }

    // ==================== DruidPasswordCallback机制测试 ====================

    /**
     * 测试使用自定义路径的PasswordCallback（单元测试场景）
     */
    @Test
    public void testPasswordCallbackWithCustomPath() throws Exception {
        // 使用构造函数指定自定义路径
        Properties callbackProps = new Properties();
        callbackProps.setProperty("keyFilePath", keyFilePath.toString());
        callbackProps.setProperty("pwdFilePath", pwdFilePath.toString());

        DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback(callbackProps);

        Properties props = new Properties();
        props.setProperty("password", TEST_PLACEHOLDER);

        callback.setProperties(props);

        // 验证密码已被解密
        assertNotNull(callback.getPassword());
        assertEquals(TEST_PASSWORD, new String(callback.getPassword()));
    }

    /**
     * 测试使用setter设置自定义路径
     */
    @Test
    public void testPasswordCallbackWithSetterPath() throws Exception {
        DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback();

        // 使用setter设置路径
        callback.setKeyFilePath(keyFilePath.toString());
        callback.setPwdFilePath(pwdFilePath.toString());

        Properties props = new Properties();
        props.setProperty("password", TEST_PLACEHOLDER);

        callback.setProperties(props);

        // 验证密码已被解密
        assertNotNull(callback.getPassword());
        assertEquals(TEST_PASSWORD, new String(callback.getPassword()));
    }

    /**
     * 测试DruidDataSource与PasswordCallback集成（使用H2 FILE数据库）
     */
    @Test
    public void testDruidDataSourceWithPasswordCallbackAndH2Database() throws Exception {
        // 创建PasswordCallback并设置自定义路径
        Properties callbackProps = new Properties();
        callbackProps.setProperty("keyFilePath", keyFilePath.toString());
        callbackProps.setProperty("pwdFilePath", pwdFilePath.toString());

        DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback(callbackProps);

        DruidDataSource dataSource = new DruidDataSource();
        // 添加 AUTO_SERVER=TRUE 避免锁问题
        String h2Url = "jdbc:h2:file:" + h2DataDir.toString().replace("\\", "/") + "/" + H2_DB_NAME + ";AUTO_SERVER=TRUE;DB_CLOSE_ON_EXIT=TRUE";

        dataSource.setUrl(h2Url);
        dataSource.setUsername("sa");
        dataSource.setPassword(TEST_PLACEHOLDER);  // 设置占位符 @test_db@
        dataSource.setDriverClassName("org.h2.Driver");
        
        // 关键：设置 DataSource 引用到 callback
        callback.setDataSource(dataSource);
        dataSource.setPasswordCallback(callback);

        // 关键：设置验证查询，避免 testWhileIdle 错误
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestWhileIdle(false);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);

        // 关键：Druid需要显式初始化才会触发PasswordCallback
        dataSource.init();

        // 测试数据库连接（H2 FILE模式会验证密码）
        try (java.sql.Connection conn = dataSource.getConnection();
             java.sql.Statement stmt = conn.createStatement();
             java.sql.ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM test_table")) {

            assertTrue(rs.next());
            assertTrue(rs.getInt(1) > 0, "数据库连接成功，密码解密正确");
        }

        assertNotNull(dataSource);
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

    /**
     * 测试密码缓存功能
     */
    @Test
    public void testPasswordCaching() throws Exception {
        Properties callbackProps = new Properties();
        callbackProps.setProperty("keyFilePath", keyFilePath.toString());
        callbackProps.setProperty("pwdFilePath", pwdFilePath.toString());

        DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback(callbackProps);

        Properties props = new Properties();
        props.setProperty("password", TEST_PLACEHOLDER);

        // 第一次调用
        callback.setProperties(props);
        char[] password1 = callback.getPassword();

        // 第二次调用（应该使用缓存）
        callback.setProperties(props);
        char[] password2 = callback.getPassword();

        // 验证两次结果一致
        assertEquals(new String(password1), new String(password2));
        assertEquals(TEST_PASSWORD, new String(password2));
    }
}
