package com.dbpwd.demo.servlet.dao;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.demo.servlet.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * User数据访问对象
 *
 * 注意: 数据库表和初始数据由 DatabaseInitializer 预先创建
 */
public class UserDao {
    private final DruidDataSource dataSource;

    public UserDao(DruidDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public User findById(Long id) throws Exception {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setLong(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<User> findAll() throws Exception {
        String sql = "SELECT * FROM users ORDER BY id";
        List<User> users = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapRow(rs));
            }
        }
        return users;
    }

    private User mapRow(ResultSet rs) throws Exception {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        return user;
    }
}
