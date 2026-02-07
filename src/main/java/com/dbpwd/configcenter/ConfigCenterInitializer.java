package com.dbpwd.configcenter;

import com.dbpwd.DruidConfigUploader;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;

/**
 * 配置中心初始化器
 * 负责配置中心的初始化操作
 *
 * 功能：
 * - 上传模板文件到 templates/ 目录
 * - 上传密码文件到 database/ 目录
 * - 批量上传多个文件
 */
public class ConfigCenterInitializer {

    private static final Logger logger = Logger.getLogger(ConfigCenterInitializer.class.getName());

    private final String configCenterUrl;
    private final String apiKey;
    private final String baseDirectory;
    private final DruidConfigUploader uploader;

    /**
     * 构造函数
     *
     * @param configCenterUrl 配置中心 URL
     * @param apiKey API 密钥
     */
    public ConfigCenterInitializer(String configCenterUrl, String apiKey) {
        this(configCenterUrl, apiKey, "/druid/config");
    }

    /**
     * 构造函数（指定基础目录）
     *
     * @param configCenterUrl 配置中心 URL
     * @param apiKey API 密钥
     * @param baseDirectory 基础目录
     */
    public ConfigCenterInitializer(String configCenterUrl, String apiKey, String baseDirectory) {
        this.configCenterUrl = configCenterUrl;
        this.apiKey = apiKey;
        this.baseDirectory = baseDirectory;
        this.uploader = new DruidConfigUploader(configCenterUrl, apiKey, baseDirectory);
    }

    /**
     * 初始化目录结构（仅上传空文件占位）
     * 注意：实际配置中心可能不支持目录创建，此方法用于记录日志
     */
    public void initializeDirectory() {
        logger.info("配置中心目录结构初始化:");
        logger.info("  基础目录: " + baseDirectory);
        logger.info("  templates/ 目录: " + baseDirectory + "/templates");
        logger.info("  database/ 目录: " + baseDirectory + "/database");
        logger.info("说明: 配置中心可能不支持预创建目录，目录将在上传文件时自动创建");
    }

    /**
     * 上传模板文件
     *
     * @param templateFile 本地模板文件路径
     * @param templateName 模板名称
     * @return 是否上传成功
     * @throws Exception 上传异常
     */
    public boolean uploadTemplate(String templateFile, String templateName) throws Exception {
        validateFileExists(templateFile);

        logger.info("开始上传模板文件: " + templateName);
        boolean success = uploader.uploadTemplate(templateFile, templateName);

        if (success) {
            logger.info("模板文件上传成功: " + templateName);
        } else {
            logger.warning("模板文件上传失败: " + templateName);
        }

        return success;
    }

    /**
     * 上传密码文件
     *
     * @param passwordFile 本地密码文件路径
     * @return 是否上传成功
     * @throws Exception 上传异常
     */
    public boolean uploadPasswordFile(String passwordFile) throws Exception {
        return uploadPasswordFile(passwordFile, "passwords.txt");
    }

    /**
     * 上传密码文件（指定文件名）
     *
     * @param passwordFile 本地密码文件路径
     * @param passwordFileName 密码文件名称
     * @return 是否上传成功
     * @throws Exception 上传异常
     */
    public boolean uploadPasswordFile(String passwordFile, String passwordFileName) throws Exception {
        validateFileExists(passwordFile);

        logger.info("开始上传密码文件: " + passwordFileName);
        boolean success = uploader.uploadPasswordFile(passwordFile, passwordFileName);

        if (success) {
            logger.info("密码文件上传成功: " + passwordFileName);
        } else {
            logger.warning("密码文件上传失败: " + passwordFileName);
        }

        return success;
    }

    /**
     * 批量上传模板文件
     *
     * @param templateFiles 模板文件列表
     * @return 成功上传的文件数量
     * @throws Exception 上传异常
     */
    public int uploadTemplates(List<TemplateFile> templateFiles) throws Exception {
        int successCount = 0;

        for (TemplateFile templateFile : templateFiles) {
            if (uploadTemplate(templateFile.filePath, templateFile.templateName)) {
                successCount++;
            }
        }

        logger.info("批量上传模板文件完成: " + successCount + "/" + templateFiles.size() + " 成功");
        return successCount;
    }

    /**
     * 批量上传文件（模板和密码文件）
     *
     * @param templateFiles 模板文件列表
     * @param passwordFile 密码文件路径
     * @return 成功上传的文件数量
     * @throws Exception 上传异常
     */
    public int uploadAll(List<TemplateFile> templateFiles, String passwordFile) throws Exception {
        int successCount = 0;

        // 上传模板文件
        for (TemplateFile templateFile : templateFiles) {
            if (uploadTemplate(templateFile.filePath, templateFile.templateName)) {
                successCount++;
            }
        }

        // 上传密码文件
        if (passwordFile != null && !passwordFile.isEmpty()) {
            if (uploadPasswordFile(passwordFile)) {
                successCount++;
            }
        }

        int totalFiles = templateFiles.size() + (passwordFile != null && !passwordFile.isEmpty() ? 1 : 0);
        logger.info("批量上传完成: " + successCount + "/" + totalFiles + " 成功");
        return successCount;
    }

    /**
     * 验证文件是否存在
     *
     * @param filePath 文件路径
     * @throws IllegalArgumentException 文件不存在时抛出
     */
    private void validateFileExists(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IllegalArgumentException("文件不存在: " + filePath);
        }
        if (!file.isFile()) {
            throw new IllegalArgumentException("路径不是文件: " + filePath);
        }
    }

    /**
     * 获取上传器实例
     *
     * @return DruidConfigUploader
     */
    public DruidConfigUploader getUploader() {
        return uploader;
    }

    /**
     * 获取配置中心 URL
     *
     * @return 配置中心 URL
     */
    public String getConfigCenterUrl() {
        return configCenterUrl;
    }

    /**
     * 获取 API 密钥
     *
     * @return API 密钥
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * 获取基础目录
     *
     * @return 基础目录
     */
    public String getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * 模板文件包装类
     */
    public static class TemplateFile {
        private final String filePath;
        private final String templateName;

        public TemplateFile(String filePath, String templateName) {
            this.filePath = filePath;
            this.templateName = templateName;
        }

        public String getFilePath() {
            return filePath;
        }

        public String getTemplateName() {
            return templateName;
        }

        @Override
        public String toString() {
            return "(" + templateName + " -> " + filePath + ")";
        }
    }
}
