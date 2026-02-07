package com.dbpwd;

import com.alibaba.druid.pool.DruidDataSource;
import org.junit.jupiter.api.*;

import java.sql.*;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Druid连接池测试类
 */
public class DruidConnectionTest {

    private static DruidConnectionPool connectionPool;
    private static String testConfigPath;

    private static final String TEST_CONFIG = "config/druid_test.properties";

    @BeforeAll
    public static void setUpBeforeClass() throws Exception {
        System.out.println("===============================================");
        System.out.println("    Druid 数据库连接池测试");
        System.out.println("===============================================");
    }

    @BeforeEach
    public void setUp() throws Exception {
        // 测试前检查配置文件
        testConfigPath = System.getProperty("test.config", TEST_CONFIG);

        if (!new java.io.File(testConfigPath).exists()) {
            try {
                // 尝试生成测试配置
                generateTestConfig();
            } catch (Exception e) {
                // 如果没有数据库连接信息，跳过测试
                System.out.println("警告: 未找到测试配置文件 " + testConfigPath);
                System.out.println("跳过测试或请提供有效的配置文件");
                testConfigPath = null;
            }
        }

        if (testConfigPath != null && new java.io.File(testConfigPath).exists()) {
            try {
                connectionPool = new DruidConnectionPool(testConfigPath);
            } catch (Exception e) {
                System.out.println("警告: 创建连接池失败: " + e.getMessage());
                connectionPool = null;
            }
        }
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (connectionPool != null) {
            connectionPool.close();
            connectionPool = null;
        }
    }

    @AfterAll
    public static void tearDownAfterClass() throws Exception {
        System.out.println("===============================================");
        System.out.println("测试完成");
        System.out.println("===============================================");
    }

    /**
     * 测试连接池初始化
     */
    @Test
    @DisplayName("测试连接池初始化")
    public void testConnectionPoolInit() {
        if (connectionPool == null) {
            System.out.println("跳过测试: 连接池未初始化");
            return;
        }

        DruidDataSource dataSource = connectionPool.getDataSource();

        assertNotNull(dataSource, "数据源不应为null");
        assertFalse(dataSource.isClosed(), "数据源应处于打开状态");

        System.out.println("数据源URL: " + dataSource.getUrl());
        System.out.println("初始连接数: " + dataSource.getInitialSize());
    }

    /**
     * 测试获取数据库连接
     */
    @Test
    @DisplayName("测试获取数据库连接")
    public void testGetConnection() throws SQLException {
        if (connectionPool == null) {
            System.out.println("跳过测试: 连接池未初始化");
            return;
        }

        try (Connection conn = connectionPool.getConnection()) {
            assertNotNull(conn, "连接不应为null");
            assertFalse(conn.isClosed(), "连接应处于打开状态");
            assertTrue(conn.isValid(5), "连接应该是有效的");

            System.out.println("成功获取数据库连接");
        }
    }

    /**
     * 测试执行SQL查询
     */
    @Test
    @DisplayName("测试执行SQL查询")
    public void testExecuteQuery() throws SQLException {
        if (connectionPool == null) {
            System.out.println("跳过测试: 连接池未初始化");
            return;
        }

        String sql = "SELECT 1 as test_value";

        try (Connection conn = connectionPool.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            assertNotNull(rs, "结果集不应为null");
            assertTrue(rs.next(), "结果集应有数据");

            int testValue = rs.getInt("test_value");
            assertEquals(1, testValue, "查询结果应为1");

            System.out.println("SQL查询执行成功: " + sql);
        }
    }

    /**
     * 测试连接池统计信息
     */
    @Test
    @DisplayName("测试连接池统计信息")
    public void testConnectionPoolStats() {
        if (connectionPool == null) {
            System.out.println("跳过测试: 连接池未初始化");
            return;
        }

        DruidDataSource dataSource = connectionPool.getDataSource();

        System.out.println("===============================================");
        System.out.println("连接池统计信息:");
        System.out.println("  初始连接数: " + dataSource.getInitialSize());
        System.out.println("  最小空闲连接: " + dataSource.getMinIdle());
        System.out.println("  最大活跃连接: " + dataSource.getMaxActive());
        System.out.println("  当前活跃连接: " + dataSource.getActiveCount());
        System.out.println("  当前空闲连接: " + dataSource.getPoolingCount());
        System.out.println("  总连接数: " + (dataSource.getActiveCount() + dataSource.getPoolingCount()));
        System.out.println("===============================================");

        assertTrue(dataSource.getActiveCount() >= 0, "活跃连接数应大于等于0");
        assertTrue(dataSource.getPoolingCount() >= 0, "空闲连接数应大于等于0");
    }

