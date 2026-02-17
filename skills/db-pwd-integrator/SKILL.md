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

### 1. 项目扫描

使用内置工具扫描项目：

```
glob("**/pom.xml")
glob("**/application*.yml")
grep("DataSource|@SpringBootApplication|@Configuration", include="*.java")
```

### Phase 2: 项目类型识别

按以下优先级检测：

| 优先级 | 检测信号 | 项目类型 | 集成方案 |
|--------|---------|---------|---------|
| 1 | `@SpringBootApplication` 或 `spring-boot-starter-parent` | Spring Boot | 方案2: Starter |
| 2 | `DruidDataSource` + `@Configuration` | Spring + Druid | 方案1+3 |
| 3 | `@Configuration` + `spring-context` | Spring | 方案3 |
| 4 | `mybatis-config.xml` 或 `*Mapper.xml` | MyBatis | 方案4 |
| 5 | `web.xml` 或 `@WebServlet` | Servlet | 方案6 |
| 6 | `DriverManager.getConnection` 无框架 | 纯 JDBC | 方案5 |

**决策树**：
```
IF @SpringBootApplication OR spring-boot-starter-parent:
    → 方案2: db-pwd-spring-boot-starter
ELSE IF DruidDataSource:
    → 方案1: db-pwd-druid (+ db-pwd-spring 如有 @Configuration)
ELSE IF @Configuration + DataSource Bean:
    → 方案3: db-pwd-spring
ELSE IF mybatis-config.xml OR *Mapper.xml:
    → 方案4: db-pwd-mybatis
ELSE IF web.xml OR @WebServlet/@WebListener:
    → 方案6: db-pwd-web
ELSE IF DriverManager.getConnection:
    → 方案5: db-pwd-core (手动调用)
ELSE:
    → 询问用户选择项目类型
```

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

**通用依赖（所有项目）**：
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

**规则4: Spring 配置（方案3）**
```java
@Configuration
public class DatabaseConfig {
    @Bean
    public DataSourcePasswordProcessor dataSourcePasswordProcessor() {
        return new DataSourcePasswordProcessor();
    }
}
```

**规则5: MyBatis 配置（方案4）**
```xml
<dataSource type="com.dbpwd.mybatis.DbPwdDataSourceFactory">
    <property name="password" value="@my_db@"/>
</dataSource>
```

**规则6: Servlet 配置（方案6）**
```xml
<listener>
    <listener-class>com.dbpwd.web.DatabasePasswordInitListener</listener-class>
</listener>
```

**规则7: 更新 .gitignore**
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
1. java -jar db-pwd-demo-init-1.0.0.jar  # 初始化密钥
2. java -jar db-pwd-core-1.0.0.jar add my_db  # 添加密码配置
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

## 详细参考

- `references/integration-patterns.md` - 6 种集成模式详细说明
- `references/code-examples.md` - 代码修改前后对比示例
- `references/troubleshooting.md` - 常见问题处理

## 项目文档

- `AGENTS.md` - 项目上下文
- `docs/integration-patterns.md` - 6 种集成模式
- `docs/integration-code-details.md` - 代码修改细节
- `docs/api-reference.md` - PasswordService API

## 示例

**用户**: 帮我把这个项目集成数据库密码加密

**响应**:
1. 扫描项目结构
2. 识别为 Spring Boot 项目
3. 推荐方案2: Spring Boot Starter
4. 获取用户确认
5. 执行修改：添加依赖、修改配置
6. 验证编译
7. 提供后续操作指南
