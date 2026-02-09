# 项目代码结构

> 版本: v5.0 (多模块架构)
> 更新日期: 2026-02-09

## 多模块项目结构

项目采用Maven多模块架构，包含核心模块和三个Demo应用模块。

```
db-pwd/                                # 父项目 (打包类型: pom)
├── pom.xml                             # 父POM，管理全局配置和依赖版本
├── CLAUDE.md                           # 项目配置文档
├── README.md                           # 项目说明
├── overview.md                         # 技术概览
├── technical/                          # 技术文档目录
├── db-pwd-core/                        # 核心模块 (打包类型: jar)
│   ├── pom.xml                         # 核心模块POM
│   └── src/
│       ├── main/
│       │   ├── java/com/dbpwd/
│       │   │   ├── model/              # 数据模型
│       │   │   │   ├── PasswordFile.java
│       │   │   │   ├── DatabaseEntry.java
│       │   │   │   └── PasswordFormat.java
│       │   │   ├── core/               # 核心模块
│       │   │   │   ├── PasswordFileManager.java
│       │   │   │   ├── Sm4KeyLoader.java
│       │   │   │   ├── FilePermissionChecker.java
│       │   │   │   ├── ConnectionValidator.java
│       │   │   │   └── ConfigCenterDownloader.java
│       │   │   ├── crypto/             # 加密解密
│       │   │   │   ├── Sm4Cipher.java
│       │   │   │   ├── Sm4GCMCipher.java
│       │   │   │   ├── ENCFormat.java
│       │   │   │   ├── PasswordEncryptor.java
│       │   │   │   └── PasswordDecryptor.java
│       │   │   ├── framework/          # 框架集成
│       │   │   │   ├── DataSourceDecorator.java
│       │   │   │   └── DataSourceDecoratorFactory.java
│       │   │   ├── spring/             # Spring集成
│       │   │   │   ├── DbPwdAutoConfiguration.java
│       │   │   │   └── DbPwdProperties.java
│       │   │   └── cli/                # 命令行工具
│       │   │       └── DBPasswordCLI.java
│       │   ├── resources/
│       │   │   ├── META-INF/spring.factories  # Spring Boot自动配置
│       │   │   └── logback.xml               # 日志配置
│       │   └── assembly/              # 打包配置
│       │       ├── docs.xml              # 文档打包配置
│       │       ├── source.xml            # 源码打包配置
│       │       └── dist.xml              # 发布包配置
│       └── test/
│           └── java/com/dbpwd/          # 单元测试
├── db-pwd-simple-jdbc-demo/             # 简单JDBC Demo (打包类型: jar)
│   ├── pom.xml                         # Demo模块POM
│   └── src/main/java/com/dbpwd/demo/
│       ├── SimpleJdbcDemo.java         # 简单JDBC示例
│       └── SqlCipherDatabaseSetup.java # 数据库初始化工具
├── db-pwd-spring-boot-demo/             # Spring Boot Demo (打包类型: jar)
│   ├── pom.xml                         # Demo模块POM
│   └── src/main/java/com/dbpwd/demo/
│       └── SpringBootDemo.java         # Spring Boot Web示例
└── db-pwd-struts-demo/                 # Struts Demo (打包类型: war)
    ├── pom.xml                         # Demo模块POM
    └── src/main/java/com/dbpwd/demo/
        └── StrutsDemo.java             # Struts 1.x示例
```

## 模块职责说明

### db-pwd-core - 核心模块

核心模块包含所有数据库密码加密和管理的核心功能，可独立使用。

#### model/ - 数据模型层

| 类名 | 职责 | 说明 |
|------|------|------|
| PasswordFile | 密码文件完整模型，包含版本、修改时间和数据库数组 |
| DatabaseEntry | 数据库条目模型，包含完整的数据库连接信息和扩展属性 |
| PasswordFormat | 密码格式枚举，自动识别SM4/Druid/明文格式 |

#### core/ - 核心模块

| 类名 | 职责 | 说明 |
|------|------|------|
| PasswordFileManager | 密码文件读写、增删改查、合并操作 |
| Sm4KeyLoader | SM4密钥文件加载，hex格式和600权限验证 |
| FilePermissionChecker | 文件权限检查，支持Linux/Unix系统 |
| ConnectionValidator | 数据库连接验证，支持多种数据库类型 |
| ConfigCenterDownloader | 配置中心文件下载和对比工具 |

#### crypto/ - 加密解密层

| 类名 | 职责 | 说明 |
|------|------|------|
| Sm4Cipher | SM4-ECB模式加解密实现 |
| Sm4GCMCipher | SM4-GCM认证加密模式实现 |
| ENCFormat | ENC格式解析器，解析 `ENC(算法:密文)` 格式 |
| PasswordEncryptor | 统一加密入口，支持ENC格式 |
| PasswordDecryptor | 统一解密入口，自动格式检测 |

#### framework/ - 框架集成

| 类名 | 职责 | 说明 |
|------|------|------|
| DataSourceDecorator | 数据源装饰器，在getConnection()时解密密码 |
| DataSourceDecoratorFactory | 装饰器工厂，支持Druid/HikariCP/DBCP2 |

#### spring/ - Spring集成

| 类名 | 职责 | 说明 |
|------|------|------|
| DbPwdAutoConfiguration | Spring Boot自动配置类 |
| DbPwdProperties | Spring配置属性类 |

