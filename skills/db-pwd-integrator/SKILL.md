---
name: db-pwd-integrator
description: 自动扫描 Java 数据库应用并集成 db-pwd 密码加密功能，支持 Spring Boot/Spring/Druid/MyBatis/Servlet/纯JDBC 六种项目类型
license: MIT
compatibility: opencode
keywords:
  - 集成数据库密码加密
  - 添加 db-pwd 支持
  - 改造数据库密码管理
  - 密码加密集成
  - 集成 dbpwd
---

# db-pwd-integrator

自动扫描 Java 数据库应用，集成 db-pwd 数据库密码加密功能。

## 平台兼容性

- ✅ Windows (PowerShell/CMD)
- ✅ macOS
- ✅ Linux

本 Skill 仅使用内置工具 (`glob`, `grep`, `Read`, `Write`, `Edit`, `Bash`)，无需外部脚本，天然跨平台。

## 触发条件

- "集成数据库密码加密"
- "添加 db-pwd 支持"
- "改造数据库密码管理"
- "密码加密集成"
- "集成 dbpwd"

## 工作流程

### Phase 1: 项目扫描

使用内置工具扫描项目：

```
glob("**/pom.xml")
glob("**/application*.yml")
grep("DataSource|@SpringBootApplication|@Configuration", include="*.java")
```

#### 多模块项目处理

当检测到多个 pom.xml 时：

```
🔍 检测到多模块项目

┌────┬─────────────────────────┬──────────────┬─────────────┐
│ #  │ 模块名称                 │ 有数据库依赖  │ 需要集成    │
├────┼─────────────────────────┼──────────────┼─────────────┤
│ 0  │ parent (根目录)          │ -            │ ❌ 跳过     │
│ 1  │ service-a               │ ✅           │ ？请确认   │
│ 2  │ service-b               │ ✅           │ ？请确认   │
│ 3  │ common                  │ ❌           │ ❌ 跳过     │
└────┴─────────────────────────┴──────────────┴─────────────┘

请选择要集成的模块（输入编号，多个用逗号分隔）：1,2
```

**判断逻辑**：
- 父 pom（packaging=pom）：跳过
- 有 `spring-boot-starter-jdbc`/`druid`/`mybatis` 依赖的模块：标记为有数据库
- 无数据库依赖的 common/util 模块：建议跳过

**集成策略**：
1. 依赖添加到**具体子模块**的 pom.xml（不是父 pom）
2. 配置文件修改在**对应模块**的 resources 目录
3. 代码修改在**对应模块**的 src 目录

### Phase 2: 项目类型识别

按以下优先级检测：

| 优先级 | 检测信号 | 项目类型 | 集成方案 |
|--------|---------|---------|---------|
| 1 | `@SpringBootApplication` 或 `spring-boot-starter-parent` | Spring Boot | **方案C: BeanPostProcessor** |
| 2 | `DruidDataSource` + `@Configuration` | Spring + Druid | 方案1+3 |
| 3 | `@Configuration` + `spring-context` | Spring | 方案3 |
| 4 | `mybatis-config.xml` 或 `*Mapper.xml` | MyBatis | 方案4 |
| 5 | `web.xml` 或 `@WebServlet` | Servlet | 方案6 |
| 6 | `DriverManager.getConnection` 无框架 | 纯 JDBC | 方案5 |

**决策树**：
```
IF @SpringBootApplication OR spring-boot-starter-parent:
    → 方案C: db-pwd-spring-boot-starter (BeanPostProcessor 自动解密)
ELSE IF DruidDataSource:
    → 方案1: db-pwd-druid (使用 DbPwdDruidPasswordCallback 自动解密)
    → 注意: 不要使用 PasswordService.getDatabasePassword()
    → 如有 @Configuration: 可选加 db-pwd-spring 实现自动解密
ELSE IF @Configuration + DataSource Bean:
    → 方案3: db-pwd-spring
ELSE IF mybatis-config.xml OR *Mapper.xml:
    → 方案4: db-pwd-mybatis
ELSE IF web.xml OR @WebServlet/@WebListener:
    → 方案6: db-pwd-web
ELSE IF DriverManager.getConnection:
    → 方案5: db-pwd-core (手动调用 PasswordService)
ELSE:
    → 询问用户选择项目类型
```

**重要：方案1 vs 方案5 的选择**
```
Druid 项目 → 必须使用方案1 (DbPwdDruidPasswordCallback)
  ✅ 正确: dataSource.setPasswordCallback(new DbPwdDruidPasswordCallback())
  ❌ 错误: PasswordService.getDatabasePassword() + dataSource.setPassword()

纯 JDBC 项目 → 使用方案5 (PasswordService.getDatabasePassword)
  ✅ 正确: String pwd = PasswordService.getDatabasePassword("@db@")
```

