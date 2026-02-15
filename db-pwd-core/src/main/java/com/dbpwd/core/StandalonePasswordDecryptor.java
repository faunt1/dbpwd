package com.dbpwd.core;

import com.dbpwd.model.DatabaseEntry;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 纯JDBC数据库密码解密器
 *
 * 简单的纯JDBC应用密码管理工具类
 * 负责从配置文件中读取加密密码并解密，支持数据库连接获取
 *
 * 使用场景：
 * - 无框架依赖的独立JDBC应用
 * - 测试工具和脚本
 * - 不使用Spring/MyBatis等框架的应用
 *
 * @author Database Password Manager
 * @version 5.3
 */
public class StandalonePasswordDecryptor {

    private static final Logger logger = Logger.getLogger(StandalonePasswordDecryptor.class.getName());

    // 密钥缓存（密钥文件路径 -> 解密后的密钥）
    private static final Map<String, String> keyCache = new ConcurrentHashMap<>();

    /**
     * 获取数据库明文密码
     *
     * @param dbId 数据库ID占位符
     * @param keyFilePath SM4密钥文件路径（可空，使用默认路径）
     * @param pwdFilePath 密码文件路径（可空，使用默认路径）
     * @return 解密后的明文密码
     * @throws Exception 获取或解密失败
     */
    public static String getPlainPassword(String dbId, String keyFilePath, String pwdFilePath) throws Exception {
        // 1. 加载SM4密钥
        String sm4Key = loadSm4Key(keyFilePath, dbId);

        // 2. 加载密码文件
        PasswordFileManager passwordManager = loadPasswordFile(pwdFilePath);

        // 3. 根据数据库ID查找加密密码
        DatabaseEntry entry = passwordManager.getById(dbId);
        if (entry == null) {
            throw new IllegalArgumentException("未找到数据库配置，占位符: " + dbId);
        }

        String encryptedPassword = entry.getPassword();
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            throw new IllegalArgumentException("数据库密码为空: " + dbId);
        }

        // 4. 解密密码
        return decryptPassword(encryptedPassword, sm4Key);
    }

    /**
     * 获取数据库连接
     *
     * @param dbId 数据库ID占位符
     * @param keyFilePath SM4密钥文件路径（可空，使用默认路径）
     * @param pwdFilePath 密码文件路径（可空，使用默认路径）
     * @return 数据库连接
     * @throws Exception 获取或解密失败或连接失败
     */
    public static Connection getConnection(String dbId, String keyFilePath, String pwdFilePath) throws Exception {
        // 获取解密后的密码
        String plainPassword = getPlainPassword(dbId, keyFilePath, pwdFilePath);

        // 获取数据库连接信息
        DatabaseEntry entry = getDatabaseEntry(dbId, pwdFilePath);
        if (entry == null) {
            throw new IllegalArgumentException("未找到数据库配置，占位符: " + dbId);
        }

        Connection connection = DriverManager.getConnection(
            entry.getUrl(), entry.getUsername(), plainPassword);

        return connection;
    }

    /**
     * 获取数据库连接信息
     *
     * @param dbId 数据库ID占位符
     * @param pwdFilePath 密码文件路径（可空，使用默认路径）
     * @return 数据库连接信息
     * @throws Exception 查找失败
     */
    private static DatabaseEntry getDatabaseEntry(String dbId, String pwdFilePath) throws Exception {
        PasswordFileManager passwordManager = loadPasswordFile(pwdFilePath);
        DatabaseEntry entry = passwordManager.getById(dbId);
        if (entry == null) {
            throw new IllegalArgumentException("未找到数据库配置，占位符: " + dbId);
        }
        return entry;
    }

    /**
     * 加载SM4密钥（带缓存）
     *
     * @param keyFilePath SM4密钥文件路径
     * @param dbId 数据库ID（仅用于缓存标识）
     * @return SM4密钥
     * @throws Exception 加载失败
     */
    private static String loadSm4Key(String keyFilePath, String dbId) throws Exception {
        // 使用默认路径如果参数为空或空字符串
        String actualKeyPath = (keyFilePath != null && !keyFilePath.trim().isEmpty()) ?
            keyFilePath : getDefaultKeyPath();

        // 使用缓存：keyFilePath:dbId
        String cacheKey = actualKeyPath + ":" + dbId;
        if (keyCache.containsKey(cacheKey)) {
            return keyCache.get(cacheKey);
        }

        // 加载密钥
        Sm4KeyLoader loader = new Sm4KeyLoader(actualKeyPath);
        String key = loader.loadKey(true);

        // 缓存密钥
        keyCache.put(cacheKey, key);

        logger.fine("已缓存密钥: " + cacheKey);
        return key;
    }

    /**
     * 解密密码
     *
     * @param encryptedPassword 加密后的密码
     * @param sm4Key SM4密钥
     * @return 明文密码
     * @throws Exception 解密失败
     */
    private static String decryptPassword(String encryptedPassword, String sm4Key) throws Exception {
        String algorithm = com.dbpwd.crypto.ENCFormat.extractAlgorithm(encryptedPassword);
        String cipherText = com.dbpwd.crypto.ENCFormat.extractCipherText(encryptedPassword);

        // 仅支持SM4-GCM格式，ECB格式抛出异常（新应用不支持）
        if (!"SM4-GCM".equalsIgnoreCase(algorithm)) {
            throw new IllegalArgumentException(
                "不支持的加密算法: " + algorithm + "。仅支持SM4-GCM格式（新应用必须使用GCM认证加密）");
        }

        return com.dbpwd.crypto.Sm4GCMCipher.decrypt(cipherText, sm4Key);
    }

    /**
     * 从文件加载密码文件（带缓存）
     *
     * @param pwdFilePath 密码文件路径（可空，使用默认路径）
     * @return PasswordFileManager实例
     * @throws Exception 加载失败
     */
    private static PasswordFileManager loadPasswordFile(String pwdFilePath) throws Exception {
        String actualPwdPath = (pwdFilePath != null && !pwdFilePath.trim().isEmpty()) ?
            pwdFilePath : getDefaultPwdPath();
        return PasswordFileManager.loadFromFile(actualPwdPath);
    }

    /**
     * 获取默认密钥文件路径
     */
    private static String getDefaultKeyPath() {
        return System.getProperty("user.home") + "/.dbpwd/sm4.key";
    }

    /**
     * 获取默认密码文件路径
     */
    private static String getDefaultPwdPath() {
        return System.getProperty("user.home") + "/.dbpwd/db.pwd";
    }
}
