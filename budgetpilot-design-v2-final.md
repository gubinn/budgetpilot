# BudgetPilot —— 个人预算管理系统设计文档

> **版本**: v2.0（定稿）  
> **日期**: 2026-04-15  
> **定位**: 替代 Actual Budget，部署于家庭 Linux 服务器  
> **关键决策**: 月度起始日=每月1日 | 通知渠道=Telegram Bot | v1.0 即支持多币种

---

## 1. 系统概述

### 1.1 核心目标

| 目标 | 说明 |
|------|------|
| 消费记录 | 快速录入日常消费，支持多账户、多币种、多分类、标签、附件 |
| 预算管控 | 按月/按分类设定预算（本位币 CNY），实时追踪执行率 |
| 统计报表 | 月度/年度消费趋势、分类占比、同比环比（统一换算本位币） |
| 智能预警 | 预算超支、异常消费、周期账单提醒，通过 Telegram Bot 实时推送 |
| 多币种 | 多币种账户和交易，自动汇率换算，双金额存储（原始 + 本位币） |
| 可扩展 | 账户表、交易表均提供 JSON 扩展字段，无需改表即可承载新业务属性 |
| 数据主权 | 全部数据留在家庭服务器，流量不出境 |

### 1.2 技术选型

```json
{
  "backend": {
    "framework": "Spring Boot 3.3.x",
    "language": "Java 21",
    "orm": "MyBatis-Plus 3.5.x",
    "api_style": "RESTful（/api/v1/ 前缀）",
    "auth": "Sa-Token 1.39.x"
  },
  "database": {
    "primary": "MySQL 8.0（JSON 字段 + 虚拟列索引）",
    "cache": "Redis 7.x（已有实例复用）"
  },
  "frontend": {
    "framework": "Vue 3 + Vite",
    "ui": "Naive UI",
    "chart": "ECharts 5",
    "mobile": "PWA（Service Worker + 添加到主屏幕）"
  },
  "notification": {
    "channel": "Telegram Bot API",
    "library": "java-telegram-bot-api 7.x",
    "fallback": "App 内通知（alert_log 表）"
  },
  "currency": {
    "base_currency": "CNY",
    "exchange_rate_source": "ExchangeRate-API（免费档，每日 1500 次）",
    "update_frequency": "每日 08:00 定时拉取",
    "supported": "CNY, USD, EUR, GBP, JPY, HKD, SGD, THB, KRW"
  },
  "deploy": {
    "container": "Docker Compose",
    "proxy": "Nginx（已有，反向代理）",
    "network": "Tailscale 内网，不暴露公网",
    "server": "家庭 Linux 服务器（i7-8550U / 16GB）"
  }
}
```

**选型理由**：

1. **MySQL 而非 SQLite**：已有实例，复用降低运维成本；JSON 字段 + 虚拟列索引提供结构化查询与灵活扩展的平衡。
2. **MyBatis-Plus 而非 JPA**：报表模块涉及大量聚合 SQL，MyBatis 对复杂查询的控制力更强。
3. **Vue 3 + PWA 而非原生 App**：一套代码覆盖桌面和 iPhone，PWA 在 iOS 16.4+ 已支持推送通知。
4. **Telegram Bot 而非邮件/微信**：Telegram 不受大陆推送限制（你有科学上网环境），API 简单可靠，支持 Markdown 富文本。
5. **双金额策略而非实时换算**：交易记录同时存 `amount`（原始币种）和 `amount_base`（CNY），预算和报表直接用 `amount_base` 聚合，避免汇率波动导致历史数据失真。

---

## 2. 系统架构

### 2.1 整体架构

```
                        ┌─────────────────┐
                        │  Telegram Bot   │
                        │   (推送通知)     │
                        └────────▲────────┘
                                 │
┌──────────────────────────────────────────────────────────┐
│                     Nginx (反向代理)                       │
│              budget.tailnet.ts.net / :443                  │
├──────────────┬────────────────────────────────────────────┤
│   Vue 3 SPA  │              Spring Boot API               │
│  (静态资源)   │                                            │
│              │  ┌─────────┐ ┌──────────┐ ┌─────────────┐ │
│              │  │ 交易模块 │ │ 预算模块  │ │   报表模块   │ │
│              │  └────┬────┘ └────┬─────┘ └──────┬──────┘ │
│              │       │          │               │        │
│              │  ┌────┴──────────┴───────────────┴─────┐  │
│              │  │            Service Layer             │  │
│              │  │  ┌──────────┐ ┌──────────┐          │  │
│              │  │  │ 汇率服务  │ │ 预警引擎  │──→ TG Bot│  │
│              │  │  └────┬─────┘ └──────────┘          │  │
│              │  └───────┼─────────────────────────────┘  │
│              │          │                                │
│              │  ┌───────┴───┐  ┌────────┐               │
│              │  │   MySQL   │  │ Redis  │               │
│              │  │ (数据+汇率) │  │ (缓存)  │               │
│              │  └───────────┘  └────────┘               │
└──────────────┴────────────────────────────────────────────┘
            ↓ 备份
     ┌──────────────┐
     │ NAS (QNAP)   │
     │ mysqldump/日  │
     └──────────────┘
```

### 2.2 模块划分

```
budgetpilot/
├── src/main/java/uk/gubin/budgetpilot/
│   ├── BudgetPilotApplication.java
│   ├── common/             # 公共基础
│   │   ├── Result.java          # 统一响应包装
│   │   ├── PageResult.java      # 分页响应
│   │   ├── ErrorCode.java       # 错误码枚举
│   │   ├── BizException.java    # 业务异常
│   │   ├── BaseEntity.java      # 基础实体（id, createdAt, updatedAt）
│   │   └── GlobalExceptionHandler.java
│   ├── config/             # 配置类
│   │   └── AppConfig.java       # MP分页 + 自动填充 + 异步线程池
│   ├── entity/             # 数据实体
│   │   ├── Account.java
│   │   ├── Transaction.java
│   │   ├── Category.java
│   │   ├── Merchant.java
│   │   ├── CurrencyRate.java
│   │   ├── Budget.java         # 注意：yearMonth字段需用@TableField("`year_month`")
│   │   ├── BudgetItem.java
│   │   ├── RecurringRule.java
│   │   ├── AlertRule.java
│   │   └── AlertLog.java       # 注意：不继承BaseEntity，表中无created_at/updated_at
│   ├── dto/                # 请求参数
│   │   ├── AccountCreateDTO.java
│   │   ├── AccountUpdateDTO.java
│   │   ├── BalanceAdjustDTO.java
│   │   ├── TransactionCreateDTO.java
│   │   ├── TransactionUpdateDTO.java
│   │   ├── TransactionQueryDTO.java
│   │   ├── CategoryCreateDTO.java
│   │   ├── MerchantCreateDTO.java
│   │   ├── MerchantUpdateDTO.java
│   │   ├── MerchantQueryDTO.java
│   │   ├── BudgetCreateDTO.java
│   │   ├── BudgetUpdateDTO.java
│   │   ├── RecurringRuleCreateDTO.java
│   │   ├── RecurringRuleUpdateDTO.java
│   │   ├── AlertRuleCreateDTO.java
│   │   └── AlertRuleUpdateDTO.java
│   ├── vo/                 # 响应视图
│   │   ├── AccountVO.java
│   │   ├── TransactionVO.java
│   │   ├── CategoryVO.java
│   │   ├── MerchantVO.java
│   │   ├── BudgetProgressVO.java
│   │   ├── RecurringRuleVO.java
│   │   ├── AlertRuleVO.java
│   │   └── ReportVO.java
│   ├── mapper/             # 持久层
│   ├── service/            # 业务层
│   │   └── impl/
│   ├── controller/         # RESTful 接口层
│   └── event/              # 事件驱动（异步预算更新 + 预警检查）
├── src/main/resources/
│   ├── application.yml
│   └── mapper/*.xml
├── sql/
│   ├── schema.sql
│   └── init_data.sql
├── pom.xml
├── Dockerfile
└── docker-compose.yml
```

---

## 3. 数据库设计

### 3.1 ER 关系概览

```
                    currency_rate
                         │
account ──1:N── transaction ──N:1── category
   │                 │                 │
   │                 │                 │
   └── currency      │                 │
                     │                 │
budget ──1:N── budget_item ──1:1── category

transaction ──N:1── merchant

alert_rule ──1:N── alert_log ──→ Telegram Bot

recurring_rule ──1:N── transaction (auto-generated)

config (系统配置键值对)
```

### 3.2 核心设计原则：扩展字段体系

在进入具体表结构之前，先说明贯穿 `t_account` 和 `t_transaction` 两张核心表的扩展字段设计。

#### 3.2.1 为什么需要扩展字段？

个人记账需求是渐进式发现的：今天你只需要金额和分类，明天可能想记录商户名、地理位置、聚餐参与人；信用卡账户未来可能需要记录积分兑换比例。如果每次新增属性都要 ALTER TABLE，维护成本高且停机风险大。

#### 3.2.2 双 JSON 字段策略

每张核心表提供两个 JSON 类型的扩展列：

| 字段 | 归属 | 用途 | 写入者 | 典型内容 |
|------|------|------|--------|----------|
| `ext_fields` | 业务层 | 承载用户可感知的业务扩展属性 | 用户/前端 | 商户、地点、参与人、积分 |
| `metadata` | 系统层 | 承载系统内部的技术元数据 | 后端服务 | 导入来源、设备信息、原始币种、审计痕迹 |

**分离的好处**：
1. `ext_fields` 可以直接在前端表单中展示和编辑，用户自由扩展。
2. `metadata` 对用户不可见，系统自行维护，不会被用户误改。
3. 两个字段可以独立演进，互不干扰。

