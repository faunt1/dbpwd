package com.dbpwd.demo;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Simple JDBC demo application - ORIGINAL VERSION (before integration)
 *
 * 原始版本 - 使用明文密码，未集成密码加密功能
 * 此版本用于对比展示集成密码加密功能前后的代码差异
 *
 * 【集成改动点】
 * 1. 添加 import com.dbpwd.core.PasswordService;
 * 2. 将 DB_PASSWORD 改为 DB_PLACEHOLDER
 * 3. 将 PasswordService.getDatabasePassword(DB_PLACEHOLDER) 替换直接使用密码
 *
 * @author Database Password Manager
 * @version 1.0
 */
public class SimpleJdbcDemo {

    // 【原始版本】明文密码 - 集成时需改为占位符
    private static final String DB_PASSWORD = "sa";

    // H2 FILE database configuration
    private static final String DB_URL_BASE = "file:~/.dbpwd/h2_data/dbpwd_simple_demo";
    private static final String DB_URL = "jdbc:h2:" + DB_URL_BASE + ";DB_CLOSE_DELAY=-1;MODE=MySQL";
    private static final String DB_USERNAME = "sa";

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   Simple JDBC Demo - ORIGINAL VERSION");
        System.out.println("   (使用明文密码，未集成密码加密)");
        System.out.println("=========================================\n");

        System.out.println("Testing database connection...");
        System.out.println("Initializing database tables...\n");

        System.out.println("=========================================");
        System.out.println("   Simple JDBC Demo CRUD Operations");
        System.out.println("=========================================\n");

        try {
            // 【原始版本】直接使用明文密码
            // 【集成改动】改为: String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
            String plainPassword = DB_PASSWORD;
            System.out.println("Testing connection with plaintext password...");

            Connection mainConnection = testDatabaseConnection(DB_URL, DB_USERNAME, plainPassword);

            // Create demo tables first
            createTables(mainConnection);

            // Insert users
            System.out.println("1. Creating users...");
            insertUser(mainConnection, "alice", "user1@example.com", "hashed_password1");
            System.out.println("   Created user: alice (user1@example.com, hashed_password1)");

            // Insert products
            System.out.println("\n2. Creating products...");
            insertProduct(mainConnection, "Laptop", 1299.99, 50);
            System.out.println("   Created product: Laptop (1299.99, 50)");
            insertProduct(mainConnection, "Smartphone", 899.99, 100);
            System.out.println("   Created product: Smartphone (899.99, 100)");

            // Query products
            System.out.println("\n3. Querying all products...");
            queryProducts(mainConnection);

            // Query user
            System.out.println("\n4. Querying user by ID...");
            queryUser(mainConnection, 1);

            // Close connection
            mainConnection.close();

            System.out.println("\n=========================================");
            System.out.println("   Demo completed successfully!");
            System.out.println("=========================================\n");

            System.out.println("Security Issues in ORIGINAL version:");
            System.out.println("- Password is hardcoded in source code");
            System.out.println("- Password visible in configuration");

        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static Connection testDatabaseConnection(String url, String username, String password) throws Exception {
        Class.forName("org.h2.Driver");
        return DriverManager.getConnection(url, username, password);
    }

    private static void createTables(Connection connection) throws Exception {
        Statement stmt = connection.createStatement();

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
    }

    private static void insertUser(Connection connection, String username, String email, String passwordHash) throws Exception {
        PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO users (username, email, password_hash) VALUES (?, ?, ?)");
        stmt.setString(1, username);
        stmt.setString(2, email);
        stmt.setString(3, passwordHash);
        stmt.executeUpdate();
        stmt.close();
    }

    private static void insertProduct(Connection connection, String name, double price, int stock) throws Exception {
        PreparedStatement stmt = connection.prepareStatement(
                "INSERT INTO products (name, price, stock) VALUES (?, ?, ?)");
        stmt.setString(1, name);
        stmt.setDouble(2, price);
        stmt.setInt(3, stock);
        stmt.executeUpdate();
        stmt.close();
    }

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
