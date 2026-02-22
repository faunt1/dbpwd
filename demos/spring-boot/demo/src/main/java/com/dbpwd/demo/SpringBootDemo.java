package com.dbpwd.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Spring Boot demo application (方案C集成版本)
 *
 * 集成方式：BeanPostProcessor 自动解密
 * - 数据源配置：application.yml
 * - 密码格式：@spring_boot_db@
 * - 自动解密：DataSourcePasswordProcessor 自动处理
 *
 * 注意：数据库表和初始数据由 DatabaseInitializer 预先创建
 *
 * @author Database Password Manager
 * @version 3.0
 */
@SpringBootApplication
public class SpringBootDemo {

    public static void main(String[] args) {
        SpringApplication.run(SpringBootDemo.class, args);
    }

    @Bean
    public JdbcTemplate jdbcTemplate(javax.sql.DataSource dataSource) {
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
            result.put("application", "Database Password Manager - Spring Boot Demo");
            result.put("integraton_mode", "方案C: BeanPostProcessor (自动解密)");
            result.put("status", "running");
            result.put("encryption", "SM4-GCM (Authenticated Encryption)");
            result.put("placeholder", "@spring_boot_db@");
            result.put("note", "应用代码无需任何修改，仅需配置 application.yml");
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
