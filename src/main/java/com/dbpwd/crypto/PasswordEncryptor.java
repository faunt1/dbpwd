package com.dbpwd.crypto;

import com.dbpwd.model.ENCFormat;
import com.dbpwd.model.PasswordFormat;

import java.io.Console;
import java.util.logging.Logger;

/**
 * 密码加密器
 *
 * 提供统一的密码加密入口，支持多种加密格式
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class PasswordEncryptor {

    private static final Logger logger = Logger.getLogger(PasswordEncryptor.class.getName());

    /**
     * 加密密码为SM4密文（ENC格式）
     *
     * @param plainPassword 明文密码
     * @param sm4Key SM4密钥
     * @return ENC格式加密密码
     * @throws Exception 加密失败
     */
    public static String encryptSM4ENC(String plainPassword, String sm4Key) throws Exception {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("明文密码不能为空");
        }
        if (sm4Key == null || sm4Key.isEmpty()) {
            throw new IllegalArgumentException("SM4密钥不能为空");
        }

        String cipherText = Sm4Cipher.encrypt(plainPassword, sm4Key);
        return ENCFormat.formatSM4(cipherText);
    }

    /**
     * 加密密码为SM4标准格式（[16]Base64...）
     *
     * @param plainPassword 明文密码
     * @param sm4Key SM4密钥
     * @return SM4标准格式加密密码
     * @throws Exception 加密失败
     */
    public static String encryptSM4Standard(String plainPassword, String sm4Key) throws Exception {
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("明文密码不能为空");
        }
        if (sm4Key == null || sm4Key.isEmpty()) {
            throw new IllegalArgumentException("SM4密钥不能为空");
        }

        return Sm4Cipher.encrypt(plainPassword, sm4Key);
    }

    /**
     * 自动检测并加密为ENC格式
     *
     * @param plainPassword 明文密码
     * @param sm4Key SM4密钥
     * @return 加密密码（ENC格式）
     * @throws Exception 加密失败
     */
    public static String encrypt(String plainPassword, String sm4Key) throws Exception {
        return encryptSM4ENC(plainPassword, sm4Key);
    }

    /**
     * 交互式读取密码（隐藏输入）
     *
     * @param prompt 提示信息
     * @return 输入的密码
     */
    public static String readPasswordInteractive(String prompt) {
        Console console = System.console();
        if (console != null) {
            char[] passwordChars = console.readPassword(prompt);
            return new String(passwordChars);
        } else {
            // 回退到普通输入（不隐藏）
            System.out.print(prompt);
            return new java.util.Scanner(System.in).nextLine();
        }
    }

    /**
     * 验证密码强度
     *
     * @param password 密码
     * @return 验证结果
     */
    public static PasswordStrength validatePassword(String password) {
        if (password == null || password.isEmpty()) {
            return new PasswordStrength(PasswordStrengthLevel.EMPTY, "密码为空");
        }

        int score = 0;
        StringBuilder issues = new StringBuilder();

        // 检查长度
        if (password.length() >= 8) {
            score += 20;
        } else {
            issues.append("密码长度不足8位; ");
        }

        if (password.length() >= 12) {
            score += 10;
        }

        // 检查大写字母
        if (password.matches(".*[A-Z].*")) {
            score += 15;
        } else {
            issues.append("缺少大写字母; ");
        }

        // 检查小写字母
        if (password.matches(".*[a-z].*")) {
            score += 15;
        } else {
            issues.append("缺少小写字母; ");
        }

        // 检查数字
        if (password.matches(".*\\d.*")) {
            score += 15;
        } else {
            issues.append("缺少数字; ");
        }

        // 检查特殊字符
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) {
            score += 15;
        } else {
            issues.append("缺少特殊字符; ");
        }

        // 检查常见弱密码
        String[] weakPasswords = {"password", "123456", "12345678", "qwerty", "abc123"};
        for (String weak : weakPasswords) {
            if (password.toLowerCase().contains(weak)) {
                score -= 30;
                issues.append("包含弱密码模式; ");
                break;
            }
        }

        if (score < 40) {
            return new PasswordStrength(PasswordStrengthLevel.WEAK, issues.toString());
        } else if (score < 70) {
            return new PasswordStrength(PasswordStrengthLevel.MEDIUM, issues.toString());
        } else if (score < 85) {
            return new PasswordStrength(PasswordStrengthLevel.STRONG,
                issues.length() > 0 ? issues.toString() : "密码强度良好");
        } else {
            return new PasswordStrength(PasswordStrengthLevel.VERY_STRONG, "密码强度优秀");
        }
    }

    /**
     * 密码强度等级
     */
    public enum PasswordStrengthLevel {
        EMPTY,
        WEAK,
        MEDIUM,
        STRONG,
        VERY_STRONG
    }

    /**
     * 密码强度结果
     */
    public static class PasswordStrength {
        private final PasswordStrengthLevel level;
        private final String message;

        public PasswordStrength(PasswordStrengthLevel level, String message) {
            this.level = level;
            this.message = message;
        }

        public PasswordStrengthLevel getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }

        public boolean isStrong() {
            return level == PasswordStrengthLevel.STRONG ||
                   level == PasswordStrengthLevel.VERY_STRONG;
        }

        @Override
        public String toString() {
            return String.format("[%s] %s", level.name(), message);
        }
    }

    /**
     * 检查是否为ENC格式
     */
    public static boolean isENCFormat(String password) {
        return ENCFormat.isEncrypted(password);
    }

    /**
     * 检查是否为Druid ENC格式
     */
    public static boolean isDruidFormat(String password) {
        return password != null && password.startsWith("ENC(");
    }

    /**
     * 检查是否为SM4标准格式
     */
    public static boolean isSM4StandardFormat(String password) {
        return PasswordFormat.SM4_STANDARD == PasswordFormat.detectFormat(password);
    }

    /**
     * 检查是否为明文格式
     */
    public static boolean isPlainText(String password) {
        return PasswordFormat.PLAIN == PasswordFormat.detectFormat(password);
    }

    /**
     * 隐藏密码用于日志（保留部分字符）
     *
     * @param password 密码
     * @return 隐藏后的密码
     */
    public static String maskPassword(String password) {
        if (password == null || password.isEmpty()) {
            return "***";
        }

        int len = password.length();
        if (len <= 4) {
            return "***";
        }

        // 显示前2位和后2位，中间用*代替
        return password.substring(0, 2) + "*****" + password.substring(len - 2);
    }

    /**
     * 检查密码是否已加密
     *
     * @param password 密码
     * @return true=已加密，false=明文
     */
    public static boolean isEncrypted(String password) {
        PasswordFormat format = PasswordFormat.detectFormat(password);
        return format == PasswordFormat.SM4_STANDARD ||
               format == PasswordFormat.DRUID_ENCRYPTED ||
               ENCFormat.isEncrypted(password);
    }

    /**
     * 获取密码格式描述
     *
     * @param password 密码
     * @return 格式描述
     */
    public static String getFormatDescription(String password) {
        PasswordFormat format = PasswordFormat.detectFormat(password);
        return format.getDescription();
    }
}