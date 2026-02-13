package com.dbpwd.druid;

import com.alibaba.druid.util.DruidPasswordCallback;
import com.dbpwd.core.PasswordProvider;

import java.util.Properties;
import java.util.logging.Logger;

/**
 * Druid连接池密码回调处理器
 *
 * 继承DruidPasswordCallback，利用Druid原生的密码回调机制
 * 在Druid初始化时自动解密数据库密码
 *
 * 使用方式：
 * <pre>
 * // 使用DruidDataSourceFactory和properties配置
 * Properties props = new Properties();
 * props.setProperty("url", "jdbc:mysql://localhost:3306/mydb");
 * props.setProperty("username", "user");
 * props.setProperty("password", "db_placeholder");  // 使用占位符
 * props.setProperty("passwordCallbackClassName", "com.dbpwd.druid.DbPwdDruidPasswordCallback");
 * DataSource ds = DruidDataSourceFactory.createDataSource(props);
 *
 * // 或者直接设置DruidDataSource（需要手动触发properties设置）
 * DruidDataSource dataSource = new DruidDataSource();
 * dataSource.setUrl("jdbc:mysql://localhost:3306/mydb");
 * dataSource.setUsername("user");
 *
 * DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback();
 * Properties props = new Properties();
 * props.setProperty("password", "db_placeholder");
 * callback.setProperties(props);  // 手动设置properties
 * dataSource.setPasswordCallback(callback);
 * </pre>
 *
 * @author Database Password Manager
 * @version 5.3
 */
public class DbPwdDruidPasswordCallback extends DruidPasswordCallback {

    private static final Logger logger = Logger.getLogger(DbPwdDruidPasswordCallback.class.getName());

    /**
     * 默认构造函数
     */
    public DbPwdDruidPasswordCallback() {
        // 默认提示，echoOn=false表示不回显示密码
        super("Password:", false);
    }

    /**
     * 重写setProperties方法，DruidDataSourceFactory会调用此方法
     *
     * 根据用户提供的DruidPasswordCallback集成示例实现
     */
    @Override
    public void setProperties(Properties properties) {
        super.setProperties(properties);

        try {
            // 从properties中获取密码（可能是明文、占位符或密文）
            String encryptedPwd = properties.getProperty("password");

            if (encryptedPwd == null || encryptedPwd.trim().isEmpty()) {
                throw new IllegalArgumentException("密码不能为空");
            }

            logger.fine("收到密码（可能是占位符）: " + encryptedPwd);

            // 解密密码（PasswordProvider会自动判断是占位符还是密文格式）
            String plainPassword = PasswordProvider.getDatabasePassword(encryptedPwd);

            logger.fine("密码解密成功");

            // 设置解密后的密码
            setPassword(plainPassword.toCharArray());

        } catch (Exception e) {
            logger.severe("解密数据库密码失败: " + e.getMessage());
            throw new RuntimeException("数据库密码解密失败: " + e.getMessage(), e);
        }
    }
}
