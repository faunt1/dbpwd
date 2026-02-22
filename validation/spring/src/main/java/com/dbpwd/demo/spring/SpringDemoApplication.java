package com.dbpwd.demo.spring;

import com.dbpwd.demo.spring.dao.UserDao;
import com.dbpwd.demo.spring.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

/**
 * Spring Demo application - ORIGINAL VERSION (before integration)
 *
 * 原始版本 - 使用明文密码，未集成密码加密功能
 *
 * 注意: 数据库表和初始数据由 DatabaseInitializer 预先创建
 *
 * @author Database Password Manager
 * @version 2.0
 */
public class SpringDemoApplication {

    private static final Logger logger = LoggerFactory.getLogger(SpringDemoApplication.class);

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("   Spring Demo - ORIGINAL VERSION");
        logger.info("   (使用明文密码，未集成密码加密)");
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
            logger.info("Security Issues in ORIGINAL version:");
            logger.info("- Password is hardcoded in configuration class");

        } catch (Exception e) {
            logger.error("Error during demo execution", e);
        } finally {
            context.close();
            logger.info("Spring container closed.");
        }
    }
}
