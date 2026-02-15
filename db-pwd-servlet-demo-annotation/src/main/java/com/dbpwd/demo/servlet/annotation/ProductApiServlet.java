package com.dbpwd.demo.servlet.annotation;

import com.alibaba.druid.pool.DruidDataSource;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 产品API Servlet - 使用 @WebServlet 注解配置
 *
 * 演示在注解方式下使用 DataSource 进行数据库操作
 */
@WebServlet(urlPatterns = {"/api/products", "/api/products/*"})
public class ProductApiServlet extends HttpServlet {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        resp.setContentType("application/json;charset=UTF-8");

        DataSource dataSource = (DataSource) getServletContext().getAttribute("dataSource");

        Map<String, Object> result = new HashMap<>();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            // 插入测试数据
            stmt.execute("INSERT INTO products (name, price) VALUES ('Widget', 29.99)");

            // 查询所有产品
            List<Map<String, Object>> products = new ArrayList<>();
            ResultSet rs = stmt.executeQuery("SELECT * FROM products ORDER BY id");

            while (rs.next()) {
                Map<String, Object> product = new HashMap<>();
                product.put("id", rs.getInt("id"));
                product.put("name", rs.getString("name"));
                product.put("price", rs.getBigDecimal("price"));
                products.add(product);
            }

            result.put("status", "success");
            result.put("count", products.size());
            result.put("products", products);
            result.put("integrationMode", "Servlet @WebListener + @WebServlet");
            result.put("configuration", "Java Annotations (no web.xml)");

        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        objectMapper.writeValue(resp.getWriter(), result);
    }
}
