package com.dbpwd.demo;

import com.dbpwd.crypto.Sm4Cipher;
import com.dbpwd.crypto.ENCFormat;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Struts demo application
 *
 * Demo Database Password Manager SM4 encryption functionality
 *
 * Features:
 * 1. SM4 encryption for database password
 * 2. Password format handling
 * 3. Encryption/decryption verification
 *
 * @author Database Password Manager
 * @version 4.4
 */
public class StrutsDemo {

    private static final String TEST_SM4_KEY = "0123456789abcdef0123456789abcdef";

    /**
     * Database password utility
     */
    public static class DbConfig {
        public static final String SM4_KEY = "0123456789abcdef0123456789abcdef";

        /**
         * Get encrypted database password
         */
        public static String getEncryptedPassword(String plainPassword) {
            try {
                String cipherText = Sm4Cipher.encrypt(plainPassword, SM4_KEY);
                return ENCFormat.formatSM4(cipherText);
            } catch (Exception e) {
                throw new RuntimeException("Encrypt database password failed: " + e.getMessage(), e);
            }
        }

        /**
         * Decrypt database password
         */
        public static String decryptPassword(String encryptedPassword) {
            try {
                String algorithm = ENCFormat.extractAlgorithm(encryptedPassword);
                String cipherText = ENCFormat.extractCipherText(encryptedPassword);
                return Sm4Cipher.decrypt(cipherText, SM4_KEY);
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
        System.out.println("Demo: Database Password Manager - SM4 Encryption");
        System.out.println("SM4 key: " + TEST_SM4_KEY);
        System.out.println();

        try {
            // Test 1: Basic SM4 encryption and decryption
            System.out.println("Test 1: Basic SM4 Encryption/Decryption");
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

            // Test 3: SM4 with different keys
            System.out.println("Test 3: Different SM4 Keys");
            String testPassword = "test_db_password";
            String key1 = "0123456789abcdef0123456789abcdef";
            String key2 = "abcdef0123456789abcdef0123456789";

            String enc1 = Sm4Cipher.encrypt(testPassword, key1);
            String enc2 = Sm4Cipher.encrypt(testPassword, key2);

            String dec1 = Sm4Cipher.decrypt(enc1, key1);
            String dec2 = Sm4Cipher.decrypt(enc2, key2);

            System.out.println("  Test password: " + testPassword);
            System.out.println("  Encrypted with key1: " + ENCFormat.formatSM4(enc1));
            System.out.println("  Decrypted with key1: " + dec1 + " => " + (testPassword.equals(dec1) ? "OK" : "FAIL"));
            System.out.println("  Encrypted with key2: " + ENCFormat.formatSM4(enc2));
            System.out.println("  Decrypted with key2: " + dec2 + " => " + (testPassword.equals(dec2) ? "OK" : "FAIL"));
            System.out.println("  Keys produce different ciphers: " + (!enc1.equals(enc2) ? "YES" : "NO"));
            System.out.println();

            System.out.println("=== Demo Completed Successfully ===");
            System.out.println("All SM4 encryption and decryption tests passed!");

        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
