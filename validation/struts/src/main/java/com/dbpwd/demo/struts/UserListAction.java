package com.dbpwd.demo.struts;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.core.PasswordService;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserListAction extends Action {

    private static final String H2_URL = "jdbc:h2:file:" + System.getProperty("user.home") + "/.dbpwd/h2_data/dbpwd_struts_demo;IFEXISTS=TRUE;MODE=MySQL";
    private static final String H2_USERNAME = "sa";
    private static final String DB_PLACEHOLDER = "@struts_demo_db@";
    private static final String H2_DRIVER = "org.h2.Driver";

    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {

        String password = PasswordService.getDatabasePassword(DB_PLACEHOLDER);

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(H2_URL);
        dataSource.setUsername(H2_USERNAME);
        dataSource.setPassword(password);
        dataSource.setDriverClassName(H2_DRIVER);
        dataSource.setInitialSize(2);
        dataSource.setMaxActive(10);

        List<Map<String, Object>> users = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT id, username, email FROM users ORDER BY id LIMIT 10")) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Map<String, Object> user = new HashMap<>();
                user.put("id", rs.getInt("id"));
                user.put("username", rs.getString("username"));
                user.put("email", rs.getString("email"));
                users.add(user);
            }
        } finally {
            dataSource.close();
        }

        request.setAttribute("users", users);
        return mapping.findForward("success");
    }
}
