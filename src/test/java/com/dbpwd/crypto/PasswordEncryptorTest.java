package com.dbpwd.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PasswordEncryptor单元测试
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class PasswordEncryptorTest {

    private static final String TEST_HEX_KEY = "AABBCCDD1234567890ABCDEF12345678";
    private static final String TEST_PLAIN_PASSWORD = "MySecurePassword123!";

    @Test
    public void testEncryptSuccess() throws Exception {
        String encrypted = PasswordEncryptor.encrypt(TEST_PLAIN_PASSWORD, TEST_HEX_KEY);

        assertNotNull(encrypted, "加密结果不应为null");
        assertTrue(encrypted.startsWith("ENC(SM4:"), "加密结果应为ENC格式");
        assertTrue(encrypted.endsWith(")"), "加密结果应以括号结束");
        assertNotEquals(TEST_PLAIN_PASSWORD, encrypted, "加密结果应与原文不同");
    }

    @Test
    public void testEncryptThenDecryptRoundTrip() throws Exception {
        String encrypted = PasswordEncryptor.encrypt(TEST_PLAIN_PASSWORD, TEST_HEX_KEY);
        String decrypted = PasswordDecryptor.decrypt(encrypted, TEST_HEX_KEY);

        assertEquals(TEST_PLAIN_PASSWORD, decrypted, "加密解密往返测试应成功");
    }

    @Test
    public void testIsEncrypted() {
        String encrypted = "ENC(SM4:someCipherText)";
        String plain = "plainPassword";

        assertTrue(PasswordEncryptor.isEncrypted(encrypted), "ENC格式应被识别为加密");
        assertFalse(PasswordEncryptor.isEncrypted(plain), "明文密码不应被识别为加密");
        assertFalse(PasswordEncryptor.isEncrypted(null), "null不应被识别为加密");
        assertFalse(PasswordEncryptor.isEncrypted(""), "空字符串不应被识别为加密");
    }

    @Test
    public void testEncryptDifferentInputsProduceDifferentOutputs() throws Exception {
        String encrypted1 = PasswordEncryptor.encrypt("password1", TEST_HEX_KEY);
        String encrypted2 = PasswordEncryptor.encrypt("password2", TEST_HEX_KEY);

        assertNotEquals(encrypted1, encrypted2, "不同输入应产生不同密文");
    }

    @Test
    public void testEncryptSameInputSameKeyProducesSameOutput() throws Exception {
        String encrypted1 = PasswordEncryptor.encrypt(TEST_PLAIN_PASSWORD, TEST_HEX_KEY);
        String encrypted2 = PasswordEncryptor.encrypt(TEST_PLAIN_PASSWORD, TEST_HEX_KEY);

        assertEquals(encrypted1, encrypted2, "相同输入和密钥应产生相同密文");
    }

    @Test
    public void testEncryptWithSpecialCharacters() throws Exception {
        String[] passwords = {
            "密码123",
            "!@#$%^&*()",
            "a A b B c C",
            "line1\nline2",
            "tab\there"
        };

        for (String password : passwords) {
            String encrypted = PasswordEncryptor.encrypt(password, TEST_HEX_KEY);
            String decrypted = PasswordDecryptor.decrypt(encrypted, TEST_HEX_KEY);
            assertEquals(password, decrypted, "特殊字符加密解密测试失败: " + password);
        }
    }

    @Test
    public void testEncryptWithNullPassword() {
        assertThrows(IllegalArgumentException.class, () -> {
            PasswordEncryptor.encrypt(null, TEST_HEX_KEY);
        });
    }

    @Test
    public void testEncryptWithEmptyPassword() {
        assertThrows(IllegalArgumentException.class, () -> {
            PasswordEncryptor.encrypt("", TEST_HEX_KEY);
        });
    }

    @Test
    public void testEncryptWithNullKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            PasswordEncryptor.encrypt(TEST_PLAIN_PASSWORD, null);
        });
    }

    @Test
    public void testEncryptWithEmptyKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            PasswordEncryptor.encrypt(TEST_PLAIN_PASSWORD, "");
        });
    }

    @Test
    public void testReencrypt() throws Exception {
        String originalKey = "AABBCCDD1234567890ABCDEF12345678";
        String encrypted = PasswordEncryptor.encrypt(TEST_PLAIN_PASSWORD, originalKey);

        // 用同一个密钥重新加密
        String reencrypted = PasswordEncryptor.reencrypt(encrypted, originalKey);

        // 使用新密钥解密应该失败，使用原密钥解密应该成功
        String decrypted1 = PasswordDecryptor.decrypt(reencrypted, originalKey);
        assertEquals(TEST_PLAIN_PASSWORD, decrypted1, "重新加密后解密应成功");
    }

    @Test
    public void testReencryptWithInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> {
            PasswordEncryptor.reencrypt("notEncrypted", TEST_HEX_KEY);
        });
    }

    @Test
    public void testEncryptProducesValidSM4Format() throws Exception {
        String encrypted = PasswordEncryptor.encrypt(TEST_PLAIN_PASSWORD, TEST_HEX_KEY);

        assertTrue(ENCFormat.isEncrypted(encrypted), "应为ENC格式");
        assertTrue(ENCFormat.isSM4Encrypted(encrypted), "应为SM4算法");

        String cipherText = ENCFormat.extractCipherText(encrypted);
        assertNotNull(cipherText, "密文不应为null");
        assertFalse(cipherText.isEmpty(), "密文不应为空");
    }
}
