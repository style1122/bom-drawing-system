package com.bom.erp;

import java.util.ArrayList;
import java.util.List;

/**
 * ERP getlist 清单查询结果。
 */
public class ErpGetListResult {

    /** 执行结果：1 成功 / 0 失败 */
    private int status;
    /** 错误信息 */
    private String error;
    /** 错误码 */
    private String errorcode;
    /** 本页物料列表 */
    private List<ErpMaterial> materials = new ArrayList<>();
    /** 最后一笔主键，用于下一页查询 */
    private String lastpkvalues;
    /** 是否还有数据 */
    private boolean hasnext;

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public String getError() { return error; }
    public void setError(String error) { this.error = error; }
    public String getErrorcode() { return errorcode; }
    public void setErrorcode(String errorcode) { this.errorcode = errorcode; }
    public List<ErpMaterial> getMaterials() { return materials; }
    public void setMaterials(List<ErpMaterial> materials) { this.materials = materials; }
    public String getLastpkvalues() { return lastpkvalues; }
    public void setLastpkvalues(String lastpkvalues) { this.lastpkvalues = lastpkvalues; }
    public boolean isHasnext() { return hasnext; }
    public void setHasnext(boolean hasnext) { this.hasnext = hasnext; }
}
