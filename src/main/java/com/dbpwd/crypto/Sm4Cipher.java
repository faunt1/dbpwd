package com.dbpwd.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.apache.commons.lang.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Security;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * SM4 国密算法加密解密工具类
 *
 * 提供独立的 SM4 加密解密功能，GCM 模式
 */
public class Sm4Cipher {

    private static final Logger logger = Logger.getLogger(Sm4Cipher.class.getName());

    // SM4 GCM 模式参数
    private static final String ALGORITHM_SM4 = "SM4";
    private static final String CIPHER_MODE_SM4 = "SM4/GCM/NoPadding";
    private static final int SM4_GCM_TAG_LENGTH = 16;
    private static final int SM4_GCM_IV_LENGTH = 12;
    private static final int SM4_KEY_LENGTH = 16;
    private static final int CHARSET = "UTF-8";

    static {
        // 注册 BouncyCastle Provider（如已注册则跳过）
        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
            logger.info("BouncyCastle Provider 已注册");
        }
    }

    /**
     * SM4 加密
     *
     * @param plainText 原始明文
     * @param key 加密密钥
     * @return 加密后的密码（Base64 编码）
     * @throws Exception 加密失败
     */
    public static String encrypt(String plainText, String key) throws Exception {
        if (StringUtils.isEmpty(plainText)) {
            throw new IllegalArgumentException("明文不能为空");
        }
        if (StringUtils.isEmpty(key)) {
            throw new IllegalArgumentException("密钥不能为空");
        }

        // 派生密钥
        byte[] keyBytes = deriveKey(key, SM4_KEY_LENGTH);

        // 生成随机 IV
        byte[] iv = new byte[SM4_GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        // 创建 Cipher
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(CIPHER_MODE_SM4);
        } catch (Exception e) {
            try {
                cipher = Cipher.getInstance(CIPHER_MODE_SM4, "BC");
            } catch (Exception ex) {
                throw new RuntimeException("SM4 cipher 不可用: " + ex.getMessage(), ex);
            }
        }

        GCMParameterSpec spec = new GCMParameterSpec(SM4_GCM_TAG_LENGTH * 8, iv);
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM_SM4);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // 组合 IV 和密文
        byte[] ivAndCiphertext = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, ivAndCiphertext, 0, iv.length);
        System.arraycopy(encrypted, 0, ivAndCiphertext, iv.length, encrypted.length);

        // 返回格式: [16]Base64(IV+Ciphertext)
        return "[" + SM4_KEY_LENGTH + "]" + Base64.getEncoder().encodeToString(ivAndCiphertext);
    }

    /**
     * SM4 解密
     *
     * @param encryptedText 加密后的密码（格式：[16]Base64(IV+Ciphertext)）
     * @param key 解密密钥
     * @return 原始明文
     * @throws Exception 解密失败
     */
    public static String decrypt(String encryptedText, String key) throws Exception {
        if (StringUtils.isEmpty(encryptedText)) {
            throw new IllegalArgumentException("加密密码不能为空");
        }
        if (StringUtils.isEmpty(key)) {
            throw new IllegalArgumentException("密钥不能为空");
        }

        // 解析密码格式
        if (!encryptedText.matches("\\[\\d+\\].+")) {
            throw new IllegalArgumentException("加密密码格式错误，应为 [keyLength]Base64(data)");
        }

        int keyLength = Integer.parseInt(encryptedText.substring(1, encryptedText.indexOf("]")));
        byte[] ivAndCiphertext = Base64.getDecoder().decode(encryptedText.substring(encryptedText.indexOf("]") + 1));

        // 解析 IV 和密文
        byte[] iv = new byte[SM4_GCM_IV_LENGTH];
        byte[] ciphertext = new byte[ivAndCiphertext.length - SM4_GCM_IV_LENGTH];
        System.arraycopy(ivAndCiphertext, 0, iv, 0, SM4_GCM_IV_LENGTH);
        System.arraycopy(ivAndCiphertext, SM4_GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

        // 派生密钥
        byte[] keyBytes = deriveKey(key, SM4_KEY_LENGTH);

        // 创建 Cipher
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(CIPHER_MODE_SM4);
        } catch (Exception e) {
            try {
                cipher = Cipher.getInstance(CIPHER_MODE_SM4, "BC");
            } catch (Exception ex) {
                throw new RuntimeException("SM4 cipher 不可用: " + ex.getMessage(), ex);
            }
        }

        GCMParameterSpec spec = new GCMParameterSpec(SM4_GCM_TAG_LENGTH * 8, iv);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, ALGORITHM_SM4);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, spec);

        byte[] decrypted = cipher.doFinal(ciphertext);
        return new String(decrypted, CHARSET);
    }

    /**
     * 密钥派生函数 (PBKDF2)
     *
     * @param password 密码
     * @param keyLength 密钥长度（字节）
     * @return 派生密钥
     * @throws Exception 派生失败
     */
    private static byte[] deriveKey(String password, int keyLength) throws Exception {
        int iterations = 10000;
        int saltLength = 16;

        // 使用密码前16字节作为盐
        byte[] salt = password.getBytes(CHARSET);
        if (salt.length < saltLength) {
            byte[] temp = new byte[saltLength];
            System.arraycopy(salt, 0, temp, 0, salt.length);
            for (int i = salt.length; i < saltLength; i++) {
                temp[i] = (byte) (i % 256);
            }
            salt = temp;
        } else {
            byte[] temp = new byte[saltLength];
            System.arraycopy(salt, 0, temp, 0, saltLength);
            salt = temp;
        }

        javax.crypto.SecretKeyFactory keyFactory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                password.toCharArray(),
                salt,
                iterations,
                keyLength * 8
        );

        javax.crypto.SecretKey tmp = keyFactory.generateSecret(spec);
        return tmp.getEncoded();
    }

    /**
     * 生成随机密钥
     *
     * @param length 密钥长度（字节）
     * @return Base64 编码的密钥
     */
    public static String generateRandomKey(int length) {
        try {
            SecureRandom random = SecureRandom.getInstanceStrong();
            byte[] keyBytes = new byte[length];
            random.nextBytes(keyBytes);
            return Base64.getEncoder().encodeToString(keyBytes);
        } catch (Exception e) {
            throw new RuntimeException("生成随机密钥失败", e);
        }
    }

    // ==================== 配置中心集成 ====================

    /**
     * 生成适用于配置中心的密码格式
     * 包含 uid 和加密密码
     *
     * @param uid 数据库唯一标识符
     * @param plainPassword 明文密码
     * @param key 加密密钥
     * @return 配置中心格式的字符串
     * @throws Exception 异常
     */
    public static String generateForConfigCenter(String uid, String plainPassword, String key) throws Exception {
        String encryptedPassword = encrypt(plainPassword, key);
        return uid + ":" + encryptedPassword;
    }

    /**
     * 解析配置中心格式的密码
     *
     * @param configString 格式：uid:encryptedPassword
     * @param key 解密密钥
     * @return 明文密码
     * @throws Exception 解密失败
     */
    public static String decryptFromConfigCenter(String configString, String key) throws Exception {
        int colonPos = configString.indexOf(':');
        if (colonPos <= 0) {
            throw new IllegalArgumentException("配置格式错误，应为 uid:encryptedPassword");
        }

        String uid = configString.substring(0, colonPos).trim();
        String encryptedPassword = configString.substring(colonPos + 1).trim();

        logger.fine("解析配置中心密码 (uid: " + uid + ")");
        return decrypt(encryptedPassword, key);
    }
}
