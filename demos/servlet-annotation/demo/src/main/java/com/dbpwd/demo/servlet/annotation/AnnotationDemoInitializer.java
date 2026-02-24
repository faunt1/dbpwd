package com.dbpwd.demo.servlet.annotation;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.web.WebPasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Servlet注解方式初始化监听器 - 方案6: ContextListener 自动解密
 *
 * 使用 @WebListener 注解替代 web.xml 配置
 * 在 Servlet 3.0+ 容器中自动注册
 *
 * 集成方式：
 * - 通过 WebPasswordUtil.getPlainPassword() 获取密码
 * - 密码在容器启动时预加载
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
        logger.info("   方案6: ContextListener 自动解密");
        logger.info("========================================");

        try {
            // 方案6: 通过 WebPasswordUtil 获取密码 (自动解密)
            String plainPassword = WebPasswordUtil.getPlainPassword(sce.getServletContext(), DB_PLACEHOLDER);
            logger.info("Password retrieved via WebPasswordUtil for: {}", DB_PLACEHOLDER);

            // 创建 Druid DataSource
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
