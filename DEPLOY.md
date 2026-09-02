# BOM 图纸管理系统 — 部署文档

> 版本：v1.4.0
> 适用环境：Windows / Linux 生产部署

---

## 一、环境要求

| 软件 | 版本 | 用途 |
|------|------|------|
| JDK | 1.8+ | Java 运行环境 |
| Maven | 3.6+ | 后端项目构建 |
| SQL Server | 2019+ | 业务数据库 |
| Tomcat | 9+ | 应用服务器（war 包部署） |
| Node.js | 18+ | 前端构建 |
| pnpm | 最新版 | 前端依赖管理 |

---

## 二、数据库初始化

### 2.1 创建数据库

使用 SSMS 或 sqlcmd 连接到 SQL Server，执行以下脚本：

```bash
sqlcmd -S localhost -U sa -P 'your_password' -i sql/01_create_database.sql
```

脚本位置：`sql/01_create_database.sql`

执行完成后，数据库 `BOM_DB` 及其所有表将被创建。

### 2.2 数据库表说明

| 表名 | 说明 |
|------|------|
| `sys_user` | 系统用户表，存储用户信息和登录凭据，默认包含管理员账号 admin/admin123 |
| `material` | 物料主数据表，存储物料编码、名称、规格、单位等基础信息 |
| `drawing` | 图纸文件表，存储上传的图纸文件元信息（文件名、路径、类型、关联物料等） |
| `audit_log` | 操作日志表，记录用户的关键操作（登录、增删改等），用于审计追溯 |
| `share_token` | 分享令牌表，用于生成和管理图纸/文档的外部分享链接，支持过期时间控制 |

---

## 三、修改配置

### 3.1 数据库连接

编辑 `bom-web/src/main/resources/jdbc.properties`：

```properties
jdbc.driver=com.microsoft.sqlserver.jdbc.SQLServerDriver
jdbc.url=jdbc:sqlserver://localhost:1433;databaseName=BOM_DB;encrypt=false;trustServerCertificate=true
jdbc.username=sa
jdbc.password=your_password_here
```

| 参数 | 说明 |
|------|------|
| `jdbc.url` | 数据库连接 URL，修改 IP、端口、数据库名以匹配实际环境 |
| `jdbc.username` | 数据库用户名 |
| `jdbc.password` | 数据库密码 |

### 3.2 文件上传目录

编辑 `bom-service/src/main/java/com/bom/service/DrawingService.java`，修改 `BASE_STORAGE_PATH` 常量：

```java
// 根据实际服务器环境修改此路径
private static final String BASE_STORAGE_PATH = "/data/bom-uploads/";
```

> 确保该目录存在且 Tomcat 进程有读写权限。

### 3.3 前端开发代理

编辑 `bom-frontend/vite.config.js`，修改代理目标地址：

```js
export default defineConfig({
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',  // 修改为后端实际地址
        changeOrigin: true
      }
    }
  }
})
```

### 3.4 正航 T9 ERP 物料同步配置（可选）

如需从正航 T9 ERP 同步物料基础数据，编辑 `bom-web/src/main/resources/erp.properties`：

```properties
erp.base-url=http://10.1.1.15:860
erp.appid=chiesbeac90d2bcca79c04
erp.appsecret=5EBABC79A0AD621B6CFCEA67C92C3064
```

存量数据库需先执行 `sql/03_erp_material.sql` 补充物料同步字段。部署完成后，系统默认每 1 分钟自动增量拉取 ERP 新增物料（可在 `erp.properties` 中调整 `erp.sync.poll.enabled` / `erp.sync.poll.interval-ms`），图纸管理页的“ERP物料同步”按钮仍可手动全量同步。详细说明见 `docs/erp-integration.md`。

---

## 四、项目构建

### 4.1 前端构建

```bash
cd bom-frontend
pnpm install
pnpm run build
```

构建产物输出到 `bom-frontend/dist/` 目录。

### 4.2 后端构建

```bash
# 在项目根目录执行
mvn clean package -DskipTests
```

构建成功后，war 包位于：`bom-web/target/bom-web-1.0.0.war`

---

## 五、部署

### 5.1 Tomcat 部署

1. 将 `bom-web/target/bom-web-1.0.0.war` 复制到 Tomcat 的 `webapps/` 目录
2. 建议重命名为 `bom.war`（访问路径为 `/bom`）
3. 启动 Tomcat：

```bash
# Linux / macOS
$TOMCAT_HOME/bin/startup.sh

# Windows
%TOMCAT_HOME%\bin\startup.bat
```

4. 访问 `http://localhost:8080/bom/` 验证部署成功

### 5.2 默认管理员

| 账号 | 密码 |
|------|------|
| `admin` | `admin123` |

> 首次登录后请立即修改默认密码。

---

