---
marp: true
theme: default
paginate: true
---

<style>
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+JP:wght@400;500;700&display=swap');

:root {
  --color-background: #ffffff;
  --color-foreground: #1f2937;
  --color-heading: #1e40af;
  --color-accent: #3b82f6;
  --color-border: #d1d5db;
  --font-default: 'Noto Sans JP', 'Hiragino Kaku Gothic ProN', 'Meiryo', sans-serif;
}

section {
  background-color: var(--color-background);
  color: var(--color-foreground);
  font-family: var(--font-default);
  font-weight: 400;
  box-sizing: border-box;
  border-top: 8px solid var(--color-heading);
  position: relative;
  line-height: 1.7;
  font-size: 22px;
  padding: 56px;
}

h1, h2, h3, h4, h5, h6 {
  font-weight: 700;
  color: var(--color-heading);
  margin: 0;
  padding: 0;
}

h1 {
  font-size: 54px;
  line-height: 1.3;
  text-align: left;
  font-weight: 700;
  letter-spacing: -0.02em;
}

h2 {
  position: absolute;
  top: 40px;
  left: 56px;
  right: 56px;
  font-size: 38px;
  padding-top: 0;
  padding-bottom: 16px;
  border-bottom: 3px solid var(--color-accent);
}

h2 + * {
  margin-top: 112px;
}

h3 {
  color: var(--color-accent);
  font-size: 26px;
  margin-top: 32px;
  margin-bottom: 12px;
  font-weight: 600;
}

ul, ol {
  padding-left: 32px;
}

li {
  margin-bottom: 10px;
  line-height: 1.7;
}

