package com.dbpwd.crypto;

import com.dbpwd.model.ENCFormat;
import com.dbpwd.model.ENCFormat.EncryptedInfo;
import com.dbpwd.model.PasswordFormat;

import java.util.logging.Logger;

/**
 * 密码解密器
 *
 * 提供统一的密码解密入口，支持多种加密格式
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class PasswordDecryptor {

    private static final Logger logger = Logger.getLogger(PasswordDecryptor.class.getName());

    /**
     * 解密密码（自动检测格式）
     *
     * @param encryptedPassword 加密密码
     * @param sm4Key SM4密钥
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

        // 检查格式
        PasswordFormat format = PasswordFormat.detectFormat(encryptedPassword);

        switch (format) {
            case SM4_STANDARD:
                return decryptSM4Standard(encryptedPassword, sm4Key);

            case DRUID_ENCRYPTED:
                return decryptDruidEncrypted(encryptedPassword, sm4Key);

            case PLAIN:
                // PLAIN前缀，直接返回明文
                return encryptedPassword.substring(6);

            default:
                // 检查是否为ENC格式
                if (ENCFormat.isEncrypted(encryptedPassword)) {
                    return decryptENC(encryptedPassword, sm4Key);
                }

                // 无法识别的格式，假设为明文
                logger.warning("密码格式无法识别，假设为明文");
                return encryptedPassword;
        }
    }

    /**
     * 解密SM4标准格式密码（[16]Base64...）
     *
     * @param encryptedPassword 加密密码
     * @param sm4Key SM4密钥
     * @return 明文密码
     * @throws Exception 解密失败
     */
    public static String decryptSM4Standard(String encryptedPassword, String sm4Key) throws Exception {
        return Sm4Cipher.decrypt(encryptedPassword, sm4Key);
    }

    /**
     * 解密Druid ENC格式密码（ENC_...）
     *
     * @param encryptedPassword 加密密码
     * @param sm4Key SM4密钥
     * @return 明文密码
     * @throws Exception 解密失败
     */
    public static String decryptDruidEncrypted(String encryptedPassword, String sm4Key) throws Exception {
        if (!encryptedPassword.startsWith("ENC_")) {
            throw new IllegalArgumentException("不是Druid ENC格式");
        }

        String cipherText = encryptedPassword.substring(4); // 去除ENC_前缀
        return Sm4Cipher.decrypt(cipherText, sm4Key);
    }

    /**
     * 解密ENC格式密码（ENC(算法:密文)）
     *
     * @param encryptedPassword 加密密码
     * @param sm4Key SM4密钥
     * @return 明文密码
     * @throws Exception 解密失败
     */
    public static String decryptENC(String encryptedPassword, String sm4Key) throws Exception {
        try {
            if (!ENCFormat.isEncrypted(encryptedPassword)) {
                throw new IllegalArgumentException("不是ENC格式");
            }

            EncryptedInfo info = ENCFormat.parse(encryptedPassword);
            if (info == null) {
                throw new IllegalArgumentException("无法解析ENC格式");
            }

            String algorithm = info.getAlgorithm();
            String cipherText = info.getCipherText();

            switch (algorithm) {
                case "SM4":
                    return Sm4Cipher.decrypt(cipherText, sm4Key);

                // 预留其他算法
                case "AES":
                    // 未来支持
                    throw new UnsupportedOperationException("AES算法暂未实现");

                default:
                    throw new IllegalArgumentException("不支持的加密算法: " + algorithm);
            }
        } catch (Exception e) {
            logger.severe("解密ENC格式密码失败: " + e.getMessage());
            throw e;
        }
    }

    /**
     * 尝试解密，失败时返回原值
     *
     * @param encryptedPassword 加密密码
     * @param sm4Key SM4密钥
     * @return 明文密码，解密失败返回原值
     */
    public static String tryDecrypt(String encryptedPassword, String sm4Key) {
        try {
            return decrypt(encryptedPassword, sm4Key);
        } catch (Exception e) {
            logger.warning("解密失败，返回原值: " + e.getMessage());
            return encryptedPassword;
        }
    }

    /**
     * 批量解密
     *
     * @param encryptedPasswords 加密密码数组
     * @param sm4Key SM4密钥
     * @return 明文密码数组
     */
    public static String[] decryptBatch(String[] encryptedPasswords, String sm4Key) {
        String[] result = new String[encryptedPasswords.length];

        for (int i = 0; i < encryptedPasswords.length; i++) {
            try {
                result[i] = decrypt(encryptedPasswords[i], sm4Key);
            } catch (Exception e) {
                logger.warning("解密第" + (i + 1) + "个密码失败: " + e.getMessage());
                result[i] = encryptedPasswords[i];
            }
        }

        return result;
    }

    /**
     * 检查密码是否为加密格式
     *
     * @param password 密码
     * @return true=加密格式，false=明文
     */
    public static boolean isEncrypted(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }

        PasswordFormat format = PasswordFormat.detectFormat(password);
        return format == PasswordFormat.SM4_STANDARD ||
               format == PasswordFormat.DRUID_ENCRYPTED ||
               ENCFormat.isEncrypted(password);
    }

    /**
     * 识别密码格式
     *
     * @param password 密码
     * @return 格式描述
     */
    public static String identifyFormat(String password) {
        if (password == null || password.isEmpty()) {
            return "EMPTY";
        }

        PasswordFormat format = PasswordFormat.detectFormat(password);
        return format.name();
    }

    /**
     * 解密并提供详细信息（用于日志/调试）
     *
     * @param encryptedPassword 加密密码
     * @param sm4Key SM4密钥
     * @return 解密详情
     */
    public static DecryptDetail decryptWithDetail(String encryptedPassword, String sm4Key) {
        DecryptDetail detail = new DecryptDetail();
        detail.setEncryptedPassword(encryptedPassword);
        detail.setFormat(identifyFormat(encryptedPassword));
        detail.setEncrypted(isEncrypted(encryptedPassword));

        long startTime = System.currentTimeMillis();

        try {
            String plainPassword = decrypt(encryptedPassword, sm4Key);
            detail.setPlainPassword(plainPassword);
            detail.setSuccess(true);
            detail.setDuration(System.currentTimeMillis() - startTime);
            detail.setMessage("解密成功");
        } catch (Exception e) {
            detail.setSuccess(false);
            detail.setDuration(System.currentTimeMillis() - startTime);
            detail.setMessage("解密失败: " + e.getMessage());
            detail.setError(e);
        }

        return detail;
    }

    /**
     * 解密详情
     */
    public static class DecryptDetail {
        private String encryptedPassword;
        private String plainPassword;
        private String format;
        private boolean encrypted;
        private boolean success;
        private long duration;
        private String message;
        private Throwable error;

        public String getEncryptedPassword() {
            return encryptedPassword;
        }

        public void setEncryptedPassword(String encryptedPassword) {
            this.encryptedPassword = encryptedPassword;
        }

        public String getPlainPassword() {
            return plainPassword;
        }

        public void setPlainPassword(String plainPassword) {
            this.plainPassword = plainPassword;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }

        public boolean isEncrypted() {
            return encrypted;
        }

        public void setEncrypted(boolean encrypted) {
            this.encrypted = encrypted;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public long getDuration() {
            return duration;
        }

        public void setDuration(long duration) {
            this.duration = duration;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Throwable getError() {
            return error;
        }

        public void setError(Throwable error) {
            this.error = error;
        }

        @Override
        public String toString() {
            return String.format("DecryptDetail{format='%s', encrypted=%b, success=%b, duration=%dms, message='%s'}",
                format, encrypted, success, duration, message);
        }
    }

    /**
     * 验证密钥格式
     *
     * @param sm4Key SM4密钥
     * @return true=有效，false=无效
     */
    public static boolean isValidKey(String sm4Key) {
        if (sm4Key == null || sm4Key.isEmpty()) {
            return false;
        }

        // hex编码的密钥应该为32字符（16字节）
        if (sm4Key.length() == 32) {
            // 检查是否为hex字符串
            return sm4Key.matches("[0-9A-Fa-f]{32}");
        }

        // Base64编码的密钥
        try {
            byte[] decoded = java.util.Base64.getDecoder().decode(sm4Key);
            return decoded.length == 16;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}