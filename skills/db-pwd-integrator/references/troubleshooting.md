# 常见问题处理

本文档记录集成过程中的常见问题及解决方案。

---

## 多模块项目处理

### 检测多模块项目

```
🔍 检测到多模块项目

发现 4 个 pom.xml 文件：
- /pom.xml (parent, packaging=pom)
- /service-a/pom.xml
- /service-b/pom.xml
- /common/pom.xml
```

### 模块选择策略

**自动判断规则**：

| 模块特征 | 判断结果 | 建议 |
|---------|---------|------|
| packaging=pom | 父模块 | 跳过 |
| 有 spring-boot-starter-jdbc | 有数据库 | 需要集成 |
| 有 druid 依赖 | 有数据库 | 需要集成 |
| 有 mybatis 依赖 | 有数据库 | 需要集成 |
| 无数据库依赖 | 公共模块 | 跳过 |
| 有 @SpringBootApplication | 可执行模块 | 需要集成 |

### 交互示例

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 🔍 多模块项目检测
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

项目结构：
my-project/
├── pom.xml                    # 父 pom (packaging=pom)
├── common/                    # 公共模块
│   └── pom.xml
├── service-user/              # 用户服务 (有数据库)
│   └── pom.xml
├── service-order/             # 订单服务 (有数据库)
│   └── pom.xml
└── web-admin/                 # 管理后台 (有数据库)
    └── pom.xml

━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 模块分析结果
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

┌────┬─────────────────┬──────────────┬─────────────┬─────────┐
│ #  │ 模块             │ 数据库依赖    │ 项目类型    │ 建议    │
├────┼─────────────────┼──────────────┼─────────────┼─────────┤
│ 1  │ common          │ ❌ 无        │ -          │ 跳过    │
│ 2  │ service-user    │ ✅ Druid     │ Spring Boot│ 集成    │
│ 3  │ service-order   │ ✅ Druid     │ Spring Boot│ 集成    │
│ 4  │ web-admin       │ ✅ MyBatis   │ Spring MVC │ 集成    │
└────┴─────────────────┴──────────────┴─────────────┴─────────┘

推荐集成模块：2, 3, 4

请选择要集成的模块（输入编号，多个用逗号分隔，回车全选）：
```

### 集成策略

#### 1. 依赖添加位置

**错误做法**：添加到父 pom
```xml
<!-- ❌ 错误：不要在父 pom 添加 -->
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.dbpwd</groupId>
            <artifactId>db-pwd-core</artifactId>
            <version>1.0.0</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```

**正确做法**：添加到具体子模块
```xml
<!-- ✅ 正确：在 service-user/pom.xml 添加 -->
<!-- Spring Boot 项目仅需 starter，会自动传递 db-pwd-core -->
<dependencies>
    <dependency>
        <groupId>com.dbpwd</groupId>
        <artifactId>db-pwd-spring-boot-starter</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

#### 2. 不同模块可能使用不同方案

```
多模块集成计划：

┌─────────────────┬─────────────────┬───────────────────────────┐
│ 模块             │ 集成方案        │ 添加依赖                   │
├─────────────────┼─────────────────┼───────────────────────────┤
│ service-user    │ 方案2: Starter  │ db-pwd-spring-boot-starter│
│ service-order   │ 方案2: Starter  │ db-pwd-spring-boot-starter│
│ web-admin       │ 方案4: MyBatis  │ db-pwd-core               │
└─────────────────┴─────────────────┴───────────────────────────┘

注意：所有模块共享同一个密钥文件 ~/.dbpwd/
```

#### 3. 密钥文件共享

多模块项目使用**统一的密钥文件**：

```
~/.dbpwd/
├── sm4.key              # 所有模块共享
└── db.pwd               # 包含所有数据库配置
    ├── @service_user_db@
    ├── @service_order_db@
    └── @web_admin_db@
```

### 批量集成命令

```
是否批量集成所有推荐模块？[Y/n/逐个确认]

选择 Y → 自动集成所有推荐模块
选择 N → 取消
选择 逐个确认 → 每个模块单独确认
```

### 常见问题

#### 问题：父 pom 中已经有 db-pwd 依赖管理

**症状**：
```
ℹ️ 父 pom 中已有 db-pwd 依赖管理
```

**处理方式**：
- 子模块只需添加依赖，无需指定版本
- 版本由父 pom 统一管理

```xml
<!-- 子模块只需这样写 -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <!-- 无需 version，继承父 pom -->
</dependency>
```

#### 问题：模块间有依赖关系

**场景**：service-user 依赖 common 模块

