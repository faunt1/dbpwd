# 密码属性配置

> 文档版本: v3.0
> 最后更新: 2026-02-08

## 应用配置

### dbpwd.enabled

是否启用dbpwd服务：
- `true`: 启用
- `false`: 禁用（默认）

### dbpwd.password-file

密码文件路径，默认值：`~/.dbpwd/db.pwd`

**Linux/Unix**:
```yaml
dbpwd:
  password-file: /home/appuser/.dbpwd/db.pwd
```

**Windows**:
```yaml
dbpwd:
  password-file: C:/Users/user/.dbpwd/db.pwd
```

**容器部署推荐**:
```yaml
dbpwd:
  password-file: /app/.dbpwd/db.pwd
```

### dbpwd.key-file

SM4密钥文件路径，默认值：`~/.dbpwd/sm4.key`

### dbpwd.connection.timeout

数据库连接测试超时时间，默认值：5000（毫秒）

**配置示例**:
```yaml
dbpwd:
  connection:
    timeout: 5000
```

## 同步工具配置

### config-center.url

配置中心URL地址

**示例**:
```yaml
config-center:
  url: http://config-center.example.com
```

### config-center.api-key

配置中心API密钥

```yaml
config-center:
  api-key: ${CONFIG_CENTER_API_KEY:your-api-key}
```

### config-center.environment

应用环境标识

```yaml
config-center:
  environment: ${APP_ENV:prod}
```

### config-center.base-path

配置中心基础路径

```yaml
config-center:
  base-path: /databases
```

### 路径层级配置

```yaml
config-center:
  organization: ${ORG_NAME:物理组-财务系统}
  system: ${SYSTEM_NAME:财务系统}
  application: ${APP_NAME:支付应用}
```

### local.directory

本地存储目录

```yaml
local:
  directory: ~/.dbpwd
```

### local.files

需要同步的文件列表

```yaml
local:
  files:
    - db.pwd
    - sm4.key
```

### sync.interval

定时同步间隔（毫秒）

```yaml
sync:
  interval: 300000         # 5分钟
```

### sync.verify-file-integrity

是否验证文件完整性

```yaml
sync:
  verify-file-integrity: true
```

### sync.set-permissions

文件权限设置

```yaml
sync:
  set-permissions: 600    # 仅所有者读写
```

## 完整配置示例

### 应用配置（application.yml）

```yaml
dbpwd:
  enabled: true
  password-file: /app/.dbpwd/db.pwd
  key-file: /app/.dbpwd/sm4.key
  connection:
    timeout: 5000
```

### 同步工具配置（sync-config.yml）

```yaml
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

## 环境变量

| 变量名 | 说明 | 示例 |
|--------|------|------|
| `APP_ENV` | 应用环境标识 | `prod`, `test`, `dev` |
| `CONFIG_CENTER_API_KEY` | 配置中心API密钥 | `your-api-key` |
| `ORG_NAME` | 物理组织分组 | `物理组-财务系统` |
| `SYSTEM_NAME` | 系统名称 | `财务系统` |
| `APP_NAME` | 应用名称 | `支付应用` |
| `config-center.enabled` | 是否启用配置中心 | `false` |

## 相关文档

- [配置中心集成](config-center.md) - 配置中心详细说明
- [命令行工具](command-line.md) - 命令行使用说明
- [配置属性说明](properties.md) - 所有配置属性合集
