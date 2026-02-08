# SM4密钥管理

> 文档版本: v3.0
> 最后更新: 2026-02-08

## 目录

1. [密钥文件规范](#1-密钥文件规范)
2. [密钥生成](#2-密钥生成)
3. [密钥保护机制](#3-密钥保护机制)
4. [密钥轮换流程](#4-密钥轮换流程)
5. [密钥验证](#5-密钥验证)
6. [密钥备份与恢复](#6-密钥备份与恢复)
7. [密钥安全最佳实践](#7-密钥安全最佳实践)

---

## 1. 密钥文件规范

### 1.1 文件位置

```bash
# 标准位置
~/.dbpwd/sm4.key

# 容器部署
/app/.dbpwd/sm4.key
```

### 1.2 文件权限

```bash
# 设置权限为600（仅所有者可读写）
chmod 600 ~/.dbpwd/sm4.key

# 验证权限
ls -l ~/.dbpwd/sm4.key
# 输出: -rw------- 1 user group 32 Feb  8 12:00 sm4.key
```

### 1.3 格式规范

**格式**：16字节hex编码字符串
**长度**：32个字符（0-9, A-F）
**示例**：
```
AABBCCDD1234567890ABCDEF12345678
```

### 1.4 验证规范

| 验证项 | 要求 | 示例 |
|-------|-----|------|
| 文件存在 | 文件必须存在 | 检查文件路径 |
| 文件权限 | 600（Linux/Unix） | `chmod 600 sm4.key` |
| 内容长度 | 32字符 | `AABBCCDD1234567890ABCDEF12345678` |
| 内容格式 | 仅包含0-9, A-F | 无空格、换行、特殊字符 |
| 内容有效性 | 16字节hex解码成功 | 有效密钥数据 |

---

## 2. 密钥生成

### 2.1 生成流程

```
┌─────────────────────────────────────────────────────────────┐
│                      密钥生成流程                              │
└─────────────────────────────────────────────────────────────┘

生成随机密钥
    ↓
┌───────────────────────────────────────────────────────────┐
│ 1. 使用SecureRandom生成16字节随机数                        │
│ 2. 转换为hex编码字符串                                    │
│ 3. 验证密钥强度和唯一性                                   │
└───────────────────────────────────────────────────────────┘
    ↓
记录元数据
    ↓
┌───────────────────────────────────────────────────────────┐
│ 1. 生成时间                                              │
│ 2. 生成人/系统标识                                        │
│ 3. 用途描述                                              │
│ 4. 环境信息（开发/测试/生产）                             │
└───────────────────────────────────────────────────────────┘
    ↓
安全存储
    ↓
┌───────────────────────────────────────────────────────────┐
│ 1. 写入sm4.key文件，权限600                              │
│ 2. 可选：使用密码短语加密                              │
│ 3. 执行三地备份                                         │
└───────────────────────────────────────────────────────────┘
```

### 2.2 生成方法

#### 方法1：使用命令行工具

```bash
# 生成新密钥
dbpwd-cli generate-key --key-file ~/.dbpwd/sm4.key

# 带密码短语保护
dbpwd-cli generate-key --key-file ~/.dbpwd/sm4.key --passphrase
```

#### 方法2：使用Java代码

```java
import java.security.SecureRandom;

public class Sm4KeyGenerator {
    private static final int KEY_LENGTH = 16; // 16字节

    public static String generateKey() {
        // 使用密码学安全的随机数生成器
        SecureRandom random = new SecureRandom();
        byte[] keyBytes = new byte[KEY_LENGTH];
        random.nextBytes(keyBytes);

        // 转换为hex编码字符串
        StringBuilder hexString = new StringBuilder();
        for (byte b : keyBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString().toUpperCase();
    }

    public static void main(String[] args) {
        String key = generateKey();
        System.out.println("生成的SM4密钥: " + key);
        System.out.println("密钥长度: " + key.length() + " 字符");
    }
}
```

#### 方法3：使用OpenSSL

```bash
# 生成16字节随机数，并转换为hex编码
openssl rand -hex 16 | tr '[:lower:]' '[:upper:]' > ~/.dbpwd/sm4.key
chmod 600 ~/.dbpwd/sm4.key
```

---

## 3. 密钥保护机制

### 3.1 密钥密码短语保护（可选）

SM4密钥支持使用密码短语进行额外保护，增强安全性。

#### 保护流程

```
原始密钥（16字节）
    ↓
┌───────────────────────────────────────────────────────────┐
│ 1. 使用PBKDF2WithHmacSHA256派生保护密钥                   │
│    - 密码：用户提供的密码短语                              │
│    - 盐值：从密钥派生或随机生成                           │
│    - 迭代次数：10,000次                                   │
│    - 输出长度：16字节                                     │
└───────────────────────────────────────────────────────────┘
    ↓
┌───────────────────────────────────────────────────────────┐
│ 2. 使用保护密钥加密原始密钥                               │
│    - 算法：AES-CBC或相同SM4算法                           │
│    - 模式：带填充                                          │
└───────────────────────────────────────────────────────────┘
    ↓
┌───────────────────────────────────────────────────────────┐
│ 3. 存储加密后的密钥                                       │
│    - 文件：~/.dbpwd/sm4.key（加密后内容）                 │
│    - 文件：~/.dbpwd/sm4.key.salt（盐值，可选）            │
│    - 文件：~/.dbpwd/sm4.key.iv（初始向量，可选）          │
└───────────────────────────────────────────────────────────┘
```

#### 密码短语设置

```bash
# 启用密码短语保护
dbpwd-cli key-set-passphrase --key-file ~/.dbpwd/sm4.key

# 更改密码短语
dbpwd-cli key-set-passphrase --key-file ~/.dbpwd/sm4.key --change
```

### 3.2 密钥访问控制

| 访问场景 | 权限要求 | 说明 |
|---------|---------|------|
| 加密密码 | 只读 | 应用程序只需要加载密钥 |
| 解密密码 | 只读 | 应用程序只需要加载密钥 |
| 生成密钥 | 写入+执行 | 需要创建新密钥文件 |
| 密钥轮换 | 写入+执行 | 需要更新密钥文件 |

### 3.3 密钥审计

所有密钥操作应当记录审计日志：

```java
public class KeyAuditLogger {
    private static final Logger logger = Logger.getLogger(KeyAuditLogger.class.getName());

    /**
     * 记录密钥加载事件
     */
    public static void logKeyLoad(String keyFilePath, String userId) {
        AuditEntry entry = new AuditEntry();
        entry.setTimestamp(Instant.now().toString());
        entry.setAction("KEY_LOAD");
        entry.setFilePath(keyFilePath);
        entry.setUserId(userId);
        entry.setSuccess(true);

        logger.info("密钥加载审计: " + entry.toString());
    }

    /**
     * 记录密钥生成事件
     */
    public static void logKeyGeneration(String keyFilePath, String userId, String purpose) {
        AuditEntry entry = new AuditEntry();
        entry.setTimestamp(Instant.now().toString());
        entry.setAction("KEY_GENERATION");
        entry.setFilePath(keyFilePath);
        entry.setUserId(userId);
        entry.setPurpose(purpose);
        entry.setSuccess(true);

        logger.info("密钥生成审计: " + entry.toString());
    }

    /**
     * 记录密钥轮换事件
     */
    public static void logKeyRotation(String keyFilePath, String userId, String oldKeyId, String newKeyId) {
        AuditEntry entry = new AuditEntry();
        entry.setTimestamp(Instant.now().toString());
        entry.setAction("KEY_ROTATION");
        entry.setFilePath(keyFilePath);
        entry.setUserId(userId);
        entry.setDetails("旧密钥: " + oldKeyId + " -> 新密钥: " + newKeyId);
        entry.setSuccess(true);

        logger.info("密钥轮换审计: " + entry.toString());
    }
}
```

---

## 4. 密钥轮换流程

### 4.1 轮换触发条件

| 触发条件 | 说明 | 建议频率 |
|---------|-----|---------|
| 定期轮换 | 安全合规要求 | 每年至少一次 |
| 密钥泄露 | 安全事件响应 | 立即执行 |
| 人员变动 | 负责人离职 | 交接时执行 |
| 系统升级 | 加密算法更新 | 按需执行 |

### 4.2 轮换流程

```
┌─────────────────────────────────────────────────────────────┐
│                      密钥轮换完整流程                          │
└─────────────────────────────────────────────────────────────┘

准备阶段
    ↓
┌───────────────────────────────────────────────────────────┐
│ 1. 备份当前密钥和密码文件                                  │
│ 2. 选择低峰期执行窗口                                      │
│ 3. 准备应急预案（回滚方案）                                │
│ 4. 通知相关运维和应用团队                                  │
└───────────────────────────────────────────────────────────┘
    ↓
执行阶段
    ↓
┌───────────────────────────────────────────────────────────┐
│ 1. 生成新密钥                                              │
│    - 使用密码学安全的随机数生成器                          │
│    - 记录新密钥元数据                                      │
│ 2. 使用新密钥重新加密所有密码                              │
│    - 读取当前db.pwd文件                                    │
│    - 解密每个密码                                          │
│    - 使用新密钥重新加密                                    │
│ 3. 更新db.pwd文件                                          │
│    - 替换所有ENC(SM4:密文)                                │
│    - 更新lastModified时间戳                                │
│ 4. 原子性替换密钥文件                                      │
│    - 先写临时文件sm4.key.tmp                               │
│    - 验证内容正确                                          │
│    - 设置权限600                                           │
│    - 重命名为sm4.key                                       │
└───────────────────────────────────────────────────────────┘
    ↓
验证阶段
    ↓
┌───────────────────────────────────────────────────────────┐
│ 1. 验证程序加载新密钥                                      │
│ 2. 验证密码解密成功                                        │
│ 3. 测试数据库连接                                          │
│ 4. 检查应用日志，确认无错误                               │
└───────────────────────────────────────────────────────────┘
    ↓
同步阶段
    ↓
┌───────────────────────────────────────────────────────────┐
│ 1. 上传新文件到配置中心                                    │
│ 2. 提交评审流程                                            │
│ 3. 评审通过后发布到各环境                                  │
└───────────────────────────────────────────────────────────┘
    ↓
清理阶段
    ↓
┌───────────────────────────────────────────────────────────┐
│ 1. 安全备份旧密钥到冷存储                                  │
│ 2. 等待30天确认稳定后安全销毁旧密钥                        │
│ 3. 更新密钥轮换历史记录                                    │
│ 4. 通知相关团队轮换完成                                    │
└───────────────────────────────────────────────────────────┘
```

### 4.3 轮换命令

```bash
# 执行密钥轮换（自动重新加密所有密码）
dbpwd-cli rotate-key --key-file ~/.dbpwd/sm4.key --password-file ~/.dbpwd/db.pwd

# 验证轮换后的密钥
dbpwd-cli test-all --key-file ~/.dbpwd/sm4.key --password-file ~/.dbpwd/db.pwd
```

---

## 5. 密钥验证

### 5.1 验证清单

| 验证项 | 方法 | 预期结果 |
|-------|-----|---------|
| 文件存在 | `ls -l ~/.dbpwd/sm4.key` | 文件存在 |
| 文件权限 | `stat -c %a ~/.dbpwd/sm4.key` | 600 |
| 文件内容 | `head -1 ~/.dbpwd/sm4.key` | 32字符hex |
| 内容格式 | `grep -E '^[0-9A-F]{32}$' ~/.dbpwd/sm4.key` | 匹配成功 |
| 可用性 | `dbpwd-cli encrypt "test" --key-file ~/.dbpwd/sm4.key` | 加密成功 |

### 5.2 验证代码

```java
public class Sm4KeyValidator {

    /**
     * 验证密钥文件
     */
    public static ValidationResult validateKeyFile(String keyFilePath) {
        ValidationResult result = new ValidationResult();
        File keyFile = new File(keyFilePath);

        // 1. 检查文件存在
        if (!keyFile.exists()) {
            result.addError("密钥文件不存在: " + keyFilePath);
            return result;
        }

        // 2. 检查文件权限（Linux/Unix）
        try {
            Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(
                keyFile.toPath(), LinkOption.NOFOLLOW_LINKS
            );

            int mode = 0;
            if (permissions.contains(PosixFilePermission.OWNER_READ)) mode += 400;
            if (permissions.contains(PosixFilePermission.OWNER_WRITE)) mode += 200;
            if (permissions.contains(GROUP_READ) ||
                permissions.contains(GROUP_WRITE) ||
                permissions.contains(OTHERS_READ) ||
                permissions.contains(OTHERS_WRITE)) {
                result.addWarning("密钥文件权限不安全，应为600。当前权限: " + mode);
            }

            if (mode != 600) {
                result.addError("密钥文件权限不安全，必须设置为600。请运行：chmod 600 " + keyFilePath);
            }
        } catch (IOException e) {
            // Windows系统不支持Posix权限，跳过权限检查
        }

        // 3. 检查文件内容
        try {
            String keyContent = new String(Files.readAllBytes(keyFile.toPath())).trim();

            // 检查长度
            if (keyContent.length() != 32) {
                result.addError("SM4密钥格式不正确，必须为16字节的hex编码（32字符）");
                return result;
            }

            // 检查格式
            if (!keyContent.matches("^[0-9A-Fa-f]{32}$")) {
                result.addError("SM4密钥格式不正确，必须为hex编码字符串");
                return result;
            }

            // 验证可以正常使用（尝试解码）
            hexStringToBytes(keyContent);

            result.setValid(true);
        } catch (Exception e) {
            result.addError("密钥文件内容无效: " + e.getMessage());
        }

        return result;
    }

    /**
     * Hex字符串转字节数组
     */
    private static byte[] hexStringToBytes(String hexString) {
        int len = hexString.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                                 + Character.digit(hexString.charAt(i + 1), 16));
        }
        return data;
    }

    public static class ValidationResult {
        private boolean valid = false;
        private List<String> errors = new ArrayList<>();
        private List<String> warnings = new ArrayList<>();

        // getters, setters, addError, addWarning methods...
    }
}
```

---

## 6. 密钥备份与恢复

### 6.1 备份策略

**三地备份方案**：
1. **生产环境**：主备份，在线热备份
2. **同城备份**：灾备中心，定期同步
3. **异地备份**：异地冷备，离线存储

### 6.2 备份流程

```bash
#!/bin/bash
# 密钥备份脚本

BACKUP_DIR="/secure/backups/dbpwd/$(date +%Y%m%d)"
mkdir -p "$BACKUP_DIR"

# 备份密钥文件
cp ~/.dbpwd/sm4.key "$BACKUP_DIR/sm4.key"
chmod 600 "$BACKUP_DIR/sm4.key"

# 备份密码文件
cp ~/.dbpwd/db.pwd "$BACKUP_DIR/db.pwd"
chmod 600 "$BACKUP_DIR/db.pwd"

# 记备份元数据
echo "{" > "$BACKUP_DIR/backup-metadata.json"
echo "  \"backupTime\": \"$(date -u +%Y-%m-%dT%H:%M:%SZ)\"," >> "$BACKUP_DIR/backup-metadata.json"
echo "  \"keyFile\": \"sm4.key\"," >> "$BACKUP_DIR/backup-metadata.json"
echo "  \"passwordFile\": \"db.pwd\"," >> "$BACKUP_DIR/backup-metadata.json"
echo "  \"environment\": \"$(hostname)\"," >> "$BACKUP_DIR/backup-metadata.json"
echo "  \"operator\": \"$(whoami)\"" >> "$BACKUP_DIR/backup-metadata.json"
echo "}" >> "$BACKUP_DIR/backup-metadata.json"

# 打包并加密（可选）
tar -czf "$BACKUP_DIR.tar.gz" -C $(dirname "$BACKUP_DIR") $(basename "$BACKUP_DIR")
gpg --symmetric --cipher-algo AES256 "$BACKUP_DIR.tar.gz"

echo "备份完成: $BACKUP_DIR.tar.gz.gpg"
```

### 6.3 恢复流程

```bash
#!/bin/bash
# 密钥恢复脚本

BACKUP_FILE="/secure/backups/dbpwd/20260208/20260208.tar.gz.gpg"

# 解密备份
gpg --decrypt "$BACKUP_FILE" > backup.tar.gz

# 解压备份
tar -xzf backup.tar.gz

# 恢复密钥文件
cp */sm4.key ~/.dbpwd/sm4.key
chmod 600 ~/.dbpwd/sm4.key

# 恢复密码文件
cp */db.pwd ~/.dbpwd/db.pwd
chmod 600 ~/.dbpwd/db.pwd

# 清理临时文件
rm -rf backup.tar.gz */

echo "恢复完成"
```

---

## 7. 密钥安全最佳实践

### 7.1 命名和存储

| 事项 | 建议做法 | 说明 |
|-----|---------|------|
| 文件命名 | sm4.key | 清晰标识用途 |
| 文件位置 | ~/.dbpwd/ | 标准化路径 |
| 环境隔离 | 各环境独立密钥 | 开发/测试/生产分离 |
| 存储介质 | 加密磁盘 | 避免物理泄露风险 |

### 7.2 访问控制

| 事项 | 建议做法 | 说明 |
|-----|---------|------|
| 操作系统权限 | 600 | 仅所有者可读写 |
| 文件系统 | SELinux/AppArmor | 额外的访问控制 |
| 审计日志 | 启用完整审计 | 记录所有访问 |
| 监控告警 | 访问异常告警 | 及时发现异常访问 |

### 7.3 生命周期管理

| 阶段 | 建议做法 | 说明 |
|-----|---------|------|
| 生成 | 安全随机数生成器 | 确保密钥随机性 |
| 存储 | 加密+权限控制 | 多层安全防护 |
| 使用 | 负载共享 | 避免单点风险 |
| 轮换 | 每年至少一次 | 定期更新减少风险 |
| 销毁 | 安全擦除 | 多次覆写删除 |

### 7.4 合规要求

| 合规项 | 要求 | 说明 |
|-------|-----|------|
| 存储保护 | 600权限 | 文件系统级别访问控制 |
| 传输保护 | 避免网络传输 | 本地文件存储 |
| 使用审计 | 完整审计日志 | 记录所有密钥操作 |
| 密钥轮换 | 每年至少一次 | 符合安全合规要求 |
| 日志保留 | 至少180天 | 审计日志长期保留 |

---

## 相关文档

- [SM4加密详解](sm4-encryption.md) - 加密流程和密钥派生
- [安全机制设计](security.md) - 密钥轮换和安全增强
- [命令行工具](command-line.md) - 密钥生成和验证命令
- [密码文件格式](password-format.md) - 密码文件使用密钥的方式
