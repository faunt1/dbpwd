package com.dbpwd.demo.druid;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.druid.DbPwdDruidPasswordCallback;
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
 * 集成要点:
 * 1. 设置 password 为占位符格式 @druid_demo_db@
 * 2. 创建 DbPwdDruidPasswordCallback 并设置到 DataSource
 * 3. Druid 在获取连接时自动调用 callback 解密密码
 *
 * 注意: 数据库表和初始数据由 DatabaseInitializer 预先创建
 *
 * @author Database Password Manager
 * @version 3.0
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
                logger.info("- DbPwdDruidPasswordCallback extends DruidPasswordCallback");
                logger.info("- Password set as placeholder: @druid_demo_db@");
                logger.info("- Druid automatically calls callback to decrypt password");
            }

            dataSource.close();
            logger.info("");
            logger.info("Druid DataSource closed.");

        } catch (Exception e) {
            logger.error("Error during demo execution", e);
        }
    }

    /**
     * 创建 DataSource 并配置 PasswordCallback
     *
     * 核心集成点:
     * 1. password 设置为占位符 @druid_demo_db@
     * 2. 创建 DbPwdDruidPasswordCallback
     * 3. callback.setDataSource(dataSource) - 关键步骤
     * 4. dataSource.setPasswordCallback(callback)
     */
    private static DruidDataSource createDataSource() throws Exception {
        DruidDataSource dataSource = new DruidDataSource();

        dataSource.setUrl("jdbc:h2:file:" + System.getProperty("user.home") + "/.dbpwd/h2_data/dbpwd_druid_demo;IFEXISTS=TRUE;MODE=MySQL");
        dataSource.setUsername("sa");

        // 设置占位符格式的密码，不是明文密码
        dataSource.setPassword("@druid_demo_db@");
        dataSource.setDriverClassName("org.h2.Driver");

        // 配置 PasswordCallback - 这是集成的关键
        DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback();
        callback.setDataSource(dataSource);  // 重要：让 callback 能获取原始密码
        dataSource.setPasswordCallback(callback);

        dataSource.setInitialSize(2);
        dataSource.setMaxActive(10);
        dataSource.setMinIdle(2);
        dataSource.setMaxWait(60000);

        logger.info("Druid DataSource configured with PasswordCallback:");
        logger.info("  URL: {}", dataSource.getUrl());
        logger.info("  Username: {}", dataSource.getUsername());
        logger.info("  Password: @druid_demo_db@ (placeholder)");
        logger.info("  PasswordCallback: DbPwdDruidPasswordCallback");

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
