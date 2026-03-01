---
marp: true
theme: default
paginate: true
---

<style>
@import url('https://fonts.googleapis.com/css2?family=Noto+Sans+JP:wght@400;500;700&display=swap');

:root {
  --color-background: #ffffff;
  --color-foreground: #1f2937;
  --color-heading: #1e40af;
  --color-accent: #3b82f6;
  --color-border: #d1d5db;
  --color-success: #059669;
  --color-warning: #d97706;
  --font-default: 'Noto Sans JP', sans-serif;
}

section {
  background-color: var(--color-background);
  color: var(--color-foreground);
  font-family: var(--font-default);
  font-weight: 400;
  box-sizing: border-box;
  border-top: 8px solid var(--color-heading);
  position: relative;
  line-height: 1.7;
  font-size: 22px;
  padding: 56px;
}

h1, h2, h3, h4 {
  font-weight: 700;
  color: var(--color-heading);
  margin: 0;
  padding: 0;
}

h1 {
  font-size: 52px;
  line-height: 1.3;
  text-align: left;
}

h2 {
  position: absolute;
  top: 40px;
  left: 56px;
  right: 56px;
  font-size: 36px;
  padding-bottom: 16px;
  border-bottom: 3px solid var(--color-accent);
}

h2 + * {
  margin-top: 112px;
}

h3 {
  font-size: 24px;
  margin-top: 28px;
  margin-bottom: 12px;
}

ul, ol {
  padding-left: 32px;
}

li {
  margin-bottom: 10px;
}

footer {
  font-size: 14px;
  color: #6b7280;
  position: absolute;
  left: 56px;
  right: 56px;
  bottom: 40px;
}

