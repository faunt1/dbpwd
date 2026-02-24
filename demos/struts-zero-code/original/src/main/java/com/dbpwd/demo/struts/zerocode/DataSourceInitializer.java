package com.dbpwd.demo.struts.zerocode;

import com.alibaba.druid.pool.DruidDataSource;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * DataSource 初始化器 - 原始版本（明文密码）
 * 与集成版本的区别：密码来自代码而非加密文件
 */
@WebListener
public class DataSourceInitializer implements ServletContextListener {

    private static final String DATASOURCE_KEY = "dataSource";

    private static final String H2_URL = "jdbc:h2:file:" + System.getProperty("user.home")
            + "/.dbpwd/h2_data/dbpwd_struts_original_demo;IFEXISTS=TRUE;MODE=MySQL";
    private static final String H2_USERNAME = "sa";
    private static final String H2_PASSWORD = "sa";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        System.out.println("[Original] Initializing DataSource with plain text password...");

        try {
            DruidDataSource dataSource = new DruidDataSource();
            dataSource.setUrl(H2_URL);
            dataSource.setUsername(H2_USERNAME);
            dataSource.setPassword(H2_PASSWORD);
            dataSource.setDriverClassName("org.h2.Driver");
            
            dataSource.setInitialSize(2);
            dataSource.setMaxActive(10);
            dataSource.setMaxWait(10000);
            dataSource.setTestWhileIdle(true);
            dataSource.setValidationQuery("SELECT 1");

            context.setAttribute(DATASOURCE_KEY, dataSource);
            System.out.println("[Original] DataSource initialized successfully");

        } catch (Exception e) {
            System.err.println("[Original] Failed to initialize DataSource: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        Object ds = context.getAttribute(DATASOURCE_KEY);

        if (ds instanceof DruidDataSource) {
            try {
                ((DruidDataSource) ds).close();
                System.out.println("[Original] DataSource closed");
            } catch (Exception e) {
                System.err.println("[Original] Error closing DataSource: " + e.getMessage());
            }
        }
    }
}
