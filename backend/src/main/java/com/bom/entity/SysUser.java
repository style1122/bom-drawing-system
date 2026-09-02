package com.bom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    @TableField("password_hash")
    private String passwordHash;
    @TableField("display_name")
    private String displayName;
    private String role;
    private String status;
    @TableField("reject_reason")
    private String rejectReason;
    @TableField("reviewed_by")
    private Long reviewedBy;
    @TableField("reviewed_at")
    private Date reviewedAt;
    @TableField("login_failures")
    private Integer loginFailures;
    @TableField("locked_until")
    private Date lockedUntil;
    @TableField("last_login_at")
    private Date lastLoginAt;
    @TableField("created_at")
    private Date createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }
    public Date getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(Date reviewedAt) { this.reviewedAt = reviewedAt; }
    public Integer getLoginFailures() { return loginFailures; }
    public void setLoginFailures(Integer loginFailures) { this.loginFailures = loginFailures; }
    public Date getLockedUntil() { return lockedUntil; }
    public void setLockedUntil(Date lockedUntil) { this.lockedUntil = lockedUntil; }
    public Date getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(Date lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
