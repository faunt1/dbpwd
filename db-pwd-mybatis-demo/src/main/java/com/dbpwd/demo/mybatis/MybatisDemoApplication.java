package com.dbpwd.demo.mybatis;

import com.dbpwd.core.PasswordService;
import com.dbpwd.demo.mybatis.dao.ProductMapper;
import com.dbpwd.demo.mybatis.model.Product;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

/**
 * MyBatis集成Demo主程序
 *
 * 演示如何使用db-pwd-mybatis模块集成MyBatis密码加密
 *
 * 集成模式: 模式4（MyBatis DataSourceFactory模式）
 * 解密实现: 程序调用 PasswordService 获取解密后的密码
 * 占位符: mybatis_demo_db
 *
 * 工作机制:
 * 1. 程序调用 PasswordService.getDatabasePassword() 获取明文密码
 * 2. 密码从 ~/.dbpwd/db.pwd 和 ~/.dbpwd/sm4.key 自动读取和解密
 * 3. 使用明文密码创建数据源
 * 4. MyBatis 正常使用数据源进行数据库操作
 *
 * @author Database Password Manager
 * @version 6.1
 */
public class MybatisDemoApplication {

    // 占位符标识（用于标识不同的数据库配置）
    private static final String DB_PLACEHOLDER = "mybatis_demo_db";

    // H2 FILE模式以支持密码验证
    private static final String H2_URL = "jdbc:h2:file:" + System.getProperty("user.home") + "/.dbpwd/h2_data/dbpwd_mybatis_demo;MODE=MySQL";
    private static final String H2_USERNAME = "sa";
    private static final String H2_DRIVER = "org.h2.Driver";

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   Database Password Manager - MyBatis Demo");
        System.out.println("=========================================");
        System.out.println();

        SqlSession sqlSession = null;
        try {
            // 1. 从 db.pwd 和 sm4.key 获取解密后的数据库密码
            System.out.println("Getting database password from ~/.dbpwd/...");
            String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
            System.out.println("  Password decrypted successfully from db.pwd");
            System.out.println("  Using placeholder: " + DB_PLACEHOLDER);

            // 2. 构建MyBatis配置
            System.out.println("\nBuilding MyBatis configuration...");
            SqlSessionFactory sqlSessionFactory = buildSqlSessionFactory(plainPassword);

            // 3. 初始化数据库表
            System.out.println("\nInitializing database tables...");
            initDatabase(sqlSessionFactory);

            // 4. 演示CRUD操作
            System.out.println("\n=========================================");
            System.out.println("   CRUD Operations Demo");
            System.out.println("=========================================");

            sqlSession = sqlSessionFactory.openSession(true); // auto-commit
            ProductMapper productMapper = sqlSession.getMapper(ProductMapper.class);

            // 插入产品
            System.out.println("\n1. Creating products...");
            Product laptop = new Product("Laptop", "High-performance laptop", 1299.99, 50, "Electronics");
            productMapper.insert(laptop);
            System.out.println("   Created product: " + laptop);

            Product smartphone = new Product("Smartphone", "Latest smartphone model", 899.99, 100, "Electronics");
            productMapper.insert(smartphone);
            System.out.println("   Created product: " + smartphone);

            Product book = new Product("Java Programming", "Comprehensive Java guide", 49.99, 200, "Books");
            productMapper.insert(book);
            System.out.println("   Created product: " + book);

            // 查询所有产品
            System.out.println("\n2. Querying all products...");
            List<Product> allProducts = productMapper.selectAll();
            System.out.println("   Found " + allProducts.size() + " product(s):");
            for (Product p : allProducts) {
                System.out.println("   - " + p);
            }

            // 根据ID查询
            System.out.println("\n3. Querying product by ID: " + laptop.getId() + "...");
            Product foundProduct = productMapper.selectById(laptop.getId());
            System.out.println("   Found: " + foundProduct);

            // 更新产品
            System.out.println("\n4. Updating product ID: " + laptop.getId() + "...");
            laptop.setPrice(1199.99);
            laptop.setStock(45);
            int updated = productMapper.update(laptop);
            System.out.println("   Updated " + updated + " row(s).");
            Product updatedProduct = productMapper.selectById(laptop.getId());
            System.out.println("   After update: " + updatedProduct);

            // 根据分类查询
            System.out.println("\n5. Querying products by category: Electronics...");
            List<Product> electronics = productMapper.selectByCategory("Electronics");
            System.out.println("   Found " + electronics.size() + " product(s):");
            for (Product p : electronics) {
                System.out.println("   - " + p);
            }

            // 删除产品
            System.out.println("\n6. Deleting product ID: " + smartphone.getId() + "...");
            int deleted = productMapper.delete(smartphone.getId());
            System.out.println("   Deleted " + deleted + " row(s).");

            // 查询剩余产品
            System.out.println("\n7. Querying remaining products...");
            List<Product> remainingProducts = productMapper.selectAll();
            System.out.println("   Found " + remainingProducts.size() + " product(s):");
            for (Product p : remainingProducts) {
                System.out.println("   - " + p);
            }

            System.out.println("\n=========================================");
            System.out.println("   Demo completed successfully!");
            System.out.println("=========================================");
            System.out.println();
            System.out.println("Key points demonstrated:");
            System.out.println("- PasswordService.getDatabasePassword() for password decryption");
            System.out.println("- Password automatically read from ~/.dbpwd/db.pwd and ~/.dbpwd/sm4.key");
            System.out.println("- MyBatis mapper with CRUD operations");
            System.out.println("- H2 FILE database for demo");

        } catch (Exception e) {
            System.err.println("Error running demo: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (sqlSession != null) {
                try {
                    sqlSession.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    /**
     * 构建MyBatis SqlSessionFactory
     */
    private static SqlSessionFactory buildSqlSessionFactory(String plainPassword) throws Exception {
        // 创建数据源，使用解密后的明文密码
        DataSource dataSource = new UnpooledDataSource();
        ((UnpooledDataSource) dataSource).setUrl(H2_URL);
        ((UnpooledDataSource) dataSource).setUsername(H2_USERNAME);
        ((UnpooledDataSource) dataSource).setPassword(plainPassword);
        ((UnpooledDataSource) dataSource).setDriver(H2_DRIVER);

        // 创建MyBatis配置
        InputStream inputStream = Resources.getResourceAsStream("mybatis-config.xml");
        SqlSessionFactoryBuilder builder = new SqlSessionFactoryBuilder();
        SqlSessionFactory sessionFactory = builder.build(inputStream);
        org.apache.ibatis.session.Configuration config = sessionFactory.getConfiguration();
        config.setEnvironment(new org.apache.ibatis.mapping.Environment(
            "development",
            new org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory(),
            dataSource
        ));

        return sessionFactory;
    }

    /**
     * 初始化数据库表
     */
    private static void initDatabase(SqlSessionFactory sqlSessionFactory) throws Exception {
        SqlSession session = sqlSessionFactory.openSession(true);
        try {
            Connection conn = session.getConnection();
            Statement stmt = conn.createStatement();

            // 创建产品表
            String createTableSql = "CREATE TABLE IF NOT EXISTS products (" +
                    "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "description VARCHAR(500), " +
                    "price DOUBLE NOT NULL, " +
                    "stock INTEGER NOT NULL, " +
                    "category VARCHAR(50) NOT NULL" +
                    ")";

            stmt.execute(createTableSql);
            System.out.println("  Products table created successfully.");

        } finally {
            session.close();
        }
    }
}
