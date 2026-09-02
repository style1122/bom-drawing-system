package com.bom.entity;

import java.util.Date;

public class AuditLog {

    private Long id;
    private Long userId;
    private String operation;
    private String targetType;
    private Long targetId;
    private String detail;
    private String ipAddress;
    private Date createdAt;

    // 非持久化字段，联表查询时填充
    private String userName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public Long getTargetId() { return targetId; }
    public void setTargetId(Long targetId) { this.targetId = targetId; }
    public String getDetail() { return detail; }
    public void setDetail(String detail) { this.detail = detail; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
}
