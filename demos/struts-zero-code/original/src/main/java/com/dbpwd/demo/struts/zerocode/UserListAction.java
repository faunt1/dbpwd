package com.dbpwd.demo.struts.zerocode;

import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户列表Action - 原始版本（未集成密码加密）
 *
 * 【原始版本说明】
 * - 使用明文密码：DB_PASSWORD = "sa"
 * - 从 ServletContext 获取共享 DataSource（符合 Struts 最佳实践）
 * - 密码硬编码在代码中
 * - 不符合安全最佳实践（密码明文）
 *
 * 【与集成版本对比】
 * - 两者都从 ServletContext 获取 DataSource
 * - 区别：密码来源不同（明文 vs 加密）
 *
 * @author Database Password Manager
 * @version 1.0 - 原始版本
 */
public class UserListAction extends Action {

    private static final String SELECT_USERS_SQL =
            "SELECT id, username, email, status FROM users ORDER BY id LIMIT 20";

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
                                   HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        // 从 ServletContext 获取共享 DataSource（符合 Struts 最佳实践）
        DataSource dataSource = (DataSource) getServlet().getServletContext()
                .getAttribute("dataSource");

        if (dataSource == null) {
            throw new IllegalStateException("DataSource not found in ServletContext. " +
                    "Ensure DataSourceInitializer is configured.");
        }

        List<Map<String, Object>> users = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(SELECT_USERS_SQL)) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> user = new HashMap<>();
                user.put("id", rs.getLong("id"));
                user.put("username", rs.getString("username"));
                user.put("email", rs.getString("email"));
                user.put("status", rs.getString("status"));
                users.add(user);
            }

        } catch (Exception e) {
            System.err.println("Error fetching users: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }

        request.setAttribute("users", users);
        return mapping.findForward("success");
    }
}
