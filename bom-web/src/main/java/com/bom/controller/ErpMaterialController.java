package com.bom.controller;

import com.bom.erp.ErpApiClient;
import com.bom.erp.ErpMaterialSyncService;
import com.bom.erp.ErpSyncResult;
import com.bom.exception.BusinessException;
import com.bom.service.AuditLogService;
import com.bom.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 正航 T9 ERP 物料基础数据对接接口。
 */
@RestController
@RequestMapping("/api/material/erp")
public class ErpMaterialController {

    @Autowired
    private ErpApiClient erpApiClient;

    @Autowired
    private ErpMaterialSyncService erpMaterialSyncService;

    @Autowired
    private AuditLogService auditLogService;

    /**
     * 测试 ERP 连接与认证凭据。
     */
    @PostMapping("/test")
    public ResponseEntity<Result> testConnection(HttpServletRequest request) {
        try {
            Map<String, Object> data = erpApiClient.testConnection();
            Long userId = (Long) request.getAttribute("userId");
            auditLogService.log(userId, "ERP连接测试", "MATERIAL", null, "测试正航T9 ERP连接", request.getRemoteAddr());
            return ResponseEntity.ok(Result.success("ERP连接成功", data));
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error(e.getCode(), e.getMessage()));
        }
    }

    /**
     * 手动触发 ERP 物料同步。
     * 请求体可选：{"condition": "MaterialId = 'xxx'"}，缺省为全量同步。
     */
    @PostMapping("/sync")
    public ResponseEntity<Result> sync(@RequestBody(required = false) Map<String, String> body, HttpServletRequest request) {
        try {
            String condition = body == null ? null : body.get("condition");
            ErpSyncResult result = erpMaterialSyncService.syncMaterials(condition);
            Long userId = (Long) request.getAttribute("userId");
            auditLogService.log(userId, "ERP同步", "MATERIAL", null,
                    result.getMessage(), request.getRemoteAddr());
            return result.isSuccess()
                    ? ResponseEntity.ok(Result.success(result.getMessage(), result))
                    : ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Result.error(result.getMessage()));
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error(e.getCode(), e.getMessage()));
        }
    }

    /**
     * 查询最近一次同步结果。
     */
    @GetMapping("/status")
    public Result status() {
        return Result.success(erpMaterialSyncService.getLastResult());
    }

    /**
     * 查询当前生效的 ERP 连接配置（不含密钥），用于排查部署是否生效。
     */
    @GetMapping("/config")
    public Result config() {
        return Result.success(erpApiClient.getConfigInfo());
    }

    /**
     * 手动同步指定物料的“是否存在图纸”标记到 ERP（CU_HaveDrawing）。
     * 根据本地 drawing 表统计结果推送 1/0。
     * 请求体：{"materialCode": "100400009193"}
     */
    @PostMapping("/sync-drawing-flag")
    public ResponseEntity<Result> syncDrawingFlag(@RequestBody Map<String, String> body, HttpServletRequest request) {
        String materialCode = body == null ? null : body.get("materialCode");
        if (materialCode == null || materialCode.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Result.error("缺少参数 materialCode"));
        }
        try {
            Map<String, Object> result = erpMaterialSyncService.syncDrawingFlagForMaterial(materialCode.trim());
            Long userId = (Long) request.getAttribute("userId");
            auditLogService.log(userId, "ERP图纸标记同步", "MATERIAL", null,
                    String.valueOf(result.get("message")), request.getRemoteAddr());
            return ResponseEntity.ok(Result.success(String.valueOf(result.get("message")), result));
        } catch (BusinessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error(e.getCode(), e.getMessage()));
        }
    }
}
