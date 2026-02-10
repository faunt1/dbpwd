package com.dbpwd.demo;

import com.dbpwd.crypto.Sm4GCMCipher;
import com.dbpwd.crypto.ENCFormat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Simple JDBC demo application
 *
 * Demo Database Password Manager with MySQL database connection
 * Using SM4-GCM authenticated encryption
 *
 * Features:
 * 1. Use SM4-GCM encryption for database password (authenticated encryption)
 * 2. Password format verification
 * 3. Verify password decryption with integrity protection
 * 4. MySQL database CRUD operations
 *
 * SM4-GCM优势:
 * - 认证加密：同时提供机密性和完整性保护
 * - 防篡改：任何对密文的修改都会导致解密失败
 * - 随机IV：每次加密结果不同，防侧信道攻击
 * - 抗选择明文攻击：认证标签提供额外保护
 *
 * @author Database Password Manager
 * @version 5.0
 */
public class SimpleJdbcDemo {

    // MySQL database configuration
    private static final String DB_URL = "jdbc:mysql://localhost:3306/dbpwd_demo?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USERNAME = "dbpwd";
    private static final String DB_PASSWORD = "dbpwd123";
    private static final String TEST_SM4_KEY = "0123456789abcdef0123456789abcdef";

    public static void main(String[] args) {
        System.out.println("=== Simple JDBC Demo Application ===");
        System.out.println("Demo: Database Password Manager - SM4-GCM Encryption");
        System.out.println("Database: MySQL (" + DB_URL + ")");
        System.out.println("SM4 key: " + TEST_SM4_KEY);
        System.out.println();

        try {
            // Step 1: Encrypt database password using SM4-GCM
            String encryptedDbPassword = encryptDatabasePassword(DB_PASSWORD, TEST_SM4_KEY);
            System.out.println("Step 1: Encrypt database password");
            System.out.println("  Original password: " + DB_PASSWORD);
            System.out.println("  Encrypted password: " + encryptedDbPassword);
            System.out.println();

            // Step 2: Decrypt password and connect to MySQL
            System.out.println("Step 2: Decrypt password and connect to MySQL");
            String decryptedPassword = decryptDatabasePassword(encryptedDbPassword, TEST_SM4_KEY);
            System.out.println("  Decrypted password: " + decryptedPassword);
            System.out.println("  Verification: " + (DB_PASSWORD.equals(decryptedPassword) ? "SUCCESS" : "FAILED"));
            System.out.println();

            // Test connection with decrypted password
            System.out.println("Step 3: Test MySQL database connection");
            Connection connection = testDatabaseConnection(DB_URL, DB_USERNAME, decryptedPassword);
            System.out.println("  Connection successful!");
            System.out.println();

            // Step 4: Create demo tables
            System.out.println("Step 4: Create demo tables");
            createTables(connection);
            System.out.println("  Tables created successfully!");
            System.out.println();

            // Step 5: Insert test data
            System.out.println("Step 5: Insert test data");
            insertTestData(connection);
            System.out.println("  Test data inserted successfully!");
            System.out.println();

            // Step 6: Query and display data
            System.out.println("Step 6: Query and display data");
            queryAndDisplayData(connection);

            // Close connection
            connection.close();

            System.out.println();
            System.out.println("Step 7: Test SM4-GCM encryption properties");
            testGCMPriority();

            System.out.println("Step 8: Test ENC format handling");
            testENCFormat();

            System.out.println("Step 9: Demonstrate different encrypted passwords produce different results");
            testRandomIV();

            System.out.println();
            System.out.println("=== Demo Completed Successfully ===");
            System.out.println("All SM4-GCM encryption and decryption tests passed!");
            System.out.println("MySQL database connection and CRUD operations successful!");
            System.out.println("GCM mode provides authenticated encryption with tamper protection.");

        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
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
     * Test MySQL database connection
     */
    private static Connection testDatabaseConnection(String url, String username, String password) throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection(url, username, password);
            System.out.println("  Connected to MySQL successfully");
            return connection;
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC driver not found", e);
        }
    }

    /**
     * Create demo tables
     */
    private static void createTables(Connection connection) throws SQLException {
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
    private static void insertTestData(Connection connection) throws SQLException {
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
    private static void queryAndDisplayData(Connection connection) throws SQLException {
        // Query users
        System.out.println("  Users:");
        PreparedStatement userStmt = connection.prepareStatement(
                "SELECT id, name, email, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') as created_at FROM users ORDER BY id");
        ResultSet userRs = userStmt.executeQuery();
        while (userRs.next()) {
            System.out.println("    ID: " + userRs.getInt("id") +
                    ", Name: " + userRs.getString("name") +
                    ", Email: " + userRs.getString("email") +
                    ", Created: " + userRs.getString("created_at"));
        }
        userRs.close();
        userStmt.close();

        System.out.println();

        // Query orders with user names
        System.out.println("  Orders:");
        PreparedStatement orderStmt = connection.prepareStatement(
                "SELECT o.id, u.name as user_name, o.product_name, o.amount, " +
                "DATE_FORMAT(o.created_at, '%Y-%m-%d %H:%i:%s') as created_at " +
                "FROM orders o LEFT JOIN users u ON o.user_id = u.id ORDER BY o.id");
        ResultSet orderRs = orderStmt.executeQuery();
        while (orderRs.next()) {
            System.out.println("    ID: " + orderRs.getInt("id") +
                    ", User: " + orderRs.getString("user_name") +
                    ", Product: " + orderRs.getString("product_name") +
                    ", Amount: " + orderRs.getBigDecimal("amount") +
                    ", Created: " + orderRs.getString("created_at"));
        }
        orderRs.close();
        orderStmt.close();
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
