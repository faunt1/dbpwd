package com.dbpwd.boot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 集成场景和方案 - 方案2: Spring Boot Starter集成
 *
 * Spring Boot集成配置属性类
 *
 * 配置属性示例：
 * <pre>
 * dbpwd:
 *   enabled: true                           # 是否启用密码管理
 *   mode: bean-post-processor              # 模式: bean-post-processor(默认)/property-source
 *   password-file: ~/.dbpwd/db.pwd         # 密码文件路径
 *   key-file: ~/.dbpwd/sm4.key             # 密钥文件路径
 *   placeholder-prefix: "@"                # 占位符前缀
 *   placeholder-suffix: "@"                # 占位符后缀
 *   enable-password-service: false         # 启用密码服务Bean（可选）
 *   enable-placeholder-resolver: false     # 启用占位符解析器Bean（可选）
 * </pre>
 *
 * 模式说明：
 * - bean-post-processor (默认): 方案C - 拦截 DataSource Bean，自动解密密码
 * - property-source: 方案B - 装饰 PropertySource，自动解密所有密码属性
 *
 * @author Database Password Manager
 * @version 6.0
 */
@ConfigurationProperties(prefix = "dbpwd")
public class DbPwdProperties {

    /**
     * 是否启用密码管理功能
     */
    private boolean enabled = true;

    /**
     * 集成模式
     * - bean-post-processor (默认): 方案C - BeanPostProcessor 模式
     * - property-source: 方案B - PropertySource 装饰器模式
     */
    private String mode = "bean-post-processor";

    /**
     * 密码文件路径
     */
    private String passwordFile = System.getProperty("user.home") + "/.dbpwd/db.pwd";

    /**
     * SM4密钥文件路径
     */
    private String keyFile = System.getProperty("user.home") + "/.dbpwd/sm4.key";

    /**
     * 占位符前缀
     */
    private String placeholderPrefix = "@";

    /**
     * 占位符后缀
     */
    private String placeholderSuffix = "@";

    /**
     * 是否启用占位符替换（支持@db_id@格式）
     */
    private boolean placeholderReplacement = true;

    // Getters and Setters

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getPasswordFile() {
        return passwordFile;
    }

    public void setPasswordFile(String passwordFile) {
        this.passwordFile = passwordFile;
    }

    public String getKeyFile() {
        return keyFile;
    }

    public void setKeyFile(String keyFile) {
        this.keyFile = keyFile;
    }

    public String getPlaceholderPrefix() {
        return placeholderPrefix;
    }

    public void setPlaceholderPrefix(String placeholderPrefix) {
        this.placeholderPrefix = placeholderPrefix;
    }

    public String getPlaceholderSuffix() {
        return placeholderSuffix;
    }

    public void setPlaceholderSuffix(String placeholderSuffix) {
        this.placeholderSuffix = placeholderSuffix;
    }

    public boolean isPlaceholderReplacement() {
        return placeholderReplacement;
    }

    public void setPlaceholderReplacement(boolean placeholderReplacement) {
        this.placeholderReplacement = placeholderReplacement;
    }

    /**
     * 获取完整的占位符格式
     * @return 占位符格式（如@db_id@）
     */
    public String getPlaceholderFormat() {
        return placeholderPrefix + "%s" + placeholderSuffix;
    }
}