#### 3.2.3 ext_fields 使用示例

**账户表 ext_fields**：

```json
// 储蓄卡
{"bank_name": "招商银行", "card_last4": "8888", "branch": "深圳南山支行"}

// 信用卡
{"bank_name": "中信银行", "card_last4": "6666", "points_ratio": 1.5, "annual_fee": 600}

// 电子钱包
{"bindPhone": "138****1234", "bindEmail": "bin@gubin.uk"}

// 投资账户
{"broker": "富途证券", "account_type": "港股通"}
```

**交易表 ext_fields**：

```json
// 日常消费
{"merchant": "星巴克", "location": "深圳南山科技园"}

// 聚餐
{"merchant": "海底捞", "participants": ["张三", "李四"], "per_person": 125.50}

// 出差
{"project": "杭州客户对接", "reimbursable": true, "invoice_no": "INV-2026-0042"}

// 境外消费
{"merchant": "Amazon.co.uk", "original_amount": 29.99, "original_currency": "GBP"}
```

#### 3.2.4 metadata 使用示例

```json
// 手动录入
{"source": "manual", "device": "iPhone 16 Pro Max", "app_version": "1.0.0"}

// CSV 批量导入
{"source": "csv_import", "import_batch": "20260415_001", "original_row": 42}

// 周期交易自动生成
{"source": "recurring", "rule_id": 7, "auto_confirmed": false}

// 汇率相关（系统自动填充）
{"rate_source": "exchangerate-api", "rate_date": "2026-04-15"}
```

#### 3.2.5 JSON 字段查询能力

MySQL 8.0 的 JSON 函数支持对扩展字段的结构化查询：

```sql
-- 精确查询：查找商户为"星巴克"的所有交易
SELECT * FROM t_transaction
WHERE JSON_UNQUOTE(JSON_EXTRACT(ext_fields, '$.merchant')) = '星巴克';

-- 数组包含：查找张三参与的聚餐
SELECT * FROM t_transaction
WHERE JSON_CONTAINS(ext_fields, '"张三"', '$.participants');

-- 布尔过滤：查找可报销的交易
SELECT * FROM t_transaction
WHERE JSON_EXTRACT(ext_fields, '$.reimbursable') = true;

-- 数值范围：查找人均超过 200 的聚餐
SELECT * FROM t_transaction
WHERE JSON_EXTRACT(ext_fields, '$.per_person') > 200;

-- 标签过滤（tags 字段也是 JSON 数组）
SELECT * FROM t_transaction
WHERE JSON_CONTAINS(tags, '"日常"');
```

**性能优化**：对高频查询的 JSON 路径，可创建虚拟生成列 + 索引：

```sql
-- 为 merchant 创建虚拟列索引（按需添加，不急于 v1.0）
ALTER TABLE t_transaction
ADD COLUMN ext_merchant VARCHAR(100) GENERATED ALWAYS AS
    (JSON_UNQUOTE(JSON_EXTRACT(ext_fields, '$.merchant'))) VIRTUAL,
ADD INDEX idx_ext_merchant (ext_merchant);
```

#### 3.2.6 API 层如何处理扩展字段

```
创建时：前端传入 ext_fields / metadata JSON 对象，后端直接持久化
更新时：整体替换（PUT 语义），前端负责合并逻辑
查询时：TransactionQueryDTO 提供 extKey + extValue 参数，映射为 JSON_EXTRACT
响应时：VO 直接透传 Map<String, Object>，前端自由渲染
```

---

### 3.3 表结构定义

#### 3.3.1 账户表 `t_account`

管理资金来源：现金、银行卡、信用卡、支付宝、微信、投资账户等。

```sql
CREATE TABLE t_account (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(50)     NOT NULL COMMENT '账户名称（唯一）',
    type            TINYINT         NOT NULL COMMENT '1-现金 2-储蓄卡 3-信用卡 4-电子钱包 5-投资账户',
    icon            VARCHAR(50)     DEFAULT 'wallet' COMMENT '图标标识',
    currency        CHAR(3)         DEFAULT 'CNY' COMMENT '账户币种 ISO 4217',
    initial_balance DECIMAL(14,2)   DEFAULT 0.00 COMMENT '初始余额（账户币种）',
    current_balance DECIMAL(14,2)   DEFAULT 0.00 COMMENT '当前余额（冗余，定时校准）',
    credit_limit    DECIMAL(14,2)   DEFAULT NULL COMMENT '信用额度（信用卡专用）',
    billing_day     TINYINT         DEFAULT NULL COMMENT '账单日 1-28（信用卡专用）',
    payment_day     TINYINT         DEFAULT NULL COMMENT '还款日 1-28（信用卡专用）',
    sort_order      INT             DEFAULT 0 COMMENT '排序权重，越小越靠前',
    is_active       TINYINT(1)      DEFAULT 1,
    ext_fields      JSON            DEFAULT NULL COMMENT '业务扩展字段',
    metadata        JSON            DEFAULT NULL COMMENT '系统元数据',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_type (type),
    INDEX idx_active_sort (is_active, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='账户表';
```

**设计要点**：

- `currency` 决定该账户下交易的默认币种。USD 账户记录的交易默认 `currency=USD`。
- `initial_balance` 字段已弃用，仅保留用于兼容旧数据。新建账户余额默认为 0，用户需通过记录收入交易来初始化余额。
- `current_balance` 以账户币种计，随交易实时变动。支持手动调整，但调整会创建交易记录留痕。
- `credit_limit / billing_day / payment_day` 仅信用卡类型使用，用于还款提醒预警。
- `ext_fields` 典型用途：银行名称、卡尾号、支行信息、积分兑换比例。
- `metadata` 典型用途：UI 配色、导入来源。

**余额调整机制**：

为确保账本完整性，账户余额调整遵循以下规则：

| 操作 | 说明 |
|------|------|
| 新建账户 | 余额默认为 0，不提供余额输入 |
| 初始化余额 | 用户记录一笔收入交易（如工资、转账） |
| 手动调整余额 | 编辑账户时修改余额，系统自动创建交易记录并要求填写调整原因 |
| 调增余额 | 创建收入类型交易，分类为"余额调整" |
| 调减余额 | 创建支出类型交易，分类为"余额调整" |

API：`POST /api/v1/accounts/{id}/adjust-balance`，参数 `{newBalance, reason}`。

#### 3.3.2 分类表 `t_category`

两级分类树：大类 → 子类（如 餐饮 → 外卖/聚餐/食材/饮品）。

```sql
CREATE TABLE t_category (
    id          BIGINT          PRIMARY KEY AUTO_INCREMENT,
    parent_id   BIGINT          DEFAULT 0 COMMENT '父分类ID，0=顶级',
    name        VARCHAR(30)     NOT NULL COMMENT '分类名称',
    type        TINYINT         NOT NULL COMMENT '1-支出 2-收入',
    icon        VARCHAR(50)     DEFAULT NULL,
    color       CHAR(7)         DEFAULT NULL COMMENT '#RRGGBB',
    sort_order  INT             DEFAULT 0,
    is_system   TINYINT(1)      DEFAULT 0 COMMENT '系统预设，不可删除',
    is_active   TINYINT(1)      DEFAULT 1,
    created_at  DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_parent (parent_id),
    INDEX idx_type_active (type, is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收支分类表';
```

**预设分类**（`is_system=1`，系统初始化时写入）：

```
支出（type=1）
├── 餐饮：外卖、聚餐、食材、饮品
├── 交通：公交地铁、打车、加油、停车
├── 住房：房租/房贷、水电燃气、物业、维修
├── 购物：日用品、服饰、数码、家具家电
├── 医疗：门诊、药品、体检、保险
├── 教育：课程、书籍、考试
├── 娱乐：电影、游戏、旅行、运动
├── 社交：礼金、聚会
├── 金融：利息、手续费、投资亏损
├── 其他支出
└── 余额调整（id=59，type=1支出类型，账户余额调减专用）

收入（type=2）
├── 工资：基本工资、奖金、补贴
├── 副业：兼职、稿费、咨询
├── 投资：股息、利息、理财收益
├── 报销
├── 其他收入
└── 余额调整（id=58，type=2收入类型，账户余额调增专用）

特殊
└── 内部转账（type=1, 转账交易专用）
```

> **余额调整分类说明**：这两个分类是系统预设的，专门用于账户余额手动调整时自动创建的交易记录。用户不能删除或修改其类型。余额调增时创建收入交易(type=2, categoryId=58)，余额调减时创建支出交易(type=1, categoryId=59)。

#### 3.3.3 交易记录表 `t_transaction`

核心流水表。每笔收支一条记录，采用**双金额策略**支持多币种。

```sql
CREATE TABLE t_transaction (
    id                  BIGINT          PRIMARY KEY AUTO_INCREMENT,
    type                TINYINT         NOT NULL COMMENT '1-支出 2-收入 3-转账',
    amount              DECIMAL(14,2)   NOT NULL COMMENT '原始金额（原始币种，正数）',
    currency            CHAR(3)         DEFAULT 'CNY' COMMENT '原始币种 ISO 4217',
    amount_base         DECIMAL(14,2)   NOT NULL COMMENT '本位币金额（CNY）',
    exchange_rate       DECIMAL(12,6)   DEFAULT 1.000000 COMMENT '汇率快照：1原始币种=?CNY',
    account_id          BIGINT          NOT NULL COMMENT '所属账户',
    target_account_id   BIGINT          DEFAULT NULL COMMENT '转账目标账户',
    category_id         BIGINT          NOT NULL COMMENT '分类ID',
    merchant_id         BIGINT          DEFAULT NULL COMMENT '商户ID',
    transaction_date    DATE            NOT NULL COMMENT '交易日期',
    transaction_time    TIME            DEFAULT NULL COMMENT '交易时间',
    note                VARCHAR(200)    DEFAULT NULL COMMENT '备注',
    tags                JSON            DEFAULT NULL COMMENT '标签数组 ["日常","必要"]',
    attachment_urls     JSON            DEFAULT NULL COMMENT '附件路径数组',
    is_confirmed        TINYINT(1)      DEFAULT 1 COMMENT '0=待确认（周期交易自动生成）',
    is_recurring        TINYINT(1)      DEFAULT 0,
    recurring_id        BIGINT          DEFAULT NULL COMMENT '关联周期规则ID',
    ext_fields          JSON            DEFAULT NULL COMMENT '业务扩展字段',
    metadata            JSON            DEFAULT NULL COMMENT '系统元数据',
    created_at          DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_date (transaction_date),
    INDEX idx_account (account_id),
    INDEX idx_category (category_id),
    INDEX idx_merchant (merchant_id),
    INDEX idx_type_date (type, transaction_date),
    INDEX idx_confirmed (is_confirmed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易记录表';
```

