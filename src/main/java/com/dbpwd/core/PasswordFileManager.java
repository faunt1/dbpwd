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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
    private volatile long lastLoadedTime = 0;

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

        // 检查文件是否存在
        if (!file.exists()) {
            logger.warning("密码文件不存在，将创建新文件: " + filePath);
            this.passwordFile = new PasswordFile();
            save();
            return this;
        }

        // 验证文件权限
        if (verifyPermissions) {
            FilePermissionChecker.verifyPermission600(filePath);
        }

        // 读取文件内容
        String content = new String(Files.readAllBytes(file.toPath()));

        // 解析JSON
        try {
            this.passwordFile = objectMapper.readValue(content, PasswordFile.class);
            this.lastLoadedTime = file.lastModified();
            logger.info("密码文件加载成功: " + filePath + ", 包含 " +
                passwordFile.getDatabases().size() + " 个数据库");
        } catch (Exception e) {
            throw new IOException("解析密码文件失败: " + e.getMessage(), e);
        }

        return this;
    }

    /**
     * 刷新密码文件（如果文件被外部修改）
     *
     * @return 是否成功刷新
     * @throws IOException 刷新失败
     */
    public boolean refresh() throws IOException {
        File file = new File(filePath);

        if (!file.exists()) {
            logger.warning("密码文件不存在，无法刷新");
            return false;
        }

        long currentModified = file.lastModified();
        if (currentModified > lastLoadedTime) {
            logger.info("检测到密码文件已修改，重新加载");
            load(true);
            return true;
        }

        return false;
    }

    /**
     * 保存密码文件
     *
     * @throws IOException 保存失败
     */
    public void save() throws IOException {
        // 确保父目录存在
        FilePermissionChecker.ensureParentDirectoryExists(filePath);

        // 更新最后修改时间
        if (passwordFile != null) {
            passwordFile.touchLastModified();
        }

        // 先写入临时文件
        File file = new File(filePath);
        File tempFile = new File(filePath + ".tmp");

        try {
            // 写入临时文件
            objectMapper.writeValue(tempFile, passwordFile);

            // 设置临时文件权限
            FilePermissionChecker.setPermission600(tempFile);

            // 原子性替换
            Files.move(tempFile.toPath(), file.toPath(),
                      StandardCopyOption.REPLACE_EXISTING,
                      StandardCopyOption.ATOMIC_MOVE);

            // 确保目标文件权限正确
            FilePermissionChecker.setPermission600(filePath);

            this.lastLoadedTime = file.lastModified();
            logger.info("密码文件已保存: " + filePath);
        } catch (Exception e) {
            // 清理临时文件
            if (tempFile.exists()) {
                tempFile.delete();
            }
            throw new IOException("保存密码文件失败: " + e.getMessage(), e);
        }
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
     * 根据标签获取数据库条目列表
     *
     * @param tag 标签
     * @return 数据库条目列表
     */
    public List<DatabaseEntry> getByTag(String tag) {
        if (passwordFile == null) {
            return List.of();
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
            return List.of();
        }
        return passwordFile.getAllDatabases();
    }

    /**
     * 添加数据库条目
     *
     * @param entry 数据库条目
     */
    public void addDatabase(DatabaseEntry entry) {
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
        if (passwordFile == null) {
            passwordFile = new PasswordFile();
        }
        passwordFile.updateDatabase(entry);
    }

    /**
     * 删除数据库条目
     *
     * @param id 数据库ID
     * @return 是否删除成功
     */
    public boolean removeDatabase(String id) {
        if (passwordFile == null) {
            return false;
        }
        return passwordFile.removeDatabase(id);
    }

    /**
     * 解密密码
     *
     * @param sm4Key SM4密钥
     * @param password 加密密码
     * @return 明文密码
     * @throws Exception 解密失败
     */
    public static String decryptPassword(String sm4Key, String password) throws Exception {
        // 检查是否为ENC格式
        if (ENCFormat.isEncrypted(password)) {
            String algorithm = ENCFormat.extractAlgorithm(password);
            String cipherText = ENCFormat.extractCipherText(password);

            if ("SM4".equals(algorithm)) {
                return com.dbpwd.crypto.Sm4Cipher.decrypt(cipherText, sm4Key);
            } else {
                throw new IllegalArgumentException("不支持的加密算法: " + algorithm);
            }
        }

        // 如果不是加密格式，直接返回（明文）
        return password;
    }

    /**
     * 加密密码
     *
     * @param sm4Key SM4密钥
     * @param plainPassword 明文密码
     * @return ENC格式加密密码
     * @throws Exception 加密失败
     */
    public static String encryptPassword(String sm4Key, String plainPassword) throws Exception {
        String cipherText = com.dbpwd.crypto.Sm4Cipher.encrypt(plainPassword, sm4Key);
        return ENCFormat.formatSM4(cipherText);
    }

    /**
     * 获取数据库ID的解密密码
     *
     * @param sm4Key SM4密钥
     * @param dbId 数据库ID
     * @return 明文密码
     * @throws Exception 解密失败
     */
    public String getDecryptedPassword(String sm4Key, String dbId) throws Exception {
        DatabaseEntry entry = getById(dbId);
        if (entry == null) {
            throw new IllegalArgumentException("未找到数据库条目: " + dbId);
        }
        return decryptPassword(sm4Key, entry.getPassword());
    }

    /**
     * 对比两个密码文件的更新时间
     *
     * @param other 另一个密码文件管理器
     * @return 比较结果：<0表示当前文件较新，0表示相同，>0表示另一个文件较新
     */
    public int compareUpdateTime(PasswordFileManager other) {
        if (passwordFile == null || other == null || other.passwordFile == null) {
            return 0;
        }

        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;
        Instant thisTime = Instant.from(formatter.parse(passwordFile.getLastModified()));
        Instant otherTime = Instant.from(formatter.parse(other.passwordFile.getLastModified()));

        return thisTime.compareTo(otherTime) * -1; // 反转，返回正数表示other较新
    }

    /**
     * 合并另一个密码文件的内容（保留较新的条目）
     *
     * @param other 另一个密码文件管理器
     * @return 合并的条目数量
     */
    public int merge(PasswordFileManager other) {
        if (passwordFile == null || other == null || other.passwordFile == null) {
            return 0;
        }

        int mergedCount = 0;
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

        for (DatabaseEntry otherEntry : other.getAllDatabases()) {
            DatabaseEntry existing = getById(otherEntry.getId());

            if (existing == null) {
                // 新增
                addDatabase(otherEntry);
                mergedCount++;
            } else {
                // 比较更新时间
                Instant existingTime = Instant.from(formatter.parse(existing.getUpdatedAt()));
                Instant otherTime = Instant.from(formatter.parse(otherEntry.getUpdatedAt()));

                if (otherTime.isAfter(existingTime)) {
                    // 更新
                    updateDatabase(otherEntry);
                    mergedCount++;
                }
            }
        }

        if (mergedCount > 0) {
            logger.info("合并了 " + mergedCount + " 个数据库条目");
        }

        return mergedCount;
    }

    // ==================== Getters ====================

    public String getFilePath() {
        return filePath;
    }

    public PasswordFile getPasswordFile() {
        return passwordFile;
    }

    public void setPasswordFile(PasswordFile passwordFile) {
        this.passwordFile = passwordFile;
    }

    public boolean isEmpty() {
        return passwordFile == null || passwordFile.isEmpty();
    }

    public int size() {
        return passwordFile == null ? 0 : passwordFile.size();
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
     * 创建ObjectMapper实例
     */
    private ObjectMapper createObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        return mapper;
    }

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
}