**其他连接池支持**：
- HikariCP（Spring Boot 默认）→ 与 Starter 相同处理
- DBCP2 / C3P0 / Tomcat JDBC → 方案5（手动调用 PasswordService）
- JNDI DataSource → 在应用服务器配置或使用 DataSourceDecorator

**特殊场景**（详见 [troubleshooting.md](references/troubleshooting.md)）：
- Gradle 项目、多环境配置、配置中心、Docker/K8s、密码轮换、CI/CD

### Phase 3: 用户确认

展示检测结果并获取用户确认：

```
📊 项目分析结果

**检测到项目类型**: {type}
**推荐集成方案**: {pattern}
**集成模块**: {module}

需要修改的文件：
- {file1}: {change1}
- {file2}: {change2}

是否继续执行集成？[Y/n]
```

### Phase 4: 执行集成

#### 4.1 创建备份

```bash
mkdir -p .dbpwd-backup/$(date +%Y%m%d_%H%M%S)
cp pom.xml .dbpwd-backup/$(date +%Y%m%d_%H%M%S)/
# 备份所有将被修改的文件
```

#### 4.2 添加 Maven 依赖

**按方案添加依赖**：

| 方案 | 需要的依赖 | 说明 |
|------|-----------|------|
| 方案1: Druid | db-pwd-core + db-pwd-druid | 两个依赖都需要 |
| **方案C: Spring Boot Starter** | db-pwd-spring-boot-starter | **仅需此一个**，会自动传递 db-pwd-core |
| 方案3: Spring | db-pwd-core + db-pwd-spring | 两个依赖都需要 |
| 方案4: MyBatis | db-pwd-core | 纯 MyBatis 直接调用 API |
| 方案4变体: MyBatis XML | db-pwd-core + db-pwd-mybatis | 使用 DbPwdDataSourceFactory |
| 方案5: 纯 JDBC | db-pwd-core | 直接调用 PasswordService |
| 方案6: Servlet | db-pwd-core + db-pwd-web | 两个依赖都需要 |

**依赖示例**：
```xml
<!-- 方案1: Druid -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-druid</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- **方案C: Spring Boot Starter**（推荐，仅需一个）-->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 方案3: Spring -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-spring</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 方案4: MyBatis -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 方案5: 纯 JDBC -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 方案6: Servlet -->
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

#### 4.3 代码修改规则

**规则1: Java 代码替换**
```java
// BEFORE
private static final String DB_PASSWORD = "sa";

// AFTER
import com.dbpwd.core.PasswordService;
private static final String DB_PLACEHOLDER = "@my_db@";
```

**规则2: 配置文件替换**
```yaml
# BEFORE
spring.datasource.password: sa

# AFTER
spring.datasource.password: "@my_db@"
```

**规则3: 密码获取（方案5 纯 JDBC）**
```java
// BEFORE
String password = DB_PASSWORD;

// AFTER
String password = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
```

**规则4: Druid 配置（方案1 - 必须使用 PasswordCallback）**
```java
// BEFORE (明文密码)
DruidDataSource dataSource = new DruidDataSource();
dataSource.setPassword("sa");  // 明文密码

// AFTER (使用 PasswordCallback 自动解密)
import com.dbpwd.druid.DbPwdDruidPasswordCallback;

DruidDataSource dataSource = new DruidDataSource();
dataSource.setPassword("@my_db@");  // 设置占位符

DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback();
callback.setDataSource(dataSource);  // 重要: 让 callback 能获取原始密码
dataSource.setPasswordCallback(callback);

// ❌ 错误做法 - 不要在 Druid 项目中使用 PasswordService.getDatabasePassword()
// String password = PasswordService.getDatabasePassword("@my_db@");
// dataSource.setPassword(password);  // 这是方案5的做法，不适用于 Druid
```

**规则5: Spring 配置（方案3）**
```java
@Configuration
public class DatabaseConfig {
    @Bean
    public DataSourcePasswordProcessor dataSourcePasswordProcessor() {
        return new DataSourcePasswordProcessor();
    }
}
```

**规则6: MyBatis 配置（方案4）**
```xml
<dataSource type="com.dbpwd.mybatis.DbPwdDataSourceFactory">
    <property name="password" value="@my_db@"/>
