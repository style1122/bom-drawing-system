-- =============================================
-- BOM图纸管理系统 - 物料 ERP 图纸标记字段
-- 数据库: BOM_DB (SQL Server 2019)
-- 版本: v1.7.0
-- 说明: material 表补充 erp_have_drawing 字段（0=否 1=是，记录 ERP CU_HaveDrawing 同步结果）
-- =============================================

IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('material') AND name = 'erp_have_drawing')
ALTER TABLE material ADD erp_have_drawing TINYINT;

PRINT 'material 表 erp_have_drawing 字段补充完成';
GO
