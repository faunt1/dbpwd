package com.dbpwd.demo.druid;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.crypto.Sm4GCMCipher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DruidDemoApplication {

    private static final Logger logger = LoggerFactory.getLogger(DruidDemoApplication.class);
    private static final String DEFAULT_SM4_KEY = "000102030405060708090a0b0c0d0e0f";

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("   Database Password Manager - Druid Demo");
        logger.info("========================================");
        logger.info("");

        try {
            // 创建Druid数据源（使用ENC格式密码）
            DruidDataSource dataSource = createDataSource();

            // 测试数据库连接
            logger.info("Testing database connection...");
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                // 初始化数据表
                logger.info("Initializing database tables...");
                stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "name VARCHAR(100) NOT NULL, " +
                        "description TEXT, " +
                        "price DECIMAL(10,2) NOT NULL, " +
                        "stock INT DEFAULT 0, " +
                        "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                        ")");
                logger.info("Products table created successfully.");

                // 演示CRUD操作
                logger.info("");
                logger.info("========================================");
                logger.info("   CRUD Operations Demo");
                logger.info("========================================");

                // 1. 创建产品
                logger.info("");
                logger.info("1. Creating products...");
                long id1 = insertProduct(conn, "Laptop", "High-performance laptop", new BigDecimal("1299.99"), 50);
                logger.info("   Created product: Laptop - 1299.99");

                long id2 = insertProduct(conn, "Smartphone", "Latest smartphone model", new BigDecimal("899.99"), 100);
                logger.info("   Created product: Smartphone - 899.99");

                // 2. 查询所有产品
                logger.info("");
                logger.info("2. Querying all products...");
                List<Product> products = findAllProducts(conn);
                logger.info("   Found {} product(s):", products.size());
                for (Product p : products) {
                    logger.info("   - ID: {}, Name: {}, Price: {}, Stock: {}",
                        p.id, p.name, p.price, p.stock);
                }

                // 3. 根据ID查询产品
                logger.info("");
                logger.info("3. Querying product by ID: {}...", id1);
                Product product = findProductById(conn, id1);
                if (product != null) {
                    logger.info("   Found: {} - {} (Stock: {})",
                        product.name, product.description, product.stock);
                }

                // 4. 更新产品
                logger.info("");
                logger.info("4. Updating product ID: {}...", id1);
                if (product != null) {
                    product.price = new BigDecimal("1199.99");
                    product.stock = 45;
                    updateProduct(conn, product);
                    logger.info("   Updated product: Laptop - 1199.99");
                }

                // 5. 删除产品
                logger.info("");
                logger.info("5. Deleting product ID: {}...", id2);
                deleteProductById(conn, id2);
                logger.info("   Deleted 1 row(s).");

                // 6. 统计剩余产品
                logger.info("");
                logger.info("6. Querying remaining products...");
                products = findAllProducts(conn);
                logger.info("   Found {} product(s):", products.size());
                for (Product p : products) {
                    logger.info("   - ID: {}, Name: {}, Price: {}, Stock: {}",
                        p.id, p.name, p.price, p.stock);
                }

                logger.info("");
                logger.info("========================================");
                logger.info("   Demo completed successfully!");
                logger.info("========================================");
                logger.info("");
                logger.info("Key points demonstrated:");
                logger.info("- ENC(SM4-GCM:xxx) format for encrypted password");
                logger.info("- SM4-GCM encryption for secure password storage");
                logger.info("- Druid connection pool manages connections");
                logger.info("- No need for additional DruidPasswordCallback");
            }

            // 关闭数据源
            dataSource.close();
            logger.info("");
            logger.info("Druid DataSource closed.");

        } catch (Exception e) {
            logger.error("Error during demo execution", e);
        }
    }

    private static DruidDataSource createDataSource() throws Exception {
        DruidDataSource dataSource = new DruidDataSource();

        // 数据库连接配置
        dataSource.setUrl("jdbc:h2:mem:dbpwd_druid_demo;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");

        // 生成ENC格式密码
        String encPassword = generateEncryptedPassword("sa");
        dataSource.setPassword(encPassword);
        dataSource.setDriverClassName("org.h2.Driver");

        // 连接池配置
        dataSource.setInitialSize(2);
        dataSource.setMaxActive(10);
        dataSource.setMinIdle(2);
        dataSource.setMaxWait(60000);

        logger.info("Druid DataSource configured:");
        logger.info("  URL: {}", dataSource.getUrl());
        logger.info("  Username: {}", dataSource.getUsername());
        logger.info("  Password: ENC(SM4-GCM:...) [encrypted]");

        return dataSource;
    }

    private static String generateEncryptedPassword(String plainPassword) throws Exception {
        String encryptedPassword = Sm4GCMCipher.encrypt(plainPassword, DEFAULT_SM4_KEY);
        String encPassword = "ENC(SM4-GCM:" + encryptedPassword + ")";
        logger.info("Generated ENC password: {}", encPassword);
        return encPassword;
    }

    private static long insertProduct(Connection conn, String name, String description,
            BigDecimal price, int stock) throws SQLException {
        String sql = "INSERT INTO products (name, description, price, stock, created_at) VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, description);
            ps.setBigDecimal(3, price);
            ps.setInt(4, stock);
            ps.setTimestamp(5, new Timestamp(new Date().getTime()));

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1;
    }

    private static Product findProductById(Connection conn, long id) throws SQLException {
        String sql = "SELECT id, name, description, price, stock, created_at FROM products WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Product product = new Product();
                    product.id = rs.getLong("id");
                    product.name = rs.getString("name");
                    product.description = rs.getString("description");
                    product.price = rs.getBigDecimal("price");
                    product.stock = rs.getInt("stock");
                    product.createdAt = rs.getTimestamp("created_at");
                    return product;
                }
            }
        }
        return null;
    }

    private static List<Product> findAllProducts(Connection conn) throws SQLException {
        String sql = "SELECT id, name, description, price, stock, created_at FROM products ORDER BY id";
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
                product.createdAt = rs.getTimestamp("created_at");
                products.add(product);
            }
        }
        return products;
    }

    private static void updateProduct(Connection conn, Product product) throws SQLException {
        String sql = "UPDATE products SET name = ?, description = ?, price = ?, stock = ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, product.name);
            ps.setString(2, product.description);
            ps.setBigDecimal(3, product.price);
            ps.setInt(4, product.stock);
            ps.setLong(5, product.id);

            ps.executeUpdate();
        }
    }

    private static void deleteProductById(Connection conn, long id) throws SQLException {
        String sql = "DELETE FROM products WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private static class Product {
        long id;
        String name;
        String description;
        BigDecimal price;
        int stock;
        java.util.Date createdAt;
    }
}
