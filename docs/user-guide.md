# 用户使用手册

> 文档版本: v1.0
> 最后更新: 2026-02-10

## 目录

1. [概述](#概述)
2. [安装部署](#安装部署)
3. [快速开始](#快速开始)
4. [命令行工具使用](#命令行工具使用)
5. [配置说明](#配置说明)
6. [密码更新流程](#密码更新流程)
7. [常见操作场景](#常见操作场景)
8. [故障排查](#故障排查)

---

## 概述

### 系统介绍

Database Password Manager 是一个基于 SM4 国密算法的数据库密码管理系统，提供统一的密码加密、存储、分发和管理功能。

### 核心特性

- **SM4国密加密**: 采用符合国家密码标准的SM4算法
- **JSON格式存储**: 清晰易读，便于管理
- **配置中心集成**: 支持从配置中心同步密码文件
- **框架无缝集成**: 支持Spring Boot、MyBatis、Struts等框架
- **多环境支持**: 支持开发、测试、生产等多环境管理
- **本地安全管理**: 应用与配置中心完全解耦

### 适用场景

- 数据库密码需要加密存储
- 多应用共享数据库连接配置
- 密码需要集中管理和分发
- 需要与Spring Boot/MyBatis等框架集成

---

## 安装部署

### 环境要求

| 组件 | 版本要求 | 必需 |
|------|---------|------|
| JDK | 8+ (推荐11+) | 是 |
| Maven | 3.6+ | 是 (用于构建) |

### 安装步骤

#### 1. 下载项目

```bash
git clone https://github.com/faunt1/dbpwd.git
cd dbpwd
```

#### 2. 编译项目

```bash
mvn clean package
```

编译成功后，在 `db-pwd-core/target/` 目录下会生成 `dbpwd-1.0.0-jar-with-dependencies.jar` 文件。

#### 3. 配置目录

创建配置目录（默认位置为 `~/.dbpwd/`）：

```bash
# Linux/Mac
mkdir -p ~/.dbpwd
chmod 700 ~/.dbpwd

# Windows (PowerShell)
New-Item -ItemType Directory -Path $env:USERPROFILE\.dbpwd -Force
```

---

## 快速开始

### 第一次使用

#### 1. 生成SM4密钥

```bash
# 使用命令行工具自动生成密钥
java -cp db-pwd-core/target/dbpwd-1.0.0-jar-with-dependencies.jar \
  com.dbpwd.cli.DBPasswordCLI generate-key
```

密钥会保存到 `~/.dbpwd/sm4.key`

#### 2. 添加数据库配置

**交互模式**：
```bash
java -cp db-pwd-core/target/dbpwd-1.0.0-jar-with-dependencies.jar \
  com.dbpwd.cli.DBPasswordCLI
```

进入交互模式后，选择 `add` 命令，按提示输入数据库信息。

**命令行模式**：
```bash
java -cp db-pwd-core/target/dbpwd-1.0.0-jar-with-dependencies.jar \
  com.dbpwd.cli.DBPasswordCLI add \
  --id db001 \
  --name "主数据库" \
  --type mysql \
  --host 192.168.1.100 \
  --port 3306 \
  --database mydb \
  --url "jdbc:mysql://192.168.1.100:3306/mydb" \
  --username admin \
  --password "your_password_here"
```

#### 3. 测试数据库连接

```bash
java -cp db-pwd-core/target/dbpwd-1.0.0-jar-with-dependencies.jar \
  com.dbpwd.cli.DBPasswordCLI test --database-id db001
```

#### 4. 查看数据库列表

```bash
java -cp db-pwd-core/target/dbpwd-1.0.0-jar-with-dependencies.jar \
  com.dbpwd.cli.DBPasswordCLI list
```

---

## 命令行工具使用

### 交互模式

运行命令行工具进入交互模式：

```bash
java -cp db-pwd-core/target/dbpwd-1.0.0-jar-with-dependencies.jar \
  com.dbpwd.cli.DBPasswordCLI
```

进入交互模式后，会显示菜单：

```
Database Password Manager CLI
===============================

Available Commands:
  1. List all databases     - 查看所有数据库
  2. Add new database       - 添加新数据库
  3. Update database        - 更新数据库配置
  4. Remove database        - 删除数据库
  5. Test connection        - 测试数据库连接
  6. Encrypt password       - 加密密码
  7. Decrypt password       - 解密密码
  8. Download from config   - 从配置中心下载
  0. Exit                  - 退出

Enter command number or command name:
```

### 命令行模式

#### help - 帮助

```bash
# 查看帮助信息
java -jar dbpwd.jar help

# 查看特定命令帮助
java -jar dbpwd.jar help add
```

#### generate-key - 生成密钥

```bash
java -jar dbpwd.jar generate-key
```

#### list - 列出数据库

```bash
# 列出所有数据库
java -jar dbpwd.jar list

# 列出指定类型的数据库
java -jar dbpwd.jar list --type mysql

# 详细模式
java -jar dbpwd.jar list --verbose
```

#### add - 添加数据库

```bash
# 完整参数
java -jar dbpwd.jar add \
  --id db001 \
  --name "主数据库" \
  --type mysql \
  --host 192.168.1.100 \
  --port 3306 \
  --database mydb \
  --url "jdbc:mysql://192.168.1.100:3306/mydb" \
  --username admin \
  --password "your_password_here"

# 最简参数（其他会自动推断）
java -jar dbpwd.jar add \
  --id db001 \
  --type mysql \
  --host 192.168.1.100 \
  --database mydb \
  --username admin \
  --password "your_password_here"
```

#### update - 更新数据库

```bash
# 更新所有字段
java -jar dbpwd.jar update --id db001 \
  --host 192.168.1.200 \
  --password "new_password_here"

# 仅更新部分字段
java -jar dbpwd.jar update --id db001 \
  --password "new_password_here"
```

#### remove - 删除数据库

```bash
java -jar dbpwd.jar remove --id db001
```

#### test - 测试连接

```bash
# 测试指定数据库
java -jar dbpwd.jar test --database-id db001

# 测试所有数据库
java -jar dbpwd.jar test --all
```

#### encrypt - 加密密码

```bash
# 加密密码
java -jar dbpwd.jar encrypt --password "my_password"

# 指定密钥文件
java -jar dbpwd.jar encrypt \
  --password "my_password" \
  --key-file /path/to/sm4.key
```

输出格式示例：
```
Encrypted password: ENC(SM4:r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=)
```

#### decrypt - 解密密码

```bash
# 解密密码
java -jar dbpwd.jar decrypt \
  --password "ENC(SM4:r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=)"

# 指定密钥文件
java -jar dbpwd.jar decrypt \
  --password "ENC(SM4:r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=)" \
  --key-file /path/to/sm4.key
```

#### download - 从配置中心下载

```bash
# 从HTTP/HTTPS配置中心下载
java -jar dbpwd.jar download \
  --config-center-url "https://config.example.com/api/dbpwd/g1/a1/app1/prod" \
  --api-key "your_api_key"

# 从本地文件系统下载
java -jar dbpwd.jar download \
  --config-center-url "/path/to/config/g1/a1/app1/prod"

# 对比模式（不自动下载）
java -jar dbpwd.jar download \
  --config-center-url "https://config.example.com/api/dbpwd/g1/a1/app1/prod" \
  --api-key "your_api_key" \
  --compare-only
```

---

## 配置说明

### 配置文件位置

| 文件 | 默认位置 | 用途 |
|------|---------|------|
| 密钥文件 | `~/.dbpwd/sm4.key` | SM4加密密钥 |
| 密码文件 | `~/.dbpwd/db.pwd` | 数据库配置文件 |
| 配置目录 | `~/.dbpwd/` | 配置文件所在目录 |

### 配置文件权限（Linux/Mac）

出于安全考虑，建议设置正确的文件权限：

```bash
# 配置目录权限：700（仅所有者可访问）
chmod 700 ~/.dbpwd

# 密钥文件权限：600（仅所有者可读写）
chmod 600 ~/.dbpwd/sm4.key

# 密码文件权限：600（仅所有者可读写）
chmod 600 ~/.dbpwd/db.pwd
```

### 密码文件格式

密码文件 `db.pwd` 使用JSON格式，示例：

```json
{
  "version": "1.0",
  "lastModified": "2026-02-10T10:30:45Z",
  "databases": [
    {
      "id": "db001",
      "name": "主数据库",
      "type": "mysql",
      "host": "192.168.1.100",
      "port": 3306,
      "database": "mydb",
      "url": "jdbc:mysql://192.168.1.100:3306/mydb",
      "username": "admin",
      "password": "ENC(SM4:r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=)",
      "encrypted": true,
      "createdAt": "2026-02-08T08:00:00Z",
      "updatedAt": "2026-02-10T10:30:45Z",
      "properties": {
        "useSSL": "false",
        "serverTimezone": "UTC"
      }
    }
  ]
}
```

### 支持的数据库类型

| 类型 | type值 | JDBC驱动类 |
|------|--------|-----------|
| MySQL | mysql | com.mysql.cj.jdbc.Driver |
| PostgreSQL | postgresql | org.postgresql.Driver |
| Oracle | oracle | oracle.jdbc.OracleDriver |
| SQL Server | sqlserver | com.microsoft.sqlserver.jdbc.SQLServerDriver |
| H2 | h2 | org.h2.Driver |
| DB2 | db2 | com.ibm.db2.jcc.DB2Driver |
| Sybase | sybase | com.sybase.jdbc4.jdbc.SybDriver |
| SQLite | sqlite | org.sqlite.JDBC |

---

## 密码更新流程

### 标准密码更新步骤

#### 1. 测试环境先行

```bash
# 加密新密码
java -jar dbpwd.jar encrypt --password "new_test_password"

# 更新测试环境数据库配置
java -jar dbpwd.jar update --id test-db001 --password "new_test_password"

# 测试连接
java -jar dbpwd.jar test --database-id test-db001
```

#### 2. 更新配置中心

将新的密码文件上传到配置中心，路径格式为：
```
{配置中心根目录}/{物理组}/{应用名称}/{应用模块}/{环境}/
```

例如：
```
/config/groupA/app-module1/app1/test/db.pwd
/config/groupA/app-module1/app1/prod/db.pwd
```

#### 3. 生产环境同步

```bash
# 在生产环境服务器上执行
java -jar dbpwd.jar download \
  --config-center-url "https://config.example.com/api/dbpwd/groupA/app-module1/app1/prod" \
  --api-key "your_api_key"
```

#### 4. 验证连接

```bash
# 验证生产环境连接
java -jar dbpwd.jar test --database-id prod-db001
```

#### 5. 重启应用

重启使用该数据库连接的应用，让新密码生效。

### 密码迁移（明文→加密）

如果现有数据库配置使用明文密码，可以按以下步骤迁移：

#### 1. 查看现有配置

```bash
cat ~/.dbpwd/db.pwd
```

#### 2. 识别明文密码

查找没有 `ENC(SM4:...)` 格式的密码字段。

#### 3. 加密并替换

```bash
# 加密明文密码
java -jar dbpwd.jar encrypt --password "plain_password_here"

# 获取加密后的密文，然后更新数据库配置
java -jar dbpwd.jar update --id db001 --password "ENC(SM4:...)"
```

或使用 add 命令重新添加（会自动加密）。

---

## 常见操作场景

### 场景1：配置新的数据库连接

**需求**：为新的Spring Boot项目配置数据库连接

**步骤**：
1. 生成SM4密钥（如果还没有）
   ```bash
   java -jar dbpwd.jar generate-key
   ```

2. 添加数据库配置
   ```bash
   java -jar dbpwd.jar add \
     --id myapp-main \
     --name "MyApp主数据库" \
     --type mysql \
     --host localhost \
     --port 3306 \
     --database myapp \
     --username myapp_user \
     --password "my_secure_password"
   ```

3. 测试连接
   ```bash
   java -jar dbpwd.jar test --database-id myapp-main
   ```

4. 配置Spring Boot应用

   在 `application.yml` 或 `application.properties` 中配置：
   ```yaml
   dbpwd:
     enabled: true
     password-file: ~/.dbpwd/db.pwd
     key-file: ~/.dbpwd/sm4.key

   spring:
     datasource:
       type: com.alibaba.druid.pool.DruidDataSource
       url: jdbc:mysql://localhost:3306/myapp
       username: myapp_user
       password: myapp-main  # 使用数据库ID作为占位符
       driver-class-name: com.mysql.cj.jdbc.Driver
   ```

### 场景2：定期轮换数据库密码

**需求**：每90天轮换一次生产数据库密码

**步骤**：
1. 生成新密码（建议使用密码生成器）
2. 在测试环境验证新密码
3. 更新生产环境配置中心
4. 同步到生产服务器
5. 验证后重启应用

详细步骤参见[密码更新流程](#密码更新流程)。

### 场景3：多环境管理

**需求**：一个应用在dev、test、prod三个环境使用不同的数据库

**方法**：使用不同的配置文件路径或不同的 `database.id`

**方法1：使用不同配置目录**
```bash
# 开发环境
~/.dbpwd/dev/db.pwd
~/.dbpwd/dev/sm4.key

# 测试环境
~/.dbpwd/test/db.pwd
~/.dbpwd/test/sm4.key

# 生产环境
~/.dbpwd/prod/db.pwd
~/.dbpwd/prod/sm4.key
```

在Spring Boot应用中通过环境变量指定路径：
```bash
# 开发环境
export DBPWD_CONFIG_DIR=~/.dbpwd/dev

# 测试环境
export DBPWD_CONFIG_DIR=~/.dbpwd/test

# 生产环境
export DBPWD_CONFIG_DIR=~/.dbpwd/prod
```

应用配置：
```yaml
dbpwd:
  password-file: ${DBPWD_CONFIG_DIR}/db.pwd
  key-file: ${DBPWD_CONFIG_DIR}/sm4.key
```

**方法2：使用不同的数据库ID**
在每个环境的 `db.pwd` 文件中，使用相同的ID（如 `main-db`），但配置不同的连接信息。这样应用配置无需修改。

### 场景4：密码泄露应急处理

**需求**：发现密码可能泄露，需要紧急更换

**步骤**：
1. 立即在所有相关环境密码文件中删除该数据库条目
2. 更新数据库密码（直接在数据库中修改）
3. 生成新的SM4密钥（可选，增加安全性）
   ```bash
   java -jar dbpwd.jar generate-key
   ```
4. 重新添加数据库配置
5. 更新配置中心
6. 紧急同步到所有环境
7. 检查访问日志，确认是否有异常访问

---

## 故障排查

### 问题1：命令行工具无法启动

**现象**：运行命令出现 `ClassNotFoundException`

**原因**：JAR文件路径不正确或依赖缺失

**解决**：
1. 确认使用的是带有所有依赖的JAR文件
   ```bash
   # 使用 jar-with-dependencies 版本
   java -cp db-pwd-core/target/dbpwd-1.0.0-jar-with-dependencies.jar \
     com.dbpwd.cli.DBPasswordCLI

   # 或者使用完整jar命令
   java -jar db-pwd-core/target/dbpwd-1.0.0-jar-with-dependencies.jar
   ```

2. 确认项目已正确编译
   ```bash
   mvn clean package
   ```

### 问题2：连接数据库失败

**现象**：执行 `test` 命令时连接失败

**检查项**：
1. 确认数据库服务正在运行
2. 确认网络可达（ping数据库主机）
3. 确认数据库端口正确
4. 确认用户名和密码正确
5. 确认JDBC驱动已安装

**解决**：
```bash
# 1. 测试网络连通性
ping 192.168.1.100

# 2. 测试端口
telnet 192.168.1.100 3306

# 3. 查看详细错误信息
java -jar dbpwd.jar test --database-id db001 --verbose
```

### 问题3：密码解密失败

**现象**：应用启动时提示密码解密失败

**原因**：密钥文件不存在或密钥不匹配

**解决**：
1. 确认密钥文件存在
   ```bash
   ls -la ~/.dbpwd/sm4.key
   ```

2. 确认密钥文件权限正确
   ```bash
   chmod 600 ~/.dbpwd/sm4.key
   ```

3. 确认密钥与密码文件匹配
   ```bash
   # 尝试手动解密验证
   java -jar dbpwd.jar decrypt \
     --password "ENC(SM4:r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=)"
   ```

### 问题4：从配置中心下载失败

**现象**：执行 `download` 命令失败

**检查项**：
1. 确认配置中心URL正确
2. 确认API Key正确（HTTP/HTTPS模式）
3. 确认网络可以访问配置中心
4. 确认配置中心有对应的文件

**解决**：
```bash
# 使用对比模式查看差异
java -jar dbpwd.jar download \
  --config-center-url "https://config.example.com/api/dbpwd/g1/a1/app1/prod" \
  --api-key "your_api_key" \
  --compare-only

# 使用详细模式查看更多信息
java -jar dbpwd.jar download \
  --config-center-url "https://config.example.com/api/dbpwd/g1/a1/app1/prod" \
  --api-key "your_api_key" \
  --verbose
```

### 问题5：Spring Boot应用启动失败

**现象**：Spring Boot应用启动时DataSource初始化失败

**解决**：
1. 确认 `dbpwd` 相关配置正确
   ```yaml
   dbpwd:
     enabled: true
     password-file: ~/.dbpwd/db.pwd
     key-file: ~/.dbpwd/sm4.key
   ```

2. 确认数据库配置中的password字段使用的是数据库ID
   ```yaml
   spring:
     datasource:
       password: myapp-main  # 必须是数据库ID，不是密文
       # 不要使用：password: ENC(SM4:...)
   ```

3. 检查日志中是否有更详细的错误信息

### 问题6：密钥文件警告

**现象**：系统提示密钥文件权限不安全

**原因**：密钥或密码文件权限不是600

**解决**：
```bash
# Linux/Mac
chmod 600 ~/.dbpwd/sm4.key
chmod 600 ~/.dbpwd/db.pwd
chmod 700 ~/.dbpwd

# Windows
# Windows不支持文件权限设置，可以忽略此警告
# 但建议确保配置目录只有当前用户可访问
```

### 获取更多帮助

如果以上方法无法解决问题：

1. 查看日志：命令行工具会输出详细的错误信息
2. 查看技术文档：[技术设计文档](../overview.md)
3. 提交Issue：在项目仓库提交问题报告

---

## 附录

### A. 环境变量

| 变量名 | 说明 | 默认值 |
|--------|------|--------|
| `DBPWD_CONFIG_DIR` | 配置目录路径 | `~/.dbpwd` |
| `DBPWD_PASSWORD_FILE` | 密码文件路径 | `${DBPWD_CONFIG_DIR}/db.pwd` |
| `DBPWD_KEY_FILE` | 密钥文件路径 | `${DBPWD_CONFIG_DIR}/sm4.key` |
| `DBPWD_ENABLED` | 是否启用dbpwd | `true` |

### B. 配置中心目录规范

配置中心目录结构应遵循以下规范：
```
{配置中心根目录}/{物理组}/{应用名称}/{应用模块}/{环境}/
```

示例：
```
/config/
  ├── groupA/
  │   └── finance-system/
  │       └── payment-app/
  │           ├── prod/
  │           │   ├── db.pwd
  │           │   └── sm4.key
  │           └── test/
  │               ├── db.pwd
  │               └── sm4.key
```

### C. 相关文档链接

- [技术设计文档](../overview.md) - 系统架构和技术设计
- [开发计划](../technical/development-plan.md) - 开发进度和规划
- [常见问题FAQ](FAQ.md) - 常见问题和解答
- [API文档](api-reference.md) - API参考手册
