package com.dbpwd.core;

import com.dbpwd.crypto.ENCFormat;
import com.dbpwd.crypto.Sm4GCMCipher;
import com.dbpwd.model.DatabaseEntry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据库密码提供者
 *
 * 封装从db.pwd和sm4.key获取解密后密码的完整流程，提供简洁的API供应用调用
 *
 * 功能：
 * - 从指定的密钥文件加载SM4密钥
 * - 从指定的密码文件加载db.pwd
 * - 根据占位符查找对应的加密密码
 * - 解密密文密码返回明文密码
 *
 * 使用场景：Java应用需要安全地获取数据库密码，但不想手动处理密钥加载和密码解密的细节
 *
 * @author Database Password Manager
 * @version 5.3
 */
public class PasswordProvider {

    private static final Map<String, String> keyCache = new ConcurrentHashMap<>();
    private static final Map<String, PasswordFileManager> fileCache = new ConcurrentHashMap<>();

    /**
     * 获取数据库明文密码（一步调用，使用默认路径）
     *
     * 默认路径：
     * - 密钥文件: ~/.dbpwd/sm4.key
     * - 密码文件: ~/.dbpwd/db.pwd
     *
     * @param placeholder 占位符（数据库ID）
     * @return 解密后的明文密码
     * @throws Exception 获取或解密失败
     */
    public static String getDatabasePassword(String placeholder) throws Exception {
        return getDatabasePasswordInternal(placeholder, null, null, null, null);
    }

    /**
     * 获取数据库明文密码（一步调用，指定文件路径）
     *
     * @param placeholder 占位符（数据库ID）
     * @param keyFilePath SM4密钥文件路径（可空，使用默认路径）
     * @param pwdFilePath 密码文件路径（可空，使用默认路径）
     * @return 解密后的明文密码
     * @throws Exception 获取或解密失败
     */
    public static String getDatabasePassword(String placeholder, String keyFilePath, String pwdFilePath)
            throws Exception {
        return getDatabasePasswordInternal(placeholder, keyFilePath, pwdFilePath, null, null);
    }

    /**
     * 获取数据库明文密码（一步调用，带密钥对象）
     *
     * @param placeholder 占位符（数据库ID）
     * @param loader SM4KeyLoader实例
     * @param pwdFileManager PasswordFileManager实例
     * @return 解密后的明文密码
     * @throws Exception 获取或解密失败
     */
    public static String getDatabasePassword(String placeholder, Sm4KeyLoader loader,
                                             PasswordFileManager passwordFileManager)
            throws Exception {
        return getDatabasePasswordInternal(placeholder, null, null, loader, passwordFileManager);
    }

    /**
     * 内部实现方法 - 获取数据库明文密码
     *
     * @param placeholder 占位符（数据库ID）
     * @param keyFilePath SM4密钥文件路径（可空）
     * @param pwdFilePath 密码文件路径（可空）
     * @param loader SM4KeyLoader实例（可空）
     * @param pwdFileManager PasswordFileManager实例（可空）
     * @return 解密后的明文密码
     * @throws Exception 获取或解密失败
     */
    private static String getDatabasePasswordInternal(String placeholder, String keyFilePath,
                                                       String pwdFilePath, Sm4KeyLoader loader,
                                                       PasswordFileManager pwdFileManager)
            throws Exception {
        // 1. 加载SM4密钥
        String sm4Key;
        PasswordFileManager passwordManager;

        if (loader != null && pwdFileManager != null) {
            // 使用提供的loader和manager
            sm4Key = loader.loadKey(true);
            passwordManager = pwdFileManager;
        } else {
            // 使用文件路径
            sm4Key = loadSm4Key(keyFilePath);
            passwordManager = loadPasswordFile(pwdFilePath);
        }

        // 2. 根据占位符查找加密密码
        DatabaseEntry entry = passwordManager.getById(placeholder);
        if (entry == null) {
            throw new IllegalArgumentException("未找到数据库配置，占位符: " + placeholder);
        }

        String encryptedPassword = entry.getPassword();
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            throw new IllegalArgumentException("数据库密码为空: " + placeholder);
        }

        // 3. 解密密码
        return decryptPassword(encryptedPassword, sm4Key);
    }

    /**
     * 从文件加载SM4密钥（带缓存）
     *
     * @param keyFilePath 密钥文件路径（可空，使用默认路径）
     * @return SM4密钥
     * @throws Exception 加载失败
     */
    private static String loadSm4Key(String keyFilePath) throws Exception {
        if (keyFilePath != null && !keyFilePath.isEmpty()) {
            // 使用指定路径
            Sm4KeyLoader loader = new Sm4KeyLoader(keyFilePath);
            return loader.loadKey(true);
        } else {
            // 使用默认路径
            Sm4KeyLoader loader = new Sm4KeyLoader();
            return loader.loadKey(true);
        }
    }

    /**
     * 从文件加载密码文件（带缓存）
     *
     * @param pwdFilePath 密码文件路径（可空，使用默认路径）
     * @return PasswordFileManager实例
     * @throws Exception 加载失败
     */
    private static PasswordFileManager loadPasswordFile(String pwdFilePath) throws Exception {
        String actualPwdPath = (pwdFilePath != null && !pwdFilePath.isEmpty()) ?
            pwdFilePath : getDefaultPwdPath();
        return PasswordFileManager.loadFromFile(actualPwdPath);
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
        String algorithm = ENCFormat.extractAlgorithm(encryptedPassword);
        String cipherText = ENCFormat.extractCipherText(encryptedPassword);

        // 仅支持SM4-GCM格式，ECB格式抛出异常（新应用不支持）
        if (!"SM4-GCM".equalsIgnoreCase(algorithm)) {
            throw new IllegalArgumentException(
                "不支持的加密算法: " + algorithm + "。仅支持SM4-GCM格式（新应用必须使用GCM认证加密）");
        }

        return Sm4GCMCipher.decrypt(cipherText, sm4Key);
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
