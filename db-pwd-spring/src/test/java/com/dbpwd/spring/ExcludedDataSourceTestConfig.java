package com.dbpwd.spring;

import com.alibaba.druid.pool.DruidDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 *排除DataSource测试配置类
 *
 * @author Database Password Manager
 * @version 5.3
 */
@Configuration
public class ExcludedDataSourceTestConfig {

    /**
     * 配置DataSourcePasswordProcessor并添加排除列表
     */
    @Bean
    public DataSourcePasswordProcessor dataSourcePasswordProcessor() {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();
        processor.addExcludedBeanName("excludedDataSource");
        return processor;
    }

    /**
     * 配置排除的DataSource（密码占位符）
     */
    @Bean
    public DataSource excludedDataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mysql://localhost:3306/test");
        dataSource.setUsername("root");
        // 使用占位符，但由于被排除，不会被处理
        dataSource.setPassword("excluded_placeholder");
        return dataSource;
    }
}
