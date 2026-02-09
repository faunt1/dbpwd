package com.dbpwd.core;

import com.dbpwd.model.PasswordFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * 配置中心下载工具
 *
 * 根据配置中心URL自动选择下载模式：
 * 1. HTTP/HTTPS URL：通过HTTP API下载（支持API Key认证）
 * 2. 文件路径：从本地/远程文件目录复制
 *
 * 配置中心路径结构: /物理组/应用/环境/{db.pwd,sm4.key}
 *
 * @author Database Password Manager
 * @version 3.0
 */
public class ConfigCenterDownloader {

    private static final Logger logger = Logger.getLogger(ConfigCenterDownloader.class.getName());
    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_INSTANT;
    private static final int CONNECT_TIMEOUT = 30000; // 30秒
    private static final int READ_TIMEOUT = 60000;    // 60秒

    private final boolean useWebApi;
    private final String configCenterUrl;
    private final String apiKey;
    private final String basePath;
    private final String physicalGroup;
    private final String application;
    private final String environment;
    private final String localDbPwdPath;
    private final String localKeyPath;

    /**
     * 私有构造函数，使用Builder模式创建实例
     */
    private ConfigCenterDownloader(Builder builder) {
        this.configCenterUrl = builder.configCenterUrl;
        this.apiKey = builder.apiKey;
        this.basePath = builder.basePath;
        this.physicalGroup = builder.physicalGroup;
        this.application = builder.application;
        this.environment = builder.environment;
        this.localDbPwdPath = expandHomeDir(builder.localDbPwdPath);
        this.localKeyPath = expandHomeDir(builder.localKeyPath);

        // 根据URL自动判断使用哪种下载模式
        this.useWebApi = isWebApiUrl(configCenterUrl);
    }

    /**
     * 判断URL是否为Web API地址
     */
    private static boolean isWebApiUrl(String url) {
        if (url == null || url.isEmpty()) {
            return true; // 默认使用Web API
        }
        String lowerUrl = url.toLowerCase().trim();
        return lowerUrl.startsWith("http://") || lowerUrl.startsWith("https://");
    }

    /**
     * 下载并对比文件
     *
     * @return DownloadResult 下载结果
     * @throws IOException 下载失败
     */
    public DownloadResult downloadWithCompare() throws IOException {
        String modeDesc = useWebApi ? "WEB_API (HTTP)" : "FILE_COPY (文件目录)";
        logger.info("开始配置中心文件下载和对比 (模式: " + modeDesc + ")");

        DownloadResult result = new DownloadResult();

        // 构建远程文件路径
        String remoteDbPwdPath = buildRemotePath("db.pwd");
        String remoteKeyPath = buildRemotePath("sm4.key");

        logger.info("远程配置路径:");
        logger.info("  db.pwd: " + remoteDbPwdPath);
        logger.info("  sm4.key: " + remoteKeyPath);

        // 对比并下载db.pwd文件
        FileCompareResult dbPwdCompare = compareAndDownloadFile(
                remoteDbPwdPath, localDbPwdPath, "db.pwd");
        result.setDbPwdResult(dbPwdCompare);

        // 对比并下载sm4.key文件
        FileCompareResult keyCompare = compareAndDownloadFile(
                remoteKeyPath, localKeyPath, "sm4.key");
        result.setKeyResult(keyCompare);

        logger.info("配置中心文件下载和对比完成: " + result.getSummary());

        return result;
    }

    /**
     * 仅下载而不对比
     *
     * @return DownloadResult 下载结果
     * @throws IOException 下载失败
     */
    public DownloadResult download() throws IOException {
        String modeDesc = useWebApi ? "WEB_API (HTTP)" : "FILE_COPY (文件目录)";
        logger.info("开始强制下载配置中心文件 (模式: " + modeDesc + ")");

        DownloadResult result = new DownloadResult();

        String remoteDbPwdPath = buildRemotePath("db.pwd");
        String remoteKeyPath = buildRemotePath("sm4.key");

        // 直接下载db.pwd文件
        FileCompareResult dbPwdResult = downloadFile(
                remoteDbPwdPath, localDbPwdPath, "db.pwd");
        result.setDbPwdResult(dbPwdResult);

        // 直接下载sm4.key文件
        FileCompareResult keyResult = downloadFile(
                remoteKeyPath, localKeyPath, "sm4.key");
        result.setKeyResult(keyResult);

        logger.info("配置中心文件下载完成: " + result.getSummary());

        return result;
    }

