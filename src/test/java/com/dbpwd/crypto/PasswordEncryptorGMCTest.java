package com.dbpwd.crypto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PasswordEncryptor GCM功能测试
 *
 * 测试SM4-GCM认证加密功能集成，以及从ECB到GCM的完整迁移
 *
 * @author Druid DB Password Manager
 * @version 4.0
 */
public class PasswordEncryptorGMCTest {

    private static final String TEST_KEY = "00112233445566778899AABBCCDDEEFF";

    // ==================== GCM加密解密测试 ====================

    @Test
    public void testEncryptUsesGCM() throws Exception {
        String plainText = "TestPassword123!";

        // encrypt()现在默认使用GCM
        String encrypted = PasswordEncryptor.encrypt(plainText, TEST_KEY);
        String decrypted = PasswordDecryptor.decrypt(encrypted, TEST_KEY);

        assertEquals(plainText, decrypted);
    }

    @Test
    public void testEncryptGCMFormat() throws Exception {
        String plainText = "TestPassword";

        String encrypted = PasswordEncryptor.encrypt(plainText, TEST_KEY);

        // 验证格式：ENC(SM4-GCM:IV:密文)
        assertTrue(encrypted.startsWith("ENC(SM4-GCM:"));
        assertTrue(encrypted.endsWith(")"));

        // 验证ENC格式识别
        assertTrue(ENCFormat.isSM4GCMEncrypted(encrypted));
        assertTrue(ENCFormat.isEncrypted(encrypted));

        // 提取算法
        assertEquals("SM4-GCM", ENCFormat.extractAlgorithm(encrypted));
    }

    @Test
    public void testEncryptGCMRandomIV() throws Exception {
        String plainText = "SamePassword123";

        String encrypted1 = PasswordEncryptor.encrypt(plainText, TEST_KEY);
        String encrypted2 = PasswordEncryptor.encrypt(plainText, TEST_KEY);

        // GCM使用随机IV，应该产生不同的密文
        assertNotEquals(encrypted1, encrypted2);

        // 解密后应该得到相同明文
        assertEquals(plainText, PasswordDecryptor.decrypt(encrypted1, TEST_KEY));
        assertEquals(plainText, PasswordDecryptor.decrypt(encrypted2, TEST_KEY));
    }

    // ==================== GCM防篡改测试 ====================

    @Test
    public void testGCMIntegrityCheck() throws Exception {
        String plainText = "OriginalPassword";
        String encrypted = PasswordEncryptor.encrypt(plainText, TEST_KEY);

        // 篡改密文中的某一部分
        String tampered = encrypted.replace(encrypted.charAt(10), 'X');

        // GCM应该检测到篡改并抛出异常
        Exception e = assertThrows(Exception.class, () -> {
            PasswordDecryptor.decrypt(tampered, TEST_KEY);
        });

        assertNotNull(e);
    }

    // ==================== ECB与GCM对比测试 ====================

    @Test
    public void testECBvsGCM() throws Exception {
        String plainText = "Password123";

        // ECB加密（使用Sm4Cipher直接）
        String encryptedECB = ENCFormat.formatSM4(Sm4Cipher.encrypt(plainText, TEST_KEY));

        // GCM加密（新方式，默认）
        String encryptedGCM = PasswordEncryptor.encrypt(plainText, TEST_KEY);

        // 两种方式都应该能正确解密
        assertEquals(plainText, PasswordDecryptor.decrypt(encryptedECB, TEST_KEY));
        assertEquals(plainText, PasswordDecryptor.decrypt(encryptedGCM, TEST_KEY));

        // 但密文格式不同
        assertNotEquals(encryptedECB, encryptedGCM);
        assertTrue(encryptedECB.startsWith("ENC(SM4:"));
        assertTrue(encryptedGCM.startsWith("ENC(SM4-GCM:"));
    }

    @Test
    public void testECBDeterministic() throws Exception {
        String plainText = "DeterministicPassword";

        // ECB是确定性的（相同密钥和明文产生相同密文）
        String encrypted1 = ENCFormat.formatSM4(Sm4Cipher.encrypt(plainText, TEST_KEY));
        String encrypted2 = ENCFormat.formatSM4(Sm4Cipher.encrypt(plainText, TEST_KEY));

        assertEquals(encrypted1, encrypted2);

        // GCM是非确定性的（随机IV）
        String encryptedGCM1 = PasswordEncryptor.encrypt(plainText, TEST_KEY);
        String encryptedGCM2 = PasswordEncryptor.encrypt(plainText, TEST_KEY);

        assertNotEquals(encryptedGCM1, encryptedGCM2);
    }

