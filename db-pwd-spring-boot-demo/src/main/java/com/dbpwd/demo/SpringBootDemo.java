package com.dbpwd.demo;

import com.dbpwd.core.PasswordService;
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
 * 集成模式: 模式2（配置文件装饰器模式）
 * 解密实现: 框架实现（DataSourceDecorator自动拦截和替换）
 * 占位符: @spring_boot_db@
 *
 * 工作机制:
 * 1. Spring Boot应用启动时加载配置文件
 * 2. 程序启动时从 db.pwd 和 sm4.key 获取解密后的明文密码（通过PasswordService一步调用）
 * 3. 将明文密码注入到DataSource配置中
 * 4. 建立连接池和数据库连接
 * 5. 应用正常使用数据库连接
 *
 * @author Database Password Manager
 * @version 5.3
 */
@SpringBootApplication
public class SpringBootDemo {

    // 占位符标识（用于标识不同的数据库配置）
    private static final String DB_PLACEHOLDER = "spring_boot_db";

    // MySQL database configuration
    private static final String DB_URL = "jdbc:mysql://localhost:3306/dbpwd_demo?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_USERNAME = "dbpwd";

    public static void main(String[] args) {
        SpringApplication.run(SpringBootDemo.class, args);
    }

    /**
     * 创建DataSource Bean
     *
     * 使用PasswordService从 db.pwd 和 sm4.key 获取解密后的密码
     */
    @Bean
    public DataSource dataSource() throws Exception {
        // 从 db.pwd 和 sm4.key 获取解密后的数据库密码
        String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
        System.out.println("解密后的密码: " + plainPassword);

        // 创建数据源，使用解密后的明文密码
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setUrl(DB_URL);
        dataSource.setUsername(DB_USERNAME);
        dataSource.setPassword(plainPassword);

        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public CommandLineRunner initDatabase(JdbcTemplate jdbcTemplate) {
        return args -> {
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
            result.put("integrationMode", "模式2（配置文件装饰器模式）");
            result.put("placeholder", "@spring_boot_db@");
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
