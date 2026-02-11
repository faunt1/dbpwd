package com.dbpwd.druid;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.core.PasswordFileManager;
import com.dbpwd.core.Sm4KeyLoader;
import com.dbpwd.model.DatabaseEntry;
import com.dbpwd.crypto.ENCFormat;
import com.dbpwd.crypto.Sm4GCMCipher;

import java.util.logging.Logger;

/**
 * Druid密码回调工具类
 *
 * 配合Druid DataSource使用，提供密码自动解密功能
 *
 * 使用方式1：使用工厂方法创建DataSource
 * <pre>
 * DruidDataSource dataSource = DruidPasswordCallback.createDataSource(
 *     "db_placeholder",      // 数据库占位符
 *     true                  // 自动解密密码
 * );
 * </pre>
 *
 * 使用方式2：手动设置密码解密
 * <pre>
 * DruidDataSource dataSource = new DruidDataSource();
 * dataSource.setUrl("jdbc:mysql://localhost:3306/mydb");
 * dataSource.setUsername("user");
 * String plainPassword = DruidPasswordCallback.decryptPassword("db_placeholder");
 * dataSource.setPassword(plainPassword);
 * </pre>
 *
 * @author Database Password Manager
 * @version 5.3
 */
public class DruidPasswordCallback {

    private static final Logger logger = Logger.getLogger(DruidPasswordCallback.class.getName());

    private static final String DEFAULT_KEY_PATH = System.getProperty("user.home") + "/.dbpwd/sm4.key";
    private static final String DEFAULT_PWD_PATH = System.getProperty("user.home") + "/.dbpwd/db.pwd";

    private final String dbPlaceholder;
    private final String keyFilePath;
    private final String pwdFilePath;

    /**
     * 构造函数（使用默认路径）
     *
     * @param dbPlaceholder 数据库占位符（用于在db.pwd中查找配置）
     */
    public DruidPasswordCallback(String dbPlaceholder) {
        this(dbPlaceholder, null, null);
    }

    /**
     * 构造函数（自定义路径）
     *
     * @param dbPlaceholder 数据库占位符（用于在db.pwd中查找配置）
     * @param keyFilePath SM4密钥文件路径（null则使用默认路径）
     * @param pwdFilePath 密码文件路径（null则使用默认路径）
     */
    public DruidPasswordCallback(String dbPlaceholder, String keyFilePath, String pwdFilePath) {
        this.dbPlaceholder = dbPlaceholder;
        this.keyFilePath = keyFilePath;
        this.pwdFilePath = pwdFilePath;
    }

    /**
     * 获取数据库占位符
     *
     * @return 数据库占位符
     */
    public String getDbPlaceholder() {
        return dbPlaceholder;
    }

    /**
     * 解密数据库密码
     *
     * @return 明文密码
     * @throws Exception 解密失败
     */
    public String decryptPassword() throws Exception {
        // 1. 加载SM4密钥
        String sm4Key = loadSm4Key(keyFilePath);

        // 2. 加载密码文件并查找数据库配置
        DatabaseEntry entry = loadDatabaseEntry(pwdFilePath, dbPlaceholder);

        if (entry == null) {
            throw new IllegalArgumentException("未找到数据库配置，占位符: " + dbPlaceholder);
        }

        String encryptedPassword = entry.getPassword();
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            throw new IllegalArgumentException("数据库密码为空: " + dbPlaceholder);
        }

