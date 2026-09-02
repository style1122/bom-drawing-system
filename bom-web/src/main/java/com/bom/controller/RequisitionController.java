package com.bom.controller;

import com.bom.entity.Requisition;
import com.bom.entity.SysUser;
import com.bom.exception.BusinessException;
import com.bom.service.AuditLogService;
import com.bom.service.RequisitionService;
import com.bom.service.UserService;
import com.bom.util.PageResult;
import com.bom.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/requisition")
public class RequisitionController {

    private static final Logger logger = LoggerFactory.getLogger(RequisitionController.class);

    @Autowired
    private RequisitionService requisitionService;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private UserService userService;

    /**
     * 获取当前登录用户的采购人员名称（仅采购角色需要过滤）
     * 返回 null 表示不做过滤（管理员/研发可查看全部）
     */
    private String getPurchaserFilter(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String userRole = (String) request.getAttribute("userRole");
        if ("PURCHASER".equals(userRole) && userId != null) {
            SysUser user = userService.getById(userId);
            if (user != null) {
                return user.getDisplayName();
            }
        }
        return null;
    }

    /**
     * 分页查询采购订单列表
     * 采购角色只能看到采购人员是自己名字的订单
     */
    @GetMapping("/list")
    public Result list(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size,
                       HttpServletRequest request) {
        String purchaserFilter = getPurchaserFilter(request);
        PageResult<Requisition> result = requisitionService.getAllPaged(page, size, purchaserFilter);
        return Result.success(result);
    }

    /**
     * 搜索采购订单
     * 采购角色只能看到采购人员是自己名字的订单
     */
    @GetMapping("/search")
    public Result search(@RequestParam(required = false) String keyword,
                         @RequestParam(defaultValue = "1") int page,
                         @RequestParam(defaultValue = "20") int size,
                         HttpServletRequest request) {
        String purchaserFilter = getPurchaserFilter(request);
        PageResult<Requisition> result = requisitionService.searchPaged(keyword, page, size, purchaserFilter);
        return Result.success(result);
    }

    /**
     * 获取采购订单详情（含物料明细 + 图纸关联信息）
     * 采购角色只能查看自己名字的订单
     */
    @GetMapping("/{id}")
    public Result detail(@PathVariable Long id, HttpServletRequest request) {
        Requisition requisition = requisitionService.getDetailById(id);
        // 采购角色权限校验：只能查看自己的订单
        String purchaserFilter = getPurchaserFilter(request);
        if (purchaserFilter != null && !purchaserFilter.equals(requisition.getRequester())) {
            return Result.error("无权查看该采购订单");
        }
        return Result.success(requisition);
    }

    /**
     * 导出采购订单为 Excel
     */
    @GetMapping("/export/excel/{id}")
    public void exportExcel(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response) {
        try {
            // 采购角色权限校验
            String purchaserFilter = getPurchaserFilter(request);
            Requisition requisition = requisitionService.getDetailById(id);
            if (purchaserFilter != null && !purchaserFilter.equals(requisition.getRequester())) {
                handleError(response, 403, "无权导出该采购订单");
                return;
            }

            byte[] data = requisitionService.exportExcel(id);

            String fileName = "采购订单_" + requisition.getRequisitionNo() + ".xlsx";
            String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replace("+", "%20");

            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
            response.setContentLength(data.length);

            Long userId = (Long) request.getAttribute("userId");
            auditLogService.log(userId, "导出Excel", "REQUISITION", id,
                    "采购订单: " + requisition.getRequisitionNo(), request.getRemoteAddr());

            try (OutputStream os = response.getOutputStream()) {
                os.write(data);
                os.flush();
            }
        } catch (BusinessException e) {
            logger.warn("导出Excel业务异常 requisitionId={}, msg={}", id, e.getMessage());
            handleError(response, 200, e.getMessage());
        } catch (Throwable e) {
            logger.error("导出Excel失败 requisitionId={}", id, e);
            handleError(response, 500, "导出Excel失败: " + e.getMessage());
        }
    }

    /**
     * 导出采购订单中所有物料的图纸（ZIP 包）
     */
    @GetMapping("/export/drawings/{id}")
    public void exportDrawings(@PathVariable Long id, HttpServletRequest request, HttpServletResponse response) {
        try {
            // 采购角色权限校验
            String purchaserFilter = getPurchaserFilter(request);
            Requisition requisition = requisitionService.getDetailById(id);
            if (purchaserFilter != null && !purchaserFilter.equals(requisition.getRequester())) {
                handleError(response, 403, "无权导出该采购订单图纸");
                return;
            }

            byte[] data = requisitionService.exportDrawings(id);

            String fileName = "图纸包_" + requisition.getRequisitionNo() + ".zip";
            String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name()).replace("+", "%20");

            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedName);
            response.setContentLength(data.length);

            Long userId = (Long) request.getAttribute("userId");
            auditLogService.log(userId, "导出图纸", "REQUISITION", id,
                    "采购订单: " + requisition.getRequisitionNo(), request.getRemoteAddr());

            try (OutputStream os = response.getOutputStream()) {
                os.write(data);
                os.flush();
            }
        } catch (BusinessException e) {
            logger.warn("导出图纸业务异常 requisitionId={}, msg={}", id, e.getMessage());
            handleError(response, 200, e.getMessage());
        } catch (Throwable e) {
            logger.error("导出图纸失败 requisitionId={}", id, e);
            handleError(response, 500, "导出图纸失败: " + e.getMessage());
        }
    }

    private void handleError(HttpServletResponse response, int status, String msg) {
        try {
            if (!response.isCommitted()) {
                response.reset();
                response.setStatus(status);
                response.setContentType("application/json;charset=UTF-8");
                // 转义消息中的特殊字符，避免 JSON 格式错误
                String safeMsg = msg.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
                response.getWriter().write("{\"code\":" + status + ",\"msg\":\"" + safeMsg + "\",\"data\":null}");
            }
        } catch (Exception ignored) {
        }
    }
}
