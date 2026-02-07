package com.dbpwd.universal;

import com.dbpwd.DruidEncryptedPasswordCallback;

import java.util.Properties;
import java.util.logging.Logger;

/**
 * 配置属性密码拦截器
 *
 * 功能：
 * 1. 拦截配置文件中的 Properties
 * 2. 解析并替换 @config(uid) 和 @key(keyId) 等占位符
 * 3. 将加密密码解密为明文密码
 * 4. 支持配置中心下载优先、本地文件兜底的策略
 *
 * @author Druid DB Password Manager
 * @version 2.0
 */
public class PasswordPropertiesInterceptor {

    private static final Logger logger = Logger.getLogger(PasswordPropertiesInterceptor.class.getName());

    private final PasswordProvider passwordProvider;
    private final KeyProvider keyProvider;
    private final com.dbpwd.configcenter.ConfigCenterFallbackManager fallbackManager;

    /**
     * @config(uid) 占位符前缀
     */
    private static final String CONFIG_PLACEHOLDER_PREFIX = "@config(";

    /**
     * @key(keyId) 占位符前缀
     */
    private static final String KEY_PLACEHOLDER_PREFIX = "@key(";

    /**
     * 使用默认的密码提供者创建拦截器
     */
    public PasswordPropertiesInterceptor() {
        this(null, null, null);
        logger.warning("未指定密码提供者，将不支持占位符替换");
    }

    /**
     * 使用回退管理器创建拦截器（推荐方式）
     *
     * @param fallbackManager 配置中心回退管理器
     */
    public PasswordPropertiesInterceptor(com.dbpwd.configcenter.ConfigCenterFallbackManager fallbackManager) {
        this(fallbackManager, null);
    }

    /**
     * 使用回退管理器和密钥提供者创建拦截器
     *
     * @param fallbackManager 配置中心回退管理器
     * @param keyProvider 密钥提供者
     */
    public PasswordPropertiesInterceptor(com.dbpwd.configcenter.ConfigCenterFallbackManager fallbackManager,
                                         KeyProvider keyProvider) {
        this(null, keyProvider, fallbackManager);
    }

    /**
     * 使用密码提供者创建拦截器
     *
     * @param passwordProvider 密码提供者
     */
    public PasswordPropertiesInterceptor(PasswordProvider passwordProvider) {
        this(passwordProvider, null, null);
    }

    /**
     * 使用密码提供者和密钥提供者创建拦截器
     *
     * @param passwordProvider 密码提供者
     * @param keyProvider 密钥提供者
     */
    public PasswordPropertiesInterceptor(PasswordProvider passwordProvider, KeyProvider keyProvider) {
        this(passwordProvider, keyProvider, null);
    }

    /**
     * 全参构造函数
     *
     * @param passwordProvider 密码提供者
     * @param keyProvider 密钥提供者
     * @param fallbackManager 配置中心回退管理器
     */
    public PasswordPropertiesInterceptor(PasswordProvider passwordProvider,
                                         KeyProvider keyProvider,
                                         com.dbpwd.configcenter.ConfigCenterFallbackManager fallbackManager) {
        this.passwordProvider = passwordProvider;
        this.keyProvider = keyProvider;
        this.fallbackManager = fallbackManager;

        if (fallbackManager != null) {
            logger.info("使用回退管理器，支持配置中心下载优先、本地文件兜底策略");
        } else if (passwordProvider == null) {
            logger.warning("未指定密码提供者，将不支持占位符替换");
        }
    }

    /**
     * 拦截并处理 Properties
     * 1. 解析 @config(uid) 占位符，从密码提供者获取加密密码
     * 2. 将加密密码解密为明文密码
     * 3. 解析 @key(keyId) 占位符，从密钥提供者获取密钥
     *
     * @param props 原始配置属性
     * @return 处理后的配置属性（密码已解密为明文）
     */
    public Properties intercept(Properties props) {
        if (props == null) {
            return new Properties();
        }

        Properties result = new Properties();
        int replacedCount = 0;

        for (String key : props.stringPropertyNames()) {
            String value = props.getProperty(key);
            String processedValue = processValue(key, value);

            if (!processedValue.equals(value)) {
                replacedCount++;
                logger.fine(String.format("占位符替换: %s = %s -> %s",
                        key, value, maskedValue(processedValue)));
            }

            result.setProperty(key, processedValue);
        }

        if (replacedCount > 0 && logger.isLoggable(java.util.logging.Level.INFO)) {
            logger.info(String.format("密码属性拦截完成，共替换 %d 个占位符", replacedCount));
        }

        return result;
    }

    /**
     * 处理单个配置值
     *
     * @param key 配置键
     * @param value 配置值
     * @return 处理后的值
     */
    private String processValue(String key, String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // 处理 @config(uid) - 密码占位符（带解密）
        if (value.startsWith(CONFIG_PLACEHOLDER_PREFIX) && value.endsWith(")")) {
            String uid = extractPlaceholderValue(value, CONFIG_PLACEHOLDER_PREFIX);
            return resolveConfig(uid);
        }

        // 处理 @key(keyId) - 密钥占位符
        if (keyProvider != null && value.startsWith(KEY_PLACEHOLDER_PREFIX) && value.endsWith(")")) {
            String keyId = extractPlaceholderValue(value, KEY_PLACEHOLDER_PREFIX);
            String keyValue = keyProvider.getKey(keyId);
            if (keyValue != null) {
                return keyValue;
            }
        }

        return value;
    }

