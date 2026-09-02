# 正航 T9 ERP 物料基础数据对接说明

> 版本：v1.6.0 | 日期：2026-08-15

## 1. 对接范围

本功能对接正航 T9 ERP 的 **物料基础数据（Mat01）** 模块，将 ERP 中的物料主数据同步到图纸管理系统的本地 `material` 表，实现物料编码、名称、规格、类型、类别、单位、有效期等基础数据的统一管理，避免重复录入。

### 字段映射

| ERP 字段（MaterialGroup） | 本地字段（material 表） | 说明 |
|------|------|------|
| MaterialId | material_code | 物料代码（主键，唯一） |
| MaterialName | material_name | 物料名称 |
| MaterialSpec | specification | 物料规格 |
| MaterialTypeId | material_type | 物料类型代码 |
| MaterialCategoryId | material_category | 物料类别代码（v1.6.0 新增列） |
| UnitId | unit | 基本单位代码 |
| ValidityFromDate | validity_from_date | 有效期从（v1.6.0 新增列） |
| ValidityToDate | validity_to_date | 有效期至（v1.6.0 新增列） |

同步规则：按物料编码判断，本地不存在则新增，已存在则更新（更新时 ERP 字段为空则保留本地原值）；同步来源标记为 `ERP`，并记录 `erp_sync_time`。

## 2. 接口说明（ESB）

### 认证接口

| 项 | 值 |
|------|------|
| 地址 | `{erp.base-url}/esb/api/auth.do` |
| 方式 | POST，JSON，UTF-8 |
| 参数 | `appid`、`time`、`sign`、`language` |
| 签名 | `sign = MD5(appid + appsecret + time)`（大写） |

### 物料清单查询

| 项 | 值 |
|------|------|
| 地址 | `{erp.base-url}/esb/erp/getlist.do` |
| 方式 | POST，JSON，UTF-8 |
| 参数 | `token`、`time`、`sign`、`progid=Mat01`、`data.condition`、`data.lastpkvalues` |
| 签名 | `sign = MD5(token + appsecret + time)`（大写） |

### time 参数

- 默认格式 `millis6`：UTC 毫秒时间戳 + 6 位随机数（与认证文档示例一致）
- 如 ESB 环境要求 `yyyyMMddHHmmss` 格式，可在 `erp.properties` 中配置 `erp.time-format=datetime14`

### 认证请求字段顺序（重要）

实测该 ESB 的 CapCRL 解析器对认证接口**严格要求字段顺序**：

```json
{"appid":"...","sign":"...","language":"zh-CHS","time":"..."}
```

即 `appid → sign → language → time`。文档示例中的 `appid → time → sign → language` 顺序会被解析器拒绝（报 “Deserialize error, can't read '}'” 或“缺少参数'appid'”）。代码已用 `LinkedHashMap` 固定该顺序，勿自行调整。

物料清单查询（getlist）经实测对字段顺序不敏感，按文档顺序即可。

### 分页

首次请求 `data.lastpkvalues` 为空，响应返回 `lastpkvalues` 与 `hasnext`；当 `hasnext=true` 时，用返回的 `lastpkvalues` 作为下一页请求参数，直到 `hasnext=false` 为止。

## 3. 配置

### 3.1 ERP 连接配置（bom-web/src/main/resources/erp.properties）

```properties
erp.base-url=http://10.1.1.15:860      # ESB 服务地址（含端口，正航 ESB 端口为 860）
erp.appid=chiesbeac90d2bcca79c04       # 正航提供的 appid
erp.appsecret=5EBABC79A0AD621B6CFCEA67C92C3064
erp.progid=Mat01                       # 物料基础数据功能编号
erp.language=zh-CHS
erp.time-format=millis6                # millis6 / datetime14
erp.connect-timeout=10000
erp.read-timeout=120000
```

### 3.2 订阅同步与兜底轮询

```properties
erp.subscribe.enabled=true             # 订阅同步总开关
erp.subscribe.sscrid=MA01              # 订阅号
erp.subscribe.poll.enabled=true        # 兜底轮询开关
erp.subscribe.poll.interval-ms=300000  # 兜底轮询间隔（毫秒），默认 5 分钟
```

