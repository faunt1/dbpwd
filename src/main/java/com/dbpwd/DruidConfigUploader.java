package com.dbpwd;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

/**
 * Druid配置中心上传下载工具
 * 用于将配置文件上传到配置中心或从配置中心下载配置
 */
public class DruidConfigUploader {

    private static final Logger logger = Logger.getLogger(DruidConfigUploader.class.getName());

    private String configCenterUrl;
    private String apiKey;
    private String directory;
    private int timeout = 30000;  // 默认30秒
    private ConfigFileNameGenerator fileNameGenerator;

    /**
     * 默认构造函数
     */
    public DruidConfigUploader() {
    }

    /**
     * 全参构造函数
     */
    public DruidConfigUploader(String configCenterUrl, String apiKey) {
        this.configCenterUrl = configCenterUrl;
        this.apiKey = apiKey;
    }

    /**
     * 全参构造函数（带目录）
     */
    public DruidConfigUploader(String configCenterUrl, String apiKey, String directory) {
        this.configCenterUrl = configCenterUrl;
        this.apiKey = apiKey;
        this.directory = directory;
    }

    /**
     * 上传配置文件到配置中心
     */
    public boolean upload(String configFilePath, String fileName) throws Exception {
        File file = new File(configFilePath);
        if (!file.exists()) {
            throw new FileNotFoundException("配置文件不存在: " + configFilePath);
        }

        if (!file.isFile()) {
            throw new IllegalArgumentException("路径不是文件: " + configFilePath);
        }

        String uploadUrl = configCenterUrl + "/upload";

        logger.info("上传配置文件: " + configFilePath);
        logger.info("Upload URL: " + uploadUrl);
        logger.info("Directory: " + directory);
        logger.info("FileName: " + fileName);

        HttpURLConnection connection = null;
        OutputStream outputStream = null;

        try {
            URL url = new URL(uploadUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestProperty("X-API-Key", apiKey);
            connection.setDoOutput(true);

            // 构建multipart/form-data请求
            String boundary = "----WebKitFormBoundary" + System.currentTimeMillis();
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            outputStream = connection.getOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true);

            // 添加directory字段
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"directory\"").append("\r\n");
            writer.append("\r\n");
            writer.append(directory).append("\r\n");
            writer.flush();

            // 添加path字段
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"path\"").append("\r\n");
            writer.append("\r\n");
            writer.append(fileName).append("\r\n");
            writer.flush();

