package com.dbpwd.crypto;

import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

/**
 * SM4-GCM认证加密算法（更安全的版本，JDK 8兼容）
 *
 * 算法参数：
 * - 算法名称：SM4
 * - 密钥长度：128位（16字节，hex编码32字符）
 * - 块大小：128位（16字节）
 * - 加密模式：GCM（伽罗瓦计数器模式）- 认证加密
 * - IV长度：12字节（96位，推荐用于GCM）
 * - 认证标签长度：16字节（128位，最大安全级别）
 *
 * GCM模式优势：
 * 1. **认证加密**：同时提供机密性和完整性保护
 * 2. **防篡改**：任何对密文的修改都会导致解密失败
 * 3. **并行处理**：支持并行加密/解密，性能更好
 * 4. **现代标准**：符合NIST SP 800-38D和ISO/IEC 19772标准
 *
 * @author Druid DB Password Manager
 * @version 3.2
 */
public class Sm4GCMCipher {

    /** SM4密钥长度：128位 = 16字节 */
    private static final int SM4_KEY_LENGTH = 16;

    /** GCM IV长度：12字节（96位，NIST推荐值） */
    private static final int GCM_IV_LENGTH = 12;

    /** GCM认证标签长度：16字节（128位，最大安全级别） */
    private static final int GCM_TAG_LENGTH = 16;

    static {
        // 注册Bouncy Castle Provider
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * 加密字符串（使用GCM模式）
     *
     * 输出格式：IV(12字节) + 密文(可变长度) + 认证标签(16字节) 的Base64编码
     *
     * @param plainText 明文字符串
     * @param hexKey    SM4密钥，hex编码格式（32字符）
     * @return Base64编码的密文字符串（包含IV和认证标签）
     * @throws Exception 加密失败
     */
    public static String encrypt(String plainText, String hexKey) throws Exception {
        if (plainText == null || plainText.isEmpty()) {
            throw new IllegalArgumentException("明文不能为空");
        }
        if (hexKey == null || hexKey.isEmpty()) {
            throw new IllegalArgumentException("SM4密钥不能为空");
        }

        byte[] keyBytes = HexUtil.hexStringToBytes(hexKey);
        validateKeyLength(keyBytes);

        // 生成随机IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        byte[] plainBytes = plainText.getBytes("UTF-8");

        // 使用Bouncy Castle的GCM实现
        GCMBlockCipher gcm = new GCMBlockCipher(new SM4Engine());
        AEADParameters parameters = new AEADParameters(
            new KeyParameter(keyBytes),
            GCM_TAG_LENGTH * 8,
            iv
        );

        gcm.init(true, parameters);

        byte[] encrypted = new byte[gcm.getOutputSize(plainBytes.length)];
        int len = gcm.processBytes(plainBytes, 0, plainBytes.length, encrypted, 0);
        len += gcm.doFinal(encrypted, len);

        // 返回 IV + 密文（包含认证标签）的Base64编码
        return Base64.getEncoder().encodeToString(iv) + ":" +
               Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * 解密字符串（使用GCM模式）
     *
     * @param cipherText 密文字符串，格式为：Base64(IV):Base64(密文+认证标签)
     * @param hexKey     SM4密钥，hex编码格式（32字符）
     * @return 解密后的明文字符串
     * @throws Exception 解密失败或认证失败
     */
    public static String decrypt(String cipherText, String hexKey) throws Exception {
        if (cipherText == null || cipherText.isEmpty()) {
            throw new IllegalArgumentException("密文不能为空");
        }
        if (hexKey == null || hexKey.isEmpty()) {
            throw new IllegalArgumentException("SM4密钥不能为空");
        }

        // 解析密文格式：IV:encryptedData
        String[] parts = cipherText.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("密文格式不正确，应为 Base64(IV):Base64(密文)");
        }

        byte[] iv = Base64.getDecoder().decode(parts[0]);
        byte[] encrypted = Base64.getDecoder().decode(parts[1]);

        if (iv.length != GCM_IV_LENGTH) {
            throw new IllegalArgumentException("IV长度不正确，必须为" + GCM_IV_LENGTH + "字节");
        }

        byte[] keyBytes = HexUtil.hexStringToBytes(hexKey);
        validateKeyLength(keyBytes);

        // 使用Bouncy Castle的GCM实现
        GCMBlockCipher gcm = new GCMBlockCipher(new SM4Engine());
        AEADParameters parameters = new AEADParameters(
            new KeyParameter(keyBytes),
            GCM_TAG_LENGTH * 8,
            iv
        );

        gcm.init(false, parameters);

        byte[] decrypted = new byte[gcm.getOutputSize(encrypted.length)];
        int len = gcm.processBytes(encrypted, 0, encrypted.length, decrypted, 0);
        len += gcm.doFinal(decrypted, len);

        return new String(decrypted, 0, len, "UTF-8");
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
        return HexUtil.bytesToHex(key);
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
        return "SM4/GCM/NoPadding";
    }

    /**
     * 获取GCM IV长度
     *
     * @return IV长度（字节数）
     */
    public static int getIvLength() {
        return GCM_IV_LENGTH;
    }

    /**
     * 获取GCM认证标签长度
     *
     * @return 认证标签长度（字节数）
     */
    public static int getTagLength() {
        return GCM_TAG_LENGTH;
    }
}

/**
 * Hex编码/解码工具类
 */
class HexUtil {
    /**
     * 将hex字符串转换为字节数组
     *
     * @param hex hex字符串
     * @return 字节数组
     */
    public static byte[] hexStringToBytes(String hex) {
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
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
