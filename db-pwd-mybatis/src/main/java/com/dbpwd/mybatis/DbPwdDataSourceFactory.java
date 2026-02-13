package com.dbpwd.mybatis;

import com.dbpwd.core.PasswordFileManager;
import com.dbpwd.core.Sm4KeyLoader;
import com.dbpwd.model.DatabaseEntry;
import com.dbpwd.crypto.ENCFormat;
import com.dbpwd.crypto.Sm4GCMCipher;
import org.apache.ibatis.datasource.DataSourceFactory;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;

import javax.sql.DataSource;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * 集成场景和方案 - 方案4: MyBatis DataSourceFactory集成
 *
 * MyBatis密码管理DataSource工厂
 * 实现MyBatis的DataSourceFactory接口，支持在MyBatis XML配置中使用
 *
 * 使用方式1：在mybatis-config.xml中配置
 * <pre>
 * &lt;dataSource type="com.dbpwd.mybatis.DbPwdDataSourceFactory"&gt;
 *   &lt;property name="dbId" value="my_database"/&gt;
 * &lt;/dataSource&gt;
 * </pre>
 *
 * 使用方式2：纯Java配置
 * <pre>
 * DbPwdDataSourceFactory factory = new DbPwdDataSourceFactory();
 * factory.setDbId("my_database");
 * DataSource dataSource = factory.getDataSource();
 * </pre>
 *
 * @author Database Password Manager
 * @version 5.3
 */
public class DbPwdDataSourceFactory implements DataSourceFactory {

    private static final Logger logger = Logger.getLogger(DbPwdDataSourceFactory.class.getName());

    private static final String DEFAULT_KEY_PATH = System.getProperty("user.home") + "/.dbpwd/sm4.key";
    private static final String DEFAULT_PWD_PATH = System.getProperty("user.home") + "/.dbpwd/db.pwd";

    private UnpooledDataSource dataSource;
    private String dbId;
    private String keyFile;
    private String pwdFile;
    private String sm4Key;
    private String url;
    private String username;
    private String driverClassName;
    private PasswordFileManager passwordManager;

    public DbPwdDataSourceFactory() {
        this.dataSource = new UnpooledDataSource();
    }

    public DbPwdDataSourceFactory(String dbId) {
        this();
        this.dbId = dbId;
    }

    /**
     * 实现DataSourceFactory接口 - 设置属性
     * MyBatis会调用此方法传入配置属性
     */
    @Override
    public void setProperties(Properties properties) {
        this.dbId = properties.getProperty("dbId");
        this.keyFile = properties.getProperty("keyFile", DEFAULT_KEY_PATH);
        this.pwdFile = properties.getProperty("pwdFile", DEFAULT_PWD_PATH);
        this.url = properties.getProperty("url");
        this.username = properties.getProperty("username");
        this.driverClassName = properties.getProperty("driverClassName");

        // 直接设置到dataSource
        if (url != null && !url.isEmpty()) {
            dataSource.setUrl(url);
        }
        if (username != null && !username.isEmpty()) {
            dataSource.setUsername(username);
        }
        if (driverClassName != null && !driverClassName.isEmpty()) {
            dataSource.setDriver(driverClassName);
        }
    }

    /**
     * 设置数据库ID占位符
     */
    public void setDbId(String dbId) {
        this.dbId = dbId;
    }

    /**
     * 设置JDBC URL
     */
    public void setUrl(String url) {
        this.url = url;
        if (url != null && !url.isEmpty()) {
            dataSource.setUrl(url);
        }
    }

    /**
     * 设置用户名
     */
    public void setUsername(String username) {
        this.username = username;
        if (username != null && !username.isEmpty()) {
            dataSource.setUsername(username);
        }
    }

    /**
     * 设置驱动类名
     */
    public void setDriverClassName(String driverClassName) {
        this.driverClassName = driverClassName;
        if (driverClassName != null && !driverClassName.isEmpty()) {
            dataSource.setDriver(driverClassName);
        }
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
     * 实现DataSourceFactory接口 - 获取数据源
     * MyBatis会调用此方法获取DataSource
     */
    @Override
    public DataSource getDataSource() {
        try {
            if (dbId == null || dbId.isEmpty()) {
                throw new IllegalStateException("dbId must be set");
            }

            loadPasswordManager();
            DatabaseEntry entry = passwordManager.getById(dbId);
            if (entry == null) {
                throw new IllegalArgumentException("未找到数据库配置: " + dbId);
            }

            // 解密密码
            String plainPassword = decryptPassword(entry.getPassword());

            // 使用db.pwd中的配置，可以被手动设置的值覆盖
            String actualUrl = (url != null && !url.isEmpty()) ? url : entry.getUrl();
            String actualUsername = (username != null && !username.isEmpty()) ? username : entry.getUsername();
            String actualDriver = (driverClassName != null && !driverClassName.isEmpty()) ? driverClassName : "org.h2.Driver";

            dataSource.setUrl(actualUrl);
            dataSource.setUsername(actualUsername);
            dataSource.setPassword(plainPassword);
            dataSource.setDriver(actualDriver);

            logger.info("成功创建UnpooledDataSource，dbId: " + dbId);

            return dataSource;

        } catch (Exception e) {
            logger.severe("创建数据源失败: " + e.getMessage());
            throw new RuntimeException("创建数据源失败: " + e.getMessage(), e);
        }
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
