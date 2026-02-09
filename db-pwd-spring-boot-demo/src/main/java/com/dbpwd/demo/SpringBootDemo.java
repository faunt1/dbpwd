import com.dbpwd.crypto.Sm4Cipher;
import com.dbpwd.crypto.ENCFormat;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.web.bind.annotation.GetMapping;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

/**
 * Spring Boot demo application
 *
 * Demo Database Password Manager in Spring Boot web application
 * Using SQLite as demo database
 *
 * Features:
 * 1. Spring Boot web application with REST API
 * 2. JdbcTemplate for database access
 * 3. SM4 encryption for database password
 * 4. SQLite database connection
 *
 * @author Database Password Manager
 * @version 4.3
 */
@SpringBootApplication
public class SpringBootDemo {

    private static final String DB_PATH = "target/dbpwd-demo.db";
    private static final String DB_PASSWORD = "test123"; // SQLite database password
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;
    private static final String TEST_SM4_KEY = "0123456789abcdef0123456789abcdef";

    public static void main(String[] args) {
        System.out.println("=== Spring Boot Demo Application ===");
        System.out.println("Database: SQLite");
        System.out.println("Database path: " + DB_PATH);
        System.out.println("Database password: " + DB_PASSWORD);
        System.out.println("SM4 key: " + TEST_SM4_KEY);
        System.out.println();

        try {
            // Test SM4 encryption
            String encryptedDbPassword = Sm4Cipher.encrypt(DB_PASSWORD, TEST_SM4_KEY);
            String formattedEncrypted = ENCFormat.formatSM4(encryptedDbPassword);
            System.out.println("SM4 Encryption Test:");
            System.out.println("  Original password: " + DB_PASSWORD);
            System.out.println("  Encrypted password: " + formattedEncrypted);
            System.out.println();

            // Start Spring Boot application
            SpringApplication.run(SpringBootDemo.class, args);
            System.out.println("Spring Boot application started successfully!");
            System.out.println("Access http://localhost:8080 for demo");
            System.out.println();

        } catch (Exception e) {
            System.err.println("Application failed to start: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Bean
    public DataSource dataSource() {
        try {
            Class.forName("org.sqlite.JDBC");
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.sqlite.JDBC");
            dataSource.setUrl(DB_URL);
            return dataSource;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("SQLite JDBC driver not found", e);
        }
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @org.springframework.web.bind.annotation.RestController
    public static class DemoController {

        private final JdbcTemplate jdbcTemplate;

        public DemoController(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        @GetMapping("/")
        public Map<String, Object> index() {
            return Map.of(
                "application", "Database Password Manager - Spring Boot Demo",
                "status", "running",
                "database", "SQLite",
                "description", "Demo application with encrypted database password"
            );
        }

        @GetMapping("/health")
        public Map<String, Object> health() {
            try {
                Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) as count FROM users", Integer.class
                );
                return Map.of(
                    "status", "healthy",
                    "user_count", count,
                    "database", "connected"
                );
            } catch (Exception e) {
                return Map.of(
                    "status", "unhealthy",
                    "error", e.getMessage()
                );
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
