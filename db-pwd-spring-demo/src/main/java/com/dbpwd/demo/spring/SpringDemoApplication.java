package com.dbpwd.demo.spring;

import com.dbpwd.demo.spring.dao.UserDao;
import com.dbpwd.demo.spring.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.List;

/**
 * Spring集成Demo应用主程序
 *
 * 演示如何使用DataSourcePasswordProcessor在传统Spring应用中自动解密数据库密码
 *
 * @author Database Password Manager
 * @version 5.3
 */
public class SpringDemoApplication {

    private static final Logger logger = LoggerFactory.getLogger(SpringDemoApplication.class);

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("   Database Password Manager - Spring Demo");
        logger.info("========================================");
        logger.info("");

        // 启动Spring容器
        logger.info("Starting Spring container...");
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(
                DemoConfig.class
        );

        try {
            // 获取UserDao
            UserDao userDao = context.getBean(UserDao.class);

            logger.info("");
            logger.info("========================================");
            logger.info("   Query Operations Demo");
            logger.info("========================================");

            // 1. 查询用户总数
            logger.info("");
            logger.info("1. Counting users...");
            int count = userDao.count();
            logger.info("   Total users: {}", count);

            // 2. 查询所有用户
            logger.info("");
            logger.info("2. Querying all users...");
            List<User> users = userDao.findAll();
            logger.info("   Found {} user(s):", users.size());
            for (User user : users) {
                logger.info("   - ID: {}, Username: {}, Email: {}",
                    user.getId(), user.getUsername(), user.getEmail());
            }

            // 3. 根据ID查询用户
            if (!users.isEmpty()) {
                Long firstId = users.get(0).getId();
                logger.info("");
                logger.info("3. Querying user by ID: {}...", firstId);
                User user = userDao.findById(firstId);
                if (user != null) {
                    logger.info("   Found user: {} ({})", user.getUsername(), user.getEmail());
                }
            }

            // 4. 根据用户名查询
            logger.info("");
            logger.info("4. Querying user by username: 'alice'...");
            User alice = userDao.findByUsername("alice");
            if (alice != null) {
                logger.info("   Found user: {} ({})", alice.getUsername(), alice.getEmail());
            }

            logger.info("");
            logger.info("========================================");
            logger.info("   Demo completed successfully!");
            logger.info("========================================");
            logger.info("");
            logger.info("Key points demonstrated:");
            logger.info("- DataSourcePasswordProcessor automatically decrypts password");
            logger.info("- SM4-GCM encryption for secure password storage");
            logger.info("- Encrypted password format: ENC(SM4-GCM:xxx)");
            logger.info("- No code changes needed in business logic");

        } catch (Exception e) {
            logger.error("Error during demo execution", e);
        } finally {
            // 关闭Spring容器
            context.close();
            logger.info("Spring container closed.");
        }
    }
}
