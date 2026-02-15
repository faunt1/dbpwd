package com.dbpwd.demo.springboot.api;

import com.dbpwd.core.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

/**
 * Spring Boot 编程API方式Demo
 *
 * 集成模式: 方案3 - Spring Boot
 * 配置方式: 编程API (手动配置DataSource)
 *
 * 工作机制:
 * 1. 不使用Spring Boot Starter自动配置
 * 2. 手动创建DataSource Bean
 * 3. 使用PasswordService.getDatabasePassword() API解密密码
 * 4. 完全控制密码管理流程
 *
 * 适用场景:
 * - 需要完全控制DataSource配置
 * - 不想使用自动配置
 * - 需要在配置过程中执行额外逻辑
 *
 * @author Database Password Manager
 * @version 1.0
 */
@SpringBootApplication
public class SpringBootApiDemoApplication {

    private static final Logger logger = LoggerFactory.getLogger(SpringBootApiDemoApplication.class);

    private static final String DB_PLACEHOLDER = "@spring_boot_api_db@";

    public static void main(String[] args) {
        SpringApplication.run(SpringBootApiDemoApplication.class, args);
    }

    /**
     * 手动配置DataSource - 使用编程API获取密码
     *
     * 这种方式让开发者完全控制:
     * 1. 何时获取密码
     * 2. 如何配置DataSource
     * 3. 添加自定义逻辑
     */
    @Bean
    public DataSource dataSource() {
        logger.info("========================================");
        logger.info("   Configuring DataSource with PasswordService API");
        logger.info("========================================");

        try {
            // 使用PasswordService API解密密码
            // 这会自动读取 ~/.dbpwd/db.pwd 和 ~/.dbpwd/sm4.key
            String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
            logger.info("Password retrieved for placeholder: {}", DB_PLACEHOLDER);

            // 手动创建DataSource
            DriverManagerDataSource dataSource = new DriverManagerDataSource();
            dataSource.setDriverClassName("org.h2.Driver");
            dataSource.setUrl("jdbc:h2:file:~/.dbpwd/h2_data/spring_boot_api_db;MODE=MySQL");
            dataSource.setUsername("sa");
            dataSource.setPassword(plainPassword);

            logger.info("DataSource configured successfully");
            logger.info("========================================");

            return dataSource;
        } catch (Exception e) {
            logger.error("Failed to configure DataSource", e);
            throw new RuntimeException("Failed to configure DataSource", e);
        }
    }

    /**
     * JdbcTemplate for database operations
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
