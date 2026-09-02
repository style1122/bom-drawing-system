package com.bom.entity;

import java.util.Date;

/**
 * ERP 订阅增量游标。
 */
public class ErpSyncCursor {

    private String cursorKey;
    private String cursorValue;
    private Date updatedAt;

    public String getCursorKey() { return cursorKey; }
    public void setCursorKey(String cursorKey) { this.cursorKey = cursorKey; }
    public String getCursorValue() { return cursorValue; }
    public void setCursorValue(String cursorValue) { this.cursorValue = cursorValue; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
