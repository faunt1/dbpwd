package com.dbpwd.demo.druid;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.druid.DbPwdDruidPasswordCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Druid Demo application - INTEGRATED VERSION
 *
 * 集成版本 - 使用 DbPwdDruidPasswordCallback 自动解密密码
 *
 * 【集成改动点】
 * 1. 添加 import com.dbpwd.druid.DbPwdDruidPasswordCallback;
 * 2. 在 createDataSource() 方法中:
 *    原始: String password = "sa"; dataSource.setPassword(password);
 *    集成: dataSource.setPassword("@druid_demo_db@"); 
 *         dataSource.setPasswordCallback(new DbPwdDruidPasswordCallback());
 *
 * 注意: 使用 DbPwdDruidPasswordCallback 实现 Druid 连接池自动解密
 *      不要使用 PasswordService.getDatabasePassword() + setPassword()
 *
 * @author Database Password Manager
 * @version 3.0
 */
public class DruidDemoApplication {

    private static final Logger logger = LoggerFactory.getLogger(DruidDemoApplication.class);

    private static final String DB_PLACEHOLDER = "@druid_demo_db@";

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("   Druid Demo - INTEGRATED VERSION");
        logger.info("   (使用 DbPwdDruidPasswordCallback 自动解密)");
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
                logger.info("Security Features in INTEGRATED version:");
                logger.info("- Password is encrypted with SM4-GCM");
                logger.info("- Using DbPwdDruidPasswordCallback for auto-decryption");
                logger.info("- Placeholder: {}", DB_PLACEHOLDER);
            }

            dataSource.close();
            logger.info("Druid DataSource closed.");

        } catch (Exception e) {
            logger.error("Error during demo execution", e);
        }
    }

    private static DruidDataSource createDataSource() throws Exception {
        DruidDataSource dataSource = new DruidDataSource();

        dataSource.setUrl("jdbc:h2:file:" + System.getProperty("user.home") + "/.dbpwd/h2_data/dbpwd_druid_demo;IFEXISTS=TRUE;MODE=MySQL");
        dataSource.setUsername("sa");

        // 方案1: Druid PasswordCallback 自动解密
        // 设置占位符密码
        dataSource.setPassword(DB_PLACEHOLDER);
        dataSource.setDriverClassName("org.h2.Driver");

        // 配置 PasswordCallback - Druid 获取连接时自动解密
        DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback();
        callback.setDataSource(dataSource);  // 重要: 让 callback 能获取原始密码
        dataSource.setPasswordCallback(callback);

        dataSource.setInitialSize(2);
        dataSource.setMaxActive(10);
        dataSource.setMinIdle(2);
        dataSource.setMaxWait(60000);

        logger.info("Druid DataSource configured:");
        logger.info("  URL: {}", dataSource.getUrl());
        logger.info("  Username: {}", dataSource.getUsername());
        logger.info("  Password: {} (encrypted, auto-decrypted by DbPwdDruidPasswordCallback)", DB_PLACEHOLDER);
        logger.info("  Integration Mode: 方案1 - Druid PasswordCallback");

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
