package com.bom.mapper;

import com.bom.entity.AuditLog;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AuditLogMapper {

    int insert(AuditLog log);

    List<AuditLog> findByUserId(@Param("userId") Long userId);

    List<AuditLog> findRecent(@Param("limit") int limit);
}
