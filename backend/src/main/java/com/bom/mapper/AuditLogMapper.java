package com.bom.mapper;

import com.bom.entity.AuditLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface AuditLogMapper extends BaseMapper<AuditLog> {

    int insert(AuditLog log);

    List<AuditLog> findByUserId(@Param("userId") Long userId);

    List<AuditLog> findRecent(@Param("limit") int limit);

    /** 今日（服务器当天）指定操作的记录数量，用于仪表盘统计 */
    int countTodayByOperation(@Param("operation") String operation);
}