footer {
  font-size: 16px;
  color: #6b7280;
  position: absolute;
  left: 56px;
  right: 56px;
  bottom: 40px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

footer::before {
  content: '';
  flex: 1;
  height: 2px;
  background-color: var(--color-border);
  margin-right: 20px;
}

section.lead {
  border-top: 8px solid var(--color-heading);
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: linear-gradient(135deg, #ffffff 0%, #f3f4f6 100%);
}

section.lead footer {
  display: none;
}

section.lead h1 {
  margin-bottom: 32px;
  color: var(--color-heading);
}

section.lead p {
  font-size: 24px;
  color: var(--color-foreground);
  font-weight: 500;
}

table {
  border-collapse: collapse;
  width: 100%;
  margin: 20px 0;
  font-size: 18px;
}

th, td {
  border: 1px solid var(--color-border);
  padding: 12px;
  text-align: left;
}

th {
  background-color: var(--color-heading);
  color: #ffffff;
  font-weight: 700;
}

tr:nth-child(even) {
  background-color: #f9fafb;
}

strong {
  color: var(--color-heading);
  font-weight: 700;
}

code {
  background-color: #f3f4f6;
  color: var(--color-heading);
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 0.9em;
}

pre {
  background-color: #f3f4f6;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  padding: 16px;
  overflow-x: auto;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 14px;
  line-height: 1.4;
}

pre code {
  background-color: transparent;
  padding: 0;
  color: var(--color-foreground);
}
</style>

<!-- _class: lead -->

# Database Password Manager

## 基于 SM4 国密算法的数据库密码管理系统

**设计思路与实践分享**

---

# 目录

1. 问题背景与需求
2. 解决方案概述
3. 核心设计思路
4. 架构设计
5. 六种集成模式
6. 使用演示
7. 项目成果
8. 总结与展望

---

## 1.1 问题背景

企业在数据库密码管理中面临的挑战：

| 问题 | 风险 |
|------|------|
| 密码明文存储 | 配置文件泄露 → 数据库被入侵 |
| 密码提交到代码库 | 版本控制暴露敏感信息 |
| 多环境密码管理混乱 | 开发/测试/生产密码混淆 |
| 人员流动 | 密码泄露无法追踪 |

**典型场景**：配置文件中的明文密码

```yaml
spring:
  datasource:
    password: "MySecret123"  # ⚠️ 安全隐患
```

---

## 1.2 核心需求

| 需求 | 说明 |
|------|------|
| **密码安全存储** | 生产密码不在代码库和配置文件中明文出现 |
| **应用无感知** | 应用代码无需关心密码文件位置 |
| **多框架支持** | 覆盖主流 Java 技术栈 |
| **国密合规** | 符合国家密码标准 |

---

## 2.1 解决方案

**Database Password Manager**

- 基于 **SM4-GCM** 国密算法
- 提供 **6 种集成模式**
- 覆盖主流 Java 技术栈
- **应用无感知设计**

---

## 2.2 核心特性

| 特性 | 说明 |
|------|------|
| 🔐 SM4-GCM 认证加密 | 国密标准，认证加密防篡改 |
| 🔧 6 种集成模式 | Druid/Spring Boot/Spring/MyBatis/Servlet/JDBC |
| 🙈 应用无感知 | 文件路径固定为 `~/.dbpwd/`，应用无需配置 |
| 📝 占位符格式 | `@db_name@` 格式，自动解密 |

---

## 2.3 解决方案对比

| 方案 | 优点 | 缺点 |
|------|------|------|
| 环境变量 | 简单 | 明文传输，不安全 |
| 配置中心 | 集中管理 | 依赖外部服务 |
| Jasypt | Spring 集成 | 非国密算法 |
| **本项目** | **国密 + 无感知** | 需要初始化 |

---

## 3.1 设计原则

```
┌─────────────────────────────────────────────────────────┐
│                    设计原则                              │
├─────────────────────────────────────────────────────────┤
│  1. 应用无感知  →  文件路径内部固定，应用只需用占位符    │
│  2. 最小权限    →  文件权限严格设置为 600               │
│  3. 职责分离    →  运维管理密钥，应用使用密码           │
│  4. 可审计      →  所有密文操作记录更新时间             │
└─────────────────────────────────────────────────────────┘
```

---

## 3.2 核心设计决策

| 决策点 | 选择 | 理由 |
|-------|------|------|
| 文件路径 | 固定为 `~/.dbpwd/` | 参考 SSH 机制，应用无感知 |
| 占位符格式 | `@db_name@` | 明确标识，与配置格式区分 |
| 加密算法 | SM4-GCM | 国密标准，认证加密防篡改 |
| 应用集成 | 装饰器模式 | 最小改动，自动解密 |

---

## 3.3 占位符格式设计

**应用配置中使用占位符**：

```yaml
# Spring Boot 示例
spring:
  datasource:
    password: "@spring_boot_db@"  # 占位符
```

**系统自动解密**：

```
@spring_boot_db@  →  PasswordService  →  "sa"
```

---

## 3.4 加密算法选择

**SM4-GCM 认证加密**

```
密文格式: ENC(SM4-GCM:Base64(IV):Base64(密文+标签))
```

| 特性 | 说明 |
|------|------|
| 加密算法 | SM4（国密标准） |
| 工作模式 | GCM（认证加密） |
| 密钥长度 | 128 位 |
| 安全特性 | 机密性 + 完整性 + 认证 |

---

## 4.1 整体架构

```
┌─────────────────────────────────────────────────────────────────┐
│                        应用层 (Application)                       │
├─────────────────────────────────────────────────────────────────┤
│  SpringBoot   │   MyBatis   │   纯JDBC   │   Servlet        │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                      集成层 (Integration Layer)                   │
├─────────────────────────────────────────────────────────────────┤
│  方案1:Druid  │  方案2:Boot  │  方案3:Spring  │  方案4:MyBatis │
│  方案5:JDBC   │  方案6:Servlet                                   │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                  核心服务层 (PasswordService)                    │
├─────────────────────────────────────────────────────────────────┤
│  SM4加解密  │  占位符解析  │  文件管理                          │
│           (固定路径: ~/.dbpwd/ 应用无感知)                       │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│                        存储层 (Storage)                         │
├─────────────────────────────────────────────────────────────────┤
│  ~/.dbpwd/db.pwd    │    ~/.dbpwd/sm4.key                     │
│  (密码文件 权限600) │    (密钥文件 权限600)                    │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4.2 模块划分

| 模块 | 职责 |
|------|------|
| `db-pwd-core` | 核心加解密、密码服务、文件管理 |
| `db-pwd-druid` | Druid PasswordCallback 集成 |
| `db-pwd-spring` | Spring BeanPostProcessor 集成 |
| `db-pwd-spring-boot-starter` | Spring Boot 自动配置 |
| `db-pwd-mybatis` | MyBatis DataSourceFactory 集成 |
| `db-pwd-web` | Servlet ContextListener 集成 |

---

## 4.3 密码文件管理

**密码文件路径**：`~/.dbpwd/db.pwd`

**JSON 格式**：

```json
{
  "databases": [
    {
      "id": "spring_boot_db",
      "url": "jdbc:h2:file:...",
      "username": "sa",
      "password": "ENC(SM4-GCM:xxx)",
      "updatedAt": "2026-02-20T10:00:00Z"
    }
  ]
}
```

---

## 4.4 安全设计

| 安全特性 | 实现方式 |
|---------|---------|
| 密码存储 | 加密后存储在 `~/.dbpwd/db.pwd` |
| 密钥保护 | 存储在 `~/.dbpwd/sm4.key`，权限 600 |
| 应用隔离 | 应用无需知道文件路径 |
| 配置安全 | 配置文件中只有占位符 |

---

## 5.1 集成模式总览

| 模式 | 适用场景 | 核心机制 | 自动解密 |
|------|---------|---------|----------|
| 方案1 | Druid 连接池 | PasswordCallback | ✅ |
| 方案2 | Spring Boot | Starter 自动配置 | ✅ |
| 方案3 | 传统 Spring | BeanPostProcessor | ✅ |
| 方案4 | MyBatis | API 调用 | ❌ 手动 |
| 方案5 | 纯 JDBC | API 调用 | ❌ 手动 |
| 方案6 | Servlet/Web | ContextListener | ✅ |

---

## 5.2 方案1: Druid PasswordCallback

**适用场景**：使用 Druid 连接池的应用

**集成原理**：Druid 在获取连接时自动调用 PasswordCallback 解密密码

```java
// 配置 PasswordCallback
DruidDataSource dataSource = new DruidDataSource();
dataSource.setPassword("@druid_demo_db@");

DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback();
callback.setDataSource(dataSource);
dataSource.setPasswordCallback(callback);
```

---

## 5.3 方案2: Spring Boot Starter

**适用场景**：Spring Boot 应用

**集成方式**：零代码修改，仅需添加依赖和修改配置

**添加依赖**：
```xml
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

**修改配置**：
```yaml
spring:
  datasource:
    password: "@spring_boot_db@"
```

---

## 5.4 方案3: Spring BeanPostProcessor

**适用场景**：传统 Spring 应用（Java Config 或 XML Config）

**XML 方式**：
```xml
<!-- 添加 BeanPostProcessor -->
<bean class="com.dbpwd.spring.DataSourcePasswordProcessor"/>

<bean id="dataSource" class="...">
    <property name="password" value="@my_db@"/>
</bean>
```

**Java Config 方式**：
```java
@Bean
public DataSourcePasswordProcessor dataSourcePasswordProcessor() {
    return new DataSourcePasswordProcessor();
}
```

---

## 5.5 方案4: MyBatis

**适用场景**：纯 MyBatis 应用（无 Spring 依赖）

**集成原理**：在构建 SqlSessionFactory 时，使用 PasswordService API 获取密码

**添加依赖**：
```xml
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 5.6 方案4: MyBatis（续）

**原始代码**：
```java
public class MybatisDemoApplication {
    private static final String H2_PASSWORD = "sa";  // 明文密码

    public static void main(String[] args) throws Exception {
        SqlSessionFactory sqlSessionFactory = buildSqlSessionFactory(H2_PASSWORD);
    }
}
```

**集成后代码**：
```java
import com.dbpwd.core.PasswordService;

public class MybatisDemoApplication {
    private static final String DB_PLACEHOLDER = "mybatis_demo_db";  // 占位符

    public static void main(String[] args) throws Exception {
        String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
        SqlSessionFactory sqlSessionFactory = buildSqlSessionFactory(plainPassword);
    }
}
```

**关键改动点**：
1. 将密码常量从 `"sa"` 改为占位符
2. 使用 `PasswordService.getDatabasePassword()` 获取密码

---

## 5.7 方案5: 纯 JDBC

**适用场景**：无框架的独立 Java 应用

**集成原理**：直接调用 PasswordService API 获取解密后的密码

**添加依赖**：
```xml
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 5.8 方案5: 纯 JDBC（续）

**原始代码**：
```java
public class SimpleJdbcDemo {
    private static final String DB_PASSWORD = "sa";  // 明文密码

    public static void main(String[] args) {
        String plainPassword = DB_PASSWORD;
        Connection conn = DriverManager.getConnection(url, username, plainPassword);
    }
}
```

**集成后代码**：
```java
import com.dbpwd.core.PasswordService;

public class SimpleJdbcDemo {
    private static final String DB_PLACEHOLDER = "simple_jdbc_db";  // 占位符

    public static void main(String[] args) {
        String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
        Connection conn = DriverManager.getConnection(url, username, plainPassword);
    }
}
```

**关键改动点**：
1. 添加 `import com.dbpwd.core.PasswordService;`
2. 密码常量从明文改为占位符
3. 使用 API 获取解密后的密码

---

## 5.9 方案6: Servlet ContextListener

**适用场景**：Servlet/Struts 等传统 Web 应用

**集成原理**：ServletContextListener 在容器启动时初始化密码服务

**添加依赖**：
```xml
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-web</artifactId>
    <version>1.0.0</version>
</dependency>
```

---

## 5.10 方案6: Servlet（续）

**web.xml 配置**：
```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app version="4.0">
    <!-- 配置 DatabasePasswordInitListener -->
    <listener>
        <listener-class>com.dbpwd.web.DatabasePasswordInitListener</listener-class>
    </listener>
</web-app>
```

**可选：自定义配置路径**：
```xml
<context-param>
    <param-name>dbpwd.keyFile</param-name>
    <param-value>/path/to/sm4.key</param-value>
</context-param>
```

---

## 5.11 方案6: Servlet（续）

**Servlet 代码修改**：

```java
import com.dbpwd.core.PasswordService;

@WebServlet("/api/users/*")
public class UserApiServlet extends HttpServlet {
    private static final String DB_PLACEHOLDER = "@servlet_demo_db@";

    @Override
    public void init() throws ServletException {
        // 使用 PasswordService 获取密码
        String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
        
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:file:...");
        dataSource.setUsername("sa");
        dataSource.setPassword(plainPassword);
    }
}
```

**关键改动点**：
1. web.xml 添加 `DatabasePasswordInitListener`
2. 使用 `PasswordService.getDatabasePassword()` 获取密码

---

## 6.1 集成模式选择指南

| 你的应用类型 | 推荐模式 | 难度 |
|------------|---------|-----|
| Spring Boot 新项目 | 方案2: Starter | ⭐ |
| 传统 Spring 项目 | 方案3: BeanPostProcessor | ⭐⭐ |
| Druid 连接池 | 方案1: PasswordCallback | ⭐⭐ |
| MyBatis 应用 | 方案4: PasswordService API | ⭐⭐ |
| 纯 JDBC 应用 | 方案5: PasswordService API | ⭐ |
| Servlet/Web 应用 | 方案6: ContextListener | ⭐⭐ |

---

## 6.2 使用流程

```
1. 安装
   mvn clean install -DskipTests

2. 初始化
   java -jar db-pwd-demo-init.jar

3. 集成
   添加依赖 + 修改配置

4. 验证
   mvn test
```

---

## 6.3 初始化步骤

```bash
# 初始化 H2 数据库和密码文件
java -jar db-pwd-demo-init/target/db-pwd-demo-init-1.0.0.jar --all
```

**生成的文件**：

```
~/.dbpwd/
├── sm4.key      # SM4 密钥（32字符 hex）
└── db.pwd       # 密码文件（JSON 格式）
```

---

## 6.4 Spring Boot 集成示例

**步骤 1：添加依赖**
```xml
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

**步骤 2：修改配置**
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: root
    password: "@my_db@"  # 改为占位符
```

---

## 6.5 纯 JDBC 集成示例

**原始代码**：
```java
String password = "sa";  // 明文密码
Connection conn = DriverManager.getConnection(url, user, password);
```

**集成后代码**：
```java
import com.dbpwd.core.PasswordService;

String password = PasswordService.getDatabasePassword("@my_db@");
Connection conn = DriverManager.getConnection(url, user, password);
```

---

## 7.1 功能完成度

| 功能模块 | 状态 | 说明 |
|---------|------|------|
| SM4-GCM 加密 | ✅ | 国密标准，认证加密防篡改 |
| 密码文件管理 | ✅ | JSON 格式，支持多数据库配置 |
| 密钥管理 | ✅ | 固定路径 `~/.dbpwd/` |
| 占位符解析 | ✅ | `@db_name@` 格式自动解密 |

---

## 7.2 集成模式完成度

| 集成模式 | Demo 应用 | 测试状态 |
|---------|----------|---------|
| 方案1: Druid | demos/druid/ | ✅ 通过 |
| 方案2: Spring Boot | demos/spring-boot/ | ✅ 通过 |
| 方案3: Spring | demos/spring/ | ✅ 通过 |
| 方案4: MyBatis | demos/mybatis/ | ✅ 通过 |
| 方案5: 纯 JDBC | demos/simple-jdbc/ | ✅ 通过 |
| 方案6: Servlet | demos/servlet/ | ✅ 通过 |

**总计**：22 个应用（11 demo + 11 original）全部测试通过

---

## 7.3 项目统计

| 指标 | 数量 |
|------|------|
| 核心模块 | 6 个 |
| Demo 应用 | 11 组 (22 个) |
| Java 代码 | ~20,300 行 |
| 文档 | 12 个 |

---

## 7.4 框架兼容性

| 框架 | 兼容版本 | 集成方式 |
|------|---------|---------|
| Spring Boot | 2.x | Starter 自动配置 |
| Spring Framework | 4.x ~ 5.x | BeanPostProcessor |
| Druid | 1.0.x ~ 1.2.x | PasswordCallback |
| MyBatis | 3.x | API 调用 |
| Servlet | 3.0+ / 4.x | ContextListener |

---

## 8.1 项目优势

| 优势 | 说明 |
|------|------|
| 🔐 **高安全性** | SM4-GCM 认证加密，应用无感知文件路径 |
| 🚀 **易集成** | 6 种模式，覆盖主流 Java 技术栈 |
| 📦 **零侵入** | 最小代码改动，配置文件只有占位符 |
| 📖 **文档完善** | 完整的集成指南和 API 文档 |
| 🧪 **测试充分** | 22 个 Demo 应用全部验证通过 |

---

## 8.2 适用场景

1. **企业应用**：生产环境密码安全管理
2. **合规要求**：国密算法合规场景
3. **多环境管理**：开发/测试/生产环境隔离
4. **旧系统改造**：Struts 1.x 等旧框架支持

---

## 8.3 未来规划

| 规划 | 说明 |
|------|------|
| Spring Boot 3.x 支持 | 需升级到 Java 17+ |
| 密钥轮换工具 | 定期更换密钥 |
| 配置中心集成 | 与 Apollo/Nacos 集成 |
| 英文文档 | 国际化支持 |

---

# 附录：文档索引

| 文档 | 说明 |
|------|------|
| `README.md` | 项目入口 |
| `docs/architecture.md` | 架构设计 |
| `docs/integration-guide.md` | **应用集成参考** |
| `docs/demo-applications.md` | Demo 应用设计 |
| `docs/api-reference.md` | API 参考 |
| `docs/user-guide.md` | 用户手册 |
| `docs/FAQ.md` | 常见问题 |
