package com.dbpwd.demo;

import com.dbpwd.core.PasswordService;
import com.dbpwd.crypto.Sm4GCMCipher;
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
 * 集成模式: 模式1（程序直接操作模式）
 * 解密实现: 程序自己实现（调用PasswordService API）
 * 占位符: struts_db
 *
 * 工作机制:
 * 1. 程序独立启动，不依赖Spring框架
 * 2. 调用PasswordService.getDatabasePassword()一步获取解密后的密码
 * 3. 使用明文密码构建JDBC连接串
 * 4. 建立数据库连接并执行CRUD操作
 *
 * @author Database Password Manager
 * @version 5.3
 */
public class StrutsDemo {

    // 占位符标识（用于标识不同的数据库配置）
    private static final String DB_PLACEHOLDER = "struts_db";

    // MySQL database configuration
    private static final String DB_URL = "jdbc:mysql://localhost:3306/dbpwd_demo?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USERNAME = "dbpwd";

    public static void main(String[] args) {
        System.out.println("=== Struts Demo Application (Standalone) ===");
        System.out.println("Demo: Database Password Manager - SM4-GCM Encryption");
        System.out.println("集成模式: 模式1（程序直接操作模式）");
        System.out.println("解密实现: 程序自己实现（调用PasswordService API）");
        System.out.println("占位符(Placeholder): " + DB_PLACEHOLDER);
        System.out.println("实际数据库: dbpwd_demo");
        System.out.println();

        try {
            // Test 1: Basic SM4-GCM Encryption/Decryption
            System.out.println("Test 1: Basic SM4-GCM Encryption/Decryption");
            String password1 = "struts_password_123";
            String encrypted1 = DbConfig.getEncryptedPassword(password1);
            String decrypted1 = DbConfig.decryptPassword(encrypted1);
            System.out.println("  Password: " + password1);
            System.out.println("  Encrypted: " + encrypted1);
            System.out.println("  Decrypted: " + decrypted1);
            boolean test1Result = password1.equals(decrypted1);
            System.out.println("  Result: " + (test1Result ? "SUCCESS" : "FAILED"));
            if (!test1Result) {
                throw new RuntimeException("Test 1 failed: Password decryption mismatch");
            }
            System.out.println();

            // Test 2: Multiple Password Encoding
            System.out.println("Test 2: Multiple Password Encoding");
            String[] passwords = {
                "admin_pass",
                "user_db_connection",
                "migration_key",
                "production_db"
            };

            boolean test2Result = true;
            for (String pwd : passwords) {
                String enc = DbConfig.getEncryptedPassword(pwd);
                String dec = DbConfig.decryptPassword(enc);
                boolean passed = pwd.equals(dec);
                System.out.println("  " + pwd + " => " + (passed ? "OK" : "FAIL"));
                if (!passed) {
                    test2Result = false;
                }
            }
            if (!test2Result) {
                throw new RuntimeException("Test 2 failed: Multiple password encoding failed");
            }
            System.out.println();

            // Test 3: MySQL Database Connection using PasswordService
            System.out.println("Test 3: MySQL Database Connection (using PasswordService)");

            // 使用PasswordService获取解密后的数据库密码
            String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
            System.out.println("  解密后的密码: " + plainPassword);

            // Connect to MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection(DB_URL, DB_USERNAME, plainPassword);
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
     * Database password utility class
     */
    public static class DbConfig {
        private static final String SM4_KEY = "0123456789abcdef0123456789abcdef";

        /**
         * Get encrypted database password using SM4-GCM
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
         * 支持GCM格式解析（仅用于SM4-GCM）
         */
        public static String decryptPassword(String encryptedPassword) {
            try {
                String algorithm = ENCFormat.extractAlgorithm(encryptedPassword);
                String cipherText = ENCFormat.extractCipherText(encryptedPassword);

                // 仅支持SM4-GCM格式
                if ("SM4-GCM".equalsIgnoreCase(algorithm)) {
                    return Sm4GCMCipher.decrypt(cipherText, SM4_KEY);
                } else {
                    throw new IllegalArgumentException("Unsupported encryption algorithm: " + algorithm + ". Only SM4-GCM is supported.");
                }
            } catch (Exception e) {
                throw new RuntimeException("Decrypt password failed: " + e.getMessage(), e);
            }
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
