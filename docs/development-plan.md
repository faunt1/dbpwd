# Database Password Manager - 开发计划

> 文档版本: v2.0
> 最后更新: 2026-02-14

## 项目概述

基于 SM4 国密算法的数据库密码管理系统，提供 6 种集成模式，覆盖主流 Java 技术栈。

### 核心设计要点

1. **文件路径屏蔽**: `~/.dbpwd/` 由 `PasswordService` 内部固定，应用无感知
2. **占位符格式**: 使用 `@db_name@` 格式
3. **SM4-GCM 加密**: 认证加密，防篡改
4. **H2 FILE 验证**: 真正的密码验证机制

---

## 模块结构

```
db-pwd/
├── db-pwd-core/                    # 核心模块（Sprint 1）
├── db-pwd-druid/                   # Druid集成（Sprint 2）
├── db-pwd-spring/                  # Spring集成（Sprint 3）
├── db-pwd-spring-boot-starter/     # Spring Boot Starter（Sprint 3）
├── db-pwd-mybatis/                 # MyBatis集成（Sprint 4）
├── db-pwd-web/                     # Servlet集成（Sprint 5）
├── db-pwd-demo-init/               # Demo初始化工具（Sprint 6）
├── db-pwd-simple-jdbc-demo/        # Demo 1（Sprint 6）
├── db-pwd-druid-demo/              # Demo 2（Sprint 6）
├── db-pwd-spring-boot-demo/        # Demo 3（Sprint 6）
├── db-pwd-spring-demo/             # Demo 4（Sprint 6）
├── db-pwd-mybatis-demo/            # Demo 5（Sprint 6）
└── db-pwd-servlet-demo/            # Demo 6（Sprint 6）
```

---

## Sprint 1: 核心模块开发

### 目标

实现密码管理的核心功能，包括加密解密、文件管理、密码服务。

### 任务列表

#### 1.1 加密模块 (crypto/)

| 任务 | 说明 | 状态 |
|------|------|------|
| Sm4Cipher | SM4-ECB模式加解密 | ⚪ 待开发 |
| Sm4GCMCipher | SM4-GCM认证加密模式 | ⚪ 待开发 |
| ENCFormat | ENC格式解析器 `ENC(SM4-GCM:iv:cipher)` | ⚪ 待开发 |
| PasswordEncryptor | 密码加密器 | ⚪ 待开发 |
| PasswordDecryptor | 密码解密器，自动识别格式 | ⚪ 待开发 |

#### 1.2 数据模型 (model/)

| 任务 | 说明 | 状态 |
|------|------|------|
| PasswordFile | 密码文件JSON模型 | ⚪ 待开发 |
| DatabaseEntry | 数据库条目模型 | ⚪ 待开发 |

#### 1.3 核心服务 (core/)

| 任务 | 说明 | 状态 |
|------|------|------|
| Sm4KeyLoader | SM4密钥加载器，固定路径 `~/.dbpwd/sm4.key` | ⚪ 待开发 |
| PasswordFileManager | 密码文件管理器，固定路径 `~/.dbpwd/db.pwd` | ⚪ 待开发 |
| PasswordService | 核心服务类，封装所有操作，路径内部固定 | ⚪ 待开发 |
| PlaceholderParser | 占位符解析器，识别 `@db_name@` 格式 | ⚪ 待开发 |

#### 1.4 独立工具类 (core/)

| 任务 | 说明 | 状态 |
|------|------|------|
| StandalonePasswordDecryptor | 独立密码解密工具，方案5实现 | ⚪ 待开发 |
| DbUtil | 数据库操作工具类 | ⚪ 待开发 |

#### 1.5 命令行工具 (cli/)

| 任务 | 说明 | 状态 |
|------|------|------|
| DBPasswordCLI | 交互式命令行工具 | ⚪ 待开发 |

### 验收标准

- [ ] SM4-GCM 加密解密单元测试通过
- [ ] 密码文件读写正常
- [ ] 占位符 `@db_name@` 正确解析
- [ ] CLI 工具可正常使用

---

## Sprint 2: Druid 集成模块