section.lead {
  border-top: 8px solid var(--color-heading);
  display: flex;
  flex-direction: column;
  justify-content: center;
  background: linear-gradient(135deg, #ffffff 0%, #f3f4f6 100%);
}

section.lead footer {
  display: none;
}

section.lead h1 {
  margin-bottom: 24px;
}

table {
  border-collapse: collapse;
  width: 100%;
  margin: 20px 0;
  font-size: 18px;
}

th, td {
  border: 1px solid var(--color-border);
  padding: 12px 16px;
  text-align: left;
}

th {
  background-color: var(--color-heading);
  color: #ffffff;
}

tr:nth-child(even) {
  background-color: #f9fafb;
}

strong {
  color: var(--color-heading);
  font-weight: 700;
}

code {
  background-color: #f3f4f6;
  color: var(--color-heading);
  padding: 2px 6px;
  border-radius: 3px;
}

.alert {
  padding: 16px 20px;
  border-radius: 8px;
  margin: 16px 0;
  border-left: 4px solid;
}

.alert-info { background-color: #eff6ff; border-color: var(--color-accent); }
.alert-success { background-color: #ecfdf5; border-color: var(--color-success); }
.alert-warning { background-color: #fffbeb; border-color: var(--color-warning); }

.badge {
  display: inline-block;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  background-color: #dbeafe;
  color: #1e40af;
}

.columns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
}

.column {
  padding: 24px;
  background-color: #f9fafb;
  border-radius: 8px;
  border-left: 4px solid var(--color-accent);
}
</style>

<!-- _class: lead -->

# Code Agent编程模式


**落地实施探讨**

<span class="badge">2026年3月</span>

---

## 目录

- **01** Vibe Coding 是什么
- **02** 市场成熟度数据
- **03** CLI Code Agent 趋势数据
- **04** Vibe Coding vs IDE辅助编程
- **05** 国内外主流 Code Agent
- **06** 典型案例：数据库密码管理系统开发尝试
- **07** 传统模式 vs AI模式：从手册到自动化
- **08** 适用场景与方法论
- **19** 1000人团队算力需求分析
- **11** 公司级落地战略建议
- **12** FTC实施路线图

---

## Vibe Coding 定义

> "用自然语言描述你想要什么，让AI生成代码。你'顺从于感觉(surrender to the vibes)'，忘记了代码的存在。"
> — Andrej Karpathy, 2025年2月

### 核心特征

- **自然语言驱动**：用描述性语言表达需求，降低技术门槛
- **AI主导开发**：AI负责大部分编码工作，人类聚焦设计
- **快速迭代**：通过对话式提示不断调整，实现分钟级交付

### 来源与演进

- Andrej Karpathy 提出概念 (2025年2月)
- Salesforce 发布 Agentforce Vibes (2025年10月)
- Cognizant 发布企业Vibe Coding蓝图
- **2026年现状**：已成为主流研发范式，从辅助走向核心

---

## 市场成熟度 — 关键数据

### 全球采用率与效能提升

| 指标                 | 2025-2026数据           | 来源/备注                      |
| -------------------- | ----------------------- | ------------------------------ |
| **开发者日均使用率** | **92%** (美国开发者)    | 每日必用，非偶尔尝试           |
| **AI生成代码占比**   | **41%** (全年2560亿行)  | GitHub State of Octoverse 2025 |
| **企业渗透率**       | **75%** 财富500强已部署 | 企业级市场全面攻占             |
| **Claude Code提交占GitHub总共** | **4%**       | SemiAnalysis |

### Claude Code 爆发式增长 (Anthropic)

- **用户增速**: 发布后 **6个月** 内，周活跃开发者突破 **500万**
- **Token消耗**: 编程类 Token 占比从 15% 飙升至 **60%+** (2025 Q4)
- **企业签约**: **85%** 的 Fortune 100 企业在 2025 年内签署企业级协议
- **资本验证**: Anthropic 2025年总营收突破 **$100亿**, 估值跃升至 **$1800亿**
<div class="alert alert-success">
<strong>核心洞察</strong>：AI编程已从"工具升级"演变为"范式革命"。2026年，<strong>Claude Code 成为事实标准</strong>。
</div>

---


## Vibe Coding vs IDE辅助编程

### 核心差异

| 特征         | IDE辅助           | Vibe Coding           |
| ------------ | ----------------- | --------------------- |
| **角色定位** | AI是辅助工具      | AI是主导开发者        |
| **代码编写** | 开发者手动+AI补全 | AI完成大部分逻辑      |
| **交互方式** | 自动补全/片段     | 自然语言对话/任务指令 |
| **知识要求** | 精通编程语言语法  | 只需理解业务逻辑      |
| **产出效率** | 线性提升          | 指数级跃升            |

---

## 国内外主流 Code Agent

<div class="columns">

<div class="column">

| 产品                   | 厂商      | 定位       | 适用场景              |
| ---------------------- | --------- | ---------- | --------------------- |
| **Claude Code**        | Anthropic | 企业级首选 | 复杂重构、大型项目    |
| **OpenCode**           | 社区/开源 | 开放平台   | 私有化部署、定制SKILL |
| **Codex CLI**          | OpenAI    | 轻量级     | 快速脚本、原型        |
| **GitHub Copilot CLI** | GitHub    | 命令行增强 | Git工作流集成         |
| **Codeium**            | Codeium   | 免费方案   | 预算受限团队          |
| **Aider**              | 社区      | 结对编程   | Git深度集成           |

</div>

<div class="column">

| 产品                | 厂商     | 定位       | 核心优势     |
| ------------------- | -------- | ---------- | ------------ |
| **CodeFuse CLI**    | 蚂蚁集团 | 金融级     | 金融场景适配 |
| **文心快码**        | 百度     | 全栈智能   | 中文理解强   |
| **CodeGeeX CLI**    | 智谱AI   | 多语言     | 国产模型生态 |
| **iFlyCode CLI**    | 科大讯飞 | 信创环境   | 语音交互结合 |
| **华为云 CodeArts** | 华为云   | 全生命周期 | DevOps集成   |

</div>
</div>

---

## Claude Code 与OpenCode

<div class="columns">

<div class="column">


### Claude Code 核心能力

- **200k+上下文**：超长窗口处理大型项目架构
- **MCP支持**：标准化工具调用协议
- **Artifacts**：代码实时预览与运行
- **安全扫描**：内置漏洞检测与修复建议


</div>

<div class="column">

### OpenCode核心能力

- **多Agent编排**：并行/串行执行复杂任务链
- **多模型后端**：无缝切换 Qwen3.5 / GLM-5 / Minimax-M2.5
- **Skill系统**：自定义封装复用，沉淀组织资产
- **开源可部署**：完全自主可控，数据不出域

</div>

</div>


<div class="alert alert-info">
<strong>核心价值</strong>：不仅是工具，更是承载企业知识资产的智能操作系统，啥事都能干。
</div>

---

## 数据库密码管理(原型) --- 背景

**Database Password Manager** 是一个基于 SM4 国密算法的数据库密码管理系统，提供 **7 种** 核心集成模式，覆盖主流 Java 技术栈。核心目标是确保生产环境数据库密码不在代码库和配置文件中明文出现。

### 核心特性

- **SM4-GCM 认证加密**: 采用国密算法，支持认证加密防篡改
- **7 种集成模式**: Druid/Spring Boot/Spring/MyBatis/Servlet/纯JDBC/**零代码集成**
- **应用无感知**: 文件路径由 PasswordService 内部固定为 `~/.dbpwd/`
- **占位符格式**: 使用 `@db_name@` 格式，应用配置无需知道文件位置
- **零代码集成**: 通过 ServletContext 共享 DataSource，Action 代码完全不需要修改

### 密码安全需求

- 生产环境数据库密码不在代码库中出现
- 生产环境数据库密码不在开放测试环境的各类配置中出现
- 密码在传输、存储、使用过程中全程加密保护

---

## 数库密码管理(原型) --- 开发过程

### 开发模式

通过分层设计，形成7种集成模式，所有设计文档、代码都通过 OpenCode 大模型自主生成。

### 开发特点

- **全流程AI生成**：架构设计、核心代码、集成模块、Demo应用全部由 AI Agent 生成
- **单元测试覆盖**：所有代码都有自动生成的单元测试
- **集成验证**：通过 10+ 个典型场景应用进行集成测试，验证正确性
- **人机协作**：在专人控制下由代码智能体自主完成开发


---

## 数据库密码管理(原型) --- AI驱动开发全过程

<div class="columns">

<div class="column">

### 开发模式对比

| 维度 | 传统开发 | AI Agent开发 |
|------|----------|-------------|
| 架构设计 | 2-3天 | 30分钟 |
| 代码生成 | 逐行编写 | 批量生成 |
| 多框架适配 | 逐一学习 | AI自动匹配 |
| 测试验证 | 手动编写 | AI生成并执行 |

### 实际产出

- 核心模块：5个
- Demo应用：9个
- 代码行数：15,000+
- **耗时**：1人12天 (传统需2人月+)

</div>

<div class="column">

### 项目结构

```
db-pwd/
├── db-pwd-core/           # 核心加解密
├── db-pwd-druid/          # Druid集成
├── db-pwd-spring/         # Spring集成
├── db-pwd-spring-boot-starter/  # Spring Boot
├── db-pwd-mybatis/        # MyBatis集成
├── db-pwd-web/            # Servlet集成
└── demos/                 # Demo应用(9个)
```

### 7种集成模式

| 模式 | 适用场景 | 自动解密 |
|------|---------|----------|
| 方案1 | Druid连接池 | ✅ |
| 方案2 | Spring Boot | ✅ |
| 方案3 | 传统Spring | ✅ |
| 方案4 | MyBatis | ✅ |
| 方案5 | 纯JDBC | ❌ 手动 |
| 方案6 | Servlet/Web | ✅ |
| 方案7 | 零代码集成 | ✅ |

</div>

</div>

---

## 数据库密码管理(原型) -- 开发SKILL

<div class="columns">

<div class="column">

```
┌─────────────────────────────────────────────────────────┐
│                    db-pwd-integrator                     │
├─────────────────────────────────────────────────────────┤
│  Phase 1: 项目扫描                                       │
│  ├── glob: 查找 pom.xml, build.gradle                   │
│  ├── grep: 检测密码配置模式                              │
│  └── 分析: 识别技术栈类型                                │
├─────────────────────────────────────────────────────────┤
│  Phase 2: 类型识别 (优先级匹配)                          │
│  Spring Boot > Spring > MyBatis > Druid > Servlet       │
├─────────────────────────────────────────────────────────┤
│  Phase 3: 执行集成                                       │
│  ├── 添加 db-pwd-core 依赖                               │
│  ├── 替换明文密码为占位符                                │
│  └── 注入 PasswordService 调用                           │
├─────────────────────────────────────────────────────────┤
│  Phase 4: 验证构建                                       │
│  └── mvn compile 确保集成正确                            │
└─────────────────────────────────────────────────────────┘
```

</div>

<div class="column">


<!-- class: small -->

```
skills/
└── db-pwd-integrator/
    ├── SKILL.md                    # 主技能文件 (539行)
    └── references/
        ├── code-examples.md        # 代码修改示例 (505行)
        ├── integration-patterns.md  # 集成模式详解
        └── troubleshooting.md       # 问题排查指南
```

**文件职责：**
- `SKILL.md`: 核心逻辑、工作流程、用户交互模板
- `code-examples.md`: 6种集成模式的代码修改对照
- `integration-patterns.md`: 各技术栈的集成细节
- `troubleshooting.md`: 常见问题与解决方案

</div>
</div>

---


## 数据库密码管理(原型) --- SKILL自动化集成

<div class="columns">

<div class="column">

### db-pwd-integrator

用户输入 → "帮我集成数据库密码加密"
↓
AI扫描 → 检测项目类型
↓
AI推荐 → 匹配最佳方案(7选1)
↓
AI执行 → 添加依赖、修改配置
↓
AI验证 → 编译检查

</div>

<div class="column">

### SKILL能力

- ✅ 自动识别7种项目类型
- ✅ 自动选择最佳方案
- ✅ 自动生成代码修改
- ✅ 边界情况处理

</div>
</div>

---

## 传统模式 vs AI模式

<div class="columns">

<div class="column">

### 传统模式

| 环节     | 内容         | 现状             |
| -------- | ------------ | ---------------- |
| 框架开发 | 实现底层组件 | ✅                |
| 使用手册 | 提供集成文档 | ✅                |
| 集成实施 | 应用自行集成 | ⚠️ 效率低，易出错 |

</div>

<div class="column">


### AI新模式：用SKILL替代手册

| 对比     | 传统手册          | SKILL自动化       |
| -------- | ----------------- | ----------------- |
| 交互方式 | 文档阅读+手动操作 | 自然语言指令      |
| 学习成本 | 高 (需理解细节)   | 低 (描述需求即可) |
| 出错概率 | 高 (人为失误)     | 低 (标准化执行)   |
| 执行速度 | 天级              | 分钟级            |

</div>
</div>

---

## 自动化集成改造流程

<div class="columns">

<div class="column">

应用项目仓库
↓
AI拉取分支 (feature/xxx-integration)
↓
自动执行SKILL完成改造
↓
提交并添加标签
↓
应用项目组审核合并

</div>

<div class="column">

### 为什么适合自动化

- 影响范围小（以配置和少量代码为主）
- 改造文件应用通常不修改
- 分支模式可回滚，风险可控
- 标准化保证一致性

</div>
</div>

---

## 适用场景分析

### 适合 Vibe Coding

| 场景             | 适用程度 | 说明           |
| ---------------- | -------- | -------------- |
| 全新项目开发     | ⭐⭐⭐⭐⭐    | 从0到1最快     |
| 原型/MVP快速验证 | ⭐⭐⭐⭐⭐    | 极速迭代       |
| 技术改造类       | ⭐⭐⭐⭐     | 批量迁移/重构  |
| 标准CRUD开发     | ⭐⭐⭐⭐     | 模板化生成     |
| 遗留系统维护     | ⭐⭐       | 需人工深度介入 |

---

## 方法论：SKILL驱动技术改造

1. **AI生成改造辅助类** - 分析项目，生成定制工具
2. **基于场景试点改造** - 边缘系统验证，积累信心
3. **抽象为SKILL** - 将个人经验凝聚为可复用技能
4. **批量改造同类应用** - SKILL快速部署，规模化效应
5. **增量更新** - 持续扩大覆盖范围，自我进化

<div class="alert alert-info">
<strong>核心本质</strong>：Vibe Coding开发 = a.直接业务逻辑实现 + b.个人经验通过SKILL凝聚并使用
</div>

---

## SKILL增量更新机制

<div class="columns">

<div class="column">

新场景 → 人工介入分析
↓
生成新改造类 + 新场景改造
↓
增量更新SKILL
↓
覆盖范围扩大
↓
下一轮更智能

</div>

<div class="column">

### 人机分工新范式
- **设计、开发并行**：不再严格区分阶段
- **人的核心作用**：**控制方向**。重点在于如何有效控制大模型往需要的方向发展，防止幻觉偏离。
- **AI的核心作用**：高效执行、细节填充、批量处理。

</div>
</div>

---

### OpenCode 实际使用数据参考（dbpwd项目，1人12天）

| 指标           | 数值      | 说明          |
| -------------- | --------- | ------------- |
| 统计周期       | 12天      | 2026年2月     |
| 会话数         | 722       | 日均60个      |
| 总Input Tokens | 1.82亿    | 日均1516万    |
| 平均每会话     | 68万Token | 中位数2.6万   |
| 用户规模       | **1人**   | dbpwd项目开发 |

### 模型使用分布

| 模型                       | 消息数 | Input Tokens | Output Tokens | Cache Read |
| -------------------------- | ------ | ------------ | ------------- | ---------- |
| opencode/minimax-m2.5-free | 3,604  | 699万        | 210.9万       | 8.5M       |
| opencode/glm-5-free        | 1,324  | 97.1万       | 437.8K        | 64.8万     |
| faunt/z-ai/glm4.7          | 637    | 72.3万       | 199.6K        | 0          |

<div class="alert alert-warning">
<strong>说明</strong>：MiniMax M2.5 模型使用最多，Cache Read占比高说明上下文复用效果好。<br>
</div>

---

## 1000人团队Token需求估算

### 估算依据

- 人均日均Token: **1516万**（基于dbpwd项目实际数据）
- 计算公式: 1516万 × 1000人 × 强度系数

### Token需求

| 使用强度 | 系数 | 人均Token/天 | 1000人日均Token |
| -------- | ---- | ------------ | --------------- |
| 轻度     | 0.3x | 455万        | **45.5亿**      |
| 中度     | 1x   | 1516万       | **152亿**       |
| 重度     | 1.5x | 2274万       | **227亿**       |

<div class="alert alert-warning">
<strong>警示</strong>：Vibe Coding模式算力消耗巨大，必须提前规划充足算力。
</div>

---

## 落地挑战与应对

### 四大挑战

| 挑战         | 核心问题            | 应对策略                     |
| ------------ | ------------------- | ---------------------------- |
| **算力成本** | Token消耗指数级增加 | 按需选模型，预留冗余         |
| **模型能力** | 上下文限制/幻觉     | 选用最新满血模型，人工控向   |
| **质量控制** | 代码质量参差不齐    | 强制Code Review，SKILL标准化 |
| **研发流程** | 遗留兼容/习惯阻力   | 从上往下推广，典型场景突破   |

---

<!-- _class: lead -->
# 落地建议：公司级战略

## 拥抱Vibe Coding，构建智能研发新引擎

---

## 公司级落地建议 (1/2)

### 1. 战略定调：必须试点，全面推广
- **成熟度判断**：Vibe Coding已足够成熟，不再是“可选项”，而是“必选项”。
- **行动方针**：立即启动试点，验证成功后全公司推广。犹豫将导致研发效率代差。

### 2. 资源规划：算力即生产力
- **算力先行**：Vibe Coding高频交互、长上下文特性导致**算力消耗巨大**。
- **策略**：先期限制使用用户（如核心团队），但必须确保**算力储备充足**，避免体验中断。

### 3. 模型选型：唯新唯强
- **原则**：必须使用**最新的、满血非量化**模型。实践来看，版本差异导致产出质量鸿沟。
- **推荐梯队**：锁定 **Qwen3.5**、**GLM-5**、**Minimax-M2.5**。

---

## 公司级落地建议 (2/2)

### 4. 推广策略：自上而下，典型突破
- **领导先行**：**从上往下推广**，至少从团队长/架构师开始。管理者先用，消除团队顾虑。
- **场景切入**：挑选**典型应用**（如快速原型、遗留重构）作为第一批突破口，“真正去用，就有感觉了”。

### 5. 平台赋能：OpenCode定位
- **定位**：**开箱即用**的开放开发运行平台 + 大模型内积的广泛知识库。
- **目标**：实现“啥事都能干”的全能覆盖，降低上手门槛。

### 6. 核心方法论：人机协同新范式
- **开发本质**：
  - a. **直接业务逻辑实现** (AI主导)
  - b. **经验资产化** (将个人经验通过SKILL凝聚并使用)
- **人的作用**：设计与开发并行。重点在于**有效控制大模型往需要的方向发展**，人是方向的掌控者。

---

<!-- _class: lead -->
# 落地建议：FTC实施路线

## 试点先行，资产沉淀，迈向端到端智能交付

---

## FTC实施路线图 (1/2)

### 1. 试点启动：精准选点
- **行动**：各团队立即挑选 **1-2个项目** 试点用起来。
- **推荐场景**：
  - **数据库密码管理项目** (涉及安全规范与多模式适配)
  - **RPA信创改造项目** (涉及大量代码迁移与异构适配)

### 2. 实施策略：双管齐下
在试点过程中，必须同步完成两项核心任务：
- **任务A：基础工具智能化加工**
  - 利用大模型对基础工具进行加工，形成供项目组用的 **JAR包/SDK**。
  - 目标：标准化、开箱即用，减少重复造轮子。
- **任务B：SKILL沉淀**
  - 基于试点应用形成 **SKILL库**。
  - 目标：供项目组实际集成时直接调用，实现“一次成功，全员复用”。

---

## FTC实施路线图 (2/2)

### 3. 新系统设计：原生AI化 (通用平台等)
针对**通用平台**等全新系统设计，必须从架构层面充分考虑大模型能力，实现双重转型：
- **开发态转型 (Vibe Coding)**：
  - 全面采用 Vibe Coding 模式进行系统构建。利用 AI 快速生成骨架、业务逻辑及测试用例，缩短交付周期。
- **运行态转型 (Agent-Driven)**：
  - 架构转向**大模型驱动的开放框架**。参考**Claude Cowork**、**OpenClaw** (自动化运维) 等。

### 4. 演进目标：从辅助到全量承接
- **短期**：验证“人控方向+模型执行”模式，产出高质量组件与SKILL。
- **中长期**：**承接应用集成的全生命周期**。
  - 考虑将应用的集成完整的承接过来。真正实现 **端到端 (End-to-End)** 的全量基础组件的开发、使用、集成。

<div class="alert alert-success">
<strong>愿景</strong>：通过Vibe Coding与SKILL体系，实现研发效能的指数级跃升。
</div>

---

<!-- _class: lead -->

# Q&A

**感谢聆听**

