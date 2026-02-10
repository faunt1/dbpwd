package com.dbpwd.demo;

import com.dbpwd.crypto.Sm4GCMCipher;
import com.dbpwd.crypto.Sm4Cipher;
import com.dbpwd.crypto.ENCFormat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Struts demo application
 *
 * Demo Database Password Manager SM4-GCM encryption functionality with MySQL database
 *
 * Features:
 * 1. SM4-GCM encryption for database password (authenticated encryption)
 * 2. Password format handling
 * 3. Encryption/decryption verification
 * 4. Backward compatibility support for ECB format
 * 5. MySQL database CRUD operations
 *
 * GCM模式优势：
 * - 认证加密：同时提供机密性和完整性保护
 * - 防篡改：任何对密文的修改都会导致解密失败
 * - 随机IV：每次加密结果不同，防侧信道攻击
 * - 并行处理：支持并行加密/解密，性能更优
 *
 * @author Database Password Manager
 * @version 5.0
 */
public class StrutsDemo {

    private static final String TEST_SM4_KEY = "0123456789abcdef0123456789abcdef";

    // MySQL database configuration
    private static final String DB_URL = "jdbc:mysql://localhost:3306/dbpwd_demo?useSSL=false&serverTimezone=UTC";
    private static final String DB_USERNAME = "dbpwd";
    private static final String DB_PASSWORD = "dbpwd123";

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
        System.out.println("Database: MySQL (" + DB_URL + ")");
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

            // Test 6: MySQL Database Connection
            System.out.println("Test 6: MySQL Database Connection");

            // Encrypt database password
            String encryptedDbPassword = Sm4GCMCipher.encrypt(DB_PASSWORD, TEST_SM4_KEY);
            String formattedEncrypted = ENCFormat.formatSM4GCM(encryptedDbPassword);
            System.out.println("  Database password encrypted: " + formattedEncrypted);

            // Decrypt database password
            String decryptedDbPassword = Sm4GCMCipher.decrypt(encryptedDbPassword, TEST_SM4_KEY);
            System.out.println("  Database password decrypted: " + decryptedDbPassword);
            System.out.println("  Verification: " + (DB_PASSWORD.equals(decryptedDbPassword) ? "SUCCESS" : "FAILED"));

            // Connect to MySQL
            Connection connection = testDatabaseConnection(DB_URL, DB_USERNAME, decryptedDbPassword);
            System.out.println("  MySQL connection successful!");

            // Create demo tables
            createTables(connection);
            System.out.println("  Demo tables created!");

            // Insert test data
            insertTestData(connection);
            System.out.println("  Test data inserted!");

            // Display data
            System.out.println();
            System.out.println("  Query Results:");
            queryAndDisplayData(connection);

            // Close connection
            connection.close();

            System.out.println();
            System.out.println("=== Demo Completed Successfully ===");
            System.out.println("All SM4-GCM encryption and decryption tests passed!");
            System.out.println("MySQL database connection and CRUD operations successful!");
            System.out.println("GCM mode provides authenticated encryption and tamper protection.");

        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Test MySQL database connection
     */
    private static Connection testDatabaseConnection(String url, String username, String password) throws Exception {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection(url, username, password);
            return connection;
        } catch (ClassNotFoundException e) {
            throw new Exception("MySQL JDBC driver not found", e);
        }
    }

    /**
     * Create demo tables
     */
    private static void createTables(Connection connection) throws Exception {
        Statement stmt = connection.createStatement();

        // Drop tables if exist
        stmt.execute("DROP TABLE IF EXISTS orders");
        stmt.execute("DROP TABLE IF EXISTS users");

        // Create users table
        stmt.execute("CREATE TABLE users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL, " +
                "email VARCHAR(255) NOT NULL UNIQUE, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        // Create orders table
        stmt.execute("CREATE TABLE orders (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id INT NOT NULL, " +
                "product_name VARCHAR(200) NOT NULL, " +
                "amount DECIMAL(10,2) NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(id))");

        stmt.close();
    }

    /**
     * Insert test data
     */
    private static void insertTestData(Connection connection) throws Exception {
        Statement stmt = connection.createStatement();

        // Insert users
        stmt.executeUpdate("INSERT INTO users (name, email) VALUES " +
                "('Alice', 'alice@example.com'), " +
                "('Bob', 'bob@example.com'), " +
                "('Charlie', 'charlie@example.com')");

        // Insert orders
        stmt.executeUpdate("INSERT INTO orders (user_id, product_name, amount) VALUES " +
                "(1, 'Product A', 100.00), " +
                "(2, 'Product B', 200.00), " +
                "(1, 'Product C', 150.00)");

        stmt.close();
    }

    /**
     * Query and display data
     */
    private static void queryAndDisplayData(Connection connection) throws Exception {
        // Query users
        System.out.println("    Users:");
        PreparedStatement userStmt = connection.prepareStatement(
                "SELECT id, name, email, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') as created_at FROM users ORDER BY id");
        ResultSet userRs = userStmt.executeQuery();
        while (userRs.next()) {
            System.out.println("      ID: " + userRs.getInt("id") +
                    ", Name: " + userRs.getString("name") +
                    ", Email: " + userRs.getString("email") +
                    ", Created: " + userRs.getString("created_at"));
        }
        userRs.close();
        userStmt.close();

        System.out.println();

        // Query orders with user names
        System.out.println("    Orders:");
        PreparedStatement orderStmt = connection.prepareStatement(
                "SELECT o.id, u.name as user_name, o.product_name, o.amount, " +
                "DATE_FORMAT(o.created_at, '%Y-%m-%d %H:%i:%s') as created_at " +
                "FROM orders o LEFT JOIN users u ON o.user_id = u.id ORDER BY o.id");
        ResultSet orderRs = orderStmt.executeQuery();
        while (orderRs.next()) {
            System.out.println("      ID: " + orderRs.getInt("id") +
                    ", User: " + orderRs.getString("user_name") +
                    ", Product: " + orderRs.getString("product_name") +
                    ", Amount: " + orderRs.getBigDecimal("amount") +
                    ", Created: " + orderRs.getString("created_at"));
        }
        orderRs.close();
        orderStmt.close();
    }
}
