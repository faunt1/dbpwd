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
import java.util.List;

public class MybatisDemoApplication {

    private static final String H2_URL = "jdbc:h2:file:" + System.getProperty("user.home") + "/.dbpwd/h2_data/dbpwd_mybatis_demo;IFEXISTS=TRUE;MODE=MySQL";
    private static final String H2_USERNAME = "sa";
    private static final String DB_PLACEHOLDER = "@mybatis_demo_db@";
    private static final String H2_DRIVER = "org.h2.Driver";

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("   Database Password Manager - MyBatis Demo");
        System.out.println("=========================================");
        System.out.println();

        SqlSession sqlSession = null;
        try {
            System.out.println("Getting database password (encrypted)...");
            String password = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
            System.out.println("  Using encrypted password from ~/.dbpwd/db.pwd");

            SqlSessionFactory sqlSessionFactory = buildSqlSessionFactory(password);

            System.out.println("\n=========================================");
            System.out.println("   Query Operations Demo");
            System.out.println("=========================================");

            sqlSession = sqlSessionFactory.openSession(true);
            ProductMapper productMapper = sqlSession.getMapper(ProductMapper.class);

            System.out.println("\n1. Querying all products...");
            List<Product> allProducts = productMapper.selectAll();
            System.out.println("   Found " + allProducts.size() + " product(s):");
            for (Product p : allProducts) {
                System.out.println("   - " + p);
            }

            System.out.println("\n2. Querying products by category: Electronics...");
            List<Product> electronics = productMapper.selectByCategory("Electronics");
            System.out.println("   Found " + electronics.size() + " product(s):");
            for (Product p : electronics) {
                System.out.println("   - " + p);
            }

            System.out.println("\n=========================================");
            System.out.println("   Demo completed successfully!");
            System.out.println("=========================================");

        } catch (Exception e) {
            System.err.println("Error running demo: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (sqlSession != null) {
                try {
                    sqlSession.close();
                } catch (Exception e) {
                }
            }
        }
    }

    private static SqlSessionFactory buildSqlSessionFactory(String password) throws Exception {
        DataSource dataSource = new UnpooledDataSource();
        ((UnpooledDataSource) dataSource).setUrl(H2_URL);
        ((UnpooledDataSource) dataSource).setUsername(H2_USERNAME);
        ((UnpooledDataSource) dataSource).setPassword(password);
        ((UnpooledDataSource) dataSource).setDriver(H2_DRIVER);

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
}
