# Spring Framework 集成模块

> 版本: 5.3
> 更新时间: 2026-02-11

## 概述

`db-pwd-spring` 模块提供针对传统 Spring 框架（非 Spring Boot）的数据库密码管理集成，通过 `BeanPostProcessor` 机制自动解密 DataSource 密码。

## 特性

- **自动解密**: 通过 `DataSourcePasswordProcessor` 自动拦截并解密 DataSource 密码
- **透明集成**: 对现有 Spring 应用无侵入性，仅需添加一个 Bean 配置
- **多种配置方式**: 支持 XML 配置、Java 配置、属性文件配置
- **多数据源支持**: 支持应用中配置多个 DataSource
- **排除机制**: 可排除特定 DataSource 不进行密码解密处理
- **ENC 格式支持**: 支持 `ENC(SM4-GCM:xxx)` 格式的加密密码

## Maven 依赖

```xml
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-spring</artifactId>
    <version>1.0.0</version>
</dependency>
```

## 使用方式

### 1. XML 配置方式

在 Spring XML 配置文件中添加 `DataSourcePasswordProcessor`：

```xml
<beans xmlns="http://www.springframework.org/schema/beans"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://www.springframework.org/schema/beans
       http://www.springframework.org/schema/beans/spring-beans.xsd">

    <!-- 配置DataSourcePasswordProcessor -->
    <bean id="dataSourcePasswordProcessor"
          class="com.dbpwd.spring.DataSourcePasswordProcessor">
        <property name="keyFilePath" value="/path/to/.dbpwd/sm4.key"/>
        <property name="pwdFilePath" value="/path/to/.dbpwd/db.pwd"/>
    </bean>

    <!-- 配置Druid DataSource（密码使用ENC格式） -->
    <bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource">
        <property name="url" value="jdbc:mysql://localhost:3306/mydb"/>
        <property name="username" value="root"/>
        <property name="password" value="ENC(SM4-GCM:your-encrypted-password-here)"/>
    </bean>

</beans>
```

详细配置示例请参考: `src/main/resources/examples/spring-xml-config.xml`

### 2. Java 配置方式

使用 Spring Java Config：

```java
@Configuration
public class SpringDataSourceConfig {

    @Bean
    public DataSourcePasswordProcessor dataSourcePasswordProcessor() {
        DataSourcePasswordProcessor processor = new DataSourcePasswordProcessor();
        processor.setKeyFilePath("/path/to/.dbpwd/sm4.key");
        processor.setPwdFilePath("/path/to/.dbpwd/db.pwd");
        return processor;
    }

    @Bean
    public DataSource dataSource() {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:mysql://localhost:3306/mydb");
        dataSource.setUsername("root");
        dataSource.setPassword("ENC(SM4-GCM:your-encrypted-password-here)");
        return dataSource;
    }
}
```

详细配置示例请参考: `src/main/resources/examples/spring-java-config.java`

### 3. 属性文件配置方式

搭配 `PropertyPlaceholderConfigurer` 使用：

```xml
<!-- 加载属性文件 -->
<context:property-placeholder location="classpath:database.properties"/>

<bean id="dataSourcePasswordProcessor"
      class="com.dbpwd.spring.DataSourcePasswordProcessor">
    <property name="keyFilePath" value="${dbpwd.keyFilePath}" />
    <property name="pwdFilePath" value="${dbpwd.pwdFilePath}" />
</bean>

<bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource">
    <property name="url" value="${db.url}"/>
    <property name="username" value="${db.username}"/>
    <property name="password" value="${db.password}"/>
</bean>
```

属性文件示例 (`database.properties`):
```properties
db.url=jdbc:mysql://localhost:3306/mydb
db.username=root
db.password=ENC(SM4-GCM:your-encrypted-password-here)

dbpwd.keyFilePath=/path/to/.dbpwd/sm4.key
dbpwd.pwdFilePath=/path/to/.dbpwd/db.pwd
```

详细配置示例请参考: `src/main/resources/examples/spring-properties-config.properties`

## 生成加密密码

使用命令行工具生成加密密码：

```bash
# 加密明文密码
java -jar db-pwd-cli.jar encrypt --password "yourPlainPassword"

# 输出示例:
# ENC(SM4-GCM:abc123def456...)
```

或使用核心模块 API：

```java
import com.dbpwd.crypto.Sm4GCMCipher;
import com.dbpwd.core.Sm4KeyLoader;

// 从文件加载密钥
String sm4Key = Sm4KeyLoader.loadKeyFromFile("~/.dbpwd/sm4.key");

// 加密密码
String plainPassword = "yourPlainPassword";
String encryptedPassword = Sm4GCMCipher.encrypt(plainPassword, sm4Key);
String encPassword = "ENC(SM4-GCM:" + encryptedPassword + ")";
```

## 多数据源配置

支持在一个应用中配置多个 DataSource，每个 DataSource 的密码都会被自动解密：

