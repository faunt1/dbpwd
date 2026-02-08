# 密码文件格式规范

> 文档版本: v3.0
> 最后更新: 2026-02-08

## JSON结构

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

## 字段定义

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

## 密码字段格式

| 格式 | 示例 | 说明 |
|------|------|------|
| SM4加密 | `ENC(SM4:AABBCCDD1234567890ABCDEF12345678)` | 格式：ENC(算法:密文) |
| 明文密码 | `myPassword123` | 直接明文（不推荐） |

## JSON格式 vs 编码格式（密码字段包含所有数据库信息）对比

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
| **验证逻辑** | �简单，JSON schema验证 | 复杂，需解析编码格式 |

## 编码格式（密码字段包含所有数据库信息）示例（不采用的原因）

```
# 编码格式会将URL、用户名、密码串在一起
password: "ENC(SM4:AABBCCDD...|jdbc:mysql://...:3306/db|username)"
```

## 不采用编码格式（密码字段包含所有数据库信息）的原因

1. **密码字段过于复杂**: 将多个不同性质的信息强制合并到同一字段，违反单一职责原则
2. **维护困难**: 修改URL或用户名需要重新编码密码字段
3. **不支持增量更新**: 更新单个数据库条目需要处理整个文件
4. **难以审计**: 密码字段过长且包含非密码信息，不利于安全审计
5. **工具不友好**: 需要自定义解析器，无法使用标准JSON工具

## 相关文档

- [SM4加密详解](sm4-encryption.md) - 加密流程和实现
- [SM4密钥管理](../overview.md#sm4密钥管理) - 密钥文件格式
