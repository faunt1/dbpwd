package com.dbpwd.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PasswordDecryptor单元测试
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class PasswordDecryptorTest {

    private static final String TEST_HEX_KEY = "AABBCCDD1234567890ABCDEF12345678";
    private static final String TEST_PLAIN_PASSWORD = "MySecurePassword123!";

    @Test
    public void testDecryptSuccess() throws Exception {
        String encrypted = PasswordEncryptor.encrypt(TEST_PLAIN_PASSWORD, TEST_HEX_KEY);
        String decrypted = PasswordDecryptor.decrypt(encrypted, TEST_HEX_KEY);

        assertEquals(TEST_PLAIN_PASSWORD, decrypted, "解密应成功");
    }

    @Test
    public void testDecryptPlainTextReturnsAsIs() throws Exception {
        String plainPassword = "plainPassword";
        String result = PasswordDecryptor.decrypt(plainPassword, TEST_HEX_KEY);

        assertEquals(plainPassword, result, "明文密码应原样返回");
    }

    @Test
    public void testAutoDecryptWithEncryptedPassword() throws Exception {
        String encrypted = PasswordEncryptor.encrypt(TEST_PLAIN_PASSWORD, TEST_HEX_KEY);
        String result = PasswordDecryptor.autoDecrypt(encrypted, TEST_HEX_KEY);

        assertEquals(TEST_PLAIN_PASSWORD, result, "自动解密应成功");
    }

    @Test
    public void testAutoDecryptWithPlainText() throws Exception {
        String plainPassword = "plainPassword";
        String result = PasswordDecryptor.autoDecrypt(plainPassword, TEST_HEX_KEY);

        assertEquals(plainPassword, result, "明文密码应原样返回");
    }

    @Test
    public void testAutoDecryptWithNull() throws Exception {
        String result = PasswordDecryptor.autoDecrypt(null, TEST_HEX_KEY);
        assertNull(result, "null应返回null");
    }

    @Test
    public void testAutoDecryptWithEmptyString() throws Exception {
        String result = PasswordDecryptor.autoDecrypt("", TEST_HEX_KEY);
        assertEquals("", result, "空字符串应返回空字符串");
    }

    @Test
    public void testDecryptWithNull() {
        assertThrows(IllegalArgumentException.class, () -> {
            PasswordDecryptor.decrypt(null, TEST_HEX_KEY);
        });
    }

    @Test
    public void testDecryptWithEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            PasswordDecryptor.decrypt("", TEST_HEX_KEY);
        });
    }

    @Test
    public void testDecryptWithNullKey() throws Exception {
        String encrypted = PasswordEncryptor.encrypt(TEST_PLAIN_PASSWORD, TEST_HEX_KEY);
        String finalEncrypted = encrypted;
        assertThrows(IllegalArgumentException.class, () -> {
            PasswordDecryptor.decrypt(finalEncrypted, null);
        });
    }

    @Test
    public void testDecryptWithEmptyKey() throws Exception {
        String encrypted = PasswordEncryptor.encrypt(TEST_PLAIN_PASSWORD, TEST_HEX_KEY);
        String finalEncrypted = encrypted;
        assertThrows(IllegalArgumentException.class, () -> {
            PasswordDecryptor.decrypt(finalEncrypted, "");
        });
    }

    @Test
    public void testDecryptWithInvalidFormat() {
        String invalidFormat = "ENC(INVALID:someText)";
        String finalInvalidFormat = invalidFormat;
        assertThrows(UnsupportedOperationException.class, () -> {
            PasswordDecryptor.decrypt(finalInvalidFormat, TEST_HEX_KEY);
        });
    }

    @Test
    public void testDecryptWithUnsupportedAlgorithm() {
        String encrypted = "ENC(AES:unsupportedAlgorithm)";
        String finalEncrypted = encrypted;
        assertThrows(UnsupportedOperationException.class, () -> {
            PasswordDecryptor.decrypt(finalEncrypted, TEST_HEX_KEY);
        });
    }

    @Test
    public void testEncryptDecryptRoundTripWithMultipleInputs() throws Exception {
        String[] passwords = {
            "short",
            "MediumPassword123",
            "LongPasswordWithSpecialChars!@#$%^&*()",
            "密码测试",
            "1234567890",
            "a A b B c C",
            "!@#$%^&*()_+-=[]{}|;':\",./<>?"
        };

        for (String password : passwords) {
            String encrypted = PasswordEncryptor.encrypt(password, TEST_HEX_KEY);
            String decrypted = PasswordDecryptor.decrypt(encrypted, TEST_HEX_KEY);
            assertEquals(password, decrypted, "加密解密往返测试失败: " + password);
        }
    }

    @Test
    public void testDecryptWithWrongKey() {
        String wrongKey = "11111111111111111111111111111111";
        assertThrows(Exception.class, () -> {
            String encrypted = PasswordEncryptor.encrypt(TEST_PLAIN_PASSWORD, TEST_HEX_KEY);
            PasswordDecryptor.decrypt(encrypted, wrongKey);
        });
    }

    @Test
    public void testMalformedENCFormat() {
        String malformed = "ENC(SM4";
        String finalMalformed = malformed;
        assertThrows(IllegalArgumentException.class, () -> {
            PasswordDecryptor.decrypt(finalMalformed, TEST_HEX_KEY);
        });
    }
}
