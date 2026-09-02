package com.bom.erp;

import java.util.ArrayList;
import java.util.List;

/**
 * ERP 订阅查询返回的单条异动数据。
 */
public class ErpSscrDetail {

    /** 数据主键（物料编码） */
    private List<String> pkvalues = new ArrayList<>();
    /** 数据的最后修改时间 */
    private String lastoperatetime;
    /** 数据状态：0=非删除，2=删除 */
    private int action;
    /** 数据内容（action=2 时为 null） */
    private ErpMaterial data;

    public List<String> getPkvalues() { return pkvalues; }
    public void setPkvalues(List<String> pkvalues) { this.pkvalues = pkvalues; }
    public String getLastoperatetime() { return lastoperatetime; }
    public void setLastoperatetime(String lastoperatetime) { this.lastoperatetime = lastoperatetime; }
    public int getAction() { return action; }
    public void setAction(int action) { this.action = action; }
    public ErpMaterial getData() { return data; }
    public void setData(ErpMaterial data) { this.data = data; }
}
