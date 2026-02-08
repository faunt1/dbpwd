package com.dbpwd.crypto;

/**
 * 密码解密器
 *
 * 提供统一的密码解密接口
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class PasswordDecryptor {

    /**
     * 解密ENC格式的加密密码
     *
     * @param encryptedPassword 加密密码（ENC格式）
     * @param sm4Key        SM4密钥，hex编码格式（32字符）
     * @return 明文密码
     * @throws Exception 解密失败
     */
    public static String decrypt(String encryptedPassword, String sm4Key) throws Exception {
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            throw new IllegalArgumentException("加密密码不能为空");
        }
        if (sm4Key == null || sm4Key.isEmpty()) {
            throw new IllegalArgumentException("SM4密钥不能为空");
        }

        // 检查是否为加密格式
        if (!ENCFormat.isEncrypted(encryptedPassword)) {
            // 如果不是加密格式，直接返回原文
            return encryptedPassword;
        }

        // 提取算法和密文
        String algorithm = ENCFormat.extractAlgorithm(encryptedPassword);
        String cipherText = ENCFormat.extractCipherText(encryptedPassword);

        if (algorithm == null || cipherText == null) {
            throw new IllegalArgumentException("密码格式不正确，应为ENC(算法:密文)格式");
        }

        // 根据算法解密
        if ("SM4".equalsIgnoreCase(algorithm)) {
            return Sm4Cipher.decrypt(cipherText, sm4Key);
        } else {
            throw new UnsupportedOperationException("不支持的加密算法: " + algorithm);
        }
    }

    /**
     * 自动检测并解密密码
     *
     * 如果密码是ENC格式则解密，否则直接返回原文
     *
     * @param password 密码字符串（可能是加密或明文）
     * @param sm4Key SM4密钥，hex编码格式（32字符）
     * @return 明文密码
     * @throws Exception 解密失败
     */
    public static String autoDecrypt(String password, String sm4Key) throws Exception {
        if (password == null || password.isEmpty()) {
            return password;
        }

        if (ENCFormat.isEncrypted(password)) {
            return decrypt(password, sm4Key);
        }
        return password;
    }
}
