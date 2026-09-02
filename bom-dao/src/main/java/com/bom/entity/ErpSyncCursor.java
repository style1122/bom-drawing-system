package com.bom.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.util.Date;

/**
 * ERP 订阅增量游标。
 * 主键为业务赋值字符串键（cursorKey），非自增，使用 IdType.INPUT。
 */
@TableName("erp_sync_cursor")
public class ErpSyncCursor {

    @TableId(value = "cursor_key", type = IdType.INPUT)
    private String cursorKey;
    @TableField("cursor_value")
    private String cursorValue;
    @TableField("updated_at")
    private Date updatedAt;

    public String getCursorKey() { return cursorKey; }
    public void setCursorKey(String cursorKey) { this.cursorKey = cursorKey; }
    public String getCursorValue() { return cursorValue; }
    public void setCursorValue(String cursorValue) { this.cursorValue = cursorValue; }
    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
