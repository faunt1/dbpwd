package com.dbpwd.boot.postprocessor;

import com.dbpwd.boot.DbPwdProperties;
import com.dbpwd.boot.encrypt.DbPwdEncryptablePropertySource;
import com.dbpwd.boot.encrypt.DbPwdPropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

/**
 * db-pwd BeanFactoryPostProcessor
 *
 * 方案B的核心实现，在Bean创建前装饰所有 PropertySource
 *
 * 工作流程：
 * 1. 在 Spring Boot 启动时，Environment 已加载所有配置源
 * 2. 此 PostProcessor 遍历所有 PropertySource
 * 3. 将原始 PropertySource 包装为 DbPwdEncryptablePropertySource
 * 4. 应用读取配置时，自动解密密码相关属性
 *
 * @author Database Password Manager
 * @version 6.0
 */
public class DbPwdBeanFactoryPostProcessor implements BeanFactoryPostProcessor, Ordered {
    
    private static final Logger log = LoggerFactory.getLogger(DbPwdBeanFactoryPostProcessor.class);
    
    private final DbPwdProperties properties;
    
    public DbPwdBeanFactoryPostProcessor(DbPwdProperties properties) {
        this.properties = properties;
    }
    
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        log.info("========================================");
        log.info("   Initializing DBPWD-2B: PropertySource Encryption");
        log.info("========================================");
        
        // 获取 Environment
        ConfigurableEnvironment env = beanFactory.getBean(ConfigurableEnvironment.class);
        MutablePropertySources propertySources = env.getPropertySources();
        
        // 创建属性解析器
        DbPwdPropertyResolver resolver = new DbPwdPropertyResolver(
            properties.getPlaceholderPrefix(),
            properties.getPlaceholderSuffix()
        );
        
        // 遍历并包装所有 PropertySource
        int wrappedCount = 0;
        for (PropertySource<?> propertySource : propertySources) {
            if (propertySource instanceof DbPwdEncryptablePropertySource) {
                // 已经包装过，跳过
                continue;
            }
            
            log.debug("Wrapping PropertySource: {}", propertySource.getName());
            
            // 创建包装器
            DbPwdEncryptablePropertySource encryptableSource = 
                new DbPwdEncryptablePropertySource(propertySource, resolver);
            
            // 替换原始 PropertySource
            propertySources.replace(propertySource.getName(), encryptableSource);
            wrappedCount++;
        }
        
        log.info("  Wrapped {} PropertySource(s)", wrappedCount);
        log.info("  Placeholder format: {}xxx{}", 
            properties.getPlaceholderPrefix(), properties.getPlaceholderSuffix());
        log.info("  Target properties: password/pwd/secret/key");
        log.info("========================================");
        log.info("DBPWD-2B initialized successfully");
    }
    
    @Override
    public int getOrder() {
        // 低优先级，确保在其他 PostProcessor 之后执行
        return Ordered.LOWEST_PRECEDENCE;
    }
}
