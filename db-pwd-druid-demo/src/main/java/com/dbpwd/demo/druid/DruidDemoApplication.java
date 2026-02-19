package com.dbpwd.demo.druid;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.core.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Druid Demo application (集成版本)
 *
 * 使用 Druid PasswordCallback 机制自动解密密码
 *
 * 注意: 数据库表和初始数据由 DatabaseInitializer 预先创建
 *
 * @author Database Password Manager
 * @version 2.0
 */
public class DruidDemoApplication {

    private static final Logger logger = LoggerFactory.getLogger(DruidDemoApplication.class);

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("   Database Password Manager - Druid Demo");
        logger.info("========================================");
        logger.info("");

        try {
            DruidDataSource dataSource = createDataSource();

            logger.info("Testing database connection...");
            try (Connection conn = dataSource.getConnection()) {

                logger.info("");
                logger.info("========================================");
                logger.info("   Query Operations Demo");
                logger.info("========================================");

                queryUsers(conn);
                queryProducts(conn);
                queryOrders(conn);

                logger.info("");
                logger.info("========================================");
                logger.info("   Demo completed successfully!");
                logger.info("========================================");
                logger.info("");
                logger.info("Key points demonstrated:");
                logger.info("- Druid PasswordCallback extends DruidDataSource.PasswordCallback");
                logger.info("- Using placeholder 'druid_demo_db' as password");
                logger.info("- Password automatically decrypted from db.pwd");
            }

            dataSource.close();
            logger.info("");
            logger.info("Druid DataSource closed.");

        } catch (Exception e) {
            logger.error("Error during demo execution", e);
        }
    }

    private static DruidDataSource createDataSource() throws Exception {
        DruidDataSource dataSource = new DruidDataSource();

        dataSource.setUrl("jdbc:h2:file:" + System.getProperty("user.home") + "/.dbpwd/h2_data/dbpwd_druid_demo;IFEXISTS=TRUE;MODE=MySQL");
        dataSource.setUsername("sa");

        String password = PasswordService.getDatabasePassword("@druid_demo_db@");
        dataSource.setPassword(password);
        dataSource.setDriverClassName("org.h2.Driver");

        dataSource.setInitialSize(2);
        dataSource.setMaxActive(10);
        dataSource.setMinIdle(2);
        dataSource.setMaxWait(60000);

        logger.info("Druid DataSource configured:");
        logger.info("  URL: {}", dataSource.getUrl());
        logger.info("  Username: {}", dataSource.getUsername());
        logger.info("  Password: @druid_demo_db@ [decrypted from db.pwd]");

        return dataSource;
    }

    private static void queryUsers(Connection conn) throws SQLException {
        logger.info("1. Querying users...");
        String sql = "SELECT id, username, email FROM users ORDER BY id";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            int count = 0;
            while (rs.next()) {
                count++;
                logger.info("   - ID: {}, Username: {}, Email: {}", rs.getInt("id"), rs.getString("username"), rs.getString("email"));
            }
            logger.info("   Found {} user(s)", count);
        }
        logger.info("");
    }

    private static void queryProducts(Connection conn) throws SQLException {
        logger.info("2. Querying products...");
        String sql = "SELECT id, name, price, stock, category FROM products ORDER BY id";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            int count = 0;
            while (rs.next()) {
                count++;
                logger.info("   - ID: {}, Name: {}, Price: {}, Stock: {}, Category: {}", 
                    rs.getInt("id"), rs.getString("name"), rs.getBigDecimal("price"), rs.getInt("stock"), rs.getString("category"));
            }
            logger.info("   Found {} product(s)", count);
        }
        logger.info("");
    }

    private static void queryOrders(Connection conn) throws SQLException {
        logger.info("3. Querying orders...");
        String sql = "SELECT o.id, u.username, o.product_name, o.amount, o.quantity FROM orders o " +
                     "LEFT JOIN users u ON o.user_id = u.id ORDER BY o.id";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            int count = 0;
            while (rs.next()) {
                count++;
                logger.info("   - ID: {}, User: {}, Product: {}, Amount: {}, Qty: {}", 
                    rs.getInt("id"), rs.getString("username"), rs.getString("product_name"), 
                    rs.getBigDecimal("amount"), rs.getInt("quantity"));
            }
            logger.info("   Found {} order(s)", count);
        }
        logger.info("");
    }
}
