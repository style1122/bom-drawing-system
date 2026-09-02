# BOM 图纸管理系统 —— Windows Server 2019 部署文档

> 技术栈：Spring Boot 2.7.18（内嵌 Tomcat 9 / `javax.servlet`）+ MyBatis-Plus 3.5.7 + SQL Server 2019 + Druid
> 前端：Vue 3 + Vite，构建为静态资源由 nginx 托管
> 打包形态：**可执行 JAR**（单文件部署，无需外置 Tomcat）

---

## 1. 架构与部署拓扑

```
[ 浏览器 ]  ──HTTP 80──▶  [ nginx for Windows ]
                                │  /            └── 静态文件：Vue dist
                                │  /api/**      └── 反向代理 ──▶ [ Spring Boot JAR :8080 ]
                                                                      │
                                                                      └──▶ [ SQL Server 2019 :1433 / BOM_DB ]
```

- 后端只提供 `/api/**` 接口；前端页面与静态资源由 nginx 直接返回。
- 后端注册为 Windows 服务（WinSW），开机自启、崩溃自动重启。
- 所有密钥（数据库密码、ERP 密钥）通过**环境变量**注入，不落明文。

---

## 2. 环境要求

| 组件 | 版本 | 说明 |
|------|------|------|
| Windows Server | 2019 | 64 位 |
| JDK | 8uXXX 64-bit（或 11/17） | Spring Boot 2.7 支持 Java 8–19；需 `java` 在 PATH |
| SQL Server | 2019 | 数据库引擎，混合验证模式 |
| Maven | 3.6+ | 仅构建机需要 |
| Node.js | 18+ | 仅构建前端需要（含 npm） |
| nginx | for Windows 1.24+ | 托管前端 + 反向代理 |
| WinSW | v2/v3 (x64) | 将 JAR 注册为 Windows 服务 |

> 构建机与部署机可同一台，也可分开：只需把 `bom-drawing-system.jar` 与 `bom-frontend/dist` 拷贝到服务器。

---

## 3. 数据库准备（SQL Server 2019）

### 3.1 安装与网络
1. 安装 SQL Server 2019 数据库引擎，身份验证选**混合模式**，设置 `sa` 密码（该密码即后续的 `JDBC_PASSWORD`）。
2. 打开 **SQL Server Configuration Manager** → *SQL Server Network Configuration* → *Protocols for MSSQLSERVER* → **TCP/IP** → 启用（Yes）。
3. 同窗口 *TCP/IP* → *IP Addresses* → 拉到 **IPAll** → `TCP Port = 1433`（清空 TCP Dynamic Ports）。
4. 重启 **SQL Server (MSSQLSERVER)** 服务使配置生效。
5. **Windows 防火墙** → 高级设置 → 入站规则 → 新建规则 → 端口 → TCP 1433 → 允许。

### 3.2 建库与建表
用 SSMS 或 `sqlcmd` 依次执行项目 `sql/` 目录下的脚本：

```sql
-- 01_create_database.sql   建库 BOM_DB（建议排序规则 Chinese_PRC_CI_AS）
-- 02_create_requisition.sql
-- 03_erp_material.sql / 03_update_roles.sql / 04_erp_have_drawing.sql
-- 05_erp_sync_cursor.sql
```

> 若使用专用应用账号（而非 `sa`），请在 BOM_DB 中创建登录名并映射为 db_owner（或最小必需权限）。

---

## 4. 构建

在**构建机**上（仓库根目录）：

```bat
build.bat
```

或手动：

```bat
mvn clean package -DskipTests
cd bom-frontend && npm install && npm run build
```

产物：
- 后端：`bom-web\target\bom-drawing-system.jar`（finalName=`bom-drawing-system`）
- 前端：`bom-frontend\dist\`

> ⚠️ 本环境未能联网执行 `mvn` 验证编译，请在构建机上执行 `mvn clean package` 确认通过后，
> 再将 jar 部署到服务器。如编译报错，按错误修正后重新打包（迁移已尽量保持与现有代码兼容）。

---

## 5. 配置密钥（环境变量）

`application.yml` 中数据库与 ERP 密钥均使用 `${ENV_VAR}` 占位，并保留**本地开发默认值**。
**生产服务器必须通过环境变量覆盖敏感项**（尤其是 `JDBC_PASSWORD`、`ERP_APPSECRET`）。

在服务器上设置 **系统环境变量**（控制面板 → 系统 → 高级 → 环境变量 → 系统变量）：

| 变量名 | 说明 | 默认值（开发） |
|--------|------|----------------|
| `JDBC_URL` | SQL Server 连接串 | `jdbc:sqlserver://localhost:1433;databaseName=BOM_DB;encrypt=false;trustServerCertificate=true;sendStringParametersAsUnicode=true` |
| `JDBC_USERNAME` | 数据库账号 | `sa` |
| `JDBC_PASSWORD` | 数据库密码 | `Sync@2026`（**生产务必修改**） |
| `SERVER_PORT` | 后端监听端口 | `8080` |
| `ERP_BASE_URL` | 正航 ESB 地址 | `http://10.1.1.15:860` |
| `ERP_APPID` | ERP 应用 ID | 见 application.yml |
| `ERP_APPSECRET` | ERP 密钥 | 见 application.yml（**生产务必修改**） |

> WinSW 启动的 Java 进程会**继承系统环境变量**，因此配置在系统变量中即可被服务读取。

---

## 6. 部署后端（JAR + WinSW 服务）

