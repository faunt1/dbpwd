# 配置中心目录结构方案

## 需求分析

在配置中心保存多个数据库连接配置时，需要能够清晰地区分不同的数据库。

## 目录结构方案对比

### 方案1: 按IP:端口分组

```
/druid/config/MySQL/
  ├── 192.168.1.100_3306.properties         (生产数据库1)
  ├── 192.168.1.101_3306.properties         (生产数据库2)
  ├── localhost_3306.properties             (开发数据库)
  └── 10.0.0.5_3306.properties              (测试数据库)
/druid/config/PostgreSQL/
  ├── 192.168.1.50_5432.properties
  └── localhost_5432.properties
/druid/config/Oracle/
  └── 192.168.1.60_1521.properties
```

**优点**:
- 清晰区分不同数据库类型
- IP:端口唯一标识数据库实例

**缺点**:
- 需要额外显示数据库名称

---

### 方案2: 按 IP:端口/数据库名 分组（推荐）

```
/druid/config/
  ├── prod/
  │   ├── mysql_192.168.1.100_3306_db1.properties
  │   ├── mysql_192.168.1.100_3306_db2.properties
  │   └── postgres_192.168.1.50_5432_appdb.properties
  ├── test/
  │   ├── mysql_192.168.1.101_3306_db1.properties
  │   └── postgres_192.168.1.51_5432_appdb.properties
  └── dev/
      └── mysql_localhost_3306_testdb.properties
```

**优点**:
- 按环境分组，适合多环境部署
- 文件名包含数据库类型、IP、端口、数据库名
- 层次清晰，易于管理

**缺点**:
- 文件名较长

---

### 方案3: 按业务/环境/数据库标识符（最灵活）

```
/druid/config/
  ├── business_order/
  │   ├── prod/
  │   │   ├── primary.properties
  │   │   └── secondary.properties
  │   ├── test/
  │   │   └── primary.properties
  │   └── dev/
  │       └── primary.properties
  ├── business_payment/
  │   ├── prod/
  │   │   ├── primary.properties
  │   │   └── secondary.properties
  │   ├── test/
  │   │   └── primary.properties
  │   └── dev/
  │       └── primary.properties
  └── common/
      ├── redis/
      │   └── prod.properties
      └── es/
          └── prod.properties
```

**优点**:
- 最灵活，符合业务组织结构
- 支持 primary/secondary 等语义化命名
- 易于权限管理

**缺点**:
- 需要手动指定业务分组
- 配置较复杂

---

### 方案4: 综合方案（推荐实施方案）

结合以上方案的优点，采用统一的配置文件命名规范：

```
/druid/config/
  ├── MySQL/
  │   ├── prod_192.168.1.100_3306_userdb.properties
  │   ├── prod_192.168.1.100_3306_orderdb.properties
  │   ├── test_192.168.1.101_3306_userdb.properties
  │   ├── dev_localhost_3306_testdb.properties
  │   └── dev_localhost_3306_demodb.properties
  ├── PostgreSQL/
  │   ├── prod_192.168.1.50_5432_analytics.properties
  │   ├── test_192.168.1.51_5432_analytics.properties
  │   └── dev_localhost_5432_testdb.properties
  ├── Oracle/
  │   ├── prod_192.168.1.60_1521_hrm.properties
  │   └── test_192.168.1.61_1521_hrm.properties
  └── SQLServer/
      └── prod_192.168.1.70_1433_crm.properties
```

**文件名格式**: `{env}_{host}_{port}_{database}.properties`

**示例**: `prod_192.168.1.100_3306_userdb.properties`

---

## 配置中心目录结构实现规范

### 目录生成规则

| 字段 | 说明 | 示例 |
|------|------|------|
| 根目录 | 固定前缀 | `/druid/config` |
| 数据库类型 | MySQL/PostgreSQL/Oracle/SQLServer | `/MySQL` |
| 环境 | prod/test/dev/uat (可选) | `/prod` |
| 文件名 | 格式化标识符 | `192.168.1.100_3306_userdb.properties` |

### 文件名生成规则

支持多种命名策略：

```java
public enum NamingStrategy {
    // 策略1: host_port_database
    HOST_PORT_DB,
    // 策略2: db_host_port
    DB_HOST_PORT,
    // 策略3: env_host_port_database
    ENV_HOST_PORT_DB,
    // 策略4: custom 自定义名称
    CUSTOM
}
```

### 数据库URL解析示例

```java
// MySQL URL: jdbc:mysql://192.168.1.100:3306/userdb?useSSL=false
解析结果:
  - 数据库类型: MySQL
  - 主机: 192.168.1.100
  - 端口: 3306
  - 数据库名: userdb
  - 文件名: 192.168.1.100_3306_userdb.properties
```

```java
// PostgreSQL URL: jdbc:postgresql://192.168.1.50:5432/analytics
解析结果:
  - 数据库类型: PostgreSQL
  - 主机: 192.168.1.50
  - 端口: 5432
  - 数据库名: analytics
  - 文件名: 192.168.1.50_5432_analytics.properties
```

## 应用场景映射

| 场景 | 推荐结构 | 说明 |
|------|---------|------|
| 单环境多数据库 | 按数据库类型分组 | 方案1 |
| 多环境多数据库 | 按环境/数据库类型双重分组 | 方案2 |
| 多业务系统 | 按业务/环境分组 | 方案3 |
| 统一管理 | 综合方案 | 方案4 |

## 配置迁移指南

### 从简单结构迁移

**原结构**: `/druid/config/druid.properties`

**迁移到新结构**:
1. 解析现有配置文件的URL
2. 提取数据库类型、主机、端口、数据库名
3. 生成新的文件名
4. 上传到新路径

### 目录结构配置

```properties
# 新的目录配置
config.center.directory=/druid/config/MySQL/prod
config.center.naming.strategy=HOST_PORT_DB
config.center.database.identifier=userdb
```

## 最佳实践建议

1. **使用环境前缀**: 添加 prod/test/dev 等环境标识
2. **使用有意义的名称**: 不要只用 ip_port，加上业务含义
3. **保持一致性**: 所有项目使用相同的目录结构规范
4. **文档化**: 维护一份配置中心目录结构文档
5. **自动化**: 使用脚本自动生成目录结构和文件名
