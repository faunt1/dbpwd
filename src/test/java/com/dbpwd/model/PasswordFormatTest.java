package com.dbpwd.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PasswordFormat单元测试
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class PasswordFormatTest {

    // ==================== detectFormat测试 ====================

    @Test
    public void testDetectFormatWithNull() {
        assertEquals(PasswordFormat.UNKNOWN, PasswordFormat.detectFormat(null));
    }

    @Test
    public void testDetectFormatWithEmptyString() {
        assertEquals(PasswordFormat.UNKNOWN, PasswordFormat.detectFormat(""));
    }

    @Test
    public void testDetectFormatSm4Standard() {
        String password = "[16]r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=";
        assertEquals(PasswordFormat.SM4_STANDARD, PasswordFormat.detectFormat(password));
    }

    @Test
    public void testDetectFormatSm4StandardWithDifferentKeyLength() {
        assertEquals(PasswordFormat.SM4_STANDARD, PasswordFormat.detectFormat("[32]abc123"));
        assertEquals(PasswordFormat.SM4_STANDARD, PasswordFormat.detectFormat("[8]xyz789"));
    }

    @Test
    public void testDetectFormatSm4StandardWithBase64Chars() {
        String password = "[16]r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=";
        assertEquals(PasswordFormat.SM4_STANDARD, PasswordFormat.detectFormat(password));
    }

    @Test
    public void testDetectFormatSm4StandardMinimalBase64() {
        String password = "[16]ab";
        assertEquals(PasswordFormat.SM4_STANDARD, PasswordFormat.detectFormat(password));
    }

    @Test
    public void testDetectFormatSm4StandardWithPadding() {
        String password = "[16]ab=";
        assertEquals(PasswordFormat.SM4_STANDARD, PasswordFormat.detectFormat(password));
    }

    @Test
    public void testDetectFormatSm4StandardWithPaddingDouble() {
        String password = "[16]ab==";
        assertEquals(PasswordFormat.SM4_STANDARD, PasswordFormat.detectFormat(password));
    }

    @Test
    public void testDetectFormatSm4StandardWithSpecialChars() {
        String password = "[16]ab/c+de";
        assertEquals(PasswordFormat.SM4_STANDARD, PasswordFormat.detectFormat(password));
    }

    @Test
    public void testDetectFormatDruidEncrypted() {
        String password = "ENC(r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=)";
        assertEquals(PasswordFormat.DRUID_ENCRYPTED, PasswordFormat.detectFormat(password));
    }

    @Test
    public void testDetectFormatDruidEncryptedWithBase64Chars() {
        String password = "ENC(r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=)";
        assertEquals(PasswordFormat.DRUID_ENCRYPTED, PasswordFormat.detectFormat(password));
    }

    @Test
    public void testDetectFormatDruidEncryptedWithPadding() {
        String password = "ENC(ab==)";
        assertEquals(PasswordFormat.DRUID_ENCRYPTED, PasswordFormat.detectFormat(password));
    }

    @Test
    public void testDetectFormatDruidEncryptedMinimal() {
        String password = "ENC(ab)";
        assertEquals(PasswordFormat.DRUID_ENCRYPTED, PasswordFormat.detectFormat(password));
    }

    @Test
    public void testDetectFormatDruidEncryptedEmptyContent() {
        String password = "ENC()";
        // 正则表达式要求内容，空括号可能是UNKNOWN
        assertEquals(PasswordFormat.UNKNOWN, PasswordFormat.detectFormat(password));
    }

    @Test
    public void testDetectFormatPlain() {
        String password = "PLAIN_myPassword123";
        assertEquals(PasswordFormat.PLAIN, PasswordFormat.detectFormat(password));
    }

    @Test
    public void testDetectFormatPlainWithSpecialChars() {
        String password = "PLAIN_my@Pass#123$";
        assertEquals(PasswordFormat.PLAIN, PasswordFormat.detectFormat(password));
    }

    @Test
    public void testDetectFormatPlainWithSpaces() {
        String password = "PLAIN_my password";
        assertEquals(PasswordFormat.PLAIN, PasswordFormat.detectFormat(password));
    }

    @Test
    public void testDetectFormatPlainEmptyAfterPrefix() {
        String password = "PLAIN_";
        assertEquals(PasswordFormat.PLAIN, PasswordFormat.detectFormat(password));
    }

    @Test
    public void testDetectFormatUnknownForPlainPassword() {
        String password = "mypassword123";
        assertEquals(PasswordFormat.UNKNOWN, PasswordFormat.detectFormat(password));
    }

    @Test
    public void testDetectFormatUnknownForSpecialString() {
        String password = "XYZ_something";
        assertEquals(PasswordFormat.UNKNOWN, PasswordFormat.detectFormat(password));
    }

    @Test
    public void testDetectFormatUnknownForInvalidBracketFormat() {
        String password = "[abc]def";
        assertEquals(PasswordFormat.UNKNOWN, PasswordFormat.detectFormat(password));
    }

    // ==================== formatEncryptedPassword测试 ====================

    @Test
    public void testFormatEncryptedPasswordSm4StandardAlreadyFormatted() {
        PasswordFormat format = PasswordFormat.SM4_STANDARD;
        String password = "[16]r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=";

        String result = format.formatEncryptedPassword(password, 16);

        assertEquals(password, result, "已经是SM4标准格式应该原样返回");
    }

    @Test
    public void testFormatEncryptedPasswordSm4StandardNeedsFormatting() {
        PasswordFormat format = PasswordFormat.SM4_STANDARD;
        String password = "r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=";

        String result = format.formatEncryptedPassword(password, 16);

        assertEquals("[16]r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=", result);
    }

    @Test
    public void testFormatEncryptedPasswordSm4StandardWithDifferentKeyLength() {
        PasswordFormat format = PasswordFormat.SM4_STANDARD;
        String password = "abc123";

        String result = format.formatEncryptedPassword(password, 32);

        assertEquals("[32]abc123", result);
    }

    @Test
    public void testFormatEncryptedPasswordDruidEncryptedAlreadyFormatted() {
        PasswordFormat format = PasswordFormat.DRUID_ENCRYPTED;
        String password = "ENC(r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=)";

        String result = format.formatEncryptedPassword(password, 16);

        assertEquals(password, result, "已经是Druid格式应该原样返回");
    }

    @Test
    public void testFormatEncryptedPasswordDruidEncryptedNeedsFormatting() {
        PasswordFormat format = PasswordFormat.DRUID_ENCRYPTED;
        String password = "r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=";

        String result = format.formatEncryptedPassword(password, 16);

        assertEquals("ENC(r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=)", result);
    }

    @Test
    public void testFormatEncryptedPasswordPlainAlreadyFormatted() {
        PasswordFormat format = PasswordFormat.PLAIN;
        String password = "PLAIN_myPassword123";

        String result = format.formatEncryptedPassword(password, 16);

        assertEquals(password, result, "已经是PLAIN格式应该原样返回");
    }

    @Test
    public void testFormatEncryptedPasswordPlainNeedsFormatting() {
        PasswordFormat format = PasswordFormat.PLAIN;
        String password = "myPassword123";

        String result = format.formatEncryptedPassword(password, 16);

        assertEquals("PLAIN_myPassword123", result);
    }

    @Test
    public void testFormatEncryptedPasswordUnknown() {
        PasswordFormat format = PasswordFormat.UNKNOWN;
        String password = "somePassword";

        String result = format.formatEncryptedPassword(password, 16);

        assertEquals("somePassword", result, "UNKNOWN格式应该原样返回");
    }

    // ==================== extractPassword测试 ====================

    @Test
    public void testExtractPasswordSm4Standard() {
        PasswordFormat format = PasswordFormat.SM4_STANDARD;
        String passwordWithFormat = "[16]r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=";

        String result = format.extractPassword(passwordWithFormat);

        assertEquals("r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=", result);
    }

    @Test
    public void testExtractPasswordSm4StandardWithNull() {
        PasswordFormat format = PasswordFormat.SM4_STANDARD;

        String result = format.extractPassword(null);

        assertNull(result);
    }

    @Test
    public void testExtractPasswordSm4StandardNoClosingBracket() {
        PasswordFormat format = PasswordFormat.SM4_STANDARD;
        String passwordWithFormat = "[16r7X9k3mP8nQ2vL5w";

        String result = format.extractPassword(passwordWithFormat);

        assertEquals("[16r7X9k3mP8nQ2vL5w", result);
    }

    @Test
    public void testExtractPasswordSm4StandardEmptyBrackets() {
        PasswordFormat format = PasswordFormat.SM4_STANDARD;
        String passwordWithFormat = "[]r7X9k3mP8nQ2vL5w";

        String result = format.extractPassword(passwordWithFormat);

        assertEquals("r7X9k3mP8nQ2vL5w", result);
    }

    @Test
    public void testExtractPasswordDruidEncrypted() {
        PasswordFormat format = PasswordFormat.DRUID_ENCRYPTED;
        String passwordWithFormat = "ENC(r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=)";

        String result = format.extractPassword(passwordWithFormat);

        assertEquals("r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=", result);
    }

    @Test
    public void testExtractPasswordDruidEncryptedWithNull() {
        PasswordFormat format = PasswordFormat.DRUID_ENCRYPTED;

        String result = format.extractPassword(null);

        assertNull(result);
    }

    @Test
    public void testExtractPasswordDruidEncryptedNoPrefix() {
        PasswordFormat format = PasswordFormat.DRUID_ENCRYPTED;
        String passwordWithFormat = "r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=";

        String result = format.extractPassword(passwordWithFormat);

        assertEquals("r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=", result);
    }

    @Test
    public void testExtractPasswordDruidEncryptedNoClosingParen() {
        PasswordFormat format = PasswordFormat.DRUID_ENCRYPTED;
        String passwordWithFormat = "ENC(r7X9k3mP8nQ2vL5w";

        String result = format.extractPassword(passwordWithFormat);

        assertEquals("ENC(r7X9k3mP8nQ2vL5w", result);
    }

    @Test
    public void testExtractPasswordPlain() {
        PasswordFormat format = PasswordFormat.PLAIN;
        String passwordWithFormat = "PLAIN_myPassword123";

        String result = format.extractPassword(passwordWithFormat);

        assertEquals("myPassword123", result);
    }

    @Test
    public void testExtractPasswordPlainWithNull() {
        PasswordFormat format = PasswordFormat.PLAIN;

        String result = format.extractPassword(null);

        assertNull(result);
    }

    @Test
    public void testExtractPasswordPlainNoPrefix() {
        PasswordFormat format = PasswordFormat.PLAIN;
        String passwordWithFormat = "myPassword123";

        String result = format.extractPassword(passwordWithFormat);

        assertEquals("myPassword123", result);
    }

    @Test
    public void testExtractPasswordUnknown() {
        PasswordFormat format = PasswordFormat.UNKNOWN;
        String passwordWithFormat = "somePassword";

        String result = format.extractPassword(passwordWithFormat);

        assertEquals("somePassword", result);
    }

    @Test
    public void testExtractPasswordUnknownWithNull() {
        PasswordFormat format = PasswordFormat.UNKNOWN;

        String result = format.extractPassword(null);

        assertNull(result);
    }

    // ==================== isPlainText测试 ====================

    @Test
    public void testIsPlainTextPlainFormat() {
        String password = "PLAIN_myPassword123";
        assertTrue(PasswordFormat.isPlainText(password));
    }

    @Test
    public void testIsPlainTextNotEncryptedFormats() {
        assertFalse(PasswordFormat.isPlainText("[16]base64data"), "SM4标准格式不应该返回true");
        assertFalse(PasswordFormat.isPlainText("ENC(base64data)"), "Druid加密格式不应该返回true");
    }

    @Test
    public void testIsPlainTextNull() {
        assertFalse(PasswordFormat.isPlainText(null));
    }

    @Test
    public void testIsPlainTextEmpty() {
        assertFalse(PasswordFormat.isPlainText(""));
    }

    @Test
    public void testIsPlainTextPlainPassword() {
        assertFalse(PasswordFormat.isPlainText("mypassword123"), "普通密码不是PLAIN格式");
    }

    // ==================== isEncrypted测试 ====================

    @Test
    public void testIsEncryptedSm4Standard() {
        String password = "[16]r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=";
        assertTrue(PasswordFormat.isEncrypted(password), "SM4标准格式应该是加密格式");
    }

    @Test
    public void testIsEncryptedDruidEncrypted() {
        String password = "ENC(r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=)";
        assertTrue(PasswordFormat.isEncrypted(password), "Druid加密格式应该是加密格式");
    }

    @Test
    public void testIsEncryptedPlainFormat() {
        String password = "PLAIN_myPassword123";
        assertFalse(PasswordFormat.isEncrypted(password), "PLAIN格式不是加密格式");
    }

    @Test
    public void testIsEncryptedPlainPassword() {
        String password = "mypassword123";
        assertFalse(PasswordFormat.isEncrypted(password), "普通密码不是加密格式");
    }

    @Test
    public void testIsEncryptedNull() {
        assertFalse(PasswordFormat.isEncrypted(null));
    }

    @Test
    public void testIsEncryptedEmpty() {
        assertFalse(PasswordFormat.isEncrypted(""));
    }

    @Test
    public void testIsEncryptedUnknownFormat() {
        String password = "XYZ_something";
        assertFalse(PasswordFormat.isEncrypted(password), "未知格式不是加密格式");
    }

    // ==================== Getter测试 ====================

    @Test
    public void testGetPatternSm4Standard() {
        java.util.regex.Pattern pattern = PasswordFormat.SM4_STANDARD.getPattern();

        assertNotNull(pattern);
        assertTrue(pattern.matcher("[16]base64data").matches());
        assertTrue(pattern.matcher("[32]base64data").matches());
        assertTrue(pattern.matcher("[8]ab+c=").matches());
    }

    @Test
    public void testGetPatternDruidEncrypted() {
        java.util.regex.Pattern pattern = PasswordFormat.DRUID_ENCRYPTED.getPattern();

        assertNotNull(pattern);
        assertNotNull(pattern.pattern());

        // ENC(base64) 格式
        assertTrue(pattern.matcher("ENC(base64data)").matches());
        assertTrue(pattern.matcher("ENC(ab+c)").matches());
        assertTrue(pattern.matcher("ENC(ab)").matches());
        assertTrue(pattern.matcher("ENC(ab=)").matches());
        assertTrue(pattern.matcher("ENC(ab==)").matches());
        assertFalse(pattern.matcher("ENC()").matches()); // 空内容不匹配
    }

    @Test
    public void testGetPatternPlain() {
        java.util.regex.Pattern pattern = PasswordFormat.PLAIN.getPattern();

        assertNotNull(pattern);
        assertTrue(pattern.matcher("PLAIN_myPassword").matches());
        assertTrue(pattern.matcher("PLAIN_any text").matches());
        assertTrue(pattern.matcher("PLAIN_123!@#").matches());
    }

    @Test
    public void testGetPatternUnknown() {
        java.util.regex.Pattern pattern = PasswordFormat.UNKNOWN.getPattern();

        assertNotNull(pattern);
        // UNKNOWN匹配任何字符串
        assertTrue(pattern.matcher("").matches());
        assertTrue(pattern.matcher("anything").matches());
        assertTrue(pattern.matcher("[16]base64").matches());
        assertTrue(pattern.matcher("ENC(base64)").matches());
    }

    @Test
    public void testGetDescription() {
        assertEquals("SM4标准格式", PasswordFormat.SM4_STANDARD.getDescription());
        assertEquals("Jasypt风格ENC格式", PasswordFormat.DRUID_ENCRYPTED.getDescription());
        assertEquals("明文格式", PasswordFormat.PLAIN.getDescription());
        assertEquals("未知格式", PasswordFormat.UNKNOWN.getDescription());
    }

    // ==================== 边界测试 ====================

    @Test
    public void testExtractPasswordFromEmptyString() {
        assertEquals("", PasswordFormat.SM4_STANDARD.extractPassword(""));
        assertEquals("", PasswordFormat.DRUID_ENCRYPTED.extractPassword(""));
        assertEquals("", PasswordFormat.PLAIN.extractPassword(""));
        assertEquals("", PasswordFormat.UNKNOWN.extractPassword(""));
    }

    @Test
    public void testFormatEncryptedPasswordWithEmptyString() {
        assertEquals("[16]", PasswordFormat.SM4_STANDARD.formatEncryptedPassword("", 16));
        assertEquals("ENC()", PasswordFormat.DRUID_ENCRYPTED.formatEncryptedPassword("", 16));
        assertEquals("PLAIN_", PasswordFormat.PLAIN.formatEncryptedPassword("", 16));
        assertEquals("", PasswordFormat.UNKNOWN.formatEncryptedPassword("", 16));
    }

    @Test
    public void testSm4StandardWithMultipleDigits() {
        String password = "[12345]base64data";
        assertEquals(PasswordFormat.SM4_STANDARD, PasswordFormat.detectFormat(password));
        assertEquals("base64data", PasswordFormat.SM4_STANDARD.extractPassword(password));
    }

    @Test
    public void testBase64Padding() {
        assertTrue(PasswordFormat.SM4_STANDARD.getPattern().matcher("[16]abc=").matches());
        assertTrue(PasswordFormat.SM4_STANDARD.getPattern().matcher("[16]abc==").matches());
        assertTrue(PasswordFormat.DRUID_ENCRYPTED.getPattern().matcher("ENC(abc=)").matches());
        assertTrue(PasswordFormat.DRUID_ENCRYPTED.getPattern().matcher("ENC(abc==)").matches());
    }
}
