package com.dbpwd;

import com.alibaba.druid.util.DruidPasswordCallback;
import org.apache.commons.lang.StringUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Druid密码回调类，支持国密SM4算法和AES算法
 * 可通过配置选择加密算法
 */
public class DruidEncryptedPasswordCallback extends DruidPasswordCallback {

    private static final long serialVersionUID = 1L;

    // 加密类型配置
    private static final String CONFIG_DECRYPT = "config.decrypt";
    private static final String CONFIG_DECRYPT_KEY = "config.decrypt.key";
    private static final String CONFIG_DECRYPT_ALGORITHM = "config.decrypt.algorithm";

    // 算法常量
    private static final String ALGORITHM_SM4 = "SM4";
    private static final String ALGORITHM_AES = "AES";
    private static final String DEFAULT_ALGORITHM = "AES";
    private static final String CIPHER_MODE_SM4 = "SM4/GCM/NoPadding";
    private static final String CIPHER_MODE_AES = "AES/GCM/NoPadding";
    private static final String CHARSET = "UTF-8";

    // SM4 算法参数
    private static final int SM4_GCM_TAG_LENGTH = 16;
    private static final int SM4_GCM_IV_LENGTH = 12;
    private static final int SM4_KEY_LENGTH = 16;

    // AES 算法参数
    private static final int AES_GCM_TAG_LENGTH = 16;
    private static final int AES_GCM_IV_LENGTH = 12;
    private static final int AES_KEY_LENGTH = 16;
    private static final int AES_KEY_SIZE = 256;

    static {
        try {
            if (Security.getProvider("BC") == null) {
                Security.addProvider(new BouncyCastleProvider());
            }
        } catch (Throwable t) {
            // ignore — provider registration best-effort
        }
    }

    @Override
    public void setProperties(java.util.Properties properties) {
        super.setProperties(properties);

        boolean decrypt = Boolean.parseBoolean(properties.getProperty(CONFIG_DECRYPT, "false"));

        if (decrypt) {
            String encryptedPassword = getEncryptedPassword(properties);
            String decryptKey = properties.getProperty(CONFIG_DECRYPT_KEY, "");
            String algorithm = properties.getProperty(CONFIG_DECRYPT_ALGORITHM, DEFAULT_ALGORITHM);

            if (!StringUtils.isEmpty(encryptedPassword) && !StringUtils.isEmpty(decryptKey)) {
                try {
                    String decryptedPassword;
                    if (ALGORITHM_SM4.equalsIgnoreCase(algorithm)) {
                        decryptedPassword = decodeSM4(encryptedPassword, decryptKey);
                    } else {
                        decryptedPassword = decodeAES(encryptedPassword, decryptKey);
                    }
                    setPassword(decryptedPassword.toCharArray());
                } catch (Exception e) {
                    throw new RuntimeException("密码解密失败: " + e.getMessage(), e);
                }
            }
        } else {
            // 未加密，直接使用明文密码
            char[] pwd = getPassword();
            setPassword(pwd != null ? pwd : new char[0]);
        }
    }

    /**
     * 获取加密后的密码
     */
    private String getEncryptedPassword(java.util.Properties properties) {
        // 优先从getProperty获取，如果为空则使用父类的getPasswordText
        String encryptedPassword = properties.getProperty("password");
        if (encryptedPassword == null) {
            char[] pwd = getPassword();
            encryptedPassword = (pwd != null) ? new String(pwd) : null;
        }
        return encryptedPassword;
    }

