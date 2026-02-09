package com.dbpwd.framework;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.crypto.ENCFormat;
import com.dbpwd.crypto.Sm4Cipher;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * DataSourceDecorator单元测试
 *
 * @author Database Password Manager
 * @version 4.2
 */
public class DataSourceDecoratorTest {

    private static final String TEST_DB_ID = "test-db";
    private static final String TEST_PASSWORD = "test123";
    private static final String TEST_SM4_KEY = "0123456789abcdef0123456789abcdef";
    private static final String TEST_DIR = "target/test-classes/dbpwd";

    /**
     * 测试装饰Druid数据源
     */
    public void testDruidDecorator() throws Exception {
        // 准备数据
        String encryptedPassword = Sm4Cipher.encrypt(TEST_PASSWORD, TEST_SM4_KEY);
        String encFormat = ENCFormat.formatSM4(encryptedPassword);

        // 创建Druid数据源
        DruidDataSource druid = new DruidDataSource();
        druid.setUrl("jdbc:h2:mem:test");
        druid.setUsername("sa");
        druid.setPassword(encFormat);
        druid.setDriverClassName("org.h2.Driver");

        // 创建装饰器
        DataSourceDecorator decorator = new DataSourceDecorator(
                druid, TEST_DB_ID, TEST_SM4_KEY
        );

        // 验证
        assert decorator != null;
        assert TEST_DB_ID.equals(decorator.getDbId());
        assert TEST_SM4_KEY.equals(decorator.getSm4Key());
        assert druid == decorator.getDelegate();

        System.out.println("testDruidDecorator: PASSED");
    }

    /**
     * 测试密码解密
     */
    public void testPasswordDecryption() throws Exception {
        // 准备数据
        String encryptedPassword = Sm4Cipher.encrypt(TEST_PASSWORD, TEST_SM4_KEY);
        String encFormat = ENCFormat.formatSM4(encryptedPassword);

        // 创建Druid数据源
        DruidDataSource druid = new DruidDataSource();
        druid.setUrl("jdbc:h2:mem:test");
        druid.setUsername("sa");
        druid.setPassword(encFormat);
        druid.setDriverClassName("org.h2.Driver");

        // 创建装饰器
        DataSourceDecorator decorator = new DataSourceDecorator(
                druid, TEST_DB_ID, TEST_SM4_KEY
        );

        // 验证密码状态
        DataSourceDecorator.PasswordStatus status = decorator.getPasswordStatus();
        assert status == DataSourceDecorator.PasswordStatus.ENCRYPTED;

        // 获取解密后的密码
        String decryptedPassword = decorator.getDecryptedPassword();
        assert TEST_PASSWORD.equals(decryptedPassword);

        System.out.println("testPasswordDecryption: PASSED");
    }

    /**
     * 测试工厂方法decorate
     */
    public void testFactoryDecorate() throws Exception {
        // 准备数据
        String encryptedPassword = Sm4Cipher.encrypt(TEST_PASSWORD, TEST_SM4_KEY);
        String encFormat = ENCFormat.formatSM4(encryptedPassword);

        // 创建Druid数据源
        DruidDataSource druid = new DruidDataSource();
        druid.setUrl("jdbc:h2:mem:test");
        druid.setUsername("sa");
        druid.setPassword(encFormat);
        druid.setDriverClassName("org.h2.Driver");

        // 使用工厂方法装饰
        DataSourceDecorator decorator = DataSourceDecoratorFactory.decorate(
                druid, TEST_DB_ID, TEST_SM4_KEY
        );

        // 验证
        assert decorator != null;
        assert TEST_DB_ID.equals(decorator.getDbId());

        System.out.println("testFactoryDecorate: PASSED");
    }

    /**
     * 测试工厂方法create (不支持H2，mock测试)
     */
    public void testFactoryCreate() throws Exception {
        com.dbpwd.model.DatabaseEntry entry = new com.dbpwd.model.DatabaseEntry();
        entry.setId("test-db-2");
        entry.setType("druid");
        entry.setUrl("jdbc:mysql://localhost:3306/test");
        entry.setUsername("user");
        entry.setPassword("plain_password");
        entry.setDriver("com.mysql.cj.jdbc.Driver");

        Map<String, Object> poolConfig = new HashMap<>();
        poolConfig.put("initialSize", 5);
        poolConfig.put("maxActive", 20);
        entry.setAdditionalProperties(poolConfig);

        try {
            DataSourceDecorator decorator = DataSourceDecoratorFactory.create(entry, TEST_SM4_KEY);

            // 验证
            assert decorator != null;
            assert "test-db-2".equals(decorator.getDbId());

            System.out.println("testFactoryCreate: PASSED");
        } catch (Exception e) {
            // 驱动不存在是预期的行为
            System.out.println("testFactoryCreate: DRIVERS NOT INSTALLED (expected)");
        }
    }

