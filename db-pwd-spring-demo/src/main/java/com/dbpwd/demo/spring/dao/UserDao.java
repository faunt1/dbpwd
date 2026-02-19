package com.dbpwd.demo.spring.dao;

import com.dbpwd.demo.spring.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Date;
import java.util.List;

/**
 * 用户数据访问对象（DAO）
 *
 * 演示使用JdbcTemplate进行CRUD操作
 *
 * @author Database Password Manager
 * @version 5.3
 */
@Repository
public class UserDao {

    private static final Logger logger = LoggerFactory.getLogger(UserDao.class);

    private final JdbcTemplate jdbcTemplate;

    public UserDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 创建数据库表
     */
    public void createTables() {
        String sql = "CREATE TABLE IF NOT EXISTS users (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(50) NOT NULL UNIQUE, " +
                "email VARCHAR(100) NOT NULL, " +
                "password VARCHAR(255) NOT NULL, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        jdbcTemplate.execute(sql);
        logger.info("Created users table");
    }

    /**
     * 插入用户
     */
    public Long insert(User user) {
        String sql = "INSERT INTO users (username, email, password, created_at) VALUES (?, ?, ?, ?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setTimestamp(4, new java.sql.Timestamp(new Date().getTime()));
            return ps;
        }, keyHolder);

        // 使用getKeysAsMap来获取ID
        Number id = (Number) keyHolder.getKeys().get("id");
        user.setId(id.longValue());
        logger.debug("Inserted user: {}", user.getUsername());
        return id.longValue();
    }

    /**
     * 根据ID查询用户
     */
    public User findById(Long id) {
        String sql = "SELECT id, username, email, password, created_at FROM users WHERE id = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(User.class), id);
        } catch (Exception e) {
            logger.warn("User not found with ID: {}", id);
            return null;
        }
    }

    /**
     * 查询所有用户
     */
    public List<User> findAll() {
        String sql = "SELECT id, username, email, password, created_at FROM users ORDER BY id";
        return jdbcTemplate.query(sql, new BeanPropertyRowMapper<>(User.class));
    }

    /**
     * 根据用户名查询用户
     */
    public User findByUsername(String username) {
        String sql = "SELECT id, username, email, password, created_at FROM users WHERE username = ?";
        try {
            return jdbcTemplate.queryForObject(sql, new BeanPropertyRowMapper<>(User.class), username);
        } catch (Exception e) {
            logger.warn("User not found with username: {}", username);
            return null;
        }
    }

    /**
     * 更新用户
     */
    public int update(User user) {
        String sql = "UPDATE users SET username = ?, email = ?, password = ? WHERE id = ?";
        int updated = jdbcTemplate.update(sql, user.getUsername(), user.getEmail(),
                user.getPassword(), user.getId());
        if (updated > 0) {
            logger.debug("Updated user: {}", user.getUsername());
        }
        return updated;
    }

    /**
     * 删除用户
     */
    public int deleteById(Long id) {
        String sql = "DELETE FROM users WHERE id = ?";
        int deleted = jdbcTemplate.update(sql, id);
        if (deleted > 0) {
            logger.debug("Deleted user with ID: {}", id);
        }
        return deleted;
    }

    /**
     * 删除所有用户
     */
    public int deleteAll() {
        String sql = "DELETE FROM users";
        int deleted = jdbcTemplate.update(sql);
        logger.debug("Deleted all users: {} row(s)", deleted);
        return deleted;
    }

    /**
     * 统计用户数量
     */
    public int count() {
        String sql = "SELECT COUNT(*) FROM users";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count != null ? count : 0;
    }
}
