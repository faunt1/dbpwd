package com.dbpwd.model;

import java.util.regex.Pattern;

/**
 * 密码格式枚举
 *
 * 支持的密码格式：
 * 1. SM4标准格式：[16]Base64(IV+Ciphertext)
 * 2. Druid加密格式：ENC(xxxxxxxxxxxxx)
 * 3. 明文格式：PLAIN_xxxxxxx
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public enum PasswordFormat {
    /**
     * SM4标准格式：[16]Base64(IV+Ciphertext)
     * 示例：[16]r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=
     */
    SM4_STANDARD("\\[\\d+\\][A-Za-z0-9+/]+=*", "SM4标准格式"),

    /**
     * Druid加密格式：ENC(xxxxxxxxxxxxx)
     * 示例：ENC(r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=)
     */
    DRUID_ENCRYPTED("ENC\\([A-Za-z0-9+/]+={0,2}\\)", "Jasypt风格ENC格式"),

    /**
     * 明文格式：PLAIN_xxxxxxx
     * 示例：PLAIN_myPassword123
     */
    PLAIN("PLAIN_.*", "明文格式"),

    /**
     * 未知格式
     */
    UNKNOWN("", "未知格式");

    private final Pattern pattern;
    private final String description;

    PasswordFormat(String regex, String description) {
        this.pattern = regex.isEmpty() ? Pattern.compile(".*") : Pattern.compile(regex);
        this.description = description;
    }

    /**
     * 格式化密码为加密格式（SM4标准）
     *
     * @param encryptedPassword 加密后的密码
     * @param keyLength 密钥长度（字节）
     * @return 格式化后的密码
     */
    public String formatEncryptedPassword(String encryptedPassword, int keyLength) {
        if (this == SM4_STANDARD) {
            // 已经是标准格式，直接返回
            if (encryptedPassword.matches(pattern.pattern())) {
                return encryptedPassword;
            }
            // 转换为标准格式
            return "[" + keyLength + "]" + encryptedPassword;
        } else if (this == DRUID_ENCRYPTED) {
            if (encryptedPassword.startsWith("ENC(")) {
                return encryptedPassword;
            }
            return "ENC(" + encryptedPassword + ")";
        } else if (this == PLAIN) {
            if (encryptedPassword.startsWith("PLAIN_")) {
                return encryptedPassword;
            }
            return "PLAIN_" + encryptedPassword;
        }
        return encryptedPassword;
    }

    /**
     * 提取密码实际内容（去除格式前缀）
     *
     * @param passwordWithFormat 带格式的密码
     * @return 实际密码内容
     */
    public String extractPassword(String passwordWithFormat) {
        if (passwordWithFormat == null) {
            return null;
        }
        if (this == SM4_STANDARD) {
            // 提取 [16]后面Base64部分
            int closeBracket = passwordWithFormat.indexOf(']');
            if (closeBracket > 0 && closeBracket < passwordWithFormat.length() - 1) {
                return passwordWithFormat.substring(closeBracket + 1);
            }
            return passwordWithFormat;
        } else if (this == DRUID_ENCRYPTED) {
            if (passwordWithFormat.startsWith("ENC(") && passwordWithFormat.endsWith(")")) {
                return passwordWithFormat.substring(4, passwordWithFormat.length() - 1);
            }
            return passwordWithFormat;
        } else if (this == PLAIN) {
            if (passwordWithFormat.startsWith("PLAIN_")) {
                return passwordWithFormat.substring(6);
            }
            return passwordWithFormat;
        }
        return passwordWithFormat;
    }

    /**
     * 识别密码格式
     *
     * @param password 密码字符串
     * @return 密码格式
     */
    public static PasswordFormat detectFormat(String password) {
        if (password == null || password.isEmpty()) {
            return UNKNOWN;
        }

        for (PasswordFormat format : values()) {
            if (format == UNKNOWN) {
                continue;
            }
            if (format.pattern.matcher(password).matches()) {
                return format;
            }
        }

        return UNKNOWN;
    }

    /**
     * 检查密码是否为明文格式
     *
     * @param password 密码字符串
     * @return 是否为明文格式
     */
    public static boolean isPlainText(String password) {
        return PLAIN == detectFormat(password);
    }

    /**
     * 检查密码是否为加密格式
     *
     * @param password 密码字符串
     * @return 是否为加密格式
     */
    public static boolean isEncrypted(String password) {
        PasswordFormat format = detectFormat(password);
        return format == SM4_STANDARD || format == DRUID_ENCRYPTED;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public String getDescription() {
        return description;
    }
}
