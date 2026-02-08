# Druid Database Password Manager - 设计文档

> 文档版本: v3.0
> 最后更新: 2026-02-08

## 目录

1. [项目目标](#项目目标)
2. [核心设计决策](#核心设计决策)
3. [整体架构](overview.md)
4. [密码文件格式规范](technical/password-format.md)
5. [SM4密钥管理](technical/key-management.md)
6. [密钥管理概述](overview.md#4-sm4密钥管理)
7. [密码更新流程](technical/password-update-flow.md)
8. [密码更新概述](overview.md#5-密码更新流程)
9. [项目代码结构](technical/code-structure.md)
10. [配置中心集成](technical/config-center.md)
11. [命令行工具](technical/command-line.md)
12. [配置属性说明](technical/properties.md)
13. [安全机制设计](technical/security.md)
14. [技术栈](#技术栈)
15. [SM4加密详解](technical/sm4-encryption.md)
16. [连接池包装类](technical/connection-pool.md)
17. [框架集成](technical/framework-integration.md)
18. [安全检查清单](technical/security.md#安全检查清单)
19. [设计决策总结](overview.md#设计决策总结)
20. [JDK版本兼容](technical/jdk-compatibility.md)
21. [开发计划](technical/development-plan.md)

---

## 项目目标

实现一个基于 SM4 加密的数据库密码管理系统，采用JSON格式存储密码文件，支持本地安全管理和数据库连接测试。

## 快速开始

### 1. 安装与配置

```bash
# 编译项目
mvn clean package

# 创建配置目录
mkdir -p ~/.dbpwd && chmod 700 ~/.dbpwd

# 密钥文件（16字节hex编码）
echo "AABBCCDD1234567890ABCDEF12345678" > ~/.dbpwd/sm4.key
chmod 600 ~/.dbpwd/sm4.key
```

### 2. 密码文件示例

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
      "encrypted": true
    }
  ]
}
```

### 3. 运行工具

```bash
java -jar dbpwd-cli.jar
```

---

## 核心设计决策

| 决策点 | 选项 | 理由 |
|-------|------|------|
| 密码文件格式 | JSON格式 | 可读性强，易扩展，工具友好 |
| 密码字段格式 | Druid ENC格式 | 兼容性强，可扩展其他算法 |
| 配置中心使用 | 存储实际文件+多环境管理 | 支持多环境分层 |
| 应用与配置中心 | 完全解耦 | 应用只读本地文件，提高稳定性 |
| 同步机制 | 独立同步工具 | 不影响应用运行，可控的文件更新 |
| 密钥存储位置 | ~/.dbpwd/sm4.key | 标准化路径，权限可控 |
| 密钥保护 | 600权限，可选密码短语 | 参照SSH机制，增强安全 |
| 容器部署 | 外挂文件600权限 | 灵活部署，安全可控 |
| JDK版本 | JDK 8+ | 兼容现有系统 |

---

## 技术栈

| 组件 | 技术选择 |
|------|---------|
| **JDK版本** | JDK 8+ |
| **加密库** | Bouncy Castle (1.69+) SM4实现 |
| **JSON库** | Jackson (2.12+) |
| **日志框架** | JDK Logging / SLF4J |
| **构建工具** | Maven 3.6+ |

---

## 快速参考

### 常用命令

```bash
# 查看帮助
dbpwd-cli --help

# 交互式模式
java -cp . com.dbpwd.cli.DBPasswordCLI

# 测试数据库连接
dbpwd-cli test db001

# 加密密码
dbpwd-cli encrypt "mypassword"

# 解密密码
dbpwd-cli decrypt ENC(SM4:r7X9k3mP8nQ2vL5w)
```

### 应用配置

```yaml
dbpwd:
  enabled: true
  password-file: ~/.dbpwd/db.pwd
  key-file: ~/.dbpwd/sm4.key
  connection:
    timeout: 5000
```

---

## 许可证

> 文档版本: v3.0
> 最后更新: 2026-02-08
> 系统要求: JDK 8+

## 相关链接

项目主页: [https://github.com/your-org/dbpwd](https://github.com/your-org/dbpwd)
