package com.bom.controller;

import com.bom.entity.Drawing;
import com.bom.service.ShareService;
import com.bom.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 公开分享页面 API（无需登录认证）
 * 路径 /share/** 不在 /api/** 下，不会被 LoginInterceptor 拦截
 */
@RestController
@RequestMapping("/api/share/public")
public class SharePageController {

    @Autowired
    private ShareService shareService;

    /**
     * 获取分享页数据（物料信息 + 图纸列表）
     * GET /api/share/public/{token}/data
     */
    @GetMapping("/{token}/data")
    public Result getShareData(@PathVariable String token) {
        try {
            Map<String, Object> data = shareService.getShareData(token);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 下载/预览图纸文件（公开访问）
     * GET /api/share/public/{token}/download/{drawingId}?inline=true
     */
    @GetMapping("/{token}/download/{drawingId}")
    public void download(@PathVariable String token,
                         @PathVariable Long drawingId,
                         @RequestParam(required = false, defaultValue = "false") Boolean inline,
                         HttpServletResponse response) {
        try {
            Drawing drawing = shareService.getShareDrawing(drawingId, token);
            String filePath = shareService.getShareDrawingPath(drawingId, token);
            File file = new File(filePath);

            if (!file.exists()) {
                response.setStatus(404);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":404,\"msg\":\"文件不存在\"}");
                return;
            }

            // 根据扩展名设置 Content-Type
            String fileName = drawing.getDrawingName();
            String ext = "";
            int dotIndex = fileName == null ? -1 : fileName.lastIndexOf('.');
            if (dotIndex > 0) {
                ext = fileName.substring(dotIndex + 1).toLowerCase();
            }
            String contentType;
            switch (ext) {
                case "pdf":  contentType = "application/pdf"; break;
                case "dwg":  contentType = "application/acad"; break;
                case "dxf":  contentType = "application/dxf"; break;
                case "step":
                case "stp":  contentType = "application/step"; break;
                case "iges":
                case "igs":  contentType = "application/iges"; break;
                default:     contentType = "application/octet-stream";
            }
            response.setContentType(contentType);

            // RFC 5987 文件名编码
            String encodedName = URLEncoder.encode(
                    fileName == null ? file.getName() : fileName,
                    StandardCharsets.UTF_8.name()).replace("+", "%20");
            String disposition = inline ? "inline" : "attachment";
            response.setHeader("Content-Disposition",
                    disposition + "; filename*=UTF-8''" + encodedName);
            response.setContentLengthLong(file.length());

            // 输出文件流
            try (FileInputStream fis = new FileInputStream(file);
                 OutputStream os = response.getOutputStream()) {
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
                os.flush();
            }
        } catch (Exception e) {
            try {
                response.setStatus(500);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":500,\"msg\":\"下载失败\"}");
            } catch (IOException ignored) {}
        }
    }
}