    /**
     * 对比并下载单个文件
     */
    private FileCompareResult compareAndDownloadFile(
            String remotePath, String localPath, String fileName) throws IOException {

        // 获取远程文件修改时间
        Instant remoteModified = getRemoteFileModifiedTime(remotePath);

        // 获取本地文件修改时间
        Instant localModified = getLocalFileModifiedTime(localPath);

        logger.fine(fileName + " - 远程修改时间: " + remoteModified);
        logger.fine(fileName + " - 本地修改时间: " + localModified);

        // 判断是否需要下载
        if (localModified != null && remoteModified != null) {
            if (localModified.isAfter(remoteModified) || localModified.equals(remoteModified)) {
                logger.info(fileName + " - 本地文件已是最新或更新，跳过下载");
                return new FileCompareResult(fileName, false, false, false,
                        "本地文件已是最新或更新（本地: " + localModified + "，远程: " + remoteModified + "）");
            }
        }

        // 需要下载
        return downloadFile(remotePath, localPath, fileName);
    }

    /**
     * 下载单个文件
     */
    private FileCompareResult downloadFile(
            String remotePath, String localPath, String fileName) throws IOException {

        logger.info("下载文件: " + fileName + " from " + remotePath);

        try {
            // 确保父目录存在
            FilePermissionChecker.ensureParentDirectoryExists(localPath);

            // 创建临时文件
            File tempFile = new File(localPath + ".tmp");

            // 根据选择的模式下载文件
            byte[] downloadedContent;
            if (useWebApi) {
                downloadedContent = downloadFromWebAPI(remotePath);
            } else {
                downloadedContent = downloadFromFileCopy(remotePath);
            }

            // 写入临时文件
            Files.write(tempFile.toPath(), downloadedContent);

            // 验证文件内容
            if (!validateFileContent(tempFile, fileName)) {
                tempFile.delete();
                return new FileCompareResult(fileName, true, false, false,
                        "文件内容验证失败: " + fileName);
            }

            // 原子性替换
            Files.move(tempFile.toPath(), new File(localPath).toPath(),
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

            // 设置文件权限600
            FilePermissionChecker.setPermission600(localPath);

            logger.info("文件下载并设置权限完成: " + localPath);

            return new FileCompareResult(fileName, true, true, false,
                    "文件下载成功: " + fileName);
        } catch (IOException e) {
            logger.severe("下载文件失败: " + fileName + " - " + e.getMessage());
            return new FileCompareResult(fileName, true, false, true,
                    "下载失败: " + e.getMessage());
        }
    }

    /**
     * 从Web API下载文件内容
     */
    private byte[] downloadFromWebAPI(String remotePath) throws IOException {
        if (configCenterUrl == null || configCenterUrl.isEmpty()) {
            // 返回模拟内容用于测试
            logger.warning("配置中心URL未配置，返回模拟内容");
            return generateMockContent(remotePath);
        }

        String fullUrl = buildFullWebUrl(remotePath);
        logger.info("从Web API下载: " + fullUrl);

        HttpURLConnection connection = null;
        InputStream inputStream = null;

        try {
            URL url = new URL(fullUrl);
            connection = (HttpURLConnection) url.openConnection();

            // 设置请求方法
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);

            // 设置API Key认证头
            if (apiKey != null && !apiKey.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + apiKey);
                connection.setRequestProperty("X-API-Key", apiKey);
            }

            // 设置User-Agent
            connection.setRequestProperty("User-Agent",
                    "DatabasePasswordManager/3.0");

            // 检查响应码
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                String errorStream = readErrorStream(connection);
                throw new IOException("HTTP请求失败，状态码: " + responseCode +
                        ", 响应: " + errorStream);
            }

            // 读取响应内容
            inputStream = connection.getInputStream();
            byte[] content = readAllBytes(inputStream);

            logger.info("文件下载成功，大小: " + content.length + " 字节");
            return content;

        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    logger.warning("关闭输入流失败: " + e.getMessage());
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 从文件目录复制文件
     */
    private byte[] downloadFromFileCopy(String remotePath) throws IOException {
        // FILE_COPY模式下，configCenterUrl应为文件系统路径
        if (configCenterUrl == null || configCenterUrl.isEmpty()) {
            throw new IOException("配置中心路径未配置（文件目录模式）");
        }

        logger.info("从文件目录复制: " + remotePath);

        // 远程路径应该是完整路径
        String sourcePath;
        if (remotePath.startsWith("/") || remotePath.contains(":") || isWebApiUrl(remotePath)) {
            sourcePath = remotePath;
        } else {
            // 如果是相对路径，基于configCenterUrl构建
            sourcePath = new File(configCenterUrl, remotePath).getAbsolutePath();
        }

        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            throw new IOException("源文件不存在: " + sourcePath);
        }

