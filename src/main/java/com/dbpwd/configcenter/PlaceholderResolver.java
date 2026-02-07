package com.dbpwd.configcenter;

import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Placeholder 解析器
 * 负责解析和替换配置模板中的 placeholder 变量
 *
 * 支持的 placeholder 语法：
 * - @config(uid) - 密码 placeholder，从密码映射中查找
 * - @key(keyId) - 密钥 placeholder，从密钥映射中查找
 * - ${env:VAR_NAME} - 环境变量 placeholder
 * - ${prop:system.property.name} - 系统属性 placeholder
 */
public class PlaceholderResolver {

    private static final Logger logger = Logger.getLogger(PlaceholderResolver.class.getName());

    // Placeholder 匹配模式
    private static final Pattern CONFIG_PATTERN = Pattern.compile("@config\\(([^)]+)\\)");
    private static final Pattern KEY_PATTERN = Pattern.compile("@key\\(([^)]+)\\)");
    private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{env:([^}]+)\\}");
    private static final Pattern PROP_PATTERN = Pattern.compile("\\$\\{prop:([^}]+)\\}");

    private final Map<String, String> passwordMap;
    private final Map<String, String> keyMap;

    /**
     * 构造函数（只有密码映射）
     *
     * @param passwordMap 密码映射（uid -> encryptedPassword）
     */
    public PlaceholderResolver(Map<String, String> passwordMap) {
        this.passwordMap = passwordMap;
        this.keyMap = null;
    }

    /**
     * 构造函数（密码映射和密钥映射）
     *
     * @param passwordMap 密码映射（uid -> encryptedPassword）
     * @param keyMap 密钥映射（keyId -> key）
     */
    public PlaceholderResolver(Map<String, String> passwordMap, Map<String, String> keyMap) {
        this.passwordMap = passwordMap;
        this.keyMap = keyMap;
    }

    /**
     * 解析 Properties 中的所有 placeholder
     *
     * @param properties 配置属性
     */
    public void resolvePlaceholders(Properties properties) {
        if (properties == null) {
            return;
        }

        for (String key : properties.stringPropertyNames()) {
            String value = properties.getProperty(key);
            String resolvedValue = resolveValue(value);
            properties.setProperty(key, resolvedValue);

            if (!value.equals(resolvedValue)) {
                logger.fine("Placeholder 替换: " + key + " = " + resolvedValue);
            }
        }
    }

    /**
     * 解析单个值中的 placeholder
     *
     * @param value 原始值
     * @return 替换后的值
     */
    public String resolveValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        String result = value;

        // 1. 替换 @config(uid) placeholder
        result = resolveConfigPlaceholder(result);

        // 2. 替换 @key(keyId) placeholder
        result = resolveKeyPlaceholder(result);

        // 3. 替换 ${env:VAR_NAME} placeholder
        result = resolveEnvPlaceholder(result);

        // 4. 替换 ${prop:system.property.name} placeholder
        result = resolvePropPlaceholder(result);

        return result;
    }

    /**
     * 替换 @config(uid) placeholder
     *
     * @param value 原始值
     * @return 替换后的值
     */
    private String resolveConfigPlaceholder(String value) {
        Matcher matcher = CONFIG_PATTERN.matcher(value);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String uid = matcher.group(1);
            String encryptedPassword = passwordMap.get(uid);

            if (encryptedPassword == null || encryptedPassword.isEmpty()) {
                throw new IllegalArgumentException(
                    "未找到 uid: " + uid + " 对应的密码。请检查密码配置文件。");
            }

            logger.fine("密码 placeholder 替换: @config(" + uid + ") -> [***HIDDEN***]");
            matcher.appendReplacement(sb, encryptedPassword);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 替换 @key(keyId) placeholder
     *
     * @param value 原始值
     * @return 替换后的值
     */
    private String resolveKeyPlaceholder(String value) {
        if (keyMap == null) {
            return value;
        }

        Matcher matcher = KEY_PATTERN.matcher(value);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String keyId = matcher.group(1);
            String key = keyMap.get(keyId);

            if (key == null || key.isEmpty()) {
                throw new IllegalArgumentException(
                    "未找到 keyId: " + keyId + " 对应的密钥。请检查密钥配置。");
            }

            logger.fine("密钥 placeholder 替换: @key(" + keyId + ") -> [***HIDDEN***]");
            matcher.appendReplacement(sb, key);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 替换 ${env:VAR_NAME} placeholder
     *
     * @param value 原始值
     * @return 替换后的值
     */
    private String resolveEnvPlaceholder(String value) {
        Matcher matcher = ENV_PATTERN.matcher(value);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String varName = matcher.group(1);
            String envValue = System.getenv(varName);

            if (envValue == null) {
                throw new IllegalArgumentException(
                    "未找到环境变量: " + varName);
            }

            logger.fine("环境变量 placeholder 替换: ${env:" + varName + "} -> [***HIDDEN***]");
            matcher.appendReplacement(sb, envValue);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 替换 ${prop:system.property.name} placeholder
     *
     * @param value 原始值
     * @return 替换后的值
     */
    private String resolvePropPlaceholder(String value) {
        Matcher matcher = PROP_PATTERN.matcher(value);
        StringBuffer sb = new StringBuffer();

        while (matcher.find()) {
            String propName = matcher.group(1);
            String propValue = System.getProperty(propName);

            if (propValue == null) {
                throw new IllegalArgumentException(
                    "未找到系统属性: " + propName);
            }

            logger.fine("系统属性 placeholder 替换: ${prop:" + propName + "} -> [***HIDDEN***]");
            matcher.appendReplacement(sb, propValue);
        }
        matcher.appendTail(sb);

        return sb.toString();
    }

    /**
     * 检查值中是否包含 placeholder
     *
     * @param value 待检查的值
     * @return 是否包含 placeholder
     */
    public static boolean hasPlaceholder(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return CONFIG_PATTERN.matcher(value).find()
            || KEY_PATTERN.matcher(value).find()
            || ENV_PATTERN.matcher(value).find()
            || PROP_PATTERN.matcher(value).find();
    }

    /**
     * 提取值中的所有 placeholder
     *
     * @param value 待提取的值
     * @return placeholder 列表
     */
    public static java.util.List<String> extractPlaceholders(String value) {
        java.util.List<String> placeholders = new java.util.ArrayList<>();

        if (value == null || value.isEmpty()) {
            return placeholders;
        }

        // 提取 @config(uid)
        Matcher configMatcher = CONFIG_PATTERN.matcher(value);
        while (configMatcher.find()) {
            placeholders.add(configMatcher.group());
        }

        // 提取 @key(keyId)
        Matcher keyMatcher = KEY_PATTERN.matcher(value);
        while (keyMatcher.find()) {
            placeholders.add(keyMatcher.group());
        }

        // 提取 ${env:VAR_NAME}
        Matcher envMatcher = ENV_PATTERN.matcher(value);
        while (envMatcher.find()) {
            placeholders.add(envMatcher.group());
        }

        // 提取 ${prop:system.property.name}
        Matcher propMatcher = PROP_PATTERN.matcher(value);
        while (propMatcher.find()) {
            placeholders.add(propMatcher.group());
        }

        return placeholders;
    }
}
