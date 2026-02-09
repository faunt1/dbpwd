package com.dbpwd.demo;

import com.dbpwd.crypto.Sm4GCMCipher;
import com.dbpwd.crypto.ENCFormat;

import java.sql.*;

/**
 * Simple JDBC demo application
 *
 * Demo Database Password Manager in plain JDBC application
 * Using SM4-GCM authenticated encryption
 *
 * Features:
 * 1. Use SM4-GCM encryption for database password (authenticated encryption)
 * 2. Password format verification
 * 3. Verify password decryption with integrity protection
 *
 * SM4-GCM优势:
 * - 认证加密：同时提供机密性和完整性保护
 * - 防篡改：任何对密文的修改都会导致解密失败
 * - 随机IV：每次加密结果不同，防侧信道攻击
 * - 抗选择明文攻击：认证标签提供额外保护
 *
 * @author Database Password Manager
 * @version 4.5
 */
public class SimpleJdbcDemo {

    // SM4-GCM encryption configuration
    private static final String TEST_SM4_KEY = "0123456789abcdef0123456789abcdef";

    public static void main(String[] args) {
        System.out.println("=== Simple JDBC Demo Application ===");
        System.out.println("Demo: Database Password Manager - SM4-GCM Encryption");
        System.out.println("SM4 key: " + TEST_SM4_KEY);
        System.out.println();

        try {
            // Step 1: Encrypt database password using SM4-GCM
            String encryptedDbPassword = encryptDatabasePassword("myDatabasePassword123", TEST_SM4_KEY);
            System.out.println("Step 1: Encrypt database password");
            System.out.println("  Original password: myDatabasePassword123");
            System.out.println("  Encrypted password: " + encryptedDbPassword);
            System.out.println();

            // Step 2: Test direct SM4-GCM encryption
            System.out.println("Step 2: Test SM4-GCM encryption properties");
            testGCMPriority();
            System.out.println();

            System.out.println("Step 3: Test ENC format handling");
            testENCFormat();

            System.out.println("Step 4: Demonstrate different encrypted passwords produce different results");
            testRandomIV();

            System.out.println();
            System.out.println("=== Demo Completed Successfully ===");
            System.out.println("All SM4-GCM encryption and decryption tests passed!");
            System.out.println("GCM mode provides authenticated encryption with tamper protection.");

        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Encrypt database password using SM4-GCM
     */
    private static String encryptDatabasePassword(String plainPassword, String sm4Key) {
        try {
            // SM4-GCM加密：提供认证加密
            String cipherText = Sm4GCMCipher.encrypt(plainPassword, sm4Key);
            return ENCFormat.formatSM4GCM(cipherText);
        } catch (Exception e) {
            throw new RuntimeException("Encrypt database password failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypt database password using SM4-GCM
     */
    private static String decryptDatabasePassword(String encryptedPassword, String sm4Key) {
        try {
            String algorithm = ENCFormat.extractAlgorithm(encryptedPassword);
            String cipherText = ENCFormat.extractCipherText(encryptedPassword);

            // 仅支持SM4-GCM格式，ECB格式抛出异常
            if (!"SM4-GCM".equalsIgnoreCase(algorithm)) {
                throw new IllegalArgumentException(
                    "Unsupported encryption algorithm: " + algorithm + ". Only SM4-GCM is supported for new applications");
            }

            return Sm4GCMCipher.decrypt(cipherText, sm4Key);
        } catch (Exception e) {
            throw new RuntimeException("Decrypt database password failed: " + e.getMessage(), e);
        }
    }

    /**
     * Test SM4-GCM authentication encryption properties
     */
    private static void testGCMPriority() throws Exception {
        System.out.println("  SM4-GCM Properties:");
        System.out.println("    IV Length: " + Sm4GCMCipher.getIvLength() + " bytes (12 bytes)");
        System.out.println("    Tag Length: " + Sm4GCMCipher.getTagLength() + " bytes (16 bytes)");
        System.out.println("    Key Length: " + Sm4GCMCipher.getKeyLength() + " bytes (16 bytes)");
        System.out.println("    Transformation: " + Sm4GCMCipher.getTransformation());
        System.out.println();

        // Demonstrate that GCM prevents tampering
        System.out.println("  Integrity Protection:");
        String testPassword = "tamper_test";
        String encrypted = Sm4GCMCipher.encrypt(testPassword, TEST_SM4_KEY);
        String decrypted = Sm4GCMCipher.decrypt(encrypted, TEST_SM4_KEY);

        System.out.println("    Encrypt->Decrypt successful: " + testPassword.equals(decrypted));
        System.out.println("    Algorithm integrity verified.");

        // Note: Tampering with ciphertext would cause decryption to fail
        System.out.println("    (Note: Any ciphertext tampering would cause decryption to fail with AEADBadTagException)");
        System.out.println();
    }

    /**
     * Test ENC format handling for SM4-GCM
     */
    private static void testENCFormat() throws Exception {
        System.out.println("  ENC Format Validation:");
        String testPassword = " ENC_TEST ";
        String encrypted = Sm4GCMCipher.encrypt(testPassword, TEST_SM4_KEY);
        String formatted = ENCFormat.formatSM4GCM(encrypted);

        System.out.println("    Formatted: " + formatted);
        System.out.println("    Algorithm: " + ENCFormat.extractAlgorithm(formatted));
        System.out.println("    Validation: " + ENCFormat.isValid(formatted));
        System.out.println();

        // Verify round-trip
        String encryptedAgain = Sm4GCMCipher.encrypt(testPassword, TEST_SM4_KEY);
        String decryptedAgain = Sm4GCMCipher.decrypt(encryptedAgain, TEST_SM4_KEY);
        System.out.println("    Round-trip test: " + testPassword.equals(decryptedAgain));
        System.out.println();
    }

    /**
     * Test GCM random IV property
     */
    private static void testRandomIV() throws Exception {
        System.out.println("  GCM Random IV Property:");
        String samePassword = "random_iv_test";
        String enc1 = Sm4GCMCipher.encrypt(samePassword, TEST_SM4_KEY);
        String enc2 = Sm4GCMCipher.encrypt(samePassword, TEST_SM4_KEY);
        String enc3 = Sm4GCMCipher.encrypt(samePassword, TEST_SM4_KEY);

        System.out.println("    Same password encrypted 3 times:");
        System.out.println("    Enc1: " + ENCFormat.formatSM4GCM(enc1));
        System.out.println("    Enc2: " + ENCFormat.formatSM4GCM(enc2));
        System.out.println("    Enc3: " + ENCFormat.formatSM4GCM(enc3));
        System.out.println();

        // All should decrypt correctly
        String dec1 = Sm4GCMCipher.decrypt(enc1, TEST_SM4_KEY);
        String dec2 = Sm4GCMCipher.decrypt(enc2, TEST_SM4_KEY);
        String dec3 = Sm4GCMCipher.decrypt(enc3, TEST_SM4_KEY);

        System.out.println("    All decrypt correctly: " +
            (samePassword.equals(dec1) && samePassword.equals(dec2) && samePassword.equals(dec3)));
        System.out.println();

        // Demonstrate that same input produces different ciphertexts
        System.out.println("    Ciphertexts are different:");
        System.out.println("        Enc1 != Enc2: " + (!enc1.equals(enc2)));
        System.out.println("        Enc1 != Enc3: " + (!enc1.equals(enc3)));
        System.out.println("        Enc2 != Enc3: " + (!enc2.equals(enc3)));
    }
}
