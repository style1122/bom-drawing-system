package com.bom.mapper;

import com.bom.entity.Requisition;
import com.bom.entity.RequisitionItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RequisitionMapper extends BaseMapper<Requisition> {

    List<Requisition> findAllPaged(@Param("offset") int offset, @Param("size") int size, @Param("purchaserName") String purchaserName);

    long countAll(@Param("purchaserName") String purchaserName);

    List<Requisition> searchPaged(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size, @Param("purchaserName") String purchaserName);

    long countSearch(@Param("keyword") String keyword, @Param("purchaserName") String purchaserName);

    Requisition findById(@Param("id") Long id);

    List<RequisitionItem> findItemsByRequisitionId(@Param("requisitionId") Long requisitionId);
}
