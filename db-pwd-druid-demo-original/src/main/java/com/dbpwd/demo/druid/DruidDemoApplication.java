package com.dbpwd.demo.druid;

import com.alibaba.druid.pool.DruidDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Druid Demo application - ORIGINAL VERSION (before integration)
 *
 * 原始版本 - 使用明文密码，未集成密码加密功能
 * 此版本用于对比展示集成密码加密功能前后的代码差异
 *
 * 【集成改动点】
 * 1. 添加 import com.dbpwd.core.PasswordService;
 * 2. 在 createDataSource() 方法中:
 *    原始: String password = "sa";
 *    集成: String password = PasswordService.getDatabasePassword("@druid_demo_db@");
 *
 * @author Database Password Manager
 * @version 1.0
 */
public class DruidDemoApplication {

    private static final Logger logger = LoggerFactory.getLogger(DruidDemoApplication.class);

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("   Druid Demo - ORIGINAL VERSION");
        logger.info("   (使用明文密码，未集成密码加密)");
        logger.info("========================================");
        logger.info("");

        try {
            DruidDataSource dataSource = createDataSource();

            logger.info("Testing database connection...");
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                logger.info("Initializing database tables...");
                stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "name VARCHAR(100) NOT NULL, " +
                        "description TEXT, " +
                        "price DECIMAL(10,2) NOT NULL, " +
                        "stock INT DEFAULT 0, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")");

                logger.info("");
                logger.info("========================================");
                logger.info("   CRUD Operations Demo");
                logger.info("========================================");

                logger.info("");
                logger.info("1. Creating products...");
                long id1 = insertProduct(conn, "Laptop", "High-performance laptop", new BigDecimal("1299.99"), 50);
                logger.info("   Created product: Laptop - 1299.99");

                logger.info("");
                logger.info("2. Querying all products...");
                List<Product> products = findAllProducts(conn);
                logger.info("   Found {} product(s):", products.size());
                for (Product p : products) {
                    logger.info("   - ID: {}, Name: {}, Price: {}", p.id, p.name, p.price);
                }

                logger.info("");
                logger.info("========================================");
                logger.info("   Demo completed successfully!");
                logger.info("========================================");
                logger.info("");
                logger.info("Security Issues in ORIGINAL version:");
                logger.info("- Password is hardcoded in source code");
            }

            dataSource.close();
            logger.info("Druid DataSource closed.");

        } catch (Exception e) {
            logger.error("Error during demo execution", e);
        }
    }

    /**
     * 创建Druid数据源 - 使用明文密码
     *
     * 【集成改动】
     * 原始: String password = "sa";
     * 集成: String password = PasswordService.getDatabasePassword("@druid_demo_db@");
     */
    private static DruidDataSource createDataSource() throws Exception {
        DruidDataSource dataSource = new DruidDataSource();

        dataSource.setUrl("jdbc:h2:file:~/.dbpwd/h2_data/dbpwd_druid_original;MODE=MySQL");
        dataSource.setUsername("sa");

        // 【原始版本】直接使用明文密码
        String password = "sa";
        dataSource.setPassword(password);
        dataSource.setDriverClassName("org.h2.Driver");

        dataSource.setInitialSize(2);
        dataSource.setMaxActive(10);
        dataSource.setMinIdle(2);
        dataSource.setMaxWait(60000);

        logger.info("Druid DataSource configured:");
        logger.info("  URL: {}", dataSource.getUrl());
        logger.info("  Username: {}", dataSource.getUsername());
        logger.warn("  Password: sa (PLAINTEXT - WARNING!)");

        return dataSource;
    }

    private static long insertProduct(Connection conn, String name, String description,
            BigDecimal price, int stock) throws SQLException {
        String sql = "INSERT INTO products (name, description, price, stock, created_at) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setBigDecimal(3, price);
            ps.setInt(4, stock);
            ps.setTimestamp(5, new Timestamp(System.currentTimeMillis()));

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1;
    }

    private static List<Product> findAllProducts(Connection conn) throws SQLException {
        String sql = "SELECT id, name, description, price, stock FROM products ORDER BY id";
        List<Product> products = new ArrayList<>();

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Product product = new Product();
                product.id = rs.getLong("id");
                product.name = rs.getString("name");
                product.description = rs.getString("description");
                product.price = rs.getBigDecimal("price");
                product.stock = rs.getInt("stock");
                products.add(product);
            }
        }
        return products;
    }

    private static class Product {
        long id;
        String name;
        String description;
        BigDecimal price;
        int stock;
    }
}
