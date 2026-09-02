package com.bom.entity;

import java.util.Date;
import java.util.List;

public class Requisition {

    private Long id;
    private String requisitionNo;       // 单据编号
    private Date requisitionDate;       // 单据日期
    private String requester;           // 采购人员
    private String department;          // 部门
    private String remark;              // 备注
    private Date erpSyncTime;           // ERP同步时间
    private Date createdAt;

    // 非持久化字段
    private Integer itemCount;          // 物料明细数量
    private List<RequisitionItem> items; // 明细列表

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRequisitionNo() { return requisitionNo; }
    public void setRequisitionNo(String requisitionNo) { this.requisitionNo = requisitionNo; }
    public Date getRequisitionDate() { return requisitionDate; }
    public void setRequisitionDate(Date requisitionDate) { this.requisitionDate = requisitionDate; }
    public String getRequester() { return requester; }
    public void setRequester(String requester) { this.requester = requester; }
    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public Date getErpSyncTime() { return erpSyncTime; }
    public void setErpSyncTime(Date erpSyncTime) { this.erpSyncTime = erpSyncTime; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    public List<RequisitionItem> getItems() { return items; }
    public void setItems(List<RequisitionItem> items) { this.items = items; }
}
