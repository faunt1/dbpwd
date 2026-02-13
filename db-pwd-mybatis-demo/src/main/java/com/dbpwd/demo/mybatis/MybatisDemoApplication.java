package com.dbpwd.demo.mybatis;

import com.dbpwd.core.Sm4KeyLoader;
import com.dbpwd.core.PasswordFileManager;
import com.dbpwd.crypto.Sm4GCMCipher;
import com.dbpwd.crypto.ENCFormat;
import com.dbpwd.crypto.PasswordDecryptor;
import com.dbpwd.demo.mybatis.dao.ProductMapper;
import com.dbpwd.demo.mybatis.model.Product;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.apache.ibatis.datasource.unpooled.UnpooledDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

/**
 * MyBatis集成Demo主程序
 *
 * 演示如何使用db-pwd-mybatis模块集成MyBatis密码加密
 */
public class MybatisDemoApplication {

    // FILE模式以支持密码验证
    private static final String H2_URL = "jdbc:h2:file:~/.dbpwd/h2_data/dbpwd_mybatis_demo;MODE=MySQL";
    private static final String H2_USERNAME = "sa";
    private static final String H2_PASSWORD = "sa";
    private static final String H2_DRIVER = "org.h2.Driver";

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   Database Password Manager - MyBatis Demo");
        System.out.println("=========================================");
        System.out.println();

        SqlSession sqlSession = null;
        try {
            // 1. 创建示例密钥文件（仅用于Demo）
            System.out.println("Creating demo key file...");
            String demoKeyPath = System.getProperty("user.home") + "/.dbpwd/demo_mybatis_sm4.key";
            String sm4Key = Sm4KeyLoader.generateKey(demoKeyPath, true);

            // 2. 加密H2密码
            System.out.println("Encrypting H2 password...");
            String cipherText = Sm4GCMCipher.encrypt(H2_PASSWORD, sm4Key);
            String encPassword = ENCFormat.formatSM4GCM(cipherText);
            System.out.println("  Encrypted password: " + encPassword.substring(0, Math.min(50, encPassword.length())) + "...");

            // 3. 构建MyBatis配置
            System.out.println("\nBuilding MyBatis configuration...");
            SqlSessionFactory sqlSessionFactory = buildSqlSessionFactory(sm4Key);

            // 4. 初始化数据库表
            System.out.println("\nInitializing database tables...");
            initDatabase(sqlSessionFactory);

            // 5. 演示CRUD操作
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
            System.out.println("- ENC(SM4-GCM:xxx) format for encrypted password");
            System.out.println("- Password automatically decrypted in DbPwdDataSourceFactory");
            System.out.println("- MyBatis mapper with CRUD operations");
            System.out.println("- H2 in-memory database for demo");

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
    private static SqlSessionFactory buildSqlSessionFactory(String sm4Key) throws Exception {
        // 解密密码
        String encPassword = "ENC(SM4-GCM:" + Sm4GCMCipher.encrypt(H2_PASSWORD, sm4Key) + ")";
        String plainPassword = decryptPassword(encPassword, sm4Key);

        // 创建数据源
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
     * 解密密码
     */
    private static String decryptPassword(String encryptedPassword, String sm4Key) throws Exception {
        String algorithm = ENCFormat.extractAlgorithm(encryptedPassword);
        if (!"SM4-GCM".equalsIgnoreCase(algorithm)) {
            throw new IllegalArgumentException(
                "不支持的加密算法: " + algorithm + "。仅支持SM4-GCM格式");
        }

        String cipherText = ENCFormat.extractCipherText(encryptedPassword);
        return Sm4GCMCipher.decrypt(cipherText, sm4Key);
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