**多币种处理流程**：

```
用户创建交易
  │
  ├─ currency == CNY ?
  │     ├─ YES → amount_base = amount, exchange_rate = 1.0
  │     └─ NO  → 查询当日汇率（Redis → MySQL → ExchangeRate-API）
  │              amount_base = amount × exchange_rate
  │              exchange_rate 写入交易记录（快照，不可变）
  │
  └─ 后续所有预算比较、报表聚合一律使用 amount_base
```

**设计要点**：

- `amount` 保留原始币种金额（用户看到的），`amount_base` 是换算后的 CNY 金额（系统用的）。
- `exchange_rate` 是交易时刻的汇率快照，后续汇率波动不影响历史记录。
- `currency` 默认继承自账户的 `currency` 字段，用户可手动覆盖（如用 CNY 账户记录境外消费）。
- `tags` 和 `attachment_urls` 使用 JSON 数组，比逗号分隔更规范，支持 `JSON_CONTAINS` 查询。
- `is_confirmed=0` 表示待确认交易（周期规则自动生成），确认后才影响账户余额和预算。
- `ext_fields` 典型用途：商户、地点、参与人、项目编号、是否可报销。
- `metadata` 典型用途：导入来源、导入批次号、设备信息、汇率数据来源。

#### 3.3.4 商户表 `t_merchant`

管理常用消费商户，支持模糊匹配、关联分类、消费统计。

```sql
CREATE TABLE t_merchant (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(100)    NOT NULL COMMENT '商户名称',
    alias           VARCHAR(200)    DEFAULT NULL COMMENT '别名（用于模糊匹配）',
    category_id     BIGINT          DEFAULT NULL COMMENT '关联分类ID',
    icon            VARCHAR(50)     DEFAULT NULL COMMENT '图标标识',
    color           CHAR(7)         DEFAULT NULL COMMENT '#RRGGBB',
    description     VARCHAR(200)    DEFAULT NULL COMMENT '商户描述',
    tags            JSON            DEFAULT NULL COMMENT '标签数组',
    usage_count     INT             DEFAULT 0 COMMENT '使用次数',
    last_used_at    DATE            DEFAULT NULL COMMENT '最近使用日期',
    is_active       TINYINT(1)      DEFAULT 1 COMMENT '是否启用',
    is_system       TINYINT(1)      DEFAULT 0 COMMENT '系统预设商户',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_name (name),
    INDEX idx_category (category_id),
    INDEX idx_usage (usage_count DESC),
    INDEX idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商户表';
```

**设计要点**：

- `name` 商户名称，唯一索引，防止重复创建。
- `alias` 别名字段，用于模糊匹配。如"星巴克"可设置别名"SBK,Starbucks"。
- `category_id` 关联默认分类，新建交易时自动带入。
- `usage_count` 使用次数，按此字段排序推荐高频商户。
- `last_used_at` 最近使用日期，用于推荐排序。
- `is_system` 系统预设商户，不可删除修改。

**商户匹配逻辑**：

```
用户在交易表单输入商户名 → 触发模糊搜索（300ms 防抖）
    │
    ├─ 后端搜索: WHERE name LIKE '%keyword%' OR alias LIKE '%keyword%'
    │              ORDER BY usage_count DESC LIMIT 10
    │
    ├─ 有匹配结果 → 显示商户列表，用户选择已有商户
    │
    └─ 无匹配结果 → 显示"创建新商户"选项
                   用户提交交易时设置 autoCreateMerchant=true
                   后端自动创建新商户并关联
```

**商户统计报表**：

商户维度消费分布，用于首页和报表页展示：

```json
{
  "merchantShares": [
    {
      "merchantId": 1,
      "merchantName": "星巴克",
      "merchantColor": "#00704A",
      "categoryId": 1,
      "categoryName": "餐饮",
      "amount": 350.00,
      "percentage": 15.5,
      "transactionCount": 10
    }
  ]
}
```

#### 3.3.6 汇率表 `t_currency_rate`

每日缓存汇率快照，避免频繁调用外部 API。

```sql
CREATE TABLE t_currency_rate (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    base_currency   CHAR(3)         NOT NULL DEFAULT 'CNY' COMMENT '基准货币（本位币）',
    target_currency CHAR(3)         NOT NULL COMMENT '目标货币',
    rate            DECIMAL(12,6)   NOT NULL COMMENT '1单位目标货币 = rate 基准货币',
    rate_date       DATE            NOT NULL COMMENT '汇率日期',
    source          VARCHAR(50)     DEFAULT 'exchangerate-api' COMMENT '数据来源',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_pair_date (base_currency, target_currency, rate_date),
    INDEX idx_date (rate_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日汇率表';
```

**设计要点**：

- `rate` 含义举例：1 USD = 7.24 CNY → `target_currency='USD', rate=7.240000`。
- 交易创建时查 `t_currency_rate` 取当日汇率，写入 `t_transaction.exchange_rate`。
- 定时任务每日 08:00 从 ExchangeRate-API 拉取所有支持币种的当日汇率，批量写入。
- Redis 缓存 Key：`rate:{target}:{date}`，TTL=25h（覆盖跨日窗口）。
- v1.0 支持的币种：CNY, USD, EUR, GBP, JPY, HKD, SGD, THB, KRW（`t_config` 可配置）。

**汇率获取优先级**：

```
1. Redis 缓存 → rate:USD:2026-04-15
2. MySQL 表   → SELECT rate FROM t_currency_rate WHERE target_currency='USD' AND rate_date='2026-04-15'
3. 外部 API   → https://v6.exchangerate-api.com/v6/{API_KEY}/latest/CNY → 写入 MySQL + Redis
4. 降级策略   → 取最近一天的汇率（最多回溯 3 天），仍无则拒绝创建并提示
```

#### 3.3.7 周期性交易规则表 `t_recurring_rule`

自动生成水电费、房贷、订阅服务等固定支出。

```sql
CREATE TABLE t_recurring_rule (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(50)     NOT NULL COMMENT '规则名称',
    type            TINYINT         NOT NULL COMMENT '1-支出 2-收入',
    amount          DECIMAL(14,2)   NOT NULL,
    currency        CHAR(3)         DEFAULT 'CNY' COMMENT '币种',
    account_id      BIGINT          NOT NULL,
    category_id     BIGINT          NOT NULL,
    frequency       VARCHAR(20)     NOT NULL COMMENT 'DAILY/WEEKLY/MONTHLY/YEARLY',
    execute_day     TINYINT         DEFAULT NULL COMMENT '执行日（月=1-28，周=1-7）',
    start_date      DATE            NOT NULL,
    end_date        DATE            DEFAULT NULL COMMENT 'NULL=永久',
    last_executed   DATE            DEFAULT NULL,
    next_execute    DATE            NOT NULL,
    auto_confirm    TINYINT(1)      DEFAULT 0 COMMENT '0=生成待确认交易',
    note            VARCHAR(200)    DEFAULT NULL,
    ext_fields      JSON            DEFAULT NULL COMMENT '自动填入生成交易的ext_fields',
    is_active       TINYINT(1)      DEFAULT 1,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_next (next_execute),
    INDEX idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='周期性交易规则表';
```

**关键逻辑**：

- `execute_day` 限制为 1-28，规避月末日期不一致问题。
- `auto_confirm=0` 时生成「待确认」交易（`is_confirmed=0`），打开 App 时提示确认。
- `ext_fields` 会被复制到自动生成的交易记录中，如房贷交易自动带上 `{"project":"房贷"}`。

**周期记账详细说明**：

#### 执行频率详解

| frequency | 说明 | executeDay含义 | nextExecute计算 |
|-----------|------|----------------|-----------------|
| DAILY | 每天 | 无效(不需要) | nextExecute + 1天 |
| WEEKLY | 每周 | 1-7表示周一到周日 | nextExecute + 7天 |
| MONTHLY | 每月 | 1-28表示几号 | nextExecute + 1月 |
| YEARLY | 每年 | 无效(按startDate日期) | nextExecute + 1年 |

#### 自动执行流程

每天 08:00 定时任务自动执行：

```
1. 查询满足条件的规则:
   - is_active = true
   - next_execute <= 今天
   - end_date 为空 或 >= 今天

2. 对每个规则执行:
   a. 创建交易记录
      - type/amount/accountId/categoryId 从规则复制
      - is_confirmed = autoConfirm
      - is_recurring = true
      - recurring_id = rule.id
      - metadata = {"source":"recurring", "rule_id": xxx}

   b. 更新账户余额（如果autoConfirm=true）

   c. 计算下次执行日期
      - last_executed = 今天
      - next_execute = 根据frequency计算

3. 触发预算更新 + 预警检查
```

#### autoConfirm 设置说明

