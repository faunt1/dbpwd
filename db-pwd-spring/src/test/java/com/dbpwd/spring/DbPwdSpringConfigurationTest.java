package com.dbpwd.spring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DbPwdSpringConfiguration单元测试
 */
@DisplayName("DbPwdSpringConfiguration测试")
public class DbPwdSpringConfigurationTest {

    @Test
    @DisplayName("测试DbPwdSpringConfiguration实例化")
    public void testInstantiation() {
        DbPwdSpringConfiguration config = new DbPwdSpringConfiguration();

        assertNotNull(config);
    }

    @Test
    @DisplayName("测试dataSourcePasswordProcessor方法")
    public void testDataSourcePasswordProcessorMethod() {
        DbPwdSpringConfiguration config = new DbPwdSpringConfiguration();

        DataSourcePasswordProcessor processor = config.dataSourcePasswordProcessor();

        assertNotNull(processor);
        assertTrue(processor instanceof DataSourcePasswordProcessor);
    }

    @Test
    @DisplayName("测试DataSourcePasswordProcessor配置")
    public void testProcessorConfiguration() {
        DbPwdSpringConfiguration config = new DbPwdSpringConfiguration();

        DataSourcePasswordProcessor processor = config.dataSourcePasswordProcessor();

        // 验证处理器可以设置自定义路径
        String customKeyPath = "/custom/key.path";
        String customPwdPath = "/custom/pwd.path";

        processor.setKeyFilePath(customKeyPath);
        processor.setPwdFilePath(customPwdPath);

        // 注意：由于没有getter方法，验证主要通过方法调用不抛异常
        processor.addExcludedBeanName("testDataSource1");
        processor.addExcludedBeanName("testDataSource2");
    }

    @Test
    @DisplayName("测试排除列表")
    public void testExcludedList() {
        DbPwdSpringConfiguration config = new DbPwdSpringConfiguration();

        DataSourcePasswordProcessor processor = config.dataSourcePasswordProcessor();

        // 添加排除的Bean名称
        processor.addExcludedBeanName("testDataSource1");
        processor.addExcludedBeanName("testDataSource2");

        // 注意：由于没有getter方法获取排除列表，我们通过重复添加相同名称来验证
        // Set特性会允许重复添加同一元素
        processor.addExcludedBeanName("testDataSource1");
    }
}
