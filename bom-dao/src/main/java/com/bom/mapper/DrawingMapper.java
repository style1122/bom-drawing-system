package com.bom.mapper;

import com.bom.entity.Drawing;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DrawingMapper {

    int insert(Drawing drawing);

    Drawing findById(@Param("id") Long id);

    Drawing findByCode(@Param("code") String code);

    List<Drawing> search(@Param("keyword") String keyword);

    List<Drawing> findByBomNodeId(@Param("bomNodeId") Long bomNodeId);

    List<Drawing> findByMaterialId(@Param("materialId") Long materialId);

    /** 统计指定物料的图纸数量 */
    int countByMaterialId(@Param("materialId") Long materialId);

    /** 删除指定物料的所有图纸记录（订阅同步删除物料时用） */
    int deleteByMaterialId(@Param("materialId") Long materialId);

    int updatePreviewStatus(@Param("id") Long id, @Param("previewStatus") String previewStatus,
                            @Param("previewPath") String previewPath);

    List<Drawing> findAll();

    int update(Drawing drawing);
}