| autoConfirm | 说明 | 影响 |
|-------------|------|------|
| true | 自动确认 | 交易直接计入预算和报表，账户余额立即更新 |
| false | 待确认 | 交易创建但未确认，需手动确认后才生效 |

**推荐设置**：
- 固定支出（房租、订阅）→ `autoConfirm=true`
- 变动收入（工资）→ `autoConfirm=false`

#### 典型使用场景

**场景1: 每月房租**
```json
{
  "name": "每月房租",
  "type": 1,
  "amount": 2000,
  "accountId": 2,
  "categoryId": 19,
  "frequency": "MONTHLY",
  "executeDay": 1,
  "startDate": "2026-04-01",
  "autoConfirm": true
}
```
效果：每月1号自动创建-2000支出

**场景2: 每月工资**
```json
{
  "name": "每月工资",
  "type": 2,
  "amount": 15000,
  "accountId": 2,
  "categoryId": 48,
  "frequency": "MONTHLY",
  "executeDay": 10,
  "startDate": "2026-04-10",
  "autoConfirm": false
}
```
效果：每月10号创建待确认收入交易

**场景3: 每周订阅**
```json
{
  "name": "每周视频会员",
  "type": 1,
  "amount": 15,
  "frequency": "WEEKLY",
  "executeDay": 7,
  "autoConfirm": true
}
```
效果：每周日自动扣费15元

#### 手动执行功能

API: `POST /api/v1/recurring-rules/{id}/execute`

用途：
- 立即生成一笔周期交易（提前记录）
- 测试规则是否正常工作
- 补录遗漏的周期交易

#### 待确认交易功能详解

**业务规则**：

| 状态 | is_confirmed | 对余额影响 | 对预算影响 | 对报表影响 |
|------|-------------|-----------|-----------|-----------|
| 待确认 | 0 | 不影响 | 不计入 | 不显示 |
| 已确认 | 1 | 正常更新 | 正常计入 | 正常显示 |

**典型应用场景**：

1. **周期交易手动确认**：工资周期规则设置 `autoConfirm=false`，每月自动生成待确认收入，用户核实后确认
2. **批量导入预审核**：导入历史账单时先创建待确认状态，逐笔审核
3. **预估消费记录**：提前记录预计支出，实际发生后确认

**前端实现**：

```vue
<!-- TransactionList.vue 状态筛选 -->
<n-form-item label="状态">
  <n-select 
    v-model:value="filters.isConfirmed" 
    :options="confirmOptions" 
    clearable 
  />
</n-form-item>

<!-- confirmOptions 定义 -->
const confirmOptions = [
  { label: '已确认', value: true },
  { label: '待确认', value: false }
]

<!-- loadData 参数传递 -->
const params = {
  page: pagination.value.page,
  size: pagination.value.pageSize,
  confirmed: filters.value.isConfirmed  // 前端用 isConfirmed，传给后端用 confirmed
}
```

**后端实现**：

```java
// TransactionServiceImpl.java - 创建交易
public TransactionVO create(TransactionCreateDTO dto) {
    // ... 构建 entity ...
    
    // 只有已确认交易才更新余额和发布事件
    if (Boolean.TRUE.equals(entity.getIsConfirmed())) {
        applyBalance(entity);
        eventPublisher.publishEvent(new TransactionEvent(this, entity, Action.CREATE));
    }
    return toVO(entity, account, category);
}

// TransactionServiceImpl.java - 确认交易
public TransactionVO confirm(Long id) {
    Transaction entity = baseMapper.selectById(id);
    // ... 校验 ...
    
    entity.setIsConfirmed(true);
    baseMapper.updateById(entity);
    
    applyBalance(entity);
    // 关键：发布事件清除缓存和更新预算
    eventPublisher.publishEvent(new TransactionEvent(this, entity, Action.CREATE));
    
    return toVO(entity, account, category);
}

// TransactionServiceImpl.java - 查询交易
public PageResult<TransactionVO> query(TransactionQueryDTO dto) {
    LambdaQueryWrapper<Transaction> query = new LambdaQueryWrapper<>();
    // 只有明确指定 confirmed 参数时才过滤，否则查询全部
    if (dto.getConfirmed() != null) {
        query.eq(Transaction::getIsConfirmed, dto.getConfirmed());
    }
    // ... 其他查询条件 ...
}
```

**API 参数说明**：

| 参数 | 说明 | 示例 |
|------|------|------|
| `confirmed=true` | 只返回已确认交易 | 已计入余额的交易 |
| `confirmed=false` | 只返回待确认交易 | 未计入余额的交易 |
| 不传 `confirmed` | 返回全部交易 | 默认行为 |

**确认交易 API**：

```
POST /api/v1/transactions/{id}/confirm
```

响应示例：
```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "id": 14,
    "isConfirmed": true,
    "amount": 200.00,
    ...
  }
}
```

确认后自动触发：
1. 账户余额更新
2. 报表缓存清除（首页数据实时更新）
3. 预算进度更新
4. 预警规则检查

#### 3.3.8 预算表 `t_budget`

月度预算，统一以本位币 CNY 计价。月度起始日=每月1日。

```sql
CREATE TABLE t_budget (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    year_month      CHAR(7)         NOT NULL COMMENT 'YYYY-MM',
    total_amount    DECIMAL(14,2)   NOT NULL COMMENT '月度总预算（CNY）',
    note            VARCHAR(200)    DEFAULT NULL,
    is_locked       TINYINT(1)      DEFAULT 0 COMMENT '锁定后不可修改',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_month (year_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月度预算表';
```

#### 3.3.9 预算明细表 `t_budget_item`

将总预算拆解到各一级分类。

```sql
CREATE TABLE t_budget_item (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    budget_id       BIGINT          NOT NULL,
    category_id     BIGINT          NOT NULL COMMENT '一级分类',
    amount          DECIMAL(14,2)   NOT NULL COMMENT '该分类预算额度（CNY）',
    spent           DECIMAL(14,2)   DEFAULT 0.00 COMMENT '已消费 amount_base 汇总（冗余，异步更新）',
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_budget_cat (budget_id, category_id),
    INDEX idx_budget (budget_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预算分类明细表';
```

**设计要点**：

- `spent` 冗余字段，每次交易写入时异步更新（Spring Event），聚合的是 `t_transaction.amount_base`。
- 预算按一级分类粒度管控，子分类消费自动归集到父类。
- 预算和已消费均以 CNY 计，多币种交易通过 `amount_base` 统一口径。

#### 3.3.10 预警规则表 `t_alert_rule`

```sql
CREATE TABLE t_alert_rule (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    name            VARCHAR(50)     NOT NULL COMMENT '规则名称',
    type            TINYINT         NOT NULL COMMENT '类型：见枚举',
    config          JSON            NOT NULL COMMENT '规则配置',
    notify_channel  VARCHAR(50)     DEFAULT 'TELEGRAM' COMMENT 'TELEGRAM/APP',
    is_active       TINYINT(1)      DEFAULT 1,
    created_at      DATETIME        DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警规则表';
```

**预警类型枚举**：

| type | 名称 | config 示例 | 触发时机 |
|------|------|-------------|----------|
| 1 | 预算阈值预警 | `{"threshold_pct": 80}` | 每笔支出后 |
| 2 | 单笔大额预警 | `{"max_amount": 1000}` | 每笔支出后 |
| 3 | 日消费上限 | `{"daily_limit": 500}` | 每笔支出后 |
| 4 | 周消费异常 | `{"deviation_pct": 50}` | 每周日 22:00 |
| 5 | 信用卡还款提醒 | `{"advance_days": 3}` | 每日 09:00 |
| 6 | 周期账单提醒 | `{"advance_days": 1}` | 每日 09:00 |
| 7 | 月度预算未设定 | `{"check_day": 25}` | 每月 25 日 |
| 8 | 待确认交易提醒 | `{"min_count": 1}` | 每日 10:00 |

**预警规则详细说明**：

#### 1️⃣ 预算阈值预警 (type=1)

**触发条件**: 创建支出交易时实时检查

**工作原理**:
```
当创建一笔支出交易时:
1. 找到该交易分类对应的一级分类
2. 查询该分类的本月预算额度
3. 计算: 已消费 / 预算额度 × 100%
4. 如果百分比 ≥ 阈值(默认80%) → 触发预警
```

**推送内容示例**:
```
📊 预算预警

• 进度: 85%
• 本月预算: ¥1500
• 已消费: ¥1275
• 剩余: ¥225

━━━━━━━━━━━━━
_2026-04-18_
```

#### 2️⃣ 单笔大额预警 (type=2)

**触发条件**: 创建支出交易时实时检查

**工作原理**: 较交易金额(amountBase)与设定阈值，超过则触发

**推送内容示例**:
```
💰 大额支出提醒

• 金额: ¥1200
• 分类: 购物
• 备注: 购买笔记本电脑

━━━━━━━━━━━━━
_2026-04-18_
```

#### 3️⃣ 日消费上限 (type=3)

**触发条件**: 创建支出交易时实时检查

**工作原理**: 查询今日所有已确认支出交易总额，超过上限则触发

**推送内容示例**:
```
⚠️ 日消费上限预警

• 今日消费: ¥650.00
• 已超过设定的上限

━━━━━━━━━━━━━
_2026-04-18_
```

#### 4️⃣ 周消费异常检测 (type=4)

**触发条件**: 每周日 22:00 定时检查

**工作原理**:
```
1. 计算本周消费总额
2. 计算过去4周的平均消费
3. 计算偏离百分比: (本周-平均)/平均 × 100%
4. 偏离 > 50% → "异常"预警
5. 偏离 < -50% → "偏低"预警
```

