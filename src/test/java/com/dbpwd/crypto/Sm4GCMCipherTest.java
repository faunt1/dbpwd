package com.dbpwd.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Sm4GCMCipher单元测试
 *
 * 测试SM4-GCM认证加密功能
 *
 * @author Druid DB Password Manager
 * @version 3.2
 */
public class Sm4GCMCipherTest {

    private static final String TEST_KEY = "00112233445566778899AABBCCDDEEFF";
    private static final String TEST_KEY2 = "FFEEDDCCBBAA99887766554433221100";

    // ==================== 基本加密解密测试 ====================

    @Test
    public void testEncryptAndDecrypt() throws Exception {
        String plainText = "Hello, SM4-GCM!";

        String encrypted = Sm4GCMCipher.encrypt(plainText, TEST_KEY);
        String decrypted = Sm4GCMCipher.decrypt(encrypted, TEST_KEY);

        assertEquals(plainText, decrypted);
    }

    @Test
    public void testEncryptAndDecryptLongPassword() throws Exception {
        String plainText = "ThisIsAVeryLongDatabasePassword123!@#$%^&*()";

        String encrypted = Sm4GCMCipher.encrypt(plainText, TEST_KEY);
        String decrypted = Sm4GCMCipher.decrypt(encrypted, TEST_KEY);

        assertEquals(plainText, decrypted);
    }

    @Test
    public void testEncryptDifferentKeys() throws Exception {
        String plainText = "SamePassword123";

        String encrypted1 = Sm4GCMCipher.encrypt(plainText, TEST_KEY);
        String encrypted2 = Sm4GCMCipher.encrypt(plainText, TEST_KEY2);

        // GCM使用随机IV，相同密钥加密相同明文会产生不同密文
        assertNotEquals(encrypted1, encrypted2);

        // 解密后应该得到相同明文
        assertEquals(plainText, Sm4GCMCipher.decrypt(encrypted1, TEST_KEY));
        assertEquals(plainText, Sm4GCMCipher.decrypt(encrypted2, TEST_KEY2));
    }

    @Test
    public void testEncryptSameKeyDifferentResults() throws Exception {
        String plainText = "TestPassword";

        String encrypted1 = Sm4GCMCipher.encrypt(plainText, TEST_KEY);
        String encrypted2 = Sm4GCMCipher.encrypt(plainText, TEST_KEY);

        // GCM使用随机IV，应该产生不同的密文
        assertNotEquals(encrypted1, encrypted2);

        // 但解密后应该得到相同明文
        assertEquals(plainText, Sm4GCMCipher.decrypt(encrypted1, TEST_KEY));
        assertEquals(plainText, Sm4GCMCipher.decrypt(encrypted2, TEST_KEY));
    }

    // ==================== 认证标签防篡改测试 ====================

    @Test
    public void testDetectTampering() throws Exception {
        String plainText = "OriginalPassword123";
        String encrypted = Sm4GCMCipher.encrypt(plainText, TEST_KEY);

        // 篡改密文（修改某一部分）
        String[] parts = encrypted.split(":");
        String base64Iv = parts[0];
        String base64Ciphertext = parts[1];
        byte[] ciphertext = java.util.Base64.getDecoder().decode(base64Ciphertext);

        // 修改密文的一个字节
        ciphertext[0] = (byte) (ciphertext[0] ^ 0x01);

        String tampered = base64Iv + ":" + java.util.Base64.getEncoder().encodeToString(ciphertext);

        // 应该在解密时抛出异常（认证标签验证失败）
        assertThrows(Exception.class, () -> {
            Sm4GCMCipher.decrypt(tampered, TEST_KEY);
        });
    }

    @Test
    public void testDetectIvTampering() throws Exception {
        String plainText = "Password123";
        String encrypted = Sm4GCMCipher.encrypt(plainText, TEST_KEY);

        // 修改IV
        String[] parts = encrypted.split(":");
        byte[] iv = java.util.Base64.getDecoder().decode(parts[0]);
        iv[0] = (byte) (iv[0] ^ 0x01);

        String tampered = java.util.Base64.getEncoder().encodeToString(iv) + ":" + parts[1];

        // IV错误时解密应该失败（或产生错误结果，但GCM会检测到）
        Exception e = assertThrows(Exception.class, () -> {
            Sm4GCMCipher.decrypt(tampered, TEST_KEY);
        });
        assertTrue(e.getMessage().contains("GCM") || e.getMessage().contains("认证"));
    }

