package com.dbpwd.core;

import com.dbpwd.core.Sm4KeyLoader.InvalidKeyException;
import com.dbpwd.model.DatabaseEntry;
import com.dbpwd.model.ENCFormat;
import com.dbpwd.model.PasswordFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.nio.file.StandardOpenOption;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * 密码文件管理器
 *
 * 负责密码文件的加载、保存、查询等操作
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class PasswordFileManager {

    private static final Logger logger = Logger.getLogger(PasswordFileManager.class.getName());

    private final String filePath;
    private PasswordFile passwordFile;
    private final ObjectMapper objectMapper;

    /**
     * 构造函数
     *
     * @param filePath 密码文件路径
     */
    public PasswordFileManager(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            throw new IllegalArgumentException("密码文件路径不能为空");
        }
        this.filePath = expandHomeDir(filePath);
        this.objectMapper = createObjectMapper();
    }

    /**
     * 加载密码文件
     *
     * @return PasswordFileManager实例
     * @throws IOException 加载失败
     */
    public PasswordFileManager load() throws IOException {
        return load(false);
    }

    /**
     * 加载密码文件
     *
     * @param verifyPermissions 是否验证文件权限
     * @return PasswordFileManager实例
     * @throws IOException 加载失败
     */
    public PasswordFileManager load(boolean verifyPermissions) throws IOException {
        File file = new File(filePath);

        if (!file.exists()) {
            // 文件不存在，创建新的空文件
            passwordFile = new PasswordFile();
            logger.info("密码文件不存在，创建新的密码文件: " + filePath);
            return this;
        }

        if (file.isDirectory()) {
            throw new IOException("密码路径是目录而非文件: " + filePath);
        }

        // 验证文件权限
        if (verifyPermissions) {
            checkFilePermissions(file);
        }

        // 读取文件内容
        String jsonContent = new String(Files.readAllBytes(file.toPath()), "UTF-8")
                .trim();

        if (jsonContent.isEmpty()) {
            passwordFile = new PasswordFile();
            logger.warning("密码文件为空，初始化为空密码文件");
            return this;
        }

        // 解析JSON
        try {
            passwordFile = objectMapper.readValue(jsonContent, PasswordFile.class);
            logger.info("密码文件加载成功: " + filePath + "，包含 " +
                passwordFile.size() + " 个数据库配置");
        } catch (Exception e) {
            throw new IOException("解析密码文件失败: " + e.getMessage(), e);
        }

        return this;
    }

    /**
     * 保存密码文件
     *
     * 更新lastModified时间戳
     *
     * @throws IOException 保存失败
     */
    public void save() throws IOException {
        if (passwordFile == null) {
            passwordFile = new PasswordFile();
        }

        // 更新时间戳
        passwordFile.touchLastModified();

        // 序列化为JSON
        String json = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(passwordFile);

        // 写入临时文件，然后重命名（原子性操作）
        File file = new File(filePath);
        File tempFile = new File(filePath + ".tmp");

        Files.write(tempFile.toPath(), json.getBytes("UTF-8"),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // 重命名
        Files.move(tempFile.toPath(), file.toPath(),
                StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);

        // 设置文件权限（Unix-like系统）
        try {
            if (!isWindows()) {
                java.util.Set<java.nio.file.attribute.PosixFilePermission> permissions =
                    java.nio.file.attribute.PosixFilePermissions.fromString("rw-------");
                Files.setPosixFilePermissions(file.toPath(), permissions);
                logger.fine("密码文件权限已设置为600");
            }
        } catch (UnsupportedOperationException | java.nio.file.FileSystemException e) {
            logger.warning("无法设置文件权限: " + e.getMessage());
        }

        logger.info("密码文件已保存: " + filePath);
    }

    /**
     * 添加数据库条目
     *
     * @param entry 数据库条目
     */
    public void addDatabase(DatabaseEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("DatabaseEntry不能为null");
        }

        if (passwordFile == null) {
            passwordFile = new PasswordFile();
        }

        passwordFile.addDatabase(entry);
    }

    /**
     * 更新数据库条目
     *
     * @param entry 数据库条目
     */
    public void updateDatabase(DatabaseEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("DatabaseEntry不能为null");
        }

        if (passwordFile == null) {
            passwordFile = new PasswordFile();
        }

        passwordFile.updateDatabase(entry);
    }

    /**
     * 删除数据库条目
     *
     * @param id 数据库ID
     * @return true=删除成功，false=不存在
     */
    public boolean removeDatabase(String id) {
        if (passwordFile == null) {
            return false;
        }
        return passwordFile.removeDatabase(id);
    }

    /**
     * 根据ID获取数据库条目
     *
     * @param id 数据库ID
     * @return 数据库条目，不存在返回null
     */
    public DatabaseEntry getById(String id) {
        if (passwordFile == null) {
            return null;
        }
        return passwordFile.getById(id);
    }

    /**
     * 根据URL获取数据库条目
     *
     * @param url JDBC URL
     * @return 数据库条目，不存在返回null
     */
    public DatabaseEntry getByUrl(String url) {
        if (passwordFile == null) {
            return null;
        }
        return passwordFile.getByUrl(url);
    }

    /**
     * 根据用户名获取数据库条目列表
     *
     * @param username 用户名
     * @return 数据库条目列表
     */
    public List<DatabaseEntry> getByUsername(String username) {
        if (passwordFile == null) {
            return Collections.emptyList();
        }
        return passwordFile.getByUsername(username);
    }

    /**
     * 根据标签获取数据库条目列表
     *
     * @param tag 标签
     * @return 数据库条目列表
     */
    public List<DatabaseEntry> getByTag(String tag) {
        if (passwordFile == null) {
            return Collections.emptyList();
        }
        return passwordFile.getByTag(tag);
    }

    /**
     * 获取所有数据库条目
     *
     * @return 数据库条目列表
     */
    public List<DatabaseEntry> getAllDatabases() {
        if (passwordFile == null) {
            return Collections.emptyList();
        }
        return passwordFile.getAllDatabases();
    }

    /**
     * 获取数据库条目数量
     *
     * @return 数量
     */
    public int size() {
        if (passwordFile == null) {
            return 0;
        }
        return passwordFile.size();
    }

    /**
     * 检查是否为空
     *
     * @return true=空，false=非空
     */
    public boolean isEmpty() {
        if (passwordFile == null) {
            return true;
        }
        return passwordFile.isEmpty();
    }

    /**
     * 清空所有数据库条目
     */
    public void clear() {
        if (passwordFile != null) {
            passwordFile.clear();
        }
    }

    /**
     * 解密指定数据库的密码
     *
     * @param dbId    数据库ID
     * @param sm4Key  SM4密钥
     * @return 解密后的明文密码
     * @throws Exception 解密失败
     */
    public String getDecryptedPassword(String dbId, String sm4Key) throws Exception {
        DatabaseEntry entry = getById(dbId);
        if (entry == null) {
            throw new IllegalArgumentException("未找到数据库配置: " + dbId);
        }

        String password = entry.getPassword();
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("数据库密码为空: " + dbId);
        }

        // 如果是加密格式，解密
        if (ENCFormat.isEncrypted(password)) {
            String cipherText = ENCFormat.extractCipherText(password);
            return com.dbpwd.crypto.Sm4Cipher.decrypt(cipherText, sm4Key);
        }

        // 明文密码直接返回
        return password;
    }

    /**
     * 加密指定数据库的密码
     *
     * @param dbId    数据库ID
     * @param sm4Key  SM4密钥
     * @throws Exception 加密失败
     */
    public void encryptPassword(String dbId, String sm4Key) throws Exception {
        DatabaseEntry entry = getById(dbId);
        if (entry == null) {
            throw new IllegalArgumentException("未找到数据库配置: " + dbId);
        }

        String plainPassword = entry.getPassword();
        if (plainPassword == null || plainPassword.isEmpty()) {
            throw new IllegalArgumentException("数据库密码为空: " + dbId);
        }

        // 如果已经是加密格式，跳过
        if (ENCFormat.isEncrypted(plainPassword)) {
            logger.fine("数据库密码已经是加密格式: " + dbId);
            return;
        }

        // 加密
        String cipherText = com.dbpwd.crypto.Sm4Cipher.encrypt(plainPassword, sm4Key);
        entry.setPassword(ENCFormat.formatSM4(cipherText));
        entry.setEncrypted(true);
    }

    /**
     * 获取最后的加载时间
     *
     * @return 最后加载时间戳（毫秒）
     */
    public long getLastLoadedTime() {
        return lastLoadedTime;
    }

    /**
     * 获取文件路径
     *
     * @return 文件路径
     */
    public String getFilePath() {
        return filePath;
    }

    /**
     * 获取密码文件对象（内部使用）
     *
     * @return PasswordFile对象
     */
    public PasswordFile getPasswordFile() {
        return passwordFile;
    }

    // ==================== 私有辅助方法 ====================

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
     * 检查文件权限
     */
    private void checkFilePermissions(File file) throws IOException {
        try {
            java.nio.file.Path path = file.toPath();

            // 在Unix-like系统上检查Posix权限
            Set<java.nio.file.attribute.PosixFilePermission> permissions =
                Files.getPosixFilePermissions(path);

            Set<java.nio.file.attribute.PosixFilePermission> requiredPermissions =
                new java.util.HashSet<>();
            requiredPermissions.add(java.nio.file.attribute.PosixFilePermission.OWNER_READ);
            requiredPermissions.add(java.nio.file.attribute.PosixFilePermission.OWNER_WRITE);

            // 检查是否有其他用户的权限
            boolean hasOtherPermission = permissions.stream()
                .filter(p -> !requiredPermissions.contains(p))
                .count() > 0;

            if (hasOtherPermission) {
                logger.warning("密码文件权限不安全，建议设置为600。当前权限: " +
                    java.nio.file.attribute.PosixFilePermissions.toString(permissions));
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
     * 创建配置好的ObjectMapper
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

    // ==================== 静态工厂方法 ====================

    /**
     * 工厂方法：从文件路径加载
     *
     * @param filePath 文件路径
     * @return PasswordFileManager实例
     * @throws IOException 加载失败
     */
    public static PasswordFileManager loadFromFile(String filePath) throws IOException {
        PasswordFileManager manager = new PasswordFileManager(filePath);
        manager.load();
        return manager;
    }

    /**
     * 安全加载文件（失败时返回新实例）
     *
     * @param filePath 文件路径
     * @return PasswordFileManager实例
     */
    public static PasswordFileManager loadFromFileSafe(String filePath) {
        try {
            return loadFromFile(filePath);
        } catch (IOException e) {
            Logger.getLogger(PasswordFileManager.class.getName())
                .warning("加载密码文件失败: " + filePath + " - " + e.getMessage());
            PasswordFileManager manager = new PasswordFileManager(filePath);
            manager.passwordFile = new PasswordFile();
            return manager;
        }
    }

    /**
     * 检查文件是否需要重新加载
     *
     * @param filePath 文件路径
     * @param lastModified 上次修改时间
     * @return true=需要重新加载，false=不需重新加载
     */
    public static boolean needsReload(String filePath, long lastModified) {
        File file = new File(filePath);
        if (!file.exists()) {
            return false;
        }
        return file.lastModified() > lastModified;
    }

    /**
     * 获取文件修改时间
     *
     * @param filePath 文件路径
     * @return 文件修改时间（毫秒）
     */
    public static long getFileModifiedTime(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return 0;
        }
        return file.lastModified();
    }
}