            // 添加file字段
            writer.append("--").append(boundary).append("\r\n");
            writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"").append(fileName).append("\"\r\n");
            writer.append("Content-Type: application/octet-stream").append("\r\n");
            writer.append("\r\n");
            writer.flush();

            // 写入文件内容
            try (FileInputStream fileInputStream = new FileInputStream(file)) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
            outputStream.flush();

            // 结束multipart
            writer.append("\r\n").append("--").append(boundary).append("--").append("\r\n");
            writer.flush();

            int responseCode = connection.getResponseCode();

            if (responseCode == 200 || responseCode == 201) {
                logger.info("上传配置成功");
                return true;
            } else {
                String errorMsg;
                try {
                    BufferedReader errorReader = new BufferedReader(
                            new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8));
                    StringBuilder errorResponse = new StringBuilder();
                    String line;
                    while ((line = errorReader.readLine()) != null) {
                        errorResponse.append(line);
                    }
                    errorReader.close();
                    errorMsg = errorResponse.toString();
                } catch (Exception e) {
                    errorMsg = "Unknown error";
                }
                throw new IOException("上传失败，响应码: " + responseCode + ", 错误: " + errorMsg);
            }

        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    logger.warning("关闭输出流失败: " + e.getMessage());
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 上传配置文件 (使用文件名)
     */
    public boolean upload(String configFilePath) throws Exception {
        File file = new File(configFilePath);
        return upload(configFilePath, file.getName());
    }

    /**
     * 从配置中心下载配置文件
     */
    public String download(String fileName) throws Exception {
        String downloadUrl = configCenterUrl + "/download";

        logger.info("Download URL: " + downloadUrl);
        logger.info("Directory: " + directory);
        logger.info("FileName: " + fileName);

        HttpURLConnection connection = null;
        try {
            // 构建带参数的URL
            String fullUrl = downloadUrl + "?directory=" + java.net.URLEncoder.encode(directory, "UTF-8") +
                    "&path=" + java.net.URLEncoder.encode(fileName, "UTF-8");

            URL url = new URL(fullUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestProperty("X-API-Key", apiKey);

            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine).append("\n");
                    }
                    logger.info("下载配置成功");
                    return response.toString();
                }
            } else {
                String errorMsg;
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        errorResponse.append(inputLine);
                    }
                    errorMsg = errorResponse.toString();
                }
                throw new IOException("下载失败，响应码: " + responseCode + ", 错误: " + errorMsg);
            }

        } catch (Exception e) {
            logger.severe("下载配置失败: " + e.getMessage());
            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 从配置中心下载配置文件 (指定目录)
     */
    public String download(String fileName, String downloadDirectory) throws Exception {
        String downloadUrl = configCenterUrl + "/download";

        logger.info("Download URL: " + downloadUrl);
        logger.info("Directory: " + downloadDirectory);
        logger.info("FileName: " + fileName);

        HttpURLConnection connection = null;
        try {
            String fullUrl = downloadUrl + "?directory=" + java.net.URLEncoder.encode(downloadDirectory, "UTF-8") +
                    "&path=" + java.net.URLEncoder.encode(fileName, "UTF-8");

            URL url = new URL(fullUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setRequestProperty("X-API-Key", apiKey);

            int responseCode = connection.getResponseCode();

            if (responseCode == 200) {
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine).append("\n");
                    }
                    logger.info("下载配置成功");
                    return response.toString();
                }
            } else {
                String errorMsg;
                try (BufferedReader in = new BufferedReader(
                        new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
                    StringBuilder errorResponse = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        errorResponse.append(inputLine);
                    }
                    errorMsg = errorResponse.toString();
                }
                throw new IOException("下载失败，响应码: " + responseCode + ", 错误: " + errorMsg);
            }

        } catch (Exception e) {
            logger.severe("下载配置失败: " + e.getMessage());
            throw e;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * 下载并保存配置文件到本地
     */
    public void downloadAndSave(String fileName, String localPath) throws Exception {
        String content = download(fileName);

        File file = new File(localPath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }

        logger.info("配置文件已保存到: " + file.getAbsolutePath());
    }

    /**
     * 使用curl上传配置文件 (指定文件名)
     */
    public boolean uploadWithCurl(String configFilePath, String fileName) throws Exception {
        File file = new File(configFilePath);
        if (!file.exists()) {
            throw new FileNotFoundException("配置文件不存在: " + configFilePath);
        }

        String uploadUrl = configCenterUrl + "/upload";
        String fullUrl = uploadUrl + "?directory=" + java.net.URLEncoder.encode(directory, "UTF-8") +
                "&path=" + java.net.URLEncoder.encode(fileName, "UTF-8");

        String[] command = {
                "curl",
                "-X", "POST",
                fullUrl,
                "-H", "X-API-Key: " + apiKey,
                "-F", "directory=" + directory,
                "-F", "path=" + fileName,
                "-F", "file=@" + configFilePath,
                "--connect-timeout", String.valueOf(timeout / 1000),
                "--max-time", String.valueOf(timeout / 1000)
        };

        String result = executeCommand(command, "上传");
        return result != null && !result.isEmpty();
    }

    /**
     * 使用curl上传配置文件 (使用文件名)
     */
    public boolean uploadWithCurl(String configFilePath) throws Exception {
        File file = new File(configFilePath);
        return uploadWithCurl(configFilePath, file.getName());
    }

    /**
     * 使用curl下载配置文件
     */
    public String downloadWithCurl(String fileName) throws Exception {
        String downloadUrl = configCenterUrl + "/download";
        String fullUrl = downloadUrl + "?directory=" + java.net.URLEncoder.encode(directory, "UTF-8") +
                "&path=" + java.net.URLEncoder.encode(fileName, "UTF-8");

        String[] command = {
                "curl",
                "-X", "GET",
                fullUrl,
                "-H", "X-API-Key: " + apiKey,
                "--connect-timeout", String.valueOf(timeout / 1000),
                "--max-time", String.valueOf(timeout / 1000)
        };

        return executeCommand(command, "下载");
    }

    /**
     * 使用curl下载并保存配置文件
     */
    public void downloadAndSaveWithCurl(String fileName, String localPath) throws Exception {
        String content = downloadWithCurl(fileName);

        File file = new File(localPath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }

        logger.info("配置文件已保存到: " + file.getAbsolutePath());
    }

    /**
     * 执行shell命令
     */
    private String executeCommand(String[] command, String operation) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);

        Process process = null;
        try {
            process = processBuilder.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                logger.info(operation + "成功");
                return output.toString();
            } else {
                logger.warning(operation + "失败，退出码: " + exitCode);
                throw new RuntimeException(operation + "失败，退出码: " + exitCode + "\n" + output.toString());
            }

        } catch (Exception e) {
            logger.severe("执行命令失败: " + e.getMessage());
            throw e;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    /**
     * 根据JDBC URL自动生成文件名并上传
     */
    public boolean uploadByUrl(String configFilePath, String jdbcUrl) throws Exception {
        File file = new File(configFilePath);
        String fileName = getFileNameGenerator().generate(jdbcUrl);
        return upload(configFilePath, fileName);
    }

    /**
     * 根据JDBC URL自动生成文件名并下载
     */
    public String downloadByUrl(String jdbcUrl) throws Exception {
        if (fileNameGenerator == null) {
            throw new IllegalStateException("未配置文件名生成器");
        }

        String fileName = fileNameGenerator.generate(jdbcUrl);
        String fullPath = fileNameGenerator.getFullPath(jdbcUrl);

        String[] parts = fullPath.split("/");

        if (parts.length != 2) {
            throw new IllegalStateException("生成的路径格式错误: " + fullPath);
        }

        return download(parts[1], parts[0]);
    }

    // ==================== Getters and Setters ====================

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

    public String getDirectory() {
        return directory;
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public ConfigFileNameGenerator getFileNameGenerator() {
        return fileNameGenerator;
    }

    public void setFileNameGenerator(ConfigFileNameGenerator fileNameGenerator) {
        this.fileNameGenerator = fileNameGenerator;
    }

    // ==================== Config Center Template & Password File Methods ====================

    private static final String TEMPLATES_DIR_SUFFIX = "/templates";
    private static final String DATABASE_DIR_SUFFIX = "/database";
    private static final String PASSWORD_FILE_NAME = "passwords.txt";

    /**
     * 上传模板文件到 templates/ 目录
     *
     * @param templateFile 本地模板文件路径
     * @param templateName 模板名称（文件名）
     * @return 是否上传成功
     * @throws Exception 上传异常
     */
    public boolean uploadTemplate(String templateFile, String templateName) throws Exception {
        String originalDirectory = this.directory;
        try {
            String templateDir = (originalDirectory != null ? originalDirectory : "/druid/config") + TEMPLATES_DIR_SUFFIX;
            this.directory = templateDir;
            logger.info("上传模板文件到 templates/ 目录: " + templateName);
            return upload(templateFile, templateName);
        } finally {
            this.directory = originalDirectory;
        }
    }

    /**
     * 上传密码文件到 database/ 目录
     *
     * @param passwordFile 本地密码文件路径
     * @return 是否上传成功
     * @throws Exception 上传异常
     */
    public boolean uploadPasswordFile(String passwordFile) throws Exception {
        return uploadPasswordFile(passwordFile, PASSWORD_FILE_NAME);
    }

    /**
     * 上传密码文件到 database/ 目录（指定文件名）
     *
     * @param passwordFile 本地密码文件路径
     * @param passwordFileName 密码文件名称
     * @return 是否上传成功
     * @throws Exception 上传异常
     */
    public boolean uploadPasswordFile(String passwordFile, String passwordFileName) throws Exception {
        String originalDirectory = this.directory;
        try {
            String databaseDir = (originalDirectory != null ? originalDirectory : "/druid/config") + DATABASE_DIR_SUFFIX;
            this.directory = databaseDir;
            logger.info("上传密码文件到 database/ 目录: " + passwordFileName);
            return upload(passwordFile, passwordFileName);
        } finally {
            this.directory = originalDirectory;
        }
    }

    /**
     * 下载模板文件
     *
     * @param templateName 模板名称
     * @return 模板内容
     * @throws Exception 下载异常
     */
    public String downloadTemplate(String templateName) throws Exception {
        String originalDirectory = this.directory;
        try {
            String templateDir = (originalDirectory != null ? originalDirectory : "/druid/config") + TEMPLATES_DIR_SUFFIX;
            return download(templateName, templateDir);
        } finally {
            this.directory = originalDirectory;
        }
    }

    /**
     * 下载模板文件（指定目录）
     *
     * @param templateName 模板名称
     * @param downloadDirectory 下载目录
     * @return 模板内容
     * @throws Exception 下载异常
     */
    public String downloadTemplate(String templateName, String downloadDirectory) throws Exception {
        String originalDirectory = this.directory;
        try {
            String templateDir = downloadDirectory + TEMPLATES_DIR_SUFFIX;
            return download(templateName, templateDir);
        } finally {
            this.directory = originalDirectory;
        }
    }

    /**
     * 下载密码文件
     *
     * @return 密码文件内容
     * @throws Exception 下载异常
     */
    public String downloadPasswordFile() throws Exception {
        return downloadPasswordFile(PASSWORD_FILE_NAME);
    }

    /**
     * 下载密码文件（指定文件名）
     *
     * @param passwordFileName 密码文件名称
     * @return 密码文件内容
     * @throws Exception 下载异常
     */
    public String downloadPasswordFile(String passwordFileName) throws Exception {
        String originalDirectory = this.directory;
        try {
            String databaseDir = (originalDirectory != null ? originalDirectory : "/druid/config") + DATABASE_DIR_SUFFIX;
            return download(passwordFileName, databaseDir);
        } finally {
            this.directory = originalDirectory;
        }
    }

    /**
     * 下载密码文件（指定目录）
     *
     * @param passwordFileName 密码文件名称
     * @param downloadDirectory 下载目录
     * @return 密码文件内容
     * @throws Exception 下载异常
     */
    public String downloadPasswordFile(String passwordFileName, String downloadDirectory) throws Exception {
        String originalDirectory = this.directory;
        try {
            String databaseDir = downloadDirectory + DATABASE_DIR_SUFFIX;
            return download(passwordFileName, databaseDir);
        } finally {
            this.directory = originalDirectory;
        }
    }

    /**
     * 下载并保存模板文件到本地
     *
     * @param templateName 模板名称
     * @param localPath 本地保存路径
     * @throws Exception 下载异常
     */
    public void downloadTemplateAndSave(String templateName, String localPath) throws Exception {
        String content = downloadTemplate(templateName);
        saveContentToFile(content, localPath);
    }

    /**
     * 下载并保存密码文件到本地
     *
     * @param localPath 本地保存路径
     * @throws Exception 下载异常
     */
    public void downloadPasswordFileAndSave(String localPath) throws Exception {
        downloadPasswordFileAndSave(PASSWORD_FILE_NAME, localPath);
    }

    /**
     * 下载并保存密码文件到本地（指定文件名）
     *
     * @param passwordFileName 密码文件名称
     * @param localPath 本地保存路径
     * @throws Exception 下载异常
     */
    public void downloadPasswordFileAndSave(String passwordFileName, String localPath) throws Exception {
        String content = downloadPasswordFile(passwordFileName);
        saveContentToFile(content, localPath);
    }

    /**
     * 保存内容到文件
     *
     * @param content 文件内容
     * @param localPath 本地路径
     * @throws IOException 写入异常
     */
    private void saveContentToFile(String content, String localPath) throws IOException {
        File file = new File(localPath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }

        logger.info("文件已保存到: " + file.getAbsolutePath());
    }
}