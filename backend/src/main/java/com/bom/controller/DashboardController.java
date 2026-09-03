package com.bom.controller;

import com.bom.entity.AuditLog;
import com.bom.mapper.DrawingMapper;
import com.bom.mapper.SysUserMapper;
import com.bom.service.AuditLogService;
import com.bom.service.DashboardService;
import com.bom.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DrawingMapper drawingMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private DashboardService dashboardService;

    @GetMapping("/stats")
    public Result stats() {
        Map<String, Object> data = new HashMap<>();
        data.put("drawingCount", drawingMapper.findAll().size());
        data.put("activeUserCount", sysUserMapper.findApprovedUsers().size());

        List<AuditLog> recentLogs = auditLogService.getRecent(100);
        data.put("recentLogs", recentLogs);

        // 图纸上传量与存储占用统计（每日上传、今日上传、总存储、增长趋势）
        data.putAll(dashboardService.getDrawingStats());

        return Result.success(data);
    }
}
