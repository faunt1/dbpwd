package com.dbpwd.demo;

import com.dbpwd.crypto.Sm4Cipher;
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
 * Using H2 in-memory database
 *
 * Features:
 * 1. Spring Boot web application with REST API
 * 2. JdbcTemplate for database access
 * 3. SM4 encryption for database password
 * 4. H2 in-memory database connection
 *
 * @author Database Password Manager
 * @version 4.4
 */
@SpringBootApplication
public class SpringBootDemo {

    private static final String DB_URL = "jdbc:h2:mem:dbpwd_demo;DB_CLOSE_DELAY=-1";
    private static final String DB_USERNAME = "sa";
    private static final String DB_PASSWORD = ""; // H2 default: empty password
    private static final String TEST_SM4_KEY = "0123456789abcdef0123456789abcdef";

    static {
        // Test SM4 encryption at class load time
        try {
            String encryptedDbPassword = Sm4Cipher.encrypt(DB_PASSWORD, TEST_SM4_KEY);
            String formattedEncrypted = ENCFormat.formatSM4(encryptedDbPassword);
            System.out.println("=== Spring Boot Demo Application ===");
            System.out.println("Database: H2 In-Memory");
            System.out.println("Database URL: " + DB_URL);
            System.out.println("Database password: " + DB_PASSWORD);
            System.out.println("SM4 key: " + TEST_SM4_KEY);
            System.out.println("SM4 Encryption Test:");
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
            // Test SM4 decryption after application started
            String decryptedPassword = Sm4Cipher.decrypt(
                ENCFormat.extractCipherText(ENCFormat.formatSM4(Sm4Cipher.encrypt(DB_PASSWORD, TEST_SM4_KEY))),
                TEST_SM4_KEY
            );
            System.out.println("SM4 Decryption Test:");
            System.out.println("  Decrypted password: " + decryptedPassword);
            System.out.println("  Verification: " + (DB_PASSWORD.equals(decryptedPassword) ? "SUCCESS" : "FAILED"));
            System.out.println();
            System.out.println("=== Demo Completed Successfully ===");
        } catch (Exception e) {
            System.err.println("SM4 decryption test failed: " + e.getMessage());
        }
    }

    @Bean
    public DataSource dataSource() {
        try {
            Class.forName("org.h2.Driver");
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl(DB_URL);
            dataSource.setUsername(DB_USERNAME);
            dataSource.setPassword(DB_PASSWORD);
            return dataSource;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("H2 JDBC driver not found", e);
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
                    "id IDENTITY PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(255) NOT NULL UNIQUE, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP)");

            // Create orders table
            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS orders (" +
                    "id IDENTITY PRIMARY KEY, " +
                    "user_id BIGINT NOT NULL, " +
                    "product_name VARCHAR(200) NOT NULL, " +
                    "amount DECIMAL(10,2) NOT NULL, " +
                    "created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id))");

            // Insert demo data
            jdbcTemplate.update("INSERT INTO users (name, email) VALUES " +
                    "('Alice', 'alice@example.com'), " +
                    "('Bob', 'bob@example.com'), " +
                    "('Charlie', 'charlie@example.com')");

            jdbcTemplate.update("INSERT INTO orders (user_id, product_name, amount) VALUES " +
                    "(1, 'Product A', 100.00), " +
                    "(2, 'Product B', 200.00), " +
                    "(1, 'Product C', 150.00)");

            int userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
            System.out.println("Database initialized with " + userCount + " users");
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
            result.put("database", "SQLite");
            result.put("description", "Demo application with encrypted database password");
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
                result.put("database", "connected");
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
                "SELECT id, name, email, created_at as createdAt FROM users ORDER BY id"
            );
        }

        @GetMapping("/orders")
        public List<Map<String, Object>> getOrders() {
            return jdbcTemplate.queryForList(
                "SELECT o.id, u.name as user_name, o.product_name as productName, " +
                "o.amount, o.created_at " +
                "FROM orders o LEFT JOIN users u ON o.user_id = u.id ORDER BY o.id"
            );
        }
    }
}
