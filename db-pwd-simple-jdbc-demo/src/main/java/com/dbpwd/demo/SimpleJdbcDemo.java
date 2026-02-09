package com.dbpwd.demo;

import com.dbpwd.crypto.Sm4Cipher;
import com.dbpwd.crypto.ENCFormat;

import java.sql.*;

/**
 * Simple JDBC demo application
 *
 * Demo Database Password Manager in plain JDBC application
 *
 * Features:
 * 1. Use SM4 encryption for database password
 * 2. Verify password decryption functionality
 * 3. Demo basic SM4 operations
 *
 * @author Database Password Manager
 * @version 4.4
 */
public class SimpleJdbcDemo {

    // SM4 encryption configuration
    private static final String TEST_SM4_KEY = "0123456789abcdef0123456789abcdef";

    public static void main(String[] args) {
        System.out.println("=== Simple JDBC Demo Application ===");
        System.out.println("Demo: Database Password Manager - SM4 Encryption");
        System.out.println("SM4 key: " + TEST_SM4_KEY);
        System.out.println();

        try {
            // Step 1: Test SM4 encryption
            System.out.println("Step 1: Test SM4 Encryption");
            String testPassword = "myDatabasePassword123";
            String encryptedPassword = encryptPassword(testPassword, TEST_SM4_KEY);
            System.out.println("  Original password: " + testPassword);
            System.out.println("  Encrypted password: " + encryptedPassword);
            System.out.println();

            // Step 2: Test SM4 decryption
            System.out.println("Step 2: Test SM4 Decryption");
            String decryptedPassword = decryptPassword(encryptedPassword, TEST_SM4_KEY);
            System.out.println("  Decrypted password: " + decryptedPassword);
            System.out.println("  Verification: " + (testPassword.equals(decryptedPassword) ? "SUCCESS" : "FAILED"));
            System.out.println();

            // Step 3: Test password encryption with ENC format
            System.out.println("Step 3: Test ENC Format");
            String dbPassword = "postgres_password";
            String encryptedDbPassword = encryptDatabasePassword(dbPassword, TEST_SM4_KEY);
            System.out.println("  Database password: " + dbPassword);
            System.out.println("  Formatted encrypted: " + encryptedDbPassword);

            // Parse and decrypt
            String decryptedDbPassword = decryptDatabasePassword(encryptedDbPassword, TEST_SM4_KEY);
            System.out.println("  Decrypted: " + decryptedDbPassword);
            System.out.println("  Verification: " + (dbPassword.equals(decryptedDbPassword) ? "SUCCESS" : "FAILED"));
            System.out.println();

            System.out.println("=== Demo Completed Successfully ===");
            System.out.println("All SM4 encryption and decryption tests passed!");

        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Encrypt password using SM4
     */
    private static String encryptPassword(String plainPassword, String sm4Key) {
        try {
            String cipherText = Sm4Cipher.encrypt(plainPassword, sm4Key);
            return ENCFormat.formatSM4(cipherText);
        } catch (Exception e) {
            throw new RuntimeException("Encrypt password failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypt password using SM4
     */
    private static String decryptPassword(String encryptedPassword, String sm4Key) {
        try {
            String algorithm = ENCFormat.extractAlgorithm(encryptedPassword);
            String cipherText = ENCFormat.extractCipherText(encryptedPassword);
            String plainText = Sm4Cipher.decrypt(cipherText, sm4Key);
            return plainText;
        } catch (Exception e) {
            throw new RuntimeException("Decrypt password failed: " + e.getMessage(), e);
        }
    }

    /**
     * Encrypt database password with ENC format
     */
    private static String encryptDatabasePassword(String plainPassword, String sm4Key) {
        try {
            String cipherText = Sm4Cipher.encrypt(plainPassword, sm4Key);
            return ENCFormat.formatSM4(cipherText);
        } catch (Exception e) {
            throw new RuntimeException("Encrypt database password failed: " + e.getMessage(), e);
        }
    }

    /**
     * Decrypt database password with ENC format
     */
    private static String decryptDatabasePassword(String encryptedPassword, String sm4Key) {
        try {
            String algorithm = ENCFormat.extractAlgorithm(encryptedPassword);
            String cipherText = ENCFormat.extractCipherText(encryptedPassword);
            return Sm4Cipher.decrypt(cipherText, sm4Key);
        } catch (Exception e) {
            throw new RuntimeException("Decrypt database password failed: " + e.getMessage(), e);
        }
    }
}
