# Druid Database Password Manager 设计文档

## 项目目标

实现一个基于 SM4 加密的数据库密码管理系统，支持配置中心和本地文件的双路访问。

## 密码文件格式规范

### 本地密码文件（三字段格式）
- **后缀**：`.600`
- **格式**：`数据库:用户名:加密密码`
- **组织方式**：按项目维度，单个文件多行，每行对应一个连接
- **示例**：
  ```
  mysql_prod:root:[16]r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=
  mysql_prod:readonly:[16]aBcDeFgHiJkLmNoPqRsTuVwXyZ012345=
  oracle_test:admin:[16]XYZ123456789abcdef0123456789=
  ```

### 配置中心密码文件（两字段格式）
- **格式**：`username:encryptedPassword`
- **组织方式**：按数据库用户维度管理，每个文件只保存单个数据库+用户名+密码
- **示例**：
  ```
  root:[16]r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=
  readonly:[16]aBcDeFgHiJkLmNoPqRsTuVwXyZ012345=
  admin:[16]XYZ123456789abcdef0123456789=
  ```

### 密码匹配优先级
当需要获取密码时：
1. **提供 database 和 username**：优先匹配本地文件的三字段格式（`database:username`），回退到配置中心的单字段格式（`username`）
2. **仅提供 username**：从配置中心或本地文件的单字段格式获取

---

## 功能模块

### 目录1：配置中心通讯 ✅
- 功能：负责和配置中心通讯，将配置中心的 uid/pwd 下载后保存在本地一个 600 的文件中
- 本地文件格式：每行一个 `数据库:用户名:加密密码` 形式
- 配置中心格式：每行一个 `用户名:加密密码` 形式
- 支持上传 uid/pwd 到配置中心
- 实现：`configcenter/` 目录
  - `PasswordStorageManager.java` - 密码存储管理器（支持三字段和两字段）
  - `ConfigCenterTemplateResolver.java` - 配置中心模板解析器
  - `DruidConfigUploader.java` - 配置中心上传下载工具
  - `ConfigCenterFallbackManager.java` - 回退管理器（配置中心优先，本地兜底）
  - `CompoundPasswordProvider.java` - 复合密码提供者

### 目录2：SM4 加密解密 ✅
- 功能：调用 SM4 进行加密、解密
- 实现：`crypto/SM4Cipher.java`, `DruidEncryptedPasswordCallback.java`

### 目录3：命令行界面 ✅
- 功能：提供命令行界面，输入相关信息（密码明文等）后，加密生成密文密码，并上传至配置中心
- 实现：`DruidConfigGeneratorCLI.java`

### 目录4：Java Web 框架适配 ✅
- 功能：为主要的 Java Web 框架（Spring 等），通过框架的机制或者相对通用的前置处理机制将原配置文件中的占位符解密为明文后调用真正的连接池处理
- 占位符替换策略：先配置中心下载，如果配置中心未能下载，用本地 600 文件兜底
- 实现：`spring/` 目录
  - `SpringPasswordPropertyPostProcessor.java` - Spring 属性后置处理器
  - `SpringPasswordConfiguration.java` - Spring 配置类
  - `DruidSpringDataSourceFactory.java` - Druid Spring 数据源工厂

### 目录5：连接池包装类 ✅
- 功能：为主要的连接池实现，形成包装类，包装类中只处理基于占位符的替换和解密（如果有占位符）
- 占位符替换策略：先配置中心下载，如果配置中心未能下载，用本地 600 文件兜底
- 实现：`DruidConnectionPool.java` 和 `universal/` 目录
  - `universal/PasswordPropertiesInterceptor.java` - 通用属性拦截器

---

## 配置中心目录结构

```
/druid/config/
├── templates/                    # 配置模板
│   ├── mysql-prod.properties
│   ├── oracle-test.properties
│   └── ...
└── database/                     # 密码文件
    ├── passwords.txt             # 统一密码文件
    └── ...                       # 或按数据库分文件
```

---

## 核心组件说明

### ConfigCenterFallbackManager（回退管理器）
负责实现"配置中心下载优先、本地文件兜底"策略：

#### 双缓存机制
- **单字段缓存**：`username -> encryptedPassword`（配置中心格式）
- **多字段缓存**：`database:username -> encryptedPassword`（本地文件格式）

#### 获取密码流程
1. 检查是否需要从配置中心刷新
2. 如果提供 `database` 和 `username`：
   - 优先从多字段缓存查找（`database:username`）
   - 回退到单字段缓存查找（`username`）
3. 如果仅提供 `uid`：
   - 优先从单字段缓存查找
   - 回退到多字段缓存查找

### PasswordPropertiesInterceptor（属性拦截器）
通用的 Properties 拦截器，支持：
1. 解析 `@config(uid)` 占位符并替换为加密密码
2. 自动解密加密密码为明文
3. 支持使用 `ConfigCenterFallbackManager` 作为密码提供者

