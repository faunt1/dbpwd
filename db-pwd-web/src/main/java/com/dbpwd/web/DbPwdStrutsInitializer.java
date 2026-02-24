package com.dbpwd.web;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.core.Sm4KeyLoader;
import com.dbpwd.framework.DataSourceDecorator;
import com.dbpwd.model.DatabaseEntry;
import com.dbpwd.core.Sm4KeyLoader;
import com.dbpwd.framework.DataSourceDecorator;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;

/**
 * Struts/Servlet DataSource 初始化监听器
 *
 * 在Web应用启动时创建并装饰DataSource，支持配置化数据库列表
 *
 * 使用方式：在web.xml中配置
 * <pre>
 * &lt;listener&gt;
 *   &lt;listener-class&gt;com.dbpwd.web.DbPwdStrutsInitializer&lt;/listener-class&gt;
 * &lt;/listener&gt;
 *
 * &lt;!-- 可选：配置要初始化的数据库（逗号分隔） --&gt;
 * &lt;context-param&gt;
 *   &lt;param-name&gt;dbpwd.databases&lt;/param-name&gt;
 *   &lt;param-value&gt;@db1@,@db2@&lt;/param-value&gt;
 * &lt;/context-param&gt;
 *
 * &lt;!-- 可选：自定义配置路径 --&gt;
 * &lt;context-param&gt;
 *   &lt;param-name&gt;dbpwd.keyFile&lt;/param-name&gt;
 *   &lt;param-value&gt;/path/to/sm4.key&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * &lt;context-param&gt;
 *   &lt;param-name&gt;dbpwd.pwdFile&lt;/param-name&gt;
 *   &lt;param-value&gt;/path/to/db.pwd&lt;/param-value&gt;
 * &lt;/context-param&gt;
 *
 * &lt;!-- 可选：DataSource属性配置 --&gt;
 * &lt;context-param&gt;
 *   &lt;param-name&gt;dbpwd.datasource.initialSize&lt;/param-name&gt;
 *   &lt;param-value&gt;2&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * &lt;context-param&gt;
 *   &lt;param-name&gt;dbpwd.datasource.maxActive&lt;/param-name&gt;
 *   &lt;param-value&gt;10&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 *
 * 应用中使用：
 * <pre>
 * DataSource ds = (DataSource) getServletContext().getAttribute("dataSource");
 * Connection conn = ds.getConnection();  // 自动解密
 * </pre>
 *
 * @author Database Password Manager
 * @version 6.0
 */
@WebListener
public class DbPwdStrutsInitializer implements ServletContextListener {

    // 参数名称
    private static final String DATABASES_PARAM = "dbpwd.databases";
    private static final String KEY_FILE_PARAM = "dbpwd.keyFile";
    private static final String PWD_FILE_PARAM = "dbpwd.pwdFile";
    private static final String DS_INITIAL_SIZE = "dbpwd.datasource.initialSize";
    private static final String DS_MAX_ACTIVE = "dbpwd.datasource.maxActive";
    private static final String DS_MAX_WAIT = "dbpwd.datasource.maxActive";

    // 默认DataSource属性
    private static final int DEFAULT_INITIAL_SIZE = 2;
    private static final int DEFAULT_MAX_ACTIVE = 10;
    private static final int DEFAULT_MAX_WAIT = 10000;

    // ServletContext中存储的key
    public static final String DATASOURCE_KEY = "dataSource";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        System.out.println("[DbPwd] Initializing DataSource Decorator...");

