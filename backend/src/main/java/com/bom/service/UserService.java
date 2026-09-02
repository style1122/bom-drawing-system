package com.bom.service;

import com.bom.entity.AuditLog;
import com.bom.entity.SysUser;
import com.bom.exception.BusinessException;
import com.bom.mapper.AuditLogMapper;
import com.bom.mapper.SysUserMapper;
import com.bom.util.MD5Util;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class UserService {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private AuditLogMapper auditLogMapper;

    @Transactional
    public SysUser register(String username, String password, String displayName, String role) {
        SysUser existing = sysUserMapper.findByUsername(username);
        if (existing != null) {
            throw new BusinessException("用户名已存在");
        }
        SysUser user = new SysUser();
        user.setUsername(username);
        user.setPasswordHash(MD5Util.md5(password));
        user.setDisplayName(displayName);
        user.setRole(role != null ? role : "USER");
        user.setStatus("PENDING");
        user.setLoginFailures(0);
        user.setCreatedAt(new Date());
        sysUserMapper.insert(user);
        return user;
    }

    public SysUser login(String username, String password) {
        SysUser user = sysUserMapper.findByUsername(username);
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }

        String passwordHash = MD5Util.md5(password);
        if (!passwordHash.equals(user.getPasswordHash())) {
            // 登录失败递增计数器
            int failures = (user.getLoginFailures() == null ? 0 : user.getLoginFailures()) + 1;
            user.setLoginFailures(failures);
            // 5次失败锁定30分钟
            if (failures >= 5) {
                user.setLockedUntil(new Date(System.currentTimeMillis() + 30 * 60 * 1000));
            }
            sysUserMapper.update(user);
            throw new BusinessException("用户名或密码错误");
        }

        // 检查状态
        if ("PENDING".equals(user.getStatus())) {
            throw new BusinessException("账户正在审核中，请等待管理员审核");
        }
        if ("REJECTED".equals(user.getStatus())) {
            throw new BusinessException("账户已被驳回: " + user.getRejectReason());
        }
        if ("LOCKED".equals(user.getStatus())) {
            if (user.getLockedUntil() != null && user.getLockedUntil().after(new Date())) {
                throw new BusinessException("账户已被锁定，请稍后再试");
            }
            // 锁定已过期，自动解锁
            user.setStatus("ACTIVE");
            user.setLoginFailures(0);
            user.setLockedUntil(null);
        }

        // 登录成功，清零失败计数
        user.setLoginFailures(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(new Date());
        sysUserMapper.update(user);

        // 清除密码
        user.setPasswordHash(null);
        return user;
    }

    @Transactional
    public void approveUser(Long adminId, Long userId) {
        sysUserMapper.updateStatus(userId, "ACTIVE", null, adminId);
        AuditLog log = new AuditLog();
        log.setUserId(adminId);
        log.setOperation("APPROVE_USER");
        log.setTargetType("USER");
        log.setTargetId(userId);
        log.setDetail("审核通过用户");
        log.setCreatedAt(new Date());
        auditLogMapper.insert(log);
    }

    @Transactional
    public void rejectUser(Long adminId, Long userId, String reason) {
        sysUserMapper.updateStatus(userId, "REJECTED", reason, adminId);
        AuditLog log = new AuditLog();
        log.setUserId(adminId);
        log.setOperation("REJECT_USER");
        log.setTargetType("USER");
        log.setTargetId(userId);
        log.setDetail("驳回用户: " + reason);
        log.setCreatedAt(new Date());
        auditLogMapper.insert(log);
    }

    public List<SysUser> getPendingUsers() {
        return sysUserMapper.findPendingUsers();
    }

    public List<SysUser> getAllUsers() {
        List<SysUser> users = sysUserMapper.findAll();
        for (SysUser u : users) {
            u.setPasswordHash(null);
        }
        return users;
    }

    public void disableUser(Long userId) {
        SysUser user = sysUserMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus("DISABLED");
        sysUserMapper.update(user);
    }

    @Transactional
    public void resetPassword(Long userId) {
        SysUser user = sysUserMapper.findById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPasswordHash(MD5Util.md5("123456"));
        sysUserMapper.update(user);
    }

    public SysUser getById(Long id) {
        return sysUserMapper.findById(id);
    }
}
