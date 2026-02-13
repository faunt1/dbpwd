package com.dbpwd.web;

import com.dbpwd.core.PasswordFileManager;
import com.dbpwd.core.Sm4KeyLoader;
import com.dbpwd.model.DatabaseEntry;
import com.dbpwd.crypto.ENCFormat;
import com.dbpwd.crypto.Sm4GCMCipher;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 集成场景和方案 - 方案6: Servlet/Struts InitListener集成
 *
 * Web环境密码服务类
 *
 * 负责提供密码缓存和快速访问功能
 *
 * @author Database Password Manager
 * @version 5.3
 */
public class WebPasswordService {

    // ServletContext属性键名常量
    public static final String PASSWORD_SERVICE_KEY = "dbpwd.passwordService";
    public static final String DEFAULT_KEY_PATH = System.getProperty("user.home") + "/.dbpwd/sm4.key";

    private String keyFilePath;
    private String pwdFilePath;
    private String sm4Key;
    private PasswordFileManager passwordManager;

    // 密码缓存：dbId -> plainPassword
    private final Map<String, String> passwordCache = new ConcurrentHashMap<>();

    // 密文缓存：dbId -> encryptedPassword
    private final Map<String, String> encryptedCache = new ConcurrentHashMap<>();

    public WebPasswordService(String keyFilePath, String pwdFilePath) {
        this.keyFilePath = keyFilePath;
        this.pwdFilePath = pwdFilePath;
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
     */
    public DatabaseEntry getDatabaseEntry(String dbId) throws Exception {
        loadPasswordFileIfNecessary();
        DatabaseEntry entry = passwordManager.getById(dbId);
        if (entry == null) {
            throw new IllegalArgumentException("未找到数据库配置: " + dbId);
        }

        // 返回副本
        DatabaseEntry result = new DatabaseEntry();
        result.setId(entry.getId());
        result.setName(entry.getName());
        result.setType(entry.getType());
        result.setHost(entry.getHost());
        result.setPort(entry.getPort());
        result.setDatabase(entry.getDatabase());
        result.setUrl(entry.getUrl());
        result.setUsername(entry.getUsername());
        result.setPassword(getPlainPassword(dbId));

        return result;
    }

    /**
     * 检查是否包含指定的数据库配置
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
     */
    public void clearCacheForDatabase(String dbId) {
        passwordCache.remove(dbId);
        encryptedCache.remove(dbId);
    }

    /**
     * 自定义密钥和密码文件路径
     */
    public void setCustomPaths(String keyFile, String pwdFile) {
        this.keyFilePath = keyFile;
        this.pwdFilePath = pwdFile;
        // 清除缓存以强制重新加载
        this.sm4Key = null;
        this.passwordManager = null;
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
            passwordManager = PasswordFileManager.loadFromFile(pwdFilePath);
        }
    }

    private void loadPasswordFileIfNecessary() throws Exception {
        loadPasswordFile();
    }
}