### 目标

实现方案1: Druid PasswordCallback 集成。

### 任务列表

#### 2.1 Druid 集成 (db-pwd-druid/)

| 任务 | 说明 | 状态 |
|------|------|------|
| DruidPasswordCallback | Druid密码回调实现 | ⚪ 待开发 |
| DruidDataSourceFactory | Druid数据源工厂（可选） | ⚪ 待开发 |

### 关键代码

```java
public class DruidPasswordCallback {
    private String placeholder;  // 如 druid_demo_db

    public String getPassword() {
        // PasswordService 内部固定路径 ~/.dbpwd/
        return PasswordService.getDatabasePassword(placeholder);
    }
}
```

### 验收标准

- [ ] DruidPasswordCallback 正确触发
- [ ] 密码在 getConnection() 时自动解密
- [ ] 单元测试通过

---

## Sprint 3: Spring 生态集成

### 目标

实现方案2和方案3: Spring Boot Starter 和 Spring BeanPostProcessor。

### 任务列表

#### 3.1 Spring 集成 (db-pwd-spring/)

| 任务 | 说明 | 状态 |
|------|------|------|
| DataSourcePasswordProcessor | BeanPostProcessor实现 | ⚪ 待开发 |
| SpringConfig | Spring配置类 | ⚪ 待开发 |

#### 3.2 Spring Boot Starter (db-pwd-spring-boot-starter/)

| 任务 | 说明 | 状态 |
|------|------|------|
| DbPwdAutoConfiguration | Spring Boot自动配置 | ⚪ 待开发 |
| DbPwdProperties | 配置属性类 | ⚪ 待开发 |
| spring.factories | 自动配置注册 | ⚪ 待开发 |

### 关键配置

```java
// DataSourcePasswordProcessor
public Object postProcessAfterInitialization(Object bean, String beanName) {
    if (bean instanceof DataSource) {
        String password = getDataSourcePassword(bean);
        if (PlaceholderParser.isPlaceholder(password)) {  // 检查 @xxx@
            String decrypted = PasswordService.decrypt(password);
            setDataSourcePassword(bean, decrypted);
        }
    }
    return bean;
}
```

### 验收标准

- [ ] Spring Boot 自动配置生效
- [ ] DataSource Bean 自动装饰
- [ ] 占位符 `@spring_boot_db@` 正确解密

---

## Sprint 4: MyBatis 集成

### 目标

实现方案4: MyBatis DataSourceFactory。

### 任务列表

#### 4.1 MyBatis 集成 (db-pwd-mybatis/)

| 任务 | 说明 | 状态 |
|------|------|------|
| DbPwdDataSourceFactory | MyBatis数据源工厂 | ⚪ 待开发 |
| DbPwdUnpooledDataSource | 自定义非池化数据源 | ⚪ 待开发 |

### 关键代码

```java
public class DbPwdDataSourceFactory extends UnpooledDataSourceFactory {
    @Override
    public void setProperties(Properties props) {
        String password = props.getProperty("password");
        if (PlaceholderParser.isPlaceholder(password)) {
            // PasswordService 内部固定路径 ~/.dbpwd/
            String decrypted = PasswordService.decrypt(password);
            props.setProperty("password", decrypted);
        }
        super.setProperties(props);
    }
}
```

### 验收标准

- [ ] MyBatis 配置正确解析
- [ ] 占位符 `@mybatis_demo_db@` 正确解密
- [ ] Mapper 接口正常工作

---

## Sprint 5: Servlet 集成

### 目标

实现方案6: Servlet InitListener。

### 任务列表

#### 5.1 Servlet 集成 (db-pwd-web/)

| 任务 | 说明 | 状态 |
|------|------|------|
| DatabasePasswordInitListener | ServletContextListener实现 | ⚪ 待开发 |
| WebPasswordUtil | Web应用密码工具类 | ⚪ 待开发 |

### 关键代码

```java
public class DatabasePasswordInitListener implements ServletContextListener {
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // PasswordService 内部固定路径 ~/.dbpwd/
        PasswordService passwordService = new PasswordService();
        sce.getServletContext().setAttribute("passwordService", passwordService);
    }
}
```