**处理**：
- 在 common 模块**不需要**添加 db-pwd 依赖
- 只在有数据库操作的模块添加依赖
- common 模块如果使用 PasswordService，会通过传递依赖获得

#### 问题：共享配置文件

**场景**：多个模块共享 application.yml

**处理**：
1. 如果配置在父模块 resources：在父模块修改
2. 如果各模块独立配置：分别修改
3. 使用 Spring Cloud Config：在配置中心修改

---

## 其他连接池支持

### HikariCP（Spring Boot 默认）

**检测信号**：
```
grep("HikariDataSource|HikariConfig|hikari", include="*.java")
grep("hikari", include="*.yml")
```

**处理方式**：与 Spring Boot Starter 相同，HikariCP 会自动被装饰。

```yaml
# application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 10
    password: "@my_db@"  # 占位符格式
```

### DBCP2 / C3P0 / Tomcat JDBC

这些连接池的集成方式与 Druid 类似：

```java
// DBCP2
import org.apache.commons.dbcp2.BasicDataSource;
import com.dbpwd.core.PasswordService;

BasicDataSource ds = new BasicDataSource();
ds.setUrl("jdbc:mysql://localhost:3306/mydb");
ds.setUsername("root");
ds.setPassword(PasswordService.getDatabasePassword("@my_db@"));

// C3P0
import com.mchange.v2.c3p0.ComboPooledDataSource;
ComboPooledDataSource ds = new ComboPooledDataSource();
ds.setJdbcUrl("jdbc:mysql://localhost:3306/mydb");
ds.setUser("root");
ds.setPassword(PasswordService.getDatabasePassword("@my_db@"));

// Tomcat JDBC
import org.apache.tomcat.jdbc.pool.DataSource;
DataSource ds = new DataSource();
ds.setUrl("jdbc:mysql://localhost:3306/mydb");
ds.setUsername("root");
ds.setPassword(PasswordService.getDatabasePassword("@my_db@"));
```

---

## JNDI DataSource 处理

### 场景说明

当应用服务器（Tomcat, WebLogic, WebSphere 等）通过 JNDI 提供 DataSource 时：

```java
// BEFORE
@Resource(lookup = "java:comp/env/jdbc/mydb")
private DataSource dataSource;

// 或
Context ctx = new InitialContext();
DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/mydb");
```

### 处理方式

**方案 A**：在应用服务器配置中使用占位符
```xml
<!-- Tomcat context.xml -->
<Resource name="jdbc/mydb" 
          auth="Container"
          type="javax.sql.DataSource"
          password="@my_db@"/>
```

**方案 B**：使用 DataSourceDecorator 包装
```java
import com.dbpwd.framework.DataSourceDecorator;

@Resource(lookup = "java:comp/env/jdbc/mydb")
private DataSource rawDataSource;

// 装饰后使用
DataSource dataSource = DataSourceDecorator.decorate(rawDataSource);
```

---

## 多环境配置处理

### 场景说明

项目通常有多套环境配置：
- `application-dev.yml` - 开发环境
- `application-test.yml` - 测试环境
- `application-prod.yml` - 生产环境

### 处理方式

**策略**：所有环境使用相同的占位符，但配置不同的密码文件。

```yaml
# application-dev.yml
spring:
  datasource:
    password: "@my_db@"  # 开发环境占位符

# application-prod.yml
spring:
  datasource:
    password: "@my_db@"  # 生产环境占位符（同名）
```

**密码文件配置**：
```json
// 开发环境 ~/.dbpwd/db.pwd
{
  "databases": [
    {"name": "my_db", "encrypted_password": "ENC(SM4-GCM:dev_encrypted...)"},
  ]
}

// 生产环境 ~/.dbpwd/db.pwd（部署时替换）
{
  "databases": [
    {"name": "my_db", "encrypted_password": "ENC(SM4-GCM:prod_encrypted...)"},
  ]
}
```

### 交互模板

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
 🌍 多环境配置检测
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━

检测到多环境配置文件：
- application.yml
- application-dev.yml
- application-test.yml
- application-prod.yml

建议：
1. 所有环境配置使用相同占位符（如 @my_db@）
2. 不同环境部署不同的 db.pwd 文件
3. 密钥文件（sm4.key）建议环境隔离

是否批量修改所有环境配置文件？[Y/n/仅修改默认]
```

---

## Gradle 项目处理

### 检测信号
```
glob("**/build.gradle")
glob("**/build.gradle.kts")
```

### 依赖添加

```groovy
// build.gradle
// Spring Boot 项目仅需 starter，会自动传递 db-pwd-core
dependencies {
    implementation 'com.dbpwd:db-pwd-spring-boot-starter:1.0.0'
}
```

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.dbpwd:db-pwd-spring-boot-starter:1.0.0")
}
```

