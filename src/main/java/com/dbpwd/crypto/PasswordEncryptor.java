package com.dbpwd.crypto;

/**
 * 密码加密器
 *
 * 提供统一的密码加密接口，使用SM4-GCM认证加密
 *
 * GCM模式优势：
 * - 认证加密：同时提供机密性和完整性保护
 * - 防篡改：任何对密文的修改都会导致解密失败
 * - 随机IV：每次加密结果不同，防侧信道攻击
 * - 并行处理：支持并行加密/解密，性能更优
 *
 * @author Druid DB Password Manager
 * @version 4.0
 */
public class PasswordEncryptor {

    /**
     * 使用SM4-GCM加密密码，并格式化为ENC格式
     *
     * GCM模式提供认证加密，同时保护机密性和完整性：
     * - 使用12字节随机IV（初始化向量）
     * - 使用16字节认证标签（完整性验证）
     * - 每次加密结果不同，防模式分析和侧信道攻击
     *
     * @param plainPassword 明文密码
     * @param sm4Key    SM4密钥，hex编码格式（32字符）
     * @return ENC(SM4-GCM:IV:密文)格式的加密密码
     * @throws Exception 加密失败
     */
    public static String encrypt(String plainPassword, String sm4Key) throws Exception {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("明文密码不能为空");
        }
        if (sm4Key == null || sm4Key.isEmpty()) {
            throw new IllegalArgumentException("SM4密钥不能为空");
        }

        // 使用SM4-GCM加密（提供认证加密和防篡改保护）
        String cipherText = Sm4GCMCipher.encrypt(plainPassword, sm4Key);

        // 格式化为ENC格式
        return ENCFormat.formatSM4GCM(cipherText);
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
     * 将旧的ECB加密密文迁移到GCM
     *
     * 用于将历史数据从ECB安全迁移到GCM
     *
     * @param oldSm4Password 旧的ENC(SM4:密文)格式密码
     * @param sm4Key        SM4密钥
     * @return ENC(SM4-GCM:...)格式的加密密码
     * @throws Exception 加密失败
     */
    public static String migrateECBToGCM(String oldSm4Password, String sm4Key) throws Exception {
        // 验证是否为ENC格式
        if (!ENCFormat.isEncrypted(oldSm4Password)) {
            throw new IllegalArgumentException("密码不是有效的ENC格式");
        }

        // 提取算法
        String algorithm = ENCFormat.extractAlgorithm(oldSm4Password);
        if (algorithm == null) {
            throw new IllegalArgumentException("无法提取加密算法");
        }

        // 验证算法为SM4（ECB格式）
        if (!"SM4".equalsIgnoreCase(algorithm)) {
            throw new IllegalArgumentException("输入密码必须为SM4-ECB格式，当前算法: " + algorithm);
        }

        // 提取密文
        String cipherText = ENCFormat.extractCipherText(oldSm4Password);
        if (cipherText == null || cipherText.isEmpty()) {
            throw new IllegalArgumentException("无效的密文");
        }

        // 解密ECB加密的密码
        String plainPassword = Sm4Cipher.decrypt(cipherText, sm4Key);

        // 使用GCM重新加密
        return encrypt(plainPassword, sm4Key);
    }
}
