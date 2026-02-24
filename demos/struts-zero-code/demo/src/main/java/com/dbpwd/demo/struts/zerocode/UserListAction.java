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
 * 用户列表Action - 零代码集成示例
 *
 * <b>零代码集成的关键</b>：
 * 1. 不创建DataSource
 * 2. 不调用密码解密API
 * 3. 直接从ServletContext获取已装饰的DataSource
 * 4. 所有密码解密由DataSourceDecorator自动完成
 *
 * <pre>
 * 与原始代码对比：
 *
 * 【原始方式 - 需要代码修改】
 * DruidDataSource dataSource = new DruidDataSource();
 * dataSource.setPassword("sa");  // ← 明文密码
 * String pwd = WebPasswordUtil.getPlainPassword(context, "@db@");
 * dataSource.setPassword(pwd);  // ← 需要修改代码调用解密API
 * conn = dataSource.getConnection();
 *
 * 【零代码方式 - 无需代码修改】
 * DataSource ds = (DataSource) getServlet().getServletContext().getAttribute("dataSource");
 * conn = ds.getConnection();  // ← 自动解密，无需任何额外代码
 * </pre>
 *
 * @author Database Password Manager
 * @version 6.0 - 零代码集成版
 */
public class UserListAction extends Action {

    private static final String SELECT_USERS_SQL =
            "SELECT id, username, email, status FROM users ORDER BY id LIMIT 20";

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
                                   HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        // ========== 零代码集成的关键 ==========
        // 直接从ServletContext获取已装饰的DataSource
        // 密码解密由DataSourceDecorator自动完成，无需任何额外代码
        DataSource dataSource = (DataSource) getServlet().getServletContext().getAttribute("dataSource");

        if (dataSource == null) {
            throw new IllegalStateException("DataSource not found in ServletContext. " +
                    "Ensure DbPwdStrutsInitializer is properly configured.");
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
