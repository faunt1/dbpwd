package com.dbpwd.mybatis;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.core.PasswordFileManager;
import com.dbpwd.core.Sm4KeyLoader;
import com.dbpwd.model.DatabaseEntry;
import com.dbpwd.crypto.ENCFormat;
import com.dbpwd.crypto.Sm4GCMCipher;
import org.apache.ibatis.datasource.DataSourceFactory;

import javax.sql.DataSource;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * 集成场景和方案 - 方案4: MyBatis DataSourceFactory集成
 *
 * MyBatis密码管理Druid DataSource工厂
 * 实现MyBatis的DataSourceFactory接口，支持在MyBatis XML配置中使用
 *
 * 使用方式1：在mybatis-config.xml中配置
 * <pre>
 * &lt;dataSource type="com.dbpwd.mybatis.DbPwdDruidDataSourceFactory"&gt;
 *   &lt;property name="dbId" value="my_database"/&gt;
 *   &lt;property name="initialSize" value="5"/&gt;
 *   &lt;property name="maxActive" value="20"/&gt;
 * &lt;/dataSource&gt;
 * </pre>
 *
 * 使用方式2：纯Java配置
 * <pre>
 * DbPwdDruidDataSourceFactory factory = new DbPwdDruidDataSourceFactory();
 * Properties props = new Properties();
 * props.setProperty("dbId", "my_database");
 * factory.setProperties(props);
 * DataSource dataSource = factory.getDataSource();
 * </pre>
 *
 * @author Database Password Manager
 * @version 5.3
 */
public class DbPwdDruidDataSourceFactory implements DataSourceFactory {

    private static final Logger logger = Logger.getLogger(DbPwdDruidDataSourceFactory.class.getName());

    private static final String DEFAULT_KEY_PATH = System.getProperty("user.home") + "/.dbpwd/sm4.key";
    private static final String DEFAULT_PWD_PATH = System.getProperty("user.home") + "/.dbpwd/db.pwd";

    private DruidDataSource dataSource;
    private String dbId;
    private String keyFile;
    private String pwdFile;
    private String sm4Key;
    private PasswordFileManager passwordManager;
    private Properties properties = new Properties();

    public DbPwdDruidDataSourceFactory() {
        this.dataSource = new DruidDataSource();
    }

    public DbPwdDruidDataSourceFactory(String dbId) {
        this();
        this.dbId = dbId;
    }

    /**
     * 实现DataSourceFactory接口 - 设置属性
     * MyBatis会调用此方法传入配置属性
     */
    @Override
    public void setProperties(Properties properties) {
        this.properties.putAll(properties);
        this.dbId = properties.getProperty("dbId");
        this.keyFile = properties.getProperty("keyFile", DEFAULT_KEY_PATH);
        this.pwdFile = properties.getProperty("pwdFile", DEFAULT_PWD_PATH);

        // 设置Druid连接池参数
        if (properties.containsKey("initialSize")) {
            dataSource.setInitialSize(Integer.parseInt(properties.getProperty("initialSize")));
        }
        if (properties.containsKey("minIdle")) {
            dataSource.setMinIdle(Integer.parseInt(properties.getProperty("minIdle")));
        }
        if (properties.containsKey("maxActive")) {
            dataSource.setMaxActive(Integer.parseInt(properties.getProperty("maxActive")));
        }
        if (properties.containsKey("maxWait")) {
            dataSource.setMaxWait(Long.parseLong(properties.getProperty("maxWait")));
        }
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
     * 实现DataSourceFactory接口 - 获取数据源
     * MyBatis会调用此方法获取DataSource
     */
    @Override
    public DataSource getDataSource() {
        // 验证dbId是否有效
        if (dbId == null || dbId.isEmpty()) {
            throw new IllegalStateException("dbId未设置或为空，请通过构造函数、setDbId()或setProperties()设置dbId");
        }

        try {
            // 从db.pwd加载配置
            loadPasswordManager();
            DatabaseEntry entry = passwordManager.getById(dbId);
            if (entry == null) {
                throw new IllegalArgumentException("未找到数据库配置: " + dbId);
            }

            // 设置配置（如果未手动设置）
            if (dataSource.getUrl() == null || dataSource.getUrl().isEmpty()) {
                dataSource.setUrl(entry.getUrl());
            }
            if (dataSource.getUsername() == null || dataSource.getUsername().isEmpty()) {
                dataSource.setUsername(entry.getUsername());
            }

            // 解密并设置密码
            String plainPassword = decryptPassword(entry.getPassword());
            dataSource.setPassword(plainPassword);

            logger.info("成功创建Druid数据源，dbId: " + dbId);

            return dataSource;

        } catch (Exception e) {
            logger.severe("创建Druid数据源失败: " + e.getMessage());
            throw new RuntimeException("创建Druid数据源失败: " + e.getMessage(), e);
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