**推送内容示例**:
```
🔍 周消费异常警告

• 本周消费: ¥3500
• 4周平均: ¥2000
• 偏离: +75%
• 阈值: 50%

━━━━━━━━━━━━━
_2026-04-18_
```

#### 5️⃣ 信用卡还款提醒 (type=5)

**触发条件**: 每日 09:00 定时检查

**工作原理**: 检查所有信用卡账户，距离还款日 ≤ 提前天数则提醒

**推送内容示例**:
```
💳 信用卡还款提醒

• 卡名: 招商信用卡
• 还款日: 4月25日
• 信用额度: ¥50000
• 当前余额: ¥-3200
• 剩余天数: 3 天

━━━━━━━━━━━━━
_2026-04-18_
```

#### 6️⃣ 周期账单提醒 (type=6)

**触发条件**: 每日 09:00 定时检查

**工作原理**: 检查周期规则的 next_execute 是否等于明天，提前提醒

**推送内容示例**:
```
📋 周期账单提醒

• 名称: 每月房租
• 金额: ¥2000
• 执行日期: 明天

━━━━━━━━━━━━━
_2026-04-18_
```

#### 7️⃣ 预算未设定提醒 (type=7)

**触发条件**: 每月 25 日 09:00 定时检查

**工作原理**: 检查下个月预算是否存在，不存在则提醒

**推送内容示例**:
```
⚠️ 预算未设定提醒

• 2026-05 月预算尚未创建
• 请及时设定下月预算

━━━━━━━━━━━━━
_2026-04-18_
```

#### 8️⃣ 待确认交易提醒 (type=8)

**触发条件**: 每日 10:00 定时检查，也可通过 API 手动触发

**手动触发接口**: `POST /api/v1/system/check-unconfirmed`

**工作原理**:
```
1. 查询所有 is_confirmed=false 的交易
2. 检查数量是否 ≥ min_count
3. 汇总交易信息和涉及金额
4. 发送提醒通知
```

**推送内容示例**:
```
📝 待确认交易提醒

• 待确认数量: 3 条
• 涉及金额: ¥4500.00

• 房租 - ¥2000.00
• 工资收入 - ¥5000.00
• 超市购物 - ¥150.00

请及时确认这些交易！

━━━━━━━━━━━━━
_2026-04-18_
```

**应用场景**:
- 周期交易自动生成但未设置自动确认
- 手动创建的交易标记为待确认
- 系统提醒用户及时核对账目

#### 3.3.11 预警日志表 `t_alert_log`

```sql
CREATE TABLE t_alert_log (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    rule_id         BIGINT          NOT NULL,
    alert_type      TINYINT         NOT NULL,
    title           VARCHAR(100)    NOT NULL,
    content         VARCHAR(500)    NOT NULL,
    notify_channel  VARCHAR(50)     NOT NULL COMMENT '实际推送渠道',
    is_sent         TINYINT(1)      DEFAULT 0 COMMENT 'Telegram 是否发送成功',
    is_read         TINYINT(1)      DEFAULT 0,
    triggered_at    DATETIME        DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_unread (is_read, triggered_at),
    INDEX idx_rule (rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='预警日志表';
```

#### 3.3.12 系统配置表 `t_config`

```sql
CREATE TABLE t_config (
    id              BIGINT          PRIMARY KEY AUTO_INCREMENT,
    config_key      VARCHAR(50)     NOT NULL,
    config_value    TEXT            NOT NULL,
    description     VARCHAR(100)    DEFAULT NULL,
    updated_at      DATETIME        DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX uk_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';
```

**预置配置项**：

| config_key | config_value | 说明 |
|------------|-------------|------|
| `base_currency` | `CNY` | 本位币 |
| `supported_currencies` | `CNY,USD,EUR,GBP,JPY,HKD,SGD,THB,KRW` | 支持的币种列表 |
| `month_start_day` | `1` | 月度统计起始日（已确认=1号） |
| `telegram_bot_token` | *(待配置)* | Telegram Bot Token |
| `telegram_chat_id` | *(待配置)* | 接收通知的 Chat ID |
| `exchange_rate_api_key` | *(待配置)* | ExchangeRate-API Key |
| `backup_cron` | `0 3 * * *` | 自动备份 cron |
| `data_retention_months` | `0` | 数据保留月数，0=永久 |

---

## 4. 核心功能设计

### 4.1 交易记录模块

#### 4.1.1 快速记账流程

```
用户打开 App
  → 点击 "+" FAB 按钮
  → 选择类型（支出/收入/转账）
  → 输入金额（数字键盘优先）
  → 选择账户（默认上次使用的账户，自动带入币种）
  → 如果币种 ≠ CNY → 自动获取汇率并显示本位币等值金额
  → 选择分类（展示最近使用 Top 5 + 全部分类）
  → [可选] 备注、标签、日期修改、拍照、扩展字段
  → 保存
```

**性能目标**：记账接口响应 < 100ms。

- 保存交易后，**异步更新**账户余额和预算已消费（Spring Event + @Async）。
- Redis 缓存：最近使用分类 Top 5、默认账户、当日汇率。

#### 4.1.2 RESTful API 设计

```
POST   /api/v1/transactions              # 创建交易
GET    /api/v1/transactions              # 分页查询（多条件筛选）
GET    /api/v1/transactions/{id}         # 查询详情
PUT    /api/v1/transactions/{id}         # 全量更新
DELETE /api/v1/transactions/{id}         # 删除（物理删除 + 余额回滚）
POST   /api/v1/transactions/{id}/confirm # 确认待确认交易
POST   /api/v1/transactions/batch-import # 批量导入（CSV）
GET    /api/v1/transactions/export       # 导出 CSV
```

**创建交易请求体**：

```json
{
  "type": 1,
  "amount": 29.99,
  "currency": "GBP",
  "accountId": 3,
  "categoryId": 4,
  "transactionDate": "2026-04-15",
  "transactionTime": "14:30:00",
  "note": "Amazon UK 买书",
  "tags": ["教育", "境外"],
  "extFields": {
    "merchant": "Amazon.co.uk",
    "location": "London"
  }
}
```

**系统自动补全**：

```json
{
  "currency": "GBP",
  "exchangeRate": 9.350000,
  "amountBase": 280.47,
  "metadata": {
    "source": "manual",
    "device": "iPhone",
    "rate_source": "exchangerate-api",
    "rate_date": "2026-04-15"
  }
}
```

**查询参数**：

| 参数 | 类型 | 说明 |
|------|------|------|
| `type` | int | 1-支出 2-收入 3-转账 |
| `accountId` | long | 账户ID |
| `categoryId` | long | 分类ID（自动包含子分类） |
| `startDate` / `endDate` | date | 日期范围 |
| `minAmount` / `maxAmount` | decimal | 金额范围（按 amount_base） |
| `currency` | string | 按原始币种筛选 |
| `keyword` | string | 备注关键词模糊搜索 |
| `tags` | string[] | 标签（任意匹配） |
| `confirmed` | boolean | 是否已确认 |
| `extKey` + `extValue` | string | 扩展字段精确查询 |
| `page` / `size` | int | 分页，默认 1/20 |
| `sort` | string | 排序：`transaction_date,desc`（默认）、`amount,asc` 等 |

#### 4.1.3 转账逻辑

转账本质是一条交易记录的两侧操作：

```java
@Transactional
public TransactionVO create(TransactionCreateDTO dto) {
    // ... 校验省略 ...

    // 构建实体
    Transaction entity = buildEntity(dto);
    transactionMapper.insert(entity);

    // 更新账户余额（用原始币种 amount 操作，非 amount_base）
    switch (entity.getType()) {
        case 1 -> // 支出：源账户扣减
            accountMapper.adjustBalance(entity.getAccountId(), entity.getAmount().negate());
        case 2 -> // 收入：源账户增加
            accountMapper.adjustBalance(entity.getAccountId(), entity.getAmount());
        case 3 -> { // 转账：源扣减，目标增加
            accountMapper.adjustBalance(entity.getAccountId(), entity.getAmount().negate());
            accountMapper.adjustBalance(entity.getTargetAccountId(), entity.getAmount());
        }
    }

    // 异步：预算更新 + 预警检查
    eventPublisher.publishEvent(new TransactionEvent(this, entity, CREATE));
    return buildVO(entity);
}
```

**跨币种转账**：从 CNY 账户转到 USD 账户时，需要用户指定目标金额或汇率，系统不自动转换。这是 v1.0 的简化处理——跨币种转账场景少，后续可扩展。

### 4.2 多币种模块

#### 4.2.1 汇率服务架构

```
CurrencyService
├── getRate(currency, date)
│   ├── 1. Redis: rate:{currency}:{date}
│   ├── 2. MySQL: t_currency_rate
│   ├── 3. ExchangeRate-API (外部)
│   └── 4. 降级: 最近3天汇率
│
├── refreshRates()            ← 定时任务每日 08:00
│   ├── 调用 API 获取所有支持币种
│   ├── 批量写入 t_currency_rate
│   └── 批量写入 Redis (TTL=25h)
│
└── convertToBase(amount, currency, date)
    └── return amount × getRate(currency, date)
```

#### 4.2.2 汇率 API 调用

```
ExchangeRate-API（免费档）
├── 额度：每月 1,500 次
├── 端点：https://v6.exchangerate-api.com/v6/{KEY}/latest/CNY
├── 返回所有币种对 CNY 的汇率
├── 每日调用 1 次，月消耗约 30 次，远低于额度
└── 注意：API 请求从家庭服务器发出（通过 Clash 代理），不走大陆 VPS
```

**关键问题：汇率请求的流量出境**

ExchangeRate-API 是外部服务，请求必须经过海外网络。这不违反「数据不出境」原则：

