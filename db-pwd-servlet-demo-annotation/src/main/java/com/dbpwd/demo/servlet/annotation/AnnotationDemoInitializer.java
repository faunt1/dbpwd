package com.dbpwd.demo.servlet.annotation;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.core.PasswordService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.sql.Connection;
import java.sql.Statement;

/**
 * Servlet注解方式初始化监听器
 *
 * 使用 @WebListener 注解替代 web.xml 配置
 * 在 Servlet 3.0+ 容器中自动注册
 *
 * 工作机制:
 * 1. 容器启动时自动扫描 @WebListener 注解
 * 2. 调用 contextInitialized() 方法
 * 3. 使用 PasswordService API 初始化 DataSource
 *
 * 与 web.xml 方式的区别:
 * - 无需 web.xml 配置文件
 * - 使用 Java 注解配置
 * - 更现代的配置方式
 * - Servlet 3.0+ 支持
 */
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
            // 使用 PasswordService API 解密密码
            String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
            logger.info("Password retrieved for placeholder: {}", DB_PLACEHOLDER);

            // 创建 Druid DataSource
            dataSource = new DruidDataSource();
            dataSource.setUrl("jdbc:h2:file:~/.dbpwd/h2_data/servlet_demo_annotation_db;MODE=MySQL");
            dataSource.setUsername("sa");
            dataSource.setPassword(plainPassword);
            dataSource.setInitialSize(2);
            dataSource.setMaxActive(10);

            // 初始化数据库表
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                        "id INT AUTO_INCREMENT PRIMARY KEY, " +
                        "name VARCHAR(100) NOT NULL, " +
                        "price DECIMAL(10,2) NOT NULL)");
                logger.info("Database table created successfully");
            }

            // 将 DataSource 存入 ServletContext
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
