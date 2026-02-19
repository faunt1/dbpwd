package com.dbpwd.demo;

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
 *
 * 【集成改动点】
 * 1. 添加 import com.dbpwd.core.PasswordService;
 * 2. 将 DB_PASSWORD 改为 DB_PLACEHOLDER
 * 3. 在 dataSource() 方法中调用 PasswordService.getDatabasePassword(DB_PLACEHOLDER)
 *
 * 注意: 数据库表和初始数据由 DatabaseInitializer 预先创建
 *
 * @author Database Password Manager
 * @version 2.0
 */
@SpringBootApplication
public class SpringBootDemo {

    private static final String DB_PASSWORD = "sa";

    private static final String DB_URL = "jdbc:h2:file:" + System.getProperty("user.home") + "/.dbpwd/h2_data/dbpwd_spring_boot_demo;IFEXISTS=TRUE;MODE=MySQL";
    private static final String DB_USERNAME = "sa";

    public static void main(String[] args) {
        SpringApplication.run(SpringBootDemo.class, args);
    }

    @Bean
    public DataSource dataSource() throws Exception {
        String plainPassword = DB_PASSWORD;
        System.out.println("WARNING: Using plaintext password!");

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
                "SELECT id, username, email FROM users ORDER BY id"
            );
        }

        @GetMapping("/products")
        public List<Map<String, Object>> getProducts() {
            return jdbcTemplate.queryForList(
                "SELECT id, name, price, stock, category FROM products ORDER BY id"
            );
        }

        @GetMapping("/orders")
        public List<Map<String, Object>> getOrders() {
            return jdbcTemplate.queryForList(
                "SELECT o.id, u.username, o.product_name, o.amount, o.quantity FROM orders o " +
                "LEFT JOIN users u ON o.user_id = u.id ORDER BY o.id"
            );
        }
    }
}
