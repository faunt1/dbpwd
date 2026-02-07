package com.dbpwd.configcenter;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 复合密码提供者
 *
 * 支持多字段格式的密码匹配：
 * 1. 本地文件格式：数据库:用户名:加密密码（三字段）
 * 2. 配置中心格式：用户名:加密密码（两字段）
 *
 * 使用方式：
 * - 单字段查找：getPassword(username) - 从配置中心获取
 * - 多字段查找：getPassword(database, username) - 从本地文件匹配
 *
 * @author Druid DB Password Manager
 * @version 2.0
 */
public class CompoundPasswordProvider {

    private static final Logger logger = Logger.getLogger(CompoundPasswordProvider.class.getName());

    // 单字段缓存：username -> encryptedPassword（配置中心格式）
    private final Map<String, String> singleFieldCache;

    // 多字段缓存：database:username -> encryptedPassword（本地文件格式）
    private final Map<String, String> multiFieldCache;

    // 原始多字段行列表：用于多字段匹配
    private final List<PasswordEntry> multiFieldEntries;

    /**
     * 密码条目（多字段）
     */
    public static class PasswordEntry {
        private final String database;
        private final String username;
        private final String encryptedPassword;

        public PasswordEntry(String database, String username, String encryptedPassword) {
            this.database = database;
            this.username = username;
            this.encryptedPassword = encryptedPassword;
        }

        public String getDatabase() {
            return database;
        }

        public String getUsername() {
            return username;
        }

        public String getEncryptedPassword() {
            return encryptedPassword;
        }

        /**
         * 获取复合 key：database:username
         */
        public String getCompositeKey() {
            return database + ":" + username;
        }
    }

    /**
     * 默认构造函数
     */
    public CompoundPasswordProvider() {
        this.singleFieldCache = new ConcurrentHashMap<>();
        this.multiFieldCache = new ConcurrentHashMap<>();
        this.multiFieldEntries = new java.util.ArrayList<>();
    }

    /**
     * 使用初始密码映射构造（单字段格式）
     *
     * @param passwordMap 密码映射（username -> encryptedPassword）
     */
    public CompoundPasswordProvider(Map<String, String> passwordMap) {
        this();
        if (passwordMap != null) {
            this.singleFieldCache.putAll(passwordMap);
        }
    }

    /**
     * 添加单字段密码映射（配置中心格式：username:encryptedPassword）
     *
     * @param username 用户名
     * @param encryptedPassword 加密密码
     */
    public void addSingleFieldMapping(String username, String encryptedPassword) {
        singleFieldCache.put(username, encryptedPassword);
        logger.fine("添加单字段映射: " + username);
    }

    /**
     * 添加单字段密码映射（配置中心格式）
     *
     * @param passwordMap 密码映射
     */
    public void addSingleFieldMappings(Map<String, String> passwordMap) {
        if (passwordMap != null) {
            singleFieldCache.putAll(passwordMap);
            logger.info("添加 " + passwordMap.size() + " 个单字段映射");
        }
    }

    /**
     * 添加多字段密码条目（本地文件格式：database:username:encryptedPassword）
     *
     * @param database 数据库标识
     * @param username 用户名
     * @param encryptedPassword 加密密码
     */
    public void addMultiFieldEntry(String database, String username, String encryptedPassword) {
        PasswordEntry entry = new PasswordEntry(database, username, encryptedPassword);
        String key = entry.getCompositeKey();

        multiFieldCache.put(key, encryptedPassword);
        multiFieldEntries.add(entry);
        logger.fine("添加多字段条目: " + key);
    }

    /**
     * 添加多字段密码条目（从字符串解析）
     *
     * @param line 格式：database:username:encryptedPassword
     */
    public void addMultiFieldEntry(String line) {
        // 解析三字段格式：database:username:encryptedPassword
        String[] parts = line.split(":");
        if (parts.length == 3) {
            addMultiFieldEntry(parts[0], parts[1], parts[2]);
        } else if (parts.length == 2) {
            // 两字段格式，视为配置中心格式
            addSingleFieldMapping(parts[0], parts[1]);
        } else {
            logger.warning("无法解析密码行: " + line);
        }
    }

    /**
     * 获取单字段密码（配置中心格式）
     *
     * @param username 用户名
     * @return 加密密码，如果不存在返回 null
     */
    public String getPassword(String username) {
        String password = singleFieldCache.get(username);
        if (password != null) {
            logger.fine("从单字段缓存获取密码: " + username);
        }
        return password;
    }

    /**
     * 获取多字段密码（本地文件格式，匹配数据库和用户名）
     *
     * @param database 数据库标识
     * @param username 用户名
     * @return 加密密码，如果不存在返回 null
     */
    public String getPassword(String database, String username) {
        if (database == null || database.isEmpty()) {
            logger.warning("数据库标识为空，无法查询多字段密码");
            return null;
        }

        String key = database + ":" + username;
        String password = multiFieldCache.get(key);

        if (password != null) {
            logger.fine("从多字段缓存获取密码: " + key);
        } else {
            logger.fine("未找到多字段密码: " + key);
        }

        return password;
    }

    /**
     * 获取密码（优先匹配多字段，回退到单字段）
     *
     * @param database 数据库标识（可为 null）
     * @param username 用户名
     * @return 加密密码，如果不存在返回 null
     */
    public String getPasswordWithFallback(String database, String username) {
        // 1. 优先尝试多字段匹配（本地文件格式）
        if (database != null && !database.isEmpty()) {
            String password = getPassword(database, username);
            if (password != null) {
                return password;
            }
        }

        // 2. 回退到单字段匹配（配置中心格式）
        return getPassword(username);
    }

    /**
     * 解析多字段密码文件内容
     *
     * @param content 文件内容
     * @return PasswordStorageManager 实例
     * @throws IOException 解析异常
     */
    public static PasswordStorageManager parseContent(String content) throws IOException {
        return PasswordStorageManager.loadFromString(content);
    }

    /**
     * 清空所有缓存
     */
    public void clear() {
        singleFieldCache.clear();
        multiFieldCache.clear();
        multiFieldEntries.clear();
        logger.info("所有缓存已清空");
    }

    /**
     * 获取单字段缓存大小
     */
    public int singleFieldSize() {
        return singleFieldCache.size();
    }

    /**
     * 获取多字段缓存大小
     */
    public int multiFieldSize() {
        return multiFieldCache.size();
    }

    /**
     * 检查单字段密码是否存在
     */
    public boolean containsSingleField(String username) {
        return singleFieldCache.containsKey(username);
    }

    /**
     * 检查多字段密码是否存在
     */
    public boolean containsMultiField(String database, String username) {
        if (database == null || database.isEmpty()) {
            return false;
        }
        return multiFieldCache.containsKey(database + ":" + username);
    }

    /**
     * 获取所有单字段密钥
     */
    public List<String> singleFieldKeys() {
        return new java.util.ArrayList<>(singleFieldCache.keySet());
    }

    /**
     * 获取所有多字段条目
     */
    public List<PasswordEntry> multiFieldEntries() {
        return new java.util.ArrayList<>(multiFieldEntries);
    }
}
