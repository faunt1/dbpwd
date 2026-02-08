package com.dbpwd.crypto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sm4Cipher单元测试
 *
 * @author Druid DB Password Manager
 * @version 3.0
 */
public class Sm4CipherTest {

    private static final String TEST_HEX_KEY = "AABBCCDD1234567890ABCDEF12345678";
    private static final String TEST_PLAIN_TEXT = "MySecurePassword123!";
    private static final String INVALID_KEY_TOO_SHORT = "ABCD";
    private static final String INVALID_KEY_TOO_LONG = "AABBCCDD1234567890ABCDEF1234567890";
    private static final String INVALID_KEY_NON_HEX = "ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ";

    @BeforeEach
    public void setUp() {
        // 确保Provider已注册
        java.security.Security.addProvider(
            new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }

    @Test
    public void testEncryptSuccess() throws Exception {
        String encrypted = Sm4Cipher.encrypt(TEST_PLAIN_TEXT, TEST_HEX_KEY);

        assertNotNull(encrypted, "加密结果不应为null");
        assertNotEquals(TEST_PLAIN_TEXT, encrypted, "加密结果应与原文不同");
        assertFalse(encrypted.contains(TEST_PLAIN_TEXT), "加密结果不应包含原文");
    }

    @Test
    public void testDecryptSuccess() throws Exception {
        String encrypted = Sm4Cipher.encrypt(TEST_PLAIN_TEXT, TEST_HEX_KEY);
        String decrypted = Sm4Cipher.decrypt(encrypted, TEST_HEX_KEY);

        assertEquals(TEST_PLAIN_TEXT, decrypted, "解密结果应与原文一致");
    }

    @Test
    public void testEncryptDecryptRoundTrip() throws Exception {
        String[] testCases = {
            "short",
            "MediumPassword123",
            "LongPasswordWithSpecialChars!@#$%^&*()",
            "密码测试",
            "1234567890",
            ""
        };

        for (String plainText : testCases) {
            if (!plainText.isEmpty()) {  // 空字符串在后面单独测试
                String encrypted = Sm4Cipher.encrypt(plainText, TEST_HEX_KEY);
                String decrypted = Sm4Cipher.decrypt(encrypted, TEST_HEX_KEY);
                assertEquals(plainText, decrypted, "加密解密往返测试失败: " + plainText);
            }
        }
    }

    @Test
    public void testEncryptWithNullPlainText() {
        assertThrows(IllegalArgumentException.class, () -> {
            Sm4Cipher.encrypt(null, TEST_HEX_KEY);
        });
    }

    @Test
    public void testEncryptWithEmptyPlainText() {
        assertThrows(IllegalArgumentException.class, () -> {
            Sm4Cipher.encrypt("", TEST_HEX_KEY);
        });
    }

    @Test
    public void testEncryptWithNullKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            Sm4Cipher.encrypt(TEST_PLAIN_TEXT, null);
        });
    }

    @Test
    public void testEncryptWithEmptyKey() {
        assertThrows(IllegalArgumentException.class, () -> {
            Sm4Cipher.encrypt(TEST_PLAIN_TEXT, "");
        });
    }

    @Test
    public void testEncryptWithInvalidKeyLength() {
        assertThrows(IllegalArgumentException.class, () -> {
            Sm4Cipher.encrypt(TEST_PLAIN_TEXT, INVALID_KEY_TOO_SHORT);
        });
    }

    @Test
    public void testDecryptWithNullCipherText() {
        assertThrows(IllegalArgumentException.class, () -> {
            Sm4Cipher.decrypt(null, TEST_HEX_KEY);
        });
    }

    @Test
    public void testDecryptWithEmptyCipherText() {
        assertThrows(IllegalArgumentException.class, () -> {
            Sm4Cipher.decrypt("", TEST_HEX_KEY);
        });
    }

    @Test
    public void testDecryptWithInvalidKeyLength() throws Exception {
        String encrypted = Sm4Cipher.encrypt(TEST_PLAIN_TEXT, TEST_HEX_KEY);
        String finalEncrypted = encrypted;
        assertThrows(IllegalArgumentException.class, () -> {
            Sm4Cipher.decrypt(finalEncrypted, INVALID_KEY_TOO_LONG);
        });
    }

    @Test
    public void testGenerateRandomHexKey() {
        String key1 = Sm4Cipher.generateRandomHexKey();
        String key2 = Sm4Cipher.generateRandomHexKey();

        assertNotNull(key1, "生成的密钥不应为null");
        assertNotNull(key2, "生成的密钥不应为null");
        assertEquals(32, key1.length(), "密钥长度应为32字符");
        assertEquals(32, key2.length(), "密钥长度应为32字符");
        assertNotEquals(key1, key2, "两次生成的密钥应不同");

        // 验证密钥有效性
        assertTrue(Sm4Cipher.isValidKey(key1), "密钥应为有效hex格式");
        assertTrue(Sm4Cipher.isValidKey(key2), "密钥应为有效hex格式");
    }

    @Test
    public void testIsValidKey() {
        assertTrue(Sm4Cipher.isValidKey(TEST_HEX_KEY), "正确的密钥应有效");
        assertFalse(Sm4Cipher.isValidKey(INVALID_KEY_TOO_SHORT), "长度不足的密钥应无效");
        assertFalse(Sm4Cipher.isValidKey(INVALID_KEY_TOO_LONG), "长度超出的密钥应无效");
        assertFalse(Sm4Cipher.isValidKey(INVALID_KEY_NON_HEX), "非hex字符密钥应无效");
        assertFalse(Sm4Cipher.isValidKey(null), "null密钥应无效");
        assertFalse(Sm4Cipher.isValidKey(""), "空字符串密钥应无效");
    }

    @Test
    public void testGetKeyLength() {
        assertEquals(16, Sm4Cipher.getKeyLength(), "密钥长度应为16字节");
    }

    @Test
    public void testGetTransformation() {
        assertEquals("SM4/ECB/PKCS7Padding", Sm4Cipher.getTransformation(),
            "加密模式应为SM4/ECB/PKCS7Padding");
    }

    @Test
    public void testDifferentKeysProduceDifferentCipherText() throws Exception {
        String key1 = Sm4Cipher.generateRandomHexKey();
        String key2 = Sm4Cipher.generateRandomHexKey();

        String cipher1 = Sm4Cipher.encrypt(TEST_PLAIN_TEXT, key1);
        String cipher2 = Sm4Cipher.encrypt(TEST_PLAIN_TEXT, key2);

        assertNotEquals(cipher1, cipher2, "不同密钥应产生不同密文");
    }

    @Test
    public void testSameInputSameKeyProducesSameCipherText() throws Exception {
        String cipher1 = Sm4Cipher.encrypt(TEST_PLAIN_TEXT, TEST_HEX_KEY);
        String cipher2 = Sm4Cipher.encrypt(TEST_PLAIN_TEXT, TEST_HEX_KEY);

        assertEquals(cipher1, cipher2, "相同输入和密钥应产生相同密文");
    }
}
