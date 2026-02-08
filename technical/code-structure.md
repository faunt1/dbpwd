# 项目代码结构

## 目录结构

```
src/main/java/com/dbpwd/
├── model/                           # 数据模型
│   ├── PasswordFile.java           # 密码文件数据模型
│   ├── DatabaseEntry.java          # 数据库条目模型
│   └── PasswordFormat.java         # 密码格式枚举
├── core/                                 # 核心模块
│   ├── PasswordFileManager.java    # 密码文件管理器
│   ├── Sm4KeyLoader.java           # SM4密钥加载器
│   ├── FilePermissionChecker.java  # 文件权限检查
│   └── ConnectionValidator.java    # 数据库连接验证器
├── crypto/                               # 加密解密
│   ├── Sm4Cipher.java              # SM4国密算法
│   ├── ENCFormat.java              # ENC格式解析
│   ├── PasswordEncryptor.java      # 密码加密器
│   └── PasswordDecryptor.java      # 密码解密器
└── cli/                                  # 命令行工具
    └── DBPasswordCLI.java          # 命令行工具（交互式）
```

## 模块职责说明

### model/ - 数据模型层

| 类名 | 职责 | 说明 |
|------|------|------|
| PasswordFile | 密码文件完整模型，包含版本、修改时间和数据库数组 |
| DatabaseEntry | 数据库条目模型，包含完整的数据库连接信息 |
| ENCFormat | ENC格式解析器，解析 `ENC(算法:密文)` 格式 |
| PasswordFormat | 密码格式枚举，自动识别SM4/Druid/明文格式 |

### core/ - 核心模块

| 类名 | 职责 | 说明 |
|------|------|------|
| PasswordFileManager | 密码文件读写、增删改查、合并操作 |
| Sm4KeyLoader | SM4密钥文件加载，hex格式和600权限验证 |
| FilePermissionChecker | 文件权限检查，支持Linux/Unix系统 |
| ConnectionValidator | 数据库连接验证，支持多种数据库类型 |

### crypto/ - 加密解密层

| 类名 | 职责 | 说明 |
|------|------|------|
| Sm4Cipher | SM4加解密核心实现 |
| PasswordEncryptor | 统一加密入口，支持ENC格式 |
| PasswordDecryptor | 统一解密入口，自动格式检测 |

### cli/ - 命令行工具

| 类名 | 职责 | 说明 |
|------|------|------|
| DBPasswordCLI | 交互式和命令行两种使用模式，支持密码增删改查 |

## 包依赖关系

```
DruidConnectionPoolManager
    ├── DruidConnectionPool
    │   └── DruidDataSource (Druid提供)
    └── DruidPasswordCallback
        └── 加密算法 (SM4/AES)

PasswordEncryptor
    └── PasswordDecryptor
        └── PasswordFormat
        └── ENCFormat
```

## 设计模式

### 模块模式策略

| 模式 | 说明 | 应用场景 |
|------|------|----------|
| 工厂简化 | `Sm4Cipher` 作为唯一实现 | 轻量级应用 |
| 策略模式 | `ConnectionPoolWrapper` 接口支持多种连接池 | 企业级应用 |
| 服务模式 | `PasswordService` 统一密码服务 | 微服务架构 |

### 数据流

```
用户请求
    ↓
PasswordService.getDecryptedPassword(dbId)
    ↓
PasswordFileManager.getById(dbId)
    ↓
DatabaseEntry.getPassword()
    ↓
PasswordDecryptor.decrypt(password, sm4Key)
    ↓
明文密码
```

## 相关文档

- [SM4加密详解](sm4-encryption.md) - 加密解密层说明
- [连接池包装](connection-pool.md) - 核心模块说明
- [密码文件格式](password-format.md) - 数据模型说明
