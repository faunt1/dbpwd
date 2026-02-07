package com.dbpwd.configcenter;

import com.dbpwd.DruidEncryptedPasswordCallback;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 密码存储管理器
 * 负责密码文件的读写、加密解密、查询操作
 *
 * 支持两种密码文件格式：
 * 1. 本地文件格式（三字段）：数据库:用户名:加密密码
 *    按项目维度，单个文件多行，每行对应一个连接
 *    示例：mysql_prod:root:[16]r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=
 *
 * 2. 配置中心格式（两字段）：username:encryptedPassword
 *    按数据库用户维度管理，每个文件只保存单个数据库+用户名+密码
 *    示例：root:[16]r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=
 */
public class PasswordStorageManager {

    private static final Logger logger = Logger.getLogger(PasswordStorageManager.class.getName());
    private static final String PASSWORD_FILE_CHARSET = "UTF-8";
    private static final String DEFAULT_ALGORITHM = "SM4";

    private Map<String, String> passwordMap;

    /**
     * 默认构造函数
     */
    public PasswordStorageManager() {
        this.passwordMap = new HashMap<>();
    }

    /**
     * 构造函数（已加载密码映射）
     */
    public PasswordStorageManager(Map<String, String> passwordMap) {
        this.passwordMap = new HashMap<>(passwordMap);
    }