    // ==================== 升级测试 ====================

    @Test
    public void testMigrateECBToGCM() throws Exception {
        String plainText = "ToBeMigrated";

        // 原有ECB加密
        String encryptedECB = ENCFormat.formatSM4(Sm4Cipher.encrypt(plainText, TEST_KEY));

        // 迁移到GCM
        String encryptedGCM = PasswordEncryptor.migrateECBToGCM(encryptedECB, TEST_KEY);

        // 解密GCM加密的密码
        String decrypted = PasswordDecryptor.decrypt(encryptedGCM, TEST_KEY);

        assertEquals(plainText, decrypted);
        assertTrue(encryptedGCM.startsWith("ENC(SM4-GCM:"));
    }

    @Test
    public void testMigrateECBPasswords() throws Exception {
        String[] passwords = {
            "password1", "P@ssw0rd!", "Test123", "DbP@ss2024!"
        };

        for (String password : passwords) {
            // ECB加密
            String encryptedECB = ENCFormat.formatSM4(Sm4Cipher.encrypt(password, TEST_KEY));

            // 解密ECB（向后兼容）
            String decrypted = PasswordDecryptor.decrypt(encryptedECB, TEST_KEY);
            assertEquals(password, decrypted);

            // 迁移到GCM
            String encryptedGCM = PasswordEncryptor.migrateECBToGCM(encryptedECB, TEST_KEY);

            // 解密GCM
            String decryptedGCM = PasswordDecryptor.decrypt(encryptedGCM, TEST_KEY);
            assertEquals(password, decryptedGCM);
        }
    }

    // ==================== 混合格式测试 ====================

    @Test
    public void testDecryptMixedFormats() throws Exception {
        String plainText = "MixedFormatPassword";

        // ECB加密
        String encryptedECB = ENCFormat.formatSM4(Sm4Cipher.encrypt(plainText, TEST_KEY));

        // GCM加密
        String encryptedGCM = PasswordEncryptor.encrypt(plainText, TEST_KEY);

        // 明文
        String plain = plainText;

        // 都应该能通过PasswordDecryptor.decrypt正确解密/识别
        assertEquals(plainText, PasswordDecryptor.decrypt(encryptedECB, TEST_KEY));
        assertEquals(plainText, PasswordDecryptor.decrypt(encryptedGCM, TEST_KEY));
        assertEquals(plain, PasswordDecryptor.autoDecrypt(plain, TEST_KEY));
    }

    // ==================== 性能测试 ====================

    @Test
    public void testGCMMultipleEncryptions() throws Exception {
        String plainText = "TestPassword123!";

        for (int i = 0; i < 50; i++) {
            String encrypted = PasswordEncryptor.encrypt(plainText, TEST_KEY);
            String decrypted = PasswordDecryptor.decrypt(encrypted, TEST_KEY);
            assertEquals(plainText, decrypted);
        }
    }

    @Test
    public void testGCMPerformance() throws Exception {
        String plainText = "PasswordPerformanceTest";
        int iterations = 100;

        // 预热
        for (int i = 0; i < 10; i++) {
            PasswordEncryptor.encrypt(plainText, TEST_KEY);
        }

        long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            String encrypted = PasswordEncryptor.encrypt(plainText, TEST_KEY);
            PasswordDecryptor.decrypt(encrypted, TEST_KEY);
        }
        long endTime = System.nanoTime();

        long durationMs = (endTime - startTime) / 1_000_000;
        System.out.println("GCM加解密 " + iterations + " 次耗时: " + durationMs + "ms");

