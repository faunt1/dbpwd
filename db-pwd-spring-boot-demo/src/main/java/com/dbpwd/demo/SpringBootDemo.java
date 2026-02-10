package com.dbpwd.demo;

import com.dbpwd.crypto.Sm4GCMCipher;
import com.dbpwd.crypto.ENCFormat;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.bind.annotation.GetMapping;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Boot demo application
 *
 * Demo Database Password Manager in Spring Boot web application
 * Using MySQL database
 *
 * Features:
 * 1. Spring Boot web application with REST API
 * 2. JdbcTemplate for database access
 * 3. SM4 encryption for database password
 * 4. MySQL database connection with CRUD operations
 *
 * @author Database Password Manager
 * @version 5.0
 */
@SpringBootApplication
public class SpringBootDemo {

    // MySQL database configuration
    private static final String DB_URL = "jdbc:mysql://localhost:3306/dbpwd_demo?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USERNAME = "dbpwd";
    private static final String DB_PASSWORD = "dbpwd123";
    private static final String TEST_SM4_KEY = "0123456789abcdef0123456789abcdef";

    static {
        // Test SM4-GCM encryption at class load time
        try {
            String encryptedDbPassword = Sm4GCMCipher.encrypt(DB_PASSWORD, TEST_SM4_KEY);
            String formattedEncrypted = ENCFormat.formatSM4GCM(encryptedDbPassword);
            System.out.println("=== Spring Boot Demo Application ===");
            System.out.println("Database: MySQL");
            System.out.println("Database URL: " + DB_URL);
            System.out.println("Database password: " + DB_PASSWORD);
            System.out.println("SM4 key: " + TEST_SM4_KEY);
            System.out.println("SM4-GCM Encryption Test:");
            System.out.println("  Original password: " + DB_PASSWORD);
            System.out.println("  Encrypted password: " + formattedEncrypted);
            System.out.println();
        } catch (Exception e) {
            System.err.println("SM4 encryption test failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        System.out.println("Starting Spring Boot application...");
        SpringApplication.run(SpringBootDemo.class, args);
        System.out.println("Spring Boot application started successfully!");
        System.out.println("Access http://localhost:8080 for demo");
        System.out.println();

        try {
            // Test SM4-GCM decryption after application started
            String formattedEncrypted = ENCFormat.formatSM4GCM(Sm4GCMCipher.encrypt(DB_PASSWORD, TEST_SM4_KEY));
            String algorithm = ENCFormat.extractAlgorithm(formattedEncrypted);
            String cipherText = ENCFormat.extractCipherText(formattedEncrypted);

            // 仅支持SM4-GCM格式，ECB格式抛出异常
            if (!"SM4-GCM".equalsIgnoreCase(algorithm)) {
                throw new IllegalArgumentException(
                    "Unsupported encryption algorithm: " + algorithm + ". Only SM4-GCM is supported for new applications");
            }

            String decryptedPassword = Sm4GCMCipher.decrypt(cipherText, TEST_SM4_KEY);

            System.out.println("SM4-GCM Decryption Test:");
            System.out.println("  Algorithm: " + algorithm);
            System.out.println("  Decrypted password: " + decryptedPassword);
            System.out.println("  Verification: " + (DB_PASSWORD.equals(decryptedPassword) ? "SUCCESS" : "FAILED"));
            System.out.println();
            System.out.println("=== Demo Completed Successfully ===");
        } catch (Exception e) {
            System.err.println("SM4-GCM decryption test failed: " + e.getMessage());
        }
    }

    @Bean
    public DataSource dataSource() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
            dataSource.setUrl(DB_URL);
            dataSource.setUsername(DB_USERNAME);
            dataSource.setPassword(DB_PASSWORD);
            return dataSource;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC driver not found", e);
        }
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public CommandLineRunner initDatabase(JdbcTemplate jdbcTemplate) {
        return args -> {
            // Initialize demo database
            System.out.println("Initializing demo database...");

            // Create users table
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(255) NOT NULL UNIQUE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            // Create orders table
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS orders (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id INT NOT NULL, " +
                    "product_name VARCHAR(200) NOT NULL, " +
                    "amount DECIMAL(10,2) NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id))");

            // Check if data already exists
            Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
            if (userCount == 0) {
                // Insert demo data
                jdbcTemplate.update("INSERT INTO users (name, email) VALUES " +
                        "('Alice', 'alice@example.com'), " +
                        "('Bob', 'bob@example.com'), " +
                        "('Charlie', 'charlie@example.com')");

                jdbcTemplate.update("INSERT INTO orders (user_id, product_name, amount) VALUES " +
                        "(1, 'Product A', 100.00), " +
                        "(2, 'Product B', 200.00), " +
                        "(1, 'Product C', 150.00)");

                System.out.println("Database initialized with 3 users and 3 orders");
            } else {
                System.out.println("Database already contains " + userCount + " users");
            }
        };
    }

    @org.springframework.web.bind.annotation.RestController
    public static class DemoController {

        private final JdbcTemplate jdbcTemplate;

        public DemoController(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @GetMapping("/")
        public Map<String, Object> index() {
            Map<String, Object> result = new HashMap<>();
            result.put("application", "Database Password Manager - Spring Boot Demo");
            result.put("status", "running");
            result.put("database", "MySQL");
            result.put("description", "Demo application with SM4-GCM encrypted database password");
            result.put("encryption", "SM4-GCM (Authenticated Encryption)");
            return result;
        }

        @GetMapping("/health")
        public Map<String, Object> health() {
            try {
                Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) as count FROM users", Integer.class
                );
                Map<String, Object> result = new HashMap<>();
                result.put("status", "healthy");
                result.put("user_count", count);
                result.put("database", "MySQL - connected");
                return result;
            } catch (Exception e) {
                Map<String, Object> result = new HashMap<>();
                result.put("status", "unhealthy");
                result.put("error", e.getMessage());
                return result;
            }
        }

        @GetMapping("/users")
        public List<Map<String, Object>> getUsers() {
            return jdbcTemplate.queryForList(
                "SELECT id, name, email, " +
                "DATE_FORMAT(created_at, '%Y-%m-%dT%H:%i:%s') as createdAt " +
                "FROM users ORDER BY id"
            );
        }

        @GetMapping("/orders")
        public List<Map<String, Object>> getOrders() {
            return jdbcTemplate.queryForList(
                "SELECT o.id, u.name as userName, o.product_name as productName, " +
                "o.amount, DATE_FORMAT(o.created_at, '%Y-%m-%dT%H:%i:%s') as createdAt " +
                "FROM orders o LEFT JOIN users u ON o.user_id = u.id ORDER BY o.id"
            );
        }

        @GetMapping("/stats")
        public Map<String, Object> getStats() {
            Map<String, Object> result = new HashMap<>();

            try {
                Integer userCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users", Integer.class
                );
                Integer orderCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM orders", Integer.class
                );
                Double totalAmount = jdbcTemplate.queryForObject(
                    "SELECT SUM(amount) FROM orders", Double.class
                );

                result.put("userCount", userCount);
                result.put("orderCount", orderCount);
                result.put("totalAmount", totalAmount != null ? totalAmount : 0.0);
                result.put("status", "success");
            } catch (Exception e) {
                result.put("status", "error");
                result.put("message", e.getMessage());
            }

            return result;
        }
    }
}
