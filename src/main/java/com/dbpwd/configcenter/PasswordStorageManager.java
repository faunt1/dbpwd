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
 * 密码文件格式：
 * uid=encryptedPassword
 * 每行一个映射，使用 = 分隔
 *
 * 示例：
 * userdb_prod=[16]r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=
 * analytics_prod=[32]aBcDeFgHiJkLmNoPqRsTuVwXyZ012345=
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

                // 解析 uid=encryptedPassword 格式
                int equalPos = line.indexOf('=');
                if (equalPos <= 0) {
                    logger.warning("密码文件第 " + lineNumber + " 行格式错误: " + line);
                    continue;
                }

                String uid = line.substring(0, equalPos).trim();
                String encryptedPassword = line.substring(equalPos + 1).trim();

                if (!uid.isEmpty() && !encryptedPassword.isEmpty()) {
                    map.put(uid, encryptedPassword);
                }
            }
        }

        logger.info("密码文件加载成功: " + filePath + ", 共 " + map.size() + " 个密码映射");
        return new PasswordStorageManager(map);
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

            // 解析 uid=encryptedPassword 格式
            int equalPos = line.indexOf('=');
            if (equalPos <= 0) {
                continue;
            }

            String uid = line.substring(0, equalPos).trim();
            String encryptedPassword = line.substring(equalPos + 1).trim();

            if (!uid.isEmpty() && !encryptedPassword.isEmpty()) {
                map.put(uid, encryptedPassword);
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
        File file = new File(filePath);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(new FileOutputStream(file), PASSWORD_FILE_CHARSET))) {

            writer.println("# Druid Database Password Storage");
            writer.println("# Format: uid=encryptedPassword");
            writer.println("# Generated: " + new java.util.Date());
            writer.println();

            for (Map.Entry<String, String> entry : passwordMap.entrySet()) {
                writer.println(entry.getKey() + "=" + entry.getValue());
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
     * 获取加密密码
     *
     * @param uid 数据库唯一标识符
     * @return 加密后的密码，如果不存在返回 null
     */
    public String getPassword(String uid) {
        return passwordMap.get(uid);
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
