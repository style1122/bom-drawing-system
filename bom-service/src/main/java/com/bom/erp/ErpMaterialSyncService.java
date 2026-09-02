package com.bom.erp;

import com.bom.entity.Material;
import com.bom.exception.BusinessException;
import com.bom.mapper.DrawingMapper;
import com.bom.mapper.MaterialMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 正航 T9 ERP 物料基础数据同步服务。
 * <p>
 * 从 ERP 的 Mat01 接口分页拉取物料清单，按物料编码 upsert 到本地 material 表，
 * 并标记 source=ERP、记录 erp_sync_time。
 */
@Service
public class ErpMaterialSyncService {

    private static final Logger logger = LoggerFactory.getLogger(ErpMaterialSyncService.class);

    @Autowired
    private ErpApiClient erpApiClient;

    @Autowired
    private MaterialMapper materialMapper;

    @Autowired
    private DrawingMapper drawingMapper;

    /** 防止同步任务并发执行 */
    private final AtomicBoolean syncing = new AtomicBoolean(false);

    private volatile ErpSyncResult lastResult;

    /**
     * 手动全量/按条件同步物料基础数据（前端“ERP物料同步”按钮）。
     *
     * @param condition ERP 侧 where 条件，可为 null 表示全量
     */
    public ErpSyncResult syncMaterials(String condition) {
        if (!syncing.compareAndSet(false, true)) {
            throw new BusinessException("ERP 物料同步正在进行中，请稍后再试");
        }
        try {
            ErpSyncResult result = doSync(condition);
            lastResult = result;
            return result;
        } catch (Exception e) {
            ErpSyncResult result = new ErpSyncResult();
            result.setSuccess(false);
            result.setMessage("ERP物料同步失败: " + e.getMessage());
            lastResult = result;
            logger.error("ERP物料同步失败", e);
            throw e instanceof BusinessException
                    ? (BusinessException) e
                    : new BusinessException(result.getMessage());
        } finally {
            syncing.set(false);
        }
    }

    /**
     * 执行分页拉取 + upsert 同步。
     *
     * @param condition 查询条件（null/空串=全量）
     */
    private ErpSyncResult doSync(String condition) {
        Date start = new Date();
        ErpSyncResult result = new ErpSyncResult();
        result.setCondition(condition == null ? "" : condition);
        result.setStartTime(start);

        String[] tokenHolder = { erpApiClient.getToken() };
        String lastpkvalues = "";
        int page = 0;
        boolean hasMore = true;
        while (hasMore) {
            ErpGetListResult pageResult = getListWithRetry(tokenHolder, condition, lastpkvalues);
            page++;
            result.setPages(page);

            List<ErpMaterial> materials = pageResult.getMaterials();
            result.setFetched(result.getFetched() + materials.size());
            for (ErpMaterial erpMaterial : materials) {
                try {
                    upsert(erpMaterial, result);
                } catch (Exception e) {
                    result.setFailed(result.getFailed() + 1);
                    logger.error("物料同步失败: {} - {}", erpMaterial.getMaterialId(), e.getMessage(), e);
                }
            }

            lastpkvalues = pageResult.getLastpkvalues();
            hasMore = pageResult.isHasnext()
                    && lastpkvalues != null
                    && !lastpkvalues.trim().isEmpty();

            if (hasMore && materials.isEmpty()) {
                // 防御：hasnext=true 但本页为空，避免死循环
                logger.warn("ERP 返回 hasnext=true 但本页无数据，停止翻页: {}", lastpkvalues);
                hasMore = false;
            }
        }

        Date end = new Date();
        result.setEndTime(end);
        result.setDurationSeconds((end.getTime() - start.getTime()) / 1000L);
        result.setSuccess(true);
        result.setMessage(String.format(
                "ERP物料同步完成：拉取 %d 条，新增 %d 条，更新 %d 条，失败 %d 条，耗时 %d 秒",
                result.getFetched(), result.getInserted(), result.getUpdated(),
                result.getFailed(), result.getDurationSeconds()));
        logger.info(result.getMessage());
        return result;
    }

