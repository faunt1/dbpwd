# 系统部署文档

> 文档版本: v1.0
> 最后更新: 2026-02-10

## 目录

1. [部署概述](#部署概述)
2. [单机部署](#单机部署)
3. [多环境部署](#多环境部署)
4. [容器化部署](#容器化部署)
5. [配置中心集成](#配置中心集成)
6. [安全配置](#安全配置)
7. [升级迁移](#升级迁移)
8. [监控运维](#监控运维)

---

## 部署概述

### 部署架构

Database Password Manager 可以以以下方式部署：

| 部署方式 | 适用场景 | 复杂度 |
|---------|---------|--------|
| **单机部署** | 单个应用服务器 | 简单 |
| **多环境部署** | 开发/测试/生产环境 | 中等 |
| **容器化部署** | Kubernetes/Docker环境 | 中等 |
| **分布式部署** | 多个应用共享配置中心 | 复杂 |

### 部署前检查

在部署前，请确认满足以下条件：

- [ ] JDK 8+ 已安装
- [ ] Maven 3.6+ 已安装（用于构建）
- [ ] 目标服务器配置目录已创建
- [ ] 网络连通性确认（如需配置中心）
- [ ] 数据库访问权限已配置

---

## 单机部署

### 1. 安装准备

#### 1.1 创建安装目录

```bash
# 创建应用目录
sudo mkdir -p /opt/dbpwd
sudo chown $USER:$USER /opt/dbpwd

# 创建配置目录
mkdir -p ~/.dbpwd
chmod 700 ~/.dbpwd
```

#### 1.2 上传JAR文件

将编译好的JAR文件上传到服务器：

```bash
# 使用scp上传
scp db-pwd-core/target/dbpwd-1.0.0-jar-with-dependencies.jar \
  user@server:/opt/dbpwd/dbpwd.jar

# 或使用rsync
rsync -avz db-pwd-core/target/dbpwd-1.0.0-jar-with-dependencies.jar \
  user@server:/opt/dbpwd/dbpwd.jar
```

### 2. 初始化配置

#### 2.1 生成SM4密钥

```bash
cd /opt/dbpwd
java -jar dbpwd.jar generate-key
```

#### 2.2 添加数据库配置

```bash
# 交互模式
java -jar dbpwd.jar

# 或命令行模式
java -jar dbpwd.jar add \
  --id main-db \
  --name "生产数据库" \
  --type h2 \
  --url jdbc:h2:mem:myapp;DB_CLOSE_DELAY=-1 \
  --username sa \
  --password "secure_password_here"
```

#### 2.3 测试连接

```bash
java -jar dbpwd.jar test --database-id main-db
```

### 3. 集成到应用

#### 3.1 Spring Boot应用集成

在应用的 `application.yml` 中添加配置：

```yaml
dbpwd:
  enabled: true
  password-file: ~/.dbpwd/db.pwd
  key-file: ~/.dbpwd/sm4.key

spring:
  datasource:
    type: com.alibaba.druid.pool.DruidDataSource
    url: jdbc:h2:mem:myapp;DB_CLOSE_DELAY=-1
    username: sa
    password: main-db  # 使用数据库ID
    driver-class-name: org.h2.Driver

# 确保dbpwd在spring.datasource之前加载
```

#### 3.2 添加依赖

在应用的 `pom.xml` 中添加依赖：

```xml
<dependency>
  <groupId>com.dbpwd</groupId>
  <artifactId>db-pwd-core</artifactId>
  <version>1.0.0</version>
</dependency>

<!-- 确保Druid连接池依赖 -->
<dependency>
  <groupId>com.alibaba</groupId>
  <artifactId>druid-spring-boot-starter</artifactId>
  <version>1.2.20</version>
</dependency>
```

### 4. 启动应用

```bash
# 如果使用dbpwd作为独立工具
java -jar /opt/dbpwd/dbpwd.jar

# 如果是Spring Boot应用
java -jar /opt/myapp/myapp.jar
```

---

## 多环境部署

### 环境规划

| 环境 | 用途 | 配置目录 | 配置中心路径 |
|------|------|---------|-------------|
| dev | 开发环境 | `~/.dbpwd/dev` | `/config/group1/app1/dev` |
| test | 测试环境 | `~/.dbpwd/test` | `/config/group1/app1/test` |
| prod | 生产环境 | `~/.dbpwd/prod` | `/config/group1/app1/prod` |

### 1. 创建多环境配置目录

```bash
# 创建各环境目录
mkdir -p ~/.dbpwd/dev ~/.dbpwd/test ~/.dbpwd/prod
chmod 700 ~/.dbpwd/dev ~/.dbpwd/test ~/.dbpwd/prod
```

### 2. 为每个环境生成密钥

```bash
# 开发环境
java -jar dbpwd.jar generate-key --output ~/.dbpwd/dev/sm4.key
chmod 600 ~/.dbpwd/dev/sm4.key

# 测试环境
java -jar dbpwd.jar generate-key --output ~/.dbpwd/test/sm4.key
chmod 600 ~/.dbpwd/test/sm4.key

# 生产环境
java -jar dbpwd.jar generate-key --output ~/.dbpwd/prod/sm4.key
chmod 600 ~/.dbpwd/prod/sm4.key
```

### 3. 配置各应用环境

#### 开发环境

```bash
# 配置开发数据库
java -jar dbpwd.jar add \
  --id dev-db \
  --type mysql \
  --host localhost \
  --port 3306 \
  --database myapp_dev \
  --username dev_user \
  --password "dev_password" \
  --password-file ~/.dbpwd/dev/db.pwd \
  --key-file ~/.dbpwd/dev/sm4.key
```

#### 测试环境

```bash
# 配置测试数据库
java -jar dbpwd.jar add \
  --id test-db \
  --type mysql \
  --host test-db-server \
  --port 3306 \
  --database myapp_test \
  --username test_user \
  --password "test_password" \
  --password-file ~/.dbpwd/test/db.pwd \
  --key-file ~/.dbpwd/test/sm4.key
```

#### 生产环境

```bash
# 配置生产数据库
java -jar dbpwd.jar add \
  --id prod-db \
  --type mysql \
  --host prod-db-server \
  --port 3306 \
  --database myapp_prod \
  --username prod_user \
  --password "prod_secure_password" \
  --password-file ~/.dbpwd/prod/db.pwd \
  --key-file ~/.dbpwd/prod/sm4.key
```

### 4. 应用配置

#### 使用环境变量切换环境

创建脚本 `set-env.sh`：

```bash
#!/bin/bash
# 设置环境变量

ENV=$1

case $ENV in
  dev)
    export DBPWD_PASSWORD_FILE=~/.dbpwd/dev/db.pwd
    export DBPWD_KEY_FILE=~/.dbpwd/dev/sm4.key
    ;;
  test)
    export DBPWD_PASSWORD_FILE=~/.dbpwd/test/db.pwd
    export DBPWD_KEY_FILE=~/.dbpwd/test/sm4.key
    ;;
  prod)
    export DBPWD_PASSWORD_FILE=~/.dbpwd/prod/db.pwd
    export DBPWD_KEY_FILE=~/.dbpwd/prod/sm4.key
    ;;
  *)
    echo "Usage: $0 {dev|test|prod}"
    exit 1
    ;;
esac

echo "Environment set to: $ENV"
echo "DBPWD_PASSWORD_FILE=$DBPWD_PASSWORD_FILE"
echo "DBPWD_KEY_FILE=$DBPWD_KEY_FILE"
```

使用：
```bash
source set-env.sh dev
java -jar myapp.jar
```

#### Spring Boot Profiles配置

创建 `application-dev.yml`：

```yaml
dbpwd:
  password-file: ~/.dbpwd/dev/db.pwd
  key-file: ~/.dbpwd/dev/sm4.key

spring:
  datasource:
    password: dev-db
```

创建 `application-prod.yml`：

```yaml
dbpwd:
  password-file: ~/.dbpwd/prod/db.pwd
  key-file: ~/.dbpwd/prod/sm4.key

spring:
  datasource:
    password: prod-db
```

启动时指定Profile：
```bash
java -jar myapp.jar --spring.profiles.active=prod
```

---

## 容器化部署

### 1. Docker部署

#### 1.1 创建Dockerfile

```dockerfile
FROM openjdk:8-jre-alpine

# 安装必要的工具
RUN apk add --no-cache bash curl

# 创建应用目录
WORKDIR /opt/app

# 复制应用JAR
COPY target/myapp.jar /opt/app/myapp.jar

# 复制dbpwd依赖
COPY db-pwd-core/target/dbpwd-1.0.0-jar-with-dependencies.jar /opt/app/dbpwd.jar

# 创建配置目录
RUN mkdir -p /root/.dbpwd && \
    chmod 700 /root/.dbpwd

# 挂载点
VOLUME ["/root/.dbpwd"]

# 环境变量
ENV DBPWD_ENABLED=true
ENV DBPWD_PASSWORD_FILE=/root/.dbpwd/db.pwd
ENV DBPWD_KEY_FILE=/root/.dbpwd/sm4.key

# 暴露端口
EXPOSE 8080

# 启动命令
ENTRYPOINT ["java", "-jar", "/opt/app/myapp.jar"]
```

#### 1.2 构建镜像

```bash
docker build -t myapp:1.0.0 .
```

#### 1.3 运行容器

```bash
# 创建配置目录
sudo mkdir -p /var/dbpwd/myapp
sudo chown 1000:1000 /var/dbpwd/myapp

# 准备配置文件
# 将 db.pwd 和 sm4.key 放到 /var/dbpwd/myapp/
# 并设置正确的权限（600）

# 运行容器
docker run -d \
  --name myapp \
  -p 8080:8080 \
  -v /var/dbpwd/myapp:/root/.dbpwd \
  myapp:1.0.0
```

### 2. Kubernetes部署

#### 2.1 创建ConfigMap

创建 `dbpwd-configmap.yaml`：

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: dbpwd-config
  namespace: default
data:
  db.pwd: |
    {
      "version": "1.0",
      "lastModified": "2026-02-10T10:00:00Z",
      "databases": [
        {
          "id": "main-db",
          "name": "主数据库",
          "type": "mysql",
          "host": "mysql-service",
          "port": 3306,
          "database": "myapp",
          "url": "jdbc:mysql://mysql-service:3306/myapp",
          "username": "myapp_user",
          "password": "ENC(SM4:your_encrypted_password_here)",
          "encrypted": true,
          "createdAt": "2026-02-10T08:00:00Z",
          "updatedAt": "2026-02-10T10:00:00Z"
        }
      ]
    }
```

**注意**：ConfigMap不适合存储密钥！应该使用Secret。

#### 2.2 创建Secret

创建 `dbpwd-secret.yaml`：

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: dbpwd-secret
  namespace: default
type: Opaque
data:
  sm4.key: YOUR_BASE64_ENCODED_KEY_HERE
```

生成Base64编码的密钥：
```bash
echo -n "your_key_here" | base64
```

#### 2.3 创建Deployment

创建 `myapp-deployment.yaml`：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: myapp
  namespace: default
spec:
  replicas: 3
  selector:
    matchLabels:
      app: myapp
  template:
    metadata:
      labels:
        app: myapp
    spec:
      containers:
      - name: myapp
        image: myapp:1.0.0
        ports:
        - containerPort: 8080
        env:
        - name: DBPWD_ENABLED
          value: "true"
        - name: DBPWD_PASSWORD_FILE
          value: "/root/.dbpwd/db.pwd"
        - name: DBPWD_KEY_FILE
          value: "/root/.dbpwd/sm4.key"
        volumeMounts:
        - name: dbpwd-config
          mountPath: /root/.dbpwd/db.pwd
          subPath: db.pwd
        - name: dbpwd-secret
          mountPath: /root/.dbpwd/sm4.key
          subPath: sm4.key
      volumes:
      - name: dbpwd-config
        configMap:
          name: dbpwd-config
      - name: dbpwd-secret
        secret:
          secretName: dbpwd-secret
```

#### 2.4 部署

```bash
# 应用配置
kubectl apply -f dbpwd-configmap.yaml
kubectl apply -f dbpwd-secret.yaml
kubectl apply -f myapp-deployment.yaml

# 查看状态
kubectl get pods -l app=myapp

# 查看日志
kubectl logs -f deployment/myapp
```

#### 2.5 密钥文件权限问题

在Kubernetes中，无法直接设置挂载文件的权限（600）。这里有几种解决方案：

**方案1：使用initContainer**

```yaml
spec:
  initContainers:
  - name: setup-permissions
    image: busybox:1.35
    command: ["sh", "-c", "chmod 600 /root/.dbpwd/sm4.key && chmod 600 /root/.dbpwd/db.pwd"]
    volumeMounts:
    - name: dbpwd-config
      mountPath: /root/.dbpwd
  containers:
  - name: myapp
    # ... 其他配置
    volumeMounts:
    - name: dbpwd-config
      mountPath: /root/.dbpwd
```

**方案2：使用sidecar容器**

```yaml
spec:
  containers:
  - name: myapp
    # ... 主应用配置
  - name: permission-helper
    image: busybox:1.35
    command: ["sh", "-c", "while true; do sleep 3600; done"]
    lifecycle:
      postStart:
        exec:
          command: ["sh", "-c", "chmod 600 /root/.dbpwd/sm4.key && chmod 600 /root/.dbpwd/db.pwd"]
    volumeMounts:
    - name: dbpwd-config
      mountPath: /root/.dbpwd
```

**方案3：在应用启动时设置权限**

在应用启动脚本中添加：

```bash
#!/bin/bash
chmod 600 /root/.dbpwd/sm4.key
chmod 600 /root/.dbpwd/db.pwd
java -jar /opt/app/myapp.jar
```

---

## 配置中心集成

### 方案概述

dbpwd支持与配置中心集成，实现密码文件的集中管理和分发。

### 支持的配置中心类型

| 类型 | 配置中心 | 集成方式 |
|------|---------|---------|
| HTTP API | 自定义配置中心API | HTTP GET请求 |
| HTTPS API | 配置中心API（带认证） | HTTPS GET请求 + API Key |
| 文件系统 | NFS/SMB共享存储 | 直接文件复制 |

### 1. HTTP API配置中心

#### 1.1 配置中心API规范

配置中心应提供以下REST API：

```
GET /api/dbpwd/{path}
```

响应格式：
```json
{
  "success": true,
  "data": {
    "db.pwd": "base64_encoded_content",
    "sm4.key": "base64_encoded_content",
    "timestamp": "2026-02-10T10:00:00Z"
  }
}
```

#### 1.2 配置下载

```bash
# 从HTTP API下载
java -jar dbpwd.jar download \
  --config-center-url "http://config-center.example.com/api/dbpwd/group1/app1/prod"
```

### 2. HTTPS API配置中心（带认证）

#### 2.1 API Key认证

```bash
# 使用API Key认证
java -jar dbpwd.jar download \
  --config-center-url "https://config-center.example.com/api/dbpwd/group1/app1/prod" \
  --api-key "your_api_key_here"
```

#### 2.2 配置中心URL格式

```
{protocol}://{config-center-host}/{api-path}/{group}/{app}/{module}/{env}
```

示例：
```
https://config.example.com/api/dbpwd/groupA/finance-system/payment-app/prod
```

### 3. 配置中心目录规范

请遵循以下目录结构：

```
{配置中心根目录}/
├── {物理组}/
│   └── {应用名称}/
│       └── {应用模块}/
│           ├── dev/
│           │   ├── db.pwd
│           │   └── sm4.key
│           ├── test/
│           │   ├── db.pwd
│           │   └── sm4.key
│           └── prod/
│               ├── db.pwd
│               └── sm4.key
```

### 4. 定期同步任务

#### 4.1 创建同步脚本

创建 `sync-dbpwd.sh`：

```bash
#!/bin/bash

# 配置参数
CONFIG_CENTER_URL="${DBPWD_CONFIG_CENTER_URL}"
API_KEY="${DBPWD_API_KEY}"
LOG_FILE="/var/log/dbpwd-sync.log"

# 日志函数
log() {
  echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

# 执行同步
log "Starting config synchronization..."

java -jar /opt/dbpwd/dbpwd.jar download \
  --config-center-url "$CONFIG_CENTER_URL" \
  --api-key "$API_KEY" \
  --verbose 2>&1 | tee -a "$LOG_FILE"

if [ $? -eq 0 ]; then
  log "Synchronization completed successfully"
else
  log "Synchronization failed!"
  exit 1
fi
```

#### 4.2 配置Cron任务

```bash
# 编辑crontab
crontab -e

# 添加定时任务（每小时同步一次）
0 * * * * /opt/dbpwd/sync-dbpwd.sh

# 添加定时任务（每天凌晨2点同步）
0 2 * * * /opt/dbpwd/sync-dbpwd.sh
```

#### 4.3 Systemd定时器（推荐）

创建 `/etc/systemd/system/dbpwd-sync.service`：

```ini
[Unit]
Description=DBPassword Config Sync Service
After=network.target

[Service]
Type=oneshot
ExecStart=/opt/dbpwd/sync-dbpwd.sh
User=dbpwd
Group=dbpwd
```

创建 `/etc/systemd/system/dbpwd-sync.timer`：

```ini
[Unit]
Description=DBPassword Config Sync Timer

[Timer]
OnCalendar=hourly
Persistent=true

[Install]
WantedBy=timers.target
```

启用定时器：
```bash
sudo systemctl daemon-reload
sudo systemctl enable dbpwd-sync.timer
sudo systemctl start dbpwd-sync.timer
sudo systemctl status dbpwd-sync.timer
```

---

## 安全配置

### 1. 文件权限

Linux/Mac环境必须设置正确的文件权限：

```bash
# 配置目录
chmod 700 ~/.dbpwd

# 密钥文件
chmod 600 ~/.dbpwd/sm4.key

# 密码文件
chmod 600 ~/.dbpwd/db.pwd
```

权限检查脚本：

```bash
#!/bin/bash

# 检查dbpwd配置目录权限
check_dbpwd_permissions() {
  local config_dir="${1:-$HOME/.dbpwd}"

  if [ ! -d "$config_dir" ]; then
    echo "Error: Config directory $config_dir does not exist"
    return 1
  fi

  # 检查目录权限
  dir_perm=$(stat -c "%a" "$config_dir" 2>/dev/null || stat -f "%A" "$config_dir" 2>/dev/null)
  if [ "$dir_perm" != "700" ]; then
    echo "Warning: Config directory $config_dir has insecure permissions: $dir_perm (should be 700)"
    chmod 700 "$config_dir"
    echo "Fixed: Set permissions to 700"
  fi

  # 检查密钥文件权限
  key_file="$config_dir/sm4.key"
  if [ -f "$key_file" ]; then
    key_perm=$(stat -c "%a" "$key_file" 2>/dev/null || stat -f "%A" "$key_file" 2>/dev/null)
    if [ "$key_perm" != "600" ]; then
      echo "Warning: Key file $key_file has insecure permissions: $key_perm (should be 600)"
      chmod 600 "$key_file"
      echo "Fixed: Set permissions to 600"
    fi
  fi

  # 检查密码文件权限
  pwd_file="$config_dir/db.pwd"
  if [ -f "$pwd_file" ]; then
    pwd_perm=$(stat -c "%a" "$pwd_file" 2>/dev/null || stat -f "%A" "$pwd_file" 2>/dev/null)
    if [ "$pwd_perm" != "600" ]; then
      echo "Warning: Password file $pwd_file has insecure permissions: $pwd_perm (should be 600)"
      chmod 600 "$pwd_file"
      echo "Fixed: Set permissions to 600"
    fi
  fi

  echo "Permission check completed"
}

check_dbpwd_permissions
```

### 2. 密钥管理最佳实践

#### 2.1 密钥生成建议

- 使用加密安全的随机生成器（dbpwd已内置）
- 密钥长度固定为128位（16字节）
- 不要使用可预测的密钥
- 不要使用简单密码作为密钥

#### 2.2 密钥存储建议

- 在Linux/Mac上使用600权限
- 不要将密钥提交到代码仓库
- 在Windows上确保只有当前用户可访问
- 考虑使用密码短语加密密钥（PBKDF2）

#### 2.3 密钥轮换建议

- 建议每90-180天轮换一次密钥
- 轮换密钥时需要重新加密所有密码
- 密钥轮换前先在测试环境验证
- 保留旧密钥备份直到新密钥确认正常使用

### 3. 审计日志

#### 3.1 启用日志记录

在Spring Boot应用中启用dbpwd日志：

```yaml
logging:
  level:
    com.dbpwd: DEBUG
```

#### 3.2 日志内容

dbpwd会记录以下关键操作：

- 密码解密
- 数据库连接
- 配置文件加载
- 错误和异常

#### 3.3 集中日志收集

可以使用以下工具收集和转发日志：

- **ELK Stack**: Elasticsearch + Logstash + Kibana
- **Fluentd**: 统一日志收集层
- **Loki**: Grafana Loki日志聚合系统

---

## 升级迁移

### 升级检查清单

在升级前，请完成以下检查：

- [ ] 备份当前配置文件（db.pwd 和 sm4.key）
- [ ] 备份当前JAR文件
- [ ] 在测试环境验证新版本
- [ ] 准备回滚方案
- [ ] 确认升级窗口和通知相关人员

### 升级步骤

#### 1. 备份配置

```bash
# 创建备份目录
mkdir -p ~/dbpwd-backup/$(date +%Y%m%d)

# 备份配置文件
cp ~/.dbpwd/db.pwd ~/dbpwd-backup/$(date +%Y%m%d)/
cp ~/.dbpwd/sm4.key ~/dbpwd-backup/$(date +%Y%m%d)/

# 备份JAR文件
cp /opt/dbpwd/dbpwd.jar ~/dbpwd-backup/$(date +%Y%m%d)/
```

#### 2. 停止应用

```bash
# 停止Spring Boot应用
sudo systemctl stop myapp

# 或手动停止
kill $(ps aux | grep myapp.jar | grep -v grep | awk '{print $2}')
```

#### 3. 替换JAR文件

```bash
# 上传新版本JAR
scp dbpwd-1.0.0-jar-with-dependencies.jar \
  user@server:/opt/dbpwd/dbpwd.jar

# 或在服务器上解压
unzip db-pwd-1.0.0-dist.zip -d /opt/dbpwd/
```

#### 4. 验证配置兼容性

```bash
# 验证密码格式
java -jar /opt/dbpwd/dbpwd.jar list

# 测试数据库连接
java -jar /opt/dbpwd/dbpwd.jar test --all
```

#### 5. 更新应用依赖

如果将dbpwd作为应用依赖使用，需要更新应用的pom.xml：

```xml
<dependency>
  <groupId>com.dbpwd</groupId>
  <artifactId>db-pwd-core</artifactId>
  <version>1.0.0</version>  <!-- 更新版本号 -->
</dependency>
```

重新编译应用：
```bash
mvn clean package
```

#### 6. 启动应用

```bash
# 启动应用
sudo systemctl start myapp

# 检查状态
sudo systemctl status myapp

# 查看日志
sudo journalctl -u myapp -f
```

#### 7. 验证功能

```bash
# 检查应用健康状态
curl http://localhost:8080/actuator/health

# 测试应用功能
curl http://localhost:8080/api/test
```

### 回滚方案

如果升级后出现问题，按以下步骤回滚：

```bash
# 1. 停止应用
sudo systemctl stop myapp

# 2. 恢复旧版本JAR
cp ~/dbpwd-backup/XXXXXXXX/dbpwd.jar /opt/dbpwd/dbpwd.jar

# 3. 恢复配置文件（如果需要）
cp ~/dbpwd-backup/XXXXXXXX/db.pwd ~/.dbpwd/db.pwd
cp ~/dbpwd-backup/XXXXXXXX/sm4.key ~/.dbpwd/sm4.key

# 4. 恢复应用依赖（如果需要）
# 在应用pom.xml中恢复旧版本号，重新编译

# 5. 启动应用
sudo systemctl start myapp
```

---

## 监控运维

### 1. 健康检查

#### 1.1 数据库连接健康检查

定期检查数据库连接状态：

```bash
#!/bin/bash

check_db_connection() {
  local db_id=$1
  local result=$(java -jar /opt/dbpwd/dbpwd.jar test --database-id "$db_id" 2>&1)

  if echo "$result" | grep -q "Connection successful"; then
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Database $db_id: OK"
    return 0
  else
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] Database $db_id: FAILED"
    echo "$result"
    return 1
  fi
}

# 检查所有数据库
for db_id in $(java -jar /opt/dbpwd/dbpwd.jar list --id-only); do
  check_db_connection "$db_id"
done
```

#### 1.2 配置文件健康检查

检查配置文件的完整性和权限：

```bash
#!/bin/bash

check_config() {
  local config_dir="${1:-$HOME/.dbpwd}"

  # 检查密钥文件
  if [ ! -f "$config_dir/sm4.key" ]; then
    echo "ERROR: Key file not found: $config_dir/sm4.key"
    return 1
  fi

  # 检查密码文件
  if [ ! -f "$config_dir/db.pwd" ]; then
    echo "ERROR: Password file not found: $config_dir/db.pwd"
    return 1
  fi

  # 检查文件权限
  if [ "$(stat -c '%a' "$config_dir/sm4.key" 2>/dev/null || stat -f '%A' "$config_dir/sm4.key" 2>/dev/null)" != "600" ]; then
    echo "WARNING: Key file has insecure permissions"
    return 1
  fi

  echo "Config check: OK"
  return 0
}

check_config
```

### 2. 告警配置

#### 2.1 基于日志的告警

配置日志监控工具（如Logstash、Fluentd）监控关键错误：

```yaml
# Logstash 示例配置
input {
  file {
    path => "/var/log/myapp.log"
  }
}

filter {
  if [message] =~ /ERROR.*dbpwd/ {
    mutate {
      add_tag => ["dbpwd_error"]
    }
  }
}

output {
  if "dbpwd_error" in [tags] {
    email {
      to => "ops@example.com"
      subject => "dbpwd Error Alert"
      body => "Detected dbpwd error: %{message}"
    }
  }
}
```

#### 2.2 系统监控指标

监控以下关键指标：

- 数据库连接成功率
- 密码解密失败次数
- 配置文件加载时间
- 配置同步成功率

### 3. 故障处理流程

#### 3.1 密码解密失败

**症状**：应用启动时提示密码解密失败

**处理**：
1. 检查密钥文件是否存在：`ls -la ~/.dbpwd/sm4.key`
2. 检查密钥文件权限：`stat ~/.dbpwd/sm4.key`
3. 验证密钥和密码文件匹配：`java -jar dbpwd.jar test --all`
4. 如不匹配，从备份恢复或重新生成密钥并更新密码

#### 3.2 数据库连接失败

**症状**：应用无法连接数据库

**处理**：
1. 检查数据库服务是否运行
2. 检查网络连通性
3. 检查用户名密码是否正确
4. 使用dbpwd测试工具手动测试连接

#### 3.3 配置同步失败

**症状**：定时同步任务失败

**处理**：
1. 检查配置中心URL是否正确
2. 检查API Key是否有效
3. 检查网络连接
4. 查看同步日志获取详细错误信息

---

## 附录

### A. 推荐目录结构

```
/opt/
├── dbpwd/
│   ├── dbpwd.jar              # dbpwd工具JAR
│   ├── sync-dbpwd.sh          # 同步脚本
│   ├── backup/                # 备份目录
│   └── logs/                  # 日志目录
├── myapp/
│   ├── myapp.jar              # 应用JAR
│   └── config/                # 应用配置
```

### B. 系统服务配置示例

创建 `/etc/systemd/system/dbpwd.service`：

```ini
[Unit]
Description=Database Password Manager Service
After=network.target

[Service]
Type=simple
ExecStart=/opt/dbpwd/dbpwd.jar
WorkingDirectory=/opt/dbpwd
User=dbpwd
Group=dbpwd
Restart=on-failure
RestartSec=10

# 环境变量
Environment="DBPWD_ENABLED=true"
Environment="DBPWD_PASSWORD_FILE=/root/.dbpwd/db.pwd"
Environment="DBPWD_KEY_FILE=/root/.dbpwd/sm4.key"

[Install]
WantedBy=multi-user.target
```

启用服务：
```bash
sudo systemctl daemon-reload
sudo systemctl enable dbpwd.service
sudo systemctl start dbpwd.service
```

### C. 相关文档

- [用户使用手册](user-guide.md) - 用户操作指南
- [开发者文档](developer-guide.md) - 开发者参考
- [API文档](api-reference.md) - API参考手册
- [常见问题FAQ](FAQ.md) - 常见问题解答
