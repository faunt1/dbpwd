package com.dbpwd.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PasswordEncryptor单元测试
 *
 * 测试SM4-GCM密码加密功能
 *
 * @author Druid DB Password Manager
 * @version 4.0
 */
public class PasswordEncryptorTest {

    private static final String TEST_HEX_KEY = "AABBCCDD1234567890ABCDEF12345678";
    private static final String TEST_PLAIN_PASSWORD = "MySecurePassword123!";

    @Test
    public void testEncryptSuccess() throws Exception {
        String encrypted = PasswordEncryptor.encrypt(TEST_PLAIN_PASSWORD, TEST_HEX_KEY);

        assertNotNull(encrypted, "加密结果不应为null");
        assertTrue(encrypted.startsWith("ENC(SM4-GCM:"), "加密结果应为SM4-GCM的ENC格式");
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
        String encryptedGCM = "ENC(SM4-GCM:iv:ciphertext)";
        String encryptedECB = "ENC(SM4:ciphertext)";
        String plain = "plainPassword";

        assertTrue(PasswordEncryptor.isEncrypted(encryptedGCM), "SM4-GCM的ENC格式应被识别为加密");
        assertTrue(PasswordEncryptor.isEncrypted(encryptedECB), "SM4的ENC格式应被识别为加密");
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
    public void testEncryptSameInputProducesDifferentOutputs_GCMRandomIV() throws Exception {
        // GCM模式使用随机IV，即使相同输入也会产生不同输出
        String encrypted1 = PasswordEncryptor.encrypt(TEST_PLAIN_PASSWORD, TEST_HEX_KEY);
        String encrypted2 = PasswordEncryptor.encrypt(TEST_PLAIN_PASSWORD, TEST_HEX_KEY);

        assertNotEquals(encrypted1, encrypted2, "GCM模式下相同输入应产生不同密文（随机IV）");

        // 但解密结果应该相同
        String decrypted1 = PasswordDecryptor.decrypt(encrypted1, TEST_HEX_KEY);
        String decrypted2 = PasswordDecryptor.decrypt(encrypted2, TEST_HEX_KEY);
        assertEquals(TEST_PLAIN_PASSWORD, decrypted1);
        assertEquals(TEST_PLAIN_PASSWORD, decrypted2);
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
    public void testMigrateECBToGCM() throws Exception {
        // 使用Sm4Cipher创建ECB加密
        String ecbCipher = Sm4Cipher.encrypt(TEST_PLAIN_PASSWORD, TEST_HEX_KEY);
        String ecbEncrypted = ENCFormat.formatSM4(ecbCipher);

        // 迁移到GCM
        String gcmEncrypted = PasswordEncryptor.migrateECBToGCM(ecbEncrypted, TEST_HEX_KEY);

        // 验证迁移结果
        assertTrue(ENCFormat.isSM4GCMEncrypted(gcmEncrypted), "迁移后应为SM4-GCM格式");

        // 解密验证
        String decrypted = PasswordDecryptor.decrypt(gcmEncrypted, TEST_HEX_KEY);
        assertEquals(TEST_PLAIN_PASSWORD, decrypted, "迁移后应能正确解密");
    }

    @Test
    public void testMigrateECBToGCMWithInvalidFormat() {
        assertThrows(IllegalArgumentException.class, () -> {
            PasswordEncryptor.migrateECBToGCM("notEncrypted", TEST_HEX_KEY);
        });
    }

    @Test
    public void testEncryptProducesValidSM4GCMFormat() throws Exception {
        String encrypted = PasswordEncryptor.encrypt(TEST_PLAIN_PASSWORD, TEST_HEX_KEY);

        assertTrue(ENCFormat.isEncrypted(encrypted), "应为ENC格式");
        assertTrue(ENCFormat.isSM4GCMEncrypted(encrypted), "应为SM4-GCM算法");

        String cipherText = ENCFormat.extractCipherText(encrypted);
        assertNotNull(cipherText, "密文不应为null");
        assertFalse(cipherText.isEmpty(), "密文不应为空");

        String algorithm = ENCFormat.extractAlgorithm(encrypted);
        assertEquals("SM4-GCM", algorithm, "算法应为SM4-GCM");
    }

    @Test
    public void testGCMProvidesIntegrityProtection() throws Exception {
        String encrypted = PasswordEncryptor.encrypt(TEST_PLAIN_PASSWORD, TEST_HEX_KEY);

        // 篡改密文
        String tampered = encrypted.replace(encrypted.charAt(15), 'X');

        // GCM应该检测到篡改
        assertThrows(Exception.class, () -> {
            PasswordDecryptor.decrypt(tampered, TEST_HEX_KEY);
        }, "GCM模式应该检测到密文篡改");
    }
}