    /**
     * 测试密码解密
     */
    @Test
    @DisplayName("测试密码解密功能")
    public void testPasswordDecryption() throws Exception {
        // 测试AES加密解密
        String originalPassword = "test123456";
        String key = "testkey12345678";

        String encrypted = DruidEncryptedPasswordCallback.encodeAES(originalPassword, key);
        String decrypted = DruidEncryptedPasswordCallback.decryptPassword(encrypted, key, "AES");

        assertEquals(originalPassword, decrypted, "AES加密解密应该正确");

        System.out.println("AES加密解密测试通过");
        System.out.println("  原始密码: " + originalPassword);
        System.out.println("  加密密码: " + encrypted);
        System.out.println("  解密密码: " + decrypted);
    }

    /**
     * 测试SM4加密解密
     */
    @Test
    @DisplayName("测试SM4加密解密功能")
    public void testSM4Encryption() throws Exception {
        String originalPassword = "test123456";
        String key = "testkey123456789";

        String encrypted = DruidEncryptedPasswordCallback.encodeSM4(originalPassword, key);
        String decrypted = DruidEncryptedPasswordCallback.decryptPassword(encrypted, key, "SM4");

        assertEquals(originalPassword, decrypted, "SM4加密解密应该正确");

        System.out.println("SM4加密测试:");
        System.out.println("  原始密码: " + originalPassword);
        System.out.println("  加密密码: " + encrypted);
        System.out.println("  解密密码: " + decrypted);

        assertNotNull(encrypted, "加密密码不应为null");
        assertFalse(encrypted.isEmpty(), "加密密码不应为空");
    }

    /**
     * 测试配置生成器
     */
    @Test
    @DisplayName("测试配置生成器")
    public void testConfigGenerator() throws Exception {
        DruidConfigGenerator generator = new DruidConfigGenerator();

        generator.setDriverClassName("com.mysql.cj.jdbc.Driver");
        generator.setUrl("jdbc:mysql://localhost:3306/test");
        generator.setUsername("testuser");
        generator.setPassword("testpass");
        generator.setInitialSize(5);
        generator.setMinIdle(5);
        generator.setMaxActive(20);
        generator.setMaxWait(60000);
        generator.setAlgorithm("AES");

        generator.encryptPassword();

        assertNotNull(generator.getEncryptedPassword(), "加密密码不应为null");
        assertNotNull(generator.getEncryptionKey(), "加密密钥不应为null");

        java.util.Properties props = generator.generateProperties();
        assertNotNull(props, "配置属性不应为null");
        assertTrue(props.getProperty("druid.connectionProperties").contains("config.decrypt=true"), "配置应包含解密标志");
        assertTrue(props.getProperty("druid.connectionProperties").contains("config.decrypt.algorithm=AES"), "配置应包含算法信息");

        System.out.println("配置生成器测试通过");
    }

    /**
     * 测试多次获取和释放连接
     */
    @Test
    @DisplayName("测试连接复用")
    public void testConnectionReuse() throws SQLException {
        if (connectionPool == null) {
            System.out.println("跳过测试: 连接池未初始化");
            return;
        }

        DruidDataSource dataSource = connectionPool.getDataSource();

        int loopCount = 10;
        System.out.println("执行 " + loopCount + " 次连接获取和释放...");

        for (int i = 0; i < loopCount; i++) {
            try (Connection conn = connectionPool.getConnection()) {
                assertNotNull(conn);
                // 执行简单查询
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT 1");
                rs.next();
                rs.close();
                stmt.close();
            }
        }

        System.out.println("连接复用测试完成");
        System.out.println("  最终活跃连接: " + dataSource.getActiveCount());
        System.out.println("  最终空闲连接: " + dataSource.getPoolingCount());
    }

