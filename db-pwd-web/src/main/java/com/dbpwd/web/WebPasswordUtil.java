package com.dbpwd.web;

import com.dbpwd.core.PasswordFileManager;
import com.dbpwd.core.Sm4KeyLoader;
import com.dbpwd.model.DatabaseEntry;
import com.dbpwd.crypto.ENCFormat;
import com.dbpwd.crypto.Sm4GCMCipher;

import javax.servlet.ServletContext;
import java.util.logging.Logger;

/**
 * 集成场景和方案 - 方案6: Servlet/Struts InitListener集成
 *
 * Web环境密码工具类
 *
 * 使用ServletContext存储密码服务，方便Servlet和Struts Action中使用
 *
 * 使用方式：
 * <pre>
 * // 在Servlet中
 * ServletContext context = getServletContext();
 * String password = WebPasswordUtil.getPlainPassword(context, "my_database");
 *
 * // 在Struts Action中
 * ServletContext context = getServlet().getServletContext();
 * String password = WebPasswordUtil.getPlainPassword(context, "my_database");
 * </pre>
 *
 * @author Database Password Manager
 * @version 5.3
 */
public class WebPasswordUtil {

    private static final Logger logger = Logger.getLogger(WebPasswordUtil.class.getName());

    // 默认密钥和密码文件路径
    public static final String DEFAULT_KEY_PATH = WebPasswordService.DEFAULT_KEY_PATH;
    public static final String DEFAULT_PWD_PATH = System.getProperty("user.home") + "/.dbpwd/db.pwd";

    /**
     * 获取数据库明文密码
     *
     * @param context ServletContext
     * @param dbId 数据库ID占位符
     * @return 明文密码
     * @throws Exception 解密失败
     */
    public static String getPlainPassword(ServletContext context, String dbId) throws Exception {
        WebPasswordService service = getOrCreateService(context);
        return service.getPlainPassword(dbId);
    }

    /**
     * 获取数据库配置信息（包括明文密码）
     *
     * @param context ServletContext
     * @param dbId 数据库ID占位符
     * @return 数据库配置信息
     * @throws Exception 加载或解密失败
     */
    public static DatabaseEntry getDatabaseEntry(ServletContext context, String dbId) throws Exception {
        WebPasswordService service = getOrCreateService(context);
        return service.getDatabaseEntry(dbId);
    }

    /**
     * 检查是否包含指定的数据库配置
     *
     * @param context ServletContext
     * @param dbId 数据库ID
     * @return 是否包含
     */
    public static boolean containsDatabase(ServletContext context, String dbId) {
        try {
            WebPasswordService service = getOrCreateService(context);
            return service.containsDatabase(dbId);
        } catch (Exception e) {
            logger.warning("Failed to check database: " + e.getMessage());
            return false;
        }
    }

    /**
     * 预加载密码到缓存（可选性能优化）
     *
     * @param context ServletContext
     * @throws Exception 加载失败
     */
    public static void preloadPasswords(ServletContext context) throws Exception {
        WebPasswordService service = getOrCreateService(context);
        service.preloadPasswords();
        logger.info("Database passwords preloaded successfully");
    }

    /**
     * 清除密码缓存
     *
     * @param context ServletContext
     */
    public static void clearCache(ServletContext context) {
        WebPasswordService service = getOrCreateService(context);
        service.clearCache();
        logger.info("Password cache cleared");
    }

    /**
     * 清除指定数据库的缓存
     *
     * @param context ServletContext
     * @param dbId 数据库ID
     */
    public static void clearCacheForDatabase(ServletContext context, String dbId) {
        WebPasswordService service = getOrCreateService(context);
        service.clearCacheForDatabase(dbId);
    }

    /**
     * 自定义密钥和密码文件路径
     *
     * @param context ServletContext
     * @param keyFile SM4密钥文件路径
     * @param pwdFile 密码文件路径
     */
    public static void setCustomPaths(ServletContext context, String keyFile, String pwdFile) {
        WebPasswordService service = getOrCreateService(context);
        service.setCustomPaths(keyFile, pwdFile);
    }

    // ============ Private Methods ============

    private static WebPasswordService getOrCreateService(ServletContext context) {
        WebPasswordService service = (WebPasswordService) context.getAttribute(WebPasswordService.PASSWORD_SERVICE_KEY);
        if (service == null) {
            // 使用线程安全的初始化
            synchronized (WebPasswordUtil.class) {
                service = (WebPasswordService) context.getAttribute(WebPasswordService.PASSWORD_SERVICE_KEY);
                if (service == null) {
                    service = new WebPasswordService(DEFAULT_KEY_PATH, DEFAULT_PWD_PATH);
                    context.setAttribute(WebPasswordService.PASSWORD_SERVICE_KEY, service);
                    logger.info("WebPasswordService initialized and stored in ServletContext");
                }
            }
        }
        return service;
    }
}
