package com.bom.interceptor;

import com.bom.entity.SysUser;
import com.bom.mapper.SysUserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class AdminInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AdminInterceptor.class);

    @Autowired
    private SysUserMapper sysUserMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Long userId = (Long) request.getAttribute("userId");

        if (userId == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录\",\"data\":null}");
            return false;
        }

        SysUser user = sysUserMapper.findById(userId);
        if (user == null || !"ADMIN".equals(user.getRole())) {
            response.setStatus(403);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"msg\":\"权限不足，仅管理员可操作\",\"data\":null}");
            return false;
        }

        return true;
    }
}
