package com.bom.mapper;

import com.bom.entity.Material;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface MaterialMapper extends BaseMapper<Material> {

    int insert(Material material);

    int batchInsert(@Param("list") List<Material> list);

    Material findByCode(@Param("code") String code);

    List<Material> search(@Param("keyword") String keyword);

    List<Material> findAll();

    Material findById(@Param("id") Long id);

    /** 按图号查询物料（匹配 drawing_no 或 specification 字段） */
    Material findByDrawingNo(@Param("drawingNo") String drawingNo);

    int update(Material material);

    /** 更新 ERP 同步字段（按 material_code 匹配），用于 ERP 物料同步 */
    int updateErpFields(Material material);

    /** 更新 ERP 图纸标记与同步时间（按 material_code 匹配） */
    int updateErpHaveDrawing(@Param("code") String code, @Param("flag") Integer flag);

    int deleteById(@Param("id") Long id);

    /** 按物料编码删除（订阅同步删除用） */
    int deleteByCode(@Param("code") String code);

    // ===== 分页查询 =====

    /** 分页查询全部物料（带图纸统计 + 图纸存在性筛选） */
    List<Material> findAllPaged(@Param("offset") int offset, @Param("size") int size,
                                @Param("hasDrawing") Integer hasDrawing,
                                @Param("has3d") Integer has3d,
                                @Param("hasEngineering") Integer hasEngineering);

    /** 全部物料总数（含图纸存在性筛选） */
    long countAll(@Param("hasDrawing") Integer hasDrawing,
                  @Param("has3d") Integer has3d,
                  @Param("hasEngineering") Integer hasEngineering);

    /** 分页搜索物料（带图纸统计 + 图纸存在性筛选） */
    List<Material> searchPaged(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size,
                                @Param("hasDrawing") Integer hasDrawing,
                                @Param("has3d") Integer has3d,
                                @Param("hasEngineering") Integer hasEngineering);

    /** 搜索结果总数（含图纸存在性筛选） */
    long countSearch(@Param("keyword") String keyword,
                     @Param("hasDrawing") Integer hasDrawing,
                     @Param("has3d") Integer has3d,
                     @Param("hasEngineering") Integer hasEngineering);
}
