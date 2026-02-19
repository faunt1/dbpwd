# Servlet ContextListener 集成 Demo

> 集成模块: db-pwd-web (方案6: Servlet/Struts InitListener集成)
> 版本: 5.3
> 更新时间: 2026-02-12

## 概述

本Demo演示如何在Servlet/Struts应用中使用 `db-pwd-web` 模块，通过 `ServletContextListener` 自动初始化密码管理服务。支持纯Servlet应用、Struts 1.x应用和Struts 2.x应用。

## 功能演示

- **自动密码解密**: ENC(SM4-GCM:xxx) 格式密码自动解密
- **ServletContext初始化**: 在Web应用启动时初始化密码管理服务
- **REST API**: 提供完整的用户CRUD API接口
- **H2内存数据库**: 使用 H2 内存数据库，无需外部数据库
- **Druid连接池**: 使用 Druid 连接池管理数据库连接
- **纯Servlet**: 使用纯Servlet API，不依赖Spring框架

## 项目结构

```
db-pwd-servlet-demo/
├── src/main/java/com/dbpwd/demo/servlet/
│   ├── api/
│   │   └── UserApiServlet.java              # REST API Servlet
│   ├── dao/
│   │   └── UserDao.java                     # User数据访问对象
│   └── model/
│       └── User.java                        # User实体类
├── src/main/webapp/
│   ├── WEB-INF/
│   │   └── web.xml                          # Web配置文件
│   └── index.html                           # 欢迎页面
└── pom.xml                                  # Maven配置
```

## 运行Demo

### 1. 编译项目

```bash
# 安装核心模块和Web集成模块
mvn install -pl db-pwd-core,db-pwd-web -am -DskipTests

# 编译Demo
mvn compile -pl db-pwd-servlet-demo
```

### 2. 使用Tomcat Maven Plugin运行

```bash
# 运行Demo（默认端口8081）
mvn tomcat7:run -pl db-pwd-servlet-demo
```

### 2.1 使用Jetty Maven Plugin运行（推荐）

```powershell
# 后台启动Jetty（Windows PowerShell）
powershell -Command "Start-Process -FilePath 'cmd.exe' -ArgumentList '/c', 'mvn jetty:run -pl db-pwd-servlet-demo' -WorkingDirectory 'C:\gitprjs\dbpwd' -NoNewWindow"

# 等待服务器启动
ping -n 15 127.0.0.1 >nul

# 测试API
curl -s http://localhost:8081/dbpwd-servlet-demo/api/users

# 停止服务器
mvn jetty:stop -pl db-pwd-servlet-demo
```

### 3. 访问REST API

打开浏览器访问：`http://localhost:8081/dbpwd-servlet-demo/api/users`

或使用curl测试：

```bash
# 获取所有用户
curl http://localhost:8081/dbpwd-servlet-demo/api/users

# 获取指定ID的用户
curl http://localhost:8081/dbpwd-servlet-demo/api/users/1

# 创建用户
curl -X POST http://localhost:8081/dbpwd-servlet-demo/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"test_user","email":"test@example.com","password":"password123"}'

# 更新用户
curl -X PUT http://localhost:8081/dbpwd-servlet-demo/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{"username":"updated_user","email":"updated@example.com"}'

# 删除用户
curl -X DELETE http://localhost:8081/dbpwd-servlet-demo/api/users/1
```

### 4. 打包部署

```bash
# 打包为WAR文件
mvn package -pl db-pwd-servlet-demo

# WAR文件位置：db-pwd-servlet-demo/target/db-pwd-servlet-demo-1.0.0.war

# 部署到Tomcat
cp target/db-pwd-servlet-demo-1.0.0.war $TOMCAT_HOME/webapps/
```

## 输出示例

### 控制台输出

```
=========================================
   Database Password Manager - Servlet Demo
=========================================
Demo key file created: C:\Users\faunt\.dbpwd/demo_servlet_sm4.key
H2 password encrypted: ENC(SM4-GCM:wJI/4aKmPUP9/57v:ue8nB...
H2 password decrypted successfully
Users table initialized
  Initial data loaded: 3 users created
=========================================
   Servlet Demo initialized successfully!
   Access: http://localhost:8081/dbpwd-servlet-demo/api/users
=========================================
```

### API响应示例

