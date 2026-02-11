package com.dbpwd.boot;

import com.dbpwd.core.PasswordFileManager;
import com.dbpwd.core.Sm4KeyLoader;
import com.dbpwd.model.DatabaseEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * 集成场景和方案 - 方案2: Spring Boot Starter集成
 *
 * Spring Boot自动配置类
 *
 * 工作机制：
 * 1. 加载db.pwd配置文件
 * 2. 加载sm4.key密钥文件
 * 3. 提供密码解密服务Bean
 * 4. 自动装饰DataSource（如果启用）
 *
 * 使用方式：
 * 1. 在application.yml中配置：
 * <pre>
 * spring:
 *   datasource:
 *     url: jdbc:h2:mem:mydb
 *     username: sa
 *     password: "@my_database@"  # 使用占位符
 * dbpwd:
 *   enabled: true
 * </pre>
 *
 * 2. 自动注入密码服务：
 * <pre>
 * @Autowired
 * private DbPwdPasswordService passwordService;
 * </pre>
 *
 * @author Database Password Manager
 * @version 5.3
 */
@Configuration
@EnableConfigurationProperties(DbPwdProperties.class)
@ConditionalOnProperty(prefix = "dbpwd", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DbPwdAutoConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DbPwdAutoConfiguration.class);

    /**
     * 创建密码服务Bean
     */
    @Bean
    public DbPwdPasswordService dbPwdPasswordService(DbPwdProperties properties) {
        log.info("Initializing Database Password Manager...");
        log.info("  Password file: {}", properties.getPasswordFile());
        log.info("  Key file: {}", properties.getKeyFile());
        
        DbPwdPasswordService service = new DbPwdPasswordService(
            properties.getPasswordFile(),
            properties.getKeyFile()
        );
        
        // 预加载密码到缓存
        try {
            service.preloadPasswords();
            log.info("Database Password Manager initialized successfully");
        } catch (Exception e) {
            log.warn("Failed to preload passwords: {}", e.getMessage());
        }
        
        return service;
    }

    /**
     * 创建密码替换器Bean（如果启用占位符替换）
     */
    @Bean
    @ConditionalOnProperty(prefix = "dbpwd", name = "placeholder-replacement", havingValue = "true", matchIfMissing = true)
    public DbPwdPlaceholderResolver dbPwdPlaceholderResolver(DbPwdProperties properties, DbPwdPasswordService passwordService) {
        log.info("Initializing Placeholder Resolver with prefix='{}', suffix='{}'", 
            properties.getPlaceholderPrefix(), properties.getPlaceholderSuffix());
        return new DbPwdPlaceholderResolver(
            passwordService,
            properties.getPlaceholderPrefix(),
            properties.getPlaceholderSuffix()
        );
    }
}
