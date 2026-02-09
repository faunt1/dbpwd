package com.dbpwd.spring;

import com.dbpwd.core.PasswordFileManager;
import com.dbpwd.core.Sm4KeyLoader;
import com.dbpwd.framework.DataSourceDecorator;
import com.dbpwd.framework.DataSourceDecoratorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Database Password Manager Spring Boot 自动配置类
 *
 * 自动配置流程：
 * 1. 加载dbpwd配置属性
 * 2. 验证密钥文件和密码文件
 * 3. 创建PasswordFileManager和Sm4KeyLoader Bean
 * 4. 提供装饰DataSource的辅助方法
 *
 * 使用方式：
 * 1. 在application.yml中配置dbpwd属性
 * 2. 启用dbpwd自动配置
 * 3. DataSource会自动被装饰，密码自动解密
 *
 * <pre>
 * # application.yml
 * dbpwd:
 *   enabled: true
 *   password-file: ~/.dbpwd/db.pwd
 *   key-file: ~/.dbpwd/sm4.key
 *
 * spring:
 *   datasource:
 *     url: jdbc:mysql://localhost:3306/test
 *     username: user
 *     password: ENC(SM4:密文)  # 密文密码
 * </pre>
 *
 * @author Database Password Manager
 * @version 4.2
 */
@Configuration
@EnableConfigurationProperties(DbPwdProperties.class)
@ConditionalOnClass(DataSource.class)
@ConditionalOnProperty(prefix = "dbpwd", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DbPwdAutoConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DbPwdAutoConfiguration.class);

    @Autowired
    private DbPwdProperties properties;

    /**
     * 创建PasswordFileManager Bean
     *
     * @return PasswordFileManager实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "dbpwd", name = "enabled", havingValue = "true")
    public PasswordFileManager passwordFileManager() {
        if (!properties.isEnabled()) {
            logger.info("Database Password Manager is disabled");
            throw new IllegalStateException("Database Password Manager is disabled but PasswordFileManager bean is requested");
        }

        String passwordFilePath = expandPath(properties.getPasswordFile());
        logger.info("Loading password file from: {}", passwordFilePath);

        PasswordFileManager fileManager = PasswordFileManager.loadFromFileSafe(passwordFilePath);
        logger.info("Password file loaded successfully, {} entries", fileManager.getAllDatabases().size());
        return fileManager;
    }

    /**
     * 创建Sm4KeyLoader Bean
     *
     * @return Sm4KeyLoader实例
     */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "dbpwd", name = "enabled", havingValue = "true")
    public Sm4KeyLoader sm4KeyLoader() {
        if (!properties.isEnabled()) {
            throw new IllegalStateException("Database Password Manager is disabled but Sm4KeyLoader bean is requested");
        }

        String keyFilePath = expandPath(properties.getKeyFile());
        logger.info("Loading SM4 key file from: {}", keyFilePath);

        Sm4KeyLoader keyLoader = new Sm4KeyLoader(keyFilePath);

        if (properties.isValidateKeyOnStartup()) {
            try {
                keyLoader.loadKey();
                logger.info("SM4 key loaded and validated successfully: " );
            } catch (Exception e) {
                logger.error("Failed to load SM4 key: {}", e.getMessage());
                throw new IllegalStateException("Failed to load SM4 key: " + e.getMessage(), e);
            }
        }

        return keyLoader;
    }

    /**
     * 展开路径中的~符号
     */
    private String expandPath(String path) {
        if (path == null || path.isEmpty()) {
            return path;
        }

        if (path.startsWith("~/") || path.equals("~")) {
            String home = System.getProperty("user.home");
            if (path.equals("~")) {
                return home;
            }
            return home + path.substring(1);
        }
        return path;
    }

    /**
     * 静态辅助方法：装饰DataSource
     *
     * 此方法提供便捷的DataSource装饰功能
     *
     * @param dataSource   原始DataSource
     * @param dbId         数据库ID
     * @param sm4Key       SM4密钥
     * @param passwordFile 密码文件路径
     * @return 装饰后的DataSource
     */
    public static DataSourceDecorator decorateDataSource(
            DataSource dataSource,
            String dbId,
            String sm4Key,
            String passwordFile) {
        return DataSourceDecoratorFactory.decorate(dataSource, dbId, sm4Key, passwordFile);
    }

    /**
     * 静态辅助方法：装饰DataSource（使用默认密码文件）
     *
     * @param dataSource 原始DataSource
     * @param dbId       数据库ID
     * @param sm4Key     SM4密钥
     * @return 装饰后的DataSource
     */
    public static DataSourceDecorator decorateDataSource(
            DataSource dataSource,
            String dbId,
            String sm4Key) {
        return DataSourceDecoratorFactory.decorate(dataSource, dbId, sm4Key);
    }
}
