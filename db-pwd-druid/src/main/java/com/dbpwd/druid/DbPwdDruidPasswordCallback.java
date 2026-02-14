package com.dbpwd.druid;

import com.alibaba.druid.util.DruidPasswordCallback;
import com.dbpwd.core.PasswordProvider;

import java.util.Properties;
import java.util.logging.Logger;

/**
 * Druid连接池密码回调处理器
 *
 * 继承Druid的DruidPasswordCallback类，在获取连接时自动解密密码
 *
 * 使用方式：
 * <pre>
 * DruidDataSource dataSource = new DruidDataSource();
 * dataSource.setUrl("jdbc:mysql://localhost:3306/mydb");
 * dataSource.setUsername("user");
 * dataSource.setPassword("@my_db@");  // 设置占位符
 * dataSource.setPasswordCallback(new DbPwdDruidPasswordCallback());
 * </pre>
 *
 * @author Database Password Manager
 * @version 6.0
 */
public class DbPwdDruidPasswordCallback extends DruidPasswordCallback {

    private static final Logger logger = Logger.getLogger(DbPwdDruidPasswordCallback.class.getName());

    // 缓存解密后的密码，避免重复解密
    private volatile char[] cachedPassword;
    private volatile String cachedPlaceholder;

    /**
     * 默认构造函数
     */
    public DbPwdDruidPasswordCallback() {
        super("Password:", false);
    }

    /**
     * 重写setProperties方法
     *
     * Druid会在创建连接时调用此方法，传递的properties包含password属性
     * 如果password是占位符格式（如@my_db@），需要解密为明文密码
     */
    @Override
    public void setProperties(Properties properties) {
        // 先调用父类方法
        super.setProperties(properties);

        String password = properties.getProperty("password");

        // 如果密码为空，直接返回（Druid会在其他地方处理）
        if (password == null || password.trim().isEmpty()) {
            return;
        }

        try {
            // 检查是否已缓存
            if (cachedPlaceholder != null && cachedPlaceholder.equals(password) && cachedPassword != null) {
                logger.fine("使用缓存的密码");
                setPassword(cachedPassword);
                return;
            }

            logger.fine("处理密码: " + password);

            // 使用PasswordProvider解密（支持占位符和ENC格式）
            String plainPassword = PasswordProvider.getDatabasePassword(password);

            logger.fine("密码解密成功");

            // 设置解密后的明文密码
            char[] passwordChars = plainPassword.toCharArray();
            setPassword(passwordChars);

            // 缓存结果
            cachedPlaceholder = password;
            cachedPassword = passwordChars;

        } catch (Exception e) {
            // 记录错误但不抛出异常，避免影响Druid的重试机制
            logger.severe("密码解密失败: " + e.getMessage() +
                "。请检查 ~/.dbpwd/db.pwd 和 ~/.dbpwd/sm4.key 配置文件。");
            // 不设置密码，让Druid自然失败
        }
    }
}