    // ==================== 边界条件测试 ====================

    @Test
    public void testEncryptEmptyPassword() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            Sm4GCMCipher.encrypt("", TEST_KEY);
        });
        assertTrue(e.getMessage().contains("明文"));
    }

    @Test
    public void testDecryptWithNullCipherText() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            Sm4GCMCipher.decrypt(null, TEST_KEY);
        });
        assertTrue(e.getMessage().contains("密文"));
    }

    @Test
    public void testDecryptWithEmptyCipherText() {
        assertThrows(Exception.class, () -> {
            Sm4GCMCipher.decrypt("", TEST_KEY);
        });
    }

    @Test
    public void testEncryptWithNullKey() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            Sm4GCMCipher.encrypt("test", null);
        });
        assertTrue(e.getMessage().contains("密钥"));
    }

    @Test
    public void testEncryptWithEmptyKey() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            Sm4GCMCipher.encrypt("test", "");
        });
        assertTrue(e.getMessage().contains("密钥"));
    }

    @Test
    public void testDecryptWithNullKey() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            Sm4GCMCipher.decrypt("test", null);
        });
        assertTrue(e.getMessage().contains("密钥"));
    }

    // ==================== 密钥格式测试 ====================

    @Test
    public void testIsValidKeyValid() {
        assertTrue(Sm4GCMCipher.isValidKey(TEST_KEY));
    }

    @Test
    public void testIsValidKeyInvalidLength() {
        assertFalse(Sm4GCMCipher.isValidKey("0011223344556677"));  // 16字符 = 8字节
        assertFalse(Sm4GCMCipher.isValidKey("00112233445566778899AABBCCDDEEFF00112233"));  // 40字符 = 20字节
    }

    @Test
    public void testIsValidKeyInvalidCharacters() {
        assertFalse(Sm4GCMCipher.isValidKey("GGGGGGGGGGGGGGGGGGGGGGGGGGGGGGGG"));
        assertFalse(Sm4GCMCipher.isValidKey("ZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZZ"));
    }

    @Test
    public void testIsValidKeyNull() {
        assertFalse(Sm4GCMCipher.isValidKey(null));
    }

    @Test
    public void testIsValidKeyEmpty() {
        assertFalse(Sm4GCMCipher.isValidKey(""));
    }

    @Test
    public void testIsValidKeyLowerCase() {
        assertTrue(Sm4GCMCipher.isValidKey("00112233445566778899aabbccddeeff"));
    }

    @Test
    public void testIsValidKeyMixedCase() {
        assertTrue(Sm4GCMCipher.isValidKey("00112233445566778899AaBbCcDdEeFf"));
    }

    // ==================== 密钥生成测试 ====================

    @Test
    public void testGenerateRandomHexKey() {
        String key = Sm4GCMCipher.generateRandomHexKey();

        assertNotNull(key);
        assertEquals(32, key.length());  // 16字节 = 32个hex字符
        assertTrue(Sm4GCMCipher.isValidKey(key));
    }

    @Test
    public void testGenerateRandomHexKeyUniqueness() {
        String key1 = Sm4GCMCipher.generateRandomHexKey();
        String key2 = Sm4GCMCipher.generateRandomHexKey();

        // 随机生成的密钥不太可能相同
        assertNotEquals(key1, key2);
    }

    // ==================== 特殊字符测试 ====================

    @Test
    public void testEncryptDecryptSpecialCharacters() throws Exception {
        String plainText = "P@ssw0rd!#$%^&*()_-+={}[]|\\:;\"'<>,.?/~`";

        String encrypted = Sm4GCMCipher.encrypt(plainText, TEST_KEY);
        String decrypted = Sm4GCMCipher.decrypt(encrypted, TEST_KEY);

        assertEquals(plainText, decrypted);
    }

    @Test
    public void testEncryptDecryptUnicode() throws Exception {
        String plainText = "密码中文测试123测试！@#￥%";

        String encrypted = Sm4GCMCipher.encrypt(plainText, TEST_KEY);
        String decrypted = Sm4GCMCipher.decrypt(encrypted, TEST_KEY);

        assertEquals(plainText, decrypted);
    }

    @Test
    public void testEncryptDecryptEmoji() throws Exception {
        String plainText = "Test😀🎉🚀Password🌟✨";

        String encrypted = Sm4GCMCipher.encrypt(plainText, TEST_KEY);
        String decrypted = Sm4GCMCipher.decrypt(encrypted, TEST_KEY);

        assertEquals(plainText, decrypted);
    }

    // ==================== 密文格式测试 ====================

    @Test
    public void testCipherTextFormat() throws Exception {
        String plainText = "TestPassword";
        String encrypted = Sm4GCMCipher.encrypt(plainText, TEST_KEY);

        // 验证格式：IV:密文
        assertTrue(encrypted.contains(":"));
        String[] parts = encrypted.split(":");
        assertEquals(2, parts.length);

        // 验证IV部分是有效的Base64
        byte[] iv = java.util.Base64.getDecoder().decode(parts[0]);
        assertEquals(Sm4GCMCipher.getIvLength(), iv.length);
    }

    @Test
    public void testDecryptInvalidFormat() {
        assertThrows(Exception.class, () -> {
            Sm4GCMCipher.decrypt("invalidformat", TEST_KEY);
        });
    }

    @Test
    public void testDecryptInvalidBase64() {
        assertThrows(Exception.class, () -> {
            Sm4GCMCipher.decrypt("notbase64:notbase64", TEST_KEY);
        });
    }

    // ==================== 属性获取测试 ====================

    @Test
    public void testGetKeyLength() {
        assertEquals(16, Sm4GCMCipher.getKeyLength());
    }

    @Test
    public void testGetTransformation() {
        assertEquals("SM4/GCM/NoPadding", Sm4GCMCipher.getTransformation());
    }

    @Test
    public void testGetIvLength() {
        assertEquals(12, Sm4GCMCipher.getIvLength());
    }

    @Test
    public void testGetTagLength() {
        assertEquals(16, Sm4GCMCipher.getTagLength());
    }

    // ==================== 性能测试示例 ====================

    @Test
    public void testMultipleEncryptions() throws Exception {
        String plainText = "DatabasePassword123!";

        for (int i = 0; i < 100; i++) {
            String encrypted = Sm4GCMCipher.encrypt(plainText, TEST_KEY);
            String decrypted = Sm4GCMCipher.decrypt(encrypted, TEST_KEY);
            assertEquals(plainText, decrypted);
        }
    }

    @Test
    public void testDifferentPasswords() throws Exception {
        String[] passwords = {
            "password1", "P@ssw0rd!", "Test123", "密码测试",
            generateString('A', 100), generateString('Z', 100)
        };

        for (String password : passwords) {
            String encrypted = Sm4GCMCipher.encrypt(password, TEST_KEY);
            String decrypted = Sm4GCMCipher.decrypt(encrypted, TEST_KEY);
            assertEquals(password, decrypted);
        }
    }

    /**
     * JDK 8兼容的字符串重复方法
     */
    private String generateString(char c, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    // ==================== 错误密钥测试 ====================

    @Test
    public void testDecryptWithDifferentKey() throws Exception {
        String plainText = "OriginalPassword";
        String encrypted = Sm4GCMCipher.encrypt(plainText, TEST_KEY);

        // 使用错误的密钥解密（GCM会检测到认证错误）
        Exception e = assertThrows(Exception.class, () -> {
            Sm4GCMCipher.decrypt(encrypted, TEST_KEY2);
        });

        // 错误密钥应该导致解密失败（认证标签不匹配）
        assertNotNull(e);
    }

    @Test
    public void testEncryptWithInvalidKeyLength() {
        String invalidKey = "0011223344556677";  // 只有8字节

        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            Sm4GCMCipher.encrypt("test", invalidKey);
        });
        assertTrue(e.getMessage().contains("密钥长度不正确"));
    }
}