```xml
<bean id="primaryDataSource" class="com.alibaba.druid.pool.DruidDataSource">
    <property name="url" value="jdbc:mysql://localhost:3306/primary"/>
    <property name="password" value="ENC(SM4-GCM:primary-password)"/>
</bean>

<bean id="secondaryDataSource" class="com.alibaba.druid.pool.DruidDataSource">
    <property name="url" value="jdbc:postgresql://localhost:5432/secondary"/>
    <property name="password" value="ENC(SM4-GCM:secondary-password)"/>
</bean>
```

## 排除特定 DataSource

如果某些 DataSource 不需要密码解密，可以将其添加到排除列表：

```xml
<bean id="dataSourcePasswordProcessor"
      class="com.dbpwd.spring.DataSourcePasswordProcessor">
    <property name="excludedBeanNames">
        <set>
            <value>testDataSource</value>
            <value>noDecryptDataSource</value>
        </set>
    </property>
</bean>

<!-- 这个DataSource不会被处理 -->
<bean id="testDataSource" class="com.alibaba.druid.pool.DruidDataSource">
    <property name="password" value="plainPassword"/>
</bean>
```

## 工作原理

1. Spring 容器创建 DataSource Bean
2. `DataSourcePasswordProcessor` 在 Bean 初始化后拦截（`postProcessAfterInitialization`）
3. 读取 DataSource 的密码配置
4. 如果密码是 `ENC(SM4-GCM:xxx)` 格式，使用 `sm4.key` 解密
5. 将解密后的明文密码设置回 DataSource
6. DataSource 正常建立数据库连接

## 支持的连接池

- Druid (`com.alibaba.druid.pool.DruidDataSource`)
- HikariCP (`com.zaxxer.hikari.HikariDataSource`)
- Apache DBCP2 (`org.apache.commons.dbcp2.BasicDataSource`)
- 其他实现了 `getPassword()` 和 `setPassword()` 方法的 DataSource

## 配置属性

| 属性 | 类型 | 默认值 | 说明 |
|-----|------|--------|------|
| `keyFilePath` | String | `~/.dbpwd/sm4.key` | SM4 密钥文件路径 |
| `pwdFilePath` | String | `~/.dbpwd/db.pwd` | 密码文件路径（用于占位符模式） |
| `excludedBeanNames` | Set<String> | 空 | 排除的 DataSource Bean 名称列表 |

## 注意事项

1. **密钥文件权限**: 确保 `sm4.key` 文件权限为 600（仅所有者读写）
2. **密码文件权限**: 确保 `db.pwd` 文件权限为 600（仅所有者读写）
3. **ENC 格式**: 新应用必须使用 `ENC(SM4-GCM:xxx)` 格式以获取完整性保护
4. **Windows 兼容性**: Windows 系统跳过文件权限检查，记录警告日志
5. **Spring 版本**: 兼容 Spring Framework 4.x 和 5.x 版本

## 与 Spring Boot Starter 的区别

| 特性 | Spring Boot Starter | Spring Integration |
|-----|---------------------|-------------------|
| 适用场景 | Spring Boot 应用 | 传统 Spring 应用 |
| 自动配置 | `spring.factories` | 需手动注册 Bean |
| 配置方式 | `application.yml` | XML/Java/属性文件 |
| 装饰器模式 | 是 | 否（直接解密密码） |

## 故障排查

### 密码未解密

检查以下几点：
1. 密钥文件 `sm4.key` 是否存在且路径正确
2. 密码格式是否为 `ENC(SM4-GCM:xxx)` 格式
3. 日志中是否有解密成功的输出

### 找不到dataSourcePasswordProcessor Bean

确保在 Spring 容器中注册了 `DataSourcePasswordProcessor`：
```xml
<bean class="com.dbpwd.spring.DataSourcePasswordProcessor"/>
```
或
```java
@Bean
public DataSourcePasswordProcessor dataSourcePasswordProcessor() {
    return new DataSourcePasswordProcessor();
}
```

### 密钥文件格式错误

确保密钥文件是 32 字符的 hex 编码（16 字节）：
```bash
# 示例密钥（32个字符）
0123456789abcdeffedcba9876543210
```

## 示例代码

完整的示例代码请参考：
- XML 配置: `src/main/resources/examples/spring-xml-config.xml`
- Java 配置: `src/main/resources/examples/spring-java-config.java`
- 属性文件: `src/main/resources/examples/spring-properties-config.properties`

## 单元测试

运行单元测试：
```bash
mvn test -pl db-pwd-spring
```

测试覆盖率:
- 总测试用例: 8 个
- 覆盖率: ≥ 80%

## 相关文档

- [核心模块 README](../db-pwd-core/README.md)
- [Druid 集成模块](../db-pwd-druid/README.md)
- [Spring Boot Starter](../db-pwd-spring-boot-starter/README.md)
- [集成场景和方案](../technical/integration-scenarios.md)

## 许可证

MIT License