### SpringPasswordPropertyPostProcessor
Spring 框架适配器：
1. 实现 `BeanFactoryPostProcessor` 接口
2. 在 Spring 上下文初始化前处理密码属性
3. 支持通过 Spring 配置属性配置参数
4. 自动处理配置中心回退

### PasswordStorageManager
密码文件管理器（支持三字段和两字段格式）：
- 支持本地 .600 文件的读写（三字段：`database:username:password`）
- 支持配置中心格式（两字段：`username:password`）
- 支持注释行（# 或 ; 开头）
- 自动根据文件后缀判断格式（.600 文件自动识别为三字段）

---

## 项目代码结构

```
src/main/java/com/dbpwd/
├── DruidConfigGenerator.java           # 配置生成器
├── DruidConfigGeneratorCLI.java        # 命令行工具
├── DruidConfigUploader.java            # 配置中心上传下载
├── DruidConnectionPool.java            # 连接池包装类（支持回退）
├── DruidConnectionPoolManager.java     # 多连接池管理器
├── DruidEncryptedPasswordCallback.java # 密码解密回调
├── JdbcUrlParser.java                 # JDBC URL解析
├── ConfigFileNameGenerator.java        # 文件名生成器
├── MultiDataSourceExample.java         # 多数据源示例
├── configcenter/                       # 配置中心相关
│   ├── PasswordStorageManager.java     # 密码存储（支持三/两字段）
│   ├── PlaceholderResolver.java        # 占位符解析
│   ├── ConfigCenterTemplateResolver.java # 模板解析
│   ├── ConfigCenterInitializer.java    # 配置中心初始化
│   ├── DruidConfigTemplateGenerator.java # 模板生成器
│   ├── ConfigCenterFallbackManager.java # 回退管理器
│   └── CompoundPasswordProvider.java   # 复合密码提供者
├── crypto/                             # 加密相关
│   └── Sm4Cipher.java                  # SM4国密算法
├── universal/                          # 通用拦截器
│   └── PasswordPropertiesInterceptor.java # 通用属性拦截器
├── spring/                             # Spring框架适配
│   ├── SpringPasswordPropertyPostProcessor.java # Spring处理器
│   ├── SpringPasswordConfiguration.java # Spring配置
│   └── DruidSpringDataSourceFactory.java # Druid数据源工厂

claude.md                               # 设计文档（本文档）
```

---

## 使用示例

### 方式1：直接使用连接池包装类
```java
// 使用配置中心 + 本地文件兜底
ConfigCenterFallbackManager fallbackManager = new ConfigCenterFallbackManager(
    "http://config-center.example.com",
    "your-api-key",
    "/druid/config",
    "/path/to/project.600"
);

DruidConnectionPool pool = new DruidConnectionPool(
    "/path/to/config-template.properties",
    fallbackManager
);
```

### 方式2：使用 Spring 配置
```java
// 在 @Configuration 类中
@Import(SpringPasswordConfiguration.class)

// 在 application.yml 中：
spring:
  dbpwd:
    config-center:
      url: http://config-center.example.com
      api-key: your-api-key
    local-password-file: /path/to/project.600
```

### 方式3：使用通用拦截器
```java
ConfigCenterFallbackManager fallbackManager = new ConfigCenterFallbackManager(
    "http://config-center.example.com",
    "your-api-key",
    "/druid/config",
    "/path/to/project.600"
);

Properties props = new Properties();
// 加载配置文件...
props.setProperty("password", "@config(mysql_prod:root)");  // 指定数据库和用户名

PasswordPropertiesInterceptor interceptor = new PasswordPropertiesInterceptor(fallbackManager);
Properties processedProps = interceptor.intercept(props);
// processedProps 中 password 已被替换为解密后的明文
```

---

## 配置属性说明

### ConfigCenterFallbackManager 构造参数
| 参数 | 说明 | 是否必填 |
|------|------|----------|
| configCenterUrl | 配置中心 URL | 否（仅本地模式时可为null） |
| apiKey | API 密钥 | 否（仅本地模式时可为null） |
| baseDirectory | 基础目录（如 /druid/config） | 否，默认 /druid/config |
| localPasswordFilePath | 本地 .600 文件路径 | 是 |
| syncInterval | 同步间隔（毫秒） | 否，默认5分钟 |

### Spring 配置属性
| 属性名 | 说明 | 默认值 |
|--------|------|--------|
| dbpwd.config-center.url | 配置中心 URL | - |
| dbpwd.api-key | API 密钥 | - |
| dbpwd.base-directory | 基础目录 | /druid/config |
| dbpwd.local-password-file | 本地 .600 文件路径 | - |
| dbpwd.sync-interval | 同步间隔（毫秒） | 300000 (5分钟) |
