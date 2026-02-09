package com.dbpwd.framework;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.core.PasswordFileManager;
import com.dbpwd.crypto.ENCFormat;
import com.dbpwd.crypto.PasswordDecryptor;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * 数据源装饰器（简化版）
 *
 * 简化设计说明：
 * - 单一装饰器处理所有类型的数据源
 * - 通过类型判断连接池类型（Druid/HikariCP/DBCP2等）
 * - 在getConnection()时自动解密密码
 * - 直接调用实际DataSource建立连接
 *
 * 支持的连接池类型：
 * - Druid
 * - HikariCP
 * - DBCP2
 * - 其他标准的DataSource实现
 *
 * @author Database Password Manager
 * @version 4.1
 */
public class DataSourceDecorator implements DataSource {

    private static final Logger logger = Logger.getLogger(DataSourceDecorator.class.getName());

    private final DataSource delegate;
    private final String dbId;
    private final String sm4Key;
    private final PasswordFileManager passwordFileManager;
    private boolean passwordDecrypted = false;
    private final String passwordFilePath;

    /**
     * 构造函数
     *
     * @param delegate 被装饰的原始数据源
     * @param dbId     数据库ID，用于查找密码
     * @param sm4Key   SM4密钥，用于解密密码
     */
    public DataSourceDecorator(DataSource delegate, String dbId, String sm4Key) throws IOException {
        this(delegate, dbId, sm4Key, getDefaultPasswordFilePath());
    }

    /**
     * 构造函数（指定密码文件）
     *
     * @param delegate     被装饰的原始数据源
     * @param dbId         数据库ID，用于查找密码
     * @param sm4Key       SM4密钥，用于解密密码
     * @param passwordFile 密码文件路径
     */
    public DataSourceDecorator(DataSource delegate, String dbId, String sm4Key, String passwordFile) throws IOException {
        this.delegate = Objects.requireNonNull(delegate, "数据源不能为null");
        this.dbId = Objects.requireNonNull(dbId, "数据库ID不能为空");
        this.sm4Key = Objects.requireNonNull(sm4Key, "SM4密钥不能为空");
        this.passwordFilePath = expandHomeDir(passwordFile);
        this.passwordFileManager = PasswordFileManager.loadFromFileSafe(this.passwordFilePath);
    }

