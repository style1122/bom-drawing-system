-- =============================================
-- BOM图纸管理系统 - 采购订单管理表
-- 数据库: BOM_DB (SQL Server 2019)
-- 版本: v1.5.0
-- =============================================

-- 6. 采购订单主表
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='requisition' AND xtype='U')
CREATE TABLE requisition (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    requisition_no  NVARCHAR(64) NOT NULL UNIQUE,   -- 单据编号
    requisition_date DATETIME2 NOT NULL,             -- 单据日期
    requester       NVARCHAR(64) NOT NULL,           -- 采购人员
    department      NVARCHAR(128),                   -- 部门
    remark          NVARCHAR(512),                   -- 备注
    erp_sync_time   DATETIME2,                       -- ERP同步时间
    created_at      DATETIME2 DEFAULT GETDATE()
);
CREATE INDEX idx_requisition_date ON requisition(requisition_date DESC);
CREATE INDEX idx_requisition_requester ON requisition(requester);

-- 7. 采购订单明细表
IF NOT EXISTS (SELECT * FROM sysobjects WHERE name='requisition_item' AND xtype='U')
CREATE TABLE requisition_item (
    id              BIGINT IDENTITY(1,1) PRIMARY KEY,
    requisition_id  BIGINT NOT NULL,                 -- 关联采购订单ID
    material_code   NVARCHAR(64) NOT NULL,           -- 物料编码
    material_name   NVARCHAR(256) NOT NULL,          -- 物料名称
    specification   NVARCHAR(512),                   -- 规格型号
    quantity        DECIMAL(18,4) NOT NULL DEFAULT 0,-- 采购数量
    unit            NVARCHAR(16) DEFAULT N'个',      -- 单位
    remark          NVARCHAR(512),                   -- 备注
    created_at      DATETIME2 DEFAULT GETDATE(),
    CONSTRAINT FK_requisition_item FOREIGN KEY (requisition_id) REFERENCES requisition(id)
);
CREATE INDEX idx_req_item_requisition ON requisition_item(requisition_id);
CREATE INDEX idx_req_item_material_code ON requisition_item(material_code);

-- =============================================
-- 初始化测试数据
-- =============================================

-- 采购订单主表测试数据
IF NOT EXISTS (SELECT * FROM requisition WHERE requisition_no = 'PR-2026-0001')
INSERT INTO requisition (requisition_no, requisition_date, requester, department, remark, erp_sync_time)
VALUES
('PR-2026-0001', '2026-07-10', N'张工', N'研发部', N'新项目物料采购', '2026-07-10 08:00:00'),
('PR-2026-0002', '2026-07-12', N'李明', N'生产部', N'产线补料采购', '2026-07-12 09:30:00'),
('PR-2026-0003', '2026-07-15', N'王芳', N'研发部', N'样机试制采购', '2026-07-15 14:00:00'),
('PR-2026-0004', '2026-07-16', N'赵强', N'采购部', N'常规采购', '2026-07-16 10:00:00'),
('PR-2026-0005', '2026-07-18', N'张工', N'研发部', N'设计变更物料采购', '2026-07-18 11:00:00');

-- 采购订单明细测试数据（物料编码需与 material 表中的数据对应）
INSERT INTO requisition_item (requisition_id, material_code, material_name, specification, quantity, unit, remark)
SELECT r.id, 'MAT-001', N'法兰盘', N'OD100 ID40 厚10mm', 50, N'个', N'标准件'
FROM requisition r WHERE r.requisition_no = 'PR-2026-0001'
AND NOT EXISTS (SELECT 1 FROM requisition_item ri WHERE ri.requisition_id = r.id AND ri.material_code = 'MAT-001');

INSERT INTO requisition_item (requisition_id, material_code, material_name, specification, quantity, unit, remark)
SELECT r.id, 'MAT-002', N'螺栓', N'M8x30 不锈钢304', 200, N'个', N''
FROM requisition r WHERE r.requisition_no = 'PR-2026-0001'
AND NOT EXISTS (SELECT 1 FROM requisition_item ri WHERE ri.requisition_id = r.id AND ri.material_code = 'MAT-002');

INSERT INTO requisition_item (requisition_id, material_code, material_name, specification, quantity, unit, remark)
SELECT r.id, 'MAT-003', N'密封圈', N'OD50 内径40 氟橡胶', 100, N'个', N''
FROM requisition r WHERE r.requisition_no = 'PR-2026-0001'
AND NOT EXISTS (SELECT 1 FROM requisition_item ri WHERE ri.requisition_id = r.id AND ri.material_code = 'MAT-003');

