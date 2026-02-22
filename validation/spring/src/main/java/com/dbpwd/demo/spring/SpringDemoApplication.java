package com.dbpwd.demo.spring;

import com.dbpwd.demo.spring.dao.UserDao;
import com.dbpwd.demo.spring.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

/**
 * Spring Demo application - INTEGRATED VERSION (with db-pwd)
 *
 * 集成版本 - 使用 db-pwd-spring 自动解密密码
 *
 * 【集成改动】
 * 1. 添加 db-pwd-spring 依赖
 * 2. DataSourcePasswordProcessor 自动解密密码
 *
 * @author Database Password Manager
 * @version 2.0
 */
public class SpringDemoApplication {

    private static final Logger logger = LoggerFactory.getLogger(SpringDemoApplication.class);

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("   Spring Demo - INTEGRATED VERSION");
        logger.info("   (使用加密密码，已集成密码管理)");
        logger.info("========================================");
        logger.info("");

        logger.info("Starting Spring container...");
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                DemoConfig.class
        );

        try {
            UserDao userDao = context.getBean(UserDao.class);

            logger.info("");
            logger.info("========================================");
            logger.info("   Query Operations Demo");
            logger.info("========================================");

            logger.info("");
            logger.info("1. Querying all users...");
            List<User> users = userDao.findAll();
            logger.info("   Found {} user(s):", users.size());
            for (User user : users) {
                logger.info("   - ID: {}, Username: {}, Email: {}", user.getId(), user.getUsername(), user.getEmail());
            }

            logger.info("");
            logger.info("2. Querying user by ID: 1...");
            User user = userDao.findById(1L);
            if (user != null) {
                logger.info("   Found: {} ({})", user.getUsername(), user.getEmail());
            }

            logger.info("");
            logger.info("========================================");
            logger.info("   Demo completed successfully!");
            logger.info("========================================");
            logger.info("");
            logger.info("Security Features in INTEGRATED version:");
            logger.info("- Password encrypted with SM4-GCM");
            logger.info("- Password stored in ~/.dbpwd/db.pwd");
            logger.info("- DataSourcePasswordProcessor auto-decrypts password");

        } catch (Exception e) {
            logger.error("Error during demo execution", e);
        } finally {
            context.close();
            logger.info("Spring container closed.");
        }
    }
}
