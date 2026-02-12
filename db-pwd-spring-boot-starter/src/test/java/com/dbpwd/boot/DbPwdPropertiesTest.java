package com.dbpwd.boot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DbPwdProperties单元测试
 */
@DisplayName("DbPwdProperties (boot包) 测试")
public class DbPwdPropertiesTest {

    private DbPwdProperties properties;
    private static final String USER_HOME = System.getProperty("user.home");

    @BeforeEach
    public void setUp() {
        properties = new DbPwdProperties();
    }

    @Test
    @DisplayName("测试默认值")
    public void testDefaultValues() {
        assertTrue(properties.isEnabled());
        assertTrue(properties.isPlaceholderReplacement());
        assertEquals("@", properties.getPlaceholderPrefix());
        assertEquals("@", properties.getPlaceholderSuffix());

        // 验证默认路径
        assertTrue(properties.getPasswordFile().contains(USER_HOME));
        assertTrue(properties.getPasswordFile().contains(".dbpwd"));
        assertTrue(properties.getPasswordFile().contains("db.pwd"));
        assertTrue(properties.getKeyFile().contains(USER_HOME));
        assertTrue(properties.getKeyFile().contains(".dbpwd"));
        assertTrue(properties.getKeyFile().contains("sm4.key"));
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
    @DisplayName("测试setPlaceholderPrefix和getPlaceholderPrefix")
    public void testPlaceholderPrefix() {
        String prefix = "${";
        properties.setPlaceholderPrefix(prefix);

        assertEquals(prefix, properties.getPlaceholderPrefix());
    }

    @Test
    @DisplayName("测试setPlaceholderSuffix和getPlaceholderSuffix")
    public void testPlaceholderSuffix() {
        String suffix = "}";
        properties.setPlaceholderSuffix(suffix);

        assertEquals(suffix, properties.getPlaceholderSuffix());
    }

    @Test
    @DisplayName("测试setPlaceholderReplacement")
    public void testPlaceholderReplacement() {
        assertTrue(properties.isPlaceholderReplacement());

        properties.setPlaceholderReplacement(false);
        assertFalse(properties.isPlaceholderReplacement());

        properties.setPlaceholderReplacement(true);
        assertTrue(properties.isPlaceholderReplacement());
    }

    @Test
    @DisplayName("测试getPlaceholderFormat方法")
    public void testGetPlaceholderFormat() {
        // 默认格式
        String format = properties.getPlaceholderFormat();
        assertEquals("@%s@", format);

        // 自定义格式
        properties.setPlaceholderPrefix("${");
        properties.setPlaceholderSuffix("}");
        format = properties.getPlaceholderFormat();
        assertEquals("${%s}", format);

        // 另一种格式
        properties.setPlaceholderPrefix("#");
        properties.setPlaceholderSuffix("#");
        format = properties.getPlaceholderFormat();
        assertEquals("#%s#", format);
    }

    @Test
    @DisplayName("测试所有属性设置")
    public void testSetAllProperties() {
        properties.setEnabled(false);
        properties.setPasswordFile("/custom/db.pwd");
        properties.setKeyFile("/custom/sm4.key");
        properties.setPlaceholderPrefix("${");
        properties.setPlaceholderSuffix("}");
        properties.setPlaceholderReplacement(false);

        assertFalse(properties.isEnabled());
        assertEquals("/custom/db.pwd", properties.getPasswordFile());
        assertEquals("/custom/sm4.key", properties.getKeyFile());
        assertEquals("${", properties.getPlaceholderPrefix());
        assertEquals("}", properties.getPlaceholderSuffix());
        assertFalse(properties.isPlaceholderReplacement());
    }

    @Test
    @DisplayName("测试空前缀后缀的占位符格式")
    public void testEmptyPlaceholderFormat() {
        properties.setPlaceholderPrefix("");
        properties.setPlaceholderSuffix("");
        String format = properties.getPlaceholderFormat();

        assertEquals("%s", format);
    }

    @Test
    @DisplayName("测试不同的占位符配置")
    public void testDifferentPlaceholderConfigs() {
        // 情况1: Spring风格
        properties.setPlaceholderPrefix("${");
        properties.setPlaceholderSuffix("}");
        assertEquals("${%s}", properties.getPlaceholderFormat());

        // 情况2: #格式
        properties.setPlaceholderPrefix("#");
        properties.setPlaceholderSuffix("#");
        assertEquals("#%s#", properties.getPlaceholderFormat());

        // 情况3: @格式（默认）
        properties.setPlaceholderPrefix("@");
        properties.setPlaceholderSuffix("@");
        assertEquals("@%s@", properties.getPlaceholderFormat());
    }
}
