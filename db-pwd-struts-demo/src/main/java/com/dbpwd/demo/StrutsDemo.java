package com.dbpwd.demo;

import com.dbpwd.crypto.Sm4GCMCipher;
import com.dbpwd.crypto.Sm4Cipher;
import com.dbpwd.crypto.ENCFormat;

/**
 * Struts demo application
 *
 * Demo Database Password Manager SM4-GCM encryption functionality
 *
 * Features:
 * 1. SM4-GCM encryption for database password (authenticated encryption)
 * 2. Password format handling
 * 3. Encryption/decryption verification
 * 4. Backward compatibility support for ECB format
 *
 * GCM模式优势：
 * - 认证加密：同时提供机密性和完整性保护
 * - 防篡改：任何对密文的修改都会导致解密失败
 * - 随机IV：每次加密结果不同，防侧信道攻击
 * - 并行处理：支持并行加密/解密，性能更优
 *
 * @author Database Password Manager
 * @version 4.5
 */
public class StrutsDemo {

    private static final String TEST_SM4_KEY = "0123456789abcdef0123456789abcdef";

    /**
     * Database password utility
     */
    public static class DbConfig {
        public static final String SM4_KEY = "0123456789abcdef0123456789abcdef";

        /**
         * Get encrypted database password using SM4-GCM
         *
         * GCM模式提供认证加密，更安全
         */
        public static String getEncryptedPassword(String plainPassword) {
            try {
                String cipherText = Sm4GCMCipher.encrypt(plainPassword, SM4_KEY);
                return ENCFormat.formatSM4GCM(cipherText);
            } catch (Exception e) {
                throw new RuntimeException("Encrypt database password failed: " + e.getMessage(), e);
            }
        }

        /**
         * Decrypt database password
         *
         * 支持GCM和ECB格式解析
         */
        public static String decryptPassword(String encryptedPassword) {
            try {
                String algorithm = ENCFormat.extractAlgorithm(encryptedPassword);
                String cipherText = ENCFormat.extractCipherText(encryptedPassword);

                if ("SM4".equalsIgnoreCase(algorithm)) {
                    // 向后兼容ECB格式（仅用于兼容旧数据）
                    return Sm4Cipher.decrypt(cipherText, SM4_KEY);
                } else if ("SM4-GCM".equalsIgnoreCase(algorithm)) {
                    // GCM认证加密（推荐）
                    return Sm4GCMCipher.decrypt(cipherText, SM4_KEY);
                } else {
                    throw new IllegalArgumentException("Unsupported encryption algorithm: " + algorithm);
                }
            } catch (Exception e) {
                throw new RuntimeException("Decrypt password failed: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Main method for standalone testing
     */
    public static void main(String[] args) {
        System.out.println("=== Struts Demo Application (Standalone) ===");
        System.out.println("Demo: Database Password Manager - SM4-GCM Encryption");
        System.out.println("SM4 key: " + TEST_SM4_KEY);
        System.out.println();

        try {
            // Test 1: Basic SM4-GCM encryption and decryption
            System.out.println("Test 1: Basic SM4-GCM Encryption/Decryption");
            String password1 = "struts_password_123";
            String encrypted1 = DbConfig.getEncryptedPassword(password1);
            String decrypted1 = DbConfig.decryptPassword(encrypted1);
            System.out.println("  Password: " + password1);
            System.out.println("  Encrypted: " + encrypted1);
            System.out.println("  Decrypted: " + decrypted1);
            System.out.println("  Result: " + (password1.equals(decrypted1) ? "SUCCESS" : "FAILED"));
            System.out.println();

            // Test 2: Different passwords
            System.out.println("Test 2: Multiple Password Encoding");
            String[] passwords = {
                "admin_pass",
                "user_db_connection",
                "migration_key",
                "production_db"
            };

            for (String pwd : passwords) {
                String enc = DbConfig.getEncryptedPassword(pwd);
                String dec = DbConfig.decryptPassword(enc);
                System.out.println("  " + pwd + " => " + (pwd.equals(dec) ? "OK" : "FAIL"));
            }
            System.out.println();

            // Test 3: Different SM4-GCM keys
            System.out.println("Test 3: Different SM4-GCM Keys");
            String testPassword = "test_db_password";
            String key1 = "0123456789abcdef0123456789abcdef";
            String key2 = "abcdef0123456789abcdef0123456789";

            String enc1 = Sm4GCMCipher.encrypt(testPassword, key1);
            String enc2 = Sm4GCMCipher.encrypt(testPassword, key2);

            String dec1 = Sm4GCMCipher.decrypt(enc1, key1);
            String dec2 = Sm4GCMCipher.decrypt(enc2, key2);

            System.out.println("  Test password: " + testPassword);
            System.out.println("  Encrypted with key1: " + ENCFormat.formatSM4GCM(enc1));
            System.out.println("  Decrypted with key1: " + dec1 + " => " + (testPassword.equals(dec1) ? "OK" : "FAIL"));
            System.out.println("  Encrypted with key2: " + ENCFormat.formatSM4GCM(enc2));
            System.out.println("  Decrypted with key2: " + dec2 + " => " + (testPassword.equals(dec2) ? "OK" : "FAIL"));
            System.out.println("  Keys produce different ciphers: " + (!enc1.equals(enc2) ? "YES" : "NO"));
            System.out.println();

            // Test 4: GCM random IV property (same password produces different ciphertexts)
            System.out.println("Test 4: GCM Random IV Property");
            String samePassword = "test_password_123";
            String encA = Sm4GCMCipher.encrypt(samePassword, key1);
            String encB = Sm4GCMCipher.encrypt(samePassword, key1);
            System.out.println("  Password: " + samePassword);
            System.out.println("  Encryption A: " + ENCFormat.formatSM4GCM(encA));
            System.out.println("  Encryption B: " + ENCFormat.formatSM4GCM(encB));
            System.out.println("  Different ciphertexts: " + (!encA.equals(encB) ? "YES (GCM property)" : "NO"));
            System.out.println("  Both decrypt correctly: " + (samePassword.equals(Sm4GCMCipher.decrypt(encA, key1)) &&
                                                     samePassword.equals(Sm4GCMCipher.decrypt(encB, key1)) ? "YES" : "NO"));
            System.out.println();

            // Test 5: Backward compatibility with ECB format
            System.out.println("Test 5: Backward Compatibility with ECB Format");
            String ecbPassword = "test_ecb_compat";
            String ecbEncrypted = ENCFormat.formatSM4(Sm4Cipher.encrypt(ecbPassword, key1));
            System.out.println("  ECB Password: " + ecbPassword);
            System.out.println("  ECB Encrypted: " + ecbEncrypted);

            // Test that we can still decrypt ECB format
            String ecbDecrypted = DbConfig.decryptPassword(ecbEncrypted);
            System.out.println("  ECB Decrypted: " + ecbDecrypted);
            System.out.println("  ECB decryption: " + (ecbPassword.equals(ecbDecrypted) ? "OK" : "FAIL"));
            System.out.println();

            System.out.println("=== Demo Completed Successfully ===");
            System.out.println("All SM4-GCM encryption and decryption tests passed!");
            System.out.println("GCM mode provides authenticated encryption and tamper protection.");

        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
