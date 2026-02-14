package com.dbpwd.core;

/**
 * 占位符解析器
 *
 * 解析 @db_name@ 格式的占位符，提取数据库ID
 *
 * 示例：
 * - "@spring_boot_db@" -> "spring_boot_db"
 * - "@druid_demo_db@" -> "druid_demo_db"
 * - "spring_boot_db" -> "spring_boot_db" (已经是ID格式)
 *
 * @author Database Password Manager
 * @version 1.0
 */
public class PlaceholderParser {

    /**
     * 解析占位符，提取数据库ID
     *
     * 支持两种格式：
     * 1. @db_name@ 格式 - 提取中间的db_name
     * 2. db_name 格式 - 直接返回
     *
     * @param placeholder 占位符或数据库ID
     * @return 数据库ID
     * @throws IllegalArgumentException 如果占位符格式无效
     */
    public static String parse(String placeholder) {
        if (placeholder == null || placeholder.isEmpty()) {
            throw new IllegalArgumentException("占位符不能为空");
        }

        // 如果是 @xxx@ 格式，提取中间部分
        if (placeholder.startsWith("@") && placeholder.endsWith("@")) {
            if (placeholder.length() < 3) {
                throw new IllegalArgumentException("占位符格式无效: " + placeholder);
            }
            return placeholder.substring(1, placeholder.length() - 1);
        }

        // 否则直接返回（已经是ID格式）
        return placeholder;
    }

    /**
     * 检查是否为占位符格式
     *
     * @param value 待检查的值
     * @return 如果是 @xxx@ 格式返回true，否则返回false
     */
    public static boolean isPlaceholder(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return value.startsWith("@") && value.endsWith("@");
    }
}
