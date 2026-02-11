package com.dbpwd.boot;

/**
 * 集成场景和方案 - 方案2: Spring Boot Starter集成
 *
 * 占位符解析器
 *
 * 负责将@db_id@格式的占位符替换为明文密码
 *
 * 使用方式：
 * <pre>
 * @Autowired
 * private DbPwdPlaceholderResolver placeholderResolver;
 *
 * String password = placeholderResolver.resolve("@my_database@");
 * </pre>
 *
 * @author Database Password Manager
 * @version 5.3
 */
public class DbPwdPlaceholderResolver {

    private final DbPwdPasswordService passwordService;
    private final String placeholderPrefix;
    private final String placeholderSuffix;

    public DbPwdPlaceholderResolver(DbPwdPasswordService passwordService,
                                    String placeholderPrefix,
                                    String placeholderSuffix) {
        this.passwordService = passwordService;
        this.placeholderPrefix = placeholderPrefix;
        this.placeholderSuffix = placeholderSuffix;
    }

    /**
     * 解析占位符并返回明文密码
     *
     * @param value 可能包含占位符的字符串
     * @return 解析后的明文密码，如果不是占位符则返回原值
     * @throws Exception 解析失败
     */
    public String resolve(String value) throws Exception {
        if (value == null || value.isEmpty()) {
            return value;
        }

        if (isPlaceholder(value)) {
            String dbId = extractDbId(value);
            return passwordService.getPlainPassword(dbId);
        }

        return value;
    }

    /**
     * 检查是否是占位符
     */
    private boolean isPlaceholder(String value) {
        return DbPwdPasswordService.isPlaceholder(value, placeholderPrefix, placeholderSuffix);
    }

    /**
     * 从占位符中提取数据库ID
     */
    private String extractDbId(String placeholder) {
        return DbPwdPasswordService.extractDbId(placeholder, placeholderPrefix, placeholderSuffix);
    }

    /**
     * 批量解析占位符
     *
     * @param values 可能包含占位符的字符串数组
     * @return 解析后的字符串数组
     * @throws Exception 解析失败
     */
    public String[] resolveAll(String[] values) throws Exception {
        if (values == null) {
            return null;
        }
        String[] result = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = resolve(values[i]);
        }
        return result;
    }
}
