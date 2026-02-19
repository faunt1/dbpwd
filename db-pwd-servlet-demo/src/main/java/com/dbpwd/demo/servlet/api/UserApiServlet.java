package com.dbpwd.demo.servlet.api;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.druid.pool.DruidPooledConnection;
import com.dbpwd.core.Sm4KeyLoader;
import com.dbpwd.crypto.Sm4GCMCipher;
import com.dbpwd.crypto.ENCFormat;
import com.dbpwd.demo.servlet.dao.UserDao;
import com.dbpwd.demo.servlet.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户API Servlet
 */
@WebServlet("/api/users/*")
public class UserApiServlet extends HttpServlet {

    private static final String H2_URL = "jdbc:h2:file:" + System.getProperty("user.home") + "/.dbpwd/h2_data/dbpwd_servlet_demo;MODE=MySQL;DB_CLOSE_DELAY=-1";
    private static final String H2_USERNAME = "sa";
    private static final String H2_DRIVER = "org.h2.Driver";

    private static final long serialVersionUID = 1L;

    private DruidDataSource dataSource;
    private UserDao userDao;
    private ObjectMapper objectMapper;

    @Override
    public void init() throws ServletException {
        System.out.println("=========================================");
        System.out.println("   Database Password Manager - Servlet Demo");
        System.out.println("=========================================");

        try {
            // 1. 创建示例密钥文件（仅用于Demo）
            String demoKeyPath = System.getProperty("user.home") + "/.dbpwd/demo_servlet_sm4.key";
            String sm4Key = Sm4KeyLoader.generateKey(demoKeyPath, true);
            System.out.println("Demo key file created: " + demoKeyPath);

            // 2. 加密H2密码
            String h2Password = "sa";
            String cipherText = Sm4GCMCipher.encrypt(h2Password, sm4Key);
            String encPassword = ENCFormat.formatSM4GCM(cipherText);
            System.out.println("H2 password encrypted: " + encPassword.substring(0, Math.min(50, encPassword.length())) + "...");

            // 3. 解密密码（演示流程）
            String plainPassword = decryptPassword(encPassword, sm4Key);
            System.out.println("H2 password decrypted successfully");

            // 4. 创建Druid数据源
            dataSource = new DruidDataSource();
            dataSource.setUrl(H2_URL);
            dataSource.setUsername(H2_USERNAME);
            dataSource.setPassword(plainPassword);
            dataSource.setDriverClassName(H2_DRIVER);
            dataSource.setInitialSize(2);
            dataSource.setMaxActive(10);

            // 5. 创建UserDao并初始化表
            userDao = new UserDao(dataSource);
            userDao.createTable();
            System.out.println("Users table initialized");

            // 6. 初始化JSON对象映射器
            objectMapper = new ObjectMapper();

            // 7. 加载一些初始数据用于演示
            loadInitialData();

            System.out.println("=========================================");
            System.out.println("   Servlet Demo initialized successfully!");
            System.out.println("   Access: http://localhost:8081/dbpwd-servlet-demo/api/users");
            System.out.println("=========================================");

        } catch (Exception e) {
            throw new ServletException("Failed to initialize Servlet Demo", e);
        }
    }

    @Override
    public void destroy() {
        if (dataSource != null) {
            try {
                dataSource.close();
            } catch (Exception e) {
                // ignore
            }
        }
        System.out.println("[DbPwd] Servlet Demo destroyed");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            String pathInfo = req.getPathInfo();

            if (pathInfo == null || pathInfo.equals("/") || pathInfo.equals("")) {
                // 获取所有用户
                List<User> users = userDao.findAll();
                sendSuccessResponse(resp, users);
            } else if (pathInfo.matches("/\\d+")) {
                // 获取指定ID的用户
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

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            User user = objectMapper.readValue(req.getReader(), User.class);
            User created = userDao.insert(user);
            sendSuccessResponse(resp, created);

        } catch (Exception e) {
            sendErrorResponse(resp, 500, e.getMessage());
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || !pathInfo.matches("/\\d+")) {
                sendErrorResponse(resp, 400, "Invalid request path");
                return;
            }

            Long id = Long.parseLong(pathInfo.substring(1));
            User user = objectMapper.readValue(req.getReader(), User.class);
            user.setId(id);

            boolean updated = userDao.update(user);
            if (updated) {
                User updatedUser = userDao.findById(id);
                sendSuccessResponse(resp, updatedUser);
            } else {
                sendErrorResponse(resp, 404, "User not found");
            }

        } catch (Exception e) {
            sendErrorResponse(resp, 500, e.getMessage());
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json;charset=UTF-8");

        try {
            String pathInfo = req.getPathInfo();
            if (pathInfo == null || !pathInfo.matches("/\\d+")) {
                sendErrorResponse(resp, 400, "Invalid request path");
                return;
            }

            Long id = Long.parseLong(pathInfo.substring(1));
            boolean deleted = userDao.delete(id);

            if (deleted) {
                Map<String, Object> result = new HashMap<>();
                result.put("success", true);
                result.put("message", "User deleted successfully");
                sendSuccessResponse(resp, result);
            } else {
                sendErrorResponse(resp, 404, "User not found");
            }

        } catch (Exception e) {
            sendErrorResponse(resp, 500, e.getMessage());
        }
    }

    /**
     * 加载初始数据
     */
    private void loadInitialData() throws Exception {
        try {
            userDao.insert(new User("john_doe", "john@example.com", "password123"));
            userDao.insert(new User("jane_smith", "jane@example.com", "password456"));
            userDao.insert(new User("bob_wilson", "bob@example.com", "password789"));
            System.out.println("  Initial data loaded: 3 users created");
        } catch (Exception e) {
            System.out.println("  Warning: Failed to load initial data (may already exist)");
        }
    }

    /**
     * 解密密码
     */
    private String decryptPassword(String encryptedPassword, String sm4Key) throws Exception {
        String algorithm = ENCFormat.extractAlgorithm(encryptedPassword);
        if (!"SM4-GCM".equalsIgnoreCase(algorithm)) {
            throw new IllegalArgumentException(
                "不支持的加密算法: " + algorithm + "。仅支持SM4-GCM格式");
        }

        String cipherText = ENCFormat.extractCipherText(encryptedPassword);
        return Sm4GCMCipher.decrypt(cipherText, sm4Key);
    }

    /**
     * 发送成功响应
     */
    private void sendSuccessResponse(HttpServletResponse resp, Object data) throws IOException {
        resp.setStatus(200);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("data", data);

        PrintWriter writer = resp.getWriter();
        writer.write(objectMapper.writeValueAsString(result));
        writer.flush();
    }

    /**
     * 发送错误响应
     */
    private void sendErrorResponse(HttpServletResponse resp, int status, String message) throws IOException {
        resp.setStatus(status);
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", message);

        PrintWriter writer = resp.getWriter();
        writer.write(objectMapper.writeValueAsString(result));
        writer.flush();
    }
}
