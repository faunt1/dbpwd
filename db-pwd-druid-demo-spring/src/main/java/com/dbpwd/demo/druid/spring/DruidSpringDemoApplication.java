package com.dbpwd.demo.druid.spring;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.core.PasswordService;
import com.dbpwd.druid.DbPwdDruidPasswordCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Druid Spring XML配置方式Demo
 *
 * 集成模式: 方案1 - Druid PasswordCallback
 * 配置方式: Spring XML配置文件 + Java代码配置
 *
 * 工作机制:
 * 1. Spring容器加载XML配置文件
 * 2. 创建DruidDataSource Bean
 * 3. 在Java代码中设置PasswordCallback
 * 4. Druid在获取连接时自动调用PasswordCallback解密密码
 *
 * 配置要点:
 * - password: 设置为占位符 @druid_demo_spring_db@
 * - 在Java代码中设置 DbPwdDruidPasswordCallback
 * - PasswordCallback自动解密密码
 *
 * @author Database Password Manager
 * @version 1.0
 */
public class DruidSpringDemoApplication {

    private static final Logger logger = LoggerFactory.getLogger(DruidSpringDemoApplication.class);

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("   Database Password Manager - Druid Spring XML Demo");
        logger.info("========================================");
        logger.info("");
        logger.info("Integration Mode: Druid PasswordCallback");
        logger.info("Configuration: Spring XML (druid-datasource.xml) + Java");
        logger.info("");

        try {
            logger.info("Loading Spring configuration...");
            ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                    "druid-datasource.xml");

            DruidDataSource dataSource = context.getBean(DruidDataSource.class);
            
            DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback();
            callback.setDataSource(dataSource);
            dataSource.setPasswordCallback(callback);
            
            logger.info("DataSource initialized with PasswordCallback");
            logger.info("Password placeholder: {}", dataSource.getPassword());

            logger.info("");
            logger.info("Testing database connection...");
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM products");
                if (rs.next()) {
                    logger.info("Products count: {}", rs.getInt(1));
                }

                logger.info("");
                logger.info("========================================");
                logger.info("   Demo completed successfully!");
                logger.info("========================================");
                logger.info("");
                logger.info("Key points demonstrated:");
                logger.info("- Druid PasswordCallback configured in Java code");
                logger.info("- Password automatically decrypted from db.pwd");
                logger.info("- Works with Spring XML configuration");
            }

            context.close();

        } catch (Exception e) {
            logger.error("Demo failed", e);
            System.exit(1);
        }
    }
}
