package com.bom.entity;

import java.util.Date;

public class Drawing {

    private Long id;
    private String drawingCode;
    private String drawingName;
    private String fileFormat;
    private Long fileSize;
    private String storagePath;
    private String previewPath;
    private String previewStatus;
    private String latestVersion;
    private Long materialId;
    private Long createdBy;
    private Date createdAt;

    // 关联字段（非持久化）
    private String materialCode;
    private String materialName;
    private String materialSpec;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDrawingCode() { return drawingCode; }
    public void setDrawingCode(String drawingCode) { this.drawingCode = drawingCode; }
    public String getDrawingName() { return drawingName; }
    public void setDrawingName(String drawingName) { this.drawingName = drawingName; }
    public String getFileFormat() { return fileFormat; }
    public void setFileFormat(String fileFormat) { this.fileFormat = fileFormat; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public String getPreviewPath() { return previewPath; }
    public void setPreviewPath(String previewPath) { this.previewPath = previewPath; }
    public String getPreviewStatus() { return previewStatus; }
    public void setPreviewStatus(String previewStatus) { this.previewStatus = previewStatus; }
    public String getLatestVersion() { return latestVersion; }
    public void setLatestVersion(String latestVersion) { this.latestVersion = latestVersion; }
    public Long getMaterialId() { return materialId; }
    public void setMaterialId(Long materialId) { this.materialId = materialId; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public String getMaterialCode() { return materialCode; }
    public void setMaterialCode(String materialCode) { this.materialCode = materialCode; }
    public String getMaterialName() { return materialName; }
    public void setMaterialName(String materialName) { this.materialName = materialName; }
    public String getMaterialSpec() { return materialSpec; }
    public void setMaterialSpec(String materialSpec) { this.materialSpec = materialSpec; }
}