        // 期望在合理时间内完成（5秒内完成100次加解密）
        assertTrue(durationMs < 5000, "GCM性能测试超时");
    }

    // ==================== 特殊字符测试 ====================

    @Test
    public void testGCMEncryptSpecialCharacters() throws Exception {
        String plainText = "P@ssw0rd!#$%^&*()_+-={}[]|\\:;\"'<>,.?/~`";

        String encrypted = PasswordEncryptor.encrypt(plainText, TEST_KEY);
        String decrypted = PasswordDecryptor.decrypt(encrypted, TEST_KEY);

        assertEquals(plainText, decrypted);
    }

    @Test
    public void testGCMEncryptUnicode() throws Exception {
        String plainText = "密码中文测试123测试！@#￥%";

        String encrypted = PasswordEncryptor.encrypt(plainText, TEST_KEY);
        String decrypted = PasswordDecryptor.decrypt(encrypted, TEST_KEY);

        assertEquals(plainText, decrypted);
    }

    @Test
    public void testGCMEncryptEmoji() throws Exception {
        String plainText = "Test😀🎉😀🎉🚀Password🌟✨";

        String encrypted = PasswordEncryptor.encrypt(plainText, TEST_KEY);
        String decrypted = PasswordDecryptor.decrypt(encrypted, TEST_KEY);

        assertEquals(plainText, decrypted);
    }

    // ==================== 边界条件测试 ====================

    @Test
    public void testEncryptNullPassword() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            PasswordEncryptor.encrypt(null, TEST_KEY);
        });
        assertTrue(e.getMessage().contains("明文"));
    }

    @Test
    public void testEncryptEmptyPassword() {
        Exception e = assertThrows(IllegalArgumentException.class, () -> {
            PasswordEncryptor.encrypt("", TEST_KEY);
        });
        assertTrue(e.getMessage().contains("明文"));
    }

    @Test
    public void testDecryptGCMWithWrongKey() throws Exception {
        String plainText = "OriginalPassword";
        String correctKey = TEST_KEY;
        String wrongKey = "FFEEDDCCBBAA99887766554433221100";

        String encrypted = PasswordEncryptor.encrypt(plainText, correctKey);

        // 使用错误的密钥解密应该失败（GCM认证失败）
        Exception e = assertThrows(Exception.class, () -> {
            PasswordDecryptor.decrypt(encrypted, wrongKey);
        });

        assertNotNull(e);
    }

    // ==================== autoDecrypt功能测试 ====================

    @Test
    public void testAutoDecryptWithAllFormats() throws Exception {
        String plainText = "AutoDecryptTest";

        // 明文
        assertEquals(plainText, PasswordDecryptor.autoDecrypt(plainText, TEST_KEY));

        // ECB加密
        String encryptedECB = ENCFormat.formatSM4(Sm4Cipher.encrypt(plainText, TEST_KEY));
        assertEquals(plainText, PasswordDecryptor.autoDecrypt(encryptedECB, TEST_KEY));

        // GCM加密
        String encryptedGCM = PasswordEncryptor.encrypt(plainText, TEST_KEY);
        assertEquals(plainText, PasswordDecryptor.autoDecrypt(encryptedGCM, TEST_KEY));

        // 空字符串
        assertEquals("", PasswordDecryptor.autoDecrypt("", TEST_KEY));

        // null
        assertNull(PasswordDecryptor.autoDecrypt(null, TEST_KEY));
    }

    // ==================== isEncrypted功能测试 ====================

    @Test
    public void testIsEncrypted() throws Exception {
        String plainText = "TestPassword";

        // GCM加密应该被识别
        String encryptedGCM = PasswordEncryptor.encrypt(plainText, TEST_KEY);
        assertTrue(PasswordEncryptor.isEncrypted(encryptedGCM));

        // ECB加密应该被识别
        String encryptedECB = ENCFormat.formatSM4(Sm4Cipher.encrypt(plainText, TEST_KEY));
        assertTrue(PasswordEncryptor.isEncrypted(encryptedECB));

        // 明文不应该被识别
        assertFalse(PasswordEncryptor.isEncrypted(plainText));
        assertFalse(PasswordEncryptor.isEncrypted(""));
    }

    // ==================== 批量迁移测试 ====================

    @Test
    public void testBatchMigration() throws Exception {
        String[] passwords = {"pass1", "pass2", "pass3", "P@ss#123", "测试密码"};

        for (String password : passwords) {
            // ECB
            String ecbPassword = ENCFormat.formatSM4(Sm4Cipher.encrypt(password, TEST_KEY));

            // 迁移到GCM
            String gcmPassword = PasswordEncryptor.migrateECBToGCM(ecbPassword, TEST_KEY);

            // 验证
            assertTrue(ENCFormat.isSM4GCMEncrypted(gcmPassword),
                "迁移后应该是GCM格式: " + password);
            assertEquals(password, PasswordDecryptor.decrypt(gcmPassword, TEST_KEY),
                "迁移后应该能够解密: " + password);
        }
    }

    // ==================== 迁移错误处理测试 ====================

    @Test
    public void testMigrateInvalidFormat() {
        assertThrows(IllegalArgumentException.class,
            () -> PasswordEncryptor.migrateECBToGCM("not_encrypted", TEST_KEY),
            "非ENC格式应该抛出异常");

        assertThrows(IllegalArgumentException.class,
            () -> PasswordEncryptor.migrateECBToGCM("ENC(InvalidFormat:cipher)", TEST_KEY),
            "无效的ENC格式应该抛出异常");
    }
}
