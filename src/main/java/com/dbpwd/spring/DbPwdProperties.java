package com.dbpwd.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.io.File;

/**
 * Database Password Manager 配置属性
 *
 * 通过application.yml或application.properties配置
 *
 * <pre>
 * dbpwd:
 *   enabled: true
 *   password-file: ~/.dbpwd/db.pwd
 *   key-file: ~/.dbpwd/sm4.key
 * </pre>
 *
 * @author Database Password Manager
 * @version 4.2
 */
@ConfigurationProperties(prefix = "dbpwd")
public class DbPwdProperties {

    /**
     * 是否启用dbpwd功能
     */
    private boolean enabled = true;

    /**
     * 密码文件路径
     */
    private String passwordFile = System.getProperty("user.home") + File.separator + ".dbpwd" + File.separator + "db.pwd";

    /**
     * SM4密钥文件路径
     */
    private String keyFile = System.getProperty("user.home") + File.separator + ".dbpwd" + File.separator + "sm4.key";

    /**
     * 是否在应用启动时验证密钥文件
     */
    private boolean validateKeyOnStartup = true;

    /**
     * 是否在应用启动时验证密码文件
     */
    private boolean validatePasswordFileOnStartup = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getPasswordFile() {
        return passwordFile;
    }

    public void setPasswordFile(String passwordFile) {
        this.passwordFile = passwordFile;
    }

    public String getKeyFile() {
        return keyFile;
    }

    public void setKeyFile(String keyFile) {
        this.keyFile = keyFile;
    }

    public boolean isValidateKeyOnStartup() {
        return validateKeyOnStartup;
    }

    public void setValidateKeyOnStartup(boolean validateKeyOnStartup) {
        this.validateKeyOnStartup = validateKeyOnStartup;
    }

    public boolean isValidatePasswordFileOnStartup() {
        return validatePasswordFileOnStartup;
    }

    public void setValidatePasswordFileOnStartup(boolean validatePasswordFileOnStartup) {
        this.validatePasswordFileOnStartup = validatePasswordFileOnStartup;
    }

    @Override
    public String toString() {
        return "DbPwdProperties{" +
                "enabled=" + enabled +
                ", passwordFile='" + passwordFile + '\'' +
                ", keyFile='" + keyFile + '\'' +
                ", validateKeyOnStartup=" + validateKeyOnStartup +
                ", validatePasswordFileOnStartup=" + validatePasswordFileOnStartup +
                '}';
    }
}
