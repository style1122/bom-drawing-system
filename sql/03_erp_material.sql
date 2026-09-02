-- =============================================
-- BOM图纸管理系统 - 正航T9 ERP 物料同步升级脚本
-- 数据库: BOM_DB (SQL Server 2019)
-- 版本: v1.6.0
-- 说明: 为 material 表补充 ERP 物料基础数据字段
-- =============================================

-- 物料类别（ERP MaterialCategoryId）
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('material') AND name = 'material_category')
ALTER TABLE material ADD material_category NVARCHAR(32);

-- 有效期从（ERP ValidityFromDate）
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('material') AND name = 'validity_from_date')
ALTER TABLE material ADD validity_from_date DATE;

-- 有效期至（ERP ValidityToDate）
IF NOT EXISTS (SELECT * FROM sys.columns WHERE object_id = OBJECT_ID('material') AND name = 'validity_to_date')
ALTER TABLE material ADD validity_to_date DATE;

PRINT 'material 表 ERP 字段补充完成';
GO
