package com.dbpwd.configcenter;

import com.dbpwd.DruidConfigUploader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;

/**
 * 配置中心模板解析器
 * 负责从配置中心下载模板文件并解析 placeholder
 *
 * 工作流程：
 * 1. 从配置中心的 templates/ 目录下载模板文件
 * 2. 从配置中心的 database/ 目录下载密码文件
 * 3. 解析模板中的 @config(uid) 和 @key(keyId) placeholder
 * 4. 替换为实际的加密密码和密钥
 * 5. 返回完整的 Properties 配置
 */
public class ConfigCenterTemplateResolver {

    private static final Logger logger = Logger.getLogger(ConfigCenterTemplateResolver.class.getName());

    private static final String TEMPLATES_DIR = "templates";
    private static final String DATABASE_DIR = "database";
    private static final String PASSWORD_FILE_NAME = "passwords.txt";

    private final String configCenterUrl;
    private final String apiKey;
    private final String baseDirectory;
    private final DruidConfigUploader uploader;

    private String customPasswordFileName;

    /**
     * 构造函数
     *
     * @param configCenterUrl 配置中心 URL
     * @param apiKey API 密钥
     */
    public ConfigCenterTemplateResolver(String configCenterUrl, String apiKey) {
        this(configCenterUrl, apiKey, "/druid/config");
    }

    /**
     * 构造函数（指定基础目录）
     *
     * @param configCenterUrl 配置中心 URL
     * @param apiKey API 密钥
     * @param baseDirectory 基础目录
     */
    public ConfigCenterTemplateResolver(String configCenterUrl, String apiKey, String baseDirectory) {
        this.configCenterUrl = configCenterUrl;
        this.apiKey = apiKey;
        this.baseDirectory = baseDirectory;
        this.uploader = new DruidConfigUploader(configCenterUrl, apiKey);
 this.customPasswordFileName = PASSWORD_FILE_NAME;
    }

    /**
     * 解析模板（使用默认密码文件）
     *
     * @param templateName 模板名称
     * @return 解析后的 Properties
     * @throws Exception 解析异常
     */
    public Properties resolveTemplate(String templateName) throws Exception {
        return resolveTemplate(templateName, null);
    }

    /**
     * 解析模板（指定密钥映射）
     *
     * @param templateName 模板名称
     * @param keyMap 密钥映射（keyId -> key）
     * @return 解析后的 Properties
     * @throws Exception 解析异常
     */
    public Properties resolveTemplate(String templateName, Map<String, String> keyMap) throws Exception {
        logger.info("开始解析模板: " + templateName);

        // 1. 下载模板文件
        String templateContent = downloadTemplate(templateName);

        // 2. 解析模板为 Properties
        Properties properties = parseTemplate(templateContent);

        // 3. 下载密码文件
        String passwordFileContent = downloadPasswordFile();

        // 4. 解析密码映射
        PasswordStorageManager passwordStorage = PasswordStorageManager.loadFromString(passwordFileContent);

        // 5. 创建 placeholder 解析器并替换
        PlaceholderResolver placeholderResolver = new PlaceholderResolver(
            passwordStorage.getPasswordMap(),
            keyMap
        );
        placeholderResolver.resolvePlaceholders(properties);

        logger.info("模板解析成功: " + templateName);
        return properties;
    }

    /**
     * 下载模板文件
     *
     * @param templateName 模板名称
     * @return 模板内容
     * @throws Exception 下载异常
     */
    public String downloadTemplate(String templateName) throws Exception {
        String templateDir = baseDirectory + "/" + TEMPLATES_DIR;
        logger.info("从配置中心下载模板: " + templateName + " (目录: " + templateDir + ")");

        String content = uploader.download(templateName, templateDir);
        logger.info("模板下载成功: " + templateName);

        return content;
    }

    /**
     * 下载密码文件
     *
     * @return 密码文件内容
     * @throws Exception 下载异常
     */
    public String downloadPasswordFile() throws Exception {
        String databaseDir = baseDirectory + "/" + DATABASE_DIR;
        logger.info("从配置中心下载密码文件: " + customPasswordFileName + " (目录: " + databaseDir + ")");

        String content = uploader.download(customPasswordFileName, databaseDir);
        logger.info("密码文件下载成功");

        return content;
    }

    /**
     * 解析模板内容为 Properties
     *
     * @param content 模板内容
     * @return Properties
     * @throws IOException 解析异常
     */
    private Properties parseTemplate(String content) throws IOException {
        Properties properties = new Properties();
        try (StringReader reader = new StringReader(content)) {
            properties.load(reader);
        }
        return properties;
    }

    /**
     * 解析模板文件为 Properties
     *
     * @param templateFilePath 模板文件路径
     * @return Properties
     * @throws IOException 解析异常
     */
    public static Properties parseTemplateFile(String templateFilePath) throws IOException {
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(templateFilePath);
             InputStreamReader reader = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
            properties.load(reader);
        }
        return properties;
    }

    /**
     * 解析模板内容为 Properties（本地密码文件）
     *
     * @param templateFilePath 模板文件路径
     * @param passwordFilePath 密码文件路径
     * @return 解析后的 Properties
     * @throws Exception 解析异常
     */
    public static Properties resolveTemplateLocal(String templateFilePath, String passwordFilePath) throws Exception {
        return resolveTemplateLocal(templateFilePath, passwordFilePath, null);
    }

    /**
     * 解析模板内容为 Properties（本地密码文件，指定密钥映射）
     *
     * @param templateFilePath 模板文件路径
     * @param passwordFilePath 密码文件路径
     * @param keyMap 密钥映射
     * @return 解析后的 Properties
     * @throws Exception 解析异常
     */
    public static Properties resolveTemplateLocal(
        String templateFilePath,
        String passwordFilePath,
        Map<String, String> keyMap
    ) throws Exception {
        logger.info("本地解析模板: " + templateFilePath);

        // 1. 解析模板为 Properties
        Properties properties = parseTemplateFile(templateFilePath);

        // 2. 解析密码文件
        PasswordStorageManager passwordStorage = PasswordStorageManager.load(passwordFilePath);

        // 3. 创建 placeholder 解析器并替换
        PlaceholderResolver placeholderResolver = new PlaceholderResolver(
            passwordStorage.getPasswordMap(),
            keyMap
        );
        placeholderResolver.resolvePlaceholders(properties);

        logger.info("本地模板解析成功");
        return properties;
    }

    // ==================== Getters and Setters ====================

    public String getConfigCenterUrl() {
        return configCenterUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseDirectory() {
        return baseDirectory;
    }

    public DruidConfigUploader getUploader() {
        return uploader;
    }

    /**
     * 设置自定义密码文件名
     *
     * @param passwordFileName 密码文件名
     */
    public void setCustomPasswordFileName(String passwordFileName) {
        if (passwordFileName == null || passwordFileName.isEmpty()) {
            throw new IllegalArgumentException("passwordFileName 不能为空");
        }
        this.customPasswordFileName = passwordFileName;
    }

    public String getCustomPasswordFileName() {
        return customPasswordFileName;
    }

    public static String getTemplatesDir() {
        return TEMPLATES_DIR;
    }

    public static String getDatabaseDir() {
        return DATABASE_DIR;
    }

    public static String getPasswordFileName() {
        return PASSWORD_FILE_NAME;
    }
}