```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "username": "john_doe",
      "email": "john@example.com",
      "password": "***",
      "status": 1
    },
    {
      "id": 2,
      "username": "jane_smith",
      "email": "jane@example.com",
      "password": "***",
      "status": 1
    },
    {
      "id": 3,
      "username": "bob_wilson",
      "email": "bob@example.com",
      "password": "***",
      "status": 1
    }
  ]
}
```

## 核心代码说明

### 1. web.xml配置

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app version="4.0">
    <!-- 配置DatabasePasswordInitListener -->
    <listener>
        <listener-class>com.dbpwd.web.DatabasePasswordInitListener</listener-class>
    </listener>

    <!-- 可选：自定义配置路径 -->
    <context-param>
        <param-name>dbpwd.keyFile</param-name>
        <param-value>/path/to/sm4.key</param-value>
    </context-param>

    <context-param>
        <param-name>dbpwd.pwdFile</param-name>
        <param-value>/path/to/db.pwd</param-value>
    </context-param>

    <context-param>
        <param-name>dbpwd.preload</param-name>
        <param-value>true</param-value>
    </context-param>
</web-app>
```

### 2. Servlet初始化

```java
@WebServlet("/api/users/*")
public class UserApiServlet extends HttpServlet {

    @Override
    public void init() throws ServletException {
        // 1. 生成SM4密钥（演示用）
        String sm4Key = Sm4KeyLoader.generateKey(demoKeyPath, true);

        // 2. 加密H2密码
        String encPassword = ENCFormat.formatSM4GCM(
            Sm4GCMCipher.encrypt(H2_PASSWORD, sm4Key)
        );

        // 3. 解密密码（演示流程）
        String plainPassword = decryptPassword(encPassword, sm4Key);

        // 4. 创建Druid数据源
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(H2_URL);
        dataSource.setUsername(H2_USERNAME);
        dataSource.setPassword(plainPassword);

        // 5. 创建UserDao
        userDao = new UserDao(dataSource);
        userDao.createTable();
    }
}
```

### 3. 使用WebPasswordUtil

```java
// 在Servlet中获取密码服务
WebPasswordService service = (WebPasswordService) getServletContext()
    .getAttribute(WebPasswordUtil.PASSWORD_SERVICE_KEY);

// 解密密码
String plainPassword = service.decryptPassword("ENC(SM4-GCM:...)");

// 获取数据库连接
Connection conn = service.getConnection("my_database");
```

## 配置说明

### 1. 密钥文件路径

- 默认路径: `~/.dbpwd/sm4.key`
- 自定义路径: 通过 `dbpwd.keyFile` context-param配置

### 2. 密码文件路径

- 默认路径: `~/.dbpwd/db.pwd`
- 自定义路径: 通过 `dbpwd.pwdFile` context-param配置

### 3. 预加载密码

- 默认: `true` - Web应用启动时预加载所有密码
- 可通过 `dbpwd.preload` context-param配置

## 技术栈

| 组件 | 版本 | 说明 |
|-----|------|------|
| JDK | 8+ | Java开发环境 |
| Servlet API | 4.0.1 | Web容器版本 |
| Druid | 1.2.20 | 数据库连接池 |
| H2 | 2.1.214 | 内存数据库（Demo用） |
| db-pwd-core | 1.0.0 | 核心加解密模块 |
| db-pwd-web | 1.0.0 | Web集成模块 |

## 与其他集成模块的对比

| 特性 | Servlet Demo | Spring Demo | Spring Boot Demo | MyBatis Demo | Druid Demo |
|-----|------------|------------|------------------|------------|------------|
| 集成方式 | InitListener | BeanPostProcessor | Auto Configuration | DataSourceFactory | PasswordCallback |
| 适用场景 | Servlet/Struts应用 | 传统Spring应用 | Spring Boot应用 | MyBatis应用 | Druid连接池应用 |
| 配置方式 | web.xml | Java/XML配置 | application.yml | XML/Java配置 | API调用 |
| 框架依赖 | Servlet API | Spring Framework | Spring Boot | MyBatis | Druid |

## 相关文档

- [Web集成模块 README](../db-pwd-web/README.md)
- [核心模块 README](../db-pwd-core/README.md)
- [集成场景和方案](../technical/integration-scenarios.md)

## 许可证

MIT License
