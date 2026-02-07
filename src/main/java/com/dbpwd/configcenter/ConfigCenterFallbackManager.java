package com.dbpwd.configcenter;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * 配置中心回退管理器
 *
 * 负责实现"配置中心下载优先、本地文件兜底"的策略：
 * 1. 先尝试从配置中心下载密码文件
 * 2. 如果配置中心不可用或下载失败，使用本地 .600 文件作为兜底
 * 3. 维护本地缓存，避免频繁访问配置中心
 *
 * 支持两种密码文件格式：
 * - 本地文件格式（三字段）：数据库:用户名:加密密码
 * - 配置中心格式（两字段）：username:encryptedPassword
 *
 * @author Druid DB Password Manager
 * @version 2.1
 */
public class ConfigCenterFallbackManager {

    private static final Logger logger = Logger.getLogger(ConfigCenterFallbackManager.class.getName());

    // 本地密码文件后缀（如：project.600）
    private static final String LOCAL_FILE_SUFFIX = ".600";

    // 单字段缓存：username -> encryptedPassword（配置中心格式）
    private final Map<String, String> singleFieldPasswordCache;

    // 多字段缓存：database:username -> encryptedPassword（本地文件格式）
    private final Map<String, String> multiFieldPasswordCache;

    // 配置中心配置
    private final String configCenterUrl;
    private final String apiKey;
    private final String baseDirectory;

    // 本地密码文件路径
    private final String localPasswordFilePath;

    // 上次同步时间
    private volatile long lastSyncTime = 0;

    // 同步间隔（毫秒），默认5分钟
    private final long syncInterval;

    // 是否强制使用本地文件（用于降级）
    private volatile boolean forceLocalFallback = false;

    /**
     * 构造函数（仅本地文件模式）
     *
     * @param localPasswordFilePath 本地密码文件路径
     */
    public ConfigCenterFallbackManager(String localPasswordFilePath) {
        this(null, null, null, localPasswordFilePath, 5 * 60 * 1000);
        this.forceLocalFallback = true;
    }

    /**
     * 构造函数（配置中心 + 本地文件兜底）
     *
     * @param configCenterUrl 配置中心 URL
     * @param apiKey API 密钥
     * @param baseDirectory 基础目录
     * @param localPasswordFilePath 本地密码文件路径
     */
    public ConfigCenterFallbackManager(String configCenterUrl, String apiKey, String baseDirectory,
                                       String localPasswordFilePath) {
        this(configCenterUrl, apiKey, baseDirectory, localPasswordFilePath, 5 * 60 * 1000);
    }

    /**
     * 全参构造函数
     *
     * @param configCenterUrl 配置中心 URL
     * @param apiKey API 密钥
     * @param baseDirectory 基础目录
     * @param localPasswordFilePath 本地密码文件路径
     * @param syncInterval 同步间隔（毫秒）
     */
    public ConfigCenterFallbackManager(String configCenterUrl, String apiKey, String baseDirectory,
                                       String localPasswordFilePath, long syncInterval) {
        this.configCenterUrl = configCenterUrl;
        this.apiKey = apiKey;
        this.baseDirectory = baseDirectory;
        this.localPasswordFilePath = localPasswordFilePath;
        this.syncInterval = syncInterval;
        this.singleFieldPasswordCache = new ConcurrentHashMap<>();
        this.multiFieldPasswordCache = new ConcurrentHashMap<>();

        // 初始化时加载
        initialize();
    }

    /**
     * 初始化管理器
     */
    private void initialize() {
        try {
            // 优先尝试从配置中心同步
            if (!forceLocalFallback && configCenterUrl != null && apiKey != null) {
                syncFromConfigCenter();
            }

            // 加载本地文件（补充或兜底）
            loadLocalPasswordFile();

            logger.info("配置中心回退管理器初始化完成");
            logger.info("单字段缓存密码数量: " + singleFieldPasswordCache.size());
            logger.info("多字段缓存密码数量: " + multiFieldPasswordCache.size());
            logger.info("本地文件路径: " + localPasswordFilePath);
            logger.info("配置中心: " + (forceLocalFallback ? "禁用（本地模式）" : "启用"));

        } catch (Exception e) {
            logger.warning("初始化失败，仅使用本地文件: " + e.getMessage());
            try {
                loadLocalPasswordFile();
            } catch (IOException ex) {
                logger.severe("加载本地密码文件失败: " + ex.getMessage());
            }
        }
    }