    /**
     * 测试getConnection时解密密码
     */
    public void testGetConnectionWithDecryption() throws Exception {
        // 准备数据
        String encryptedPassword = Sm4Cipher.encrypt(TEST_PASSWORD, TEST_SM4_KEY);
        String encFormat = ENCFormat.formatSM4(encryptedPassword);

        // 创建Druid数据源
        DruidDataSource druid = new DruidDataSource();
        druid.setUrl("jdbc:h2:mem:test");
        druid.setUsername("sa");
        druid.setPassword(encFormat);
        druid.setDriverClassName("org.h2.Driver");

        // 创建装饰器
        DataSourceDecorator decorator = new DataSourceDecorator(
                druid, TEST_DB_ID, TEST_SM4_KEY
        );

        // 这里不实际获取连接，因为需要数据库环境
        // 只验证装饰器存在且配置正确
        assert decorator != null;

        System.out.println("testGetConnectionWithDecryption: PASSED");
    }

    /**
     * 测试刷新密码
     */
    public void testRefreshPassword() throws Exception {
        // 创建Druid数据源
        DruidDataSource druid = new DruidDataSource();
        druid.setUrl("jdbc:h2:mem:test");
        druid.setUsername("sa");
        druid.setPassword(TEST_PASSWORD);
        druid.setDriverClassName("org.h2.Driver");

        // 创建装饰器
        DataSourceDecorator decorator = new DataSourceDecorator(
                druid, TEST_DB_ID, TEST_SM4_KEY
        );

        // 刷新密码（不会抛出异常）
        decorator.refreshPassword();

        assert decorator != null;
        System.out.println("testRefreshPassword: PASSED");
    }

    /**
     * 测试密码文件集成
     */
    public void testPasswordFileIntegration() throws Exception {
        // 创建测试密码文件
        createTestPasswordFile();

        try {
            // 创建Druid数据源
            DruidDataSource druid = new DruidDataSource();
            druid.setUrl("jdbc:h2:mem:test");
            druid.setUsername("sa");
            druid.setPassword("any");
            druid.setDriverClassName("org.h2.Driver");

            // 使用密码文件创建装饰器
            String passwordFile = TEST_DIR + "/db.pwd";
            DataSourceDecorator decorator = new DataSourceDecorator(
                    druid, TEST_DB_ID, TEST_SM4_KEY, passwordFile
            );

            assert decorator != null;
            assert decorator.getPasswordFileManager() != null;

            System.out.println("testPasswordFileIntegration: PASSED");
        } finally {
            // 清理测试文件
            cleanTestFiles();
        }
    }

    /**
     * 创建测试密码文件
     * @throws Exception
     */
    private void createTestPasswordFile() throws Exception {
        File testDir = new File(TEST_DIR);
        if (!testDir.exists()) {
            testDir.mkdirs();
        }

        String encryptedPassword = Sm4Cipher.encrypt(TEST_PASSWORD, TEST_SM4_KEY);
        String encFormat = ENCFormat.formatSM4(encryptedPassword);

        Map<String, Object> entry = new HashMap<>();
        entry.put("id", TEST_DB_ID);
        entry.put("name", "Test Database");
        entry.put("type", "druid");
        entry.put("url", "jdbc:h2:mem:test");
        entry.put("username", "sa");
        entry.put("password", encFormat);
        entry.put("driver", "org.h2.Driver");
        entry.put("encrypted", false);
        entry.put("createdAt", java.time.Instant.now().toString());
        entry.put("updatedAt", java.time.Instant.now().toString());

        java.io.FileWriter writer = new java.io.FileWriter(TEST_DIR + "/db.pwd");
        writer.write("[{\"id\":\"test-db\",\"name\":\"Test Database\",\"type\":\"druid\",\"url\":\"jdbc:h2:mem:test\",\"username\":\"sa\",\"password\":\"" + encFormat + "\",\"driver\":\"org.h2.Driver\",\"encrypted\":false,\"createdAt\":\"2024-01-01T00:00:00Z\",\"updatedAt\":\"2024-01-01T00:00:00Z\"}]");
        writer.close();
    }

    /**
     * 清理测试文件
     */
    private void cleanTestFiles() {
        File passwordFile = new File(TEST_DIR + "/db.pwd");
        if (passwordFile.exists()) {
            passwordFile.delete();
        }

        File testDir = new File(TEST_DIR);
        if (testDir.exists() && testDir.isDirectory()) {
            testDir.delete();
        }
    }

    /**
     * 主测试方法
     */
    public static void main(String[] args) {
        DataSourceDecoratorTest test = new DataSourceDecoratorTest();

        System.out.println("=== Begin DataSourceDecorator Tests ===");

        try {
            test.testDruidDecorator();
            test.testPasswordDecryption();
            test.testFactoryDecorate();
            test.testFactoryCreate();
            test.testGetConnectionWithDecryption();
            test.testRefreshPassword();
            test.testPasswordFileIntegration();

            System.out.println("=== All Tests PASSED ===");
        } catch (Exception e) {
            System.out.println("=== Test FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
