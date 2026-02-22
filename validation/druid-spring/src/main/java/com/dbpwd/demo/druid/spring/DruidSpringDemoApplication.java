package com.dbpwd.demo.druid.spring;

import com.alibaba.druid.pool.DruidDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class DruidSpringDemoApplication {

    private static final Logger logger = LoggerFactory.getLogger(DruidSpringDemoApplication.class);

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("   Database Password Manager - Druid Spring XML Demo");
        logger.info("========================================");
        logger.info("");
        logger.info("Mode: INTEGRATED (encrypted password)");
        logger.info("Configuration: Spring XML (druid-datasource.xml)");
        logger.info("");

        try {
            logger.info("Loading Spring configuration...");
            ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                    "druid-datasource.xml");

            DruidDataSource dataSource = context.getBean(DruidDataSource.class);
            
            logger.info("DataSource initialized");
            logger.info("Using encrypted password (auto-decrypted)");

            logger.info("");
            logger.info("Testing database connection...");
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM products");
                if (rs.next()) {
                    logger.info("Products count: {}", rs.getInt(1));
                }

                logger.info("");
                logger.info("========================================");
                logger.info("   Demo completed successfully!");
                logger.info("========================================");
                logger.info("");
                logger.info("Key points demonstrated:");
                logger.info("- Druid DataSource with encrypted password");
                logger.info("- Works with Spring XML configuration");
                logger.info("- DataSourcePasswordProcessor auto-decrypts");
            }

            context.close();

        } catch (Exception e) {
            logger.error("Demo failed", e);
            System.exit(1);
        }
    }
}
