package com.bom.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 图纸管理模块角色权限拦截器。
 *
 * 权限规则：
 * 1. 所有已登录角色（管理员 ADMIN、研发工程师 ENGINEER、生产 PRODUCTION、采购 PURCHASER）
 *    均可查看图纸列表、图纸信息与预览（GET 请求）。
 * 2. 只有管理员（ADMIN）和研发工程师（ENGINEER）可以执行写操作（POST/PUT/DELETE），
 *    即上传图纸、删除图纸/物料、新增/修改/导入物料、ERP 同步与分享等。
 * 3. 生产（PRODUCTION）与采购（PURCHASER）角色为只读：可查看图纸列表并下载图纸，
 *    但不能上传或删除图纸。
 *
 * 运行在 LoginInterceptor 之后，读取 request attribute "userRole"。
 * 若 userRole 为 null（LoginInterceptor 放行的公开接口，如 /api/share/public/**），则直接放行。
 */
@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String role = (String) request.getAttribute("userRole");

        // LoginInterceptor 放行的公开接口（如 /api/share/public/**），userRole 为 null
        if (role == null) {
            return true;
        }

        // 只读操作（GET）：查看图纸列表 / 图纸信息 / 预览，所有角色均可访问
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 写操作（上传、删除、新增、修改、同步、分享等）：仅管理员和研发工程师
        if ("ADMIN".equals(role) || "ENGINEER".equals(role)) {
            return true;
        }

        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"msg\":\"权限不足，仅管理员和研发工程师可上传、删除图纸及管理物料\",\"data\":null}");
        return false;
    }
}