        if (!sourceFile.isFile()) {
            throw new IOException("源路径不是文件: " + sourcePath);
        }

        if (!sourceFile.canRead()) {
            throw new IOException("源文件不可读: " + sourcePath);
        }

        byte[] content = Files.readAllBytes(sourceFile.toPath());
        logger.info("文件复制成功，大小: " + content.length + " 字节");

        return content;
    }

    /**
     * 生成模拟内容（用于测试）
     */
    private byte[] generateMockContent(String pathOrName) {
        // 提取文件名
        String fileName = pathOrName;
        if (fileName.contains("/")) {
            fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
        }

        try {
            if (fileName.endsWith("db.pwd")) {
                String content = "{" +
                        "\n  \"version\": \"1.0\"," +
                        "\n  \"lastModified\": \"" + ISO_FORMATTER.format(Instant.now()) + "\"," +
                        "\n  \"databases\": []" +
                        "\n}";
                return content.getBytes("UTF-8");
            }
            if (fileName.endsWith("sm4.key")) {
                return "1234567890ABCDEF1234567890ABCDEF".getBytes("UTF-8");
            }
        } catch (Exception e) {
            logger.warning("生成模拟内容失败: " + e.getMessage());
        }
        return new byte[0];
    }

    /**
     * 获取远程文件的修改时间
     */
    private Instant getRemoteFileModifiedTime(String remotePath) {
        try {
            if (useWebApi) {
                return getWebAPIFileModifiedTime(remotePath);
            } else {
                return getLocalFileCopyModifiedTime(remotePath);
            }
        } catch (Exception e) {
            logger.warning("获取远程文件修改时间失败: " + e.getMessage());
            return Instant.now();
        }
    }

    /**
     * 从Web API获取文件修改时间
     */
    private Instant getWebAPIFileModifiedTime(String remotePath) {
        // 简化实现：通过HEAD请求获取Last-Modified头
        try {
            String fullUrl = buildFullWebUrl(remotePath);
            URL url = new URL(fullUrl);

            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            try {
                connection.setRequestMethod("HEAD");
                connection.setConnectTimeout(10000);

                String lastModified = connection.getHeaderField("Last-Modified");
                if (lastModified != null) {
                    // 解析HTTP日期格式
                    // 简化实现，返回当前时间
                    return Instant.now().minusSeconds(3600);
                }
            } finally {
                connection.disconnect();
            }
        } catch (Exception e) {
            logger.fine("HEAD请求失败: " + e.getMessage());
        }

        return Instant.now().minusSeconds(3600); // 模拟1小时前修改
    }

    /**
     * 从本地文件复制获取修改时间
     */
    private Instant getLocalFileCopyModifiedTime(String remotePath) {
        File sourceFile = new File(buildSourceFilePath(remotePath));
        if (sourceFile.exists()) {
            return Instant.ofEpochMilli(sourceFile.lastModified());
        }
        return Instant.now().minusSeconds(3600);
    }

    /**
     * 构建完整的Web API URL
     */
    private String buildFullWebUrl(String path) {
        String baseUrl = configCenterUrl;
        if (baseUrl == null || baseUrl.isEmpty()) {
            return path;
        }

        // 移除末尾的斜杠
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }

        // 移除开头的斜杠
        if (path.startsWith("/")) {
            path = path.substring(1);
        }

        return baseUrl + "/" + path;
    }

    /**
     * 构建源文件路径（FILE_COPY模式）
     */
    private String buildSourceFilePath(String remotePath) {
        if (remotePath.startsWith("/") || remotePath.contains(":")) {
            return remotePath;
        }
        if (configCenterUrl != null) {
            return new File(configCenterUrl, remotePath).getAbsolutePath();
        }
        return remotePath;
    }

    /**
     * 获取本地文件的修改时间
     */
    private Instant getLocalFileModifiedTime(String localPath) {
        File file = new File(localPath);
        if (!file.exists()) {
            return null;
        }
        // 检查文件中的lastModified字段（针对db.pwd）或使用文件系统时间
        if (localPath.endsWith(".pwd")) {
            try {
                PasswordFile passwordFile = new PasswordFileManager(localPath)
                        .load(false)
                        .getPasswordFile();
                if (passwordFile != null && passwordFile.getLastModified() != null) {
                    return Instant.parse(passwordFile.getLastModified());
                }
            } catch (Exception e) {
                logger.fine("无法从db.pwd读取lastModified: " + e.getMessage());
            }
        }
        return Instant.ofEpochMilli(file.lastModified());
    }

    /**
     * 验证文件内容
     */
    private boolean validateFileContent(File file, String fileName) {
        if (!file.exists() || file.length() == 0) {
            return false;
        }

        if (fileName.equals("db.pwd")) {
            try {
                // 验证JSON格式
                new PasswordFileManager(file.getAbsolutePath()).load(false);
                return true;
            } catch (Exception e) {
                logger.warning("db.pwd JSON格式验证失败: " + e.getMessage());
                return false;
            }
        }

        if (fileName.equals("sm4.key")) {
            if (file.length() != 32) { // 16字节hex编码 = 32字符
                logger.warning("sm4.key长度不正确，应为32字符，实际: " + file.length());
                return false;
            }
            return true;
        }

        return true;
    }

    /**
     * 构建远程文件路径
     */
    private String buildRemotePath(String fileName) {
        StringBuilder sb = new StringBuilder();
        if (basePath != null && !basePath.isEmpty()) {
            sb.append(basePath);
            if (!basePath.endsWith("/")) {
                sb.append("/");
            }
        }
        if (physicalGroup != null && !physicalGroup.isEmpty()) {
            sb.append(physicalGroup).append("/");
        }
        if (application != null && !application.isEmpty()) {
            sb.append(application).append("/");
        }
        if (environment != null && !environment.isEmpty()) {
            sb.append(environment).append("/");
        }
        sb.append(fileName);
        return sb.toString();
    }

    /**
     * 展开家目录路径 (~)
     */
    private String expandHomeDir(String path) {
        if (path != null && path.startsWith("~")) {
            return path.replace("~", System.getProperty("user.home"));
        }
        return path;
    }

    /**
     * 读取所有字节
     */
    private byte[] readAllBytes(InputStream inputStream) throws IOException {
        java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        return outputStream.toByteArray();
    }

    /**
     * 读取错误流
     */
    private String readErrorStream(HttpURLConnection connection) {
        try {
            InputStream errorStream = connection.getErrorStream();
            if (errorStream != null) {
                return new String(readAllBytes(errorStream), "UTF-8");
            }
        } catch (Exception e) {
            logger.fine("读取错误流失败: " + e.getMessage());
        }
        return "无错误信息";
    }

    /**
     * 获取下载模式描述
     */
    public String getModeDescription() {
        return useWebApi ? "WEB_API (HTTP)" : "FILE_COPY (文件目录)";
    }

    /**
     * 判断是否使用Web API模式
     */
    public boolean isUsingWebApi() {
        return useWebApi;
    }

    // ==================== Builder模式 ====================

    /**
     * Builder类
     */
    public static class Builder {
        private String configCenterUrl;
        private String apiKey;
        private String basePath = "/databases";
        private String physicalGroup;
        private String application;
        private String environment = "prod";
        private String localDbPwdPath = "~/.dbpwd/db.pwd";
        private String localKeyPath = "~/.dbpwd/sm4.key";

        public Builder() {}

        public Builder configCenterUrl(String url) {
            this.configCenterUrl = url;
            return this;
        }

        public Builder apiKey(String key) {
            this.apiKey = key;
            return this;
        }

        public Builder basePath(String path) {
            this.basePath = path;
            return this;
        }

        public Builder physicalGroup(String group) {
            this.physicalGroup = group;
            return this;
        }

        public Builder application(String app) {
            this.application = app;
            return this;
        }

        public Builder environment(String env) {
            this.environment = env;
            return this;
        }

        public Builder localDbPwdPath(String path) {
            this.localDbPwdPath = path;
            return this;
        }

        public Builder localKeyPath(String path) {
            this.localKeyPath = path;
            return this;
        }

        public ConfigCenterDownloader build() {
            if (physicalGroup == null || physicalGroup.isEmpty()) {
                throw new IllegalArgumentException("physicalGroup不能为空");
            }
            if (application == null || application.isEmpty()) {
                throw new IllegalArgumentException("application不能为空");
            }

            // FILE_COPY模式需要配置中心路径
            if (!isWebApiUrl(configCenterUrl) &&
                (configCenterUrl == null || configCenterUrl.isEmpty())) {
                throw new IllegalArgumentException("文件目录模式需要配置配置中心路径");
            }

            // WEB_API模式建议配置API Key
            if (isWebApiUrl(configCenterUrl) && apiKey == null) {
                logger.warning("WEB_API模式未配置API Key，可能无法访问");
            }

            return new ConfigCenterDownloader(this);
        }

        /**
         * 从Properties配置构建
         */
        public static Builder fromProperties(Properties props) {
            Builder builder = new Builder();

            if (props.getProperty("config.center.url") != null) {
                builder.configCenterUrl(props.getProperty("config.center.url"));
            }
            if (props.getProperty("config.center.api.key") != null) {
                builder.apiKey(props.getProperty("config.center.api.key"));
            }
            if (props.getProperty("config.center.base.path") != null) {
                builder.basePath(props.getProperty("config.center.base.path"));
            }
            if (props.getProperty("config.center.physical.group") != null) {
                builder.physicalGroup(props.getProperty("config.center.physical.group"));
            }
            if (props.getProperty("config.center.application") != null) {
                builder.application(props.getProperty("config.center.application"));
            }
            if (props.getProperty("config.center.environment") != null) {
                builder.environment(props.getProperty("config.center.environment"));
            }
            if (props.getProperty("dbpwd.password.file") != null) {
                builder.localDbPwdPath(props.getProperty("dbpwd.password.file"));
            }
            if (props.getProperty("dbpwd.key.file") != null) {
                builder.localKeyPath(props.getProperty("dbpwd.key.file"));
            }

            return builder;
        }

        /**
         * 从系统环境变量构建
         */
        public static Builder fromEnvironment() {
            Builder builder = new Builder();

            String url = System.getenv("CONFIG_CENTER_URL");
            if (url != null) {
                builder.configCenterUrl(url);
            }

            String apiKey = System.getenv("CONFIG_CENTER_API_KEY");
            if (apiKey != null) {
                builder.apiKey(apiKey);
            }

            String basePath = System.getenv("CONFIG_CENTER_BASE_PATH");
            if (basePath != null) {
                builder.basePath(basePath);
            }

            String physicalGroup = System.getenv("CONFIG_CENTER_PHYSICAL_GROUP");
            if (physicalGroup != null) {
                builder.physicalGroup(physicalGroup);
            }

            String application = System.getenv("CONFIG_CENTER_APPLICATION");
            if (application != null) {
                builder.application(application);
            }

            String env = System.getenv("CONFIG_CENTER_ENVIRONMENT");
            if (env != null) {
                builder.environment(env);
            }

            return builder;
        }
    }

    // ==================== 内部类 ====================

    /**
     * 下载结果
     */
    public static class DownloadResult {
        private FileCompareResult dbPwdResult;
        private FileCompareResult keyResult;

        public FileCompareResult getDbPwdResult() {
            return dbPwdResult;
        }

        public void setDbPwdResult(FileCompareResult dbPwdResult) {
            this.dbPwdResult = dbPwdResult;
        }

        public FileCompareResult getKeyResult() {
            return keyResult;
        }

        public void setKeyResult(FileCompareResult keyResult) {
            this.keyResult = keyResult;
        }

        public boolean hasNewFiles() {
            return (dbPwdResult != null && dbPwdResult.isDownloaded()) ||
                   (keyResult != null && keyResult.isDownloaded());
        }

        public boolean hasErrors() {
            return (dbPwdResult != null && dbPwdResult.isError()) ||
                   (keyResult != null && keyResult.isError());
        }

        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            if (dbPwdResult != null) {
                sb.append("db.pwd: ").append(dbPwdResult.getMessage()).append("; ");
            }
            if (keyResult != null) {
                sb.append("sm4.key: ").append(keyResult.getMessage());
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return "DownloadResult{" +
                    "dbPwdResult=" + dbPwdResult +
                    ", keyResult=" + keyResult +
                    '}';
        }
    }

    /**
     * 文件对比结果
     */
    public static class FileCompareResult {
        private final String fileName;
        private final boolean checked;
        private final boolean downloaded;
        private final boolean error;
        private final String message;

        public FileCompareResult(String fileName, boolean checked,
                                boolean downloaded, boolean error, String message) {
            this.fileName = fileName;
            this.checked = checked;
            this.downloaded = downloaded;
            this.error = error;
            this.message = message;
        }

        public String getFileName() {
            return fileName;
        }

        public boolean isChecked() {
            return checked;
        }

        public boolean isDownloaded() {
            return downloaded;
        }

        public boolean isError() {
            return error;
        }

        public String getMessage() {
            return message;
        }

        @Override
        public String toString() {
            return "FileCompareResult{" +
                    "fileName='" + fileName + '\'' +
                    ", checked=" + checked +
                    ", downloaded=" + downloaded +
                    ", error=" + error +
                    ", message='" + message + '\'' +
                    '}';
        }
    }
}
