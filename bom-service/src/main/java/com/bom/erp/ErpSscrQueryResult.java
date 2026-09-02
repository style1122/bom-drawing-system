package com.bom.erp;

import java.util.ArrayList;
import java.util.List;

/**
 * ERP 订阅查询（sscrquery）结果。
 */
public class ErpSscrQueryResult {

    private String funcid;
    /** 数据是否取回完毕 */
    private boolean hasnext;
    /** 本次返回数据中最后一笔的最后修改时间（下次查询的 timestamp） */
    private String lastoperatetime;
    /** 本次返回数据中最后一笔的主键（下次查询的 pkvalues） */
    private List<String> pkvalues = new ArrayList<>();
    /** 异动明细 */
    private List<ErpSscrDetail> detail = new ArrayList<>();
    private int status;
    private String error;

    public String getFuncid() { return funcid; }
    public void setFuncid(String funcid) { this.funcid = funcid; }
    public boolean isHasnext() { return hasnext; }
    public void setHasnext(boolean hasnext) { this.hasnext = hasnext; }
    public String getLastoperatetime() { return lastoperatetime; }
    public void setLastoperatetime(String lastoperatetime) { this.lastoperatetime = lastoperatetime; }
    public List<String> getPkvalues() { return pkvalues; }
    public void setPkvalues(List<String> pkvalues) { this.pkvalues = pkvalues; }
    public List<ErpSscrDetail> getDetail() { return detail; }
    public void setDetail(List<ErpSscrDetail> detail) { this.detail = detail; }
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
}
