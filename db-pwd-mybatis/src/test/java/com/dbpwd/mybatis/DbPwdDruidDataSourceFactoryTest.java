package com.dbpwd.mybatis;

import com.dbpwd.core.Sm4KeyLoader;
import com.dbpwd.crypto.Sm4GCMCipher;
import com.dbpwd.model.DatabaseEntry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DbPwdDruidDataSourceFactory单元测试
 *
 * 设计原则：
 * - 单元测试使用临时目录（@TempDir），自包含、自解释
 * - 通过 keyFile 和 pwdFile 属性指定自定义路径
 * - 不依赖用户主目录下的 ~/.dbpwd/ 配置
 *
 * @author Database Password Manager
 * @version 6.1
 */
public class DbPwdDruidDataSourceFactoryTest {

    @TempDir
    Path tempDir;

    private String testKeyPath;
    private String testPwdPath;

    @BeforeEach
    public void setUp() throws Exception {
        testKeyPath = tempDir.resolve("test_mybatis_sm4.key").toString();
        testPwdPath = tempDir.resolve("test_mybatis_dbpwd.pwd").toString();
    }

    @AfterEach
    public void tearDown() throws Exception {
        // @TempDir 会自动清理临时目录，无需手动删除
    }

    @Test
    public void testInstantiation() {
        DbPwdDruidDataSourceFactory factory = new DbPwdDruidDataSourceFactory();
        assertNotNull(factory);
    }

    @Test
    public void testInstantiationWithDbId() {
        DbPwdDruidDataSourceFactory factory = new DbPwdDruidDataSourceFactory("test_db");
        assertNotNull(factory);
    }

    @Test
    public void testSetDbId() {
        DbPwdDruidDataSourceFactory factory = new DbPwdDruidDataSourceFactory();
        factory.setDbId("test_db");
        // 验证方法不会抛出异常
        assertDoesNotThrow(() -> factory.setDbId("another_db"));
    }

    @Test
    public void testSetKeyFile() {
        DbPwdDruidDataSourceFactory factory = new DbPwdDruidDataSourceFactory();
        factory.setKeyFile("/custom/key.file");
        // 验证方法不会抛出异常
        assertDoesNotThrow(() -> factory.setKeyFile("/custom/key.file"));
    }

    @Test
    public void testSetPwdFile() {
        DbPwdDruidDataSourceFactory factory = new DbPwdDruidDataSourceFactory();
        factory.setPwdFile("/custom/pwd.file");
        // 验证方法不会抛出异常
        assertDoesNotThrow(() -> factory.setPwdFile("/custom/pwd.file"));
    }

    @Test
    public void testSetProperty() {
        DbPwdDruidDataSourceFactory factory = new DbPwdDruidDataSourceFactory();
        factory.setProperty("url", "jdbc:mysql://localhost:3306/test");
        factory.setProperty("username", "test_user");
        factory.setProperty("initialSize", "5");
        factory.setProperty("maxActive", "20");

        // 验证方法不会抛出异常
        assertDoesNotThrow(() -> factory.setProperty("minIdle", "2"));
    }

    @Test
    public void testSetProperties() {
        DbPwdDruidDataSourceFactory factory = new DbPwdDruidDataSourceFactory();
        Properties props = new Properties();
        props.setProperty("url", "jdbc:mysql://localhost:3306/test");
        props.setProperty("username", "test_user");
        props.setProperty("initialSize", "5");
        factory.setProperties(props);

        // 验证方法不会抛出异常
        assertDoesNotThrow(() -> factory.setProperties(props));
    }

    @Test
    public void testGetDataSourceThrowsWhenDbIdNotSet() throws Exception {
        DbPwdDruidDataSourceFactory factory = new DbPwdDruidDataSourceFactory();

        assertThrows(IllegalStateException.class, factory::getDataSource);
    }

