# Struts 零代码集成 Demo

> **数据库密码管理零代码集成方案 - Struts 1.x 演示**
>
> 版本: 6.0 - 零代码集成版
> 更新时间: 2026-02-23

---

## 📋 目录

1. [概述](#概述)
2. [零代码集成原理](#零代码集成原理)
3. [快速开始](#快速开始)
4. [代码对比](#代码对比)
5. [集成步骤](#集成步骤)
6. [技术细节](#技术细节)
7. [常见问题](#常见问题)

---

## 概述

### 什么是零代码集成？

传统的密码加密集成方案通常需要修改 Action 代码，引入密码解密 API 调用。本 demo 展示了如何通过 `ServletContextListener` 和 `DataSourceDecorator` 实现真正的零代码集成。

**核心思想：**
- 在应用启动时，通过 Listener 初始化并装饰 DataSource
- 将装饰后的 DataSource 存储到 ServletContext
- Action 代码保持不变，直接从 ServletContext 获取 DataSource
- 密码解密由 DataSourceDecorator 在 getConnection() 时自动完成

### 关键特性

| 特性 | 说明 |
|------|------|
| 🚫 **零代码修改** | Action 代码完全不需要修改 |
| 🔐 **SM4-GCM 加密** | 国密算法，安全可靠 |
| ⚡ **自动解密** | getConnection() 时自动解密密码 |
| 🎯 **典型模式** | 符合 Struts 最佳实践 |
| 📊 **最小改动** | 只需配置文件修改 |

---

## 零代码集成原理

### 架构图

```
┌─────────────────────────────────────────────────────────┐
│              应用启动流程（Listener执行）                  │
├─────────────────────────────────────────────────────────┤
│  1. contextInitialized() 被调用                         │
│  2. 加载 SM4 密钥                                    │
│  3. 创建原始 DruidDataSource（密码占位符）               │
│  4. 使用 DataSourceDecorator 包装（自动解密）            │
│  5. 存储到 ServletContext.getAttribute("dataSource")   │
└─────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────┐
│              Action 执行流程（获取连接）                  │
├─────────────────────────────────────────────────────────┤
│  1. Action.execute() 执行                              │
│  2. 从 ServletContext 获取 DataSource                  │
│     ds = (DataSource) context.getAttribute("dataSource")│
│  3. 调用 ds.getConnection()                            │
│  4. DataSourceDecorator 自动解密密码                     │
│  5. 返回数据库连接                                      │
└─────────────────────────────────────────────────────────┘
```

### 核心组件

#### 1. DbPwdStrutsInitializer（ServletContextListener）

```java
@WebListener
public class DbPwdStrutsInitializer implements ServletContextListener {
    public void contextInitialized(ServletContextEvent sce) {
        // 1. 加载SM4密钥
        String sm4Key = Sm4KeyLoader.loadKey();

        // 2.创建原始数据源（密码占位符）
        DruidDataSource rawDs = createRawDataSource();
        rawDs.setPassword("@db_name@");

        // 3. 使用装饰器包装
        DataSourceDecorator decoratedDs = new DataSourceDecorator(
            rawDs, "db_name", sm4Key
        );

        // 4. 存储到ServletContext
        context.setAttribute("dataSource", decoratedDs);
    }
}
```

#### 2. UserListAction（Struts Action）

```java
public class UserListAction extends Action {
    public ActionForward execute(...) throws Exception {
        // ← 关键：直接获取，无需调用解密API
        DataSource ds = (DataSource) getServlet()
            .getServletContext().getAttribute("dataSource");

        // ← 自动解密，零代码！
        try (Connection conn = ds.getConnection()) {
            // 使用数据库连接
        }
    }
}
```

#### 3. DataSourceDecorator（自动解密）

```java
public class DataSourceDecorator implements DataSource {

    @Override
    public Connection getConnection() throws SQLException {
        // 在获取连接前确保密码已解密
        ensurePasswordDecrypted();
        return delegate.getConnection();
    }

    private void ensurePasswordDecrypted() {
        if (!passwordDecrypted) {
            // 从 db.pwd 文件获取加密密码
            String encrypted = getPasswordFromDbFile();

            // 使用 SM4-GCM 解密
            String plain = PasswordDecryptor.decrypt(encrypted, sm4Key);

            // 设置到原始数据源
            ((DruidDataSource) delegate).setPassword(plain);

            passwordDecrypted = true;
        }
    }
}
```

---

## 快速开始

### 前置条件

- JDK 8 或更高版本
- Maven 3.6+
- 已编译 db-pwd 核心模块

### 1. 编译项目

```bash
# 编译核心模块
mvn clean install -pl db-pwd-core -am -DskipTests

# 编译 demo
mvn clean package -pl demos/struts-zero-code/demo -DskipTests
```

### 2. 初始化数据库

```bash
# 使用 demo-init 工具初始化
java -jar db-pwd-demo-init/target/db-pwd-demo-init-1.0.0.jar \
  --db struts_demo_zero_code_db \
  --username sa \
  --password sa
```

### 3. 运行 Demo

```bash
# 使用 Jetty 运行（推荐）
mvn jetty:run -pl demos/struts-zero-code/demo
```

### 4. 访问应用

打开浏览器访问：

- **首页**: http://localhost:8085/dbpwd-struts-zero-code-demo
- **用户列表**: http://localhost:8085/dbpwd-struts-zero-code-demo/users

---

## 代码对比

### 原始版本（不安全）

```java
public class UserListAction extends Action {
    private static final String H2_PASSWORD = "sa";  // ⚠️ 明文密码

    public ActionForward execute(...) throws Exception {
        // 直接使用 DriverManager，密码明文
        Connection conn = DriverManager.getConnection(
            H2_URL, H2_USERNAME, H2_PASSWORD
        );
        // ...
    }
}
```

**问题：**
- ❌ 密码明文硬编码
- ❌ 每次都创建连接（无连接池）
- ❌ 不符合安全要求

### 零代码集成版本（安全）

```java
public class UserListAction extends Action {
    public ActionForward execute(...) throws Exception {
        // ← 直接获取，无需任何密码处理代码
        DataSource ds = (DataSource) getServlet()
            .getServletContext().getAttribute("dataSource");

        // ← 自动解密，零代码！连接池已配置
        try (Connection conn = ds.getConnection()) {
            // ...
        }
    }
}
```

**改进：**
- ✅ 密码加密存储（SM4-GCM）
- ✅ 使用连接池（Druid）
- ✅ 符合安全要求
- ✅ Action 代码零修改

---

## 集成步骤

### 步骤 1: 添加依赖

在 `pom.xml` 中添加 db-pwd 核心模块依赖：

```xml
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 步骤 2: 配置 Listener（唯一需要修改的配置文件）

在 `web.xml` 中添加：

```xml
<listener>
    <listener-class>com.dbpwd.web.DbPwdStrutsInitializer</listener-class>
</listener>
```

**或使用注解（Servlet 3.0+）**

在 Listener 类上添加 `@WebListener` 注解：

```java
@WebListener
public class DbPwdStrutsInitializer implements ServletContextListener {
    // ...
}
```

### 步骤 3: Action 保持原有逻辑

无需修改 Action 代码！这是零代码集成的核心优势。

```java
// 原有代码保持不变
DataSource ds = (DataSource) getServlet()
    .getServletContext().getAttribute("dataSource");

// 现在会自动解密密码，效果与原来完全相同
Connection conn = ds.getConnection();
```

### 步骤 4: 完成！

密码已自动加密/解密，应用无需其他修改。

---

## 技术细节

### 密码存储

SM4-GCM 加密的密码存储在 `~/.dbpwd/db.pwd`：

```json
{
  "databases": [
    {
      "id": "struts_demo_zero_code_db",
      "password": "ENC(SM4-GCM:abc123:xyz789)",
      "url": "jdbc:h2:file:~/.dbpwd/h2_data/dbpwd_struts_zero_code_demo",
      "username": "sa",
      "createdAt": "2026-02-23T00:00:00Z"
    }
  ]
}
```

### 密钥存储

SM4 密钥存储在 `~/.dbpwd/sm4.key`（32位十六进制字符串）：

```
a1b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6
```

### 占位符格式

在代码和配置中使用占位符：

```java
// 占位符格式
String DB_PLACEHOLDER = "@struts_demo_zero_code_db@";
```

占位符会被 `DataSourceDecorator` 自动识别并到 `db.pwd` 文件查找对应的加密密码。

### 自动解密时机

密码解密发生在第一次调用 `ds.getConnection()` 时：

```
ds.getConnection()
    ↓
DataSourceDecorator.getConnection()
    ↓
ensurePasswordDecrypted() [第一次]
    ↓
从 ~/.dbpwd/db.pwd 读取加密密码
    ↓
使用 ~/.dbpwd/sm4.key 解密
    ↓
设置到原始 DataSource
    ↓
返回连接
```

第二次及以后的 `getConnection()` 调用会直接使用已解密的密码，无需重复解密操作。

---

## 常见问题

### Q1: 为什么不需要修改 Action 代码？

**A:**
- DataSource 在 ServletContextListener 启动时创建并装饰
- Action 只需从 ServletContext 获取，无需关心密码细节
- 密码解密在第一次 `getConnection()` 时自动完成

### Q2: 与当前方案5相比有什么优势？

**A:**

| 对比项 | 当前方案5（WebPasswordUtil） | 零代码集成方案 |
|--------|----------------------------|---------------|
| Action 代码修改 | ✅ 需要 | ❌ 不需要 |
| 代码复杂度 | 增加 | 不变 |
| 集成难度 | 中等 | 简单 |
| 维护成本 | 较高 | 低 |
| 可移植性 | 一般 | 好 |

### Q3: 什么时候应该使用零代码集成？

**A:**
- 你的 Action 已经从 ServletContext 获取 DataSource
- 你希望最小化代码改动
- 你正在维护大型遗留应用
- 你遵循 Struts 最佳实践

### Q4: 如果我的 Action 自己创建 DataSource 怎么办？

**A:**
需要重构代码，使用 ServletContext 存储共享的 DataSource。这是 Struts 应用的典型做法，也是零代码集成的必要条件。

**修改前（反模式）：**
```java
public ActionForward execute(...) {
    DruidDataSource ds = new DruidDataSource();  // ❌ 自己创建
    ds.setPassword(password);
}
```

**修改后（推荐模式）：**
```java
public ActionForward execute(...) {
    DataSource ds = (DataSource) context.getAttribute("dataSource");  // ✅ 共享
}
```

### Q5: DataSourceDecorator 如何工作？

**A:**
`DataSourceDecorator` 使用装饰器模式包装原始 DataSource：
- 实现相同的 `javax.sql.DataSource` 接口
- 第一次调用 `getConnection()` 时自动解密密码
- 透明地转发所有其他方法调用到原始 DataSource
- 对 Action 代码完全透明

### Q6: 如何处理多个数据库？

**A:**
可以在 ServletContextListener 中创建多个装饰后的 DataSource，使用不同的 key 存储：

```java
context.setAttribute("db1DataSource", decoratedDs1);
context.setAttribute("db2DataSource", decoratedDs2);

// Action 中使用
DataSource ds1 = (DataSource) context.getAttribute("db1DataSource");
DataSource ds2 = (DataSource) context.getAttribute("db2DataSource");
```

### Q7: 连接池配置在哪里？

**A:**
在 `DbPwdStrutsInitializer` 中配置。示例代码已经配置了以下参数：

```java
dataSource.setInitialSize(2);
dataSource.setMaxActive(10);
dataSource.setMaxWait(10000);
dataSource.setTestWhileIdle(true);
dataSource.setValidationQuery("SELECT 1");
```

可以根据实际需求调整这些参数。

---

## 总结

### 零代码集成的价值

1. **最小化风险** - 只修改配置文件，代码改动最小
2. **易于维护** - 业务逻辑不受影响，降低维护成本
3. **快速集成** - 只需3步完成集成（依赖+Listener+使用DataSource）
4. **符合最佳实践** - 遵循 Struts 应用的典型架构模式

### 与其他方案的对比

| 方案 | 改动量 | 代码修改难度 | 适用场景 |
|------|--------|------------|---------|
| 方案5 | 中 | 需要修改代码 | Action 自己创建 DataSource |
| **零代码集成** | **小** | **无需修改** | **Action 从 context 获取** |
| 方案1 | 小 | 中等 | Druid 连接池 |
| 方案2 | 最小 | 无 | Spring Boot |

### 下一步

- 查看 `DbPwdStrutsInitializer.java` 了解详细实现
- 查看 `UserListAction.java` 了解 Action 使用方式
- 参考 `original/` 目录对比原始实现

---

> 文档版本: 6.0
> 最后更新: 2026-02-23
> 作者: Database Password Manager Team
