package com.bom.controller;

import com.bom.service.ShareService;
import com.bom.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 分享管理 API（需登录认证）
 * 路径 /api/share/** 会被 LoginInterceptor 拦截
 */
@RestController
@RequestMapping("/api/share")
public class ShareController {

    @Autowired
    private ShareService shareService;

    /**
     * 创建分享链接
     * POST /api/share/create
     * Body: { "materialId": 123 }
     */
    @PostMapping("/create")
    public Result createShare(@RequestBody Map<String, Long> body,
                              HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Long materialId = body.get("materialId");

        if (materialId == null) {
            return Result.error("物料ID不能为空");
        }

        String token = shareService.createShareToken(materialId, userId);

        Map<String, String> result = new HashMap<>();
        result.put("token", token);
        result.put("shareUrl", "/share/" + token);
        return Result.success("分享链接已生成", result);
    }

    /**
     * 作废分享链接
     * PUT /api/share/invalidate/{token}
     */
    @PutMapping("/invalidate/{token}")
    public Result invalidate(@PathVariable String token) {
        shareService.invalidate(token);
        return Result.success("分享链接已作废", null);
    }
}
