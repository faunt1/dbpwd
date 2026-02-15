package com.dbpwd.init;

import com.dbpwd.core.PasswordService;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.logging.Logger;

/**
 * Demo数据库初始化工具
 *
 * 为所有Demo应用初始化H2 FILE格式的数据库
 * 每个使用独立的数据库文件，支持密码验证
 *
 * @author Database Password Manager
 * @version 1.0
 */
public class DatabaseInitializer {

    private static final Logger logger = Logger.getLogger(DatabaseInitializer.class.getName());

    // Demo数据库配置
    private static final DemoDatabase[] DEMO_DATABASES = {
        new DemoDatabase("simple_jdbc_db", "dbpwd_simple_demo",
            "Simple JDBC Demo Database"),
        new DemoDatabase("druid_demo_db", "dbpwd_druid_demo",
            "Druid Demo Database"),
        new DemoDatabase("spring_boot_db", "dbpwd_spring_boot_demo",
            "Spring Boot Demo Database"),
        new DemoDatabase("spring_framework_db", "dbpwd_spring_demo",
            "Spring Framework Demo Database"),
        new DemoDatabase("mybatis_demo_db", "dbpwd_mybatis_demo",
            "MyBatis Demo Database"),
        new DemoDatabase("servlet_demo_db", "dbpwd_servlet_demo",
            "Servlet Demo Database"),
        new DemoDatabase("struts_demo_db", "dbpwd_struts_demo",
            "Struts Demo Database")
    };

    // 数据库表初始化SQL
    private static final String[] TABLE_SQLS = {
        "CREATE TABLE IF NOT EXISTS users (id INT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(50) NOT NULL, email VARCHAR(100) NOT NULL, password VARCHAR(100) NOT NULL)",
        "CREATE TABLE IF NOT EXISTS products (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, description VARCHAR(255), price DECIMAL(10,2) NOT NULL, stock INT NOT NULL)"
    };

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   Database Password Manager - Demo Database Initialization");
        System.out.println("=========================================");
        System.out.println();

        String userHome = System.getProperty("user.home");
        File h2DataDir = new File(userHome, ".dbpwd/h2_data");

        if (!h2DataDir.exists() && !h2DataDir.mkdirs()) {
            logger.warning("无法创建H2数据目录: " + h2DataDir.getAbsolutePath());
            return;
        }

        System.out.println("H2 data directory: " + h2DataDir.getAbsolutePath());
        System.out.println();

        int successCount = 0;
        int skipCount = 0;
        int failCount = 0;

        for (DemoDatabase demoDb : DEMO_DATABASES) {
            System.out.print("Initializing: " + demoDb.placeholder + "  → " + demoDb.dbName + " ... ");
            try {
                if (initializeDatabase(h2DataDir, demoDb)) {
                    System.out.println("SUCCESS");
                    System.out.println("  Database file: jdbc:h2:file:" + h2DataDir + "/" + demoDb.dbName);
                    System.out.println("  Password verification: ENABLED");
                    successCount++;
                } else {
                    System.out.println("SKIPPED (database file already exists)");
                    skipCount++;
                }
                System.out.println();
            } catch (Exception e) {
                System.out.println("FAILED");
                System.out.println("  Error: " + e.getMessage());
                failCount++;
                System.out.println();
            }
        }

        System.out.println("=========================================");
        System.out.println("   Initialization Summary");
        System.out.println("=========================================");
        System.out.println("  Total databases: " + DEMO_DATABASES.length);
        System.out.println("  Successfully initialized: " + successCount);
        System.out.println("  Skipped (already exists): " + skipCount);
        System.out.println("  Failed: " + failCount);
        System.out.println("  H2 data directory: " + h2DataDir.getAbsolutePath());
        System.out.println("=========================================");
        System.out.println();

        if (failCount > 0) {
            logger.warning("部分数据库初始化失败，请检查错误信息");
            System.exit(1);
        }
    }

    /**
     * 初始化单个数据库
     */
    private static boolean initializeDatabase(File dataDir, DemoDatabase demoDb) throws Exception {
        // 检查数据库文件是否已存在
        File dbFile = new File(dataDir, demoDb.dbName + ".mv.db");
        if (dbFile.exists()) {
            return false;
        }

        // 从db.pwd中获取明文密码
        String plainPassword = PasswordService.getDatabasePassword(demoDb.placeholder);
        System.out.println();
        System.out.println("  Password retrieved from db.pwd: OK");

        // 构建H2 FILE模式连接URL
        String url = "jdbc:h2:file:" + dataDir + "/" + demoDb.dbName + ";MODE=MySQL";

        try (Connection conn = DriverManager.getConnection(url, "sa", plainPassword);
             Statement stmt = conn.createStatement()) {

            // 创建数据表
            for (String sql : TABLE_SQLS) {
                stmt.execute(sql.replace("AUTO_INCREMENT", "IDENTITY"));
            }

            System.out.println("  Table(s) created successfully: " + demoDb.dbName);
            return true;
        }
    }

    /**
     * Demo数据库配置
     */
    static class DemoDatabase {
        String placeholder;
        String dbName;
        String name;

        DemoDatabase(String placeholder, String dbName, String name) {
            this.placeholder = placeholder;
            this.dbName = dbName;
            this.name = name;
        }
    }
}