    /**
     * 从配置中心同步密码（两字段格式：username:encryptedPassword）
     *
     * @return 是否同步成功
     */
    private boolean syncFromConfigCenter() {
        if (forceLocalFallback) {
            logger.fine("强制使用本地文件模式，跳过配置中心同步");
            return false;
        }

        if (configCenterUrl == null || apiKey == null) {
            logger.fine("配置中心未配置，跳过同步");
            return false;
        }

        try {
            logger.info("开始从配置中心同步密码...");

            com.dbpwd.DruidConfigUploader uploader =
                new com.dbpwd.DruidConfigUploader(configCenterUrl, apiKey, baseDirectory);

            String passwordFileContent = uploader.downloadPasswordFile();

            // 解析密码文件内容（配置中心格式：两字段）
            PasswordStorageManager passwordStorage =
                PasswordStorageManager.loadFromString(passwordFileContent, false);

            // 更新单字段缓存（配置中心格式）
            Map<String, String> passwordMap = passwordStorage.getPasswordMap();
            singleFieldPasswordCache.clear();
            singleFieldPasswordCache.putAll(passwordMap);

            lastSyncTime = System.currentTimeMillis();
            logger.info("配置中心同步成功，获取 " + passwordMap.size() + " 个单字段密码映射");

            // 同时更新本地缓存文件（.600 文件，三字段格式）
            // 注意：这里需要转换，因为配置中心是两字段，本地是三字段
            // 我们可以根据需要将单字段缓存转换为多字段缓存

            return true;

        } catch (Exception e) {
            logger.warning("配置中心同步失败，将使用本地文件兜底: " + e.getMessage());
            return false;
        }
    }

    /**
     * 加载本地密码文件（.600 文件，三字段格式：database:username:encryptedPassword）
     */
    private void loadLocalPasswordFile() throws IOException {
        if (localPasswordFilePath == null || localPasswordFilePath.isEmpty()) {
            logger.warning("本地密码文件路径未配置");
            return;
        }

        File localFile = new File(localPasswordFilePath);
        if (!localFile.exists()) {
            logger.info("本地密码文件不存在: " + localPasswordFilePath);
            return;
        }

        logger.info("加载本地密码文件: " + localPasswordFilePath);

        PasswordStorageManager passwordStorage = PasswordStorageManager.load(localPasswordFilePath);
        Map<String, String> localMap = passwordStorage.getPasswordMap();

        // 本地文件使用多字段缓存（database:username -> encryptedPassword）
        // 本地文件中的密码优先级低于配置中心（如果已经从配置中心加载）
        // 这里我们将本地文件的内容合并到多字段缓存
        for (Map.Entry<String, String> entry : localMap.entrySet()) {
            String key = entry.getKey();
            String encryptedPassword = entry.getValue();

            // 判断 key 的格式
            if (key.contains(":")) {
                // 多字段格式（database:username），放入多字段缓存
                if (!multiFieldPasswordCache.containsKey(key)) {
                    multiFieldPasswordCache.put(key, encryptedPassword);
                    logger.fine("从本地文件添加多字段密码映射: " + key);
                }
            } else {
                // 单字段格式（username），放入单字段缓存
                if (!singleFieldPasswordCache.containsKey(key)) {
                    singleFieldPasswordCache.put(key, encryptedPassword);
                    logger.fine("从本地文件添加单字段密码映射: " + key);
                }
            }
        }

        logger.info("本地密码文件加载完成");
    }

