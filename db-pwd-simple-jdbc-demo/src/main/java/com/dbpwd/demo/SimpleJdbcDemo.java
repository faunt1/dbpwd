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

    // MySQL database configuration
    private static final String DB_URL = "jdbc:mysql://localhost:3306/dbpwd_demo?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USERNAME = "dbpwd";

    public static void main(String[] args) {
        System.out.println("=== Simple JDBC Demo Application ===");
        System.out.println("Demo: Database Password Manager - SM4-GCM Encryption");
        System.out.println("集成模式: 模式1（程序直接操作模式）");
        System.out.println("解密实现: 程序自己实现（调用PasswordProvider API）");
        System.out.println("占位符(Placeholder): " + DB_PLACEHOLDER);
        System.out.println("实际数据库: dbpwd_demo");
        System.out.println();

        try {
            // Step 1: 一次性获取解密后的数据库密码
            System.out.println("Step 1: 获取数据库明文密码");
            String plainPassword = PasswordProvider.getDatabasePassword(DB_PLACEHOLDER);
            System.out.println("  解密后的密码: " + plainPassword);
            System.out.println();

            // Step 2: Test MySQL database connection with decrypted password
            System.out.println("Step 2: 测试MySQL数据库连接");
            Connection connection = testDatabaseConnection(DB_URL, DB_USERNAME, plainPassword);
            System.out.println("  Connected to MySQL successfully");
            System.out.println();

            // Step 3: Create demo tables
            System.out.println("Step 3: 创建数据库表");
            createTables(connection);
            System.out.println("  表创建成功!");
            System.out.println();

            // Step 4: Insert test data
            System.out.println("Step 4: 插入测试数据");
            insertTestData(connection);
            System.out.println("  测试数据插入成功!");
            System.out.println();

            // Step 5: Query and display data
            System.out.println("Step 5: 查询并展示数据");
            queryAndDisplayData(connection);

            // Close connection
            connection.close();

            System.out.println();
            System.out.println("=== Demo Completed Successfully ===");
            System.out.println("PasswordProvider一步调用成功！");
            System.out.println("SM4-GCM认证加密功能正常！");
            System.out.println("MySQL数据库连接成功！");
            System.out.println("数据库CRUD操作正常！");

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
        Class.forName("com.mysql.cj.jdbc.Driver");
        return DriverManager.getConnection(url, username, password);
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
        System.out.println("  用户:");
        PreparedStatement userStmt = connection.prepareStatement(
                "SELECT id, name, email, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') as created_at FROM users ORDER BY id");
        ResultSet userRs = userStmt.executeQuery();
        while (userRs.next()) {
            System.out.println("    ID: " + userRs.getInt("id") +
                    ", 姓名: " + userRs.getString("name") +
                    ", 邮箱: " + userRs.getString("email") +
                    ", 创建时间: " + userRs.getString("created_at"));
        }
        userRs.close();
        userStmt.close();

        System.out.println();

        // Query orders with user names
        System.out.println("  订单:");
        PreparedStatement orderStmt = connection.prepareStatement(
                "SELECT o.id, u.name as user_name, o.product_name, o.amount, " +
                "DATE_FORMAT(o.created_at, '%Y-%m-%d %H:%i:%s') as created_at " +
                "FROM orders o LEFT JOIN users u ON o.user_id = u.id ORDER BY o.id");
        ResultSet orderRs = orderStmt.executeQuery();
        while (orderRs.next()) {
            System.out.println("    ID: " + orderRs.getInt("id") +
                    ", 用户: " + orderRs.getString("user_name") +
                    ", 产品: " + orderRs.getString("product_name") +
                    ", 金额: " + orderRs.getBigDecimal("amount") +
                    ", 创建时间: " + orderRs.getString("created_at"));
        }
        orderRs.close();
        orderStmt.close();
    }
}
