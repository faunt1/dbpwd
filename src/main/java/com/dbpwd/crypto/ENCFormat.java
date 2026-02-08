package com.dbpwd.crypto;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ENC格式解析器
 *
 * 支持格式：ENC(算法:密文)
 * 示例：ENC(SM4:AABBCCDD1234567890ABCDEF12345678)
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class ENCFormat {

    private static final Pattern ENC_PREFIX_PATTERN = Pattern.compile("^ENC\\(([A-Z0-9]+):(.*)\\)$", Pattern.DOTALL);

    /**
     * 检查密码是否为加密格式
     *
     * @param password 密码字符串
     * @return 是否为加密格式
     */
    public static boolean isEncrypted(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        return password.startsWith("ENC(");
    }

    /**
     * 提取算法名称
     *
     * @param password 密码字符串
     * @return 算法名称，如果不是加密格式返回null
     */
    public static String extractAlgorithm(String password) {
        if (password == null || password.isEmpty()) {
            return null;
        }

        Matcher matcher = ENC_PREFIX_PATTERN.matcher(password);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 提取密文
     *
     * @param password 密码字符串
     * @return 密文，如果不是加密格式返回null
     */
    public static String extractCipherText(String password) {
        if (password == null || password.isEmpty()) {
            return null;
        }

        Matcher matcher = ENC_PREFIX_PATTERN.matcher(password);
        if (matcher.matches()) {
            return matcher.group(2);
        }
        return null;
    }

    /**
     * 格式化为加密格式
     *
     * @param algorithm 算法名称
     * @param cipherText 密文
     * @return 格式化后的加密密码
     */
    public static String formatEncrypted(String algorithm, String cipherText) {
        if (algorithm == null || cipherText == null) {
            throw new IllegalArgumentException("算法和密文不能为null");
        }
        return String.format("ENC(%s:%s)", algorithm, cipherText);
    }

    /**
     * 格式化为SM4加密格式
     *
     * @param cipherText 密文
     * @return 格式化后的加密密码
     */
    public static String formatSM4(String cipherText) {
        return formatEncrypted("SM4", cipherText);
    }

    /**
     * 检查是否为SM4加密格式
     *
     * @param password 密码字符串
     * @return 是否为SM4加密格式
     */
    public static boolean isSM4Encrypted(String password) {
        return "SM4".equals(extractAlgorithm(password));
    }

    /**
     * 验证加密格式是否有效
     *
     * @param password 密码字符串
     * @return 是否有效
     */
    public static boolean isValid(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }
        return ENC_PREFIX_PATTERN.matcher(password).matches();
    }
}
