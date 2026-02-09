# 安全机制设计

> 文档版本: v4.0
> 最后更新: 2026-02-08

## SSH vs SM4密钥机制对比

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

## 安全增强建议

### 密钥保护机制增强

#### 1. 密钥密码短语（可选）

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

#### 2. 密钥拆分（Shamir秘密共享）

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

#### 3. 硬件安全模块（HSM）集成（未来）

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
```

## 文件权限控制

### Linux/Unix权限设置

**标准权限设置**:
```bash
chmod 600 ~/.dbpwd/sm4.key
chmod 600 ~/.dbpwd/db.pwd
chown appuser:appgroup ~/.dbpwd/sm4.key
chown appuser:appgroup ~/.dbpwd/db.pwd

# 验证权限
ls -l ~/.dbpwd/
```

### 容器部署权限设置

**docker-compose.yml**:
```yaml
volumes:
  - /host/.dbpwd:/app/.dbpwd
command: >
  sh -c "chmod 600 /app/.dbpwd/* && ls -la /app/.dbpwd/"
```

### NAS权限设置

```bash
chmod 600 /nas/volume/app/.dbpwd/sm4.key
chmod 600 /nas/volume/app/.dbpwd/db.pwd
```

## 密钥轮换流程

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

## 审计日志

### 审计字段

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

### 审计日志实现示例

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
    }

    public static void logDecrypt(String dbId, String userId) {
        log("decrypt", dbId, userId, true, "Password decrypted");
    }

    public static void logKeyLoad(String userId, boolean success) {
        log("loadKey", "sm4.key", userId, success, "SM4密钥加载");
    }

    private static String toJson(AuditEntry entry) {
        return entry.toString();
    }

    public static class AuditEntry {
        private String timestamp;
        private String action;
        private String dbId;
        private String userId;
        private String result;
        private String details;
    }
}
```

## 安全合规建议

| 合规项 | 建议措施 |
|-------|---------|
| **密钥存储** | 600权限，独立文件存储 |
| **密码轮换** | 每90天至少一次 |
| **密钥轮换** | 每年至少一次 |
| **审计日志** | 保留至少180天 |
| **备份策略** | 三地备份（生产、同城、异地） |
| **访问控制** | 最小权限原则，职责分离 |
| **传输安全** | 本地文件存储，不通过网络传输密钥 |

## 相关文档

- [密钥管理规范](../overview.md#sm4密钥管理) - 密钥文件格式说明
- [安全增强建议](#安全增强建议) - 详细安全增强方案
