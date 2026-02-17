# db-pwd-integrator Skill 设计参考文档

> 本文档为 db-pwd-integrator skill 的完整设计参考，包含详细的工作流程、交互模板和实现细节。
>
> 最后更新: 2026-02-17

## 目录

1. [概述](#概述)
2. [触发条件](#触发条件)
3. [工作流程](#工作流程)
4. [用户交互模板](#用户交互模板)
5. [代码转换规则](#代码转换规则)
6. [错误处理](#错误处理)
7. [边界情况处理](#边界情况处理)
8. [回滚机制](#回滚机制)
9. [安全检查清单](#安全检查清单)
10. [诊断功能](#诊断功能)

---

## 概述

### 功能说明

db-pwd-integrator 是一个自动扫描 Java 数据库应用并集成 db-pwd 密码加密功能的 skill。

**核心能力**：
1. **自动检测**项目类型（Spring Boot/Spring/Druid/MyBatis/Servlet/纯JDBC）
2. **智能推荐**最适合的集成方案
3. **自动执行**代码和配置修改
4. **验证集成**是否成功

### 平台兼容性

- ✅ Windows (PowerShell/CMD)
- ✅ macOS
- ✅ Linux

本 Skill 仅使用内置工具 (`glob`, `grep`, `Read`, `Write`, `Edit`, `Bash`)，无需外部脚本，天然跨平台。

---

## 触发条件

用户请求包含以下关键词时触发此 skill：

- "集成数据库密码加密"
- "添加 db-pwd 支持"
- "改造数据库密码管理"
- "密码加密集成"
- "集成 dbpwd"
- "dbpwd integrate"
- "密码加密改造"

---

## 工作流程

### Phase 1: 项目扫描

**任务**：收集项目结构信息

**操作步骤**：

1. **扫描构建文件**
```
使用工具：
- glob("**/pom.xml")
- glob("**/build.gradle")

提取信息：
- 项目依赖列表
- Java 版本
- 框架版本
- 多模块结构
```

2. **扫描配置文件**
```
使用工具：
- glob("**/application*.yml")
- glob("**/application*.properties")
- glob("**/applicationContext*.xml")
- glob("**/mybatis-config.xml")
- glob("**/web.xml")

提取信息：
- 数据库连接配置
- 密码存储位置
- 已有加密配置
```

3. **扫描 Java 源码**
```
使用工具：
- grep("DataSource|getConnection|setPassword", include="*.java")
- grep("@Configuration|@Bean|@SpringBootApplication", include="*.java")
- grep("extends HttpServlet|@WebServlet|@WebListener", include="*.java")

提取信息：
- DataSource 配置位置
- 密码获取方式
- 框架使用情况
```

4. **检测明文密码**
```
使用工具：
- grep('password\\s*=\\s*"[^"@]', include="*.java")
- grep('password\\s*:\\s*[^"@]', include="*.yml")
- grep('password\\s*=\\s*[^@]', include="*.properties")

输出：
- 明文密码位置列表（文件:行号:内容预览）
```

**输出结果示例**：
```
📊 项目扫描结果

项目信息：
- 构建工具: Maven
- Java 版本: 1.8
- 模块数量: 1

检测到的框架：
- Spring Boot 2.7.x ✅
- Druid 1.2.x ✅

配置文件：
- src/main/resources/application.yml
- src/main/resources/application-dev.yml

检测到的 DataSource：
- primaryDataSource (DruidDataSource)

明文密码位置：
- application.yml:15 - password: sa
- DatabaseConfig.java:23 - private String password = "sa";
```

---

### Phase 2: 项目类型识别

**检测规则（按优先级）**：

| 优先级 | 类型 | 检测信号 | 集成方案 |
|--------|------|---------|---------|
| 1 | Spring Boot | `@SpringBootApplication` 或 `spring-boot-starter-parent` | 方案2: Starter |
| 2 | Spring + Druid | `@Configuration` + `DruidDataSource` 类 | 方案1+3: Druid+Spring |
| 3 | Spring Framework | `@Configuration` + `spring-context` | 方案3: BeanPostProcessor |
| 4 | MyBatis | `mybatis-config.xml` 或 `*Mapper.xml` | 方案4: DataSourceFactory |
| 5 | Servlet | `web.xml` 或 `@WebServlet` | 方案6: InitListener |
| 6 | 纯 JDBC | `DriverManager.getConnection` 无框架 | 方案5: Standalone |

**决策树逻辑**：

```
IF pom.xml 包含 spring-boot-starter OR 检测到 @SpringBootApplication:
    → 方案2: Spring Boot Starter
    → 使用 db-pwd-spring-boot-starter

ELSE IF 检测到 DruidDataSource 类:
    IF 检测到 @Configuration:
        → 方案1+3: Druid + Spring
        → 使用 db-pwd-druid + db-pwd-spring
    ELSE:
        → 方案1: Druid PasswordCallback
        → 使用 db-pwd-druid

ELSE IF 检测到 @Configuration + DataSource Bean:
    → 方案3: Spring BeanPostProcessor
    → 使用 db-pwd-spring

ELSE IF 检测到 mybatis-config.xml OR *Mapper.xml:
    → 方案4: MyBatis DataSourceFactory
    → 使用 db-pwd-mybatis

ELSE IF 检测到 web.xml OR @WebServlet/@WebListener:
    → 方案6: Servlet InitListener
    → 使用 db-pwd-web

ELSE IF 检测到 DriverManager.getConnection:
    → 方案5: StandalonePasswordDecryptor
    → 使用 db-pwd-core

ELSE:
    → 无法识别，进入交互模式
```

**混合框架处理**：

当检测到多个框架信号时，按以下规则处理：

```
检测到的框架：
- Spring Boot ✅
- Druid ✅
- MyBatis ✅

推荐策略：
1. 以 Spring Boot 为主框架
2. Druid 和 MyBatis 透明继承 Spring Boot 的配置
3. 只需添加 db-pwd-spring-boot-starter

理由：
- Spring Boot Starter 会自动装饰所有 DataSource
- 包括 Druid 和 MyBatis 使用的数据源
- 改动最小，兼容性最好
```

---

### Phase 3: 用户确认

参见 [用户交互模板](#用户交互模板)

---

### Phase 4: 执行集成修改

**执行前准备**：

1. **创建备份**
```
备份目录: .dbpwd-backup/{timestamp}/
备份内容:
- pom.xml.bak
- 所有将被修改的配置文件
- manifest.json (备份清单)
```

2. **确认 db-pwd 版本**
```
检查项目是否已有 db-pwd 依赖：
- 如果有，使用相同版本
- 如果没有，使用最新稳定版本 (1.0.0)
```

参见 [代码转换规则](#代码转换规则)

---

### Phase 5: 验证与后续

#### 5.1 编译验证

```
执行命令: mvn clean compile -DskipTests

验证项:
✅ pom.xml 语法正确
✅ 依赖可解析
✅ Java 语法正确
✅ 无编译错误
```

#### 5.2 配置验证

参见 [安全检查清单](#安全检查清单)

#### 5.3 完成提示

参见 [用户交互模板 - 完成提示](#场景-d完成提示)

---

## 用户交互模板

### 场景 A：识别成功

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 📊 项目分析结果
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

**检测到项目类型**: Spring Boot 2.7.18
**推荐集成方案**: 方案2 - Spring Boot Starter
**集成模块**: db-pwd-spring-boot-starter

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 📝 修改计划
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

需要修改的文件：

┌───────────────────────────────────────────────────────────────┐
│ 文件                                    │ 修改内容            │
├───────────────────────────────────────────────────────────────┤
│ pom.xml                                │ 添加 2 个依赖       │
│ src/main/resources/application.yml     │ 密码改为占位符格式  │
│ .gitignore                             │ 添加敏感文件配置    │
└───────────────────────────────────────────────────────────────┘

将添加的依赖：

  <dependency>
      <groupId>com.dbpwd</groupId>
      <artifactId>db-pwd-core</artifactId>
      <version>1.0.0</version>
  </dependency>
  <dependency>
      <groupId>com.dbpwd</groupId>
      <artifactId>db-pwd-spring-boot-starter</artifactId>
      <version>1.0.0</version>
  </dependency>

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 ⚠️  后续操作提醒
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. 需要初始化密钥文件：
   java -jar db-pwd-demo-init-1.0.0.jar

2. 需要添加数据库密码配置：
   java -jar db-pwd-core-1.0.0.jar add <db_name>

3. 建议将 .dbpwd/ 添加到 .gitignore（将自动处理）

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

是否继续执行集成？[Y/n/详细]
```

### 场景 B：无法识别

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 ❓ 无法自动识别项目类型
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

检测到的信号：
- 存在 pom.xml
- 存在 Java 源码
- 未检测到已知框架

可能的原因：
1. 使用了非标准的框架配置
2. 项目结构特殊
3. 这是一个新项目

请选择您的项目类型：

┌────┬─────────────────────────┬────────────────────────────┐
│ #  │ 项目类型                 │ 说明                       │
├────┼─────────────────────────┼────────────────────────────┤
│ 1  │ Spring Boot 应用        │ 使用 Spring Boot 框架      │
│ 2  │ Spring + Druid 应用     │ 传统 Spring + Druid 连接池 │
│ 3  │ 传统 Spring 应用        │ 使用 Spring Framework     │
│ 4  │ MyBatis 应用            │ 纯 MyBatis，无 Spring     │
│ 5  │ Servlet/Web 应用        │ 传统 Java Web 应用        │
│ 6  │ 纯 JDBC 应用            │ 无框架，直接使用 JDBC     │
│ 7  │ 自定义配置              │ 手动指定集成参数          │
└────┴─────────────────────────┴────────────────────────────┘

请输入选项编号 (1-7)：
```

### 场景 C：已集成 db-pwd

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 ℹ️ 项目已集成 db-pwd
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

当前集成状态：
- db-pwd-core: 1.0.0 ✅
- db-pwd-spring-boot-starter: 1.0.0 ✅
- 占位符配置: 已配置 ✅
- 密钥文件: 存在 ✅

可用的操作：

┌────┬─────────────────────────────────────────────────────────┐
│ #  │ 操作                                                   │
├────┼─────────────────────────────────────────────────────────┤
│ 1  │ 升级到最新版本                                         │
│ 2  │ 检查集成状态（诊断）                                   │
│ 3  │ 添加新的数据库配置                                     │
│ 4  │ 重新集成（覆盖现有配置）                               │
│ 5  │ 取消                                                   │
└────┴─────────────────────────────────────────────────────────┘

请选择操作 [1-5]：
```

### 场景 D：完成提示

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 ✅ 集成完成！
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

📋 修改摘要：
- 修改文件数: 3
- 添加依赖数: 2
- 添加导入语句: 1
- 替换密码位置: 2

📁 备份位置：
.dbpwd-backup/20260217_134500/

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 📝 后续操作（必须执行）
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

1. 初始化密钥文件（首次使用必须执行）：
   
   java -jar db-pwd-demo-init-1.0.0.jar
   
   这将在 ~/.dbpwd/ 目录创建：
   - sm4.key (加密密钥)
   - db.pwd (密码文件)

2. 添加数据库密码配置：
   
   java -jar db-pwd-core-1.0.0.jar add my_db
   
   或直接编辑 ~/.dbpwd/db.pwd 文件

3. 验证集成：
   
   mvn clean compile
   mvn test

4. 运行应用测试数据库连接

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 🔒 安全提醒
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

Linux/Mac 用户请设置文件权限：
  chmod 700 ~/.dbpwd
  chmod 600 ~/.dbpwd/sm4.key
  chmod 600 ~/.dbpwd/db.pwd

⚠️ 切勿将以下文件提交到版本控制：
  - ~/.dbpwd/ 目录
  - sm4.key
  - db.pwd

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

如需回滚，执行：
  cp .dbpwd-backup/20260217_134500/* . -r

或使用 git checkout 恢复文件。
```

**用户输入处理**：

| 用户输入 | 处理方式 |
|---------|---------|
| `Y` / `y` / `yes` / `是` | 继续执行集成 |
| `N` / `n` / `no` / `否` | 取消操作，不做任何修改 |
| `详细` / `detail` | 显示详细的修改内容 |
| `1-7` 数字 | 选择对应的项目类型 |

---

## 代码转换规则

### 规则 1: 添加 Maven 依赖

**通用依赖（所有项目都需要）**：
```xml
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>${dbpwd.version}</version>
</dependency>
```

**按方案添加额外依赖**：

| 方案 | 额外依赖 |
|------|---------|
| 方案1: Druid | db-pwd-druid |
| 方案2: Spring Boot | db-pwd-spring-boot-starter |
| 方案3: Spring | db-pwd-spring |
| 方案4: MyBatis | db-pwd-mybatis |
| 方案5: 纯 JDBC | 无 |
| 方案6: Servlet | db-pwd-web |

**插入位置**：
- 在 `</dependencies>` 标签前插入
- 如果有 `spring-boot-starter` 依赖，在其后插入
- 保持依赖按 groupId 字母排序

---

### 规则 2: 密码常量/变量替换

**Java 代码替换**：

```java
// ========== BEFORE ==========
private static final String DB_PASSWORD = "sa";
// 或
private String password = "your_password";
// 或
String dbPassword = "admin123";

// ========== AFTER ==========
import com.dbpwd.core.PasswordService;  // 添加导入

private static final String DB_PLACEHOLDER = "@my_db@";  // 占位符常量
```

**配置文件替换**：

```yaml
# ========== BEFORE ==========
spring:
  datasource:
    password: sa

# ========== AFTER ==========
spring:
  datasource:
    password: "@my_db@"  # 占位符格式
```

```properties
# ========== BEFORE ==========
spring.datasource.password=sa

# ========== AFTER ==========
spring.datasource.password=@my_db@
```

---

### 规则 3: 密码获取方式替换

**方案 1-4, 6（自动解密）**：

只需将密码改为占位符格式，框架会自动解密：
```java
// 配置文件或代码中
dataSource.setPassword("@my_db@");
// 或
spring.datasource.password: "@my_db@"
```

**方案 5: 纯 JDBC（手动调用）**：

```java
// ========== BEFORE ==========
String password = DB_PASSWORD;
Connection conn = DriverManager.getConnection(url, username, password);

// ========== AFTER ==========
import com.dbpwd.core.PasswordService;  // 添加导入

String password = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
Connection conn = DriverManager.getConnection(url, username, password);
```

---

### 规则 4: Spring 特定配置

**方案 3: Spring BeanPostProcessor（非 Boot）**：

在 `@Configuration` 类中添加：

```java
import com.dbpwd.spring.DataSourcePasswordProcessor;

@Configuration
public class DatabaseConfig {
    
    // 新增 Bean
    @Bean
    public DataSourcePasswordProcessor dataSourcePasswordProcessor() {
        return new DataSourcePasswordProcessor();  // 使用默认路径 ~/.dbpwd/
    }
    
    @Bean
    public DataSource dataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:mysql://localhost:3306/mydb");
        ds.setUsername("root");
        ds.setPassword("@my_db@");  // 占位符格式
        return ds;
    }
}
```

---

### 规则 5: MyBatis 特定配置

```xml
<!-- ========== BEFORE ========== -->
<dataSource type="POOLED">
    <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
    <property name="url" value="jdbc:mysql://localhost:3306/mydb"/>
    <property name="username" value="root"/>
    <property name="password" value="sa"/>
</dataSource>

<!-- ========== AFTER ========== -->
<dataSource type="com.dbpwd.mybatis.DbPwdDataSourceFactory">
    <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
    <property name="url" value="jdbc:mysql://localhost:3306/mydb"/>
    <property name="username" value="root"/>
    <property name="password" value="@my_db@"/>
</dataSource>
```

---

### 规则 6: Servlet 特定配置

**web.xml 方式**：

```xml
<!-- 添加监听器 -->
<listener>
    <listener-class>com.dbpwd.web.DatabasePasswordInitListener</listener-class>
</listener>

<!-- Servlet 中使用 -->
<context-param>
    <param-name>dbPlaceholder</param-name>
    <param-value>@my_db@</param-value>
</context-param>
```

**注解方式**：

```java
@WebListener
public class MyContextListener implements ServletContextListener {
    
    private static final String DB_PLACEHOLDER = "@my_db@";
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // 使用 PasswordService 获取密码
        String password = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
        
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:mysql://localhost:3306/mydb");
        ds.setUsername("root");
        ds.setPassword(password);
        
        sce.getServletContext().setAttribute("dataSource", ds);
    }
}
```

---

### 规则 7: 更新 .gitignore

在 `.gitignore` 文件末尾添加：

```gitignore
# db-pwd 密码管理
.dbpwd/
sm4.key
db.pwd
```

---

## 错误处理

### 扫描阶段错误

| 错误 | 处理方式 |
|------|---------|
| 找不到 pom.xml | 询问是否为 Gradle 项目，或手动提供项目信息 |
| 无法读取配置文件 | 尝试多种编码，提示用户确认文件位置 |
| 找不到 src 目录 | 询问用户源码目录位置 |

### 识别阶段错误

| 错误 | 处理方式 |
|------|---------|
| 混合框架 | 按优先级推荐，列出所有检测到的框架供用户选择 |
| 版本不兼容 | 警告用户并提供升级建议或替代方案 |
| 已集成 | 询问是升级、检查状态还是重新集成 |

### 执行阶段错误

| 错误 | 处理方式 |
|------|---------|
| 文件内容与预期不符 | 显示 diff，让用户确认处理方式 |
| 编译错误 | 回滚修改，显示错误详情 |
| 依赖下载失败 | 检查仓库配置，提供离线方案 |

---

## 边界情况处理

### 环境变量密码

```java
// BEFORE
@Value("${DB_PASSWORD}")
private String password;

// AFTER - 方案 A：保留环境变量，值改为占位符
// 环境变量: DB_PASSWORD=@my_db@
@Value("${DB_PASSWORD}")
private String placeholder;

String password = PasswordService.getDatabasePassword(placeholder);
```

### 多数据源

```
检测到多数据源时：

┌────┬──────────────────┬───────────────────────────┐
│ #  │ Bean 名称         │ 占位符名称                │
├────┼──────────────────┼───────────────────────────┤
│ 1  │ primaryDataSource │ @primary_db@             │
│ 2  │ secondaryDS       │ [请输入或跳过]            │
└────┴──────────────────┴───────────────────────────┘

逐个确认每个数据源的占位符名称。
```

### Jasypt 冲突

```
检测到 Jasypt 时的提示：

⚠️ 检测到 Jasypt 加密已集成

选项：
1. 完全迁移到 db-pwd（推荐）- 更强的安全性，国密合规
2. 并行使用（不推荐）- 两套密钥管理，增加复杂度
3. 保持现状 - 不进行集成
```

---

## 回滚机制

### 自动备份

修改前自动创建备份：
```
.dbpwd-backup/{timestamp}/
├── pom.xml.bak
├── application.yml.bak
├── DatabaseConfig.java.bak
└── manifest.json
```

### manifest.json 结构

```json
{
  "timestamp": "2026-02-17T13:45:00Z",
  "projectType": "spring-boot",
  "integrationPattern": "starter",
  "files": [
    {
      "path": "pom.xml",
      "backup": "pom.xml.bak",
      "changes": ["添加 db-pwd-core 依赖", "添加 db-pwd-spring-boot-starter 依赖"]
    }
  ]
}
```

### 回滚命令

```
🔄 执行回滚...

从备份 .dbpwd-backup/20260217_134500/ 恢复：
✅ pom.xml → 已恢复
✅ application.yml → 已恢复
✅ DatabaseConfig.java → 已恢复

回滚完成！

提示：密钥文件 (~/.dbpwd/) 未被删除，如需完全清理请手动删除。
```

---

## 安全检查清单

### 集成前检查

- [ ] S001: 代码库中无明文密码（除测试代码）
- [ ] S002: .gitignore 已配置

### 集成后检查

- [ ] S003: 密钥文件权限为 600（Linux/Mac）
- [ ] S004: 配置目录权限为 700（Linux/Mac）
- [ ] S005: 密钥文件不在版本控制中
- [ ] S006: 密码文件不在版本控制中

### 持续检查

- [ ] S007: 定期密钥轮换（建议每 90 天）

---

## 诊断功能

### 诊断命令

当用户请求诊断时，执行以下检查：

```
🔍 db-pwd 集成诊断

环境信息：
- Java 版本
- Maven/Gradle 版本
- 操作系统

密钥文件状态：
- ~/.dbpwd/sm4.key 是否存在
- 密钥格式是否正确（32 字符 hex）
- ~/.dbpwd/db.pwd 是否存在
- 密码文件格式是否正确
- 已配置的数据库列表

集成状态：
- 集成模式
- 集成模块版本
- 占位符配置

连接测试：
- 密码解密测试
- 数据库连接测试
- 简单查询测试
```

### 常见问题

| 问题 | 可能原因 | 解决方案 |
|------|---------|---------|
| 密码解密失败 | 密钥文件不存在/格式错误 | 运行 db-pwd-demo-init.jar 初始化 |
| 找不到占位符 | db.pwd 中未配置 | 运行 db-pwd-core.jar add 添加 |
| DataSource 初始化失败 | BeanPostProcessor 未配置 | 检查 Spring 配置 |
| 编译错误 | 依赖未正确添加 | 检查 pom.xml，重新添加依赖 |

---

## 参考文档

### Skill 内部参考

- `references/integration-patterns.md` - 6 种集成模式详细说明
- `references/code-examples.md` - 代码修改前后对比示例
- `references/troubleshooting.md` - 常见问题处理

### 项目文档

- `AGENTS.md` - 项目上下文和技术栈
- `docs/integration-patterns.md` - 6 种集成模式详细说明
- `docs/integration-code-details.md` - 代码修改细节对比
- `docs/api-reference.md` - PasswordService API 使用指南
