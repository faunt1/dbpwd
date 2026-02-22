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
            ResultSet rs = conn.createStatement().executeQuery("SELECT COUNT(*) FROM products");
            if (rs.next()) {
                result.put("productCount", rs.getInt(1));
            }

            result.put("status", "success");
            result.put("message", "Database connection successful!");
            result.put("integrationMode", "Spring Boot Programming API (Original)");
            result.put("passwordManagement", "Plain text password 'sa'");

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
        info.put("demo", "Spring Boot API Original Demo");
        info.put("integrationMode", "Programming API (Original)");
        info.put("description", "Uses plain text password directly");
        info.put("securityNote", "Password is hardcoded in source code");
        return info;
    }
}
