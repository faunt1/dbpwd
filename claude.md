# Druid Database Password Manager 设计文档

## 目录

1. [项目目标](#项目目标)
2. [核心设计决策](#核心设计决策)
3. [整体架构](#整体架构)
4. [密码文件格式规范](#密码文件格式规范)
5. [SM4密钥管理](#sm4密钥管理)
6. [密码更新流程](#密码更新流程)
7. [项目代码结构](#项目代码结构)
8. [配置中心集成](#配置中心集成)
9. [命令行工具](#命令行工具)
10. [配置属性说明](#配置属性说明)
11. [安全机制设计](#安全机制设计)
12. [技术栈](#技术栈)
13. [连接池包装类（待实现）](#连接池包装类待实现)
14. [框架集成（待实现）](#框架集成待实现)
15. [安全检查清单](#安全检查清单)
16. [设计决策总结](#设计决策总结)
17. [开发计划](#开发计划)

---

## 项目目标

实现一个基于 SM4 加密的数据库密码管理系统，采用JSON格式存储密码文件，支持本地安全管理和数据库连接测试。

---

## 核心设计决策

### 密码文件格式：完整JSON结构

采用完整的JSON文件格式存储密码信息：
- 文件包含版本信息和数据库数组
- 每个数据库条目包含完整的连接信息（id, name, type, host, port, database, url, username, password等）
- 支持更新时间对比和同步

### 密码字段格式：Druid兼容

参考Apache Druid的 ENC 格式：
- 加密密码：`ENC(SM4:AABBCCDD1234567890...)`
- 明文密码：直接存储明文字符串（特殊场景）
- 支持未来扩展其他算法

### 配置中心策略

- **文件存储**：配置中心存储实际的 `db.pwd` (密码文件) 和 `sm4.key` (密钥文件)，按环境分层
- **多环境支持**：按生产、测试等环境目录层次保存，每个环境有自己的配置中心评审管理流程
- **应用解耦**：应用程序只使用本地文件，与配置中心完全解耦
- **同步机制**：通过独立的同步工具或脚本从配置中心拉取文件到本地
- **评审流程**：通过配置中心的评审管理流程控制密码和密钥的更新

---

## 整体架构

### 系统目标

提供统一的数据库密码管理方案，解决以下问题：
- 数据库密码集中化管理
- 密码加密存储和传输
- 密码安全分发和更新
- 连接池集成无缝使用

### 架构组件

```
┌─────────────────────────────────────────────────────────────┐
│                    应用层 (Application)                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  SpringBoot  │  │   MyBatis    │  │   原生JDBC    │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
└─────────┼─────────────────┼─────────────────┼──────────────┘
          │                 │                 │
          └─────────────────┴─────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────────┐
│              连接池包装层 (Connection Pool Wrapper)          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │  DruidWrap   │  │  HikariWrap  │  │ DBCP2Wrap    │      │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘      │
└─────────┼─────────────────┼─────────────────┼──────────────┘
          │                 │                 │
          └─────────────────┴─────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────────┐
│              密码服务核心层 (Password Service Core)          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │ SM4加解密     │  │ 密钥加载      │  │ 本地文件管理  │      │
│  │  Service     │  │  Service     │  │  Service     │      │
│  └──────┴───────┘  └──────┴───────┘  └──────┴───────┘      │
└─────────────────────────────────────────────────────────────┘
                            │
┌───────────────────────────▼──────────────────────────────────┐
│              存储层 (Storage - 仅本地文件)                     │
│  ┌──────────────────────────────┐  ┌──────────────┐        │
│  │  ~/.dbpwd/db.pwd             │  │ ~/.dbpwd/    │        │
│  │  (密码文件 - JSON格式)        │  │ sm4.key      │        │
│  │  权限: 600                    │  │ 权限: 600    │        │
│  └──────────────────────────────┘  └──────────────┘        │
│                                                                    │
│  容器部署: 挂载目录 /app/.dbpwd (外部文件600权限)                   │
└────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│           配置中心 (Config Center) - 独立部署                  │
│                   ↓ 仅文件存储和分发                          │
│              (启动前/定时同步工具拉取到本地)                   │
└────────────────────────────────────────────────────────────┘
```

### 设计原则

1. **最小权限原则**: 文件权限严格设置为600
2. **职责分离**: 系统管理密钥，应用管理密码文件
3. **可审计**: 所有密文操作记录更新时间
4. **兼容性**: 支持Druid ENC格式明文/密文混用
5. **可测试**: 仅需sm4.key即可测试所有数据库连接

---

## 密码文件格式规范

### JSON结构

**文件路径**：`~/.dbpwd/db.pwd`
**文件权限**：600（仅所有者读写）

```json
{
  "version": "1.0",
  "lastModified": "2026-02-08T10:30:45Z",
  "databases": [
    {
      "id": "db001",
      "name": "主账务数据库",
      "type": "mysql",
      "host": "192.168.1.100",
      "port": 3306,
      "database": "acdb_main",
      "url": "jdbc:mysql://192.168.1.100:3306/acdb_main",
      "username": "app_user",
      "password": "ENC(SM4:AABBCCDD1234567890ABCDEF12345678)",
      "encrypted": true,
      "createdAt": "2026-01-15T08:00:00Z",
      "updatedAt": "2026-02-08T10:30:45Z",
      "description": "生产环境主数据库",
      "tags": ["prod", "primary"]
    },
    {
      "id": "db002",
      "name": "只读从库",
      "type": "postgresql",
      "host": "192.168.1.101",
      "port": 5432,
      "database": "analytics",
      "url": "jdbc:postgresql://192.168.1.101:5432/analytics",
      "username": "readonly",
      "password": "plain_text_password",
      "encrypted": false,
      "createdAt": "2026-01-20T09:00:00Z",
      "updatedAt": "2026-01-20T09:00:00Z"
    }
  ]
}
```

### 字段定义

| 字段名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| id | String | 是 | 数据库唯一标识 |
| name | String | 是 | 数据库描述名称 |
| type | String | 是 | 数据库类型（mysql/postgresql/oracle/sqlserver/db2/sybase/h2/hsqldb） |
| host | String | 是 | 数据库主机地址 |
| port | Integer | 是 | 数据库端口 |
| database | String | 是 | 数据库名称 |
| url | String | 是 | JDBC连接URL |
| username | String | 是 | 连接用户名 |
| password | String | 是 | 密码（支持密文或明文） |
| encrypted | Boolean | 是 | 是否加密存储 |
| createdAt | String | 是 | 创建时间（ISO8601） |
| updatedAt | String | 是 | 最后更新时间（ISO8601） |
| description | String | 否 | 备注说明 |
| tags | Array | 否 | 标签数组 |
| driver | String | 否 | JDBC驱动类名 |

### 密码字段格式

| 格式 | 示例 | 说明 |
|------|------|------|
| SM4加密 | `ENC(SM4:AABBCCDD1234567890ABCDEF12345678)` | 格式：ENC(算法:密文) |
| 明文密码 | `myPassword123` | 直接明文（不推荐） |

### JSON格式 vs 编码格式（密码字段包含所有数据库信息）对比

**设计选择**: 采用完整JSON结构存储所有数据库信息

| 对比维度 | JSON格式（本文档方案） | 编码格式（密码字段包含所有数据库信息）（备选方案） |
|---------|---------------------|-------------------|
| **信息组织** | 数据库信息独立字段，清晰明了 | 所有信息编码到密码字段 |
| **密码字段** | 仅存储加密密码 | 存储URL、用户名、密码混合编码 |
| **可读性** | JSON清晰易读，便于审计 | 密码字段过长，难以人工读取 |
| **可扩展性** | 新增字段不破坏向后兼容 | 字段变更需要重新编码 |
| **工具支持** | 各语言原生JSON支持 | 需要自定义解析器 |
| **增量更新** | 单条记录独立更新 | 整体编码，批量处理困难 |
| **维护复杂度** | 低，标准JSON操作 | 高，需要编码解码逻辑 |
| **验证逻辑** | 简单，JSON schema验证 | 复杂，需解析编码格式 |

**编码格式（密码字段包含所有数据库信息）示例（不采用的原因）**:
```
# 编码格式会将URL、用户名、密码串在一起
password: "ENC(SM4:AABBCCDD...|jdbc:mysql://...:3306/db|username)"
```

**不采用编码格式（密码字段包含所有数据库信息）的原因**:
1. **密码字段过于复杂**: 将多个不同性质的信息强制合并到同一字段，违反单一职责原则
2. **维护困难**: 修改URL或用户名需要重新编码密码字段
3. **不支持增量更新**: 更新单个数据库条目需要处理整个文件
4. **难以审计**: 密码字段过长且包含非密码信息，不利于安全审计
5. **工具不友好**: 需要自定义解析器，无法使用标准JSON工具

---

## SM4密钥管理

### 密钥文件

**路径**：`~/.dbpwd/sm4.key`
**权限**：600（仅所有者读写）
**内容**：16字节hex编码，32字符

```
AABBCCDD1234567890ABCDEF12345678
```

### SSH对比参考

| 特性 | SSH私钥 | SM4密钥设计 |
|------|---------|-------------|
| 文件路径 | ~/.ssh/id_rsa | ~/.dbpwd/sm4.key |
| 权限要求 | 600 | 600 |
| 警告机制 | 权限不符时拒绝加载 | 权限不符时拒绝加载，记录警告 |
| 密码短语 | 可选 | 可选（未来增强） |

---

## SM4加密流程

### 加密流程图

```
┌─────────────────────────────────────────────────────────────────┐
│                        SM4密码加密流程                            │
└─────────────────────────────────────────────────────────────────┘

明文密码 + SM4密钥（16字节，hex编码32字符）
    ↓
┌───────────────────────────────────────────────────────────┐
│ 1. 密钥验证                                               │
│    - 验证密钥格式（32字符hex）                             │
│    - 验证密钥长度（16字节）                                │
└───────────────────────────────────────────────────────────┘
    ↓
┌───────────────────────────────────────────────────────────┐
│ 2. 密钥派生（可选增强，JDK 8+兼容）                       │
│    - 使用PBKDF2WithHmacSHA256                             │
│    - 迭代次数：10,000                                     │
│    - 盐值：根据应用环境生成（如 "dbpwd_salt" + JVM标识）  │
│    - 输出长度：16字节（SM4）                              │
│    - 注意：当前版本直接使用原始密钥，未来可开启密钥派生    │
└───────────────────────────────────────────────────────────┘
    ↓
┌───────────────────────────────────────────────────────────┐
│ 3. SM4加密                                                 │
│    - 加密模式：ECB（块大小16字节）                         │
│    - 输入：明文密码（UTF-8编码）                          │
│    - 填充：PKCS7Padding（自动处理块边界）                  │
│    - 输出：加密后的密文                                    │
└───────────────────────────────────────────────────────────┘
    ↓
┌───────────────────────────────────────────────────────────┐
│ 4. Base64编码                                              │
│    - 将二进制密文编码为Base64字符串                       │
└───────────────────────────────────────────────────────────┘
    ↓
┌───────────────────────────────────────────────────────────┐
│ 5. 格式化输出                                              │
│    - 格式：ENC(SM4:密文)                                  │
│    - 示例：ENC(SM4:r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=)    │
└───────────────────────────────────────────────────────────┘
    ↓
 加密密码（存储到 db.pwd）
```

### 解密流程图

```
┌─────────────────────────────────────────────────────────────────┐
│                        SM4密码解密流程                            │
└─────────────────────────────────────────────────────────────────┘

加密密码（格式：ENC(SM4:密文)）
    ↓
┌───────────────────────────────────────────────────────────┐
│ 1. 格式解析                                               │
│    - 验证格式：ENC(SM4:xxxxxxxx)                          │
│    - 提取算法标识：SM4                                     │
│    - 提取密文：去除ENC(SM4:)前缀                           │
└───────────────────────────────────────────────────────────┘
    ↓
┌───────────────────────────────────────────────────────────┐
│ 2. 密钥加载                                               │
│    - 从 ~/.dbpwd/sm4.key 读取密钥                         │
│    - 验证文件权限：600                                     │
│    - 验证密钥格式（32字符hex）                             │
└───────────────────────────────────────────────────────────┘
    ↓
┌───────────────────────────────────────────────────────────┐
│ 3. 密钥派生（可选增强，JDK 8+兼容）                       │
│    - 使用PBKDF2WithHmacSHA256                             │
│    - 迭代次数：10,000                                     │
│    - 盐值：根据应用环境生成（需与加密时一致）              │
│    - 输出长度：16字节（SM4）                              │
└───────────────────────────────────────────────────────────┘
    ↓
┌───────────────────────────────────────────────────────────┐
│ 4. Base64解码                                              │
│    - 将密文字符串解码为二进制                             │
└───────────────────────────────────────────────────────────┘
    ↓
┌───────────────────────────────────────────────────────────┐
│ 5. SM4解密                                                 │
│    - 加密模式：ECB                                         │
│    - 输入：Base64解码后的密文                              │
│    - 去除填充：自动处理PKCS7Padding                        │
│    - 输出：明文密码（UTF-8编码）                          │
└───────────────────────────────────────────────────────────┘
    ↓
 明文密码（用于数据库连接）
```

### 加密算法参数

| 参数 | 值 | 说明 |
|------|-----|------|
| **算法** | SM4 | 国密对称加密算法 |
| **密钥长度** | 128位 | 16字节，hex编码32字符 |
| **块大小** | 128位 | 16字节，SM4标准块大小 |
| **加密模式** | ECB | 电子密码本模式 |
| **填充方式** | PKCS7Padding | 块填充，自动处理边界 |
| **编码方式** | Base64 | 二进制到文本转换 |

### 性能指标

| 操作 | 目标性能 | 说明 |
|------|---------|------|
| **加密** | < 10ms | 单次密码加密耗时 |
| **解密** | < 5ms | 单次密码解密耗时 |
| **密钥加载** | < 1ms | 密钥文件读取和验证 |
| **文件加载** | < 50ms | JSON密码文件加载和解析 |

---

## 密钥派生算法说明

### PBKDF2密钥派生（可选增强）

**目的**: 通过密钥派生增强密钥安全性，防止密钥文件泄露后的直接使用风险

**算法**: PBKDF2WithHmacSHA256（基于密码的密钥派生函数2，使用HMAC-SHA256）

### 派生参数

| 参数 | 值 | 说明 |
|------|-----|------|
| **输入密钥** | sm4.key内容 | 从密钥文件读取的hex字符串 |
| **盐值** | 根据环境生成 | 格式：`{app_name}_{env}_{uuid}` |
| **迭代次数** | 10,000 | 密钥派生迭代次数 |
| **输出长度** | 16字节（128位） | SM4密钥长度 |
| **算法标识** | HMAC-SHA256 | 底层哈希算法 |

### 派生流程

```
输入密钥 + 盐值 + 迭代次数
    ↓
PBKDF2WithHmacSHA256
    ↓
派生密钥（16字节）
    ↓
用于SM4加解密
```

### 盐值生成策略

| 策略 | 说明 | 示例 |
|------|------|------|
| **环境标识** | 应用名称 + 环境 | `dbpwd_prod` |
| **主机标识** | 应用名称 + 主机名 | `dbpwd_server-001` |
| **唯一标识** | 应用名称 + JVM UUID | `dbpwd_a1b2c3d4...` |
| **固定盐值** | 生产、测试使用不同固定值 | 生产：`prod_salt_key_2024` |

**推荐策略**: 使用 `应用名称 + 环境 + JVM UUID` 组合，确保每个应用实例有独特的盐值。

### 是否使用密钥派生

| 场景 | 是否使用派生 | 原因 |
|------|-------------|------|
| **开发环境** | ❌ 否 | 简化调试，直接使用原始密钥 |
| **测试环境** | ⚪ 可选 | 根据安全策略选择 |
| **生产环境** | ✅ 推荐 | 增强安全性，防止单点密钥泄露 |

**实现方式**: 通过配置控制是否启用密钥派生功能

```yaml
# application.yml
dbpwd:
  key-derivation:
    enabled: true              # 是否启用密钥派生
    iterations: 10000          # 迭代次数
    salt-strategy: uuid        # 盐值策略：uuid/host/fixed
    fixed-salt: prod_salt      # 固定盐值（策略为fixed时使用）
```

---

## JDK版本兼容

### JDK版本支持矩阵

| 特性 | JDK 8 | JDK 11 | JDK 17+ | 说明 |
|------|-------|--------|---------|------|
| **核心语言** | ✅ 支持 | ✅ 支持 | ✅ 支持 | 使用JDK 8语法编译 |
| **SM4算法** | ⚠️ 需额外依赖 | ✅ 原生支持 | ✅ 原生支持 | JDK 8需Bouncy Castle |
| **AES算法** | ✅ 原生支持 | ✅ 原生支持 | ✅ 原生支持 | 原生支持所有版本 |
| **JSON库** | ✅ Jackson 2.12+ | ✅ Jackson 2.12+ | ✅ Jackson 2.12+ | Jackson跨版本兼容 |
| **文件权限** | ⚠️ 仅Unix | ⚠️ 仅Unix | ⚠️ 仅Unix | Windows跳过权限检查 |
| **HTTP Client** | ❌ HttpURLConnection | ✅ HttpClient | ✅ HttpClient | 配置中心API调用 |
| **模块化** | ❌ 不支持 | ⚠️ 部分支持 | ✅ 完全支持 | JDK 9+模块化特性 |

### JDK 8 配置

**Bouncy Castle依赖**（SM4算法支持）:
```xml
<dependency>
    <groupId>org.bouncycastle</groupId>
    <artifactId>bcprov-jdk15on</artifactId>
    <version>1.70</version>
</dependency>
```

**注意**: Bouncy Castle 1.70+ 版本支持SM4算法，但验证兼容性。

### JDK 11 配置

JDK 11原生支持SM4算法，无需额外依赖。

### JDK 17+ 配置

JDK 17完全支持SM4算法，同时支持模块化部署。

```xml
<!-- module-info.java -->
module com.dbpwd {
    requires java.base;
    requires java.sql;
    requires jdk.crypto.cryptoki;  // SM4算法支持
}
```

### 版本兼容注意事项

| 注意事项 | JDK 8 | JDK 11 | JDK 17+ |
|---------|-------|--------|---------|
| **SM4可用性** | 需Bouncy Castle | 原生支持 | 原生支持 |
| **文件权限** | Unix系统600权限 | Unix系统600权限 | Unix系统600权限 |
| **编码兼容** | UTF-8编码 | UTF-8编码 | UTF-8编码 |
| **JSON解析** | Jackson 2.12+ | Jackson 2.12+ | Jackson 2.12+ |
| **连接池** | Druid/HikariCP | Druid/HikariCP | Druid/HikariCP |

### 推荐版本

| 环境 | 推荐JDK版本 | 原因 |
|------|------------|------|
| **开发环境** | JDK 11 | SM4原生支持，生态稳定 |
| **测试环境** | JDK 11 | 与生产环境版本一致 |
| **生产环境** | JDK 11 / JDK 17 | SM4原生支持，性能和安全最佳 |

### SM4算法实现方式

| JDK版本 | SM4实现 | 来源 |
|---------|---------|------|
| JDK 8 | Bouncy Castle Provider | `org.bouncycastle.jcajce.provider.BouncyCastleFipsProvider` |
| JDK 11+ | JDK内置Provider | `SunJCE` 或 `PKCS11` |

**代码示例**：
```java
// JDK 8 - 使用Bouncy Castle
Security.addProvider(new BouncyCastleProvider());
Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS7Padding", "BC");

// JDK 11+ - 使用内置Provider
Cipher cipher = Cipher.getInstance("SM4/ECB/PKCS7Padding");
```

---

## 密码更新流程

**设计选择**: 通过配置中心评审管理流程更新密码和密钥文件

```
1. 系统经理/安全管理员发起更新请求
   ↓
2. 进入配置中心审批流程（按环境区分审批层级）
   ↓
3. 审批通过后，管理员更新db.pwd或sm4.key文件
   ↓
4. 配置中心验证文件格式和权限
   ↓
5. 配置中心保存文件并记录审计日志
   ↓
6. 应用启动时或定时同步下载最新文件
   ↓
7. 对比更新时间，仅下载变更的文件
   ↓
8. 应用使用新文件自动重新加载
```

---

## 项目代码结构

```
src/main/java/com/dbpwd/
├── model/
│   ├── PasswordFile.java           # 密码文件数据模型
│   ├── DatabaseEntry.java          # 数据库条目模型
│   ├── ENCFormat.java              # ENC格式解析
│   └── PasswordFormat.java         # 密码格式枚举
├── core/
│   ├── PasswordFileManager.java    # 密码文件管理器
│   ├── Sm4KeyLoader.java           # SM4密钥加载器
│   ├── FilePermissionChecker.java  # 文件权限检查
│   └── ConnectionTester.java       # 数据库连接测试工具
├── crypto/
│   ├── Sm4Cipher.java              # SM4国密算法
│   ├── PasswordEncryptor.java      # 密码加密器
│   └── PasswordDecryptor.java      # 密码解密器
└── cli/
    └── DBPasswordCLI.java          # 命令行工具（交互式）
```

---

## 配置中心集成

### 配置中心目录结构

配置中心按物理组/应用/环境三层目录结构保存文件：

```
/databases/
├── 物理组A/                       # 物理组织分组
├── └── 财务系统/                   # 系统名称
│   └── 应用名称/                  # 应用名称
│       ├── prod/                  # 生产环境
│       │   ├── db.pwd            # 密码文件
│       │   └── sm4.key           # SM4密钥文件
│       ├── test/                  # 测试环境
│       │   ├── db.pwd
│       │   └── sm4.key
│       ├── dev/                   # 开发环境
│       │   ├── db.pwd
│       │   └── sm4.key
│       └── uat/                   # UAT环境
│           ├── db.pwd
│           └── sm4.key
├── 物理组B/
│   └── 支付系统/
│       └── 支付应用/
│           ├── prod/
│           │   ├── db.pwd
│           │   └── sm4.key
│           └── test/
│               ├── db.pwd
│               └── sm4.key
```

#### 完整路径示例

```
/path: /物理组-财务系统/支付应用/prod/db.pwd
/path: /物理组-财务系统/支付应用/test/sm4.key
/path: /支付中心/订单服务/uat/db.pwd
```

### 配置中心配置

```yaml
# 配置中心配置
config-center:
  url: http://config-center.example.com
  api-key: your-api-token
  environment: ${APP_ENV:prod}      # 应用环境标识
  base-path: /databases            # 配置中心基础路径
```

### 配置下载流程（独立同步工具）

```
┌─────────────────────────────────────────────────────────────┐
│                    同步工具 (Sync Tool)                      │
│  （独立进程，定时任务或手动触发）                             │
└─────────────────────────────────────────────────────────────┘
                            │
                            ↓
1. 从配置中心获取当前环境的文件（根据environment参数）
   ↓
2. 对比本地文件更新时间（lastModified字段）
   - 如果本地文件不存在或配置中心更新时间更新：下载文件
   - 如果本地文件更新时间较新：跳过下载，记录警告
   ↓
3. 验证下载文件的完整性，设置为600权限
   ↓
4. 原子性替换本地文件
   ↓
5. 记录同步审计日志

┌─────────────────────────────────────────────────────────────┐
│                   应用程序 (Application)                     │
│  （启动时只读取本地文件，与配置中心完全解耦）                  │
└─────────────────────────────────────────────────────────────┘
                            │
                            ↓
应用启动
   ↓
1. 验证本地文件存在和权限（600）
   ↓
2. 加载密码文件（JSON格式）
   ↓
3. 加载密钥文件（sm4.key）
   ↓
4. 初始化密码服务
   ↓
5. 连接池使用密码服务获取数据库密码
```

### 本地文件路径

配置中心文件下载到本地固定路径：

```
~/.dbpwd/
├── db.pwd          # 密码文件（权限600）
└── sm4.key         # SM4密钥文件（权限600）
```

### 评审管理流程

每个环境独立的评审管理流程：

```
1. 系统经理/安全管理员发起密码或密钥更新请求
   ↓
2. 进入配置中心的审批流程
   - 生产环境：需要多层级审批（部门主管→安全管理员→平台审批）
   - 测试环境：需要部门主管审批
   - 开发环境：自动审批或快速审批
   ↓
3. 审批通过后，文件上传到配置中心（自动设置文件权限600）
   ↓
4. 配置中心记录更新时间和审计日志
   ↓
5. 应用下次启动或定时同步时下载最新文件
```

---

## 命令行工具

### 功能模块

- 从配置中心下载密钥或密码文件
- 加密/解密密码
- 数据库连接测试
- 数据库配置管理（添加/更新/删除/列表）
- 配置中心同步和对比

### 工具功能模块结构

```
dbpwd-cli/
├── download/           # 下载功能
│   ├── downloadKey()    # 下载sm4.key
│   ├── downloadPwd()    # 下载db.pwd
│   └── compare()        # 对比更新时间
├── encrypt/            # 加密功能
│   ├── encryptPwd()     # 加密单个密码
│   ├── encryptFile()    # 加密密码文件
│   └── autoUpdate()     # 自动更新密文
├── decrypt/            # 解密功能
│   ├── decryptPwd()     # 解密单个密码
│   ├── decryptFile()    # 解密密码文件
│   └── decryptDb()      # 解密指定数据库密码
├── test/               # 测试功能
│   ├── testConnection() # 测试单个连接
│   ├── testAll()        # 测试所有连接
│   └── validateFile()   # 验证文件格式
└── admin/               # 管理功能
    ├── addDb()          # 添加数据库
    ├── updateDb()       # 更新数据库
    ├── deleteDb()       # 删除数据库
    └── listDb()         # 列出所有数据库
```

### 命令行接口设计

```bash
# dbpwd-cli --help
数据库密码管理命令行工具 v1.0

用法:
  dbpwd-cli [command] [options]

命令:
  download    下载密钥或密码文件
  encrypt     加密密码
  decrypt     解密密码
  test        测试数据库连接
  admin       管理数据库配置
  version     显示版本信息

# dbpwd-cli download --help
下载密钥或密码文件

用法:
  dbpwd-cli download [key|pwd] [options]

选项:
  -v, --verify    下载后验证文件完整性
  -o, --output    指定输出路径（默认: ~/.dbpwd/）
  --compare       下载后与本地文件对比更新时间

示例:
  dbpwd-cli download key          # 下载sm4.key
  dbpwd-cli download pwd --compare # 下载db.pwd并对比
  dbpwd-cli download key -v       # 下载并验证密钥

# dbpwd-cli encrypt --help
加密密码

用法:
  dbpwd-cli encrypt [password] [options]

选项:
  -f, --file      从文件读取密码
  -d, --db-id     为指定数据库ID加密
  --interactive   交互式输入密码（隐藏输入）

示例:
  dbpwd-cli encrypt "myPassword"           # 直接加密
  dbpwd-cli encrypt -d db001                  # 为db001更新密码
  dbpwd-cli encrypt --interactive             # 交互式加密

# dbpwd-cli test --help
测试数据库连接

用法:
  dbpwd-cli test [db-id|all] [options]

选项:
  -v, --verbose   显示详细信息
  --timeout       连接超时时间（秒，默认30）

示例:
  dbpwd-cli test db001              # 测试db001连接
  dbpwd-cli test all                    # 测试所有连接
  dbpwd-cli test db001 --verbose       # 详细测试信息

# dbpwd-cli admin --help
管理数据库配置

用法:
  dbpwd-cli admin [action] [options]

动作:
  add         添加数据库
  update      更新数据库
  delete      删除数据库
  list        列出所有数据库

示例:
  dbpwd-cli admin add                  # 交互式添加
  dbpwd-cli admin update db001         # 更新db001
  dbpwd-cli admin delete db001         # 删除db001
  dbpwd-cli admin list                 # 列出所有
```

### 自动下载和对比功能

**实现逻辑**:
```java
public class ConfigCenterDownloader {

    public boolean downloadWithCompare(String sourceUrl, String localPath) throws Exception {
        // 1. 下载远端文件到临时位置
        Path tempFile = downloadToTemp(sourceUrl);
        String remoteJson = Files.readString(tempFile);

        // 2. 读取本地文件
        String localJson = null;
        Path localPathObj = Paths.get(localPath);
        if (Files.exists(localPathObj)) {
            localJson = Files.readString(localPathObj);
        }

        // 3. 对比更新时间
        if (localJson != null) {
            PasswordFile remoteFile = objectMapper.readValue(remoteJson, PasswordFile.class);
            PasswordFile localFile = objectMapper.readValue(localJson, PasswordFile.class);

            Instant remoteTime = parseInstant(remoteFile.getLastModified());
            Instant localTime = parseInstant(localFile.getLastModified());

            if (remoteTime != null && localTime != null && !remoteTime.isAfter(localTime)) {
                logger.info("本地文件已是最新 (更新时间: " + localTime);
                Files.deleteIfExists(tempFile);
                return false;
            }
        }

        // 4. 替换本地文件（原子性操作）
        Files.move(tempFile, localPathObj, StandardCopyOption.REPLACE_EXISTING);
        setFilePermissions(localPath, 0o600);

        logger.info("文件已更新: " + localPath);
        return true;
    }

    private void setFilePermissions(String path, int permissions) throws IOException {
        try {
            Files.setPosixFilePermissions(Paths.get(path),
                PosixFilePermissions.fromString(String.format("%o", permissions)));
        } catch (UnsupportedOperationException e) {
            logger.warning("无法设置文件权限: " + path);
        }
    }
}
```

### 密钥文件验证

**验证规则**:
```java
public class Sm4KeyValidator {

    public static boolean validate(String keyFilePath) throws InvalidKeyException {
        // 1. 检查文件权限
        if (!checkPermissions(keyFilePath, 0o600)) {
            throw new InvalidKeyException("密钥文件权限不正确，应为600");
        }

        // 2. 验证密钥格式（16字节hex编码，32字符）
        String keyContent = readKeyFile(keyFilePath).trim();
        if (keyContent.length() != 32 || !isHexString(keyContent)) {
            throw new InvalidKeyException("SM4密钥格式不正确，应为16字节hex编码");
        }

        // 3. 验证密钥长度（16字节）
        byte[] keyBytes = fromHexString(keyContent);
        if (keyBytes.length != 16) {
            throw new InvalidKeyException("SM4密钥长度不正确，应为16字节");
        }

        return true;
    }
}
```

### 使用示例

```bash
# 交互式模式
java -cp . com.dbpwd.cli.DBPasswordCLI

# 交互式命令
list     # 列出所有数据库
add      # 添加新数据库
update   # 更新数据库配置
delete   # 删除数据库配置
test     # 测试数据库连接
encrypt  # 加密密码
decrypt  # 解密密码
sync     # 从配置中心同步文件
info     # 显示配置信息
exit     # 退出程序
```

### 命令行参数

```bash
# 下载配置中心文件
java -cp . com.dbpwd.cli.DBPasswordCLI \
  --config-center-url=http://config-center.com \
  --config-center-api-key=xxx \
  --environment=prod \
  --command=sync

# 指定文件路径
java -cp . com.dbpwd.cli.DBPasswordCLI \
  --password-file=~/.dbpwd/db.pwd \
  --key-file=~/.dbpwd/sm4.key
```

---

## 配置属性说明

| 属性名 | 说明 | 默认值 |
|--------|------|--------|
| dbpwd.enabled | 是否启用dbpwd | false |
| dbpwd.password-file | 密码文件路径 | ~/.dbpwd/db.pwd |
| dbpwd.key-file | SM4密钥文件路径 | ~/.dbpwd/sm4.key |
| dbpwd.connection.timeout | 连接测试超时（毫秒） | 5000 |

**重要说明**：应用程序仅需配置本地文件路径，不依赖配置中心。配置中心相关的配置由独立同步工具使用。

### 应用配置（application.yml）

```yaml
# application.yml - 应用程序配置
dbpwd:
  enabled: true
  password-file: ~/.dbpwd/db.pwd
  key-file: ~/.dbpwd/sm4.key
  connection:
    timeout: 5000
```

### 同步工具配置（独立文件或参数）

```yaml
# sync-config.yml - 同步工具配置（独立使用）
config-center:
  url: http://config-center.example.com
  api-key: ${CONFIG_CENTER_API_KEY:your-api-key}
  environment: ${APP_ENV:prod}
  base-path: /databases

  # 路径层级配置
  organization: ${ORG_NAME:物理组-财务系统}      # 物理组织分组
  system: ${SYSTEM_NAME:财务系统}                 # 系统名称
  application: ${APP_NAME:支付应用}               # 应用名称

local:
  directory: ~/.dbpwd
  files:
    - db.pwd
    - sm4.key

sync:
  interval: 300000         # 5分钟（定时同步）
  verify-file-integrity: true
  set-permissions: 600
```

### 完整配置中心路径

同步工具会组合以下参数生成完整路径：

```
/config-center/base-path
  /organization         # 物理组织分组
  /system              # 系统名称
  /application         # 应用名称
  /environment         # 环境(prod/test/dev/uat)
  /filename            # 文件名(db.pwd/sm4.key)

示例: /databases/物理组-财务系统/支付应用/prod/db.pwd
```

---

## 安全机制设计

### SSH vs SM4密钥机制对比

| 对比项 | SSH私钥 | SM4密钥 | 建议 |
|-------|---------|---------|------|
| **密钥长度** | 2048/4096位（RSA/ECDSA） | 128位（SM4） | SM4满足对称加密需求 |
| **密钥类型** | 非对称（一对） | 对称（单个） | 符合使用场景 |
| **文件权限** | 600或400 | 600 | 保持一致 |
| **密钥保护** | 密码短语可选 | 密码文件独立存储 | 可选增加密码短语 |
| **密钥分发** | 公钥公开，私钥保密 | 密钥不通过网络传输 | 保持安全 |
| **密钥轮换** | 生成新密钥对 | 生成新密钥并重新加密 | 记录轮换历史 |
| **备份策略** | 安全离线备份 | 安全离线备份 | 多级备份 |
| **使用审计** | 日志记录公钥使用 | 日志记录解密操作 | 全程审计 |

### 安全增强建议

#### 7.2.1 密钥保护机制增强

**1. 密钥密码短语（可选）**

```java
/**
 * 密钥密码短语保护
 *
 * sm4.key 支持密码短语保护
 * ~/.dbpwd/sm4.key (加密存储，使用密码短语解锁)
 * ~/.dbpwd/sm4.key.passphrase (可选，文件存储密码短语)
 */
public class Sm4KeyPassphraseProtection {

    /**
     * 使用密码短语加密密钥
     *
     * @param keyBytes SM4密钥（16字节）
     * @param passphrase 密码短语
     * @return 加密后的密钥
     */
    public static byte[] encryptKey(byte[] keyBytes, String passphrase) {
        // 使用PBKDF2派生密钥，密码短语作为盐
        PBEKeySpec keySpec = new PBEKeySpec(
            passphrase.toCharArray(),
            deriveSalt(passphrase),
            10000,
            keyBytes.length * 8);

        SecretKey secretKey = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(keySpec);

        byte[] IV = new byte[12];
        SecureRandom random = new SecureRandom();
        random.nextBytes(IV);

        GCMParameterSpec gcmSpec = new GCMParameterSpec(IV, 16 * 8);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

        return cipher.doFinal(keyBytes);
    }

    /**
     * 使用密码短语解密密钥
     *
     * @param encryptedKey 加密的密钥
     * @param passphrase 密码短语
     * @return 解密后的密钥
     */
    public static byte[] decryptKey(byte[] encryptedKey, String passphrase) {
        // 使用PBKDF2派生密钥，密码短语作为盐
        PBEKeySpec keySpec = new PBEKeySpec(
            passphrase.toCharArray(),
            deriveSalt(passphrase),
            10000,
            encryptedKey.length * 8);

        SecretKey secretKey = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(keySpec);

        byte[] IV = Arrays.copyOfRange(encryptedKey, 0, 12);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(IV, 16 * 8);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

        return cipher.doFinal(encryptedKey);
    }
}
```

**2. 密钥拆分（Shamir秘密共享）**

```java
/**
 * Shamir秘密共享（密钥拆分）
 *
 * 将SM4密钥拆分为N份，需要M份才能重构
 * ~/.dbpwd/sm4.key.shard1 (管理员1持有)
 * ~/.dbpwd/sm4.key.shard2 (管理员2持有)
 * ~/.dbpwd/sm4.key.shard3 (备份持有)
 */
public class ShamirSecretSharing {

    private static final int DEFAULT_SHARES = 3;
    private static final int DEFAULT_THRESHOLD = 2;

    /**
     * 拆分SM4密钥
     *
     * @param keyBytes SM4密钥（16字节）
     * @param shares 生成密钥份额数
     * @param threshold 重建需要的最少份额数
     * @return 密钥份额列表
     */
    public static List<byte[]> splitKey(byte[] keyBytes, int shares, int threshold) {
        checkSplitParameters(shares, threshold, keyBytes.length);
        // 实现Shamir秘密共享算法
        // ...
        return new ArrayList<>();
    }

    /**
     * 重构SM4密钥
     *
     * @param shares 密钥份额列表
     * @param threshold 重建需要的最少份额数
     * @return 重构后的密钥
     */
    public static byte[] combineKey(List<byte[]> shares, int threshold) {
        if (shares.size() < threshold) {
            throw new IllegalArgumentException("密钥份额不足，至少需要 " + threshold + " 份");
        }
        // 实现Shamir秘密共享重构算法
        // ...
        return keyBytes;
    }

    private static void checkSplitParameters(int shares, int threshold, int keyLength) {
        if (shares < threshold || threshold < 1) {
            throw new IllegalArgumentException("拆分参数无效");
        }
        if (shares > keyLength * 8) {
            throw new IllegalArgumentException("密钥份额数不能超过密钥长度");
        }
    }
}
```

**3. 硬件安全模块（HSM）集成（未来）**

```java
/**
 * 硬件安全模块集成（HSM）
 *
 * 将SM4密钥存储在HSM中，密码文件使用HSM加密
 */
public interface HsmKeyProvider {

    /**
     * 生成HSM密钥句柄
     *
     * @param keyId 密钥ID
     * @return 密钥句柄
     */
    byte[] generateHsmKey(String keyId) throws HsmException;

    /**
     * 使用HSM加密密码
     *
     * @param keyId 密钥ID
     * @param plainPassword 明文密码
     * @return 加密后的密码
     */
    String encryptWithHsm(String keyId, String plainPassword) throws HsmException;

    /**
     * 使用HSM解密密码
     *
     * @param keyId 密钥ID
     * @param encryptedPassword 加密密码
     * @return 明文密码
     */
    String decryptWithHsm(String keyId, String encryptedPassword) throws HsmException;
}

/**
 * HSM密钥提供者抽象接口
 */
public class HsmKeyProviderImpl implements HsmKeyProvider {

    // 实际实现会调用HSM SDK（如AWS CloudHSM、Azure Key Vault等）
    @Override
    public byte[] generateHsmKey(String keyId) throws HsmException {
        // 实现HSM密钥生成
        return new byte[0];
    }

    @Override
    public String encryptWithHsm(String keyId, String plainPassword) throws HsmException {
        // 实现HSM加密
        return "";
    }

    @Override
    public String decryptWithHsm(String keyId, String encryptedPassword) throws HsmException {
        // 实现HSM解密
        return "";
    }
}
```

### 文件权限控制

**Linux/Unix权限设置**:
```bash
chmod 600 ~/.dbpwd/sm4.key
chmod 600 ~/.dbpwd/db.pwd
chown appuser:appgroup ~/.dbpwd/sm4.key
chown appuser:appgroup ~/.dbpwd/db.pwd
```

**容器部署权限设置**:
```yaml
volumes:
  - /host/.dbpwd:/app/.dbpwd
command: >
  sh -c "chmod 600 /app/.dbpwd/* && ls -la /app/.dbpwd/"
```

**NAS权限设置**:
```bash
chmod 600 /nas/volume/app/.dbpwd/sm4.key
chmod 600 /nas/volume/app/.dbpwd/db.pwd
```

### 密钥轮换流程

```
1. 系统经理生成新SM4密钥
   ↓
2. 使用新密钥重新加密所有密码
   ↓
3. 备份旧密钥（安全离线存储）
   ↓
4. 部署新密钥文件到生产环境
   ↓
5. 应用重新加载并验证
   ↓
6. 确认无误后安全删除旧密钥
```

---

## 技术栈

| 组件 | 技术选择 |
|------|---------|
| **JDK版本** | JDK 8+ |
| **核心语言** | Java 8+ |
| **加密库** | Bouncy Castle (1.69+) SM4实现 |
| **JSON库** | Jackson (2.12+) |
| **日志框架** | JDK Logging / SLF4J |
| **构建工具** | Maven 3.6+ |

---

## 连接池包装类（待实现）

### 包装类架构

```
ConnectionPoolWrapper (接口)
         ↑
         │ implements
         │
┌────────┴────────┐
│                 │
DruidWrap         HikariWrap    DBCP2Wrap
```

### 包装类接口定义

```java
/**
 * 连接池包装接口
 */
public interface ConnectionPoolWrapper {

    /**
     * 包装现有数据源，使用密码服务自动处理密码
     */
    DataSource wrap(DataSource originalDataSource, String dbId);

    /**
     * 创建新数据源并配置密码
     */
    DataSource create(String dbId, DataSourceConfig config);

    /**
     * 刷新数据源密码
     */
    void refreshPassword(DataSource dataSource, String dbId);

    /**
     * 验证数据源配置
     */
    boolean validate(DataSource dataSource, String dbId);
}

/**
 * 数据源配置
 */
public class DataSourceConfig {
    private int initialSize = 5;
    private int minIdle = 5;
    private int maxActive = 20;
    private long maxWait = 60000;
    private String validationQuery;
    private boolean testWhileIdle = true;
    private int timeBetweenEvictionRuns = 60000;

    // getters and setters
}
```

### Druid包装实现

```java
/**
 * Druid连接池包装实现
 */
public class DruidWrap implements ConnectionPoolWrapper {

    private static final Logger logger = Logger.getLogger(DruidWrap.class.getName());

    private final PasswordService passwordService;

    public DruidWrap(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    @Override
    public DataSource wrap(DataSource originalDataSource, String dbId) {
        if (!(originalDataSource instanceof DruidDataSource)) {
            throw new IllegalArgumentException("Expected DruidDataSource, got: " +
                originalDataSource.getClass().getName());
        }

        DruidDataSource druid = (DruidDataSource) originalDataSource;
        String password = passwordService.getDecryptedPassword(dbId);
        druid.setPassword(password);

        logger.info("已更新Druid数据源密码: " + dbId);
        return druid;
    }

    @Override
    public DataSource create(String dbId, DataSourceConfig config) {
        DatabaseEntry dbEntry = passwordService.getConfig(dbId);
        if (dbEntry == null) {
            throw new IllegalArgumentException("未找到数据库配置: " + dbId);
        }

        DruidDataSource druid = new DruidDataSource();
        druid.setUrl(dbEntry.getJdbcUrl());
        druid.setUsername(dbEntry.getUsername());
        druid.setPassword(passwordService.getDecryptedPassword(dbId));

        // 设置其他连接池属性
        druid.setInitialSize(config.getInitialSize());
        druid.setMinIdle(config.getMinIdle());
        druid.setMaxActive(config.getMaxActive());
        druid.setMaxWait(config.getMaxWait());

        if (config.getValidationQuery() != null) {
            druid.setValidationQuery(config.getValidationQuery());
        } else {
            // 根据数据库类型设置默认验证查询
            druid.setValidationQuery(getDefaultValidationQuery(dbEntry.getType()));
        }

        druid.setTestWhileIdle(config.isTestWhileIdle());
        druid.setTimeBetweenEvictionRuns(config.getTimeBetweenEvictionRuns());

        logger.info("已创建Druid数据源: " + dbId);
        return druid;
    }

    @Override
    public void refreshPassword(DataSource dataSource, String dbId) {
        if (!(dataSource instanceof DruidDataSource)) {
            throw new IllegalArgumentException("Expected DruidDataSource, got: " +
                dataSource.getClass().getName());
        }

        DruidDataSource druid = (DruidDataSource) dataSource;
        String newPassword = passwordService.getDecryptedPassword(dbId);
        druid.setPassword(newPassword);

        // 关闭活跃连接，强制重新连接
        closeConnections(druid);

        logger.info("已刷新Druid数据源密码: " + dbId);
    }

    @Override
    public boolean validate(DataSource dataSource, String dbId) {
        if (!(dataSource instanceof DruidDataSource)) {
            return false;
        }

        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            logger.warning("验证数据源失败: " + dbId + ", 错误: " + e.getMessage());
            return false;
        }
    }

    private void closeConnections(DruidDataSource dataSource) {
        try {
            dataSource.close();
        } catch (Exception e) {
            logger.warning("关闭连接失败: " + e.getMessage());
        }
    }

    private String getDefaultValidationQuery(String dbType) {
        if (dbType == null) {
            return "SELECT 1";
        }
        switch (dbType.toLowerCase()) {
            case "mysql":
                return "SELECT 1";
            case "postgresql":
            case "oracle":
            case "sqlserver":
                return "SELECT 1";
            case "db2":
                return "SELECT 1 FROM sysibm.sysdummy1";
            case "sybase":
                return "SELECT 1";
            case "h2":
                return "SELECT 1";
            default:
                return "SELECT 1";
        }
    }
}
```

### HikariCP包装实现

```java
/**
 * HikariCP连接池包装实现
 */
public class HikariWrap implements ConnectionPoolWrapper {

    private static final Logger logger = Logger.getLogger(HikariWrap.class.getName());

    private final PasswordService passwordService;

    public HikariWrap(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    @Override
    public DataSource wrap(DataSource originalDataSource, String dbId) {
        if (!(originalDataSource instanceof HikariDataSource)) {
            throw new IllegalArgumentException("Expected HikariDataSource, got: " +
                originalDataSource.getClass().getName());
        }

        // HikariCP不支持动态修改配置，需要重建数据源
        DatabaseEntry dbEntry = passwordService.getConfig(dbId);
        if (dbEntry == null) {
            throw new IllegalArgumentException("未找到数据库配置: " + dbId);
        }

        HikariDataSource original = (HikariDataSource) originalDataSource;
        HikariConfig config = original.getHikariConfigMXBean().getHikariConfig();

        // 更新密码
        config.setUsername(dbEntry.getUsername());
        config.setPassword(passwordService.getDecryptedPassword(dbId));

        logger.info("已更新HikariCP数据源密码: " + dbId);
        return new HikariDataSource(config);
    }

    @Override
    public DataSource create(String dbId, DataSourceConfig config) {
        DatabaseEntry dbEntry = passwordService.getConfig(dbId);
        if (dbEntry == null) {
            throw new IllegalArgumentException("未找到数据库配置: " + dbId);
        }

        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(dbEntry.getJdbcUrl());
        hikariConfig.setUsername(dbEntry.getUsername());
        hikariConfig.setPassword(passwordService.getDecryptedPassword(dbId));

        // 连接池属性
        hikariConfig.setMinimumIdle(config.getMinIdle());
        hikariConfig.setMaximumPoolSize(config.getMaxActive());
        hikariConfig.setConnectionTimeout((int) config.getMaxWait());
        hikariConfig.setIdleTimeout(config.getTimeBetweenEvictionRuns());
        hikariConfig.setAutoCommit(true);

        if (config.getValidationQuery() != null) {
            hikariConfig.setConnectionTestQuery(config.getValidationQuery());
        } else {
            // HikariCP默认没有验证查询，需要显式设置
            hikariConfig.setConnectionTestQuery("SELECT 1");
        }

        logger.info("已创建HikariCP数据源: " + dbId);
        return new HikariDataSource(hikariConfig);
    }

    @Override
    public void refreshPassword(DataSource dataSource, String dbId) {
        // HikariCP不支持动态修改密码，需要重建数据源
        DatabaseEntry dbEntry = passwordService.getConfig(dbId);
        if (dbEntry == null) {
            throw new IllegalArgumentException("未找到数据库配置: " + dbId);
        }

        if (!(dataSource instanceof HikariDataSource)) {
            throw new IllegalArgumentException("Expected HikariDataSource");
        }

        HikariDataSource hikari = (HikariDataSource) dataSource;
        HikariConfig config = hikari.getHikariConfigMXBean().getHikariConfig();
        config.setPassword(passwordService.getDecryptedPassword(dbId));

        // 关闭原数据源
        hikari.close();

        logger.info("已刷新HikariCP数据源密码（需重建数据源）: " + dbId);
    }

    @Override
    public boolean validate(DataSource dataSource, String dbId) {
        if (!(dataSource instanceof HikariDataSource)) {
            return false;
        }

        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(5);
        } catch (SQLException e) {
            logger.warning("验证数据源失败: " + dbId + ", 错误: " + e.getMessage());
            return false;
        }
    }
}
```

### 使用示例

```java
@Configuration
public class DataSourceConfig {

    @Autowired
    private PasswordService passwordService;

    @Bean
    public DruidWrap druidWrap() {
        return new DruidWrap(passwordService);
    }

    @Bean
    public HikariWrap hikariWrap() {
        return new HikariWrap(passwordService);
    }

    @Bean
    @Primary
    public DataSource primaryDataSource(DruidWrap druidWrap) {
        DataSourceConfig config = new DataSourceConfig();
        config.setInitialSize(5);
        config.setMinIdle(5);
        config.setMaxActive(20);
        config.setMaxWait(60000);
        config.setTestWhileIdle(true);
        config.setTimeBetweenEvictionRuns(60000);

        return druidWrap.create("db001", config);
    }

    @Bean
    public DataSource secondaryDataSource(HikariWrap hikariWrap) {
        DataSourceConfig config = new DataSourceConfig();
        config.setInitialSize(3);
        config.setMinIdle(2);
        config.setMaxActive(10);

        return hikariWrap.create("db002", config);
    }
}
```

---

## 框架集成（待实现）

### Spring Boot集成

**配置方式**:
```yaml
# application.yml
spring:
  datasource:
    druid:
      url: jdbc:mysql://192.168.1.100:3306/acdb_main
      username: app_user
      password: ${dbpwd:password:db001}  # 使用密码服务

  dbpwd:
    enabled: true
    password-file: /app/.dbpwd/db.pwd
    key-file: /app/.dbpwd/sm4.key
    connection:
      timeout: 5000
```

**自动配置类**:
```java
@Configuration
@EnableAutoConfiguration
@ConditionalOnProperty(name = "dbpwd.enabled", havingValue = "true")
public class DbPwdAutoConfiguration {

    @Bean
    public PasswordService passwordService(
            @Value("${dbpwd.password-file:~/.dbpwd/db.pwd}") String passwordFile,
            @Value("${dbpwd.key-file:~/.dbpwd/sm4.key}") String keyFile) {
        return new PasswordServiceImpl(passwordFile, keyFile);
    }

    @Bean
    @ConditionalOnClass(DruidDataSource.class)
    public DruidWrap druidWrap(PasswordService passwordService) {
        return new DruidWrap(passwordService);
    }

    @Bean
    @ConditionalOnClass(HikariDataSource.class)
    public HikariWrap hikariWrap(PasswordService passwordService) {
        return new HikariWrap(passwordService);
    }
}
```

### MyBatis集成

**配置方式**:
```yaml
# application.yml
mybatis:
  datasource:
    db-id: db001  # 指定数据库ID

spring:
  dbpwd:
    enabled: true
    password-file: /app/.dbpwd/db.pwd
    key-file: /app/.dbpwd/sm4.key
```

**数据源构建器**:
```java
@Configuration
public class MyBatisDataSourceConfig {

    private final PasswordService passwordService;

    @Autowired
    public MyBatisDataSourceConfig(PasswordService passwordService) {
        this.passwordService = passwordService;
    }

    @Bean
    @Primary
    public DataSource mybatisDataSource() {
        DatabaseEntry config = passwordService.getConfig("db001");
        DataSourceBuilder builder = DataSourceBuilder.create()
                .url(config.getJdbcUrl())
                .username(config.getUsername())
                .password(passwordService.getDecryptedPassword("db001"))
                .type(com.alibaba.druid.pool.DruidDataSource.class)
                .build();

        // 配置Druid连接池
        if (builder instanceof DruidDataSource) {
            DruidDataSource druid = (DruidDataSource) builder;
            druid.setInitialSize(5);
            druid.setMinIdle(5);
            druid.setMaxActive(20);
            druid.setMaxWait(60000);
            druid.setTestWhileIdle(true);
            druid.setTimeBetweenEvictionRuns(60000);
        }

        return builder;
    }
}
```

### PasswordService接口定义

```java
/**
 * 密码服务接口
 */
public interface PasswordService {

    /**
     * 获取解密后的密码
     *
     * @param dbId 数据库ID
     * @return 明文密码
     * @throws Exception 解密失败
     */
    String getDecryptedPassword(String dbId) throws Exception;

    /**
     * 获取数据库配置
     *
     * @param dbId 数据库ID
     * @return 数据库配置
     */
    DatabaseEntry getConfig(String dbId);

    /**
     * 包装数据源并替换密码
     *
     * @param dataSource 原始数据源
     * @param dbId 数据库ID
     * @return 包装后的数据源
     * @throws Exception 包装失败
     */
    DataSource wrapDataSource(DataSource dataSource, String dbId) throws Exception;

    /**
     * 创建数据源
     *
     * @param dbId 数据库ID
     * @param config 数据源配置
     * @return 数据源
     * @throws Exception 创建失败
     */
    DataSource createDataSource(String dbId, DataSourceConfig config) throws Exception;
}
```

### PasswordService实现

```java
/**
 * 密码服务实现
 */
@Service
public class PasswordServiceImpl implements PasswordService {

    private static final Logger logger = Logger.getLogger(PasswordServiceImpl.class.getName());

    private final String sm4Key;
    private final String passwordFilePath;
    private PasswordFileManager passwordFileManager;

    public PasswordServiceImpl(String passwordFilePath, String keyFile) throws Exception {
        this.passwordFilePath = passwordFilePath;

        // 加载SM4密钥
        Sm4KeyLoader keyLoader = new Sm4KeyLoader(keyFile);
        this.sm4Key = keyLoader.loadKey();

        // 加载密码文件
        this.passwordFileManager = PasswordFileManager.loadFromFile(passwordFilePath);

        logger.info("密码服务初始化完成，密钥文件: " + keyFile + "，密码文件: " + passwordFilePath);
    }

    @Override
    public String getDecryptedPassword(String dbId) throws Exception {
        DatabaseEntry entry = passwordFileManager.getById(dbId);
        if (entry == null) {
            throw new IllegalArgumentException("未找到数据库配置: " + dbId);
        }

        String password = entry.getPassword();
        if (ENCFormat.isEncrypted(password)) {
            return PasswordDecryptor.decrypt(password, sm4Key);
        }

        return password;
    }

    @Override
    public DatabaseEntry getConfig(String dbId) {
        return passwordFileManager.getById(dbId);
    }

    @Override
    public DataSource wrapDataSource(DataSource dataSource, String dbId) throws Exception {
        if (dataSource instanceof DruidDataSource) {
            DruidWrap wrap = new DruidWrap(this);
            return wrap.wrap(dataSource, dbId);
        } else if (dataSource instanceof HikariDataSource) {
            HikariWrap wrap = new HikariWrap(this);
            return wrap.wrap(dataSource, dbId);
        } else {
            throw new IllegalArgumentException("不支持的数据源类型: " +
                dataSource.getClass().getName());
        }
    }

    @Override
    public DataSource createDataSource(String dbId, DataSourceConfig config) throws Exception {
        DatabaseEntry entry = getConfig(dbId);
        String password = getDecryptedPassword(dbId);

        // 根据配置的type选择合适的连接池
        String poolType = config.getPoolType() != null ?
                              config.getPoolType() : "druid";

        if ("druid".equalsIgnoreCase(poolType)) {
            DruidDataSource dataSource = new DruidDataSource();
            dataSource.setUrl(entry.getJdbcUrl());
            dataSource.setUsername(entry.getUsername());
            dataSource.setPassword(password);
            return dataSource;
        } else if ("hikari".equalsIgnoreCase(poolType)) {
            HikariConfig configClass = new HikariConfig();
            configClass.setJdbcUrl(entry.getJdbcUrl());
            configClass.setUsername(entry.getUsername());
            configClass.setPassword(password);
            return new HikariDataSource(configClass);
        } else {
            throw new IllegalArgumentException("不支持的连接池类型: " + poolType);
        }
    }
}
```

### 使用示例

```java
@Service
public class UserService {

    @Autowired
    private PasswordService passwordService;

    public void connectToDatabase() {
        try {
            // 方式1: 获取解密后的密码
            String password = passwordService.getDecryptedPassword("db001");

            // 方式2: 获取完整数据库配置
            DatabaseEntry config = passwordService.getConfig("db001");
            String dbType = config.getType();
            String jdbcUrl = config.getJdbcUrl();
            String username = config.getUsername();

            // 方式3: 使用连接池包装
            DataSource dataSource = passwordService.createDataSource("db001", new DataSourceConfig());

            // 使用数据源...
            Connection conn = dataSource.getConnection();
            // ...

        } catch (Exception e) {
            throw new RuntimeException("数据库连接失败", e);
        }
    }

    public void updateDatabasePassword(String dbId, String newPassword) throws Exception {
        // 获取配置
        DatabaseEntry entry = passwordService.getConfig(dbId);
        if (entry == null) {
            throw new IllegalArgumentException("未找到数据库配置: " + dbId);
        }

        // 加密新密码
        String encryptedPassword = PasswordEncryptor.encryptSM4ENC(newPassword, sm4Key);
        entry.setPassword(encryptedPassword);
        entry.setEncrypted(true);
        entry.touchUpdateTime();

        // 保存
        passwordFileManager.updateDatabase(entry);
        passwordFileManager.save();

        logger.info("数据库密码已更新: " + dbId);
    }
}
```

---

## 安全机制设计

### 文件权限控制

**Linux/Unix权限设置**:
```bash
# 设置600权限
chmod 600 ~/.dbpwd/sm4.key
chmod 600 ~/.dbpwd/db.pwd

# 设置所有者
chown appuser:appgroup ~/.dbpwd/sm4.key
chown appuser:appgroup ~/.dbpwd/db.pwd

# 验证权限
ls -l ~/.dbpwd/
```

**容器部署权限设置**:
```yaml
# docker-compose.yml
volumes:
  - /host/.dbpwd:/app/.dbpwd
command: >
  sh -c "chmod 600 /app/.dbpwd/* && ls -la /app/.dbpwd/"
```

**NAS权限设置**:
```bash
chmod 600 /nas/volume/app/.dbpwd/sm4.key
chmod 600 /nas/volume/app/.dbpwd/db.pwd
```

### 密钥轮换流程

```
1. 系统经理生成新SM4密钥
   ↓
2. 使用新密钥重新加密所有密码
   ↓
3. 备份旧密钥（安全离线存储）
   ↓
4. 部署新密钥文件到生产环境
   ↓
5. 应用重新加载并验证
   ↓
6. 确认无误后安全删除旧密钥
```

### 审计日志

**审计字段**:
```json
{
  "timestamp": "2026-02-08T10:30:45Z",
  "action": "decrypt",
  "dbId": "db001",
  "userId": "appuser",
  "sourceIp": "192.168.1.50",
  "result": "success",
  "details": "Password decrypted for database connection"
}
```

**审计日志实现示例**:
```java
public class AuditLogger {

    private static final Logger auditLogger = Logger.getLogger("AUDIT");

    public static void log(String action, String dbId, String userId, boolean success, String details) {
        AuditEntry entry = new AuditEntry();
        entry.setTimestamp(Instant.now().toString());
        entry.setAction(action);
        entry.setDbId(dbId);
        entry.setUserId(userId);
        entry.setResult(success ? "success" : "failure");
        entry.setDetails(details);

        // 记录到日志文件（加密存储敏感信息）
        auditLogger.info(toJson(entry));

        // 存储到审计数据库（如有）
        // auditRepository.save(entry);
    }

    public static void logDecrypt(String dbId, String userId) {
        log("decrypt", dbId, userId, true, "Password decrypted");
    }

    public static void logKeyLoad(String userId, boolean success) {
        log("loadKey", "sm4.key", userId, success, "SM4密钥加载");
    }

    private static String toJson(AuditEntry entry) {
        // 使用JSON序列化
        return entry.toString();
    }

    public static class AuditEntry {
        private String timestamp;
        private String action;
        private String dbId;
        private String userId;
        private String result;
        private String details;
        // ...
    }
}
```

---

## 设计决策总结

| 决策点 | 选项 | 选择理由 |
|-------|------|---------|
| 密码文件格式 | JSON格式 | 可读性强，易扩展，工具友好 |
| 密码字段格式 | Druid ENC格式 | 兼容性强，可扩展其他算法 |
| 配置中心使用 | 存储实际文件+多环境管理 | 支持多环境分层，自带评审流程 |
| 应用与配置中心 | 完全解耦 | 应用只读本地文件，提高稳定性 |
| 同步机制 | 独立同步工具 | 不影响应用运行，可控的文件更新 |
| 密钥存储位置 | ~/.dbpwd/sm4.key | 标准化路径，权限可控 |
| 密钥保护 | 600权限，可选密码短语 | 参照SSH机制，增强安全 |
| 容器部署 | 外挂文件600权限 | 灵活部署，安全可控 |
| JDK版本 | JDK 8+ | 兼容现有系统 |

---

## 开发计划

### 开发阶段划分

整个项目分为5个Sprint，每个Sprint 2-3周，总计10-15周。

#### Sprint 1: 核心加密解密模块（2周）
**目标**: 实现SM4加密解密核心功能和密码文件管理

**任务清单**:
- [ ] 实现Sm4Cipher类（SM4加解密核心）
- [ ] 实现ENC格式解析和格式化
- [ ] 实现PasswordFormat枚举和自动检测
- [ ] 实现PasswordEncryptor密码加密器
- [ ] 实现PasswordDecryptor密码解密器
- [ ] 实现Sm4KeyLoader密钥加载器
- [ ] 实现FilePermissionChecker文件权限检查

**验收标准**:
- SM4加解密功能正常，密钥格式正确验证
- 支持ENC(SM4:密文)格式加密解密
- 自动检测密码格式（SM4/Druid/明文）
- 密钥文件权限验证600，警告机制正常

**测试要求**:
- SM4加解密单元测试覆盖率≥90%
- 密钥格式验证测试用例完整
- 文件权限检查测试覆盖Linux/Unix/Windows

---

#### Sprint 2: 密码文件管理和连接测试（2周）
**目标**: 实现JSON密码文件管理和数据库连接测试

**任务清单**:
- [ ] 实现DatabaseEntry数据模型（17个字段）
- [ ] 实现PasswordFile数据模型（版本和时间戳）
- [ ] 实现PasswordFileManager文件管理器
- [ ] 实现ConnectionTester连接测试工具
- [ ] 实现JSON文件读写和序列化
- [ ] 实现addDatabase/updateDatabase/removeDatabase方法
- [ ] 实现连接池驱动自动加载

**验收标准**:
- JSON密码文件读写正常，格式正确
- 支持数据库条目的增删改查
- 数据库连接测试功能正常
- 支持MySQL/PostgreSQL/Oracle/SQLServer等主流数据库
- 支持更新时间对比和文件合并

**测试要求**:
- JSON读写测试覆盖率≥85%
- 数据库连接测试覆盖8种数据库类型
- 连接超时和错误处理测试完整

---

#### Sprint 3: 命令行工具（2周）
**目标**: 实现交互式命令行工具和配置下载功能

**任务清单**:
- [ ] 实现DBPasswordCLI主程序（交互式模式）
- [ ] 实现list/add/update/delete/test/encrypt/decrypt/sync命令
- [ ] 实现interactiveMode交互式界面
- [ ] 实现commandMode命令行参数模式
- [ ] 实现password strength validation密码强度验证
- [ ] 实现ConfigCenterDownloader配置下载工具
- [ ] 实现downloadWithCompare自动下载对比

**验收标准**:
- 交互式命令行界面友好，命令响应正确
- 支持数据库配置的增删改查
- 支持密码加密解密操作
- 支持数据库连接测试
- 支持从配置中心下载和对比文件
- 密码强度验证功能正常

**测试要求**:
- 命令行交互测试覆盖率≥80%
- 各命令功能测试完整
- 配置下载和文件对比测试正常

---

#### Sprint 4: 框架集成（3周）
**目标**: 实现Spring Boot和MyBatis集成，连接池包装类

**任务清单**:
- [ ] 实现ConnectionPoolWrapper接口
- [ ] 实现DruidWrap包装类
- [ ] 实现HikariWrap包装类
- [ ] 实现PasswordService接口
- [ ] 实现PasswordServiceImpl服务实现
- [ ] 实现DbPwdAutoConfiguration自动配置类
- [ ] 实现MyBatis数据源配置
- [ ] 实现DataSourceConfig配置类

**验收标准**:
- Druid和HikariCP连接池包装正常
- Spring Boot自动配置正常
- MyBatis集成正常
- 支持多数据源配置
- 密码自动解密和应用

**测试要求**:
- 连接池包装类测试覆盖率≥85%
- Spring Boot集成测试完整
- MyBatis集成测试完整
- 多数据源测试正常

---

#### Sprint 5: 安全增强和文档完善（1-2周）
**目标**: 实现安全增强功能和完善系统文档

**任务清单**:
- [ ] 实现AuditLogger审计日志
- [ ] 实现密钥轮换流程
- [ ] 实现配置中心路径层级管理
- [ ] 完善系统部署文档
- [ ] 完善用户使用手册
- [ ] 完善开发者文档
- [ ] 性能测试和优化

**验收标准**:
- 审计日志功能正常
- 密钥轮换流程文档完整
- 配置中心三层路径管理正常
- 系统部署文档完整可用
- 用户手册和开发者文档完整

**测试要求**:
- 安全功能测试覆盖率≥90%
- 审计日志测试完整
- 密钥轮换测试正常
- 性能测试达标（解密耗时<100ms）

---

### 里程碑计划

| 里程碑 | 时间点 | 交付物 | 负责人 |
|--------|--------|--------|--------|
| **M1: 核心加密解密模块完成** | Sprint 1结束 | SM4加解密模块、密码格式解析 | 开发组 |
| **M2: 密码文件管理完成** | Sprint 2结束 | JSON文件管理、连接测试工具 | 开发组 |
| **M3: 命令行工具完成** | Sprint 3结束 | CLI工具、配置下载工具 | 开发组 |
| **M4: 框架集成完成** | Sprint 4结束 | Spring/MyBatis集成、连接池包装 | 开发组 |
| **M5: 系统发布** | Sprint 5结束 | 完整系统、文档、部署包 | 项目组 |

---

### 技术栈和版本要求

| 组件 | 版本要求 | 说明 |
|------|---------|------|
| **JDK** | 8+ | 最低JDK 8，推荐JDK 11+ |
| **Bouncy Castle** | 1.69+ | SM4加密算法支持 |
| **Jackson** | 2.12+ | JSON序列化和反序列化 |
| **Druid** | 1.2.x+ | Druid连接池（可选） |
| **HikariCP** | 4.x+ | HikariCP连接池（可选） |
| **Spring Boot** | 2.x+/3.x+ | Spring Boot框架（可选） |
| **MyBatis** | 3.x+ | MyBatis ORM框架（可选） |
| **SLF4J** | 1.7.x+ | 日志框架（可选） |
| **Maven** | 3.6+ | 构建工具 |
| **JUnit** | 4.x+/5.x+ | 单元测试框架 |

---

### 风险与应对

#### 技术风险

| 风险 | 等级 | 应对策略 |
|------|------|---------|
| SM4算法实现兼容性问题 | 高 | 使用成熟的Bouncy Castle库，进行充分测试 |
| 文件权限在Windows上不支持 | 中 | Windows跳过权限检查，记录警告 |
| 配置中心接口变动 | 中 | 抽象配置中心接口，降低耦合度 |
| 连接池版本兼容性 | 中 | 测试主流版本，提供版本兼容性说明 |

#### 开发风险

| 风险 | 等级 | 应对策略 |
|------|------|---------|
| 开发进度延期 | 中 | 预留20%的缓冲时间，关键路径优先 |
| 人员变动 | 低 | 代码规范化，文档完善，降低学习成本 |
| 需求变更 | 中 | 需求评审，变更控制，影响评估 |

---

### 测试覆盖率要求

| 模块 | 单元测试覆盖率 | 集成测试要求 |
|------|---------------|------------|
| crypto（加密解密） | ≥90% | - |
| core（核心模块） | ≥85% | 连接池驱动测试 |
| model（数据模型） | ≥80% | JSON序列化测试 |
| cli（命令行工具） | ≥80% | 命令功能测试 |
| **framework（框架集成）** | - | Spring/MyBatis集成测试 |

---

### 集成测试场景

| 场景 | 测试目标 | 测试方法 |
|------|---------|---------|
| **S1: 密码文件读写** | JSON文件读写正确性 | 创建、修改、删除数据库条目 |
| **S2: SM4加解密** | 加解密功能正确性 | 使用已知明文和密文验证 |
| **S3: 数据库连接** | 数据库连接正确性 | 连接测试8种数据库类型 |
| **S4: 配置下载** | 配置中心下载对比 | 模拟配置中心下载和对比流程 |
| **S5: 命令行工具** | CLI功能正确性 | 测试所有命令和参数 |
| **S6: Spring集成** | Spring自动配置 | Spring Boot启动测试 |
| **S7: 连接池包装** | 连接池包装正确性 | Druid和HikariCP包装测试 |
| **S8: 多数据源** | 多数据源支持 | 配置和使用多个数据源 |

---

### 发布检查清单

#### 代码检查
- [ ] 单元测试覆盖率达标（≥85%）
- [ ] 集成测试全部通过
- [ ] 代码审查完成
- [ ] 静态代码分析通过
- [ ] 无严重安全漏洞

#### 功能检查
- [ ] SM4加解密功能正常
- [ ] 密码文件读写正常
- [ ] 数据库连接测试正常
- [ ] 命令行工具功能正常
- [ ] 框架集成正常
- [ ] 连接池包装正常

#### 文档检查
- [ ] 设计文档更新
- [ ] 用户手册完整
- [ ] 开发者文档完整
- [ ] 部署文档完整
- [ ] API文档完整

#### 部署检查
- [ ] Docker镜像构建成功
- [ ] 容器部署测试通过
- [ ] 配置文件模板正确
- [ ] 密钥文件权限设置正确
- [ ] 日志配置正确

#### 安全检查
- [ ] 密钥文件权限600
- [ ] 密码文件权限600
- [ ] 审计日志功能正常
- [ ] 密钥轮换流程文档完整
- [ ] 勒索软件防护措施落实

---

> 文档版本: v3.0
> 最后更新: 2026-02-08
> 系统要求: JDK 8+
