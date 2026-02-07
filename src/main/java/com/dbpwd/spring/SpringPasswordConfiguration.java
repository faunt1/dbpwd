package com.dbpwd.spring;

import com.dbpwd.configcenter.ConfigCenterFallbackManager;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.logging.Logger;

/**
 * Spring 密码管理配置类
 *
 * 提供基于 Spring 注解的配置方式，支持配置中心和本地文件兜底
 *
 * 使用方式：
 * 1. 在 @Configuration 类中引入此配置：@Import(SpringPasswordConfiguration.class)
 * 2. 在 application.yml 或 application.properties 中配置相关属性
 *
 * 示例配置：
 * spring:
 *   dbpwd:
 *     config-center:
 *       url: http://config-center.example.com
 *       api-key: your-api-key
 *       base-directory: /druid/config
 *     local-password-file: /path/to/passwords.600
 *     sync-interval: 300000
 *
 * @author Druid DB Password Manager
 * @version 2.0
 */
@Configuration
public class SpringPasswordConfiguration {

    private static final Logger logger = Logger.getLogger(SpringPasswordConfiguration.class.getName());

    @Value("${dbpwd.config-center.url:}")
    private String configCenterUrl;

    @Value("${dbpwd.api-key:}")
    private String apiKey;

    @Value("${dbpwd.base-directory:/druid/config}")
    private String baseDirectory;

    @Value("${dbpwd.local-password-file:}")
    private String localPasswordFile;

    @Value("${dbpwd.sync-interval:300000}")
    private long syncInterval;

    /**
     * 配置中心回退管理器 Bean
     *
     * @return 配置中心回退管理器
     */
    @Bean(name = "dbpwdFallbackManager")
    public ConfigCenterFallbackManager fallbackManager() {
        logger.info("初始化配置中心回退管理器...");

        if (configCenterUrl != null && !configCenterUrl.isEmpty()
                && apiKey != null && !apiKey.isEmpty()) {
            logger.info("配置中心: " + configCenterUrl);
            logger.info("本地密码文件: " + (localPasswordFile != null ? localPasswordFile : "未配置"));
            logger.info("同步间隔: " + syncInterval + " 毫秒");
            return new ConfigCenterFallbackManager(
                    configCenterUrl, apiKey, baseDirectory, localPasswordFile, syncInterval);
        } else if (localPasswordFile != null && !localPasswordFile.isEmpty()) {
            logger.info("仅使用本地文件模式: " + localPasswordFile);
            return new ConfigCenterFallbackManager(localPasswordFile);
        } else {
            logger.warning("未配置密码文件，创建空回退管理器");
            // 创建一个仅使用本地文件的回退管理器，路径为空
            return new ConfigCenterFallbackManager(null);
        }
    }

    /**
     * Spring 密码属性后置处理器 Bean
     *
     * @param fallbackManager 配置中心回退管理器
     * @return 密码属性后置处理器
     */
    @Bean(name = "dbpwdPropertyPostProcessor")
    public SpringPasswordPropertyPostProcessor propertyPostProcessor(
            ConfigCenterFallbackManager fallbackManager) {
        logger.info("初始化密码属性后置处理器");
        return new SpringPasswordPropertyPostProcessor(fallbackManager);
    }

    // ==================== Getters ====================

    public String getConfigCenterUrl() {
        return configCenterUrl;
    }

    public void setConfigCenterUrl(String configCenterUrl) {
        this.configCenterUrl = configCenterUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public String getLocalPasswordFile() {
        return localPasswordFile;
    }

    public void setLocalPasswordFile(String localPasswordFile) {
        this.localPasswordFile = localPasswordFile;
    }

    public long getSyncInterval() {
        return syncInterval;
    }

    public void setSyncInterval(long syncInterval) {
        this.syncInterval = syncInterval;
    }
}