- 出境的只是一个无参 GET 请求（不含任何个人/财务数据）。
- 返回的汇率数据是公开信息。
- 个人交易数据始终只存在于家庭服务器的 MySQL 中。

#### 4.2.3 预算与报表中的多币种处理

```
预算系统 → 统一使用 amount_base (CNY)
报表系统 → 统一使用 amount_base (CNY)
账户余额 → 使用账户原始币种 (account.currency)
交易列表 → 同时展示原始金额和本位币金额

月度总览示例：
  总支出：¥12,500.00（含 £29.99 × 9.35 = ¥280.47）
```

### 4.3 预算管理模块

#### 4.3.1 预算生命周期

```
每月 1 日（月度起始日，已确认）
  → 系统检查当月预算是否已创建
  → 未创建 → 触发「预算未设定」预警（Telegram 推送）
  → 已创建 → 激活当月预算

月中
  → 每笔支出实时更新 budget_item.spent（异步，使用 amount_base）
  → 触发阈值检查（80% / 100%）

月末（下月 1 日 03:00）
  → 锁定上月预算（is_locked=1）
  → 生成月度总结并 Telegram 推送
```

#### 4.3.2 API 设计

```
POST /api/v1/budgets                                    # 创建月度预算
GET  /api/v1/budgets/{yearMonth}                        # 查询月度预算
PUT  /api/v1/budgets/{yearMonth}                        # 修改预算
POST /api/v1/budgets/{yearMonth}/copy-from/{sourceMonth}# 复制预算
GET  /api/v1/budgets/{yearMonth}/progress               # 预算执行进度
```

#### 4.3.3 预算执行进度响应

```json
{
  "yearMonth": "2026-04",
  "totalBudget": 15000.00,
  "totalSpent": 8750.00,
  "remaining": 6250.00,
  "progressPct": 58.3,
  "daysPassed": 15,
  "daysTotal": 30,
  "dailyAvgSpent": 583.33,
  "dailyAvgRemaining": 416.67,
  "items": [
    {
      "categoryId": 1,
      "categoryName": "餐饮",
      "budget": 3000.00,
      "spent": 2100.00,
      "remaining": 900.00,
      "progressPct": 70.0,
      "status": "WARNING"
    }
  ]
}
```

**status 计算逻辑**：

```
时间进度 = daysPassed / daysTotal
消费进度 = spent / budget

EXCEEDED  → 消费进度 > 1.0（已超支）
WARNING   → 消费进度 > 时间进度 × 1.2（消费速度偏快）
CAUTION   → 消费进度 > 0.8（接近上限）
NORMAL    → 其他
```

### 4.4 统计报表模块

#### 4.4.1 报表类型

| 报表 | 维度 | 金额口径 | 用途 |
|------|------|----------|------|
| 月度总览 | 收入/支出/结余 + 分类占比 | amount_base | 每月回顾 |
| 分类明细 | 某分类下所有交易 | 原始 + 本位币 | 消费归因 |
| 趋势分析 | 近 6/12 个月支出折线图 | amount_base | 长期观察 |
| 同比分析 | 本月 vs 上月 / 去年同月 | amount_base | 对比变化 |
| 账户汇总 | 各账户余额 + 流水统计 | 各账户原始币种 | 资产全景 |
| 日历热力图 | 每日消费热力图 | amount_base | 直觉感知 |
| 预算执行报告 | 各分类预算完成度 | amount_base | 预算复盘 |
| 币种分布 | 各币种消费占比 | amount_base | 多币种分析 |

#### 4.4.2 API 设计

```
GET /api/v1/reports/monthly-summary?month=2026-04
GET /api/v1/reports/category-detail?month=2026-04&categoryId=1
GET /api/v1/reports/trend?months=12&type=1
GET /api/v1/reports/compare?month=2026-04&compareWith=2026-03
GET /api/v1/reports/account-summary
GET /api/v1/reports/daily-heatmap?year=2026
GET /api/v1/reports/budget-review?month=2026-04
GET /api/v1/reports/currency-distribution?month=2026-04
```

#### 4.4.3 缓存策略

**问题背景**：报表数据涉及大量聚合计算（按月统计收入/支出/分类占比），如果每次请求都实时查询数据库，性能较差。但缓存如果与交易变更无联动，会导致首页数据延迟更新，用户体验不佳。

**设计方案**：

```
Redis 缓存方案：
├── 当月报表       → 缓存 1 小时（数据频繁变动）
├── 历史月报表     → 缓存 24 小时（预算已锁定，数据稳定）
├── 年度趋势       → 缓存 1 小时
├── 汇率数据       → 缓存 25 小时（per currency per date）
└── 缓存 Key 规范  → report:monthly-summary:{month}
                     report:category-detail:{month}:{categoryId}
                     report:account-summary
                     rate:{currency}:{date}
```

**缓存失效机制**（关键设计）：

数据变更时主动清除相关报表缓存，确保数据实时性：

| 触发场景 | 清除的缓存 Key | 实现位置 |
|----------|---------------|----------|
| 创建/更新/删除交易 | `report:monthly-summary:{交易月份}`<br>`report:category-detail:{月份}:*`<br>`report:account-summary` | TransactionEventListener（@EventListener） |
| 余额调整（调增/调减） | `report:monthly-summary:{交易月份}`<br>`report:account-summary` | AccountServiceImpl.adjustBalanceWithRecord() |
| 账户信息更新/删除 | `report:monthly-summary:*`<br>`report:category-detail:*`<br>`report:account-summary` | AccountServiceImpl.update()/delete() |
| 分类信息更新/删除 | `report:monthly-summary:*`<br>`report:category-detail:*` | CategoryServiceImpl.update()/delete() |

**代码实现要点**：

```java
// TransactionEventListener.java - 使用 SCAN 替代 keys（性能优化）
@Async("asyncExecutor")
@EventListener
public void onTransactionEvent(TransactionEvent event) {
    Transaction tx = event.getTransaction();
    // 清除报表缓存
    clearReportCache(tx.getTransactionDate());
    // ... 其他处理（预算更新、预警检查）
}

private void clearReportCache(LocalDate date) {
    String month = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
    redisTemplate.delete("report:monthly-summary:" + month);
    scanAndDelete("report:category-detail:" + month + ":*");
    redisTemplate.delete("report:account-summary");
}

// AccountServiceImpl.java - 账户变更清除所有报表缓存
public AccountVO update(Long id, AccountUpdateDTO dto) {
    // ... 更新逻辑 ...
    baseMapper.updateById(entity);
    clearAllReportCache(); // 新增：清除所有报表缓存
    return toVO(entity);
}

// CategoryServiceImpl.java - 分类变更清除所有报表缓存
public CategoryVO update(Long id, CategoryCreateDTO dto) {
    // ... 更新逻辑 ...
    baseMapper.updateById(entity);
    clearAllReportCache(); // 新增：清除所有报表缓存
    return toVO(entity);
}
```

**设计权衡**：

| 方案 | 优点 | 缺点 | 选择 |
|------|------|------|------|
| 不缓存 | 数据实时 | 性能差 | ❌ |
| 缓存 + 定时刷新 | 性能好 | 数据延迟 | ❌ |
| 缓存 + 主动清除 | 性能好 + 数据实时 | 实现稍复杂 | ✅ |

**注意事项**：
- 缓存清除通过 Spring Event 异步执行，不影响主流程响应时间
- 余额调整直接创建交易记录（不经过 TransactionService），需单独处理缓存清除
- 当前月份缓存时间缩短为 1 小时（原设计 5 分钟过短，5 小时过长）

### 4.5 预警模块

#### 4.5.1 预警引擎架构

```
触发源                     预警引擎                    通知渠道
─────────              ─────────────              ──────────
交易写入  ──→ ┐                                    ┌→ Telegram Bot（主）
              ├──→  规则匹配器  ──→  去重过滤  ──→ ┤
定时任务  ──→ ┘     (责任链模式)     (Redis)       └→ App 内通知（备）
                                                     (alert_log 表)
```

#### 4.5.2 预警规则详细设计

**规则 1：预算阈值预警**（交易触发）

```java
public class BudgetThresholdChecker implements AlertChecker {
    @Override
    public boolean check(Transaction tx, AlertRule rule) {
        BigDecimal threshold = rule.getConfig().getThresholdPct();
        BudgetItem item = budgetService.getItem(tx.getCategoryId(), currentMonth());
        if (item == null) return false;
        BigDecimal pct = item.getSpent()
            .divide(item.getAmount(), 4, HALF_UP)
            .multiply(BigDecimal.valueOf(100));
        return pct.compareTo(threshold) >= 0;
    }
}
```

**规则 2：单笔大额预警**（交易触发）

```
config: {"max_amount": 1000}
逻辑: transaction.amount_base > max_amount（统一用本位币比较）
```

**规则 3：日消费上限**（交易触发）

```
config: {"daily_limit": 500}
逻辑: SELECT SUM(amount_base) FROM t_transaction
      WHERE transaction_date = CURDATE() AND type = 1 AND is_confirmed = 1
合计 > daily_limit 时触发
```

**规则 4：周消费异常检测**（定时任务，每周日 22:00）

```
config: {"deviation_pct": 50}
逻辑: 本周消费 vs 近 4 周平均消费
偏离超过 deviation_pct% 时触发
```

**规则 5：信用卡还款提醒**（定时任务，每日 09:00）

```
config: {"advance_days": 3}
逻辑: 遍历所有信用卡账户，当前日期 >= payment_day - advance_days 时触发
```

**规则 6：周期账单提醒**（定时任务，每日 09:00）

```
config: {"advance_days": 1}
逻辑: SELECT * FROM t_recurring_rule WHERE next_execute <= CURDATE() + advance_days
```

**规则 7：月度预算未设定**（定时任务，每月 25 日）

