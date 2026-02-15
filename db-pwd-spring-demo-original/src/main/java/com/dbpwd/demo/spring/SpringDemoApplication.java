package com.dbpwd.demo.spring;

import com.dbpwd.demo.spring.dao.UserDao;
import com.dbpwd.demo.spring.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Date;
import java.util.List;

/**
 * Spring Demo application - ORIGINAL VERSION (before integration)
 *
 * 原始版本 - 使用明文密码，未集成密码加密功能
 * 此版本用于对比展示集成密码加密功能前后的代码差异
 *
 * @author Database Password Manager
 * @version 1.0
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

            logger.info("Initializing database tables...");
            userDao.createTables();
            logger.info("Database tables initialized successfully.");

            logger.info("");
            logger.info("========================================");
            logger.info("   CRUD Operations Demo");
            logger.info("========================================");

            logger.info("");
            logger.info("1. Creating users...");
            User user1 = new User();
            user1.setUsername("john_doe");
            user1.setEmail("john@example.com");
            user1.setPassword("$2a$10$encryptedPasswordHash");
            user1.setCreatedAt(new Date());
            userDao.insert(user1);
            logger.info("   Created user: {}", user1.getUsername());

            logger.info("");
            logger.info("2. Querying all users...");
            List<User> users = userDao.findAll();
            logger.info("   Found {} user(s):", users.size());
            for (User user : users) {
                logger.info("   - ID: {}, Username: {}", user.getId(), user.getUsername());
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
