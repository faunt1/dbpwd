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

@SpringBootApplication
public class SpringBootApiDemoApplication {

    private static final Logger logger = LoggerFactory.getLogger(SpringBootApiDemoApplication.class);
    private static final String DB_PLACEHOLDER = "@spring_boot_api_db@";

    public static void main(String[] args) {
        SpringApplication.run(SpringBootApiDemoApplication.class, args);
    }

    @Bean
    public DataSource dataSource() throws Exception {
        logger.info("========================================");
        logger.info("   Configuring DataSource with encrypted password");
        logger.info("========================================");

        String password = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
        logger.info("Using encrypted password from ~/.dbpwd/db.pwd");

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:file:" + System.getProperty("user.home") + "/.dbpwd/h2_data/dbpwd_spring_boot_api_demo;IFEXISTS=TRUE;MODE=MySQL");
        dataSource.setUsername("sa");
        dataSource.setPassword(password);

        logger.info("DataSource configured successfully");
        logger.info("========================================");

        return dataSource;
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
