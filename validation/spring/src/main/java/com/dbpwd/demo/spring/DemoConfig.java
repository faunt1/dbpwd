package com.dbpwd.demo.spring;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.demo.spring.dao.UserDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Demo应用Spring配置类 - ORIGINAL VERSION (before integration)
 *
 * 原始版本 - 使用明文密码，未集成密码加密功能
 * 此版本用于对比展示集成密码加密功能前后的代码差异
 *
 * 【集成改动点】
 * 1. 添加 import com.dbpwd.spring.DataSourcePasswordProcessor;
 * 2. 添加 dataSourcePasswordProcessor() Bean 方法
 * 3. 将 setPassword("sa") 改为 setPassword("ENC(SM4-GCM:...)")
 *
 * @author Database Password Manager
 * @version 1.0
 */
@Configuration
public class DemoConfig {

    private static final Logger logger = LoggerFactory.getLogger(DemoConfig.class);

    /**
     * 配置Druid DataSource - 使用明文密码
     *
     * 【集成改动】
     * 原始: dataSource.setPassword("sa");
     * 集成: dataSource.setPassword("ENC(SM4-GCM:...)");
     *       并添加 DataSourcePasswordProcessor Bean
     */
    @Bean
    public DataSource dataSource() {
        logger.info("Configuring Druid dataSource with PLAINTEXT password...");

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:file:" + System.getProperty("user.home") + "/.dbpwd/h2_data/dbpwd_spring_demo;IFEXISTS=TRUE;MODE=MySQL");
        dataSource.setUsername("sa");
        // 【原始版本】直接使用明文密码
        dataSource.setPassword("sa");
        dataSource.setDriverClassName("org.h2.Driver");

        dataSource.setInitialSize(2);
        dataSource.setMaxActive(10);
        dataSource.setMinIdle(2);
        dataSource.setMaxWait(60000);

        logger.info("Druid dataSource configured with URL: {}", dataSource.getUrl());
        logger.warn("WARNING: Using PLAINTEXT password!");
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
}
