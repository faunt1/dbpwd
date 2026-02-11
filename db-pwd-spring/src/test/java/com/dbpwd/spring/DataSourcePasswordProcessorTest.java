package com.dbpwd.spring;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.crypto.ENCFormat;
import com.dbpwd.crypto.Sm4GCMCipher;
import com.dbpwd.core.PasswordFileManager;
import com.dbpwd.core.Sm4KeyLoader;
import com.dbpwd.model.DatabaseEntry;
import com.dbpwd.model.PasswordFile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.sql.DataSource;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DataSourcePasswordProcessor单元测试
 *
 * @author Database Password Manager
 * @version 5.3
 */
public class DataSourcePasswordProcessorTest {

    @TempDir
    Path tempDir;

    private String keyFilePath;
    private String pwdFilePath;
    private String sm4Key;
    private String encryptedPassword;

    /**
     * 生成测试用的SM4密钥
     */
    private String generateSm4Key() {
        // 生成128位（16字节）的SM4密钥
        byte[] keyBytes = new byte[16];
        for (int i = 0; i < 16; i++) {
            keyBytes[i] = (byte) (i & 0xFF);
        }
        StringBuilder sb = new StringBuilder();
        for (byte b : keyBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @BeforeEach
    void setUp() throws Exception {
        // 创建密钥文件路径
        keyFilePath = tempDir.resolve("sm4.key").toString();
        pwdFilePath = tempDir.resolve("db.pwd").toString();

        // 生成测试密钥并写入到临时文件
        sm4Key = generateSm4Key();
        Files.write(Paths.get(keyFilePath), sm4Key.getBytes());

        // 创建测试密码文件
        String plainPassword = "testPassword123";

        // 加密密码
        encryptedPassword = Sm4GCMCipher.encrypt(plainPassword, sm4Key);
        String encFormat = "ENC(SM4-GCM:" + encryptedPassword + ")";

        // 创建数据库条目
        DatabaseEntry entry = new DatabaseEntry();
        entry.setId("testdb");
        entry.setName("Test Database");
        entry.setType("mysql");
        entry.setHost("localhost");
        entry.setPort(3306);
        entry.setDatabase("test");
        entry.setUsername("root");
        entry.setPassword(encFormat);
        entry.setUrl("jdbc:mysql://localhost:3306/test");
        entry.setUpdatedAt(String.valueOf(System.currentTimeMillis()));

        // 写入密码文件 - 使用addDatabase方法
        PasswordFileManager manager = PasswordFileManager.loadFromFileSafe(pwdFilePath);
        manager.addDatabase(entry);
        manager.save();
    }

    @AfterEach
    void tearDown() {
        // 清理临时文件由JUnit @TempDir自动处理
    }

    /**
     * 测试DataSourcePasswordProcessor基本功能
     */
    @Test
    void testDataSourcePasswordProcessorBasic() throws Exception {
        // 创建Spring容器
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        // 注册配置类
        context.register(DataSourcePasswordProcessorTestConfig.class);
        context.refresh();

        // 获取DataSource bean
        DataSource dataSource = context.getBean("encryptedDataSource", DataSource.class);

        // 验证密码已解密
        assertNotNull(dataSource);
        assertTrue(dataSource instanceof DruidDataSource);

        DruidDataSource druidDataSource = (DruidDataSource) dataSource;
        assertEquals("root", druidDataSource.getUsername());
        assertEquals("testPassword123", druidDataSource.getPassword());

        context.close();
    }

    /**
     * 测试排除功能：排除的DataSource不应被处理
     */
    @Test
    void testExcludedDataSource() throws Exception {
        // 创建Spring容器
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();

        // 注册配置类
        context.register(ExcludedDataSourceTestConfig.class);
        context.refresh();

        // 获取DataSource bean
        DataSource excludedDataSource = context.getBean("excludedDataSource", DataSource.class);

        // 验证排除的DataSource密码未被解密
        assertNotNull(excludedDataSource);
        assertTrue(excludedDataSource instanceof DruidDataSource);

        DruidDataSource druidDataSource = (DruidDataSource) excludedDataSource;
        // 密码应该是原始的占位符
        assertEquals("excluded_placeholder", druidDataSource.getPassword());

        context.close();
    }

    /**
     * 测试占位符格式解密
     */
    @Test
    void testPlaceholderDecryption() throws Exception {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();
        processor.setKeyFilePath(keyFilePath);
        processor.setPwdFilePath(pwdFilePath);

        // 创建DataSource并设置ENC格式密码
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mysql://localhost:3306/test");
        dataSource.setUsername("root");
        dataSource.setPassword("ENC(SM4-GCM:" + encryptedPassword + ")");

        // 处理DataSource
        Object processed = processor.postProcessAfterInitialization(dataSource, "dataSource");

        // 验证密码已解密
        assertNotNull(processed);
        DruidDataSource processedDataSource = (DruidDataSource) processed;
        assertEquals("testPassword123", processedDataSource.getPassword());
    }

    /**
     * 测试ENC格式直接解密
     */
    @Test
    void testENCFormatDecryption() {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();
        processor.setKeyFilePath(keyFilePath);

        // 创建DataSource并设置ENC格式的密码
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mysql://localhost:3306/test");
        dataSource.setUsername("root");
        dataSource.setPassword("ENC(SM4-GCM:" + encryptedPassword + ")");

        // 处理DataSource
        Object processed = processor.postProcessAfterInitialization(dataSource, "dataSource");

        // 验证密码已解密
        assertNotNull(processed);
        DruidDataSource processedDataSource = (DruidDataSource) processed;
        assertEquals("testPassword123", processedDataSource.getPassword());
    }

    /**
     * 测试设置自定义排除列表
     */
    @Test
    void testSetExcludedBeanNames() {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();

        Set<String> excludedNames = new HashSet<>();
        excludedNames.add("dataSource1");
        excludedNames.add("dataSource2");

        processor.setExcludedBeanNames(excludedNames);

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mysql://localhost:3306/test");
        dataSource.setUsername("root");
        dataSource.setPassword("testPassword123");

        // 处理排除的DataSource
        Object processed1 = processor.postProcessAfterInitialization(dataSource, "dataSource1");

        // 验证未处理
        DruidDataSource processedDataSource1 = (DruidDataSource) processed1;
        assertEquals("testPassword123", processedDataSource1.getPassword());
    }

    /**
     * 测试非DataSource bean不被处理
     */
    @Test
    void testNonDataSourceBean() {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();

        String nonDataSourceBean = "This is not a DataSource";

        // 处理非DataSource bean
        Object processed = processor.postProcessAfterInitialization(nonDataSourceBean, "stringBean");

        // 验证返回原对象
        assertSame(nonDataSourceBean, processed);
    }

    /**
     * 测试空密码或null密码跳过处理
     */
    @Test
    void testNullOrEmptyPassword() {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();
        processor.setKeyFilePath(keyFilePath);

        // 测试null密码
        DruidDataSource dataSource1 = new DruidDataSource();
        dataSource1.setUrl("jdbc:mysql://localhost:3306/test");
        dataSource1.setUsername("root");
        dataSource1.setPassword(null);

        Object processed1 = processor.postProcessAfterInitialization(dataSource1, "dataSource1");
        assertSame(dataSource1, processed1);

        // 测试空密码
        DruidDataSource dataSource2 = new DruidDataSource();
        dataSource2.setUrl("jdbc:mysql://localhost:3306/test");
        dataSource2.setUsername("root");
        dataSource2.setPassword("");

        Object processed2 = processor.postProcessAfterInitialization(dataSource2, "dataSource2");
        assertSame(dataSource2, processed2);
    }

    /**
     * 测试加密格式检测
     */
    @Test
    void testIsEncryptedPassword() throws Exception {
        // 测试ENC格式
        String encPassword = "ENC(SM4-GCM:abc123)";
        assertTrue(ENCFormat.isEncrypted(encPassword));

        // 测试SM4-GCM格式
        assertTrue(ENCFormat.isSM4GCMEncrypted(encPassword));

        // 测试占位符格式
        String placeholder = "testdb";
        assertFalse(ENCFormat.isEncrypted(placeholder));

        // 测试空字符串
        String empty = "";
        assertFalse(ENCFormat.isEncrypted(empty));

        // 测试null
        assertFalse(ENCFormat.isEncrypted(null));
    }

}