### 版本管理

```groovy
// 使用 ext 管理版本
ext {
    dbpwdVersion = '1.0.0'
}

dependencies {
    implementation "com.dbpwd:db-pwd-core:${dbpwdVersion}"
}
```

---

## 配置中心集成

### Nacos

```yaml
# application.yml
spring:
  cloud:
    nacos:
      config:
        server-addr: ${NACOS_SERVER:localhost:8848}
        namespace: ${NACOS_NAMESPACE:dev}
  datasource:
    password: "@my_db@"  # 占位符，在 Nacos 中配置实际密码
```

**处理方式**：在 Nacos 配置中使用占位符，db-pwd 在应用启动时解密。

### Apollo

```yaml
# application.yml
app:
  id: my-app
apollo:
  meta: ${APOLLO_META:http://localhost:8080}
spring:
  datasource:
    password: "@my_db@"
```

### Spring Cloud Config

```yaml
# bootstrap.yml
spring:
  cloud:
    config:
      uri: ${CONFIG_SERVER:http://localhost:8888}
```

---

## Docker/Kubernetes 部署

### Docker

**Dockerfile 示例**：
```dockerfile
FROM openjdk:11-jre
COPY target/app.jar /app.jar
# 密钥文件通过 volume 挂载
VOLUME ["/root/.dbpwd"]
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

**docker-compose.yml**：
```yaml
services:
  app:
    image: my-app
    volumes:
      - ~/.dbpwd:/root/.dbpwd:ro  # 只读挂载密钥文件
```

### Kubernetes

**Secret 配置**：
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: dbpwd-secrets
type: Opaque
data:
  sm4.key: <base64-encoded-key>
  db.pwd: <base64-encoded-password-file>
```

**Deployment 挂载**：
```yaml
apiVersion: apps/v1
kind: Deployment
spec:
  template:
    spec:
      volumes:
      - name: dbpwd
        secret:
          secretName: dbpwd-secrets
      containers:
      - name: app
        volumeMounts:
        - name: dbpwd
          mountPath: /root/.dbpwd
          readOnly: true
```

---

## 密码轮换

### 场景说明

安全合规要求定期更换数据库密码和加密密钥。

### 轮换步骤

```
📋 密码轮换流程

1. 生成新密钥
   java -jar db-pwd-core.jar generate-key --output ~/.dbpwd/sm4.key.new

2. 使用新密钥重新加密所有密码
   java -jar db-pwd-core.jar rotate-key \
     --old-key ~/.dbpwd/sm4.key \
     --new-key ~/.dbpwd/sm4.key.new \
     --output ~/.dbpwd/db.pwd.new

3. 备份旧文件
   cp ~/.dbpwd/sm4.key ~/.dbpwd/sm4.key.backup
   cp ~/.dbpwd/db.pwd ~/.dbpwd/db.pwd.backup

4. 替换文件（建议分批发布）
   mv ~/.dbpwd/sm4.key.new ~/.dbpwd/sm4.key
   mv ~/.dbpwd/db.pwd.new ~/.dbpwd/db.pwd

5. 重启应用验证
```

### 零停机轮换策略

对于高可用系统，建议：
1. 新旧密钥共存期（支持双密钥解密）
2. 分批发布应用
3. 确认全部成功后移除旧密钥

---

## CI/CD 集成提示

### GitHub Actions

```yaml
# .github/workflows/build.yml
jobs:
  build:
    steps:
      - uses: actions/checkout@v3
      
      # 从 Secrets 读取密钥
      - name: Setup db-pwd
        run: |
          mkdir -p ~/.dbpwd
          echo "${{ secrets.DBPWD_SM4_KEY }}" > ~/.dbpwd/sm4.key
          echo "${{ secrets.DBPWD_DB_FILE }}" > ~/.dbpwd/db.pwd
          chmod 600 ~/.dbpwd/*
      
      - name: Build
        run: mvn clean verify
```

### Jenkins

```groovy
pipeline {
    agent any
    stages {
        stage('Setup') {
            steps {
                sh '''
                    mkdir -p ~/.dbpwd
                    echo "${DBPWD_SM4_KEY}" > ~/.dbpwd/sm4.key
                    echo "${DBPWD_DB_FILE}" > ~/.dbpwd/db.pwd
                    chmod 600 ~/.dbpwd/*
                '''
            }
        }
        stage('Build') {
            steps {
                sh 'mvn clean verify'
            }
        }
    }
    environment {
        DBPWD_SM4_KEY = credentials('dbpwd-sm4-key')
        DBPWD_DB_FILE = credentials('dbpwd-db-file')
    }
}
```

