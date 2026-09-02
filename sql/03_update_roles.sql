-- =============================================
-- BOM图纸管理系统 - 角色权限更新脚本
-- 将旧角色 VIEWER(生产查看者) 更新为 PURCHASER(采购)
-- =============================================

-- 将现有 VIEWER 角色用户更新为 PURCHASER
UPDATE sys_user SET role = 'PURCHASER' WHERE role = 'VIEWER';

PRINT '已将所有 VIEWER 角色用户更新为 PURCHASER';
GO
