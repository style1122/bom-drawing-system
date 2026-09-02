package com.bom.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class LoginInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(LoginInterceptor.class);

    /** Token 信息（userId + role） */
    public static class TokenInfo {
        private final Long userId;
        private final String role;

        public TokenInfo(Long userId, String role) {
            this.userId = userId;
            this.role = role;
        }
        public Long getUserId() { return userId; }
        public String getRole() { return role; }
    }

    // 内存 Token 存储: token -> TokenInfo(userId, role)
    public static final Map<String, TokenInfo> TOKEN_MAP = new ConcurrentHashMap<>();
    // userId -> token 反向映射
    public static final Map<Long, String> USER_TOKEN_MAP = new ConcurrentHashMap<>();

    public static String generateToken(Long userId, String role) {
        String token = UUID.randomUUID().toString().replace("-", "");
        TOKEN_MAP.put(token, new TokenInfo(userId, role));
        USER_TOKEN_MAP.put(userId, token);
        return token;
    }

    public static Long getUserIdByToken(String token) {
        TokenInfo info = TOKEN_MAP.get(token);
        return info != null ? info.getUserId() : null;
    }

    public static void removeToken(Long userId) {
        String token = USER_TOKEN_MAP.remove(userId);
        if (token != null) {
            TOKEN_MAP.remove(token);
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String uri = request.getRequestURI();

        // 放行登录、注册、公开分享接口和图纸下载/预览接口
        // 图纸下载接口放行原因：Chrome PDF 查看器会二次请求但丢失 token
        if (uri.contains("/api/user/login") || uri.contains("/api/user/register")
                || uri.contains("/api/share/public/")
                || uri.contains("/api/drawing/download/")
                || uri.contains("/api/erp/subscribe/")) {
            return true;
        }

        // 从请求头获取 token，支持 Bearer 前缀
        String token = request.getHeader("Authorization");
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (token == null || token.isEmpty()) {
            token = request.getParameter("token");
        }

        if (token == null || token.isEmpty()) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"未登录，请先登录\",\"data\":null}");
            return false;
        }

        TokenInfo tokenInfo = TOKEN_MAP.get(token);
        if (tokenInfo == null) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"Token已失效，请重新登录\",\"data\":null}");
            return false;
        }

        // 将 userId 和 userRole 存入 request attribute
        request.setAttribute("userId", tokenInfo.getUserId());
        request.setAttribute("userRole", tokenInfo.getRole());
        return true;
    }
}