-- 第二张采购订单
INSERT INTO requisition_item (requisition_id, material_code, material_name, specification, quantity, unit, remark)
SELECT r.id, 'MAT-001', N'法兰盘', N'OD100 ID40 厚10mm', 30, N'个', N''
FROM requisition r WHERE r.requisition_no = 'PR-2026-0002'
AND NOT EXISTS (SELECT 1 FROM requisition_item ri WHERE ri.requisition_id = r.id AND ri.material_code = 'MAT-001');

INSERT INTO requisition_item (requisition_id, material_code, material_name, specification, quantity, unit, remark)
SELECT r.id, 'MAT-004', N'电机支架', N'铝合金 150x100x80mm', 10, N'个', N'定制件'
FROM requisition r WHERE r.requisition_no = 'PR-2026-0002'
AND NOT EXISTS (SELECT 1 FROM requisition_item ri WHERE ri.requisition_id = r.id AND ri.material_code = 'MAT-004');

-- 第三张采购订单
INSERT INTO requisition_item (requisition_id, material_code, material_name, specification, quantity, unit, remark)
SELECT r.id, 'MAT-002', N'螺栓', N'M8x30 不锈钢304', 500, N'个', N''
FROM requisition r WHERE r.requisition_no = 'PR-2026-0003'
AND NOT EXISTS (SELECT 1 FROM requisition_item ri WHERE ri.requisition_id = r.id AND ri.material_code = 'MAT-002');

INSERT INTO requisition_item (requisition_id, material_code, material_name, specification, quantity, unit, remark)
SELECT r.id, 'MAT-005', N'轴承座', N'UCP205 铸铁', 20, N'套', N''
FROM requisition r WHERE r.requisition_no = 'PR-2026-0003'
AND NOT EXISTS (SELECT 1 FROM requisition_item ri WHERE ri.requisition_id = r.id AND ri.material_code = 'MAT-005');

-- 第四张采购订单
INSERT INTO requisition_item (requisition_id, material_code, material_name, specification, quantity, unit, remark)
SELECT r.id, 'MAT-003', N'密封圈', N'OD50 内径40 氟橡胶', 80, N'个', N''
FROM requisition r WHERE r.requisition_no = 'PR-2026-0004'
AND NOT EXISTS (SELECT 1 FROM requisition_item ri WHERE ri.requisition_id = r.id AND ri.material_code = 'MAT-003');

INSERT INTO requisition_item (requisition_id, material_code, material_name, specification, quantity, unit, remark)
SELECT r.id, 'MAT-006', N'传感器', N'电感式 M12 检测距离4mm', 15, N'个', N''
FROM requisition r WHERE r.requisition_no = 'PR-2026-0004'
AND NOT EXISTS (SELECT 1 FROM requisition_item ri WHERE ri.requisition_id = r.id AND ri.material_code = 'MAT-006');

-- 第五张采购订单
INSERT INTO requisition_item (requisition_id, material_code, material_name, specification, quantity, unit, remark)
SELECT r.id, 'MAT-004', N'电机支架', N'铝合金 150x100x80mm', 5, N'个', N''
FROM requisition r WHERE r.requisition_no = 'PR-2026-0005'
AND NOT EXISTS (SELECT 1 FROM requisition_item ri WHERE ri.requisition_id = r.id AND ri.material_code = 'MAT-004');

INSERT INTO requisition_item (requisition_id, material_code, material_name, specification, quantity, unit, remark)
SELECT r.id, 'MAT-005', N'轴承座', N'UCP205 铸铁', 8, N'套', N''
FROM requisition r WHERE r.requisition_no = 'PR-2026-0005'
AND NOT EXISTS (SELECT 1 FROM requisition_item ri WHERE ri.requisition_id = r.id AND ri.material_code = 'MAT-005');

INSERT INTO requisition_item (requisition_id, material_code, material_name, specification, quantity, unit, remark)
SELECT r.id, 'MAT-006', N'传感器', N'电感式 M12 检测距离4mm', 3, N'个', N''
FROM requisition r WHERE r.requisition_no = 'PR-2026-0005'
AND NOT EXISTS (SELECT 1 FROM requisition_item ri WHERE ri.requisition_id = r.id AND ri.material_code = 'MAT-006');

PRINT '采购订单管理表初始化完成';
GO
