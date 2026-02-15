package com.dbpwd.demo.springboot.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Demo REST Controller
 *
 * 演示使用编程API方式配置的DataSource进行数据库操作
 */
@RestController
@RequestMapping("/api")
public class DemoController {

    private static final Logger logger = LoggerFactory.getLogger(DemoController.class);

    @Autowired
    private DataSource dataSource;

    @GetMapping("/test")
    public Map<String, Object> testConnection() {
        Map<String, Object> result = new HashMap<>();

        try (Connection conn = dataSource.getConnection()) {
            // 创建测试表
            conn.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS products (" +
                    "id INT IDENTITY PRIMARY KEY, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "price DECIMAL(10,2) NOT NULL)"
            );

            // 插入测试数据
            conn.createStatement().execute(
                    "INSERT INTO products (name, price) VALUES ('Laptop', 1299.99)"
            );

            // 查询数据
            ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM products");
            if (rs.next()) {
                result.put("productCount", rs.getInt(1));
            }

            result.put("status", "success");
            result.put("message", "Database connection successful!");
            result.put("integrationMode", "Spring Boot Programming API");
            result.put("passwordManagement", "PasswordService.getDatabasePassword()");

            logger.info("Database test successful - products: {}", result.get("productCount"));

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
            logger.error("Database test failed", e);
        }

        return result;
    }

    @GetMapping("/info")
    public Map<String, Object> getInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("demo", "Spring Boot API Demo");
        info.put("integrationMode", "Programming API");
        info.put("description", "Uses PasswordService.getDatabasePassword() API");
        info.put("advantages", Arrays.asList(
                "Full control over DataSource configuration",
                "No auto-configuration dependency",
                "Custom logic during setup",
                "Explicit password management"
        ));
        return info;
    }
}
