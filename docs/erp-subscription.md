# 正航 T9 ERP 订阅模式实时同步实现说明

> 版本：v1.1 | 日期：2026-08-18

> 依据 2026-08-18 版《ERPAPI接口文档(TuZhi01).html》（含“订阅”章节）实现。

## 1. 订阅模式原理（依据 ERPAPI 接口文档 F&Q）

正航 ERP 的订阅模式是**“通知 + 主动拉取”**：

1. ERP 中物料数据发生异动（新增/修改/删除）时，ERP 会**通知**第三方（本图纸管理系统）；
2. 本系统收到通知后，**主动调用订阅查询接口**拉取异动的数据；
3. 增量依据：第三方应记录订阅查询返回的**每一笔数据的最后修改时间**；当返回数据的最后修改时间与第三方记录的一致时，视为该笔数据无异动；
4. 下次查询时，应取**上次订阅查询返回的最后一笔数据的最后修改时间**作为本次查询的起始时间戳（不要用 ERP 通知报文里的时间）。

## 2. 整体流程

```
ERP 物料增/改/删
   │  ① 通知（HTTP 回调到本系统）
   ▼
本系统回调接口 /api/erp/subscribe/material
   │  ② 校验订阅密钥，快速应答 200
   ▼
触发增量拉取（异步）
   │  ③ 调用 ERP 订阅查询接口（按最后修改时间 > 本地游标）
   ▼
解析异动物料
   ├─ 新增/修改 → 本地 material upsert
   └─ 删除     → 按 ERP 返回的删除标记/编码处理（需确认删除如何体现）
   │  ④ 更新本地增量游标（最后一笔的最后修改时间）
   ▼
完成
```

## 3. 接口规范（文档确认）

### 3.1 ERP 通知报文（ERP → 本系统回调）

| 参数 | 说明 |
|------|------|
| funcid | 功能编号，如 comMaterialGroup |
| sscrid | 订阅号，如 MA01 |
| lastoperatetime | 数据异动时间 |
| method | 调用方法 |
| trcode / cltrcode | 事务号 |

```json
{"funcid":"comMaterialGroup","sscrid":"MA01","lastoperatetime":"2018-08-24 08:48:37","method":"MA01","cltrcode":-8731150889872662943,"trcode":25895}
```

### 3.2 订阅查询接口（本系统 → ERP）

- 地址：`POST /esb/erp/sscrquery.do`
- 请求：`token`、`time`、`sign`（token+appsecret+time MD5）、`data.sscrid=MA01`、`data.timestamp`（查询最后修改时间大于该时间戳的数据）、`data.pkvalues`（上一页最后一笔主键）
- 响应：
  - `hasnext`：是否还有数据
  - `lastoperatetime`：本次最后一笔的最后修改时间（作为下次查询的 timestamp）
  - `pkvalues`：本次最后一笔的主键（作为下次查询的 pkvalues）
  - `detail[]`：每笔异动，含 `pkvalues`、`lastoperatetime`、`action`（0=非删除，2=删除）、`data`（action=2 时为 null）

## 4. 需要向 ERP 提供（本系统对外暴露）

| 项目 | 内容 | 说明 |
|------|------|------|
| 回调地址 | `http://<本机局域网IP>:8080/bom/api/erp/subscribe/material` | ERP 服务器（10.1.1.15）必须能访问该地址；不能用 localhost |
| 订阅对象 | 物料基础数据（Mat01 / MaterialGroup）的新增、修改、删除 | 请 ERP 管理员配置订阅范围 |
| 回调鉴权 | 双方约定的订阅密钥（token） | 本系统校验回调请求携带的密钥，防止伪造通知 |
| 网络/防火墙 | 开放本机 8080 端口入站（TCP） | 允许 10.1.1.15 访问 Tomcat |

> 如果本系统部署在其它服务器，回调地址中的 IP 和端口随之调整；若经 Nginx 代理，则用 Nginx 对外地址。
> 回调接口已放行登录拦截器（/api/erp/subscribe/**），ERP 可直接访问。

## 5. 需要从正航/ERP 管理员确认

1. **订阅注册**：由正航/ERP 管理员在 ERP 后台为本系统注册订阅（订阅号 MA01），并配置回调地址为本系统 `POST /api/erp/subscribe/material`；
2. **订阅号启用**：确认 MA01 已启用（否则 sscrquery 返回“订阅号[MA01]不存在或未启用！”）；
3. **回调密钥**：如需防伪造，双方约定密钥并配置 `erp.subscribe.secret`，ERP 回调时在请求头 `X-Subscribe-Secret` 携带。

## 6. 本系统侧实现（已完成）

### 6.1 回调接收接口

```text
POST /api/erp/subscribe/material
Content-Type: application/json
```

- 公开接口（已放行登录拦截器），校验订阅号 sscrid（可选校验请求头密钥）；
- 收到通知立即返回 200，避免 ERP 重试；
- 随后异步触发“订阅增量拉取”。

### 6.2 订阅增量拉取

- 调用 `/esb/erp/sscrquery.do`，`data.timestamp` 取上次查询返回的 `lastoperatetime`；
- `action=0`：物料 upsert（新增/修改）；
- `action=2`：删除本地物料及其图纸记录（磁盘文件保留，可另行清理）；
- 增量游标（最后修改时间 + 最后主键）持久化到 `erp_sync_cursor` 表（sql/05_erp_sync_cursor.sql），服务重启后继续增量；
- 兜底轮询：默认每 5 分钟调用一次订阅查询接口（`erp.subscribe.poll.interval-ms`，可关闭），防止通知丢失；
- 无异动时空轮询不写数据库，资源开销极小。

### 6.3 配置项（erp.properties）

```properties
erp.subscribe.enabled=true
erp.subscribe.sscrid=MA01
erp.subscribe.init-timestamp=2018-01-01 00:00:00
erp.subscribe.secret=
erp.subscribe.poll.enabled=true
erp.subscribe.poll.interval-ms=300000
```

### 6.4 辅助接口

| 接口 | 说明 |
|------|------|
| `GET /api/erp/subscribe/status` | 查询订阅开关、订阅号、最近一次拉取结果 |
| `POST /api/erp/subscribe/pull` | 手动触发一次订阅增量拉取（联调/测试用） |

## 7. 实施步骤

1. 向正航确认第 4 节所列接口规范；
2. 正航在 ERP 后台注册订阅（订阅号 MA01）并配置回调地址；
3. 部署本系统（含 sql/05_erp_sync_cursor.sql），配置 `erp.subscribe.*`；
4. 联调：ERP 新增/修改/删除一个物料 → 观察本系统实时同步；
5. 也可先调用 `POST /api/erp/subscribe/pull` 手动拉取验证；
6. 上线后保留 1 分钟轮询兜底，订阅通道异常时自动降级。