    /**
     * 从文件加载密码映射
     *
     * @param filePath 密码文件路径
     * @return PasswordStorageManager 实例
     * @throws IOException 读取异常
     */
    public static PasswordStorageManager load(String filePath) throws IOException {
        Map<String, String> map = new HashMap<>();
        File file = new File(filePath);

        if (!file.exists()) {
            throw new FileNotFoundException("密码文件不存在: " + filePath);
        }

        // 判断文件类型：本地文件（三字段）还是配置中心文件（两字段）
        boolean isLocalFile = filePath.endsWith(".600") || filePath.contains(".600");

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), PASSWORD_FILE_CHARSET))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // 跳过空行和注释行（# 或 ; 开头）
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                    continue;
                }

                if (isLocalFile) {
                    // 本地文件格式：database:username:encryptedPassword
                    parseLocalFileLine(line, lineNumber, map);
                } else {
                    // 配置中心格式：username:encryptedPassword
                    parseConfigCenterLine(line, lineNumber, map);
                }
            }
        }

        logger.info("密码文件加载成功: " + filePath + ", 共 " + map.size() + " 个密码映射");
        return new PasswordStorageManager(map);
    }

    /**
     * 解析本地文件行（三字段格式）
     *
     * @param line 行内容
     * @param lineNumber 行号
     * @param map 密码映射
     */
    private static void parseLocalFileLine(String line, int lineNumber, Map<String, String> map) {
        // 三字段格式：database:username:encryptedPassword
        String[] parts = line.split(":");
        if (parts.length == 3) {
            String database = parts[0].trim();
            String username = parts[1].trim();
            String encryptedPassword = parts[2].trim();

            if (!database.isEmpty() && !username.isEmpty() && !encryptedPassword.isEmpty()) {
                // 使用 composite key：database:username
                String key = database + ":" + username;
                map.put(key, encryptedPassword);
            } else {
                logger.warning("密码文件第 " + lineNumber + " 行格式错误（字段为空）: " + line);
            }
        } else {
            logger.warning("密码文件第 " + lineNumber + " 行格式错误（应为三字段 database:username:password）: " + line);
        }
    }

    /**
     * 解析配置中心行（两字段格式）
     *
     * @param line 行内容
     * @param lineNumber 行号
     * @param map 密码映射
     */
    private static void parseConfigCenterLine(String line, int lineNumber, Map<String, String> map) {
        // 两字段格式：username:encryptedPassword
        int colonPos = line.indexOf(':');
        if (colonPos <= 0) {
            logger.warning("密码文件第 " + lineNumber + " 行格式错误: " + line);
            return;
        }

        String username = line.substring(0, colonPos).trim();
        String encryptedPassword = line.substring(colonPos + 1).trim();

        if (!username.isEmpty() && !encryptedPassword.isEmpty()) {
            map.put(username, encryptedPassword);
        }
    }

    /**
     * 从字符串内容加载密码映射
     *
     * @param content 密码文件内容
     * @return PasswordStorageManager 实例
     * @throws IOException 读取异常
     */
    public static PasswordStorageManager loadFromString(String content) throws IOException {
        Map<String, String> map = new HashMap<>();

        String[] lines = content.split("\n");
        for (String line : lines) {
            line = line.trim();

            // 跳过空行和注释行
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                continue;
            }

            // 判断是三字段还是两字段格式
            String[] parts = line.split(":");
            if (parts.length == 3) {
                // 本地文件格式：database:username:encryptedPassword
                String database = parts[0].trim();
                String username = parts[1].trim();
                String encryptedPassword = parts[2].trim();

                if (!database.isEmpty() && !username.isEmpty() && !encryptedPassword.isEmpty()) {
                    String key = database + ":" + username;
                    map.put(key, encryptedPassword);
                }
            } else if (parts.length == 2) {
                // 配置中心格式：username:encryptedPassword
                String username = parts[0].trim();
                String encryptedPassword = parts[1].trim();

                if (!username.isEmpty() && !encryptedPassword.isEmpty()) {
                    map.put(username, encryptedPassword);
                }
            }
        }

        return new PasswordStorageManager(map);
    }

    /**
     * 从字符串内容加载密码映射（指定文件类型）
     *
     * @param content 密码文件内容
     * @param isLocalFile 是否为本地文件（三字段格式）
     * @return PasswordStorageManager 实例
     * @throws IOException 读取异常
     */
    public static PasswordStorageManager loadFromString(String content, boolean isLocalFile) throws IOException {
        Map<String, String> map = new HashMap<>();

        String[] lines = content.split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            // 跳过空行和注释行
            if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) {
                continue;
            }

            if (isLocalFile) {
                parseLocalFileLine(line, i + 1, map);
            } else {
                parseConfigCenterLine(line, i + 1, map);
            }
        }

        return new PasswordStorageManager(map);
    }

    /**
     * 保存密码映射到文件
     *
     * @param filePath 文件路径
     * @throws IOException 写入异常
     */
    public void save(String filePath) throws IOException {
        save(filePath, filePath.endsWith(".600") || filePath.contains(".600"));
    }

    /**
     * 保存密码映射到文件（指定文件类型）
     *
     * @param filePath 文件路径
     * @param isLocalFile 是否为本地文件（三字段格式）
     * @throws IOException 写入异常
     */
    public void save(String filePath, boolean isLocalFile) throws IOException {
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(file), PASSWORD_FILE_CHARSET))) {

            writer.println("# Druid Database Password Storage");
            if (isLocalFile) {
                writer.println("# Format: database:username:encryptedPassword");
                writer.println("# Local file format (multi-line file per project)");
            } else {
                writer.println("# Format: username:encryptedPassword");
                writer.println("# Config center format (single file per database user)");
            }
            writer.println("# Generated: " + new java.util.Date());
            writer.println();

            if (isLocalFile) {
                // 保存三字段格式（假设 key 是 database:username）
                for (Map.Entry<String, String> entry : passwordMap.entrySet()) {
                    String key = entry.getKey();
                    String encryptedPassword = entry.getValue();

                    // 如果 key 包含冒号，拆分为 database 和 username
                    int colonPos = key.indexOf(':');
                    if (colonPos > 0) {
                        String database = key.substring(0, colonPos);
                        String username = key.substring(colonPos + 1);
                        writer.println(database + ":" + username + ":" + encryptedPassword);
                    } else {
                        // 无法拆分，作为 username 处理
                        writer.println(key + ":" + encryptedPassword);
                    }
                }
            } else {
                // 保存两字段格式
                for (Map.Entry<String, String> entry : passwordMap.entrySet()) {
                    writer.println(entry.getKey() + ":" + entry.getValue());
                }
            }
        }

        logger.info("密码文件保存成功: " + filePath + ", 共 " + passwordMap.size() + " 个密码映射");
    }

    /**
     * 获取原始密码映射
     *
     * @return 密码映射 Map
     */
    public Map<String, String> getPasswordMap() {
        return new HashMap<>(passwordMap);
    }

    /**
     * 添加密码映射
     *
     * @param uid 数据库唯一标识符
     * @param encryptedPassword 加密后的密码
     */
    public void addPasswordMapping(String uid, String encryptedPassword) {
        if (uid == null || uid.isEmpty()) {
            throw new IllegalArgumentException("uid 不能为空");
        }
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            throw new IllegalArgumentException("encryptedPassword 不能为空");
        }
        passwordMap.put(uid, encryptedPassword);
    }

    /**
     * 获取加密密码（单参数）
     *
     * @param key 密码键（可能是 username 或 database:username）
     * @return 加密后的密码，如果不存在返回 null
     */
    public String getPassword(String key) {
        return passwordMap.get(key);
    }

    /**
     * 获取加密密码（按数据库和用户名）
     *
     * @param database 数据库标识
     * @param username 用户名
     * @return 加密后的密码，如果不存在返回 null
     */
    public String getPassword(String database, String username) {
        if (database == null || database.isEmpty()) {
            // 回退到单字段查询
            return passwordMap.get(username);
        }
        // 复合 key 查询
        return passwordMap.get(database + ":" + username);
    }

    /**
     * 检查是否存在指定数据库和用户名的密码
     *
     * @param database 数据库标识
     * @param username 用户名
     * @return 是否存在
     */
    public boolean contains(String database, String username) {
        if (database == null || database.isEmpty()) {
            return passwordMap.containsKey(username);
        }
        return passwordMap.containsKey(database + ":" + username);
    }

    /**
     * 移除密码映射
     *
     * @param uid 数据库唯一标识符
     * @return 被移除的密码，如果不存在返回 null
     */
    public String removePassword(String uid) {
        return passwordMap.remove(uid);
    }

    /**
     * 清空所有密码映射
     */
    public void clear() {
        passwordMap.clear();
    }

    /**
     * 获取密码映射数量
     *
     * @return 密码映射数量
     */
    public int size() {
        return passwordMap.size();
    }

    /**
     * 检查是否存在指定 uid 的密码
     *
     * @param uid 数据库唯一标识符
     * @return 是否存在
     */
    public boolean contains(String uid) {
        return passwordMap.containsKey(uid);
    }

    /**
     * 加密原始密码
     *
     * @param plainPassword 原始密码
     * @param key 加密密钥
     * @return 加密后的密码
     * @throws Exception 加密异常
     */
    public static String encryptPassword(String plainPassword, String key) throws Exception {
        return encryptPassword(plainPassword, key, DEFAULT_ALGORITHM);
    }

    /**
     * 加密原始密码（指定算法）
     *
     * @param plainPassword 原始密码
     * @param key 加密密钥
     * @param algorithm 加密算法（SM4 或 AES）
     * @return 加密后的密码
     * @throws Exception 加密异常
     */
    public static String encryptPassword(String plainPassword, String key, String algorithm) throws Exception {
        if ("SM4".equalsIgnoreCase(algorithm)) {
            return DruidEncryptedPasswordCallback.encodeSM4(plainPassword, key);
        } else if ("AES".equalsIgnoreCase(algorithm)) {
            return DruidEncryptedPasswordCallback.encodeAES(plainPassword, key);
        } else {
            throw new IllegalArgumentException("不支持的加密算法: " + algorithm);
        }
    }

    /**
     * 解密密码
     *
     * @param encryptedPassword 加密后的密码
     * @param key 解密密钥
     * @return 原始密码
     * @throws Exception 解密异常
     */
    public static String decryptPassword(String encryptedPassword, String key) throws Exception {
        return decryptPassword(encryptedPassword, key, DEFAULT_ALGORITHM);
    }

    /**
     * 解密密码（指定算法）
     *
     * @param encryptedPassword 加密后的密码
     * @param key 解密密钥
     * @param algorithm 加密算法（SM4 或 AES）
     * @return 原始密码
     * @throws Exception 解密异常
     */
    public static String decryptPassword(String encryptedPassword, String key, String algorithm) throws Exception {
        return DruidEncryptedPasswordCallback.decryptPassword(encryptedPassword, key, algorithm);
    }
}
