package com.dbpwd.demo.servlet.api;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.core.PasswordService;
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

@WebServlet("/api/users/*")
public class UserApiServlet extends HttpServlet {

    private static final String H2_URL = "jdbc:h2:file:" + System.getProperty("user.home") + "/.dbpwd/h2_data/dbpwd_servlet_demo;IFEXISTS=TRUE;MODE=MySQL";
    private static final String H2_USERNAME = "sa";
    private static final String DB_PLACEHOLDER = "@servlet_demo_db@";
    private static final String H2_DRIVER = "org.h2.Driver";

    private static final long serialVersionUID = 1L;

    private DruidDataSource dataSource;
    private UserDao userDao;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        System.out.println("=========================================");
        System.out.println("   Servlet Demo - INTEGRATED VERSION");
        System.out.println("   (使用加密密码，已集成密码管理)");
        System.out.println("=========================================");

        try {
            String password = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
            System.out.println("INFO: Using encrypted password from ~/.dbpwd/db.pwd");

            dataSource = new DruidDataSource();
            dataSource.setUrl(H2_URL);
            dataSource.setUsername(H2_USERNAME);
            dataSource.setPassword(password);
            dataSource.setDriverClassName(H2_DRIVER);
            dataSource.setInitialSize(2);
            dataSource.setMaxActive(10);

            userDao = new UserDao(dataSource);
            objectMapper = new ObjectMapper();

            System.out.println("Servlet initialized successfully");

        } catch (Exception e) {
            throw new ServletException("Failed to initialize servlet", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            String pathInfo = req.getPathInfo();

            if (pathInfo == null || pathInfo.equals("/") || pathInfo.equals("")) {
                List<User> users = userDao.findAll();
                sendSuccessResponse(resp, users);
            } else if (pathInfo.matches("/\d+")) {
                Long id = Long.parseLong(pathInfo.substring(1));
                User user = userDao.findById(id);
                if (user != null) {
                    sendSuccessResponse(resp, user);
                } else {
                    sendErrorResponse(resp, 404, "User not found");
                }
            } else {
                sendErrorResponse(resp, 400, "Invalid request path");
            }

        } catch (Exception e) {
            sendErrorResponse(resp, 500, e.getMessage());
        }
    }

    private void sendSuccessResponse(HttpServletResponse resp, Object data) throws IOException {
        resp.setStatus(200);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", data);

        resp.getWriter().write(objectMapper.writeValueAsString(result));
    }

    private void sendErrorResponse(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", message);

        resp.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
