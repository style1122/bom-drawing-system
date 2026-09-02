package com.bom.entity;

import java.util.Date;

/**
 * 图纸分享 Token 实体
 */
public class ShareToken {
    private Long id;
    private String token;
    private Long materialId;
    private Long createdBy;
    private Date expireAt;
    private Integer visitCount;
    private Date lastVisitAt;
    private Date createdAt;
    private Integer isValid;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }

    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }

    public Date getExpireAt() { return expireAt; }
    public void setExpireAt(Date expireAt) { this.expireAt = expireAt; }

    public Integer getVisitCount() { return visitCount; }
    public void setVisitCount(Integer visitCount) { this.visitCount = visitCount; }

    public Date getLastVisitAt() { return lastVisitAt; }
    public void setLastVisitAt(Date lastVisitAt) { this.lastVisitAt = lastVisitAt; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Integer getIsValid() { return isValid; }
    public void setIsValid(Integer isValid) { this.isValid = isValid; }
}