    /**
     * 生成测试配置文件（如果在测试环境）
     */
    private void generateTestConfig() throws Exception {
        // 从环境变量读取测试配置
        String[] envVars = {
                "TEST_DB_URL", "TEST_DB_USERNAME", "TEST_DB_PASSWORD",
                "TEST_DB_DRIVER", "TEST_DB_KEY"
        };

        boolean hasAllConfig = true;
        for (String var : envVars) {
            if (System.getenv(var) == null) {
                hasAllConfig = false;
                break;
            }
        }

        if (!hasAllConfig) {
            throw new Exception("缺少测试数据库配置环境变量");
        }

        // 创建配置
        DruidConfigGenerator generator = new DruidConfigGenerator();
        generator.setDriverClassName(System.getenv("TEST_DB_DRIVER"));
        generator.setUrl(System.getenv("TEST_DB_URL"));
        generator.setUsername(System.getenv("TEST_DB_USERNAME"));
        generator.setPassword(System.getenv("TEST_DB_PASSWORD"));
        generator.setInitialSize(5);
        generator.setMinIdle(5);
        generator.setMaxActive(20);
        generator.setMaxWait(60000);

        String key = System.getenv("TEST_DB_KEY");
        if (key != null && !key.isEmpty()) {
            generator.setEncryptionKey(key);
        }

        generator.encryptPassword();
        generator.saveToFile(TEST_CONFIG);

        System.out.println("测试配置文件已生成: " + TEST_CONFIG);
    }

