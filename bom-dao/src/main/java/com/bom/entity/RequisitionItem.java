package com.bom.entity;

import java.math.BigDecimal;
import java.util.Date;

public class RequisitionItem {

    private Long id;
    private Long requisitionId;
    private String materialCode;        // 物料编码
    private String materialName;        // 物料名称
    private String specification;       // 规格型号
    private BigDecimal quantity;        // 采购数量
    private String unit;                // 单位
    private String remark;              // 备注
    private Date createdAt;

    // 非持久化字段：关联图纸信息
    private Long materialId;            // 关联物料ID
    private Boolean hasDrawing;         // 是否有图纸
    private Long drawingId;             // 最新PDF图纸ID
    private String drawingName;         // 图纸文件名
    private Integer drawingCount;       // 图纸总数

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getRequisitionId() { return requisitionId; }
    public void setRequisitionId(Long requisitionId) { this.requisitionId = requisitionId; }
    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getSpecification() { return specification; }
    public void setSpecification(String specification) { this.specification = specification; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public Boolean getHasDrawing() { return hasDrawing; }
    public void setHasDrawing(Boolean hasDrawing) { this.hasDrawing = hasDrawing; }
    public Long getDrawingId() { return drawingId; }
    public void setDrawingId(Long drawingId) { this.drawingId = drawingId; }
    public String getDrawingName() { return drawingName; }
    public void setDrawingName(String drawingName) { this.drawingName = drawingName; }
    public Integer getDrawingCount() { return drawingCount; }
    public void setDrawingCount(Integer drawingCount) { this.drawingCount = drawingCount; }
}
