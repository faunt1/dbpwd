package com.dbpwd.demo.servlet.api;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.demo.servlet.dao.UserDao;
import com.dbpwd.demo.servlet.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户API Servlet - ORIGINAL VERSION (before integration)
 *
 * 原始版本 - 使用明文密码，未集成密码加密功能
 * 此版本用于对比展示集成密码加密功能前后的代码差异
 *
 * 【集成改动点】
 * 1. 添加 import com.dbpwd.core.PasswordService;
 * 2. 在 init() 方法中:
 *    原始: String plainPassword = "sa";
 *    集成: String plainPassword = PasswordService.getDatabasePassword("@servlet_demo_db@");
 *
 * @author Database Password Manager
 * @version 1.0
 */
@WebServlet("/api/users/*")
public class UserApiServlet extends HttpServlet {

    private static final String H2_URL = "jdbc:h2:file:" + System.getProperty("user.home") + "/.dbpwd/h2_data/dbpwd_servlet_original_demo;MODE=MySQL;DB_CLOSE_DELAY=-1";
    private static final String H2_USERNAME = "sa";
    private static final String H2_DRIVER = "org.h2.Driver";

    private static final long serialVersionUID = 1L;

    private DruidDataSource dataSource;
    private UserDao userDao;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        System.out.println("=========================================");
        System.out.println("   Servlet Demo - ORIGINAL VERSION");
        System.out.println("   (使用明文密码，未集成密码加密)");
        System.out.println("=========================================");

        try {
            // 【原始版本】直接使用明文密码
            // 【集成改动】改为: String plainPassword = PasswordService.getDatabasePassword("@servlet_demo_db@");
            String plainPassword = "sa";
            System.out.println("WARNING: Using PLAINTEXT password!");

            // 创建Druid数据源
            dataSource = new DruidDataSource();
            dataSource.setUrl(H2_URL);
            dataSource.setUsername(H2_USERNAME);
            dataSource.setPassword(plainPassword);
            dataSource.setDriverClassName(H2_DRIVER);
            dataSource.setInitialSize(2);
            dataSource.setMaxActive(10);

            // 创建UserDao并初始化表
            userDao = new UserDao(dataSource);
            userDao.createTable();
            System.out.println("Users table initialized");

            objectMapper = new ObjectMapper();

            // 加载初始数据
            loadInitialData();

            System.out.println("Servlet initialized successfully");

        } catch (Exception e) {
            throw new ServletException("Failed to initialize servlet", e);
        }
    }

    private void loadInitialData() throws Exception {
        User user1 = new User();
        user1.setUsername("alice");
        user1.setEmail("alice@example.com");
        userDao.insert(user1);

        User user2 = new User();
        user2.setUsername("bob");
        user2.setEmail("bob@example.com");
        userDao.insert(user2);

        System.out.println("Loaded 2 demo users");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        resp.setContentType("application/json;charset=UTF-8");

        try {
            if (pathInfo == null || pathInfo.equals("/")) {
                List<User> users = userDao.findAll();
                objectMapper.writeValue(resp.getWriter(), users);
            } else {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Not found");
                objectMapper.writeValue(resp.getWriter(), error);
            }
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            objectMapper.writeValue(resp.getWriter(), error);
        }
    }

    @Override
    public void destroy() {
        if (dataSource != null) {
            dataSource.close();
        }
        System.out.println("Servlet destroyed");
    }
}
