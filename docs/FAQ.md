# 常见问题FAQ (Frequently Asked Questions)

> 文档版本: v1.0
> 最后更新: 2026-02-10

## 目录

1. [安装与配置](#安装与配置)
2. [加密解密](#加密解密)
3. [数据库连接](#数据库连接)
4. [框架集成](#框架集成)
5. [配置中心](#配置中心)
6. [安全相关](#安全相关)
7. [故障排查](#故障排查)
8. [性能与优化](#性能与优化)

---

## 安装与配置

### Q1: 系统的最低要求是什么？

**A**:
- **JDK版本**: JDK 8或更高版本（推荐JDK 11+）
- **Maven版本**: 3.6或更高版本（用于构建）
- **操作系统**: Linux/macOS/Windows
- **内存建议**: 最小256MB，推荐512MB+

### Q2: 如何安装dbpwd？

**A**:
```bash
# 1. 克隆项目
git clone https://github.com/faunt1/dbpwd.git
cd dbpwd

# 2. 编译项目
mvn clean package

# 3. 生成密钥
java -jar db-pwd-core/target/dbpwd-1.0.0-jar-with-dependencies.jar generate-key
```

### Q3: 配置目录必须在 `~/.dbpwd` 下吗？

**A**: 不是。`~/.dbpwd` 是默认路径，你可以自定义路径：

**命令行工具**:
```bash
java -jar dbpwd.jar add \
  --password-file /custom/path/db.pwd \
  --key-file /custom/path/sm4.key \
  ...
```

**Spring Boot应用**:
```yaml
dbpwd:
  password-file: /custom/path/db.pwd
  key-file: /custom/path/sm4.key
```

### Q4: Windows上如何设置文件权限为600？

**A**: Windows不支持Unix风格的文件权限设置。你可以忽略权限警告，但建议：

- 确保配置目录只有当前用户可访问
- 使用NTFS权限设置目录访问权限
- 避免将密钥文件存放在共享目录

### Q5: 如何验证安装是否成功？

**A**:
```bash
# 查看帮助
java -jar dbpwd.jar help

# 测试加密
java -jar dbpwd.jar encrypt --password "test"

# 查看数据库列表
java -jar dbpwd.jar list
```

---

## 加密解密

### Q6: 为什么选择SM4而不是AES？

**A**:
1. **符合国家标准**: SM4是中国国家密码标准（GM/T 0002-2012）
2. **安全强度**: 128位密钥长度，安全强度与AES-128相当
3. **合规要求**: 某些行业（金融、政务）要求使用国密算法

如果你更偏好AES，可以扩展dbpwd以支持AES。

### Q7: SM4 ECB模式和GCM模式有什么区别？该选哪个？

**A**:

| 特性 | ECB模式 | GCM模式 |
|------|---------|---------|
| 安全性 | 较低，不支持认证 | 较高，支持认证加密 |
| 性能 | 更快 | 稍慢 |
| IV要求 | 不需要 | 需要 |
| 推荐使用 | 保持兼容性 | 默认推荐 |

**建议**: 默认使用GCM模式，除非有兼容性需求。

### Q8: 如何生成安全的密钥？

**A**: 使用dbpwd内置的密钥生成器：

```bash
java -jar dbpwd.jar generate-key
```

不要：
- 使用简单密码作为密钥
- 使用可预测的字符串
- 在代码中硬编码密钥

### Q9: 密钥文件丢了怎么办？

**A**: 密钥文件丢失后，加密的密码无法解密。应对措施：

1. **预防**:
   - 定期备份密钥文件到安全位置
   - 使用配置中心存储密钥
   - 文档化密钥位置

2. **恢复**:
   - 如果有备份：使用备份恢复密钥
   - 如果无备份：重新生成密钥并重新加密所有密码

3. **流程**:
   ```bash
   # 生成新密钥
   java -jar dbpwd.jar generate-key

   # 重新添加数据库配置（会自动重新加密）
   java -jar dbpwd.jar add --id db001 --type mysql --password "new_password" ...
   ```

### Q10: 可以更换已经使用的密钥吗？

**A**: 可以，但需要重新加密所有密码：

```bash
# 1. 生成新密钥（备份旧密钥）
cp ~/.dbpwd/sm4.key ~/.dbpwd/sm4.key.backup
java -jar dbpwd.jar generate-key

# 2. 重新加密密码文件中的所有密码
# 需要编写脚本或手动更新每个数据库的密码

# 3. 更新配置中心（如果使用）

# 4. 确认正常后删除旧密钥备份
```

### Q11: ENC(SM4:...) 格式的说明

**A**: ENC格式：`ENC(算法:密文)`

- `ENC`: 固定前缀
- `算法`: 加密算法（SM4、SM4-GCM等）
- `密文`: Base64编码的加密数据

示例：
```text
ENC(SM4:r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=)
ENC(SM4-GCM:abc123xyz456789def...")
```

### Q12: 如何验证密码格式是否正确？

**A**: 使用解密命令测试：

```bash
# 尝试解密
java -jar dbpwd.jar decrypt --password "ENC(SM4:r7X9k3mP...)"

# 如果成功会显示明文，失败会显示错误信息
```

---

## 数据库连接

### Q13: 支持哪些数据库类型？

**A**: 支持以下主流数据库：

| 数据库 | type值 |
|--------|--------|
| MySQL | mysql |
| PostgreSQL | postgresql |
| Oracle | oracle |
| SQL Server | sqlserver |
| H2 | h2 |
| DB2 | db2 |
| Sybase | sybase |
| SQLite | sqlite |

其他数据库也可能工作，但需要对应JDBC驱动。

### Q14: 如何测试数据库连接？

**A**:
```bash
# 测试指定数据库
java -jar dbpwd.jar test --database-id db001

# 测试所有数据库
java -jar dbpwd.jar test --all
```

### Q15: 连接超时怎么设置？

**A**: 在数据库连接URL中设置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb?connectTimeout=10000&socketTimeout=30000
```

### Q16: 如何使用自定义JDBC驱动？

**A**:
1. 将JDBC驱动JAR放到classpath
2. 在数据库配置中指定驱动类名

**Spring Boot**:
```yaml
spring:
  datasource:
    driver-class-name: com.custom.jdbc.Driver
```

**Java配置**:
```java
dataSource.setDriverClassName("com.custom.jdbc.Driver");
```

### Q17: 连接到Oracle数据库时出现错误怎么办？

**A**: Oracle连接常见问题：

1. **检查驱动**: 确保Oracle JDBC驱动在classpath
2. **检查URL格式**: Oracle URL格式为：
   ```
   jdbc:oracle:thin:@host:port:sid
   jdbc:oracle:thin:@//host:port/service_name
   ```
3. **检查TNS设置**: 如果使用TNS，需要配置tnsnames.ora

### Q18: 如何处理连接池连接泄露？

**A**:
1. 配置连接池的最大连接数
2. 使用合适的连接超时设置
3. 监控活跃连接数
4. 定期检查代码中是否有未关闭的连接

**Druid示例**:
```yaml
spring:
  datasource:
    druid:
      max-active: 20
      max-wait: 10000
      remove-abandoned: true
      remove-abandoned-timeout: 180
```

---

## 框架集成

### Q19: 如何在Spring Boot中集成dbpwd？

**A**:
1. 添加依赖：
```xml
<dependency>
  <groupId>com.dbpwd</groupId>
  <artifactId>db-pwd-core</artifactId>
  <version>1.0.0</version>
</dependency>
```

2. 配置application.yml：
```yaml
dbpwd:
  enabled: true
  password-file: ~/.dbpwd/db.pwd
  key-file: ~/.dbpwd/sm4.key

spring:
  datasource:
    password: myapp-db  # 使用数据库ID
```

3. Spring Boot会自动配置dbpwd

### Q20: Spring Boot中密码字段应该用密文还是数据库ID？

**A**: 使用**数据库ID**，不要用密文：

**正确**:
```yaml
spring:
  datasource:
    password: myapp-db  # 数据库ID
```

**错误**:
```yaml
spring:
  datasource:
    password: ENC(SM4:...)  # 不要直接使用密文
```

dbpwd装饰器会自动根据数据库ID查找并解密密码。

### Q21: 支持哪些连接池？

**A**: 内置支持以下连接池：

| 连接池 | 支持状态 |
|--------|---------|
| Druid | ✅ 完全支持 |
| HikariCP | ✅ 完全支持 |
| DBCP2 | ✅ 完全支持 |
| 其他 | ⚠️ 部分支持（可能需要反射） |

### Q22: 如何在普通Spring应用中集成dbpwd？

**A**:
```java
@Configuration
public class DataSourceConfig {

  @Bean
  public DataSource dataSource() throws Exception {
    DruidDataSource dataSource = new DruidDataSource();
    dataSource.setUrl("jdbc:mysql://localhost:3306/mydb");
    dataSource.setUsername("user");
    dataSource.setPassword("myapp-db");  # 数据库ID
    dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

    byte[] key = Sm4KeyLoader.loadKeyFromFile("~/.dbpwd/sm4.key");
    DataSourceDecorator decorator = new DataSourceDecorator(dataSource, key);

    return decorator.decorate("myapp-db");
  }
}
```

### Q23: 如何在MyBatis中集成dbpwd？

**A**: MyBatis使用DataSource，与Spring Boot集成方法相同：

```yaml
# application.yml
dbpwd:
  enabled: true
  password-file: ~/.dbpwd/db.pwd
  key-file: ~/.dbpwd/sm4.key

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mydb
    username: user
    password: myapp-db  # 数据库ID

mybatis:
  mapper-locations: classpath:mapper/*.xml
```

### Q24: Spring Boot启动时报错"DataSourceDecorator failed to decrypt password"？

**A**: 可能原因：

1. **密钥文件不存在或路径错误**: 检查配置的密钥文件路径
2. **密钥不匹配**: 确保密钥与加密密码时使用的密钥一致
3. **数据库ID不存在**: 确认数据库ID在密码文件中存在
4. **密码格式错误**: 确认密码字段是数据库ID而不是密文

解决步骤：
```bash
# 1. 检查密钥文件
ls -la ~/.dbpwd/sm4.key

# 2. 测试解密
java -jar dbpwd.jar decrypt --password "ENC(SM4:...)"

# 3. 检查密码文件
java -jar dbpwd.jar list

# 4. 查看详细日志
logging:
  level:
    com.dbpwd: DEBUG
```

### Q25: 如何配置多数据源？

**A**:
```java
@Configuration
public class MultiDataSourceConfig {

  @Bean
  @Primary
  public DataSource primaryDataSource() throws Exception {
    DruidDataSource ds = new DruidDataSource();
    ds.setPassword("primary-db");
    // ... 其他配置

    byte[] key = Sm4KeyLoader.loadKeyFromFile("~/.dbpwd/sm4.key");
    return new DataSourceDecorator(ds, key).decorate("primary-db");
  }

  @Bean
  public DataSource secondaryDataSource() throws Exception {
    DruidDataSource ds = new DruidDataSource();
    ds.setPassword("secondary-db");
    // ... 其他配置

    byte[] key = Sm4KeyLoader.loadKeyFromFile("~/.dbpwd/sm4.key");
    return new DataSourceDecorator(ds, key).decorate("secondary-db");
  }
}
```

---

## 配置中心

### Q26: 如何从配置中心下载配置？

**A**:
```bash
# HTTP API
java -jar dbpwd.jar download \
  --config-center-url "http://config.example.com/api/dbpwd/g1/a1/app1/prod"

# HTTPS API（带认证）
java -jar dbpwd.jar download \
  --config-center-url "https://config.example.com/api/dbpwd/g1/a1/app1/prod" \
  --api-key "your_api_key"

# 本地文件系统
java -jar dbpwd.jar download \
  --config-center-url "/path/to/config/g1/a1/app1/prod"
```

### Q27: 配置中心的URL格式是什么？

**A**: 格式：`{protocol}://{config-center}/{api-path}/{group}/{app}/{module}/{env}`

示例：
```
# HTTP API
http://config.example.com/api/dbpwd/groupA/finance-system/payment-app/prod

# HTTPS API
https://config.example.com/api/dbpwd/groupA/finance-system/payment-app/prod

# 本地文件
/config/groupA/finance-system/payment-app/prod
```

### Q28: 如何自动同步配置中心？

**A**: 使用cron或systemd定时器：

**Cron示例**（每小时同步）:
```bash
crontab -e
# 添加
0 * * * * /opt/dbpwd/sync-script.sh
```

**Systemd Timer示例**:
```ini
[Unit]
Description=DBPWD Config Sync

[Timer]
OnCalendar=hourly
Persistent=true

[Install]
WantedBy=timers.target
```

### Q29: 配置中心目录结构有什么要求？

**A**: 推荐结构：
```
{配置中心根目录}/
├── {物理组}/
│   └── {应用名称}/
│       └── {应用模块}/
│           ├── dev/
│           │   ├── db.pwd
│           │   └── sm4.key
│           ├── test/
│           │   ├── db.pwd
│           │   └── sm4.key
│           └── prod/
│               ├── db.pwd
│               └── sm4.key
```

### Q30: 下载失败怎么办？

**A**: 常见原因和解决：

1. **网络问题**: 检查网络连接
2. **URL错误**: 检查配置中心URL是否正确
3. **认证失败**: 检查API Key是否正确
4. **权限不足**: 检查配置中心访问权限
5. **文件不存在**: 检查配置中心是否有对应文件

调试：
```bash
# 使用详细模式
java -jar dbpwd.jar download \
  --config-center-url "..." \
  --api-key "..." \
  --verbose

# 仅对比，不下载
java -jar dbpwd.jar download \
  --config-center-url "..." \
  --api-key "..." \
  --compare-only
```

### Q31: 如何比较本地和远程配置？

**A**:
```bash
java -jar dbpwd.jar download \
  --config-center-url "..." \
  --api-key "..." \
  --compare-only
```

输出会显示差异：
- 本地有、远程没有的文件
- 远程有、本地没有的文件
- 内容不同的文件

---

## 安全相关

### Q32: 密钥文件权限总是警告，怎么办？

**A**:

**Linux/Mac**:
```bash
chmod 600 ~/.dbpwd/sm4.key
chmod 600 ~/.dbpwd/db.pwd
chmod 700 ~/.dbpwd
```

**Windows**:
- Windows不支持Unix权限设置
- 可以忽略警告，但要确保：
  - 配置目录只有当前用户可访问
  - 使用NTFS权限控制
  - 避免存放在共享目录

### Q33: 密钥文件应该多人共享吗？

**A**: 不建议。不同环境或不同应用应该使用不同的密钥：

**推荐做法**:
- 每个环境（dev/test/prod）使用独立密钥
- 每个应用使用独立密钥（特别是敏感应用）
- 密钥文件通过配置中心独立分发

**示例**:
```
~/.dbpwd/dev/sm4.key
~/.dbpwd/test/sm4.key
~/.dbpwd/prod/sm4.key
```

### Q34: 密码文件泄露有什么风险？如何防范？

**A**:

**风险**:
- 加密的密码可能被暴力破解
- 密钥文件和密码文件一起泄露会导致明文泄露

**防范措施**:
1. **密钥分离**: 密钥和密码文件存储在不同位置
2. **严格权限**: 文件权限设置为600
3. **定期更换**: 定期轮换密钥和密码
4. **监控审计**: 记录所有密码解密操作
5. **使用GCM**: 使用GCM模式提供认证加密

### Q35: 如何启用审计日志？

**A**: 配置日志级别：

**application.yml**:
```yaml
logging:
  level:
    com.dbpwd: DEBUG
```

dbpwd会记录：
- 密码解密操作
- 数据库连接操作
- 配置文件加载操作
- 配置同步操作

### Q36: 如何处理密钥轮换？

**A**:
1. **生成新密钥**:
```bash
cp ~/.dbpwd/sm4.key ~/.dbpwd/sm4.key.old
java -jar dbpwd.jar generate-key
```

2. **重新加密所有密码**:
```bash
# 需要重新添加所有数据库配置
java -jar dbpwd.jar list --id-only | while read id; do
  # 获取明文密码并使用新密钥加密
  ...
done
```

3. **测试并更新配置中心**

4. **删除旧密钥**（确认新密钥正常后）:
```bash
rm ~/.dbpwd/sm4.key.old
```

### Q37: 可以禁用密码加密功能吗？

**A**: 不推荐，但可以在测试环境使用明文密码：

```yaml
dbpwd:
  enabled: false  # 禁用dbpwd

spring:
  datasource:
    password: plain_password_here  # 使用明文密码
```

**警告**: 生产环境必须启用密码加密！

---

## 故障排查

### Q38: 如何获取详细的错误信息？

**A**:

**命令行工具**:
```bash
java -jar dbpwd.jar --verbose <command>
java -jar dbpwd.jar <command> --verbose
```

**Spring Boot应用**:
```yaml
logging:
  level:
    com.dbpwd: DEBUG
    com.alibaba.druid: DEBUG
```

### Q39: ClassNotFoundException错误怎么办？

**A**:
1. 确认使用正确的JAR文件
```bash
# 使用带依赖的JAR
java -jar db-pwd-core/target/dbpwd-1.0.0-jar-with-dependencies.jar
```

2. 确认依赖已正确添加到应用
```xml
<dependency>
  <groupId>com.dbpwd</groupId>
  <artifactId>db-pwd-core</artifactId>
  <version>1.0.0</version>
</dependency>
```

3. 重新构建项目
```bash
mvn clean package
```

### Q40: 连接数据库失败，如何排查？

**A**:
1. **检查数据库服务**:
```bash
# 检查数据库是否运行
# MySQL: systemctl status mysql
# PostgreSQL: systemctl status postgresql
```

2. **检查网络连通性**:
```bash
ping database-host
telnet database-host port
```

3. **使用dbpwd测试**:
```bash
java -jar dbpwd.jar test --database-id db001 --verbose
```

4. **直接使用JDBC测试**:
```bash
# 使用JDBC驱动直接测试，排除dbpwd因素
```

### Q41: Spring Boot应用启动失败，提示DataSource初始化错误？

**A**:
1. 检查dbpwd配置：
```yaml
dbpwd:
  enabled: true
  password-file: ~/.dbpwd/db.pwd
  key-file: ~/.dbpwd/sm4.key
```

2. 检查数据库配置：
```yaml
spring:
  datasource:
    password: myapp-db  # 必须是数据库ID
    # 不要使用 ENC(SM4:...')
```

3. 检查数据库ID存在：
```bash
java -jar dbpwd.jar list
```

4. 查看详细错误日志：
```yaml
logging:
  level:
    com.dbpwd: DEBUG
```

### Q42: 如何查看dbpwd的版本？

**A**:
```bash
# 查看帮助信息（包含版本）
java -jar dbpwd.jar --version

# 或查看JAR的MANIFEST
jar tf db-pwd-1.0.0.jar | grep MANIFEST
```

### Q43: 内存不足错误（OutOfMemoryError）怎么办？

**A**:
1. 增加JVM内存：
```bash
java -Xmx512m -Xms256m -jar dbpwd.jar
```

2. 检查是否有内存泄露
3. 减少连接池大小
4. 监控内存使用

Spring Boot配置：
```yaml
spring:
  datasource:
    druid:
      max-active: 10  # 减少最大连接数
```

### Q44: 如何重置配置？

**A**:
```bash
# 1. 备份当前配置
cp -r ~/.dbpwd ~/.dbpwd.backup

# 2. 删除配置目录
rm -rf ~/.dbpwd

# 3. 重新初始化
java -jar dbpwd.jar generate-key

# 4. 重新添加数据库配置
# 或从备份恢复
```

---

## 性能与优化

### Q45: dbpwd对性能有影响吗？

**A**: 影响很小：
- 密码解密只在初始化时执行一次
- SM4加密速度很快（每秒可处理数百万次加密）
- 连接池建立后无需重复解密

### Q46: 如何优化数据库连接性能？

**A**:
1. 合理配置连接池大小
2. 使用连接池（推荐Druid或HikariCP）
3. 设置合理的连接超时
4. 使用连接池监控

**Druid优化配置**:
```yaml
spring:
  datasource:
    druid:
      max-active: 20
      min-idle: 5
      max-wait: 60000
      initial-size: 5
```

### Q47: 密码解密很慢怎么办？

**A**: 通常不会很慢。如果遇到：
1. 检查密钥文件是否在本地（不是网络路径）
2. 检查密码文件大小（不应该过大）
3. 使用GCM模式（性能更好）
4. 检查系统资源使用情况

### Q48: 可以缓存解密后的密码吗？

**A**: dbpwd装饰器已经实现了缓存机制。`getConnection()` 第一次调用时会解密密码，后续调用会使用缓存的值。

### Q49: 如何监控dbpwd性能？

**A**:
1. 启用DEBUG日志查看解密时间
2. 使用JMX监控DataSource
3. 使用Druid监控页面
4. 使用APM工具（如Prometheus + Grafana）

### Q50: 高并发场景下有什么注意事项？

**A**:
1. 增加连接池大小
2. 设置合理的等待超时
3. 做好连接池监控
4. 避免N+1查询问题
5. 考虑读写分离

---

## 其他

### Q51: 如何贡献代码？

**A**:
1. Fork项目仓库
2. 创建特性分支
3. 提交PR并描述改动
4. 等待review和合并

### Q52: 如何报告bug？

**A**:
- 在GitHub上提交Issue
- 提供详细的bug描述
- 附上错误日志和环境信息
- 如果可能，提供重现步骤

### Q53: 是否支持商业化使用？

**A**: 请查看项目许可证（LICENSE文件）。目前项目使用开源许可证，允许商业使用。

### Q54: 是否有官方支持？

**A**: 目前由社区维护支持。如需企业级支持，请联系项目维护者。

### Q55: 未来计划支持哪些功能？

**A**:
- 支持更多加密算法（AES等）
- Web管理界面
- 更完善的监控和审计
- 密钥托管服务
- 密码策略管理

---

## 附录

### A. 有用的命令速查

```bash
# 生成密钥
java -jar dbpwd.jar generate-key

# 列出数据库
java -jar dbpwd.jar list

# 添加数据库
java -jar dbpwd.jar add --id db001 --type mysql --host localhost ...

# 测试连接
java -jar dbpwd.jar test --database-id db001

# 加密密码
java -jar dbpwd.jar encrypt --password "password"

# 解密密码
java -jar dbpwd.jar decrypt --password "ENC(SM4:...)"

# 从配置中心下载
java -jar dbpwd.jar download --config-center-url "..."

# 查看帮助
java -jar dbpwd.jar help
```

### B. 连接池配置参考

**Druid默认配置**:
```yaml
spring:
  datasource:
    druid:
      initial-size: 5
      min-idle: 5
      max-active: 20
      max-wait: 60000
      time-between-eviction-runs-millis: 60000
      min-evictable-idle-time-millis: 300000
```

**HikariCP默认配置**:
```yaml
spring:
  datasource:
    hikari:
      minimum-idle: 10
      maximum-pool-size: 20
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### C. 相关文档

- [用户使用手册](user-guide.md) - 用户操作指南
- [系统部署文档](deployment-guide.md) - 部署运维指南
- [API参考文档](api-reference.md) - API参考手册
- [开发者文档](developer-guide.md) - 开发者参考