    /**
     * 获取密码（带自动刷新）
     *
     * @param uid 数据库用户名或数据库:用户名
     * @return 加密密码，如果不存在返回 null
     */
    public String getPassword(String uid) {
        // 检查是否需要刷新
        if (!forceLocalFallback && shouldRefresh()) {
            try {
                syncFromConfigCenter();
            } catch (Exception e) {
                logger.warning("刷新配置中心失败: " + e.getMessage());
            }
        }

        // 优先从单字段缓存查找
        String password = singleFieldPasswordCache.get(uid);
        if (password != null) {
            return password;
        }

        // 从多字段缓存查找
        return multiFieldPasswordCache.get(uid);
    }

    /**
     * 获取密码（带自动刷新，指定数据库和用户名）
     *
     * @param database 数据库标识
     * @param username 用户名
     * @return 加密密码，如果不存在返回 null
     */
    public String getPassword(String database, String username) {
        // 检查是否需要刷新
        if (!forceLocalFallback && shouldRefresh()) {
            try {
                syncFromConfigCenter();
            } catch (Exception e) {
                logger.warning("刷新配置中心失败: " + e.getMessage());
            }
        }

        if (database == null || database.isEmpty()) {
            // 如果没有指定数据库，只按用户名查找
            return singleFieldPasswordCache.get(username);
        }

        // 优先从多字段缓存查找（本地文件格式：database:username）
        String key = database + ":" + username;
        String password = multiFieldPasswordCache.get(key);
        if (password != null) {
            return password;
        }

        // 回退到单字段缓存（配置中心格式：username）
        return singleFieldPasswordCache.get(username);
    }

    /**
     * 检查是否需要刷新
     */
    private boolean shouldRefresh() {
        long currentTime = System.currentTimeMillis();
        return (currentTime - lastSyncTime) > syncInterval;
    }

    /**
     * 获取所有密码映射（单字段）
     */
    public Map<String, String> getSingleFieldPasswords() {
        return new java.util.HashMap<>(singleFieldPasswordCache);
    }

    /**
     * 获取所有密码映射（多字段）
     */
    public Map<String, String> getMultiFieldPasswords() {
        return new java.util.HashMap<>(multiFieldPasswordCache);
    }

    /**
     * 刷新配置中心（强制同步）
     */
    public void refresh() {
        logger.info("强制刷新配置中心...");
        syncFromConfigCenter();
        try {
            loadLocalPasswordFile();
        } catch (IOException e) {
            logger.warning("加载本地文件失败: " + e.getMessage());
        }
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        singleFieldPasswordCache.clear();
        multiFieldPasswordCache.clear();
        logger.info("缓存已清空");
    }

    /**
     * 设置强制使用本地文件
     *
     * @param force 是否强制使用本地文件
     */
    public void setForceLocalFallback(boolean force) {
        this.forceLocalFallback = force;
        logger.info("强制本地文件模式: " + force);
    }

    /**
     * 检查密码是否存在于缓存中
     */
    public boolean contains(String uid) {
        return singleFieldPasswordCache.containsKey(uid)
            || multiFieldPasswordCache.containsKey(uid);
    }

    /**
     * 检查密码是否存在于缓存中（指定数据库和用户名）
     */
    public boolean contains(String database, String username) {
        if (database == null || database.isEmpty()) {
            return singleFieldPasswordCache.containsKey(username);
        }
        String key = database + ":" + username;
        return multiFieldPasswordCache.containsKey(key)
            || singleFieldPasswordCache.containsKey(username);
    }

    /**
     * 获取缓存中的密码数量
     */
    public int size() {
        return singleFieldPasswordCache.size() + multiFieldPasswordCache.size();
    }

    // ==================== Getters ====================

    public String getConfigCenterUrl() {
        return configCenterUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseDirectory() {
        return baseDirectory;
    }

    public String getLocalPasswordFilePath() {
        return localPasswordFilePath;
    }

    public long getSyncInterval() {
        return syncInterval;
    }

    public long getLastSyncTime() {
        return lastSyncTime;
    }

    public boolean isForceLocalFallback() {
        return forceLocalFallback;
    }
}
