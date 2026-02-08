package com.dbpwd.crypto;

/**
 * 密码加密器
 *
 * 提供统一的密码加密接口
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class PasswordEncryptor {

    /**
     * 使用SM4加密密码，并格式化为ENC格式
     *
     * @param plainPassword 明文密码
     * @param sm4Key    SM4密钥，hex编码格式（32字符）
     * @return ENC(SM4:密文)格式的加密密码
     * @throws Exception 加密失败
     */
    public static String encrypt(String plainPassword, String sm4Key) throws Exception {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("明文密码不能为空");
        }
        if (sm4Key == null || sm4Key.isEmpty()) {
            throw new IllegalArgumentException("SM4密钥不能为空");
        }

        // 使用SM4加密
        String cipherText = Sm4Cipher.encrypt(plainPassword, sm4Key);

        // 格式化为ENC格式
        return ENCFormat.formatSM4(cipherText);
    }

    /**
     * 检查密码是否已经是加密格式
     *
     * @param password 密码字符串
     * @return 是否为加密格式
     */
    public static boolean isEncrypted(String password) {
        return ENCFormat.isEncrypted(password);
    }

    /**
     * 加密SM4格式的密码（保持ENC格式）
     *
     * @param encryptedPassword 已加密的ENC格式密码
     * @param sm4Key        SM4密钥
     * @return 重新加密后的ENC格式密码
     * @throws Exception 加密失败
     */
    public static String reencrypt(String encryptedPassword, String sm4Key) throws Exception {
        String cipherText = ENCFormat.extractCipherText(encryptedPassword);
        if (cipherText == null) {
            throw new IllegalArgumentException("密码不是有效的ENC格式");
        }

        // 解密
        String plainPassword = Sm4Cipher.decrypt(cipherText, sm4Key);

        // 重新加密
        return encrypt(plainPassword, sm4Key);
    }
}