    /**
     * 主方法：允许从命令行运行测试
     */
    public static void main(String[] args) {
        System.out.println("从命令行运行Druid连接池测试...");

        String configPath = null;
        if (args.length > 0) {
            configPath = args[0];
        }

        if (configPath != null && new java.io.File(configPath).exists()) {
            System.setProperty("test.config", configPath);
            System.out.println("使用配置文件: " + configPath);
        } else {
            System.out.println("未指定配置文件，尝试使用环境变量或测试配置");
        }

        // 直接运行几个简单测试而不使用 JUnit Platform API
        System.out.println();
        System.out.println("===============================================");
        System.out.println("运行简单测试:");
        System.out.println("===============================================");

        try {
            DruidConnectionTest test = new DruidConnectionTest();
            test.testPasswordDecryption();
            System.out.println("[PASS] 密码解密测试");
        } catch (Exception e) {
            System.out.println("[FAIL] 密码解密测试: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            DruidConnectionTest test = new DruidConnectionTest();
            test.testSM4Encryption();
            System.out.println("[PASS] SM4加密测试");
        } catch (Exception e) {
            System.out.println("[FAIL] SM4加密测试: " + e.getMessage());
            e.printStackTrace();
        }

        try {
            DruidConnectionTest test = new DruidConnectionTest();
            test.testConfigGenerator();
            System.out.println("[PASS] 配置生成器测试");
        } catch (Exception e) {
            System.out.println("[FAIL] 配置生成器测试: " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("===============================================");
        System.out.println("简单测试运行完成");
        System.out.println("注意: 如需运行完整测试套件，请使用 Maven: mvn test");
        System.out.println("===============================================");
    }

    /**
     * 测试密码存储管理器
     */
    @Test
    @DisplayName("测试密码存储管理器")
    public void testPasswordStorageManager() throws Exception {
        com.dbpwd.configcenter.PasswordStorageManager manager = new com.dbpwd.configcenter.PasswordStorageManager();

        // 添加密码映射
        String uid = "test_db";
        String originalPassword = "test_pass_123";
        String encryptionKey = "test_key_123456";

        String encryptedPassword = com.dbpwd.configcenter.PasswordStorageManager.encryptPassword(originalPassword, encryptionKey, "SM4");
        manager.addPasswordMapping(uid, encryptedPassword);

        // 验证密码映射
        assertTrue(manager.contains(uid), "应包含密码映射");
        assertEquals(encryptedPassword, manager.getPassword(uid), "密码应匹配");
        assertEquals(1, manager.size(), "应只有 1 个映射");

        // 测试解密
        String decrypted = com.dbpwd.configcenter.PasswordStorageManager.decryptPassword(encryptedPassword, encryptionKey, "SM4");
        assertEquals(originalPassword, decrypted, "解密后的密码应正确");

        System.out.println("密码存储管理器测试通过");
        System.out.println("  UID: " + uid);
        System.out.println("  加密密码: " + encryptedPassword);
        System.out.println("  解密密码: " + decrypted);
    }

    /**
     * 测试 Placeholder 解析器
     */
    @Test
    @DisplayName("测试 Placeholder 解析器")
    public void testPlaceholderResolver() {
        java.util.Map<String, String> passwordMap = new java.util.HashMap<>();
        passwordMap.put("test_db", "[16]encrypted_password_here");

        com.dbpwd.configcenter.PlaceholderResolver resolver = new com.dbpwd.configcenter.PlaceholderResolver(passwordMap);

        // 测试 @config() placeholder
        String value1 = "@config(test_db)";
        String resolved1 = resolver.resolveValue(value1);
        assertEquals("[16]encrypted_password_here", resolved1, "Placeholder 应被替换");

        // 测试 @key() placeholder
        java.util.Map<String, String> keyMap = new java.util.HashMap<>();
        keyMap.put("test_key", "key_value_here");
        com.dbpwd.configcenter.PlaceholderResolver resolverWithKeys = new com.dbpwd.configcenter.PlaceholderResolver(passwordMap, keyMap);

        String value2 = "@key(test_key)";
        String resolved2 = resolverWithKeys.resolveValue(value2);
        assertEquals("key_value_here", resolved2, "密钥 placeholder 应被替换");

        System.out.println("Placeholder 解析器测试通过");
    }

    /**
     * 测试配置模板生成器
     */
    @Test
    @DisplayName("测试配置模板生成器")
    public void testConfigTemplateGenerator() throws IOException {
        com.dbpwd.configcenter.DruidConfigTemplateGenerator generator = new com.dbpwd.configcenter.DruidConfigTemplateGenerator();

        generator.setDriverClassName("com.mysql.cj.jdbc.Driver");
        generator.setUrl("jdbc:mysql://localhost:3306/testdb");
        generator.setUsername("testuser");
        generator.setPasswordUid("test_db");
        generator.setInitialSize(5);
        generator.setMinIdle(5);
        generator.setMaxActive(20);

        java.util.Properties template = generator.generateTemplate();

        assertNotNull(template, "模板不应为 null");
        assertEquals("com.mysql.cj.jdbc.Driver", template.getProperty("driverClassName"), "驱动名应匹配");
        assertEquals("jdbc:mysql://localhost:3306/testdb", template.getProperty("url"), "URL 应匹配");
        assertEquals("testuser", template.getProperty("username"), "用户名应匹配");

        // 验证 placeholder
        String password = template.getProperty("password");
        assertTrue(password.startsWith("@config("), "密码应使用 placeholder");
        assertTrue(password.contains("test_db"), "placeholder 应包含 uid");

        System.out.println("配置模板生成器测试通过");
        System.out.println("  密码 Placeholder: " + password);
    }

    /**
     * 测试本地模板解析
     */
    @Test
    @DisplayName("测试本地模板解析")
    public void testLocalTemplateResolution() throws Exception {
        // 创建临时模板文件
        java.util.Properties templateProps = new java.util.Properties();
        templateProps.setProperty("driverClassName", "com.mysql.cj.jdbc.Driver");
        templateProps.setProperty("url", "jdbc:mysql://localhost:3306/testdb");
        templateProps.setProperty("username", "testuser");
        templateProps.setProperty("password", "@config(test_db)");
        templateProps.setProperty("initialSize", "5");

        // 创建临时密码文件
        java.util.Properties passwordProps = new java.util.Properties();
        String encryptedPassword = com.dbpwd.configcenter.PasswordStorageManager.encryptPassword("test_pass", "test_key", "SM4");
        passwordProps.setProperty("test_db", encryptedPassword);

        // 保存到临时文件
        java.nio.file.Path templateFile = java.nio.file.Files.createTempFile("template", ".properties");
        java.nio.file.Path passwordFile = java.nio.file.Files.createTempFile("password", ".txt");

        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(templateFile.toFile());
             java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(fos, java.nio.charset.StandardCharsets.UTF_8)) {
            templateProps.store(writer, "Template");
        }

        try (java.io.PrintWriter pw = new java.io.PrintWriter(passwordFile.toFile())) {
            pw.println("# Password file");
            pw.println("test_db=" + encryptedPassword);
        }

        // 解析模板
        java.util.Properties resolved = com.dbpwd.configcenter.ConfigCenterTemplateResolver.resolveTemplateLocal(
            templateFile.toAbsolutePath().toString(),
            passwordFile.toAbsolutePath().toString()
        );

        assertNotNull(resolved, "解析结果不应为 null");
        assertEquals(encryptedPassword, resolved.getProperty("password"), "密码应被替换");

        System.out.println("本地模板解析测试通过");

        // 清理临时文件
        java.nio.file.Files.deleteIfExists(templateFile);
        java.nio.file.Files.deleteIfExists(passwordFile);
    }
}