---

## 扫描阶段问题

### 问题: 找不到 pom.xml

**症状**:
```
❌ 未检测到构建文件
```

**可能原因**:
1. Gradle 项目（使用 build.gradle）
2. 项目根目录不正确
3. 非标准项目结构

**解决方案**:
```bash
# 检查是否为 Gradle 项目
ls build.gradle* 2>/dev/null && echo "Gradle 项目"

# 指定项目根目录
cd /path/to/project
```

### 问题: 无法读取配置文件

**症状**:
```
❌ 无法读取 application.yml
```

**可能原因**:
1. 文件编码问题
2. 文件权限问题
3. 文件不存在

**解决方案**:
```bash
# 检查文件编码
file -i src/main/resources/application.yml

# 检查文件权限
ls -la src/main/resources/

# 检查文件是否存在
find . -name "application*" -type f
```

### 问题: 检测到多个框架

**症状**:
```
✅ Spring Boot
✅ Druid
✅ MyBatis
推荐方案: ？
```

**解决方案**:
按优先级选择：
1. 有 `@SpringBootApplication` → 使用 Spring Boot Starter
2. Spring Boot 会自动处理 Druid 和 MyBatis 的数据源

---

## 识别阶段问题

### 问题: 版本不兼容

**症状**:
```
⚠️ Spring Boot 3.x 检测到
当前支持: Spring Boot 2.x
```

**原因**: Spring Boot 3.x 使用 Jakarta EE（`jakarta.*`）

**解决方案**:

选项 1: 升级 db-pwd（如果支持）
```xml
<!-- 使用支持 Spring Boot 3.x 的版本 -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-spring-boot-starter</artifactId>
    <version>2.0.0</version>  <!-- 假设有新版本 -->
</dependency>
```

选项 2: 手动适配
```java
// 将 javax.* 替换为 jakarta.*
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
```

选项 3: 降级项目
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>2.7.18</version>
</parent>
```

### 问题: 已集成旧版本 db-pwd

**症状**:
```
ℹ️ 项目已集成 db-pwd
当前版本: 0.9.0
```

**解决方案**:

```bash
# 查看当前版本
grep "db-pwd" pom.xml

# 升级到最新版本
# 在 pom.xml 中修改版本号
```

---

## 执行阶段问题

### 问题: 依赖下载失败

**症状**:
```
❌ Could not resolve com.dbpwd:db-pwd-core:1.0.0
```

**可能原因**:
1. db-pwd 未发布到中央仓库
2. 网络问题
3. 仓库配置问题

**解决方案**:

选项 1: 本地安装
```bash
# 下载或克隆 db-pwd 项目
git clone https://github.com/faunt1/dbpwd.git
cd dbpwd

# 安装到本地仓库
mvn clean install -DskipTests
```

选项 2: 配置私有仓库
```xml
<repositories>
    <repository>
        <id>dbpwd-releases</id>
        <url>https://your-nexus.example.com/releases</url>
    </repository>
</repositories>
```

### 问题: 文件内容与预期不符

**症状**:
```
⚠️ 文件内容与预期不符
预期: password = "sa"
实际: password = "${DB_PASSWORD}"
```

**解决方案**:

这是环境变量密码场景，处理方式：
```java
// 保留环境变量，值改为占位符
@Value("${DB_PASSWORD}")
private String placeholder;

String password = PasswordService.getDatabasePassword(placeholder);
```

### 问题: 编译错误

**症状**:
```
❌ 编译失败
cannot find symbol: class PasswordService
```

**解决方案**:

1. 检查依赖是否正确添加
```bash
mvn dependency:tree | grep db-pwd
```

2. 强制更新依赖
```bash
mvn clean install -U
```

3. 检查导入语句
```java
// 确保正确导入
import com.dbpwd.core.PasswordService;
```

### 问题: 找不到 DataSource 配置

**症状**:
```
❌ 未检测到 DataSource 配置
```

**可能原因**:
1. 使用了自动配置（Spring Boot）
2. 配置在其他模块
3. 使用了 JNDI

**解决方案**:

Spring Boot 自动配置无需显式 DataSource Bean，只需修改配置文件：
```yaml
spring:
  datasource:
    password: "@my_db@"
