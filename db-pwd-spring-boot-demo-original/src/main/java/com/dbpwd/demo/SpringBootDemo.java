package com.dbpwd.demo;

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
 * Spring Boot demo application - ORIGINAL VERSION (before integration)
 *
 * 原始版本 - 使用明文密码，未集成密码加密功能
 * 此版本用于对比展示集成密码加密功能前后的代码差异
 *
 * 【集成改动点】
 * 1. 添加 import com.dbpwd.core.PasswordService;
 * 2. 将 DB_PASSWORD 改为 DB_PLACEHOLDER
 * 3. 在 dataSource() 方法中调用 PasswordService.getDatabasePassword(DB_PLACEHOLDER)
 *
 * @author Database Password Manager
 * @version 1.0
 */
@SpringBootApplication
public class SpringBootDemo {

    // 【原始版本】明文密码 - 集成时需改为占位符
    private static final String DB_PASSWORD = "sa";

    // H2 database configuration
    private static final String DB_URL = "jdbc:h2:file:~/.dbpwd/h2_data/dbpwd_spring_boot_original;DB_CLOSE_DELAY=-1;MODE=MySQL";
    private static final String DB_USERNAME = "sa";

    public static void main(String[] args) {
        SpringApplication.run(SpringBootDemo.class, args);
    }

    /**
     * 创建DataSource Bean - 使用明文密码
     *
     * 【集成改动】
     * 原始: String plainPassword = DB_PASSWORD;
     * 集成: String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
     */
    @Bean
    public DataSource dataSource() throws Exception {
        // 【原始版本】直接使用明文密码
        String plainPassword = DB_PASSWORD;
        System.out.println("WARNING: Using plaintext password!");

        // 创建数据源
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
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

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "email VARCHAR(255) NOT NULL UNIQUE, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");

            jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS orders (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY, " +
                    "user_id INT NOT NULL, " +
                    "product_name VARCHAR(200) NOT NULL, " +
                    "amount DECIMAL(10,2) NOT NULL, " +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                    "FOREIGN KEY (user_id) REFERENCES users(id))");

            Integer userCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Integer.class);
            if (userCount == 0) {
                jdbcTemplate.update("INSERT INTO users (name, email) VALUES " +
                        "('Alice', 'alice@example.com'), " +
                        "('Bob', 'bob@example.com')");

                jdbcTemplate.update("INSERT INTO orders (user_id, product_name, amount) VALUES " +
                        "(1, 'Product A', 100.00), " +
                        "(2, 'Product B', 200.00)");

                System.out.println("Database initialized with demo data");
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
            result.put("application", "Database Password Manager - Spring Boot Demo (ORIGINAL)");
            result.put("status", "running");
            result.put("warning", "Using PLAINTEXT password!");
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
                "SELECT id, name, email FROM users ORDER BY id"
            );
        }
    }
}
