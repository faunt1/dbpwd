package com.dbpwd.demo.spring.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Spring XML Demo - INTEGRATED VERSION (with db-pwd)
 *
 * 集成版本 - 使用 db-pwd-spring 自动解密密码
 *
 * 【集成改动】
 * 1. 添加 db-pwd-spring 依赖
 * 2. applicationContext.xml 中配置 DataSourcePasswordProcessor
 * 3. 密码使用占位符 @spring_demo_xml_db@
 *
 * @author Database Password Manager
 * @version 2.0
 */
public class SpringXmlDemoApplication {

    private static final Logger logger = LoggerFactory.getLogger(SpringXmlDemoApplication.class);

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("   Database Password Manager - Spring XML Demo");
        logger.info("========================================");
        logger.info("");
        logger.info("Mode: INTEGRATED (encrypted password)");
        logger.info("Configuration: Spring XML (applicationContext.xml)");
        logger.info("");

        try {
            logger.info("Loading Spring configuration...");
            ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                    "applicationContext.xml");

            DataSource dataSource = context.getBean(DataSource.class);
            logger.info("DataSource initialized successfully");
            logger.info("Using encrypted password (auto-decrypted)");

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
                logger.info("- Spring DataSource with encrypted password");
                logger.info("- DataSourcePasswordProcessor auto-decrypts password");
                logger.info("- Works with Spring XML configuration");
            }

            context.close();

        } catch (Exception e) {
            logger.error("Demo failed", e);
            System.exit(1);
        }
    }
}
