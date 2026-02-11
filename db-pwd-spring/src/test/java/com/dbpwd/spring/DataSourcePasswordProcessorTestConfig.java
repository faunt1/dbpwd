package com.dbpwd.spring;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.crypto.Sm4GCMCipher;
import com.dbpwd.core.PasswordFileManager;
import com.dbpwd.model.DatabaseEntry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 测试配置类
 *
 * @author Database Password Manager
 * @version 5.3
 */
@Configuration
public class DataSourcePasswordProcessorTestConfig {

    static String keyFilePath;
    static String pwdFilePath;
    static String sm4Key;
    static String testEncPassword;

    static {
        try {
            // 使用临时目录
            Path tempDir = Files.createTempDirectory("dbpwd-test");
            keyFilePath = tempDir.resolve("sm4.key").toString();
            pwdFilePath = tempDir.resolve("db.pwd").toString();

            // 生成测试密钥
            byte[] keyBytes = new byte[16];
            for (int i = 0; i < 16; i++) {
                keyBytes[i] = (byte) (i & 0xFF);
            }
            StringBuilder sb = new StringBuilder();
            for (byte b : keyBytes) {
                sb.append(String.format("%02x", b));
            }
            sm4Key = sb.toString();
            Files.write(tempDir.resolve("sm4.key"), sm4Key.getBytes());

            // 简单测试用的ENC格式密码
            String plainPassword = "testPassword123";
            String encryptedPassword = Sm4GCMCipher.encrypt(plainPassword, sm4Key);
            testEncPassword = "ENC(SM4-GCM:" + encryptedPassword + ")";

            // 数据库条目（用于后续可能需要从db.pwd查找的场景）
            DatabaseEntry entry = new DatabaseEntry();
            entry.setId("testdb");
            entry.setName("Test Database");
            entry.setType("mysql");
            entry.setHost("localhost");
            entry.setPort(3306);
            entry.setDatabase("test");
            entry.setUsername("root");
            entry.setPassword(testEncPassword);
            entry.setUrl("jdbc:mysql://localhost:3306/test");
            entry.setUpdatedAt(String.valueOf(System.currentTimeMillis()));

            // 保存密码文件
            PasswordFileManager manager = PasswordFileManager.loadFromFileSafe(pwdFilePath);
            manager.addDatabase(entry);
            manager.save();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 配置DataSourcePasswordProcessor
     */
    @Bean
    public DataSourcePasswordProcessor dataSourcePasswordProcessor() {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();
        processor.setKeyFilePath(keyFilePath);
        processor.setPwdFilePath(pwdFilePath);
        return processor;
    }

    /**
     * 配置加密DataSource（使用ENC格式密码）
     */
    @Bean
    public DataSource encryptedDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mysql://localhost:3306/test");
        dataSource.setUsername("root");
        // 使用ENC格式密码
        dataSource.setPassword(testEncPassword);
        return dataSource;
    }
}
