package com.dbpwd.demo.spring;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.demo.spring.dao.UserDao;
import com.dbpwd.spring.DataSourcePasswordProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Demo应用Spring配置类 - INTEGRATED VERSION (with db-pwd-spring)
 *
 * 集成版本 - 使用占位符密码，自动解密
 *
 * 【集成改动】
 * 1. 添加 db-pwd-spring 依赖
 * 2. 使用占位符 @spring_framework_db@
 * 3. DataSourcePasswordProcessor 自动解密密码
 *
 * @author Database Password Manager
 * @version 1.0
 */
@Configuration
public class DemoConfig {

    private static final Logger logger = LoggerFactory.getLogger(DemoConfig.class);

    private static final String DB_PLACEHOLDER = "@spring_framework_db@";

    /**
     * 配置Druid DataSource - 使用占位符密码
     */
    @Bean
    public DataSource dataSource() {
        logger.info("Configuring Druid dataSource with encrypted password placeholder...");

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:file:" + System.getProperty("user.home") + "/.dbpwd/h2_data/dbpwd_spring_demo;IFEXISTS=TRUE;MODE=MySQL");
        dataSource.setUsername("sa");
        // 【集成版本】使用占位符密码，DataSourcePasswordProcessor会自动解密
        dataSource.setPassword(DB_PLACEHOLDER);
        dataSource.setDriverClassName("org.h2.Driver");

        dataSource.setInitialSize(2);
        dataSource.setMaxActive(10);
        dataSource.setMinIdle(2);
        dataSource.setMaxWait(60000);

        logger.info("Druid dataSource configured with URL: {}", dataSource.getUrl());
        logger.info("Password: encrypted (placeholder: {})", DB_PLACEHOLDER);
        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean
    public UserDao userDao(JdbcTemplate jdbcTemplate) {
        return new UserDao(jdbcTemplate);
    }

    @Bean
    public DataSourcePasswordProcessor dataSourcePasswordProcessor() {
        return new DataSourcePasswordProcessor();
    }
}
