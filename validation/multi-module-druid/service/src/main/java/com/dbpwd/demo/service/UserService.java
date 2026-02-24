package com.dbpwd.demo.service;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.demo.common.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class UserService {
    
    private DruidDataSource dataSource;
    
    public UserService(DruidDataSource dataSource) {
        this.dataSource = dataSource;
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
    
    public void close() {
        if (dataSource != null) {
            dataSource.close();
        }
    }
}
