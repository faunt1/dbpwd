package com.dbpwd.demo.servlet.annotation;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.core.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class AnnotationDemoInitializer implements ServletContextListener {

    private static final Logger logger = LoggerFactory.getLogger(AnnotationDemoInitializer.class);

    private static final String DB_PLACEHOLDER = "@servlet_demo_annotation_db@";

    private DruidDataSource dataSource;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("========================================");
        logger.info("   Servlet Annotation Demo - Initializing");
        logger.info("========================================");
        logger.info("Integration Mode: Servlet @WebListener");
        logger.info("Configuration: Java Annotations (no web.xml)");

        try {
            String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
            logger.info("Password retrieved for placeholder: {}", DB_PLACEHOLDER);

            dataSource = new DruidDataSource();
            dataSource.setUrl("jdbc:h2:file:" + System.getProperty("user.home") + "/.dbpwd/h2_data/dbpwd_servlet_annotation_demo;IFEXISTS=TRUE;MODE=MySQL");
            dataSource.setUsername("sa");
            dataSource.setPassword(plainPassword);
            dataSource.setInitialSize(2);
            dataSource.setMaxActive(10);

            sce.getServletContext().setAttribute("dataSource", dataSource);

            logger.info("========================================");
            logger.info("   Initialization completed successfully");
            logger.info("========================================");

        } catch (Exception e) {
            logger.error("Initialization failed", e);
            throw new RuntimeException("Failed to initialize datasource", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        if (dataSource != null) {
            dataSource.close();
            logger.info("DataSource closed");
        }
    }
}