</dataSource>
```

**规则7: Servlet 配置（方案6）**
```xml
<listener>
    <listener-class>com.dbpwd.web.DatabasePasswordInitListener</listener-class>
</listener>
```

**规则8: 更新 .gitignore**
```gitignore
# db-pwd 密码管理
.dbpwd/
sm4.key
db.pwd
```

### Phase 5: 验证

```bash
# 编译验证
mvn clean compile -DskipTests

# 检查项目
lsp_diagnostics
```

## 输出格式

集成完成后输出：

```
✅ 集成完成！

📋 修改摘要：
- 修改文件数: N
- 添加依赖数: M

📝 后续操作：
1. java -jar db-pwd-cli.jar --demo --generate-key  # 初始化密钥和Demo数据库
2. java -jar db-pwd-cli.jar --add --id my_db --url <url> --username <user> --password <pwd>  # 添加密码配置
3. mvn test  # 验证

🔒 安全提醒：
- 设置文件权限: chmod 600 ~/.dbpwd/sm4.key
- 切勿将密钥文件提交到版本控制
```

## 错误处理

| 场景 | 处理方式 |
|------|---------|
| 找不到 pom.xml | 询问是否为 Gradle 项目 |
| 混合框架 | 按优先级推荐，让用户确认 |
| 已集成 db-pwd | 询问是升级还是跳过 |
| 编译错误 | 回滚修改，显示错误详情 |

---

## 用户交互模板

### 场景 A：识别成功

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 📊 项目分析结果
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

**检测到项目类型**: Spring Boot 2.7.18
**推荐集成方案**: 方案C - BeanPostProcessor (默认模式)
**集成模块**: db-pwd-spring-boot-starter

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 📝 修改计划
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

需要修改的文件：
┌───────────────────────────────────────────────────────────────┐
│ 文件                                    │ 修改内容            │
├───────────────────────────────────────────────────────────────┤
│ pom.xml                                │ 添加依赖            │
│ src/main/resources/application.yml     │ 密码改为占位符格式  │
│ .gitignore                             │ 添加敏感文件配置    │
└───────────────────────────────────────────────────────────────┘

是否继续执行集成？[Y/n]
```

### 场景 B：无法识别

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 ❓ 无法自动识别项目类型
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

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
└────┴─────────────────────────┴────────────────────────────┘

请输入选项编号 (1-6)：
```

### 场景 C：已集成 db-pwd

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 ℹ️ 项目已集成 db-pwd
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

当前集成状态：
- db-pwd-core: 1.0.0 ✅
- db-pwd-spring-boot-starter: 1.0.0 ✅
- 占位符配置: 已配置 ✅

可用的操作：
┌────┬─────────────────────────────────────────────────────────┐
│ #  │ 操作                                                   │
├────┼─────────────────────────────────────────────────────────┤
│ 1  │ 升级到最新版本                                         │
│ 2  │ 检查集成状态（诊断）                                   │
│ 3  │ 添加新的数据库配置                                     │
│ 4  │ 取消                                                   │
└────┴─────────────────────────────────────────────────────────┘

请选择操作 [1-4]：
```

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
⚠️ 检测到 Jasypt 加密已集成

选项：
1. 完全迁移到 db-pwd（推荐）- 更强的安全性，国密合规
2. 并行使用（不推荐）- 两套密钥管理，增加复杂度
3. 保持现状 - 不进行集成
```

---

## 安全检查清单

### 集成前检查
- [ ] 代码库中无明文密码（除测试代码）
- [ ] .gitignore 已配置

### 集成后检查
- [ ] 密钥文件权限为 600（Linux/Mac）
- [ ] 配置目录权限为 700（Linux/Mac）
- [ ] 密钥文件不在版本控制中
- [ ] 密码文件不在版本控制中

---

## 详细参考

- [integration-patterns.md](references/integration-patterns.md) - 6 种集成模式详细说明
- [code-examples.md](references/code-examples.md) - 代码修改前后对比示例
- [troubleshooting.md](references/troubleshooting.md) - 常见问题处理

## 项目文档

- [AGENTS.md](../../AGENTS.md) - 项目上下文
- [integration-guide.md](../../docs/integration-guide.md) - 应用集成指南
- [api-reference.md](../../docs/api-reference.md) - PasswordService API

## 示例

**用户**: 帮我把这个项目集成数据库密码加密

**响应**:
1. 扫描项目结构
2. 识别为 Spring Boot 项目
3. 推荐方案C: BeanPostProcessor
4. 获取用户确认
5. 执行修改：添加依赖、修改配置
6. 验证编译
7. 提供后续操作指南
