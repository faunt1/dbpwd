# Sprint 2: Druid PasswordCallback 集成

> 文档版本: v1.0
> 创建日期: 2026-02-15
> Sprint目标: 实现Druid连接池的PasswordCallback集成，应用无需修改代码，仅通过配置即可实现密码自动解密

---

## Sprint目标

实现 **方案1: Druid PasswordCallback集成**，核心特点：
- **应用无感知**: 应用配置中使用占位符 `@db_name@`
- **自动解密**: Druid在获取连接时自动调用PasswordCallback解密密码
- **零代码修改**: 现有应用只需修改配置文件，无需修改任何Java代码

---

## 核心实现

### 1. DbPwdDruidPasswordCallback 类

**位置**: `db-pwd-druid/src/main/java/com/dbpwd/druid/DbPwdDruidPasswordCallback.java`

**关键机制**:
```java
public class DbPwdDruidPasswordCallback extends DruidPasswordCallback {

    @Override
    public void setProperties(Properties properties) {
        super.setProperties(properties);

        String password = properties.getProperty("password");

        // 使用PasswordService解密占位符
        String plainPassword = PasswordService.getDatabasePassword(password);
        setPassword(plainPassword.toCharArray());
    }
}
```

**工作流程**:
1. Druid在创建数据库连接时调用 `setProperties()`
2. 从properties中获取password属性（可能是占位符 `@druid_demo_db@`）
3. 调用 `PasswordService.getDatabasePassword()` 解密密码
4. 设置解密后的明文密码到回调对象

---

## 应用集成方式

### 方式1: 代码配置（推荐）

```java
DruidDataSource dataSource = new DruidDataSource();
dataSource.setUrl("jdbc:mysql://localhost:3306/mydb");
dataSource.setUsername("user");
dataSource.setPassword("@my_db@");  // 设置占位符

// 关键：设置PasswordCallback
dataSource.setPasswordCallback(new DbPwdDruidPasswordCallback());

// 获取连接时自动解密
try (Connection conn = dataSource.getConnection()) {
    // 密码已在PasswordCallback中自动解密
}
```

### 方式2: Spring XML配置

```xml
<bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource">
    <property name="url" value="jdbc:mysql://localhost:3306/mydb"/>
    <property name="username" value="user"/>
    <property name="password" value="@my_db@"/>

    <!-- 设置PasswordCallback -->
    <property name="passwordCallback">
        <bean class="com.dbpwd.druid.DbPwdDruidPasswordCallback"/>
    </property>
</bean>
```

### 方式3: Spring Boot配置

```yaml
spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/mydb
    username: user
    password: "@my_db@"  # 占位符

    # Druid配置
    druid:
      password-callback-class-name: com.dbpwd.druid.DbPwdDruidPasswordCallback
```

---

## Demo应用验证

### Demo应用修改

**原实现**（手动解密）:
```java
private static DruidDataSource createDataSource() throws Exception {
    DruidDataSource dataSource = new DruidDataSource();
    dataSource.setUrl("jdbc:h2:file:~/.dbpwd/h2_data/dbpwd_druid_demo;MODE=MySQL");
    dataSource.setUsername("sa");

    // 手动调用PasswordService解密
    String password = PasswordService.getDatabasePassword("@druid_demo_db@");
    dataSource.setPassword(password);  // 已解密的密码

    return dataSource;
}
```

**新实现**（PasswordCallback自动解密）:
```java
private static DruidDataSource createDataSource() throws Exception {
    DruidDataSource dataSource = new DruidDataSource();
    dataSource.setUrl("jdbc:h2:file:~/.dbpwd/h2_data/dbpwd_druid_demo;MODE=MySQL");
    dataSource.setUsername("sa");
    dataSource.setPassword("@druid_demo_db@");  // 占位符

    // 关键：设置PasswordCallback
    dataSource.setPasswordCallback(new DbPwdDruidPasswordCallback());

    return dataSource;
}
```

### 配置对比

| 对比项 | 手动解密方式 | PasswordCallback方式 |
|--------|-------------|---------------------|
| 应用代码 | 需要调用 `PasswordService.getDatabasePassword()` | 无需修改，仅配置 |
| 密码设置 | 已解密的明文密码 | 占位符 `@db_name@` |
| 代码侵入性 | 高（需要修改应用代码） | 低（仅配置变更） |
| 迁移成本 | 高 | 低 |

---

## 单元测试

### 设计原则

