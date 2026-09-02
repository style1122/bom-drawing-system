package com.bom.erp;

import java.util.Date;

/**
 * ERP 物料同步结果统计。
 */
public class ErpSyncResult {

    private boolean success;
    private String message;
    /** 从 ERP 拉取的物料条数 */
    private int fetched;
    /** 新增条数 */
    private int inserted;
    /** 更新条数 */
    private int updated;
    /** 失败条数 */
    private int failed;
    /** 请求页数 */
    private int pages;
    /** 查询条件 */
    private String condition;
    private Date startTime;
    private Date endTime;
    private long durationSeconds;

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public int getFetched() { return fetched; }
    public void setFetched(int fetched) { this.fetched = fetched; }
    public int getInserted() { return inserted; }
    public void setInserted(int inserted) { this.inserted = inserted; }
    public int getUpdated() { return updated; }
    public void setUpdated(int updated) { this.updated = updated; }
    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }
    public int getPages() { return pages; }
    public void setPages(int pages) { this.pages = pages; }
    public String getCondition() { return condition; }
    public void setCondition(String condition) { this.condition = condition; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    public long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(long durationSeconds) { this.durationSeconds = durationSeconds; }
}
