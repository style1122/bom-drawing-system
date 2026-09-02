package com.bom.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 分享页面路由控制器
 * 生产环境部署 war 包后，/share/{token} 需要返回 index.html，
 * 由 Vue Router 处理 /share/:token 前端路由。
 */
@Controller
@RequestMapping("/share")
public class ShareRouteController {

    @GetMapping("/{token}")
    public void sharePage(@PathVariable String token,
                          HttpServletRequest request,
                          HttpServletResponse response) throws IOException {
        String indexPath = request.getServletContext().getRealPath("/index.html");
        File file = new File(indexPath);
        if (!file.exists()) {
            response.setStatus(404);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":404,\"msg\":\"页面不存在\"}");
            return;
        }

        response.setContentType("text/html;charset=UTF-8");
        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {
            byte[] buffer = new byte[4096];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            os.flush();
        }
    }
}