```

---

## 运行时问题

### 问题: 密码解密失败

**症状**:
```
❌ Failed to decrypt password: @my_db@
```

**可能原因**:
1. 密钥文件不存在
2. 占位符未配置
3. 密钥格式错误

**诊断步骤**:
```bash
# 检查密钥文件
ls -la ~/.dbpwd/sm4.key

# 检查密钥格式（应为 32 字符 hex）
cat ~/.dbpwd/sm4.key | wc -c

# 检查密码文件
cat ~/.dbpwd/db.pwd
```

**解决方案**:
```bash
# 初始化密钥和密码文件
java -jar db-pwd-demo-init-1.0.0.jar

# 添加密码配置
java -jar db-pwd-core-1.0.0.jar add my_db

# 或手动编辑 db.pwd
```

### 问题: 找不到占位符

**症状**:
```
❌ Placeholder not found: @my_db@
```

**解决方案**:
```bash
# 检查 db.pwd 内容
cat ~/.dbpwd/db.pwd

# 确保有对应条目
# db.pwd 格式:
{
  "databases": [
    {
      "name": "my_db",
      "encrypted_password": "ENC(SM4-GCM:...)"
    }
  ]
}
```

### 问题: DataSource 初始化失败

**症状**:
```
❌ Failed to initialize DataSource
```

**可能原因**:
1. BeanPostProcessor 未注册
2. 循环依赖
3. 配置错误

**解决方案**:

检查 Spring 配置：
```java
// 确保注册了 BeanPostProcessor
@Bean
public DataSourcePasswordProcessor dataSourcePasswordProcessor() {
    return new DataSourcePasswordProcessor();
}
```

检查组件扫描：
```java
@SpringBootApplication(scanBasePackages = {
    "com.example",      // 你的应用包
    "com.dbpwd.spring"  // db-pwd 包
})
public class Application { ... }
```

---

## 安全相关问题

### 问题: 密钥文件权限不正确

**症状**:
```
⚠️ 密钥文件权限过于开放
```

**解决方案 (Linux/Mac)**:
```bash
# 设置目录权限
chmod 700 ~/.dbpwd

# 设置文件权限
chmod 600 ~/.dbpwd/sm4.key
chmod 600 ~/.dbpwd/db.pwd
```

### 问题: 密钥文件被提交到版本控制

**症状**:
```
⚠️ 检测到密钥文件在 git 追踪中
```

**解决方案**:
```bash
# 添加到 .gitignore
echo ".dbpwd/" >> .gitignore
echo "sm4.key" >> .gitignore
echo "db.pwd" >> .gitignore

# 从 git 中移除（保留本地文件）
git rm --cached ~/.dbpwd/sm4.key
git rm --cached ~/.dbpwd/db.pwd

# 提交 .gitignore
git add .gitignore
git commit -m "chore: 添加密钥文件到 .gitignore"
```

---

## 回滚操作

### 手动回滚

```bash
# 从备份恢复
cp .dbpwd-backup/20260217_134500/pom.xml.bak pom.xml
cp .dbpwd-backup/20260217_134500/application.yml.bak src/main/resources/application.yml

# 或使用 git
git checkout HEAD -- pom.xml
git checkout HEAD -- src/main/resources/application.yml
```

### 完全清理

```bash
# 删除 db-pwd 依赖
# 编辑 pom.xml，移除 com.dbpwd 依赖

# 恢复原始密码
# 编辑配置文件，将占位符改回明文密码

# 删除密钥文件（可选）
rm -rf ~/.dbpwd

# 删除备份（可选）
rm -rf .dbpwd-backup
```

---

## 诊断命令

```bash
# 检查 Java 版本
java -version

# 检查 Maven 版本
mvn -version

# 检查项目依赖
mvn dependency:tree | grep db-pwd

# 检查密钥文件
cat ~/.dbpwd/sm4.key

# 检查密码文件
cat ~/.dbpwd/db.pwd

# 编译测试
mvn clean compile -DskipTests

# 运行测试
mvn test
```

---

## 联系支持

如果以上方案无法解决问题，请：

1. 查看项目文档：
   - [AGENTS.md](../../AGENTS.md)
   - [FAQ.md](../../docs/FAQ.md)
   - [api-reference.md](../../docs/api-reference.md)

2. 运行诊断并收集信息：
   ```bash
   # 收集诊断信息
   java -version > diagnostics.txt
   mvn -version >> diagnostics.txt
   cat pom.xml >> diagnostics.txt
   ls -la ~/.dbpwd/ >> diagnostics.txt
   ```

3. 提交 Issue 到项目仓库