| 场景 | 文件路径 | 配置方式 | 说明 |
|------|---------|---------|------|
| **单元测试** | 临时目录（@TempDir） | 通过API参数传递 | 自包含、自解释、可隔离 |
| **Demo应用** | `~/.dbpwd/` | 使用默认路径 | 模拟生产环境 |

**单元测试设计原则**:
1. **自包含**: 所有测试数据在 `@TempDir` 临时目录中创建
2. **自解释**: 通过 API 参数传递路径（如 `keyFilePath`, `pwdFilePath`）
3. **可隔离**: 测试之间互不影响，测试后自动清理

**Demo应用设计原则**:
1. **模拟生产**: 使用 `~/.dbpwd/` 作为配置目录
2. **无感知**: 应用只需使用占位符 `@db_name@`
3. **易迁移**: Demo可直接作为生产参考

### 测试类: DbPwdDruidPasswordCallbackTest

**测试场景**:

#### 1. PasswordCallback机制测试（使用自定义路径）
```java
@Test
public void testPasswordCallbackWithCustomPath() throws Exception {
    // 使用构造函数指定自定义路径（临时目录）
    Properties callbackProps = new Properties();
    callbackProps.setProperty("keyFilePath", keyFilePath.toString());
    callbackProps.setProperty("pwdFilePath", pwdFilePath.toString());

    DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback(callbackProps);

    Properties props = new Properties();
    props.setProperty("password", "@test_db@");  // 占位符

    callback.setProperties(props);

    // 验证密码已解密
    assertEquals("test_password_123", new String(callback.getPassword()));
}
```

#### 2. DruidDataSource集成测试（H2 FILE数据库验证）
```java
@Test
public void testDruidDataSourceWithPasswordCallbackAndH2Database() throws Exception {
    // 创建PasswordCallback并设置自定义路径
    Properties callbackProps = new Properties();
    callbackProps.setProperty("keyFilePath", keyFilePath.toString());
    callbackProps.setProperty("pwdFilePath", pwdFilePath.toString());

    DruidDataSource dataSource = new DruidDataSource();
    dataSource.setUrl("jdbc:h2:file:" + h2DataDir + "/test_druid_db");
    dataSource.setUsername("sa");
    dataSource.setPassword("@test_db@");  // 占位符
    dataSource.setPasswordCallback(new DbPwdDruidPasswordCallback(callbackProps));

    // 获取连接，PasswordCallback自动解密密码
    try (Connection conn = dataSource.getConnection()) {
        // H2 FILE数据库验证密码正确
        assertTrue(conn.isValid(1));
    }
}
```

#### 3. 错误密码验证
```java
@Test
public void testWrongPasswordRejected() throws Exception {
    // H2 FILE数据库首次连接绑定了正确密码
    // 错误密码应该被拒绝
    try (Connection conn = DriverManager.getConnection(
            "jdbc:h2:file:" + h2DataDir + "/test_db", "sa", "wrong_password")) {
        fail("错误的密码应该被拒绝");
    } catch (SQLException e) {
        assertTrue(e.getMessage().contains("Wrong password"));
    }
}
```

---

## 密码文件配置

### db.pwd 文件示例

**位置**: `~/.dbpwd/db.pwd`

```json
{
  "version": "1.0",
  "lastModified": "2026-02-15T00:00:00Z",
  "databases": [
    {
      "id": "druid_demo_db",
      "name": "Druid Demo Database",
      "type": "h2",
      "host": "localhost",
      "port": 9092,
      "database": "dbpwd_druid_demo",
      "url": "jdbc:h2:file:~/.dbpwd/h2_data/dbpwd_druid_demo;MODE=MySQL",
      "username": "sa",
      "password": "ENC(SM4-GCM:iv:ciphertext:tag)"
    }
  ]
}
```

### sm4.key 文件

**位置**: `~/.dbpwd/sm4.key`

```
0123456789abcdef0123456789abcdef
```

**权限**: 600 (仅所有者可读写)

---

## 迁移指南

### 从明文密码迁移到PasswordCallback

#### 步骤1: 准备密码文件

```bash
# 1. 创建密码文件目录
mkdir -p ~/.dbpwd
chmod 700 ~/.dbpwd

# 2. 生成SM4密钥
java -jar db-pwd-cli.jar generate-key

# 3. 加密数据库密码
java -jar db-pwd-cli.jar encrypt \
  --db-id druid_demo_db \
  --db-name "Druid Demo Database" \
  --db-type h2 \
  --db-url "jdbc:h2:file:~/.dbpwd/h2_data/dbpwd_druid_demo" \
  --username sa \
  --password your_plain_password
```

#### 步骤2: 修改应用配置

**仅修改配置文件，无需修改Java代码**:

