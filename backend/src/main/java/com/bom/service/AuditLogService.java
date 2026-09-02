package com.bom.service;

import com.bom.entity.AuditLog;
import com.bom.mapper.AuditLogMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class AuditLogService {

    @Autowired
    private AuditLogMapper auditLogMapper;

    public void log(Long userId, String operation, String targetType, Long targetId, String detail, String ipAddress) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setOperation(operation);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetail(detail);
        log.setIpAddress(ipAddress);
        log.setCreatedAt(new Date());
        auditLogMapper.insert(log);
    }

    public List<AuditLog> getRecent(int limit) {
        return auditLogMapper.findRecent(limit);
    }
}
