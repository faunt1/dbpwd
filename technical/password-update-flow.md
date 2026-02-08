# 密码更新流程

> 文档版本: v3.0
> 最后更新: 2026-02-08

## 目录

1. [密码更新场景](#1-密码更新场景)
2. [密码更新完整流程](#2-密码更新完整流程)
3. [密码更新方法](#3-密码更新方法)
4. [连接池密码动态更新](#4-连接池密码动态更新)
5. [密码更新验证](#5-密码更新验证)
6. [密码更新失败处理](#6-密码更新失败处理)
7. [密码更新最佳实践](#7-密码更新最佳实践)

---

## 1. 密码更新场景

### 1.1 定期轮换

**场景描述**：根据安全策略，数据库密码需要定期轮换。

**触发条件**：
- 时间到达预定轮换周期（如90天）
- 安全审计要求
- 合规检查要求

**更新范围**：
- 单个数据库密码
- 多个数据库密码（批量）
- 所有环境密码（开发、测试、生产）

### 1.2 安全事件响应

**场景描述**：检测到密码泄露或安全威胁时，需要紧急更新密码。

**触发条件**：
- 数据库异常登录检测
- 密码文件未授权访问
- 人员离职（密钥和密码相关的）
- 第三方安全报告

**响应等级**：
- P0级：立即执行（疑似泄露）
- P1级：24小时内执行（高风险）
- P2级：一周内执行（中等风险）

### 1.3 数据库迁移

**场景描述**：数据库迁移到新环境时需要更新密码。

**触发条件**：
- 数据库升级迁移
- 云平台迁移
- 数据中心迁移
- 数据库重构

**更新范围**：
- 新环境数据库部署
- 应用系统连接信息变更
- 连接测试验证

### 1.4 密码策略变更

**场景描述**：密码强度策略提升，现有密码不符合要求。

**触发条件**：
- 企业安全策略变更
- 行业合规要求变更
- 监管机构要求

**变更内容**：
- 密码长度提升
- 复杂度要求增加
- 特殊字符要求

---

## 2. 密码更新完整流程

### 2.1 更新流程图

```
┌─────────────────────────────────────────────────────────────┐
│                 密码更新完整流程（8步骤）                      │
└─────────────────────────────────────────────────────────────┘

┌───────────────────────────────────────────────────────────┐
│ 步骤1: 前置准备                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ 1.1 备份当前db.pwd文件                               │  │
│  │      - 复制到临时目录或备份目录                      │  │
│  │      - 验证备份完整性                                │  │
│  │ 1.2 确认新密码强度                                    │  │
│  │      - 密码长度（≥12字符）                           │  │
│  │      - 复杂度（大小写+数字+特殊字符）                 │  │
│  │      - 重复密码检查                                  │  │
│  │ 1.3 协调数据库访问时间窗口                            │  │
│  │      - 选择低峰期                                    │  │
│  │      - 通知应用团队                                  │  │
│  │      - 准备回滚方案                                  │  │
│  └─────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────┘
                        ↓
┌───────────────────────────────────────────────────────────┐
│ 步骤2: 更新数据库实际密码                                   │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ 2.1 连接到目标数据库                                 │  │
│  │      - 使用管理员账号                                │  │
│  │      - 确认连接正常                                  │  │
│  │ 2.2 使用数据库管理命令更新密码                       │  │
│  │      - MySQL: ALTER USER                           │  │
│  │      - PostgreSQL: ALTER ROLE                      │  │
│  │      - Oracle: ALTER USER                          │  │
│  │      - SQL Server: ALTER LOGIN                     │  │
│  │ 2.3 验证新密码可用性                                 │  │
│  │      - 使用新密码登录                                │  │
│  │      - 执行简单查询验证                              │  │
│  └─────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────┘
                        ↓
┌───────────────────────────────────────────────────────────┐
│ 步骤3: 加密新密码                                           │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ 3.1 加载SM4密钥                                      │  │
│  │      - 从sm4.key文件读取                             │  │
│  │      - 验证密钥格式                                  │  │
│  │ 3.2 使用SM4密钥加密新密码                            │  │
│  │      - 算法: SM4/ECB/PKCS5Padding                   │  │
│  │      - 模式: 默认填充模式                            │  │
│  │ 3.3 生成ENC(SM4:密文)格式                           │  │
│  │      - 格式: ENC(SM4:<base64编码的密文>)            │  │
│  │      - 示例: ENC(SM4:r7X9k3mP8nQ2vL5wY1zA4cD6eF8=)│  │
│  │ 3.4 验证加密正确性                                   │  │
│  │      - 解密密文验证                                  │  │
│  │      - 与原明文密码比对                              │  │
│  └─────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────┘
                        ↓
┌───────────────────────────────────────────────────────────┐
│ 步骤4: 更新密码文件                                         │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ 4.1 读取db.pwd文件                                   │  │
│  │      - 解析JSON内容                                  │  │
│  │      - 加载版本和时间戳                              │  │
│  │ 4.2 找到对应的DatabaseEntry                         │  │
│  │      - 根据id查找（如"db001"）                       │  │
│  │      - 确认条目存在                                  │  │
│  │ 4.3 更新entry字段                                    │  │
│  │      - password: ENC(SM4:<新密文>)                  │  │
│  │      - updatedAt: 当前时间戳                         │  │
│  │      - encrypted: true                               │  │
│  │ 4.4 更新文件元数据                                   │  │
│  │      - lastModified: 当前时间戳                      │  │
│  └─────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────┘
                        ↓
┌───────────────────────────────────────────────────────────┐
│ 步骤5: 保存文件                                             │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ 5.1 序列化为JSON并写入db.pwd                         │  │
│  │      - 临时文件原子写入                              │  │
│  │      - 先写入db.pwd.tmp                              │  │
│  │ 5.2 验证文件内容完整性                               │  │
│  │      - JSON解析验证                                  │  │
│  │      - 数据完整性检查                                │  │
│  │ 5.3 重命名为正式文件名                               │  │
│  │      - mv db.pwd.tmp db.pwd                          │  │
│  │ 5.4 设置文件权限600                                  │  │
│  │      - chmod 600 db.pwd                              │  │
│  │ 5.5 记录操作审计日志                                 │  │
│  │      - 操作时间                                      │  │
│  │      - 操作人                                        │  │
│  │      - 数据库ID                                      │  │
│  │      - 操作类型: 密码更新                            │  │
│  └─────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────┘
                        ↓
┌───────────────────────────────────────────────────────────┐
│ 步骤6: 同步到配置中心                                       │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ 6.1 上传更新后的db.pwd到配置中心                     │  │
│  │      - 配置中心API调用                               │  │
│  │      - 按环境上传（dev/test/prod）                    │  │
│  │ 6.2 提交评审流程                                     │  │
│  │      - 配置变更说明                                  │  │
│  │      - 影响范围评估                                  │  │
│  │      - 安全影响评估                                  │  │
│  │ 6.3 评审通过后发布                                   │  │
│  │      - 指定发布时间窗口                              │  │
│  │      - 发布确认通知                                  │  │
│  └─────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────┘
                        ↓
┌───────────────────────────────────────────────────────────┐
│ 步骤7: 应用程序同步                                         │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ 7.1 生产环境同步工具拉取新文件                       │  │
│  │      - 从配置中心下载新db.pwd                        │  │
│  │      - 验证文件完整性                                │  │
│  │      - 替换本地文件                                  │  │
│  │ 7.2 应用程序重新加载密码文件                        │  │
│  │      - 方式1: 自动监听文件变更，重载                  │  │
│  │      - 方式2: 接收信号重载（SIGUSR1）                │  │
│  │      - 方式3: 定时扫描，发现变更后重载                │  │
│  │ 7.3 连接池验证新密码（可选）                         │  │
│  │      - 创建测试连接                                  │  │
│  │      - 验证连接成功                                  │  │
│  │      - 失败则回滚                                    │  │
│  └─────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────┘
                        ↓
┌───────────────────────────────────────────────────────────┐
│ 步骤8: 验证和监控                                           │
│  ┌─────────────────────────────────────────────────────┐  │
│  │ 8.1 验证应用程序连接正常                             │  │
│  │      - 应用健康检查                                  │  │
│  │      - 数据库连接池状态                              │  │
│  │      - 业务功能测试                                  │  │
│  │ 8.2 监控错误日志                                     │  │
│  │      - 数据库连接失败日志                            │  │
│  │      - 认证失败日志                                  │  │
│  │      - 认证超时日志                                  │  │
│  │ 8.3 确认无连接失败                                   │  │
│  │      - 监控连接失败告警                              │  │
│  │      - 监控重试次数                                  │  │
│  │ 8.4 记录完成日志                                     │  │
│  │      - 密码更新成功日志                              │  │
│  │      - 通知相关团队                                  │  │
│  └─────────────────────────────────────────────────────┘  │
└───────────────────────────────────────────────────────────┘
                        ↓
                  [密码更新完成]
```

### 2.2 回滚流程

```
┌─────────────────────────────────────────────────────────────┐
│                    回滚触发条件                               │
└─────────────────────────────────────────────────────────────┘

❌ 新密码无法使用
❌ 应用连接失败
❌ 性能显著下降
❌ 监控告警触发

                        ↓
┌───────────────────────────────────────────────────────────┐
│ 回滚步骤                                                  │
└───────────────────────────────────────────────────────────┘

1. 从备份恢复db.pwd文件
   - 使用步骤1的备份
   - 恢复到更新前状态
   - 设置权限600

2. 回滚数据库密码
   - 数据库管理命令
   - 恢复原密码
   - 验证连接正常

3. 应用程序重载
   - 重启应用或重载配置
   - 验证连接成功
   - 监控运行状态

4. 记录回滚日志
   - 回滚原因
   - 回滚时间
   - 影响评估
```

---

## 3. 密码更新方法

### 3.1 交互式更新

```bash
# 使用交互式CLI更新密码
dbpwd-cli

> update db001
请输入新密码: *******
请再次输入: *******
密码强度: 强
确认更新密码? (y/n): y

正在更新数据库密码...
✓ 数据库密码已更新
✓ 密码已加密
✓ 文件已保存
✓ 连接测试通过

密码更新完成!
```

### 3.2 命令行更新

```bash
# 直接指定新密码
dbpwd-cli update db001 --password "NewStrongPassword123!"

# 强制更新（不进行连接测试）
dbpwd-cli update db001 --password "NewPassword123!" --force

# 批量更新（从环境变量）
export DB_PWD_NEW_PASSWORD="NewPassword123!"
dbpwd-cli update db001 db002 db003 --password "$DB_PWD_NEW_PASSWORD"
```

### 3.3 批量更新

```bash
# 更新所有数据库密码
dbpwd-cli update-all --password-policy "complex"

# 使用脚本批量更新
#!/bin/bash
NEW_PASSWORD="MyNewPassword123!"
DBS=("db001" "db002" "db003")

for db in "${DBS[@]}"; do
  echo "Updating $db..."
  dbpwd-cli update "$db" --password "$NEW_PASSWORD"
  if [ $? -eq 0 ]; then
    echo "✓ $db updated successfully"
  else
    echo "✗ $db update failed"
    exit 1
  fi
done

echo "All passwords updated!"
```

### 3.4 API更新

```java
// 使用API更新密码
public class PasswordUpdateDemo {

    public static void updatePassword(String dbId, String newPassword) throws Exception {
        // 1. 加载密码文件管理器
        PasswordFileManager fileManager = PasswordFileManager.loadFromFile("~/.dbpwd/db.pwd");

        // 2. 查找数据库条目
        DatabaseEntry entry = fileManager.getById(dbId);
        if (entry == null) {
            throw new IllegalArgumentException("未找到数据库配置: " + dbId);
        }

        // 3. 加载SM4密钥
        Sm4KeyLoader keyLoader = new Sm4KeyLoader("~/.dbpwd/sm4.key");
        String sm4Key = keyLoader.loadKey();

        // 4. 加密新密码
        String encryptedPassword = PasswordEncryptor.encryptSM4ENC(newPassword, sm4Key);

        // 5. 更新条目
        entry.setPassword(encryptedPassword);
        entry.setEncrypted(true);
        entry.touchUpdateTime();

        // 6. 保存文件
        fileManager.save();

        System.out.println("密码更新成功: " + dbId);
    }

    public static void main(String[] args) throws Exception {
        updatePassword("db001", "NewStrongPassword123!");
    }
}
```

---

## 4. 连接池密码动态更新

### 4.1 Druid连接池

Druid连接池支持动态更新密码，无需重建连接池。

```java
import com.alibaba.druid.pool.DruidDataSource;

public class DruidPasswordUpdate {

    /**
     * 更新Druid连接池密码
     */
    public static void updateDruidPassword(
            DruidDataSource dataSource,
            String newPassword) {

        // Druid支持直接设置新密码
        dataSource.setPassword(newPassword);
        dataSource.close();  // 关闭现有连接
        dataSource.restart();  // Druid重启连接池（可选，旧连接会自动回收）

        System.out.println("Druid密码已更新");
    }
}
```

### 4.2 HikariCP连接池

HikariCP不支持动态修改密码，需要重建数据源。

```java
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.alibaba.druid.pool.DruidDataSource;

public class HikariPasswordUpdate {

    /**
     * 更新HikariCP连接池密码（需要重建数据源）
     */
    public static HikariDataSource updateHikariPassword(
            HikariDataSource originalDataSource,
            String newPassword) throws Exception {

        // HikariCP不支持动态修改，需要重建数据源
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(originalDataSource.getJdbcUrl());
        config.setUsername(originalDataSource.getUsername());
        config.setPassword(newPassword);

        // 复制其他配置
        if (originalDataSource.getMinimumIdle() > 0) {
            config.setMaximumPoolSize(originalDataSource.getMaximumPoolSize());
            config.setMinimumIdle(originalDataSource.getMinimumIdle());
        }
        config.setConnectionTimeout(originalDataSource.getConnectionTimeout());
        config.setIdleTimeout(originalDataSource.getIdleTimeout());
        config.setMaxLifetime(originalDataSource.getMaxLifetime());

        // 创建新数据源
        HikariDataSource newDataSource = new HikariDataSource(config);

        // 温和关闭旧数据源（现有连接会完成）
        originalDataSource.close();

        return newDataSource;
    }

    /**
     * 使用包装类统一处理
     */
    public static void updatePassword(
            DataSource dataSource,
            String dbId,
            PasswordService passwordService) throws Exception {

        String newPassword = passwordService.getDecryptedPassword(dbId);

        if (dataSource instanceof DruidDataSource) {
            DruidDataSource druidDataSource = (DruidDataSource) dataSource;
            druidDataSource.setPassword(newPassword);
            druidDataSource.restart();
        } else if (dataSource instanceof HikariDataSource) {
            HikariDataSource hikariDataSource = (HikariDataSource) dataSource;
            HikariDataSource newDataSource = (HikariDataSource) updateHikariPassword(
                hikariDataSource, newPassword);
            // 替换数据源引用（需要应用层处理）
        } else {
            throw new IllegalArgumentException("不支持的数据源类型");
        }
    }
}
```

### 4.3 Spring Boot集成

```java
import org.springframework.stereotype.Component;
import javax.sql.DataSource;
import com.alibaba.druid.pool.DruidDataSource;
import com.zaxxer.hikari.HikariDataSource;

@Component
public class DatabasePasswordUpdater {

    private final DataSource dataSource;

    public DatabasePasswordUpdater(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * 更新数据库密码（Spring Boot集成）
     */
    public void updateDatabasePassword(String dbId, String newPassword) throws Exception {
        if (dataSource instanceof DruidDataSource) {
            DruidDataSource druidDataSource = (DruidDataSource) dataSource;
            druidDataSource.setPassword(newPassword);
            druidDataSource.restart();
        } else if (dataSource instanceof HikariDataSource) {
            // HikariCP需要重建数据源，需要替换Bean
            throw new UnsupportedOperationException(
                "HikariCP不支持动态更新密码，请使用Druid或重启应用");
        }
    }
}
```

---

## 5. 密码更新验证

### 5.1 连接测试

```bash
# 使用CLI测试连接
dbpwd-cli test db001

# 输出示例:
测试数据库连接: db001
主机: 192.168.1.100:3306
数据库: mydb
✓ 连接成功
```

### 5.2 密码解密验证

```java
public class PasswordVerification {

    /**
     * 验证密码加密正确性
     */
    public static boolean verifyPasswordEncryption(
            String originalPassword,
            String encryptedPassword,
            String sm4Key) throws Exception {

        // 解密
        String decrypted = PasswordDecryptor.decrypt(encryptedPassword, sm4Key);

        // 验证
        return originalPassword.equals(decrypted);
    }

    public static void main(String[] args) throws Exception {
        String original = "MyPassword123!";
        String sm4Key = "AABBCCDD1234567890ABCDEF12345678";

        // 加密
        String encrypted = PasswordEncryptor.encryptSM4ENC(original, sm4Key);

        // 验证
        boolean verified = verifyPasswordEncryption(original, encrypted, sm4Key);
        System.out.println("密码加密验证: " + (verified ? "通过" : "失败"));
    }
}
```

### 5.3 完整性验证

```bash
#!/bin/bash
# 密码更新验证脚本

DB_ID="db001"

echo "=== 密码更新验证 ==="

# 1. 验证文件完整性
echo "1. 验证db.pwd文件..."
if [ -f ~/.dbpwd/db.pwd ]; then
    echo "   ✓ 文件存在"
    chmod 600 ~/.dbpwd/db.pwd
    echo "   ✓ 权限已设置为600"
else
    echo "   ✗ 文件不存在"
    exit 1
fi

# 2. 验证JSON格式
echo "2. 验证JSON格式..."
if jq empty ~/.dbpwd/db.pwd 2>/dev/null; then
    echo "   ✓ JSON格式正确"
else
    echo "   ✗ JSON格式错误"
    exit 1
fi

# 3. 测试数据库连接
echo "3. 测试数据库连接..."
if dbpwd-cli test $DB_ID >/dev/null 2>&1; then
    echo "   ✓ 数据库连接成功"
else
    echo "   ✗ 数据库连接失败"
    exit 1
fi

# 4. 验证密码解密
echo "4. 验证密码解密..."
ENCRYPTED=$(dbpwd-cli show $DB_ID | grep password)
if [[ $ENCRYPTED == *"ENC(SM4:"* ]]; then
    PLAIN=$(dbpwd-cli decrypt $ENCRYPTED)
    if [ $? -eq 0 ]; then
        echo "   ✓ 密码解密成功"
    else
        echo "   ✗ 密码解密失败"
        exit 1
    fi
else
    echo "   ✗ 密码格式不正确"
    exit 1
fi

echo ""
echo "=== 所有验证通过 ==="
```

---

## 6. 密码更新失败处理

### 6.1 常见失败场景

| 失败场景 | 原因 | 处理方法 |
|---------|-----|---------|
| 数据库连接失败 | 新密码不正确或数据库故障 | 验证数据库密码，检查数据库状态 |
| 加密失败 | SM4密钥无效或格式错误 | 验证sm4.key文件，检查格式 |
| 文件保存失败 | 磁盘空间不足或权限问题 | 检查磁盘空间，验证写入权限 |
| 连接池更新失败 | 连接池不支持动态更新 | 重启应用或重建数据源 |

### 6.2 失败恢复流程

```
密码更新失败
    ↓
┌───────────────────────────────────────────────────────────┐
│ 1. 确定失败类型                                           │
│    - 数据库密码更新失败                                   │
│    - 密码加密失败                                         │
│    - 文件保存失败                                         │
│    - 程序错误                                             │
└───────────────────────────────────────────────────────────┘
    ↓
┌───────────────────────────────────────────────────────────┐
│ 2. 记录失败信息                                           │
│    - 失败时间                                             │
│    - 失败原因                                             │
│    - 失败阶段                                             │
│    - 错误消息                                             │
└───────────────────────────────────────────────────────────┘
    ↓
┌───────────────────────────────────────────────────────────┐
│ 3. 评估影响                                               │
│    - 是否影响系统运行                                     │
│    - 是否需要立即回滚                                     │
│    - 是否影响其他系统                                     │
└───────────────────────────────────────────────────────────┘
    ↓
┌───────────────────────────────────────────────────────────┐
│ 4. 执行回滚                                               │
│    - 从备份恢复db.pwd文件                                 │
│    - 回滚数据库密码（如已更新）                           │
│    - 重启应用或重载配置                                   │
└───────────────────────────────────────────────────────────┘
    ↓
┌───────────────────────────────────────────────────────────┐
│ 5. 分析根本原因                                           │
│    - 查看错误日志                                         │
│    - 复现失败场景                                         │
│    - 定位问题代码                                         │
└───────────────────────────────────────────────────────────┘
    ↓
┌───────────────────────────────────────────────────────────┐
│ 6. 修复问题                                               │
│    - 修复代码问题                                         │
│    - 更新配置                                             │
│    - 补充测试                                             │
└───────────────────────────────────────────────────────────┘
    ↓
┌───────────────────────────────────────────────────────────┐
│ 7. 验证修复                                               │
│    - 在测试环境验证                                       │
│    - 灰度发布到生产环境                                   │
│    - 全量发布                                             │
└───────────────────────────────────────────────────────────┘
```

### 6.3 错误代码

| 错误代码 | 错误描述 | 处理方法 |
|---------|---------|---------|
| DBPWD_ERR_001 | 数据库连接失败 | 检查网络和数据库状态 |
| DBPWD_ERR_002 | SM4密钥无效 | 验证sm4.key文件 |
| DBPWD_ERR_003 | 密码加密失败 | 检查SM4算法和密钥格式 |
| DBPWD_ERR_004 | 文件保存失败 | 检查磁盘和权限 |
| DBPWD_ERR_005 | 数据库条目不存在 | 检查db.pwd文件内容 |
| DBPWD_ERR_006 | 密码强度不足 | 使用更强的密码 |
| DBPWD_ERR_007 | 连接池更新失败 | 重启应用或使用Druid |

---

## 7. 密码更新最佳实践

### 7.1 密码强度策略

| 要求 | 说明 | 示例 |
|-----|-----|------|
| 长度 | ≥12个字符 | MySecurePassword123 |
| 复杂度 | 大小写+数字+特殊字符 | MySec#ure$Pass123 |
| 唯一性 | 不与其他密码重复 | 每个数据库独立密码 |
| 定期轮换 | 每90天或按要求 | 日历提醒到期密码 |
| 历史检查 | 不重复近期密码 | 检查最近3个密码周期 |

### 7.2 更新时机选择

| 场景 | 推荐时间 | 说明 |
|-----|---------|------|
| 定期轮换 | 业务低峰期 | 如凌晨2:00-4:00 |
| 紧急更新 | 立即执行 | 安全事件响应 |
| 应用升级 | 升级前执行 | 避免影响升级过程 |
| 数据库迁移 | 迁移完成后 | 确认新环境稳定 |

### 7.3 通知和协调

| 事项 | 说明 |
|-----|-----|
| 通知范围 | 运维团队、开发团队、数据库管理员 |
| 通知内容 | 更新时间、影响范围、恢复计划 |
| 协调事项 | 时间窗口、应急预案、回滚方案 |
| 监控重点 | 数据库连接、应用错误、性能指标 |

### 7.4 安全注意事项

| 注意事项 | 说明 |
|---------|-----|
| 禁止明文传输 | 密码始终加密存储和传输 |
| 最小权限原则 | 更新操作使用专用账号，操作后立即撤销 |
| 审计日志 | 记录所有密码更新操作 |
| 备份检查 | 更新前和更新后都应验证备份可用 |

---

## 相关文档

- [配置中心集成](config-center.md) - 配置中心同步策略
- [连接池包装类](connection-pool.md) - 连接池密码更新方法
- [框架集成](framework-integration.md) - Spring Boot集成
- [SM4加密详解](sm4-encryption.md) - 加密和解密流程
