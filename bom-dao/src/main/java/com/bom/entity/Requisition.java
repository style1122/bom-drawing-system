package com.bom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;
import java.util.List;

@TableName("requisition")
public class Requisition {

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("requisition_no")
    private String requisitionNo;       // 单据编号
    @TableField("requisition_date")
    private Date requisitionDate;       // 单据日期
    private String requester;           // 采购人员
    private String department;          // 部门
    private String remark;              // 备注
    @TableField("erp_sync_time")
    private Date erpSyncTime;           // ERP同步时间
    @TableField("created_at")
    private Date createdAt;

    // 非持久化字段
    @TableField(exist = false)
    private Integer itemCount;          // 物料明细数量
    @TableField(exist = false)
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
