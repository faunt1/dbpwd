package com.dbpwd.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

/**
 * SM4国密算法加密解密工具
 *
 * 算法参数：
 * - 算法名称：SM4
 * - 密钥长度：128位（16字节，hex编码32字符）
 * - 块大小：128位（16字节）
 * - 加密模式：ECB（电子密码本模式）
 * - 填充方式：PKCS7Padding
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class Sm4Cipher {

    /** SM4算法名称 */
    private static final String ALGORITHM_SM4 = "SM4";

    /** 加密模式：ECB + PKCS7Padding */
    private static final String TRANSFORMATION = "SM4/ECB/PKCS7Padding";

    /** SM4密钥长度：128位 = 16字节 */
    private static final int SM4_KEY_LENGTH = 16;

    /** Bouncy Castle Provider名称 */
    private static final String BC_PROVIDER = "BC";

    static {
        // 注册Bouncy Castle Provider
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * 加密字符串
     *
     * @param plainText 明文字符串
     * @param hexKey    SM4密钥，hex编码格式（32字符）
     * @return Base64编码的密文字符串
     * @throws Exception 加密失败
     */
    public static String encrypt(String plainText, String hexKey) throws Exception {
        if (plainText == null || plainText.isEmpty()) {
            throw new IllegalArgumentException("明文不能为空");
        }
        if (hexKey == null || hexKey.isEmpty()) {
            throw new IllegalArgumentException("SM4密钥不能为空");
        }

        byte[] keyBytes = hexStringToBytes(hexKey);
        validateKeyLength(keyBytes);

        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM_SM4);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION, BC_PROVIDER);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);

        byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * 解密字符串
     *
     * @param cipherText Base64编码的密文字符串
     * @param hexKey    SM4密钥，hex编码格式（32字符）
     * @return 解密后的明文字符串
     * @throws Exception 解密失败
     */
    public static String decrypt(String cipherText, String hexKey) throws Exception {
        if (cipherText == null || cipherText.isEmpty()) {
            throw new IllegalArgumentException("密文不能为空");
        }
        if (hexKey == null || hexKey.isEmpty()) {
            throw new IllegalArgumentException("SM4密钥不能为空");
        }

        byte[] keyBytes = hexStringToBytes(hexKey);
        validateKeyLength(keyBytes);

        SecretKeySpec secretKey = new SecretKeySpec(keyBytes, ALGORITHM_SM4);
        Cipher cipher = Cipher.getInstance(TRANSFORMATION, BC_PROVIDER);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);

        byte[] decoded = Base64.getDecoder().decode(cipherText);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted, "UTF-8");
    }

    /**
     * 生成随机的SM4密钥
     *
     * @return hex编码的密钥字符串（32字符）
     */
    public static String generateRandomHexKey() {
        byte[] key = new byte[SM4_KEY_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(key);
        return bytesToHex(key);
    }

    /**
     * 验证密钥格式是否有效
     *
     * @param hexKey hex编码的密钥字符串
     * @return 是否有效
     */
    public static boolean isValidKey(String hexKey) {
        if (hexKey == null || hexKey.length() != SM4_KEY_LENGTH * 2) {
            return false;
        }
        // 检查是否都为有效的hex字符
        for (int i = 0; i < hexKey.length(); i++) {
            char c = hexKey.charAt(i);
            int digit = Character.digit(c, 16);
            if (digit == -1) {
                return false;
            }
        }
        return true;
    }

    /**
     * 将hex字符串转换为字节数组
     *
     * @param hex hex字符串
     * @return 字节数组
     */
    private static byte[] hexStringToBytes(String hex) {
        if (hex == null || hex.isEmpty()) {
            throw new IllegalArgumentException("hex字符串不能为空");
        }
        if (hex.length() % 2 != 0) {
            throw new IllegalArgumentException("hex字符串长度必须为偶数");
        }

        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                               + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    /**
     * 将字节数组转换为hex字符串
     *
     * @param bytes 字节数组
     * @return hex字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * 验证密钥长度
     *
     * @param keyBytes 密钥字节数组
     * @throws IllegalArgumentException 密钥长度不正确时抛出异常
     */
    private static void validateKeyLength(byte[] keyBytes) {
        if (keyBytes.length != SM4_KEY_LENGTH) {
            throw new IllegalArgumentException(
                "SM4密钥长度不正确，必须为16字节（32个hex字符），当前为" + keyBytes.length + "字节");
        }
    }

    /**
     * 获取SM4密钥长度
     *
     * @return 密钥长度（字节数）
     */
    public static int getKeyLength() {
        return SM4_KEY_LENGTH;
    }

    /**
     * 获取加密模式
     *
     * @return 加密模式字符串
     */
    public static String getTransformation() {
        return TRANSFORMATION;
    }
}
