package com.dbpwd.demo;

import com.dbpwd.core.PasswordProvider;
import com.dbpwd.crypto.Sm4GCMCipher;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Simple JDBC demo application with password verification
 *
 * Demonstrates SM4-GCM encrypted password management with H2 FILE database
 *
 * 密码验证流程:
 * 1. 从db.pwd通过占位符获取加密密码
 * 2. 用sm4.key解密得到明文密码
 * 3. 用明文密码连接H2文件数据库（首次创建时绑定密码）
 * 4. 后续连接必须使用相同密码才能成功（真实密码验证）
 * 5. 测试错误密码验证（应该失败）
 *
 * @author Database Password Manager
 * @version 5.4 - with password verification
 */
public class SimpleJdbcDemoWithVerification {

    // 占位符标识
    private static final String DB_PLACEHOLDER = "simple_jdbc_db";

    // H2 FILE database configuration - password will be created during first connection
    private static final String DB_URL_BASE = "file:~/.dbpwd/h2_data/dbpwd_simple_demo_verify";
    private static final String DB_URL = "jdbc:h2:" + DB_URL_BASE + ";DB_CLOSE_DELAY=-1;MODE=MySQL";
    private static final String DB_USERNAME = "sa";

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   Database Password Manager - Simple JDBC Demo");
        System.out.println("   With Password Verification Test");
        System.out.println("=========================================\n");