```
config: {"check_day": 25}
逻辑: 检查下月预算是否已创建，未创建则提醒
```

#### 4.5.3 通知去重

```
Redis Key: alert:dedup:{rule_id}:{date}:{context_hash}
TTL: 24h
同一规则、同一天、同一上下文只触发一次
```

#### 4.5.4 Telegram Bot 集成

**配置流程**：

```
1. 在 Telegram 中找 @BotFather → /newbot → 获取 Bot Token
2. 发送一条消息给你的 Bot
3. 访问 https://api.telegram.org/bot{TOKEN}/getUpdates → 获取 Chat ID
4. 将 Token 和 Chat ID 写入 t_config 表
```

**消息格式**（Markdown V2）：

```
📊 *预算预警*

餐饮预算已达 *80%*
本月预算：¥3,000.00
已消费：¥2,400.00
剩余：¥600.00

按当前速度，月底预计超支约 ¥400

_2026-04-15 14:30_
```

```
💰 *大额支出提醒*

金额：£29.99（≈ ¥280.47）
分类：购物 > 数码
账户：中信信用卡
备注：Amazon UK 买书

_2026-04-15 14:30_
```

```
💳 *信用卡还款提醒*

中信信用卡将于 *4月18日* 到期
本期账单金额：¥5,234.00

_距还款日还有 3 天_
```

**发送逻辑**：

```java
@Service
@RequiredArgsConstructor
public class TelegramNotifyService {

    private final RestTemplate restTemplate;
    private final ConfigService configService;

    private static final String API_URL = "https://api.telegram.org/bot%s/sendMessage";

    public boolean send(String title, String content) {
        String token = configService.get("telegram_bot_token");
        String chatId = configService.get("telegram_chat_id");
        if (token.isBlank() || chatId.isBlank()) {
            log.warn("Telegram 未配置，跳过推送");
            return false;
        }

        String url = String.format(API_URL, token);
        Map<String, Object> body = Map.of(
            "chat_id", chatId,
            "text", content,
            "parse_mode", "MarkdownV2"
        );

        try {
            restTemplate.postForEntity(url, body, String.class);
            return true;
        } catch (Exception e) {
            log.error("Telegram 推送失败", e);
            return false;
        }
    }
}
```

**网络路径**：

```
家庭 Linux 服务器 → Clash 代理 → api.telegram.org
（仅推送通知文本，不含任何个人财务原始数据，
  消息内容为脱敏后的汇总信息）
```

---

## 5. 定时任务设计

| 任务 | Cron | 说明 |
|------|------|------|
| 汇率更新 | `0 0 8 * * ?` | 每日 08:00 拉取所有币种汇率，写入 MySQL + Redis |
| 周期交易生成 | `0 0 8 * * ?` | 每日 08:00 检查到期规则，生成交易（待确认/自动确认） |
| 信用卡还款提醒 | `0 0 9 * * ?` | 每日 09:00 检查还款日，Telegram 推送 |
| 账户余额校准 | `0 30 2 * * ?` | 每日 02:30 重算所有账户余额 |
| 预算进度检查 | `0 0 20 * * ?` | 每日 20:00 检查预算执行状态 |
| 周消费异常检测 | `0 0 22 ? * SUN` | 每周日 22:00 |
| 月度预算提醒 | `0 0 9 25 * ?` | 每月 25 日提醒设定下月预算 |
| 月度报表 + 锁定 | `0 0 3 1 * ?` | 每月 1 日 03:00 锁定上月预算、生成总结、Telegram 推送 |
| 数据库备份 | `0 0 3 * * ?` | 每日 03:00 mysqldump 到 NAS |
| 汇率数据清理 | `0 0 4 1 * ?` | 每月 1 日 04:00 清理 6 个月前的汇率记录 |

---

## 6. 完整 RESTful API 汇总

### 6.1 账户 `/api/v1/accounts`

| 方法 | 路径 | 说明 | 状态码 |
|------|------|------|--------|
| POST | `/api/v1/accounts` | 创建账户 | 201 |
| GET | `/api/v1/accounts` | 账户列表（?active=true） | 200 |
| GET | `/api/v1/accounts/{id}` | 账户详情 | 200 |
| PATCH | `/api/v1/accounts/{id}` | 部分更新 | 200 |
| DELETE | `/api/v1/accounts/{id}` | 逻辑删除（停用） | 204 |

### 6.2 交易 `/api/v1/transactions`

| 方法 | 路径 | 说明 | 状态码 |
|------|------|------|--------|
| POST | `/api/v1/transactions` | 创建交易 | 201 |
| GET | `/api/v1/transactions` | 分页查询（14+ 筛选条件） | 200 |
| GET | `/api/v1/transactions/{id}` | 交易详情 | 200 |
| PUT | `/api/v1/transactions/{id}` | 全量更新（自动回滚+重算余额） | 200 |
| DELETE | `/api/v1/transactions/{id}` | 物理删除 + 余额回滚 | 204 |
| POST | `/api/v1/transactions/{id}/confirm` | 确认待确认交易 | 200 |
| POST | `/api/v1/transactions/batch-import` | 批量导入 CSV | 200 |
| GET | `/api/v1/transactions/export` | 导出 CSV | 200 |

### 6.3 分类 `/api/v1/categories`

| 方法 | 路径 | 说明 | 状态码 |
|------|------|------|--------|
| POST | `/api/v1/categories` | 创建分类 | 201 |
| GET | `/api/v1/categories/tree` | 树形结构（?type=1） | 200 |
| PUT | `/api/v1/categories/{id}` | 更新分类 | 200 |
| DELETE | `/api/v1/categories/{id}` | 逻辑删除 | 204 |

### 6.4 商户 `/api/v1/merchants`

| 方法 | 路径 | 说明 | 状态码 |
|------|------|------|--------|
| POST | `/api/v1/merchants` | 创建商户 | 201 |
| GET | `/api/v1/merchants` | 商户列表（分页） | 200 |
| GET | `/api/v1/merchants/{id}` | 商户详情 | 200 |
| PUT | `/api/v1/merchants/{id}` | 更新商户 | 200 |
| DELETE | `/api/v1/merchants/{id}` | 删除商户 | 204 |
| GET | `/api/v1/merchants/search?keyword=xxx` | 模糊搜索商户 | 200 |

### 6.5 预算 `/api/v1/budgets`

| 方法 | 路径 | 说明 | 状态码 |
|------|------|------|--------|
| POST | `/api/v1/budgets` | 创建月度预算 | 201 |
| GET | `/api/v1/budgets/{yearMonth}` | 查询预算 | 200 |
| PUT | `/api/v1/budgets/{yearMonth}` | 修改预算 | 200 |
| POST | `/api/v1/budgets/{yearMonth}/copy-from/{source}` | 复制预算 | 201 |
| GET | `/api/v1/budgets/{yearMonth}/progress` | 执行进度 | 200 |

### 6.6 报表 `/api/v1/reports`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/reports/monthly-summary` | 月度总览 |
| GET | `/api/v1/reports/category-detail` | 分类明细 |
| GET | `/api/v1/reports/trend` | 趋势分析 |
| GET | `/api/v1/reports/compare` | 同比分析 |
| GET | `/api/v1/reports/account-summary` | 账户汇总 |
| GET | `/api/v1/reports/daily-heatmap` | 日历热力图 |
| GET | `/api/v1/reports/budget-review` | 预算执行报告 |
| GET | `/api/v1/reports/currency-distribution` | 币种分布 |
| GET | `/api/v1/reports/merchant-distribution` | 商户消费分布 |

### 6.7 周期规则 `/api/v1/recurring-rules`

| 方法 | 路径 | 说明 | 状态码 |
|------|------|------|--------|
| GET | `/api/v1/recurring-rules` | 规则列表 | 200 |
| GET | `/api/v1/recurring-rules/{id}` | 查询规则 | 200 |
| POST | `/api/v1/recurring-rules` | 创建规则 | 200 |
| PUT | `/api/v1/recurring-rules/{id}` | 更新规则 | 200 |
| DELETE | `/api/v1/recurring-rules/{id}` | 删除规则 | 200 |
| POST | `/api/v1/recurring-rules/{id}/toggle` | 启用/停用 | 200 |
| POST | `/api/v1/recurring-rules/{id}/execute` | 立即执行 | 200 |

### 6.8 预警规则 `/api/v1/alert-rules`

| 方法 | 路径 | 说明 | 状态码 |
|------|------|------|--------|
| GET | `/api/v1/alert-rules` | 规则列表 | 200 |
| GET | `/api/v1/alert-rules/{id}` | 查询规则 | 200 |
| POST | `/api/v1/alert-rules` | 创建规则 | 200 |
| PUT | `/api/v1/alert-rules/{id}` | 更新规则 | 200 |
| DELETE | `/api/v1/alert-rules/{id}` | 删除规则 | 200 |
| POST | `/api/v1/alert-rules/{id}/toggle` | 启用/停用 | 200 |

### 6.9 系统 `/api/v1/system`

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/system/config` | 查询所有配置 |
| PUT | `/api/v1/system/config/{key}` | 修改配置 |
| GET | `/api/v1/system/currencies` | 支持的币种列表 |
| GET | `/api/v1/system/rates?date=2026-04-15` | 查询汇率 |
| POST | `/api/v1/system/rates/refresh` | 手动刷新汇率 |
| GET | `/api/v1/system/alerts` | 预警日志列表 |
| PUT | `/api/v1/system/alerts/{id}/read` | 标记已读 |
| POST | `/api/v1/system/telegram/test` | Telegram 推送测试 |

### 6.10 统一响应结构

```json
// 成功
{
  "code": 0,
  "message": "ok",
  "data": { ... },
  "timestamp": 1713168000
}

