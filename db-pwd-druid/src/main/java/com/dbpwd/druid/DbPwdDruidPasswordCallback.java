package com.dbpwd.druid;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.util.DruidPasswordCallback;
import com.dbpwd.core.PasswordService;

import java.util.Properties;
import java.util.logging.Logger;

/**
 * Druid连接池密码回调处理器
 *
 * 继承Druid的DruidPasswordCallback类，在获取连接时自动解密密码
 *
 * 使用方式（生产环境，使用默认路径 ~/.dbpwd/）：
 * <pre>
 * DruidDataSource dataSource = new DruidDataSource();
 * dataSource.setUrl("jdbc:mysql://localhost:3306/mydb");
 * dataSource.setUsername("user");
 * dataSource.setPassword("@my_db@");  // 设置占位符
 * dataSource.setPasswordCallback(new DbPwdDruidPasswordCallback());
 * </pre>
 *
 * 使用方式（单元测试，指定自定义路径）：
 * <pre>
 * Properties callbackProps = new Properties();
 * callbackProps.setProperty("keyFilePath", "/tmp/test/sm4.key");
 * callbackProps.setProperty("pwdFilePath", "/tmp/test/db.pwd");
 * dataSource.setPasswordCallback(new DbPwdDruidPasswordCallback(callbackProps));
 * </pre>
 *
 * @author Database Password Manager
 * @version 6.2
 */
public class DbPwdDruidPasswordCallback extends DruidPasswordCallback {

    private static final Logger logger = Logger.getLogger(DbPwdDruidPasswordCallback.class.getName());

    // 可选：自定义密钥文件路径（用于单元测试）
    private String keyFilePath;
    // 可选：自定义密码文件路径（用于单元测试）
    private String pwdFilePath;

    // 保存对 DruidDataSource 的引用，用于获取原始密码
    private DruidDataSource dataSource;

    // 缓存解密后的密码，避免重复解密
    private volatile char[] cachedPassword;
    private volatile String cachedPlaceholder;

    /**
     * 默认构造函数（使用默认路径 ~/.dbpwd/）
     *
     * 适用于生产环境和Demo应用
     */
    public DbPwdDruidPasswordCallback() {
        super("Password:", false);
    }

    /**
     * 带配置的构造函数（用于单元测试）
     *
     * @param properties 配置属性，可包含：
     *   - keyFilePath: SM4密钥文件路径（可选）
     *   - pwdFilePath: 密码文件路径（可选）
     */
    public DbPwdDruidPasswordCallback(Properties properties) {
        super("Password:", false);
        if (properties != null) {
            this.keyFilePath = properties.getProperty("keyFilePath");
            this.pwdFilePath = properties.getProperty("pwdFilePath");
        }
    }

    /**
     * 设置密钥文件路径
     *
     * @param keyFilePath SM4密钥文件路径
     */
    public void setKeyFilePath(String keyFilePath) {
        this.keyFilePath = keyFilePath;
    }

    /**
     * 设置密码文件路径
     *
     * @param pwdFilePath 密码文件路径
     */
    public void setPwdFilePath(String pwdFilePath) {
        this.pwdFilePath = pwdFilePath;
    }

    /**
     * 设置 DataSource 引用
     *
     * @param dataSource DruidDataSource 实例
     */
    public void setDataSource(DruidDataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 重写 getPassword 方法
     *
     * Druid 在创建连接时会调用此方法获取密码
     * 我们在这里解密占位符格式的密码
     */
    @Override
    public char[] getPassword() {
        // 如果已经设置了密码（通过 setProperties 或直接设置），直接返回
        char[] currentPassword = super.getPassword();
        if (currentPassword != null && currentPassword.length > 0) {
            logger.fine("返回已设置的密码");
            return currentPassword;
        }

        // 尝试从 DataSource 获取原始密码
        String originalPassword = null;
        if (dataSource != null) {
            originalPassword = dataSource.getPassword();
        }

        if (originalPassword == null || originalPassword.isEmpty()) {
            return null;
        }

        // 检查缓存
        if (cachedPlaceholder != null && cachedPlaceholder.equals(originalPassword) && cachedPassword != null) {
            logger.fine("使用缓存的密码");
            return cachedPassword;
        }

        try {
            logger.fine("解密密码: " + originalPassword);

            // 使用 PasswordService 解密
            String plainPassword;
            if (keyFilePath != null || pwdFilePath != null) {
                plainPassword = PasswordService.getDatabasePassword(originalPassword, keyFilePath, pwdFilePath);
            } else {
                plainPassword = PasswordService.getDatabasePassword(originalPassword);
            }

            logger.fine("密码解密成功");

            // 缓存结果
            cachedPlaceholder = originalPassword;
            cachedPassword = plainPassword.toCharArray();

            return cachedPassword;

        } catch (Exception e) {
            String pathInfo = (keyFilePath != null || pwdFilePath != null) ?
                "keyFile=" + keyFilePath + ", pwdFile=" + pwdFilePath :
                "~/.dbpwd/db.pwd 和 ~/.dbpwd/sm4.key";
            logger.severe("密码解密失败: " + e.getMessage() + "。请检查配置文件: " + pathInfo);
            return null;
        }
    }

    /**
     * 重写 setProperties 方法
     *
     * Druid会在创建连接时调用此方法
     */
    @Override
    public void setProperties(Properties properties) {
        super.setProperties(properties);

        // 如果 properties 中包含 password，直接解密
        String password = properties.getProperty("password");
        if (password != null && !password.trim().isEmpty()) {
            try {
                logger.fine("处理密码: " + password);

                String plainPassword;
                if (keyFilePath != null || pwdFilePath != null) {
                    plainPassword = PasswordService.getDatabasePassword(password, keyFilePath, pwdFilePath);
                } else {
                    plainPassword = PasswordService.getDatabasePassword(password);
                }

                char[] passwordChars = plainPassword.toCharArray();
                setPassword(passwordChars);

                // 缓存
                cachedPlaceholder = password;
                cachedPassword = passwordChars;

                logger.fine("密码解密成功");

            } catch (Exception e) {
                String pathInfo = (keyFilePath != null || pwdFilePath != null) ?
                    "keyFile=" + keyFilePath + ", pwdFile=" + pwdFilePath :
                    "~/.dbpwd/db.pwd 和 ~/.dbpwd/sm4.key";
                logger.severe("密码解密失败: " + e.getMessage() + "。请检查配置文件: " + pathInfo);
            }
        }
    }
}