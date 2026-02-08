# 命令行工具

> 文档版本: v3.0
> 最后更新: 2026-02-08

## 功能模块

- 从配置中心下载密钥或密码文件
- 加密/解密密码
- 数据库连接测试
- 数据库配置管理（添加/更新/删除/列表）
- 配置中心同步和对比

## 工具功能模块结构

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

## 命令行接口设计

### 基本信息

```bash
# 显示帮助信息
dbpwd-cli --help
dbpwd-cli -h

# 显示版本信息
dbpwd-cli version
dbpwd-cli -v
```

### download 命令

```bash
# 下载密钥文件
dbpwd-cli download key [options]

# 下载密码文件
dbpwd-cli download pwd [options]
```

**选项**:
- `-v, --verify`: 下载后验证文件完整性
- `-o, --output`: 指定输出路径（默认: ~/.dbpwd/）
- `--compare`: 下载后与本地文件对比更新时间

**示例**:
```bash
# 下载sm4.key
dbpwd-cli download key

# 下载并验证文件完整性
dbpwd-cli download key -v

# 下载db.pwd并对比更新时间
dbpwd-cli download pwd --compare

# 指定输出路径
dbpwd-cli download key -o /custom/path
```

### encrypt 命令

```bash
# 加密密码
dbpwd-cli encrypt [password] [options]

# 为指定数据库ID加密
dbpwd-cli encrypt -d db001 [options]
```

**选项**:
- `-f, --file`: 从文件读取密码
- `-d, --db-id`: 为指定数据库ID加密
- `--interactive`: 交互式输入密码（隐藏输入）

**示例**:
```bash
# 直接加密
dbpwd-cli encrypt "myPassword"

# 为db001更新密码
dbpwd-cli encrypt -d db001

# 交互式加密
dbpwd-cli encrypt --interactive
```

### decrypt 命令

```bash
# 解密密码
dbpwd-cli decrypt <encrypted-password>

# 解密指定数据库密码
dbpwd-cli decrypt -d db001
```

### test 命令

```bash
# 测试单个数据库连接
dbpwd-cli test db001

# 测试所有数据库连接
dbpwd-cli test all

# 显示详细信息
dbpwd-cli test all -v

# 自定义超时时间
dbpwd-cli test db001 --timeout 10
```

### admin 命令

```bash
# 列出所有数据库
dbpwd-cli admin list

# 添加数据库
dbpwd-cli admin add

# 更新数据库
dbpwd-cli admin update db001

# 删除数据库
dbpwd-cli admin delete db001
```

## 自动下载和对比功能

### ConfigCenterDownloader

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
}
```

## 密钥文件验证

### Sm4KeyValidator

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

## 使用示例

### 交互式模式

```bash
# 启动交互式CLI
java -cp . com.dbpwd.cli.DBPasswordCLI

# 可用命令
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

### 命令行参数模式

```bash
# 设置文件路径
java -cp . com.dbpwd.cli.DBPasswordCLI \
  --password-file=~/.dbpwd/db.pwd \
  --key-file=~/.dbpwd/sm4.key

# 设置配置中心信息
java -cp . com.dbpwd.cli.DBPasswordCLI \
  --config-center-url=http://config-center.com \
  --config-center-api-key=xxx \
  --environment=prod
```

## 配置文件

### 同步工具配置

```yaml
# sync-config.yml
config-center:
  url: http://config-center.example.com
  api-key: ${CONFIG_CENTER_API_KEY:your-api-key}
  environment: ${APP_ENV:prod}
  base-path: /databases

  # 路径层级配置
  organization: ${ORG_NAME:物理组-财务系统}
  system: ${SYSTEM_NAME:财务系统}
  application: ${APP_NAME:支付应用}

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

## 相关文档

- [配置属性说明](properties.md) - 配置参数说明
- [配置中心集成](config-center.md) - 配置中心详细说明
- [密钥派生算法](sm4-encryption.md) - 密钥派生说明
