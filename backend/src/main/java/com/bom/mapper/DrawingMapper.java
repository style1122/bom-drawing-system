package com.bom.mapper;

import com.bom.entity.Drawing;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface DrawingMapper extends BaseMapper<Drawing> {

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

    /** 近 days 天每日上传图纸数量（按创建日期分组） */
    List<Map<String, Object>> countByDay(@Param("days") int days);

    /** 近 days 天每日新增文件占用空间（按创建日期分组，单位字节） */
    List<Map<String, Object>> sumSizeByDay(@Param("days") int days);

    /** 今日（服务器当天）上传图纸数量 */
    int countToday();

    /** 图纸文件总占用空间（字节，无记录返回 0） */
    long sumTotalSize();
}
