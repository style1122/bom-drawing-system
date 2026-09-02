package com.bom.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 角色权限拦截器：仅允许 ADMIN、ENGINEER 和 PRODUCTION 访问图纸管理相关接口。
 * 采购人员（PURCHASER）只能访问采购订单管理和仪表盘接口。
 *
 * 运行在 LoginInterceptor 之后，读取 request attribute "userRole"。
 * 若 userRole 为 null（LoginInterceptor 放行的公开接口），则直接放行。
 */
@Component
public class RoleInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String role = (String) request.getAttribute("userRole");

        // LoginInterceptor 放行的公开接口（如 /api/drawing/download/），userRole 为 null
        if (role == null) {
            return true;
        }

        // 管理员、研发工程师和生产人员可访问图纸管理模块
        if ("ADMIN".equals(role) || "ENGINEER".equals(role) || "PRODUCTION".equals(role)) {
            return true;
        }

        // 采购人员无权访问
        response.setStatus(403);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"msg\":\"权限不足，图纸管理模块仅研发工程师和生产人员可访问\",\"data\":null}");
        return false;
    }
}
