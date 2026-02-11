package com.dbpwd.boot;

import com.dbpwd.core.PasswordFileManager;
import com.dbpwd.core.Sm4KeyLoader;
import com.dbpwd.model.DatabaseEntry;
import com.dbpwd.crypto.ENCFormat;
import com.dbpwd.crypto.Sm4GCMCipher;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 集成场景和方案 - 方案2: Spring Boot Starter集成
 *
 * 密码服务类
 *
 * 负责从配置文件中加载和缓存密码，提供解密服务
 *
 * @author Database Password Manager
 * @version 5.3
 */
public class DbPwdPasswordService {

    private final String passwordFilePath;
    private final String keyFilePath;

    // 密码缓存：dbId -> plainPassword
    private final Map<String, String> passwordCache = new ConcurrentHashMap<>();

    // 密文缓存：dbId -> encryptedPassword
    private final Map<String, String> encryptedCache = new ConcurrentHashMap<>();

    private String sm4Key;
    private PasswordFileManager passwordManager;

    public DbPwdPasswordService(String passwordFilePath, String keyFilePath) {
        this.passwordFilePath = passwordFilePath;
        this.keyFilePath = keyFilePath;
    }

    /**
     * 预加载所有密码到缓存
     */
    public void preloadPasswords() throws Exception {
        loadSm4Key();
        loadPasswordFile();

        for (DatabaseEntry entry : passwordManager.getAllDatabases()) {
            String dbId = entry.getId();
            if (entry.getPassword() != null && !entry.getPassword().isEmpty()) {
                encryptedCache.put(dbId, entry.getPassword());
                try {
                    String plainPassword = decrypt(entry.getPassword());
                    passwordCache.put(dbId, plainPassword);
                } catch (Exception e) {
                    // 记录但不中断加载
                }
            }
        }
    }

    /**
     * 获取数据库明文密码
     *
     * @param dbId 数据库ID占位符
     * @return 明文密码
     * @throws Exception 解密失败
     */
    public String getPlainPassword(String dbId) throws Exception {
        // 先从缓存查找
        if (passwordCache.containsKey(dbId)) {
            return passwordCache.get(dbId);
        }

        // 缓存未命中，解密
        String encrypted = getEncryptedPassword(dbId);
        String plain = decrypt(encrypted);

        // 加入缓存
        passwordCache.put(dbId, plain);

        return plain;
    }

    /**
     * 获取数据库配置（包括明文密码）
     *
     * @param dbId 数据库ID占位符
     * @return 数据库配置信息
     * @throws Exception 加载或解密失败
     */
    public DatabaseEntry getDatabaseEntry(String dbId) throws Exception {
        loadPasswordFileIfNecessary();
        DatabaseEntry entry = passwordManager.getById(dbId);
        if (entry == null) {
            throw new IllegalArgumentException("未找到数据库配置: " + dbId);
        }

        // 返回副本，避免修改原始配置
        DatabaseEntry result = new DatabaseEntry();
        result.setId(entry.getId());
        result.setName(entry.getName());
        result.setType(entry.getType());
        result.setHost(entry.getHost());
        result.setPort(entry.getPort());
        result.setDatabase(entry.getDatabase());
        result.setUrl(entry.getUrl());
        result.setUsername(entry.getUsername());
        // 密码通过getPlainPassword获取
        result.setPassword(getPlainPassword(dbId));

        return result;
    }

    /**
     * 检查是否包含指定的数据库配置
     *
     * @param dbId 数据库ID
     * @return 是否包含
     */
    public boolean containsDatabase(String dbId) throws Exception {
        loadPasswordFileIfNecessary();
        return passwordManager.getById(dbId) != null;
    }

    /**
     * 清除密码缓存
     */
    public void clearCache() {
        passwordCache.clear();
        encryptedCache.clear();
    }

    /**
     * 清除指定数据库的缓存
     *
     * @param dbId 数据库ID
     */
    public void clearCacheForDatabase(String dbId) {
        passwordCache.remove(dbId);
        encryptedCache.remove(dbId);
    }

    // ============ Private Methods ============

    private String getEncryptedPassword(String dbId) throws Exception {
        if (encryptedCache.containsKey(dbId)) {
            return encryptedCache.get(dbId);
        }

        loadPasswordFileIfNecessary();
        DatabaseEntry entry = passwordManager.getById(dbId);
        if (entry == null) {
            throw new IllegalArgumentException("未找到数据库配置: " + dbId);
        }

        String encrypted = entry.getPassword();
        if (encrypted == null || encrypted.isEmpty()) {
            throw new IllegalArgumentException("数据库密码为空: " + dbId);
        }

        encryptedCache.put(dbId, encrypted);
        return encrypted;
    }

    private String decrypt(String encryptedPassword) throws Exception {
        String algorithm = ENCFormat.extractAlgorithm(encryptedPassword);
        String cipherText = ENCFormat.extractCipherText(encryptedPassword);

        if (!"SM4-GCM".equalsIgnoreCase(algorithm)) {
            throw new IllegalArgumentException(
                "不支持的加密算法: " + algorithm + "。仅支持SM4-GCM格式");
        }

        return Sm4GCMCipher.decrypt(cipherText, getSm4Key());
    }

    private void loadSm4Key() throws Exception {
        if (sm4Key == null) {
            Sm4KeyLoader loader = new Sm4KeyLoader(keyFilePath);
            sm4Key = loader.loadKey(true);
        }
    }

    private String getSm4Key() throws Exception {
        if (sm4Key == null) {
            loadSm4Key();
        }
        return sm4Key;
    }

    private void loadPasswordFile() throws Exception {
        if (passwordManager == null) {
            passwordManager = PasswordFileManager.loadFromFile(passwordFilePath);
        }
    }

    private void loadPasswordFileIfNecessary() throws Exception {
        loadPasswordFile();
    }

    /**
     * 检查占位符格式是否匹配
     *
     * @param value 待检查值
     * @param prefix 前缀
     * @param suffix 后缀
     * @return 是否是占位符
     */
    public static boolean isPlaceholder(String value, String prefix, String suffix) {
        return value != null
            && value.startsWith(prefix)
            && value.endsWith(suffix)
            && value.length() > prefix.length() + suffix.length();
    }

    /**
     * 从占位符中提取数据库ID
     *
     * @param placeholder 占位符（如@my_database@）
     * @param prefix 前缀
     * @param suffix 后缀
     * @return 数据库ID
     */
    public static String extractDbId(String placeholder, String prefix, String suffix) {
        return placeholder.substring(prefix.length(), placeholder.length() - suffix.length());
    }
}
