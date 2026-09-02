package com.bom.mapper;

import com.bom.entity.ErpSyncCursor;
import org.apache.ibatis.annotations.Param;

/**
 * ERP 订阅增量游标。
 */
public interface ErpSyncCursorMapper {

    ErpSyncCursor findByKey(@Param("cursorKey") String cursorKey);

    int upsert(@Param("cursorKey") String cursorKey, @Param("cursorValue") String cursorValue);
}
