package com.bom.config;

import com.bom.interceptor.AdminInterceptor;
import com.bom.interceptor.LoginInterceptor;
import com.bom.interceptor.RoleInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置（替代原 spring-mvc.xml 中的 &lt;mvc:interceptors&gt;）
 *
 * <p>将三个拦截器按原 XML 定义的路径映射注册到 Spring Boot：
 * 1. LoginInterceptor  —— 全局登录校验（/api/**，放行登录/注册/公开分享/下载/ERP 订阅）
 * 2. RoleInterceptor   —— 图纸模块写操作角色权限（ADMIN/ENGINEER）
 * 3. AdminInterceptor  —— 用户管理类接口仅管理员可访问</p>
 *
 * <p>注意：此处不要加 @EnableWebMvc，否则会关闭 Spring Boot 的 Web 自动配置。</p>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final LoginInterceptor loginInterceptor;
    private final RoleInterceptor roleInterceptor;
    private final AdminInterceptor adminInterceptor;

    @Autowired
    public WebMvcConfig(LoginInterceptor loginInterceptor,
                        RoleInterceptor roleInterceptor,
                        AdminInterceptor adminInterceptor) {
        this.loginInterceptor = loginInterceptor;
        this.roleInterceptor = roleInterceptor;
        this.adminInterceptor = adminInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1. 登录认证：所有 /api/**
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/user/login",
                        "/api/user/register",
                        "/api/share/public/**",
                        "/api/drawing/download/**",
                        "/api/erp/subscribe/**");

        // 2. 角色权限：图纸模块写操作
        registry.addInterceptor(roleInterceptor)
                .addPathPatterns(
                        "/api/material/**",
                        "/api/drawing/upload",
                        "/api/drawing/batch-upload",
                        "/api/drawing/preview/**",
                        "/api/drawing/material/**",
                        "/api/drawing/node/**",
                        "/api/drawing/search",
                        "/api/share/**");

        // 3. 管理员权限
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns(
                        "/api/user/approve/**",
                        "/api/user/reject/**",
                        "/api/user/list",
                        "/api/user/disable/**",
                        "/api/user/reset-password/**",
                        "/api/user/pending");
    }
}
