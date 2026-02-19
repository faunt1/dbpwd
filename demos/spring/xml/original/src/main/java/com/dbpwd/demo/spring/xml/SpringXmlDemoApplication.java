package com.dbpwd.demo.spring.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

public class SpringXmlDemoApplication {

    private static final Logger logger = LoggerFactory.getLogger(SpringXmlDemoApplication.class);

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("   Database Password Manager - Spring XML Demo");
        logger.info("========================================");
        logger.info("");
        logger.info("Mode: Original (plain text password)");
        logger.info("Configuration: Spring XML (applicationContext.xml)");
        logger.info("");

        try {
            logger.info("Loading Spring configuration...");
            ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                    "applicationContext.xml");

            DataSource dataSource = context.getBean(DataSource.class);
            logger.info("DataSource initialized successfully");
            logger.info("Using plain text password");

            logger.info("");
            logger.info("Testing database connection...");
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM orders");
                if (rs.next()) {
                    logger.info("Orders count: {}", rs.getInt(1));
                }

                logger.info("");
                logger.info("========================================");
                logger.info("   Demo completed successfully!");
                logger.info("========================================");
                logger.info("");
                logger.info("Key points demonstrated:");
                logger.info("- Spring DataSource with plain text password");
                logger.info("- Works with Spring XML configuration");
            }

            context.close();

        } catch (Exception e) {
            logger.error("Demo failed", e);
            System.exit(1);
        }
    }
}