        try {
            String databasesParam = context.getInitParameter(DATABASES_PARAM);
            String keyFile = context.getInitParameter(KEY_FILE_PARAM);
            String pwdFile = context.getInitParameter(PWD_FILE_PARAM);

            if (keyFile == null || keyFile.isEmpty()) {
                keyFile = System.getProperty("user.home") + "/.dbpwd/sm4.key";
            }
            if (pwdFile == null || pwdFile.isEmpty()) {
                pwdFile = System.getProperty("user.home") + "/.dbpwd/db.pwd";
            }

            List<String> databaseIds = parseDatabaseList(databasesParam);

            if (databaseIds.isEmpty()) {
                System.out.println("[DbPwd] No databases configured, skipping DataSource initialization");
                return;
            }

            Sm4KeyLoader keyLoader = new Sm4KeyLoader(keyFile);
            String sm4Key = keyLoader.loadKey();

            for (String dbId : databaseIds) {
                createAndStoreDataSource(context, dbId, sm4Key);
            }

            System.out.println("[DbPwd] DataSource initialization completed for " + databaseIds.size() + " database(s)");

        } catch (Exception e) {
            System.err.println("[DbPwd] Failed to initialize DataSource: " + e.getMessage());
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
                System.out.println("[DbPwd] DataSource closed");
            } catch (Exception e) {
                System.err.println("[DbPwd] Error closing DataSource: " + e.getMessage());
            }
        }

        try {
            Enumeration<Driver> drivers = DriverManager.getDrivers();
            while (drivers.hasMoreElements()) {
                Driver driver = drivers.nextElement();
                if (driver != null) {
                    DriverManager.deregisterDriver(driver);
                }
            }
        } catch (SQLException e) {
            System.err.println("[DbPwd] Error deregistering drivers: " + e.getMessage());
        }

        System.out.println("[DbPwd] DataSource cleanup completed");
    }

    private List<String> parseDatabaseList(String param) {
        List<String> result = new ArrayList<>();
        if (param == null || param.isEmpty()) {
            return result;
        }

        String[] parts = param.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private void createAndStoreDataSource(ServletContext context, String dbId, String sm4Key) throws Exception {
        WebPasswordService service = getOrCreateService(context);

        DatabaseEntry entry = service.getDatabaseEntry(stripPlaceholder(dbId));

        DruidDataSource rawDs = new DruidDataSource();
        rawDs.setUrl(entry.getUrl());
        rawDs.setUsername(entry.getUsername());
        rawDs.setPassword(dbId);
        rawDs.setDriverClassName(getDriverClassName(entry.getUrl()));

        rawDs.setInitialSize(getIntParameter(context, DS_INITIAL_SIZE, DEFAULT_INITIAL_SIZE));
        rawDs.setMaxActive(getIntParameter(context, DS_MAX_ACTIVE, DEFAULT_MAX_ACTIVE));
        rawDs.setMaxWait(getIntParameter(context, DS_MAX_WAIT, DEFAULT_MAX_WAIT));
        rawDs.setTestWhileIdle(true);
        rawDs.setValidationQuery("SELECT 1");

        DataSourceDecorator decoratedDs = new DataSourceDecorator(rawDs, stripPlaceholder(dbId), sm4Key);

        context.setAttribute(DATASOURCE_KEY, decoratedDs);
        System.out.println("[DbPwd] DataSource created for: " + dbId);
    }

    private WebPasswordService getOrCreateService(ServletContext context) {
        WebPasswordService service = (WebPasswordService) context.getAttribute(
            WebPasswordService.PASSWORD_SERVICE_KEY);

        if (service == null) {
            synchronized (DbPwdStrutsInitializer.class) {
                service = (WebPasswordService) context.getAttribute(
                    WebPasswordService.PASSWORD_SERVICE_KEY);
                if (service == null) {
                    String keyFile = context.getInitParameter(KEY_FILE_PARAM);
                    String pwdFile = context.getInitParameter(PWD_FILE_PARAM);

                    if (keyFile == null || keyFile.isEmpty()) {
                        keyFile = System.getProperty("user.home") + "/.dbpwd/sm4.key";
                    }
                    if (pwdFile == null || pwdFile.isEmpty()) {
                        pwdFile = System.getProperty("user.home") + "/.dbpwd/db.pwd";
                    }

                    service = new WebPasswordService(keyFile, pwdFile);
                    context.setAttribute(WebPasswordService.PASSWORD_SERVICE_KEY, service);
                }
            }
        }
        return service;
    }

    private int getIntParameter(ServletContext context, String name, int defaultValue) {
        String value = context.getInitParameter(name);
        if (value != null && !value.isEmpty()) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.out.println("[DbPwd] Invalid integer for " + name + ": " + value);
            }
        }
        return defaultValue;
    }

    private String stripPlaceholder(String id) {
        if (id != null && id.startsWith("@") && id.endsWith("@") && id.length() > 2) {
            return id.substring(1, id.length() - 1);
        }
        return id;
    }

    private String getDriverClassName(String url) {
        if (url == null) {
            return "org.h2.Driver";
        }
        if (url.contains(":h2:")) {
            return "org.h2.Driver";
        }
        if (url.contains(":mysql:")) {
            return "com.mysql.cj.jdbc.Driver";
        }
        if (url.contains(":postgresql:")) {
            return "org.postgresql.Driver";
        }
        if (url.contains(":oracle:")) {
            return "oracle.jdbc.OracleDriver";
        }
        if (url.contains(":sqlserver:")) {
            return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        }
        return "org.h2.Driver";
    }
}
