package com.bom.service;

import com.bom.entity.Material;
import com.bom.exception.BusinessException;
import com.bom.mapper.MaterialMapper;
import com.bom.util.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class MaterialService {

    @Autowired
    private MaterialMapper materialMapper;

    public Material add(Material material) {
        Material existing = materialMapper.findByCode(material.getMaterialCode());
        if (existing != null) {
            throw new BusinessException("物料编码已存在: " + material.getMaterialCode());
        }
        material.setCreatedAt(new Date());
        material.setUpdatedAt(new Date());
        materialMapper.insert(material);
        return material;
    }

    @Transactional
    public void batchImport(List<Material> list) {
        if (list != null && !list.isEmpty()) {
            Date now = new Date();
            for (Material m : list) {
                m.setCreatedAt(now);
                m.setUpdatedAt(now);
            }
            materialMapper.batchInsert(list);
        }
    }

    public int update(Material material) {
        material.setUpdatedAt(new Date());
        return materialMapper.update(material);
    }

    public int delete(Long id) {
        return materialMapper.deleteById(id);
    }

    public List<Material> search(String keyword) {
        return materialMapper.search(keyword);
    }

    public Material getByCode(String code) {
        return materialMapper.findByCode(code);
    }

    public Material getById(Long id) {
        return materialMapper.findById(id);
    }

    public List<Material> getAll() {
        return materialMapper.findAll();
    }

    // ===== 分页查询 =====

    /**
     * 分页查询全部物料
     *
     * @param page 页码（从 1 开始）
     * @param size 每页条数
     */
    public PageResult<Material> getAllPaged(int page, int size,
                                            Integer hasDrawing, Integer has3d, Integer hasEngineering) {
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        int offset = (page - 1) * size;
        List<Material> list = materialMapper.findAllPaged(offset, size, hasDrawing, has3d, hasEngineering);
        long total = materialMapper.countAll(hasDrawing, has3d, hasEngineering);
        return new PageResult<>(list, total, page, size);
    }

    /**
     * 分页搜索物料
     *
     * @param keyword 关键词
     * @param page    页码（从 1 开始）
     * @param size    每页条数
     * @param hasDrawing     图纸存在性筛选（null=不过滤，1=有PDF，0=无）
     * @param has3d          三维存在性筛选（null=不过滤，1=有，0=无）
     * @param hasEngineering 工程图存在性筛选（null=不过滤，1=有，0=无）
     */
    public PageResult<Material> searchPaged(String keyword, int page, int size,
                                            Integer hasDrawing, Integer has3d, Integer hasEngineering) {
        if (keyword == null || keyword.isEmpty()) {
            return getAllPaged(page, size, hasDrawing, has3d, hasEngineering);
        }
        if (page < 1) page = 1;
        if (size < 1) size = 20;
        int offset = (page - 1) * size;
        List<Material> list = materialMapper.searchPaged(keyword, offset, size, hasDrawing, has3d, hasEngineering);
        long total = materialMapper.countSearch(keyword, hasDrawing, has3d, hasEngineering);
        return new PageResult<>(list, total, page, size);
    }
}
