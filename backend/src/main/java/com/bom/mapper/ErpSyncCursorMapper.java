package com.bom.mapper;

import com.bom.entity.ErpSyncCursor;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * ERP 订阅增量游标。
 */
public interface ErpSyncCursorMapper extends BaseMapper<ErpSyncCursor> {

    ErpSyncCursor findByKey(@Param("cursorKey") String cursorKey);

    int upsert(@Param("cursorKey") String cursorKey, @Param("cursorValue") String cursorValue);
}
