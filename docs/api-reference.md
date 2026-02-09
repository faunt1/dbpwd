# API 参考文档

> 文档版本: v1.0
> 最后更新: 2026-02-10

## 目录

1. [概述](#概述)
2. [加密解密API](#加密解密api)
3. [密码文件管理API](#密码文件管理api)
4. [数据库连接API](#数据库连接api)
5. [配置中心API](#配置中心api)
6. [框架集成API](#框架集成api)
7. [异常处理](#异常处理)
8. [代码示例](#代码示例)

---

## 概述

### API分类

Database Password Manager 提供以下API：

| API类型 | 包路径 | 主要功能 |
|---------|--------|---------|
| 加密解密 | `com.dbpwd.crypto` | SM4加解密、密码格式处理 |
| 密码文件管理 | `com.dbpwd.model`、`com.dbpwd.core` | JSON文件读写、数据库条目管理 |
| 数据库连接 | `com.dbpwd.core` | 连接测试、连接池驱动加载 |
| 配置中心 | `com.dbpwd.core` | 配置下载、文件对比 |
| 框架集成 | `com.dbpwd.framework` | Spring Boot自动配置、连接池装饰器 |

### Maven依赖

```xml
<dependency>
  <groupId>com.dbpwd</groupId>
  <artifactId>db-pwd-core</artifactId>
  <version>1.0.0</version>
</dependency>
```

### Java版本要求

- 最低版本：JDK 8
- 推荐版本：JDK 11+

---

## 加密解密API

### Sm4Cipher

SM4加密算法核心类。

#### 构造方法

| 方法 | 说明 |
|------|------|
| `Sm4Cipher(byte[] key)` | 使用指定密钥初始化 |

#### 主要方法

##### encrypt

加密数据：

```java
public byte[] encrypt(byte[] data) throws CryptoException
```

**参数**：
- `data` - 待加密数据（必须是16字节对齐）

**返回**：
- 加密后的数据

**抛出**：
- `CryptoException` - 加密失败时抛出

##### decrypt

解密数据：

```java
public byte[] decrypt(byte[] encryptedData) throws CryptoException
```

**参数**：
- `encryptedData` - 待解密数据

**返回**：
- 解密后的数据

**抛出**：
- `CryptoException` - 解密失败时抛出

#### 使用示例

```java
import com.dbpwd.crypto.Sm4Cipher;
import com.dbpwd.crypto.CryptoException;

// 创建密钥（16字节）
byte[] key = new byte[] {
  0x01, 0x23, 0x45, 0x67, (byte)0x89, (byte)0xAB, (byte)0xCD, (byte)0xEF,
  (byte)0xFE, (byte)0xDC, (byte)0xBA, (byte)0x98, 0x76, 0x54, 0x32, 0x10
};

// 创建加密器
Sm4Cipher cipher = new Sm4Cipher(key);

// 加密
byte[] plainText = "Hello, World!".getBytes(StandardCharsets.UTF_8);
byte[] encrypted = cipher.encrypt(plainText);

// 解密
byte[] decrypted = cipher.decrypt(encrypted);
```

---

### Sm4GCMCipher

SM4 GCM认证加密模式类（推荐使用）。

#### 构造方法

| 方法 | 说明 |
|------|------|
| `Sm4GCMCipher(byte[] key)` | 使用指定密钥初始化，自动生成IV |
| `Sm4GCMCipher(byte[] key, byte[] iv)` | 使用指定密钥和IV初始化 |

#### 主要方法

##### encrypt

加密数据：

```java
public byte[] encrypt(byte[] data, byte[] aad)
```

**参数**：
- `data` - 待加密数据
- `aad` - 附加认证数据可为null

**返回**：
- 加密后的数据（包含IV和认证标签）

##### decrypt

解密数据：

```java
public byte[] decrypt(byte[] encryptedData, byte[] aad)
```

**参数**：
- `encryptedData` - 待解密数据
- `aad` - 附加认证数据（需与加密时一致）

**返回**：
- 解密后的数据

**抛出**：
- `CryptoException` - 解密失败或认证失败时抛出

#### 使用示例

```java
import com.dbpwd.crypto.Sm4GCMCipher;

// 创建密钥
byte[] key = new byte[16]; // 16字节密钥

// 创建GCM加密器
Sm4GCMCipher gcmCipher = new Sm4GCMCipher(key);

// 加密
byte[] data = "Secret Message".getBytes(StandardCharsets.UTF_8);
byte[] encrypted = gcmCipher.encrypt(data, null);

// 解密
byte[] decrypted = gcmCipher.decrypt(encrypted, null);
```

---

### ENCFormat

ENC格式工具类，用于处理 `ENC(算法:密文)` 格式。

#### 主要方法

##### formatPassword

格式化密码为ENC格式：

```java
public static String formatPassword(String algorithm, String encryptedPassword)
```

**参数**：
- `algorithm` - 加密算法（如SM4、SM4-GCM）
- `encryptedPassword` - 加密后的密码

**返回**：
- 格式化后的密码字符串

##### parsePassword

解析ENC格式密码：

```java
public static ParsedPassword parsePassword(String password)
```

**参数**：
- `password` - 密码字符串

**返回**：
- `ParsedPassword` 对象，包含算法和密文

**抛出**：
- `IllegalArgumentException` - 格式错误时抛出

#### 使用示例

```java
import com.dbpwd.crypto.ENCFormat;
import com.dbpwd.crypto.ENCFormat.ParsedPassword;

// 格式化密码
String formatted = ENCFormat.formatPassword("SM4", "r7X9k3mP8nQ2vL5wY1zA");
// 结果: ENC(SM4:r7X9k3mP8nQ2vL5wY1zA)

// 解析密码
ParsedPassword parsed = ENCFormat.parsePassword("ENC(SM4:r7X9k3mP8nQ2vL5wY1zA)");
System.out.println("Algorithm: " + parsed.getAlgorithm());
System.out.println("Cipher: " + parsed.getEncryptedPassword());
```

---

### PasswordEncryptor

密码加密器，提供加密密码到ENC格式的功能。

#### 主要方法

##### encrypt

加密密码：

```java
public String encrypt(String plainPassword, byte[] sm4Key)
```

**参数**：
- `plainPassword` - 明文密码
- `sm4Key` - SM4加密密钥

**返回**：
- 加密后的密码（ENC格式）

##### encryptWithGCM

使用GCM模式加密密码：

```java
public String encryptWithGCM(String plainPassword, byte[] sm4Key)
```

**参数**：
- `plainPassword` - 明文密码
- `sm4Key` - SM4加密密钥

**返回**：
- 加密后的密码（ENC格式，使用SM4-GCM算法）

#### 使用示例

```java
import com.dbpwd.crypto.PasswordEncryptor;
import com.dbpwd.crypto.Sm4KeyLoader;

// 加载密钥
byte[] key = Sm4KeyLoader.loadKeyFromFile("~/.dbpwd/sm4.key");

// 创建加密器
PasswordEncryptor encryptor = new PasswordEncryptor();

// 加密密码（ECB模式）
String encrypted = encryptor.encrypt("my_password", key);
// 结果: ENC(SM4:r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=)

// 加密密码（GCM模式）
String encryptedGCM = encryptor.encryptWithGCM("my_password", key);
// 结果: ENC(SM4-GCM:abc123...)
```

---

### PasswordDecryptor

密码解密器，提供从ENC格式解密密码的功能。

#### 主要方法

##### decrypt

解密密码：

```java
public String decrypt(String encryptedPassword, byte[] sm4Key)
```

**参数**：
- `encryptedPassword` - 加密后的密码（ENC格式）
- `sm4Key` - SM4解密密钥

**返回**：
- 解密后的明文密码

**抛出**：
- `CryptoException` - 解密失败时抛出

#### 使用示例

```java
import com.dbpwd.crypto.PasswordDecryptor;
import com.dbpwd.crypto.Sm4KeyLoader;

// 加载密钥
byte[] key = Sm4KeyLoader.loadKeyFromFile("~/.dbpwd/sm4.key");

// 创建解密器
PasswordDecryptor decryptor = new PasswordDecryptor();

// 解密密码
String plain = decryptor.decrypt("ENC(SM4:r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=)", key);
System.out.println("Plain password: " + plain);
```

---

### Sm4KeyLoader

SM4密钥加载器，用于从文件加载密钥。

#### 主要方法

##### loadKeyFromFile

从文件加载密钥：

```java
public static byte[] loadKeyFromFile(String filePath) throws IOException
```

**参数**：
- `filePath` - 密钥文件路径

**返回**：
- 16字节的密钥

**抛出**：
- `IOException` - 文件读取失败时抛出
- `IllegalArgumentException` - 密钥格式错误时抛出

##### generateRandomKey

生成随机密钥：

```java
public static byte[] generateRandomKey()
```

**返回**：
- 16字节的随机密钥

#### 使用示例

```java
import com.dbpwd.crypto.Sm4KeyLoader;
import java.io.IOException;
import java.util.Base64;

// 从文件加载密钥
byte[] key = Sm4KeyLoader.loadKeyFromFile("~/.dbpwd/sm4.key");

// 生成随机密钥
byte[] newKey = Sm4KeyLoader.generateRandomKey();
System.out.println("Generated key: " + Base64.getEncoder().encodeToString(newKey));

// 保存密钥到文件
Files.write(Paths.get("~/.dbpwd/sm4.key"),
  Base64.getEncoder().encode(newKey));
```

---

## 密码文件管理API

### PasswordFile

密码文件数据模型。

#### 属性

| 属性 | 类型 | 说明 |
|------|------|------|
| version | String | 文件版本 |
| lastModified | Instant | 最后修改时间 |
| databases | List<DatabaseEntry> | 数据库条目列表 |

#### 主要方法

##### addDatabase

添加数据库条目：

```java
public void addDatabase(DatabaseEntry database)
```

##### updateDatabase

更新数据库条目：

```java
public void updateDatabase(DatabaseEntry database)
```

##### removeDatabase

删除数据库条目：

```java
public void removeDatabase(String databaseId)
```

##### getDatabaseById

根据ID获取数据库条目：

```java
public Optional<DatabaseEntry> getDatabaseById(String databaseId)
```

##### getDatabaseByName

根据名称获取数据库条目：

```java
public Optional<DatabaseEntry> getDatabaseByName(String databaseName)
```

#### 使用示例

```java
import com.dbpwd.model.PasswordFile;
import com.dbpwd.model.DatabaseEntry;
import java.time.Instant;

// 创建密码文件
PasswordFile passwordFile = new PasswordFile("1.0", Instant.now());

// 添加数据库
DatabaseEntry db = new DatabaseEntry(
  "db001",
  "主数据库",
  "mysql",
  "localhost",
  3306,
  "mydb",
  "jdbc:mysql://localhost:3306/mydb",
  "admin",
  "ENC(SM4:..., true
);
passwordFile.addDatabase(db);

// 查找数据库
Optional<DatabaseEntry> found = passwordFile.getDatabaseById("db001");
if (found.isPresent()) {
  System.out.println("Found: " + found.get().getName());
}

// 删除数据库
passwordFile.removeDatabase("db001");
```

---

### DatabaseEntry

数据库条目数据模型。

#### 构造方法

```java
public DatabaseEntry(
  String id,
  String name,
  String type,
  String host,
  int port,
  String database,
  String url,
  String username,
  String password,
  boolean encrypted
)
```

#### 属性

| 属性 | 类型 | 说明 |
|------|------|------|
| id | String | 数据库ID（唯一标识） |
| name | String | 数据库名称 |
| type | String | 数据库类型（mysql/postgresql/oracle等） |
| host | String | 数据库主机 |
| port | int | 数据库端口 |
| database | String | 数据库名称 |
| url | String | JDBC URL |
| username | String | 数据库用户名 |
| password | String | 数据库密码（加密或明文） |
| encrypted | boolean | 密码是否加密 |
| createdAt | Instant | 创建时间 |
| updatedAt | Instant | 更新时间 |
| properties | Map<String, String> | 额外属性 |

#### 使用示例

```java
import com.dbpwd.model.DatabaseEntry;
import java.time.Instant;

DatabaseEntry db = new DatabaseEntry(
  "db001",                      // id
  "主数据库",                    // name
  "mysql",                      // type
  "localhost",                  // host
  3306,                         // port
  "mydb",                       // database
  "jdbc:mysql://localhost:3306/mydb",  // url
  "admin",                      // username
  "ENC(SM4:...)",               // password
  true                          // encrypted
);

// 设置额外属性
db.addProperty("useSSL", "false");
db.addProperty("serverTimezone", "UTC");

// 访问属性
String useSSL = db.getProperty("useSSL");
```

---

### PasswordFileManager

密码文件管理器，提供文件的读写操作。

#### 主要方法

##### load

加载密码文件：

```java
public static PasswordFile load(String filePath) throws IOException
```

**参数**：
- `filePath` - 密码文件路径

**返回**：
- `PasswordFile` 对象

**抛出**：
- `IOException` - 文件读取失败时抛出

##### save

保存密码文件：

```java
public static void save(String filePath, PasswordFile passwordFile) throws IOException
```

**参数**：
- `filePath` - 密码文件路径
- `passwordFile` - 密码文件对象

**抛出**：
- `IOException` - 文件保存失败时抛出

##### merge

合并密码文件（更新时间较新的版本优先）：

```java
public static PasswordFile merge(PasswordFile file1, PasswordFile file2)
```

**参数**：
- `file1` - 第一个密码文件
- `file2` - 第二个密码文件

**返回**：
- 合并后的密码文件

#### 使用示例

```java
import com.dbpwd.core.PasswordFileManager;
import com.dbpwd.model.PasswordFile;
import java.io.IOException;

try {
  // 加载密码文件
  PasswordFile passwordFile = PasswordFileManager.load("~/.dbpwd/db.pwd");

  // 修改密码文件
  passwordFile.getDatabaseById("db001").ifPresent(db -> {
    db.setPassword("new_encrypted_password");
    db.setUpdatedAt(Instant.now());
  });

  // 保存密码文件
  PasswordFileManager.save("~/.dbpwd/db.pwd", passwordFile);

} catch (IOException e) {
  System.err.println("Failed to manage password file: " + e.getMessage());
}
```

---

## 数据库连接API

### ConnectionValidator

数据库连接验证器，用于测试数据库连接。

#### 主要方法

##### testConnection

测试数据库连接：

```java
public static ConnectionTestResult testConnection(
  String type,
  String host,
  int port,
  String database,
  String username,
  String password
)
```

**参数**：
- `type` - 数据库类型
- `host` - 数据库主机
- `port` - 数据库端口
- `database` - 数据库名称
- `username` - 数据库用户名
- `password` - 数据库密码

**返回**：
- `ConnectionTestResult` 对象，包含测试结果

##### testConnectionWithURL

使用URL测试数据库连接：

```java
public static ConnectionTestResult testConnection(
  String type,
  String url,
  String username,
  String password
)
```

##### testDatabaseEntry

测试DatabaseEntry的连接：

```java
public static ConnectionTestResult testDatabaseEntry(
  DatabaseEntry entry,
  Function<String, String> passwordDecryptor
)
```

**参数**：
- `entry` - 数据库条目
- `passwordDecryptor` - 密码解密函数

**返回**：
- `ConnectionTestResult` 对象

#### 使用示例

```java
import com.dbpwd.core.ConnectionValidator;
import com.dbpwd.core.ConnectionTestResult;

// 测试连接
ConnectionTestResult result = ConnectionValidator.testConnection(
  "mysql",
  "localhost",
  3306,
  "mydb",
  "admin",
  "password"
);

if (result.isSuccess()) {
  System.out.println("Connection successful!");
} else {
  System.out.println("Connection failed: " + result.getErrorMessage());
}

// 测试带加密密码的连接
ConnectionTestResult result2 = ConnectionValidator.testDatabaseEntry(
  entry,
  encryptedPassword -> {
    // 解密密码逻辑
    return decryptPassword(encryptedPassword);
  }
);
```

---

### ConnectionTestResult

数据库连接测试结果。

#### 属性

| 属性 | 类型 | 说明 |
|------|------|------|
| success | boolean | 测试是否成功 |
| errorMessage | String | 错误消息（失败时） |
| connectionTimeMs | long | 连接耗时（毫秒） |

#### 使用示例

```java
ConnectionTestResult result = ConnectionValidator.testConnection(...);

if (result.isSuccess()) {
  System.out.println("Success! Time: " + result.getConnectionTimeMs() + "ms");
} else {
  System.err.println("Failed: " + result.getErrorMessage());
}
```

---

## 配置中心API

### ConfigCenterDownloader

配置中心下载器，用于从配置中心下载密码文件。

#### 构造方法

| 方法 | 说明 |
|------|------|
| `ConfigCenterDownloader(String configCenterUrl)` | 创建下载器 |
| `ConfigCenterDownloader(String configCenterUrl, String apiKey)` | 创建带认证的下载器 |

#### 主要方法

##### download

下载配置文件：

```java
public void download(String targetDir) throws DownloadException
```

**参数**：
- `targetDir` - 目标目录

**抛出**：
- `DownloadException` - 下载失败时抛出

##### downloadWithCompare

对比并下载（仅远程文件更新时下载）：

```java
public boolean downloadWithCompare(String targetDir) throws DownloadException
```

**返回**：
- `true` - 已下载，`false` - 无需下载

##### compare

对比本地和远程配置：

```java
public ComparisonResult compare(String targetDir) throws DownloadException
```

**返回**：
- `ComparisonResult` 对象，包含对比结果

#### 使用示例

```java
import com.dbpwd.core.ConfigCenterDownloader;
import com.dbpwd.core.DownloadException;
import com.dbpwd.core.ComparisonResult;

// 创建下载器（HTTP）
ConfigCenterDownloader downloader = new ConfigCenterDownloader(
  "http://config-center.example.com/api/dbpwd/group1/app1/prod"
);

// 下载配置
try {
  downloader.download("~/.dbpwd/");
  System.out.println("Download completed");
} catch (DownloadException e) {
  System.err.println("Download failed: " + e.getMessage());
}

// 对比并下载
try {
  if (downloader.downloadWithCompare("~/.dbpwd/")) {
    System.out.println("Configuration updated and downloaded");
  } else {
    System.out.println("Configuration already up to date");
  }
} catch (DownloadException e) {
  System.err.println("Download failed: " + e.getMessage());
}

// 仅对比（不下载）
try {
  ComparisonResult result = downloader.compare("~/.dbpwd/");
  if (result.hasDifferences()) {
    System.out.println("Differences found: " + result.getDifferences());
  } else {
    System.out.println("No differences found");
  }
} catch (DownloadException e) {
  System.err.println("Compare failed: " + e.getMessage());
}
```

---

## 框架集成API

### DataSourceDecorator

数据源装饰器，用于自动解密连接池密码并注入到数据源配置中。

#### 构造方法

| 方法 | 说明 |
|------|------|
| `DataSourceDecorator(DataSource dataSource, byte[] sm4Key)` | 创建装饰器 |
| `DataSourceDecorator(DataSource dataSource, String keyFilePath)` | 使用密钥文件路径创建装饰器 |

#### 主要方法

##### decorate

装饰数据源并设置密码：

```java
public DataSource decorate(String databaseId)
```

**参数**：
- `databaseId` - 数据库ID（用于从密码文件中查找对应条目）

**返回**：
- 装饰后的DataSource

---

### DbPwdAutoConfiguration

Spring Boot自动配置类，自动配置dbpwd功能。

#### 配置属性

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| dbpwd.enabled | Boolean | true | 是否启用dbpwd |
| dbpwd.password-file | String | ~/.dbpwd/db.pwd | 密码文件路径 |
| dbpwd.key-file | String | ~/.dbpwd/sm4.key | 密钥文件路径 |

#### 使用示例

在Spring Boot应用中，只需添加依赖和配置，即可自动使用dbpwd：

**application.yml**:
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
    password: myapp-main  # 使用数据库ID
    driver-class-name: com.mysql.cj.jdbc.Driver
```

**Java Config**:
```java
@Configuration
public class DataSourceConfig {

  @Bean
  public DataSource dataSource() throws Exception {
    // 创建原始数据源
    DruidDataSource dataSource = new DruidDataSource();
    dataSource.setUrl("jdbc:mysql://localhost:3306/myapp");
    dataSource.setUsername("myapp_user");
    dataSource.setPassword("myapp-main");  // 使用数据库ID

    // 加载密钥
    byte[] key = Sm4KeyLoader.loadKeyFromFile("~/.dbpwd/sm4.key");

    // 装饰数据源
    DataSourceDecorator decorator = new DataSourceDecorator(dataSource, key);
    return decorator.decorate("myapp-main");
  }
}
```

---

## 异常处理

### CryptoException

加密解密异常。

#### 使用场景

- 密钥格式错误
- 加密解密失败
- 密码格式错误

#### 处理示例

```java
try {
  String encrypted = encryptor.encrypt("password", key);
} catch (CryptoException e) {
  System.err.println("Encryption failed: " + e.getMessage());
  // 记录日志、通知用户等
}
```

---

### DownloadException

配置下载异常。

#### 使用场景

- 配置中心访问失败
- 文件写入失败
- API认证失败

#### 处理示例

```java
try {
  downloader.download("~/.dbpwd/");
} catch (DownloadException e) {
  System.err.println("Download failed: " + e.getMessage());
  // 重试、使用本地缓存等
}
```

---

### PasswordFileException

密码文件异常。

#### 使用场景

- JSON解析失败
- 文件格式错误
- 文件不存在

#### 处理示例

```java
try {
  PasswordFile file = PasswordFileManager.load("~/.dbpwd/db.pwd");
} catch (IOException e) {
  System.err.println("Failed to load password file: " + e.getMessage());
  // 检查文件是否存在、权限是否正确等
}
```

---

## 代码示例

### 完整示例：添加并测试数据库

```java
import com.dbpwd.crypto.*;
import com.dbpwd.core.*;
import com.dbpwd.model.*;
import java.io.IOException;
import java.time.Instant;

public class DbPwdExample {

  public static void main(String[] args) {
    try {
      // 1. 加载或生成密钥
      byte[] key;
      try {
        key = Sm4KeyLoader.loadKeyFromFile("~/.dbpwd/sm4.key");
      } catch (IOException e) {
        key = Sm4KeyLoader.generateRandomKey();
        Files.write(Paths.get("~/.dbpwd/sm4.key"),
          Base64.getEncoder().encode(key));
      }

      // 2. 创建密码加密器
      PasswordEncryptor encryptor = new PasswordEncryptor();

      // 3. 加载或创建密码文件
      PasswordFile passwordFile;
      try {
        passwordFile = PasswordFileManager.load("~/.dbpwd/db.pwd");
      } catch (IOException e) {
        passwordFile = new PasswordFile("1.0", Instant.now());
      }

      // 4. 添加数据库条目
      String plainPassword = "my_secure_password";
      String encryptedPassword = encryptor.encrypt(plainPassword, key);

      DatabaseEntry entry = new DatabaseEntry(
        "myapp-db",
        "MyApp数据库",
        "mysql",
        "localhost",
        3306,
        "myapp",
        "jdbc:mysql://localhost:3306/myapp",
        "myapp_user",
        encryptedPassword,
        true
      );

      passwordFile.addDatabase(entry);

      // 5. 保存密码文件
      PasswordFileManager.save("~/.dbpwd/db.pwd", passwordFile);

      // 6. 测试数据库连接
      ConnectionTestResult result = ConnectionValidator.testDatabaseEntry(
        entry,
        encryptedPwd -> {
          try {
            return new PasswordDecryptor().decrypt(encryptedPwd, key);
          } catch (CryptoException e) {
            throw new RuntimeException("Decryption failed", e);
          }
        }
      );

      if (result.isSuccess()) {
        System.out.println("Connection successful! Time: " +
          result.getConnectionTimeMs() + "ms");
      } else {
        System.err.println("Connection failed: " + result.getErrorMessage());
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
```

---

### 完整示例：Spring Boot集成

```java
import com.dbpwd.crypto.Sm4KeyLoader;
import com.dbpwd.framework.DataSourceDecorator;
import com.alibaba.druid.pool.DruidDataSource;
import javax.sql.DataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DbPwdConfig {

  @Bean
  public DataSource dataSource() throws Exception {
    // 1. 创建原始数据源
    DruidDataSource dataSource = new DruidDataSource();
    dataSource.setUrl("jdbc:mysql://localhost:3306/myapp");
    dataSource.setUsername("myapp_user");
    // 使用数据库ID作为占位符
    dataSource.setPassword("myapp-main");
    dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

    // 2. 加载SM4密钥
    byte[] key = Sm4KeyLoader.loadKeyFromFile("~/.dbpwd/sm4.key");

    // 3. 创建装饰器
    DataSourceDecorator decorator = new DataSourceDecorator(dataSource, key);

    // 4. 装饰数据源（会自动解密密码）
    return decorator.decorate("myapp-main");
  }
}
```

---

## 附录

### A. 数据库类型映射

| 数据库类型 | type值 | JDBC驱动类名 | 默认端口 |
|-----------|--------|------------|----------|
| MySQL | mysql | com.mysql.cj.jdbc.Driver | 3306 |
| PostgreSQL | postgresql | org.postgresql.Driver | 5432 |
| Oracle | oracle | oracle.jdbc.OracleDriver | 1521 |
| SQL Server | sqlserver | com.microsoft.sqlserver.jdbc.SQLServerDriver | 1433 |
| H2 | h2 | org.h2.Driver | 9092 |
| DB2 | db2 | com.ibm.db2.jcc.DB2Driver | 50000 |
| Sybase | sybase | com.sybase.jdbc4.jdbc.SybDriver | 5000 |
| SQLite | sqlite | org.sqlite.JDBC | N/A |

### B. ENC格式规范

ENC格式：`ENC(算法:密文)`

- `ENC`：固定前缀，不区分大小写
- `算法`：加密算法名称（SM4、SM4-GCM等）
- `密文`：加密后的密文，通常使用Base64编码

示例：
- `ENC(SM4:r7X9k3mP8nQ2vL5wY1zA4cD6eF8gH0iJ=)`
- `ENC(SM4-GCM:abc123xyz456...)`

### C. 密钥文件格式

密钥文件格式：16字节的密钥，使用Base64编码

示例文件内容：
```
AABBCCDD1234567890ABCDEF12345678
```

长度：32个字符（16字节的Base64编码）

### D. 相关文档

- [用户使用手册](user-guide.md) - 用户操作指南
- [系统部署文档](deployment-guide.md) - 部署运维指南
- [开发者文档](developer-guide.md) - 开发者参考
- [常见问题FAQ](FAQ.md) - 常见问题解答
