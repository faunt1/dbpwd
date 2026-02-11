package com.example.config;

import com.alibaba.druid.pool.DruidDataSource;
import com.dbpwd.spring.DataSourcePasswordProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Spring集成Java配置示例
 *
 * @author Database Password Manager
 * @version 5.3
 */
@Configuration
public class SpringDataSourceConfig {

    /**
     * 配置DataSourcePasswordProcessor
     *
     * @return 数据源密码处理器
     */
    @Bean
    public DataSourcePasswordProcessor dataSourcePasswordProcessor() {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();
        // 可选：设置自定义路径
        // processor.setKeyFilePath("/path/to/.dbpwd/sm4.key");
        // processor.setPwdFilePath("/path/to/.dbpwd/db.pwd");
        // processor.addExcludedBeanName("excludedDataSource");
        return processor;
    }

    /**
     * 配置Druid DataSource（密码使用ENC格式）
     *
     * @return 数据源
     */
    @Bean
    public DataSource dataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mysql://localhost:3306/mydb");
        dataSource.setUsername("root");
        // 使用ENC(SM4-GCM:xxx)格式的加密密码
        dataSource.setPassword("ENC(SM4-GCM:your-encrypted-password-here)");
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        dataSource.setInitialSize(5);
        dataSource.setMaxActive(20);
        dataSource.setMaxWait(60000);
        return dataSource;
    }

    /**
     * 配置另一个DataSource（多数据源示例）
     *
     * @return 另一个数据源
     */
    @Bean
    public DataSource anotherDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:postgresql://localhost:5432/mydb");
        dataSource.setUsername("postgres");
        dataSource.setPassword("ENC(SM4-GCM:another-encrypted-password-here)");
        dataSource.setDriverClassName("org.postgresql.Driver");
        return dataSource;
    }

    /**
     * 配置JdbcTemplate
     *
     * @return JdbcTemplate实例
     */
    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }
}