### 验收标准

- [ ] ServletContextListener 正确注册
- [ ] WebPasswordUtil 正常工作
- [ ] 占位符 `@servlet_demo_db@` 正确解密

---

## Sprint 6: Demo 应用开发

### 目标

开发 7 个 Demo 应用，验证 6 种集成模式。

### 任务列表

#### 6.1 初始化工具 (db-pwd-demo-init/)

| 任务 | 说明 | 状态 |
|------|------|------|
| DatabaseInitializer | 创建H2 FILE数据库 | ⚪ 待开发 |

**功能**: 为所有Demo创建H2 FILE数据库，绑定密码。

#### 6.2 Demo应用

| Demo | 集成模式 | 占位符 | 状态 |
|------|---------|--------|------|
| db-pwd-simple-jdbc-demo | 方案5: StandalonePasswordDecryptor | `@simple_jdbc_db@` | ⚪ 待开发 |
| db-pwd-druid-demo | 方案1: Druid PasswordCallback | `@druid_demo_db@` | ⚪ 待开发 |
| db-pwd-spring-boot-demo | 方案2: Spring Boot Starter | `@spring_boot_db@` | ⚪ 待开发 |
| db-pwd-spring-demo | 方案3: Spring BeanPostProcessor | `@spring_framework_db@` | ⚪ 待开发 |
| db-pwd-mybatis-demo | 方案4: MyBatis DataSourceFactory | `@mybatis_demo_db@` | ⚪ 待开发 |
| db-pwd-servlet-demo | 方案6: Servlet InitListener | `@servlet_demo_db@` | ⚪ 待开发 |

### 验证流程

```bash
# 1. 构建
mvn clean install -DskipTests

# 2. 初始化H2 FILE数据库
java -jar db-pwd-demo-init/target/db-pwd-demo-init-1.0.0.jar

# 3. 运行Demo验证
java -jar db-pwd-simple-jdbc-demo/target/db-pwd-simple-jdbc-demo-1.0.0.jar
```

### 验收标准

每个Demo验证:
- [ ] 密码正确解密
- [ ] H2 FILE 数据库密码验证通过
- [ ] CRUD 操作正常
- [ ] 无异常日志

---

## 开发里程碑

| Sprint | 内容 | 预计完成 |
|--------|------|----------|
| Sprint 1 | 核心模块开发 | - |
| Sprint 2 | Druid集成 | - |
| Sprint 3 | Spring生态集成 | - |
| Sprint 4 | MyBatis集成 | - |
| Sprint 5 | Servlet集成 | - |
| Sprint 6 | Demo应用开发 | - |

---

## 技术要点

### 1. PasswordService 设计

```java
public class PasswordService {
    // 固定路径，应用无感知
    private static final String CONFIG_DIR = ".dbpwd";
    private static final String PASSWORD_FILE = "db.pwd";
    private static final String KEY_FILE = "sm4.key";

    public static String getDatabasePassword(String placeholder) {
        // 1. 解析占位符 @db_name@ -> db_name
        String dbId = PlaceholderParser.parse(placeholder);

        // 2. 从固定路径读取
        PasswordFile pf = load(getPasswordFilePath());
        byte[] key = loadKey(getKeyFilePath());

        // 3. 解密返回
        return decrypt(pf.getDatabaseById(dbId).getPassword(), key);
    }
}
```

### 2. 占位符格式

- 格式: `@db_name@`
- 示例: `@spring_boot_db@`, `@druid_demo_db@`
- 解析: 去除首尾 `@` 符号获取数据库ID

### 3. H2 FILE 数据库

- URL格式: `jdbc:h2:file:~/.dbpwd/h2_data/db_name`
- 特点: 首次连接绑定密码，后续连接验证密码
- 优势: 真正验证密码解密有效性

---

## 相关文档

- [集成模式设计](integration-patterns.md) - 6种集成模式详细设计
- [Demo应用设计](demo-applications.md) - Demo应用设计和验证
- [API参考文档](api-reference.md) - 核心API使用指南