物料异动时 ERP 通过订阅通知实时回调本系统，本系统立即调用 `sscrquery` 拉取增量（秒级）；
同时保留兜底轮询（默认每 5 分钟，可配置/关闭），防止通知丢失：

- 轮询任务通过 `@Scheduled` 实现，由 `spring-context.xml` 中的 `<task:annotation-driven/>` 启用，并带并发防重入（手动同步进行中会跳过本轮）；
- 无异动时轮询只做一次空查询，不写数据库；
- 前端“ERP物料同步”按钮仍可手动全量同步。

### 3.3 数据库升级

新库：`sql/01_create_database.sql` 已包含新字段。

存量库：执行 `sql/03_erp_material.sql`，为 `material` 表补充 `material_category`、`validity_from_date`、`validity_to_date` 三个字段。

## 4. 使用方式

### 4.1 后端接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/material/erp/test` | POST | 测试 ERP 连接与认证凭据 |
| `/api/material/erp/sync` | POST | 手动同步物料（body 可选 `{"condition": "MaterialId = 'xxx'"}`，缺省全量） |
| `/api/material/erp/status` | GET | 查询最近一次同步结果 |
| `/api/material/erp/config` | GET | 查询当前生效的 ERP 地址/功能编号等配置（不含密钥），用于排查部署是否生效 |

接口位于 `/api/material/**` 下，沿用登录认证与角色权限（ADMIN / ENGINEER / PRODUCTION 可访问）。

### 4.2 前端入口

登录图纸管理系统 → 图纸管理（物料列表）页面，工具栏提供：

- **测试ERP连接**：验证 ESB 地址、appid/appsecret 是否可用
- **ERP物料同步**：一键全量同步，完成后展示“拉取/新增/更新/失败”统计并自动刷新列表

物料列表新增“来源”列（ERP / 手工）与“ERP同步时间”列，方便确认数据来源。

### 4.3 是否存在图纸（CU_HaveDrawing）回写 ERP

图纸系统上传图纸成功后，会自动调用 ERP 更新接口（`POST /esb/erp/update.do`，progid=Mat01），把该物料的 `CU_HaveDrawing`（是否存在图纸）字段置为 `1`；没有图纸时置为 `0`。物料列表新增“ERP图纸标记”列（是/否）展示回写结果，工具栏提供“同步ERP图纸标记”按钮可手动重算并回写。

> ⚠️ 前置条件：ERP 后台需要将物料基础数据中的“是否存在图纸（CU_HaveDrawing）”字段配置为**允许导入**。若未配置，ERP 会返回“字段 是否存在图纸[CU_HaveDrawing] 未配置为允许导入”，此时本地上传不受影响，但 ERP 标记不会更新，需要联系 ERP 管理员在正航 T9 后台开放该字段的 API 导入权限。

## 5. 常见问题

| 现象 | 排查方向 |
|------|------|
| 测试连接报“ERP请求失败（网络或服务不可达）” | 确认本机可访问 `10.1.1.15` 的 ESB 端口（当前配置为 860）；可先执行 `Test-NetConnection 10.1.1.15 -Port 860` 验证连通性 |
| 报“验证AppSecret失败” | 核对 `erp.appid` / `erp.appsecret` 是否正确 |
| 报“验证Token失败” | token 过期或网络抖动，系统会自动重新认证；若频繁出现，检查服务器时间是否与 ERP 同步 |
| 同步后无新增 | 确认 ERP 中物料是否启用/有效，或通过 `condition` 限定范围；可通过 `/api/material/erp/status` 查看最近一次同步统计 |
| 认证接口报“Deserialize json content error” | 说明当前地址上的 `/esb/api/auth.do` 不是文档所述的 ESB 网关（例如是 ERP 应用服务器自身的接口，其参数类型与文档不一致）。请向 ERP 管理员确认文档对应的 ESB 服务地址及端口，并核对 `erp.base-url` |
