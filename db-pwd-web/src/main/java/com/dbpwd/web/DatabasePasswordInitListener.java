package com.dbpwd.web;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * 集成场景和方案 - 方案6: Servlet/Struts InitListener集成
 *
 * 数据库密码初始化监听器
 *
 * 在Web应用启动时初始化密码管理服务，密码预加载到ServletContext中
 *
 * 使用方式：在web.xml中配置
 * <pre>
 * &lt;listener&gt;
 *   &lt;listener-class&gt;com.dbpwd.web.DatabasePasswordInitListener&lt;/listener-class&gt;
 * &lt;/listener&gt;
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
 * &lt;context-param&gt;
 *   &lt;param-name&gt;dbpwd.preload&lt;/param-name&gt;
 *   &lt;param-value&gt;true&lt;/param-value&gt;
 * &lt;/context-param&gt;
 * </pre>
 *
 * 或者使用Servlet 3.0+注解（无需web.xml配置）：
 * <pre>
 * @WebListener
 * public class DatabasePasswordInitListener extends DatabasePasswordInitListener { }
 * </pre>
 *
 * @author Database Password Manager
 * @version 5.3
 */
@WebListener
public class DatabasePasswordInitListener implements ServletContextListener {

    // Context参数名称
    private static final String KEY_FILE_PARAM = "dbpwd.keyFile";
    private static final String PWD_FILE_PARAM = "dbpwd.pwdFile";
    private static final String PRELOAD_PARAM = "dbpwd.preload";

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            // 读取自定义配置
            String customKeyFile = sce.getServletContext().getInitParameter(KEY_FILE_PARAM);
            String customPwdFile = sce.getServletContext().getInitParameter(PWD_FILE_PARAM);
            String preloadConfig = sce.getServletContext().getInitParameter(PRELOAD_PARAM);
            boolean preload = preloadConfig == null || Boolean.parseBoolean(preloadConfig);

            // 获取或创建密码服务
            WebPasswordService service = getOrCreateService(sce.getServletContext(), customKeyFile, customPwdFile);

            // 预加载密码（可选）
            if (preload) {
                service.preloadPasswords();
                System.out.println("[DbPwd] Database passwords preloaded successfully");
            }

            System.out.println("[DbPwd] Database Password Manager initialized");

        } catch (Exception e) {
            System.err.println("[DbPwd] Failed to initialize Database Password Manager: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // 清除缓存
        WebPasswordUtil.clearCache(sce.getServletContext());
        System.out.println("[DbPwd] Database Password Manager shut down");
    }

    /**
     * 获取或创建密码服务
     */
    private WebPasswordService getOrCreateService(javax.servlet.ServletContext context,
                                                  String customKeyFile,
                                                  String customPwdFile) {
        String keyFile = (customKeyFile != null && !customKeyFile.isEmpty())
            ? customKeyFile : WebPasswordService.DEFAULT_KEY_PATH;
        String pwdFile = (customPwdFile != null && !customPwdFile.isEmpty())
            ? customPwdFile : System.getProperty("user.home") + "/.dbpwd/db.pwd";

        WebPasswordService service = (WebPasswordService) context.getAttribute(
            WebPasswordService.PASSWORD_SERVICE_KEY);

        if (service == null) {
            synchronized (DatabasePasswordInitListener.class) {
                service = (WebPasswordService) context.getAttribute(
                    WebPasswordService.PASSWORD_SERVICE_KEY);
                if (service == null) {
                    service = new WebPasswordService(keyFile, pwdFile);
                    context.setAttribute(WebPasswordService.PASSWORD_SERVICE_KEY, service);
                    System.out.println("[DbPwd] WebPasswordService created");
                }
            }
        }

        return service;
    }
}
