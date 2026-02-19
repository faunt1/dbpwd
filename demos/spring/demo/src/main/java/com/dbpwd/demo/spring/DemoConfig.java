package com.dbpwd.demo.spring;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.core.PasswordService;
import com.dbpwd.demo.spring.dao.UserDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Demo应用Spring配置类
 *
 * 配置数据源，使用PasswordService获取密码
 *
 * @author Database Password Manager
 * @version 5.3
 */
@Configuration
public class DemoConfig {

    private static final Logger logger = LoggerFactory.getLogger(DemoConfig.class);

    private static final String DB_PLACEHOLDER = "@spring_framework_db@";

    /**
     * 配置Druid DataSource
     *
     * 使用PasswordService获取解密后的密码
     *
     * @return Druid数据源
     */
    @Bean
    public DataSource dataSource() {
        logger.info("Configuring Druid dataSource...");

        String plainPassword;
        try {
            plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
            logger.info("Password retrieved for placeholder: {}", DB_PLACEHOLDER);
        } catch (Exception e) {
            logger.error("Failed to get password", e);
            throw new RuntimeException("Failed to get database password", e);
        }

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:file:" + System.getProperty("user.home") + "/.dbpwd/h2_data/dbpwd_spring_demo;IFEXISTS=TRUE;MODE=MySQL");
        dataSource.setUsername("sa");
        dataSource.setPassword(plainPassword);
        dataSource.setDriverClassName("org.h2.Driver");

        dataSource.setInitialSize(2);
        dataSource.setMaxActive(10);
        dataSource.setMinIdle(2);
        dataSource.setMaxWait(60000);

        logger.info("Druid dataSource configured with URL: {}", dataSource.getUrl());
        return dataSource;
    }

    /**
     * 配置JdbcTemplate
     *
     * @return JdbcTemplate实例
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        logger.info("JdbcTemplate bean configured");
        return template;
    }

    /**
     * 配置UserDao
     *
     * @return UserDao实例
     */
    @Bean
    public UserDao userDao(JdbcTemplate jdbcTemplate) {
        UserDao dao = new UserDao(jdbcTemplate);
        logger.info("UserDao bean configured");
        return dao;
    }
}
