package com.bom.erp;

/**
 * 正航 T9 ERP 物料基础数据（Mat01 / MaterialGroup）。
 */
public class ErpMaterial {

    /** 物料代码（主键） */
    private String materialId;
    /** 物料名称 */
    private String materialName;
    /** 物料类型代码 */
    private String materialTypeId;
    /** 物料规格 */
    private String materialSpec;
    /** 物料类别代码 */
    private String materialCategoryId;
    /** 基本单位代码 */
    private String unitId;
    /** 有效期从（yyyy-MM-dd） */
    private String validityFromDate;
    /** 有效期至（yyyy-MM-dd） */
    private String validityToDate;
    /** 是否存在图纸（订阅/查询返回时可能有值） */
    private Boolean cuHaveDrawing;

    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getMaterialTypeId() { return materialTypeId; }
    public void setMaterialTypeId(String materialTypeId) { this.materialTypeId = materialTypeId; }
    public String getMaterialSpec() { return materialSpec; }
    public void setMaterialSpec(String materialSpec) { this.materialSpec = materialSpec; }
    public String getMaterialCategoryId() { return materialCategoryId; }
    public void setMaterialCategoryId(String materialCategoryId) { this.materialCategoryId = materialCategoryId; }
    public String getUnitId() { return unitId; }
    public void setUnitId(String unitId) { this.unitId = unitId; }
    public String getValidityFromDate() { return validityFromDate; }
    public void setValidityFromDate(String validityFromDate) { this.validityFromDate = validityFromDate; }
    public String getValidityToDate() { return validityToDate; }
    public void setValidityToDate(String validityToDate) { this.validityToDate = validityToDate; }
    public Boolean getCuHaveDrawing() { return cuHaveDrawing; }
    public void setCuHaveDrawing(Boolean cuHaveDrawing) { this.cuHaveDrawing = cuHaveDrawing; }
}