    @Override
    public Connection getConnection() throws SQLException {
        // 在获取连接前，确保密码已解密
        ensurePasswordDecrypted();
        return delegate.getConnection();
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        // 如果传入用户名和密码，则使用传入的凭证
        return delegate.getConnection(username, password);
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (iface.equals(DataSource.class)) {
            return (T) this;
        }
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        if (iface.equals(DataSource.class)) {
            return true;
        }
        return delegate.isWrapperFor(iface);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取默认密码文件路径
     */
    private static String getDefaultPasswordFilePath() {
        return expandHomeDirStatic("~/.dbpwd/db.pwd");
    }

    /**
     * 展开用户目录路径（如将~转换为实际路径）
     */
    private String expandHomeDir(String path) {
        return expandHomeDirStatic(path);
    }

    /**
     * 静态方法：展开用户目录路径
     */
    private static String expandHomeDirStatic(String path) {
        if (path.startsWith("~/") || path.equals("~")) {
            String home = System.getProperty("user.home");
            if (path.equals("~")) {
                return home;
            }
            return home + path.substring(1);
        }
        return path;
    }

    /**
     * 确保数据源密码已解密
     *
     * 根据数据源类型选择解密方式：
     * - Druid: 直接setPassword
     * - HikariCP: 无法修改，需重新创建（此处仅解密后存储）
     * - DBCP2: 直接setPassword
     * 其他: 使用反射设置密码
     */
    private void ensurePasswordDecrypted() {
        if (passwordDecrypted) {
            return; // 已解密，跳过
        }

        // 尝试从密码文件获取加密密码
        String encryptedPassword = null;
        if (passwordFileManager != null) {
            com.dbpwd.model.DatabaseEntry entry = passwordFileManager.getById(dbId);
            if (entry != null) {
                encryptedPassword = entry.getPassword();
            }
        }

        // 如果密码文件中没有，从数据源获取
        if (encryptedPassword == null || encryptedPassword.isEmpty()) {
            encryptedPassword = getPasswordFromDataSource();
        }

        if (encryptedPassword == null || encryptedPassword.isEmpty() ||
                !ENCFormat.isEncrypted(encryptedPassword)) {
            logger.fine("密码为空或已为明文，无需解密: " + dbId);
            passwordDecrypted = true;
            return;
        }

        // 解密密码
        String plainPassword;
        try {
            plainPassword = PasswordDecryptor.decrypt(encryptedPassword, sm4Key);
        } catch (Exception e) {
            logger.warning("解密密码失败: " + e.getMessage());
            throw new RuntimeException("解密数据源密码失败: " + e.getMessage(), e);
        }

        // 根据数据源类型设置解密后的密码
        if (delegate instanceof DruidDataSource) {
            ((DruidDataSource) delegate).setPassword(plainPassword);
            logger.fine("Druid数据源密码已解密: " + dbId);
        } else if (delegate instanceof org.apache.commons.dbcp2.BasicDataSource) {
            ((org.apache.commons.dbcp2.BasicDataSource) delegate).setPassword(plainPassword);
            logger.fine("DBCP2数据源密码已解密: " + dbId);
        } else {
            // 尝试通过反射设置密码
            setPasswordThroughReflection(delegate, plainPassword);
        }

        passwordDecrypted = true;
    }

    /**
     * 从数据源获取密码
     */
    private String getPasswordFromDataSource() {
        if (delegate instanceof DruidDataSource) {
            return ((DruidDataSource) delegate).getPassword();
        } else if (delegate instanceof org.apache.commons.dbcp2.BasicDataSource) {
            return ((org.apache.commons.dbcp2.BasicDataSource) delegate).getPassword();
        }
        // 其他类型通过反射获取
        return getPasswordThroughReflection(delegate);
    }

    /**
     * 通过反射设置密码
     */
    private void setPasswordThroughReflection(DataSource dataSource, String password) {
        try {
            java.lang.reflect.Field passwordField = dataSource.getClass().getDeclaredField("password");
            passwordField.setAccessible(true);
            passwordField.set(dataSource, password);
            logger.fine("通过反射设置数据源密码成功: " + dbId);
        } catch (NoSuchFieldException e) {
            logger.fine("数据源没有password字段: " + e.getMessage());
        } catch (IllegalAccessException e) {
            logger.warning("无法设置数据源密码: " + e.getMessage());
        } catch (Exception e) {
            logger.warning("设置数据源密码时出现异常: " + e.getMessage());
        }
    }

    /**
     * 通过反射获取密码
     */
    private String getPasswordThroughReflection(DataSource dataSource) {
        try {
            java.lang.reflect.Field passwordField = dataSource.getClass().getDeclaredField("password");
            passwordField.setAccessible(true);
            return (String) passwordField.get(dataSource);
        } catch (Exception e) {
            logger.fine("无法获取数据源密码: " + e.getMessage());
            return null;
        }
    }

    // ==================== 公共方法 ====================

    /**
     * 获取被装饰的原始数据源
     *
     * @return 原始数据源
     */
    public DataSource getDelegate() {
        return delegate;
    }

    /**
     * 获取数据库ID
     *
     * @return 数据库ID
     */
    public String getDbId() {
        return dbId;
    }

    /**
     * 获取SM4密钥
     *
     * @return SM4密钥
     */
    public String getSm4Key() {
        return sm4Key;
    }

    /**
     * 获取密码文件管理器
     *
     * @return 密码文件管理器
     */
    public PasswordFileManager getPasswordFileManager() {
        return passwordFileManager;
    }

    /**
     * 刷新密码（当密码文件更新时调用）
     */
    public void refreshPassword() {
        logger.info("刷新装饰器中的密码: " + dbId);
        passwordDecrypted = false; // 重置标志，下次getConnection时会重新解密
    }

    /**
     * 获取解密后的密码
     */
    public String getDecryptedPassword() {
        String currentPassword = getPasswordFromDataSource();
        if (currentPassword == null || currentPassword.isEmpty()) {
            // 尝试从密码文件获取
            com.dbpwd.model.DatabaseEntry entry = passwordFileManager.getById(dbId);
            if (entry != null) {
                currentPassword = entry.getPassword();
            }
        }

        if (currentPassword == null || currentPassword.isEmpty()) {
            return null;
        }

        if (ENCFormat.isEncrypted(currentPassword)) {
            try {
                return PasswordDecryptor.decrypt(currentPassword, sm4Key);
            } catch (Exception e) {
                logger.warning("解密密码失败: " + e.getMessage());
            }
        }
        return currentPassword;
    }

    /**
     * 获取密码状态
     */
    public PasswordStatus getPasswordStatus() {
        String currentPassword = getPasswordFromDataSource();
        if (currentPassword == null || currentPassword.isEmpty()) {
            com.dbpwd.model.DatabaseEntry entry = passwordFileManager.getById(dbId);
            if (entry != null) {
                currentPassword = entry.getPassword();
            }
        }

        if (currentPassword == null || currentPassword.isEmpty()) {
            return PasswordStatus.EMPTY;
        }
        if (ENCFormat.isEncrypted(currentPassword)) {
            try {
                PasswordDecryptor.decrypt(currentPassword, sm4Key);
                return PasswordStatus.ENCRYPTED;
            } catch (Exception e) {
                return PasswordStatus.INVALID;
            }
        }
        return PasswordStatus.PLAIN;
    }

    /**
     * 密码状态枚举
     */
    public enum PasswordStatus {
        /** 密码为空 */
        EMPTY,
        /** 密码是加密格式 */
        ENCRYPTED,
        /** 密码是明文 */
        PLAIN,
        /** 密码格式无效或解密失败 */
        INVALID
    }
}
