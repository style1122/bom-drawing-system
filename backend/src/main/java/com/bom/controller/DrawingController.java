package com.bom.controller;

import com.bom.entity.Drawing;
import com.bom.interceptor.LoginInterceptor;
import com.bom.service.AuditLogService;
import com.bom.service.DrawingService;
import com.bom.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drawing")
public class DrawingController {

    private static final Logger logger = LoggerFactory.getLogger(DrawingController.class);

    @Autowired
    private DrawingService drawingService;

    @Autowired
    private AuditLogService auditLogService;

    @PostMapping("/upload")
    public Result upload(@RequestParam("file") MultipartFile file,
                         @RequestParam(value = "materialId", required = false) Long materialId,
                         @RequestParam(value = "bomNodeId", required = false) Long bomNodeId,
                         HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<Drawing> drawings = drawingService.upload(file, bomNodeId, materialId, userId);
        // 记录操作日志（ZIP解压可能产生多条记录）
        for (Drawing d : drawings) {
            auditLogService.log(userId, "上传", "DRAWING", d.getId(),
                    d.getDrawingName(), request.getRemoteAddr());
        }
        String msg = drawings.size() == 1 ? "上传成功" : "上传成功，共解压 " + drawings.size() + " 个文件";
        return Result.success(msg, drawings);
    }

    /**
     * 批量上传多个图纸文件，根据文件名中的图号自动匹配物料。
     * 文件命名规范："图号 名称.扩展名"，第一个空格前为图号。
     */
    @PostMapping("/batch-upload")
    public Result batchUpload(@RequestParam("files") MultipartFile[] files,
                              HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        Map<String, Object> result = drawingService.batchUpload(files, userId);

        // 记录审计日志（仅匹配成功的图纸）
        @SuppressWarnings("unchecked")
        List<Drawing> drawings = (List<Drawing>) result.get("drawings");
        if (drawings != null) {
            for (Drawing d : drawings) {
                auditLogService.log(userId, "批量上传", "DRAWING", d.getId(),
                        d.getDrawingName(), request.getRemoteAddr());
            }
        }

        int matched = (int) result.get("matched");
        int unmatched = (int) result.get("unmatched");
        int skipped = (int) result.get("skipped");
        String msg = String.format("上传完成：共%d个文件，成功匹配%d个，未匹配%d个", 
                files.length, matched, unmatched + skipped);
        return Result.success(msg, result);
    }

    @GetMapping("/download/{id}")
    public void download(@PathVariable Long id,
                         @RequestParam(required = false, defaultValue = "false") Boolean inline,
                         HttpServletRequest request,
                         HttpServletResponse response) {
        try {
            Drawing drawing = drawingService.getById(id);
            if (drawing == null) {
                response.setStatus(404);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":404,\"msg\":\"图纸不存在\",\"data\":null}");
                return;
            }

            // 记录下载审计（失败不影响下载本身）
            // 下载接口被放行登录校验，request 中可能没有 userId；
            // 优先从 token 解析真实用户，拿不到则降级为匿名哨兵值（user_id 非空约束）。
            try {
                Long uid = (Long) request.getAttribute("userId");
                if (uid == null) {
                    String token = request.getHeader("Authorization");
                    if (token != null && token.startsWith("Bearer ")) {
                        token = token.substring(7);
                    }
                    if (token == null || token.isEmpty()) {
                        token = request.getParameter("token");
                    }
                    if (token != null && !token.isEmpty()) {
                        uid = LoginInterceptor.getUserIdByToken(token);
                    }
                }
                if (uid == null) {
                    uid = 0L; // 匿名 / 公开分享下载
                }
                auditLogService.log(uid, "下载", "DRAWING", id, drawing.getDrawingName(), request.getRemoteAddr());
            } catch (Exception ignored) {
                logger.warn("记录下载审计失败 drawingId={}", id, ignored);
            }

            String filePath = drawingService.download(id);
            File file = new File(filePath);
            if (!file.exists()) {
                response.setStatus(404);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"code\":404,\"msg\":\"文件不存在\",\"data\":\"" + filePath.replace("\\", "/") + "\"}");
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
                case "pdf":
                    contentType = "application/pdf";
                    break;
                case "sldprt":
                case "sldasm":
                    contentType = "application/octet-stream";
                    break;
                case "slddrw":
                    contentType = "application/octet-stream";
                    break;
                case "dwg":
                    contentType = "application/acad";
                    break;
                case "dxf":
                    contentType = "application/dxf";
                    break;
                case "step":
                case "stp":
                    contentType = "application/step";
                    break;
                case "iges":
                case "igs":
                    contentType = "application/iges";
                    break;
                default:
                    contentType = "application/octet-stream";
            }
            response.setContentType(contentType);
            // 移除 CharacterEncodingFilter 追加的 charset=UTF-8（PDF 是二进制）
            response.setCharacterEncoding(null);

            // RFC 5987 编码文件名，支持中文和特殊字符
            String encodedName = URLEncoder.encode(fileName == null ? file.getName() : fileName, StandardCharsets.UTF_8.name())
                    .replace("+", "%20");
            String disposition = inline ? "inline" : "attachment";
            response.setHeader("Content-Disposition",
                    disposition + "; filename*=UTF-8''" + encodedName);
            response.setContentLengthLong(file.length());

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
            logger.error("下载图纸失败 drawingId={}", id, e);
            try {
                // 避免 response 已被提交时再次写入
                if (!response.isCommitted()) {
                    response.reset();
                    response.setStatus(500);
                    response.setContentType("application/json;charset=UTF-8");
                    response.getWriter().write("{\"code\":500,\"msg\":\"下载失败:" + e.getMessage() + "\",\"data\":null}");
                }
            } catch (IOException ignored) {
            }
        }
    }

    @GetMapping("/preview/{id}")
    public Result preview(@PathVariable Long id) {
        Drawing drawing = drawingService.getById(id);
        if (drawing == null) {
            return Result.error("图纸不存在");
        }
        return Result.success(drawing);
    }

    @GetMapping("/material/{materialId}")
    public Result getByMaterialId(@PathVariable Long materialId) {
        List<Drawing> drawings = drawingService.getByMaterialId(materialId);
        return Result.success(drawings);
    }

    @GetMapping("/node/{bomNodeId}")
    public Result getByBomNodeId(@PathVariable Long bomNodeId) {
        List<Drawing> drawings = drawingService.getByBomNodeId(bomNodeId);
        return Result.success(drawings);
    }

    @GetMapping("/search")
    public Result search(@RequestParam(required = false) String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return Result.error("请输入搜索关键词");
        }
        return Result.success(drawingService.search(keyword));
    }
}
