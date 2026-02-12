package com.dbpwd.spring;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DbPwdProperties单元测试
 */
@DisplayName("DbPwdProperties测试")
public class DbPwdPropertiesTest {

    private DbPwdProperties properties;

    @BeforeEach
    public void setUp() {
        properties = new DbPwdProperties();
    }

    @Test
    @DisplayName("测试默认值")
    public void testDefaultValues() {
        assertTrue(properties.isEnabled());
        assertTrue(properties.isValidateKeyOnStartup());
        assertTrue(properties.isValidatePasswordFileOnStartup());

        String userHome = System.getProperty("user.home");
        assertTrue(properties.getPasswordFile().contains(userHome));
        assertTrue(properties.getKeyFile().contains(userHome));
        assertTrue(properties.getPasswordFile().contains(".dbpwd"));
        assertTrue(properties.getKeyFile().contains(".dbpwd"));
    }

    @Test
    @DisplayName("测试setEnabled和isEnabled")
    public void testEnabled() {
        assertTrue(properties.isEnabled());

        properties.setEnabled(false);
        assertFalse(properties.isEnabled());

        properties.setEnabled(true);
        assertTrue(properties.isEnabled());
    }

    @Test
    @DisplayName("测试setPasswordFile和getPasswordFile")
    public void testPasswordFile() {
        String customPath = "/custom/path/db.pwd";
        properties.setPasswordFile(customPath);

        assertEquals(customPath, properties.getPasswordFile());
    }

    @Test
    @DisplayName("测试setKeyFile和getKeyFile")
    public void testKeyFile() {
        String customPath = "/custom/path/sm4.key";
        properties.setKeyFile(customPath);

        assertEquals(customPath, properties.getKeyFile());
    }

    @Test
    @DisplayName("测试setValidateKeyOnStartup和isValidateKeyOnStartup")
    public void testValidateKeyOnStartup() {
        assertTrue(properties.isValidateKeyOnStartup());

        properties.setValidateKeyOnStartup(false);
        assertFalse(properties.isValidateKeyOnStartup());

        properties.setValidateKeyOnStartup(true);
        assertTrue(properties.isValidateKeyOnStartup());
    }

    @Test
    @DisplayName("测试setValidatePasswordFileOnStartup和isValidatePasswordFileOnStartup")
    public void testValidatePasswordFileOnStartup() {
        assertTrue(properties.isValidatePasswordFileOnStartup());

        properties.setValidatePasswordFileOnStartup(false);
        assertFalse(properties.isValidatePasswordFileOnStartup());

        properties.setValidatePasswordFileOnStartup(true);
        assertTrue(properties.isValidatePasswordFileOnStartup());
    }

    @Test
    @DisplayName("测试toString方法")
    public void testToString() {
        String result = properties.toString();

        assertNotNull(result);
        assertTrue(result.contains("DbPwdProperties"));
        assertTrue(result.contains("enabled=true"));
        assertTrue(result.contains("passwordFile="));
        assertTrue(result.contains("keyFile="));
        assertTrue(result.contains("validateKeyOnStartup=true"));
        assertTrue(result.contains("validatePasswordFileOnStartup=true"));
    }

    @Test
    @DisplayName("测试所有属性设置")
    public void testSetAllProperties() {
        properties.setEnabled(false);
        properties.setPasswordFile("/custom/db.pwd");
        properties.setKeyFile("/custom/sm4.key");
        properties.setValidateKeyOnStartup(false);
        properties.setValidatePasswordFileOnStartup(false);

        assertFalse(properties.isEnabled());
        assertEquals("/custom/db.pwd", properties.getPasswordFile());
        assertEquals("/custom/sm4.key", properties.getKeyFile());
        assertFalse(properties.isValidateKeyOnStartup());
        assertFalse(properties.isValidatePasswordFileOnStartup());
    }
}