// 失败
{
  "code": 30002,
  "message": "交易金额无效: 金额必须大于0",
  "timestamp": 1713168000
}

// 分页
{
  "code": 0,
  "message": "ok",
  "data": {
    "items": [ ... ],
    "total": 156,
    "page": 1,
    "size": 20,
    "totalPages": 8
  },
  "timestamp": 1713168000
}
```

### 6.11 错误码体系

| 范围 | 模块 | 示例 |
|------|------|------|
| 10001-10099 | 通用 | 参数校验失败、资源不存在、系统异常 |
| 20001-20099 | 账户 | 不存在、已停用、余额不足、名称重复 |
| 30001-30099 | 交易 | 不存在、金额无效、转账同账户、分类不匹配 |
| 40001-40099 | 分类 | 不存在、系统分类不可改、有子分类、有交易引用 |
| 50001-50099 | 预算 | 不存在、已锁定、月份重复 |
| 60001-60099 | 汇率 | 币种不支持、汇率获取失败、API 限流 |
| 70001-70099 | 商户 | 不存在、名称已存在、有交易引用、系统商户不可改 |

---

## 7. 安全与数据保护

### 7.1 访问控制

```json
{
  "auth": {
    "mode": "PIN / 密码登录（Sa-Token）",
    "session": "JWT Token，7 天有效，Redis 存储",
    "rate_limit": "Redis 令牌桶，60 次/分钟"
  },
  "network": {
    "access": "仅 Tailscale 内网 + Nginx 反向代理",
    "https": "Tailscale MagicDNS 自带 TLS",
    "exposed": "不暴露到公网"
  }
}
```

### 7.2 数据流量分析

```
✅ 不出境的数据流
├── MySQL          → 127.0.0.1:3306
├── Redis          → 127.0.0.1:6379
├── Spring Boot    → 127.0.0.1:8080
├── Nginx          → Tailscale IP
├── 备份           → NAS 本地挂载
└── 所有个人财务数据仅在家庭服务器内流转

⚠️ 合理出境的请求（不含敏感数据）
├── ExchangeRate-API  → 获取公开汇率（每日 1 次 GET 请求）
├── Telegram Bot API  → 推送脱敏通知文本
└── 均通过 Clash 代理出境
```

### 7.3 数据备份策略

```
每日增量备份 → NAS (QNAP TS-212P3)
  └── mysqldump --single-transaction --routines
  └── 保留最近 30 天
  └── 压缩后约 < 1MB/天

每周全量备份 → NAS
  └── 保留最近 12 周

备份脚本：
#!/bin/bash
DATE=$(date +%Y%m%d)
mysqldump -u budgetpilot -p${DB_PASS} budgetpilot \
  --single-transaction --routines \
  | gzip > /mnt/nas/backup/budgetpilot/daily_${DATE}.sql.gz

find /mnt/nas/backup/budgetpilot/daily_*.sql.gz -mtime +30 -delete
```

---

## 8. 部署方案

### 8.1 Docker Compose

```yaml
version: '3.8'
services:
  budgetpilot-api:
    image: budgetpilot:latest
    build: .
    ports:
      - "127.0.0.1:8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - DB_HOST=mysql
      - DB_PORT=3306
      - REDIS_HOST=redis
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started
    restart: unless-stopped
    mem_limit: 512m

  mysql:
    image: mysql:8.0
    ports:
      - "127.0.0.1:3307:3306"
    environment:
      - MYSQL_ROOT_PASSWORD=${DB_ROOT_PASS}
      - MYSQL_DATABASE=budgetpilot
      - MYSQL_USER=budgetpilot
      - MYSQL_PASSWORD=${DB_PASS}
    volumes:
      - mysql_data:/var/lib/mysql
      - ./sql:/docker-entrypoint-initdb.d
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      retries: 5
    restart: unless-stopped
    mem_limit: 512m

  redis:
    image: redis:7-alpine
    ports:
      - "127.0.0.1:6380:6379"
    command: redis-server --maxmemory 64mb --maxmemory-policy allkeys-lru
    restart: unless-stopped
    mem_limit: 128m

volumes:
  mysql_data:
```

### 8.2 Nginx 配置

```nginx
server {
    listen 443 ssl;
    server_name budget.tailnet.ts.net;

    location / {
        root /opt/budgetpilot/web;
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    location /uploads/ {
        alias /opt/budgetpilot/uploads/;
        client_max_body_size 5m;
    }
}
```

### 8.3 资源评估

| 组件 | 预估内存 | 说明 |
|------|----------|------|
| Spring Boot | ~300-512MB | -Xmx512m |
| MySQL | ~300-500MB | buffer_pool 256MB |
| Redis | ~64MB | 缓存量极小 |
| Vue 前端 | 仅静态文件 | Nginx 提供 |
| **合计** | **~1GB** | 16GB 服务器完全够用 |

---

## 9. 移动端适配

**方案：PWA（渐进式 Web App）**

| 对比项 | PWA | 原生 App |
|--------|-----|----------|
| 开发成本 | 低，复用 Web 前端 | 高，独立开发 |
| 安装 | Safari/Chrome 添加到主屏幕 | 需分发 |
| 离线 | Service Worker 缓存 | 完整离线 |
| 推送 | iOS 16.4+ 支持 | 原生支持 |
| 记账体验 | 够用（大字体数字键盘） | 更流畅 |

**移动端关键页面**：

```
首页
├── 本月概览卡片（总收入/总支出/结余，CNY）
├── 预算进度条（整体 + Top 3 分类）
├── 最近交易列表（显示原始币种 + CNY 等值）
└── 快速记账 FAB 按钮

记账页
├── 金额数字键盘（大字体，拇指友好）
├── 币种选择器（默认跟随账户，可切换）
├── 实时显示 CNY 等值金额
├── 分类九宫格（最近使用 + 全部）
├── 账户选择器
└── 备注 + 日期 + 标签 + 扩展字段折叠面板

报表页
├── 月度切换器
├── 收支柱状图（CNY）
├── 分类饼图（可点击下钻）
├── 趋势折线图
└── 币种分布饼图

预算页
├── 本月预算概览
├── 各分类进度条
└── 一键复制上月预算
```

---

## 10. 开发路线图

```
Phase 1（2 周）—— 骨架 + 核心记账
├── 项目脚手架 + Docker 环境
├── 数据库建表 + 初始化数据
├── 账户 CRUD + 分类 CRUD（含扩展字段）
├── 交易 CRUD（含扩展字段 + 多币种）
└── 汇率服务（API 对接 + Redis 缓存）
                                          ← ✅ 已完成大部分代码
Phase 2（2 周）—— 预算 + 报表
├── 预算管理（创建/复制/进度追踪）
├── 月度总览报表
├── 分类明细 + 趋势分析
├── 币种分布报表
└── 缓存策略落地

Phase 3（1 周）—— 预警 + Telegram
├── 预警引擎（责任链模式）
├── 7 种预警规则实现
├── Telegram Bot 推送服务
├── App 内通知中心
└── 通知去重

Phase 4（1 周）—— 前端 + 部署
├── Vue 3 前端开发
├── 移动端 PWA 适配
├── Docker Compose 部署
├── Nginx 反向代理配置
└── 备份脚本 + 定时任务

Phase 5（持续）—— 打磨
├── CSV 批量导入/导出
├── AI 分类建议（接入 one-api → qwen2.5）
├── 语音记账实验
├── 性能优化
└── Blog 文章输出（gubin.uk）
```

---

## 附录 A：SQL 脚本清单

```
sql/
├── schema.sql        # 10 张表建表语句
├── init_data.sql     # 预设分类 + 默认预警规则 + 系统配置
└── upgrade/          # 版本升级脚本（预留）
```

## 附录 B：技术依赖版本

```xml
<properties>
    <java.version>21</java.version>
    <spring-boot.version>3.3.5</spring-boot.version>
    <mybatis-plus.version>3.5.9</mybatis-plus.version>
    <sa-token.version>1.39.0</sa-token.version>
    <hutool.version>5.8.32</hutool.version>
    <jackson.version>跟随 Spring Boot</jackson.version>
</properties>
```

## 附录 C：扩展字段快速参考

### 账户 ext_fields 推荐字段

| 字段名 | 类型 | 适用账户类型 | 说明 |
|--------|------|-------------|------|
| `bank_name` | string | 储蓄卡/信用卡 | 银行名称 |
| `card_last4` | string | 储蓄卡/信用卡 | 卡号后四位 |
| `branch` | string | 储蓄卡 | 开户行 |
| `points_ratio` | number | 信用卡 | 积分兑换比例 |
| `annual_fee` | number | 信用卡 | 年费 |
| `bindPhone` | string | 电子钱包 | 绑定手机号 |
| `broker` | string | 投资账户 | 券商名称 |

### 交易 ext_fields 推荐字段

| 字段名 | 类型 | 场景 | 说明 |
|--------|------|------|------|
| `merchant` | string | 所有消费 | 商户名称 |
| `location` | string | 线下消费 | 消费地点 |
| `participants` | string[] | 聚餐/社交 | 参与人列表 |
| `per_person` | number | 聚餐 | 人均金额 |
| `project` | string | 出差/报销 | 项目名称 |
| `reimbursable` | boolean | 出差 | 是否可报销 |
| `invoice_no` | string | 报销 | 发票编号 |

### 交易 metadata 系统字段

| 字段名 | 类型 | 说明 |
|--------|------|------|
| `source` | string | 来源：manual / csv_import / recurring / api |
| `device` | string | 设备信息 |
| `app_version` | string | 客户端版本 |
| `import_batch` | string | 导入批次号 |
| `original_row` | number | 导入时原始行号 |
| `rate_source` | string | 汇率数据来源 |
| `rate_date` | string | 汇率日期 |
