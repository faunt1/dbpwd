# Database Password Manager - 应用集成参考文档

> 本文档帮助用户根据应用类型选择正确的集成模式，并提供详细的代码修改指南。
>
> 最后更新: 2026-02-24

## 目录

1. [快速选择指南](#快速选择指南)
2. [集成机制原理](#集成机制原理)
3. [新集成机制开发流程](#新集成机制开发流程)
4. [集成模式详解](#集成模式详解)
5. [代码修改详解](#代码修改详解)
6. [常见问题](#常见问题)

---

## 快速选择指南

### 根据应用类型选择集成模式

| 应用类型 | 推荐集成模式 | 核心集成类 | 难度 |
|---------|------------|----------|-----|
| 纯JDBC应用 | 模式5: Standalone | `PasswordService` | ⭐ |
| Druid连接池 | 模式1: Druid PasswordCallback | `DbPwdDruidPasswordCallback` | ⭐⭐ |
| Spring Boot应用 | 模式2: Spring Boot Starter | `db-pwd-spring-boot-starter` | ⭐ |
| Spring Boot (编程方式) | 模式2变体: 编程API | `PasswordService` | ⭐⭐ |
| Spring (Java Config) | 模式3: Spring BeanPostProcessor | `DataSourcePasswordProcessor` | ⭐⭐ |
| Spring (XML Config) | 模式3: Spring BeanPostProcessor | `DataSourcePasswordProcessor` | ⭐⭐ |
| MyBatis | 模式4: 自定义集成 | `PasswordService` | ⭐⭐ |
| Servlet/Struts应用 | 模式6: Servlet ContextListener | `DatabasePasswordInitListener` | ⭐⭐ |
| Servlet/Struts应用 (共享DataSource) | 模式7: ServletContext共享DataSource | `DbPwdStrutsInitializer` | ⭐ |

## 集成机制原理

### 核心思路

密码集成的本质是**找到框架获取密码的"薄弱环节"**，利用框架提供的扩展点在密码被使用前完成解密。

**两种基本思路**：

1. **配置层面**：将配置文件读取密码替换或接管
2. **代码层面**：
   - 优选：替换整个对象（工厂类/装饰器）
   - 次选：直接修改原始代码调用解密 API

### 5 种集成机制

| # | 机制 | 拦截点 | 典型实现 |
|---|------|--------|----------|
| 1 | 回调接口替换 | 框架调用获取密码时 | Druid `PasswordCallback` |
| 2 | Bean 后置处理 | Bean 初始化完成后 | Spring `BeanPostProcessor` |
| 3 | 工厂类替换 | DataSource 创建时 | MyBatis `DataSourceFactory` |
| 4 | 配置源装饰 | 属性读取时 | Spring Boot `PropertySource` |
| 5 | 装饰器模式 | `getConnection()` 调用时 | `DataSourceDecorator` |

#### 机制 1：回调接口替换（Druid）

```
DruidDataSource.getConnection() 
    → 调用 PasswordCallback.getPassword()  ← 拦截点
    → 获取密码用于创建连接
```

继承 `DruidPasswordCallback`，重写 `getPassword()` 在此处解密占位符。

#### 机制 2：Bean 后置处理（Spring）

```
Spring 容器创建 DataSource Bean
    → BeanPostProcessor.postProcessAfterInitialization()  ← 拦截点
```

实现 `BeanPostProcessor`，在回调中通过反射获取/设置密码后解密。

#### 机制 3：工厂类替换（MyBatis）

```
MyBatis 创建 SqlSessionFactory
    → DataSourceFactory.getDataSource()  ← 拦截点
```

实现 `DataSourceFactory` 接口，在 `getDataSource()` 中解密密码后设置。

#### 机制 4：配置源装饰（Spring Boot）

```
Spring Boot 启动 → Environment 加载配置
    → 应用读取属性时 → 装饰器的 getProperty()  ← 拦截点
```

装饰 `PropertySource`，拦截属性读取，检测 password 相关属性后解密返回。

#### 机制 5：装饰器模式（零代码集成）

```
应用启动 → Listener 创建 DataSource
    → DataSourceDecorator.getConnection()  ← 拦截点（首次调用）
```

启动时用装饰器包装 DataSource，应用获取连接时首次自动解密。

### 机制选择决策树

```
需要集成的应用是什么框架？
│
├─ Druid 连接池
│   └─ 使用机制 1：PasswordCallback
│
├─ Spring + XML/注解配置
│   └─ 使用机制 2：BeanPostProcessor
│
├─ Spring Boot
│   ├─ 只需 DataSource → 机制 2：BeanPostProcessor
│   └─ 需要其他加密配置 → 机制 4：PropertySource 装饰
│
├─ MyBatis
│   └─ 使用机制 3：DataSourceFactory
│
└─ Struts/Servlet
    ├─ Action 从 ServletContext 获取 DataSource
    │   └─ 使用机制 5：装饰器（零代码）
    │
    └─ Action 自己创建 DataSource
        └─ 使用 PasswordService API（需改少量代码）
```

**优先原则**：优先使用框架原生扩展点，只有在没有扩展点时才考虑修改代码。

---

## 新集成机制开发流程

当需要为新框架开发集成方案时，按以下流程进行：

### 流程概览

```
┌─────────────────────────────────────────────────────────────────┐
│  步骤1: 分析获取密码环节                                         │
│  找出应用/框架 获取数据库密码的位置和方式                         │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  步骤2: 寻找可替换环节                                           │
│  分析该框架是否提供扩展点（回调/装饰器/后置处理器/工厂接口）       │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  步骤3: 确定集成策略                                             │
│  是否需要生成集成辅助对象？生成什么类型的辅助对象？                 │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  步骤4: 开发与测试                                               │
│  实现集成辅助类，编写 Demo 验证有效性                            │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  步骤5: 更新 SKILL                                              │
│  将新集成方案添加到 db-pwd-integrator Skill                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓
┌─────────────────────────────────────────────────────────────────┐
│  步骤6: 自动化集成                                               │
│  使用 SKILL 完成实际应用的集成                                    │
└─────────────────────────────────────────────────────────────────┘
```

### 步骤详解

#### 步骤1: 分析获取密码环节

**问题**：应用从哪里获取数据库密码？

**分析方法**：
1. 找到创建 DataSource 的代码位置
2. 追踪 `setPassword()` 方法的调用链
3. 确定密码的来源（配置文件、代码常量、环境变量等）

**常见来源**：
- 配置文件：`application.yml`、`db.properties`
- 代码常量：`private static final String PASSWORD = "xxx"`
- 环境变量：`System.getenv("DB_PASSWORD")`
- 框架自动读取：Spring/Druid/MyBatis 框架内部读取

#### 步骤2: 寻找可替换环节

**问题**：框架是否提供扩展点？

**扩展点类型**：
| 扩展点类型 | 框架示例 | 说明 |
|-----------|---------|------|
| 回调接口 | Druid `PasswordCallback` | 框架在获取密码时调用回调 |
| 装饰器 | `DataSourceDecorator` | 包装原始对象，拦截方法调用 |
| 后置处理器 | Spring `BeanPostProcessor` | Bean 初始化后拦截 |
| 工厂接口 | MyBatis `DataSourceFactory` | 替换工厂类创建对象 |
| 配置源 | Spring Boot `PropertySource` | 装饰配置读取 |

**决策树**：
```
框架是否提供扩展点？
│
├─ 是 → 使用扩展点机制（推荐）
│   ├─ 回调接口 → 实现回调接口
│   ├─ 装饰器 → 实现装饰器模式
│   ├─ 后置处理器 → 实现 BeanPostProcessor
│   ├─ 工厂接口 → 实现工厂接口
│   └─ 配置源 → 装饰 PropertySource
│
└─ 否 → 考虑以下方案
    ├─ 应用是否从统一入口获取 DataSource？
    │   ├─ 是 → 使用装饰器 + ContextListener（零代码）
    │   └─ 否 → 进入下一步
    └─ 只能修改原始代码
        → 使用 PasswordService API
```

#### 步骤3: 确定集成策略

**问题**：需要生成什么样的集成辅助对象？

**分析维度**：
1. **替换目标**：是替换密码获取逻辑，还是替换整个 DataSource？
2. **集成层次**：在框架层（推荐）还是应用层？
3. **改动量**：能否实现零代码/低代码修改？

**辅助对象类型**：
| 类型 | 适用场景 | 示例 |
|------|---------|------|
| 回调类 | 框架提供回调接口 | `DbPwdDruidPasswordCallback` |
| 装饰器类 | 需要透明替换 | `DataSourceDecorator` |
| 后置处理器 | Spring 容器 | `DataSourcePasswordProcessor` |
| 工厂类 | 框架使用工厂创建 | `DbPwdDataSourceFactory` |
| 配置装饰器 | 属性读取时解密 | `DbPwdEncryptablePropertySource` |
| 初始化器 | Web 应用启动时 | `DbPwdStrutsInitializer` |

#### 步骤4: 开发与测试

**开发步骤**：

1. **创建集成模块**（如 `db-pwd-xxx`）
   ```
   db-pwd-xxx/
   ├── pom.xml
   └── src/main/java/com/dbpwd/xxx/
       └── XxxIntegrationHelper.java
   ```

2. **实现集成辅助类**
   - 根据选择的扩展点类型实现对应接口
   - 内部调用 `PasswordService` 获取解密密码
   - 处理各种边界情况（缓存、异常、日志）

3. **创建 Demo 验证**
   ```
   demos/xxx-demo/
   ├── original/    # 原始版本（集成前）
   └── demo/       # 集成版本（集成后）
   ```

4. **测试验证**
   - 单元测试：验证辅助类本身
   - 集成测试：验证 Demo 能正常连接数据库
   - 对比测试：确保集成前后行为一致

5. **验证流程（带标签管理）**

   为确保集成方案经过完整验证，采用标签管理：

   ```
   validation/                          # 验证目录
   └── xxx-demo/                        # 某框架的验证项目
       ├── validation-xxx-before/       # 集成前版本（打标签）
       └── validation-xxx-after/       # 集成后版本（打标签）
   ```

   **验证步骤**：

   | 阶段 | 操作 | 标签 |
   |------|------|------|
   | 1. 集成前 | 创建验证项目，验证原始应用能正常工作 | `validation-xxx-before` |
   | 2. 集成前 | 提交代码并打标签 | `git tag validation-xxx-before` |
   | 3. 使用 SKILL | 执行自动化集成 | - |
   | 4. 集成后 | 验证集成后应用能正常工作 | - |
   | 5. 集成后 | 提交代码并打标签 | `git tag validation-xxx-after` |

   **标签命名规范**：
   ```
   validation-{框架名}-{集成方案}
   
   示例：
   - validation-druid-passwordcallback
   - validation-springboot-starter
   - validation-mybatis-factory
   - validation-servlet-zerocode
   ```

   **验证检查项**：
   - [ ] 集成前：`mvn test` 通过，数据库连接正常
   - [ ] 集成后：`mvn test` 通过，数据库连接正常
   - [ ] 密码确认：密码文件中不存在明文密码
   - [ ] 功能确认：原有业务功能不受影响

**示例**：为新框架 `XxxDataSource` 开发集成

```java
// 1. 分析：XxxDataSource.setPassword() 在获取连接时被调用
// 2. 寻找扩展点：发现 XxxDataSource 支持 PasswordCallback 接口
// 3. 确定策略：实现 XxxPasswordCallback 回调类

public class DbPwdXxxPasswordCallback extends XxxPasswordCallback {
    @Override
    public char[] getPassword() {
        // 从原始 DataSource 获取占位符
        String placeholder = dataSource.getPassword();
        // 调用 PasswordService 解密
        return PasswordService.getDatabasePassword(placeholder).toCharArray();
    }
}
```

#### 步骤5: 更新 SKILL

将新集成方案添加到 `db-pwd-integrator` Skill：

1. **更新决策树**：添加新框架的识别逻辑
   ```
   ELSE IF XxxDataSource:
       → 方案N: db-pwd-xxx (XxxPasswordCallback)
   ```

2. **更新依赖表**：添加需要的依赖
   ```
   | 方案N: Xxx | db-pwd-core + db-pwd-xxx | 两个依赖都需要 |
   ```

3. **添加代码修改规则**：新框架的修改示例
   ```
   // 方案N: Xxx 配置
   XxxDataSource dataSource = new XxxDataSource();
   dataSource.setPassword("@my_db@");
   dataSource.setPasswordCallback(new DbPwdXxxPasswordCallback());
   ```

4. **更新文档引用**：在 `references/integration-patterns.md` 中添加说明

#### 步骤6: 自动化集成

使用更新后的 SKILL 完成实际集成：

```
用户: 帮我集成 Xxx 框架的项目

SKILL 响应:
1. 扫描项目，识别为 Xxx 框架
2. 匹配方案N: db-pwd-xxx
3. 添加依赖：db-pwd-core + db-pwd-xxx
4. 修改代码：设置 PasswordCallback
5. 验证编译
```

### 开发 Checklist

- [ ] 步骤1：分析获取密码环节，绘制调用链
- [ ] 步骤2：确认框架提供的扩展点类型
- [ ] 步骤3：确定集成辅助对象类型
- [ ] 步骤4：实现集成辅助类
- [ ] 步骤4：创建 Demo（original + demo）
- [ ] 步骤4：创建 validation 验证项目
- [ ] 步骤4：集成前验证并打 `validation-xxx-before` 标签
- [ ] 步骤4：使用 SKILL 集成
- [ ] 步骤4：集成后验证并打 `validation-xxx-after` 标签
- [ ] 步骤5：更新 SKILL 决策树
- [ ] 步骤5：更新 SKILL 依赖表
- [ ] 步骤5：更新 SKILL 代码规则
- [ ] 步骤6：使用 SKILL 完成集成

---

## 集成模式详解

### 模式1: Druid PasswordCallback

**适用场景**: 使用Druid连接池的应用

**集成原理**: Druid的PasswordCallback机制在获取连接时自动调用回调函数解密密码

**核心类**: `com.dbpwd.druid.DbPwdDruidPasswordCallback`

---

### 模式2: Spring Boot Starter

**适用场景**: Spring Boot应用

**集成原理**: 自动配置DataSource的BeanPostProcessor

**核心类**: `com.dbpwd.spring.DbPwdAutoConfiguration`

---

### 模式3: Spring BeanPostProcessor

**适用场景**: 传统Spring应用（Java Config或XML Config）

**集成原理**: BeanPostProcessor在DataSource初始化后自动解密密码

**核心类**: `com.dbpwd.spring.DataSourcePasswordProcessor`

---

### 模式4: MyBatis集成

**适用场景**: 纯MyBatis应用（无Spring依赖）

**集成原理**: 在构建SqlSessionFactory时，使用PasswordService API获取密码

**核心类**: `com.dbpwd.core.PasswordService`

---

### 模式5: Standalone (纯JDBC)

**适用场景**: 不使用任何框架的纯JDBC应用

**集成原理**: 直接调用PasswordService API

**核心类**: `com.dbpwd.core.PasswordService`

---

### 模式6: Servlet ContextListener

**适用场景**: Servlet/Struts等传统Web应用

**集成原理**: ServletContextListener在容器启动时初始化密码服务

**核心类**: `com.dbpwd.web.DatabasePasswordInitListener`

### 模式7: ServletContext共享DataSource集成

**适用场景**: Struts/Servlet应用，Action代码已从ServletContext获取DataSource

**集成原理**: 通过ServletContext共享装饰后的DataSource,实现真正的零代码修改

**核心类**: `com.dbpwd.web.DbPwdStrutsInitializer`

**特点**:
- Action代码完全不需要修改
- 使用相同的 `context.getAttribute("dataSource")` 获取DataSource
- Listener初始化时自动配置装饰后的DataSource
- 适用于已有ServletContext共享DataSource的Struts/Servlet应用


---

## 代码修改详解


---

## 一、模式5: 纯JDBC集成

### 1.1 适用场景

- 不使用任何框架的纯JDBC应用
- 需要完全控制密码获取过程

### 1.2 目录结构变化

```
无新增文件，仅修改:
├── pom.xml                          [修改]
└── src/main/java/.../XxxDemo.java   [修改]
```

### 1.3 POM文件修改

**添加依赖**:
```xml
<!-- 添加核心依赖 -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 1.4 Java代码修改

**原始代码**:
```java
public class SimpleJdbcDemo {
    // 明文密码常量
    private static final String DB_PASSWORD = "sa";

    public static void main(String[] args) {
        // 直接使用明文密码
        String plainPassword = DB_PASSWORD;
        Connection conn = DriverManager.getConnection(url, username, plainPassword);
    }
}
```

**集成后代码**:
```java
// 添加import
import com.dbpwd.core.PasswordService;

public class SimpleJdbcDemo {
    // 改为占位符常量
    private static final String DB_PLACEHOLDER = "simple_jdbc_db";

    public static void main(String[] args) {
        // 使用PasswordService获取解密后的密码
        String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
        Connection conn = DriverManager.getConnection(url, username, plainPassword);
    }
}
```

**关键改动点**:
1. 添加 `import com.dbpwd.core.PasswordService;`
2. 将密码常量从明文 `"sa"` 改为占位符 `"simple_jdbc_db"`
3. 使用 `PasswordService.getDatabasePassword()` 获取密码

---

## 二、模式1: Druid PasswordCallback集成

### 2.1 适用场景

- 使用Druid连接池的应用
- 希望密码解密过程自动化

### 2.2 目录结构变化

```
无新增文件，仅修改:
├── pom.xml                          [修改]
└── src/main/java/.../XxxApp.java    [修改]
```

### 2.3 POM文件修改

**添加依赖**:
```xml
<!-- 添加核心依赖 -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 添加Druid集成依赖 -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-druid</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2.4 Java代码修改

**原始代码**:
```java
public class DruidDemoApplication {
    public static void main(String[] args) {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:file:...");
        dataSource.setUsername("sa");
        
        // 直接设置明文密码
        String password = "sa";
        dataSource.setPassword(password);
        
        dataSource.setDriverClassName("org.h2.Driver");
    }
}
```

**集成后代码**:
```java
// 添加import
import com.dbpwd.druid.DbPwdDruidPasswordCallback;

public class DruidDemoApplication {
    public static void main(String[] args) {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:file:...");
        dataSource.setUsername("sa");
        
        // 设置占位符格式密码
        dataSource.setPassword("@druid_demo_db@");
        dataSource.setDriverClassName("org.h2.Driver");

        // 配置PasswordCallback - 核心集成步骤
        DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback();
        callback.setDataSource(dataSource);  // 关键：让callback能获取原始密码
        dataSource.setPasswordCallback(callback);
    }
}
```

**关键改动点**:
1. 添加 `import com.dbpwd.druid.DbPwdDruidPasswordCallback;`
2. 密码从明文 `"sa"` 改为占位符 `"@druid_demo_db@"`
3. 创建 `DbPwdDruidPasswordCallback` 并设置到DataSource
4. **重要**: 调用 `callback.setDataSource(dataSource)`

### 2.5 工作原理

1. DataSource的password设置为占位符 `@druid_demo_db@`
2. Druid在获取连接时自动调用 `callback.getPassword()`
3. Callback从DataSource获取原始密码(占位符)
4. Callback内部调用 `PasswordService.getDatabasePassword()` 解密
5. 返回明文密码给Druid

---

## 三、模式1+Spring: Druid + Spring XML集成

### 3.1 适用场景

- 使用Druid连接池 + Spring XML配置
- 希望通过配置文件管理DataSource

### 3.2 目录结构变化

```
修改文件:
├── pom.xml                              [修改]
├── src/main/resources/druid-datasource.xml  [修改]
└── src/main/java/.../XxxApplication.java    [修改]
```

### 3.3 POM文件修改

```xml
<!-- 添加核心和Druid集成依赖 -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-druid</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 3.4 XML配置修改

**原始XML**:
```xml
<bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource">
    <property name="url" value="jdbc:h2:file:..."/>
    <property name="username" value="sa"/>
    <!-- 明文密码 -->
    <property name="password" value="sa"/>
</bean>
```

**集成后XML**:
```xml
<bean id="dataSource" class="com.alibaba.druid.pool.DruidDataSource">
    <property name="url" value="jdbc:h2:file:..."/>
    <property name="username" value="sa"/>
    <!-- 占位符格式密码 -->
    <property name="password" value="@druid_demo_spring_db@"/>
</bean>
```

### 3.5 Java代码修改

**原始代码**:
```java
public class DruidSpringDemoApplication {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = 
            new ClassPathXmlApplicationContext("druid-datasource.xml");
        DruidDataSource dataSource = context.getBean(DruidDataSource.class);
        // 直接使用，密码已在XML中配置为明文
    }
}
```

**集成后代码**:
```java
// 添加import
import com.dbpwd.druid.DbPwdDruidPasswordCallback;

public class DruidSpringDemoApplication {
    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = 
            new ClassPathXmlApplicationContext("druid-datasource.xml");
        DruidDataSource dataSource = context.getBean(DruidDataSource.class);
        
        // 配置PasswordCallback
        DbPwdDruidPasswordCallback callback = new DbPwdDruidPasswordCallback();
        callback.setDataSource(dataSource);
        dataSource.setPasswordCallback(callback);
    }
}
```

**关键改动点**:
1. XML中password从 `"sa"` 改为 `"@druid_demo_spring_db@"`
2. Java代码中创建并设置PasswordCallback

---

## 四、模式2: Spring Boot Starter集成

### 4.1 适用场景

- Spring Boot应用
- 希望零配置自动集成

### 4.2 目录结构变化

```
修改文件:
├── pom.xml                              [修改]
└── src/main/resources/application.properties  [可选修改]
```

### 4.3 POM文件修改

**原始POM**:
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
</dependencies>
```

**集成后POM**:
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-jdbc</artifactId>
    </dependency>
    <!-- 添加Starter依赖 -->
    <dependency>
        <groupId>com.dbpwd</groupId>
        <artifactId>db-pwd-spring-boot-starter</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

### 4.4 配置文件修改

**原始application.properties**:
```properties
spring.datasource.url=jdbc:h2:file:...
spring.datasource.username=sa
spring.datasource.password=sa
```

**集成后application.properties**:
```properties
spring.datasource.url=jdbc:h2:file:...
spring.datasource.username=sa
# 使用占位符格式密码
spring.datasource.password=@spring_boot_db@
```

### 4.5 Java代码修改

**无需修改Java代码！**Starter会自动配置。

**可选**: 如果需要手动配置DataSource:

**原始代码**:
```java
@Bean
public DataSource dataSource() {
    DriverManagerDataSource ds = new DriverManagerDataSource();
    ds.setPassword("sa");  // 明文密码
    return ds;
}
```

**集成后代码**:
```java
// 添加import
import com.dbpwd.core.PasswordService;

@Bean
public DataSource dataSource() {
    DriverManagerDataSource ds = new DriverManagerDataSource();
    String password = PasswordService.getDatabasePassword("@spring_boot_db@");
    ds.setPassword(password);
    return ds;
}
```

---

## 五、模式3: Spring BeanPostProcessor集成

### 5.1 适用场景

- 传统Spring应用（非Boot）
- Java Config或XML Config

### 5.2 Java Config方式

#### 目录结构变化
```
修改文件:
├── pom.xml                              [修改]
└── src/main/java/.../DemoConfig.java    [修改]
```

#### POM文件修改
```xml
<!-- 添加核心和Spring集成依赖 -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-spring</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### Java代码修改

**原始代码**:
```java
@Configuration
public class DemoConfig {
    @Bean
    public DataSource dataSource() {
        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:h2:file:...");
        ds.setUsername("sa");
        ds.setPassword("sa");  // 明文密码
        ds.setDriverClassName("org.h2.Driver");
        return ds;
    }
}
```

**集成后代码**:
```java
// 添加import
import com.dbpwd.core.PasswordService;

@Configuration
public class DemoConfig {

    private static final String DB_PLACEHOLDER = "@spring_framework_db@";

    @Bean
    public DataSource dataSource() {
        // 使用PasswordService获取密码
        String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);

        DruidDataSource ds = new DruidDataSource();
        ds.setUrl("jdbc:h2:file:...");
        ds.setUsername("sa");
        ds.setPassword(plainPassword);  // 解密后的密码
        ds.setDriverClassName("org.h2.Driver");
        return ds;
    }
}
```

**关键改动点**:
1. 添加 `import com.dbpwd.core.PasswordService;`
2. 定义占位符常量 `DB_PLACEHOLDER`
3. 使用 `PasswordService.getDatabasePassword()` 获取密码

### 5.3 XML Config方式

#### 目录结构变化
```
修改文件:
├── pom.xml                                  [修改]
└── src/main/resources/applicationContext.xml [修改]
```

#### POM文件修改
```xml
<!-- 添加核心和Spring集成依赖 -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-spring</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### XML配置修改

**原始XML**:
```xml
<bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource">
    <property name="driverClassName" value="org.h2.Driver"/>
    <property name="url" value="jdbc:h2:file:..."/>
    <property name="username" value="sa"/>
    <!-- 明文密码 -->
    <property name="password" value="sa"/>
</bean>
```

**集成后XML**:
```xml
<!-- 添加DataSourcePasswordProcessor Bean - 核心步骤 -->
<bean class="com.dbpwd.spring.DataSourcePasswordProcessor"/>

<bean id="dataSource" class="org.springframework.jdbc.datasource.DriverManagerDataSource">
    <property name="driverClassName" value="org.h2.Driver"/>
    <property name="url" value="jdbc:h2:file:..."/>
    <property name="username" value="sa"/>
    <!-- 占位符格式密码 - BeanPostProcessor会自动解密 -->
    <property name="password" value="@spring_demo_xml_db@"/>
</bean>
```

**关键改动点**:
1. 添加 `DataSourcePasswordProcessor` Bean声明（必须在DataSource之前）
2. 将password从 `"sa"` 改为 `"@spring_demo_xml_db@"`

### 5.4 工作原理

1. Spring容器启动，创建 `DataSourcePasswordProcessor` Bean
2. `DataSourcePasswordProcessor` 实现 `BeanPostProcessor` 接口
3. 每个DataSource Bean初始化后，Processor检查password属性
4. 如果password是占位符格式（以`@`开头和结尾），调用 `PasswordService` 解密
5. 将解密后的明文密码设置回DataSource

---

## 六、模式2变体: Spring Boot API集成

### 6.1 适用场景

- Spring Boot应用，需要完全控制DataSource配置
- 不想使用自动配置
- 需要在配置过程中执行额外逻辑

### 6.2 目录结构变化

```
修改文件:
├── pom.xml                                    [修改]
└── src/main/java/.../XxxApplication.java      [修改]
```

### 6.3 POM文件修改

**添加依赖**:
```xml
<!-- 仅添加核心依赖，不需要Starter -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 6.4 Java代码修改

**原始代码**:
```java
@SpringBootApplication
public class SpringBootApiDemoApplication {

    @Bean
    public DataSource dataSource() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:file:...");
        dataSource.setUsername("sa");
        dataSource.setPassword("sa");  // 明文密码
        return dataSource;
    }
}
```

**集成后代码**:
```java
// 添加import
import com.dbpwd.core.PasswordService;

@SpringBootApplication
public class SpringBootApiDemoApplication {

    private static final String DB_PLACEHOLDER = "@spring_boot_api_db@";

    @Bean
    public DataSource dataSource() {
        // 使用PasswordService API解密密码
        String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:file:...");
        dataSource.setUsername("sa");
        dataSource.setPassword(plainPassword);
        return dataSource;
    }
}
```

**关键改动点**:
1. 添加 `import com.dbpwd.core.PasswordService;`
2. 定义占位符常量 `DB_PLACEHOLDER`
3. 使用 `PasswordService.getDatabasePassword()` 获取密码

### 6.5 与Starter方式的区别

| 特性 | Starter方式 | API方式 |
|-----|-----------|--------|
| 配置方式 | application.properties | Java代码 |
| 依赖模块 | db-pwd-spring-boot-starter | db-pwd-core |
| 自动配置 | 是 | 否 |
| 控制程度 | 低 | 高 |
| 适用场景 | 常规应用 | 复杂配置需求 |

---

## 七、模式4: MyBatis集成

### 7.1 适用场景

- 纯MyBatis应用（无Spring依赖）
- 需要手动构建SqlSessionFactory

### 7.2 目录结构变化

```
修改文件:
├── pom.xml                                    [修改]
└── src/main/java/.../MybatisDemoApplication.java  [修改]
```

### 7.3 POM文件修改

**添加依赖**:
```xml
<!-- 添加核心依赖 -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 7.4 Java代码修改

**原始代码**:
```java
public class MybatisDemoApplication {

    private static final String H2_PASSWORD = "sa";  // 明文密码常量

    public static void main(String[] args) throws Exception {
        SqlSessionFactory sqlSessionFactory = buildSqlSessionFactory(H2_PASSWORD);
        // ...
    }

    private static SqlSessionFactory buildSqlSessionFactory(String password) throws Exception {
        DataSource dataSource = new UnpooledDataSource();
        ((UnpooledDataSource) dataSource).setUrl("jdbc:h2:file:...");
        ((UnpooledDataSource) dataSource).setUsername("sa");
        ((UnpooledDataSource) dataSource).setPassword(password);
        ((UnpooledDataSource) dataSource).setDriver("org.h2.Driver");
        // ...
    }
}
```

**集成后代码**:
```java
// 添加import
import com.dbpwd.core.PasswordService;

public class MybatisDemoApplication {

    private static final String DB_PLACEHOLDER = "mybatis_demo_db";  // 占位符

    public static void main(String[] args) throws Exception {
        // 使用PasswordService获取密码
        String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);
        SqlSessionFactory sqlSessionFactory = buildSqlSessionFactory(plainPassword);
        // ...
    }

    private static SqlSessionFactory buildSqlSessionFactory(String password) throws Exception {
        DataSource dataSource = new UnpooledDataSource();
        ((UnpooledDataSource) dataSource).setUrl("jdbc:h2:file:...");
        ((UnpooledDataSource) dataSource).setUsername("sa");
        ((UnpooledDataSource) dataSource).setPassword(password);
        ((UnpooledDataSource) dataSource).setDriver("org.h2.Driver");
        // ...
    }
}
```

**关键改动点**:
1. 添加 `import com.dbpwd.core.PasswordService;`
2. 将密码常量从 `"sa"` 改为占位符 `"mybatis_demo_db"`
3. 使用 `PasswordService.getDatabasePassword()` 获取密码

---

## 八、模式6: Servlet ContextListener集成

### 8.1 适用场景

- Servlet/Struts等传统Web应用
- 使用web.xml配置

### 8.2 目录结构变化

```
修改文件:
├── pom.xml                                    [修改]
├── src/main/webapp/WEB-INF/web.xml            [修改]
└── src/main/java/.../UserApiServlet.java      [修改]
```

### 8.3 POM文件修改

**添加依赖**:
```xml
<!-- 添加核心依赖 -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 添加Web集成依赖 -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-web</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 8.4 web.xml配置修改

**原始web.xml**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app version="4.0">
    <display-name>Servlet Demo</display-name>
    <!-- 无Listener配置 -->
</web-app>
```

**集成后web.xml**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app version="4.0">
    <display-name>Servlet Demo</display-name>

    <!-- 配置DatabasePasswordInitListener - 核心步骤 -->
    <listener>
        <listener-class>com.dbpwd.web.DatabasePasswordInitListener</listener-class>
    </listener>

    <!-- 可选：自定义配置路径 -->
    <!--
    <context-param>
        <param-name>dbpwd.keyFile</param-name>
        <param-value>/path/to/sm4.key</param-value>
    </context-param>
    <context-param>
        <param-name>dbpwd.pwdFile</param-name>
        <param-value>/path/to/db.pwd</param-value>
    </context-param>
    -->
</web-app>
```

### 8.5 Servlet代码修改

**原始代码**:
```java
@WebServlet("/api/users/*")
public class UserApiServlet extends HttpServlet {

    @Override
    public void init() throws ServletException {
        String plainPassword = "sa";  // 明文密码

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:file:...");
        dataSource.setUsername("sa");
        dataSource.setPassword(plainPassword);
        // ...
    }
}
```

**集成后代码**:
```java
// 添加import
import com.dbpwd.core.PasswordService;

@WebServlet("/api/users/*")
public class UserApiServlet extends HttpServlet {

    private static final String DB_PLACEHOLDER = "@servlet_demo_db@";

    @Override
    public void init() throws ServletException {
        // 使用PasswordService获取密码
        String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:file:...");
        dataSource.setUsername("sa");
        dataSource.setPassword(plainPassword);
        // ...
    }
}
```

**关键改动点**:
1. web.xml添加 `DatabasePasswordInitListener`
2. 添加 `import com.dbpwd.core.PasswordService;`
3. 使用 `PasswordService.getDatabasePassword()` 获取密码

---

## 九、Struts 1.x集成

### 9.1 适用场景

- Struts 1.x传统Web应用
- 维护旧系统参考

### 9.2 目录结构变化

```
修改文件:
├── pom.xml                                    [修改]
└── src/main/java/.../UserListAction.java      [修改]
```

### 9.3 POM文件修改

**添加依赖**:
```xml
<!-- 添加核心依赖 -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 9.4 Action代码修改

**原始代码**:
```java
public class UserListAction extends Action {

    private static final String H2_PASSWORD = "sa";  // 明文密码常量

    @Override
    public ActionForward execute(...) throws Exception {
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:file:...");
        dataSource.setUsername("sa");
        dataSource.setPassword(H2_PASSWORD);
        // ...
    }
}
```

**集成后代码**:
```java
// 添加import
import com.dbpwd.core.PasswordService;

public class UserListAction extends Action {

    private static final String DB_PLACEHOLDER = "@struts_demo_db@";  // 占位符

    @Override
    public ActionForward execute(...) throws Exception {
        // 使用PasswordService获取密码
        String plainPassword = PasswordService.getDatabasePassword(DB_PLACEHOLDER);

        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl("jdbc:h2:file:...");
        dataSource.setUsername("sa");
        dataSource.setPassword(plainPassword);
        // ...
    }
}
```

**关键改动点**:
1. 添加 `import com.dbpwd.core.PasswordService;`
2. 将密码常量从 `"sa"` 改为占位符 `"@struts_demo_db@"`
3. 使用 `PasswordService.getDatabasePassword()` 获取密码

## 十一、模式7: ServletContext共享DataSource集成

### 11.1 适用场景

- Struts 1.x / Servlet 应用
- Action 代码已经从 ServletContext 获取 DataSource
- 希望实现真正的零代码修改集成

### 11.2 核心特点

**与标准 Servlet 集成的区别**:

| 特性 | 标准Servlet集成 (模式6) | ServletContext共享DataSource (模式7) |
|-----|---------------|---------------|
| Action代码改动 | 需要修改 | **完全不需要修改** |
| DataSource获取方式 | ServletContext.getAttribute | ServletContext.getAttribute |
| Listener类 | DatabasePasswordInitListener | DbPwdStrutsInitializer |
| 集成模式 | init()时手动解密 | 装饰器自动解密 |
### 11.3 目录结构变化

```
修改文件:
├── pom.xml                                    [修改]
└── src/main/webapp/WEB-INF/web.xml            [修改]

无需修改:
└── src/main/java/.../*.java                   [ACTION代码完全不变]
```

### 11.4 POM文件修改

**添加依赖**:
```xml
<!-- 添加核心依赖 -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-core</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 添加Web集成依赖 -->
<dependency>
    <groupId>com.dbpwd</groupId>
    <artifactId>db-pwd-web</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 11.5 web.xml配置修改

**原始web.xml**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app version="2.5">
    <display-name>Struts Demo</display-name>
    
    <!-- 原始初始化器 (明文密码) -->
    <listener>
        <listener-class>com.dbpwd.demo.struts.zerocode.DataSourceInitializer</listener-class>
    </listener>
</web-app>
```

**集成后web.xml**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app version="2.5">
    <display-name>Struts Demo</display-name>
    
    <!-- ServletContext共享DataSource初始化器 (自动解密) -->
    <listener>
        <listener-class>com.dbpwd.web.DbPwdStrutsInitializer</listener-class>
    </listener>
    
    <!-- 配置数据库占位符 -->
    <context-param>
        <param-name>dbpwd.databases</param-name>
        <param-value>@struts_original_db@</param-value>
    </context-param>
</web-app>
```

**关键改动点**:
1. 将 `DataSourceInitializer` 替换为 `DbPwdStrutsInitializer`
2. 添加 `dbpwd.databases` context-param，配置数据库占位符

### 11.6 动作代码 - **无需修改**

**原始代码** (与集成后代码完全相同):
```java
public class UserListAction extends Action {
    
    @Override
    public ActionForward execute(ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response) throws Exception {
        
        // 从ServletContext获取DataSource - 完全不需要修改
        ServletContext servletContext = this.getServlet().getServletContext();
        DataSource dataSource = (DataSource) servletContext.getAttribute("dataSource");
        
        try (Connection conn = dataSource.getConnection()) {
            // 执行查询...
        }
        
        return mapping.findForward("success");
    }
}
```

### 11.7 工作原理

```
容器启动
    ↓
web.xml 加载 DbPwdStrutsInitializer
    ↓
contextInitialized() 被调用
    ↓
读取 dbpwd.databases 参数: @struts_original_db@
    ↓
从 ~/.dbpwd/db.pwd 文件读取数据库配置
    ↓
使用 PasswordService 解析占位符，获取明文密码
    ↓
创建 DataSource 并设置明文密码
    ↓
使用 DataSourceDecorator 装饰 DataSource
    ↓
将装饰后的 DataSource 存入 ServletContext.getAttribute("dataSource")
    ↓
Action 代码正常调用 context.getAttribute("dataSource")
    ↓
DataSourceDecorator 在getConnection时自动解密
```

### 11.8 为什么能零代码修改？

**关键设计**:
1. **相同的属性名**: 两个版本都使用 `"dataSource"` 作为ServletContext属性名
2. **装饰器模式**: 集成版本使用 `DataSourceDecorator` 包装原始DataSource
3. **透明解密**: 装饰器在 `getConnection()` 时自动解密，对Action完全透明

**对比**:

```java
// 原始 DataSourceInitializer 的工作方式
public void contextInitialized(...) {
    DataSource ds = new DruidDataSource();
    ds.setPassword("sa");  // 明文密码
    servletContext.setAttribute("dataSource", ds);
}

// DbPwdStrutsInitializer 的工作方式
public void contextInitialized(...) {
    DataSource ds = new DruidDataSource();
    ds.setPassword("@struts_original_db@");  // 占位符
    
    // 关键：装饰DataSource
    DataSource decoratedDs = new DataSourceDecorator(ds);
    servletContext.setAttribute("dataSource", decoratedDs);
}
```

**DataSourceDecorator 核心逻辑**:
```java
@Override
public Connection getConnection() throws SQLException {
    // 在获取连接时自动解密密码
    String encryptedPassword = targetDataSource.getPassword();
    String plainPassword = PasswordService.decrypt(encryptedPassword);
    // 使用解密后的密码建立连接
    return targetDataSource.getConnection();
}
```

### 11.9 与其他模式的对比

| 集成模式 | 代码修改量 | 配置修改 | 难度 |
|---------|----------|---------|-----|
| 模式1: Druid | 小 | 中等 | ⭐⭐ |
| 模式2: Spring Boot Starter | 小 | 小 | ⭐ |
| 模式3: Spring BeanPostProcessor | 小 | 中等 | ⭐⭐ |
| 模式4: MyBatis | 中等 | 小 | ⭐⭐ |
| 模式5: Standalone | 中等 | 无 | ⭐⭐ |
| 模式6: Servlet ContextListener | 中等 | 小 | ⭐⭐ |
| 模式7: ServletContext共享DataSource | **无** | **小** | **⭐** |

### 11.10 使用建议

**推荐使用ServletContext共享DataSource的场景**:
- Struts 1.x 应用，Action 已从 ServletContext 获取 DataSource
- 传统 Servlet 应用，使用 ServletContext 共享 DataSource
- 希望最小化代码改动，降低集成风险
- 维护老旧应用，不想修改核心业务代码

**不推荐使用的场景**:
- 应用未使用 ServletContext 共享 DataSource
- 需要在代码中动态配置 DataSource
- Spring Boot / Spring Framework 应用 (有更好的集成方式)

---


---

## 十、依赖汇总表

| 集成模式 | 核心依赖 | 集成依赖 | 总依赖数 |
|---------|---------|---------|---------|
| 模式5: 纯JDBC | db-pwd-core | - | 1 |
| 模式1: Druid | db-pwd-core | db-pwd-druid | 2 |
| 模式2: Spring Boot Starter | db-pwd-spring-boot-starter | - | 1 |
| 模式2变体: Spring Boot API | db-pwd-core | - | 1 |
| 模式3: Spring | db-pwd-core | db-pwd-spring | 2 |
| 模式4: MyBatis | db-pwd-core | - | 1 |
| 模式6: Servlet | db-pwd-core | db-pwd-web | 2 |
| Struts | db-pwd-core | - | 1 |

---

## 十一、常见问题

### Q1: 占位符格式是什么？

**格式**: `@db_name@`

- 必须以 `@` 开头和结尾
- `db_name` 对应 `~/.dbpwd/db.pwd` 文件中的数据库名称
- 示例: `@spring_boot_db@`, `@mybatis_demo_db@`

### Q2: 密码文件在哪里？

- 默认路径: `~/.dbpwd/db.pwd` (用户主目录下的隐藏目录)
- 密钥文件: `~/.dbpwd/sm4.key`
- 应用无需知道文件位置，由 `PasswordService` 内部处理

### Q3: 如何选择集成模式？

| 你的应用类型 | 推荐模式 | 理由 |
|------------|---------|-----|
| Spring Boot新项目 | 模式2: Starter | 零配置，最简单 |
| Spring Boot + 复杂配置 | 模式2变体: API | 完全控制 |
| 传统Spring应用 | 模式3: BeanPostProcessor | 自动解密 |
| Druid连接池 | 模式1: PasswordCallback | 原生支持 |
| MyBatis纯应用 | 模式4: API | 直接调用 |
| Servlet/Struts | 模式6: ContextListener | 容器初始化 |
| 纯JDBC/工具类 | 模式5: Standalone | 最灵活 |

### Q4: 自动解密 vs 手动解密有什么区别？

| 方式 | 自动解密 | 手动解密 |
|-----|---------|---------|
| 配置位置 | 配置文件中使用占位符 | 代码中调用API |
| 代码改动 | 小 | 中等 |
| 适用模式 | 模式1/2/3/6 | 模式4/5 |
| 示例 | `password=@db@` | `PasswordService.getDatabasePassword("@db@")` |

### Q5: 如何生成加密密码？

使用CLI工具或API:

```bash
# CLI方式
java -jar db-pwd-cli.jar encrypt --password "yourPassword"
```

```java
// API方式
import com.dbpwd.core.PasswordService;

// 解密
String plain = PasswordService.decrypt("ENC(SM4-GCM:xxx)");
```

### Q6: 文件权限要求？

| 文件 | 权限 | 说明 |
|-----|-----|-----|
| `~/.dbpwd/` | 700 | 目录权限 |
| `~/.dbpwd/sm4.key` | 600 | 密钥文件 |
| `~/.dbpwd/db.pwd` | 600 | 密码文件 |

Windows环境无需设置，Linux/Mac环境建议设置:
```bash
chmod 700 ~/.dbpwd
chmod 600 ~/.dbpwd/sm4.key ~/.dbpwd/db.pwd
```

---

## 附录: 完整集成检查清单

### 集成前检查

- [ ] 确认应用类型（Spring Boot/Spring/Servlet等）
- [ ] 选择正确的集成模式
- [ ] 准备好 `~/.dbpwd/sm4.key` 和 `~/.dbpwd/db.pwd` 文件

### 集成步骤

- [ ] 添加Maven依赖
- [ ] 修改配置文件（XML/properties）
- [ ] 修改Java代码
- [ ] 测试连接

### 集成后验证

- [ ] 应用启动无报错
- [ ] 数据库连接成功
- [ ] 业务功能正常
- [ ] 配置文件中无明文密码

---

> 文档版本: 2.0
> 最后更新: 2026-02-24