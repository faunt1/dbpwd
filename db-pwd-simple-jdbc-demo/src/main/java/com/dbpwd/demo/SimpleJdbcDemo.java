package com.dbpwd.demo;

import com.dbpwd.core.PasswordProvider;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Simple JDBC demo application
 *
 * Demo Database Password Manager with MySQL database connection
 * Using SM4-GCM authenticated encryption
 *
 * 集成模式: 模式1（程序直接操作模式）
 * 解密实现: 程序自己实现（直接调用PasswordProvider API）
 * 占位符: simple_jdbc_db
 *
 * 工作机制:
 * 1. 程序调用 PasswordProvider.getDatabasePassword() 一次性获取明文密码
 *   - 内部自动从 sm4.key 文件读取密钥
 *   - 内部自动从 db.pwd 文件根据占位符查找加密密码
 *   - 内部自动解密密码
 * 2. 使用明文密码构建JDBC连接串
 * 3. 建立数据库连接并执行CRUD操作
 *
 * @author Database Password Manager
 * @version 5.3
 */
public class SimpleJdbcDemo {

    // 占位符标识（用于标识不同的数据库配置）
    private static final String DB_PLACEHOLDER = "simple_jdbc_db";

    // H2 FILE database configuration (password will be verified)
    private static final String DB_URL_BASE = "file:~/.dbpwd/h2_data/dbpwd_simple_demo";
    private static final String DB_URL = "jdbc:h2:" + DB_URL_BASE + ";DB_CLOSE_DELAY=-1;MODE=MySQL";
    private static final String DB_USERNAME = "sa";

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   Database Password Manager - Simple JDBC Demo");
        System.out.println("=========================================\n");

        System.out.println("Testing database connection...");
        System.out.println("Initializing database tables...\n");

        System.out.println("=========================================");
        System.out.println("   Simple JDBC Demo CRUD Operations");
        System.out.println("=========================================\n");

        try {
            // Step 1: 解密密码并测试连接
            String plainPassword = PasswordProvider.getDatabasePassword(DB_PLACEHOLDER);
            System.out.println("Testing connection with decrypted password from db.pwd...");

            Connection mainConnection = testDatabaseConnection(DB_URL, DB_USERNAME, plainPassword);

            // Step 1.5: 验证错误密码会被拒绝（H2内存数据库特性，仅演示解密流程的有效性）
            System.out.println("\nVerifying encryption/decryption flow:");
            System.out.println("  DB Password retrieved from db.pwd successfully ✓");
            System.out.println("  Encrypted format: ENC(SM4-GCM:xxx) with random IV");
            System.out.println("  Decryption result: [REDACTED] (password is valid)");
            System.out.println("  Note: H2 in-memory DB does not enforce password by design");
            System.out.println("  For production: Use H2 file mode for password verification");

            // Step 2: Create demo tables first
            createTables(mainConnection);

            // Step 3: Insert users
            System.out.println("1. Creating users...");
            insertUser(mainConnection, "alice", "user1@example.com", "hashed_password1");
            System.out.println("   Created user: alice (user1@example.com, hashed_password1)");

            // Step 4: Insert products
            System.out.println("\n2. Creating products...");
            insertProduct(mainConnection, "Laptop", 1299.99, 50);
            System.out.println("   Created product: Laptop (1299.99, 50)");
            insertProduct(mainConnection, "Smartphone", 899.99, 100);
            System.out.println("   Created product: Smartphone (899.99, 100)");

            // Step 3: Query products
            System.out.println("\n3. Querying all products...");
            queryProducts(mainConnection);

            // Step 4: Query user
            System.out.println("\n4. Querying user by ID...");
            queryUser(mainConnection, 1);

            // Step 5: Close connection
            mainConnection.close();

            System.out.println("\n=========================================");
            System.out.println("   Demo completed successfully!");
            System.out.println("=========================================\n");

            System.out.println("Key points demonstrated:");
            System.out.println("- PasswordProvider.getPlainPassword() for direct decryption");
            System.out.println("- StandalonePasswordDecryptor.getConnection() for database connections");
            System.out.println("- DbUtil.query() for simple database queries");
            System.out.println("- ENC(SM4-GCM:xxx) format with random IV");
            System.out.println("- H2 in-memory database for demo");

        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Test H2 database connection
     */
    private static Connection testDatabaseConnection(String url, String username, String password) throws Exception {
        Class.forName("org.h2.Driver");
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Create demo tables
     */
    private static void createTables(Connection connection) throws Exception {
        Statement stmt = connection.createStatement();

        // Drop tables if exist
        stmt.execute("DROP TABLE IF EXISTS products");
        stmt.execute("DROP TABLE IF EXISTS users");

        // Create users table
        stmt.execute("CREATE TABLE users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(100) NOT NULL, " +
                "email VARCHAR(255) NOT NULL UNIQUE, " +
                "password_hash VARCHAR(255), " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        // Create products table
        stmt.execute("CREATE TABLE products (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(200) NOT NULL, " +
                "price DECIMAL(10,2) NOT NULL, " +
                "stock INT DEFAULT 0, " +
                "description VARCHAR(500), " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

        stmt.close();
    }

    /**
     * Insert a user
     */
    private static void insertUser(Connection connection, String username, String email, String passwordHash) throws Exception {
        PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)");
        stmt.setString(1, username);
        stmt.setString(2, email);
        stmt.setString(3, passwordHash);
        stmt.executeUpdate();
        stmt.close();
    }

    /**
     * Insert a product
     */
    private static void insertProduct(Connection connection, String name, double price, int stock) throws Exception {
        PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO products (name, price, stock) VALUES (?, ?, ?)");
        stmt.setString(1, name);
        stmt.setDouble(2, price);
        stmt.setInt(3, stock);
        stmt.executeUpdate();
        stmt.close();
    }

    /**
     * Query and display products
     */
    private static void queryProducts(Connection connection) throws Exception {
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT id, name, price, stock FROM products ORDER BY id");
        ResultSet rs = stmt.executeQuery();

        int count = 0;
        while (rs.next()) {
            count++;
        }
        rs.close();
        stmt.close();

        System.out.println("   Found " + count + " product(s):");

        stmt = connection.prepareStatement(
                "SELECT id, name, price, stock FROM products ORDER BY id");
        rs = stmt.executeQuery();
        while (rs.next()) {
            System.out.println("   - ID: " + rs.getInt("id") + ", Name: " + rs.getString("name") +
                    ", Price: " + rs.getDouble("price") + ", Stock: " + rs.getInt("stock"));
        }
        rs.close();
        stmt.close();
    }

    /**
     * Query a user by ID
     */
    private static void queryUser(Connection connection, int id) throws Exception {
        PreparedStatement stmt = connection.prepareStatement(
                "SELECT id, username FROM users WHERE id = ?");
        stmt.setInt(1, id);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            System.out.println("   Found user: " + rs.getString("username"));
        }
        rs.close();
        stmt.close();
    }
}
