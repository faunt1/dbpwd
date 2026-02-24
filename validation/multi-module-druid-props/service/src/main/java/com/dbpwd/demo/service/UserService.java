package com.dbpwd.demo.service;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.demo.common.User;
import java.sql.*;
import java.util.*;

public class UserService {
    private DruidDataSource dataSource;
    
    public UserService(DruidDataSource dataSource) { this.dataSource = dataSource; }
    
    public static DruidDataSource createDataSourceFromProperties(Properties props) {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl(props.getProperty("db.url"));
        ds.setUsername(props.getProperty("db.username"));
        ds.setPassword(props.getProperty("db.password"));
        ds.setDriverClassName(props.getProperty("db.driver", "org.h2.Driver"));
        ds.setInitialSize(2);
        ds.setMaxActive(10);
        return ds;
    }
    
    public List<User> findAllUsers() throws Exception {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, username, email, password FROM users ORDER BY id LIMIT 20";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getLong("id"));
                user.setUsername(rs.getString("username"));
                user.setEmail(rs.getString("email"));
                user.setPassword(rs.getString("password"));
                users.add(user);
            }
        }
        return users;
    }
}