    /**
     * 解析 @config(uid) 占位符
     * 1. 优先从回退管理器获取加密密码（支持配置中心下载优先、本地文件兜底）
     * 2. 如果回退管理器不可用，从密码提供者获取
     * 3. 使用 DruidEncryptedPasswordCallback 解密为明文密码
     *
     * @param uid 密码唯一标识
     * @return 明文密码
     */
    private String resolveConfig(String uid) {
        String encryptedPassword = null;

        // 1. 优先从回退管理器获取
        if (fallbackManager != null) {
            encryptedPassword = fallbackManager.getPassword(uid);
            if (encryptedPassword != null) {
                logger.fine("从回退管理器获取密码 (uid=" + uid + ")");
            }
        }

        // 2. 如果回退管理器不可用，从密码提供者获取
        if (encryptedPassword == null && passwordProvider != null) {
            encryptedPassword = passwordProvider.getPassword(uid);
        }

        if (encryptedPassword == null) {
            throw new IllegalArgumentException("未找到 UID '" + uid + "' 对应的加密密码");
        }

        // 解码加密密码格式 [length]base64...
        // 格式: [keyLength]Base64(iv+ciphertext)
        int bracketStart = encryptedPassword.indexOf("[");
        int bracketEnd = encryptedPassword.indexOf("]");
        if (bracketStart == -1 || bracketEnd == -1) {
            throw new IllegalArgumentException("加密密码格式无效，缺少密钥长度标识: " + maskedValue(encryptedPassword));
        }

        // 密钥长度（字节）
        int keyLengthKeyBytes = Integer.parseInt(encryptedPassword.substring(bracketStart + 1, bracketEnd));

        // 判断算法：SM4 使用 16 字节密钥，AES 可以是 16/24/32 字节（对应 128/192/256 位）
        String algorithm;
        if (keyLengthKeyBytes == 16) {
            // 可能是 SM4(16字节) 或 AES-128(16字节)，默认使用 SM4（国密标准）
            algorithm = "SM4";
        } else {
            // AES-256 使用 32 字节密钥，AES-192 使用 24 字节密钥
            algorithm = "AES";
        }

        // 解密密钥从配置中获取（这里简化处理，实际应该从全局配置获取）
        String decryptKey = System.getProperty("dbpwd.decrypt.key", "");
        if (decryptKey.isEmpty()) {
            decryptKey = System.getenv("DBPWD_DECRYPT_KEY");
        }
        if (decryptKey.isEmpty()) {
            throw new IllegalStateException("解密密钥未配置，请设置系统属性 dbpwd.decrypt.key 或环境变量 DBPWD_DECRYPT_KEY");
        }

        logger.fine(String.format("获取加密密码 (uid=%s, algorithm=%s): %s", uid, algorithm, maskedValue(encryptedPassword)));

        // 解密密码为明文
        try {
            String decryptedPassword = DruidEncryptedPasswordCallback.decryptPassword(
                    encryptedPassword, decryptKey, algorithm);
            logger.fine(String.format("解密密码成功 (uid=%s)", uid));
            return decryptedPassword;
        } catch (Exception e) {
            throw new RuntimeException("解密密码失败 (uid=" + uid + ", algorithm=" + algorithm + ")", e);
        }
    }

    /**
     * 提取占位符中的值
     *
     * @param placeholder 完整占位符，例如 "@config(my_db)"
     * @param prefix 占位符前缀，例如 "@config("
     * @return 占位符中的值，例如 "my_db"
     */
    private String extractPlaceholderValue(String placeholder, String prefix) {
        return placeholder.substring(prefix.length(), placeholder.length() - 1).trim();
    }

    /**
     * 遮蔽密码（用于日志输出）
     *
     * @param value 需要遮蔽的值（可能是明文密码或加密密码）
     * @return 遮蔽后的值
     */
    private String maskedValue(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        if (value.length() <= 8) {
            return "****";
        }
        // 如果是加密密码格式 [length]base64...，显示前缀和后几个字符
        if (value.startsWith("[")) {
            int bracketEnd = value.indexOf("]");
            if (bracketEnd > 0 && bracketEnd < value.length()) {
                String prefix = value.substring(0, bracketEnd + 1);
                String suffix = value.substring(Math.max(bracketEnd + 1, value.length() - 8));
                return prefix + "****" + suffix;
            }
        }
        // 明文密码，只显示首尾
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    /**
     * 密码提供者接口
     */
    public interface PasswordProvider {
        /**
         * 根据 UID 获取加密密码
         *
         * @param uid 密码唯一标识
         * @return 加密后的密码（带 [length] 前缀和算法标识）
         */
        String getPassword(String uid);
    }

    /**
     * 密钥提供者接口
     */
    public interface KeyProvider {
        /**
         * 根据 keyId 获取密钥
         *
         * @param keyId 密钥标识
         * @return 密钥
         */
        String getKey(String keyId);
    }
}