    /**
     * 分页拉取物料清单；遇到 token 失效时自动重新认证并重试一次。
     */
    private ErpGetListResult getListWithRetry(String[] tokenHolder, String condition, String lastpkvalues) {
        try {
            return erpApiClient.getList(tokenHolder[0], condition, lastpkvalues);
        } catch (BusinessException e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("Token")) {
                logger.warn("ERP token 失效，重新认证后重试");
                tokenHolder[0] = erpApiClient.getToken();
                return erpApiClient.getList(tokenHolder[0], condition, lastpkvalues);
            }
            throw e;
        }
    }

    /**
     * 单条物料 upsert：按物料编码判断存在则更新、否则新增。
     */
    private void upsert(ErpMaterial erpMaterial, ErpSyncResult result) {
        Material material = toMaterial(erpMaterial);
        Material existing = materialMapper.findByCode(material.getMaterialCode());
        if (existing == null) {
            material.setCreatedAt(new Date());
            material.setUpdatedAt(new Date());
            materialMapper.insert(material);
            result.setInserted(result.getInserted() + 1);
        } else {
            material.setId(existing.getId());
            materialMapper.updateErpFields(material);
            result.setUpdated(result.getUpdated() + 1);
        }
    }

    /**
     * 供订阅同步等场景复用的单条物料 upsert（不统计结果）。
     */
    public void upsertMaterial(ErpMaterial erpMaterial) {
        Material material = toMaterial(erpMaterial);
        Material existing = materialMapper.findByCode(material.getMaterialCode());
        if (existing == null) {
            material.setCreatedAt(new Date());
            material.setUpdatedAt(new Date());
            materialMapper.insert(material);
        } else {
            material.setId(existing.getId());
            materialMapper.updateErpFields(material);
        }
    }

    private Material toMaterial(ErpMaterial erpMaterial) {
        String code = erpMaterial.getMaterialId();
        if (!StringUtils.hasText(code)) {
            throw new BusinessException("物料编码为空，跳过");
        }

        Material material = new Material();
        material.setMaterialCode(code.trim());
        material.setMaterialName(nvl(erpMaterial.getMaterialName()));
        material.setSpecification(nvl(erpMaterial.getMaterialSpec()));
        material.setMaterialType(nvl(erpMaterial.getMaterialTypeId()));
        material.setUnit(nvl(erpMaterial.getUnitId()));
        material.setMaterialCategory(nvl(erpMaterial.getMaterialCategoryId()));
        material.setValidityFromDate(nvl(erpMaterial.getValidityFromDate()));
        material.setValidityToDate(nvl(erpMaterial.getValidityToDate()));
        material.setSource("ERP");
        return material;
    }

    private String nvl(String value) {
        return value == null ? "" : value.trim();
    }

    /**
     * 同步单个物料的“是否存在图纸”标记到 ERP（CU_HaveDrawing：1/0）。
     * ERP 更新成功后，将本地 material.erp_have_drawing 一并更新，便于界面展示同步状态。
     *
     * @param materialCode 物料代码（ERP MaterialId）
     * @param haveDrawing  本地是否存在图纸
     */
    public void syncHaveDrawingFlag(String materialCode, boolean haveDrawing) {
        if (materialCode == null || materialCode.trim().isEmpty()) {
            return;
        }
        erpApiClient.updateMaterialHaveDrawing(materialCode, haveDrawing);
        materialMapper.updateErpHaveDrawing(materialCode, haveDrawing ? 1 : 0);
        logger.info("ERP图纸标记同步成功: {} -> {}", materialCode, haveDrawing ? 1 : 0);
    }

    /**
     * 根据本地图纸数量重算并同步某个物料的 ERP 图纸标记。
     * 用于上传/删除图纸后主动修正，或手动触发放置不同步的数据。
     *
     * @param materialCode 物料代码
     * @return 同步结果信息
     */
    public Map<String, Object> syncDrawingFlagForMaterial(String materialCode) {
        Material material = materialMapper.findByCode(materialCode);
        if (material == null) {
            throw new BusinessException("物料不存在: " + materialCode);
        }
        int drawingCount = drawingMapper.countByMaterialId(material.getId());
        boolean haveDrawing = drawingCount > 0;
        syncHaveDrawingFlag(materialCode, haveDrawing);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("materialCode", materialCode);
        result.put("materialName", material.getMaterialName());
        result.put("drawingCount", drawingCount);
        result.put("haveDrawing", haveDrawing);
        result.put("erpFlag", haveDrawing ? 1 : 0);
        result.put("message", String.format("ERP图纸标记已同步：%s -> %d（本地图纸 %d 个）",
                materialCode, haveDrawing ? 1 : 0, drawingCount));
        return result;
    }

    public ErpSyncResult getLastResult() {
        return lastResult;
    }
}