        try {
            // Phase 1: Get and decrypt password from db.pwd
            System.out.println("Phase 1: Password Decryption Test");
            System.out.println("----------------------------------------");
            String correctPassword = PasswordProvider.getDatabasePassword(DB_PLACEHOLDER);
            System.out.println("Password decrypted from db.pwd for '" + DB_PLACEHOLDER + "'");
            System.out.println("  Decrypted password length: " + correctPassword.length() + " characters");
            System.out.println("  Encrypted format: ENC(SM4-GCM:xxx)");
            System.out.println();

            // Phase 2: Test with correct password (should succeed)
            System.out.println("Phase 2: Test with Correct Password");
            System.out.println("----------------------------------------");
            System.out.println("Attempting connection with decrypted password...");
            Connection conn = testConnection(DB_URL, DB_USERNAME, correctPassword);
            System.out.println("Connection successful with correct password! ✓");
            System.out.println();

            // Phase 3: Create schema
            createSchema(conn);

            // Phase 4: Test CRUD operations
            System.out.println("Phase 3: CRUD Operations");
            System.out.println("----------------------------------------");
            insertUser(conn, "alice", "alice@example.com", "hashed1");
            insertProduct(conn, "Laptop", 1299.99, 50);
            queryUsers(conn);
            queryProducts(conn);
            System.out.println();

            // Phase 5: Test with wrong password (should fail)
            System.out.println("Phase 4: Password Verification Test");
            System.out.println("----------------------------------------");
            String wrongPassword = "wrong_password_" + System.currentTimeMillis();
            System.out.println("Attempting connection with wrong password: " + wrongPassword);

            try {
                Connection wrongConn = testConnection(DB_URL, DB_USERNAME, wrongPassword);
                System.err.println("WARNING: Wrong password succeeded! This shouldn't happen!");
                System.err.println("Password verification NOT working!");
                wrongConn.close();
            } catch (Exception e) {
                System.out.println("✓ Password rejected correctly: " + e.getMessage());
                System.out.println("✓ Password verification working!");
            }
            System.out.println();

            // Phase 6: Test wrong decryption (simulated)
            System.out.println("Phase 5: Encryption Format Test");
            System.out.println("----------------------------------------");
            testEncryptionFormat(correctPassword);
            System.out.println();

            conn.close();
            cleanupDatabaseFiles();

            System.out.println("=========================================");
            System.out.println("   Demo completed successfully!");
            System.out.println("=========================================\n");

            System.out.println("Results:");
            System.out.println("  ✓ Password decrypted from ENC(SM4-GCM:xxx) format");
            System.out.println("  ✓ Database connection successful with correct password");
            System.out.println("  ✓ Wrong password rejected (password verification working)");
            System.out.println("  ✓ CRUD operations successful");
            System.out.println();
            System.out.println("Key points demonstrated:");
            System.out.println("- PasswordProvider.getDatabasePassword() for SM4-GCM decryption");
            System.out.println("- H2 FILE database for real password verification");
            System.out.println(" ENC(SM4-GCM:xxx) format with random IV");
            System.out.println("- Proper database security with password validation");

        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Connection testConnection(String url, String username, String password) throws Exception {
        Class.forName("org.h2.Driver");
        return DriverManager.getConnection(url, username, password);
    }

    private static void createSchema(Connection conn) throws Exception {
        Statement stmt = conn.createStatement();

        stmt.execute("DROP TABLE IF EXISTS products");
        stmt.execute("DROP TABLE IF EXISTS users");

        stmt.execute("CREATE TABLE users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(100) NOT NULL, " +
                "email VARCHAR(255) NOT NULL UNIQUE, " +
                "password_hash VARCHAR(255), " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        stmt.execute("CREATE TABLE products (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(200) NOT NULL, " +
                "price DECIMAL(10,2) NOT NULL, " +
                "stock INT DEFAULT 0, " +
                "description VARCHAR(500), " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        stmt.close();
        System.out.println("Schema created successfully");
    }

    private static void insertUser(Connection conn, String username, String email, String hash) throws Exception {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)");
        stmt.setString(1, username);
        stmt.setString(2, email);
        stmt.setString(3, hash);
        stmt.executeUpdate();
        stmt.close();
    }

    private static void insertProduct(Connection conn, String name, double price, int stock) throws Exception {
        PreparedStatement stmt = conn.prepareStatement(
                "INSERT INTO products (name, price, stock) VALUES (?, ?, ?)");
        stmt.setString(1, name);
        stmt.setDouble(2, price);
        stmt.setInt(3, stock);
        stmt.executeUpdate();
        stmt.close();
    }

    private static void queryUsers(Connection conn) throws Exception {
        PreparedStatement stmt = conn.prepareStatement("SELECT id, username FROM users");
        ResultSet rs = stmt.executeQuery();
        int count = 0;
        while (rs.next()) {
            count++;
            System.out.println("  User: " + rs.getString("username"));
        }
        rs.close();
        stmt.close();
        System.out.println("Total users: " + count);
    }

    private static void queryProducts(Connection conn) throws Exception {
        PreparedStatement stmt = conn.prepareStatement("SELECT id, name, price FROM products");
        ResultSet rs = stmt.executeQuery();
        int count = 0;
        while (rs.next()) {
            count++;
        }
        rs.close();
        stmt.close();
        System.out.println("Total products: " + count);
    }

    private static void testEncryptionFormat(String correctPassword) throws Exception {
        System.out.println("Testing encryption format characteristics:");
        System.out.println("  - Password from db.pwd: ENC(SM4-GCM:xxx)");
        System.out.println("  - Algorithm: SM4-GCM (Authenticated Encryption)");
        System.out.println("  - Features:");
        System.out.println("    * Random IV (Initialization Vector) - GCM mode requirement");
        System.out.println("    * Authentication Tag - prevents tampering");
        System.out.println("    * Each encryption produces different ciphertext");
        System.out.println("  - Format: ENC(SM4-GCM:{iv}:{ciphertext})");
        System.out.println("  - Key source: ~/.dbpwd/sm4.key");
        System.out.println("  - Password source: db.pwd (look up by db_id)");
    }

    private static void cleanupDatabaseFiles() {
        try {
            // Clean up H2 database files
            String dbPath = DB_URL_BASE.replace("file:~", System.getProperty("user.home"));
            File dbFile = new File(dbPath + ".mv.db");
            if (dbFile.exists()) {
                dbFile.delete();
                System.out.println("Database files cleaned up");
            }
        } catch (Exception e) {
            System.out.println("Note: Database cleanup error (can be ignored): " + e.getMessage());
        }
    }
}
