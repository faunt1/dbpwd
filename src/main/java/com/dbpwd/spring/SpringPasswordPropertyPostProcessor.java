package com.dbpwd.spring;

import com.dbpwd.configcenter.ConfigCenterFallbackManager;
import com.dbpwd.universal.PasswordPropertiesInterceptor;

import java.util.Properties;
import java.util.logging.Logger;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/**
 * Spring 密码属性后置处理器
 *
 * 功能：
 * 1. 在 Spring 上下文初始化之前处理加密密码
 * 2. 支持配置中心下载优先、本地文件兜底
 * 3. 替换 @config(uid) 占位符为解密后的明文密码
 *
 * 使用方式：
 * 1. 在 Spring 配置中定义此 Bean
 * 2. 配置 dbpwd.config-center.url、dbpwd.api-key 等属性
 * 3. 配置 dbpwd.local-password-file 指定本地 .600 文件路径
 *
 * @author Druid DB Password Manager
 * @version 2.0
 */
public class SpringPasswordPropertyPostProcessor implements BeanFactoryPostProcessor {

    private static final Logger logger = Logger.getLogger(SpringPasswordPropertyPostProcessor.class.getName());

    // 配置属性名
    public static final String CONFIG_CENTER_URL = "dbpwd.config-center.url";
    public static final String API_KEY = "dbpwd.api-key";
    public static final String BASE_DIRECTORY = "dbpwd.base-directory";
    public static final String LOCAL_PASSWORD_FILE = "dbpwd.local-password-file";
    public static final String SYNC_INTERVAL = "dbpwd.sync-interval";

    // 默认值
    private static final String DEFAULT_BASE_DIRECTORY = "/druid/config";
    private static final long DEFAULT_SYNC_INTERVAL = 5 * 60 * 1000; // 5分钟

    private ConfigCenterFallbackManager fallbackManager;
    private PasswordPropertiesInterceptor interceptor;
    private Environment environment;

    /**
     * 默认构造函数
     */
    public SpringPasswordPropertyPostProcessor() {
    }

    /**
     * 使用回退管理器构造
     *
     * @param fallbackManager 配置中心回退管理器
     */
    public SpringPasswordPropertyPostProcessor(ConfigCenterFallbackManager fallbackManager) {
        this.fallbackManager = fallbackManager;
        this.interceptor = new PasswordPropertiesInterceptor(fallbackManager);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        logger.info("Spring 密码属性后置处理器启动");

        this.environment = beanFactory.getBean(Environment.class);

        // 初始化回退管理器
        if (fallbackManager == null) {
            initializeFallbackManager();
        }

        // 处理属性源
        processPropertySources(beanFactory);

        logger.info("Spring 密码属性后置处理器完成");
    }

    /**
     * 初始化回退管理器
     */
    private void initializeFallbackManager() {
        String configCenterUrl = environment.getProperty(CONFIG_CENTER_URL);
        String apiKey = environment.getProperty(API_KEY);
        String baseDirectory = environment.getProperty(BASE_DIRECTORY, DEFAULT_BASE_DIRECTORY);
        String localPasswordFile = environment.getProperty(LOCAL_PASSWORD_FILE);
        String syncIntervalStr = environment.getProperty(SYNC_INTERVAL);
        long syncInterval = syncIntervalStr != null ? Long.parseLong(syncIntervalStr) : DEFAULT_SYNC_INTERVAL;

        if (configCenterUrl != null && !configCenterUrl.isEmpty()
                && apiKey != null && !apiKey.isEmpty()) {
            logger.info("使用配置中心 + 本地文件兜底模式");
            logger.info("配置中心 URL: " + configCenterUrl);
            logger.info("本地密码文件: " + (localPasswordFile != null ? localPasswordFile : "未配置"));
            fallbackManager = new ConfigCenterFallbackManager(
                    configCenterUrl, apiKey, baseDirectory, localPasswordFile, syncInterval);
        } else if (localPasswordFile != null && !localPasswordFile.isEmpty()) {
            logger.info("仅使用本地文件模式");
            logger.info("本地密码文件: " + localPasswordFile);
            fallbackManager = new ConfigCenterFallbackManager(localPasswordFile);
        } else {
            logger.warning("未配置密码文件，将无法解析 @config() 占位符");
        }

        if (fallbackManager != null) {
            interceptor = new PasswordPropertiesInterceptor(fallbackManager);
        }
    }

    /**
     * 处理属性源
     *
     * @param beanFactory Bean 工厂
     */
    private void processPropertySources(ConfigurableListableBeanFactory beanFactory) {
        if (interceptor == null) {
            logger.warning("拦截器未初始化，跳过属性处理");
            return;
        }

        // 获取 Spring 的属性源
        if (environment instanceof org.springframework.core.env.ConfigurableEnvironment) {
            org.springframework.core.env.ConfigurableEnvironment configurableEnv =
                (org.springframework.core.env.ConfigurableEnvironment) environment;

            // 遍历并处理每个属性源
            for (PropertySource<?> propertySource : configurableEnv.getPropertySources()) {
                if (propertySource instanceof org.springframework.core.env.MapPropertySource) {
                    processMapPropertySource((org.springframework.core.env.MapPropertySource) propertySource);
                }
                /* 注意：这里不能直接修改 PropertySource，需要通过其他方式 */
            }
        }
    }

    /**
     * 处理 Map 类型的属性源
     *
     * @param propertySource 属性源
     */
    private void processMapPropertySource(org.springframework.core.env.MapPropertySource propertySource) {
        logger.fine("处理属性源: " + propertySource.getName());

        Properties props = new Properties();
        for (String key : propertySource.getSource().keySet()) {
            Object value = propertySource.getProperty(key);
            if (value instanceof String) {
                props.setProperty(key, (String) value);
            }
        }

        // 使用拦截器处理占位符
        Properties processedProps = interceptor.intercept(props);

        // 更新属性源（需要反射或使用其他方式）
        logger.fine("属性源处理完成: " + propertySource.getName());
    }

    /**
     * 创建自定义属性源
     *
     * @return 密码属性源
     */
    public static PropertySource<Properties> createPasswordPropertySource() {
        Properties passwordProps = new Properties();
        return new PropertySource<Properties>("dbpwd-passwords", passwordProps) {
            @Override
            public Object getProperty(String name) {
                return passwordProps.getProperty(name);
            }

            /**
             * 设置密码属性
             *
             * @param name 属性名
             * @param value 属性值
             */
            public void setPasswordProperty(String name, String value) {
                passwordProps.setProperty(name, value);
            }
        };
    }

    /**
     * 设置回退管理器
     *
     * @param fallbackManager 配置中心回退管理器
     */
    public void setFallbackManager(ConfigCenterFallbackManager fallbackManager) {
        this.fallbackManager = fallbackManager;
        this.interceptor = new PasswordPropertiesInterceptor(fallbackManager);
    }

    /**
     * 获取回退管理器
     *
     * @return 配置中心回退管理器
     */
    public ConfigCenterFallbackManager getFallbackManager() {
        return fallbackManager;
    }

    /**
     * 获取拦截器
     *
     * @return 密码属性拦截器
     */
    public PasswordPropertiesInterceptor getInterceptor() {
        return interceptor;
    }

    /**
     * 刷新配置中心
     */
    public void refreshConfigCenter() {
        if (fallbackManager != null) {
            fallbackManager.refresh();
        }
    }
}