    /**
     * SM4/GCM解密
     */
    private String decodeSM4(String encryptedText, String key) throws Exception {
        int keyLength = Integer.parseInt(encryptedText.substring(encryptedText.indexOf("[") + 1, encryptedText.indexOf("]")));
        byte[] ivAndCiphertext = Base64.getDecoder().decode(encryptedText.substring(encryptedText.indexOf("]") + 1));

        // 解析IV和密文
        byte[] iv = new byte[SM4_GCM_IV_LENGTH];
        byte[] ciphertext = new byte[ivAndCiphertext.length - SM4_GCM_IV_LENGTH];
        System.arraycopy(ivAndCiphertext, 0, iv, 0, SM4_GCM_IV_LENGTH);
        System.arraycopy(ivAndCiphertext, SM4_GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

        // 密钥处理
        byte[] keyBytes = deriveKey(key, SM4_KEY_LENGTH);

        // 解密 - 使用 SM4，若不可用则抛出异常
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(CIPHER_MODE_SM4);
        } catch (Exception e) {
            try {
                cipher = Cipher.getInstance(CIPHER_MODE_SM4, "BC");
            } catch (Exception ex) {
                throw new RuntimeException("SM4 cipher not available: " + ex.getMessage(), ex);
            }
        }
        GCMParameterSpec spec = new GCMParameterSpec(SM4_GCM_TAG_LENGTH * 8, iv);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, ALGORITHM_SM4);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, spec);

        byte[] decrypted = cipher.doFinal(ciphertext);
        return new String(decrypted, CHARSET);
    }

    /**
     * SM4/GCM加密
     */
    public static String encodeSM4(String plainText, String key) throws Exception {
        byte[] keyBytes = deriveKey(key, SM4_KEY_LENGTH);

        // 生成随机IV
        byte[] iv = new byte[SM4_GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        // 加密 - 使用 SM4，若不可用则抛出异常
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(CIPHER_MODE_SM4);
        } catch (Exception e) {
            try {
                cipher = Cipher.getInstance(CIPHER_MODE_SM4, "BC");
            } catch (Exception ex) {
                throw new RuntimeException("SM4 cipher not available: " + ex.getMessage(), ex);
            }
        }
        GCMParameterSpec spec = new GCMParameterSpec(SM4_GCM_TAG_LENGTH * 8, iv);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, ALGORITHM_SM4);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, spec);

        byte[] encrypted = cipher.doFinal(plainText.getBytes(CHARSET));

        // 组合IV和密文
        byte[] ivAndCiphertext = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, ivAndCiphertext, 0, iv.length);
        System.arraycopy(encrypted, 0, ivAndCiphertext, iv.length, encrypted.length);

        // 返回格式: [keyLength]Base64(iv+ciphertext)
        return "[" + SM4_KEY_LENGTH + "]" + Base64.getEncoder().encodeToString(ivAndCiphertext);
    }

    /**
     * AES/GCM解密
     */
    private String decodeAES(String encryptedText, String key) throws Exception {
        String keyLengthStr = encryptedText.substring(encryptedText.indexOf("[") + 1, encryptedText.indexOf("]"));
        int keyLength = Integer.parseInt(keyLengthStr);
        byte[] ivAndCiphertext = Base64.getDecoder().decode(encryptedText.substring(encryptedText.indexOf("]") + 1));

        // 解析IV和密文
        byte[] iv = new byte[AES_GCM_IV_LENGTH];
        byte[] ciphertext = new byte[ivAndCiphertext.length - AES_GCM_IV_LENGTH];
        System.arraycopy(ivAndCiphertext, 0, iv, 0, AES_GCM_IV_LENGTH);
        System.arraycopy(ivAndCiphertext, AES_GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);

        // 密钥处理 - 读取key长度（以字节为单位）
        byte[] keyBytes = deriveKey(key, keyLength);

        // 解密
        Cipher cipher = Cipher.getInstance(CIPHER_MODE_AES);
        GCMParameterSpec spec = new GCMParameterSpec(AES_GCM_TAG_LENGTH * 8, iv);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, ALGORITHM_AES);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, spec);

        byte[] decrypted = cipher.doFinal(ciphertext);
        return new String(decrypted, CHARSET);
    }

    /**
     * AES/GCM加密
     */
    public static String encodeAES(String plainText, String key, int keySize) throws Exception {
        int keyLength = keySize / 8;
        byte[] keyBytes = deriveKey(key, keyLength);

        // 生成随机IV
        byte[] iv = new byte[AES_GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        // 加密
        Cipher cipher = Cipher.getInstance(CIPHER_MODE_AES);
        GCMParameterSpec spec = new GCMParameterSpec(AES_GCM_TAG_LENGTH * 8, iv);
        SecretKeySpec secretKeySpec = new SecretKeySpec(keyBytes, ALGORITHM_AES);
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, spec);

        byte[] encrypted = cipher.doFinal(plainText.getBytes(CHARSET));

        // 组合IV和密文
        byte[] ivAndCiphertext = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, ivAndCiphertext, 0, iv.length);
        System.arraycopy(encrypted, 0, ivAndCiphertext, iv.length, encrypted.length);

        // 返回格式: [keyLength]Base64(iv+ciphertext)
        byte[] keyLengthBytes = new byte[]{(byte) keyLength};
        return "[" + keyLength + "]" + Base64.getEncoder().encodeToString(ivAndCiphertext);
    }

    /**
     * 密钥派生函数 (PBKDF2)
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
     */
    public static String generateRandomKey(int length) {
        try {
            SecureRandom random = SecureRandom.getInstanceStrong();
            byte[] keyBytes = new byte[length];
            random.nextBytes(keyBytes);
            return Base64.getEncoder().encodeToString(keyBytes);
        } catch (Exception e) {
            throw new RuntimeException("生成密钥失败", e);
        }
    }

    /**
     * AES加密 (使用256位密钥)
     */
    public static String encodeAES(String plainText, String key) throws Exception {
        return encodeAES(plainText, key, AES_KEY_SIZE);
    }

    /**
     * 验证密码 (用于测试)
     */
    public static String decryptPassword(String encryptedPassword, String key, String algorithm) throws Exception {
        DruidEncryptedPasswordCallback callback = new DruidEncryptedPasswordCallback();
        java.util.Properties props = new java.util.Properties();
        props.setProperty(CONFIG_DECRYPT, "true");
        props.setProperty(CONFIG_DECRYPT_KEY, key);
        props.setProperty(CONFIG_DECRYPT_ALGORITHM, algorithm);
        props.setProperty("password", encryptedPassword);
        callback.setProperties(props);
        return new String(callback.getPassword());
    }
}