## 六、Nginx 生产环境部署（推荐）

将前端静态文件与后端 API 通过 Nginx 统一代理，避免跨域问题。

### 6.1 Nginx 配置示例

```nginx
server {
    listen       80;
    server_name  your-domain.com;

    # 前端静态文件（SPA 路由回退）
    location /bom {
        alias   /opt/bom-drawing-system/bom-frontend/dist;
        index   index.html;
        try_files $uri $uri/ /bom/index.html;
    }

    # API 代理到 Tomcat
    location /bom/api/ {
        proxy_pass http://127.0.0.1:8080/bom/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 文件上传大小限制
        client_max_body_size 100m;
    }

    # 静态资源缓存
    location ~* \.(js|css|png|jpg|jpeg|gif|ico|svg|woff|woff2|ttf)$ {
        alias   /opt/bom-drawing-system/bom-frontend/dist;
        expires 30d;
        add_header Cache-Control "public, immutable";
    }
}
```

### 6.2 配置要点说明

| 配置项 | 作用 |
|------|------|
| `try_files $uri $uri/ /bom/index.html` | SPA 路由回退，解决 Vue Router history 模式下刷新 404 问题 |
| `client_max_body_size 100m` | 允许上传较大的图纸文件 |
| 静态资源缓存规则 | 对 JS/CSS/图片等设置 30 天强缓存，优化加载速度 |

---

## 七、前端独立开发

当后端已部署运行时，前端可独立启动开发服务器进行调试：

```bash
cd bom-frontend
pnpm install
pnpm run dev
```

访问 `http://localhost:3000`，API 请求将自动代理到 `vite.config.js` 中配置的后端地址。

---

## 八、项目结构

```
bom-drawing-system/
├── pom.xml                          # Maven 父 POM（多模块管理）
├── sql/
│   └── 01_create_database.sql       # 数据库初始化脚本
├── bom-common/                      # 公共模块（工具类、常量、异常定义）
├── bom-dao/                         # 数据访问层（实体类、MyBatis Mapper）
├── bom-service/                     # 业务逻辑层（Service 接口与实现）
├── bom-web/                         # Web 层（Spring MVC Controller、配置）
│   └── src/main/resources/
│       ├── jdbc.properties          # 数据库连接配置
│       └── spring/                  # Spring 配置文件
└── bom-frontend/                    # 前端项目（Vue 3 + Element Plus + Vite）
    ├── vite.config.js               # Vite 构建与代理配置
    └── src/
        ├── api/                     # API 请求封装
        ├── views/                   # 页面组件
        ├── components/              # 公共组件
        ├── router/                  # Vue Router 路由配置
        └── store/                   # Pinia 状态管理
```

---

## 九、系统账号

| 角色 | 默认账号 | 密码 | 说明 |
|------|---------|------|------|
| 管理员 | `admin` | `admin123` | 系统内置，可管理用户和系统设置 |
| 普通用户 | 注册创建 | 自设 | 通过注册页面创建，由管理员审核 |

> 认证方式：Token 认证，Token 存储在服务端内存（ConcurrentHashMap）中，服务重启后需重新登录。

---

## 十、常见问题

### Q1：Tomcat 启动报错 "找不到 SQLServerDriver"

**原因**：Maven 依赖未正确打包到 war 中。

**解决**：确认 `mssql-jdbc` 在 `bom-web/target/bom-web-1.0.0/WEB-INF/lib/` 下存在。如缺失，手动将 `mssql-jdbc` jar 复制到 Tomcat 的 `lib/` 目录。

### Q2：前端页面空白，刷新后 404

**原因**：Vue Router history 模式下服务端未正确配置路由回退。

**解决**：Nginx 配置中确保有 `try_files $uri $uri/ /bom/index.html;`。如直接部署在 Tomcat 中，需配置 URL 重写。

### Q3：文件上传失败

**原因**：`DrawingService.java` 中配置的 `BASE_STORAGE_PATH` 目录不存在或无写入权限。

**解决**：创建对应目录并确保 Tomcat 进程有读写权限：

```bash
mkdir -p /data/bom-uploads
chmod 755 /data/bom-uploads
```

### Q4：SQL Server 连接失败

**检查清单**：

1. SQL Server 服务是否启动
2. TCP/IP 协议是否启用（SQL Server Configuration Manager）
3. 端口 1433 是否被防火墙阻止
4. SQL Server 是否启用混合模式认证（SQL Server 和 Windows 身份验证模式）

### Q5：登录后接口返回 401

**原因**：Token 存储在服务端内存中，Tomcat 重启后 Token 全部失效。

**解决**：重新登录即可获取新 Token。

---

> 如有问题，请查看 Tomcat 日志 `$TOMCAT_HOME/logs/catalina.out` 和浏览器开发者工具控制台。