    @Test
    public void testGetDataSourceThrowsWhenDbIdIsEmpty() throws Exception {
        DbPwdDruidDataSourceFactory factory = new DbPwdDruidDataSourceFactory("");
        factory.setKeyFile(testKeyPath);
        factory.setPwdFile(testPwdPath);

        assertThrows(IllegalStateException.class, factory::getDataSource);
    }

    @Test
    public void testGetDataSourceThrowsWhenDatabaseNotFound() throws Exception {
        // 准备测试数据
        Sm4KeyLoader.generateKey(testKeyPath, true);

        DbPwdDruidDataSourceFactory factory = new DbPwdDruidDataSourceFactory("non_existent_db");
        factory.setKeyFile(testKeyPath);
        factory.setPwdFile(testPwdPath);

        assertThrows(Exception.class, factory::getDataSource);
    }

    @Test
    public void testGetDataSourceWithValidConfiguration() throws Exception {
        // 准备测试数据
        String sm4Key = Sm4KeyLoader.generateKey(testKeyPath, true);
        String plainPassword = "test_password_123";
        String encryptedPassword = Sm4GCMCipher.encrypt(plainPassword, sm4Key);
        String formattedEncrypted = "ENC(SM4-GCM:" + encryptedPassword + ")";

        // 创建密码文件
        com.dbpwd.core.PasswordFileManager manager = com.dbpwd.core.PasswordFileManager.loadFromFileSafe(testPwdPath);
        DatabaseEntry entry = new DatabaseEntry();
        entry.setId("test_db");
        entry.setName("Test Database");
        entry.setUrl("jdbc:mysql://localhost:3306/test");
        entry.setUsername("test_user");
        entry.setPassword(formattedEncrypted);
        manager.addDatabase(entry);
        manager.save();

        // 测试获取数据源
        DbPwdDruidDataSourceFactory factory = new DbPwdDruidDataSourceFactory("test_db");
        factory.setKeyFile(testKeyPath);
        factory.setPwdFile(testPwdPath);
        factory.setProperty("initialSize", "5");
        factory.setProperty("maxActive", "20");

        // 验证方法不会抛出异常（但实际连接可能会失败）
        // 注意：这里我们只验证getDataSource方法不会抛出配置异常
        DataSource dataSource = factory.getDataSource();
        assertNotNull(dataSource);
    }

    @Test
    public void testGetDataSourceWithProperties() throws Exception {
        // 准备测试数据
        String sm4Key = Sm4KeyLoader.generateKey(testKeyPath, true);
        String plainPassword = "test_password_456";
        String encryptedPassword = Sm4GCMCipher.encrypt(plainPassword, sm4Key);
        String formattedEncrypted = "ENC(SM4-GCM:" + encryptedPassword + ")";

        // 创建密码文件
        com.dbpwd.core.PasswordFileManager manager = com.dbpwd.core.PasswordFileManager.loadFromFileSafe(testPwdPath);
        DatabaseEntry entry = new DatabaseEntry();
        entry.setId("test_db2");
        entry.setName("Test Database 2");
        entry.setUrl("jdbc:mysql://localhost:3306/test2");
        entry.setUsername("test_user2");
        entry.setPassword(formattedEncrypted);
        manager.addDatabase(entry);
        manager.save();

        // 测试使用Properties配置
        Properties props = new Properties();
        props.setProperty("dbId", "test_db2");
        props.setProperty("keyFile", testKeyPath);
        props.setProperty("pwdFile", testPwdPath);
        props.setProperty("url", "jdbc:mysql://localhost:3306/override");
        props.setProperty("username", "test_user_override");
        props.setProperty("initialSize", "10");
        props.setProperty("maxActive", "50");

        DbPwdDruidDataSourceFactory factory = new DbPwdDruidDataSourceFactory();
        factory.setProperties(props);

        DataSource dataSource = factory.getDataSource();
        assertNotNull(dataSource);
    }
}
