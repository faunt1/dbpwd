package com.dbpwd.mybatis;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.core.PasswordFileManager;
import com.dbpwd.core.Sm4KeyLoader;
import com.dbpwd.model.DatabaseEntry;
import com.dbpwd.crypto.ENCFormat;
import com.dbpwd.crypto.Sm4GCMCipher;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * 集成场景和方案 - 方案4: MyBatis DataSourceFactory集成
 *
 * MyBatis密码管理Druid DataSource工厂
 *
 * 使用方式：在mybatis-config.xml中配置
 * <pre>
 * &lt;dataSource type="com.dbpwd.mybatis.DbPwdDruidDataSourceFactory"&gt;
 *   &lt;property name="dbId" value="my_database"/&gt;
 * &lt;/dataSource&gt;
 * </pre>
 *
 * @author Database Password Manager
 * @version 5.3
 */
public class DbPwdDruidDataSourceFactory {

    private static final String DEFAULT_KEY_PATH = System.getProperty("user.home") + "/.dbpwd/sm4.key";
    private static final String DEFAULT_PWD_PATH = System.getProperty("user.home") + "/.dbpwd/db.pwd";

    private String dbId;
    private String keyFile;
    private String pwdFile;
    private String sm4Key;
    private PasswordFileManager passwordManager;
    private Properties properties = new Properties();

    public DbPwdDruidDataSourceFactory() {
    }

    public DbPwdDruidDataSourceFactory(String dbId) {
        this.dbId = dbId;
    }

    /**
     * 设置属性
     */
    public void setProperties(Properties properties) {
        this.properties.putAll(properties);
        this.dbId = properties.getProperty("dbId");
        this.keyFile = properties.getProperty("keyFile", DEFAULT_KEY_PATH);
        this.pwdFile = properties.getProperty("pwdFile", DEFAULT_PWD_PATH);
    }

    /**
     * 设置数据库ID占位符
     */
    public void setDbId(String dbId) {
        this.dbId = dbId;
    }

    /**
     * 设置密钥文件路径
     */
    public void setKeyFile(String keyFile) {
        this.keyFile = keyFile;
    }

    /**
     * 设置密码文件路径
     */
    public void setPwdFile(String pwdFile) {
        this.pwdFile = pwdFile;
    }

    /**
     * 设置连接池属性
     */
    public void setProperty(String key, String value) {
        properties.setProperty(key, value);
    }

    /**
     * 获取数据源（Druid连接池，密码自动解密）
     */
    public DataSource getDataSource() throws Exception {
        if (dbId == null || dbId.isEmpty()) {
            throw new IllegalStateException("dbId must be set");
        }

        loadPasswordManager();
        DatabaseEntry entry = passwordManager.getById(dbId);
        if (entry == null) {
            throw new IllegalArgumentException("未找到数据库配置: " + dbId);
        }

        DruidDataSource dataSource = new DruidDataSource();

        // 设置连接属性（优先从属性文件，回退到db.pwd）
        String url = properties.getProperty("url", entry.getUrl());
        String username = properties.getProperty("username", entry.getUsername());
        String password = decryptPassword(entry.getPassword());

        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        // 设置Druid连接池参数
        String initialSize = properties.getProperty("initialSize");
        if (initialSize != null) {
            dataSource.setInitialSize(Integer.parseInt(initialSize));
        }
        String maxActive = properties.getProperty("maxActive");
        if (maxActive != null) {
            dataSource.setMaxActive(Integer.parseInt(maxActive));
        }
        String minIdle = properties.getProperty("minIdle");
        if (minIdle != null) {
            dataSource.setMinIdle(Integer.parseInt(minIdle));
        }

        return dataSource;
    }

    // ============ Private Methods ============

    private void loadPasswordManager() throws Exception {
        if (passwordManager == null) {
            String actualPwdPath = (pwdFile != null && !pwdFile.isEmpty()) ? pwdFile : DEFAULT_PWD_PATH;
            passwordManager = PasswordFileManager.loadFromFile(actualPwdPath);
        }
    }

    private String decryptPassword(String encryptedPassword) throws Exception {
        if (sm4Key == null) {
            String actualKeyPath = (keyFile != null && !keyFile.isEmpty()) ? keyFile : DEFAULT_KEY_PATH;
            Sm4KeyLoader loader = new Sm4KeyLoader(actualKeyPath);
            sm4Key = loader.loadKey(true);
        }

        String algorithm = ENCFormat.extractAlgorithm(encryptedPassword);
        String cipherText = ENCFormat.extractCipherText(encryptedPassword);

        if (!"SM4-GCM".equalsIgnoreCase(algorithm)) {
            throw new IllegalArgumentException(
                "不支持的加密算法: " + algorithm + "。仅支持SM4-GCM格式");
        }

        return Sm4GCMCipher.decrypt(cipherText, sm4Key);
    }
}