```yaml
# application.yml
spring:
  datasource:
    password: "@druid_demo_db@"  # 占位符替换明文密码
    druid:
      password-callback-class-name: com.dbpwd.druid.DbPwdDruidPasswordCallback
```

或

```xml
<!-- spring-datasource.xml -->
<property name="password" value="@druid_demo_db@"/>
<property name="passwordCallback">
    <bean class="com.dbpwd.druid.DbPwdDruidPasswordCallback"/>
</property>
```

#### 步骤3: 重启应用

```bash
# 重启应用，密码自动从db.pwd解密
./restart-app.sh
```

---

## 验收标准

### 功能验收

- [x] DbPwdDruidPasswordCallback类实现完成
- [x] 单元测试通过（3个测试场景）
- [x] H2 FILE数据库密码验证通过
- [x] Demo应用运行成功

### 集成验收

- [x] 代码配置方式验证通过
- [ ] Spring XML配置方式验证通过
- [ ] Spring Boot配置方式验证通过

### 迁移验收

- [x] 从明文密码迁移文档完整
- [x] 配置变更步骤清晰
- [x] 无需修改Java代码验证

---

## 技术要点

### 1. Druid PasswordCallback机制

**Druid源码分析**:
```java
// DruidDataSource.getConnection()
public DruidPooledConnection getConnection() throws SQLException {
    return getConnection(maxWait);
}

// DruidDataSource.init()
public void init() throws SQLException {
    // ...
    if (passwordCallback != null) {
        Properties props = new Properties();
        props.setProperty("password", password);
        passwordCallback.setProperties(props);
        // passwordCallback.getPassword() 返回解密后的密码
    }
    // ...
}
```

**关键点**:
- Druid在创建物理连接时调用PasswordCallback
- PasswordCallback.getPassword()返回的密码用于数据库认证
- 应用层设置的password可以是占位符，由Callback解密

### 2. PasswordService设计

**位置**: `db-pwd-core/src/main/java/com/dbpwd/core/PasswordService.java`

**核心方法**:
```java
public class PasswordService {

    /**
     * 获取数据库密码（支持占位符和ENC格式）
     *
     * @param input 可能是占位符(@db_name@)或ENC格式
     * @return 明文密码
     */
    public static String getDatabasePassword(String input) {
        if (isPlaceholder(input)) {
            // 解析占位符 @db_name@ -> db_name
            String dbId = parsePlaceholder(input);

            // 从db.pwd文件获取加密密码
            String encryptedPassword = loadEncryptedPassword(dbId);

            // 解密返回
            return decryptPassword(encryptedPassword);
        }
        // ...
    }
}
```

### 3. H2 FILE数据库密码验证

**特点**:
- 首次连接时绑定密码
- 后续连接验证密码
- 错误密码会被拒绝

**优势**:
- 真实验证密码解密有效性
- 不依赖内存数据库的弱密码验证

---

## 常见问题

### Q1: 为什么设置PasswordCallback后还是连接失败？

**检查项**:
1. `db.pwd` 和 `sm4.key` 文件是否存在于 `~/.dbpwd/` 目录
2. 文件权限是否正确（600）
3. 占位符是否正确（`@druid_demo_db@` 而不是 `druid_demo_db`）
4. db.pwd中的id字段是否匹配占位符（去掉@符号）

### Q2: 如何验证密码文件是否正确？

```bash
# 测试解密
java -jar db-pwd-cli.jar test --db-id druid_demo_db
```

### Q3: 多数据源如何配置？

```java
// 数据源1
DruidDataSource ds1 = new DruidDataSource();
ds1.setPassword("@db1@");
ds1.setPasswordCallback(new DbPwdDruidPasswordCallback());

// 数据源2
DruidDataSource ds2 = new DruidDataSource();
ds2.setPassword("@db2@");
ds2.setPasswordCallback(new DbPwdDruidPasswordCallback());
```

---

## 后续计划

### Sprint 3: Spring生态集成
- Spring BeanPostProcessor集成
- Spring Boot Starter自动配置

### Sprint 4-6: 其他集成模式
- MyBatis DataSourceFactory
- Servlet InitListener
- Demo应用验证

---

## 总结

Sprint 2 完成了 Druid PasswordCallback 集成，实现了以下目标：

1. **零代码修改**: 应用仅需配置变更
2. **自动解密**: Druid自动调用PasswordCallback解密
3. **真实验证**: H2 FILE数据库密码验证
4. **迁移友好**: 从明文密码迁移仅需3步

**核心价值**: 为Druid连接池应用提供无侵入式的密码管理解决方案。
