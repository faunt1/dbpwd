package com.dbpwd.demo.druid.spring;

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
 * 配置方式: Spring XML配置文件
 *
 * 工作机制:
 * 1. Spring容器加载XML配置文件
 * 2. 创建DruidDataSource Bean
 * 3. 设置passwordCallbackClassName属性
 * 4. Druid在获取连接时自动调用PasswordCallback解密密码
 *
 * 配置要点:
 * - password: 设置为占位符 @druid_demo_spring_db@
 * - passwordCallbackClassName: 设置为 DbPwdDruidPasswordCallback
 * - 无需修改Java代码，仅通过配置文件集成
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
        logger.info("Configuration: Spring XML (druid-datasource.xml)");
        logger.info("");

        try {
            // 加载Spring配置
            logger.info("Loading Spring configuration...");
            ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                    "druid-datasource.xml");

            // 获取DataSource Bean
            DataSource dataSource = context.getBean(DataSource.class);
            logger.info("DataSource initialized successfully");

            // 测试数据库连接
            logger.info("");
            logger.info("Testing database connection...");
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                // 创建测试表
                stmt.execute("CREATE TABLE IF NOT EXISTS products (" +
                        "id INT IDENTITY PRIMARY KEY, " +
                        "name VARCHAR(100) NOT NULL, " +
                        "price DECIMAL(10,2) NOT NULL, " +
                        "stock INT NOT NULL)");

                // 插入测试数据
                stmt.execute("INSERT INTO products (name, price, stock) VALUES " +
                        "('Laptop', 1299.99, 50), " +
                        "('Smartphone', 899.99, 100)");

                // 查询数据
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
                logger.info("- Druid PasswordCallback configured via Spring XML");
                logger.info("- passwordCallbackClassName property set in XML");
                logger.info("- Password automatically decrypted from db.pwd");
                logger.info("- No Java code modification required");
            }

            context.close();

        } catch (Exception e) {
            logger.error("Demo failed", e);
            System.exit(1);
        }
    }
}
