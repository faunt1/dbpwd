package com.dbpwd.demo;

import java.io.File;
import java.sql.*;

/**
 * Demo database connection example
 *
 * Demonstrate database connection with SM4-encrypted password
 * Using H2 in-memory database for demo purposes
 *
 * @author Database Password Manager
 * @version 4.4
 */
public class SqlCipherDatabaseSetup {

    private static final String DB_URL = "jdbc:h2:mem:dbpwd_demo;DB_CLOSE_DELAY=-1";
    private static final String DB_USERNAME = "sa";
    private static final String DB_PASSWORD = ""; // H2 default: empty password

    /**
     * Initialize demo database
     */
    public static void setupDemoDatabase() {
        try {
            // Connect to H2 in-memory database
            Class.forName("org.h2.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);

            System.out.println("Demo database connected successfully: " + DB_URL);

            // Execute initialization script
            initDatabase(conn);

            conn.close();

            System.out.println("Demo database initialized successfully");

        } catch (Exception e) {
            System.err.println("Failed to initialize demo database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Execute initialization script
     */
    private static void initDatabase(Connection conn) throws Exception {
        Statement stmt = conn.createStatement();

        // Create users table
        stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                      "id IDENTITY PRIMARY KEY, " +
                      "name VARCHAR(100) NOT NULL, " +
                      "email VARCHAR(255) NOT NULL UNIQUE, " +
                      "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                      "updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");

        // Create orders table
        stmt.execute("CREATE TABLE IF NOT EXISTS orders (" +
                      "id IDENTITY PRIMARY KEY, " +
                      "user_id BIGINT NOT NULL, " +
                      "product_name VARCHAR(200) NOT NULL, " +
                      "amount DECIMAL(10,2) NOT NULL, " +
                      "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                      "FOREIGN KEY (user_id) REFERENCES users(id))");

        // Insert demo data
        stmt.execute("INSERT INTO users (name, email) VALUES " +
                      "('Alice', 'alice@example.com'), " +
                      "('Bob', 'bob@example.com'), " +
                      "('Charlie', 'charlie@example.com')");

        stmt.execute("INSERT INTO orders (user_id, product_name, amount) VALUES " +
                      "(1, 'Product A', 100.00), " +
                      "(2, 'Product B', 200.00), " +
                      "(1, 'Product C', 150.00)");

        stmt.close();
    }

    /**
     * Get demo database connection
     *
     * @return Database connection
     */
    public static Connection getDemoConnection() throws SQLException {
        try {
            Class.forName("org.h2.Driver");
            return DriverManager.getConnection(DB_URL, DB_USERNAME, DB_PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("H2 JDBC driver not found", e);
        }
    }

    /**
     * Get demo database URL
     *
     * @return Database URL
     */
    public static String getDatabaseUrl() {
        return DB_URL;
    }

    /**
     * Main method for manual database initialization and testing
     */
    public static void main(String[] args) {
        System.out.println("=== Demo Database Initialization (H2 In-Memory) ===");
        setupDemoDatabase();
        System.out.println("\nDemo database URL: " + DB_URL);
        System.out.println("Username: " + DB_USERNAME);

        // Test connection and query data
        try {
            Connection conn = getDemoConnection();
            System.out.println("\nTest connection successful!");

            // Query data
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) as count FROM users");
            if (rs.next()) {
                System.out.println("User count: " + rs.getInt("count"));
            }
            rs.close();
            stmt.close();
            conn.close();

        } catch (SQLException e) {
            System.err.println("Test connection failed: " + e.getMessage());
        }
    }
}
