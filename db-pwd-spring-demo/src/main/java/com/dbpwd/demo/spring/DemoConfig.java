package com.dbpwd.demo.spring;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.crypto.Sm4GCMCipher;
import com.dbpwd.demo.spring.dao.UserDao;
import com.dbpwd.spring.DataSourcePasswordProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Demo应用Spring配置类
 *
 * 配置DataSourcePasswordProcessor和数据源
 *
 * @author Database Password Manager
 * @version 5.3
 */
@Configuration
public class DemoConfig {

    private static final Logger logger = LoggerFactory.getLogger(DemoConfig.class);

    // 默认密钥（16字节hex = 32字符）
    private static final String DEFAULT_SM4_KEY = "000102030405060708090a0b0c0d0e0f";

    /**
     * 配置DataSourcePasswordProcessor
     *
     * @return 数据源密码处理器
     */
    @Bean
    public DataSourcePasswordProcessor dataSourcePasswordProcessor() {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();

        // 确保密钥文件存在
        String keyFilePath = ensureDemoKeyFile();

        // 使用demo密钥文件路径
        processor.setKeyFilePath(keyFilePath);
        logger.info("DataSourcePasswordProcessor configured with key file: {}", keyFilePath);
        return processor;
    }

    /**
     * 确保demo密钥文件存在
     */
    private String ensureDemoKeyFile() {
        try {
            String userHome = System.getProperty("user.home");
            String keyFilePath = userHome + "/.dbpwd/demo_sm4.key";

            // 创建.dbpwd目录
            Files.createDirectories(Paths.get(userHome, ".dbpwd"));

            // 写入密钥文件
            Files.write(Paths.get(keyFilePath), DEFAULT_SM4_KEY.getBytes());
            logger.info("Demo SM4 key file created at: {}", keyFilePath);

            return keyFilePath;
        } catch (Exception e) {
            logger.error("Failed to create demo key file", e);
            throw new RuntimeException("Failed to create demo key file", e);
        }
    }

    /**
     * 配置Druid DataSource
     *
     * 密码使用ENC(SM4-GCM:xxx)格式的加密密码
     * DataSourcePasswordProcessor会自动解密
     *
     * @return Druid数据源
     */
    @Bean
    public DataSource dataSource() {
        logger.info("Configuring Druid dataSource with encrypted password...");

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:mem:dbpwd_spring_demo;DB_CLOSE_DELAY=-1");
        dataSource.setUsername("sa");
        // 使用ENC(SM4-GCM:xxx)格式的加密密码
        // 这个密码是使用密钥 "000102030405060708090a0b0c0d0e0f" 加密 "sa" 得到的
        dataSource.setPassword(getEncryptedPassword());
        dataSource.setDriverClassName("org.h2.Driver");

        // 连接池配置
        dataSource.setInitialSize(2);
        dataSource.setMaxActive(10);
        dataSource.setMinIdle(2);
        dataSource.setMaxWait(60000);

        logger.info("Druid dataSource configured with URL: {}", dataSource.getUrl());
        return dataSource;
    }

    /**
     * 配置JdbcTemplate
     *
     * @return JdbcTemplate实例
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        logger.info("JdbcTemplate bean configured");
        return template;
    }

    /**
     * 配置UserDao
     *
     * @return UserDao实例
     */
    @Bean
    public UserDao userDao(JdbcTemplate jdbcTemplate) {
        UserDao dao = new UserDao(jdbcTemplate);
        logger.info("UserDao bean configured");
        return dao;
    }

    /**
     * 获取ENC格式的加密密码
     */
    private String getEncryptedPassword() {
        try {
            // 明文密码
            String plainPassword = "sa";

            // 加密
            String encryptedPassword = Sm4GCMCipher.encrypt(plainPassword, DEFAULT_SM4_KEY);

            // 生成ENC格式
            String encPassword = "ENC(SM4-GCM:" + encryptedPassword + ")";

            logger.info("Generated ENC password for demo: {}", encPassword);
            return encPassword;
        } catch (Exception e) {
            logger.error("Failed to generate encrypted password", e);
            throw new RuntimeException("Failed to generate encrypted password", e);
        }
    }
}
