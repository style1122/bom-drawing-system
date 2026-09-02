-- =============================================
-- BOM图纸管理系统 - 数据库初始化脚本
-- 数据库: BOM_DB (SQL Server 2019)
-- 版本: v1.4.0
-- =============================================

-- 1. 系统用户表
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='sys_user' AND xtype='U')
CREATE TABLE sys_user (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    username        NVARCHAR(64) NOT NULL UNIQUE,
    password_hash   NVARCHAR(128) NOT NULL,
    display_name    NVARCHAR(64) NOT NULL,
    role            NVARCHAR(32) DEFAULT 'ENGINEER',
    status          NVARCHAR(32) DEFAULT 'PENDING',
    reject_reason   NVARCHAR(512),
    reviewed_by     BIGINT,
    reviewed_at     DATETIME2,
    login_failures  INT DEFAULT 0,
    locked_until    DATETIME2,
    last_login_at   DATETIME2,
    created_at      DATETIME2 DEFAULT GETDATE()
);

-- 2. 物料主数据表
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='material' AND xtype='U')
CREATE TABLE material (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    material_code   NVARCHAR(64) NOT NULL UNIQUE,
    material_name   NVARCHAR(256) NOT NULL,
    specification   NVARCHAR(512),
    drawing_no      NVARCHAR(128),
    material_type   NVARCHAR(32) DEFAULT N'零件',
    unit            NVARCHAR(16) DEFAULT N'个',
    material_category NVARCHAR(32),
    validity_from_date DATE,
    validity_to_date   DATE,
    erp_have_drawing   TINYINT,
    weight          DECIMAL(18,6),
    material_attr   NVARCHAR(128),
    source          NVARCHAR(32) DEFAULT 'MANUAL',
    erp_sync_time   DATETIME2,
    created_by      BIGINT,
    created_at      DATETIME2 DEFAULT GETDATE(),
    updated_at      DATETIME2 DEFAULT GETDATE()
);

-- 3. 图纸文件表
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='drawing' AND xtype='U')
CREATE TABLE drawing (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    drawing_code    NVARCHAR(64) NOT NULL UNIQUE,
    drawing_name    NVARCHAR(256) NOT NULL,
    file_format     NVARCHAR(16),
    file_size       BIGINT,
    storage_path    NVARCHAR(512),
    preview_path    NVARCHAR(512),
    preview_status  NVARCHAR(16) DEFAULT 'NONE',
    latest_version  NVARCHAR(32) DEFAULT 'V1.0',
    material_id     BIGINT,
    created_by      BIGINT,
    created_at      DATETIME2 DEFAULT GETDATE()
);
CREATE INDEX idx_drawing_material ON drawing(material_id);

-- 4. 操作日志表
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='audit_log' AND xtype='U')
CREATE TABLE audit_log (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    user_id         BIGINT NOT NULL,
    operation       NVARCHAR(64) NOT NULL,
    target_type     NVARCHAR(64),
    target_id       BIGINT,
    detail          NVARCHAR(1024),
    ip_address      NVARCHAR(64),
    created_at      DATETIME2 DEFAULT GETDATE()
);
CREATE INDEX idx_audit_log_created ON audit_log(created_at DESC);

-- 5. 图纸分享 Token 表
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='share_token' AND xtype='U')
CREATE TABLE share_token (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    token           NVARCHAR(64) NOT NULL UNIQUE,
    material_id     BIGINT NOT NULL,
    created_by      BIGINT NOT NULL,
    expire_at       DATETIME2 NOT NULL,
    visit_count     INT DEFAULT 0,
    last_visit_at   DATETIME2,
    created_at      DATETIME2 DEFAULT GETDATE(),
    is_valid        TINYINT DEFAULT 1
);
CREATE INDEX idx_share_token_token ON share_token(token);
CREATE INDEX idx_share_token_material ON share_token(material_id);

-- 8. ERP 订阅增量游标表
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='erp_sync_cursor' AND xtype='U')
CREATE TABLE erp_sync_cursor (
    cursor_key    NVARCHAR(64) NOT NULL PRIMARY KEY,
    cursor_value  NVARCHAR(256),
    updated_at    DATETIME2 DEFAULT GETDATE()
);

-- =============================================
-- 初始化默认管理员账号
-- 用户名: admin  密码: admin123
-- =============================================
IF NOT EXISTS (SELECT * FROM sys_user WHERE username = 'admin')
    INSERT INTO sys_user (username, password_hash, display_name, role, status)
    VALUES ('admin', '0192023A7BBD73250516F069DF18B500', N'系统管理员', 'ADMIN', 'ACTIVE');

PRINT 'BOM_DB 初始化完成';
GO
