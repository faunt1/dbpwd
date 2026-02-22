package com.dbpwd.boot;

import com.dbpwd.boot.postprocessor.DbPwdBeanFactoryPostProcessor;
import com.dbpwd.spring.DataSourcePasswordProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 集成场景和方案 - 方案2: Spring Boot Starter集成
 *
 * Spring Boot自动配置类 - 支持方案C和方案B
 *
 * 方案C（默认）：BeanPostProcessor 模式
 *   - 拦截 DataSource Bean，自动解密密码
 *   - 优点：实现简单，调试容易
 *   - 缺点：仅针对 DataSource，不支持其他属性
 *
 * 方案B（通过配置启用）：PropertySource 装饰器模式
 *   - 装饰所有 PropertySource，自动解密密码属性
 *   - 优点：通用性强，适用于所有属性
 *   - 缺点：实现复杂，调试困难
 *
 * 使用方式（方案C - 默认，零代码）：
 * <pre>
 * # 1. 添加依赖
 * &lt;dependency&gt;
 *     &lt;groupId&gt;com.dbpwd&lt;/groupId&gt;
 *     &lt;artifactId&gt;db-pwd-spring-boot-starter&lt;/artifactId&gt;
 * &lt;/dependency&gt;
 *
 * # 2. 配置 application.yml
 * spring:
 *   datasource:
 *     password: "@my_database@"  # 占位符，自动解密
 * </pre>
 *
 * 使用方式（方案B - 需要启用）：
 * <pre>
 * # 配置 application.yml
 * dbpwd:
 *   mode: property-source  # 启用方案B
 *   enabled: true
 *
 * # 然后配置数据源
 * spring:
 *   datasource:
 *     password: "@my_database@"  # 占位符，自动解密
 * </pre>
 *
 * @author Database Password Manager
 * @version 6.0
 */
@Configuration
@EnableConfigurationProperties(DbPwdProperties.class)
@ConditionalOnProperty(prefix = "dbpwd", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DbPwdAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DbPwdAutoConfiguration.class);

    /**
     * 方案B：PropertySource 装饰器模式
     *
     * 当 dbpwd.mode=property-source 时启用
     * 装饰所有 PropertySource，自动解密密码属性
     */
    @Bean
    @ConditionalOnProperty(prefix = "dbpwd", name = "mode", havingValue = "property-source", matchIfMissing = false)
    public static DbPwdBeanFactoryPostProcessor dbPwdBeanFactoryPostProcessor(DbPwdProperties properties) {
        log.info("========================================");
        log.info("   DBPWD-Mode 2B: PropertySource Encryption");
        log.info("========================================");
        log.info("  This mode will:");
        log.info("  1. Wrap all PropertySource in Environment");
        log.info("  2. Intercept property reads");
        log.info("  3. Auto-decrypt password/secret/key properties");
        log.info("  4. Target: contains 'password' / 'pwd' / 'secret' / 'key'");
        log.info("========================================");
        return new DbPwdBeanFactoryPostProcessor(properties);
    }

    /**
     * 方案C：BeanPostProcessor 模式（默认）
     *
     * 当 dbpwd.mode=bean-post-processor 或未指定时启用
     * 拦截 DataSource Bean，自动解密密码
     */
    @Bean
    @ConditionalOnProperty(
        prefix = "dbpwd", 
        name = "mode", 
        havingValue = "bean-post-processor", 
        matchIfMissing = true  // 默认启用
    )
    public DataSourcePasswordProcessor dataSourcePasswordProcessor(DbPwdProperties properties) {
        log.info("========================================");
        log.info("   DBPWD-Mode 2C: BeanPostProcessor");
        log.info("========================================");
        log.info("  This mode will:");
        log.info("  1. Intercept DataSource Bean creation");
        log.info("  2. Auto-decrypt password property");
        log.info("  3. Apply to DataSource (Hikari/Druid/DBCP/etc.)");
        log.info("========================================");

        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();

        if (properties.getKeyFile() != null) {
            processor.setKeyFilePath(properties.getKeyFile());
            log.info("  Key file: {}", properties.getKeyFile());
        }
        if (properties.getPasswordFile() != null) {
            processor.setPwdFilePath(properties.getPasswordFile());
            log.info("  Password file: {}", properties.getPasswordFile());
        }

        log.info("  Password format: @database_name@");
        log.info("  Auto-decryption: ENABLED");
        log.info("========================================");

        return processor;
    }

    /**
     * 创建密码服务Bean（可选，供高级用户使用）
     * 注意：方案C和方案B的主要方式已自动处理密码解密
     *      这个 Bean 仅供需要手动获取密码的高级用户使用
     */
    @Bean
    @ConditionalOnProperty(prefix = "dbpwd", name = "enable-password-service", havingValue = "true", matchIfMissing = false)
    public DbPwdPasswordService dbPwdPasswordService(DbPwdProperties properties) {
        log.info("Creating DbPwdPasswordService for advanced usage");

        DbPwdPasswordService service = new DbPwdPasswordService(
            properties.getPasswordFile(),
            properties.getKeyFile()
        );

        try {
            service.preloadPasswords();
        } catch (Exception e) {
            log.warn("Failed to preload passwords: {}", e.getMessage());
        }

        return service;
    }

    /**
     * 创建密码替换器Bean（可选）
     * 注意：方案C和方案B 已自动处理占位符解析
     *      这个 Bean 仅供需要手动解析占位符的场景使用
     */
    @Bean
    @ConditionalOnProperty(prefix = "dbpwd", name = "enable-placeholder-resolver", havingValue = "true", matchIfMissing = false)
    public DbPwdPlaceholderResolver dbPwdPlaceholderResolver(DbPwdProperties properties, DbPwdPasswordService passwordService) {
        log.info("Creating DbPwdPlaceholderResolver for manual placeholder resolution");
        return new DbPwdPlaceholderResolver(
            passwordService,
            properties.getPlaceholderPrefix(),
            properties.getPlaceholderSuffix()
        );
    }
}
