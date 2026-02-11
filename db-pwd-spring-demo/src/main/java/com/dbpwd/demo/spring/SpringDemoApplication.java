package com.dbpwd.demo.spring;

import com.dbpwd.demo.spring.dao.UserDao;
import com.dbpwd.demo.spring.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Date;
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

            // 初始化数据库表
            logger.info("Initializing database tables...");
            userDao.createTables();
            logger.info("Database tables initialized successfully.");

            // 演示CRUD操作
            logger.info("");
            logger.info("========================================");
            logger.info("   CRUD Operations Demo");
            logger.info("========================================");

            // 1. 创建用户
            logger.info("");
            logger.info("1. Creating users...");
            User user1 = new User();
            user1.setUsername("john_doe");
            user1.setEmail("john@example.com");
            user1.setPassword("$2a$10$encryptedPasswordHash");
            user1.setCreatedAt(new Date());
            Long id1 = userDao.insert(user1);
            logger.info("   Created user: {}", user1.getUsername());

            User user2 = new User();
            user2.setUsername("jane_smith");
            user2.setEmail("jane@example.com");
            user2.setPassword("$2a$10$encryptedPasswordHash");
            user2.setCreatedAt(new Date());
            Long id2 = userDao.insert(user2);
            logger.info("   Created user: {}", user2.getUsername());

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
            logger.info("");
            logger.info("3. Querying user by ID: {}...", id1);
            User user = userDao.findById(id1);
            if (user != null) {
                logger.info("   Found user: {} ({})", user.getUsername(), user.getEmail());
            }

            // 4. 更新用户
            logger.info("");
            logger.info("4. Updating user ID: {}...", id1);
            if (user != null) {
                user.setEmail("john.updated@example.com");
                int updated = userDao.update(user);
                logger.info("   Updated {} row(s).", updated);
            }

            // 5. 删除用户
            logger.info("");
            logger.info("5. Deleting user ID: {}...", id2);
            int deleted = userDao.deleteById(id2);
            logger.info("   Deleted {} row(s).", deleted);

            // 6. 查询剩余用户
            logger.info("");
            logger.info("6. Querying remaining users...");
            List<User> remainingUsers = userDao.findAll();
            logger.info("   Found {} user(s):", remainingUsers.size());
            for (User u : remainingUsers) {
                logger.info("   - ID: {}, Username: {}, Email: {}",
                    u.getId(), u.getUsername(), u.getEmail());
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
