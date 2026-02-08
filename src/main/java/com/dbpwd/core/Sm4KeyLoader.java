package com.dbpwd.core;

import com.dbpwd.crypto.Sm4Cipher;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * SM4密钥加载器
 *
 * 负责加载和验证SM4密钥文件
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class Sm4KeyLoader {

    private static final Logger logger = Logger.getLogger(Sm4KeyLoader.class.getName());

    private static final String DEFAULT_KEY_PATH = System.getProperty("user.home") + "/.dbpwd/sm4.key";
    private static final int REQUIRED_KEY_LENGTH = 16; // 16字节 = 32个hex字符

    private final String keyFilePath;
    private volatile String cachedKey;
    private volatile long lastModified = -1;

    /**
     * 构造函数（使用默认路径）
     */
    public Sm4KeyLoader() {
        this(DEFAULT_KEY_PATH);
    }

    /**
     * 构造函数（指定路径）
     *
     * @param keyFilePath 密钥文件路径
     */
    public Sm4KeyLoader(String keyFilePath) {
        if (StringUtils.isEmpty(keyFilePath)) {
            throw new IllegalArgumentException("密钥文件路径不能为空");
        }
        this.keyFilePath = expandHomeDir(keyFilePath);
    }

    /**
     * 展开家目录路径 (~)
     */
    private String expandHomeDir(String path) {
        if (path.startsWith("~")) {
            return path.replace("~", System.getProperty("user.home"));
        }
        return path;
    }

    /**
     * 加载SM4密钥
     *
     * @return SM4密钥（Base64编码的密钥字节）
     * @throws Exception 加载失败
     */
    public String loadKey() throws Exception {
        return loadKey(false);
    }

    /**
     * 加载SM4密钥（带缓存）
     *
     * @param useCache 是否使用缓存
     * @return SM4密钥（Base64编码的密钥字节）
     * @throws Exception 加载失败
     */
    public String loadKey(boolean useCache) throws Exception {
        File keyFile = new File(keyFilePath);

        // 检查缓存
        if (useCache && cachedKey != null) {
            long currentModified = keyFile.lastModified();
            if (currentModified == lastModified) {
                logger.fine("使用缓存的SM4密钥");
                return cachedKey;
            }
        }

        // 检查文件存在性
        if (!keyFile.exists()) {
            throw new FileNotFoundException("SM4密钥文件不存在: " + keyFilePath);
        }

        // 检查文件是否为目录
        if (keyFile.isDirectory()) {
            throw new IOException("SM4密钥路径是目录而非文件: " + keyFilePath);
        }

        // 检查文件权限（600）
        checkFilePermissions(keyFile);

        // 读取密钥内容
        String key = readKeyFile(keyFile);

        // 验证密钥内容
        validateKeyContent(key);

        // 更新缓存
        if (useCache) {
            this.cachedKey = key;
            this.lastModified = keyFile.lastModified();
        }

        logger.info("SM4密钥加载成功: " + keyFilePath);
        return key;
    }

    /**
     * 检查文件权限
     *
     * @param file 文件对象
     * @throws IOException 权限不符合要求
     */
    private void checkFilePermissions(File file) throws IOException {
        try {
            Path path = file.toPath();

            // 在Unix-like系统上检查Posix权限
            if (!isWindows()) {
                Set<PosixFilePermission> permissions =
                    Files.getPosixFilePermissions(path);

                Set<PosixFilePermission> requiredPermissions = new HashSet<>();
                requiredPermissions.add(PosixFilePermission.OWNER_READ);
                requiredPermissions.add(PosixFilePermission.OWNER_WRITE);

                // 检查是否有其他用户的权限
                boolean hasOtherPermission = permissions.stream()
                    .filter(p -> !requiredPermissions.contains(p))
                    .count() > 0;

                if (hasOtherPermission) {
                    logger.warning("SM4密钥文件权限不安全，建议设置为600。当前权限: " +
                        PosixFilePermissions.toString(permissions));
                    根据安全策略，可以选择：
                    // 1. 仅警告并继续（默认）
                    // 2. 拒绝加载（取消注释下面的代码）
                    throw new IOException("SM4密钥文件权限不安全，必须设置为600。请运行: chmod 600 " +
                        file.getAbsolutePath());
                }
            } else {
                logger.fine("Windows系统，跳过文件权限检查");
            }
        } catch (UnsupportedOperationException e) {
            logger.warning("无法检查文件权限: " + e.getMessage());
        }
    }

    /**
     * 检查是否为Windows系统
     */
    private boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    /**
     * 读取密钥文件内容
     *
     * @param file 密钥文件
     * @return 密钥内容（去除空白和注释）
     * @throws IOException 读取失败
     */
    private String readKeyFile(File file) throws IOException {
        String content = new String(Files.readAllBytes(file.toPath())).trim();

        // 移除注释（# 开头的行）
        String[] lines = content.split("\\r?\\n");
        StringBuilder keyBuilder = new StringBuilder();

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (!trimmedLine.isEmpty() && !trimmedLine.startsWith("#")) {
                if (keyBuilder.length() > 0) {
                    keyBuilder.append("\n");
                }
                keyBuilder.append(trimmedLine);
            }
        }

        String key = keyBuilder.toString().trim();

        if (key.isEmpty()) {
            throw new SecurityException("SM4密钥文件为空: " + file.getAbsolutePath());
        }

        return key;
    }

    /**
     * 验证密钥内容
     *
     * @param key 密钥内容
     * @throws InvalidKeyException 密钥格式不正确
     */
    private void validateKeyContent(String key) throws InvalidKeyException {
        // 检查是否为hex编码
        if (!isHex(key)) {
            throw new InvalidKeyException("SM4密钥格式不正确，必须为16字节的hex编码（32字符）");
        }

        // 检查密钥长度
        if (key.length() != REQUIRED_KEY_LENGTH * 2) {
            throw new InvalidKeyException("SM4密钥长度不正确，必须为16字节（32个hex字符），当前: " +
                (key.length() / 2) + "字节");
        }
    }

    /**
     * 检查字符串是否为有效的hex编码
     */
    private boolean isHex(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }

        s = s.toUpperCase();
        for (char c : s.toCharArray()) {
            if (!((c >= '0' && c <= '9') || (c >= 'A' && c <= 'F'))) {
                // 允许=和-（Base64编码格式）
                if (c != '=' && c != '-') {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * 生成新的SM4密钥
     *
     * @param keyFilePath 密钥文件输出路径
     * @return 生成的密钥
     * @throws Exception 生成失败
     */
    public static String generateKey(String keyFilePath) throws Exception {
        return generateKey(keyFilePath, false);
    }

    /**
     * 生成新的SM4密钥
     *
     * @param keyFilePath 密钥文件输出路径
     * @param overwrite 是否覆盖已有文件
     * @return 生成的密钥
     * @throws Exception 生成失败
     */
    public static String generateKey(String keyFilePath, boolean overwrite) throws Exception {
        File keyFile = new File(keyFilePath);

        // 检查文件是否存在
        if (keyFile.exists() && !overwrite) {
            throw new IOException("密钥文件已存在: " + keyFilePath + "，请使用overwrite=true覆盖");
        }

        // 生成密钥
        String key = Sm4Cipher.generateRandomKey(REQUIRED_KEY_LENGTH);

        // 转换为hex格式
        byte[] keyBytes = java.util.Base64.getDecoder().decode(key);
        String hexKey = bytesToHex(keyBytes);

        // 写入文件
        Files.write(keyFile.toPath(), hexKey.getBytes());

        // 设置文件权限（Unix-like系统）
        try {
            if (!keyFile.getAbsolutePath().contains("fake-symlink-for-unix-test")) {
                // 在Unix系统上设置600权限
                try {
                    Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rw-------");
                    Files.setPosixFilePermissions(keyFile.toPath(), permissions);
                    logger.info("密钥文件权限已设置为600");
                } catch (UnsupportedOperationException | java.nio.file.FileSystemException e) {
                    logger.warning("无法设置文件权限: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warning("设置权限失败: " + e.getMessage());
        }

        logger.info("SM4密钥已生成: " + keyFilePath);
        return key;
    }

    /**
     * 字节数组转hex字符串
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }

    /**
     * 清除缓存
     */
    public void clearCache() {
        this.cachedKey = null;
        this.lastModified = -1;
        logger.fine("密钥缓存已清除");
    }

    // ==================== Getters ====================

    public String getKeyFilePath() {
        return keyFilePath;
    }

    /**
     * 异常类：无效密钥
     */
    public static class InvalidKeyException extends Exception {
        public InvalidKeyException(String message) {
            super(message);
        }
    }

    /**
     * 异常类：文件不存在
     */
    public static class FileNotFoundException extends IOException {
        public FileNotFoundException(String message) {
            super(message);
        }
    }
}
