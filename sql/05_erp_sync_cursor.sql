-- =============================================
-- BOM图纸管理系统 - ERP 订阅增量游标表
-- 数据库: BOM_DB (SQL Server 2019)
-- 版本: v1.8.0
-- 说明: 记录 ERP 订阅查询的增量游标（最后修改时间 / 最后主键），服务重启后继续增量
-- =============================================

IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='erp_sync_cursor' AND xtype='U')
CREATE TABLE erp_sync_cursor (
    cursor_key    NVARCHAR(64) NOT NULL PRIMARY KEY,
    cursor_value  NVARCHAR(256),
    updated_at    DATETIME2 DEFAULT GETDATE()
);

PRINT 'erp_sync_cursor 表创建完成';
GO
