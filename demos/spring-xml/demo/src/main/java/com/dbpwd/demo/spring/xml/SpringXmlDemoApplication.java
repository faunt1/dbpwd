package com.dbpwd.demo.spring.xml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Spring Framework XML配置方式Demo
 *
 * 集成模式: 方案2 - Spring BeanPostProcessor
 * 配置方式: Spring XML配置文件
 *
 * 工作机制:
 * 1. Spring容器加载XML配置文件
 * 2. DataSourcePasswordPostProcessor作为BeanPostProcessor注册
 * 3. 当DataSource Bean初始化时，PostProcessor自动解密密码
 * 4. 无需修改现有代码，仅需添加配置
 *
 * 配置要点:
 * - DataSource的password设置为占位符 @spring_demo_xml_db@
 * - 引入db-pwd-spring模块
 * - BeanPostProcessor自动检测并解密密码
 *
 * @author Database Password Manager
 * @version 1.0
 */
public class SpringXmlDemoApplication {

    private static final Logger logger = LoggerFactory.getLogger(SpringXmlDemoApplication.class);

    public static void main(String[] args) {
        logger.info("========================================");
        logger.info("   Database Password Manager - Spring XML Demo");
        logger.info("========================================");
        logger.info("");
        logger.info("Integration Mode: Spring BeanPostProcessor");
        logger.info("Configuration: Spring XML (applicationContext.xml)");
        logger.info("");

        try {
            // 加载Spring配置
            logger.info("Loading Spring configuration...");
            ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
                    "applicationContext.xml");

            // 获取DataSource Bean
            DataSource dataSource = context.getBean(DataSource.class);
            logger.info("DataSource initialized successfully");
            logger.info("Password was automatically decrypted by BeanPostProcessor");

            // 测试数据库连接
            logger.info("");
            logger.info("Testing database connection...");
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM orders");
                if (rs.next()) {
                    logger.info("Orders count: {}", rs.getInt(1));
                }

                logger.info("");
                logger.info("========================================");
                logger.info("   Demo completed successfully!");
                logger.info("========================================");
                logger.info("");
                logger.info("Key points demonstrated:");
                logger.info("- Spring BeanPostProcessor automatically decrypts password");
                logger.info("- No Java code modification required");
                logger.info("- Only XML configuration changes needed");
                logger.info("- Works with any DataSource implementation");
            }

            context.close();

        } catch (Exception e) {
            logger.error("Demo failed", e);
            System.exit(1);
        }
    }
}
