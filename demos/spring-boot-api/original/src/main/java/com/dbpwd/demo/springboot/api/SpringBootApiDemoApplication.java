package com.dbpwd.demo.springboot.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * Spring Boot 编程API方式Demo - ORIGINAL VERSION
 *
 * 原始版本 - 使用明文密码，未集成密码加密功能
 *
 * 【集成改动点】
 * 1. 移除 import com.dbpwd.core.PasswordService;
 * 2. 在 dataSource() 方法中:
 *    原始: String plainPassword = "sa";
 *    集成: String plainPassword = PasswordService.getDatabasePassword("@spring_boot_api_db@");
 *
 * @author Database Password Manager
 * @version 2.0
 */
@SpringBootApplication
public class SpringBootApiDemoApplication {

    private static final Logger logger = LoggerFactory.getLogger(SpringBootApiDemoApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SpringBootApiDemoApplication.class, args);
    }

    /**
     * 手动配置DataSource - 使用明文密码
     *
     * 原始版本: 直接使用明文密码
     */
    @Bean
    public DataSource dataSource() {
        logger.info("========================================");
        logger.info("   Configuring DataSource with plain text password");
        logger.info("========================================");

        // 直接使用明文密码
        String plainPassword = "sa";
        logger.info("Using plain text password: sa");

        // 手动创建DataSource
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:file:" + System.getProperty("user.home") + "/.dbpwd/h2_data/dbpwd_spring_boot_api_demo;IFEXISTS=TRUE;MODE=MySQL");
        dataSource.setUsername("sa");
        dataSource.setPassword(plainPassword);

        logger.info("DataSource configured successfully");
        logger.info("========================================");

        return dataSource;
    }

    /**
     * JdbcTemplate for database operations
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
