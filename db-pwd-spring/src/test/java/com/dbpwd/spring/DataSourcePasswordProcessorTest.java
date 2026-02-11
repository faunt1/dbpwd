package com.dbpwd.spring;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.crypto.Sm4GCMCipher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DataSourcePasswordProcessor单元测试
 *
 * @author Database Password Manager
 * @version 5.3
 */
public class DataSourcePasswordProcessorTest {

    private Path keyFilePath;
    private Path pwdFilePath;

    private static final String SM4_KEY = "0123456789abcdef0123456789abcdef";
    private static final String TEST_DB_ID = "test_db";
    private static final String TEST_PASSWORD = "test_password_123";

    @BeforeEach
    public void setUp() throws Exception {
        // 创建临时目录
        Path tempDir = Files.createTempDirectory("spring-test");
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
                "      \"id\": \"" + TEST_DB_ID + "\",
\n" +
                "      \"name\": \"Test Database\",\n" +
                "      \"type\": \"mysql\",\n" +
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
    public void testConstructor() {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();
        assertNotNull(processor);
    }

    // ==================== 后处理测试 ====================

    @Test
    public void testPostProcessNonDataSource() {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();

        Object bean = new Object();
        String beanName = "notDataSource";

        Object result = processor.postProcessAfterInitialization(bean, beanName);

        assertSame(bean, result);
    }

    @Test
    public void testProcessDataSourceWithEncryptedPasswordWithCustomPaths() throws Exception {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();
        processor.setKeyFilePath(keyFilePath.toString());
        processor.setPwdFilePath(pwdFilePath.toString());

        String encryptedPassword = Sm4GCMCipher.encrypt(TEST_PASSWORD, SM4_KEY);
        String formattedPassword = com.dbpwd.crypto.ENCFormat.formatSM4GCM(encryptedPassword);

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mysql://localhost:3306/test_db");
        dataSource.setUsername("test_user");
        dataSource.setPassword(formattedPassword);

        Object result = processor.postProcessAfterInitialization(dataSource, "dataSource");

        assertSame(dataSource, result);
        assertEquals(TEST_PASSWORD, dataSource.getPassword());
    }

    @Test
    public void testProcessDataSourceWithEncryptedPasswordGCM() throws Exception {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();
        processor.setKeyFilePath(keyFilePath.toString());
        processor.setPwdFilePath(pwdFilePath.toString());

        String encryptedPassword = Sm4GCMCipher.encrypt(TEST_PASSWORD, SM4_KEY);
        String formattedPassword = com.dbpwd.crypto.ENCFormat.formatSM4GCM(encryptedPassword);

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mysql://localhost:3306/test_db");
        dataSource.setUsername("test_user");
        dataSource.setPassword(formattedPassword);

        Object result = processor.postProcessAfterInitialization(dataSource, "dataSource");

        assertSame(dataSource, result);
        assertEquals(TEST_PASSWORD, dataSource.getPassword());
    }

    @Test
    public void testProcessDataSourceWithECBFormat() throws Exception {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();
        processor.setKeyFilePath(keyFilePath.toString());
        processor.setPwdFilePath(pwdFilePath.toString());

        String ecbCipherText = "9berVE9w9JCDneRc:Aw5LcgBid5Ezfv5tOh99Cqc";
        String ecbFormatted = "ENC(SM4-ECB:" + ecbCipherText + ")";

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mysql://localhost:3306/test_db");
        dataSource.setUsername("test_user");
        dataSource.setPassword(ecbFormatted);

        Object result = processor.postProcessAfterInitialization(dataSource, "dataSource");

        assertSame(dataSource, result);
        // ECB格式不被支持，密码保持原值
        assertEquals(ecbFormatted, dataSource.getPassword());
    }

    @Test
    public void testProcessDataSourceWithExcludedBeanName() throws Exception {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();
        processor.addExcludedBeanName("excluded");
        processor.setKeyFilePath(keyFilePath.toString());
        processor.setPwdFilePath(pwdFilePath.toString());

        String encryptedPassword = Sm4GCMCipher.encrypt(TEST_PASSWORD, SM4_KEY);
        String formattedPassword = com.dbpwd.crypto.ENCFormat.formatSM4GCM(encryptedPassword);

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setPassword(formattedPassword);

        Object result = processor.postProcessAfterInitialization(dataSource, "excluded");

        assertSame(dataSource, result);
        // 被排除的bean密码不处理
        assertEquals(formattedPassword, dataSource.getPassword());
    }

    @Test
    public void testProcessDataSourceWithNullOrEmptyPassword() {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();
        processor.setKeyFilePath(keyFilePath.toString());
        processor.setPwdFilePath(pwdFilePath.toString());

        // 测试空密码
        DruidDataSource ds1 = new DruidDataSource();
        ds1.setPassword(null);

        Object result1 = processor.postProcessAfterInitialization(ds1, "ds1");
        assertSame(ds1, result1);
        assertNull(ds1.getPassword());

        // 测试空字符串密码
        DruidDataSource ds2 = new DruidDataSource();
        ds2.setPassword("");

        Object result2 = processor.postProcessAfterInitialization(ds2, "ds2");
        assertSame(ds2, result2);
        assertEquals("", ds2.getPassword());
    }

    // ==================== 私有方法测试 ====================

    @Test
    public void testGetPlainPasswordEncryptedFormat() throws Exception {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();
        processor.setKeyFilePath(keyFilePath.toString());
        processor.setPwdFilePath(pwdFilePath.toString());

        String encryptedPassword = Sm4GCMCipher.encrypt(TEST_PASSWORD, SM4_KEY);
        String formattedPassword = com.dbpwd.crypto.ENCFormat.formatSM4GCM(encryptedPassword);

        // 测试加密格式密码解密
        java.lang.reflect.Method method = DataSourcePasswordProcessor.class.getDeclaredMethod(
                "decryptGCMPassword", String.class, String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(processor, formattedPassword, SM4_KEY);

        assertEquals(TEST_PASSWORD, result);
    }

    @Test
    public void testGetPlainPasswordFromPlaceholder() throws Exception {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();
        processor.setKeyFilePath(keyFilePath.toString());
        processor.setPwdFilePath(pwdFilePath.toString());

        // 测试占位符解密
        java.lang.reflect.Method method = DataSourcePasswordProcessor.class.getDeclaredMethod(
                "getPlainPassword", String.class);
        method.setAccessible(true);
        String result = (String) method.invoke(processor, TEST_DB_ID);

        assertEquals(TEST_PASSWORD, result);
    }

    @Test
    public void testGetPlainPasswordWithInvalidPlaceholder() throws Exception {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();
        processor.setKeyFilePath(keyFilePath.toString());
        processor.setPwdFilePath(pwdFilePath.toString());

        java.lang.reflect.Method method = DataSourcePasswordProcessor.class.getDeclaredMethod(
                "getPlainPassword", String.class);
        method.setAccessible(true);

        // 测试无效占位符
        assertThrows(Exception.class, () -> {
            method.invoke(processor, "invalid_placeholder");
        });
    }
}
