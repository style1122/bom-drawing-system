package com.bom.controller;

import com.bom.entity.SysUser;
import com.bom.interceptor.LoginInterceptor;
import com.bom.service.UserService;
import com.bom.util.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;

    // ========== 请求体 DTO ==========
    public static class RegisterRequest {
        private String username;
        private String password;
        private String displayName;
        private String role;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    public static class LoginRequest {
        private String username;
        private String password;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RejectRequest {
        private String reason;

        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    // ========== 用户接口 ==========
    @PostMapping("/register")
    public Result register(@RequestBody RegisterRequest req) {
        // 角色校验：仅允许研发、采购和生产
        String role = req.getRole();
        if (role == null || role.isEmpty()) {
            role = "ENGINEER";
        }
        if (!"ENGINEER".equals(role) && !"PURCHASER".equals(role) && !"PRODUCTION".equals(role)) {
            return Result.error("无效的角色，请选择研发、采购或生产");
        }
        SysUser user = userService.register(req.getUsername(), req.getPassword(), req.getDisplayName(), role);
        user.setPasswordHash(null);
        return Result.success("注册成功，请等待管理员审核", user);
    }

    @PostMapping("/login")
    public Result login(@RequestBody LoginRequest req) {
        SysUser user = userService.login(req.getUsername(), req.getPassword());
        // 生成 token（携带角色信息）
        String token = LoginInterceptor.generateToken(user.getId(), user.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("user", user);
        data.put("token", token);
        return Result.success("登录成功", data);
    }

    @GetMapping("/pending")
    public Result getPendingUsers() {
        List<SysUser> users = userService.getPendingUsers();
        for (SysUser u : users) {
            u.setPasswordHash(null);
        }
        return Result.success(users);
    }

    @PutMapping("/approve/{id}")
    public Result approveUser(@PathVariable Long id, HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("userId");
        userService.approveUser(adminId, id);
        return Result.success("审核通过", null);
    }

    @PutMapping("/reject/{id}")
    public Result rejectUser(@PathVariable Long id,
                             @RequestBody RejectRequest req,
                             HttpServletRequest request) {
        Long adminId = (Long) request.getAttribute("userId");
        userService.rejectUser(adminId, id, req.getReason());
        return Result.success("已驳回", null);
    }

    @GetMapping("/list")
    public Result getAllUsers() {
        List<SysUser> users = userService.getAllUsers();
        return Result.success(users);
    }

    @PutMapping("/disable/{id}")
    public Result disableUser(@PathVariable Long id) {
        userService.disableUser(id);
        return Result.success("用户已停用", null);
    }

    @PutMapping("/reset-password/{id}")
    public Result resetPassword(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.success("密码已重置为 123456", null);
    }

    @GetMapping("/current")
    public Result getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) {
            return Result.error(401, "未登录");
        }
        SysUser user = userService.getById(userId);
        if (user != null) {
            user.setPasswordHash(null);
        }
        return Result.success(user);
    }
}
