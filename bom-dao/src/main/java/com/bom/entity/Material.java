package com.bom.entity;

import java.math.BigDecimal;
import java.util.Date;

public class Material {

    private Long id;
    private String materialCode;
    private String materialName;
    private String specification;
    private String drawingNo;          // 图号
    private String materialType;
    private String unit;
    private String materialCategory;     // 物料类别（ERP MaterialCategoryId）
    private String validityFromDate;     // 有效期从（yyyy-MM-dd，ERP 同步）
    private String validityToDate;       // 有效期至（yyyy-MM-dd，ERP 同步）
    private Integer erpHaveDrawing;      // ERP 是否存在图纸标记（0=否 1=是，ERP 同步）
    private BigDecimal weight;
    private String materialAttr;
    private String source;
    private Date erpSyncTime;
    private Long createdBy;
    private Date createdAt;
    private Date updatedAt;

    // 非持久化：图纸统计字段（从 drawing 表 JOIN 计算）
    private Boolean hasDrawing;        // 是否有图纸
    private Boolean has3d;             // 是否有三维(STEP/STP)
    private Boolean hasEngineering;    // 是否有工程图(DWG/DXF)
    private Date drawingAddDate;       // 图纸新增日期
    private Date drawingUpdateDate;    // 图纸修改时间
    private Integer drawingCount;      // 图纸数量

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }
    public String getDrawingNo() { return drawingNo; }
    public void setDrawingNo(String drawingNo) { this.drawingNo = drawingNo; }
    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getMaterialCategory() { return materialCategory; }
    public void setMaterialCategory(String materialCategory) { this.materialCategory = materialCategory; }
    public String getValidityFromDate() { return validityFromDate; }
    public void setValidityFromDate(String validityFromDate) { this.validityFromDate = validityFromDate; }
    public String getValidityToDate() { return validityToDate; }
    public void setValidityToDate(String validityToDate) { this.validityToDate = validityToDate; }
    public Integer getErpHaveDrawing() { return erpHaveDrawing; }
    public void setErpHaveDrawing(Integer erpHaveDrawing) { this.erpHaveDrawing = erpHaveDrawing; }
    public BigDecimal getWeight() { return weight; }
    public void setWeight(BigDecimal weight) { this.weight = weight; }
    public String getMaterialAttr() { return materialAttr; }
    public void setMaterialAttr(String materialAttr) { this.materialAttr = materialAttr; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Date getErpSyncTime() { return erpSyncTime; }
    public void setErpSyncTime(Date erpSyncTime) { this.erpSyncTime = erpSyncTime; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }

    public Boolean getHasDrawing() { return hasDrawing; }
    public void setHasDrawing(Boolean hasDrawing) { this.hasDrawing = hasDrawing; }
    public Boolean getHas3d() { return has3d; }
    public void setHas3d(Boolean has3d) { this.has3d = has3d; }
    public Boolean getHasEngineering() { return hasEngineering; }
    public void setHasEngineering(Boolean hasEngineering) { this.hasEngineering = hasEngineering; }
    public Date getDrawingAddDate() { return drawingAddDate; }
    public void setDrawingAddDate(Date drawingAddDate) { this.drawingAddDate = drawingAddDate; }
    public Date getDrawingUpdateDate() { return drawingUpdateDate; }
    public void setDrawingUpdateDate(Date drawingUpdateDate) { this.drawingUpdateDate = drawingUpdateDate; }
    public Integer getDrawingCount() { return drawingCount; }
    public void setDrawingCount(Integer drawingCount) { this.drawingCount = drawingCount; }
}
