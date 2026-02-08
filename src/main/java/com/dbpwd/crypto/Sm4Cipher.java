package com.dbpwd.crypto;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * SM4 国密算法加密解密工具类
 *
 * 提供独立的 SM4 加密解密功能，使用 ECB/PKCS5Padding 模式
 * 符合设计文档的规范：直接使用16字节hex编码的SM4密钥
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class Sm4Cipher {

    // SM4 算法参数
    private static final String ALGORITHM_SM4 = "SM4";
    private static final String CIPHER_MODE_SM4 = "SM4/ECB/PKCS5Padding";
    private static final int SM4_KEY_LENGTH = 16; // 16字节 = 128位
    private static final int HEX_KEY_LENGTH = 32; // 32个hex字符

    static {
        // 注册 BouncyCastle Provider（如已注册则跳过）
        if (java.security.Security.getProvider("BC") == null) {
            java.security.Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * SM4 加密（使用hex编码的密钥）
     *
     * @param plainText 原始明文
     * @param hexKey 16字节的hex编码密钥（32字符）
     * @return 加密后的密文（Base64编码）
     * @throws Exception 加密失败
     */
    public static String encrypt(String plainText, String hexKey) throws Exception {
        if (StringUtils.isEmpty(plainText)) {
            throw new IllegalArgumentException("明文不能为空");
        }
        if (StringUtils.isEmpty(hexKey)) {
            throw new IllegalArgumentException("密钥不能为空");
        }

        // 将hex密钥转换为字节数组
        byte[] keyBytes = hexStringToBytes(hexKey);
        if (keyBytes.length != SM4_KEY_LENGTH) {
            throw new IllegalArgumentException("SM4密钥长度不正确，必须为16字节（32个hex字符）");
        }

        // 创建密钥
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM_SM4);

        // 创建 Cipher
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(CIPHER_MODE_SM4);
        } catch (Exception e) {
            cipher = Cipher.getInstance(CIPHER_MODE_SM4, "BC");
        }

        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        // 返回Base64编码的密文
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * SM4 解密（使用hex编码的密钥）
     *
     * @param encryptedText Base64编码的密文
     * @param hexKey 16字节的hex编码密钥（32字符）
     * @return 原始明文
     * @throws Exception 解密失败
     */
    public static String decrypt(String encryptedText, String hexKey) throws Exception {
        if (StringUtils.isEmpty(encryptedText)) {
            throw new IllegalArgumentException("加密密码不能为空");
        }
        if (StringUtils.isEmpty(hexKey)) {
            throw new IllegalArgumentException("密钥不能为空");
        }

        // 将hex密钥转换为字节数组
        byte[] keyBytes = hexStringToBytes(hexKey);
        if (keyBytes.length != SM4_KEY_LENGTH) {
            throw new IllegalArgumentException("SM4密钥长度不正确，必须为16字节（32个hex字符）");
        }

        // 创建密钥
        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM_SM4);

        // 解码Base64密文
        byte[] ciphertext = Base64.getDecoder().decode(encryptedText);

        // 创建 Cipher
        Cipher cipher;
        try {
            cipher = Cipher.getInstance(CIPHER_MODE_SM4);
        } catch (Exception e) {
            cipher = Cipher.getInstance(CIPHER_MODE_SM4, "BC");
        }

        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decrypted = cipher.doFinal(ciphertext);

        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /**
     * Hex字符串转字节数组
     *
     * @param hex hex字符串（32字符）
     * @return 字节数组
     * @throws IllegalArgumentException hex格式错误
     */
    public static byte[] hexStringToBytes(String hex) throws IllegalArgumentException {
        if (hex == null) {
            throw new IllegalArgumentException("hex字符串不能为null");
        }
        hex = hex.trim();
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("hex字符串长度必须为偶数");
        }

        int length = hex.length();
        byte[] result = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            int highDigit = Character.digit(hex.charAt(i), 16);
            int lowDigit = Character.digit(hex.charAt(i + 1), 16);

            if (highDigit < 0 || lowDigit < 0) {
                throw new IllegalArgumentException("hex字符串包含非法字符: " + hex.substring(i, i + 2));
            }

            result[i / 2] = (byte) ((highDigit << 4) + lowDigit);
        }

        return result;
    }

    /**
     * 字节数组转hex字符串
     *
     * @param bytes 字节数组
     * @return hex字符串（大写）
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }

    /**
     * 生成随机密钥（hex格式）
     *
     * 设计文档要求：16字节hex编码（32字符）
     *
     * @return hex格式的密钥字符串（32字符）
     */
    public static String generateRandomHexKey() {
        try {
            SecureRandom random = SecureRandom.getInstanceStrong();
            byte[] keyBytes = new byte[SM4_KEY_LENGTH];
            random.nextBytes(keyBytes);
            return bytesToHex(keyBytes);
        } catch (Exception e) {
            throw new RuntimeException("生成随机密钥失败", e);
        }
    }
}
