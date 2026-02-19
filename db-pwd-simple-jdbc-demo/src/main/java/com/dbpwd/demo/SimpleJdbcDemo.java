package com.dbpwd.demo;

import com.dbpwd.core.PasswordService;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Simple JDBC demo application (集成版本)
 *
 * Demo Database Password Manager with H2 FILE database connection
 * Using SM4-GCM authenticated encryption
 *
 * 集成模式: 模式5（纯JDBC模式）
 * 解密实现: 程序自己实现（直接调用PasswordService API）
 * 占位符: simple_jdbc_db
 *
 * 注意: 数据库表和初始数据由 DatabaseInitializer 预先创建
 *
 * @author Database Password Manager
 * @version 6.0
 */
public class SimpleJdbcDemo {

    private static final String DB_PLACEHOLDER = "simple_jdbc_db";

    private static final String DB_URL_BASE = "file:~/.dbpwd/h2_data/dbpwd_simple_demo";
    private static final String DB_URL = "jdbc:h2:" + DB_URL_BASE + ";IFEXISTS=TRUE;MODE=MySQL";
    private static final String DB_USERNAME = "sa";

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   Database Password Manager - Simple JDBC Demo");
        System.out.println("=========================================\n");

        try {
            System.out.println("Testing database connection...");

            String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
            System.out.println("  Password retrieved from db.pwd successfully");

            Class.forName("org.h2.Driver");
            Connection conn = DriverManager.getConnection(DB_URL, DB_USERNAME, plainPassword);
            System.out.println("  Database connection established\n");

            System.out.println("=========================================");
            System.out.println("   Query Operations Demo");
            System.out.println("=========================================\n");

            queryUsers(conn);
            queryProducts(conn);
            queryOrders(conn);

            conn.close();

            System.out.println("\n=========================================");
            System.out.println("   Demo completed successfully!");
            System.out.println("=========================================\n");

        } catch (Exception e) {
            System.err.println("Demo failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void queryUsers(Connection conn) throws Exception {
        System.out.println("1. Querying users...");
        PreparedStatement stmt = conn.prepareStatement("SELECT id, username, email FROM users ORDER BY id");
        ResultSet rs = stmt.executeQuery();
        int count = 0;
        while (rs.next()) {
            count++;
            System.out.println("   - ID: " + rs.getInt("id") + ", Username: " + rs.getString("username") + ", Email: " + rs.getString("email"));
        }
        rs.close();
        stmt.close();
        System.out.println("   Found " + count + " user(s)\n");
    }

    private static void queryProducts(Connection conn) throws Exception {
        System.out.println("2. Querying products...");
        PreparedStatement stmt = conn.prepareStatement("SELECT id, name, price, stock, category FROM products ORDER BY id");
        ResultSet rs = stmt.executeQuery();
        int count = 0;
        while (rs.next()) {
            count++;
            System.out.println("   - ID: " + rs.getInt("id") + ", Name: " + rs.getString("name") + ", Price: " + rs.getBigDecimal("price") + ", Stock: " + rs.getInt("stock") + ", Category: " + rs.getString("category"));
        }
        rs.close();
        stmt.close();
        System.out.println("   Found " + count + " product(s)\n");
    }

    private static void queryOrders(Connection conn) throws Exception {
        System.out.println("3. Querying orders...");
        PreparedStatement stmt = conn.prepareStatement(
            "SELECT o.id, u.username, o.product_name, o.amount, o.quantity FROM orders o " +
            "LEFT JOIN users u ON o.user_id = u.id ORDER BY o.id");
        ResultSet rs = stmt.executeQuery();
        int count = 0;
        while (rs.next()) {
            count++;
            System.out.println("   - ID: " + rs.getInt("id") + ", User: " + rs.getString("username") + ", Product: " + rs.getString("product_name") + ", Amount: " + rs.getBigDecimal("amount") + ", Qty: " + rs.getInt("quantity"));
        }
        rs.close();
        stmt.close();
        System.out.println("   Found " + count + " order(s)\n");
    }
}
