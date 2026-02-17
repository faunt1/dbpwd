# 常见问题处理

本文档记录集成过程中的常见问题及解决方案。

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
   - `AGENTS.md`
   - `docs/FAQ.md`
   - `docs/api-reference.md`

2. 运行诊断并收集信息：
   ```bash
   # 收集诊断信息
   java -version > diagnostics.txt
   mvn -version >> diagnostics.txt
   cat pom.xml >> diagnostics.txt
   ls -la ~/.dbpwd/ >> diagnostics.txt
   ```

3. 提交 Issue 到项目仓库
