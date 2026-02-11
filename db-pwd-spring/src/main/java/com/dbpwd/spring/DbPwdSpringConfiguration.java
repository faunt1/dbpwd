package com.dbpwd.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring配置类
 *
 * 自动配置DataSourcePasswordProcessor Bean
 *
 * 使用方式：
 * <pre>
 * &lt;context:component-scan base-package="com.dbpwd.spring"/&gt;
 * </pre>
 *
 * 或者使用Java配置：
 * <pre>
 * @Configuration
 * @Import(DbPwdSpringConfiguration.class)
 * public class AppConfig {
 *     // ...
 * }
 * </pre>
 *
 * @author Database Password Manager
 * @version 5.3
 */
@Configuration
public class DbPwdSpringConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(DbPwdSpringConfiguration.class);

    /**
     * 配置DataSourcePasswordProcessor Bean
     *
     * @return DataSourcePasswordProcessor实例
     */
    @Bean
    public DataSourcePasswordProcessor dataSourcePasswordProcessor() {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();
        logger.info("DataSourcePasswordProcessor Bean registered");
        return processor;
    }

    /**
     * 配置带自定义路径的DataSourcePasswordProcessor Bean
     *
     * @param keyFilePath SM4密钥文件路径（可选，null则使用默认路径）
     * @param pwdFilePath 密码文件路径（可选，null则使用默认路径）
     * @return DataSourcePasswordProcessor实例
     */
}
