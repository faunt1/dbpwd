# Database Password Manager

一个基于 SM4 加密的数据库密码管理系统，提供本地安全管理和数据库连接测试。

## 简介

本项目提供统一的数据库密码管理方案，解决以下问题：
- 数据库密码集中化管理
- 密码加密存储和传输
- 密码安全分发和更新
- 连接池集成无缝使用

## 快速开始

### 1. 安装

编译项目：
```bash
mvn clean package
```

### 2. 配置

创建配置目录和文件：
```bash
mkdir -p ~/.dbpwd
echo "AABBCCDD1234567890ABCDEF12345678" > ~/.dbpwd/sm4.key
chmod 600 ~/.dbpwd/sm4.key
```

创建密码文件示例（db.pwd）：
```json
{
  "version": "1.0",
  "lastModified": "2026-02-08T10:30:45Z",
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
      "createdAt": "2026-01-15T08:00:00Z",
      "updatedAt": "2026-02-08T10:30:45Z"
    }
  ]
}
```

### 3. 使用

运行命令行工具：
```bash
java -jar dbpwd-cli.jar
```

## 打包和发布

### Maven 打包命令

项目提供了三种打包模式，可通过 Maven Profile 轻松切换：

| 命令 | 说明 | 输出文件 |
|-----|------|---------|
| `mvn clean package` | 打包所有（默认） | 所有 JAR 和 ZIP 包 |
| `mvn clean package -Pdocs` | 仅打包文档 | `db-pwd-1.0.0-docs.zip` |
| `mvn clean package -Psource` | 仅打包源码 | `db-pwd-1.0.0-source.{zip,tar.gz}` |
| `mvn clean package -Pdist` | 仅打包发布包 | `db-pwd-1.0.0-dist.{zip,tar.gz}` |

### 包类型说明

#### 1. 文档包 (docs)
仅包含项目文档，不包含源码和编译文件：
- README.md、CLAUDE.md、overview.md 等根目录文档
- `technical/` 目录下的所有技术文档
- `docs/` 目录下的额外文档
- `config/` 目录配置文件

**文件名格式**：`db-pwd-{version}-docs.zip`

#### 2. 源码包 (source)
包含完整源代码，不含 target 目录：
- `src/` 源代码目录
- `pom.xml` 构建配置
- 所有文档文件
- config 和 scripts 目录

**文件名格式**：`db-pwd-{version}-source.{zip|tar.gz}`

#### 3. 发布包 (dist)
包含可执行文件和所有依赖：
- JAR 文件（含依赖）
- lib/ 目录（所有依赖 JAR）
- bin/ 目录（启动脚本）
- 完整文档
- config/ 目录配置文件

**文件名格式**：`db-pwd-{version}-dist.{zip|tar.gz}`

### 快速打包示例

```bash
# 完整打包（所有包）
mvn clean package

# 仅打包文档
mvn clean package -Pdocs

# 仅打包源码用于归档
mvn clean package -Psource

# 仅打包用于发布的版本
mvn clean package -Pdist

# 同时生成文档和源码包
mvn clean package -Pdocs,source
```

### 输出目录

所有生成的包位于 `target/` 目录下：

```
target/
├── db-pwd-1.0.0.jar                    # 普通 JAR
├── db-pwd-1.0.0-jar-with-dependencies.jar  # 可执行 JAR
├── db-pwd-1.0.0-docs.zip               # 文档包
├── db-pwd-1.0.0-source.zip             # 源码包
├── db-pwd-1.0.0-source.tar.gz          # 源码包 (tar.gz)
├── db-pwd-1.0.0-dist.zip               # 发布包
└── db-pwd-1.0.0-dist.tar.gz            # 发布包 (tar.gz)
```

## 核心特性

- **SM4国密加密**: 默认使用SM4国密算法进行密码加密
- **JSON格式存储**: 清晰易读，支持多条数据库配置
- **Druid兼容格式**: 支持ENC(SM4:密文)格式
- **本地安全管理**: 应用与配置中心完全解耦，只读本地文件
- **文件权限控制**: 密钥文件和密码文件强制600权限
- **多环境支持**: 支持prod/test/dev等环境分层管理

## 项目结构

```
src/main/java/com/dbpwd/
├── model/       # 数据模型
├── core/        # 核心模块
├── crypto/      # 加密解密
└── cli/         # 命令行工具
```

## 文档索引

### 技术设计
- [技术设计详情](overview.md) - 技术设计文档
- [密码文件格式](technical/password-format.md) - 密码文件规范
- [SM4加密详解](technical/sm4-encryption.md) - 加密流程和实现
- [密钥派生算法](technical/sm4-encryption.md#密钥派生算法说明) - PBKDF2详细说明
- [JDK版本兼容](technical/jdk-compatibility.md) - JDK版本支持对比

### 集成实现
- [配置中心集成](technical/config-center.md) - 配置中心目录结构
- [命令行工具](technical/command-line.md) - CLI命令和参数说明
- [连接池包装](technical/connection-pool.md) - Druid/HikariCP包装实现
- [框架集成](technical/framework-integration.md) - Spring/MyBatis集成

### 安全机制
- [安全机制设计](technical/security.md) - SSH对比、安全增强建议

### 开发相关
- [代码结构](technical/code-structure.md) - 项目目录结构说明
- [开发计划](technical/development-plan.md) - Sprint划分、里程碑、测试要求

## 技术要求

- **JDK版本**: JDK 8+ (推荐JDK 11)
- **加密库**: Bouncy Castle 1.69+ (JDK 8)
- **JSON库**: Jackson 2.12+
- **构建工具**: Maven 3.6+

## 许可证

> 文档版本: v3.0
> 最后更新: 2026-02-08
> 系统要求: JDK 8+