        // 3. 解密密码（仅支持GCM格式）
        return decryptPassword(encryptedPassword, sm4Key);
    }

    /**
     * 创建DruidDataSource工厂方法（使用自定义文件路径，自动加载配置）
     *
     * @param dbPlaceholder 数据库占位符
     * @param keyFilePath SM4密钥文件路径（null则使用默认路径）
     * @param pwdFilePath 密码文件路径（null则使用默认路径）
     * @param autoDecrypt 是否自动解密密码并设置到DataSource
     * @return DruidDataSource实例
     * @throws Exception 创建失败
     */
    public static DruidDataSource createDataSource(
            String dbPlaceholder,
            String keyFilePath,
            String pwdFilePath,
            boolean autoDecrypt) throws Exception {
        return createDataSource(dbPlaceholder, null, keyFilePath, pwdFilePath, autoDecrypt);
    }

    /**
     * 创建DruidDataSource工厂方法（使用默认路径和自动加载配置）
     *
     * @param dbPlaceholder 数据库占位符
     * @param autoDecrypt 是否自动解密密码并设置到DataSource
     * @return DruidDataSource实例
     * @throws Exception 创建失败
     */
    public static DruidDataSource createDataSource(String dbPlaceholder, boolean autoDecrypt) throws Exception {
        return createDataSource(dbPlaceholder, null, null, null, autoDecrypt);
    }

    /**
     * 创建DruidDataSource工厂方法（完整参数）
     *
     * @param dbPlaceholder 数据库占位符
     * @param url 数据库连接URL（为null则从db.pwd读取）
     * @param keyFilePath SM4密钥文件路径（null则使用默认路径）
     * @param pwdFilePath 密码文件路径（null则使用默认路径）
     * @param autoDecrypt 是否自动解密密码并设置到DataSource
     * @return DruidDataSource实例
     * @throws Exception 创建失败
     */
    public static DruidDataSource createDataSource(
            String dbPlaceholder,
            String url,
            String keyFilePath,
            String pwdFilePath,
            boolean autoDecrypt) throws Exception {

        DruidDataSource dataSource = new DruidDataSource();

        // 从db.pwd加载数据库配置
        loadDatabaseConfig(dataSource, dbPlaceholder, pwdFilePath);

        // 如果指定了url，覆盖从db.pwd读取的值
        if (url != null && !url.isEmpty()) {
            dataSource.setUrl(url);
        }

        // 自动解密密码
        if (autoDecrypt) {
            DruidPasswordCallback callback = new DruidPasswordCallback(dbPlaceholder, keyFilePath, pwdFilePath);
            String plainPassword = callback.decryptPassword();
            dataSource.setPassword(plainPassword);
            logger.fine("密码解密并设置成功，占位符: " + dbPlaceholder);
        }

        return dataSource;
    }

    /**
     * 静态方法：解密指定数据库占位符的密码
     *
     * @param dbPlaceholder 数据库占位符
     * @return 明文密码
     * @throws Exception 解密失败
     */
    public static String decryptPassword(String dbPlaceholder) throws Exception {
        return new DruidPasswordCallback(dbPlaceholder).decryptPassword();
    }

    /**
     * 静态方法：解密指定数据库占位符的密码（自定义路径）
     *
     * @param dbPlaceholder 数据库占位符
     * @param keyFilePath SM4密钥文件路径
     * @param pwdFilePath 密码文件路径
     * @return 明文密码
     * @throws Exception 解密失败
     */
    public static String decryptPassword(
            String dbPlaceholder,
            String keyFilePath,
            String pwdFilePath) throws Exception {
        return new DruidPasswordCallback(dbPlaceholder, keyFilePath, pwdFilePath).decryptPassword();
    }

    /**
     * 加载SM4密钥
     */
    private static String loadSm4Key(String keyFilePath) throws Exception {
        String actualKeyPath = (keyFilePath != null && !keyFilePath.isEmpty()) ?
                keyFilePath : DEFAULT_KEY_PATH;
        Sm4KeyLoader loader = new Sm4KeyLoader(actualKeyPath);
        return loader.loadKey(true);
    }

    /**
     * 加载数据库配置
     */
    private static DatabaseEntry loadDatabaseEntry(String pwdFilePath, String dbPlaceholder) throws Exception {
        String actualPwdPath = (pwdFilePath != null && !pwdFilePath.isEmpty()) ?
                pwdFilePath : DEFAULT_PWD_PATH;
        PasswordFileManager manager = PasswordFileManager.loadFromFile(actualPwdPath);
        return manager.getById(dbPlaceholder);
    }

    /**
     * 解密密码
     */
    private static String decryptPassword(String encryptedPassword, String sm4Key) throws Exception {
        String algorithm = ENCFormat.extractAlgorithm(encryptedPassword);
        String cipherText = ENCFormat.extractCipherText(encryptedPassword);

        // 仅支持SM4-GCM格式
        if (!"SM4-GCM".equalsIgnoreCase(algorithm)) {
            throw new IllegalArgumentException(
                    "不支持的加密算法: " + algorithm + "。仅支持SM4-GCM格式（新应用必须使用GCM认证加密）");
        }

        return Sm4GCMCipher.decrypt(cipherText, sm4Key);
    }

    /**
     * 从db.pwd加载数据库配置到DataSource（使用默认路径）
     */
    private static void loadDatabaseConfig(DruidDataSource dataSource, String dbPlaceholder) {
        loadDatabaseConfig(dataSource, dbPlaceholder, null);
    }

    /**
     * 从db.pwd加载数据库配置到DataSource（使用自定义路径）
     */
    private static void loadDatabaseConfig(DruidDataSource dataSource, String dbPlaceholder, String pwdFilePath) {
        try {
            DatabaseEntry entry = loadDatabaseEntry(pwdFilePath, dbPlaceholder);
            if (entry != null) {
                if (entry.getUrl() != null) {
                    dataSource.setUrl(entry.getUrl());
                }
                if (entry.getUsername() != null) {
                    dataSource.setUsername(entry.getUsername());
                }
                logger.fine("从db.pwd加载数据库配置: " + dbPlaceholder);
            }
        } catch (Exception e) {
            logger.warning("加载数据库配置失败: " + e.getMessage());
        }
    }
}