#### cli/ - 命令行工具

| 类名 | 职责 | 说明 |
|------|------|------|
| DBPasswordCLI | 交互式和命令行两种使用模式，支持密码增删改查 |

### db-pwd-simple-jdbc-demo - 简单JDBC Demo

使用纯JDBC的简单示例应用，演示如何使用SM4加密和密码管理功能。

| 类名 | 职责 | 说明 |
|------|------|------|
| SimpleJdbcDemo | 演示SM4加密和JDBC连接的完整流程 |
| SqlCipherDatabaseSetup | SQLite数据库初始化和测试数据创建 |

### db-pwd-spring-boot-demo - Spring Boot Demo

使用Spring Boot的Web应用示例，提供REST API端点。

| 类名 | 职责 | 说明 |
|------|------|------|
| SpringBootDemo | Spring Boot主应用类 |
| DemoController | REST API控制器，提供用户和订单查询功能 |

### db-pwd-struts-demo - Struts Demo

使用Struts 1.x的传统Web应用示例。

| 类名 | 职责 | 说明 |
|------|------|------|
| StrutsDemo | Struts 1.x应用，包含Action模式的数据访问 |
| DbConfig | 数据库配置工具类 |
| DataAccessUtil | 数据访问工具类 |

## 模块依赖关系

```
db-pwd (父POM)
├── db-pwd-core (核心模块)
│   ├── Bouncy Castle (SM4加密)
│   ├── Jackson (JSON处理)
│   ├── Druid/HikariCP/DBCP2 (连接池)
│   └── Spring Boot Auto-Configuration (可选)
├── db-pwd-simple-jdbc-demo
│   └── db-pwd-core (依赖)
│   └── SQLite JDBC Driver
├── db-pwd-spring-boot-demo
│   └── db-pwd-core (依赖)
│   └── Spring Boot Starter Web
│   └── SQLite JDBC Driver
└── db-pwd-struts-demo
    └── db-pwd-core (依赖)
    └── Struts 1.x
    └── SQLite JDBC Driver
```

## 构建命令

### 清理和安装所有模块

```bash
mvn clean install
```

### 仅构建核心模块

```bash
cd db-pwd-core
mvn clean install
```

### 构建特定Demo模块

```bash
cd db-pwd-simple-jdbc-demo
mvn clean install
```

### 打包文档包

```bash
cd db-pwd-core
mvn package -Pdocs
```

### 打包源码包

```bash
cd db-pwd-core
mvn package -Psource
```

### 打包发布包

```bash
cd db-pwd-core
mvn package -Pdist
```

## 打包产物

### 核心模块打包产物

| 文件 | 说明 |
|------|------|
| db-pwd-core-1.0.0.jar | 核心模块JAR |
| db-pwd-core-1.0.0-jar-with-dependencies.jar | 带依赖的完整JAR |
| db-pwd-core-1.0.0-docs.zip | 文档包 |
| db-pwd-core-1.0.0-source.zip | 源码压缩包 |
| db-pwd-core-1.0.0-source.tar.gz | 源码TAR包 |
| db-pwd-core-1.0.0-dist.zip | 发布包 |
| db-pwd-core-1.0.0-dist.tar.gz | 发布TAR包 |

### Demo模块打包产物

| 模块 | 打包方式 | 可执行文件 |
|------|---------|-----------|
| db-pwd-simple-jdbc-demo | jar | SimpleJdbcDemo |
| db-pwd-spring-boot-demo | jar | SpringBootDemo |
| db-pwd-struts-demo | war | StrutsDemo |

## 设计模式

### 多模块架构模式

| 模式 | 说明 | 应用场景 |
|------|------|----------|
| 核心模块 | db-pwd-core提供可复用的核心功能 | 作为库使用 |
| Demo模块 | 独立的示例应用，演示集成方式 | 教学和验证 |

### 装饰器模式

| 类名 | 职责 | 说明 |
|------|------|------|
| DataSourceDecorator | 装饰器接口实现，拦截密码设置 | 在getConnection()时解密密码 |

### 工厂模式

| 类名 | 职责 | 说明 |
|------|------|------|
| DataSourceDecoratorFactory | 根据配置创建装饰后数据源 | 支持多种连接池类型 |

### 自动配置模式

| 类名 | 职责 | 说明 |
|------|------|------|
| DbPwdAutoConfiguration | Spring Boot自动配置 | 无需手动配置即可使用 |

## 数据流

```
应用启动
    ↓
DbPwdAutoConfiguration 配置生效
    ↓
Spring容器创建DataSource
    ↓
DataSourceDecoratorFactory 装饰DataSource
    ↓
数据库连接请求 getConnection()
    ↓
DataSourceDecorator 拦截请求
    ↓
从DatabaseEntry读取加密密码
    ↓
使用Sm4KeyLoader加载SM4密钥
    ↓
PasswordDecryptor解密密码
    ↓
设置到真实DataSource
    ↓
建立数据库连接
```

## 相关文档

- [技术概览](../overview.md) - 技术栈和架构概述
- [SM4加密详解](sm4-encryption.md) - 加密解密层说明
- [密码文件格式](password-format.md) - 数据模型说明
- [框架集成](framework-integration.md) - Spring/MyBatis集成指南
- [Spring Boot集成](spring-boot-integration.md) - Spring Boot详细配置