1. 在服务器建目录 `C:\apps\bom-drawing-system\`。
2. 拷贝：
   - `bom-drawing-system.jar`
   - `deploy\windows\bom-drawing-system.xml`
   - 从 https://github.com/winsw/winsw/releases 下载 `WinSW-x64.exe`，重命名为 **`bom-drawing-system.exe`**，与本目录其余文件同级。
3. 以**管理员**身份运行：
   ```bat
   cd C:\apps\bom-drawing-system
   install-service.bat
   ```
4. 启动服务（可在 `services.msc` 中找到 “BOM Drawing System”，或命令）：
   ```bat
   bom-drawing-system.exe start
   ```
5. 验证：
   - 日志：`C:\apps\bom-drawing-system\logs\bom-system.log`（应用日志）与 `bom-drawing-system.out.log`（WinSW 捕获的控制台）。
   - 接口探活：`curl http://localhost:8080/api/...`（注意登录类接口已放行）。

常用命令：
```bat
bom-drawing-system.exe stop      # 停止
bom-drawing-system.exe restart   # 重启
uninstall-service.bat            # 卸载服务
```

---

## 7. 部署前端（nginx）

1. 建目录 `C:\apps\bom-drawing-system\frontend\`，将 `bom-frontend\dist\` 整个拷贝到 `...\frontend\dist`。
2. 安装 **nginx for Windows**，将 `deploy\nginx\bom-drawing-system.conf` 复制到 `nginx\conf\`。
3. 在 `nginx\conf\nginx.conf` 的 `http { ... }` 内添加：
   ```nginx
   include bom-drawing-system.conf;
   ```
4. 以管理员身份启动 nginx：
   ```bat
   cd C:\apps\nginx
   start nginx.exe
   ```
5. 验证：浏览器访问 `http://<服务器IP>/`，应能打开登录页；接口请求走 `/api/**` 被代理到后端。

> 若 80 端口被 IIS 占用，可改 `listen 80;` 为其他端口（如 8081），或停用 IIS 的默认站点。

---

## 8. 建议目录结构

```
C:\apps\
├── bom-drawing-system\
│   ├── bom-drawing-system.jar
│   ├── bom-drawing-system.exe        (WinSW)
│   ├── bom-drawing-system.xml        (WinSW 配置)
│   ├── logs\                          (应用/服务日志)
│   └── frontend\
│       └── dist\                      (Vue 构建产物)
└── nginx\
    └── conf\
        ├── nginx.conf
        └── bom-drawing-system.conf
```

---

## 9. 运维与故障排查

### 日志
- 应用日志：`C:\apps\bom-drawing-system\logs\bom-system.log`（按天滚动，保留 30 天）。
- 服务控制台：`bom-drawing-system.out.log` / `bom-drawing-system.err.log`（WinSW 生成）。
- nginx 日志：`nginx\logs\access.log` / `error.log`。

### 端口冲突
- 若 8080 被占用：设置系统环境变量 `SERVER_PORT=8090`（或改 `application.yml` 的 `server.port`），并同步修改 nginx `proxy_pass` 的端口。
- nginx 80 被占用：见第 7 步。

### 常见错误
| 现象 | 原因 / 处理 |
|------|--------------|
| 启动报 `datetime2` / `无效的列类型` | 必须保留 `mybatis-plus.configuration.jdbc-type-for-null: NULL`（已在 application.yml 配置），确保未误删。 |
| 连接 SQL Server 报 TLS / 加密错误 | 开发用 `encrypt=false;trustServerCertificate=true`；生产启用 TLS 时改 `encrypt=true` 并配置证书/信任。 |
| API 返回 401 | 未携带/已失效 token；登录、注册、公开分享、下载、ERP 订阅接口已放行。 |
| API 返回 403 | 写操作需 `ADMIN`/`ENGINEER` 角色（见 RoleInterceptor 映射）。 |
| 服务启动即退出 | 查 `bom-drawing-system.out.log`：多为 `java` 未在 PATH、或 `JDBC_PASSWORD` 等系统环境变量未设置。 |
| 前端页面白屏/刷新 404 | 确认 nginx `location /` 有 `try_files $uri $uri/ /index.html;`（SPA 路由兜底）。 |

### 升级流程
1. 构建机重新 `build.bat` 生成新 jar / dist。
2. 停止服务：`bom-drawing-system.exe stop`。
3. 替换 `bom-drawing-system.jar` 与 `frontend\dist`。
4. 启动服务：`bom-drawing-system.exe start`；如改了 nginx 配置需 `nginx -s reload`。

---

## 10. 迁移说明（本次改造要点）

- 由 **Spring MVC + MyBatis（WAR）** 改造为 **Spring Boot 2.7 + MyBatis-Plus（可执行 JAR）**。
- 移除 `web.xml`、`spring/spring-context.xml`、`spring/spring-mvc.xml`、`jdbc.properties`、`erp.properties`、`DruidConfig.java`；
  数据源/MyBatis-Plus/拦截器/上传限制/日志 全部改为 `application.yml` + Java 配置（`BomDrawingSystemApplication`、`WebMvcConfig`、`MybatisPlusConfig`）。
- 8 个实体加 `@TableName/@TableId/@TableField` 注解；7 个 Mapper 接口继承 `BaseMapper<T>`（原有 XML Mapper 语句保留，与 BaseMapper 共用）。
- 兼容 XML Mapper 中的物理分页/联表 SQL；MyBatis-Plus 分页拦截器为 `BaseMapper`/`QueryWrapper` 分页提供支撑。
- 密钥一律 `${ENV_VAR}` 占位，生产通过系统环境变量注入。
